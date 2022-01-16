package test.beast.app.beauti;




import static org.fest.swing.edt.GuiActionRunner.execute;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.fest.assertions.Assertions;
import org.fest.swing.data.TableCell;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.fixture.JTableFixture;
import org.junit.Test;

import beast.app.util.Utils;
import beast.core.BEASTInterface;
import beast.core.Distribution;
import beast.core.Function;
import beast.core.parameter.Parameter;
import beast.core.util.CompoundDistribution;
import beast.math.distributions.Prior;

public class LinkUnlinkTest extends BeautiBase {

	/** robustly select rows -- don't give up after first attempt **/
	private void selectRows(int ... rows) {
		JTableFixture t = beautiFrame.table();
		for (int attempt = 0; attempt < 5; attempt++) {
			t.selectRows(rows);
			if (t.target.getSelectedRowCount() == rows.length) {
				return;
			}
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				
			}
		}
	}

	
	@Test
	public void simpleLinkUnlinkTwoAlignmentTest() throws Exception {
		warning("Load gopher data 26.nex, 47.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"));
		
		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		selectRows(0, 1);
		
		warning("Link site models");
		f.selectTab("Partitions");
		beautiFrame.button("Link Site Models").click();
		printBeautiState(f);

		warning("Unlink site models");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Site Models").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47",  "birthRate.t:47");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "YuleBirthRatePrior.t:47");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "YuleModel.t:47", "birthRate.t:47");
		
		warning("Link clock models");
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();
		printBeautiState(f);

		warning("Unlink clock models");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Clock Models").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47",  "birthRate.t:47");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "YuleBirthRatePrior.t:47");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "YuleModel.t:47", "birthRate.t:47");

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Unlink trees");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Trees").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47",  "birthRate.t:47");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "YuleBirthRatePrior.t:47");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "YuleModel.t:47", "birthRate.t:47");

		makeSureXMLParses();
	}

	@Test
	public void simpleLinkUnlinkThreeAlignmentsTest() throws Exception {
		warning("Load gopher data 26.nex, 47.nex, 59.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"), new File("59.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "birthRate.t:47", "Tree.t:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "YuleModel.t:59", "birthRate.t:59");

		selectRows(0, 1, 2);

		warning("Link site models");
		f.selectTab("Partitions");
		beautiFrame.button("Link Site Models").click();
		printBeautiState(f);

		warning("Unlink site models");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Site Models").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "birthRate.t:47", "Tree.t:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "YuleModel.t:59", "birthRate.t:59");
		
		warning("Link clock models");
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();
		printBeautiState(f);

		warning("Unlink clock models");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Clock Models").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "birthRate.t:47", "Tree.t:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "YuleModel.t:59", "birthRate.t:59");

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Unlink trees");
		f.selectTab("Partitions");
		beautiFrame.button("Unlink Trees").click();
		printBeautiState(f);
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "birthRate.t:47", "Tree.t:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "YuleModel.t:59", "birthRate.t:59");

		makeSureXMLParses();
	}

	
	@Test
	public void linkTreesAndDeleteTest2a() throws Exception {
		warning("Load gopher data 26.nex, 47.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		selectRows(0, 1);

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Delete second partition");
		f.selectTab("Partitions");
		selectRows(1);
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

		selectRows(1, 0);

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "ClockPrior.c:47");

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
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "birthRate.t:47", "Tree.t:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "YuleModel.t:59", "birthRate.t:59");

		selectRows(2, 1, 0);

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "ClockPrior.c:47", "ClockPrior.c:59");
		makeSureXMLParses();

		warning("Delete second partition (47)");
		f.selectTab("Partitions");
		selectRows(1);
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "ClockPrior.c:59");
		makeSureXMLParses();

		warning("Delete first partition (26)");
		f.selectTab("Partitions");
		selectRows(0);
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
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "birthRate.t:47", "Tree.t:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47",  "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "YuleModel.t:59", "birthRate.t:59");

		selectRows(0, 1, 2);

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Link clocks");
		selectRows(0, 1, 2);
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();
		printBeautiState(f);

		warning("Delete second partition");
		f.selectTab("Partitions");
		selectRows(1);
		beautiFrame.button("-").click();
		printBeautiState(f);

		warning("Delete first partition");
		f.selectTab("Partitions");
		selectRows(0, 1);
		beautiFrame.button("Link Clock Models").click();
		selectRows(0);
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
		assertStateEquals("Tree.t:26", "birthRate.t:26", "Tree.t:47", "birthRate.t:47", "Tree.t:59", "birthRate.t:59");
		assertOperatorsEqual("YuleBirthRateScaler.t:26", "YuleModelTreeScaler.t:26", "YuleModelTreeRootScaler.t:26", "YuleModelUniformOperator.t:26", "YuleModelSubtreeSlide.t:26", "YuleModelNarrow.t:26", "YuleModelWide.t:26", "YuleModelWilsonBalding.t:26", "YuleBirthRateScaler.t:47", "YuleModelTreeScaler.t:47", "YuleModelTreeRootScaler.t:47", "YuleModelUniformOperator.t:47", "YuleModelSubtreeSlide.t:47", "YuleModelNarrow.t:47", "YuleModelWide.t:47", "YuleModelWilsonBalding.t:47", "YuleBirthRateScaler.t:59", "YuleModelTreeScaler.t:59", "YuleModelTreeRootScaler.t:59", "YuleModelUniformOperator.t:59", "YuleModelSubtreeSlide.t:59", "YuleModelNarrow.t:59", "YuleModelWide.t:59", "YuleModelWilsonBalding.t:59");
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:47", "YuleBirthRatePrior.t:47", "YuleModel.t:59", "YuleBirthRatePrior.t:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.26", "TreeHeight.t:26", "YuleModel.t:26", "birthRate.t:26", "treeLikelihood.47", "TreeHeight.t:47", "YuleModel.t:47", "birthRate.t:47", "treeLikelihood.59", "TreeHeight.t:59", "YuleModel.t:59", "birthRate.t:59");

		assertParameterCountInPriorIs(3);		

		selectRows(0, 1, 2);

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		assertParameterCountInPriorIs(3);
		
		f.selectTab("Site Model");
        JComboBoxFixture substModel = beautiFrame.comboBox("substModel");
        substModel.selectItem("HKY");
		assertParameterCountInPriorIs(7);		
		
		f.selectTab("Partitions");
		warning("Link site models");
		selectRows(0, 1, 2);
		beautiFrame.button("Link Site Models").click();
		printBeautiState(f);

		assertParameterCountInPriorIs(7);		
		beautiFrame.button("Unlink Site Models").click();

		printBeautiState(f);
		assertParameterCountInPriorIs(15);		

		warning("Delete second partition");
		f.selectTab("Partitions");
		selectRows(1);
		beautiFrame.button("-").click();
		printBeautiState(f);

		assertParameterCountInPriorIs(10);		

		warning("Delete first partition");
		f.selectTab("Partitions");
		selectRows(0, 1);
		beautiFrame.button("Link Clock Models").click();
		beautiFrame.table().selectCell(TableCell.row(0).column(1));
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "KappaPrior.s:59", "FrequenciesPrior.s:59");		
		assertParameterCountInPriorIs(5);
		
		makeSureXMLParses();
	}
	



	@Test
	public void linkUnlinkTreesAndSetTreePriorTest1() throws Exception {
		warning("Load gopher data 26.nex, 47.nex");
		importAlignment("examples/nexus", new File("26.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		f.selectTab("Priors");
		
		warning("Change to Coalescent - constant population");
		beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Constant Population");
		
		assertPriorsEqual("CoalescentConstant.t:26", "PopSizePrior.t:26");
		importAlignment("examples/nexus", new File("47.nex"));

		warning("Link trees");
		f.selectTab("Partitions");
		selectRows(1, 0);
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);
		assertPriorsEqual("CoalescentConstant.t:26", "ClockPrior.c:47", "PopSizePrior.t:26");

		warning("Unlink trees");
		beautiFrame.button("Unlink Trees").click();
		// should have PopSizePrior.t:47 as well?
		assertPriorsEqual("CoalescentConstant.t:26", "CoalescentConstant.t:47", "ClockPrior.c:47", "PopSizePrior.t:26", "PopSizePrior.t:47");
		
//		warning("Delete partition");
//		f.selectTab("Partitions");
//		selectRows(1);
//		beautiFrame.button("-").click();
//		printBeautiState(f);
//		assertPriorsEqual("CoalescentConstant.t:26", "PopSizePrior.t:26");

		warning("Delete partition");
		f.selectTab("Partitions");
		selectRows(0);
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("CoalescentConstant.t:47", "PopSizePrior.t:47");

		makeSureXMLParses();
	}

	
	@Test
	public void linkClocksAndDeleteTest() throws Exception {
		warning("Load gopher data 26.nex, 47.nex, 59.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"), new File("59.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		selectRows(0, 1, 2);

		warning("Link clocks");
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();
		printBeautiState(f);

		warning("Delete second partition");
		f.selectTab("Partitions");
		selectRows(1);
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:59", "YuleBirthRatePrior.t:59");

		JTableFixture t = beautiFrame.table();
		Assertions.assertThat(t.target.getRowCount()).isEqualTo(2);
		
		makeSureXMLParses();
	}

	@Test
	public void linkSiteModelssAndDeleteTest() throws Exception {
		warning("Load gopher data 26.nex, 47.nex, 59.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"), new File("59.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		selectRows(0, 1, 2);

		warning("Link clocks");
		f.selectTab("Partitions");
		beautiFrame.button("Link Site Models").click();
		printBeautiState(f);

		warning("Delete second partition");
		f.selectTab("Partitions");
		selectRows(1);
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:59", "YuleBirthRatePrior.t:59");

		JTableFixture t = beautiFrame.table();
		Assertions.assertThat(t.target.getRowCount()).isEqualTo(2);
		
		makeSureXMLParses();
	}

	@Test
	public void linkClocksSitesAndDeleteTest() throws Exception {
		warning("Load gopher data 26.nex, 47.nex, 59.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"), new File("59.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		selectRows(0, 1, 2);

		warning("Link clocks");
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();

		warning("Link site models");
		f.selectTab("Partitions");
		beautiFrame.button("Link Site Models").click();
		printBeautiState(f);

		warning("Delete second partition");
		f.selectTab("Partitions");
		selectRows(1);
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26", "YuleModel.t:59", "YuleBirthRatePrior.t:59");

		JTableFixture t = beautiFrame.table();
		Assertions.assertThat(t.target.getRowCount()).isEqualTo(2);
		
		makeSureXMLParses();
	}

	@Test
	public void linkClocksSitesTreesAndDeleteTest() throws Exception {
		warning("Load gopher data 26.nex, 47.nex, 59.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"), new File("59.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		selectRows(0, 1, 2);

		warning("Link clocks");
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();

		warning("Link site models");
		f.selectTab("Partitions");
		beautiFrame.button("Link Site Models").click();
		printBeautiState(f);

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Delete second partition");
		f.selectTab("Partitions");
		selectRows(1);
		beautiFrame.button("-").click();
		printBeautiState(f);
		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26");

		JTableFixture t = beautiFrame.table();
		Assertions.assertThat(t.target.getRowCount()).isEqualTo(2);
		
		makeSureXMLParses();
	}
	
	
	@Test // issue #413
	public void starBeastLinkTreesAndDeleteTest() throws Exception {
		warning("Select StarBeast template");
		if (!Utils.isMac()) {
			beautiFrame.menuItemWithPath("File", "Template", "StarBeast").click();
		} else {
			execute(new GuiTask() {
		        @Override
				protected void executeInEDT() {
		        	try {
		    			beauti.doc.loadNewTemplate("templates/StarBeast.xml");
		    			beauti.refreshPanel();
		        	} catch (Exception e) {
						e.printStackTrace();
					}
		        }
		    });
		}

		warning("Load gopher data 26.nex, 47.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		selectRows(0, 1);

		warning("Link trees");
		f.selectTab("Partitions");
		beautiFrame.button("Link Trees").click();
		printBeautiState(f);

		warning("Delete second partition");
		f.selectTab("Partitions");
		selectRows(1);
		beautiFrame.button("-").click();
		printBeautiState(f);
//		assertPriorsEqual("YuleModel.t:26", "YuleBirthRatePrior.t:26");
		
		JTableFixture t = beautiFrame.table();
		Assertions.assertThat(t.target.getRowCount()).isEqualTo(1);

		// does not parse unless taxon set is specified
		//makeSureXMLParses();
	}	
	
	@Test // issue #414
	public void linkClocksDeleteAllTest() throws Exception {
		warning("Load gopher data 26.nex, 47.nex, 59.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"), new File("59.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);

		selectRows(0, 1, 2);

		warning("Link clocks");
		f.selectTab("Partitions");
		beautiFrame.button("Link Clock Models").click();
		
		beautiFrame.button("-").click();

		JTableFixture t = beautiFrame.table();
		Assertions.assertThat(t.target.getRowCount()).isEqualTo(0);
	}
}
