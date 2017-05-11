package test.beast.evolution.tree;

import beast.util.TreeParser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for Node methods
 */
public class NodeTest {

    @Test
    public void binarySortedNewickTest() {

        String newick = "((D:5.0,C:4.0):6.0,(A:1.0,B:2.0):3.0):0.0;";

        TreeParser treeParser = new TreeParser();
        treeParser.initByName("IsLabelledNewick", true,
        		"newick", newick,
        		"adjustTipHeights", false);

        String sortedNewick = treeParser.getRoot().toSortedNewick(new int[1], false);
        String goal = "((1:1.0,2:2.0):3.0,(3:4.0,4:5.0):6.0):0.0";

        Assert.assertEquals(goal.split(";")[0], sortedNewick);
    }

    @Test
    public void nonBinarySortedNewickTest() {

        String newick = "((A:1.0,B:1.0,C:1.0):1.0,(D:1.0,E:1.0,F:1.0,G:1.0):1.0):0.0;";

        TreeParser treeParser = new TreeParser();
        treeParser.initByName("IsLabelledNewick", true,
        		"newick", newick,
        		"adjustTipHeights", false,
                "binarizeMultifurcations", false);

        String sortedNewick = treeParser.getRoot().toSortedNewick(new int[1], false);
        String goal = "((1:1.0,2:1.0,3:1.0):1.0,(4:1.0,5:1.0,6:1.0,7:1.0):1.0):0.0";

        Assert.assertEquals(goal.split(";")[0], sortedNewick);
    }
}
