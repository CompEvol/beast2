package beast.evolution.substitutionmodel;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.Nucleotide;


@Description("Covarion model for binary data")
public class TwoStateCovarion extends GeneralSubstitutionModel {

	public Input<RealParameter> m_alpha = new Input<RealParameter>("alpha","alpha parameter, the muatation rate between state zero and one", Validate.REQUIRED);
	public Input<RealParameter> m_switchingParameter = new Input<RealParameter>("switchingParameter","switchingParameter parameter, the rate between stable 0,1 and unstable 0,1 states", Validate.REQUIRED);
	
	/** we don't need the m_rates input for TwoStateCovarion **/
	public TwoStateCovarion() {
		m_rates.setRule(Validate.OPTIONAL);
	}
	
    @Override
    public void initAndValidate() throws Exception {
    	m_frequencies = frequenciesInput.get();
        updateMatrix = true;
        m_nStates = m_frequencies.getFreqs().length;
        relativeRates = new double[6];
        storedRelativeRates = new double[6];
    }
    
    @Override
    public void setupRateMatrix() {
    	relativeRates[0] = m_alpha.get().getValue();
        relativeRates[1] = m_switchingParameter.get().getValue();
        relativeRates[2] = 0.0;
        relativeRates[3] = 0.0;
        relativeRates[4] = m_switchingParameter.get().getValue();
        relativeRates[5] = 1.0;
    }	

	@Override
	public boolean canHandleDataType(DataType dataType) throws Exception {
		if (dataType.getStateCount() == 4) {
			return true;
		}
		throw new Exception("Can only handle 4 state data");
	}
} // class TwoStateCovarion
