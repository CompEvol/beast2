package test.beast.app.beauti;




import beast.app.util.Utils;
import org.fest.swing.data.Index;
import org.fest.swing.data.TableCell;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.*;
import org.fest.swing.image.ScreenshotTaker;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;

import static org.fest.swing.edt.GuiActionRunner.execute;
import static org.fest.swing.finder.JFileChooserFinder.findFileChooser;



public class BeautiRateTutorialTest extends BeautiBase {
	// file used to store, then reload xml
	final static String XML_FILE = "rsv.xml";
	final static String PREFIX = "doc/tutorials/MEPs/figures/generated/BEAUti_";

	@Test
	public void MEPTutorial() throws Exception {
		long t0 = System.currentTimeMillis();
		ScreenshotTaker screenshotTaker = new ScreenshotTaker();
		beauti.frame.setSize(1024, 640);
		
		File dir = new File(PREFIX.substring(0, PREFIX.lastIndexOf('/')));
		if (!dir.exists()) {
			dir.mkdir();
		}
		for (File file : dir.listFiles()) {
			file.delete();
		}
		
		// 0. Load primate-mtDNA.nex
		warning("// 0. Load RSV2.nex");
		importAlignment("examples/nexus", new File("RSV2.nex"));

        beautiFrame.menuItemWithPath("Mode", "Automatic set fix mean substitution rate flag").click();

		// load anolis.nex
		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		f.requireVisible();
		f.requireTitle("Partitions", Index.atIndex(0));
		String[] titles = f.tabTitles();
		assertArrayEquals(titles,"[Partitions, Tip Dates, Site Model, Clock Model, Priors, MCMC]");
		System.err.println(Arrays.toString(titles));

		
		f = f.selectTab("Partitions");
		JTableFixture t = beautiFrame.table();
		t.selectCell(TableCell.row(0).column(2));
		
		//0. Split partition... 
		warning("0. Split partition...");
		beautiFrame.button("Split").click();
		JOptionPaneFixture dialog = new JOptionPaneFixture(robot());
		dialog.comboBox().selectItem("1 + 2 + 3 frame 3");
		dialog.okButton().click();
		// check table
		t = beautiFrame.table();
		printTableContents(t);
		checkTableContents(t, 
				"[RSV2_1, RSV2, 129, 209, nucleotide, RSV2_1, RSV2_1, RSV2_1, false]*" +
				"[RSV2_2, RSV2, 129, 210, nucleotide, RSV2_2, RSV2_2, RSV2_1, false]*" +
				"[RSV2_3, RSV2, 129, 210, nucleotide, RSV2_3, RSV2_3, RSV2_1, false]"
			);
		printBeautiState(f);
//		assertStateEquals("Tree.t:RSV2_2", "clockRate.c:RSV2_2", "birthRate.t:RSV2_2", "Tree.t:RSV2_3", "clockRate.c:RSV2_3", "birthRate.t:RSV2_3", "Tree.t:RSV2_1", "birthRate.t:RSV2_1");
//		assertOperatorsEqual("StrictClockRateScaler.c:RSV2_2", "YuleBirthRateScaler.t:RSV2_2", "YuleModelTreeScaler.t:RSV2_2", "YuleModelTreeRootScaler.t:RSV2_2", "YuleModelUniformOperator.t:RSV2_2", "YuleModelSubtreeSlide.t:RSV2_2", "YuleModelNarrow.t:RSV2_2", "YuleModelWide.t:RSV2_2", "YuleModelWilsonBalding.t:RSV2_2", "strictClockUpDownOperator.c:RSV2_2", "StrictClockRateScaler.c:RSV2_3", "YuleBirthRateScaler.t:RSV2_3", "YuleModelTreeScaler.t:RSV2_3", "YuleModelTreeRootScaler.t:RSV2_3", "YuleModelUniformOperator.t:RSV2_3", "YuleModelSubtreeSlide.t:RSV2_3", "YuleModelNarrow.t:RSV2_3", "YuleModelWide.t:RSV2_3", "YuleModelWilsonBalding.t:RSV2_3", "strictClockUpDownOperator.c:RSV2_3", "YuleBirthRateScaler.t:RSV2_1", "YuleModelTreeScaler.t:RSV2_1", "YuleModelTreeRootScaler.t:RSV2_1", "YuleModelUniformOperator.t:RSV2_1", "YuleModelSubtreeSlide.t:RSV2_1", "YuleModelNarrow.t:RSV2_1", "YuleModelWide.t:RSV2_1", "YuleModelWilsonBalding.t:RSV2_1");
//		assertPriorsEqual("YuleModel.t:RSV2_1", "YuleModel.t:RSV2_2", "YuleModel.t:RSV2_3", "ClockPrior.c:RSV2_2", "YuleBirthRatePrior.t:RSV2_2", "ClockPrior.c:RSV2_3", "YuleBirthRatePrior.t:RSV2_3", "YuleBirthRatePrior.t:RSV2_1");
//		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "TreeHeight.t:RSV2_2", "clockRate.c:RSV2_2", "YuleModel.t:RSV2_2", "birthRate.t:RSV2_2", "treeLikelihood.RSV2_3", "TreeHeight.t:RSV2_3", "clockRate.c:RSV2_3", "YuleModel.t:RSV2_3", "birthRate.t:RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:RSV2_1", "YuleModel.t:RSV2_1", "birthRate.t:RSV2_1");

		assertStateEquals("Tree.t:RSV2_1", "clockRate.c:RSV2_2", "birthRate.t:RSV2_1", "clockRate.c:RSV2_3");
		assertOperatorsEqual("StrictClockRateScaler.c:RSV2_2", "YuleBirthRateScaler.t:RSV2_1", "strictClockUpDownOperator.c:RSV2_2", "YuleModelTreeScaler.t:RSV2_1", "YuleModelTreeRootScaler.t:RSV2_1", "YuleModelUniformOperator.t:RSV2_1", "YuleModelSubtreeSlide.t:RSV2_1", "YuleModelNarrow.t:RSV2_1", "YuleModelWide.t:RSV2_1", "YuleModelWilsonBalding.t:RSV2_1", "StrictClockRateScaler.c:RSV2_3", "strictClockUpDownOperator.c:RSV2_3");
		assertPriorsEqual("YuleModel.t:RSV2_1", "ClockPrior.c:RSV2_2", "YuleBirthRatePrior.t:RSV2_1", "ClockPrior.c:RSV2_3");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "treeLikelihood.RSV2_3", "TreeHeight.t:RSV2_1", "clockRate.c:RSV2_2", "YuleModel.t:RSV2_1", "birthRate.t:RSV2_1", "treeLikelihood.RSV2_1", "clockRate.c:RSV2_3");
	
		//1a. Link trees... 
		warning("1a. Link trees...");
		f.selectTab("Partitions");
		t = beautiFrame.table();
		t.selectCells(TableCell.row(0).column(2), TableCell.row(1).column(2), TableCell.row(2).column(2));
		JButtonFixture linkTreesButton = beautiFrame.button("Link Trees");
		linkTreesButton.click();
		printBeautiState(f);

		//1b. ...and call the tree "tree"
		warning("1b. ...and call the tree \"tree\"");
		f.selectTab("Partitions");
		JTableCellFixture cell = beautiFrame.table().cell(TableCell.row(0).column(7));
		Component editor = cell.editor();
		JComboBoxFixture comboBox = new JComboBoxFixture(robot(), (JComboBox<?>) editor);
		cell.startEditing();
		comboBox.selectAllText();
		comboBox.enterText("tree");
		comboBox.pressAndReleaseKeys(KeyEvent.VK_ENTER);
		cell.stopEditing();
		printBeautiState(f);
		assertStateEquals("clockRate.c:RSV2_2", "clockRate.c:RSV2_3", "Tree.t:tree", "birthRate.t:tree");
		assertOperatorsEqual("StrictClockRateScaler.c:RSV2_2", "StrictClockRateScaler.c:RSV2_3", "YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "strictClockUpDownOperator.c:RSV2_3", "strictClockUpDownOperator.c:RSV2_2");
		assertPriorsEqual("YuleModel.t:tree", "ClockPrior.c:RSV2_2", "ClockPrior.c:RSV2_3", "YuleBirthRatePrior.t:tree");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "clockRate.c:RSV2_2", "treeLikelihood.RSV2_3", "clockRate.c:RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree");

		
		//2a. Link clocks 
		warning("2a. Link clocks");
		f.selectTab("Partitions");
		t = beautiFrame.table();
		t.selectCells(TableCell.row(0).column(2), TableCell.row(1).column(2), TableCell.row(2).column(2));
		JButtonFixture linkClocksButton = beautiFrame.button("Link Clock Models");
		linkClocksButton.click();

		//2b. and call the uncorrelated relaxed molecular clock "clock"
		warning("2b. and call the uncorrelated relaxed molecular clock \"clock\"");
		cell = beautiFrame.table().cell(TableCell.row(0).column(6));
		editor = cell.editor();
		comboBox = new JComboBoxFixture(robot(), (JComboBox<?>) editor);
		cell.startEditing();
		comboBox.selectAllText();
		comboBox.enterText("clock");
		comboBox.pressAndReleaseKeys(KeyEvent.VK_ENTER);
		cell.stopEditing();
		printBeautiState(f);
		assertStateEquals("Tree.t:tree", "birthRate.t:tree");
		assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree");
		assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "treeLikelihood.RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree");

		
		//3a. Link site models
		warning("3a. link site models");
		f.selectTab("Partitions");
		t = beautiFrame.table();
		t.selectCells(TableCell.row(0).column(2), TableCell.row(1).column(2), TableCell.row(2).column(2));
		JButtonFixture linkSiteModelsButton = beautiFrame.button("Link Site Models");
		linkSiteModelsButton.click();
		
		//3b. Set the site model to HKY (empirical)
		warning("3b. Set the site model to HKY (empirical)");
		f.selectTab("Site Model");
		beautiFrame.comboBox("substModel").selectItem("HKY");
		JComboBoxFixture freqs = beautiFrame.comboBox("frequencies");
		freqs.selectItem("Empirical");
		beautiFrame.checkBox("mutationRate.isEstimated").check();
		JCheckBoxFixture fixMeanMutationRate = beautiFrame.checkBox("FixMeanMutationRate");
		fixMeanMutationRate.check();
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "Site_Model.png");
		printBeautiState(f);
		
		//3c. Unlink site models
		warning("3c. unlink site models");
		f.selectTab("Partitions");
		t = beautiFrame.table();
		t.selectCells(TableCell.row(0).column(2), TableCell.row(1).column(2), TableCell.row(2).column(2));
		JButtonFixture unlinkSiteModelsButton = beautiFrame.button("Unlink Site Models");
		unlinkSiteModelsButton.click();
		printBeautiState(f);
		assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3");
		assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "KappaScaler.s:RSV2_1", "KappaScaler.s:RSV2_2", "KappaScaler.s:RSV2_3", "FixMeanMutationRatesOperator");
		assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "KappaPrior.s:RSV2_1", "KappaPrior.s:RSV2_2", "KappaPrior.s:RSV2_3");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "treeLikelihood.RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3");
		
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "partition.png");
		
		//4. set up tip dates
		f = f.selectTab("Tip Dates");
		warning("4. Seting up tip dates");
		beautiFrame.checkBox().click();
		beautiFrame.button("Guess").click();
		JOptionPaneFixture dialog2 = new JOptionPaneFixture(robot());
		dialog2.textBox("SplitChar").deleteText().enterText("s");
		screenshotTaker.saveComponentAsPng(dialog2.component(), PREFIX + "GuessDates.png");
		dialog2.comboBox("delimiterCombo").selectItem("after last");		
		dialog2.okButton().click();
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "dates.png");
		printBeautiState(f);
		assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3", "clockRate.c:clock");
		assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "KappaScaler.s:RSV2_1", "KappaScaler.s:RSV2_2", "KappaScaler.s:RSV2_3", "FixMeanMutationRatesOperator", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock");
		assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "KappaPrior.s:RSV2_1", "KappaPrior.s:RSV2_2", "KappaPrior.s:RSV2_3", "ClockPrior.c:clock");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "treeLikelihood.RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3", "clockRate.c:clock");
		
		//5. Change tree prior to Coalescent with constant pop size 
		warning("5a. Change tree prior to Coalescent with constant pop size");
		f.selectTab("Priors");
		beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Constant Population");
		
		warning("5b. Change clock prior to Log Normal with M = -5, S = 1.25");
		beautiFrame.comboBox("clockRate.c:clock.distr").selectItem("Log Normal");
		beautiFrame.button("ClockPrior.c:clock.editButton").click();
		beautiFrame.textBox("M").selectAll().setText("-5");
		beautiFrame.textBox("S").selectAll().setText("1.25");
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "priors.png");
		printBeautiState(f);
		assertStateEquals("Tree.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3", "clockRate.c:clock", "popSize.t:tree");
		assertOperatorsEqual("CoalescentConstantTreeScaler.t:tree", "CoalescentConstantTreeRootScaler.t:tree", "CoalescentConstantUniformOperator.t:tree", "CoalescentConstantSubtreeSlide.t:tree", "CoalescentConstantNarrow.t:tree", "CoalescentConstantWide.t:tree", "CoalescentConstantWilsonBalding.t:tree", "KappaScaler.s:RSV2_1", "KappaScaler.s:RSV2_2", "KappaScaler.s:RSV2_3", "FixMeanMutationRatesOperator", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock", "PopSizeScaler.t:tree");
		assertPriorsEqual("CoalescentConstant.t:tree", "ClockPrior.c:clock", "KappaPrior.s:RSV2_1", "KappaPrior.s:RSV2_2", "KappaPrior.s:RSV2_3", "PopSizePrior.t:tree");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "treeLikelihood.RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3", "clockRate.c:clock", "popSize.t:tree", "CoalescentConstant.t:tree");


		//6. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
		warning("6. Setting up MCMC parameters");
		f = f.selectTab("MCMC");
		beautiFrame.textBox("chainLength").selectAll().setText("2000000");
		beautiFrame.button("tracelog.editButton").click();
		beautiFrame.textBox("logEvery").selectAll().setText("400");
		beautiFrame.button("tracelog.editButton").click();
		

		beautiFrame.button("treelog.t:tree.editButton").click();
		beautiFrame.textBox("logEvery").selectAll().setText("400");
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "mcmc.png");
		beautiFrame.button("treelog.t:tree.editButton").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3", "clockRate.c:clock", "popSize.t:tree");
		assertOperatorsEqual("CoalescentConstantTreeScaler.t:tree", "CoalescentConstantTreeRootScaler.t:tree", "CoalescentConstantUniformOperator.t:tree", "CoalescentConstantSubtreeSlide.t:tree", "CoalescentConstantNarrow.t:tree", "CoalescentConstantWide.t:tree", "CoalescentConstantWilsonBalding.t:tree", "KappaScaler.s:RSV2_1", "KappaScaler.s:RSV2_2", "KappaScaler.s:RSV2_3", "FixMeanMutationRatesOperator", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock", "PopSizeScaler.t:tree");
		assertPriorsEqual("CoalescentConstant.t:tree", "ClockPrior.c:clock", "KappaPrior.s:RSV2_1", "KappaPrior.s:RSV2_2", "KappaPrior.s:RSV2_3", "PopSizePrior.t:tree");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "treeLikelihood.RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3", "clockRate.c:clock", "popSize.t:tree", "CoalescentConstant.t:tree");
		
		
		//7. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
		warning("7. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree");
		makeSureXMLParses();

		long t1 = System.currentTimeMillis();
		System.err.println("total time: " + (t1 - t0)/1000 + " seconds");
		
	}

	@Test
	public void MEPBSPTutorial() throws InterruptedException {
		if (true) {return;}
		try {
		long t0 = System.currentTimeMillis();
		ScreenshotTaker screenshotTaker = new ScreenshotTaker();
		beauti.frame.setSize(1024, 640);

		// 1. reaload XML file
		warning("1. reload rsv.xml");
		String dir = "" + org.fest.util.Files.temporaryFolder();
		String file = XML_FILE;
		
		if (!Utils.isMac()) {
			beautiFrame.menuItemWithPath("File", "Load").click();
			JFileChooserFixture fileChooser = findFileChooser().using(robot());
			fileChooser.setCurrentDirectory(new File(dir));
			fileChooser.selectFile(new File(file)).approve();
		} else {
			_file = new File(dir + "/" + file);
			execute(new GuiTask() {
		        @Override
				protected void executeInEDT() {
	                doc.newAnalysis();
	                doc.setFileName(_file.getAbsolutePath());
	                try {
		                doc.loadXML(new File(doc.getFileName()));
		        	} catch (Exception e) {
						e.printStackTrace();
					}
		        }
		    });
							
		}
		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		// 2. change tree prior to BSP
		warning("2. change tree prior to BSP");
		f.selectTab("Priors");
		beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Bayesian Skyline");
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "priors2.png");
		printBeautiState(f);
		
		// 3. change tree prior to BSP
		warning("3. change group and population size parameters");
		beautiFrame.menuItemWithPath("View","Show Initialization panel").click();
		
		beautiFrame.button("isPopSizes.t:tree.editButton").click();
		beautiFrame.textBox("dimension").selectAll().setText("3");
		beautiFrame.button("isPopSizes.t:tree.editButton").click();
		
		beautiFrame.button("isGroupSizes.t:tree.editButton").click();
		beautiFrame.textBox("dimension").selectAll().setText("3");
		screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "init.png");
		printBeautiState(f);
		
		// 4. set chain-length to 10M, log every 10K
		warning("4. set chain-length to 10M, log every 10K");
		f = f.selectTab("MCMC");
		beautiFrame.textBox("chainLength").selectAll().setText("10000000");
		beautiFrame.button("tracelog.editButton").click();
		beautiFrame.textBox("logEvery").selectAll().setText("10000");
		beautiFrame.button("tracelog.editButton").click();
		

		beautiFrame.button("treelog.t:tree.editButton").click();
		beautiFrame.textBox("logEvery").selectAll().setText("10000");
		beautiFrame.button("treelog.t:tree.editButton").click();
		printBeautiState(f);
		
		// 5. save XML file
		warning("5. save XML file");
		File fout = new File(org.fest.util.Files.temporaryFolder() + "/" + XML_FILE);
		if (fout.exists()) {
			fout.delete();
		}
		saveFile(""+org.fest.util.Files.temporaryFolder(), XML_FILE);

		//4. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
		makeSureXMLParses();
		long t1 = System.currentTimeMillis();
		System.err.println("total time: " + (t1 - t0)/1000 + " seconds");
		} catch (Exception e) {
			e.printStackTrace();
		
		}
	}
	

}

