package test.beast.core;

import org.junit.Test;

import beast.evolution.alignment.Alignment;
import beast.evolution.tree.Tree;
import beast.util.ClusterTree;
import beast.util.TreeParser;

import test.beast.BEASTTestCase;

import junit.framework.TestCase;

public class StateNodeInitialiserTest extends TestCase {

	@Test
	public void testClusterTree() throws Exception {
		Alignment data = BEASTTestCase.getAlignment();
		Tree tree = new Tree();
		tree.initAndValidate();
		assertEquals(true, tree.getRoot() == null);
		
        TreeParser tree2 = new TreeParser();
        tree2.initByName(
        		"initial", tree,
        		"taxa", data,
                "newick", "((((human:0.024003,(chimp:0.010772,bonobo:0.010772):0.013231):0.012035,gorilla:0.036038):0.033087000000000005,orangutan:0.069125):0.030456999999999998,siamang:0.099582);");
        
		assertEquals(true, tree.getRoot() != null);
		assertEquals(11, tree.getNodeCount());
    }

	@Test
	public void testNewickTree() throws Exception {
		Alignment data = BEASTTestCase.getAlignment();
		Tree tree = new Tree();
		tree.initAndValidate();
		assertEquals(true, tree.getRoot() == null);
		
        ClusterTree tree2 = new ClusterTree();
        tree2.initByName(
        		"initial", tree,
        		"clusterType","upgma",
        		"taxa", data);        
		assertEquals(true, tree.getRoot() != null);
		assertEquals(11, tree.getNodeCount());
	}
}
