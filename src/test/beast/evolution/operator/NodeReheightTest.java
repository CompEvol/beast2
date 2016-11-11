package test.beast.evolution.operator;

import org.junit.Test;

import beast.core.State;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.operators.NodeReheight;
import beast.evolution.tree.Tree;
import beast.util.TreeParser;
import junit.framework.TestCase;

public class NodeReheightTest extends TestCase {
	@Test
	public void testNodeReheight() throws Exception {
		TaxonSet taxonset = new TaxonSet();
		taxonset.initByName("taxon", new Taxon("t1"),
				"taxon", new Taxon("t2"),
				"taxon", new Taxon("t3"),
				"taxon", new Taxon("t4"),
				"taxon", new Taxon("t5"),
				"taxon", new Taxon("t6")
				);
        Tree tree = new TreeParser();
        tree.initByName("taxonset", taxonset,
                "newick", "((((human:0.014003,(chimp:0.010772,bonobo:0.010772):0.013231):0.012035,gorilla:0.036038):0.033087000000000005,orangutan:0.069125):0.030456999999999998,siamang:0.099582);",
                "IsLabelledNewick", true,
                "adjustTipHeights", false);
        State state = new State();
        state.initByName("stateNode", tree);
        
        NodeReheight operator = new NodeReheight();
        operator.initByName("weight", 1.0, "tree", tree);


	}
}