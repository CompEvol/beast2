package test.beast.evolution.speciation;

import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.evolution.speciation.CalibratedYuleModel;
import beast.base.evolution.speciation.CalibrationPoint;
import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.parameter.RealParameter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The calibrated Yule density has a finite limit as the birth rate goes to 0, but it used
 * to compute 1 - exp(-lam * h) directly. Once lam * h is below about 1e-16, exp(-lam * h)
 * rounds to 1, so that term became 0, its log -infinity, and the log density +infinity;
 * with a proper prior that lets the birth rate get that small, MCMC stopped with
 * "Encountered a positive infinite posterior". The two-calibration marginal density also
 * lost all precision for small lam, because its constant and linear terms cancel.
 */
public class CalibratedYuleSmallBirthRateTest {

    private static final double[] SMALL_RATES = {1e-9, 1e-12, 1e-15, 1e-18, 1e-30, 1e-100, 1e-300};

    private static TaxonSet taxa(Tree tree, String... names) {
        List<Taxon> list = new ArrayList<>();
        for (String name : names) list.add(new Taxon(name));
        TaxonSet set = new TaxonSet();
        set.initByName("taxon", list);
        return set;
    }

    private static CalibrationPoint calibration(TaxonSet set, double M, double S) {
        LogNormalDistributionModel distr = new LogNormalDistributionModel();
        distr.initByName("M", new RealParameter(Double.toString(M)), "S", new RealParameter(Double.toString(S)));
        CalibrationPoint cal = new CalibrationPoint();
        cal.initByName("taxonset", set, "distr", distr);
        return cal;
    }

    private static double[] logP(Tree tree, List<CalibrationPoint> cals, double... rates) {
        RealParameter lambda = new RealParameter("1.0");
        CalibratedYuleModel model = new CalibratedYuleModel();
        model.initByName("tree", tree, "birthRate", lambda, "calibrations", cals, "type", CalibratedYuleModel.Type.OVER_ALL_TOPOS);
        double[] out = new double[rates.length];
        for (int i = 0; i < rates.length; i++) {
            lambda.setValue(rates[i]);
            out[i] = model.calculateLogP();
        }
        return out;
    }

    private static void assertFiniteLimit(double[] values) {
        for (int i = 0; i < values.length; i++) {
            assertTrue(Double.isFinite(values[i]), "log density not finite at lambda = " + SMALL_RATES[i]);
            assertEquals(values[0], values[i], 1e-6, "log density at lambda = " + SMALL_RATES[i]);
        }
    }

    /** Root calibration only: the case that made testConditionalRootCalibrationPrior stop. */
    @Test
    public void testRootCalibrationAtSmallBirthRate() {
        Tree tree = new TreeParser("((1:0.3628788136119008,2:0.3628788136119008):6.879159775547439,"
                + "(3:0.2992802234232248,4:0.2992802234232248):6.942758365736115);", false);
        List<CalibrationPoint> cals = List.of(calibration(taxa(tree, "1", "2", "3", "4"), 2.0, 0.2));

        assertFiniteLimit(logP(tree, cals, SMALL_RATES));
        // unchanged at an ordinary birth rate (the same value as before the fix)
        assertEquals(-1.9551789313971977, logP(tree, cals, 1.0)[0], 1e-12);
    }

    /** A nested clade and the root: uses the two-calibration marginal density. */
    @Test
    public void testNestedCalibrationsAtSmallBirthRate() {
        Tree tree = new TreeParser("(((1:0.5,2:0.5):1.5,3:2.0):3.0,(4:1.0,5:1.0):4.0);", false);
        List<CalibrationPoint> cals = List.of(
                calibration(taxa(tree, "1", "2", "3"), 0.7, 0.3),
                calibration(taxa(tree, "1", "2", "3", "4", "5"), 1.6, 0.3));

        assertFiniteLimit(logP(tree, cals, SMALL_RATES));
        double[] ordinary = logP(tree, cals, 1.0, 0.1);
        assertTrue(Double.isFinite(ordinary[0]) && Double.isFinite(ordinary[1]));
    }
}
