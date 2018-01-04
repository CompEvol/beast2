package test.beast.beast2vs1.tutorials;

import test.beast.beast2vs1.TestFramework;
import test.beast.beast2vs1.trace.Expectation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RateTutorialTest {

//    @Test
//    public void MEPTutorial() throws Exception {
        // TODO: this should run as a separate process since the BEAUti run can interfere with the BEAST run on Hudson
        //MEPRunner runner = new MEPRunner(org.fest.util.Files.temporaryFolder());
        //runner.analyse(0);
//    }


    // This is for debugging the test only
// 	MEPRunner should be run from MEPTutorial()
// 	@Test
// 	public void runXML() throws Exception {
// 		//System.setProperty("file.name.prefix", org.fest.util.Files.temporaryFolder().getAbsolutePath());
// 		MEPRunner runner = new MEPRunner(org.fest.util.Files.temporaryFolder());
// 		runner.analyse(0);
//
// 	}

    class MEPRunner extends TestFramework {

        MEPRunner(File file) {
            super();
            setUp(new String[]{"/x.xml"});
            dirName = file.getPath();
            logDir = "";
            useSeed = false;
            checkESS = false;
            testFile = "RSV2";
        }

        @Override
        protected List<Expectation> giveExpectations(int index_XML) throws Exception {
            List<Expectation> expList = new ArrayList<Expectation>();
            addExpIntoList(expList,"posterior", -6131.89, 0.922052);
            addExpIntoList(expList,"likelihood", -5496.28, 0.401133);
            // low ESS for seed=128
            addExpIntoList(expList,"prior", -635.603, 1.215535);
            addExpIntoList(expList,"treeLikelihood.1", -1440.16, 0.197223);
            addExpIntoList(expList,"treeLikelihood.3", -2271.52, 0.300608);
            addExpIntoList(expList,"treeLikelihood.2", -1784.59, 0.29738);
            addExpIntoList(expList,"TreeHeight", 56.06136, 0.125308);
            addExpIntoList(expList,"kappa.1", 7.727761, 0.069897);
            addExpIntoList(expList,"kappa.2", 10.41839, 0.093578);
            addExpIntoList(expList,"kappa.3", 11.97769, 0.090429);
            addExpIntoList(expList,"mutationRate.1", 0.698603, 0.000868);
            addExpIntoList(expList,"mutationRate.2", 0.960092, 0.00099);
            addExpIntoList(expList,"mutationRate.3", 1.33987, 0.001132);
            addExpIntoList(expList,"clockRate", 0.002179, 1.16E-5);
            addExpIntoList(expList,"popSize", 37.44745, 0.368656);
            // low ESS for seed=128
            addExpIntoList(expList,"CoalescentConstant", -590.862, 1.164024);
            return expList;
        }

    }



}
