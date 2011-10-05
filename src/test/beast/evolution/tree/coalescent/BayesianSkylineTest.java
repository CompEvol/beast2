package test.beast.evolution.tree.coalescent;


import beast.evolution.tree.Tree;
import beast.evolution.tree.coalescent.BayesianSkyline;
import beast.evolution.tree.coalescent.TreeIntervals;
import junit.framework.TestCase;
import org.junit.Test;


/**
 * @author Alexei Drummond
 */
public class BayesianSkylineTest extends TestCase {

    @Test
    public void testSkyline() throws Exception {

        //RealParameter popSize = new RealParameter("1.0", 0.0, 10.0, 2);
        //IntegerParameter groupSize = new IntegerParameter("2", 1, 4, 2);

        //popSize.setValue(1, 2.0);

        Tree tree = new Tree("(((1:1,2:1):2.5,(3:1.5,4:1.5):2):2,5:5.5);");
        TreeIntervals intervals = new TreeIntervals();
        intervals.init(tree);

        BayesianSkyline skyline = new BayesianSkyline();
        //skyline.init(popSize, groupSize, intervals);
        skyline.initByName("popSizes", "1.0 2.0", 
        					"groupSizes", "2 2", 
        					"treeIntervals", intervals);

        assertEquals(skyline.getPopSize(0.01), 1.0);
        assertEquals(skyline.getPopSize(1.49), 1.0);
        assertEquals(skyline.getPopSize(1.51), 2.0);
        assertEquals(skyline.getPopSize(5.51), 2.0);

    }

}
