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
import beast.core.util.Log;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.speciation.SpeciesTreePrior.TreePopSizeFunction;
import beast.evolution.tree.Node;
import beast.evolution.tree.TreeDistribution;
import beast.evolution.tree.TreeInterface;



@Description("Calculates probability of gene tree conditioned on a species tree (multi-species coalescent)")
public class GeneTreeForSpeciesTreeDistribution extends TreeDistribution {
    final public Input<TreeInterface> speciesTreeInput =
            new Input<>("speciesTree", "species tree containing the associated gene tree", Validate.REQUIRED);

//    public enum PLOIDY {autosomal_nuclear, X, Y, mitrochondrial};
    
    final public Input<Double> ploidyInput =
            new Input<>("ploidy", "ploidy (copy number) for this gene, typically a whole number or half (default 2 for autosomal_nuclear)", 2.0);
//    public Input<PLOIDY> m_ploidy =
//        new Input<>("ploidy", "ploidy for this gene (default X, Possible values: " + PLOIDY.values(), PLOIDY.X, PLOIDY.values());

    
    final public Input<SpeciesTreePrior> speciesTreePriorInput =
            new Input<>("speciesTreePrior", "defines population function and its parameters", Validate.REQUIRED);

    final public Input<TreeTopFinder> treeTopFinderInput =
            new Input<>("treetop", "calculates height of species tree, required only for linear *beast analysis");

    // intervals for each of the species tree branches
    private PriorityQueue<Double>[] intervalsInput;
    // count nr of lineages at the bottom of species tree branches
    private int[] nrOfLineages;
    // maps gene tree leaf nodes to species tree leaf nodes. Indexed by node number.
    protected int[] nrOfLineageToSpeciesMap;

    beast.evolution.speciation.SpeciesTreePrior.TreePopSizeFunction isConstantPopFunction;
    RealParameter popSizesBottom;
    RealParameter popSizesTop;

    // Ploidy is a constant - cache value of input here
    private double ploidy;

    //???
    public GeneTreeForSpeciesTreeDistribution() {
        treeInput.setRule(Validate.REQUIRED);
    }

    @SuppressWarnings("unchecked")
	@Override
    public void initAndValidate() throws Exception {
    	ploidy = ploidyInput.get();
//    	switch (m_ploidy.get()) {
//			case autosomal_nuclear: m_fPloidy = 2.0; break;
//			case X: m_fPloidy = 1.5; break;
//			case Y: m_fPloidy = 0.5; break;
//			case mitrochondrial: m_fPloidy = 0.5; break;
//			default: throw new Exception("Unknown value for ploidy");
//		}
        final Node[] gtNodes = treeInput.get().getNodesAsArray();
        final int nGtLineages = treeInput.get().getLeafNodeCount();
        final Node[] sptNodes = speciesTreeInput.get().getNodesAsArray();
        final int nSpecies = speciesTreeInput.get().getNodeCount();


        if (nSpecies <= 1 && sptNodes[0].getID().equals("Beauti2DummyTaxonSet")) {
            // we are in Beauti, don't initialise
            return;
        }


        // reserve memory for priority queues
        intervalsInput = new PriorityQueue[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            intervalsInput[i] = new PriorityQueue<>();
        }

        // sanity check lineage nodes are all at height=0
        for (int i = 0; i < nGtLineages; i++) {
            if (gtNodes[i].getHeight() != 0) {
                throw new IllegalArgumentException("Cannot deal with taxon " + gtNodes[i].getID() +
                        ", which has non-zero height + " + gtNodes[i].getHeight());
            }
        }
        // set up m_nLineageToSpeciesMap
        nrOfLineageToSpeciesMap = new int[nGtLineages];

        Arrays.fill(nrOfLineageToSpeciesMap, -1);
        for (int i = 0; i < nGtLineages; i++) {
            final String speciesID = getSetID(gtNodes[i].getID());
            // ??? can this be a startup check? can this happen during run due to tree change?
            if (speciesID == null) {
                throw new IllegalArgumentException("Cannot find species for lineage " + gtNodes[i].getID());
            }
            for (int iSpecies = 0; iSpecies < nSpecies; iSpecies++) {
                if (speciesID.equals(sptNodes[iSpecies].getID())) {
                    nrOfLineageToSpeciesMap[i] = iSpecies;
                    break;
                }
            }
            if (nrOfLineageToSpeciesMap[i] < 0) {
                throw new IllegalArgumentException("Cannot find species with name " + speciesID + " in species tree");
            }
        }

        // calculate nr of lineages per species
        nrOfLineages = new int[nSpecies];
//        for (final Node node : gtNodes) {
//            if (node.isLeaf()) {
//                final int iSpecies = m_nLineageToSpeciesMap[node.getNr()];
//                m_nLineages[iSpecies]++;
//            }
//        }

        final SpeciesTreePrior popInfo = speciesTreePriorInput.get();
        isConstantPopFunction = popInfo.popFunctionInput.get();
        popSizesBottom = popInfo.popSizesBottomInput.get();
        popSizesTop = popInfo.popSizesTopInput.get();

        assert( ! (isConstantPopFunction == TreePopSizeFunction.linear && treeTopFinderInput.get() == null ) );
    }

    /**
     * @param lineageID
     * @return species ID to which the lineage ID belongs according to the TaxonSets
     */
    String getSetID(final String lineageID) {
        final TaxonSet taxonSuperset = speciesTreePriorInput.get().taxonSetInput.get();
        final List<Taxon> taxonSets = taxonSuperset.taxonsetInput.get();
        for (final Taxon taxonSet : taxonSets) {
            final List<Taxon> taxa = ((TaxonSet) taxonSet).taxonsetInput.get();
            for (final Taxon aTaxa : taxa) {
                if (aTaxa.getID().equals(lineageID)) {
                    return taxonSet.getID();
                }
            }
        }
        return null;
    }

    @Override
    public double calculateLogP() {
        logP = 0;
        for (final PriorityQueue<Double> m_interval : intervalsInput) {
            m_interval.clear();
        }

        Arrays.fill(nrOfLineages, 0);

        final TreeInterface stree = speciesTreeInput.get();
        final Node[] speciesNodes = stree.getNodesAsArray();

        traverseLineageTree(speciesNodes, treeInput.get().getRoot());
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
        final int k = intervalsInput[iNode].size();
        final double[] fTimes = new double[k + 2];
        fTimes[0] = node.getHeight();
        for (int i = 1; i <= k; i++) {
            fTimes[i] = intervalsInput[iNode].poll();
        }
        if (!node.isRoot()) {
            fTimes[k + 1] = node.getParent().getHeight();
        } else {
            if (isConstantPopFunction == TreePopSizeFunction.linear) {
                fTimes[k + 1] = treeTopFinderInput.get().getHighestTreeHeight();
            } else {
                fTimes[k + 1] = Math.max(node.getHeight(), treeInput.get().getRoot().getHeight());
            }
        }
        // sanity check
        for (int i = 0; i <= k; i++) {
            if (fTimes[i] > fTimes[i + 1]) {
            	Log.warning.println("invalid times");
                calculateLogP();
            }
        }

        final int nLineagesBottom = nrOfLineages[iNode];

        switch (isConstantPopFunction) {
            case constant:
                calcConstantPopSizeContribution(nLineagesBottom, popSizesBottom.getValue(iNode), fTimes, k);
                break;
            case linear:
                logP += calcLinearPopSizeContributionJH(nLineagesBottom, iNode, fTimes, k, node);
                break;
            case linear_with_constant_root:
                if (node.isRoot()) {
                    final double fPopSize = getTopPopSize(node.getLeft().getNr()) + getTopPopSize(node.getRight().getNr());
                    calcConstantPopSizeContribution(nLineagesBottom, fPopSize, fTimes, k);
                } else {
                    logP += calcLinearPopSizeContribution(nLineagesBottom, iNode, fTimes, k, node);
                }
                break;
        }
    }

    /* the contribution of a branch in the species tree to
      * the log probability, for constant population function.
      */
    private void calcConstantPopSizeContribution(final int nLineagesBottom, final double fPopSize2,
                                                 final double[] fTimes, final int k) {
        final double fPopSize = fPopSize2 * ploidy;
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
    private double calcLinearPopSizeContribution(final int nLineagesBottom, final int iNode, final double[] fTimes,
                                                 final int k, final Node node) {
        double lp = 0.0;
        final double fPopSizeBottom;
        if (node.isLeaf()) {
            fPopSizeBottom = popSizesBottom.getValue(iNode) * ploidy;
        } else {
            // use sum of left and right child branches for internal nodes
            fPopSizeBottom = (getTopPopSize(node.getLeft().getNr()) + getTopPopSize(node.getRight().getNr())) * ploidy;
        }
        final double fPopSizeTop = getTopPopSize(iNode) * ploidy;
        final double a = (fPopSizeTop - fPopSizeBottom) / (fTimes[k + 1] - fTimes[0]);
        final double b = fPopSizeBottom;
        for (int i = 0; i < k; i++) {
            //double fPopSize = fPopSizeBottom + (fPopSizeTop-fPopSizeBottom) * fTimes[i+1]/(fTimes[k]-fTimes[0]);
            final double fPopSize = a * (fTimes[i + 1] - fTimes[0]) + b;
            lp += -Math.log(fPopSize);
        }
        for (int i = 0; i <= k; i++) {
            if (Math.abs(fPopSizeTop - fPopSizeBottom) < 1e-10) {
                // slope = 0, so population function is constant
                final double fPopSize = a * (fTimes[i + 1] - fTimes[0]) + b;
                lp += -((nLineagesBottom - i) * (nLineagesBottom - i - 1.0) / 2.0) * (fTimes[i + 1] - fTimes[i]) / fPopSize;
            } else {
                final double f = (a * (fTimes[i + 1] - fTimes[0]) + b) / (a * (fTimes[i] - fTimes[0]) + b);
                lp += -((nLineagesBottom - i) * (nLineagesBottom - i - 1.0) / 2.0) * Math.log(f) / a;
            }
        }
        return lp;
    }

    private double calcLinearPopSizeContributionJH(final int nLineagesBottom, final int iNode, final double[] fTimes,
                                                   final int k, final Node node) {
        double lp = 0.0;
        double fPopSizeBottom;
        if (node.isLeaf()) {
            fPopSizeBottom = popSizesBottom.getValue(iNode);
        } else {
            // use sum of left and right child branches for internal nodes
            fPopSizeBottom = (getTopPopSize(node.getLeft().getNr()) + getTopPopSize(node.getRight().getNr()));
        }
        fPopSizeBottom *= ploidy;

        final double fPopSizeTop = getTopPopSize(iNode) * ploidy;
        final double d5 = fPopSizeTop - fPopSizeBottom;
        final double fTime0 = fTimes[0];
        final double a = d5 / (fTimes[k + 1] - fTime0);
        final double b = fPopSizeBottom;

        if (Math.abs(d5) < 1e-10) {
            // use approximation for small values to bypass numerical instability
            for (int i = 0; i <= k; i++) {
                final double fTimeip1 = fTimes[i + 1];
                final double fPopSize = a * (fTimeip1 - fTime0) + b;
                if( i < k ) {
                  lp += -Math.log(fPopSize);
                }
                // slope = 0, so population function is constant

                final int i1 = nLineagesBottom - i;
                lp -= (i1 * (i1 - 1.0) / 2.0) * (fTimeip1 - fTimes[i]) / fPopSize;
            }
        } else {
            final double vv = b - a * fTime0;
            for (int i = 0; i <= k; i++) {
                final double fPopSize = a * fTimes[i + 1] + vv;
                if( i < k ) {
                  lp += -Math.log(fPopSize);
                }
                final double f = fPopSize / (a * fTimes[i] + vv);

                final int i1 = nLineagesBottom - i;
                lp += -(i1 * (i1 - 1.0) / 2.0) * Math.log(f) / a;
            }
        }
        return lp;
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
            final int iSpecies = nrOfLineageToSpeciesMap[node.getNr()];
            nrOfLineages[iSpecies]++;
            return iSpecies;
        } else {
            int nSpeciesLeft = traverseLineageTree(speciesNodes, node.getLeft());
            int nSpeciesRight = traverseLineageTree(speciesNodes, node.getRight());
            final double fHeight = node.getHeight();

            while (!speciesNodes[nSpeciesLeft].isRoot() && fHeight > speciesNodes[nSpeciesLeft].getParent().getHeight()) {
                nSpeciesLeft = speciesNodes[nSpeciesLeft].getParent().getNr();
                nrOfLineages[nSpeciesLeft]++;
            }
            while (!speciesNodes[nSpeciesRight].isRoot() && fHeight > speciesNodes[nSpeciesRight].getParent().getHeight()) {
                nSpeciesRight = speciesNodes[nSpeciesRight].getParent().getNr();
                nrOfLineages[nSpeciesRight]++;
            }
            // validity check
            if (nSpeciesLeft != nSpeciesRight) {
                // if we got here, it means the gene tree does
                // not fit in the species tree
                logP = Double.NEGATIVE_INFINITY;
            }
            intervalsInput[nSpeciesRight].add(fHeight);
            return nSpeciesRight;
        }
    }

    /* return population size at top. For linear with constant root, there is no
      * entry for the root. An internal node can have the number equal to dimension
      * of m_fPopSizesTop, then the root node can be numbered with a lower number
      * and we can use that entry in m_fPopSizesTop for the rogue internal node.
      */
    private double getTopPopSize(final int iNode) {
        if (iNode < popSizesTop.getDimension()) {
            return popSizesTop.getArrayValue(iNode);
        }
        return popSizesTop.getArrayValue(speciesTreeInput.get().getRoot().getNr());
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
