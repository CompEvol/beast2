package beast.inference.operator.kernel;



import java.text.DecimalFormat;

import beast.base.Description;
import beast.base.Input;
import beast.base.Input.Validate;
import beast.inference.parameter.RealParameter;
import beast.inference.util.InputUtil;
import beast.util.Randomizer;


@Description("A random walk operator that selects a random dimension of the real parameter and perturbs the value a " +
        "random amount according to a Bactrian distribution (Yang & Rodriguez, 2013), which is a mixture of two Gaussians:"
        + "p(x) = 1/2*N(x;-m,1-m^2) + 1/2*N(x;+m,1-m^2) and more efficient than RealRandomWalkOperator")
public class BactrianRandomWalkOperator extends KernelOperator {
    final public Input<RealParameter> parameterInput = new Input<>("parameter", "the parameter to operate a random walk on.", Validate.REQUIRED);
    public final Input<Double> scaleFactorInput = new Input<>("scaleFactor", "scaling factor: larger means more bold proposals", 1.0);
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);

    double scaleFactor;

    @Override
	public void initAndValidate() {
    	super.initAndValidate();
        scaleFactor = scaleFactorInput.get();
    }

    @Override
    public double proposal() {

        RealParameter param = (RealParameter)InputUtil.get(parameterInput, this);

        int i = Randomizer.nextInt(param.getDimension());
        double value = param.getValue(i);
        double newValue = value + kernelDistribution.getRandomDelta(i, value, scaleFactor);
        
        if (newValue < param.getLower() || newValue > param.getUpper()) {
            return Double.NEGATIVE_INFINITY;
        }

        param.setValue(i, newValue);

        return 0;
    }


    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(double value) {
    	scaleFactor = value;
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    @Override
    public void optimize(double logAlpha) {
    	if (optimiseInput.get()) {
	        // must be overridden by operator implementation to have an effect
	        double delta = calcDelta(logAlpha);
	
	        delta += Math.log(scaleFactor);
	        scaleFactor = Math.exp(delta);
    	}
    }

    @Override
    public double getTargetAcceptanceProbability() {
    	return 0.3;
    }

    @Override
    public final String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = scaleFactor * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.40) {
            return "Try setting scale factor to about " + formatter.format(newWindowSize);
        } else return "";
    }
} // class BactrianRandomWalkOperator