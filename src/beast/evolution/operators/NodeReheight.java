package beast.evolution.operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;

@Description("Tree operator which randomly changes the height of a node, " +
        "then reconstructs the tree from node heights.")
public class NodeReheight extends TreeOperator {
    public Input<TaxonSet> m_taxonSet = new Input<TaxonSet>("taxonset", "taxon set describing species tree taxa and their gene trees", Validate.REQUIRED);
    public Input<List<Tree>> m_geneTrees = new Input<List<Tree>>("genetree", "list of gene trees that constrain species tree movement", new ArrayList<Tree>());
    Node[] m_nodes;


    /**
     * map node number of leafs in gene trees to leaf nr in species tree *
     */
    List<Map<Integer, Integer>> m_taxonMap;
    int m_nGeneTrees;
    int m_nSpecies;

    @Override
    public void initAndValidate() throws Exception {
        /** maps gene taxa names to species number **/
        Map<String, Integer> taxonMap = new HashMap<String, Integer>();
        List<Taxon> list = m_taxonSet.get().m_taxonset.get();
        for (int i = 0; i < list.size(); i++) {
            Taxon taxa = list.get(i);
            // cast should be ok if taxon-set is the set for the species tree
            TaxonSet set = (TaxonSet) taxa;
            for (Taxon taxon : set.m_taxonset.get()) {
                taxonMap.put(taxon.getID(), i);
            }
        }

        /** build the taxon map for each gene tree **/
        m_taxonMap = new ArrayList<Map<Integer, Integer>>();
        for (Tree tree : m_geneTrees.get()) {
            Map<Integer, Integer> map = new HashMap<Integer, Integer>();
            setupTaxaMap(tree.getRoot(), map, taxonMap);
            m_taxonMap.add(map);
        }

        m_nGeneTrees = m_geneTrees.get().size();
        m_nSpecies = m_tree.get().getLeafNodeCount();
    }

    // initialisation code: create node number in gene tree to node number in species tree map
    private void setupTaxaMap(Node node, Map<Integer, Integer> map, Map<String, Integer> taxonMap) {
        if (node.isLeaf()) {
            map.put(node.getNr(), taxonMap.get(node.getID()));
        } else {
            setupTaxaMap(node.m_left, map, taxonMap);
            setupTaxaMap(node.m_right, map, taxonMap);
        }
    }

    @Override
    public double proposal() {
        Tree tree = m_tree.get();
        m_nodes = tree.getNodesAsArray();
        int nNodes = tree.getNodeCount();
        // randomly change left/right order
        reorder(tree.getRoot());
        // collect heights
        double[] fHeights = new double[nNodes];
        int[] iReverseOrder = new int[nNodes];
        collectHeights(tree.getRoot(), fHeights, iReverseOrder, 0);
        // change height of an internal node
        int iNode = Randomizer.nextInt(fHeights.length);
        while (m_nodes[iReverseOrder[iNode]].isLeaf()) {
            iNode = Randomizer.nextInt(fHeights.length);
        }
        double fMaxHeight = calcMaxHeight(iReverseOrder, iNode);
        fHeights[iNode] = Randomizer.nextDouble() * fMaxHeight;
        m_nodes[iReverseOrder[iNode]].setHeight(fHeights[iNode]);
        // reconstruct tree from heights
        Node root = reconstructTree(fHeights, iReverseOrder, 0, fHeights.length, new boolean[fHeights.length]);

        if (!checkConsistency(root, new boolean[fHeights.length])) {
            System.err.println("Inconsisten tree");
        }
        root.setParent(null);
        tree.setRoot(root);
        return 0;
    }

    private boolean checkConsistency(Node node, boolean[] bUsed) {
        if (bUsed[node.getNr()]) {
            return false;
        }
        bUsed[node.getNr()] = true;
        if (!node.isLeaf()) {
            return checkConsistency(node.m_left, bUsed) && checkConsistency(node.m_right, bUsed);
        }
        return true;
    }

    /**
     * calculate maximum height that node iNode can become restricted
     * by nodes on the left and right
     */
    private double calcMaxHeight(int[] iReverseOrder, int iNode) {
        // find maximum height between two species. Only upper right part is populated
        double[][] fMaxHeight = new double[m_nSpecies][m_nSpecies];
        for (int i = 0; i < m_nSpecies; i++) {
            Arrays.fill(fMaxHeight[i], Double.POSITIVE_INFINITY);
        }

        // calculate for every species tree the maximum allowable merge point
        for (int i = 0; i < m_nGeneTrees; i++) {
            Tree tree = m_geneTrees.get().get(i);
            findMaximaInGeneTree(tree.getRoot(), new boolean[m_nSpecies], m_taxonMap.get(i), fMaxHeight);
        }

        // find species on the left of selected node
        boolean[] bLowerSpecies = new boolean[m_nSpecies];
        Node[] nodes = m_tree.get().getNodesAsArray();
        for (int i = 0; i < iNode; i++) {
            Node node = nodes[iReverseOrder[i]];
            if (node.isLeaf()) {
                bLowerSpecies[node.getNr()] = true;
            }
        }
        // find species on the right of selected node
        boolean[] bUpperSpecies = new boolean[m_nSpecies];
        for (int i = iNode + 1; i < nodes.length; i++) {
            Node node = nodes[iReverseOrder[i]];
            if (node.isLeaf()) {
                bUpperSpecies[node.getNr()] = true;
            }
        }

        // find max
        double fMax = Double.POSITIVE_INFINITY;
        for (int i = 0; i < m_nSpecies; i++) {
            if (bLowerSpecies[i]) {
                for (int j = 0; j < m_nSpecies; j++) {
                    if (j != i && bUpperSpecies[j]) {
                        int x = Math.min(i, j);
                        int y = Math.max(i, j);
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
    private void findMaximaInGeneTree(Node node, boolean[] taxonSet, Map<Integer, Integer> taxonMap, double[][] fMaxHeight) {
        if (node.isLeaf()) {
            int iSpecies = taxonMap.get(node.getNr());
            taxonSet[iSpecies] = true;
        } else {
            boolean[] bLeftTaxonSet = new boolean[m_nSpecies];
            findMaximaInGeneTree(node.m_left, bLeftTaxonSet, taxonMap, fMaxHeight);
            boolean[] bRightTaxonSet = new boolean[m_nSpecies];
            findMaximaInGeneTree(node.m_right, bRightTaxonSet, taxonMap, fMaxHeight);
            for (int i = 0; i < m_nSpecies; i++) {
                if (bLeftTaxonSet[i]) {
                    for (int j = 0; j < m_nSpecies; j++) {
                        if (j != i && bRightTaxonSet[j]) {
                            int x = Math.min(i, j);
                            int y = Math.max(i, j);
                            fMaxHeight[x][y] = Math.min(fMaxHeight[x][y], node.getHeight());
                        }
                    }
                }
            }
            for (int i = 0; i < m_nSpecies; i++) {
                taxonSet[i] = bLeftTaxonSet[i] | bRightTaxonSet[i];
            }
        }
    }

    /**
     * construct tree top down by joining heighest left and right nodes *
     */


    private Node reconstructTree(double[] fHeights, int[] iReverseOrder, int iFrom, int iTo, boolean[] bHasParent) {
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
        Node node = m_nodes[iReverseOrder[iNode]];

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

        node.m_left = m_nodes[iReverseOrder[iLeft]];
        node.m_left.setParent(node);
        node.m_right = m_nodes[iReverseOrder[iRight]];
        node.m_right.setParent(node);
        if (node.m_left.isLeaf()) {
            fHeights[iLeft] = Double.NEGATIVE_INFINITY;
        }
        if (node.m_right.isLeaf()) {
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
    private int maxIndex(double[] fHeights, int iFrom, int iTo) {
        int iMax = -1;
        double fMax = Double.NEGATIVE_INFINITY;
        for (int i = iFrom; i < iTo; i++) {
            if (fMax < fHeights[i]) {
                fMax = fHeights[i];
                iMax = i;
            }
        }
        return iMax;
    }

    /**
     * gather height of each node, and order of the nodes *
     */
    private int collectHeights(Node node, double[] fHeights, int[] iReverseOrder, int iCurrent) {
        if (node.isLeaf()) {
            fHeights[iCurrent] = node.getHeight();
            iReverseOrder[iCurrent] = node.getNr();
            iCurrent++;
        } else {
            iCurrent = collectHeights(node.m_left, fHeights, iReverseOrder, iCurrent);
            fHeights[iCurrent] = node.getHeight();
            iReverseOrder[iCurrent] = node.getNr();
            iCurrent++;
            iCurrent = collectHeights(node.m_right, fHeights, iReverseOrder, iCurrent);
        }
        return iCurrent;
    }

    /**
     * randomly changes left and right children in every internal node *
     */
    private void reorder(Node node) {
        if (!node.isLeaf()) {
            if (Randomizer.nextBoolean()) {
                Node tmp = node.m_left;
                node.m_left = node.m_right;
                node.m_right = tmp;
            }
            reorder(node.m_left);
            reorder(node.m_right);
        }
    }
} // class NodeReheight
