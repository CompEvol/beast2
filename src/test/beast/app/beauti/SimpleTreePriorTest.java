package test.beast.app.beauti;

import static org.fest.swing.finder.JFileChooserFinder.findFileChooser;

import java.io.File;

import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.junit.Test;

public class SimpleTreePriorTest extends BeautiBase {
	
	/** check the standard tree priors are there and result in correct behaviour **/
	@Test
	public void simpleTreePriorTest() throws Exception {
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

		warning("Change to Coalescent - exponential population");
		beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Exponential Population");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "ePopSize.t:anolis", "growthRate.t:anolis");
		assertOperatorsEqual("allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "ePopSizeScaler.t:anolis", "GrowthRateRandomWalk.t:anolis");
		assertPriorsEqual("CoalescentExponential.t:anolis", "ePopSizePrior.t:anolis", "GrowthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "CoalescentExponential.t:anolis", "ePopSize.t:anolis", "growthRate.t:anolis");
		
		warning("Change to Coalescent - BPS");
		beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Bayesian Skyline");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "bPopSizes.t:anolis", "bGroupSizes.t:anolis");
		assertOperatorsEqual("allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "popSizesScaler.t:anolis", "groupSizesDelta.t:anolis");
		assertPriorsEqual("BayesianSkyline.t:anolis", "MarkovChainedPopSizes.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "BayesianSkyline.t:anolis", "bPopSizes.t:anolis", "bGroupSizes.t:anolis");
		
		warning("Change to Yule");
		beautiFrame.comboBox("TreeDistribution").selectItem("Yule Model");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis");
		assertOperatorsEqual("allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "YuleBirthRateScaler.t:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis");
		
		warning("Change to Birth-Death");
		beautiFrame.comboBox("TreeDistribution").selectItem("Birth Death Model");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate2.t:anolis", "relativeDeathRate2.t:anolis");
		assertOperatorsEqual("allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "BirthRateScaler.t:anolis", "DeathRateScaler.t:anolis");
		assertPriorsEqual("BirthDeath.t:anolis", "BirthRatePrior.t:anolis", "DeathRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "BirthDeath.t:anolis");

	}

}
