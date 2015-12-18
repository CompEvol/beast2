package beast.evolution.operators;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Operator;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.util.Randomizer;

@Description("Assign one or more parameter values to a uniformly selected value in its range.")
public class UniformOperator extends Operator {
    final public Input<Parameter<?>> parameterInput = new Input<>("parameter", "a real or integer parameter to sample individual values for", Validate.REQUIRED, Parameter.class);
    final public Input<Integer> howManyInput = new Input<>("howMany", "number of items to sample, default 1, must be less than the dimension of the parameter", 1);

    int howMany;
    Parameter<?> parameter;
    double fLower, fUpper;
    int iLower, iUpper;

    @Override
    public void initAndValidate() throws Exception {
        parameter = parameterInput.get();
        if (parameter instanceof RealParameter) {
            fLower = (Double) parameter.getLower();
            fUpper = (Double) parameter.getUpper();
        } else if (parameter instanceof IntegerParameter) {
            iLower = (Integer) parameter.getLower();
            iUpper = (Integer) parameter.getUpper();
        } else {
            throw new Exception("parameter should be a RealParameter or IntergerParameter, not " + parameter.getClass().getName());
        }

        howMany = howManyInput.get();
        if (howMany > parameter.getDimension()) {
            throw new Exception("howMany it too large: must be less than the dimension of the parameter");
        }
    }

    @Override
    public double proposal() {
        for (int n = 0; n < howMany; ++n) {
            // do not worry about duplication, does not matter
            int index = Randomizer.nextInt(parameter.getDimension());

            if (parameter instanceof IntegerParameter) {
                int newValue = Randomizer.nextInt(iUpper - iLower + 1) + iLower; // from 0 to n-1, n must > 0,
                ((IntegerParameter) parameter).setValue(index, newValue);
            } else {
                double newValue = Randomizer.nextDouble() * (fUpper - fLower) + fLower;
                ((RealParameter) parameter).setValue(index, newValue);
            }

        }

        return 0.0;
    }

}
