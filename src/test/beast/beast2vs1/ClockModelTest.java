package test.beast.beast2vs1;


import java.util.ArrayList;
import java.util.List;

import test.beast.beast2vs1.trace.Expectation;

/**
 * @author Walter Xie
 */
public class ClockModelTest extends TestFramework {

    private final String[] XML_FILES = new String[]{"testStrictClock.xml", "testStrictClock2.xml",
            "testRandomLocalClock.xml", "testUCRelaxedClockLogNormal.xml"};

    public void testStrictClock() throws Exception {
        analyse(0);
    }

    public void testStrictClock2() throws Exception {
        analyse(1);
    }

    public void testRandomLocalClock() throws Exception {
        analyse(2);
    }

    public void testUCRelaxedClockLogNormal() throws Exception {
        analyse(3);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp(XML_FILES);
    }

    @Override
	protected List<Expectation> giveExpectations(int index_XML) throws Exception {
        List<Expectation> expList = new ArrayList<Expectation>();

        // all values below are from BEAST 1.7
        switch (index_XML) {
            case 0: // testStrictClock.xml
//        BEAST 1 testStrictClockNoDate.xml
                addExpIntoList(expList, "posterior", -1812.939, 0.0581);
                addExpIntoList(expList, "prior", 3.752, 0.0205);
                addExpIntoList(expList, "tree.height", 6.32E-02, 6.76E-05);
                addExpIntoList(expList, "popSize", 9.67E-02, 5.99E-04);
                addExpIntoList(expList, "hky.kappa", 25.807, 0.1812);
                addExpIntoList(expList, "hky.frequencies1", 0.327, 6.15E-04);
                addExpIntoList(expList, "hky.frequencies2", 0.258, 6.09E-04);
                addExpIntoList(expList, "hky.frequencies3", 0.155, 3.88E-04);
                addExpIntoList(expList, "hky.frequencies4", 0.261, 5.17E-04);
                addExpIntoList(expList, "clockRate", 1.0, 0.0);
                addExpIntoList(expList, "treeLikelihood", -1816.691, 0.0522);
                addExpIntoList(expList, "coalescent", 7.24, 9.58E-03);
                break;

            case 1: // testStrictClock2.xml
//        BEAST 1 testStrictClockNoDate2.xml
                addExpIntoList(expList, "posterior", -1811.898, 2.89E-02);
                addExpIntoList(expList, "prior", 3.721, 2.34E-02);
                addExpIntoList(expList, "tree.height", 6.29E-02, 6.43E-05);
                addExpIntoList(expList, "popSize", 9.76E-02, 6.68E-04);
                addExpIntoList(expList, "hky.kappa", 26.491, 0.2089);
                addExpIntoList(expList, "clockRate", 1.0, 0.0);
                addExpIntoList(expList, "treeLikelihood", -1815.619, 2.20E-02);
                addExpIntoList(expList, "coalescent", 7.276, 9.55E-03);
                break;

            case 2: // testRandomLocalClock.xml
                addExpIntoList(expList, "posterior", -1821.0538, 0.1647);
                addExpIntoList(expList, "prior", -4.4935, 0.1553);
                addExpIntoList(expList, "tree.height", 6.4088E-2, 1.4663E-4);
                addExpIntoList(expList, "popSize", 9.6541E-2, 6.6609E-4);
                addExpIntoList(expList, "hky.kappa", 26.544, 0.2648);
                addExpIntoList(expList, "hky.frequencies1", 0.3253, 7.3002E-4);
                addExpIntoList(expList, "hky.frequencies2", 0.258, 5.5405E-4);
                addExpIntoList(expList, "hky.frequencies3", 0.1546, 4.6881E-4);
                addExpIntoList(expList, "hky.frequencies4", 0.262, 6.1501E-4);
                addExpIntoList(expList, "treeLikelihood", -1816.5603, 5.5936E-2);
                addExpIntoList(expList, "coalescent", 7.2815, 1.3472E-2);
                break;

            case 3: // testUCRelaxedClockLogNormal.xml
                addExpIntoList(expList, "posterior", -1812.117, 0.1369);
                addExpIntoList(expList, "prior", 4.3666, 5.2353E-2);
                addExpIntoList(expList, "treeLikelihood", -1816.4836, 9.4437E-2);
                addExpIntoList(expList, "tree.height", 6.4535E-2, 4.3471E-4);
                addExpIntoList(expList, "popSize", 9.4535E-2, 1.7803E-3);
                addExpIntoList(expList, "hky.kappa", 26.0574, 0.2775);
                addExpIntoList(expList, "hky.frequencies1", 0.3262, 9.4363E-4);
                addExpIntoList(expList, "hky.frequencies2", 0.2575, 7.5592E-4);
                addExpIntoList(expList, "hky.frequencies3", 0.1545, 5.1935E-4);
                addExpIntoList(expList, "hky.frequencies4", 0.2618, 5.9827E-4);
                addExpIntoList(expList, "S", 0.1786, 4.9947E-3);
                addExpIntoList(expList, "coalescent", 7.3012, 3.0267E-2);
                addExpIntoList(expList, "rate.mean", 0.9962, 3.1704E-3);
                addExpIntoList(expList, "rate.coefficientOfVariation", 0.1565, 4.3557E-3);
//                rate.variance   3.526E-2                
                break;

            default:
                throw new Exception("No such XML");
        }

        return expList;
    }


} // class ResumeTest