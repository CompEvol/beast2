package beast.evolution.substitutionmodel;

import beast.core.Input.Validate;
import beast.core.Description;
import beast.core.Valuable;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;

@Description("A substitution model where the rates and frequencies are obtained from " +
		"empirical evidence. Especially, amino acid models like WAG.")
public abstract class EmpiricalSubstitutionModel extends GeneralSubstitutionModel {
	
	public EmpiricalSubstitutionModel() {
		frequencies.setRule(Validate.OPTIONAL);
		m_rates.setRule(Validate.OPTIONAL);
	}
	
	@Override
    public void initAndValidate() throws Exception {
		Valuable rates = getEmpericalRateValues();
		m_rates.setValue(rates, this);
		Frequencies freqs = getEmpericalFrequencieValues();
		frequencies.setValue(freqs, this);
		int nFreqs = freqs.getFreqs().length;
		if (rates.getDimension() != nFreqs * (nFreqs-1)) {
			throw new Exception("The number of empirical rates (" + rates.getDimension() + ") should be " +
					"equal to #frequencies * (#frequencies-1) = (" + nFreqs + "*"+(nFreqs-1)+").");
		}
		
		super.initAndValidate();
    } // initAndValidate


	/** convert empirical rates into a RealParameter, only off diagonal entries are recorded **/
	Valuable getEmpericalRateValues() throws Exception {
		double[][] matrix = getEmpiricalRates();
		int nStates = matrix.length;
		RealParameter rates = new RealParameter("0",0.0, Double.POSITIVE_INFINITY, nStates*(nStates-1));

		for (int i = 0; i < nStates; i++) {
			for (int j = i + 1; j < nStates; j++) {
				rates.setValue(i*(nStates-1)+j, matrix[i][j]);
				rates.setValue(j*(nStates-1)+i, matrix[i][j]);
			}
		}
		return rates;
	}
	
	/** convert empirical frequencies into a RealParameter **/
	Frequencies getEmpericalFrequencieValues() throws Exception {
		double[] freqs = getEmpiricalFrequencies();
		int nStates = freqs.length;
		Frequencies freqsParam = new Frequencies();
		String sValues = "";

		for (int i = 0; i < nStates; i++) {
			sValues += freqs[i]+" ";
		}
        RealParameter freqsRParam = new RealParameter();
        freqsRParam.initByName(
                "values",sValues,
                "lower", 0,
                "upper", 1,
                "dimension",nStates
        );
		freqsParam.frequencies.setValue(freqsRParam, freqsParam);
		freqsParam.initAndValidate();
		return freqsParam;
	}
	
	
	/** return rate matrix (ie two dimensional array) in upper diagonal form **/
	abstract double [][] getEmpiricalRates();
	
	/** return empirical frequencies **/
	abstract double [] getEmpiricalFrequencies();

	@Override
    public double [] getRateMatrix(Node node) {
		double[][] matrix = getEmpiricalRates();
		int nStates = matrix.length;
		double [] rates = new double[nStates*nStates];
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
				fSum += rates[i*nStates+j];
			}
			rates[i*nStates+i] = -fSum;
		}
		return rates;
    }
}
