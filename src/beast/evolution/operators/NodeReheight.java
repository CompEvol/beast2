package beast.evolution.operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.util.Log;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;



@Description("Tree operator which randomly changes the height of a node, " +
        "then reconstructs the tree from node heights.")
public class NodeReheight extends TreeOperator {
    public final Input<TaxonSet> taxonSetInput = new Input<>("taxonset", "taxon set describing species tree taxa and their gene trees", Validate.REQUIRED);
    public final Input<List<Tree>> geneTreesInput = new Input<>("genetree", "list of gene trees that constrain species tree movement", new ArrayList<>());
    Node[] m_nodes;


    /**
     * map node number of leafs in gene trees to leaf nr in species tree *
     */
    List<Map<Integer, Integer>> m_taxonMap;
    int nrOfGeneTrees;
    int nrOfSpecies;

    @Override
    public void initAndValidate() throws Exception {
        /** maps gene taxa names to species number **/
        final Map<String, Integer> taxonMap = new HashMap<>();
        final List<Taxon> list = taxonSetInput.get().taxonsetInput.get();
        
        if (list.size() <= 1) {
        	Log.warning.println("NodeReheight operator requires at least 2 taxa while the taxon set (id=" + taxonSetInput.get().getID() +") has only " + list.size() + " taxa. "
        			+ "If the XML file was set up in BEAUti, this probably means a taxon assignment needs to be set up in the taxonset panel.");
        	// assume we are in BEAUti, back off for now
        	return;
        }
        
        for (int i = 0; i < list.size(); i++) {
            final Taxon taxa = list.get(i);
            // cast should be ok if taxon-set is the set for the species tree
            final TaxonSet set = (TaxonSet) taxa;
            for (final Taxon taxon : set.taxonsetInput.get()) {
                taxonMap.put(taxon.getID(), i);
            }
        }

        /** build the taxon map for each gene tree **/
        m_taxonMap = new ArrayList<>();
        for (final Tree tree : geneTreesInput.get()) {
            final Map<Integer, Integer> map = new HashMap<>();
            setupTaxaMap(tree.getRoot(), map, taxonMap);
            m_taxonMap.add(map);
        }

        nrOfGeneTrees = geneTreesInput.get().size();
        nrOfSpecies = treeInput.get().getLeafNodeCount();
    }

    // initialisation code: create node number in gene tree to node number in species tree map
    private void setupTaxaMap(final Node node, final Map<Integer, Integer> map, final Map<String, Integer> taxonMap) {
        if (node.isLeaf()) {
            map.put(node.getNr(), taxonMap.get(node.getID()));
        } else {
            setupTaxaMap(node.getLeft(), map, taxonMap);
            setupTaxaMap(node.getRight(), map, taxonMap);
        }
    }

    @Override
    public double proposal() {
        final Tree tree = treeInput.get();
        m_nodes = tree.getNodesAsArray();
        final int nNodes = tree.getNodeCount();
        // randomly change left/right order
        tree.startEditing(this);  // we change the tree
        reorder(tree.getRoot());
        // collect heights
        final double[] heights = new double[nNodes];
        final int[] iReverseOrder = new int[nNodes];
        collectHeights(tree.getRoot(), heights, iReverseOrder, 0);
        // change height of an internal node
        int iNode = Randomizer.nextInt(heights.length);
        while (m_nodes[iReverseOrder[iNode]].isLeaf()) {
            iNode = Randomizer.nextInt(heights.length);
        }
        final double maxHeight = calcMaxHeight(iReverseOrder, iNode);
        heights[iNode] = Randomizer.nextDouble() * maxHeight;
        m_nodes[iReverseOrder[iNode]].setHeight(heights[iNode]);
        // reconstruct tree from heights
        final Node root = reconstructTree(heights, iReverseOrder, 0, heights.length, new boolean[heights.length]);

        assert checkConsistency(root, new boolean[heights.length]) ;
//            System.err.println("Inconsisten tree");
//        }
        root.setParent(null);
        tree.setRoot(root);
        return 0;
    }

    private boolean checkConsistency(final Node node, final boolean[] bUsed) {
        if (bUsed[node.getNr()]) {
            // used twice? tha's bad
            return false;
        }
        bUsed[node.getNr()] = true;
        if ( node.isLeaf() ) {
            return true;
        }
        return checkConsistency(node.getLeft(), bUsed) && checkConsistency(node.getRight(), bUsed);
    }

    /**
     * calculate maximum height that node iNode can become restricted
     * by nodes on the left and right
     */
    private double calcMaxHeight(final int[] iReverseOrder, final int iNode) {
        // find maximum height between two species. Only upper right part is populated
        final double[][] maxHeight = new double[nrOfSpecies][nrOfSpecies];
        for (int i = 0; i < nrOfSpecies; i++) {
            Arrays.fill(maxHeight[i], Double.POSITIVE_INFINITY);
        }

        // calculate for every species tree the maximum allowable merge point
        for (int i = 0; i < nrOfGeneTrees; i++) {
            final Tree tree = geneTreesInput.get().get(i);
            findMaximaInGeneTree(tree.getRoot(), new boolean[nrOfSpecies], m_taxonMap.get(i), maxHeight);
        }

        // find species on the left of selected node
        final boolean[] bLowerSpecies = new boolean[nrOfSpecies];
        final Node[] nodes = treeInput.get().getNodesAsArray();
        for (int i = 0; i < iNode; i++) {
            final Node node = nodes[iReverseOrder[i]];
            if (node.isLeaf()) {
                bLowerSpecies[node.getNr()] = true;
            }
        }
        // find species on the right of selected node
        final boolean[] bUpperSpecies = new boolean[nrOfSpecies];
        for (int i = iNode + 1; i < nodes.length; i++) {
            final Node node = nodes[iReverseOrder[i]];
            if (node.isLeaf()) {
                bUpperSpecies[node.getNr()] = true;
            }
        }

        // find max
        double max = Double.POSITIVE_INFINITY;
        for (int i = 0; i < nrOfSpecies; i++) {
            if (bLowerSpecies[i]) {
                for (int j = 0; j < nrOfSpecies; j++) {
                    if (j != i && bUpperSpecies[j]) {
                        final int x = Math.min(i, j);
                        final int y = Math.max(i, j);
                        max = Math.min(max, maxHeight[x][y]);
                    }
                }
            }
        }
        return max;
    } // calcMaxHeight


    /**
     * for every species in the left on the gene tree and for every species in the right
     * cap the maximum join height by the lowest place the two join in the gene tree
     */
    private void findMaximaInGeneTree(final Node node, final boolean[] taxonSet, final Map<Integer, Integer> taxonMap, final double[][] maxHeight) {
        if (node.isLeaf()) {
            final int iSpecies = taxonMap.get(node.getNr());
            taxonSet[iSpecies] = true;
        } else {
            final boolean[] bLeftTaxonSet = new boolean[nrOfSpecies];
            findMaximaInGeneTree(node.getLeft(), bLeftTaxonSet, taxonMap, maxHeight);
            final boolean[] bRightTaxonSet = new boolean[nrOfSpecies];
            findMaximaInGeneTree(node.getRight(), bRightTaxonSet, taxonMap, maxHeight);
            for (int i = 0; i < nrOfSpecies; i++) {
                if (bLeftTaxonSet[i]) {
                    for (int j = 0; j < nrOfSpecies; j++) {
                        if (j != i && bRightTaxonSet[j]) {
                            final int x = Math.min(i, j);
                            final int y = Math.max(i, j);
                            maxHeight[x][y] = Math.min(maxHeight[x][y], node.getHeight());
                        }
                    }
                }
            }
            for (int i = 0; i < nrOfSpecies; i++) {
                taxonSet[i] = bLeftTaxonSet[i] | bRightTaxonSet[i];
            }
        }
    }

    /**
     * construct tree top down by joining heighest left and right nodes *
     */


    private Node reconstructTree(final double[] heights, final int[] iReverseOrder, final int iFrom, final int iTo, final boolean[] bHasParent) {
        //iNode = maxIndex(heights, 0, heights.length);
        int iNode = -1;
        double max = Double.NEGATIVE_INFINITY;
        for (int j = iFrom; j < iTo; j++) {
            if (max < heights[j] && !m_nodes[iReverseOrder[j]].isLeaf()) {
                max = heights[j];
                iNode = j;
            }
        }
        if (iNode < 0) {
            return null;
        }
        final Node node = m_nodes[iReverseOrder[iNode]];

        //int iLeft = maxIndex(heights, 0, iNode);
        int iLeft = -1;
        max = Double.NEGATIVE_INFINITY;
        for (int j = iFrom; j < iNode; j++) {
            if (max < heights[j] && !bHasParent[j]) {
                max = heights[j];
                iLeft = j;
            }
        }

        //int iRight = maxIndex(heights, iNode+1, heights.length);
        int iRight = -1;
        max = Double.NEGATIVE_INFINITY;
        for (int j = iNode + 1; j < iTo; j++) {
            if (max < heights[j] && !bHasParent[j]) {
                max = heights[j];
                iRight = j;
            }
        }

        node.setLeft(m_nodes[iReverseOrder[iLeft]]);
        node.getLeft().setParent(node);
        node.setRight(m_nodes[iReverseOrder[iRight]]);
        node.getRight().setParent(node);
        if (node.getLeft().isLeaf()) {
            heights[iLeft] = Double.NEGATIVE_INFINITY;
        }
        if (node.getRight().isLeaf()) {
            heights[iRight] = Double.NEGATIVE_INFINITY;
        }
        bHasParent[iLeft] = true;
        bHasParent[iRight] = true;
        heights[iNode] = Double.NEGATIVE_INFINITY;


        reconstructTree(heights, iReverseOrder, iFrom, iNode, bHasParent);
        reconstructTree(heights, iReverseOrder, iNode, iTo, bHasParent);
        return node;
    }

    // helper for reconstructTree, to find maximum in range
//    private int maxIndex(final double[] heights, final int iFrom, final int iTo) {
//        int iMax = -1;
//        double max = Double.NEGATIVE_INFINITY;
//        for (int i = iFrom; i < iTo; i++) {
//            if (max < heights[i]) {
//                max = heights[i];
//                iMax = i;
//            }
//        }
//        return iMax;
//    }

   /**
      ** gather height of each node, and the node index associated with the height.*
      **/
    private int collectHeights(final Node node, final double[] heights, final int[] iReverseOrder, int iCurrent) {
        if (node.isLeaf()) {
            heights[iCurrent] = node.getHeight();
            iReverseOrder[iCurrent] = node.getNr();
            iCurrent++;
        } else {
            iCurrent = collectHeights(node.getLeft(), heights, iReverseOrder, iCurrent);
            heights[iCurrent] = node.getHeight();
            iReverseOrder[iCurrent] = node.getNr();
            iCurrent++;
            iCurrent = collectHeights(node.getRight(), heights, iReverseOrder, iCurrent);
        }
        return iCurrent;
    }

    /**
     * randomly changes left and right children in every internal node *
     */
    private void reorder(final Node node) {
        if (!node.isLeaf()) {
            if (Randomizer.nextBoolean()) {
                final Node tmp = node.getLeft();
                node.setLeft(node.getRight());
                node.setRight(tmp);
            }
            reorder(node.getLeft());
            reorder(node.getRight());
        }
    }
} // class NodeReheight
