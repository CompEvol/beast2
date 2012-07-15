package test.beast.app.beauti;



import static org.fest.swing.finder.JFileChooserFinder.findFileChooser;

import java.io.File;

import org.fest.swing.data.TableCell;
import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.junit.Test;


public class LinkUnlinkTest extends BeautiBase {

	@Test
	public void simpleLinkUnlinkTwoAlignmentTest() throws Exception {
		warning("Load gopher data 26.nex, 47.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"));
		
		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		warning("Link site models");
		f.selectTab("Partitions");
		beautiFrame.button("Link Site Models").click();
		printBeautiState(f);

		warning("Unlink site models");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Site Models").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "clockRate.c:47", "birthRate.t:47");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "allTipDatesRandomWalker.t:26", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "allTipDatesRandomWalker.t:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "strictClockUpDownOperator.c:47");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47");
		
		warning("Link clock models");
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();
		printBeautiState(f);

		warning("Unlink clock models");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Clock Models").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "clockRate.c:47", "birthRate.t:47");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "allTipDatesRandomWalker.t:26", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "allTipDatesRandomWalker.t:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "strictClockUpDownOperator.c:47");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47");

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Unlink trees");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Trees").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "clockRate.c:47", "birthRate.t:47");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "allTipDatesRandomWalker.t:26", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "allTipDatesRandomWalker.t:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "strictClockUpDownOperator.c:47");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47");

		makeSureXMLParses();
	}

	@Test
	public void simpleLinkUnlinkThreeAlignmentsTest() throws Exception {
		warning("Load gopher data 26.nex, 47.nex, 59.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"), new File("59.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "clockRate.c:47", "birthRate.t:47", "Tree.t:59", "clockRate.c:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "allTipDatesRandomWalker.t:26", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "allTipDatesRandomWalker.t:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "allTipDatesRandomWalker.t:59", "treeScaler.t:59", "treeRootScaler.t:59", "UniformOperator.t:59", "SubtreeSlide.t:59", "narrow.t:59", "wide.t:59", "WilsonBalding.t:59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "ClockPrior.c:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "clockRate.c:59", "YuleModel.t:59", "birthRate.t:59");

		warning("Link site models");
		f.selectTab("Partitions");
		beautiFrame.button("Link Site Models").click();
		printBeautiState(f);

		warning("Unlink site models");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Site Models").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "clockRate.c:47", "birthRate.t:47", "Tree.t:59", "clockRate.c:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "allTipDatesRandomWalker.t:26", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "allTipDatesRandomWalker.t:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "allTipDatesRandomWalker.t:59", "treeScaler.t:59", "treeRootScaler.t:59", "UniformOperator.t:59", "SubtreeSlide.t:59", "narrow.t:59", "wide.t:59", "WilsonBalding.t:59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "ClockPrior.c:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "clockRate.c:59", "YuleModel.t:59", "birthRate.t:59");
		
		warning("Link clock models");
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();
		printBeautiState(f);

		warning("Unlink clock models");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Clock Models").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "clockRate.c:47", "birthRate.t:47", "Tree.t:59", "clockRate.c:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "allTipDatesRandomWalker.t:26", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "allTipDatesRandomWalker.t:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "allTipDatesRandomWalker.t:59", "treeScaler.t:59", "treeRootScaler.t:59", "UniformOperator.t:59", "SubtreeSlide.t:59", "narrow.t:59", "wide.t:59", "WilsonBalding.t:59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "ClockPrior.c:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "clockRate.c:59", "YuleModel.t:59", "birthRate.t:59");

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Unlink trees");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Trees").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "clockRate.c:47", "birthRate.t:47", "Tree.t:59", "clockRate.c:59", "birthRate.t:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "ClockPrior.c:59", "YuleBirthRatePrior.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "allTipDatesRandomWalker.t:26", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "allTipDatesRandomWalker.t:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "allTipDatesRandomWalker.t:59", "treeScaler.t:59", "treeRootScaler.t:59", "UniformOperator.t:59", "SubtreeSlide.t:59", "narrow.t:59", "wide.t:59", "WilsonBalding.t:59", "strictClockUpDownOperator.c:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "clockRate.c:59", "YuleModel.t:59", "birthRate.t:59");

		makeSureXMLParses();
	}

	
	@Test
	public void linkTreesAndDeleteTest2a() throws Exception {
		warning("Load gopher data 26.nex, 47.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Delete second partition");
		f.selectTab("Partitions");
		beautiFrame.table().selectCell(TableCell.row(1).column(0));
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26");
		
		makeSureXMLParses();
	}
	
	@Test
	public void linkTreesAndDeleteTest2b() throws Exception {
		warning("Load gopher data 26.nex, 47.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Delete first partition");
		f.selectTab("Partitions");
		beautiFrame.table().selectCell(TableCell.row(0).column(0));
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26");
		
		makeSureXMLParses();
	}
	
	@Test
	public void linkTreesAndDeleteTest3() throws Exception {
		warning("Load gopher data 26.nex, 47.nex, 59.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"), new File("59.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "clockRate.c:47", "birthRate.t:47", "Tree.t:59", "clockRate.c:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "allTipDatesRandomWalker.t:26", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "allTipDatesRandomWalker.t:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "allTipDatesRandomWalker.t:59", "treeScaler.t:59", "treeRootScaler.t:59", "UniformOperator.t:59", "SubtreeSlide.t:59", "narrow.t:59", "wide.t:59", "WilsonBalding.t:59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "ClockPrior.c:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "clockRate.c:59", "YuleModel.t:59", "birthRate.t:59");

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "ClockPrior.c:47", "ClockPrior.c:59");
		makeSureXMLParses();

		warning("Delete second partition (47)");
		f.selectTab("Partitions");
		beautiFrame.table().selectCell(TableCell.row(1).column(0));
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "ClockPrior.c:59");
		makeSureXMLParses();

		warning("Delete first partition (26)");
		f.selectTab("Partitions");
		beautiFrame.table().selectCell(TableCell.row(0).column(0));
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26");

		makeSureXMLParses();
	}


	@Test
	public void linkTreesAndClocksAndDeleteTest() throws Exception {
		warning("Load gopher data 26.nex, 47.nex, 59.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"), new File("59.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "clockRate.c:47", "birthRate.t:47", "Tree.t:59", "clockRate.c:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "allTipDatesRandomWalker.t:26", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "allTipDatesRandomWalker.t:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "allTipDatesRandomWalker.t:59", "treeScaler.t:59", "treeRootScaler.t:59", "UniformOperator.t:59", "SubtreeSlide.t:59", "narrow.t:59", "wide.t:59", "WilsonBalding.t:59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "ClockPrior.c:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "clockRate.c:59", "YuleModel.t:59", "birthRate.t:59");

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Link clocks");
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();
		printBeautiState(f);

		warning("Delete second partition");
		f.selectTab("Partitions");
		beautiFrame.table().selectCell(TableCell.row(1).column(0));
		beautiFrame.button("-").click();
		printBeautiState(f);

		warning("Delete first partition");
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();
		beautiFrame.table().selectCell(TableCell.row(0).column(0));
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26");

		makeSureXMLParses();
	}

	@Test
	public void linkSiteModelsAndDeleteTest() throws Exception {
		warning("Load gopher data 26.nex, 47.nex, 59.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"), new File("59.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "clockRate.c:47", "birthRate.t:47", "Tree.t:59", "clockRate.c:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "allTipDatesRandomWalker.t:26", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "allTipDatesRandomWalker.t:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "allTipDatesRandomWalker.t:59", "treeScaler.t:59", "treeRootScaler.t:59", "UniformOperator.t:59", "SubtreeSlide.t:59", "narrow.t:59", "wide.t:59", "WilsonBalding.t:59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "ClockPrior.c:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "clockRate.c:59", "YuleModel.t:59", "birthRate.t:59");

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Link clocks");
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();
		printBeautiState(f);

		warning("Delete second partition");
		f.selectTab("Partitions");
		beautiFrame.table().selectCell(TableCell.row(1).column(0));
		beautiFrame.button("-").click();
		printBeautiState(f);

		warning("Delete first partition");
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();
		beautiFrame.table().selectCell(TableCell.row(0).column(0));
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26");

		makeSureXMLParses();
	}

}
