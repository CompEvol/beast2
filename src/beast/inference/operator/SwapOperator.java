package beast.inference.operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import beast.base.Description;
import beast.base.Input;
import beast.base.Input.Validate;
import beast.inference.Operator;
import beast.inference.StateNode;
import beast.inference.parameter.BooleanParameter;
import beast.inference.parameter.IntegerParameter;
import beast.inference.parameter.Parameter;
import beast.inference.parameter.RealParameter;
import beast.util.Randomizer;



@Description("A generic operator swapping a one or more pairs in a multi-dimensional parameter")
public class SwapOperator extends Operator {
    final public Input<RealParameter> parameterInput = new Input<>("parameter", "a real parameter to swap individual values for");
    final public Input<IntegerParameter> intparameterInput = new Input<>("intparameter", "an integer parameter to swap individual values for", Validate.XOR, parameterInput);
    final public Input<Integer> howManyInput = new Input<>("howMany", "number of items to swap, default 1, must be less than half the dimension of the parameter", 1);
    final public Input<BooleanParameter> parameterFilterInput = new Input<>("filter", "filter to specify a subset of the parameter to operate on", Validate.OPTIONAL);

    int howMany;
    Parameter<?> parameter;
    BooleanParameter filter;
    private List<Integer> masterList = null;

    @Override
    public void initAndValidate() {
        if (parameterInput.get() != null) {
            parameter = parameterInput.get();
        } else {
            parameter = intparameterInput.get();
        }

        howMany = howManyInput.get();
        if (howMany * 2 > parameter.getDimension()) {
            throw new IllegalArgumentException("howMany too large: must be less than half the parameter dimension");
        }

        filter = parameterFilterInput.get();
        if (filter != null) {
            filter.initAndValidate();
            if (filter.getDimension() != parameter.getDimension())
                throw new IllegalArgumentException("Filter vector should have the same length as parameter");
        }

        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < parameter.getDimension(); i++) {
            if (filter == null) {
                list.add(i);
            } else if (filter.getValue(i) == true) {
                list.add(i);
            }
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

    @Override
    public List<StateNode> listStateNodes() {
        final List<StateNode> list = new ArrayList<>();
        if (parameter instanceof RealParameter) {
            RealParameter r = (RealParameter) parameter;
            list.add(r);
        } else if (parameter instanceof IntegerParameter) {
            IntegerParameter i = (IntegerParameter) parameter;
            list.add(i);
        }
        return list;
    }

}
