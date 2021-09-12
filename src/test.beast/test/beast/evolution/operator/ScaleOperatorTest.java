package test.beast.evolution.operator;

import org.junit.Test;

import beast.base.evolution.operator.ScaleOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.TreeParser;
import junit.framework.TestCase;

public class ScaleOperatorTest extends TestCase {
	final static double EPSILON = 1e-10;

	
	@Test
	public void testTreeScaling() {
        String newick = "((0:1.0,1:1.0)4:1.0,(2:1.0,3:1.0)5:0.5)6:0.0;";

        TreeParser tree = new TreeParser(newick, false, false, false, 0);

        Node [] node = tree.getNodesAsArray();
        
        ScaleOperator operator = new ScaleOperator();
        operator.initByName("tree", tree, "weight", 1.0);
        operator.proposal();
        
        // leaf node
        node = tree.getNodesAsArray();
        assertEquals(0.0, node[0].getHeight(), EPSILON);
        assertEquals(0.0, node[1].getHeight(), EPSILON);
        // leaf node, not scaled
        assertEquals(0.5, node[2].getHeight(), EPSILON);
        assertEquals(0.5, node[3].getHeight(), EPSILON);
        
        // internal nodes, all scaled
        // first determine scale factor
        double scale = node[4].getHeight() / 1.0;
        assertEquals(1.0 * scale, node[4].getHeight(), EPSILON);
        assertEquals(1.5 * scale, node[5].getHeight(), EPSILON);
        assertEquals(2.0 * scale, node[6].getHeight(), EPSILON);
	}
}
