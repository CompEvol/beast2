package test.beast.evolution.tree;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.BitSet;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import beast.app.inputeditor.BeautiDoc;
import beast.app.treeannotator.CladeSystem;
import beast.app.treeannotator.TreeAnnotator;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeParser;

/**
 * @author Walter Xie
 */
public class TreeAnnotatorTest {
    protected TreeAnnotator treeAnnotator;
    protected TreeAnnotator treeAnnotatorSA;
    protected Tree[] trees;
    protected Tree[] treesSA;
    protected CladeSystem cladeSystem;
    protected CladeSystem cladeSystemSA;

    //protected String[] clades = new String[]{"{0, 1}", "{1, 2}", "{0, 1, 2}", "{0, 1, 2, 3}", "{2, 3}"};
    protected String[] clades = new String[]{"{0, 2}", "{2, 4}", "{0, 2, 4}", "{0, 2, 4, 6}", "{4, 6}"};
    protected String[] cladesSA = new String[]{"{0, 2}", "{0, 2, 4}", "{0, 2, 4, 6, 7}", "{0, 2, 4, 6, 8}", "{0, 4}", "{6, 8}", "{0, 2, 4, 8}", "{0, 2, 4, 6, 7, 8}"};
    protected int[] cladesCount = new int[]{2, 1, 2, 3, 1};
    protected int[] cladesCountSA = new int[]{1, 4, 2, 3, 3, 1, 1, 1};
    protected double[] logTreeScoresSA = new double[] {-2.367124, -1.268511, -1.961659, -3.060271}; //scores calculated in R
    protected double[] treeScoresSA = new double[] {2.5, 3.0, 2.27, 2.25}; //scores calculated in R

    @Before
    public void setUp() throws Exception {
        final String[] treesString = new String[]{"((A:1,B:1):1,(C:1,D:1):1);",
                "(((A:1,B:1):1,C:2):2,D:3);", "((A:2,(B:1,C:1):1):2,D:3);"};
        final String[] treesSAString = new String[]{"((((0:0.5,1:1.0):1.0,2:2.0):1.0,3:0.0):2.0,4:4.0);",
                "((((0:1.0,2:1.5):1.0,1:2.5):0.5,3:0.0):2.0,4:4.0);", "(((0:0.5,2:1.0):1.0,1:2.0):3.0,(3:0.2,4:2.2):1.8);", "((((0:1.0,2:1.5):1.0,1:2.5):0.2,4:1.7):0.3,3:0.0):0.0;"};

        treeAnnotator = new TreeAnnotator();
        treeAnnotatorSA = new TreeAnnotator();
        trees = new Tree[treesString.length];
        for (int i = 0; i < trees.length; i++) {
            trees[i] = new TreeParser(treesString[i], false, false, true, 1);
        }
        treesSA = new Tree[treesSAString.length];
        for (int i = 0; i < treesSA.length; i++) {
            treesSA[i] = new TreeParser(treesSAString[i], false, false, false, 0);
        }

        cladeSystem = new CladeSystem();
        cladeSystemSA = new CladeSystem();
    }

    @Test
    public void testTreeScoreAndCladeSystem() throws Exception {
        for (Tree tree : trees) {
            cladeSystem.add(tree, false);
        }
        Assert.assertEquals(clades.length, cladeSystem.getCladeMap().size());

        cladeSystem.calculateCladeCredibilities(trees.length);

        int i=0;
        for (Map.Entry<BitSet, CladeSystem.Clade> entry : cladeSystem.getCladeMap().entrySet()) {
//            System.out.println(entry.getKey() + " = " + entry.getValue().getCount());
            int index = -1;
            //find the clade in the clades array
            for (int j=0; j<clades.length; j++) {
                if (clades[j].equals(entry.getKey().toString())) {
                    index = j;
                    break;
                }
            }
            //if the clade is not found then index = -1
            Assert.assertFalse(index ==  -1);
            Assert.assertEquals(cladesCount[index], entry.getValue().getCount());
            i++;
        }

        int maxScoreIndex = -1;
        int maxScoreLogIndex = -1;
        double maxScore = Double.NEGATIVE_INFINITY;
        double maxScoreLog = Double.NEGATIVE_INFINITY;
        i = 0;
        for (Tree tree : trees) {
            double score = treeAnnotator.scoreTree(tree, cladeSystem, true);
            double scoreLog = treeAnnotator.scoreTree(tree, cladeSystem, false);

//            System.out.println(i + " => " + score + ", log " + scoreLog);
            if (maxScore < score) {
                maxScore = score;
                maxScoreIndex = i;
            }
            if (maxScoreLog < scoreLog) {
                maxScoreLog = scoreLog;
                maxScoreLogIndex = i;
            }
            i++;
        }
//        System.out.println(maxScoreIndex + " => " + maxScore + ", log " + maxScoreLog);
        Assert.assertEquals(1, maxScoreIndex);
        Assert.assertEquals(1, maxScoreLogIndex);
    }

    @Test
    public void testTreeScoreAndCladeSystemSA() throws Exception {
        for (Tree tree : treesSA) {
            cladeSystemSA.add(tree, false);
        }
        Assert.assertEquals(cladesSA.length, cladeSystemSA.getCladeMap().size());

        cladeSystemSA.calculateCladeCredibilities(treesSA.length);

        int i=0;
        for (Map.Entry<BitSet, CladeSystem.Clade> entry : cladeSystemSA.getCladeMap().entrySet()) {
//            System.out.println(entry.getKey() + " = " + entry.getValue().getCount());
            int index = -1;
            //find the clade in the clades array
            for (int j=0; j<cladesSA.length; j++) {
                if (cladesSA[j].equals(entry.getKey().toString())) {
                    index = j;
                    break;
                }
            }
            //if the clade is not found then index = -1
            Assert.assertFalse(i == -1);
            Assert.assertEquals(cladesCountSA[index], entry.getValue().getCount());
            i++;
        }

        int maxScoreIndex = -1;
        int maxScoreLogIndex = -1;
        double maxScore = Double.NEGATIVE_INFINITY;
        double maxScoreLog = Double.NEGATIVE_INFINITY;
        i = 0;
        for (Tree tree : treesSA) {
            double score = treeAnnotatorSA.scoreTree(tree, cladeSystemSA, true);
            double scoreLog = treeAnnotatorSA.scoreTree(tree, cladeSystemSA, false);

//            Assert.assertEquals(logTreeScoresSA[i], scoreLog, 1e-6);

            System.out.println(i + " => " + score + ", log " + scoreLog);
            if (maxScore < score) {
                maxScore = score;
                maxScoreIndex = i;
            }
            if (maxScoreLog < scoreLog) {
                maxScoreLog = scoreLog;
                maxScoreLogIndex = i;
            }
            i++;
        }
//        System.out.println(maxScoreIndex + " => " + maxScore + ", log " + maxScoreLog);
        Assert.assertEquals(1, maxScoreIndex);
        Assert.assertEquals(1, maxScoreLogIndex);
    }
    
    @Test
    public void testNewickTargetTree() throws Exception {
    	// create target tree file in temp folder
    	TreeParser tree = new TreeParser("((A,B),(C,D))");
    	String tmpFolder = org.fest.util.Files.temporaryFolder().toString();
    	File target = new File(tmpFolder + "/target.tree");
        PrintStream outfile = new PrintStream(target);
        tree.init(outfile);
        outfile.println();
        tree.log(0, outfile);
        outfile.println();
        tree.close(outfile);
        outfile.close();

        // create input tree set
    	File source = new File(tmpFolder + "/source.trees");
        outfile = new PrintStream(source);
        for (String treeString : new String[]{
        		"((A:1,B:1):1,(C:1,D:1):1);",
                "(((A:1,B:1):1,C:2):2,D:3);", 
                "((A:2,(B:1,C:1):1):2,D:3);"}) {
        	outfile.println(treeString);
        };
        outfile.close();
    	
    	File summary = new File(tmpFolder + "/summary.tree");
    	
    	// run tree annotator
    	String [] args = new String[]{"-target", target.getPath(), source.getPath(), summary.getPath()};
        TreeAnnotator.main(args);
        
        // make sure we get output
        String summaryString = BeautiDoc.load(summary);
        System.out.println(summaryString);
    }
    
}
