package beast.math.distributions;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.Distribution;

import beast.core.Description;
import beast.core.Input;

@Description("Uniform distribution over a given interval (including lower and upper values)")
public class Uniform extends ParametricDistribution {
	public Input<Double> m_lower = new Input<Double>("lower","lower bound on the interval, defaul 0", 0.0);
	public Input<Double> m_upper = new Input<Double>("upper","lower bound on the interval, defaul 1", 1.0);
	
    UniformImpl distr = new UniformImpl();
    
    double m_fLower, m_fUpper, m_fDensity;
	
	@Override 
	public void initAndValidate() throws Exception {
		m_fLower = m_lower.get();
		m_fUpper = m_upper.get();
		if (m_fLower >= m_fUpper) {
			throw new Exception("Upper value should be higher than lower value");
		}
		distr.setBounds(m_fLower,m_fUpper);
		if (Double.isInfinite(m_fLower) || Double.isInfinite(m_fUpper)) {
			m_fDensity = 1.0;
		} else {
			m_fDensity = 1.0/(m_fUpper - m_fLower);
		}
	}


	class UniformImpl implements ContinuousDistribution {
		private double lower;
		private double upper;

		public void setBounds(double lower, double upper) {
			this.lower = lower;
			this.upper = upper;
		}

		@Override
		public double cumulativeProbability(double x) throws MathException {
			x = Math.max(x, lower);
			return (x - lower) / (upper - lower);
		}

		@Override
		public double cumulativeProbability(double x0, double x1) throws MathException {
			x0 = Math.max(x0, lower);
			x1 = Math.min(x1, upper);
			if (x1 < lower || x1 > upper) {
				throw new RuntimeException("Value x (" + x1 + ") out of bounds (" + lower + "," + upper + ").");
			}
			return (x1 - x0) / (upper - lower);
		}

		@Override
		public double inverseCumulativeProbability(double p) throws MathException {
			if (p < 0.0 || p > 1.0) {
				throw new RuntimeException("inverseCumulativeProbability::argument out of range [0...1]");
			}
			return (upper - lower) * p + lower;
		}

		@Override
		public double density(double x) {
			if (x >= lower && x <= upper) {
				return m_fDensity;
			} else {
				return 0;
			}
		}

		@Override
		public double logDensity(double x) {
			return Math.log(density(x));
		}
	} // class UniformImpl

	
	@Override
	public Distribution getDistribution() {
		return distr;
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
