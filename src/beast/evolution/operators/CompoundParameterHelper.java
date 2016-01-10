package beast.evolution.operators;


import java.util.List;

import beast.core.Description;
import beast.core.parameter.Parameter;


@Description("A temporary helper class to solve compound state nodes for operators, " +
        "but it cannot be used as input, before the framework is modified.")
public class CompoundParameterHelper<T> {
    protected int[] parameterIndex; // store the index of parameter list

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

        parameterIndex = new int[dim];

        for (int y = 0; y < parameterList.size(); y++) {
            final Parameter<T> para = parameterList.get(y);
            for (int d = 0; d < para.getDimension(); d++) {
                parameterIndex[y + d] = y;
            }
        }
    }

    public int getDimension() {
        return parameterIndex.length;
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

    // given {{?,?,?,?}{?,?}{?,?,?}}, parameterIndex[] is 0 0 0 0 1 1 2 2 2, param starts from 0;
    // if param < 4, then getX(param) = param;
    // if param >= 4, then getX(param) = param - the sum of previous dimensions
    // for example, param = 7, then getX = 7 - (4 + 2) = 1
    protected int getX(final int param) {
        int sumPrevDim = parameterList.get(0).getDimension();
        if (param < sumPrevDim) {
            return param;
        }
        for (int y = 1; y < getY(param); y++) {
            sumPrevDim += parameterList.get(y).getDimension();
        }

        return param - sumPrevDim;
    }

    // the index of parameter list
    protected int getY(final int param) {
        return parameterIndex[param];
    }

}
