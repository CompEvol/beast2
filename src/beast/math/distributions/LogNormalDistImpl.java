package beast.math.distributions;

import org.apache.commons.math.ArgumentOutsideDomainException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.distribution.AbstractContinuousDistribution;
import org.apache.commons.math.special.Erf;

/**
 *  math common is missing the log normal. Here is my adaptation. some code stolen.
 * 
 * @author Joseph Heled
 */
public class LogNormalDistImpl extends AbstractContinuousDistribution implements LogNormalDist {

    /** The mean of this distribution. */
    private double lmean = 0;

    /** The standard deviation of this distribution. */
    private double lstandardDeviation = 1;

    /**
     * Create a normal distribution using the given mean and standard deviation.
     * @param lmean mean for this distribution
     * @param lsd standard deviation for this distribution
     */
    public LogNormalDistImpl(double lmean, double lsd) {
        super();
        this.lmean = lmean;
        this.lstandardDeviation = lsd;
    }


    /** &sqrt;(2 &pi;) */
    private static final double SQRT2PI = Math.sqrt(2 * Math.PI);

    @Override
    public double density(Double x) {
        if( x <= 0 ) {
            return 0.0;
        }

        final double x0 = Math.log(x) - lmean;
        final double sd = lstandardDeviation;
        final double y = x0/sd;
        final double normalDensity =  Math.exp(-y*y / 2) / (sd * SQRT2PI);
        return normalDensity / x;
    }

    @Override
    public double cumulativeProbability(double x) throws MathException {
        if( x <= 0 ) {
            throw new ArgumentOutsideDomainException(x, 0, Double.POSITIVE_INFINITY);
        }

        x = Math.log(x);
        try {
            return 0.5 * (1.0 + Erf.erf((x - lmean) /
                    (lstandardDeviation * Math.sqrt(2.0))));
        } catch ( MaxIterationsExceededException ex) {
            if (x < (lmean - 20 * lstandardDeviation)) { // JDK 1.5 blows at 38
                return 0.0d;
            } else if (x > (lmean + 20 * lstandardDeviation)) {
                return 1.0d;
            } else {
                throw ex;
            }
        }
    }

    @Override
    public double getMean() {
        return Math.exp(lmean + (lstandardDeviation * lstandardDeviation / 2));
    }

    @Override
    public void setMean(double mean) {
        this.lmean = Math.log(mean) - (lstandardDeviation * lstandardDeviation / 2);
    }

    @Override
    public double getStandardDeviation() {
        final double S2 = lstandardDeviation * lstandardDeviation;

        return Math.exp(S2 + 2 * lmean) * (Math.exp(S2) - 1);
    }

    @Override
    public void setStandardDeviation(double sd) {
        sd *= Math.exp(- 2 * lmean);
        lstandardDeviation = Math.sqrt(Math.log((1 + Math.sqrt(1 + 4*sd))/2));
    }


    @Override
    protected double getInitialDomain(double p) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected double getDomainLowerBound(double p) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected double getDomainUpperBound(double p) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
