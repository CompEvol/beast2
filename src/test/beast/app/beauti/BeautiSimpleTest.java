package test.beast.app.beauti;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.swing.finder.JFileChooserFinder.findFileChooser;

import java.awt.Component;
import java.io.File;
import java.util.Arrays;

import javax.swing.JComboBox;

import org.fest.swing.data.Index;
import org.fest.swing.data.TableCell;
import org.fest.swing.fixture.JCheckBoxFixture;
import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.fixture.JTableCellFixture;
import org.fest.swing.fixture.JTableFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.junit.Test;

public class BeautiSimpleTest extends BeautiBase {

	
	@Test
	public void simpleTest() throws Exception {
		beautiFrame.menuItemWithPath("File", "Import Alignment").click();
		JFileChooserFixture fileChooser = findFileChooser().using(robot());
		fileChooser.setCurrentDirectory(new File("examples/nexus"));
		fileChooser.selectFile(new File("anolis.nex")).approve();

		// load anolis.nex
		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		f.requireVisible();
		f.requireTitle("Partitions", Index.atIndex(0));
		String[] titles = f.tabTitles();
		assertArrayEquals(titles,"[Partitions, Tip Dates, Site Model, Clock Model, Priors, MCMC]");
		System.err.println(Arrays.toString(titles));
		f = f.selectTab("Partitions");
		JTableFixture t = beautiFrame.table();
		String[][] tc = t.contents();
		System.err.println(Arrays.toString(tc[0]));
		assertArrayEquals(tc[0],"[anolis, anolis, 29, 1456, nucleotide, anolis, anolis, anolis]");
		assertThat(f).isNotNull();
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis");
		
		
		
		
		
		// Set the site model to HKY (estimated)
		f.selectTab("Site Model");
		JComboBoxFixture substModel = beautiFrame.comboBox();
		substModel.selectItem("HKY");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "kappa.s:anolis", "freqParameter.s:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "KappaScaler.s:anolis", "FrequenciesExchanger.s:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis", "KappaPrior.s:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "kappa.s:anolis", "freqParameter.s:anolis");

		// Set the site model to HKY (G4) (estimated)
		f.selectTab("Site Model");
		JTextComponentFixture categoryCount = beautiFrame.textBox("gammaCategoryCount");
		categoryCount.setText("4");
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "kappa.s:anolis", "freqParameter.s:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "KappaScaler.s:anolis", "FrequenciesExchanger.s:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis", "KappaPrior.s:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "kappa.s:anolis", "freqParameter.s:anolis");

		f.selectTab("Site Model");
		JCheckBoxFixture shapeIsEstimated = beautiFrame.checkBox("shape.isEstimated");
		shapeIsEstimated.check();
		printBeautiState(f);
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "kappa.s:anolis", "gammaShape.s:anolis", "freqParameter.s:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "allTipDatesRandomWalker.t:anolis", "treeScaler.t:anolis", "treeRootScaler.t:anolis", "UniformOperator.t:anolis", "SubtreeSlide.t:anolis", "narrow.t:anolis", "wide.t:anolis", "WilsonBalding.t:anolis", "KappaScaler.s:anolis", "gammaShapeScaler.s:anolis", "FrequenciesExchanger.s:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis", "KappaPrior.s:anolis", "GammaShapePrior.s:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "kappa.s:anolis", "gammaShape.s:anolis", "freqParameter.s:anolis");

		// rename tree from 'anolis' to 'tree'
		f.selectTab("Partitions");
		JTableCellFixture cell = beautiFrame.table().cell(TableCell.row(0).column(7));
		Component editor = cell.editor();
		JComboBoxFixture comboBox = new JComboBoxFixture(robot(), (JComboBox) editor);
		cell.startEditing();
		comboBox.selectAllText();
		comboBox.enterText("tree");
		cell.stopEditing();
		printBeautiState(f);
		assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:anolis", "gammaShape.s:anolis", "freqParameter.s:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:tree", "allTipDatesRandomWalker.t:tree", "treeScaler.t:tree", "treeRootScaler.t:tree", "UniformOperator.t:tree", "SubtreeSlide.t:tree", "narrow.t:tree", "wide.t:tree", "WilsonBalding.t:tree", "KappaScaler.s:anolis", "gammaShapeScaler.s:anolis", "FrequenciesExchanger.s:anolis");
		assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "GammaShapePrior.s:anolis", "KappaPrior.s:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "kappa.s:anolis", "gammaShape.s:anolis", "freqParameter.s:anolis");
		
		// Create a Normal calibration prior and monophyletic constraint on Human-Chimp split of 6 +/- 0.5.
		f.selectTab("Priors");

		makeSureXMLParses();
	}

}
