package test.beast.beast2vs1;


import java.util.ArrayList;
import java.util.List;

import test.beast.beast2vs1.trace.Expectation;

/**
 * @author Walter Xie
 */
public class SubstitutionModelTest extends TestFramework {

    private final String[] XML_FILES = new String[]{"testHKY.xml", "testSiteModelAlpha.xml",
            "testMultiSubstModel.xml", "testSRD06CP12_3.xml"};

    public void testHKY() throws Exception {
        analyse(0);
    }

    public void testSiteModelAlpha() throws Exception {
        analyse(1);
    }

    public void testMultiSubstModel() throws Exception {
        analyse(2);
    }

    public void testSRD06CP12_3() throws Exception {
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

            case 3: // testSRD06CP12_3.xml
                addExpIntoList(expList, "posterior", -1793.4164, 0.2044);
                addExpIntoList(expList, "prior", 0.8416, 0.2033);
                addExpIntoList(expList, "tree.height", 9.8986E-2, 3.709E-4);
                addExpIntoList(expList, "CP12.hky.kappa", 25.0699, 0.2541);
                addExpIntoList(expList, "CP12.frequencies1", 0.3444, 8.2165E-4);
                addExpIntoList(expList, "CP12.frequencies2", 0.2447, 6.885E-4);
                addExpIntoList(expList, "CP12.frequencies3", 0.1444, 5.3526E-4);
                addExpIntoList(expList, "CP12.frequencies4", 0.2666, 6.174E-4);
                addExpIntoList(expList, "CP3.hky.kappa", 82.9907, 1.9773);
                addExpIntoList(expList, "CP3.frequencies1", 0.3197, 1.1763E-3);
                addExpIntoList(expList, "CP3.frequencies2", 0.264, 9.0478E-4);
                addExpIntoList(expList, "CP3.frequencies3", 0.1675, 8.6506E-4);
                addExpIntoList(expList, "CP3.frequencies4", 0.2488, 1.059E-3);
                addExpIntoList(expList, "CP12.gammaShape", 6.4322E-2, 4.8796E-3);
                addExpIntoList(expList, "CP3.gammaShape", 8.5652E-2, 6.9946E-3);
                addExpIntoList(expList, "CP12.mutationRate", 0.9437, 1.195E-3);
                addExpIntoList(expList, "CP3.mutationRate", 1.1126, 2.3899E-3);
                addExpIntoList(expList, "likelihood", -1794.258, 9.3476E-2);
                addExpIntoList(expList, "CP12.treeLikelihood", -1181.4538, 6.0608E-2);
                addExpIntoList(expList, "CP3.treeLikelihood", -612.8041, 6.0563E-2);
                addExpIntoList(expList, "popSize", 0.1211, 7.505E-4);
                addExpIntoList(expList, "coalescent", 6.2171, 1.1266E-2);
                break;

            default:
                throw new Exception("No such XML");
        }

        return expList;
    }

} // class ResumeTest