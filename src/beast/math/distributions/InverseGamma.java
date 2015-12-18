package beast.math.distributions;


import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.Distribution;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;



@Description("Inverse Gamma distribution, used as prior.    for x>0  f(x; alpha, beta) = \frac{beta^alpha}{Gamma(alpha)} (1/x)^{alpha + 1}exp(-beta/x) " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class InverseGamma extends ParametricDistribution {
    public Input<RealParameter> alphaInput = new Input<>("alpha", "shape parameter, defaults to 2");
    public Input<RealParameter> betaInput = new Input<>("beta", "scale parameter, defaults to 2");

    InverseGammaImpl dist = new InverseGammaImpl(2, 2);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * ensure internal state is up to date *
     */
    void refresh() {
        double fAlpha;
        double fBeta;
        if (alphaInput.get() == null) {
            fAlpha = 2;
        } else {
            fAlpha = alphaInput.get().getValue();
        }
        if (betaInput.get() == null) {
            fBeta = 2;
        } else {
            fBeta = betaInput.get().getValue();
        }
        dist.setAlphaBeta(fAlpha, fBeta);
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

        InverseGammaImpl(double fAlpha, double fBeta) {
            setAlphaBeta(fAlpha, fBeta);
        }

        void setAlphaBeta(double fAlpha, double fBeta) {
            m_fAlpha = fAlpha;
            m_fBeta = fBeta;
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
        public double density(double fX) {
            double fLogP = logDensity(fX);
            return Math.exp(fLogP);
        }

        @Override
        public double logDensity(double fX) {
            double fLogP = -(m_fAlpha + 1.0) * Math.log(fX) - (m_fBeta / fX) + C;
            return fLogP;
        }
    } // class OneOnXImpl


} // class InverseGamma
