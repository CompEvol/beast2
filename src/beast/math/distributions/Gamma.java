package beast.math.distributions;


import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.GammaDistributionImpl;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;

@Description("Gamma distribution.    for x>0  g(x;alpha,beta) = \\frac{beta^{alpha}}{Gamma(alpha)} x^{alpha-1} e^{-beta {x}}" +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Gamma extends ParametricDistribution {
    public Input<RealParameter> m_alpha = new Input<RealParameter>("alpha", "shape parameter, defaults to 2");
    public Input<RealParameter> m_beta = new Input<RealParameter>("beta", "scale parameter, defaults to 2");

    static org.apache.commons.math.distribution.GammaDistribution m_dist = new GammaDistributionImpl(1, 1);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    void refresh() {
        double fAlpha;
        double fBeta;
        if (m_alpha.get() == null) {
            fAlpha = 2;
        } else {
            fAlpha = m_alpha.get().getValue();
        }
        if (m_beta.get() == null) {
            fBeta = 2;
        } else {
            fBeta = m_beta.get().getValue();
        }
        m_dist.setAlpha(fAlpha);
        m_dist.setBeta(fBeta);
    }

    @Override
    public ContinuousDistribution getDistribution() {
        refresh();
        return m_dist;
    }

    @Override
    public double getMean() {
    	return m_offset.get() + m_dist.getAlpha() / m_dist.getBeta();
    }
} // class Gamma
