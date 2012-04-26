package test.beast.math.distributions;

import org.junit.Test;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import beast.math.distributions.Exponential;
import beast.math.distributions.Gamma;
import beast.math.distributions.LogNormalDistributionModel;
import beast.math.distributions.Normal;

import junit.framework.TestCase;

public class MeanOfParametricDistributionTest extends TestCase {

	@Test
    public void testMeanOfNormal() throws Exception {
		Normal normal = new Normal();
		normal.initByName("mean","123.0","sigma","3.0");
		double mean = normal.getMean();
        assertEquals(mean, 123, 1e-10);

        normal = new Normal();
		normal.initByName("mean","123.0","sigma","30.0");
		mean = normal.getMean();
        assertEquals(mean, 123, 1e-10);

        normal = new Normal();
		normal.initByName("mean","123.0","sigma","3.0","offset","3.0");
		mean = normal.getMean();
        assertEquals(mean, 126, 1e-10);
	}

	@Test
    public void testMeanOfGamma() throws Exception {
		Gamma gamma = new Gamma();
		gamma.initByName("alpha", "100", "beta", "10");
		double mean = gamma.getMean();
        assertEquals(mean, 10, 1e-10);

        gamma = new Gamma();
		gamma.initByName("alpha", "100", "beta", "100");
		mean = gamma.getMean();
        assertEquals(mean, 1, 1e-10);

        gamma = new Gamma();
		gamma.initByName("alpha", "100", "beta", "10", "offset", "3");
		mean = gamma.getMean();
        assertEquals(mean, 13, 1e-10);
	}


	@Test
    public void testMeanOfExponential() throws Exception {
		Exponential exp = new Exponential();
		exp.initByName("lambda", "10");
		double mean = exp.getMean();
        assertEquals(mean, 10, 1e-10);

        exp = new Exponential();
		exp.initByName("lambda", "1");
		mean = exp.getMean();
        assertEquals(mean, 1, 1e-10);

        exp = new Exponential();
		exp.initByName("lambda", "1", "offset", "3");
		mean = exp.getMean();
        assertEquals(mean, 4, 1e-10);
	}

	@Test
    public void testMeanOfLogNormal() throws Exception {
		LogNormalDistributionModel exp = new LogNormalDistributionModel();
		exp.initByName("M", "10" , "S", "1", "meanInRealSpace", true);
		double mean = exp.getMean();
        assertEquals(mean, 10, 1e-10);

        exp = new LogNormalDistributionModel();
		exp.initByName("M", "1", "S", "1", "meanInRealSpace", true);
		mean = exp.getMean();
        assertEquals(mean, 1, 1e-10);

        exp = new LogNormalDistributionModel();
		exp.initByName("M", "1", "S", "1", "meanInRealSpace", true, "offset", "3");
		mean = exp.getMean();
        assertEquals(mean, 4, 1e-10);

        try {
            exp = new LogNormalDistributionModel();
    		exp.initByName("M", "1", "S", "1", "meanInRealSpace", false, "offset", "3");
    		mean = exp.getMean();
            assertEquals(mean, 4, 1e-10);
        } catch (NotImplementedException e) {
        	// we are fine here
        }
	}

}
