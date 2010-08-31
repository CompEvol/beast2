package beast.math.distributions;

import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.HasDensity;

/**
 * math common is missing the log normal. Here is my adaptation. some code stolen.
 *
 * @author Joseph Heled
 */

public interface LogNormalDist extends ContinuousDistribution, HasDensity<Double> {
    /**
     * Access the mean.
     * @return mean for this distribution
     */
    double getMean();
    /**
     * Modify the mean.
     * @param mean for this distribution
     */
    void setMean(double mean);
    /**
     * Access the standard deviation.
     * @return standard deviation for this distribution
     */
    double getStandardDeviation();
    /**
     * Modify the standard deviation.
     * @param sd standard deviation for this distribution
     */
    void setStandardDeviation(double sd);

    /**
     * Return the probability density for a particular point.
     * @param x  The point at which the density should be computed.
     * @return  The pdf at point x.
     */
    double density(Double x);
}
