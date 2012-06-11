package beast.evolution.speciation;



import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.State;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.speciation.SpeciesTreePrior.PopSizeFunction;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeDistribution;

@Description("Calculates probability of gene tree conditioned on a species tree (as in *BEAST)")
public class GeneTreeForSpeciesTreeDistribution extends TreeDistribution {
    public Input<Tree> m_speciesTree =
            new Input<Tree>("speciesTree", "species tree containing the associated gene tree", Validate.REQUIRED);

//    public enum PLOIDY {autosomal_nuclear, X, Y, mitrochondrial};
    
    public Input<Double> m_ploidy =
            new Input<Double>("ploidy", "ploidy for this gene, typically a whole number of half (default 2 for autosomal_nuclear)", 2.0);
//    public Input<PLOIDY> m_ploidy =
//        new Input<PLOIDY>("ploidy", "ploidy for this gene (default X, Possible values: " + PLOIDY.values(), PLOIDY.X, PLOIDY.values());

    
    public Input<SpeciesTreePrior> m_speciesTreePrior =
            new Input<SpeciesTreePrior>("speciesTreePrior", "defines population function and its parameters", Validate.REQUIRED);

    public Input<TreeTopFinder> treeTopFinder =
            new Input<TreeTopFinder>("treetop", "calculates height of species tree, required only for linear *beast analysis");

    // intervals for each of the species tree branches
    private PriorityQueue<Double>[] m_intervals;
    // count nr of lineages at the bottom of species tree branches
    private int[] m_nLineages;
    // maps gene tree leaf nodes to species tree leaf nodes. Indexed by node number.
    protected int[] m_nLineageToSpeciesMap;

    beast.evolution.speciation.SpeciesTreePrior.PopSizeFunction m_bIsConstantPopFunction;
    RealParameter m_fPopSizesBottom;
    RealParameter m_fPopSizesTop;

    // Ploidy is a constant - cache value of input here
    private double m_fPloidy;

    //???
    public GeneTreeForSpeciesTreeDistribution() {
        m_tree.setRule(Validate.REQUIRED);
    }

    @Override
    public void initAndValidate() throws Exception {
    	m_fPloidy = m_ploidy.get();
//    	switch (m_ploidy.get()) {
//			case autosomal_nuclear: m_fPloidy = 2.0; break;
//			case X: m_fPloidy = 1.5; break;
//			case Y: m_fPloidy = 0.5; break;
//			case mitrochondrial: m_fPloidy = 0.5; break;
//			default: throw new Exception("Unknown value for ploidy");
//		}
        final Node[] gtNodes = m_tree.get().getNodesAsArray();
        final int nGtLineages = m_tree.get().getLeafNodeCount();
        final Node[] sptNodes = m_speciesTree.get().getNodesAsArray();
        final int nSpecies = m_speciesTree.get().getNodeCount();


        if (nSpecies <= 1 && sptNodes[0].getID().equals("Beauti2DummyTaxonSet")) {
            // we are in Beauti, don't initialise
            return;
        }


        // reserve memory for priority queues
        m_intervals = new PriorityQueue[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            m_intervals[i] = new PriorityQueue<Double>();
        }

        // sanity check lineage nodes are all at height=0
        for (int i = 0; i < nGtLineages; i++) {
            if (gtNodes[i].getHeight() != 0) {
                throw new Exception("Cannot deal with taxon " + gtNodes[i].getID() +
                        ", which has non-zero height + " + gtNodes[i].getHeight());
            }
        }
        // set up m_nLineageToSpeciesMap
        m_nLineageToSpeciesMap = new int[nGtLineages];

        Arrays.fill(m_nLineageToSpeciesMap, -1);
        for (int i = 0; i < nGtLineages; i++) {
            final String sSpeciesID = getSetID(gtNodes[i].getID());
            // ??? can this be a startup check? can this happen during run due to tree change?
            if (sSpeciesID == null) {
                throw new Exception("Cannot find species for lineage " + gtNodes[i].getID());
            }
            for (int iSpecies = 0; iSpecies < nSpecies; iSpecies++) {
                if (sSpeciesID.equals(sptNodes[iSpecies].getID())) {
                    m_nLineageToSpeciesMap[i] = iSpecies;
                    break;
                }
            }
            if (m_nLineageToSpeciesMap[i] < 0) {
                throw new Exception("Cannot find species with name " + sSpeciesID + " in species tree");
            }
        }

        // calculate nr of lineages per species
        m_nLineages = new int[nSpecies];
//        for (final Node node : gtNodes) {
//            if (node.isLeaf()) {
//                final int iSpecies = m_nLineageToSpeciesMap[node.getNr()];
//                m_nLineages[iSpecies]++;
//            }
//        }

        final SpeciesTreePrior popInfo = m_speciesTreePrior.get();
        m_bIsConstantPopFunction = popInfo.m_popFunctionInput.get();
        m_fPopSizesBottom = popInfo.m_popSizesBottom.get();
        m_fPopSizesTop = popInfo.m_popSizesTop.get();

        assert( ! (m_bIsConstantPopFunction == PopSizeFunction.linear && treeTopFinder.get() == null ) );
    }

    /**
     * @param sLineageID
     * @return species ID to which the lineage ID belongs according to the TaxonSets
     */
    String getSetID(final String sLineageID) {
        final TaxonSet taxonSuperset = m_speciesTreePrior.get().m_taxonSet.get();
        final List<Taxon> taxonSets = taxonSuperset.m_taxonset.get();
        for (final Taxon taxonSet : taxonSets) {
            final List<Taxon> taxa = ((TaxonSet) taxonSet).m_taxonset.get();
            for (final Taxon aTaxa : taxa) {
                if (aTaxa.getID().equals(sLineageID)) {
                    return taxonSet.getID();
                }
            }
        }
        return null;
    }

    @Override
    public double calculateLogP() {
        logP = 0;
        for (final PriorityQueue<Double> m_interval : m_intervals) {
            m_interval.clear();
        }

        Arrays.fill(m_nLineages, 0);

        final Tree stree = m_speciesTree.get();
        final Node[] speciesNodes = stree.getNodesAsArray();

        traverseLineageTree(speciesNodes, m_tree.get().getRoot());
//		System.err.println(getID());
//		for (int i = 0; i < m_intervals.length; i++) {
//			System.err.println(m_intervals[i]);
//		}
        // if the gene tree does not fit the species tree, logP = -infinity by now
        if (logP == 0) {
            traverseSpeciesTree(stree.getRoot());
        }
//		System.err.println("logp=" + logP);
        return logP;
    }

    /**
     * calculate contribution to logP for each of the branches of the species tree
     *
     * @param node*
     */
    private void traverseSpeciesTree(final Node node) {
        if (!node.isLeaf()) {
            traverseSpeciesTree(node.getLeft());
            traverseSpeciesTree(node.getRight());
        }
        // calculate contribution of a branch in the species tree to the log probability
        final int iNode = node.getNr();

        // k, as defined in the paper
        //System.err.println(Arrays.toString(m_nLineages));
        final int k = m_intervals[iNode].size();
        final double[] fTimes = new double[k + 2];
        fTimes[0] = node.getHeight();
        for (int i = 1; i <= k; i++) {
            fTimes[i] = m_intervals[iNode].poll();
        }
        if (!node.isRoot()) {
            fTimes[k + 1] = node.getParent().getHeight();
        } else {
            if (m_bIsConstantPopFunction == PopSizeFunction.linear) {
                fTimes[k + 1] = treeTopFinder.get().getHighestTreeHeight();
            } else {
                fTimes[k + 1] = Math.max(node.getHeight(), m_tree.get().getRoot().getHeight());
            }
        }
        // sanity check
        for (int i = 0; i <= k; i++) {
            if (fTimes[i] > fTimes[i + 1]) {
                System.err.println("invalid times");
                calculateLogP();
            }
        }

        final int nLineagesBottom = m_nLineages[iNode];

        switch (m_bIsConstantPopFunction) {
            case constant:
                calcConstantPopSizeContribution(nLineagesBottom, m_fPopSizesBottom.getValue(iNode), fTimes, k);
                break;
            case linear:
                calcLinearPopSizeContribution(nLineagesBottom, iNode, fTimes, k, node);
                break;
            case linear_with_constant_root:
                if (node.isRoot()) {
                    final double fPopSize = getTopPopSize(node.getLeft().getNr()) + getTopPopSize(node.getRight().getNr());
                    calcConstantPopSizeContribution(nLineagesBottom, fPopSize, fTimes, k);
                } else {
                    calcLinearPopSizeContribution(nLineagesBottom, iNode, fTimes, k, node);
                }
                break;
        }
    }

    /* the contribution of a branch in the species tree to
      * the log probability, for constant population function.
      */
    private void calcConstantPopSizeContribution(final int nLineagesBottom, final double fPopSize2,
                                                 final double[] fTimes, final int k) {
        final double fPopSize = fPopSize2 * m_fPloidy;
        logP += -k * Math.log(fPopSize);
//		System.err.print(logP);
        for (int i = 0; i <= k; i++) {
            logP += -((nLineagesBottom - i) * (nLineagesBottom - i - 1.0) / 2.0) * (fTimes[i + 1] - fTimes[i]) / fPopSize;
        }
//		System.err.println(" " + logP + " " + Arrays.toString(fTimes) + " " + iNode + " " + k);
    }

    /* the contribution of a branch in the species tree to
      * the log probability, for linear population function.
      */
    private void calcLinearPopSizeContribution(final int nLineagesBottom, final int iNode, final double[] fTimes,
                                               final int k, final Node node) {
        final double fPopSizeBottom;
        if (node.isLeaf()) {
            fPopSizeBottom = m_fPopSizesBottom.getValue(iNode) * m_fPloidy;
        } else {
            // use sum of left and right child branches for internal nodes
            fPopSizeBottom = (getTopPopSize(node.getLeft().getNr()) + getTopPopSize(node.getRight().getNr())) * m_fPloidy;
        }
        final double fPopSizeTop = getTopPopSize(iNode) * m_fPloidy;
        final double a = (fPopSizeTop - fPopSizeBottom) / (fTimes[k + 1] - fTimes[0]);
        final double b = fPopSizeBottom;
        for (int i = 0; i < k; i++) {
            //double fPopSize = fPopSizeBottom + (fPopSizeTop-fPopSizeBottom) * fTimes[i+1]/(fTimes[k]-fTimes[0]);
            final double fPopSize = a * (fTimes[i + 1] - fTimes[0]) + b;
            logP += -Math.log(fPopSize);
        }
        for (int i = 0; i <= k; i++) {
            if (Math.abs(fPopSizeTop - fPopSizeBottom) < 1e-10) {
                // slope = 0, so population function is constant
                final double fPopSize = a * (fTimes[i + 1] - fTimes[0]) + b;
                logP += -((nLineagesBottom - i) * (nLineagesBottom - i - 1.0) / 2.0) * (fTimes[i + 1] - fTimes[i]) / fPopSize;
            } else {
                final double f = (a * (fTimes[i + 1] - fTimes[0]) + b) / (a * (fTimes[i] - fTimes[0]) + b);
                logP += -((nLineagesBottom - i) * (nLineagesBottom - i - 1.0) / 2.0) * Math.log(f) / a;
            }
        }
    }

    /**
     * collect intervals for each of the branches of the species tree
     * as defined by the lineage tree.
     *
     * @param speciesNodes
     * @param node
     * @return
     */
    private int traverseLineageTree(final Node[] speciesNodes, final Node node) {
        if (node.isLeaf()) {
            final int iSpecies = m_nLineageToSpeciesMap[node.getNr()];
            m_nLineages[iSpecies]++;
            return iSpecies;
        } else {
            int nSpeciesLeft = traverseLineageTree(speciesNodes, node.getLeft());
            int nSpeciesRight = traverseLineageTree(speciesNodes, node.getRight());
            final double fHeight = node.getHeight();

            while (!speciesNodes[nSpeciesLeft].isRoot() && fHeight > speciesNodes[nSpeciesLeft].getParent().getHeight()) {
                nSpeciesLeft = speciesNodes[nSpeciesLeft].getParent().getNr();
                m_nLineages[nSpeciesLeft]++;
            }
            while (!speciesNodes[nSpeciesRight].isRoot() && fHeight > speciesNodes[nSpeciesRight].getParent().getHeight()) {
                nSpeciesRight = speciesNodes[nSpeciesRight].getParent().getNr();
                m_nLineages[nSpeciesRight]++;
            }
            // validity check
            if (nSpeciesLeft != nSpeciesRight) {
                // if we got here, it means the gene tree does
                // not fit in the species tree
                logP = Double.NEGATIVE_INFINITY;
            }
            m_intervals[nSpeciesRight].add(fHeight);
            return nSpeciesRight;
        }
    }

    /* return population size at top. For linear with constant root, there is no
      * entry for the root. An internal node can have the number equal to dimension
      * of m_fPopSizesTop, then the root node can be numbered with a lower number
      * and we can use that entry in m_fPopSizesTop for the rogue internal node.
      */
    private double getTopPopSize(final int iNode) {
        if (iNode < m_fPopSizesTop.getDimension()) {
            return m_fPopSizesTop.getArrayValue(iNode);
        }
        return m_fPopSizesTop.getArrayValue(m_speciesTree.get().getRoot().getNr());
    }


    @Override
    public boolean requiresRecalculation() {
        // TODO: check whether this is worth optimising?
        return true;
    }

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }

    @Override
    public void sample(final State state, final Random random) {
    }
}
