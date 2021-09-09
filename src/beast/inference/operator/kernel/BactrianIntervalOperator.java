package beast.inference.operator.kernel;



import java.text.DecimalFormat;

import beast.base.Description;
import beast.base.Input;
import beast.base.Input.Validate;
import beast.inference.parameter.RealParameter;
import beast.inference.util.InputUtil;
import beast.util.Randomizer;


@Description("A scale operator that selects a random dimension of the real parameter and scales the value a " +
        "random amount according to a Bactrian distribution such that the parameter remains in its range. "
        + "Supposed to be more efficient than UniformOperator")
public class BactrianIntervalOperator extends KernelOperator {
     final public Input<RealParameter> parameterInput = new Input<>("parameter", "the parameter to operate a random walk on.", Validate.REQUIRED);
    public final Input<Double> scaleFactorInput = new Input<>("scaleFactor", "scaling factor: larger means more bold proposals", 1.0);
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);
    final public Input<Boolean> inclusiveInput = new Input<>("inclusive", "are the upper and lower limits inclusive i.e. should limit values be accepted (default true)", true);
    
    
    double scaleFactor;
    double lower, upper;
    boolean inclusive;

    @Override
	public void initAndValidate() {
    	super.initAndValidate();
        scaleFactor = scaleFactorInput.get();

        RealParameter param = parameterInput.get();
        lower = (Double) param.getLower();
        upper = (Double) param.getUpper();
        inclusive = inclusiveInput.get();

        if (Double.isInfinite(lower)) {
        	throw new IllegalArgumentException("Lower bound should be finite");
        }
        if (Double.isInfinite(upper)) {
        	throw new IllegalArgumentException("Upper bound should be finite");
        }
    }

    @Override
    public double proposal() {

        RealParameter param = (RealParameter) InputUtil.get(parameterInput, this);

        int i = Randomizer.nextInt(param.getDimension());
        double value = param.getValue(i);
        double scale = kernelDistribution.getScaler(i, value, scaleFactor);
        
        // transform value
        double y = (upper - value) / (value - lower);
        y *= scale;
        double newValue = (upper + lower * y) / (y + 1.0);
        
        if (newValue < lower || newValue > upper) {
        	throw new RuntimeException("programmer error: new value proposed outside range");
        }
        
        // Ensure that the value is not sitting on the limit (due to numerical issues for example)
        if (!inclusive && (newValue == lower || newValue == upper)) return Double.NEGATIVE_INFINITY;
        
        param.setValue(i, newValue);

        double logHR = Math.log(scale) + 2.0 * Math.log((newValue - lower)/(value - lower));
        return logHR;
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
        // must be overridden by operator implementation to have an effect
    	if (optimiseInput.get()) {
	        double delta = calcDelta(logAlpha);
	        double scaleFactor = getCoercableParameterValue();
	        delta += Math.log(scaleFactor);
	        scaleFactor = Math.exp(delta);
	        setCoercableParameterValue(scaleFactor);
    	}
    }
    
    @Override
    public double getTargetAcceptanceProbability() {
    	return 0.3;
    }
    


    @Override
    public String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = getCoercableParameterValue() * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.40) {
            return "Try setting scale factor to about " + formatter.format(newWindowSize);
        } else return "";
    }
    
} // class BactrianIntervalOperator