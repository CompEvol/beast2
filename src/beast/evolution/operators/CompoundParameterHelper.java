package beast.evolution.operators;

import beast.core.Description;
import beast.core.parameter.Parameter;

import java.util.List;

@Description("A temporary helper class to solve compound state nodes for operators, " +
        "but it cannot be used as input, before the framework is modified.")
public class CompoundParameterHelper<T> {
    protected int[] parameterIndex; // store the index of parameter list

    List<Parameter<T>> parameterList;

    public CompoundParameterHelper(List<Parameter<T>> parameterList) {
        this.parameterList = parameterList;

        if (parameterList == null || parameterList.size() < 1) {
            throw new IllegalArgumentException("There is no parameter inputted into CompoundParameter !");
        }

        int dim = 0;
        for (Parameter<T> para : parameterList) {
            dim += para.getDimension();
        }

        parameterIndex = new int[dim];

        for (int y = 0; y < parameterList.size(); y++) {
            Parameter<T> para = parameterList.get(y);
            for (int d = 0; d < para.getDimension(); d++) {
                parameterIndex[y + d] = y;
            }
        }
    }

    public int getDimension() {
        return parameterIndex.length;
    }

    public void setValue(int iParam, T fValue) {
        Parameter<T> para = parameterList.get(getY(iParam));
        para.setValue(getX(iParam), fValue);
    }

    public T getValue(int iParam) {
        return parameterList.get(getY(iParam)).getValue(getX(iParam));
    }

    public T getLower(int iParam) {
        return parameterList.get(getY(iParam)).getLower();
    }

    public T getUpper(int iParam) {
        return parameterList.get(getY(iParam)).getUpper();
    }

    // given {{?,?,?,?}{?,?}{?,?,?}}, parameterIndex[] is 0 0 0 0 1 1 2 2 2, iParam starts from 0;
    // if iParam < 4, then getX(iParam) = iParam;
    // if iParam >= 4, then getX(iParam) = iParam - the sum of previous dimensions
    // for example, iParam = 7, then getX = 7 - (4 + 2) = 1
    protected int getX(int iParam) {
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
    protected int getY(int iParam) {
        return parameterIndex[iParam];
    }

}
