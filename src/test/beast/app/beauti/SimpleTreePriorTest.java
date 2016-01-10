package test.beast.app.beauti;


import java.io.File;

import org.fest.swing.fixture.JTabbedPaneFixture;
import org.junit.Test;

public class SimpleTreePriorTest extends BeautiBase {
	
	/** check the standard tree priors are there and result in correct behaviour **/
	@Test
	public void simpleTreePriorTest() throws Exception {
		warning("Load anolis.nex");
		importAlignment("examples/nexus", new File("anolis.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		f.selectTab("Priors");
		
		warning("Change to Coalescent - constant population");
		
		beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Constant Population");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis");
		assertOperatorsEqual("CoalescentConstantTreeScaler.t:anolis", "CoalescentConstantTreeRootScaler.t:anolis", "CoalescentConstantUniformOperator.t:anolis", "CoalescentConstantSubtreeSlide.t:anolis", "CoalescentConstantNarrow.t:anolis", "CoalescentConstantWide.t:anolis", "CoalescentConstantWilsonBalding.t:anolis", "PopSizeScaler.t:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis");

		warning("Change to Coalescent - exponential population");
		beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Exponential Population");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "ePopSize.t:anolis", "growthRate.t:anolis");
		assertOperatorsEqual("CoalescentExponentialTreeScaler.t:anolis", "CoalescentExponentialTreeRootScaler.t:anolis", "CoalescentExponentialUniformOperator.t:anolis", "CoalescentExponentialSubtreeSlide.t:anolis", "CoalescentExponentialNarrow.t:anolis", "CoalescentExponentialWide.t:anolis", "CoalescentExponentialWilsonBalding.t:anolis", "ePopSizeScaler.t:anolis", "GrowthRateRandomWalk.t:anolis");
		assertPriorsEqual("CoalescentExponential.t:anolis", "ePopSizePrior.t:anolis", "GrowthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "CoalescentExponential.t:anolis", "ePopSize.t:anolis", "growthRate.t:anolis");
		
		warning("Change to Coalescent - BPS");
		beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Bayesian Skyline");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "isPopSizes.t:anolis", "isGroupSizes.t:anolis");
		assertOperatorsEqual("BayesianSkylineTreeScaler.t:anolis", "BayesianSkylineTreeRootScaler.t:anolis", "BayesianSkylineUniformOperator.t:anolis", "BayesianSkylineSubtreeSlide.t:anolis", "BayesianSkylineNarrow.t:anolis", "BayesianSkylineWide.t:anolis", "BayesianSkylineWilsonBalding.t:anolis", "popSizesScaler.t:anolis", "groupSizesDelta.t:anolis");
		assertPriorsEqual("BayesianSkyline.t:anolis", "MarkovChainedPopSizes.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "BayesianSkyline.t:anolis", "isPopSizes.t:anolis", "isGroupSizes.t:anolis");
		
		warning("Change to Yule");
		beautiFrame.comboBox("TreeDistribution").selectItem("Yule Model");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis");
		assertOperatorsEqual("YuleModelTreeScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "YuleBirthRateScaler.t:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis");
		
		warning("Change to Birth-Death");
		beautiFrame.comboBox("TreeDistribution").selectItem("Birth Death Model");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate2.t:anolis", "relativeDeathRate2.t:anolis");
		assertOperatorsEqual("BirthDeathTreeScaler.t:anolis", "BirthDeathTreeRootScaler.t:anolis", "BirthDeathUniformOperator.t:anolis", "BirthDeathSubtreeSlide.t:anolis", "BirthDeathNarrow.t:anolis", "BirthDeathWide.t:anolis", "BirthDeathWilsonBalding.t:anolis", "BirthRateScaler.t:anolis", "DeathRateScaler.t:anolis");
		assertPriorsEqual("BirthDeath.t:anolis", "BirthRatePrior.t:anolis", "DeathRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "BirthDeath.t:anolis", "birthRate2.t:anolis", "relativeDeathRate2.t:anolis");

		makeSureXMLParses();
	}

}
