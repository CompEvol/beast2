package test.beast.evolution.operator;

import org.junit.Test;

import beast.core.State;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.operators.Exchange;
import beast.util.Randomizer;
import beast.util.TreeParser;


import junit.framework.TestCase;

public class ExchangeOperatorTest extends TestCase {
	
	@Test 
	public void testNarrowExchange4Taxa() throws Exception {
		
        int runs = 10000;
        Randomizer.setSeed(666);
        // test that going from source tree to target tree 
        // is as likely as going the other way around
        // taking the HR in account.
        Sequence A = new Sequence("A", "A");
        Sequence B = new Sequence("B", "A");
        Sequence C = new Sequence("C", "A");
        Sequence D = new Sequence("D", "A");

        Alignment data = new Alignment();
        data.initByName("sequence", A, "sequence", B, "sequence", C, "sequence", D,
                "dataType", "nucleotide"
        );
        String sourceTree = "((A:2.0,B:2.0):1.0,(C:1.0,D:1.0):2.0):0.0"; // ((A,B),(C,D))
        String targetTree = "((A:2.0,(C:1.0,D:1.0):1.0):1.0,B:3.0):0.0"; // ((A,(C,D)),B)
        testNarrowExchange(sourceTree, targetTree, runs, data);
	}
	
    @Test
	public void testNarrowExchange5Taxa() throws Exception {
    	int runs = 10000;
	    Randomizer.setSeed(666);
        Sequence A = new Sequence("A", "A");
        Sequence B = new Sequence("B", "A");
        Sequence C = new Sequence("C", "A");
        Sequence D = new Sequence("D", "A");
        Sequence E = new Sequence("E", "A");

        Alignment data = new Alignment();
        data.initByName("sequence", A, "sequence", B, "sequence", C, "sequence", D, "sequence", E,
                "dataType", "nucleotide"
        );
        String sourceTree = "(((A:2.0,B:2.0):1.0,(C:1.0,D:1.0):2.0):1.0,E:4.0):0.0"; // (((A,B),(C,D)),E)
        String targetTree = "(((A:2.0,(C:1.0,D:1.0):1.0):1.0,B:3.0):1.0,E:4.0):0.0"; // (((A,(C,D)),B),E)
        testNarrowExchange(sourceTree, targetTree, runs, data);
    }

    @Test
	public void testNarrowExchange6Taxa() throws Exception {
    	int runs = 10000;
	    Randomizer.setSeed(666);
        Sequence A = new Sequence("A", "A");
        Sequence B = new Sequence("B", "A");
        Sequence C = new Sequence("C", "A");
        Sequence D = new Sequence("D", "A");
        Sequence E = new Sequence("E", "A");
        Sequence F = new Sequence("F", "A");

        Alignment data = new Alignment();
        data.initByName("sequence", A, "sequence", B, "sequence", C, "sequence", D, "sequence", E, "sequence", F,
                "dataType", "nucleotide"
        );
        //String sourceTree = "((((A:2.0,B:2.0):1.0,(C:1.0,D:1.0):2.0):1.0,E:4.0):1.0,F:5.0):0.0"; // ((((A,B),(C,D)),E),F)
        //String targetTree = "((((A:2.0,(C:1.0,D:1.0):1.0):1.0,B:3.0):1.0,E:4.0):1.0,F:5.0):0.0"; // ((((A,(C,D)),B),E),F)

        String sourceTree = "(((A:5.0,B:5.0):2.0,((C:5.0,D:5.0):1.0,E:6.0):1.0):1.0,F:8.0):0.0"; 
        String targetTree = "(((A:5.0,B:5.0):2.0,F:7.0):1.0,((C:5.0,D:5.0):1.0,E:6.0):2.0):0.0"; 

        testNarrowExchange(sourceTree, targetTree, runs, data);
    }
	
	void testNarrowExchange(String sourceTree, String targetTree, int runs, Alignment data) throws Exception {
		
        // first test going from source to target
        double match = 0;
        for (int i = 0; i < runs; i++) {
            TreeParser tree = new TreeParser();
            tree.initByName("taxa", data, "newick", sourceTree);
    		State state = new State();
    		state.initByName("stateNode", tree);
    		state.initialise();
            Exchange operator = new Exchange();
            operator.initByName("isNarrow", true, "tree", tree, "weight", 1.0);

            double logHR = operator.proposal();
            String treeString = tree.getRoot().toNewick();
            if (treeString.equals(targetTree) && !Double.isInfinite(logHR)) {
            	// proportion of accepts equals min(HR, 1.0)
            	match += Math.min(Math.exp(logHR), 1.0);
            }
        }

        System.out.println(" Matches: " + match * 100.0/runs+ "%");

        
        // now test going from target to source
        double match2 = 0;
        for (int i = 0; i < runs; i++) {
            TreeParser tree = new TreeParser();
            tree.initByName("taxa", data, "newick", targetTree);
    		State state = new State();
    		state.initByName("stateNode", tree);
    		state.initialise();
            Exchange operator = new Exchange();
            operator.initByName("isNarrow", true, "tree", tree, "weight", 1.0);

            double logHR = operator.proposal();
            String treeString = tree.getRoot().toNewick();
            if (treeString.equals(sourceTree) && !Double.isInfinite(logHR)) {
            	// proportion of accepts equals min(HR, 1.0)
            	match2 += Math.min(Math.exp(logHR), 1.0);
            }
        }
        
        System.out.println(" Matches: " + match2 * 100.0/runs+ "%");
        assertTrue("difference(" + 100*(match-match2)/runs + ") exceeds 1.0%", 100.0*Math.abs(match-match2)/runs < 1.0); 
	
	}

}
