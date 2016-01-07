package beast.evolution.substitutionmodel;

import beast.core.Description;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;

@Description("A substitution model where the rates and frequencies are obtained from " +
        "empirical evidence. Especially, amino acid models like WAG.")
public abstract class EmpiricalSubstitutionModel extends GeneralSubstitutionModel {

    public EmpiricalSubstitutionModel() {
        frequenciesInput.setRule(Validate.OPTIONAL);
        ratesInput.setRule(Validate.OPTIONAL);
    }

    double[] m_empiricalRates;

    @Override
    public void initAndValidate() throws Exception {
        frequencies = getEmpericalFrequencieValues();
        m_empiricalRates = getEmpericalRateValues();
        int nFreqs = frequencies.getFreqs().length;
        if (m_empiricalRates.length != nFreqs * (nFreqs - 1)) {
            throw new IllegalArgumentException("The number of empirical rates (" + m_empiricalRates.length + ") should be " +
                    "equal to #frequencies * (#frequencies-1) = (" + nFreqs + "*" + (nFreqs - 1) + ").");
        }

        updateMatrix = true;
        nrOfStates = frequencies.getFreqs().length;
        eigenSystem = createEigenSystem();
        rateMatrix = new double[nrOfStates][nrOfStates];
        relativeRates = new double[m_empiricalRates.length];
        storedRelativeRates = new double[m_empiricalRates.length];
    } // initAndValidate

    @Override
    protected void setupRelativeRates() {
        System.arraycopy(m_empiricalRates, 0, relativeRates, 0, m_empiricalRates.length);
    }


    /**
     * convert empirical rates into a RealParameter, only off diagonal entries are recorded *
     */
    double[] getEmpericalRateValues() throws Exception {
        double[][] matrix = getEmpiricalRates();
        int[] nOrder = getEncodingOrder();
        int nStates = matrix.length;

        double[] rates = new double[nStates * (nStates - 1)];
        int k = 0;
        for (int i = 0; i < nStates; i++) {
            int u = nOrder[i];
            for (int j = 0; j < nStates; j++) {
                int v = nOrder[j];
                if (i != j) {
                    rates[k++] = matrix[Math.min(u, v)][Math.max(u, v)];
                }
            }
        }
        return rates;
    }

    /**
     * convert empirical frequencies into a RealParameter *
     */
    Frequencies getEmpericalFrequencieValues() throws Exception {
        double[] freqs = getEmpiricalFrequencies();
        int[] nOrder = getEncodingOrder();
        int nStates = freqs.length;
        Frequencies freqsParam = new Frequencies();
        String valuesString = "";

        for (int i = 0; i < nStates; i++) {
            valuesString += freqs[nOrder[i]] + " ";
        }
        RealParameter freqsRParam = new RealParameter();
        freqsRParam.initByName(
                "value", valuesString,
                "lower", 0.0,
                "upper", 1.0,
                "dimension", nStates
        );
        freqsParam.frequenciesInput.setValue(freqsRParam, freqsParam);
        freqsParam.initAndValidate();
        return freqsParam;
    }


    /**
     * return rate matrix (ie two dimensional array) in upper diagonal form *
     */
    abstract double[][] getEmpiricalRates();

    /**
     * return empirical frequencies *
     */
    abstract double[] getEmpiricalFrequencies();

    /**
     * return character order for getEmpricialRates and getEmpriricalFrequencies
     * // The rates may be specified assuming that the amino acids are in this order:
     * // ARNDCQEGHILKMFPSTWYV
     * // but the AminoAcids dataType wants them in this order:
     * // ACDEFGHIKLMNPQRSTVWY
     * // This method returns the proper order
     */
    abstract int[] getEncodingOrder();

    @Override
    public double[] getRateMatrix(Node node) {
        double[][] matrix = getEmpiricalRates();
        int nStates = matrix.length;
        double[] rates = new double[nStates * nStates];
        for (int i = 0; i < nStates; i++) {
            for (int j = i + 1; j < nStates; j++) {
                rates[i * nStates + j] = matrix[i][j];
                rates[j * nStates + i] = matrix[i][j];
            }
        }
        // determine diagonal
        for (int i = 0; i < nStates; i++) {
            double fSum = 0;
            for (int j = i + 1; j < nStates; j++) {
                fSum += rates[i * nStates + j];
            }
            rates[i * nStates + i] = -fSum;
        }
        return rates;
    }
}
