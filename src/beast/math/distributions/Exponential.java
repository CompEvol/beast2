package beast.math.distributions;


import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.ExponentialDistributionImpl;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;


@Description("Exponential distribution.  f(x;\\lambda) = 1/\\lambda e^{-x/\\lambda}, if x >= 0 " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Exponential extends ParametricDistribution {
    final public Input<RealParameter> lambdaInput = new Input<>("mean", "mean parameter, defaults to 1");

    static org.apache.commons.math.distribution.ExponentialDistribution m_dist = new ExponentialDistributionImpl(1);

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
            lambda = lambdaInput.get().getValue();
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
