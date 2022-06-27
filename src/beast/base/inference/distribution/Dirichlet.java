package beast.base.inference.distribution;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.Distribution;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;



@Description("Dirichlet distribution.  p(x_1,...,x_n;alpha_1,...,alpha_n) = 1/B(alpha) prod_{i=1}^K x_i^{alpha_i - 1} " +
        "where B() is the beta function B(alpha) = prod_{i=1}^K Gamma(alpha_i)/ Gamma(sum_{i=1}^K alpha_i}. ")
public class Dirichlet extends ParametricDistribution {
    final public Input<Function> alphaInput = new Input<>("alpha", "coefficients of the Dirichlet distribution", Validate.REQUIRED);

    @Override
    public void initAndValidate() {
    }

    @Override
    public Distribution getDistribution() {
        return null;
    }

    class DirichletImpl implements ContinuousDistribution {
        Double[] m_fAlpha;

        void setAlpha(Double[] alpha) {
            m_fAlpha = alpha;
        }

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
            return Double.NaN;
        }

        @Override
        public double logDensity(double x) {
            return Double.NaN;
        }
        
    } // class DirichletImpl


    @Override
    public double calcLogP(Function pX) {
        double[] alpha = alphaInput.get().getDoubleValues();
        if (alphaInput.get().getDimension() != pX.getDimension()) {
            throw new IllegalArgumentException("Dimensions of alpha and x should be the same, but dim(alpha)=" + alphaInput.get().getDimension()
                    + " and dim(x)=" + pX.getDimension());
        }
        double logP = 0;
        double sumAlpha = 0;
        for (int i = 0; i < pX.getDimension(); i++) {
            double x = pX.getArrayValue(i);
            logP += (alpha[i] - 1) * Math.log(x);
            logP -= org.apache.commons.math.special.Gamma.logGamma(alpha[i]);
            sumAlpha += alpha[i];
        }
        logP += org.apache.commons.math.special.Gamma.logGamma(sumAlpha);
        return logP;
    }

	@Override
	public Double[][] sample(int size) {
		int dim = alphaInput.get().getDimension();
		Double[][] samples = new Double[size][];
		for (int i = 0; i < size; i++) {
			Double[] dirichletSample = new Double[dim];
			double sum = 0.0;
			for (int j = 0; j < dim; j++) {
				dirichletSample[j] = Randomizer.nextGamma(alphaInput.get().getArrayValue(j), 1.0);
				sum += dirichletSample[j];
			}
			for (int j = 0; j < dim; j++) {
				dirichletSample[j] = dirichletSample[j] / sum;
			}
			samples[i] = dirichletSample;

		}
		return samples;
	}
}
