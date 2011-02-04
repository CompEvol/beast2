package test.beast.math.distributions;

import org.junit.Test;

import beast.core.parameter.RealParameter;
import beast.math.distributions.LogNormalDistributionModel;

import junit.framework.TestCase;

public class LogNormalDistributionModelTest extends TestCase {

	@Test
	public void testPDF() throws Exception {
        System.out.println("Testing 10000 random pdf calls");
        LogNormalDistributionModel logNormal = new LogNormalDistributionModel();
        logNormal.init("1.0","2.0");
        
        for (int i = 0; i < 10000; i++) {
            double M = Math.random() * 10.0 - 5.0;
            double S = Math.random() * 10;

            double x = Math.log(Math.random() * 10);

            logNormal.MParameter.setValue(M+"", logNormal);
            logNormal.SParameter.setValue(S+"", logNormal);
            logNormal.initAndValidate();

            double pdf = 1.0 / (x * S * Math.sqrt(2 * Math.PI)) * Math.exp(-Math.pow(Math.log(x) - M, 2) / (2 * S * S));

            System.out.println("Testing logNormal[M=" + M + " S=" + S + "].pdf(" + x + ")");
            double f = logNormal.density(x);

            assertEquals(pdf, f , 1e-10);
        }
	}

	@Test
	public void testCalcLogP() throws Exception {
        LogNormalDistributionModel logNormal = new LogNormalDistributionModel();
        logNormal.init("1.0","2.0");
	    logNormal.m_bMeanInRealSpaceInput.setValue("true", logNormal);
	    logNormal.m_offset.setValue("1200", logNormal);
	    logNormal.MParameter.setValue("2000", logNormal);
	    logNormal.SParameter.setValue("0.6", logNormal);
	    logNormal.initAndValidate();
	    RealParameter p = new RealParameter("2952.6747000000014");
	    double f0 = logNormal.calcLogP(p);
	    assertEquals(-7.880210654973873, f0 , 1e-10);
	}


}

