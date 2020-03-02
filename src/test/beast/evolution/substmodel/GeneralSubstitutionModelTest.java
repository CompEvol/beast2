package test.beast.evolution.substmodel;

import beast.core.parameter.RealParameter;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.evolution.tree.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.DoubleStream;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

/**
 * This tests transition probability matrix from {@link GeneralSubstitutionModel} given rates.
 * Instantaneous rate q_ij can be 0, but the transition prob p_ij(t) cannot.
 *
 * @author Walter Xie
 */
public class GeneralSubstitutionModelTest {
    GeneralSubstitutionModel geneSubstModel;

    @Before
    public void setUp() {
        RealParameter f = new RealParameter(new Double[]{0.3333333, 0.3333333, 0.3333333});
        Frequencies freqs = new Frequencies();
        freqs.initByName("frequencies", f, "estimate", false);

        // A -> B -> C, not A -> C
        // off-diagonal: NrOfStates * (NrOfStates - 1)
        Double[] r = new Double[]{0.1, 0.0, 0.1, 0.2, 0.0, 0.2};
        RealParameter rates = new RealParameter(r);
        geneSubstModel = new GeneralSubstitutionModel();
        geneSubstModel.initByName("frequencies", freqs, "rates", rates);
    }

    @Test
    public void getTransitionProbabilities() {
//        double startTime = 1E-10; // when genetic distance -> 1E-10, P(t) may has 0.
        double startTime = 1;
        double endTime = 0;
        double rate = 1;

        System.out.println("freqs = \n" + Arrays.toString(geneSubstModel.getFrequencies()) + "\n");

        int len = geneSubstModel.getStateCount();
        double[] prob = new double[len*len];
        geneSubstModel.getTransitionProbabilities(new Node(), startTime, endTime, rate, prob, true);

        System.out.println("relative rates :\n" +
                Arrays.toString(geneSubstModel.getRelativeRates()) + "\n");
        System.out.println("\nrenormalised rate matrix :");
        double[][] rateM = geneSubstModel.getRateMatrix();
        for(int i = 0; i < rateM.length; i++)
            System.out.println(Arrays.toString(rateM[i]));
        System.out.println("\ntransition prob :\n" + Arrays.toString(prob));

        // P(t) row sum to 1
        for (int i=0; i < len; i++) {
            double[] row = new double[len];
            System.arraycopy(prob, i*len, row, 0, len);
            double sum = DoubleStream.of(row).sum();
            System.out.println("row " + i + " prob sum = " + sum);
            assertEquals(1, sum, 1e-15);
        }

//        for (int i=0; i < prob.length; i++)
//            assertTrue(prob[i] > 0);

        assertArrayEquals(new double[]{
                0.6674871505723157, 0.22927797953210746, 0.10323486989557647,
                0.2292779795321076, 0.415400931299253, 0.35532108916863914,
                0.10323486989557647, 0.3553210891686388, 0.5414440409357847}, prob, 0.0);
    }
}
