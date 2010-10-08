package beast.math.distributions;




import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;

@Description("Normal distribution.  f(x) = frac{1}{\\sqrt{2\\pi\\sigma^2}} e^{ -\\frac{(x-\\mu)^2}{2\\sigma^2} } " +
		"If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
		"separate independent component.")
public class Normal extends ParametricDistribution {
	public Input<RealParameter> m_mean = new Input<RealParameter>("mean","mean of the normal distribution, defaults to 0"); 
	public Input<RealParameter> m_sigma = new Input<RealParameter>("sigma","variance of the normal distribution, defaults to 1"); 
		
	static org.apache.commons.math.distribution.NormalDistribution m_dist = new NormalDistributionImpl(0,1); 
	
	@Override
	public void initAndValidate() {
		refresh();
	}	
	
	/** make sure internal state is up to date **/
	void refresh() {
		double fMean;
		double fSigma;
		if (m_mean.get() == null) {
			fMean = 0;
		} else {
			fMean = m_mean.get().getValue();
		}
		if (m_sigma.get() == null) {
			fSigma = 1;
		} else {
			fSigma = m_sigma.get().getValue();
		}
		m_dist.setMean(fMean);
		m_dist.setStandardDeviation(fSigma);
	}

	@Override
	public ContinuousDistribution getDistribution() {
		refresh();
		return m_dist;
	}
} // class Normal
