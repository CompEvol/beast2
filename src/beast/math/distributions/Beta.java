package beast.math.distributions;

import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.core.parameter.RealParameter;

@Description("Beta distribution, used as prior.  p(x;alpha,beta) = \frac{x^{alpha-1}(1-x)^{beta-1}} {B(alpha,beta)} " +
		"where B() is the beta function. " +
		"If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
		"separate independent component.")
public class Beta extends Prior {
	public Input<RealParameter> m_alpha = new Input<RealParameter>("alpha","first shape parameter, defaults to 1"); 
	public Input<RealParameter> m_beta = new Input<RealParameter>("beta","the other shape parameter, defaults to 1"); 
		
	double m_fAlpha;
	double m_fBeta;
	
	@Override
	public void initAndValidate() {
		if (m_alpha.get() == null) {
			m_fAlpha = 1;
		} else {
			m_fAlpha = m_alpha.get().getValue();
		}
		if (m_beta.get() == null) {
			m_fBeta = 1;
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
			if (fX <= 0 || fX >= 1) {
				// Beta distribution is only defined on interval (0,..,1)
				logP = Double.NEGATIVE_INFINITY;
				return logP;
			}
            logP += (m_fAlpha-1) * Math.log(fX) + (m_fBeta-1) * Math.log(1-fX);
		}
		// log of the constant beta^alpha/Gamma(alpha)
		double C = - org.apache.commons.math.special.Beta.logBeta(m_fAlpha, m_fBeta);;
		logP += C * pX.getDimension();
		return logP;
	}
	

} // class Beta
