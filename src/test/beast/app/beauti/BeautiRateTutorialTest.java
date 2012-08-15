package test.beast.app.beauti;



import static org.fest.assertions.Assertions.assertThat;

import java.awt.Component;
import java.io.File;
import java.util.Arrays;

import javax.naming.CommunicationException;
import javax.swing.JButton;
import javax.swing.JComboBox;

import org.fest.swing.data.Index;
import org.fest.swing.data.TableCell;
import org.fest.swing.exception.ComponentLookupException;
import org.fest.swing.finder.WindowFinder;
import org.fest.swing.fixture.DialogFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.fest.swing.fixture.JCheckBoxFixture;
import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.fixture.JListFixture;
import org.fest.swing.fixture.JOptionPaneFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.fixture.JTableCellFixture;
import org.fest.swing.fixture.JTableFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.junit.Test;

import beast.app.beauti.TaxonSetDialog;

public class BeautiRateTutorialTest extends BeautiBase {

	@Test
	public void DivergenceDatingTutorial() throws InterruptedException {
		long t0 = System.currentTimeMillis();
		
		// 0. Load primate-mtDNA.nex
		warning("// 0. Load RSV2.nex");
		importAlignment("examples/nexus", new File("RSV2.nex"));

		// load anolis.nex
		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		f.requireVisible();
		f.requireTitle("Partitions", Index.atIndex(0));
		String[] titles = f.tabTitles();
		assertArrayEquals(titles,"[Partitions, Tip Dates, Site Model, Clock Model, Priors, MCMC]");
		System.err.println(Arrays.toString(titles));

		
		f = f.selectTab("Partitions");
				
		beautiFrame.button("Split").click();
		JOptionPaneFixture dialog = new JOptionPaneFixture(robot());
		dialog.comboBox().selectItem("{1,2} + 3");
		dialog.okButton().click();
		// check table
		JTableFixture t = beautiFrame.table();
		printTableContents(t);
		checkTableContents(t, 
				"[RSV2_1,2, RSV2, 129, 420, nucleotide, RSV2_1,2, RSV2_1,2, RSV2_1,2]*" +
				"[RSV2_3, RSV2, 129, 209, nucleotide, RSV2_3, RSV2_3, RSV2_3]"
			);
		printBeautiState(f);
		assertStateEquals("Tree.t:RSV2_1,2", "birthRate.t:RSV2_1,2", "Tree.t:RSV2_3", "clockRate.c:RSV2_3", "birthRate.t:RSV2_3");
		assertOperatorsEqual("YuleBirthRateScaler.t:RSV2_1,2", "treeScaler.t:RSV2_1,2", "treeRootScaler.t:RSV2_1,2", "UniformOperator.t:RSV2_1,2", "SubtreeSlide.t:RSV2_1,2", "narrow.t:RSV2_1,2", "wide.t:RSV2_1,2", "WilsonBalding.t:RSV2_1,2", "StrictClockRateScaler.c:RSV2_3", "YuleBirthRateScaler.t:RSV2_3", "treeScaler.t:RSV2_3", "treeRootScaler.t:RSV2_3", "UniformOperator.t:RSV2_3", "SubtreeSlide.t:RSV2_3", "narrow.t:RSV2_3", "wide.t:RSV2_3", "WilsonBalding.t:RSV2_3", "strictClockUpDownOperator.c:RSV2_3");
		assertPriorsEqual("YuleModel.t:RSV2_1,2", "YuleModel.t:RSV2_3", "YuleBirthRatePrior.t:RSV2_1,2", "ClockPrior.c:RSV2_3", "YuleBirthRatePrior.t:RSV2_3");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_1,2", "TreeHeight.t:RSV2_1,2", "YuleModel.t:RSV2_1,2", "birthRate.t:RSV2_1,2", "treeLikelihood.RSV2_3", "TreeHeight.t:RSV2_3", "clockRate.c:RSV2_3", "YuleModel.t:RSV2_3", "birthRate.t:RSV2_3");


		//2a. Link trees... 
		warning("2a. Link trees...");
		f.selectTab("Partitions");
		t = beautiFrame.table();
		t.selectCells(TableCell.row(0).column(0), TableCell.row(1).column(0));
		JButtonFixture linkTreesButton = beautiFrame.button("Link Trees");
		linkTreesButton.click();
		printBeautiState(f);
		assertStateEquals("Tree.t:RSV2_1,2", "birthRate.t:RSV2_1,2", "clockRate.c:RSV2_3");
		assertOperatorsEqual("YuleBirthRateScaler.t:RSV2_1,2", "treeScaler.t:RSV2_1,2", "treeRootScaler.t:RSV2_1,2", "UniformOperator.t:RSV2_1,2", "SubtreeSlide.t:RSV2_1,2", "narrow.t:RSV2_1,2", "wide.t:RSV2_1,2", "WilsonBalding.t:RSV2_1,2", "StrictClockRateScaler.c:RSV2_3", "strictClockUpDownOperator.c:RSV2_3");
		assertPriorsEqual("YuleModel.t:RSV2_1,2", "YuleBirthRatePrior.t:RSV2_1,2", "ClockPrior.c:RSV2_3");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_1,2", "TreeHeight.t:RSV2_1,2", "YuleModel.t:RSV2_1,2", "birthRate.t:RSV2_1,2", "treeLikelihood.RSV2_3", "clockRate.c:RSV2_3");

		//2b. ...and call the tree "tree"
		warning("2b. ...and call the tree \"tree\"");
		f.selectTab("Partitions");
		JTableCellFixture cell = beautiFrame.table().cell(TableCell.row(0).column(7));
		Component editor = cell.editor();
		JComboBoxFixture comboBox = new JComboBoxFixture(robot(), (JComboBox) editor);
		cell.startEditing();
		comboBox.selectAllText();
		comboBox.enterText("tree");
		cell.stopEditing();
//		checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, noncoding, tree]*" +
//				"[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, 1stpos, tree]*" +
//				"[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, 2ndpos, tree]*" +
//				"[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, 3rdpos, tree]");
		printBeautiState(f);
		assertStateEquals("Tree.t:tree", "birthRate.t:tree", "clockRate.c:RSV2_3");
		assertOperatorsEqual("YuleBirthRateScaler.t:tree", "treeScaler.t:tree", "treeRootScaler.t:tree", "UniformOperator.t:tree", "SubtreeSlide.t:tree", "narrow.t:tree", "wide.t:tree", "WilsonBalding.t:tree", "StrictClockRateScaler.c:RSV2_3", "strictClockUpDownOperator.c:RSV2_3");
		assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "ClockPrior.c:RSV2_3");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_1,2", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "treeLikelihood.RSV2_3", "clockRate.c:RSV2_3");


		
		//3a. Link clocks 
		warning("3a. Link clocks");
		f.selectTab("Partitions");
		t = beautiFrame.table();
		t.selectCells(TableCell.row(0).column(0), TableCell.row(1).column(0));
		JButtonFixture linkClocksButton = beautiFrame.button("Link Clock Models");
		linkClocksButton.click();

		//3b. and call the uncorrelated relaxed molecular clock "clock"
		warning("3b. and call the uncorrelated relaxed molecular clock \"clock\"");
		cell = beautiFrame.table().cell(TableCell.row(0).column(6));
		editor = cell.editor();
		comboBox = new JComboBoxFixture(robot(), (JComboBox) editor);
		cell.startEditing();
		comboBox.selectAllText();
		comboBox.enterText("clock");
		cell.stopEditing();
		printBeautiState(f);
//		checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree]*" +
//				"[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, clock, tree]*" +
//				"[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, clock, tree]*" +
//				"[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, clock, tree]");
		assertStateEquals("Tree.t:tree", "birthRate.t:tree");
		assertOperatorsEqual("YuleBirthRateScaler.t:tree", "treeScaler.t:tree", "treeRootScaler.t:tree", "UniformOperator.t:tree", "SubtreeSlide.t:tree", "narrow.t:tree", "wide.t:tree", "WilsonBalding.t:tree");
		assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_1,2", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "treeLikelihood.RSV2_3");

		
		//4. set up tip dates
		f = f.selectTab("Tip Dates");
		warning("4. Seting up tip dates");
		beautiFrame.checkBox().click();
		beautiFrame.button("Guess").click();
		JOptionPaneFixture dialog2 = new JOptionPaneFixture(robot());
		dialog2.textBox("SplitChar").deleteText().enterText("s");
		dialog2.okButton().click();
		printBeautiState(f);
		assertStateEquals("Tree.t:tree", "birthRate.t:tree", "clockRate.c:clock");
		assertOperatorsEqual("YuleBirthRateScaler.t:tree", "treeScaler.t:tree", "treeRootScaler.t:tree", "UniformOperator.t:tree", "SubtreeSlide.t:tree", "narrow.t:tree", "wide.t:tree", "WilsonBalding.t:tree", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock");
		assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "ClockPrior.c:clock");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_1,2", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "treeLikelihood.RSV2_3", "clockRate.c:clock");

		
		//5. Set the site model to HKY+G4 (estimated)
		warning("5. Set the site model to HKY (estimated)");
		f.selectTab("Site Model");
		beautiFrame.comboBox().selectItem("HKY");

		beautiFrame.list().selectItem("RSV2_3");
		beautiFrame.comboBox().selectItem("HKY");
		printBeautiState(f);
		assertStateEquals("Tree.t:tree", "birthRate.t:tree", "clockRate.c:clock", "kappa.s:RSV2_1,2", "kappa.s:RSV2_3", "freqParameter.s:RSV2_3", "freqParameter.s:RSV2_1,2");
		assertOperatorsEqual("YuleBirthRateScaler.t:tree", "treeScaler.t:tree", "treeRootScaler.t:tree", "UniformOperator.t:tree", "SubtreeSlide.t:tree", "narrow.t:tree", "wide.t:tree", "WilsonBalding.t:tree", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock", "KappaScaler.s:RSV2_1,2", "KappaScaler.s:RSV2_3", "FrequenciesExchanger.s:RSV2_3", "FrequenciesExchanger.s:RSV2_1,2");
		assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "ClockPrior.c:clock", "KappaPrior.s:RSV2_1,2", "KappaPrior.s:RSV2_3");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_1,2", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "treeLikelihood.RSV2_3", "clockRate.c:clock", "kappa.s:RSV2_1,2", "kappa.s:RSV2_3", "freqParameter.s:RSV2_3", "freqParameter.s:RSV2_1,2");
		
		
		//7. Change tree prior to Coalescent with constant pop size 
		warning("7. Change tree prior to Coalescent with constant pop size");
		f.selectTab("Priors");
		beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Constant Population");
		
		warning("8. Change clock prior to Log Normal with M = -5, S = 1.25");
		beautiFrame.comboBox("clockRate.c:clock.distr").selectItem("Log Normal");
		beautiFrame.button("ClockPrior.c:clock.editButton").click();
		beautiFrame.textBox("M").selectAll().setText("-5");
		beautiFrame.textBox("S").selectAll().setText("1.25");
		printBeautiState(f);
		assertStateEquals("Tree.t:tree", "clockRate.c:clock", "kappa.s:RSV2_1,2", "kappa.s:RSV2_3", "popSize.t:tree", "freqParameter.s:RSV2_1,2", "freqParameter.s:RSV2_3");
		assertOperatorsEqual("treeScaler.t:tree", "treeRootScaler.t:tree", "UniformOperator.t:tree", "SubtreeSlide.t:tree", "narrow.t:tree", "wide.t:tree", "WilsonBalding.t:tree", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock", "KappaScaler.s:RSV2_1,2", "KappaScaler.s:RSV2_3", "PopSizeScaler.t:tree", "FrequenciesExchanger.s:RSV2_1,2", "FrequenciesExchanger.s:RSV2_3");
		assertPriorsEqual("CoalescentConstant.t:tree", "ClockPrior.c:clock", "KappaPrior.s:RSV2_1,2", "KappaPrior.s:RSV2_3", "PopSizePrior.t:tree");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_1,2", "TreeHeight.t:tree", "treeLikelihood.RSV2_3", "clockRate.c:clock", "kappa.s:RSV2_1,2", "kappa.s:RSV2_3", "popSize.t:tree", "CoalescentConstant.t:tree", "freqParameter.s:RSV2_1,2", "freqParameter.s:RSV2_3");


		//9. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
		warning("8. Setting up MCMC parameters");
		f = f.selectTab("MCMC");
		beautiFrame.textBox("chainLength").selectAll().setText("2000000");
		beautiFrame.button("tracelog.editButton").click();
		beautiFrame.textBox("logEvery").selectAll().setText("400");
		beautiFrame.button("tracelog.editButton").click();
		

		beautiFrame.button("treelog.t:tree.editButton").click();
		beautiFrame.textBox("logEvery").selectAll().setText("400");
		beautiFrame.button("treelog.t:tree.editButton").click();
		printBeautiState(f);
		
		
		//9. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
		warning("9. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree");
		File fout = new File(org.fest.util.Files.temporaryFolder() + "/RSV2.xml");
		if (fout.exists()) {
			fout.delete();
		}
		makeSureXMLParses();
		
		long t1 = System.currentTimeMillis();
		System.err.println("total time: " + (t1 - t0)/1000 + " seconds");
		
	}




}

