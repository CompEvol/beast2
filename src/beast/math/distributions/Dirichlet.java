package beast.math.distributions;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.Distribution;

import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;

@Description("Dirichlet distribution.  p(x_1,...,x_n;alpha_1,...,alpha_n) = 1/B(alpha) prod_{i=1}^K x_i^{alpha_i - 1} " +
		"where B() is the beta function B(alpha) = prod_{i=1}^K Gamma(alpha_i)/ Gamma(sum_{i=1}^K alpha_i}. ")
public class Dirichlet extends ParametricDistribution {
	public Input<RealParameter> m_alpha = new Input<RealParameter>("alpha","coefficients of the Dirichlet distribution",Validate.REQUIRED);

	@Override
	public void initAndValidate() throws Exception {
	}

	@Override
	public Distribution getDistribution() {
		return null;
	}	
	
	class DirichletImpl implements ContinuousDistribution {
		Double [] m_fAlpha;
		void setAlpha(Double [] fAlpha) {m_fAlpha = fAlpha;}
		
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
	} // class OneOnXImpl
	
	
	
	@Override
	public double calcLogP(Valuable pX) throws Exception {
		Double [] fAlpha = m_alpha.get().getValues(); 
		if (m_alpha.get().getDimension() != pX.getDimension()) {
			throw new Exception("Dimensions of alpha and x should be the same, but dim(alpha)=" + m_alpha.get().getDimension()
					+ " and dim(x)=" + pX.getDimension());
		}
		double fLogP = 0;
		double fSumAlpha = 0;
		for (int i = 0; i < pX.getDimension(); i++) {
			double fX = pX.getArrayValue(i);
			fLogP += (fAlpha[i]-1) * Math.log(fX);
			fLogP -= org.apache.commons.math.special.Gamma.logGamma(fAlpha[i]);
            fSumAlpha += fAlpha[i];
		}
		fLogP += org.apache.commons.math.special.Gamma.logGamma(fSumAlpha);
		return fLogP;
	}

}
