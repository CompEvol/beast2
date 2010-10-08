package beast.math.distributions;


import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.commons.math.distribution.ContinuousDistribution;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.IntegerParameter;

@Description("Chi square distribution, f(x; k) = \\frac{1}{2^{k/2}Gamma(k/2)} x^{k/2-1} e^{-x/2} " +
		"If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
		"separate independent component.")
public class ChiSquare extends ParametricDistribution {
	public Input<IntegerParameter> m_df = new Input<IntegerParameter>("df","degrees if freedin, defaults to 1"); 
		
	static org.apache.commons.math.distribution.ChiSquaredDistribution m_dist = new ChiSquaredDistributionImpl(1); 
	
	@Override
	public void initAndValidate() {
		refresh();
	}	
	
	/** make sure internal state is up to date **/
	void refresh() {
		int nDF;
		if (m_df.get() == null) {
			nDF = 1;
		} else {
			nDF = m_df.get().getValue();
			if (nDF <= 0) {
				nDF = 1;
			}
		}
		m_dist.setDegreesOfFreedom(nDF);
	}

	@Override
	public ContinuousDistribution getDistribution() {
		refresh();
		return m_dist;
	}

} // class ChiSquare
