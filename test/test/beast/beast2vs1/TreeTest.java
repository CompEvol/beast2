package test.beast.beast2vs1;


import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import test.beast.beast2vs1.trace.Expectation;

/**
 * @author Walter Xie
 */
public class TreeTest extends TestFramework {

    String[] XML_FILES = new String[]{"testCalibration.xml", "testCalibrationMono.xml"};

    public void testCalibration() throws Exception {
        analyse(0);
    }

    public void testCalibrationMono() throws Exception {
        analyse(1);
    }

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp(XML_FILES);
    }

    @Override
	protected List<Expectation> giveExpectations(int index_XML) throws Exception {
        List<Expectation> expList = new ArrayList<Expectation>();

        // all values below are from BEAST 1.7
        switch (index_XML) {
            case 0: // testCalibration.xml
//        BEAST 1 testCalibration.xml
                addExpIntoList(expList, "posterior", -1884.6966, 6.3796E-2);
                addExpIntoList(expList, "prior", -68.0023, 2.114E-2);
                addExpIntoList(expList, "tree.height", 6.3129E-2, 6.5853E-5);
                addExpIntoList(expList, "mrca.age(human,chimp)", 2.0326E-2, 3.5906E-5);
                addExpIntoList(expList, "popSize", 9.7862E-2, 6.2387E-4);
                addExpIntoList(expList, "hky.kappa", 25.8288, 0.1962);
                addExpIntoList(expList, "hky.frequencies.1", 0.3262, 5.9501E-4);
                addExpIntoList(expList, "hky.frequencies.2", 0.2569, 5.0647E-4);
                addExpIntoList(expList, "hky.frequencies.3", 0.1552, 4.4638E-4);
                addExpIntoList(expList, "hky.frequencies.4", 0.2617, 5.1085E-4);
                addExpIntoList(expList, "likelihood", -1816.6943, 5.8444E-2);
                addExpIntoList(expList, "coalescent", 7.2378, 9.1912E-3);
                break;

            case 1: // testCalibrationMono.xml
//        BEAST 1 testCalibrationMono.xml
                addExpIntoList(expList, "posterior", -1897.3811, 6.5818E-2);
                addExpIntoList(expList, "prior", -68.6144, 1.9896E-2);
                addExpIntoList(expList, "tree.height", 6.3258E-2, 6.7751E-5);
                addExpIntoList(expList, "mrca.age(human,chimp)", 1.7069E-2, 3.3455E-5);
                addExpIntoList(expList, "popSize", 0.1049, 6.4588E-4);
                addExpIntoList(expList, "hky.kappa", 26.7792, 0.1851);
                addExpIntoList(expList, "hky.frequencies.1", 0.328, 7.1121E-4);
                addExpIntoList(expList, "hky.frequencies.2", 0.2573, 5.4356E-4);
                addExpIntoList(expList, "hky.frequencies.3", 0.1548, 4.2604E-4);
                addExpIntoList(expList, "hky.frequencies.4", 0.2599, 6.0174E-4);
                addExpIntoList(expList, "likelihood", -1828.7667, 6.527E-2);
                addExpIntoList(expList, "coalescent", 6.864, 9.7699E-3);
                break;

            default:
                throw new Exception("No such XML");
        }

        return expList;
    }

} // class ResumeTest