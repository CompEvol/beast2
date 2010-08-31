package beast.densities;

import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.math.distributions.BetaDistribution;
import beast.math.distributions.Distribution;

@Description("Beta distribution, used as prior.  p(x;alpha,beta) = \frac{x^{alpha-1}(1-x)^{beta-1}} {B(alpha,beta)} " +
		"where B() is the beta function. ")
public class Beta extends ParametricDistribution {
	public Input<Valuable> m_alpha = new Input<Valuable>("alpha","first shape parameter, defaults to 1");
	public Input<Valuable> m_beta = new Input<Valuable>("beta","the other shape parameter, defaults to 1");

    double getShapeA() {
       if (m_alpha.get() == null) {
			return 1;
		} else {
			return m_alpha.get().getArrayValue();
		}
    }

    double getShapeB() {
       if (m_beta.get() == null) {
			return 1;
		} else {
			return m_beta.get().getArrayValue();
		}
    }

	
	 private BetaDistribution beta;

    @Override
	public void initAndValidate() throws Exception {
        beta = new BetaDistribution(getShapeA(), getShapeB()) ;
    }


//	@Override
//	public double calculateLogP() {
//		Valuable pX = m_x.get();
//		logP = 0;
//		for (int i = 0; i < pX.getDimension(); i++) {
//			double fX = pX.getArrayValue(i);
//			if (fX <= 0 || fX >= 1) {
//				// Beta distribution is only defined on interval (0,..,1)
//				logP = Double.NEGATIVE_INFINITY;
//				return logP;
//			}
//            logP += (m_fAlpha-1) * Math.log(fX) + (m_fBeta-1) * Math.log(1-fX);
//		}
//		// log of the constant beta^alpha/Gamma(alpha)
//		double C = - org.apache.commons.math.special.Beta.logBeta(m_fAlpha, m_fBeta);;
//		logP += C * pX.getDimension();
//		return logP;
//	}


    @Override
    public Distribution getDistribution() {
        beta.setAlpha(getShapeA());
        beta.setBeta(getShapeB());
        return beta;
    }
} // class Beta
