package test.beast.evolution.tree;

import org.junit.jupiter.api.Test;

import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.TreeParser;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TreeTest  {
	final static double EPSILON = 1e-10;

	@Test
	public void testTreeScaling() {
        String newick = "((0:1.0,1:1.0)4:1.0,(2:1.0,3:1.0)5:0.5)6:0.0;";

        TreeParser treeParser = new TreeParser(newick, false, false, false, 0);

        Node [] node = treeParser.getNodesAsArray();
        assertEquals(0.0, node[0].getHeight(), EPSILON);
        assertEquals(0.0, node[1].getHeight(), EPSILON);
        // leaf node, not scaled
        assertEquals(0.5, node[2].getHeight(), EPSILON);
        assertEquals(0.5, node[3].getHeight(), EPSILON);
        // internal nodes, all scaled
        assertEquals(1.0, node[4].getHeight(), EPSILON);
        assertEquals(1.5, node[5].getHeight(), EPSILON);
        assertEquals(2.0, node[6].getHeight(), EPSILON);
        
        treeParser.scale(2.0);
        
        // leaf node
        node = treeParser.getNodesAsArray();
        assertEquals(0.0, node[0].getHeight(), EPSILON);
        assertEquals(0.0, node[1].getHeight(), EPSILON);
        // leaf node, not scaled
        assertEquals(0.5, node[2].getHeight(), EPSILON);
        assertEquals(0.5, node[3].getHeight(), EPSILON);
        // internal nodes, all scaled
        assertEquals(2.0, node[4].getHeight(), EPSILON);
        assertEquals(3.0, node[5].getHeight(), EPSILON);
        assertEquals(4.0, node[6].getHeight(), EPSILON);
		
	}
}
