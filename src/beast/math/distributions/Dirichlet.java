package beast.math.distributions;

import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;

@Description("Dirichlet distribution, used as prior.  p(x_1,...,x_n;alpha_1,...,alpha_n) = 1/B(alpha) prod_{i=1}^K x_i^{alpha_i - 1} " +
		"where B() is the beta function B(alpha) = prod_{i=1}^K Gamma(alpha_i)/ Gamma(sum_{i=1}^K alpha_i}. ")
public class Dirichlet extends Prior {
	public Input<RealParameter> m_alpha = new Input<RealParameter>("alpha","coefficients of the Dirichlet distribution",Validate.REQUIRED);
	
	@Override
	public void initAndValidate() throws Exception {
		if (m_alpha.get().getDimension() != m_x.get().getDimension()) {
			throw new Exception("Dimensions of alpha and x should be the same, but dim(alpha)=" + m_alpha.get().getDimension()
					+ " and dim(x)=" + m_x.get().getDimension());
		}
	}
	
	@Override
	public double calculateLogP() {
		Valuable pX = m_x.get();
		Double [] fAlpha = m_alpha.get().getValues(); 
		logP = 0;
		double fSumAlpha = 0;
		for (int i = 0; i < pX.getDimension(); i++) {
			double fX = pX.getArrayValue(i);
            logP += (fAlpha[i]-1) * Math.log(fX);
            logP -= org.apache.commons.math.special.Gamma.logGamma(fAlpha[i]);
            fSumAlpha += fAlpha[i];
		}
        logP += org.apache.commons.math.special.Gamma.logGamma(fSumAlpha);
		return logP;
	}

	
	@Override
	public boolean requiresRecalculation() {
		// we only get here when a StateNode input has changed, so are guaranteed recalculation is required.
		try {
			calculateLogP();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
}
