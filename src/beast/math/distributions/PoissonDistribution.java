package beast.math.distributions;

import org.apache.commons.math.distribution.*;
import org.apache.commons.math.special.Gamma;
import org.apache.commons.math.MathException;

/**
 * @author Joseph Heled
 */
public class PoissonDistribution implements Distribution {

    private PoissonDistributionImpl distribution;

    public PoissonDistribution(double mean) {
       distribution = new PoissonDistributionImpl(mean);
    }

    public void setMean(double mean) {
       distribution.setMean(mean);
    }

    @Override
    public double pdf(double k) {
        return Math.exp(logPdf(k));
    }

    @Override
    public double logPdf(double k) {
        final double mean = mean();
        return k * Math.log(mean) - Gamma.logGamma(k + 1) - mean;
    }

    @Override
    public double cdf(double x) {
      try {
            return distribution.cumulativeProbability(x);
        } catch ( MathException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double quantile(double y) {
        try {
            return distribution.inverseCumulativeProbability(y);
        } catch (MathException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double mean() {
        return distribution.getMean();
    }

    @Override
    public double variance() {
        return mean();
    }

    @Override
    public org.apache.commons.math.distribution.PoissonDistribution getProbabilityDensity() {
        return distribution;
    }
}
