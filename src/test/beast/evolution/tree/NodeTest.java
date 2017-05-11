package test.beast.evolution.tree;

import beast.evolution.tree.Node;
import beast.util.TreeParser;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * @author Walter Xie
 */
public class NodeTest extends TestCase {
    String[] trees = new String[]{
            "((A:1.5,B:0.5):1.1,C:3.0):0.0;",
            "((2:1.5,1:0.5):1.1,3:3.0):0.0;"
    };
    Node[] roots = new Node[trees.length];

    @Override
    public void setUp() throws Exception {
        for (int i = 0; i < trees.length; i++) {
            TreeParser newickTree = new TreeParser(trees[i], false, false, true, 1);
            roots[i] = newickTree.getRoot();
        }
    }

    @Test
    public void testToNewick() {
        String newick = roots[0].toNewick();
        assertEquals("((A:1.5,B:0.5):1.1,C:3.0):0.0", newick);

        newick = roots[0].getChild(0).toNewick();
        assertEquals("(A:1.5,B:0.5):1.1", newick);

        newick = roots[0].getChild(1).toNewick();
        assertEquals("C:3.0", newick);

        newick = roots[1].toNewick();
        assertEquals("((2:1.5,1:0.5):1.1,3:3.0):0.0", newick);

    }


    @Test
    public void testRootToString() { // printInternalNodeNumbers=true
        String newick = roots[0].toString();
        assertEquals("((0:1.5,1:0.5)3:1.1,2:3.0)4:0.0", newick);

        newick = roots[0].getChild(0).toString();
        assertEquals("(0:1.5,1:0.5)3:1.1", newick);

        newick = roots[0].getChild(1).toString();
        assertEquals("2:3.0", newick);

        newick = roots[1].toString();
        assertEquals("((1:1.5,0:0.5)3:1.1,2:3.0)4:0.0", newick);
    }

}
