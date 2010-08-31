package beast.densities;


import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.math.distributions.Distribution;
import beast.math.distributions.GammaDistribution;

@Description("Gamma distribution.    for x>0  g(x;alpha,beta) = \\frac{beta^{alpha}}{Gamma(alpha)} x^{alpha-1} e^{-beta {x}}")
public class Gamma extends ParametricDistribution {
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

    private GammaDistribution gamma;

    @Override
	public void initAndValidate() throws Exception {
        gamma = new GammaDistribution(getShape(), getScale()) ;
    }

    @Override
    public Distribution getDistribution() {
        gamma.setShape(getShape());
        gamma.setScale(getScale());
        return gamma;
    }
}
