package beast.math.distributions;

import beast.math.GammaFunction;
import org.apache.commons.math.distribution.BetaDistributionImpl;
import org.apache.commons.math.special.Beta;
import org.apache.commons.math.MathException;

/**
 * @author Joseph Heled
 *
 *   Code stolen from common math.
 *
 *         Date: 1/09/2010
 */
public class BetaDistribution implements Distribution {
    private double alpha;
    private double beta;
    private double coef = Double.NaN;

    public BetaDistribution(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }


    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    @Override
    public double pdf(double x) {
        return Math.exp(logPdf(x));
    }

    // we can use either the BEAST1 implementation of log(gamma) or the common math one?
    private double getz() {
        if (Double.isNaN(coef)) {
          coef = GammaFunction.lnGamma(alpha) + GammaFunction.lnGamma(beta) - GammaFunction.lnGamma(alpha + beta);
        }
        return coef;
    }

    @Override
    public double logPdf(double x) {
        if( 0 < x && x < 1 ) {
            double logX = Math.log(x);
            double log1mX = Math.log1p(-x);
            return (alpha - 1) * logX + (beta - 1) * log1mX - getz();
        }

        if (x == 0) {
            if (alpha < 1) {
                throw new IllegalArgumentException("Cannot compute beta density at 0 when alpha < 1");
            }
            return Double.NEGATIVE_INFINITY;
        } else if (x == 1) {
            if (beta < 1) {
                throw new IllegalArgumentException("Cannot compute beta density at 1 when beta < 1");
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double cdf(double x) {
        try {
            return Beta.regularizedBeta(x, alpha, beta);
        } catch( MathException e ) {
            return Double.NaN;
        }
    }

    @Override
    public double quantile(double y) {
        try {
            return getProbabilityDensity().inverseCumulativeProbability(y);
        } catch( MathException e ) {
            return Double.NaN;
        }
    }

    @Override
    public double mean() {
        return alpha/(alpha + beta);
    }

    @Override
    public double variance() {
        final double ab = alpha + beta;
        return alpha*beta/(ab * ab *(ab -1));
    }

    @Override
    public org.apache.commons.math.distribution.BetaDistribution getProbabilityDensity() {
        return new BetaDistributionImpl(alpha, beta);
    }
}
