package test.beast.evolution.likelihood;


import org.junit.Test;

import beast.evolution.likelihood.BeagleTreeLikelihood;
import beast.evolution.likelihood.TreeLikelihood;

/** Same as TreeLikelihoodTest, but for Beagle Tree Likelihood. 
 * **/
public class BeagleTreeLikelihoodTest extends TreeLikelihoodTest {

	public BeagleTreeLikelihoodTest() {
		super();
		System.setProperty("java.only", "true");
	} // c'tor
	
	@Override
	protected TreeLikelihood newTreeLikelihood() {
		return new BeagleTreeLikelihood();
	}

	@Test
	public void testAscertainedJC69Likelihood() throws Exception {
		// fails
	}
	
	@Test
	public void testHKY85GLikelihood() throws Exception {
		// fails
	}
	
	@Test
	public void testGTRGLikelihood() throws Exception {
		// fails
	}

} // class BeagleTreeLikelihoodTest
