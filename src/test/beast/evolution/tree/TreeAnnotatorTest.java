package test.beast.evolution.tree;

import beast.app.treeannotator.CladeSystem;
import beast.app.treeannotator.TreeAnnotator;
import beast.evolution.tree.Tree;
import beast.util.TreeParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.BitSet;
import java.util.Map;

/**
 * @author Walter Xie
 */
public class TreeAnnotatorTest {
    protected TreeAnnotator treeAnnotator;
    protected Tree[] trees;
    protected CladeSystem cladeSystem;

    protected String[] clades = new String[]{"{0, 1}", "{1, 2}", "{0, 1, 2}", "{0, 1, 2, 3}", "{2, 3}"};
    protected int[] cladesCount = new int[]{2, 1, 2, 3, 1};
    @Before
    public void setUp() throws Exception {
        final String[] treesString = new String[]{"((A:1,B:1):1,(C:1,D:1):1);",
                "(((A:1,B:1):1,C:2):2,D:3);", "((A:2,(B:1,C:1):1):2,D:3);"};

        treeAnnotator = new TreeAnnotator();
        trees = new Tree[treesString.length];
        for (int i = 0; i < trees.length; i++) {
            trees[i] = new TreeParser(treesString[i], false, false, true, 1);
        }

        cladeSystem = new CladeSystem();
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
            Assert.assertEquals(clades[i], entry.getKey().toString());
            Assert.assertEquals(cladesCount[i], entry.getValue().getCount());
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
}
