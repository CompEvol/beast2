package beast.math.distributions;

import beast.math.distributions.Distribution;

/**
 * @author Joseph Heled
 */
public class OneOnXdistribution implements Distribution {
    @Override
    public double pdf(double x) {
        if( x <= 0 ) {
            return 0.0;
        }
        return 1/x;
    }

    @Override
    public double logPdf(double x) {
        if( x <= 0 ) {
            return Double.NEGATIVE_INFINITY;
        }
        return -Math.log(x);
    }

    @Override
    public double cdf(double x) {
        return 0;
    }

    @Override
    public double quantile(double y) {
        return  Double.POSITIVE_INFINITY;
    }

    @Override
    public double mean() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double variance() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public org.apache.commons.math.distribution.Distribution getProbabilityDensity() {
        return null;
    }
}
