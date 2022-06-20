package beast.base.evolution.tree;

/**
 * @author Alexei Drummond
 */
public interface TreeMetric {

    /**
     * Distance between two trees that may have different taxa sets but must have an overlap of taxa.
     * @param tree1
     * @param tree2
     * @return
     */
    public double distance(TreeInterface tree1, TreeInterface tree2);
    
    
    /**
     * Distance between a tree and a reference tree. Must set the reference tree first using setReference
     * @param tree1
     * @return
     */
    public double distance(TreeInterface tree);
    
    
    /**
     * Set the reference tree and cache. Faster than calling distance(tree1, tree2) everytime
     * @param ref
     */
    public void setReference(TreeInterface ref);
    
   
    
}
