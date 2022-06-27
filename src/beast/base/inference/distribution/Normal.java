package beast.base.inference.distribution;


import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.parameter.RealParameter;



@Description("Normal distribution.  f(x) = frac{1}{\\sqrt{2\\pi\\sigma^2}} e^{ -\\frac{(x-\\mu)^2}{2\\sigma^2} } " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Normal extends ParametricDistribution {
    final public Input<Function> meanInput = new Input<>("mean", "mean of the normal distribution, defaults to 0");
    final public Input<Function> sigmaInput = new Input<>("sigma", "standard deviation of the normal distribution, defaults to 1");
    final public Input<Function> tauInput = new Input<>("tau", "precision of the normal distribution, defaults to 1", Validate.XOR, sigmaInput);

    org.apache.commons.math.distribution.NormalDistribution dist = new NormalDistributionImpl(0, 1);

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
            mean = meanInput.get().getArrayValue();
        }
        if (sigmaInput.get() == null) {
        	if (tauInput.get() == null) {
        		sigma = 1;
        	} else {
                sigma = Math.sqrt(1.0/tauInput.get().getArrayValue());
        	}
        } else {
            sigma = sigmaInput.get().getArrayValue();
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
    public double getMeanWithoutOffset() {
        if (meanInput.get() == null) {
        	return 0.0;
        } else {
        	return meanInput.get().getArrayValue();
        }
    }
} // class Normal
