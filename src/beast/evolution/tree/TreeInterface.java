package beast.evolution.tree;



import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;

public interface TreeInterface {
    String getID();

    int getLeafNodeCount();
	int getInternalNodeCount();
	int getNodeCount();

	Node getRoot();
    Node getNode(int i);
    Node [] getNodesAsArray();

    TaxonSet getTaxonset();
    
	boolean somethingIsDirty();

    public void getMetaData(Node node, Double[] fT, String sPattern);
    public void setMetaData(Node node, Double[] fT, String sPattern);

    /*
    * Note that leaf nodes are always numbered 0,...,nodeCount-1
    * Internal nodes are numbered higher, but the root has no guaranteed 
    * number.
    */
}
