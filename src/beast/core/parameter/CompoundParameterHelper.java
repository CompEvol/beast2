package beast.core.parameter;

import beast.core.Description;

import java.util.List;

@Description("A temporary helper class to solve compound state nodes for operators, " +
        "but it cannot be used as input, before the framework is modified.")
public abstract class CompoundParameterHelper<T> {
    protected int[] parameterIndex; // store the index of parameter list

    public int getDimension() {
        return parameterIndex.length;
    }

    public abstract void setValue(int iParam, T fValue);

    public abstract T getValue(int iParam);

    public abstract T getLower(int iParam);

    public abstract T getUpper(int iParam);

    // given {{?,?,?,?}{?,?}{?,?,?}}, parameterIndex[] is 0 0 0 0 1 1 2 2 2, iParam starts from 0;
    // if iParam < 4, then getX(iParam) = iParam;
    // if iParam >= 4, then getX(iParam) = iParam - the sum of previous dimensions
    // for example, iParam = 7, then getX = 7 - (4 + 2) = 1
    protected abstract int getX(int iParam);

    // the index of parameter list
    protected int getY(int iParam) {
        return parameterIndex[iParam];
    }

}
