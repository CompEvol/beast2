package test.beast.app.beauti;




import java.io.File;

import org.fest.swing.data.TableCell;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.fixture.JTableFixture;
import org.junit.Test;


public class LinkUnlinkTest extends BeautiBase {

	@Test
	public void simpleLinkUnlinkTwoAlignmentTest() throws Exception {
		warning("Load gopher data 26.nex, 47.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"));
		
		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		JTableFixture t = beautiFrame.table();
		t.selectCells(TableCell.row(0).column(1), TableCell.row(1).column(1));
		
		warning("Link site models");
		f.selectTab("Partitions");
		beautiFrame.button("Link Site Models").click();
		printBeautiState(f);

		warning("Unlink site models");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Site Models").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "clockRate.c:47", "birthRate.t:47");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "strictClockUpDownOperator.c:47");
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
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "strictClockUpDownOperator.c:47");
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
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "strictClockUpDownOperator.c:47");
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
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "ClockPrior.c:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "clockRate.c:59", "YuleModel.t:59", "birthRate.t:59");

		JTableFixture t = beautiFrame.table();
		t.selectCells(TableCell.row(0).column(1), TableCell.row(1).column(1), TableCell.row(2).column(1));

		warning("Link site models");
		f.selectTab("Partitions");
		beautiFrame.button("Link Site Models").click();
		printBeautiState(f);

		warning("Unlink site models");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Site Models").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "clockRate.c:47", "birthRate.t:47", "Tree.t:59", "clockRate.c:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59", "strictClockUpDownOperator.c:59");
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
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59", "strictClockUpDownOperator.c:59");
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
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59", "strictClockUpDownOperator.c:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "clockRate.c:59", "YuleModel.t:59", "birthRate.t:59");

		makeSureXMLParses();
	}

	
	@Test
	public void linkTreesAndDeleteTest2a() throws Exception {
		warning("Load gopher data 26.nex, 47.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		JTableFixture t = beautiFrame.table();
		t.selectCells(TableCell.row(0).column(1), TableCell.row(1).column(1));

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Delete second partition");
		f.selectTab("Partitions");
		beautiFrame.table().selectCell(TableCell.row(1).column(1));
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

		JTableFixture t = beautiFrame.table();
		t.selectCells(TableCell.row(0).column(1), TableCell.row(1).column(1));

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Delete first partition");
		f.selectTab("Partitions");
		beautiFrame.table().selectCell(TableCell.row(0).column(1));
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
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "ClockPrior.c:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "clockRate.c:59", "YuleModel.t:59", "birthRate.t:59");

		JTableFixture t = beautiFrame.table();
		t.selectCells(TableCell.row(0).column(1), TableCell.row(1).column(1), TableCell.row(2).column(1));

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "ClockPrior.c:47", "ClockPrior.c:59");
		makeSureXMLParses();

		warning("Delete second partition (47)");
		f.selectTab("Partitions");
		beautiFrame.table().selectCell(TableCell.row(1).column(1));
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "ClockPrior.c:59");
		makeSureXMLParses();

		warning("Delete first partition (26)");
		f.selectTab("Partitions");
		beautiFrame.table().selectCell(TableCell.row(0).column(1));
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
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "ClockPrior.c:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "clockRate.c:59", "YuleModel.t:59", "birthRate.t:59");

		JTableFixture t = beautiFrame.table();
		t.selectCells(TableCell.row(0).column(1), TableCell.row(1).column(1), TableCell.row(2).column(1));

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Link clocks");
		t.selectCells(TableCell.row(0).column(1), TableCell.row(1).column(1), TableCell.row(2).column(1));
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();
		printBeautiState(f);

		warning("Delete second partition");
		f.selectTab("Partitions");
		beautiFrame.table().selectCell(TableCell.row(1).column(1));
		beautiFrame.button("-").click();
		printBeautiState(f);

		warning("Delete first partition");
		f.selectTab("Partitions");
		beautiFrame.table().selectCells(TableCell.row(0).column(1), TableCell.row(1).column(1));
		beautiFrame.button("Link Clock Models").click();
		beautiFrame.table().selectCell(TableCell.row(0).column(1));
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
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "StrictClockRateScaler.c:47", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "ClockPrior.c:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "ClockPrior.c:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "clockRate.c:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "clockRate.c:59", "YuleModel.t:59", "birthRate.t:59");

		JTableFixture t = beautiFrame.table();
		t.selectCells(TableCell.row(0).column(1), TableCell.row(1).column(1), TableCell.row(2).column(1));

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Link clocks");
		t.selectCells(TableCell.row(0).column(1), TableCell.row(1).column(1), TableCell.row(2).column(1));
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();
		printBeautiState(f);

		warning("Delete second partition");
		f.selectTab("Partitions");
		beautiFrame.table().selectCell(TableCell.row(1).column(1));
		beautiFrame.button("-").click();
		printBeautiState(f);

		warning("Delete first partition");
		f.selectTab("Partitions");
		beautiFrame.table().selectCells(TableCell.row(0).column(1), TableCell.row(1).column(1));
		beautiFrame.button("Link Clock Models").click();
		beautiFrame.table().selectCell(TableCell.row(0).column(1));
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26");

		makeSureXMLParses();
	}
}
