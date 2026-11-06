package test.beast.evolution.speciation;

import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.speciation.CalibratedBirthDeathModel;
import beast.base.evolution.speciation.CalibratedYuleModel;
import beast.base.evolution.speciation.CalibrationPoint;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.Distribution;
import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.parameter.RealParameter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Calibrated Yule and birth-death densities at small birth rates. They used to
 * compute 1 - exp(-lam * h) directly: once lam * h was below about 1e-16, exp(-lam * h) rounded
 * to 1, the log became -infinity and the log density +infinity or NaN, from lam ~ 1e-18 (or
 * ~1e-9 for a clade calibrated together with the root, whose polynomial also cancelled).
 *
 * <p>For every model and calibration layout this checks that
 * <ul>
 *   <li>values at ordinary birth rates are unchanged: they equal the values computed before the
 *       fix, at lam = 1, 0.1 and 0.01;</li>
 *   <li>the log density is finite for lam from 1e-9 down to 1e-300;</li>
 *   <li>with the root calibrated, it is at its finite limit there; without, it falls like log lam
 *       (the density goes to 0 like lam, with nothing to cancel it).</li>
 * </ul>
 */
public class CalibratedSpeciationSmallBirthRateTest {

    private static final String NEWICK = "(((1:0.5,2:0.5):1.5,3:2.0):3.0,(4:1.0,5:1.0):4.0);";
    private static final String[] AB = {"1", "2"}, ABC = {"1", "2", "3"}, DE = {"4", "5"}, ALL = {"1", "2", "3", "4", "5"};
    private static final double[] SMALL_RATES = {1e-9, 1e-12, 1e-15, 1e-18, 1e-30, 1e-100, 1e-300};

    enum Model { YULE, BIRTH_DEATH, BIRTH_DEATH_WITH_DEATH_AND_SAMPLING }

    /** calibrations as (taxa, M, S) of a log-normal; rootCalibrated: whether the root is one of them */
    enum Layout {
        ROOT(new Object[][]{cal(ALL, 1.6, 0.3)}, true),
        CLADE_AND_ROOT(new Object[][]{cal(ABC, 0.7, 0.3), cal(ALL, 1.6, 0.3)}, true),
        TWO_NON_ROOT(new Object[][]{cal(ABC, 0.7, 0.3), cal(DE, 0.0, 0.3)}, false),
        THREE(new Object[][]{cal(AB, -0.7, 0.3), cal(ABC, 0.7, 0.3), cal(ALL, 1.6, 0.3)}, true);
        final Object[][] cals;
        final boolean rootCalibrated;
        Layout(Object[][] cals, boolean rootCalibrated) { this.cals = cals; this.rootCalibrated = rootCalibrated; }
    }

    private static Object[] cal(String[] taxa, double M, double S) {
        return new Object[]{taxa, M, S};
    }

    /** log density before the fix at lam = 1, 0.1 and 0.01 */
    static Stream<Arguments> cases() {
        return Stream.of(
                arguments(Model.YULE, Layout.ROOT, new double[]{-4.8046162495816700, -5.7843973882107065, -6.1135247287222790}),
                arguments(Model.YULE, Layout.CLADE_AND_ROOT, new double[]{-3.3113677096502200, -4.5796182300526610, -4.7989572452747300}),
                arguments(Model.YULE, Layout.TWO_NON_ROOT, new double[]{-11.670882926529202, -8.8636947694156340, -10.669630417557208}),
                arguments(Model.YULE, Layout.THREE, new double[]{-6.7663523538103500, -7.7443761243204060, -7.9203645843950150}),
                arguments(Model.BIRTH_DEATH, Layout.ROOT, new double[]{-4.8046162495816700, -5.7843973882107065, -6.1135247287222790}),
                arguments(Model.BIRTH_DEATH, Layout.CLADE_AND_ROOT, new double[]{-3.3113677096502200, -4.5796182300526610, -4.7989572452747300}),
                arguments(Model.BIRTH_DEATH, Layout.TWO_NON_ROOT, new double[]{-11.670882926529202, -8.8636947694156340, -10.669630417557208}),
                arguments(Model.BIRTH_DEATH, Layout.THREE, new double[]{-6.7663523538103500, -7.7443761243204060, -7.9203645843950150}),
                arguments(Model.BIRTH_DEATH_WITH_DEATH_AND_SAMPLING, Layout.ROOT, new double[]{-8.5849790001215570, -8.9075542913898320, -9.0113406578463720}),
                arguments(Model.BIRTH_DEATH_WITH_DEATH_AND_SAMPLING, Layout.CLADE_AND_ROOT, new double[]{-4.7891453671960640, -5.4001900402377410, -5.3941880814048530}),
                arguments(Model.BIRTH_DEATH_WITH_DEATH_AND_SAMPLING, Layout.TWO_NON_ROOT, new double[]{-7.2625565526248910, -3.7981625481505580, -5.3787572222371010}),
                arguments(Model.BIRTH_DEATH_WITH_DEATH_AND_SAMPLING, Layout.THREE, new double[]{-2.3580259799060377, -2.6788439030553306, -2.6294913890749090})
        );
    }

    private static TaxonSet taxa(String[] names) {
        List<Taxon> list = new ArrayList<>();
        for (String name : names) list.add(new Taxon(name));
        TaxonSet set = new TaxonSet();
        set.initByName("taxon", list);
        return set;
    }

    private static CalibrationPoint calibration(String[] names, double M, double S) {
        LogNormalDistributionModel d = new LogNormalDistributionModel();
        d.initByName("M", new RealParameter(Double.toString(M)), "S", new RealParameter(Double.toString(S)));
        CalibrationPoint cp = new CalibrationPoint();
        cp.initByName("taxonset", taxa(names), "distr", d);
        return cp;
    }

    private static double[] logP(Model model, Layout layout, double... rates) {
        Tree tree = new TreeParser(NEWICK, false);
        RealParameter lambda = new RealParameter("1.0");
        List<CalibrationPoint> cals = new ArrayList<>();
        for (Object[] c : layout.cals) cals.add(calibration((String[]) c[0], (Double) c[1], (Double) c[2]));
        Distribution density;
        switch (model) {
            case YULE -> {
                CalibratedYuleModel m = new CalibratedYuleModel();
                m.initByName("tree", tree, "birthRate", lambda, "calibrations", cals);
                density = m;
            }
            case BIRTH_DEATH -> {
                CalibratedBirthDeathModel m = new CalibratedBirthDeathModel();
                m.initByName("tree", tree, "birthRate", lambda, "calibrations", cals);
                density = m;
            }
            default -> {
                CalibratedBirthDeathModel m = new CalibratedBirthDeathModel();
                m.initByName("tree", tree, "birthRate", lambda, "calibrations", cals,
                        "relativeDeathRate", new RealParameter("0.3"), "sampleProbability", new RealParameter("0.8"));
                density = m;
            }
        }
        double[] out = new double[rates.length];
        for (int i = 0; i < rates.length; i++) {
            double rate = rates[i];
            lambda.setValue(rate);
            out[i] = density.calculateLogP();
        }
        return out;
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("cases")
    void ordinaryRatesUnchanged(Model model, Layout layout, double[] before) {
        double[] now = logP(model, layout, 1, 0.1, 0.01);
        for (int i = 0; i < before.length; i++) {
            assertEquals(before[i], now[i], 1e-10, "lambda = " + new double[]{1, 0.1, 0.01}[i]);
        }
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("cases")
    void smallRatesFiniteAndAtTheirLimit(Model model, Layout layout, double[] ignored) {
        double[] v = logP(model, layout, SMALL_RATES);
        for (int i = 0; i < v.length; i++) {
            assertTrue(Double.isFinite(v[i]), "log density not finite at lambda = " + SMALL_RATES[i]);
        }
        for (int i = 1; i < v.length; i++) {
            if (layout.rootCalibrated) {
                assertEquals(v[0], v[i], 1e-6, "not at its limit at lambda = " + SMALL_RATES[i]);
            } else {
                double slope = (v[i] - v[i - 1]) / Math.log(SMALL_RATES[i] / SMALL_RATES[i - 1]);
                assertEquals(1.0, slope, 1e-6, "not falling like log lambda at lambda = " + SMALL_RATES[i]);
            }
        }
    }
}
