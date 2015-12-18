package test.beast.math.distributions;


import org.junit.Test;

import beast.core.parameter.RealParameter;
import beast.math.distributions.LogNormalDistributionModel;
import beast.util.XMLParser;
import junit.framework.TestCase;

public class LogNormalDistributionModelTest extends TestCase {

    @Test
    public void testPDF() throws Exception {
        System.out.println("Testing 10000 random pdf calls");
        LogNormalDistributionModel logNormal = new LogNormalDistributionModel();
        logNormal.init("1.0", "2.0");

        for (int i = 0; i < 10000; i++) {
            double M = Math.random() * 10.0 - 5.0;
            double S = Math.random() * 10;

            double x = -1;
            while( x < 0 ) {
                x = Math.log(Math.random() * 10);
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
        String sXML = "<input spec='beast.math.distributions.LogNormalDistributionModel' " +
                "offset='1200' " +
                "M='2000' " +
                "S='0.6' " +
                "meanInRealSpace='true'/>";
        RealParameter p = new RealParameter(new Double[]{2952.6747000000014});
        XMLParser parser = new XMLParser();
        LogNormalDistributionModel logNormal = (LogNormalDistributionModel) parser.parseBareFragment(sXML, true);

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
}

