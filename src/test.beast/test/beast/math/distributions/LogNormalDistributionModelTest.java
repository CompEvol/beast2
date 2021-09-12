package test.beast.math.distributions;


import org.apache.commons.math.MathException;
import org.junit.Test;

import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.parameter.RealParameter;
import beast.base.parser.XMLParser;
import beast.base.util.Randomizer;
import junit.framework.TestCase;

public class LogNormalDistributionModelTest extends TestCase {

    @Test
    public void testPDF() throws Exception {
        System.out.println("Testing 10000 random pdf calls");
        LogNormalDistributionModel logNormal = new LogNormalDistributionModel();
        logNormal.init("1.0", "2.0");

        for (int i = 0; i < 10000; i++) {
            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            double x = -1;
            while( x < 0 ) {
                x = Math.log(Randomizer.nextDouble() * 10);
            }

            logNormal.MParameterInput.setValue(M + "", logNormal);
            logNormal.SParameterInput.setValue(S + "", logNormal);
            logNormal.initAndValidate();

            double pdf = 1.0 / (x * S * Math.sqrt(2 * Math.PI)) * Math.exp(-Math.pow(Math.log(x) - M, 2) / (2 * S * S));

            System.out.println("Testing logNormal[M=" + M + " S=" + S + "].pdf(" + x + ")");
            double f = logNormal.density(x);

            assertEquals(pdf, f, 1e-10);
        }
    }


    @Test
    public void testCalcLogP() throws Exception {
        LogNormalDistributionModel logNormal = new LogNormalDistributionModel();
        logNormal.hasMeanInRealSpaceInput.setValue("true", logNormal);
        logNormal.offsetInput.setValue("1200", logNormal);
        logNormal.MParameterInput.setValue("2000", logNormal);
        logNormal.SParameterInput.setValue("0.6", logNormal);
        logNormal.initAndValidate();
        RealParameter p = new RealParameter(new Double[]{2952.6747000000014});

        double f0 = logNormal.calcLogP(p);
        assertEquals(-7.880210654973873, f0, 1e-10);
    }

    @Test
    public void testCalcLogP2() throws Exception {
        // does the same as testCalcLogP(), but with by constructing object through XML
        String xml = "<input spec='beast.math.distributions.LogNormalDistributionModel' " +
                "offset='1200' " +
                "M='2000' " +
                "S='0.6' " +
                "meanInRealSpace='true'/>";
        RealParameter p = new RealParameter(new Double[]{2952.6747000000014});
        XMLParser parser = new XMLParser();
        LogNormalDistributionModel logNormal = (LogNormalDistributionModel) parser.parseBareFragment(xml, true);

        double f0 = logNormal.calcLogP(p);
        assertEquals(-7.880210654973873, f0, 1e-10);
    }

    @Test
    public void testCalcLogP3() throws Exception {
        // does the same as testCalcLogP(), but with by constructing object through init
        LogNormalDistributionModel logNormal = new LogNormalDistributionModel();
        logNormal.init("2000", "0.6", true, "1200");
        RealParameter p = new RealParameter(new Double[]{2952.6747000000014});

        double f0 = logNormal.calcLogP(p);
        assertEquals(-7.880210654973873, f0, 1e-10);
    }


    
    // remainder is adapted from Alexei's LogNormalDistributionTest from BEAST 1
    LogNormalDistributionModel logNormal;

    public void setUp() {

        logNormal = new LogNormalDistributionModel();
        logNormal.initByName("M", "1.0", "S", "2.0");
        Randomizer.setSeed(123);
    }

    public void testPdf() {

        System.out.println("Testing 10000 random pdf calls");

        for (int i = 0; i < 10000; i++) {
            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            double x = Math.log(Randomizer.nextDouble() * 10);

            logNormal.MParameterInput.setValue(M + "", logNormal);
            logNormal.SParameterInput.setValue(S + "", logNormal);
            logNormal.initAndValidate();

            double pdf = 1.0 / (x * S * Math.sqrt(2 * Math.PI)) * Math.exp(-Math.pow(Math.log(x) - M, 2) / (2 * S * S));
            if (x <= 0) pdf = 0; // see logNormal.pdf(x)

            //System.out.println("Testing logNormal[M=" + M + " S=" + S + "].pdf(" + x + ")");

            assertEquals(pdf, logNormal.density(x), 1e-10);
        }
    }

    public void testMean() {

        for (int i = 0; i < 1000; i++) {
            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            logNormal.MParameterInput.setValue(M + "", logNormal);
            logNormal.SParameterInput.setValue(S + "", logNormal);
            logNormal.initAndValidate();
            
            double mean = Math.exp(M + S * S / 2);

            //System.out.println("Testing logNormal[M=" + M + " S=" + S + "].mean()");

            assertEquals(mean, logNormal.getMean(), 1e-10);
        }
    }

//    public void testVariance() {
//
//        for (int i = 0; i < 1000; i++) {
//            double M = Randomizer.nextDouble() * 10.0 - 5.0;
//            double S = Randomizer.nextDouble() * 10;
//
//            logNormal.MParameterInput.setValue(M, logNormal);
//            logNormal.SParameterInput.setValue(S, logNormal);
//            logNormal.initAndValidate();
//
//            double variance = (Math.exp(S * S) - 1) * Math.exp(2 * M + S * S);
//
//            //System.out.println("Testing logNormal[M=" + M + " S=" + S + "].variance()");
//
//            assertEquals(variance, logNormal.getVariance(), 1e-10);
//        }
//    }


    public void testMedian() throws MathException {

        System.out.println("Testing 10000 random quantile(0.5) calls");

        for (int i = 0; i < 10000; i++) {
            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            logNormal.MParameterInput.setValue(M + "", logNormal);
            logNormal.SParameterInput.setValue(S + "", logNormal);
            logNormal.initAndValidate();

            double median = Math.exp(M);

            //System.out.println("Testing logNormal[M=" + M + " S=" + S + "].median()");

            assertEquals(median, logNormal.inverseCumulativeProbability(0.5), median / 1e6);
        }
    }

    public void testCDFAndQuantile() throws MathException {

        System.out.println("Testing 10000 random quantile/cdf pairs");

        for (int i = 0; i < 10000; i++) {

            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            logNormal.MParameterInput.setValue(M + "", logNormal);
            logNormal.SParameterInput.setValue(S + "", logNormal);
            logNormal.initAndValidate();

            double p = Randomizer.nextDouble();
            double quantile = logNormal.inverseCumulativeProbability(p);

            double cdf = logNormal.cumulativeProbability(quantile);

            assertEquals(p, cdf, 1e-7);
        }
    }

//    public void testCDFAndQuantile2() {
//
//        final LogNormalDistributionModel f = new LogNormalDistributionModel();
//        logNormal.initByName("M", "1.0", "S", "1.0");
//        for (double i = 0.01; i < 0.95; i += 0.01) {
//            final double y = i;
//
//            BisectionZeroFinder zeroFinder = new BisectionZeroFinder(new OneVariableFunction() {
//                public double value(double x) {
//                    return f.cdf(x) - y;
//                }
//            }, 0.01, 100);
//            zeroFinder.evaluate();
//
//            assertEquals(f.quantile(i), zeroFinder.getResult(), 1e-6);
//        }
//    }
}

