package test.beast.app.beauti;


import org.fest.swing.data.Index;
import org.fest.swing.data.TableCell;
import org.fest.swing.finder.WindowFinder;
import org.fest.swing.fixture.*;
import org.fest.swing.image.ScreenshotTaker;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;

public class BeautiDivergenceDatingTest extends BeautiBase {
    final static String PREFIX = "doc/tutorials/DivergenceDating/figures/BEAUti_";

    @Test
    public void DivergenceDatingTutorial() throws Exception {
        long t0 = System.currentTimeMillis();

        // 0. Load primate-mtDNA.nex
        warning("// 0. Load primate-mtDNA.nex");
        importAlignment("examples/nexus", new File("primate-mtDNA.nex"));

        beautiFrame.menuItemWithPath("Mode", "Automatic set fix mean substitution rate flag").click();

        JTabbedPaneFixture f = beautiFrame.tabbedPane();
        f.requireVisible();
        f.requireTitle("Partitions", Index.atIndex(0));
        String[] titles = f.tabTitles();
        assertArrayEquals(titles, "[Partitions, Tip Dates, Site Model, Clock Model, Priors, MCMC]");
        System.err.println(Arrays.toString(titles));
        f = f.selectTab("Partitions");

        // check table
        JTableFixture t = beautiFrame.table();
        printTableContents(t);
        checkTableContents(f, "[coding, primate-mtDNA, 12, 693, nucleotide, coding, coding, coding, false]*" +
                "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, noncoding, noncoding, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, 1stpos, 1stpos, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, 2ndpos, 2ndpos, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, 3rdpos, 3rdpos, false]");

        assertThat(f).isNotNull();
        printBeautiState(f);
        assertStateEquals("Tree.t:noncoding", "clockRate.c:noncoding", "birthRate.t:noncoding", "Tree.t:2ndpos", "clockRate.c:2ndpos", "birthRate.t:2ndpos", "Tree.t:1stpos", "clockRate.c:1stpos", "birthRate.t:1stpos", "Tree.t:coding", "birthRate.t:coding", "Tree.t:3rdpos", "clockRate.c:3rdpos", "birthRate.t:3rdpos");
        assertOperatorsEqual("StrictClockRateScaler.c:noncoding", "YuleBirthRateScaler.t:noncoding", "YuleModelTreeScaler.t:noncoding", "YuleModelTreeRootScaler.t:noncoding", "YuleModelUniformOperator.t:noncoding", "YuleModelSubtreeSlide.t:noncoding", "YuleModelNarrow.t:noncoding", "YuleModelWide.t:noncoding", "YuleModelWilsonBalding.t:noncoding", "StrictClockRateScaler.c:2ndpos", "YuleBirthRateScaler.t:2ndpos", "YuleModelTreeScaler.t:2ndpos", "YuleModelTreeRootScaler.t:2ndpos", "YuleModelUniformOperator.t:2ndpos", "YuleModelSubtreeSlide.t:2ndpos", "YuleModelNarrow.t:2ndpos", "YuleModelWide.t:2ndpos", "YuleModelWilsonBalding.t:2ndpos", "StrictClockRateScaler.c:1stpos", "YuleBirthRateScaler.t:1stpos", "YuleModelTreeScaler.t:1stpos", "YuleModelTreeRootScaler.t:1stpos", "YuleModelUniformOperator.t:1stpos", "YuleModelSubtreeSlide.t:1stpos", "YuleModelNarrow.t:1stpos", "YuleModelWide.t:1stpos", "YuleModelWilsonBalding.t:1stpos", "YuleBirthRateScaler.t:coding", "YuleModelTreeScaler.t:coding", "YuleModelTreeRootScaler.t:coding", "YuleModelUniformOperator.t:coding", "YuleModelSubtreeSlide.t:coding", "YuleModelNarrow.t:coding", "YuleModelWide.t:coding", "YuleModelWilsonBalding.t:coding", "StrictClockRateScaler.c:3rdpos", "YuleBirthRateScaler.t:3rdpos", "YuleModelTreeScaler.t:3rdpos", "YuleModelTreeRootScaler.t:3rdpos", "YuleModelUniformOperator.t:3rdpos", "YuleModelSubtreeSlide.t:3rdpos", "YuleModelNarrow.t:3rdpos", "YuleModelWide.t:3rdpos", "YuleModelWilsonBalding.t:3rdpos", "strictClockUpDownOperator.c:3rdpos", "strictClockUpDownOperator.c:1stpos", "strictClockUpDownOperator.c:2ndpos", "strictClockUpDownOperator.c:noncoding");
        assertPriorsEqual("YuleModel.t:coding", "YuleModel.t:noncoding", "YuleModel.t:1stpos", "YuleModel.t:2ndpos", "YuleModel.t:3rdpos", "ClockPrior.c:noncoding", "YuleBirthRatePrior.t:noncoding", "ClockPrior.c:2ndpos", "YuleBirthRatePrior.t:2ndpos", "ClockPrior.c:1stpos", "YuleBirthRatePrior.t:1stpos", "YuleBirthRatePrior.t:coding", "ClockPrior.c:3rdpos", "YuleBirthRatePrior.t:3rdpos");


        //1. Delete "coding" partition as it covers the same sites as the 1stpos, 2ndpos and 3rdpos partitions.
        warning("1. Delete \"coding\" partition as it covers the same sites as the 1stpos, 2ndpos and 3rdpos partitions.");
        f.selectTab("Partitions");
        t = beautiFrame.table();
        t.selectCell(TableCell.row(0).column(2));
        JButtonFixture deleteButton = beautiFrame.button("-");
        deleteButton.click();
        printBeautiState(f);
        checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, noncoding, noncoding, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, 1stpos, 1stpos, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, 2ndpos, 2ndpos, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, 3rdpos, 3rdpos, false]");
        assertStateEquals("Tree.t:noncoding", "birthRate.t:noncoding", "Tree.t:3rdpos", "clockRate.c:3rdpos", "birthRate.t:3rdpos", "Tree.t:1stpos", "clockRate.c:1stpos", "birthRate.t:1stpos", "Tree.t:2ndpos", "clockRate.c:2ndpos", "birthRate.t:2ndpos");
        assertOperatorsEqual("YuleBirthRateScaler.t:noncoding", "YuleModelTreeScaler.t:noncoding", "YuleModelTreeRootScaler.t:noncoding", "YuleModelUniformOperator.t:noncoding", "YuleModelSubtreeSlide.t:noncoding", "YuleModelNarrow.t:noncoding", "YuleModelWide.t:noncoding", "YuleModelWilsonBalding.t:noncoding", "StrictClockRateScaler.c:3rdpos", "YuleBirthRateScaler.t:3rdpos", "YuleModelTreeScaler.t:3rdpos", "YuleModelTreeRootScaler.t:3rdpos", "YuleModelUniformOperator.t:3rdpos", "YuleModelSubtreeSlide.t:3rdpos", "YuleModelNarrow.t:3rdpos", "YuleModelWide.t:3rdpos", "YuleModelWilsonBalding.t:3rdpos", "StrictClockRateScaler.c:1stpos", "YuleBirthRateScaler.t:1stpos", "YuleModelTreeScaler.t:1stpos", "YuleModelTreeRootScaler.t:1stpos", "YuleModelUniformOperator.t:1stpos", "YuleModelSubtreeSlide.t:1stpos", "YuleModelNarrow.t:1stpos", "YuleModelWide.t:1stpos", "YuleModelWilsonBalding.t:1stpos", "StrictClockRateScaler.c:2ndpos", "YuleBirthRateScaler.t:2ndpos", "YuleModelTreeScaler.t:2ndpos", "YuleModelTreeRootScaler.t:2ndpos", "YuleModelUniformOperator.t:2ndpos", "YuleModelSubtreeSlide.t:2ndpos", "YuleModelNarrow.t:2ndpos", "YuleModelWide.t:2ndpos", "YuleModelWilsonBalding.t:2ndpos", "strictClockUpDownOperator.c:3rdpos", "strictClockUpDownOperator.c:2ndpos", "strictClockUpDownOperator.c:1stpos");
        assertPriorsEqual("YuleModel.t:1stpos", "YuleModel.t:2ndpos", "YuleModel.t:3rdpos", "YuleModel.t:noncoding", "YuleBirthRatePrior.t:1stpos", "YuleBirthRatePrior.t:2ndpos", "YuleBirthRatePrior.t:3rdpos", "YuleBirthRatePrior.t:noncoding", "ClockPrior.c:1stpos", "ClockPrior.c:2ndpos", "ClockPrior.c:3rdpos");


        //2a. Link trees...
        warning("2a. Link trees...");
        f.selectTab("Partitions");
        t = beautiFrame.table();
        t.selectCells(TableCell.row(0).column(2), TableCell.row(1).column(2), TableCell.row(2).column(2), TableCell.row(3).column(2));
        JButtonFixture linkTreesButton = beautiFrame.button("Link Trees");
        linkTreesButton.click();
        printBeautiState(f);
        assertStateEquals("Tree.t:noncoding", "birthRate.t:noncoding", "clockRate.c:2ndpos", "clockRate.c:3rdpos", "clockRate.c:1stpos");
        assertOperatorsEqual("YuleBirthRateScaler.t:noncoding", "YuleModelTreeScaler.t:noncoding", "YuleModelTreeRootScaler.t:noncoding", "YuleModelUniformOperator.t:noncoding", "YuleModelSubtreeSlide.t:noncoding", "YuleModelNarrow.t:noncoding", "YuleModelWide.t:noncoding", "YuleModelWilsonBalding.t:noncoding", "StrictClockRateScaler.c:2ndpos", "StrictClockRateScaler.c:3rdpos", "StrictClockRateScaler.c:1stpos", "strictClockUpDownOperator.c:2ndpos", "strictClockUpDownOperator.c:1stpos", "strictClockUpDownOperator.c:3rdpos");
        assertPriorsEqual("YuleModel.t:noncoding", "YuleBirthRatePrior.t:noncoding", "ClockPrior.c:1stpos", "ClockPrior.c:2ndpos", "ClockPrior.c:3rdpos");

        //2b. ...and call the tree "tree"
        warning("2b. ...and call the tree \"tree\"");
        f.selectTab("Partitions");
        JTableCellFixture cell = beautiFrame.table().cell(TableCell.row(0).column(7));
        Component editor = cell.editor();
        JComboBoxFixture comboBox = new JComboBoxFixture(robot(), (JComboBox<?>) editor);
        cell.startEditing();
        comboBox.selectAllText();
        comboBox.enterText("tree");
        comboBox.pressAndReleaseKeys(KeyEvent.VK_ENTER);
        cell.stopEditing();
        checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, noncoding, tree, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, 1stpos, tree, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, 2ndpos, tree, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, 3rdpos, tree, false]");
        printBeautiState(f);
        assertStateEquals("clockRate.c:2ndpos", "Tree.t:tree", "birthRate.t:tree", "clockRate.c:1stpos", "clockRate.c:3rdpos");
        assertOperatorsEqual("StrictClockRateScaler.c:2ndpos", "YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "StrictClockRateScaler.c:1stpos", "StrictClockRateScaler.c:3rdpos", "strictClockUpDownOperator.c:3rdpos", "strictClockUpDownOperator.c:2ndpos", "strictClockUpDownOperator.c:1stpos");
        assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "ClockPrior.c:1stpos", "ClockPrior.c:2ndpos", "ClockPrior.c:3rdpos");


        //3a. Link clocks
        warning("3a. Link clocks");
        f.selectTab("Partitions");
        t = beautiFrame.table();
        t.selectCells(TableCell.row(0).column(2), TableCell.row(1).column(2), TableCell.row(2).column(2), TableCell.row(3).column(2));
        JButtonFixture linkClocksButton = beautiFrame.button("Link Clock Models");
        linkClocksButton.click();

        //3b. and call the uncorrelated relaxed molecular clock "clock"
        warning("3b. and call the uncorrelated relaxed molecular clock \"clock\"");
        cell = beautiFrame.table().cell(TableCell.row(0).column(6));
        editor = cell.editor();
        comboBox = new JComboBoxFixture(robot(), (JComboBox<?>) editor);
        cell.startEditing();
        comboBox.selectAllText();
        comboBox.enterText("clock");
        comboBox.pressAndReleaseKeys(KeyEvent.VK_ENTER);
        cell.stopEditing();
        printBeautiState(f);
        checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, clock, tree, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, clock, tree, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, clock, tree, false]");
        assertStateEquals("Tree.t:tree", "birthRate.t:tree");
        assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree");
        assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree");


        //4. Link site models temporarily in order to set the same model for all of them.
        warning("4. Link site models temporarily in order to set the same model for all of them.");
        f.selectTab("Partitions");
        t = beautiFrame.table();
        t.selectCells(TableCell.row(0).column(2), TableCell.row(1).column(2), TableCell.row(2).column(2), TableCell.row(3).column(2));
        JButtonFixture linkSiteModelsButton = beautiFrame.button("Link Site Models");
        linkSiteModelsButton.click();
        printBeautiState(f);
        checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]");
        assertStateEquals("Tree.t:tree", "birthRate.t:tree");
        assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree");
        assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree");

        //5. Set the site model to HKY+G4 (estimated)
        warning("5. Set the site model to HKY+G4 (estimated)");
        f.selectTab("Site Model");
        JComboBoxFixture substModel = beautiFrame.comboBox("substModel");
        substModel.selectItem("HKY");

        JTextComponentFixture categoryCount = beautiFrame.textBox("gammaCategoryCount");
        categoryCount.setText("4");

        JCheckBoxFixture shapeIsEstimated = beautiFrame.checkBox("shape.isEstimated");
        shapeIsEstimated.check();
        printBeautiState(f);
        checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]");
        assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "freqParameter.s:noncoding");
        assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "KappaScaler.s:noncoding", "gammaShapeScaler.s:noncoding", "FrequenciesExchanger.s:noncoding");
        assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "KappaPrior.s:noncoding", "GammaShapePrior.s:noncoding", "FrequenciesPrior.s:noncoding");

        //6a. Unlink the site models,
        warning("6a. Unlink the site models,");
        f.selectTab("Partitions");
        t = beautiFrame.table();
        t.selectCells(TableCell.row(0).column(2), TableCell.row(1).column(2), TableCell.row(2).column(2), TableCell.row(3).column(2));
        beautiFrame.button("Unlink Site Models").click();
        printBeautiState(f);
        checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, clock, tree, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, clock, tree, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, clock, tree, false]");

        //6b. and make sure that site model mutation rates are relative to 3rdpos (i.e. 3rdpos.mutationRate = 1, other 3 estimated).
        warning("6b. and make sure that site model mutation rates are relative to 3rdpos (i.e. 3rdpos.mutationRate = 1, other 3 estimated).");
        f.selectTab("Site Model");
        JListFixture partitionList = beautiFrame.list();
        partitionList.selectItem("noncoding");
        JCheckBoxFixture mutationRateIsEstimated = beautiFrame.checkBox("mutationRate.isEstimated");
        mutationRateIsEstimated.requireNotSelected();
        mutationRateIsEstimated.check();
        mutationRateIsEstimated.requireSelected();

        partitionList.selectItem("1stpos");
        mutationRateIsEstimated = beautiFrame.checkBox("mutationRate.isEstimated");
        mutationRateIsEstimated.requireNotSelected();
        mutationRateIsEstimated.check();
        mutationRateIsEstimated.requireSelected();

        partitionList.selectItem("2ndpos");
        mutationRateIsEstimated = beautiFrame.checkBox("mutationRate.isEstimated");
        mutationRateIsEstimated.requireNotSelected();
        mutationRateIsEstimated.check();
        mutationRateIsEstimated.requireSelected();

        partitionList.selectItem("3rdpos");
        mutationRateIsEstimated = beautiFrame.checkBox("mutationRate.isEstimated");
        mutationRateIsEstimated.requireNotSelected();
        printBeautiState(f);
        assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "gammaShape.s:1stpos", "kappa.s:1stpos", "gammaShape.s:2ndpos", "kappa.s:2ndpos", "gammaShape.s:3rdpos", "kappa.s:3rdpos", "mutationRate.s:noncoding", "mutationRate.s:1stpos", "mutationRate.s:2ndpos", "freqParameter.s:noncoding", "freqParameter.s:1stpos", "freqParameter.s:3rdpos", "freqParameter.s:2ndpos");
        assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "KappaScaler.s:noncoding", "gammaShapeScaler.s:noncoding", "gammaShapeScaler.s:1stpos", "KappaScaler.s:1stpos", "gammaShapeScaler.s:2ndpos", "KappaScaler.s:2ndpos", "gammaShapeScaler.s:3rdpos", "KappaScaler.s:3rdpos", "mutationRateScaler.s:noncoding", "mutationRateScaler.s:1stpos", "mutationRateScaler.s:2ndpos", "FrequenciesExchanger.s:noncoding", "FrequenciesExchanger.s:1stpos", "FrequenciesExchanger.s:3rdpos", "FrequenciesExchanger.s:2ndpos");
        assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "GammaShapePrior.s:1stpos", "GammaShapePrior.s:2ndpos", "GammaShapePrior.s:3rdpos", "GammaShapePrior.s:noncoding", "KappaPrior.s:1stpos", "KappaPrior.s:2ndpos", "KappaPrior.s:3rdpos", "KappaPrior.s:noncoding", "MutationRatePrior.s:noncoding", "MutationRatePrior.s:1stpos", "MutationRatePrior.s:2ndpos", "FrequenciesPrior.s:noncoding", "FrequenciesPrior.s:1stpos", "FrequenciesPrior.s:2ndpos", "FrequenciesPrior.s:3rdpos");
        assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "kappa.s:noncoding", "gammaShape.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:noncoding", "mutationRate.s:1stpos", "mutationRate.s:2ndpos", "freqParameter.s:2ndpos", "freqParameter.s:noncoding", "freqParameter.s:3rdpos", "freqParameter.s:1stpos");

        //7a. Create a Normal calibration prior
        warning("7a. Create a Normal calibration prior");
        f.selectTab("Priors");
        Component c = beautiFrame.robot.finder().findByName("addItem");
        JButtonFixture addButton = new JButtonFixture(robot(), (JButton) c);
        addButton.click();
        JOptionPaneFixture dialog = new JOptionPaneFixture(robot());
        dialog.textBox("idEntry").setText("Human-Chimp");
        dialog.list("listOfTaxonCandidates").selectItems("Homo_sapiens", "Pan");
        dialog.button(">>").click();
        dialog.okButton().click();

        //7b. and monophyletic constraint on Human-Chimp split of 6 +/- 0.5.
        warning("7b. and monophyletic constraint on Human-Chimp split of 6 +/- 0.5.");
        f.selectTab("Priors");
        beautiFrame.checkBox("Human-Chimp.prior.isMonophyletic").click();
        beautiFrame.comboBox("Human-Chimp.prior.distr").selectItem("Normal");
        beautiFrame.button("Human-Chimp.prior.editButton").click();
        beautiFrame.textBox("mean").selectAll().setText("6");
        beautiFrame.textBox("sigma").selectAll().setText("0.5");
        printBeautiState(f);
        assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:noncoding", "mutationRate.s:1stpos", "mutationRate.s:2ndpos", "clockRate.c:clock", "freqParameter.s:1stpos", "freqParameter.s:3rdpos", "freqParameter.s:2ndpos", "freqParameter.s:noncoding");
        assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "KappaScaler.s:noncoding", "gammaShapeScaler.s:noncoding", "KappaScaler.s:1stpos", "gammaShapeScaler.s:1stpos", "KappaScaler.s:2ndpos", "gammaShapeScaler.s:2ndpos", "KappaScaler.s:3rdpos", "gammaShapeScaler.s:3rdpos", "mutationRateScaler.s:noncoding", "mutationRateScaler.s:1stpos", "mutationRateScaler.s:2ndpos", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock", "FrequenciesExchanger.s:1stpos", "FrequenciesExchanger.s:3rdpos", "FrequenciesExchanger.s:2ndpos", "FrequenciesExchanger.s:noncoding");
        assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "GammaShapePrior.s:1stpos", "GammaShapePrior.s:2ndpos", "GammaShapePrior.s:3rdpos", "GammaShapePrior.s:noncoding", "KappaPrior.s:1stpos", "KappaPrior.s:2ndpos", "KappaPrior.s:3rdpos", "KappaPrior.s:noncoding", "MutationRatePrior.s:1stpos", "MutationRatePrior.s:2ndpos", "MutationRatePrior.s:noncoding", "Human-Chimp.prior", "ClockPrior.c:clock", "FrequenciesPrior.s:noncoding", "FrequenciesPrior.s:1stpos", "FrequenciesPrior.s:2ndpos", "FrequenciesPrior.s:3rdpos");
        assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "kappa.s:noncoding", "gammaShape.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:noncoding", "mutationRate.s:1stpos", "mutationRate.s:2ndpos", "Human-Chimp.prior", "clockRate.c:clock", "freqParameter.s:3rdpos", "freqParameter.s:2ndpos", "freqParameter.s:noncoding", "freqParameter.s:1stpos");

        //8. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
        warning("8. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree");
        File fout = new File(org.fest.util.Files.temporaryFolder() + "/primates.xml");
        if (fout.exists()) {
            fout.delete();
        }

		// 9. Set up MCMC parameters
		warning("8. Set up MCMC parameters");
		f = f.selectTab("MCMC");
		beautiFrame.textBox("chainLength").selectAll().setText("2000000");


        fout = new File(org.fest.util.Files.temporaryFolder() + "/divtutorial.xml");
        if (fout.exists()) {
            fout.delete();
        }
		saveFile(""+org.fest.util.Files.temporaryFolder(), "divtutorial.xml");

		makeSureXMLParses();

        long t1 = System.currentTimeMillis();
        System.err.println("total time: " + (t1 - t0) / 1000 + " seconds");

    }

    private void checkTableContents(JTabbedPaneFixture f, String str) {
        f.selectTab("Partitions");
        JTableFixture t = beautiFrame.table();
        checkTableContents(t, str);
    }

    
    @Test
    public void DivergenceDatingTutorialWithEmpiricalFreqs() throws Exception {
        try {
            long t0 = System.currentTimeMillis();
            ScreenshotTaker screenshotTaker = new ScreenshotTaker();
            beauti.frame.setSize(1200, 800);

            String BASE_DIR = PREFIX.substring(0, PREFIX.lastIndexOf('/'));
            for (File file : new File(BASE_DIR).listFiles()) {
                if (file.getAbsolutePath().contains(PREFIX) && file.getName().endsWith(".png")) {
                    file.delete();
                }
            }

            // 0. Load primate-mtDNA.nex
            warning("// 0. Load primate-mtDNA.nex");
            importAlignment("examples/nexus", new File("primate-mtDNA.nex"));
            screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "DataPartitions.png");

            beautiFrame.menuItemWithPath("Mode", "Automatic set fix mean substitution rate flag").click();

            JTabbedPaneFixture f = beautiFrame.tabbedPane();
            f.requireVisible();
            f.requireTitle("Partitions", Index.atIndex(0));
            String[] titles = f.tabTitles();
            assertArrayEquals(titles, "[Partitions, Tip Dates, Site Model, Clock Model, Priors, MCMC]");
            System.err.println(Arrays.toString(titles));
            f = f.selectTab("Partitions");

            // inspect alignment
            JTableFixture t = beautiFrame.table();
            t.selectCell(TableCell.row(0).column(2)).doubleClick();
            DialogFixture dlg = WindowFinder.findDialog("AlignmentViewer").using(robot());
            dlg.target.setSize(768, 300);
            dlg.checkBox("UseColor").check();
            screenshotTaker.saveComponentAsPng(dlg.target, PREFIX + "Alignment.png");
            dlg.close();

            // check table
            printTableContents(t);
            checkTableContents(f, "[coding, primate-mtDNA, 12, 693, nucleotide, coding, coding, coding, false]*" +
                    "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, noncoding, noncoding, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, 1stpos, 1stpos, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, 2ndpos, 2ndpos, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, 3rdpos, 3rdpos, false]");

            assertThat(f).isNotNull();
            printBeautiState(f);
            assertStateEquals("Tree.t:noncoding", "clockRate.c:noncoding", "birthRate.t:noncoding", "Tree.t:2ndpos", "clockRate.c:2ndpos", "birthRate.t:2ndpos", "Tree.t:1stpos", "clockRate.c:1stpos", "birthRate.t:1stpos", "Tree.t:coding", "birthRate.t:coding", "Tree.t:3rdpos", "clockRate.c:3rdpos", "birthRate.t:3rdpos");
            assertOperatorsEqual("StrictClockRateScaler.c:noncoding", "YuleBirthRateScaler.t:noncoding", "YuleModelTreeScaler.t:noncoding", "YuleModelTreeRootScaler.t:noncoding", "YuleModelUniformOperator.t:noncoding", "YuleModelSubtreeSlide.t:noncoding", "YuleModelNarrow.t:noncoding", "YuleModelWide.t:noncoding", "YuleModelWilsonBalding.t:noncoding", "StrictClockRateScaler.c:2ndpos", "YuleBirthRateScaler.t:2ndpos", "YuleModelTreeScaler.t:2ndpos", "YuleModelTreeRootScaler.t:2ndpos", "YuleModelUniformOperator.t:2ndpos", "YuleModelSubtreeSlide.t:2ndpos", "YuleModelNarrow.t:2ndpos", "YuleModelWide.t:2ndpos", "YuleModelWilsonBalding.t:2ndpos", "StrictClockRateScaler.c:1stpos", "YuleBirthRateScaler.t:1stpos", "YuleModelTreeScaler.t:1stpos", "YuleModelTreeRootScaler.t:1stpos", "YuleModelUniformOperator.t:1stpos", "YuleModelSubtreeSlide.t:1stpos", "YuleModelNarrow.t:1stpos", "YuleModelWide.t:1stpos", "YuleModelWilsonBalding.t:1stpos", "YuleBirthRateScaler.t:coding", "YuleModelTreeScaler.t:coding", "YuleModelTreeRootScaler.t:coding", "YuleModelUniformOperator.t:coding", "YuleModelSubtreeSlide.t:coding", "YuleModelNarrow.t:coding", "YuleModelWide.t:coding", "YuleModelWilsonBalding.t:coding", "StrictClockRateScaler.c:3rdpos", "YuleBirthRateScaler.t:3rdpos", "YuleModelTreeScaler.t:3rdpos", "YuleModelTreeRootScaler.t:3rdpos", "YuleModelUniformOperator.t:3rdpos", "YuleModelSubtreeSlide.t:3rdpos", "YuleModelNarrow.t:3rdpos", "YuleModelWide.t:3rdpos", "YuleModelWilsonBalding.t:3rdpos", "strictClockUpDownOperator.c:3rdpos", "strictClockUpDownOperator.c:1stpos", "strictClockUpDownOperator.c:2ndpos", "strictClockUpDownOperator.c:noncoding");
            assertPriorsEqual("YuleModel.t:coding", "YuleModel.t:noncoding", "YuleModel.t:1stpos", "YuleModel.t:2ndpos", "YuleModel.t:3rdpos", "ClockPrior.c:noncoding", "YuleBirthRatePrior.t:noncoding", "ClockPrior.c:2ndpos", "YuleBirthRatePrior.t:2ndpos", "ClockPrior.c:1stpos", "YuleBirthRatePrior.t:1stpos", "YuleBirthRatePrior.t:coding", "ClockPrior.c:3rdpos", "YuleBirthRatePrior.t:3rdpos");


            //1. Delete "coding" partition as it covers the same sites as the 1stpos, 2ndpos and 3rdpos partitions.
            warning("1. Delete \"coding\" partition as it covers the same sites as the 1stpos, 2ndpos and 3rdpos partitions.");
            f.selectTab("Partitions");
            t = beautiFrame.table();
            t.selectCell(TableCell.row(0).column(2));
            JButtonFixture deleteButton = beautiFrame.button("-");
            deleteButton.click();
            printBeautiState(f);
            checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, noncoding, noncoding, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, 1stpos, 1stpos, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, 2ndpos, 2ndpos, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, 3rdpos, 3rdpos, false]");
            assertStateEquals("Tree.t:noncoding", "birthRate.t:noncoding", "Tree.t:3rdpos", "clockRate.c:3rdpos", "birthRate.t:3rdpos", "Tree.t:1stpos", "clockRate.c:1stpos", "birthRate.t:1stpos", "Tree.t:2ndpos", "clockRate.c:2ndpos", "birthRate.t:2ndpos");
            assertOperatorsEqual("YuleBirthRateScaler.t:noncoding", "YuleModelTreeScaler.t:noncoding", "YuleModelTreeRootScaler.t:noncoding", "YuleModelUniformOperator.t:noncoding", "YuleModelSubtreeSlide.t:noncoding", "YuleModelNarrow.t:noncoding", "YuleModelWide.t:noncoding", "YuleModelWilsonBalding.t:noncoding", "StrictClockRateScaler.c:3rdpos", "YuleBirthRateScaler.t:3rdpos", "YuleModelTreeScaler.t:3rdpos", "YuleModelTreeRootScaler.t:3rdpos", "YuleModelUniformOperator.t:3rdpos", "YuleModelSubtreeSlide.t:3rdpos", "YuleModelNarrow.t:3rdpos", "YuleModelWide.t:3rdpos", "YuleModelWilsonBalding.t:3rdpos", "StrictClockRateScaler.c:1stpos", "YuleBirthRateScaler.t:1stpos", "YuleModelTreeScaler.t:1stpos", "YuleModelTreeRootScaler.t:1stpos", "YuleModelUniformOperator.t:1stpos", "YuleModelSubtreeSlide.t:1stpos", "YuleModelNarrow.t:1stpos", "YuleModelWide.t:1stpos", "YuleModelWilsonBalding.t:1stpos", "StrictClockRateScaler.c:2ndpos", "YuleBirthRateScaler.t:2ndpos", "YuleModelTreeScaler.t:2ndpos", "YuleModelTreeRootScaler.t:2ndpos", "YuleModelUniformOperator.t:2ndpos", "YuleModelSubtreeSlide.t:2ndpos", "YuleModelNarrow.t:2ndpos", "YuleModelWide.t:2ndpos", "YuleModelWilsonBalding.t:2ndpos", "strictClockUpDownOperator.c:3rdpos", "strictClockUpDownOperator.c:2ndpos", "strictClockUpDownOperator.c:1stpos");
            assertPriorsEqual("YuleModel.t:1stpos", "YuleModel.t:2ndpos", "YuleModel.t:3rdpos", "YuleModel.t:noncoding", "YuleBirthRatePrior.t:1stpos", "YuleBirthRatePrior.t:2ndpos", "YuleBirthRatePrior.t:3rdpos", "YuleBirthRatePrior.t:noncoding", "ClockPrior.c:1stpos", "ClockPrior.c:2ndpos", "ClockPrior.c:3rdpos");

            //2a. Link trees...
            warning("2a. Link trees...");
            f.selectTab("Partitions");
            t = beautiFrame.table();
            t.selectCells(TableCell.row(0).column(2), TableCell.row(1).column(2), TableCell.row(2).column(2), TableCell.row(3).column(2));
            JButtonFixture linkTreesButton = beautiFrame.button("Link Trees");
            linkTreesButton.click();
            printBeautiState(f);
            assertStateEquals("Tree.t:noncoding", "birthRate.t:noncoding", "clockRate.c:2ndpos", "clockRate.c:3rdpos", "clockRate.c:1stpos");
            assertOperatorsEqual("YuleBirthRateScaler.t:noncoding", "YuleModelTreeScaler.t:noncoding", "YuleModelTreeRootScaler.t:noncoding", "YuleModelUniformOperator.t:noncoding", "YuleModelSubtreeSlide.t:noncoding", "YuleModelNarrow.t:noncoding", "YuleModelWide.t:noncoding", "YuleModelWilsonBalding.t:noncoding", "StrictClockRateScaler.c:2ndpos", "StrictClockRateScaler.c:3rdpos", "StrictClockRateScaler.c:1stpos", "strictClockUpDownOperator.c:2ndpos", "strictClockUpDownOperator.c:1stpos", "strictClockUpDownOperator.c:3rdpos");
            assertPriorsEqual("YuleModel.t:noncoding", "YuleBirthRatePrior.t:noncoding", "ClockPrior.c:1stpos", "ClockPrior.c:2ndpos", "ClockPrior.c:3rdpos");

            //2b. ...and call the tree "tree"
            warning("2b. ...and call the tree \"tree\"");
            f.selectTab("Partitions");
            JTableCellFixture cell = beautiFrame.table().cell(TableCell.row(0).column(7));
            Component editor = cell.editor();
            JComboBoxFixture comboBox = new JComboBoxFixture(robot(), (JComboBox<?>) editor);
            cell.startEditing();
            comboBox.selectAllText();
            comboBox.enterText("tree");
            comboBox.pressAndReleaseKeys(KeyEvent.VK_ENTER);
            cell.stopEditing();
            checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, noncoding, tree, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, 1stpos, tree, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, 2ndpos, tree, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, 3rdpos, tree, false]");
            printBeautiState(f);
            assertStateEquals("clockRate.c:2ndpos", "Tree.t:tree", "birthRate.t:tree", "clockRate.c:1stpos", "clockRate.c:3rdpos");
            assertOperatorsEqual("StrictClockRateScaler.c:2ndpos", "YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "StrictClockRateScaler.c:1stpos", "StrictClockRateScaler.c:3rdpos", "strictClockUpDownOperator.c:3rdpos", "strictClockUpDownOperator.c:2ndpos", "strictClockUpDownOperator.c:1stpos");
            assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "ClockPrior.c:1stpos", "ClockPrior.c:2ndpos", "ClockPrior.c:3rdpos");


            //3a. Link clocks
            warning("3a. Link clocks");
            f.selectTab("Partitions");
            t = beautiFrame.table();
            t.selectCells(TableCell.row(0).column(2), TableCell.row(1).column(2), TableCell.row(2).column(2), TableCell.row(3).column(2));
            JButtonFixture linkClocksButton = beautiFrame.button("Link Clock Models");
            linkClocksButton.click();

            //3b. and call the uncorrelated relaxed molecular clock "clock"
            warning("3b. and call the uncorrelated relaxed molecular clock \"clock\"");
            cell = beautiFrame.table().cell(TableCell.row(0).column(6));
            editor = cell.editor();
            comboBox = new JComboBoxFixture(robot(), (JComboBox<?>) editor);
            cell.startEditing();
            comboBox.selectAllText();
            comboBox.enterText("clock");
            comboBox.pressAndReleaseKeys(KeyEvent.VK_ENTER);
            cell.stopEditing();
            printBeautiState(f);
            checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, clock, tree, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, clock, tree, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, clock, tree, false]");
            assertStateEquals("Tree.t:tree", "birthRate.t:tree");
            assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree");
            assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree");
            screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "DataPartitions_final.png");

            //4. Link site models temporarily in order to set the same model for all of them.
            warning("4. Link site models temporarily in order to set the same model for all of them.");
            f.selectTab("Partitions");
            t = beautiFrame.table();
            t.selectCells(TableCell.row(0).column(2), TableCell.row(1).column(2), TableCell.row(2).column(2), TableCell.row(3).column(2));
            JButtonFixture linkSiteModelsButton = beautiFrame.button("Link Site Models");
            linkSiteModelsButton.click();
            printBeautiState(f);
            checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]");
            assertStateEquals("Tree.t:tree", "birthRate.t:tree");
            assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree");
            assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree");

            //5. Set the site model to HKY+G4 (estimated)
            warning("5. Set the site model to HKY+G4 (estimated)");
            f.selectTab("Site Model");
            JComboBoxFixture substModel = beautiFrame.comboBox("substModel");
            substModel.selectItem("HKY");

            JComboBoxFixture freqs = beautiFrame.comboBox("frequencies");
            freqs.selectItem("Empirical");

            JTextComponentFixture categoryCount = beautiFrame.textBox("gammaCategoryCount");
            categoryCount.setText("4");

            JCheckBoxFixture shapeIsEstimated = beautiFrame.checkBox("shape.isEstimated");
            shapeIsEstimated.check();

            beautiFrame.checkBox("mutationRate.isEstimated").check();
            beautiFrame.checkBox("FixMeanMutationRate").check();
            screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "Model.png");
            printBeautiState(f);
            checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]");
            assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding");
            assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "KappaScaler.s:noncoding", "gammaShapeScaler.s:noncoding", "FixMeanMutationRatesOperator");
            assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "KappaPrior.s:noncoding", "GammaShapePrior.s:noncoding");
            assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding");

            //6 Unlink the site models,
            warning("6 Unlink the site models,");
            f.selectTab("Partitions");
            t = beautiFrame.table();
            t.selectCells(TableCell.row(0).column(2), TableCell.row(1).column(2), TableCell.row(2).column(2), TableCell.row(3).column(2));
            beautiFrame.button("Unlink Site Models").click();
            printBeautiState(f);
            checkTableContents(f, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, clock, tree, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, clock, tree, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, clock, tree, false]");

            assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos");
            assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "KappaScaler.s:noncoding", "gammaShapeScaler.s:noncoding", "FixMeanMutationRatesOperator", "gammaShapeScaler.s:1stpos", "KappaScaler.s:1stpos", "gammaShapeScaler.s:2ndpos", "KappaScaler.s:2ndpos", "gammaShapeScaler.s:3rdpos", "KappaScaler.s:3rdpos");
            assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "KappaPrior.s:noncoding", "GammaShapePrior.s:noncoding", "GammaShapePrior.s:1stpos", "KappaPrior.s:1stpos", "GammaShapePrior.s:2ndpos", "KappaPrior.s:2ndpos", "GammaShapePrior.s:3rdpos", "KappaPrior.s:3rdpos");
            assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos");


            //7a. Create a Normal calibration prior
            warning("7a. Create a Normal calibration prior");
            f.selectTab("Priors");
            beautiFrame.comboBox("TreeDistribution").selectItem("Calibrated Yule Model");
            screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "Prior1.png");

            Component c = beautiFrame.robot.finder().findByName("addItem");
            JButtonFixture addButton = new JButtonFixture(robot(), (JButton) c);
            addButton.click();
            JOptionPaneFixture dialog = new JOptionPaneFixture(robot());
            dialog.textBox("idEntry").setText("Human-Chimp");
            dialog.list("listOfTaxonCandidates").selectItems("Homo_sapiens", "Pan");
            dialog.button(">>").click();
            dialog.okButton().click();
            printBeautiState(f);
            assertStateEquals("Tree.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos", "birthRateY.t:tree");
            assertOperatorsEqual("CalibratedYuleModelTreeScaler.t:tree", "CalibratedYuleModelTreeRootScaler.t:tree", "CalibratedYuleModelUniformOperator.t:tree", "CalibratedYuleModelSubtreeSlide.t:tree", "CalibratedYuleModelNarrow.t:tree", "CalibratedYuleModelWide.t:tree", "CalibratedYuleModelWilsonBalding.t:tree", "KappaScaler.s:noncoding", "gammaShapeScaler.s:noncoding", "FixMeanMutationRatesOperator", "gammaShapeScaler.s:1stpos", "KappaScaler.s:1stpos", "gammaShapeScaler.s:2ndpos", "KappaScaler.s:2ndpos", "gammaShapeScaler.s:3rdpos", "KappaScaler.s:3rdpos", "CalibratedYuleBirthRateScaler.t:tree");
            assertPriorsEqual("CalibratedYuleModel.t:tree", "CalibratedYuleBirthRatePrior.t:tree", "GammaShapePrior.s:1stpos", "GammaShapePrior.s:2ndpos", "GammaShapePrior.s:3rdpos", "GammaShapePrior.s:noncoding", "KappaPrior.s:1stpos", "KappaPrior.s:2ndpos", "KappaPrior.s:3rdpos", "KappaPrior.s:noncoding", "Human-Chimp.prior");
            assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos", "CalibratedYuleModel.t:tree", "birthRateY.t:tree", "Human-Chimp.prior");

            //7b. and monophyletic constraint on Human-Chimp split of 6 +/- 0.5.
            warning("7b. and monophyletic constraint on Human-Chimp split of 6 +/- 0.5.");
            f.selectTab("Priors");
            beautiFrame.checkBox("Human-Chimp.prior.isMonophyletic").click();
            beautiFrame.comboBox("Human-Chimp.prior.distr").selectItem("Normal");
            beautiFrame.button("Human-Chimp.prior.editButton").click();
            beautiFrame.textBox("mean").selectAll().setText("6");
            beautiFrame.textBox("sigma").selectAll().setText("0.5");
            // beautiFrame.scrollBar().scrollToMaximum();
            screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "TaxonSets.png");
            printBeautiState(f);
            assertStateEquals("Tree.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos", "birthRateY.t:tree", "clockRate.c:clock");
            assertOperatorsEqual("CalibratedYuleModelTreeScaler.t:tree", "CalibratedYuleModelTreeRootScaler.t:tree", "CalibratedYuleModelUniformOperator.t:tree", "CalibratedYuleModelSubtreeSlide.t:tree", "CalibratedYuleModelNarrow.t:tree", "CalibratedYuleModelWide.t:tree", "CalibratedYuleModelWilsonBalding.t:tree", "KappaScaler.s:noncoding", "gammaShapeScaler.s:noncoding", "FixMeanMutationRatesOperator", "gammaShapeScaler.s:1stpos", "KappaScaler.s:1stpos", "gammaShapeScaler.s:2ndpos", "KappaScaler.s:2ndpos", "gammaShapeScaler.s:3rdpos", "KappaScaler.s:3rdpos", "CalibratedYuleBirthRateScaler.t:tree", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock");
            assertPriorsEqual("CalibratedYuleModel.t:tree", "CalibratedYuleBirthRatePrior.t:tree", "GammaShapePrior.s:1stpos", "GammaShapePrior.s:2ndpos", "GammaShapePrior.s:3rdpos", "GammaShapePrior.s:noncoding", "KappaPrior.s:1stpos", "KappaPrior.s:2ndpos", "KappaPrior.s:3rdpos", "KappaPrior.s:noncoding", "Human-Chimp.prior", "ClockPrior.c:clock");
            assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos", "CalibratedYuleModel.t:tree", "birthRateY.t:tree", "Human-Chimp.prior", "clockRate.c:clock");


            //7c. set gamma priors on birth rate and clock rate
            warning("7c. set gamma priors on birth rate and clock rate");
            beautiFrame.comboBox("birthRateY.t:tree.distr").selectItem("Gamma");
            beautiFrame.button("CalibratedYuleBirthRatePrior.t:tree.editButton").click();
            beautiFrame.textBox("alpha").selectAll().setText("0.001");
            beautiFrame.textBox("beta").selectAll().setText("1000");
            beautiFrame.button("CalibratedYuleBirthRatePrior.t:tree.editButton").click();

            beautiFrame.comboBox("clockRate.c:clock.distr").selectItem("Gamma");
            beautiFrame.button("ClockPrior.c:clock.editButton").click();
            beautiFrame.textBox("alpha").selectAll().setText("0.001");
            beautiFrame.textBox("beta").selectAll().setText("1000");
            printBeautiState(f);
            assertStateEquals("Tree.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos", "birthRateY.t:tree", "clockRate.c:clock");
            assertOperatorsEqual("CalibratedYuleModelTreeScaler.t:tree", "CalibratedYuleModelTreeRootScaler.t:tree", "CalibratedYuleModelUniformOperator.t:tree", "CalibratedYuleModelSubtreeSlide.t:tree", "CalibratedYuleModelNarrow.t:tree", "CalibratedYuleModelWide.t:tree", "CalibratedYuleModelWilsonBalding.t:tree", "KappaScaler.s:noncoding", "gammaShapeScaler.s:noncoding", "FixMeanMutationRatesOperator", "gammaShapeScaler.s:1stpos", "KappaScaler.s:1stpos", "gammaShapeScaler.s:2ndpos", "KappaScaler.s:2ndpos", "gammaShapeScaler.s:3rdpos", "KappaScaler.s:3rdpos", "CalibratedYuleBirthRateScaler.t:tree", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock");
            assertPriorsEqual("CalibratedYuleModel.t:tree", "CalibratedYuleBirthRatePrior.t:tree", "ClockPrior.c:clock", "GammaShapePrior.s:1stpos", "GammaShapePrior.s:2ndpos", "GammaShapePrior.s:3rdpos", "GammaShapePrior.s:noncoding", "KappaPrior.s:1stpos", "KappaPrior.s:2ndpos", "KappaPrior.s:3rdpos", "KappaPrior.s:noncoding", "Human-Chimp.prior");
            assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos", "CalibratedYuleModel.t:tree", "birthRateY.t:tree", "Human-Chimp.prior", "clockRate.c:clock");


            //8. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
            warning("8. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree");
            File fout = new File(org.fest.util.Files.temporaryFolder() + "/primates.xml");
            if (fout.exists()) {
                fout.delete();
            }
            makeSureXMLParses();

            long t1 = System.currentTimeMillis();
            System.err.println("total time: " + (t1 - t0) / 1000 + " seconds");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


}

