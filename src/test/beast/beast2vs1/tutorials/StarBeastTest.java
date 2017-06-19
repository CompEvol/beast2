package test.beast.beast2vs1.tutorials;

import org.junit.Test;
import test.beast.beast2vs1.TestFramework;
import test.beast.beast2vs1.trace.Expectation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StarBeastTest {

    @Test
    public void simpleStarBeastTest() throws Exception {
        StarBEASTRunner runner = new StarBEASTRunner(org.fest.util.Files.temporaryFolder());
        runner.analyse(0);
    }

// This is for debugging the test only
//	StarBEASTRunner should be run from simpleStarBeastTest()
//	@Test
//	public void runXML() throws Exception {
//		//System.setProperty("file.name.prefix", org.fest.util.Files.temporaryFolder().getAbsolutePath());
//		StarBEASTRunner runner = new StarBEASTRunner(org.fest.util.Files.temporaryFolder());
//		runner.analyse(0);
//
//	}

    class StarBEASTRunner extends TestFramework {

        StarBEASTRunner(File file) {
            super();
            setUp(new String[]{"/x.xml"});
            dirName = file.getPath();
            logDir = "";
            testFile = "beast_";
            checkESS = false;
        }

        @Override
        protected List<Expectation> giveExpectations(int index_XML) throws Exception {
            List<Expectation> expList = new ArrayList<Expectation>();
            addExpIntoList(expList,"posterior", -3820.43, 1.405193);
            addExpIntoList(expList,"likelihood", -4297.21, 0.387458);
            addExpIntoList(expList,"prior", 21.82193, 0.088263);
            addExpIntoList(expList,"speciescoalescent", 454.9617, 1.470592);
            addExpIntoList(expList,"birthRate.t:Species", 147.594, 3.45043);
            addExpIntoList(expList,"YuleModel.t:Species", 27.19939, 0.091772);
            addExpIntoList(expList,"TreeHeight.Species", 0.014721, 0.000223);
            addExpIntoList(expList,"TreeHeight.t:47", 0.018463, 0.000238);
            addExpIntoList(expList,"TreeHeight.t:26", 0.026735, 0.000143);
            addExpIntoList(expList,"TreeHeight.t:29", 0.024885, 0.000294);
            addExpIntoList(expList,"treeLikelihood.47", -1779.19, 0.198595);
            addExpIntoList(expList,"treeLikelihood.26", -1270.7, 0.239457);
            addExpIntoList(expList,"treeLikelihood.29", -1247.32, 0.283277);
            addExpIntoList(expList,"treePrior.t:47", 115.79, 0.432574);
            addExpIntoList(expList,"treePrior.t:26", 107.3072, 0.263393);
            addExpIntoList(expList,"treePrior.t:29", 112.1106, 0.439123);
            addExpIntoList(expList,"clockRate.c:47", 1.57111, 0.021326);
            addExpIntoList(expList,"clockRate.c:29", 0.930268, 0.01279);
            addExpIntoList(expList,"kappa.s:26", 4.494062, 0.099191);
            addExpIntoList(expList,"kappa.s:29", 3.985931, 0.077119);
            addExpIntoList(expList,"kappa.s:47", 3.628151, 0.063317);
            return expList;
        }

    }





}
