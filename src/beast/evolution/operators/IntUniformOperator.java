package beast.evolution.operators;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Operator;
import beast.core.parameter.IntegerParameter;
import beast.core.util.Log;
import beast.util.Randomizer;


@Description("A uniform random operator that selects a random dimension of the integer parameter and picks a new random value within the bounds.")
@Deprecated
public class IntUniformOperator extends Operator {
    final public Input<IntegerParameter> parameterInput = new Input<>("parameter", "the parameter to operate a random walk on.", Validate.REQUIRED);


    @Override
	public void initAndValidate() {
    	Log.warning.println("\n\nIntUniformOperator is depracated. Use UniformOperator instead.\n\n");
    }

    /**
     * override this for proposals,
     * returns log of hastingRatio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {

        IntegerParameter param = parameterInput.get(this);

        int i = Randomizer.nextInt(param.getDimension());
        int newValue = Randomizer.nextInt(param.getUpper() - param.getLower() + 1) + param.getLower();

        param.setValue(i, newValue);

        return 0.0;
    }

    @Override
    public void optimize(double logAlpha) {
        // nothing to optimise
    }

} // class IntUniformOperator