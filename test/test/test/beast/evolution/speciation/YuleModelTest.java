package test.beast.evolution.speciation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import beast.base.evolution.speciation.YuleModel;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.parameter.RealParameter;

/**
 * Created by Denise on 16.11.16.
 */
public class YuleModelTest  {

	  @Test
	    public void testYule() throws Exception {

	        YuleModel bdssm =  new YuleModel();

	        Tree tree1 = new TreeParser("((A:1.0,B:1.0):1.0,C:2.0);",false);
	        bdssm.setInputValue("tree", tree1);

	        bdssm.setInputValue("birthDiffRate", new RealParameter("10."));
	        bdssm.setInputValue("originHeight", new RealParameter("10."));

	        bdssm.initAndValidate();

	        double logP1 = bdssm.calculateTreeLogLikelihood(tree1);
	        
	        double logP1alt = calcAltYuleLogP(tree1, 10, 10);
	        assertEquals(0.0, logP1-logP1alt);
	        


	        Tree tree = new TreeParser("((A:1.0,B:1.0):2.0,C:3.0);",false);
	        bdssm.setInputValue("tree", tree);

	        bdssm.setInputValue("birthDiffRate", new RealParameter("10."));
	        bdssm.setInputValue("originHeight", new RealParameter("10."));


	        bdssm.initAndValidate();

	        double logP2 = bdssm.calculateTreeLogLikelihood(tree);

	        assertEquals(logP1-logP2,10.0);

	    }

	    // double p = 0.0;
	    // p = lambda^(n-1) * exp(-lambda*rootHeight);
	    // for (int i = 1; i < n; i++) {
	    //         p *= exp(-lambda*height[i])
	    // }
	    // p = lambda^(n-1) * exp(-lambda*rootHeight) * \prod_i=n^2n-1 exp(-lambda*nodeheight[i])
		private double calcAltYuleLogP(Tree tree, double lambda, double origin) {
			int n = tree.getLeafNodeCount();
			// conditioning on leafs only
			// double logP = (n - 1) * Math.log(lambda) - lambda * tree.getRoot().getHeight();
			
			// conditioning on leafs and origin
			double logP = (n-1) * Math.log(lambda * (1 + 1 / (Math.exp(lambda * origin) - 1)));
			// p =  (lambda * (1 + 1 / (exp(lambda * origin) - 1))) ^ (n-1) * exp(-lambda * L);

			// conditioning on leafs and root
			//double logP = (n-1) * Math.log(lambda * (1 + 1 / (Math.exp(lambda * tree.getRoot().getHeight()) - 1)));

			for (int i = 0; i < tree.getInternalNodeCount(); i++) {
				logP -= lambda * tree.getNode(n+i).getHeight();
			}
			return logP;
		}

}
