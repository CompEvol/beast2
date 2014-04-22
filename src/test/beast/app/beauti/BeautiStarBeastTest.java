package test.beast.app.beauti;




import java.io.File;
import java.util.ArrayList;
import java.util.List;


import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.fixture.JMenuItemFixture;
import org.fest.swing.fixture.JOptionPaneFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.image.ScreenshotTaker;
import org.junit.Test;

import test.beast.beast2vs1.TestFramework;
import test.beast.beast2vs1.trace.Expectation;

public class BeautiStarBeastTest extends BeautiBase {
	final static String PREFIX = "doc/tutorials/STARBEAST/figures/BEAUti_";

	@Test
	public void simpleStarBeastTest() throws Exception {
		ScreenshotTaker screenshotTaker = new ScreenshotTaker();
		beauti.frame.setSize(1024, 640);

		String BASE_DIR = PREFIX.substring(0, PREFIX.lastIndexOf('/'));
		for (File file : new File(BASE_DIR).listFiles()) {
			if (file.getAbsolutePath().contains(PREFIX) && file.getName().endsWith(".png")) {
				file.delete();
			}
		}
		
		// create screen-shot showing template menu item
		warning("Select StarBeast template");
		beautiFrame.menuItemWithPath("File", "Template").click();
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "selectTemplate.png");
		JMenuItemFixture templateMenu = beautiFrame.menuItemWithPath("File", "Template", "StarBeast");
		templateMenu.click();
		// remove menu from screen
		beautiFrame.menuItemWithPath("File").click();
		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		f = f.selectTab("Priors");


		// 1. Load gopher data 26.nex, 29.nex, 47.nex
		warning("1. Load gopher data 26.nex, 29.nex, 47.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("29.nex"), new File("47.nex"));

		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "DataPartitions.png");
		printBeautiState(f);

		
		// 2. Define Taxon sets
		warning("2. Define taxon sets");
		f.selectTab("Taxon sets");
		beautiFrame.button("Guess").click();
		JOptionPaneFixture dialog = new JOptionPaneFixture(robot());
		//DialogFixture dialog = WindowFinder.findDialog("GuessTaxonSets").using(robot());
		dialog.radioButton("split on character").click();
		dialog.comboBox("splitCombo").selectItem("2");
		dialog.textBox("SplitChar2").deleteText().enterText("_");
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "Guess_Taxonsets.png");
		//JButton okButton = dialog.robot.finder().find(JButtonMatcher.withText("OK"));
		//new JButtonFixture(dialog.robot, okButton).click();
		dialog.okButton().click();
		printBeautiState(f);

		// 3. Set site model to HKY + empirical frequencies
		warning("3. Set site model to HKY + empirical frequencies");
		f.selectTab("Site Model");
		for (int i = 0; i < 3; i++) {
			beautiFrame.list().selectItem(i);
			beautiFrame.comboBox().selectItem("HKY");
			JComboBoxFixture freqs = beautiFrame.comboBox("frequencies");
			freqs.selectItem("Empirical");
			//beautiFrame.checkBox("mutationRate.isEstimated").check();
		}
		//JCheckBoxFixture fixMeanMutationRate = beautiFrame.checkBox("FixMeanMutationRate");
		//fixMeanMutationRate.check();
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "Site_Model.png");
		printBeautiState(f);
		
		// 4. Inspect clock models
		warning("4. Inspect clock models");
		f.selectTab("Clock Model");
		beautiFrame.list().selectItem(0);
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "ClockModel1.png");
		beautiFrame.list().selectItem(1);
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "ClockModel2.png");
		beautiFrame.list().selectItem(2);
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "ClockModel3.png");
		
		// 5. Inspect multispecies coalescent
		warning("5. Inspect multispecies coalescent");
		f.selectTab("Multi Species Coalescent");
		beautiFrame.button("treePrior.t:26.editButton").click();
		beautiFrame.button("treePrior.t:29.editButton").click();
		beautiFrame.button("treePrior.t:47.editButton").click();
		beautiFrame.comboBox().selectItem("linear_with_constant_root");
		beautiFrame.button("treePrior.t:26.editButton").click();
		beautiFrame.button("treePrior.t:29.editButton").click();
		beautiFrame.button("treePrior.t:47.editButton").click();
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "MSP.png");
		
		// 6. Set up MCMC parameters
		warning("6. Set up MCMC parameters");
		f = f.selectTab("MCMC");
		beautiFrame.textBox("chainLength").selectAll().setText("5000000");
		beautiFrame.button("speciesTreeLogger.editButton").click();
		beautiFrame.textBox("logEvery").selectAll().setText("1000");
		beautiFrame.button("speciesTreeLogger.editButton").click();

		beautiFrame.button("screenlog.editButton").click();
		beautiFrame.textBox("logEvery").selectAll().setText("10000");
		beautiFrame.button("speciesTreeLogger.editButton").click();
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "MCMC.png");
		
		makeSureXMLParses();
		
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
			sDir = file.getPath();
			sLogDir = "";
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
