package beast.evolution.tree;

import java.util.*;

/**
 * @author Alexei Drummond
 */
public class TreeUtils {

    /**
     * Recursively order.
     */
    public static void rotateNodeByComparator(Node node, Comparator<Node> comparator) {

        if (node.getChildCount() > 2) throw new RuntimeException("Not implemented yet!");

        for (Node child : node.getChildren()) {
            rotateNodeByComparator(child, comparator);
        }

        if (node.getChildCount() > 1) {
            if (comparator.compare(node.getLeft(), node.getRight()) > 0) {
                Node temp = node.getLeft();
                node.setLeft(node.getRight());
                node.setRight(temp);
            }
        }
    }

    public static Comparator<Node> createNodeDensityComparator() {

        return new Comparator<Node>() {

            public int compare(Node node1, Node node2) {
                return node2.getLeafNodeCount() - node1.getLeafNodeCount();
            }

            public boolean equals(Node node1, Node node2) {
                return node1.getLeafNodeCount() == node2.getLeafNodeCount();
            }
        };
    }


    public static Comparator<Node> createNodeDensityMinNodeHeightComparator() {

        return new Comparator<Node>() {

            public int compare(Node node1, Node node2) {
                int larger = node1.getLeafNodeCount() - node2.getLeafNodeCount();

                if (larger != 0) return larger;

                double tipRecent = getMinNodeHeight(node1) - getMinNodeHeight(node2);
                if (tipRecent > 0.0) return -1;
                if (tipRecent < 0.0) return 1;
                return 0;
            }

        };
    }

    public static Comparator<Node> createReverseNodeDensityMinNodeHeightComparator() {
        return new Comparator<Node>() {

            public int compare(Node node1, Node node2) {
                int larger = node2.getLeafNodeCount() - node1.getLeafNodeCount();


                if (larger != 0) return larger;

                double tipRecent = getMinNodeHeight(node2) - getMinNodeHeight(node1);
                if (tipRecent > 0.0) return -1;
                if (tipRecent < 0.0) return 1;
                return 0;
            }

        };
    }

    public static double getMinNodeHeight(Node node) {
        if (!node.isLeaf()) {
            double minNodeHeight = Double.MAX_VALUE;
            for (Node child : node.getChildren()) {
                double childMinHeight = getMinNodeHeight(child);
                if (childMinHeight < minNodeHeight) {
                    minNodeHeight = childMinHeight;
                }
            }
            return minNodeHeight;
        } else return node.getHeight();
    }

    public static double getDoubleMetaData(Node node, String metaDataName) {
        Object metaData = node.getMetaData(metaDataName);
        if (metaData instanceof Integer) return (double) ((Integer) metaData);
        if (metaData instanceof Double) return (Double) metaData;
        if (metaData instanceof String) return Double.parseDouble((String) metaData);
        return -1;
    }

    /**
     * Gets the most recent common ancestor (MRCA) node of a set of leaf nodes.
     *
     * @param tree      the Tree
     * @param leafNodes a set of names
     * @return the NodeRef of the MRCA
     */
    public static Node getCommonAncestorNode(Tree tree, Set<String> leafNodes) {

        int cardinality = leafNodes.size();

        if (cardinality == 0) {
            throw new IllegalArgumentException("No leaf nodes selected");
        }

        Node[] mrca = {null};
        getCommonAncestorNode(tree, tree.getRoot(), leafNodes, cardinality, mrca);

        return mrca[0];
    }

    /*
    * Private recursive function used by getCommonAncestorNode.
    */
    private static int getCommonAncestorNode(Tree tree, Node node,
                                             Set<String> leafNodes, int cardinality,
                                             Node[] mrca) {

        if (node.isLeaf()) {
            if (leafNodes.contains(tree.getTaxonId(node))) {
                if (cardinality == 1) {
                    mrca[0] = node;
                }
                return 1;
            } else {
                return 0;
            }
        }

        int matches = 0;

        for (Node child : node.getChildren()) {
            matches += getCommonAncestorNode(tree, child, leafNodes, cardinality, mrca);
            if (mrca[0] != null) {
                break;
            }
        }

        if (mrca[0] == null) {
            // If we haven't already found the MRCA, test this node
            if (matches == cardinality) {
                mrca[0] = node;
            }
        }

        return matches;
    }

    /**
     * @param tree
     * @param node
     * @return the length of the (sub)tree below the given node.
     */
    public static double getTreeLength(Tree tree, Node node) {

        int childCount = node.getChildCount();
        if (childCount == 0) return node.getLength();

        double length = 0;
        for (Node child : node.getChildren()) {
            length += getTreeLength(tree, child);
        }
        if (node != tree.getRoot())
            length += node.getLength();
        return length;
    }

    /**
     * @param tree
     * @return the sum of the external branch lengths of the given tree
     */
    public static double getExternalLength(Tree tree) {
        double length = 0.0;
        for (Node node : tree.getExternalNodes()) {
            length += node.getLength();
        }
        return length;
    }

    /**
     * @param tree
     * @return the sum of the internal branch lengths of the given tree
     */
    public static double getInternalLength(Tree tree) {
        double length = 0.0;
        for (Node node : tree.getExternalNodes()) {
            length += node.getLength();
        }
        return length;
    }

    /**
     * @return the intervals in an ultrametric tree in order from root to tips.
     */
    public static double[] getIntervals(Tree tree) {

        List<Double> heights = new ArrayList<Double>();

        for (Node node : tree.getInternalNodes()) {
            heights.add(node.getHeight());
        }
        Collections.sort(heights, Collections.reverseOrder());

        double[] intervals = new double[heights.size()];
        for (int i = 0; i < intervals.length - 1; i++) {
            double height1 = heights.get(i);
            double height2 = heights.get(i + 1);

            intervals[i] = height1 - height2;
        }
        intervals[intervals.length - 1] = heights.get(intervals.length - 1);

        return intervals;

    }

    /**
     * @param tree the tree
     * @param node the node to get names of leaves below
     * @return a set of taxa names (as strings) of the leaf nodes descended from the given node.
     */
    public static Set<String> getDescendantLeaves(Tree tree, Node node) {

        HashSet<String> set = new HashSet<String>();
        getDescendantLeaves(tree, node, set);
        return set;
    }

    /**
     * @param tree the tree
     * @param node the node to get name of leaves below
     * @param set  will be populated with taxa names (as strings) of the leaf nodes descended from the given node.
     */
    private static void getDescendantLeaves(Tree tree, Node node, Set<String> set) {

        if (node.isLeaf()) {
            set.add(tree.getTaxonId(node));
        } else {

            for (Node child : node.getChildren()) {
                getDescendantLeaves(tree, child, set);
            }
        }
    }

    /**
     * @param tree the tree to test fo ultrametricity
     * @param threshold the largest absolute value of height that a leaf node can have
     *                  and the tree still be regarded as ultrametric
     * @return true only if all tips have height 0.0
     */
    public static boolean isUltrametric(Tree tree, double threshold) {
        for (Node node : tree.getExternalNodes()) {
            if (Math.abs(node.getHeight()) > threshold)
                return false;
        }
        return true;
    }

    /**
     * @param tree the tree to test fo ultrametricity
     * @return true only if all tips have height exactly 0.0. Since newick is expressed in branch lengths
     * it may be necessary to use isUltrametric(tree, threshold) to allow for numerical precision issues.
     */
    public static boolean isUltrametric(Tree tree) {
        for (Node node : tree.getExternalNodes()) {
            if (node.getHeight() != 0.0)
                return false;
        }
        return true;
    }

    /**
     * @param tree the tree to test if binary
     * @return true only if internal nodes have 2 children
     */
    public static boolean isBinary(Tree tree) {
        for (Node node : tree.getInternalNodes()) {
            if (node.getChildCount() != 2)
                return false;
        }
        return true;
    }

    /**
     * get tree topology in Newick that is sorted by taxa labels or node indexes.
     * @param node
     * @param isTaxaLabel       if true, then print taxa label instead of node index
     * @return
     */
    public static String sortedNewickTopology(Node node, boolean isTaxaLabel) {
        if (node.isLeaf()) {
            if (isTaxaLabel) {
                return String.valueOf(node.getID());
            } else {
                return String.valueOf(node.getNr());
            }
        } else {
            StringBuilder builder = new StringBuilder("(");

            List<String> subTrees = new ArrayList<String>();
            for (int i = 0; i < node.getChildCount(); i++) {
                subTrees.add(sortedNewickTopology(node.getChild(i), isTaxaLabel));
            }

            Collections.sort(subTrees);

            for (int i = 0; i < subTrees.size(); i++) {
                builder.append(subTrees.get(i));
                if (i < subTrees.size() - 1) {
                    builder.append(",");
                }
            }
            builder.append(")");

            return builder.toString();
        }
    }

}
