package test.beast.math.distributions;

import org.apache.commons.math.MathException;

import beast.math.distributions.Normal;
import beast.util.Randomizer;
import junit.framework.TestCase;

/**
 * adapted from BEAST 1
 * @author Wai Lok Sibon Li
 * 
 */
public class NormalDistributionTest extends TestCase {
    Normal norm;

    public void setUp() {

        norm = new Normal();
        norm.initAndValidate();
        Randomizer.setSeed(123);
    }


    public void testPdf() {

        System.out.println("Testing 10000 random pdf calls");

        for (int i = 0; i < 10000; i++) {
            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            double x = Randomizer.nextDouble() * 10;

            norm.meanInput.setValue(M + "", norm);
            norm.sigmaInput.setValue(S + "", norm);
            norm.initAndValidate();
            
            double a = 1.0 / (Math.sqrt(2.0 * Math.PI) * S);
            double b = -(x - M) * (x - M) / (2.0 * S * S);
            double pdf =  a * Math.exp(b);

            assertEquals(pdf, norm.density(x), 1e-10);
        }

        /* Test with an example using R */
        norm.meanInput.setValue(2.835202292812448 + "", norm);
        norm.sigmaInput.setValue(3.539139491639669 + "", norm);
        assertEquals(0.1123318, norm.density(2.540111), 1e-6);
    }

    public void testMean() {

        for (int i = 0; i < 1000; i++) {
            double M = Randomizer.nextDouble() * 10.0 - 5.0;

            norm.meanInput.setValue(M + "", norm);
            norm.initAndValidate();

            assertEquals(M, norm.getMean(), 1e-10);
        }
    }

//    public void testVariance() {
//
//        for (int i = 0; i < 1000; i++) {
//            double S = Randomizer.nextDouble() * 10;
//            norm.sigmaInput.setValue(S + "", norm);
//            norm.initAndValidate();
//
//            double variance = S * S;
//
//            assertEquals(variance, norm.variance(), 1e-10);
//        }
//    }


    public void testMedian() throws MathException {

        System.out.println("Testing 10000 random quantile(0.5) calls");

        for (int i = 0; i < 10000; i++) {
            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            norm.meanInput.setValue(M + "", norm);
            norm.sigmaInput.setValue(S + "", norm);
            norm.initAndValidate();

            double median = M;

            assertEquals(median, norm.inverseCumulativeProbability(0.5), 1e-6);
        }
    }

    public void testCDFAndQuantile() throws MathException {

        System.out.println("Testing 10000 random quantile/cdf pairs");

        for (int i = 0; i < 10000; i++) {

            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            norm.meanInput.setValue(M + "", norm);
            norm.sigmaInput.setValue(S + "", norm);
            norm.initAndValidate();

            double p = Randomizer.nextDouble();
            double quantile = norm.inverseCumulativeProbability(p);
            double cdf = norm.cumulativeProbability(quantile);

            assertEquals(p, cdf, 1e-7);
        }

    }

//    public void testCDFAndQuantile2() {
//        for(int i=0; i<10000; i++) {
//            double x =Randomizer.nextDouble();
//            double m = Randomizer.nextDouble() * 10;
//            double s = Randomizer.nextDouble() * 10;
//            
//            double a = NormalDistribution.cdf(x, m, s, false);
//            double b =NormalDistribution.cdf(x, m, s);
//            
//            assertEquals(a, b, 1.0e-8);
//        }
//    }


}
