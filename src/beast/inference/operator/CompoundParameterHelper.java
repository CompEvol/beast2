package beast.inference.operator;


import java.util.List;

import beast.base.Description;
import beast.inference.parameter.Parameter;


@Description("A temporary helper class to solve compound state nodes for operators, " +
        "but it cannot be used as input, before the framework is modified.")
public class CompoundParameterHelper<T> {
    protected int[] parameterIndex1; // index to select parameter
    protected int[] parameterIndex2; // index to select dimension inside parameter

    final List<Parameter<T>> parameterList;

    public CompoundParameterHelper(final List<Parameter<T>> parameterList) {
        this.parameterList = parameterList;

        if (parameterList == null || parameterList.size() < 1) {
            throw new IllegalArgumentException("There is no parameter inputted into CompoundParameter !");
        }

        int dim = 0;
        for (final Parameter<T> para : parameterList) {
            dim += para.getDimension();
        }

        parameterIndex1 = new int[dim];
        parameterIndex2 = new int[dim];

        int k = 0;
        for (int y = 0; y < parameterList.size(); y++) {
            final Parameter<T> para = parameterList.get(y);
            for (int d = 0; d < para.getDimension(); d++) {
                parameterIndex1[k] = y;
                parameterIndex2[k] = d;
                k++;
            }
        }
    }

    public int getDimension() {
        return parameterIndex1.length;
    }

    public void setValue(final int param, final T value) {
        final Parameter<T> para = parameterList.get(getY(param));
        para.setValue(getX(param), value);
    }

    public T getValue(final int param) {
        return parameterList.get(getY(param)).getValue(getX(param));
    }

    public T getLower(final int param) {
        return parameterList.get(getY(param)).getLower();
    }

    public T getUpper(final int param) {
        return parameterList.get(getY(param)).getUpper();
    }

    // the index inside a parameter
    protected int getX(final int param) {
        return parameterIndex2[param];
    }

    // the index of parameter list
    protected int getY(final int param) {
        return parameterIndex1[param];
    }

}