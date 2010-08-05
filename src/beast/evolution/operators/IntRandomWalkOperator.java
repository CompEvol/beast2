package beast.evolution.operators;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.parameter.IntegerParameter;
import beast.util.Randomizer;


@Description("A random walk operator that selects a random dimension of the integer parameter and perturbs the value a " +
        "random amount within +/- windowSize.")
public class IntRandomWalkOperator extends Operator {
    public Input<Integer> windowSizeInput =
            new Input<Integer>("windowSize", "the size of the window both up and down", Input.Validate.REQUIRED);
    public Input<IntegerParameter> parameterInput =
            new Input<IntegerParameter>("parameter", "the parameter to operate a random walk on.");

    int windowSize = 1;

    public void initAndValidate() {
        windowSize = windowSizeInput.get();
    }

    /**
     * override this for proposals,
     * returns log of hastingRatio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() throws Exception {

        IntegerParameter param = parameterInput.get(this);//(IntegerParameter) state.getStateNode(parameterInput);

        int i = Randomizer.nextInt(param.getDimension());
        int value = param.getValue(i);
        int newValue = value + Randomizer.nextInt(2 * windowSize + 1) - windowSize;

        if (newValue < param.getLower() || newValue > param.getUpper()) {
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
    }

} // class IntRandomWalkOperator