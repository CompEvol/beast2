package test.beast.evolution.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.TreeParser;

/**
 * Test class for Node methods
 */
public class NodeTest  {
    String[] trees = new String[]{
            "((A:1.5,B:0.5):1.1,C:3.0):0.0;",
            "((2:1.5,1:0.5):1.1,3:3.0):0.0;"
    };
    Node[] roots = new Node[trees.length];

    @BeforeEach
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

    @Test
    public void testBinarySortedNewickTest() {

        String newick = "((D:5.0,C:4.0):6.0,(A:1.0,B:2.0):3.0):0.0;";

        TreeParser treeParser = new TreeParser();
        treeParser.initByName("IsLabelledNewick", true,
        		"newick", newick,
        		"adjustTipHeights", false);

        String sortedNewick = treeParser.getRoot().toSortedNewick(new int[1], false);
        String goal = "((1:1.0,2:2.0):3.0,(3:4.0,4:5.0):6.0):0.0";

        assertEquals(goal.split(";")[0], sortedNewick);
    }

    @Test
    public void testNonBinarySortedNewickTest() {

        String newick = "((A:1.0,B:1.0,C:1.0):1.0,(D:1.0,E:1.0,F:1.0,G:1.0):1.0):0.0;";

        TreeParser treeParser = new TreeParser();
        treeParser.initByName("IsLabelledNewick", true,
        		"newick", newick,
        		"adjustTipHeights", false,
                "binarizeMultifurcations", false);

        String sortedNewick = treeParser.getRoot().toSortedNewick(new int[1], false);
        String goal = "((1:1.0,2:1.0,3:1.0):1.0,(4:1.0,5:1.0,6:1.0,7:1.0):1.0):0.0";

        assertEquals(goal.split(";")[0], sortedNewick);
    }
}
