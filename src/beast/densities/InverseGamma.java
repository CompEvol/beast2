package beast.densities;


import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.math.distributions.Distribution;
import beast.math.distributions.InverseGammaDistribution;

@Description("Inverse Gamma distribution.  " +
        "for x>0  f(x; alpha, beta) = \frac{beta^alpha}{Gamma(alpha)} (1/x)^{alpha + 1}exp(-beta/x) ")
public class InverseGamma extends ParametricDistribution {
    public Input<Valuable> m_alpha = new Input<Valuable>("alpha","shape parameter, defaults to 2");
	public Input<Valuable> m_beta = new Input<Valuable>("beta","scale parameter, defaults to 2");

    double getShape() {
       if (m_alpha.get() == null) {
			return 2;
		} else {
			return m_alpha.get().getArrayValue();
		}
    }

    double getScale() {
       if (m_beta.get() == null) {
			return 2;
		} else {
			return m_beta.get().getArrayValue();
		}
    }

    private InverseGammaDistribution igamma;

    @Override
	public void initAndValidate() throws Exception {
        igamma = new InverseGammaDistribution(getShape(), getScale()) ;
    }

    @Override
    public Distribution getDistribution() {
        igamma.setShape(getShape());
        igamma.setScale(getScale());
        return igamma;
    }
}
