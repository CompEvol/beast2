package beast.base.inference.distribution;


import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.ExponentialDistributionImpl;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.parameter.RealParameter;


@Description("Exponential distribution.  f(x;\\lambda) = 1/\\lambda e^{-x/\\lambda}, if x >= 0 " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Exponential extends ParametricDistribution {
    final public Input<Function> lambdaInput = new Input<>("mean", "mean parameter, defaults to 1");

    final org.apache.commons.math.distribution.ExponentialDistribution m_dist = new ExponentialDistributionImpl(1) {
		private static final long serialVersionUID = 1L;

		@Override
    	public double logDensity(double x) {
            if (x < 0) {
                return Double.NEGATIVE_INFINITY;
            }
            double mean = getMean();
            // logDensity(x) = Math.log(density(x))
            // = Math.log(Math.exp(-x / mean) / mean)
            // = Math.log(Math.exp(-x / mean)) - Math.log(mean)
            // = (-x / mean) - Math.log(mean);
            return (-x / mean) - Math.log(mean);
    	}
    };

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    @SuppressWarnings("deprecation")
	void refresh() {
        double lambda;
        if (lambdaInput.get() == null) {
            lambda = 1;
        } else {
            lambda = lambdaInput.get().getArrayValue();
            if (lambda < 0) {
                Log.err.println("Exponential::Lambda should be positive not " + lambda + ". Assigning default value.");
                lambda = 1;
            }
        }
        m_dist.setMean(lambda);
    }

    @Override
    public ContinuousDistribution getDistribution() {
        refresh();
        return m_dist;
    }
    
    @Override
    protected double getMeanWithoutOffset() {
    	return m_dist.getMean();
    }

} // class Exponential
