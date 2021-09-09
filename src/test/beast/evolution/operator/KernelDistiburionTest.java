package test.beast.evolution.operator;

import org.junit.Test;

import beast.base.Log;
import beast.inference.operator.kernel.KernelDistribution;
import beast.inference.operator.kernel.KernelDistribution.Bactrian.mode;
import beast.util.Randomizer;
import junit.framework.TestCase;

public class KernelDistiburionTest extends TestCase {
	
	@Test
	public void testBactrianKernelDistribution() {
		Randomizer.setSeed(127);

		// testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.cauchy));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.t4));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_t4));

		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_strawhat, 0.2), false);
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_strawhat, 0.5), false);
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_airplane, 0.2), false);
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_airplane, 0.5), false);
		
		
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.normal));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.uniform));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.laplace));
		
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.95, mode.bactrian_normal));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.90, mode.bactrian_normal));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.98, mode.bactrian_normal));

		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.98, mode.bactrian_uniform));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.90, mode.bactrian_uniform));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.95, mode.bactrian_uniform));
		
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.95, mode.bactrian_laplace));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.90, mode.bactrian_laplace));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.98, mode.bactrian_laplace));

		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.95, mode.bactrian_triangle));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.90, mode.bactrian_triangle));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.98, mode.bactrian_triangle));

		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_box, 0.2));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_box, 0.5));

	}

	private void testMeanIsZeroSigmaIsOne(KernelDistribution.Bactrian distr) {
		testMeanIsZeroSigmaIsOne(distr, true);
	}
	private void testMeanIsZeroSigmaIsOne(KernelDistribution.Bactrian distr, boolean hasSigma1) {
		int N = 1000000;
		double m = 0; 
		double s = 0;
		for (int i = 0; i < N; i++) {
			double x = distr.getRandomDelta(0, Double.NaN, 1);
			s = s + x*x;
			m += x;
		}
		m /= N;
		s /=  N;
		// mean
		assertEquals(0.0, m, 1e-2);
		// variance
		if (hasSigma1) {
			assertEquals(1.0, s, 1e-2);
		}
		Log.warning("Testing " + distr.kernelmode + " s= " + s);
	}

	@Test
	public void testMirrorfKernelDistribution() {
		Log.warning("Testing mirror distribution");
		Randomizer.setSeed(125);
		
		KernelDistribution.Mirror distr = new KernelDistribution.Mirror(mode.bactrian_normal);

		int N = 10000;
		double m = 0; 
		double s = 0;
		double x = 0, delta = 0;
		for (int i = 0; i < N; i++) {
			double n = i + 1;
			delta = distr.getRandomDelta(0, x, 1);
			if (Math.abs(x + delta) < 10) {
				// x ~ uniform [-10,10]
				x += delta;				
			}
			s = s + x*x;
			m = x/n + m * (n-1)/n;
		}
		m /= N;
		s /=  N;
		// mean
		assertEquals(0.0, m, 1e-2);
		// variance
		assertEquals((10.0- -10.0)*(10.0- -10.0)/12.0, s, 1e-1);
	}

	@Test
	public void testMultiDimensionalMirrorfKernelDistribution() {
		Log.warning("Testing mirror distribution");
		Randomizer.setSeed(123);
		
		KernelDistribution.Mirror distr = new KernelDistribution.Mirror();
		distr.initByName("mode", mode.bactrian_normal, "onePerDimension", true);

		int N = 10000;
		double []m = new double[3]; 
		double []s = new double[3];
		double []x = new double[3];
		double []delta = new double[3];
		int []callcount = new int[3];
		for (int i = 0; i < N; i++) {
			int dim = Randomizer.nextInt(3);
			callcount[dim]++;
			double n = callcount[dim];
			delta[dim] = distr.getRandomDelta(dim, x[dim], 1);
			if (Math.abs(x[dim] + delta[dim]) < 10) {
				// x ~ uniform [-10,10]
				x[dim] += delta[dim];
			}
			s[dim] = s[dim] + x[dim]*x[dim];
			m[dim] = x[dim]/n + m[dim] * (n-1)/n;
		}
		for (int dim = 0; dim < 3; dim++) {
			m[dim] /= callcount[dim];
			s[dim] /= callcount[dim];
			// mean
			assertEquals(0.0, m[dim], 1e-2);
			// variance
			assertEquals((10.0- -10.0)*(10.0- -10.0)/12.0, s[dim], 2.5);
		}
	}

	@Test
	public void testMultiDimensionalMirrorfKernelDistributionScaler() {
		Log.warning("Testing mirror distribution for scaling");
		Randomizer.setSeed(123);
		
		KernelDistribution.Mirror distr = new KernelDistribution.Mirror();
		distr.initByName("mode", mode.bactrian_normal, "onePerDimension", true);

		int N = 10000;
		double B = 1.0;
		double []m = new double[3]; 
		double []s = new double[3];
		double []x = new double[3];
		x[0] = 1;x[1] = 1;x[2] = 1;
		double []delta = new double[3];
		int []callcount = new int[3];
		
		for (int i = 0; i < N; i++) {
			int dim = Randomizer.nextInt(3);
			callcount[dim]++;
			double n = callcount[dim];
			delta[dim] = distr.getScaler(dim, x[dim], 1);
			if (Math.abs(Math.log(x[dim] * delta[dim])) < B) {
				// x ~ uniform [-B,B]
				x[dim] *= delta[dim];
			}
			double y = Math.log(x[dim]);
			s[dim] = s[dim] + y;
			m[dim] = y/n + m[dim] * (n-1)/n;
//			if (i % 100 == 0) {
//				System.out.println(i + "\t" + x[0] + "\t" + x[1] + "\t" + x[2]);
//			}
		}
		for (int dim = 0; dim < 3; dim++) {
			m[dim] /= callcount[dim];
			s[dim] /= callcount[dim];
			// mean
			assertEquals(0.0, m[dim], 1e-2);
			// variance
			assertEquals((B- -B)*(B- -B)/12.0, s[dim], 0.3);
		}
	}
}
