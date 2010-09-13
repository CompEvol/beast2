package beast.math.distributions;


import org.apache.commons.math.special.Gamma;

import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.core.parameter.IntegerParameter;

@Description("Chi square distribution, used as prior  f(x; k) = \\frac{1}{2^{k/2}Gamma(k/2)} x^{k/2-1} e^{-x/2} " +
		"If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
		"separate independent component.")
public class ChiSquare extends Prior {
	public Input<IntegerParameter> m_df = new Input<IntegerParameter>("df","degrees if freedin, defaults to 1"); 
		
	int m_fDF;
	
	
	
	@Override
	public void initAndValidate() {
		if (m_df.get() == null) {
			m_fDF = 1;
		} else {
			m_fDF = m_df.get().getValue();
			if (m_fDF <= 0) {
				System.err.println("ChiSquare::degrees of freedom should be positive not "+m_fDF+". Assigning default value.");
				m_fDF = 1;
			}
		}
	}

	@Override
	public double calculateLogP() {
		logP = 0;
		Valuable pX = m_x.get();
		for (int i = 0; i < pX.getDimension(); i++) {
			double fX = pX.getArrayValue(i);
			logP += (m_fDF/2.0-1.0) * Math.log(fX) - fX / 2.0;
		}
		logP += pX.getDimension() * (-(m_fDF/2.0) * Math.log(2.0) - Gamma.logGamma(m_fDF/2.0));
		return logP;
	}
	

} // class ChiSquare
