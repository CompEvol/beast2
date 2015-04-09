package test.beast.app.beauti;


import java.io.File;

import org.fest.swing.fixture.JTabbedPaneFixture;
import org.junit.Test;

public class SimpleClockModelTest extends BeautiBase {

	/** check the standard clock models are there and result in correct behaviour **/
	@Test
	public void simpleTreePriorTest() throws Exception {
		warning("Load anolis.nex");
		importAlignment("examples/nexus", new File("anolis.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		f.selectTab("Clock Model");

		warning("Change to Relaxed Clock - exponential");
		beautiFrame.comboBox().selectItem("Relaxed Clock Exponential");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "expRateCategories.c:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "YuleModelTreeScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "ExpCategoriesRandomWalk.c:anolis", "ExpCategoriesSwapOperator.c:anolis", "ExpCategoriesUniform.c:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "rateStat.c:anolis");
		
		warning("Change to Relaxed Clock - log normal");
		beautiFrame.comboBox().selectItem("Relaxed Clock Log Normal");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "ucldStdev.c:anolis", "rateCategories.c:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "YuleModelTreeScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "ucldStdevScaler.c:anolis", "CategoriesRandomWalk.c:anolis", "CategoriesSwapOperator.c:anolis", "CategoriesUniform.c:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis", "ucldStdevPrior.c:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "ucldStdev.c:anolis", "rate.c:anolis");

		warning("Change to Random Local Clock");
		beautiFrame.comboBox().selectItem("Random Local Clock");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "Indicators.c:anolis", "clockrates.c:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "YuleModelTreeScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "IndicatorsBitFlip.c:anolis", "ClockRateScaler.c:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "Indicators.c:anolis", "clockrates.c:anolis");

		warning("Change to Strickt Clock");
		beautiFrame.comboBox().selectItem("Strict Clock");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "YuleModelTreeScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis");

		makeSureXMLParses();
	}

	/** switch to coalescent tree prior, then 
	 * check the standard clock models are there and result in correct behaviour **/
	@Test
	public void simpleTreePriorTest2() throws Exception {
		warning("Load anolis.nex");
		importAlignment("examples/nexus", new File("anolis.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		f.selectTab("Priors");
		
		warning("Change to Coalescent - constant population");
		
		beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Constant Population");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis");
		assertOperatorsEqual("YuleModelTreeScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "PopSizeScaler.t:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis");
		
		f.selectTab("Clock Model");

		warning("Change to Relaxed Clock - exponential");
		beautiFrame.comboBox().selectItem("Relaxed Clock Exponential");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis", "expRateCategories.c:anolis");
		assertOperatorsEqual("YuleModelTreeScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "PopSizeScaler.t:anolis", "ExpCategoriesRandomWalk.c:anolis", "ExpCategoriesSwapOperator.c:anolis", "ExpCategoriesUniform.c:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis", "rateStat.c:anolis");
		
		warning("Change to Relaxed Clock - log normal");
		beautiFrame.comboBox().selectItem("Relaxed Clock Log Normal");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis", "ucldStdev.c:anolis", "rateCategories.c:anolis");
		assertOperatorsEqual("YuleModelTreeScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "PopSizeScaler.t:anolis", "ucldStdevScaler.c:anolis", "CategoriesRandomWalk.c:anolis", "CategoriesSwapOperator.c:anolis", "CategoriesUniform.c:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis", "ucldStdevPrior.c:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis", "ucldStdev.c:anolis", "rate.c:anolis");

		warning("Change to Random Local Clock");
		beautiFrame.comboBox().selectItem("Random Local Clock");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis", "Indicators.c:anolis", "clockrates.c:anolis");
		assertOperatorsEqual("YuleModelTreeScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "PopSizeScaler.t:anolis", "IndicatorsBitFlip.c:anolis", "ClockRateScaler.c:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis", "Indicators.c:anolis", "clockrates.c:anolis");

		warning("Change to Strickt Clock");
		beautiFrame.comboBox().selectItem("Strict Clock");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis");
		assertOperatorsEqual("YuleModelTreeScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "PopSizeScaler.t:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis");

		makeSureXMLParses();
	}

}
