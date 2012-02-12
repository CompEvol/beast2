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
    public Input<Tree> m_speciesTree = new Input<Tree>("speciesTree", "species tree containing the associated gene tree", Validate.REQUIRED);
    //public Input<Tree> m_geneTree = new Input<Tree>("geneTree", "gene tree for which to calculate probability conditioned on the species tree", Validate.REQUIRED);
    public Input<Double> m_ploidy = new Input<Double>("ploidy", "ploidy for this gene, typically a whole number of half (default 1)", 1.0);

    public Input<SpeciesTreePrior> m_speciesTreePrior = new Input<SpeciesTreePrior>("speciesTreePrior", "defines population function and its parameters", Validate.REQUIRED);

    public Input<TreeTopFinder> treeTopFinder = new Input<TreeTopFinder>("treetop", "calculates height of species tree, required only for linear *beast analysis");
    // intervals for each of the species tree branches
    PriorityQueue<Double>[] m_intervals;
    // count nr of lineages at the bottom of species tree branches
    int[] m_nLineages;
    // maps gene tree leaf nodes to species tree leaf nodes
    int[] m_nLineageToSpeciesMap;

    beast.evolution.speciation.SpeciesTreePrior.PopSizeFunction m_bIsConstantPopFunction;
    RealParameter m_fPopSizesBottom;
    RealParameter m_fPopSizesTop;
    double m_fPloidy;

    public GeneTreeForSpeciesTreeDistribution() {
        m_tree.setRule(Validate.REQUIRED);
    }

    @Override
    public void initAndValidate() throws Exception {
        m_fPloidy = m_ploidy.get();
        Node[] nodes = m_tree.get().getNodesAsArray();
        int nLineages = m_tree.get().getLeafNodeCount();
        Node[] nodes2 = m_speciesTree.get().getNodesAsArray();
        int nSpecies = m_speciesTree.get().getNodeCount();


        if (nSpecies <= 1 && nodes2[0].getID().equals("Beauti2DummyTaxonSet")) {
            // we are in Beauti, don't initialise
            return;
        }


        // reserve memory for priority queues
        m_intervals = new PriorityQueue[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            m_intervals[i] = new PriorityQueue<Double>();
        }

        // sanity check lineage nodes are all at height=0
        for (int i = 0; i < nLineages; i++) {
            if (nodes[i].getHeight() != 0) {
                throw new Exception("Cannot deal with taxon " + nodes[i].getID() + ", which has non-zero height + " + nodes[i].getHeight());
            }
        }
        // set up m_nLineageToSpeciesMap
        m_nLineageToSpeciesMap = new int[nLineages];
        Arrays.fill(m_nLineageToSpeciesMap, -1);
        for (int i = 0; i < nLineages; i++) {
            String sSpeciesID = getSetID(nodes[i].getID());
            if (sSpeciesID == null) {
                throw new Exception("Cannot find species for lineage " + nodes[i].getID());
            }
            for (int iSpecies = 0; iSpecies < nSpecies; iSpecies++) {
                if (sSpeciesID.equals(nodes2[iSpecies].getID())) {
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
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].isLeaf()) {
                int iSpecies = m_nLineageToSpeciesMap[nodes[i].getNr()];
                m_nLineages[iSpecies]++;
            }
        }

        SpeciesTreePrior popInfo = m_speciesTreePrior.get();
        m_bIsConstantPopFunction = popInfo.m_popFunctionInput.get();
        m_fPopSizesBottom = popInfo.m_popSizesBottom.get();
        m_fPopSizesTop = popInfo.m_popSizesTop.get();
    }

    /**
     * find species ID to which the lineage ID belongs according to the TaxonSets *
     */
    String getSetID(String sLineageID) {
        TaxonSet taxonSuperset = m_speciesTreePrior.get().m_taxonSet.get();
        List<Taxon> taxonSets = taxonSuperset.m_taxonset.get();
        for (Taxon taxonSet : taxonSets) {
            List<Taxon> taxa = ((TaxonSet) taxonSet).m_taxonset.get();
            for (int i = 0; i < taxa.size(); i++) {
                if (taxa.get(i).getID().equals(sLineageID)) {
                    return taxonSet.getID();
                }
            }
        }
        return null;
    }


    @Override
    public double calculateLogP() {
        logP = 0;
        for (int i = 0; i < m_intervals.length; i++) {
            m_intervals[i].clear();
        }
        Arrays.fill(m_nLineages, 0);

        Node[] speciesNodes = m_speciesTree.get().getNodesAsArray();
        traverseLineageTree(speciesNodes, m_tree.get().getRoot());
//		System.err.println(getID());
//		for (int i = 0; i < m_intervals.length; i++) {
//			System.err.println(m_intervals[i]);
//		}
        // if the gene tree does not fit the species tree, logP = -infinity by now
        if (logP == 0) {
            traverseSpeciesTree(m_speciesTree.get().getRoot());
        }
//		System.err.println("logp=" + logP);
        return logP;
    }

    /**
     * calculate contribution to logP for each of the branches of the species tree *
     */
    private void traverseSpeciesTree(Node node) {
        if (!node.isLeaf()) {
            traverseSpeciesTree(node.getLeft());
            traverseSpeciesTree(node.getRight());
        }
        // calculate contribution of a branch in the species tree to the log probability
        int iNode = node.getNr();

        // k, as defined in the paper
        //System.err.println(Arrays.toString(m_nLineages));
        int k = m_intervals[iNode].size();
        double[] fTimes = new double[k + 2];
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

        int nLineagesBottom = m_nLineages[iNode];

        switch (m_bIsConstantPopFunction) {
            case constant:
                calcConstantPopSizeContribution(nLineagesBottom, m_fPopSizesBottom.getValue(iNode), fTimes, k);
                break;
            case linear:
                calcLinearPopSizeContribution(nLineagesBottom, iNode, fTimes, k, node);
                break;
            case linear_with_constant_root:
                if (node.isRoot()) {
                    double fPopSize = getTopPopSize(node.getLeft().getNr()) + getTopPopSize(node.getRight().getNr());
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
    private void calcConstantPopSizeContribution(int nLineagesBottom, double fPopSize2, double[] fTimes, int k) {
        double fPopSize = fPopSize2 * m_fPloidy;
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
    private void calcLinearPopSizeContribution(int nLineagesBottom, int iNode, double[] fTimes, int k, Node node) {
        double fPopSizeBottom;
        if (node.isLeaf()) {
            fPopSizeBottom = m_fPopSizesBottom.getValue(iNode) * m_fPloidy;
        } else {
            // use sum of left and right child branches for internal nodes
            fPopSizeBottom = (getTopPopSize(node.getLeft().getNr()) + getTopPopSize(node.getRight().getNr())) * m_fPloidy;
        }
        double fPopSizeTop = getTopPopSize(iNode) * m_fPloidy;
        double a = (fPopSizeTop - fPopSizeBottom) / (fTimes[k + 1] - fTimes[0]);
        double b = fPopSizeBottom;
        for (int i = 0; i < k; i++) {
            //double fPopSize = fPopSizeBottom + (fPopSizeTop-fPopSizeBottom) * fTimes[i+1]/(fTimes[k]-fTimes[0]);
            double fPopSize = a * (fTimes[i + 1] - fTimes[0]) + b;
            logP += -Math.log(fPopSize);
        }
        for (int i = 0; i <= k; i++) {
            if (Math.abs(fPopSizeTop - fPopSizeBottom) < 1e-10) {
                // slope = 0, so population function is constant
                double fPopSize = a * (fTimes[i + 1] - fTimes[0]) + b;
                logP += -((nLineagesBottom - i) * (nLineagesBottom - i - 1.0) / 2.0) * (fTimes[i + 1] - fTimes[i]) / fPopSize;
            } else {
                double f = (a * (fTimes[i + 1] - fTimes[0]) + b) / (a * (fTimes[i] - fTimes[0]) + b);
                logP += -((nLineagesBottom - i) * (nLineagesBottom - i - 1.0) / 2.0) * Math.log(f) / a;
            }
        }
    }

    /**
     * collect intervals for each of the branches of the species tree
     * as defined by the lineage tree.
     */
    private int traverseLineageTree(Node[] speciesNodes, Node node) {
        if (node.isLeaf()) {
            int nSpecies = m_nLineageToSpeciesMap[node.getNr()];
            m_nLineages[nSpecies]++;
            return nSpecies;
        } else {
            int nSpeciesLeft = traverseLineageTree(speciesNodes, node.getLeft());
            int nSpeciesRight = traverseLineageTree(speciesNodes, node.getRight());
            double fHeight = node.getHeight();

            while (!speciesNodes[nSpeciesLeft].isRoot() && fHeight > speciesNodes[nSpeciesLeft].getParent().getHeight()) {
                nSpeciesLeft = speciesNodes[nSpeciesLeft].getParent().getNr();
                m_nLineages[nSpeciesLeft]++;
            }
            while (!speciesNodes[nSpeciesRight].isRoot() && fHeight > speciesNodes[nSpeciesRight].getParent().getHeight()) {
                nSpeciesRight = speciesNodes[nSpeciesRight].getParent().getNr();
                m_nLineages[nSpeciesRight]++;
            }
            // sanity check
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
    private double getTopPopSize(int iNode) {
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
    public void sample(State state, Random random) {
    }
}
