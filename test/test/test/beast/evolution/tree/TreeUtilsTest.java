package test.beast.evolution.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import beast.base.evolution.tree.TreeParser;
import beast.base.evolution.tree.TreeUtils;

/**
 * Test class for TreeUtils methods
 */
public class TreeUtilsTest  {

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
