package beast.evolution.tree;

import java.util.Comparator;

/**
 * @author Alexei Drummond
 */
public class TreeUtils {

    /**
     * Recursively order.
     */
    public static void rotateNodeByComparator(Node node, Comparator<Node> comparator) {

        if (!node.isLeaf()) {

            rotateNodeByComparator(node.m_left, comparator);
            rotateNodeByComparator(node.m_right, comparator);

            if (comparator.compare(node.m_left, node.m_right) > 0) {
                Node temp = node.m_left;
                node.m_left = node.m_right;
                node.m_right = temp;
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

                double tipRecent = getMinNodeHeight(node2) - getMinNodeHeight(node1);
                if (tipRecent > 0.0) return 1;
                if (tipRecent < 0.0) return -1;
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
                if (tipRecent > 0.0) return 1;
                if (tipRecent < 0.0) return -1;
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

}
