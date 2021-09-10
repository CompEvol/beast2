package beast.base.inference.distribution;


import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.Distribution;

import beast.base.core.Description;



@Description("OneOnX distribution.  f(x) = C/x for some normalizing constant C. " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class OneOnX extends ParametricDistribution {

    ContinuousDistribution dist = new OneOnXImpl();

    @Override
    public void initAndValidate() {
    }

    @Override
    public Distribution getDistribution() {
        return dist;
    }

    class OneOnXImpl implements ContinuousDistribution {

        @Override
        public double cumulativeProbability(double x) throws MathException {
            throw new MathException("Not implemented yet");
        }

        @Override
        public double cumulativeProbability(double x0, double x1) throws MathException {
            throw new MathException("Not implemented yet");
        }

        @Override
        public double inverseCumulativeProbability(double p) throws MathException {
            throw new MathException("Not implemented yet");
        }

        @Override
        public double density(double x) {
            return 1 / x;
        }

        @Override
        public double logDensity(double x) {
            return -Math.log(x);
        }
    } // class OneOnXImpl


} // class OneOnX
