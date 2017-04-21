package beast.evolution.tree;

import beast.core.Description;

import java.util.ArrayList;
import java.util.List;

@Description("Tree can be changed, such as re-root. Imported from BEAST 1 FlexibleTree.")
public class FlexibleTree extends Tree {

    // Tree class does not support setLength
    double[] allBranchLengths; // index is Nr


    public FlexibleTree(final String newick) {
        super(newick);
    }

    public void setAllBranchLengths() {
        allBranchLengths = getAllBranchLengths(getRoot(), getNodeCount());
    }

    public double[] getAllBranchLengths() {
        if (allBranchLengths == null)
            setAllBranchLengths();
        return allBranchLengths;
    }

    public double getLength(Node node) {
        if (allBranchLengths == null)
            setAllBranchLengths();
        int nodeNr = node.getNr();
        return allBranchLengths[nodeNr];
    }

    /**
     * Get all branch lengths of sub/tree.
     * If lengths[nodeNr] == 0, then either is root or not child node
     *
     * @param node a given node, such as <code>getRoot()</code>
     * @param maxNr the max index of all nodes, such as <code>getNodeCount()</code>
     * @return
     */
    protected double[] getAllBranchLengths(final Node node, int maxNr) {
        double[] lengths = new double[maxNr];
        List<Node> allChildNodes = node.getAllChildNodes();
        for (Node child : allChildNodes) {
            int nodeNr = child.getNr();
            double branchLength = child.getLength();
//            if (lengths[nodeNr] > 0)
//                throw new IllegalArgumentException("Duplicate node Nr is invalid !");
            lengths[nodeNr] = branchLength;
        }
        return lengths;
    }


    /**
     * Get the maximum node height of the sub/tree including given <code>node</code>
     *
     * @param node
     * @return
     */
    public static double getMaxNodeHeight(Node node) {
        if (!node.isLeaf()) {
            double maxNodeHeight = 0;
            for (Node child : node.getAllChildNodes()) {
                double childHeight = child.getHeight();
                if (maxNodeHeight < childHeight)
                    maxNodeHeight = childHeight;
            }
            return maxNodeHeight;
        } else return node.getHeight();
    }

    /**
     * Set the node heights from the given branch lengths.
     */
    protected void setNodeHeightsByLengths() {

        nodeLengthsToHeights(getRoot(), 0.0);

        double maxHeight = FlexibleTree.getMaxNodeHeight(getRoot());

        for (int i = 0; i < getNodeCount(); i++) {
            Node node = getNode(i);
            // Set the node heights to the reversed heights
            node.setHeight(maxHeight - node.getHeight());
        }

    }

    /**
     * Set the node heights from the current node branch lengths.
     * Actually sets distance from root so the heights then need to be reversed.
     */
    private void nodeLengthsToHeights(Node node, double height) {

        double newHeight = height;

        // getLength call setAllBranchLengths() in the first time
        if (getLength(node) > 0.0)
            newHeight += getLength(node);

        node.setHeight(newHeight);

        for (Node child : node.getChildren()) {
            nodeLengthsToHeights(child, newHeight);
        }

    }


    /**
     * Re-root the tree on the branch above the given <code>node</code>
     * with the given new root.
     * <code>len(node, new_root) = len(node, parent) * propLen </code>
     *
     * @param node given node
     * @param propLen the proportion of the branch length between <code>node</code>
     *                and its parent node to define the new root, such as 0.5.
     */
    public void changeRootTo(Node node, double propLen) {
        // restrict to binary tree
        if (!TreeUtils.isBinary(this))
            throw new IllegalArgumentException("changeRootTo is only available to binary tree !");

        Node parent = node.getParent();
        if (parent == null || parent == root) {
            // the node is already the root so nothing to do...
            return;
        }

        hasStartedEditing = true;
        // todo m_tree.getState() == null
//        startEditing(null); // called in rm / add

        setAllBranchLengths();

        Node parent2 = parent.getParent();

        // only change topology
        swapParentNode(parent, parent2, null);

        // the root is now free so use it as the root again
        parent.removeChild(node);
        getRoot().addChild(node);
        getRoot().addChild(parent);

        setNodeHeightsByLengths();

        hasStartedEditing = false; // todo is it correct to use restore()? no proposal
    }

    /**
     * Work up through the tree putting the parent into the child.
     */
    private void swapParentNode(Node node, Node parent, Node child) {

        if (parent != null) {
            Node parent2 = parent.getParent();

            swapParentNode(parent, parent2, node);

            if (child != null) {
                node.removeChild(child);
                child.addChild(node);
//                node.setLength(child.getLength());
            }

        } else {
            // First remove child from the root
            node.removeChild(child);

//            if (node.getChildCount() > 1) {
//                throw new IllegalArgumentException("Trees must be binary");
//            }
//
//            Node tmp = node.getChild(0);
//            node.removeChild(tmp);
//            child.addChild(tmp);
//            tmp.setHeight(tmp.getHeight() - heightDiff);


// todo can't remove from list if browsing it with "for each" loop
            List<Node> children = new ArrayList<>(node.getChildren());
            for (int i=0; i<children.size(); i++) {
                Node tmp = children.get(i);
                node.removeChild(tmp);
                child.addChild(tmp);
//                tmp.setLength(tmp.getLength() + child.getLength());
            } // todo check if correct for > 2 children in old root
        }

    }




}
