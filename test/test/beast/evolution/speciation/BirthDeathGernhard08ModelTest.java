package test.beast.evolution.speciation;

import org.junit.jupiter.api.Test;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.speciation.BirthDeathGernhard08Model;
import beast.base.evolution.tree.Tree;
import beast.base.inference.parameter.RealParameter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import test.beast.BEASTTestCase;

public class BirthDeathGernhard08ModelTest  {


    @Test
    public void testJC69Likelihood() throws Exception {
        // Set up JC69 model: uniform freqs, kappa = 1, 0 gamma categories
        Alignment data = BEASTTestCase.getAlignment();
        Tree tree = BEASTTestCase.getTree(data);

        RealParameter birthDiffRate = new RealParameter("1.0");
        RealParameter relativeDeathRate = new RealParameter("0.5");
        RealParameter originHeight = new RealParameter("0.1");
        BirthDeathGernhard08Model likelihood = new BirthDeathGernhard08Model();
        likelihood.initByName("type", "unscaled",
                "tree", tree,
                "birthDiffRate", birthDiffRate,
                "relativeDeathRate", relativeDeathRate);


        double logP = 0;
        logP = likelihood.calculateLogP(); // -3.520936119641363
        assertEquals(logP, 2.5878899503981287, BEASTTestCase.PRECISION);

        likelihood.initByName("type", "timesonly",
                "tree", tree,
                "birthDiffRate", birthDiffRate,
                "relativeDeathRate", relativeDeathRate);
        logP = likelihood.calculateLogP();
        assertEquals(logP, 9.16714116240823, BEASTTestCase.PRECISION);

        likelihood.initByName("type", "oriented",
                "tree", tree,
                "birthDiffRate", birthDiffRate,
                "relativeDeathRate", relativeDeathRate);
        logP = likelihood.calculateLogP();
        assertEquals(logP, 4.379649419626184, BEASTTestCase.PRECISION);

        likelihood.initByName("type", "labeled",
                "tree", tree,
                "birthDiffRate", birthDiffRate,
                "relativeDeathRate", relativeDeathRate);
        logP = likelihood.calculateLogP();
        assertEquals(logP, 1.2661341104158121, BEASTTestCase.PRECISION);
        
        likelihood.initByName("type", "labeled",
        		"tree", tree,
        		"birthDiffRate", birthDiffRate,
        		"relativeDeathRate", relativeDeathRate,
        		"originHeight", originHeight);
        logP = likelihood.calculateLogP();
        assertEquals(logP, 8.41413452832378, BEASTTestCase.PRECISION);
    }

}
