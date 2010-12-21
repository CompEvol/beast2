package beast.math.distributions;

import org.apache.commons.math.distribution.Distribution;

import beast.core.Description;
import beast.core.Input;

@Description("Uniform distribution over a given interval (including lower and upper values)")
public class Uniform extends ParametricDistribution {
	public Input<Double> m_lower = new Input<Double>("lower","lower bound on the interval, defaul 0", 0.0);
	public Input<Double> m_upper = new Input<Double>("upper","lower bound on the interval, defaul 1", 1.0);
	
	double m_fLower, m_fUpper;
	
	@Override 
	public void initAndValidate() throws Exception {
		m_fLower = m_lower.get();
		m_fUpper = m_upper.get();
		if (m_fLower >= m_fUpper) {
			throw new Exception("Upper value should be higher than lower value");
		}
	}

	
	@Override
	public Distribution getDistribution() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public double density(double x) {
    	if (x >= m_fLower && x <= m_fUpper) {
    		return 1;
    	} else {
    		return 0;
    	}
    }
}
