package test.beast.math.distributions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.apache.commons.math.MathException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import beast.base.inference.distribution.LaplaceDistribution;
import beast.base.util.Randomizer;
import test.beast.BEASTTestCase;


/*
 * @author Louis du Plessis
 *         Date: 2018/08/06
 */
public class LaplaceDistributionTest  {

    LaplaceDistribution laplace;

    @BeforeEach 
    public void setUp() {
        laplace = new LaplaceDistribution();
        laplace.initAndValidate();
        Randomizer.setSeed(123);
    }


    @Test
    public void testPdf() {

        System.out.println("Testing 10000 random pdf calls");


        for (int i = 0; i < 10000; i++) {
            double mu = Randomizer.nextDouble() * 10.0 - 5.0;
            double scale = Randomizer.nextDouble() * 10;

            double x = Randomizer.nextDouble() * 10;

            laplace.muInput.setValue(mu + "", laplace);
            laplace.scaleInput.setValue(scale + "", laplace);
            laplace.initAndValidate();

            double c = 1.0/(2*scale);
            double pdf = c*Math.exp(-Math.abs(x-mu)/scale);

            //System.out.println(x+"\t"+mu+"\t"+scale+"\t"+pdf+"\t"+laplace.density(x));

            assertEquals(pdf, laplace.density(x), BEASTTestCase.PRECISION);
        }


        /* Test with an example using R */
        laplace.muInput.setValue(0 + "", laplace);
        laplace.scaleInput.setValue(2.14567 + "", laplace);
        assertEquals(0.07267657, laplace.density(2.5), BEASTTestCase.PRECISION);

        laplace.muInput.setValue(5 + "", laplace);
        laplace.scaleInput.setValue(3 + "", laplace);
        assertEquals(0.06131324, laplace.density(2), BEASTTestCase.PRECISION);
    }

    @Test
    public void testMean() {

        for (int i = 0; i < 1000; i++) {
            double mu = Randomizer.nextDouble() * 10.0 - 5.0;

            laplace.muInput.setValue(mu + "", laplace);
            laplace.initAndValidate();

            assertEquals(mu, laplace.getMean(), BEASTTestCase.PRECISION);
        }
    }

    @Test
    public void testMedian() throws MathException {

        System.out.println("Testing 10000 random quantile(0.5) calls");

        for (int i = 0; i < 10000; i++) {
            double mu = Randomizer.nextDouble() * 10.0 - 5.0;
            double scale = Randomizer.nextDouble() * 10;

            laplace.muInput.setValue(mu + "", laplace);
            laplace.scaleInput.setValue(scale + "", laplace);
            laplace.initAndValidate();

            double median = mu;

            assertEquals(median, laplace.inverseCumulativeProbability(0.5), BEASTTestCase.PRECISION);
        }
    }

    @Test
    public void testCDFAndQuantile() throws MathException {

        System.out.println("Testing 10000 random quantile/cdf pairs");

        for (int i = 0; i < 10000; i++) {

            double mu = Randomizer.nextDouble() * 10.0 - 5.0;
            double scale = Randomizer.nextDouble() * 10;

            laplace.muInput.setValue(mu + "", laplace);
            laplace.scaleInput.setValue(scale + "", laplace);
            laplace.initAndValidate();

            double p = Randomizer.nextDouble();
            double quantile = laplace.inverseCumulativeProbability(p);
            double cdf = laplace.cumulativeProbability(quantile);

            assertEquals(p, cdf, BEASTTestCase.PRECISION);
        }

    }

    @Test
    public void testLogPdf() {

        System.out.println("Testing log pdf calls");

        /* Test with an example using R */
        laplace.muInput.setValue(0 + "", laplace);
        laplace.scaleInput.setValue(2.14567 + "", laplace);
        assertEquals(-2.621736, laplace.logDensity(2.5), BEASTTestCase.PRECISION);

        laplace.muInput.setValue(5 + "", laplace);
        laplace.scaleInput.setValue(3 + "", laplace);
        assertEquals(-2.791759, laplace.logDensity(2), BEASTTestCase.PRECISION);
    }

}
