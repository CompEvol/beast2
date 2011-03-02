package beast.evolution.operators;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.parameter.RealParameter;
import beast.util.Randomizer;


@Description("A random walk operator that selects a random dimension of the real parameter and perturbs the value a " +
        "random amount within +/- windowSize.")
public class RealRandomWalkOperator extends Operator {
    public Input<Double> windowSizeInput =
            new Input<Double>("windowSize", "the size of the window both up and down when using uniform interval OR standard deviation when using Gaussian", Input.Validate.REQUIRED);
    public Input<RealParameter> parameterInput =
            new Input<RealParameter>("parameter", "the parameter to operate a random walk on.");
    public Input<Boolean> useGaussianInput =
        new Input<Boolean>("useGaussian", "Use Gaussian to move instead of uniform interval. Default false.", false);

    double windowSize = 1;
    boolean m_bUseGaussian;
    public void initAndValidate() {
        windowSize = windowSizeInput.get();
        m_bUseGaussian = useGaussianInput.get();
    }

    /**
     * override this for proposals,
     * returns log of hastingRatio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {

    	RealParameter param = parameterInput.get(this);

        int i = Randomizer.nextInt(param.getDimension());
        double value = param.getValue(i);
        double newValue = value;
        if (m_bUseGaussian) {
        	newValue += Randomizer.nextGaussian()* windowSize; 
        } else {
        	newValue += Randomizer.nextDouble() * 2 * windowSize - windowSize;
        }

        if (newValue < param.getLower() || newValue > param.getUpper()) {
        	return Double.NEGATIVE_INFINITY;
        }
        if (newValue == value) {
        	// this saves calculating the posterior
        	return Double.NEGATIVE_INFINITY;
        }

        param.setValue(i, newValue);

        return 0.0;
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
        double fDelta = calcDelta(logAlpha);
        
        fDelta += Math.log(windowSize);

        //double fScaleFactor = m_pScaleFactor.get();
        //fDelta += Math.log(1.0 / windowSize - 1.0);
        //windowSize = 1.0 / (Math.exp(fDelta) + 1.0);
        windowSize = Math.exp(fDelta);
        //System.out.println(windowSize);
    }

} // class IntRandomWalkOperator