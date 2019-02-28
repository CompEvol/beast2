package test.beast.evolution.tree.coalescent;

import org.junit.Assert;

import beast.evolution.tree.Tree;
import beast.evolution.tree.coalescent.TreeIntervals;
import beast.util.TreeParser;
import junit.framework.TestCase;

public class IntervalsTest extends TestCase {
    public void testCoalescentTimes() throws Exception {
    	runTreeTest("(t1:2,t2:2):1;", new double[] {2.0});
    	runTreeTest("(t1:2,t2:2,t3:2):1;", new double[] {2.0, 2.0});
    	runTreeTest("(t1:2,(t2:1,t3:1):1):1;", new double[] {1.0, 2.0});
    	runTreeTest("((t1:1):2,(t2:2,t3:2):1):1;", new double[] {2.0, 3.0});
    	runTreeTest("((t1:1):1,(t2:1,t3:1):1):1;", new double[] {1.0, 2.0});
    }
    
    private void runTreeTest(String tree, double[] expectedCoalescentTimes) {
        Tree treeState = new TreeParser(tree);

        TreeIntervals intervals = new TreeIntervals();                  
        intervals.initByName("tree", treeState);

        intervals.setIntervalsUnknown();
        Assert.assertArrayEquals(expectedCoalescentTimes, intervals.getCoalescentTimes(null), 1e-5);
    }
}
