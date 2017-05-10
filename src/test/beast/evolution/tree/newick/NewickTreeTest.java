package test.beast.evolution.tree.newick;

import beast.evolution.tree.Tree;
import beast.util.TreeParser;
import junit.framework.TestCase;

/**
 * @author Walter Xie
 */
public class NewickTreeTest extends TestCase {
    String[] trees1 = new String[]{
            "((A:1.0,B:1.0):1.0,C:2.0);",
            "((1:1.0,2:1.0):1.0,3:2.0);"
    }; //more trees ?
    String[] trees2 = new String[]{
            "((A:1.5,B:0.5):1.1,C:3.0):0.0;",
            "((2:1.5,1:0.5):1.1,3:3.0):0.0;"
    }; //more trees ?


    public void testLabeledNewickTrees() throws Exception {

        for (String tree : trees1) {

            TreeParser newickTree = new TreeParser(tree, false, false, true, 1);

//            System.out.println(tree);
//            System.out.println(newickTree);

            assertEquals("((0:1.0,1:1.0)3:1.0,2:2.0)4:0.0", newickTree.toString());
        }

        for (String tree : trees2) {
            TreeParser newickTree = new TreeParser(tree, false, false, true, 1);
            String newick = newickTree.getRoot().toNewick();
            assertEquals(tree, newick+";");
        }

    }

    public void testMultifurcatingTree() {
        String treeString = "(A:0.1,B:0.2,(C:0.3,D:0.4):0.5);";

        Tree multifurcatingTree = new Tree(treeString, false);
        String newick = multifurcatingTree.getRoot().toNewick();
        assertEquals(treeString, newick+";");
    }
}
