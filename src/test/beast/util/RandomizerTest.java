package test.beast.util;

import org.junit.Test;

import beast.util.DiscreteStatistics;
import beast.util.Randomizer;

import static org.junit.Assert.assertEquals;

public class RandomizerTest {

    @Test
    public void logNormalTest() {

        Randomizer.setSeed(1);

        double M1=1, M2=0, S=0.5;

        int reps=10000000;

        double [] vals1 = new double[reps];
        double [] vals2 = new double[reps];

        for (int i=0; i<reps; i++) {
            vals1[i] = Randomizer.nextLogNormal(M1, S, true);
            vals2[i] = Randomizer.nextLogNormal(M2, S, false);
        }

        assertEquals(1.0, DiscreteStatistics.mean(vals1), 1e-3);
        assertEquals(0.2840254,DiscreteStatistics.variance(vals1), 1e-3);

        assertEquals(1.133148, DiscreteStatistics.mean(vals2), 1e-3);
        assertEquals( 0.3646959, DiscreteStatistics.variance(vals2), 1e-3);
    }
}
