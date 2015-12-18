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
    public Input<RealParameter> alphaInput = new Input<>("alpha", "first shape parameter, defaults to 1");
    public Input<RealParameter> betaInput = new Input<>("beta", "the other shape parameter, defaults to 1");

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
        if (alphaInput.get() == null) {
            fAlpha = 1;
        } else {
            fAlpha = alphaInput.get().getValue();
        }
        if (betaInput.get() == null) {
            fBeta = 1;
        } else {
            fBeta = betaInput.get().getValue();
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
