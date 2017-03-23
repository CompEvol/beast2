package beast.math.distributions;


import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;



@Description("Normal distribution.  f(x) = frac{1}{\\sqrt{2\\pi\\sigma^2}} e^{ -\\frac{(x-\\mu)^2}{2\\sigma^2} } " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Normal extends ParametricDistribution {
    final public Input<RealParameter> meanInput = new Input<>("mean", "mean of the normal distribution, defaults to 0");
    final public Input<RealParameter> sigmaInput = new Input<>("sigma", "standard deviation of the normal distribution, defaults to 1");
    final public Input<RealParameter> tauInput = new Input<>("tau", "precission of the normal distribution, defaults to 1", Validate.XOR, sigmaInput);

    static org.apache.commons.math.distribution.NormalDistribution dist = new NormalDistributionImpl(0, 1);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    @SuppressWarnings("deprecation")
	void refresh() {
        double mean;
        double sigma;
        if (meanInput.get() == null) {
            mean = 0;
        } else {
            mean = meanInput.get().getValue();
        }
        if (sigmaInput.get() == null) {
        	if (tauInput.get() == null) {
        		sigma = 1;
        	} else {
                sigma = Math.sqrt(1.0/tauInput.get().getValue());
        	}
        } else {
            sigma = sigmaInput.get().getValue();
        }
        dist.setMean(mean);
        dist.setStandardDeviation(sigma);
    }

    @Override
    public ContinuousDistribution getDistribution() {
        refresh();
        return dist;
    }

    @Override
    public double getMean() {
        if (meanInput.get() == null) {
        	return offsetInput.get();
        } else {
        	return offsetInput.get() + meanInput.get().getValue();
        }
    }
} // class Normal
