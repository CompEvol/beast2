package beast.evolution.operators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.Input.Validate;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.util.Randomizer;



@Description("A generic operator swapping a one or more pairs in a multi-dimensional parameter")
public class SwapOperator extends Operator {
    public Input<RealParameter> parameterInput = new Input<>("parameter", "a real parameter to swap individual values for");
    public Input<IntegerParameter> intparameterInput = new Input<>("intparameter", "an integer parameter to swap individual values for", Validate.XOR, parameterInput);
    public Input<Integer> howManyInput = new Input<>("howMany", "number of items to swap, default 1, must be less than half the dimension of the parameter", 1);


    int howMany;
    Parameter<?> parameter;
    private List<Integer> masterList = null;

    @Override
    public void initAndValidate() throws Exception {
        if (parameterInput.get() != null) {
            parameter = parameterInput.get();
        } else {
            parameter = intparameterInput.get();
        }

        howMany = howManyInput.get();
        if (howMany * 2 > parameter.getDimension()) {
            throw new Exception("howMany it too large: must be less than half the dimension of the parameter");
        }

        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < parameter.getDimension(); i++) {
            list.add(i);
        }
        masterList = Collections.unmodifiableList(list);
    }

    @Override
    public double proposal() {
        List<Integer> allIndices = new ArrayList<>(masterList);
        int left, right;

        for (int i = 0; i < howMany; i++) {
            left = allIndices.remove(Randomizer.nextInt(allIndices.size()));
            right = allIndices.remove(Randomizer.nextInt(allIndices.size()));
            parameter.swap(left, right);
        }

        return 0.0;
    }

}
