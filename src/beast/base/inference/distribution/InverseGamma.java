package beast.base.inference.distribution;


import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.Distribution;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;



@Description("Inverse Gamma distribution, used as prior.    for x>0  f(x; alpha, beta) = \frac{beta^alpha}{Gamma(alpha)} (1/x)^{alpha + 1}exp(-beta/x) " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class InverseGamma extends ParametricDistribution {
    final public Input<Function> alphaInput = new Input<>("alpha", "shape parameter, defaults to 2");
    final public Input<Function> betaInput = new Input<>("beta", "scale parameter, defaults to 2");

    InverseGammaImpl dist = new InverseGammaImpl(2, 2);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * ensure internal state is up to date *
     */
    void refresh() {
        double alpha;
        double beta;
        if (alphaInput.get() == null) {
            alpha = 2;
        } else {
            alpha = alphaInput.get().getArrayValue();
        }
        if (betaInput.get() == null) {
            beta = 2;
        } else {
            beta = betaInput.get().getArrayValue();
        }
        dist.setAlphaBeta(alpha, beta);
    }

    @Override
    public Distribution getDistribution() {
        refresh();
        return dist;
    }

    class InverseGammaImpl implements ContinuousDistribution {
        double m_fAlpha;
        double m_fBeta;
        // log of the constant beta^alpha/Gamma(alpha)
        double C;

        InverseGammaImpl(double alpha, double beta) {
            setAlphaBeta(alpha, beta);
        }

        void setAlphaBeta(double alpha, double beta) {
            m_fAlpha = alpha;
            m_fBeta = beta;
            C = m_fAlpha * Math.log(m_fBeta) - org.apache.commons.math.special.Gamma.logGamma(m_fAlpha);
        }

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
            double logP = logDensity(x);
            return Math.exp(logP);
        }

        @Override
        public double logDensity(double x) {
            double logP = -(m_fAlpha + 1.0) * Math.log(x) - (m_fBeta / x) + C;
            return logP;
        }
    } // class InverseGammaImpl


    @Override
    public double getMeanWithoutOffset() {
    	Function alpha = alphaInput.get();
    	Function beta = betaInput.get();
    	return (beta != null ? beta.getArrayValue() : 2.0) / (alpha != null ? (alpha.getArrayValue() - 1.0) : 2.0);
    }
    
} // class InverseGamma
