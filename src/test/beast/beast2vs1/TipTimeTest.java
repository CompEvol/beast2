package test.beast.beast2vs1;


import test.beast.beast2vs1.trace.Expectation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 */
public class TipTimeTest extends TestFramework {
    private final String[] XML_FILES = new String[]{"testCoalescentTipDates.xml", "testCoalescentTipDates1.xml",
            "testStrictClockTipTime.xml", "testCoalescentTipDatesSampling.xml", "testStrictClockTipDatesSampling.xml"};//, "testTipDates.xml"};

    public void testCoalescentTipDates() throws Exception {
        analyse(0);
    }

    public void testCoalescentTipDates1() throws Exception {
        analyse(1);
    }

    public void testStrictClockTipTime() throws Exception {
        analyse(2);
    }

    public void testCoalescentTipDatesSampling() throws Exception {
        analyse(3);
    }

    public void testStrictClockTipDatesSampling() throws Exception {
        analyse(4);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp(XML_FILES);
    }

    @Override
	protected List<Expectation> giveExpectations(int index_XML) throws Exception {
        List<Expectation> expList = new ArrayList<Expectation>();

        // all values below are from BEAST 1.6.2
        switch (index_XML) {
            case 0: // testCoalescentTipDates.xml
//        BEAST 1 testCoalescent.xml
                addExpIntoList(expList, "tree.height", 19361.1519, 66.3224);
                addExpIntoList(expList, "popSize", 10000.0, 0.0);
                addExpIntoList(expList, "coalescent", -30.6263, 1.2747E-2);
                break;

            case 1: // testCoalescentTipDates1.xml
//        BEAST 1 testCoalescent.xml
                addExpIntoList(expList, "tree.height", 15000.0, 70.0);
                addExpIntoList(expList, "coalescent", -30.6163, 1.3415E-2);
                break;

            case 2: // testStrictClockTipTime.xml
//        BEAST 1 testStrictClockTipTime.xml
                addExpIntoList(expList, "posterior", -3935.6436, 6.9703E-2);
                addExpIntoList(expList, "prior", -84.8353, 3.9182E-2);
                addExpIntoList(expList, "tree.height", 68.2541, 8.7152E-2);
                addExpIntoList(expList, "popSize", 33.9172, 0.1266);
                addExpIntoList(expList, "hky.kappa", 17.2562, 7.7984E-2);
                addExpIntoList(expList, "hky.frequencies.1", 0.2838, 4.3904E-4);
                addExpIntoList(expList, "hky.frequencies.2", 0.2116, 3.8223E-4);
                addExpIntoList(expList, "hky.frequencies.3", 0.2516, 4.0877E-4);
                addExpIntoList(expList, "hky.frequencies.4", 0.253, 4.1631E-4);
                addExpIntoList(expList, "clockRate", 8.1277E-4, 1.405E-6);
                addExpIntoList(expList, "treeLikelihood", -3850.8083, 6.0429E-2);
                addExpIntoList(expList, "coalescent", -71.6925, 3.5705E-2);
                break;

            case 3: // testCoalescentTipDatesSampling.xml
                addExpIntoList(expList, "coalescent", -41.4962, 1.6251E-4);
                addExpIntoList(expList, "mrcatime(TaxonSetAll)", 20000.0, 8.9215E-2); // root height.
                addExpIntoList(expList, "tree.height", 15000.0, 8.9215E-2); // tree height.
                addExpIntoList(expList, "height(A)", 5000.0, 0.01);
                addExpIntoList(expList, "height(B)", 5000.0, 0.01);
                break;

            case 4: // testStrictClockTipDatesSampling.xml
//        BEAST 1 testStrictClockTipDatesSampling.xml
//                addExpIntoList(expList, "posterior", -4490.0503, 0.221);
//                addExpIntoList(expList, "prior", -70.0423, 0.2051);
                addExpIntoList(expList, "tree.height", 10.848, 5.6709E-2);
                addExpIntoList(expList, "popSize", 9.4786, 0.1178);
                addExpIntoList(expList, "hky.kappa", 8.906, 4.4495E-2);
                addExpIntoList(expList, "hky.frequencies.1", 0.3433, 4.5468E-4);
                addExpIntoList(expList, "hky.frequencies.2", 0.1857, 3.3512E-4);
                addExpIntoList(expList, "hky.frequencies.3", 0.2229, 4.1666E-4);
                addExpIntoList(expList, "hky.frequencies.4", 0.2481, 4.3418E-4);
                addExpIntoList(expList, "clockRate", 3.6834E-3, 2.8931E-5);
//                addExpIntoList(expList, "height(TREESPARROW_HENAN_1_2004)", 2005 - 1.502, 2.8594E-2);
                addExpIntoList(expList, "height(CHICKEN_HONGKONG_915_1997)", 2005 - 8.1493, 1.0676E-2);
                addExpIntoList(expList, "treeLikelihood", -4420.008, 0.1455);
                addExpIntoList(expList, "coalescent", -64.0766, 0.1959);
                break;

//            case 8: // testTipDates.xml
//                addExpIntoList(expList, "treeLikelihood", -15000.435271635279);
//                addExpIntoList(expList, "tree.height", 38.6109296447932);
//                addExpIntoList(expList, "hky.kappa", 2179.425239015365);
//                break;
            default:
                throw new Exception("No such XML");
        }

        return expList;
    }


} // class ResumeTest