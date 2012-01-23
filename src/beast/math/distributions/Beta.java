package beast.math.distributions;

import org.apache.commons.math.distribution.BetaDistributionImpl;
import org.apache.commons.math.distribution.ContinuousDistribution;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;

@Description("Beta distribution, used as prior.  p(x;alpha,beta) = \frac{x^{alpha-1}(1-x)^{beta-1}} {B(alpha,beta)} " +
        "where B() is the beta function. " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Beta extends ParametricDistribution {
    public Input<RealParameter> m_alpha = new Input<RealParameter>("alpha", "first shape parameter, defaults to 1");
    public Input<RealParameter> m_beta = new Input<RealParameter>("beta", "the other shape parameter, defaults to 1");

    static org.apache.commons.math.distribution.BetaDistribution m_dist = new BetaDistributionImpl(1, 1);

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
            fAlpha = 1;
        } else {
            fAlpha = m_alpha.get().getValue();
        }
        if (m_beta.get() == null) {
            fBeta = 1;
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

} // class Beta
