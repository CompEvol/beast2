package beast.base.evolution.tree;



import java.util.List;

import beast.base.evolution.alignment.TaxonSet;

public interface TreeInterface {
    String getID();

    int getLeafNodeCount();
	int getInternalNodeCount();
	int getNodeCount();

	Node getRoot();
    Node getNode(int i);
    Node [] getNodesAsArray();

    List<Node> getExternalNodes();
    List<Node> getInternalNodes();
    
    TaxonSet getTaxonset();
    
	boolean somethingIsDirty();

    public void getMetaData(Node node, Double[] t, String pattern);
    public void setMetaData(Node node, Double[] t, String pattern);

    /*
    * Note that leaf nodes are always numbered 0,...,nodeCount-1
    * Internal nodes are numbered higher, but the root has no guaranteed 
    * number.
    */

    /**
     * @param node  top of tree/sub tree (null defaults to whole tree)
     * @param nodes array to fill (null will result in creating a new one)
     * @return tree nodes in post-order, children before parents
     */
    default Node[] listNodesPostOrder(Node node, Node[] nodes) {
        if (node == null) {
            node = getRoot();
        }
        if (nodes == null) {
            // overall node count is cached, faster
            final int n = node == getRoot() ? getNodeCount() : node.getNodeCount();
            nodes = new Node[n];
        }
        getNodesPostOrder(node, nodes, 0);
        return nodes;
    }

    static int getNodesPostOrder(final Node node, final Node[] nodes, int pos) {
        //node.m_tree = this;
        for (final Node child : node.children) {
            pos = getNodesPostOrder(child, nodes, pos);
        }
        nodes[pos] = node;
        return pos + 1;
    }
}
