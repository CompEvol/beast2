package beast.base.inference.distribution;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.Distribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;



/**
 * @author Alexei Drummond
 */
@Description("A log-normal distribution with mean and variance parameters.")
public class LogNormalDistributionModel extends ParametricDistribution {
    final public Input<Function> MParameterInput = new Input<>("M", "M parameter of lognormal distribution. Equal to the mean of the log-transformed distribution.");
    final public Input<Function> SParameterInput = new Input<>("S", "S parameter of lognormal distribution. Equal to the standard deviation of the log-transformed distribution.");
    final public Input<Boolean> hasMeanInRealSpaceInput = new Input<>("meanInRealSpace", "Whether the M parameter is in real space, or in log-transformed space. Default false = log-transformed.", false);

    boolean hasMeanInRealSpace;
    protected LogNormalImpl dist = new LogNormalImpl(0, 1);

    @Override
	public void initAndValidate() {
        hasMeanInRealSpace = hasMeanInRealSpaceInput.get();
        if (MParameterInput.get() != null && MParameterInput.get() instanceof RealParameter) {
        	RealParameter M = (RealParameter) MParameterInput.get();
            if (M.getLower() == null) {
                M.setLower(Double.NEGATIVE_INFINITY);
            }
            if (M.getUpper() == null) {
                M.setUpper(Double.POSITIVE_INFINITY);
            }
        }

        if (SParameterInput.get() != null && SParameterInput.get() instanceof RealParameter) {
        	RealParameter S = (RealParameter) SParameterInput.get();
            if (S.getLower() == null) {
                S.setLower(0.0);
            }
            if (S.getUpper() == null) {
                S.setUpper(Double.POSITIVE_INFINITY);
            }
        }
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    void refresh() {
        double mean;
        double sigma;
        if (SParameterInput.get() == null) {
            sigma = 1;
        } else {
            sigma = SParameterInput.get().getArrayValue();
        }
        if (MParameterInput.get() == null) {
            mean = 0;
        } else {
            mean = MParameterInput.get().getArrayValue();
        }
        if (hasMeanInRealSpace) {
            mean = Math.log(mean) - (0.5 * sigma * sigma);
        }
        dist.setMeanAndStdDev(mean, sigma);
    }

    @Override
    public Distribution getDistribution() {
        refresh();
        return dist;
    }

    public class LogNormalImpl implements ContinuousDistribution {
        double m_fMean;
        double m_fStdDev;
        NormalDistributionImpl m_normal = new NormalDistributionImpl(0, 1);

        public LogNormalImpl(double mean, double stdDev) {
            setMeanAndStdDev(mean, stdDev);
        }

        @SuppressWarnings("deprecation")
		void setMeanAndStdDev(double mean, double stdDev) {
            m_fMean = mean;
            m_fStdDev = stdDev;
            m_normal.setMean(mean);
            m_normal.setStandardDeviation(stdDev);
        }

        @Override
        public double cumulativeProbability(double x) throws MathException {
            return m_normal.cumulativeProbability(Math.log(x));
        }

        @Override
        public double cumulativeProbability(double x0, double x1) throws MathException {
            return cumulativeProbability(x1) - cumulativeProbability(x0);
        }

        @Override
        public double inverseCumulativeProbability(double p) throws MathException {
            return Math.exp(m_normal.inverseCumulativeProbability(p));
        }

        @Override
        public double density(double x) {
            if( x <= 0 ) {
                return 0;
            }
            return m_normal.density(Math.log(x)) / x;
        }

        @Override
        public double logDensity(double x) {
            if( x <= 0 ) {
                return  Double.NEGATIVE_INFINITY;
            }
            return m_normal.logDensity(Math.log(x)) - Math.log(x);
        }
    } // class LogNormalImpl

    @Override
    protected double getMeanWithoutOffset() {
    	if (hasMeanInRealSpace) {
    		if (MParameterInput.get() != null) {
    			return MParameterInput.get().getArrayValue();
    		} else {
    			return 0.0;
    		}
    	} else {
    		double s = SParameterInput.get().getArrayValue();
    		double m = MParameterInput.get().getArrayValue();
    		return Math.exp(m + s * s/2.0);
    		//throw new RuntimeException("Not implemented yet");
    	}
    }

}
