package test.beast.beast2vs1;

import beast.trace.Expectation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 */
public class SubstitutionModelTest extends TestFramework {

    private final String[] XML_FILES = new String[]{"testHKY.xml", "testSiteModelAlpha.xml"};

    public void testHKY() throws Exception {
        analyse(0);
    }

    public void testSiteModelAlpha() throws Exception {
        analyse(1);
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


            default:
                throw new Exception("No such XML");
        }

        return expList;
    }

} // class ResumeTest