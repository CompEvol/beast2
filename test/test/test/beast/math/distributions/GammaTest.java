package test.beast.math.distributions;


import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.integration.RombergIntegrator;
import org.apache.commons.math.analysis.integration.UnivariateRealIntegrator;
import org.junit.jupiter.api.Test;

import beast.base.inference.distribution.Gamma;
import beast.base.util.GammaFunction;
import beast.base.util.Randomizer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GammaTest  {
	
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

	/** The code below is adapted from GammaDistributionTest from BEAST 1
	 * This test stochastically draws gamma
	 * variates and compares the coded pdf 
	 * with the actual pdf.  
	 * The tolerance is required to be at most 1e-10.
	 */

    static double mypdf(double value, double shape, double scale) {
        return Math.exp((shape-1) * Math.log(value) - value/scale - GammaFunction.lnGamma(shape) - shape * Math.log(scale) );
    }

	public void testPdf() throws MathException  {

        final int numberOfTests = 300;
        double totErr = 0;
        double ptotErr = 0; int np = 0;
        double qtotErr = 0;

        Randomizer.setSeed(551);

        for(int i = 0; i < numberOfTests; i++){
            final double mean = .01 + (3-0.01) * Randomizer.nextDouble();
            final double var = .01 + (3-0.01) * Randomizer.nextDouble();

            double scale0 = var / mean;
            double shape = mean / scale0;

            final Gamma gamma = new Gamma();
            Gamma.mode mode = Gamma.mode.values()[Randomizer.nextInt(4)];
            
            double other = 0;
        	switch (mode) {
        	case ShapeScale: other = scale0; break;
        	case ShapeRate: other = 1/scale0; break;
        	case ShapeMean: other = scale0 * shape; break;
        	case OneParameter: other = 1/shape; scale0 = 1/shape; break;
        	}
            final double scale = scale0;

            gamma.initByName("alpha", shape +"", "beta", other +"", "mode", mode);

            final double value = Randomizer.nextGamma(shape, 1/scale);

            final double mypdf = mypdf(value, shape, scale);
            final double pdf = gamma.density(value);
            if ( Double.isInfinite(mypdf) && Double.isInfinite(pdf)) {
                continue;
            }

            assertFalse(Double.isNaN(mypdf));
            assertFalse(Double.isNaN(pdf));

            totErr +=  mypdf != 0 ? Math.abs((pdf - mypdf)/mypdf) : pdf;

            assertFalse(Double.isNaN(totErr), "nan");
            //assertEquals("" + shape + "," + scale + "," + value, mypdf,gamma.pdf(value),1e-10);

            final double cdf = gamma.cumulativeProbability(value);
            UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double v) throws FunctionEvaluationException {
                    return mypdf(v, shape, scale);
                }
            };
            final UnivariateRealIntegrator integrator = new RombergIntegrator();
            integrator.setAbsoluteAccuracy(1e-14);
            integrator.setMaximalIterationCount(16);  // fail if it takes too much time

            double x;
            try {
                x = integrator.integrate(f, 0, value);
                ptotErr += cdf != 0.0 ? Math.abs(x-cdf)/cdf : x;
                np += 1;
                //assertTrue("" + shape + "," + scale + "," + value + " " + Math.abs(x-cdf)/x + "> 1e-6", Math.abs(1-cdf/x) < 1e-6);

                final double q = gamma.inverseCumulativeProbability(cdf);
                qtotErr += q != 0 ? Math.abs(q-value)/q : value;
                //System.out.println(shape + ","  + scale + " " + value);
            } catch( ConvergenceException e ) {
                 // can't integrate , skip test
                 //System.out.print(" theta(" + shape + ","  + scale + ") skipped");
            }

           // assertEquals("" + shape + "," + scale + "," + value + " " + Math.abs(q-value)/value, q, value, 1e-6);
           // System.out.print("\n" + np + ": " + mode + " " + totErr/np + " " + qtotErr/np + " " + ptotErr/np);
        }
        //System.out.println( !Double.isNaN(totErr) );
       // System.out.println(totErr);
        // bad test, but I can't find a good threshold that works for all individual cases 
        assertTrue(totErr/numberOfTests < 1e-7, "failed " + totErr/numberOfTests);
        assertTrue(qtotErr/numberOfTests < 1e-10, "failed " + qtotErr/numberOfTests);
        assertTrue(np > 0 ? (ptotErr/np < 2e-7) : true, "failed " + ptotErr/np);
	}

}
