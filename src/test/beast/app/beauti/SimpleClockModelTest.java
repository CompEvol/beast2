package test.beast.app.beauti;

import static org.fest.swing.finder.JFileChooserFinder.findFileChooser;

import java.io.File;

import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.junit.Test;

public class SimpleClockModelTest extends BeautiBase {

	/** check the standard clock models are there and result in correct behaviour **/
	@Test
	public void simpleTreePriorTest() throws Exception {
		warning("Load anolis.nex");
		beautiFrame.menuItemWithPath("File", "Import Alignment").click();
		JFileChooserFixture fileChooser = findFileChooser().using(robot());
		fileChooser.setCurrentDirectory(new File("examples/nexus"));
		fileChooser.selectFile(new File("anolis.nex")).approve();

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		f.selectTab("Clock Model");

		warning("Change to Relaxed Clock - exponential");
		beautiFrame.comboBox().selectItem("Relaxed Clock Exponential");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "expRateCategories.c:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "ExpCategoriesRandomWalk.c:anolis", "ExpCategoriesSwapOperator.c:anolis", "ExpCategoriesUniform.c:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "rateStat.c:anolis");
		
		warning("Change to Relaxed Clock - log normal");
		beautiFrame.comboBox().selectItem("Relaxed Clock Log Normal");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "ucldStdev.c:anolis", "rateCategories.c:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "ucldStdevScaler.c:anolis", "CategoriesRandomWalk.c:anolis", "CategoriesSwapOperator.c:anolis", "CategoriesUniform.c:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis", "ucldStdevPrior.c:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "ucldStdev.c:anolis", "rate.c:anolis");

		warning("Change to Random Local Clock");
		beautiFrame.comboBox().selectItem("Random Local Clock");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "Indicators.c:anolis", "clockrates.c:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "IndicatorsBitFlip.c:anolis", "ClockRateScaler.c:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "Indicators.c:anolis", "clockrates.c:anolis");

		warning("Change to Strickt Clock");
		beautiFrame.comboBox().selectItem("Strict Clock");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis");

		makeSureXMLParses();
	}

	/** switch to coalescent tree prior, then 
	 * check the standard clock models are there and result in correct behaviour **/
	@Test
	public void simpleTreePriorTest2() throws Exception {
		warning("Load anolis.nex");
		beautiFrame.menuItemWithPath("File", "Import Alignment").click();
		JFileChooserFixture fileChooser = findFileChooser().using(robot());
		fileChooser.setCurrentDirectory(new File("examples/nexus"));
		fileChooser.selectFile(new File("anolis.nex")).approve();

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		f.selectTab("Priors");
		
		warning("Change to Coalescent - constant population");
		
		beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Constant Population");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis");
		assertOperatorsEqual("allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "PopSizeScaler.t:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis");
		
		f.selectTab("Clock Model");

		warning("Change to Relaxed Clock - exponential");
		beautiFrame.comboBox().selectItem("Relaxed Clock Exponential");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis", "expRateCategories.c:anolis");
		assertOperatorsEqual("allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "PopSizeScaler.t:anolis", "ExpCategoriesRandomWalk.c:anolis", "ExpCategoriesSwapOperator.c:anolis", "ExpCategoriesUniform.c:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis", "rateStat.c:anolis");
		
		warning("Change to Relaxed Clock - log normal");
		beautiFrame.comboBox().selectItem("Relaxed Clock Log Normal");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis", "ucldStdev.c:anolis", "rateCategories.c:anolis");
		assertOperatorsEqual("allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "PopSizeScaler.t:anolis", "ucldStdevScaler.c:anolis", "CategoriesRandomWalk.c:anolis", "CategoriesSwapOperator.c:anolis", "CategoriesUniform.c:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis", "ucldStdevPrior.c:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis", "ucldStdev.c:anolis", "rate.c:anolis");

		warning("Change to Random Local Clock");
		beautiFrame.comboBox().selectItem("Random Local Clock");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis", "Indicators.c:anolis", "clockrates.c:anolis");
		assertOperatorsEqual("allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "PopSizeScaler.t:anolis", "IndicatorsBitFlip.c:anolis", "ClockRateScaler.c:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis", "Indicators.c:anolis", "clockrates.c:anolis");

		warning("Change to Strickt Clock");
		beautiFrame.comboBox().selectItem("Strict Clock");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis");
		assertOperatorsEqual("allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "PopSizeScaler.t:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis");

		makeSureXMLParses();
	}

}
