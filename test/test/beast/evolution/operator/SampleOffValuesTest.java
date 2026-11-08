package test.beast.evolution.operator;

import beast.base.evolution.tree.coalescent.SampleOffValues;
import beast.base.inference.State;
import beast.base.inference.distribution.Exponential;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * SampleOffValues redraws the "off" values from {@code dist}. When {@code dist} is the
 * prior on the values, the move is a Gibbs move, so its Hastings ratio must cancel the
 * change in the prior exactly.
 */
public class SampleOffValuesTest {

    private static final Double[] START = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
    // indicator k switches value k+1; values 2, 4 and 5 are off
    private static final Boolean[] INDICATORS = {true, false, true, false, false};

    private RealParameter values;
    private BooleanParameter indicators;
    private Exponential prior;

    @BeforeEach
    void setUp() {
        Randomizer.setSeed(127);
        values = new RealParameter(START.clone());
        values.setID("values");
        indicators = new BooleanParameter(INDICATORS.clone());
        indicators.setID("indicators");
        State state = new State();
        state.initByName("stateNode", values, "stateNode", indicators);
        state.initialise();
        prior = new Exponential();
        prior.initByName("mean", new RealParameter("3.0"));
    }

    private SampleOffValues operator(boolean all) {
        SampleOffValues op = new SampleOffValues();
        op.initByName("values", values, "indicators", indicators, "dist", prior, "all", all, "weight", 1.0);
        return op;
    }

    private double logPrior() {
        double logP = 0;
        for (int i = 0; i < values.getDimension(); i++) {
            logP += prior.logDensity(values.getValue(i));
        }
        return logP;
    }

    private void assertGibbs(SampleOffValues op) {
        for (int k = 0; k < 1000; k++) {
            final double before = logPrior();
            final double logHR = op.proposal();
            final double after = logPrior();
            assertEquals(0.0, logHR + after - before, 1e-9, "log acceptance ratio at proposal " + k);
        }
    }

    @Test
    void testSingleValueMoveIsGibbs() {
        assertGibbs(operator(false));
        assertOnValuesUnchangedAndOffValuesRedrawn();
    }

    @Test
    void testAllValuesMoveIsGibbs() {
        assertGibbs(operator(true));
        assertOnValuesUnchangedAndOffValuesRedrawn();
    }

    private void assertOnValuesUnchangedAndOffValuesRedrawn() {
        for (int i : new int[]{0, 1, 3}) {
            assertEquals(START[i], values.getValue(i), 0.0, "on value " + i + " changed");
        }
        for (int i : new int[]{2, 4, 5}) {
            assertNotEquals(START[i], values.getValue(i), "off value " + i + " was never redrawn");
        }
    }
}
