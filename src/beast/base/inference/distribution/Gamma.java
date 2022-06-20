package beast.base.inference.distribution;


import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.GammaDistributionImpl;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;

@Description("Gamma distribution. for x>0  g(x;alpha,beta) = 1/Gamma(alpha) beta^alpha} x^{alpha - 1} e^{-\frac{x}{beta}}" +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Gamma extends ParametricDistribution {
    final public Input<RealParameter> alphaInput = new Input<>("alpha", "shape parameter, defaults to 2");
    final public Input<RealParameter> betaInput = new Input<>("beta", "second parameter depends on mode, defaults to 2."
    		+ "For mode=ShapeScale beta is interpreted as scale. "
    		+ "For mode=ShapeRate beta is interpreted as rate. "
    		+ "For mode=ShapeMean beta is interpreted as mean."
    		+ "For mode=OneParameter beta is ignored.");
    public enum mode {ShapeScale, ShapeRate, ShapeMean, OneParameter};
    final public Input<mode> modeInput = new Input<>("mode", "determines parameterisation. "
    		+ "For ShapeScale beta is interpreted as scale. "
    		+ "For ShapeRate beta is interpreted as rate. "
    		+ "For ShapeMean beta is interpreted as mean."
    		+ "For OneParameter beta is ignored.", mode.ShapeScale, mode.values());

    org.apache.commons.math.distribution.GammaDistribution m_dist = new GammaDistributionImpl(1, 1);

    mode parameterisation = mode.ShapeScale;
    		
    @Override
    public void initAndValidate() {
    	parameterisation = modeInput.get();
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    @SuppressWarnings("deprecation")
	void refresh() {
        double alpha;
        double beta = 2.0;
        if (alphaInput.get() == null) {
            alpha = 2;
        } else {
            alpha = alphaInput.get().getValue();
        }
        
        switch (parameterisation) {
        case ShapeScale:
            if (betaInput.get() != null) {
                beta = betaInput.get().getValue();
            }
        	break;
        case ShapeRate:
            if (betaInput.get() != null) {
                beta = 1.0/betaInput.get().getValue();
            }
        	break;
        case ShapeMean:
            if (betaInput.get() != null) {
                beta = betaInput.get().getValue() / alpha;
            }
        	break;
        case OneParameter:
        	beta = 1.0 / alpha;
        	break;
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
    	refresh();
    	return m_dist.getAlpha() * m_dist.getBeta();
    }
} // class Gamma
