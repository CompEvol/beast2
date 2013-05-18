package beast.util;

import beast.evolution.tree.Tree;

/**
 * @author Alexei Drummond
 */
public interface NexusParserListener {

    /**
     * This method is called to alert a listener that a tree has been parsed by the nexus parser
     * @param treeIndex the index of the tree (starting from zero)
     * @param tree the tree that has been parsed
     */
    void treeParsed(int treeIndex, Tree tree);
}
