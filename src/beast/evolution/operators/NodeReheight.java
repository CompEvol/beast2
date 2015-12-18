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
        final double[] fHeights = new double[nNodes];
        final int[] iReverseOrder = new int[nNodes];
        collectHeights(tree.getRoot(), fHeights, iReverseOrder, 0);
        // change height of an internal node
        int iNode = Randomizer.nextInt(fHeights.length);
        while (m_nodes[iReverseOrder[iNode]].isLeaf()) {
            iNode = Randomizer.nextInt(fHeights.length);
        }
        final double fMaxHeight = calcMaxHeight(iReverseOrder, iNode);
        fHeights[iNode] = Randomizer.nextDouble() * fMaxHeight;
        m_nodes[iReverseOrder[iNode]].setHeight(fHeights[iNode]);
        // reconstruct tree from heights
        final Node root = reconstructTree(fHeights, iReverseOrder, 0, fHeights.length, new boolean[fHeights.length]);

        assert checkConsistency(root, new boolean[fHeights.length]) ;
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
        final double[][] fMaxHeight = new double[nrOfSpecies][nrOfSpecies];
        for (int i = 0; i < nrOfSpecies; i++) {
            Arrays.fill(fMaxHeight[i], Double.POSITIVE_INFINITY);
        }

        // calculate for every species tree the maximum allowable merge point
        for (int i = 0; i < nrOfGeneTrees; i++) {
            final Tree tree = geneTreesInput.get().get(i);
            findMaximaInGeneTree(tree.getRoot(), new boolean[nrOfSpecies], m_taxonMap.get(i), fMaxHeight);
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
        double fMax = Double.POSITIVE_INFINITY;
        for (int i = 0; i < nrOfSpecies; i++) {
            if (bLowerSpecies[i]) {
                for (int j = 0; j < nrOfSpecies; j++) {
                    if (j != i && bUpperSpecies[j]) {
                        final int x = Math.min(i, j);
                        final int y = Math.max(i, j);
                        fMax = Math.min(fMax, fMaxHeight[x][y]);
                    }
                }
            }
        }
        return fMax;
    } // calcMaxHeight


    /**
     * for every species in the left on the gene tree and for every species in the right
     * cap the maximum join height by the lowest place the two join in the gene tree
     */
    private void findMaximaInGeneTree(final Node node, final boolean[] taxonSet, final Map<Integer, Integer> taxonMap, final double[][] fMaxHeight) {
        if (node.isLeaf()) {
            final int iSpecies = taxonMap.get(node.getNr());
            taxonSet[iSpecies] = true;
        } else {
            final boolean[] bLeftTaxonSet = new boolean[nrOfSpecies];
            findMaximaInGeneTree(node.getLeft(), bLeftTaxonSet, taxonMap, fMaxHeight);
            final boolean[] bRightTaxonSet = new boolean[nrOfSpecies];
            findMaximaInGeneTree(node.getRight(), bRightTaxonSet, taxonMap, fMaxHeight);
            for (int i = 0; i < nrOfSpecies; i++) {
                if (bLeftTaxonSet[i]) {
                    for (int j = 0; j < nrOfSpecies; j++) {
                        if (j != i && bRightTaxonSet[j]) {
                            final int x = Math.min(i, j);
                            final int y = Math.max(i, j);
                            fMaxHeight[x][y] = Math.min(fMaxHeight[x][y], node.getHeight());
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


    private Node reconstructTree(final double[] fHeights, final int[] iReverseOrder, final int iFrom, final int iTo, final boolean[] bHasParent) {
        //iNode = maxIndex(fHeights, 0, fHeights.length);
        int iNode = -1;
        double fMax = Double.NEGATIVE_INFINITY;
        for (int j = iFrom; j < iTo; j++) {
            if (fMax < fHeights[j] && !m_nodes[iReverseOrder[j]].isLeaf()) {
                fMax = fHeights[j];
                iNode = j;
            }
        }
        if (iNode < 0) {
            return null;
        }
        final Node node = m_nodes[iReverseOrder[iNode]];

        //int iLeft = maxIndex(fHeights, 0, iNode);
        int iLeft = -1;
        fMax = Double.NEGATIVE_INFINITY;
        for (int j = iFrom; j < iNode; j++) {
            if (fMax < fHeights[j] && !bHasParent[j]) {
                fMax = fHeights[j];
                iLeft = j;
            }
        }

        //int iRight = maxIndex(fHeights, iNode+1, fHeights.length);
        int iRight = -1;
        fMax = Double.NEGATIVE_INFINITY;
        for (int j = iNode + 1; j < iTo; j++) {
            if (fMax < fHeights[j] && !bHasParent[j]) {
                fMax = fHeights[j];
                iRight = j;
            }
        }

        node.setLeft(m_nodes[iReverseOrder[iLeft]]);
        node.getLeft().setParent(node);
        node.setRight(m_nodes[iReverseOrder[iRight]]);
        node.getRight().setParent(node);
        if (node.getLeft().isLeaf()) {
            fHeights[iLeft] = Double.NEGATIVE_INFINITY;
        }
        if (node.getRight().isLeaf()) {
            fHeights[iRight] = Double.NEGATIVE_INFINITY;
        }
        bHasParent[iLeft] = true;
        bHasParent[iRight] = true;
        fHeights[iNode] = Double.NEGATIVE_INFINITY;


        reconstructTree(fHeights, iReverseOrder, iFrom, iNode, bHasParent);
        reconstructTree(fHeights, iReverseOrder, iNode, iTo, bHasParent);
        return node;
    }

    // helper for reconstructTree, to find maximum in range
//    private int maxIndex(final double[] fHeights, final int iFrom, final int iTo) {
//        int iMax = -1;
//        double fMax = Double.NEGATIVE_INFINITY;
//        for (int i = iFrom; i < iTo; i++) {
//            if (fMax < fHeights[i]) {
//                fMax = fHeights[i];
//                iMax = i;
//            }
//        }
//        return iMax;
//    }

   /**
      ** gather height of each node, and the node index associated with the height.*
      **/
    private int collectHeights(final Node node, final double[] fHeights, final int[] iReverseOrder, int iCurrent) {
        if (node.isLeaf()) {
            fHeights[iCurrent] = node.getHeight();
            iReverseOrder[iCurrent] = node.getNr();
            iCurrent++;
        } else {
            iCurrent = collectHeights(node.getLeft(), fHeights, iReverseOrder, iCurrent);
            fHeights[iCurrent] = node.getHeight();
            iReverseOrder[iCurrent] = node.getNr();
            iCurrent++;
            iCurrent = collectHeights(node.getRight(), fHeights, iReverseOrder, iCurrent);
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
