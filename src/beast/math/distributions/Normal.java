package beast.math.distributions;




import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.core.parameter.RealParameter;

@Description("Normal distribution, used as prior.  f(x) = frac{1}{\\sqrt{2\\pi\\sigma^2}} e^{ -\\frac{(x-\\mu)^2}{2\\sigma^2} } " +
		"If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
		"separate independent component.")
public class Normal extends Prior {
	public Input<RealParameter> m_mean = new Input<RealParameter>("mean","mean of the normal distribution, defaults to 0"); 
	public Input<RealParameter> m_sigma = new Input<RealParameter>("sigma","variance of the normal distribution, defaults to 1"); 
		
	double m_fMean;
	double m_fSigma;
	
	@Override
	public void initAndValidate() {
		if (m_mean.get() == null) {
			m_fMean = 0;
		} else {
			m_fMean = m_mean.get().getValue();
		}
		if (m_sigma.get() == null) {
			m_fSigma = 1;
		} else {
			m_fSigma = m_sigma.get().getValue();
		}
	}

	// log of the constant 1/sqrt(2.PI)
	final static double C = -Math.log(2.0*Math.PI)/2.0;

	@Override
	public double calculateLogP() {
		Valuable pX = m_x.get();
		double fVar = m_fSigma * m_fSigma;
		logP = 0;
		for (int i = 0; i < pX.getDimension(); i++) {
			double fX = pX.getArrayValue(i);
			logP += -(m_fMean - fX) * (m_fMean - fX) / (2.0 * fVar) - Math.log(fVar)/2.0 ;
		}
		logP += C * pX.getDimension(); 
		return logP;
	}
	
} // class Normal
