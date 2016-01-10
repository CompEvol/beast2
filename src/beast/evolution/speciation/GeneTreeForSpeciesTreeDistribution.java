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
        final int gtLineages = treeInput.get().getLeafNodeCount();
        final Node[] sptNodes = speciesTreeInput.get().getNodesAsArray();
        final int speciesCount = speciesTreeInput.get().getNodeCount();


        if (speciesCount <= 1 && sptNodes[0].getID().equals("Beauti2DummyTaxonSet")) {
            // we are in Beauti, don't initialise
            return;
        }


        // reserve memory for priority queues
        intervalsInput = new PriorityQueue[speciesCount];
        for (int i = 0; i < speciesCount; i++) {
            intervalsInput[i] = new PriorityQueue<>();
        }

        // sanity check lineage nodes are all at height=0
        for (int i = 0; i < gtLineages; i++) {
            if (gtNodes[i].getHeight() != 0) {
                throw new IllegalArgumentException("Cannot deal with taxon " + gtNodes[i].getID() +
                        ", which has non-zero height + " + gtNodes[i].getHeight());
            }
        }
        // set up m_nLineageToSpeciesMap
        nrOfLineageToSpeciesMap = new int[gtLineages];

        Arrays.fill(nrOfLineageToSpeciesMap, -1);
        for (int i = 0; i < gtLineages; i++) {
            final String speciesID = getSetID(gtNodes[i].getID());
            // ??? can this be a startup check? can this happen during run due to tree change?
            if (speciesID == null) {
                throw new IllegalArgumentException("Cannot find species for lineage " + gtNodes[i].getID());
            }
            for (int species = 0; species < speciesCount; species++) {
                if (speciesID.equals(sptNodes[species].getID())) {
                    nrOfLineageToSpeciesMap[i] = species;
                    break;
                }
            }
            if (nrOfLineageToSpeciesMap[i] < 0) {
                throw new IllegalArgumentException("Cannot find species with name " + speciesID + " in species tree");
            }
        }

        // calculate nr of lineages per species
        nrOfLineages = new int[speciesCount];
//        for (final Node node : gtNodes) {
//            if (node.isLeaf()) {
//                final int species = m_nLineageToSpeciesMap[node.getNr()];
//                m_nLineages[species]++;
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
        final int nodeIndex = node.getNr();

        // k, as defined in the paper
        //System.err.println(Arrays.toString(m_nLineages));
        final int k = intervalsInput[nodeIndex].size();
        final double[] times = new double[k + 2];
        times[0] = node.getHeight();
        for (int i = 1; i <= k; i++) {
            times[i] = intervalsInput[nodeIndex].poll();
        }
        if (!node.isRoot()) {
            times[k + 1] = node.getParent().getHeight();
        } else {
            if (isConstantPopFunction == TreePopSizeFunction.linear) {
                times[k + 1] = treeTopFinderInput.get().getHighestTreeHeight();
            } else {
                times[k + 1] = Math.max(node.getHeight(), treeInput.get().getRoot().getHeight());
            }
        }
        // sanity check
        for (int i = 0; i <= k; i++) {
            if (times[i] > times[i + 1]) {
            	Log.warning.println("invalid times");
                calculateLogP();
            }
        }

        final int lineagesBottom = nrOfLineages[nodeIndex];

        switch (isConstantPopFunction) {
            case constant:
                calcConstantPopSizeContribution(lineagesBottom, popSizesBottom.getValue(nodeIndex), times, k);
                break;
            case linear:
                logP += calcLinearPopSizeContributionJH(lineagesBottom, nodeIndex, times, k, node);
                break;
            case linear_with_constant_root:
                if (node.isRoot()) {
                    final double popSize = getTopPopSize(node.getLeft().getNr()) + getTopPopSize(node.getRight().getNr());
                    calcConstantPopSizeContribution(lineagesBottom, popSize, times, k);
                } else {
                    logP += calcLinearPopSizeContribution(lineagesBottom, nodeIndex, times, k, node);
                }
                break;
        }
    }

    /* the contribution of a branch in the species tree to
      * the log probability, for constant population function.
      */
    private void calcConstantPopSizeContribution(final int lineagesBottom, final double popSize2,
                                                 final double[] times, final int k) {
        final double popSize = popSize2 * ploidy;
        logP += -k * Math.log(popSize);
//		System.err.print(logP);
        for (int i = 0; i <= k; i++) {
            logP += -((lineagesBottom - i) * (lineagesBottom - i - 1.0) / 2.0) * (times[i + 1] - times[i]) / popSize;
        }
//		System.err.println(" " + logP + " " + Arrays.toString(times) + " " + nodeIndex + " " + k);
    }

    /* the contribution of a branch in the species tree to
      * the log probability, for linear population function.
      */
    private double calcLinearPopSizeContribution(final int lineagesBottom, final int nodeIndex, final double[] times,
                                                 final int k, final Node node) {
        double lp = 0.0;
        final double popSizeBottom;
        if (node.isLeaf()) {
            popSizeBottom = popSizesBottom.getValue(nodeIndex) * ploidy;
        } else {
            // use sum of left and right child branches for internal nodes
            popSizeBottom = (getTopPopSize(node.getLeft().getNr()) + getTopPopSize(node.getRight().getNr())) * ploidy;
        }
        final double popSizeTop = getTopPopSize(nodeIndex) * ploidy;
        final double a = (popSizeTop - popSizeBottom) / (times[k + 1] - times[0]);
        final double b = popSizeBottom;
        for (int i = 0; i < k; i++) {
            //double popSize = popSizeBottom + (popSizeTop-popSizeBottom) * times[i+1]/(times[k]-times[0]);
            final double popSize = a * (times[i + 1] - times[0]) + b;
            lp += -Math.log(popSize);
        }
        for (int i = 0; i <= k; i++) {
            if (Math.abs(popSizeTop - popSizeBottom) < 1e-10) {
                // slope = 0, so population function is constant
                final double popSize = a * (times[i + 1] - times[0]) + b;
                lp += -((lineagesBottom - i) * (lineagesBottom - i - 1.0) / 2.0) * (times[i + 1] - times[i]) / popSize;
            } else {
                final double f = (a * (times[i + 1] - times[0]) + b) / (a * (times[i] - times[0]) + b);
                lp += -((lineagesBottom - i) * (lineagesBottom - i - 1.0) / 2.0) * Math.log(f) / a;
            }
        }
        return lp;
    }

    private double calcLinearPopSizeContributionJH(final int lineagesBottom, final int nodeIndex, final double[] times,
                                                   final int k, final Node node) {
        double lp = 0.0;
        double popSizeBottom;
        if (node.isLeaf()) {
            popSizeBottom = popSizesBottom.getValue(nodeIndex);
        } else {
            // use sum of left and right child branches for internal nodes
            popSizeBottom = (getTopPopSize(node.getLeft().getNr()) + getTopPopSize(node.getRight().getNr()));
        }
        popSizeBottom *= ploidy;

        final double popSizeTop = getTopPopSize(nodeIndex) * ploidy;
        final double d5 = popSizeTop - popSizeBottom;
        final double time0 = times[0];
        final double a = d5 / (times[k + 1] - time0);
        final double b = popSizeBottom;

        if (Math.abs(d5) < 1e-10) {
            // use approximation for small values to bypass numerical instability
            for (int i = 0; i <= k; i++) {
                final double timeip1 = times[i + 1];
                final double popSize = a * (timeip1 - time0) + b;
                if( i < k ) {
                  lp += -Math.log(popSize);
                }
                // slope = 0, so population function is constant

                final int i1 = lineagesBottom - i;
                lp -= (i1 * (i1 - 1.0) / 2.0) * (timeip1 - times[i]) / popSize;
            }
        } else {
            final double vv = b - a * time0;
            for (int i = 0; i <= k; i++) {
                final double popSize = a * times[i + 1] + vv;
                if( i < k ) {
                  lp += -Math.log(popSize);
                }
                final double f = popSize / (a * times[i] + vv);

                final int i1 = lineagesBottom - i;
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
            final int species = nrOfLineageToSpeciesMap[node.getNr()];
            nrOfLineages[species]++;
            return species;
        } else {
            int speciesLeft = traverseLineageTree(speciesNodes, node.getLeft());
            int speciesRight = traverseLineageTree(speciesNodes, node.getRight());
            final double height = node.getHeight();

            while (!speciesNodes[speciesLeft].isRoot() && height > speciesNodes[speciesLeft].getParent().getHeight()) {
                speciesLeft = speciesNodes[speciesLeft].getParent().getNr();
                nrOfLineages[speciesLeft]++;
            }
            while (!speciesNodes[speciesRight].isRoot() && height > speciesNodes[speciesRight].getParent().getHeight()) {
                speciesRight = speciesNodes[speciesRight].getParent().getNr();
                nrOfLineages[speciesRight]++;
            }
            // validity check
            if (speciesLeft != speciesRight) {
                // if we got here, it means the gene tree does
                // not fit in the species tree
                logP = Double.NEGATIVE_INFINITY;
            }
            intervalsInput[speciesRight].add(height);
            return speciesRight;
        }
    }

    /* return population size at top. For linear with constant root, there is no
      * entry for the root. An internal node can have the number equal to dimension
      * of m_fPopSizesTop, then the root node can be numbered with a lower number
      * and we can use that entry in m_fPopSizesTop for the rogue internal node.
      */
    private double getTopPopSize(final int nodeIndex) {
        if (nodeIndex < popSizesTop.getDimension()) {
            return popSizesTop.getArrayValue(nodeIndex);
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
