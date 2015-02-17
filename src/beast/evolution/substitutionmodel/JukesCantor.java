package beast.evolution.substitutionmodel;

import beast.core.Description;
import beast.core.Input.Validate;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.Nucleotide;
import beast.evolution.tree.Node;

import java.util.Arrays;

@Description("Jukes Cantor substitution model: all rates equal and " + "uniformly distributed frequencies")
public class JukesCantor extends SubstitutionModel.Base {

    public JukesCantor() {
        // this is added to avoid a parsing error inherited from superclass because frequencies are not provided.
        frequenciesInput.setRule(Validate.OPTIONAL);
        try {
            // this call will be made twice when constructed from XML
            // but this ensures that the object is validly constructed for testing purposes.
            initAndValidate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("initAndValidate() call failed when constructing JukesCantor()");
        }
    }

    double[] frequencies;
    EigenDecomposition eigenDecomposition;

    @Override
    public void initAndValidate() throws Exception {
        double[] eval = new double[]{0.0, -1.3333333333333333, -1.3333333333333333, -1.3333333333333333};
        double[] evec = new double[]{1.0, 2.0, 0.0, 0.5, 1.0, -2.0, 0.5, 0.0, 1.0, 2.0, 0.0, -0.5, 1.0, -2.0, -0.5, 0.0};
        double[] ivec = new double[]{0.25, 0.25, 0.25, 0.25, 0.125, -0.125, 0.125, -0.125, 0.0, 1.0, 0.0, -1.0, 1.0, 0.0, -1.0, 0.0};

        eigenDecomposition = new EigenDecomposition(evec, ivec, eval);

        if (frequenciesInput.get() != null) {
            throw new RuntimeException("Frequencies must not be specified in Jukes-Cantor model. They are assumed equal.");
        }

        frequencies = new double[]{0.25, 0.25, 0.25, 0.25};
    }

    @Override
    public double[] getFrequencies() {
        return frequencies;
    }

    @Override
    public void getTransitionProbabilities(Node node, double fStartTime, double fEndTime, double fRate, double[] matrix) {
        double fDelta = 4.0 / 3.0 * (fStartTime - fEndTime);
        double fPStay = (1.0 + 3.0 * Math.exp(-fDelta * fRate)) / 4.0;
        double fPMove = (1.0 - Math.exp(-fDelta * fRate)) / 4.0;
        // fill the matrix with move probabilities
        Arrays.fill(matrix, fPMove);
        // fill the diagonal
        for (int i = 0; i < 4; i++) {
            matrix[i * 5] = fPStay;
        }
    }

    @Override
    public EigenDecomposition getEigenDecomposition(Node node) {
        return eigenDecomposition;
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof Nucleotide;
    }
}
