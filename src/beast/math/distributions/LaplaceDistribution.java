package beast.math.distributions;


import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.GammaDistributionImpl;
import org.apache.commons.math.MathException;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;

@Description("Laplace distribution.    f(x|\\mu,b) = \\frac{1}{2b} \\exp \\left( -\\frac{|x-\\mu|}{b} \\right)" +
        "The probability density function of the Laplace distribution is also reminiscent of the normal distribution; " +
        "however, whereas the normal distribution is expressed in terms of the squared difference from the mean ?, " +
        "the Laplace density is expressed in terms of the absolute difference from the mean. Consequently the Laplace " +
        "distribution has fatter tails than the normal distribution.")
public class LaplaceDistribution extends ParametricDistribution {
    public Input<RealParameter> m_mu = new Input<RealParameter>("mu", "location parameter, defaults to 0");
    public Input<RealParameter> m_scale = new Input<RealParameter>("scale", "scale parameter, defaults to 1");

    // the mean parameter
    double mu;
    // the scale parameter
    double scale;
    // the maximum density
    double c;

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    void refresh() {
        double fMu;
        double fScale;
        if (m_mu.get() == null) {
            fMu = 0;
        } else {
            fMu = m_mu.get().getValue();
        }
        if (m_scale.get() == null) {
            fScale = 1;
        } else {
            fScale = m_scale.get().getValue();
        }
        setParameters(fMu, fScale);
    }

    /**
     * Set parameters of this distribution
     *
     * @param m mean
     * @param s scale
     */
    protected void setParameters(double m, double s) {
        if (s <= 0) s = 1;
        mu = m;
        scale = s;

        //Normalizing constant
        c = 1 / (2 * scale);
    }

    @Override
    public double density(double x) {
        return c * Math.exp(-Math.abs(x - mu) / scale);
    }

    @Override
    public double logDensity(double x) {
        return Math.log(c) - (Math.abs(x - mu) / scale);
    }

    @Override
    public double cumulativeProbability(double x) throws MathException {
        if (x == mu) {
            return 0.5;
        } else {
            return (0.5) * (1 + ((x - mu) / Math.abs(x - mu)) * (1 - Math.exp(-Math.abs(x - mu) / scale)));
        }
    }

    @Override
    public ContinuousDistribution getDistribution() {
//		refresh();
//		return null;
        throw new UnsupportedOperationException();
    }

} // class