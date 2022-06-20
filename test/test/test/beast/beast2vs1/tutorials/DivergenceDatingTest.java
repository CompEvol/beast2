package test.beast.beast2vs1.tutorials;


import org.junit.jupiter.api.Test;
import test.beast.beast2vs1.TestFramework;
import test.beast.beast2vs1.trace.Expectation;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DivergenceDatingTest {
    @Test
    public void DivergenceDatingTutorial() throws Exception {
        DivergenceDatingRunner runner = new DivergenceDatingRunner(Files.createTempDirectory("tmp").toFile());
        runner.analyse(0);
    }

    // This is for debugging the test only
// 	DivergenceDatingRunner should be run from DivergenceDatingTutorial()
// 	@Test
// 	public void runXML() throws Exception {
// 		//System.setProperty("file.name.prefix", org.fest.util.Files.temporaryFolder().getAbsolutePath());
// 		DivergenceDatingRunner runner = new DivergenceDatingRunner(org.fest.util.Files.temporaryFolder());
// 		runner.analyse(0);
//
// 	}

    class DivergenceDatingRunner extends TestFramework {

        DivergenceDatingRunner(File file) {
            super();
            setUp(new String[]{"/x.xml"});
            dirName = file.getPath();
            logDir = "";
            useSeed = false;
            checkESS = false;
            testFile = "primate-mtDNA";
            SEED = 126;
        }

        @Override
        protected List<Expectation> giveExpectations(int index_XML) throws Exception {
            List<Expectation> expList = new ArrayList<Expectation>();
            addExpIntoList(expList,"posterior", -5508.64, 0.277076);
            addExpIntoList(expList,"likelihood", -5442.24, 0.314717);
            addExpIntoList(expList,"prior", -67.5441, 0.197599);
            addExpIntoList(expList,"treeLikelihood.1stpos", -1382.86, 0.163746);
            // low ESS for seed=128
            addExpIntoList(expList,"treeLikelihood.noncoding", -957.075, 0.157176);
            addExpIntoList(expList,"treeLikelihood.2ndpos", -954.148, 0.184448);
            addExpIntoList(expList,"treeLikelihood.3rdpos", -2148.15, 0.311767);
            addExpIntoList(expList,"TreeHeight", 83.46231, 1.039008);
            addExpIntoList(expList,"YuleModel", -51.2849, 0.115309);
            addExpIntoList(expList,"birthRate", 0.029973, 0.000342);
            addExpIntoList(expList,"kappa.noncoding", 14.67406, 0.462209);
            addExpIntoList(expList,"kappa.1stpos", 6.812315, 0.113013);
            addExpIntoList(expList,"kappa.2ndpos", 8.853521, 0.19871);
            // low ESS for seed=128
            addExpIntoList(expList,"kappa.3rdpos", 30.52025, 0.772299);
            addExpIntoList(expList,"gammaShape.noncoding", 0.241535, 0.005483);
            addExpIntoList(expList,"gammaShape.1stpos", 0.480865, 0.006024);
            addExpIntoList(expList,"gammaShape.2ndpos", 0.576606, 0.017974);
            addExpIntoList(expList,"gammaShape.3rdpos", 2.832824, 0.092259);
            // low ESS for seed=128
            addExpIntoList(expList,"mutationRate.noncoding", 0.12345, 0.003921);
            // low ESS for seed=128
            addExpIntoList(expList,"mutationRate.1stpos", 0.157503, 0.002944);
            // low ESS for seed=128
            addExpIntoList(expList,"mutationRate.2ndpos", 0.061211, 0.001608);
            addExpIntoList(expList,"logP(mrca(Human-Chimp))", -0.78481, 0.022675);
            addExpIntoList(expList,"mrcatime(Human-Chimp)", 5.845026, 0.014885);
            addExpIntoList(expList,"clockRate", 0.034266, 0.000481);
            return expList;
        }

    }

}
