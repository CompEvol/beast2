package test.beast.app.beauti;



import static org.fest.swing.edt.GuiActionRunner.execute;

import java.io.File;

import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.JCheckBoxFixture;
import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.fixture.JMenuItemFixture;
import org.fest.swing.fixture.JOptionPaneFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.image.ScreenshotTaker;
import org.junit.Test;

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
		
		warning("Select StarBeast template");
		beautiFrame.menuItemWithPath("File", "Template").click();
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "selectTemplate.png");
		JMenuItemFixture templateMenu = beautiFrame.menuItemWithPath("File", "Template", "StarBeast");
		templateMenu.click();

		// 1. Load gopher data 26.nex, 47.nex, 59.nex
		warning("1. Load gopher data 26.nex, 47.nex, 59.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"), new File("59.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "DataPartitions.png");
		printBeautiState(f);
		assertStateEquals("popSize", "Tree.t:Species", "birthRate.t:Species", "popMean", "Tree.t:26", "Tree.t:47", "clockRate.c:47", "Tree.t:59", "clockRate.c:59");
		assertOperatorsEqual("Reheight", "popSizeScaler", "updown.all", "YuleBirthRateScaler.t:Species", "popMeanScale", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "updown.47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "treeScaler.t:59", "treeRootScaler.t:59", "UniformOperator.t:59", "SubtreeSlide.t:59", "narrow.t:59", "wide.t:59", "WilsonBalding.t:59", "updown.59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.t:Species", "YuleBirthRatePrior.t:Species", "popMean.prior", "ClockPrior.c:47", "ClockPrior.c:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "speciescoalescent", "birthRate.t:Species", "YuleModel.t:Species", "TreeHeight.Species", "treeLikelihood.26", "treePrior.t:26", "TreeHeight.t:26", "treeLikelihood.47", "treePrior.t:47", "TreeHeight.t:47", "clockRate.c:47", "treeLikelihood.59", "treePrior.t:59", "TreeHeight.t:59", "clockRate.c:59");

		// 2. Define Taxon sets
		warning("2. Define taxon sets");
		f.selectTab("Taxon sets");
		beautiFrame.button("Guess").click();
		JOptionPaneFixture dialog = new JOptionPaneFixture(robot());
		//DialogFixture dialog = WindowFinder.findDialog("GuessTaxonSets").using(robot());
		dialog.radioButton("split on character").click();
		dialog.comboBox("splitCombo").selectItem("2");
		dialog.textBox("SplitChar2").deleteText().enterText("_");
		//JButton okButton = dialog.robot.finder().find(JButtonMatcher.withText("OK"));
		//new JButtonFixture(dialog.robot, okButton).click();
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "Guess_Taxonsets.png");
		dialog.okButton().click();
		printBeautiState(f);
		assertStateEquals("popSize", "Tree.t:Species", "birthRate.t:Species", "popMean", "Tree.t:26", "Tree.t:47", "clockRate.c:47", "Tree.t:59", "clockRate.c:59");
		assertOperatorsEqual("Reheight", "popSizeScaler", "updown.all", "YuleBirthRateScaler.t:Species", "popMeanScale", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "updown.47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "treeScaler.t:59", "treeRootScaler.t:59", "UniformOperator.t:59", "SubtreeSlide.t:59", "narrow.t:59", "wide.t:59", "WilsonBalding.t:59", "updown.59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.t:Species", "YuleBirthRatePrior.t:Species", "popMean.prior", "ClockPrior.c:47", "ClockPrior.c:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "speciescoalescent", "birthRate.t:Species", "YuleModel.t:Species", "TreeHeight.Species", "treeLikelihood.26", "treePrior.t:26", "TreeHeight.t:26", "treeLikelihood.47", "treePrior.t:47", "TreeHeight.t:47", "clockRate.c:47", "treeLikelihood.59", "treePrior.t:59", "TreeHeight.t:59", "clockRate.c:59");

		// 3. Set site model to HKY + empirical frequencies
		warning("3. Set site model to HKY + empirical frequencies");
		f.selectTab("Site Model");
		beautiFrame.comboBox().selectItem("HKY");
		JComboBoxFixture freqs = beautiFrame.comboBox("frequencies");
		freqs.selectItem("Empirical");
		beautiFrame.checkBox("mutationRate.isEstimated").check();
		JCheckBoxFixture fixMeanMutationRate = beautiFrame.checkBox("FixMeanMutationRate");
		fixMeanMutationRate.check();
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
	}

}
