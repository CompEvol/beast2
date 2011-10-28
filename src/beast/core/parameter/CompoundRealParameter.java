package beast.core.parameter;

import java.util.List;


public class CompoundRealParameter extends CompoundParameterHelper<Double> {
    List<RealParameter> parameterList;

    public CompoundRealParameter(List<RealParameter> parameterList) {
        this.parameterList = parameterList;

        if (parameterList == null || parameterList.size() < 1) {
            throw new IllegalArgumentException("There is no real parameter inputted into CompoundParameter !");
        }

        int dim = 0;
        for (RealParameter realPara : parameterList) {
            dim += realPara.getDimension();
        }

        parameterIndex = new int[dim];

        for (int y = 0; y < parameterList.size(); y++) {
            RealParameter realPara = parameterList.get(y);
            for (int d = 0; d < realPara.getDimension(); d++) {
                parameterIndex[y + d] = y;
            }
        }
    }

    public void setValue(int iParam, Double fValue) {
        RealParameter realPara = parameterList.get(getY(iParam));
        realPara.setValue(getX(iParam), fValue);
    }

    public Double getValue(int iParam) {
        return parameterList.get(getY(iParam)).getValue(getX(iParam));
    }

    public Double getLower(int iParam) {
        return parameterList.get(getY(iParam)).getLower();
    }

    public Double getUpper(int iParam) {
        return parameterList.get(getY(iParam)).getUpper();
    }

    @Override
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
}
