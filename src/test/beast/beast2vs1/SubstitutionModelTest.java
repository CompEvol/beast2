package test.beast.beast2vs1;

import beast.trace.Expectation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 */
public class SubstitutionModelTest extends TestFramework {

    private final String[] XML_FILES = new String[]{"testHKY.xml", "testSiteModelAlpha.xml", "testMultiSubstModel.xml"};

    public void testHKY() throws Exception {
        analyse(0);
    }

    public void testSiteModelAlpha() throws Exception {
        analyse(1);
    }

    public void testMultiSubstModel() throws Exception {
        analyse(2);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp(XML_FILES);
    }

    protected List<Expectation> giveExpectations(int index_XML) throws Exception {
        List<Expectation> expList = new ArrayList<Expectation>();

        // all values below are from BEAST 1.7
        switch (index_XML) {
            case 0: // testHKY.xml
//        BEAST 1 testMCMC.xml
//        <expectation name="likelihood" value="-1815.75"/>
//        <expectation name="treeModel.rootHeight" value="0.0642048"/>
//        <expectation name="hky.kappa" value="32.8941"/>
                addExpIntoList(expList, "treeLikelihood", -1815.766, 0.0202);
                addExpIntoList(expList, "tree.height", 6.42E-02, 6.53E-05);
                addExpIntoList(expList, "hky.kappa", 33.019, 0.1157);
                break;

            case 1: // testSiteModelAlpha.xml
                addExpIntoList(expList, "posterior", -4433.5759, 0.4005);
                addExpIntoList(expList, "tree.height", 10.8255, 0.1264);
                addExpIntoList(expList, "popSize", 10.0706, 0.254);
                addExpIntoList(expList, "hky.kappa", 9.4378, 2.8498E-2);
                addExpIntoList(expList, "siteModel.alpha", 0.1771, 1.256E-3);
                addExpIntoList(expList, "clockRate", 3.9202E-3, 6.6938E-5);
                addExpIntoList(expList, "treeLikelihood", -4369.4205, 9.9579E-2);
                addExpIntoList(expList, "coalescent", -64.1553, 0.4559);
                break;

            case 2: // testMultiSubstModel.xml
                addExpIntoList(expList, "posterior", -6001.92, 8.1888E-2);
                addExpIntoList(expList, "prior", -8.24, 1.1349E-2);
                addExpIntoList(expList, "TreeHeight.firsthalf", 0.2523, 1.2987E-4);
                addExpIntoList(expList, "kappa.firsthalf", 4.8401, 1.184E-2);
                addExpIntoList(expList, "freqParameter.firsthalf1", 0.289, 6.1115E-4);
                addExpIntoList(expList, "freqParameter.firsthalf2", 0.3204, 6.8116E-4);
                addExpIntoList(expList, "freqParameter.firsthalf3", 0.1081, 3.8843E-4);
                addExpIntoList(expList, "freqParameter.firsthalf4", 0.2825, 5.1796E-4);
                addExpIntoList(expList, "kappa.secondhalf", 5.22, 1.121E-2);
                addExpIntoList(expList, "freqParameter.secondhalf1", 0.3368, 6.3272E-4);
                addExpIntoList(expList, "freqParameter.secondhalf2", 0.2646, 4.8458E-4);
                addExpIntoList(expList, "freqParameter.secondhalf3", 0.1018, 2.8896E-4);
                addExpIntoList(expList, "freqParameter.secondhalf4", 0.2968, 5.2109E-4);
                addExpIntoList(expList, "likelihood", -5993.68, 8.1194E-2);
                addExpIntoList(expList, "treeLikelihood.firsthalf", -3049.7192, 6.5715E-2);
                addExpIntoList(expList, "treeLikelihood.secondhalf", -2943.9608, 5.2473E-2);
                addExpIntoList(expList, "popSize.firsthalf", 0.5184, 1.8192E-3);
                addExpIntoList(expList, "CoalescentConstant.firsthalf", -3.1965, 8.9084E-3);
                break;

            default:
                throw new Exception("No such XML");
        }

        return expList;
    }

} // class ResumeTest