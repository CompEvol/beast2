package beast.math.distributions;

import beast.math.ErrorFunction;
import org.apache.commons.math.distribution.NormalDistributionImpl;

/**
 * @author Joseph Heled
 * @author Korbinian Strimmer
 */

public class NormalDistribution implements Distribution {
    private double mean;
    private double sd;


    public NormalDistribution(double mean, double sd) {
        this.mean = mean;
        this.sd = sd;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public void setSD(double sd) {
        this.sd = sd;
    }

    @Override
    public double pdf(double x) {
        return Math.exp(logPdf(x));
    }

    @Override
    public double logPdf(double x) {
        final double a = 1.0 / (SQRT_2PI * sd);
        final double xNorm =  (x - mean)/sd;
        final double b = -xNorm*xNorm/2;

        return Math.log(a) + b;
    }

    // Use ErrorFunction since common-math has no inverse erf

    @Override
    public double cdf(double x) {
        final double a = (x - mean) / (SQRT_2 * sd);

        try {
            return 0.5 * (1.0 + ErrorFunction.erf(a));
        } catch ( Exception ex) {
            if (x < (mean - 20 * sd)) {
                return 0.0d;
            } else if (x > (mean + 20 * sd)) {
                return 1.0d;
            } else {
              return Double.NaN;
            }
        }
    }

    @Override
    public double quantile(double y) {
        return mean + SQRT_2 * sd * ErrorFunction.inverseErf(2.0 * y - 1.0);
    }

    @Override
    public double mean() {
        return mean;
    }

    @Override
    public double variance() {
        return sd*sd;
    }

    @Override
    public org.apache.commons.math.distribution.NormalDistribution getProbabilityDensity() {
        return new NormalDistributionImpl(mean(), sd);
    }
}
