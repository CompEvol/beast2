package test.beast.evolution.tree.coalescent;

import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.tree.coalescent.BayesianSkyline;
import beast.util.TreeParser;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Alexei Drummond
 */
public class BayesianSkylineTest extends TestCase {

    @Test
    public void testSkyline() throws Exception {

        RealParameter popSize = new RealParameter(1.0, 0.0, 10.0, 2);
        IntegerParameter groupSize = new IntegerParameter(2, 1, 4, 2);

//        List<Sequence> sequences = new ArrayList<Sequence>();
//
//        sequences.add(new Sequence("A", ""));
//        sequences.add(new Sequence("B", ""));
//        sequences.add(new Sequence("C", ""));
//        sequences.add(new Sequence("D", ""));
//        sequences.add(new Sequence("E", ""));

        popSize.setValue(1, 2.0);

//        Alignment alignment = new Alignment(sequences, 4, "nucleotide");

        TreeParser tree = new TreeParser("(((1:1,2:1):2.5,(3:1.5,4:1.5):2):2,5:5.5);");

        BayesianSkyline skyline = new BayesianSkyline();
        skyline.init(popSize, groupSize, tree);

        assertEquals(skyline.getPopSize(0.01), 1.0);
        assertEquals(skyline.getPopSize(1.49), 1.0);
        assertEquals(skyline.getPopSize(1.51), 2.0);
        assertEquals(skyline.getPopSize(5.51), 2.0);

        //assertTrue("No proper description for: " + sUndocumentedPlugins.toString(), sUndocumentedPlugins.size() == 0);
    } // testDescriptions

}
