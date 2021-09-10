package beast.base.inference.operator;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Operator;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;


@Description("A random walk operator that selects a random dimension of the integer parameter and perturbs the value a " +
        "random amount within +/- windowSize.")
public class IntRandomWalkOperator extends Operator {
    final public Input<Integer> windowSizeInput =
            new Input<>("windowSize", "the size of the window both up and down", Validate.REQUIRED);
    final public Input<IntegerParameter> parameterInput =
            new Input<>("parameter", "the parameter to operate a random walk on.", Validate.REQUIRED);

    int windowSize = 1;

    @Override
	public void initAndValidate() {
        windowSize = windowSizeInput.get();
    }

    /**
     * override this for proposals,
     * returns log of hastingRatio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {

        final IntegerParameter param = (IntegerParameter) InputUtil.get(parameterInput,this);

        final int i = Randomizer.nextInt(param.getDimension());
        final int value = param.getValue(i);
        final int newValue = value + Randomizer.nextInt(2 * windowSize + 1) - windowSize;

        if (newValue < param.getLower() || newValue > param.getUpper()) {
            // invalid move, can be rejected immediately
            return Double.NEGATIVE_INFINITY;
        }
        if (newValue == value) {
            // this saves calculating the posterior
            return Double.NEGATIVE_INFINITY;
        }

        param.setValue(i, newValue);

        return 0.0;
    }

    @Override
    public void optimize(final double logAlpha) {
        // nothing to optimise
    }

} // class IntRandomWalkOperator