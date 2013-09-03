package beast.evolution.substitutionmodel;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.TwoStateCovarion;


/**
 * <p/>
 * a	the rate of the slow rate class
 * 1	the rate of the fast rate class
 * p0	the equilibrium frequency of zero states
 * p1	1 - p0, the equilibrium frequency of one states
 * f0	the equilibrium frequency of slow rate class
 * f1	1 - f0, the equilibrium frequency of fast rate class
 * s	the rate of switching
 * <p/>
 * then the (unnormalized) instantaneous rate matrix (unnormalized Q) should be
 * <p/>
 * [ -(a*p1)-s ,   a*p1    ,    s   ,   0   ]
 * [   a*p0    , -(a*p0)-s ,    0   ,   s   ]
 * [    s      ,     0     ,  -p1-s ,  p1   ]
 * [    0      ,     s     ,    p0  , -p0-s ]
 */
@Description("Covarion model for Binary data")
public class BinaryCovarion extends GeneralSubstitutionModel {
    public Input<RealParameter> alphaInput = new Input<RealParameter>("alpha", "the rate of evolution in slow mode", Validate.REQUIRED);
    public Input<RealParameter> switchRateInput = new Input<RealParameter>("switchRate", "the rate of flipping between slow and fast modes", Validate.REQUIRED);
    public Input<RealParameter> frequenciesInput = new Input<RealParameter>("vfrequencies", "the frequencies of the visible states", Validate.REQUIRED);
    public Input<RealParameter> hfrequenciesInput = new Input<RealParameter>("hfrequencies", "the frequencies of the hidden rates", Validate.REQUIRED);

    private RealParameter alpha;
    private RealParameter switchRate;
    private RealParameter frequencies;
    private RealParameter hiddenFrequencies;

    protected double[][] unnormalizedQ;
    protected double[][] storedUnnormalizedQ;
    int stateCount;

    public BinaryCovarion() {
        ratesInput.setRule(Validate.OPTIONAL);
        frequenciesInput.setRule(Validate.OPTIONAL);
    }

    @Override
    public void initAndValidate() throws Exception {
        alpha = alphaInput.get();
        switchRate = switchRateInput.get();
        frequencies = frequenciesInput.get();
        hiddenFrequencies = hfrequenciesInput.get();

        if (alpha.getDimension() != 1) {
            throw new Exception("alpha should have dimension 1");
        }
        if (switchRate.getDimension() != 1) {
            throw new Exception("switchRate should have dimension 1");
        }
        if (frequencies.getDimension() != 2) {
            throw new Exception("frequencies should have dimension 2");
        }
        if (hiddenFrequencies.getDimension() != 2) {
            throw new Exception("hiddenFrequenciesshould have dimension 2");
        }

        nrOfStates = 4;
        unnormalizedQ = new double[4][4];
        storedUnnormalizedQ = new double[4][4];

        updateMatrix = true;
        eigenSystem = createEigenSystem();
        rateMatrix = new double[nrOfStates][nrOfStates];
        relativeRates = new double[4 * 3];
        storedRelativeRates = new double[4 * 3];
    }


    @Override
    public boolean canHandleDataType(DataType dataType) throws Exception {
        if (dataType.getClass().equals(TwoStateCovarion.class)) {
            return true;
        }
        return false;
    }


    @Override
    protected void setupRelativeRates() {
    }

    ;

    @Override
    protected void setupRateMatrix() {
        setupUnnormalizedQMatrix();

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                rateMatrix[i][j] = unnormalizedQ[i][j];
            }
        }
        // bring in frequencies
//        for (int i = 0; i < m_nStates; i++) {
//            for (int j = i + 1; j < m_nStates; j++) {
//            	m_rateMatrix[i][j] *= fFreqs[j];
//            	m_rateMatrix[j][i] *= fFreqs[i];
//            }
//        }
        // set up diagonal
        for (int i = 0; i < nrOfStates; i++) {
            double fSum = 0.0;
            for (int j = 0; j < nrOfStates; j++) {
                if (i != j)
                    fSum += rateMatrix[i][j];
            }
            rateMatrix[i][i] = -fSum;
        }
        // normalise rate matrix to one expected substitution per unit time
        normalize(rateMatrix, getFrequencies());
    } // setupRateMatrix

    @Override
    public double[] getFrequencies() {
        double[] fFreqs = new double[4];
        fFreqs[0] = frequencies.getValue(0) * hiddenFrequencies.getValue(0);
        fFreqs[1] = frequencies.getValue(1) * hiddenFrequencies.getValue(0);
        fFreqs[2] = frequencies.getValue(0) * hiddenFrequencies.getValue(1);
        fFreqs[3] = frequencies.getValue(1) * hiddenFrequencies.getValue(1);
        return fFreqs;
    }


    protected void setupUnnormalizedQMatrix() {

        double a = alpha.getValue(0);
        double s = switchRate.getValue(0);
        double f0 = hiddenFrequencies.getValue(0);
        double f1 = hiddenFrequencies.getValue(1);
        double p0 = frequencies.getValue(0);
        double p1 = frequencies.getValue(1);

        assert Math.abs(1.0 - f0 - f1) < 1e-8;
        assert Math.abs(1.0 - p0 - p1) < 1e-8;

        unnormalizedQ[0][1] = a * p1;
        unnormalizedQ[0][2] = s;
        unnormalizedQ[0][3] = 0.0;

        unnormalizedQ[1][0] = a * p0;
        unnormalizedQ[1][2] = 0.0;
        unnormalizedQ[1][3] = s;

        unnormalizedQ[2][0] = s;
        unnormalizedQ[2][1] = 0.0;
        unnormalizedQ[2][3] = p1;

        unnormalizedQ[3][0] = 0.0;
        unnormalizedQ[3][1] = s;
        unnormalizedQ[3][2] = p0;
    }

    /**
     * Normalize rate matrix to one expected substitution per unit time
     *
     * @param matrix the matrix to normalize to one expected substitution
     * @param pi     the equilibrium distribution of states
     */
    private void normalize(double[][] matrix, double[] pi) {

        double subst = 0.0;
        int dimension = pi.length;

        for (int i = 0; i < dimension; i++) {
            subst += -matrix[i][i] * pi[i];
        }

        // normalize, including switches
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                matrix[i][j] = matrix[i][j] / subst;
            }
        }

        double switchingProportion = 0.0;
        switchingProportion += matrix[0][2] * pi[2];
        switchingProportion += matrix[2][0] * pi[0];
        switchingProportion += matrix[1][3] * pi[3];
        switchingProportion += matrix[3][1] * pi[1];

        //System.out.println("switchingProportion=" + switchingProportion);

        // normalize, removing switches
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                matrix[i][j] = matrix[i][j] / (1.0 - switchingProportion);
            }
        }
    }


}