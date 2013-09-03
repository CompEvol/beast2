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

    public void setValue(final int iParam, final T fValue) {
        final Parameter<T> para = parameterList.get(getY(iParam));
        para.setValue(getX(iParam), fValue);
    }

    public T getValue(final int iParam) {
        return parameterList.get(getY(iParam)).getValue(getX(iParam));
    }

    public T getLower(final int iParam) {
        return parameterList.get(getY(iParam)).getLower();
    }

    public T getUpper(final int iParam) {
        return parameterList.get(getY(iParam)).getUpper();
    }

    // given {{?,?,?,?}{?,?}{?,?,?}}, parameterIndex[] is 0 0 0 0 1 1 2 2 2, iParam starts from 0;
    // if iParam < 4, then getX(iParam) = iParam;
    // if iParam >= 4, then getX(iParam) = iParam - the sum of previous dimensions
    // for example, iParam = 7, then getX = 7 - (4 + 2) = 1
    protected int getX(final int iParam) {
        int sumPrevDim = parameterList.get(0).getDimension();
        if (iParam < sumPrevDim) {
            return iParam;
        }
        for (int y = 1; y < getY(iParam); y++) {
            sumPrevDim += parameterList.get(y).getDimension();
        }

        return iParam - sumPrevDim;
    }

    // the index of parameter list
    protected int getY(final int iParam) {
        return parameterIndex[iParam];
    }

}
