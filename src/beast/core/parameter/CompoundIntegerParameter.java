package beast.core.parameter;

import java.util.List;

public class CompoundIntegerParameter extends CompoundParameterHelper<Integer> {

    List<IntegerParameter> intparameterList;

    public CompoundIntegerParameter(List<IntegerParameter> intparameterList) {
        this.intparameterList = intparameterList;

        if (intparameterList == null || intparameterList.size() < 1) {
            throw new IllegalArgumentException("There is no integer parameter inputted into CompoundParameter !");
        }

        int dim = 0;
        for (IntegerParameter intPara : intparameterList) {
            dim += intPara.getDimension();
        }

        parameterIndex = new int[dim];

        for (int y = 0; y < intparameterList.size(); y++) {
            IntegerParameter intPara = intparameterList.get(y);
            for (int d = 0; d < intPara.getDimension(); d++) {
                parameterIndex[y + d] = y;
            }
        }
    }

    public void setValue(int iParam, Integer fValue) {
        IntegerParameter intPara = intparameterList.get(getY(iParam));
        intPara.setValue(getX(iParam), fValue);
    }

    public Integer getValue(int iParam) {
        return intparameterList.get(getY(iParam)).getValue(getX(iParam));
    }

    public Integer getLower(int iParam) {
        return intparameterList.get(getY(iParam)).getLower();
    }

    public Integer getUpper(int iParam) {
        return intparameterList.get(getY(iParam)).getUpper();
    }

    @Override
    protected int getX(int iParam) {
        int sumPrevDim = intparameterList.get(0).getDimension();
        if (iParam < sumPrevDim) {
            return iParam;
        }
        for (int y = 1; y < getY(iParam); y++) {
            sumPrevDim += intparameterList.get(y).getDimension();
        }

        return iParam - sumPrevDim;
    }
}