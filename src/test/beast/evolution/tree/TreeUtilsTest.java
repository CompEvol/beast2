package test.beast.evolution.tree;

import beast.evolution.tree.TreeUtils;
import beast.util.TreeParser;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Test class for TreeUtils methods
 */
public class TreeUtilsTest extends TestCase {

    @Test
    public void testSortTreeAlphabetically() {

        String newick = "((D:5.0,C:4.0):6.0,(A:1.0,B:2.0):3.0):0.0;";

        TreeParser treeParser = new TreeParser();
        treeParser.initByName("IsLabelledNewick", true,
        		"newick", newick,
        		"adjustTipHeights", false);


        TreeUtils.rotateTreeAlphabetically(treeParser.getRoot());

        String result = treeParser.getRoot().toNewick();
        String goal = "((A:1.0,B:2.0):3.0,(C:4.0,D:5.0):6.0):0.0";

        assertEquals(goal, result);
    }
}
