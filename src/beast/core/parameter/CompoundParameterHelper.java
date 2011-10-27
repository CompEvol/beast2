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

    class CompoundRealParameter extends CompoundParameterHelper<Double> {
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
            realPara.setValue(iParam, fValue);
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

    class CompoundIntegerParameter extends CompoundParameterHelper<Integer> {

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
            intPara.setValue(iParam, fValue);
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

}
