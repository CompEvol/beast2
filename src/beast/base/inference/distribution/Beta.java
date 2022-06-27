package beast.base.inference.distribution;

import org.apache.commons.math.distribution.BetaDistributionImpl;
import org.apache.commons.math.distribution.ContinuousDistribution;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;



@Description("Beta distribution, used as prior.  p(x;alpha,beta) = \frac{x^{alpha-1}(1-x)^{beta-1}} {B(alpha,beta)} " +
        "where B() is the beta function. " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Beta extends ParametricDistribution {
    final public Input<Function> alphaInput = new Input<>("alpha", "first shape parameter, defaults to 1");
    final public Input<Function> betaInput = new Input<>("beta", "the other shape parameter, defaults to 1");

    org.apache.commons.math.distribution.BetaDistribution m_dist = new BetaDistributionImpl(1, 1);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    @SuppressWarnings("deprecation")
	void refresh() {
        double alpha;
        double beta;
        if (alphaInput.get() == null) {
            alpha = 1;
        } else {
            alpha = alphaInput.get().getArrayValue();
        }
        if (betaInput.get() == null) {
            beta = 1;
        } else {
            beta = betaInput.get().getArrayValue();
        }
        m_dist.setAlpha(alpha);
        m_dist.setBeta(beta);
    }

    @Override
    public ContinuousDistribution getDistribution() {
        refresh();
        return m_dist;
    }

    @Override
    protected double getMeanWithoutOffset() {
    	return m_dist.getAlpha() / (m_dist.getAlpha() + m_dist.getBeta());
    }
} // class Beta
