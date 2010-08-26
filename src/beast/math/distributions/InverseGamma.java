package beast.math.distributions;



import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.core.parameter.RealParameter;

@Description("Inverse Gamma distribution, used as prior.    for x>0  f(x; alpha, beta) = \frac{beta^alpha}{Gamma(alpha)} (1/x)^{alpha + 1}exp(-beta/x) " +
		"If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
		"separate independent component.")
public class InverseGamma extends Prior {
	public Input<RealParameter> m_alpha = new Input<RealParameter>("alpha","shape parameter, defaults to 2"); 
	public Input<RealParameter> m_beta = new Input<RealParameter>("beta","scale parameter, defaults to 2"); 
		
	double m_fAlpha;
	double m_fBeta;
	
	@Override
	public void initAndValidate() {
		if (m_alpha.get() == null) {
			m_fAlpha = 2;
		} else {
			m_fAlpha = m_alpha.get().getValue();
		}
		if (m_beta.get() == null) {
			m_fBeta = 2;
		} else {
			m_fBeta = m_beta.get().getValue();
		}
	}


	@Override
	public double calculateLogP() {
		Valuable pX = m_x.get();
		logP = 0;
		for (int i = 0; i < pX.getDimension(); i++) {
			double fX = pX.getArrayValue(i);
            logP += -(m_fAlpha + 1.0) * Math.log(fX) - (m_fBeta / fX);
		}
		// log of the constant beta^alpha/Gamma(alpha)
		double C = m_fAlpha * Math.log(m_fBeta) - org.apache.commons.math.special.Gamma.logGamma(m_fAlpha);
		logP += C * pX.getDimension();
		return logP;
	}
	

} // class InverseGamma
