package test.beast.math.distributions;

import org.junit.Test;

import beast.math.distributions.Gamma;
import junit.framework.TestCase;

public class GammaTest extends TestCase {
	
	@Test
	public void testGammaCummulative() throws Exception {
		Gamma dist = new Gamma();
		dist.initByName("alpha", "0.001", "beta", "1000.0");
		double v = dist.inverseCumulativeProbability(0.5);
		assertEquals(v, 5.244206e-299, 1e-304);
		v = dist.inverseCumulativeProbability(0.05);
		assertEquals(v, 0.0);
		v = dist.inverseCumulativeProbability(0.025);
		assertEquals(v, 0.0);
		
		v = dist.inverseCumulativeProbability(0.95);
		assertEquals(v, 2.973588e-20, 1e-24);
		v = dist.inverseCumulativeProbability(0.975);
		assertEquals(v, 5.679252e-09, 1e-13);
		
	}

}
