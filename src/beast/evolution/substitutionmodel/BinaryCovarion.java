package beast.evolution.substitutionmodel;

import java.lang.reflect.InvocationTargetException;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
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
 * s, s1, s2 the rate of switching
 * <p/>
 * then the (unnormalized) instantaneous rate matrix (unnormalized Q) should be (depending on mode)
 * <p/>
 * 
 * mode = BEAST -- using classic BEAST implementation, reversible iff hfrequencies = (0.5, 0.5)
 * FLAGS: reversible = false, TSParameterisation = false
 * 
 * [ -(a*p1)-s ,   a*p1    ,    s   ,   0   ]
 * [   a*p0    , -(a*p0)-s ,    0   ,   s   ]
 * [    s      ,     0     ,  -p1-s ,  p1   ]
 * [    0      ,     s     ,    p0  , -p0-s ]
 *
 * equilibrium frequencies
 * [ p0 * f0, p1, * f0, p0 * f1, p1, * f1 ]
 *
 * mode = REVERSIBLE -- brings in hfrequencies in rate matrix
 * reversible = true, TSParameterisation = false
 * [ - , a , s , 0 ]
 * [ a , - , 0 , s ]
 * [ s , 0 , - , 1 ]
 * [ 0 , s , 1 , - ]
 * 
 * which with frequencies becomes
 * 
 * [ -(a*p1*f0)-s*f0 ,   a*p1*f0       ,    s*f0      ,   0         ]
 * [   a*p0*f0       , -(a*p0*f0)-s*f0 ,    0         ,   s*f0      ]
 * [    s*f1         ,     0           ,  -p1*f1-s*f1 ,  p1*f1      ]
 * [    0            ,     s*f1        ,    p0*f1     , -p0*f1-s*f1 ]
 * 
 * equilibrium frequencies
 * [ p0 * f0, p1, * f0, p0 * f1, p1, * f1 ]
 * 
 * mode = TUFFLEYSTEEL uses alternative parameterisation: hfrequencies is ignored, and switch parameter is set to dimension = 2
 * [ -(a*p1)-s1 ,   a*p1     ,    s1   ,   0    ]
 * [   a*p0     , -(a*p0)-s1 ,    0    ,   s1   ]
 * [    s2      ,     0      ,  -p1-s2 ,  p1    ]
 * [    0       ,     s2     ,    p0   , -p0-s2 ]
 *
 * equilibrium frequencies
 * [ f0 * s2/(s1+s2), f1, * s2/(s1+s2), f0 * s1/(s1+s2), f1, * s1/(s1+s2) ]
 *
 *
 * Note: to use Tuffley & Steel's methods, set a = 0.
 */
@Description("Covarion model for Binary data")
public class BinaryCovarion extends GeneralSubstitutionModel {
    final public Input<RealParameter> alphaInput = new Input<>("alpha", "the rate of evolution in slow mode", Validate.REQUIRED);
    final public Input<RealParameter> switchRateInput = new Input<>("switchRate", "the rate of flipping between slow and fast modes", Validate.REQUIRED);
    final public Input<RealParameter> frequenciesInput = new Input<>("vfrequencies", "the frequencies of the visible states", Validate.REQUIRED);
    final public Input<RealParameter> hfrequenciesInput = new Input<>("hfrequencies", "the frequencies of the hidden rates");

    public enum MODE {BEAST, REVERSIBLE, TUFFLEYSTEEL};
	final public Input<MODE> modeInput = new Input<>("mode","one of BEAST, REVERSIBLE, TUFFLESTEEL "
			+ "BEAST = implementation as in BEAST 1 "
			+ "REVERSIBLE = like BEAST 1 implementation, but using frequencies to make it reversible "
			+ "TUFFLEYSTEEL = Tuffley & Steel (1996) impementation (no rates for ", MODE.BEAST,MODE.values());

    private RealParameter alpha;
    private RealParameter switchRate;
    private RealParameter frequencies;
    private RealParameter hiddenFrequencies;

    protected double[][] unnormalizedQ;
    protected double[][] storedUnnormalizedQ;
    int stateCount;
    MODE mode  = modeInput.get();

    public BinaryCovarion() {
        ratesInput.setRule(Validate.OPTIONAL);
        frequenciesInput.setRule(Validate.OPTIONAL);
    }

    @Override
    public void initAndValidate() {
        alpha = alphaInput.get();
        switchRate = switchRateInput.get();
        frequencies = frequenciesInput.get();
        hiddenFrequencies = hfrequenciesInput.get();

        
        if (mode.equals(MODE.BEAST) || mode.equals(MODE.REVERSIBLE)) {
        	if (switchRate.getDimension() != 1) {
        		throw new IllegalArgumentException("switchRate should have dimension 1");
        	}
        } else {
        	if (switchRate.getDimension() != 2) {
        		throw new IllegalArgumentException("switchRate should have dimension 2");
        	}
        }
        if (alpha.getDimension() != 1) {
            throw new IllegalArgumentException("alpha should have dimension 1");
        }
        if (frequencies.getDimension() != 2) {
            throw new IllegalArgumentException("frequencies should have dimension 2");
        }
        if (mode.equals(MODE.BEAST) || mode.equals(MODE.REVERSIBLE)) {
        	if (hfrequenciesInput.get() == null) {
        		throw new IllegalArgumentException("hiddenFrequenciesshould should be specified");
        	}
            if (hiddenFrequencies.getDimension() != 2) {
                throw new IllegalArgumentException("hiddenFrequenciesshould have dimension 2");
            }
        } else {
        	if (hfrequenciesInput.get() != null) {
        		Log.warning.println("WARNING: hfrequencies is specified, but the BinaryCovarion model ignores it.");
        	}
        }

        
        
        nrOfStates = 4;
        unnormalizedQ = new double[4][4];
        storedUnnormalizedQ = new double[4][4];

        updateMatrix = true;
        try {
			eigenSystem = createEigenSystem();
		} catch (SecurityException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
        rateMatrix = new double[nrOfStates][nrOfStates];
        relativeRates = new double[4 * 3];
        storedRelativeRates = new double[4 * 3];
    }


    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType.getClass().equals(TwoStateCovarion.class);
    }


    @Override
    protected void setupRelativeRates() {
    }


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
//            	m_rateMatrix[i][j] *= freqs[j];
//            	m_rateMatrix[j][i] *= freqs[i];
//            }
//        }
        // set up diagonal
        for (int i = 0; i < nrOfStates; i++) {
            double sum = 0.0;
            for (int j = 0; j < nrOfStates; j++) {
                if (i != j)
                    sum += rateMatrix[i][j];
            }
            rateMatrix[i][i] = -sum;
        }
        // normalise rate matrix to one expected substitution per unit time
        normalize(rateMatrix, getFrequencies());
    } // setupRateMatrix

    @Override
    public double[] getFrequencies() {
        double[] freqs = new double[4];
        if (mode.equals(MODE.BEAST) || mode.equals(MODE.REVERSIBLE)) {
	        freqs[0] = frequencies.getValue(0) * hiddenFrequencies.getValue(0);
	        freqs[1] = frequencies.getValue(1) * hiddenFrequencies.getValue(0);
	        freqs[2] = frequencies.getValue(0) * hiddenFrequencies.getValue(1);
	        freqs[3] = frequencies.getValue(1) * hiddenFrequencies.getValue(1);
        } else {
        	double h0 = alpha.getValue(1) * (alpha.getValue(0) + alpha.getValue(1));
        	double h1 = alpha.getValue(0) * (alpha.getValue(0) + alpha.getValue(1));
	        freqs[0] = frequencies.getValue(0) * h0;
	        freqs[1] = frequencies.getValue(1) * h0;
	        freqs[2] = frequencies.getValue(0) * h1;
	        freqs[3] = frequencies.getValue(1) * h1;        	
        }
        return freqs;
    }


    protected void setupUnnormalizedQMatrix() {

        switch (mode) {
        case BEAST: {

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
        break;
        case REVERSIBLE: {

            double a = alpha.getValue(0);
            double s = switchRate.getValue(0);
            double f0 = hiddenFrequencies.getValue(0);
            double f1 = hiddenFrequencies.getValue(1);
            double p0 = frequencies.getValue(0);
            double p1 = frequencies.getValue(1);

            assert Math.abs(1.0 - f0 - f1) < 1e-8;
            assert Math.abs(1.0 - p0 - p1) < 1e-8;

            unnormalizedQ[0][1] = a * p1 * f0;
            unnormalizedQ[0][2] = s * f0;
            unnormalizedQ[0][3] = 0.0;

            unnormalizedQ[1][0] = a * p0 * f0;
            unnormalizedQ[1][2] = 0.0;
            unnormalizedQ[1][3] = s * f0;

            unnormalizedQ[2][0] = s * f1;
            unnormalizedQ[2][1] = 0.0;
            unnormalizedQ[2][3] = p1 * f1;

            unnormalizedQ[3][0] = 0.0;
            unnormalizedQ[3][1] = s * f1;
            unnormalizedQ[3][2] = p0 * f1;
        }
    	break;
        case TUFFLEYSTEEL: {
            double a = alpha.getValue(0);
            double s1 = switchRate.getValue(0);
            double s2 = switchRate.getValue(0);
            double p0 = frequencies.getValue(0);
            double p1 = frequencies.getValue(1);

            assert Math.abs(1.0 - p0 - p1) < 1e-8;

            unnormalizedQ[0][1] = a * p1;
            unnormalizedQ[0][2] = s1;
            unnormalizedQ[0][3] = 0.0;

            unnormalizedQ[1][0] = a * p0;
            unnormalizedQ[1][2] = 0.0;
            unnormalizedQ[1][3] = s1;

            unnormalizedQ[2][0] = s2;
            unnormalizedQ[2][1] = 0.0;
            unnormalizedQ[2][3] = p1;

            unnormalizedQ[3][0] = 0.0;
            unnormalizedQ[3][1] = s2;
            unnormalizedQ[3][2] = p0;
        }
       	break;
        }

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
