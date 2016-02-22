package beast.evolution.speciation;


import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math.MathException;

import beast.core.Citation;
import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.core.util.CompoundDistribution;
import beast.core.util.Log;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeInterface;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.ParametricDistribution;
import beast.math.statistic.RPNcalculator;

/**
 * @author Joseph Heled
 */


@Description("Yule prior with calibrated monophyletic clades. With this prior, the marginal distribution of the" +
        " calibrated nodes (the MRCA of clades) is identical to the specified calibration, but the Yule is not preserved over" +
        " the whole tree space, only among sub-spaces.")
@Citation(value =
        "Heled J, Drummond AJ (2012) Calibrated Tree Priors for Relaxed Phylogenetics\n" +
                "  and Divergence Time Estimation. Systematic Biology 61(1):138-149."
        , DOI = "10.1093/sysbio/syr087", year = 2012, firstAuthorSurname = "heled")
public class CalibratedYuleModel extends SpeciesTreeDistribution {

    public static enum Type {
        NONE("none"),
        OVER_ALL_TOPOS("full"),
        OVER_RANKED_COUNTS("restricted");

        Type(final String name) {
            this.ename = name;
        }

        @Override
		public String toString() {
            return ename;
        }

        private final String ename;
    }

    // Q2R does this makes sense, or it has to be a realParameter??
    final public Input<RealParameter> birthRateInput =
            new Input<>("birthRate", "birth rate of splitting a linage into two", Validate.REQUIRED);

    final public Input<List<CalibrationPoint>> calibrationsInput =
            new Input<>("calibrations", "Set of calibrated nodes", new ArrayList<>());

    final public Input<Type> correctionTypeInput = new Input<>("type", "Type of correction: none for no correction " +
            "(same as BEAST1), full for Yule-like over calibrated times, and restricted for Yule-like over calibrated" +
            " times and ranked topology (default 'full'). However, 'full'" +
            " is generally slow except for in a few special cases, such as a single clade or two nested clades.",
            Type.OVER_ALL_TOPOS, Type.values());

    final public Input<RPNcalculator> userMarInput = new Input<>("logMarginal",
            "Used provided correction (log of marginal) for special cases.", (RPNcalculator) null);

    // Which correction to apply
    private Type type;

    // Calibration points, (partially) sorted by set inclusion operator on clades. (remember that partially overlapping clades are not allowed)
    CalibrationPoint[] orderedCalibrations;

    // taxa of calibrated points, in same order as 'orderedCalibrations' above. The clade is represented as an array of integers, where each
    // integer is the "node index" of the taxon in the tree, that is tree.getNode(xclades[i][k]) is the node for the k'th taxon of the i'th point.
    private int[][] xclades;

    // taxaPartialOrder[i] contains all clades immediately preceding the i'th clade under clade partial ordering.
    // (i'th clade is orderedCalibrations[i]/xclades[i]). clades are given as their index into orderedCalibrations (and so into xclades as well).
    private int[][] taxaPartialOrder;

    RPNcalculator userPDF = null; //Q2R  but would that work propagation-wise

    // whether to calculated the contribution of each of the calibrations
    // should be false, when the calibrations come from MRCA priors of a parent CompoundDistribution 
    boolean calcCalibrations = true;

    public CalibratedYuleModel() {
    }

    @Override
    public void initAndValidate() {
        super.initAndValidate();

        type = correctionTypeInput.get();

        final TreeInterface tree = treeInput.get();

        // shallow copy. we shall change cals later
        final List<CalibrationPoint> cals = new ArrayList<>(calibrationsInput.get());
        int calCount = cals.size();
        final List<TaxonSet> taxaSets = new ArrayList<>(calCount);
        if (cals.size() > 0) {
            xclades = new int[calCount][];

            // convenience
            for (final CalibrationPoint cal : cals) {
                taxaSets.add(cal.taxa());
            }

        } else {
            // find calibration points from prior
            for (final Object beastObject : getOutputs()) {
                if (beastObject instanceof CompoundDistribution) {
                    final CompoundDistribution prior = (CompoundDistribution) beastObject;
                    for (final Distribution distr : prior.pDistributions.get()) {
                        if (distr instanceof MRCAPrior) {
                            final MRCAPrior _MRCAPrior = (MRCAPrior) distr;
                            // make sure MRCAPrior is monophyletic
                            if (_MRCAPrior.distInput.get() != null) {
                                // make sure MRCAPrior is monophyletic
                                if (!_MRCAPrior.isMonophyleticInput.get()) {
                                    throw new IllegalArgumentException("MRCAPriors must be monophyletic for Calibrated Yule prior");
                                }
                                // create CalibrationPoint from MRCAPrior
                                final CalibrationPoint cal = new CalibrationPoint();
                                cal.distInput.setValue(_MRCAPrior.distInput.get(), cal);
                                cal.taxonsetInput.setValue(_MRCAPrior.taxonsetInput.get(), cal);
                                cal.initAndValidate();
                                cals.add(cal);
                                taxaSets.add(cal.taxa());
                                cal.taxa().initAndValidate();
                                calCount++;
                                calcCalibrations = false;
                            } else {
                                if (_MRCAPrior.isMonophyleticInput.get()) {
                                    Log.warning.println("WARNING: MRCAPriors (" + _MRCAPrior.getID() + ") must have a distribution when monophyletic. Ignored for Calibrated Yule prior");
                                } else {
                                	Log.warning.println("WARNING: MRCAPriors (" + _MRCAPrior.getID() + ") found that is not monophyletic. Ignored for Calibrated Yule prior");                                	
                                }
                            }
                        }
                    }
                }
            }
            xclades = new int[calCount][];
        }
        if (calCount == 0) {
        	Log.warning.println("WARNING: Calibrated Yule prior could not find any properly configured calibrations. Expect this to crash in a BEAST run.");                                	
            // assume we are in beauti, back off for now
            return;
        }

        for (int k = 0; k < calCount; ++k) {
            final TaxonSet tk = taxaSets.get(k);
            for (int i = k + 1; i < calCount; ++i) {
                final TaxonSet ti = taxaSets.get(i);
                if (ti.containsAny(tk)) {
                    if (!(ti.containsAll(tk) || tk.containsAll(ti))) {
                        throw new IllegalArgumentException("Overlapping taxaSets??");
                    }
                }
            }
        }

        orderedCalibrations = new CalibrationPoint[calCount];

        {
            int loc = taxaSets.size() - 1;
            while (loc >= 0) {
                assert loc == taxaSets.size() - 1;
                //  place maximal taxaSets at end one at a time
                int k = 0;
                for (/**/; k < taxaSets.size(); ++k) {
                    if (isMaximal(taxaSets, k)) {
                        break;
                    }
                }

                final List<String> tk = taxaSets.get(k).asStringList();
                final int tkcount = tk.size();
                this.xclades[loc] = new int[tkcount];
                for (int nt = 0; nt < tkcount; ++nt) {
                    final int taxonIndex = getTaxonIndex(tree, tk.get(nt));
                    this.xclades[loc][nt] = taxonIndex;
                    if (taxonIndex < 0) {
                        throw new IllegalArgumentException("Taxon not found in tree: " + tk.get(nt));
                    }
                }

                orderedCalibrations[loc] = cals.remove(k);
                taxaSets.remove(k);
                // cals and taxaSets should match
                --loc;
            }
        }

        // tio[i] will contain all taxaSets contained in the i'th clade, in the form of thier index into orderedCalibrations
        @SuppressWarnings("unchecked")
		final List<Integer>[] tio = new List[orderedCalibrations.length];
        for (int k = 0; k < orderedCalibrations.length; ++k) {
            tio[k] = new ArrayList<>();
        }

        for (int k = 0; k < orderedCalibrations.length; ++k) {
            final TaxonSet txk = orderedCalibrations[k].taxa();
            for (int i = k + 1; i < orderedCalibrations.length; ++i) {
                if (orderedCalibrations[i].taxa().containsAll(txk)) {
                    tio[i].add(k);
                    break;
                }
            }
        }

        this.taxaPartialOrder = new int[orderedCalibrations.length][];
        for (int k = 0; k < orderedCalibrations.length; ++k) {
            final List<Integer> tiok = tio[k];

            this.taxaPartialOrder[k] = new int[tiok.size()];
            for (int j = 0; j < tiok.size(); ++j) {
                this.taxaPartialOrder[k][j] = tiok.get(j);
            }
        }

        // true if clade is not contained in any other clade
        final boolean[] maximal = new boolean[calCount];
        for (int k = 0; k < calCount; ++k) {
            maximal[k] = true;
        }

        for (int k = 0; k < calCount; ++k) {
            for (final int i : this.taxaPartialOrder[k]) {
                maximal[i] = false;
            }
        }

        userPDF = userMarInput.get();
        if (userPDF == null) {

            if (type == Type.OVER_ALL_TOPOS) {
                if (calCount == 1) {
                    // closed form formula
                } else {
                    boolean anyParent = false;
                    for (final CalibrationPoint c : orderedCalibrations) {
                        if (c.forParentInput.get()) {
                            anyParent = true;
                        }
                    }
                    if (anyParent) {
                        throw new IllegalArgumentException("Sorry, not implemented: calibration on parent for more than one clade.");
                    }
                    if (calCount == 2 && orderedCalibrations[1].taxa().containsAll(orderedCalibrations[0].taxa())) {
                        // closed form formulas
                    } else {
                        setUpTables(tree.getLeafNodeCount() + 1);
                        linsIter = new CalibrationLineagesIterator(this.xclades, this.taxaPartialOrder, maximal,
                                tree.getLeafNodeCount());
                        lastHeights = new double[calCount];
                    }
                }
            } else if (type == Type.OVER_RANKED_COUNTS) {
                setUpTables(tree.getLeafNodeCount() + 1);
            }
        }

        final List<Node> leafs = tree.getExternalNodes();
        final double height = leafs.get(0).getHeight();
        for (final Node leaf : leafs) {
            if (Math.abs(leaf.getHeight() - height) > 1e-8) {
            	Log.warning.println("WARNING: Calibrated Yule Model cannot handle dated tips. Use for example a coalescent prior instead.");
                break;
            }
        }
    }

    public Tree compatibleInitialTree() throws MathException {
        final int calCount = orderedCalibrations.length;
        final double[] lowBound = new double[calCount];
        final double[] cladeHeight = new double[calCount];

        // get lower  bound: max(lower bound of dist , bounds of nested clades)
        for (int k = 0; k < calCount; ++k) {
            final CalibrationPoint cal = orderedCalibrations[k];
            final ParametricDistribution dist = cal.dist();
            //final double offset = dist.getOffset();
            lowBound[k] = dist.inverseCumulativeProbability(0);
            // those are node heights
            if (lowBound[k] < 0) {
                lowBound[k] = 0;
            }
            for (final int i : taxaPartialOrder[k]) {
                lowBound[k] = Math.max(lowBound[k], lowBound[i]);
            }
            cladeHeight[k] = dist.inverseCumulativeProbability(1);
//            if (! Double.isInfinite(cladeHeight[k])) {
//              cladeHeight[k] += offset;
//            }
        }

        for (int k = calCount - 1; k >= 0; --k) {
            //  cladeHeight[k] should be the upper bound of k
            double upper = cladeHeight[k];
            if (Double.isInfinite(upper)) {
                upper = lowBound[k] + 1;
            }
            cladeHeight[k] = (upper + lowBound[k]) / 2.0;

            for (final int i : taxaPartialOrder[k]) {
                cladeHeight[i] = Math.min(cladeHeight[i], cladeHeight[k]);
            }
        }

        final TreeInterface tree = treeInput.get();
        final int nodeCount = tree.getLeafNodeCount();
        final boolean[] used = new boolean[nodeCount];

        int curLeaf = -1;
        int curInternal = nodeCount - 1;

        final Node[] subTree = new Node[calCount];
        for (int k = 0; k < calCount; ++k) {
            final List<Integer> freeTaxa = new ArrayList<>();
            for (final int ti : xclades[k]) {
                freeTaxa.add(ti);
            }
            for (final int i : taxaPartialOrder[k]) {
                for (final int u : xclades[i]) {
                    freeTaxa.remove(new Integer(u));
                }
            }

            final List<Node> sbs = new ArrayList<>();
            for (final int i : freeTaxa) {
                final Node n = new Node(tree.getNode(i).getID());
                n.setNr(++curLeaf);
                n.setHeight(0.0);
                sbs.add(n);

                used[i] = true;
            }
            for (final int i : taxaPartialOrder[k]) {
                sbs.add(subTree[i]);
                subTree[i] = null;
            }
            final double base = sbs.get(sbs.size() - 1).getHeight();
            final double step = (cladeHeight[k] - base) / (sbs.size() - 1);

            Node tr = sbs.get(0);
            for (int i = 1; i < sbs.size(); ++i) {
                tr = Node.connect(tr, sbs.get(i), base + i * step);
                tr.setNr(++curInternal);
            }
            subTree[k] = tr;
        }

        Node finalTree = subTree[calCount - 1];
        double h = cladeHeight[calCount - 1];

        for (int k = 0; k < calCount - 1; ++k) {
            final Node s = subTree[k];
            if( s != null ) {
                h = Math.max(h, cladeHeight[k]) + 1;
                finalTree = Node.connect(finalTree, s, h);
                finalTree.setNr(++curInternal);
            }
        }

        for (int k = 0; k < used.length; ++k) {
            if (!used[k]) {
                final String tx = tree.getNode(k).getID();
                final Node n = new Node(tx);
                n.setHeight(0.0);
                n.setNr(++curLeaf);
                finalTree = Node.connect(finalTree, n, h + 1);
                finalTree.setNr(++curInternal);
                h += 1;
            }
        }
        final Tree t = new Tree();
        t.setRoot(finalTree);
        t.initAndValidate();
        return t;
    }

    @Override
    public double calculateTreeLogLikelihood(final TreeInterface tree) {
        final double lam = birthRateInput.get().getArrayValue();

        double logL = calculateYuleLikelihood(tree, lam);

        final double mar = getCorrection(tree, lam);
        logL += mar;
        return logL;
    }

    private static double calculateYuleLikelihood(final TreeInterface tree, final double lam) {
        final int taxonCount = tree.getLeafNodeCount();

        // add all lambda multipliers here
        // No normalization at the moment.  for n! use logGamma(taxonCount + 1);
        double logL = (taxonCount - 1) * Math.log(lam);

        final Node[] nodes = tree.getNodesAsArray();
        for (int i = taxonCount; i < nodes.length; i++) {
            final Node node = nodes[i];
            assert (!node.isLeaf());
            final double height = node.getHeight();
            final double mrh = -lam * height;
            logL += mrh + (node.isRoot() ? mrh : 0);
        }
        return logL;
    }

    public double getCorrection(final TreeInterface tree, final double lam) {
        double logL = 0.0;

        final int calCount = orderedCalibrations.length;
        final double[] hs = new double[calCount];

        for (int k = 0; k < calCount; ++k) {
            final CalibrationPoint cal = orderedCalibrations[k];
            Node c;
            final int[] taxk = xclades[k];
            if (taxk.length > 1) {
                //  find MRCA of taxa
                c = getCommonAncestor(tree, taxk);

                // only monophyletics clades can be calibrated
                if (getLeafCount(c) != taxk.length) {
                    return Double.NEGATIVE_INFINITY;
                }
            } else {
                c = tree.getNode(taxk[0]);
                assert cal.forParent();
            }

            if (cal.forParent()) {
                c = c.getParent();
            }

            final double h = c.getHeight();
            // add calibration density for point
            if (calcCalibrations) {
                logL += cal.logPdf(h);
            }

            hs[k] = h;
        }

        if (Double.isInfinite(logL)) {
            // some calibration points out of range
            return logL;
        }

        if (type == Type.NONE) {
            return logL;
        }

        if (userPDF == null) {
            switch (type) {
                case OVER_ALL_TOPOS: {
                    if (calCount == 1) {
                        logL -= logMarginalDensity(lam, tree.getLeafNodeCount(), hs[0], xclades[0].length,
                                orderedCalibrations[0].forParent());
                    } else if (calCount == 2 && taxaPartialOrder[1].length == 1) {
                        //assert !forParent[0] && !forParent[1];
                        logL -= logMarginalDensity(lam, tree.getLeafNodeCount(), hs[0], xclades[0].length,
                                hs[1], xclades[1].length);
                    } else {

                        if (lastLam == lam) {
                            int k = 0;
                            for (; k < hs.length; ++k) {
                                if (hs[k] != lastHeights[k]) {
                                    break;
                                }
                            }
                            if (k == hs.length) {
                                return lastValue;
                            }
                        }

                        // the slow and painful way
                        final double[] hss = new double[hs.length];
                        final int[] ranks = new int[hs.length];
                        for (int k = 0; k < hs.length; ++k) {
                            int r = 0;
                            for (int j = 0; j < k; ++j) {
                                r += (hs[j] <= hs[k]) ? 1 : 0;
                            }
                            for (int j = k + 1; j < hs.length; ++j) {
                                r += (hs[j] < hs[k]) ? 1 : 0;
                            }
//                            for (final double h : hs) {
//                                r += (h < hs[k]) ? 1 : 0;
//                            }
                            ranks[k] = r + 1;
                            hss[r] = hs[k];
                        }
                        logL -= logMarginalDensity(lam, hss, ranks, linsIter);

                        lastLam = lam;
                        System.arraycopy(hs, 0, lastHeights, 0, lastHeights.length);
                        lastValue = logL;
                    }
                    break;
                }

                case OVER_RANKED_COUNTS: {
                    Arrays.sort(hs);
                    final int[] cs = new int[calCount + 1];
                    for (final Node n : tree.getInternalNodes()) {
                        final double nhk = n.getHeight();
                        int i = 0;
                        for (/**/; i < hs.length; ++i) {
                            if (hs[i] >= nhk) {
                                break;
                            }
                        }
                        if (i == hs.length) {
                            cs[i]++;
                        } else {
                            if (nhk < hs[i]) {
                                cs[i]++;
                            }
                        }
                    }

                    double ll = 0;

                    ll += cs[0] * Math.log1p(-Math.exp(-lam * hs[0])) - lam * hs[0] - lfactorials[cs[0]];
                    for (int i = 1; i < cs.length - 1; ++i) {
                        final int c = cs[i];
                        ll += c * (Math.log1p(-Math.exp(-lam * (hs[i] - hs[i - 1]))) - lam * hs[i - 1]);
                        ll += -lam * hs[i] - lfactorials[c];
                    }
                    ll += -lam * (cs[calCount] + 1) * hs[calCount - 1] - lfactorials[cs[calCount] + 1];
                    ll += Math.log(lam) * calCount;

                    logL -= ll;
                    break;
                }
			default:
				break;
            }
        } else {
            final double value = userPDF.getArrayValue();
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                logL = Double.NEGATIVE_INFINITY;
            } else {
                logL -= value;
            }
        }
        return logL;
    }

    private static double logMarginalDensity(final double lam, final int taxonCount, final double h, final int clade,
                                             final boolean forParent) {
        double lgp;

        final double lh = lam * h;

        if (forParent) {
            // n(n+1) factor left out

            lgp = -2 * lh + Math.log(lam);
            if (clade > 1) {
                lgp += (clade - 1) * Math.log(1 - Math.exp(-lh));
            }
        } else {
            assert clade > 1;

            lgp = -3 * lh + (clade - 2) * Math.log(1 - Math.exp(-lh)) + Math.log(lam);

            // root is a special case
            if (taxonCount == clade) {
                // n(n-1) factor left out
                lgp += lh;
            } else {
                // (n^3-n)/2 factor left out
            }
        }

        return lgp;
    }

    private static double logMarginalDensity(final double lam, final int taxonCount, final double h2, final int n,
                                             final double h1, final int nm) {

        assert h2 <= h1 && n < nm;

        final int m = nm - n;

        final double elh2 = Math.exp(-lam * h2);
        final double elh1 = Math.exp(-lam * h1);

        double lgl = 2 * Math.log(lam);

        lgl += (n - 2) * Math.log(1 - elh2);
        lgl += (m - 3) * Math.log(1 - elh1);

        lgl += Math.log(1 - 2 * m * elh1 + 2 * (m - 1) * elh2
                - m * (m - 1) * elh1 * elh2 + (m * (m + 1) / 2.) * elh1 * elh1
                + ((m - 1) * (m - 2) / 2.) * elh2 * elh2);

        if (nm < taxonCount) {
            /* lgl += Math.log(0.5*(n*(n*n-1))*(n+1+m)) */
            lgl -= lam * (h2 + 3 * h1);
        } else {
            /* lgl += Math.log(lam) /* + Math.log(n*(n*n-1)) */
            lgl -= lam * (h2 + 2 * h1);
        }

        return lgl;
    }

    private double logMarginalDensity(final double lam, final double[] hs, final int[] ranks,
                                      final CalibrationLineagesIterator cli) {

        final int ni = cli.setup(ranks);

        final int heights = hs.length;

        final double[] lehs = new double[heights + 1];
        lehs[0] = 0.0;
        for (int i = 1; i < lehs.length; ++i) {
            lehs[i] = -lam * hs[i - 1];
        }

        // assert maxRank == len(sit)
        final boolean noRoot = ni == lehs.length;

        final int levels = heights + (noRoot ? 1 : 0);

        final double[] lebase = new double[levels];

        for (int i = 0; i < heights; ++i) {
            final double d = lehs[i + 1] - lehs[i];
            lebase[i] = d != 0 ? lehs[i] + Math.log1p(-Math.exp(d)) : -50;
        }

        if (noRoot) {
            lebase[heights] = lehs[heights];
        }

        final int[] linsAtLevel = new int[levels];

        final int[][] joiners = cli.allJoiners();

        double val = 0;
        boolean first = true;

        int[][] linsInLevels;
        //int ccc = 0;
        while ((linsInLevels = cli.next()) != null) {
            //ccc++;
            double v = countRankedTrees(levels, linsInLevels, joiners, linsAtLevel);
            // 1 for root formula, 1 for kludge in iterator which sets root as 2 lineages
            if (noRoot) {
                final int ll = linsAtLevel[levels - 1] + 2;
                linsAtLevel[levels - 1] = ll;

                v -= lc2[ll] + lg2;
            }

            for (int i = 0; i < levels; ++i) {
                v += linsAtLevel[i] * lebase[i];
            }

            if (first) {
                val = v;
                first = false;
            } else {
                if (val > v) {
                    val += Math.log1p(Math.exp(v - val));
                } else {
                    val = v + Math.log1p(Math.exp(val - v));
                }
            }
        }

        double logc0 = 0.0;
        int totLin = 0;
        for (int i = 0; i < ni; ++i) {
            final int l = cli.start(i);
            if (l > 0) {
                logc0 += lNR[l];
                totLin += l;
            }
        }

        final double logc1 = lfactorials[totLin];

        double logc2 = heights * Math.log(lam);

        for (int i = 1; i < heights + 1; ++i) {
            logc2 += lehs[i];
        }

        if (!noRoot) {
            // we dont have an iterator for 0 free lineages
            logc2 += 1 * lehs[heights];
        }

        // Missing scale by total of all possible trees over all ranking orders.
        // Add it outside if needed for comparison.

        val += logc0 + logc1 + logc2;

        return val;
    }

    private double
    countRankedTrees(final int levels, final int[][] linsAtCrossings, final int[][] joiners, final int[] linsAtLevel) {
        double logCount = 0;

        for (int i = 0; i < levels; ++i) {
            int sumLins = 0;
            for (int k = i; k < levels; ++k) {
                final int[] lack = linsAtCrossings[k];
                int cki = lack[i];
                if (joiners[k][i] > 0) {
                    ++cki;
                    if (cki > 1) {
                        // can be 1 if iterator without lins - for joiners only - need to check this is correct
                        logCount += lc2[cki];
                    } //assert(cki >= 2);
                }
                final int l = cki - lack[i + 1];   //assert(l >= 0);
                logCount -= lfactorials[l];
                sumLins += l;
            }
            linsAtLevel[i] = sumLins;
        }

        return logCount;
    }

    private CalibrationLineagesIterator linsIter = null;

    double lastLam = Double.NEGATIVE_INFINITY;
    double[] lastHeights;
    double lastValue = Double.NEGATIVE_INFINITY;

    // speedup constants
    private final double lg2 = Math.log(2.0);
    private double[] lc2;
    private double[] lNR;
    private double[] lfactorials;

    private void setUpTables(final int MAX_N) {
        final double[] lints = new double[MAX_N];
        lc2 = new double[MAX_N];
        lfactorials = new double[MAX_N];
        lNR = new double[MAX_N];

        lints[0] = Double.NEGATIVE_INFINITY; //-infinity, should never be used
        lints[1] = 0.0;
        for (int i = 2; i < MAX_N; ++i) {
            lints[i] = Math.log(i);
        }

        lc2[0] = lc2[1] = Double.NEGATIVE_INFINITY;
        for (int i = 2; i < MAX_N; ++i) {
            lc2[i] = lints[i] + lints[i - 1] - lg2;
        }

        lfactorials[0] = 0.0;
        for (int i = 1; i < MAX_N; ++i) {
            lfactorials[i] = lfactorials[i - 1] + lints[i];
        }

        lNR[0] = Double.NEGATIVE_INFINITY; //-infinity, should never be used
        lNR[1] = 0.0;

        for (int i = 2; i < MAX_N; ++i) {
            lNR[i] = lNR[i - 1] + lc2[i];
        }
    }

    // @return true if the k'th taxa is maximal under set inclusion, i.e. it is not contained in any other set
    public static boolean isMaximal(final List<TaxonSet> taxa, final int k) {
        final TaxonSet tk = taxa.get(k);
        for (int i = 0; i < taxa.size(); ++i) {
            if (i != k) {
                if (taxa.get(i).containsAll(tk)) {
                    return false;
                }
            }
        }
        return true;
    }


    // Q2R Those generic functions could find a better home

    public static int getTaxonIndex(final TreeInterface tree, final String taxon) {
        for (int i = 0; i < tree.getNodeCount(); i++) {
            final Node node = tree.getNode(i);
            if (node.isLeaf() && node.getID().equals(taxon)) {
                return i;
            }
        }
        return -1;
    }

    public static Node getCommonAncestor(Node n1, Node n2) {
        // assert n1.getTree() == n2.getTree();
        while (n1 != n2) {
            if (n1.getHeight() < n2.getHeight()) {
                n1 = n1.getParent();
            } else {
                n2 = n2.getParent();
            }
        }
        return n1;
    }

    // A lightweight version for finding the most recent common ancestor of a group of taxa.
    // return the node-ref of the MRCA.

    // would be nice to use nodeRef's, but they are not preserved :(
    public static Node getCommonAncestor(final TreeInterface tree, final int[] nodes) {
        Node cur = tree.getNode(nodes[0]);

        for (int k = 1; k < nodes.length; ++k) {
            cur = getCommonAncestor(cur, tree.getNode(nodes[k]));
        }
        return cur;
    }

    /**
     * Count number of leaves in subtree whose root is node.
     *
     * @param node
     * @return the number of leaves under this node.
     */
    public static int getLeafCount(final Node node) {
        if (node.isLeaf()) {
            return 1;
        }
        return getLeafCount(node.getLeft()) + getLeafCount(node.getRight());
    }

    // log likelihood and clades heights

    @Override
    public void init(final PrintStream out) {
        out.print(getID() + "\t");
        if (calcCalibrations) {
            for (final CalibrationPoint cp : orderedCalibrations) {
                out.print(cp.getID() + "\t");
            }
        }
    }

    @Override
    public void log(final int sample, final PrintStream out) {
        out.print(getCurrentLogP() + "\t");
        if (calcCalibrations) {
            final TreeInterface tree = treeInput.get();
            for (int k = 0; k < orderedCalibrations.length; ++k) {
                final CalibrationPoint cal = orderedCalibrations[k];
                Node c;
                final int[] taxk = xclades[k];
                if (taxk.length > 1) {
                    //  find MRCA of taxa
                    c = getCommonAncestor(tree, taxk);
                } else {
                    c = tree.getNode(taxk[0]);
                }

                if (cal.forParent()) {
                    c = c.getParent();
                }

                final double h = c.getHeight();
                out.print(h + "\t");
            }
        }
    }

    @Override
    protected boolean requiresRecalculation() {
        return super.requiresRecalculation() || birthRateInput.get().somethingIsDirty();
    }

    @Override
    public boolean canHandleTipDates() {
        return false;
    }


}
