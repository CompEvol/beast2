package test.beast.evolution.speciation;

import junit.framework.TestCase;

import org.junit.Test;

import beast.base.evolution.speciation.YuleModel;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.parameter.RealParameter;

/**
 * Created by Denise on 16.11.16.
 */
public class YuleModelTest extends TestCase {

    @Test
    public void testYule() throws Exception {

        YuleModel bdssm =  new YuleModel();

        Tree tree1 = new TreeParser("((A:1.0,B:1.0):1.0,C:2.0);",false);
        bdssm.setInputValue("tree", tree1);

        bdssm.setInputValue("birthDiffRate", new RealParameter("10."));
        bdssm.setInputValue("originHeight", new RealParameter("10."));

        bdssm.initAndValidate();

        double logP1 = bdssm.calculateTreeLogLikelihood(tree1);

        Tree tree = new TreeParser("((A:1.0,B:1.0):2.0,C:3.0);",false);
        bdssm.setInputValue("tree", tree);

        bdssm.setInputValue("birthDiffRate", new RealParameter("10."));
        bdssm.setInputValue("originHeight", new RealParameter("10."));


        bdssm.initAndValidate();

        double logP2 = bdssm.calculateTreeLogLikelihood(tree);

        assertEquals(logP1-logP2,10.0);

    }

}
