package beast.math.distributions;

import org.apache.commons.math.distribution.ExponentialDistributionImpl;

/**
 * @author Joseph Heled
 */
public class ExponentialDistribution implements Distribution {
    private double lambda;

    public ExponentialDistribution(double lambda) {
        this.lambda = lambda;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public double pdf(double x) {
        return Math.exp(logPdf(x));
    }

    @Override
    public double logPdf(double x) {
      return Math.log(lambda) - (lambda * x);
    }

    @Override
    public double cdf(double x) {
        return 1.0 - Math.exp(-lambda * x);
    }

    @Override
    public double quantile(double y) {
        return -(1.0 / lambda) * Math.log(1.0 - y);
    }

    @Override
    public double mean() {
        return 1.0 / lambda;
    }

    @Override
    public double variance() {
        return 1.0 / (lambda*lambda);
    }

    @Override
    public org.apache.commons.math.distribution.ExponentialDistribution getProbabilityDensity() {
        return new ExponentialDistributionImpl(mean());
    }
}
