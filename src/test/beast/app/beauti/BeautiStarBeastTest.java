package test.beast.app.beauti;


import static org.fest.swing.finder.JFileChooserFinder.findFileChooser;

import java.io.File;

import javax.swing.JButton;

import org.fest.swing.core.matcher.JButtonMatcher;
import org.fest.swing.finder.WindowFinder;
import org.fest.swing.fixture.DialogFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.junit.Test;

public class BeautiStarBeastTest extends BeautiBase {

	@Test
	public void simpleStarBeastTest() throws Exception {
		warning("Select StarBeast template");
		beautiFrame.menuItemWithPath("File", "Template", "StarBeast").click();

		warning("Load gopher data 26.nex, 47.nex, 59.nex");
		importAlignment("examples/nexus", new File("26.nex"), new File("47.nex"), new File("59.nex"));

		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState(f);
		assertStateEquals("popSize", "Tree.t:Species", "birthRate.Species", "popMean", "Tree.t:26", "Tree.t:47", "clockRate.c:47", "Tree.t:59", "clockRate.c:59");
		assertOperatorsEqual("Reheight", "popSizeScaler", "updown.all", "YuleBirthRateScaler.Species", "popMeanScale", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "updown.47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "treeScaler.t:59", "treeRootScaler.t:59", "UniformOperator.t:59", "SubtreeSlide.t:59", "narrow.t:59", "wide.t:59", "WilsonBalding.t:59", "updown.59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.Species", "YuleBirthRatePrior.Species", "popMean.prior", "ClockPrior.c:47", "ClockPrior.c:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "speciescoalescent", "birthRate.Species", "YuleModel.Species", "TreeHeight.Species", "treeLikelihood.26", "treePrior.t:26", "TreeHeight.t:26", "treeLikelihood.47", "treePrior.t:47", "TreeHeight.t:47", "clockRate.c:47", "treeLikelihood.59", "treePrior.t:59", "TreeHeight.t:59", "clockRate.c:59");

		warning("Define taxon sets");
		f.selectTab("Taxon sets");
		beautiFrame.button("Guess").click();
		DialogFixture dialog = WindowFinder.findDialog("GuessTaxonSets").using(robot());
		dialog.radioButton("split on character").click();
		dialog.comboBox("splitCombo").selectItem("2");
		dialog.textBox("SplitChar").deleteText().enterText("_");
		JButton okButton = dialog.robot.finder().find(JButtonMatcher.withText("OK"));
		new JButtonFixture(dialog.robot, okButton).click();
		printBeautiState(f);
		assertStateEquals("popSize", "Tree.t:Species", "birthRate.Species", "popMean", "Tree.t:26", "Tree.t:47", "clockRate.c:47", "Tree.t:59", "clockRate.c:59");
		assertOperatorsEqual("Reheight", "popSizeScaler", "updown.all", "YuleBirthRateScaler.Species", "popMeanScale", "treeScaler.t:26", "treeRootScaler.t:26", "UniformOperator.t:26", "SubtreeSlide.t:26", "narrow.t:26", "wide.t:26", "WilsonBalding.t:26", "StrictClockRateScaler.c:47", "treeScaler.t:47", "treeRootScaler.t:47", "UniformOperator.t:47", "SubtreeSlide.t:47", "narrow.t:47", "wide.t:47", "WilsonBalding.t:47", "updown.47", "strictClockUpDownOperator.c:47", "StrictClockRateScaler.c:59", "treeScaler.t:59", "treeRootScaler.t:59", "UniformOperator.t:59", "SubtreeSlide.t:59", "narrow.t:59", "wide.t:59", "WilsonBalding.t:59", "updown.59", "strictClockUpDownOperator.c:59");
		assertPriorsEqual("YuleModel.Species", "YuleBirthRatePrior.Species", "popMean.prior", "ClockPrior.c:47", "ClockPrior.c:59");
		assertTraceLogEqual("posterior", "likelihood", "prior", "speciescoalescent", "birthRate.Species", "YuleModel.Species", "TreeHeight.Species", "treeLikelihood.26", "treePrior.t:26", "TreeHeight.t:26", "treeLikelihood.47", "treePrior.t:47", "TreeHeight.t:47", "clockRate.c:47", "treeLikelihood.59", "treePrior.t:59", "TreeHeight.t:59", "clockRate.c:59");

		makeSureXMLParses();
	}

}
