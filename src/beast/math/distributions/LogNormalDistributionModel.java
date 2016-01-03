package beast.math.distributions;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.Distribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;



/**
 * @author Alexei Drummond
 */
@Description("A log-normal distribution with mean and variance parameters.")
public class LogNormalDistributionModel extends ParametricDistribution {
    final public Input<RealParameter> MParameterInput = new Input<>("M", "M parameter of lognormal distribution. Equal to the mean of the log-transformed distribution.");
    final public Input<RealParameter> SParameterInput = new Input<>("S", "S parameter of lognormal distribution. Equal to the standard deviation of the log-transformed distribution.");
    final public Input<Boolean> hasMeanInRealSpaceInput = new Input<>("meanInRealSpace", "Whether the M parameter is in real space, or in log-transformed space. Default false = log-transformed.", false);

    boolean hasMeanInRealSpace;
    LogNormalImpl dist = new LogNormalImpl(0, 1);

    @Override
	public void initAndValidate() throws Exception {
        hasMeanInRealSpace = hasMeanInRealSpaceInput.get();
        if (MParameterInput.get() != null) {
            if (MParameterInput.get().getLower() == null) {
                MParameterInput.get().setLower(Double.NEGATIVE_INFINITY);
            }
            if (MParameterInput.get().getUpper() == null) {
                MParameterInput.get().setUpper(Double.POSITIVE_INFINITY);
            }
        }

        if (SParameterInput.get() != null) {
            if (SParameterInput.get().getLower() == null) {
                SParameterInput.get().setLower(0.0);
            }
            if (SParameterInput.get().getUpper() == null) {
                SParameterInput.get().setUpper(Double.POSITIVE_INFINITY);
            }
        }
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    void refresh() {
        double fMean;
        double fSigma;
        if (SParameterInput.get() == null) {
            fSigma = 1;
        } else {
            fSigma = SParameterInput.get().getValue();
        }
        if (MParameterInput.get() == null) {
            fMean = 0;
        } else {
            fMean = MParameterInput.get().getValue();
        }
        if (hasMeanInRealSpace) {
            fMean = Math.log(fMean) - (0.5 * fSigma * fSigma);
        }
        dist.setMeanAndStdDev(fMean, fSigma);
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

        public LogNormalImpl(double fMean, double fStdDev) {
            setMeanAndStdDev(fMean, fStdDev);
        }

        @SuppressWarnings("deprecation")
		void setMeanAndStdDev(double fMean, double fStdDev) {
            m_fMean = fMean;
            m_fStdDev = fStdDev;
            m_normal.setMean(fMean);
            m_normal.setStandardDeviation(fStdDev);
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
        public double density(double fX) {
            if( fX <= 0 ) {
                return 0;
            }
            return m_normal.density(Math.log(fX)) / fX;
        }

        @Override
        public double logDensity(double fX) {
            if( fX <= 0 ) {
                return  Double.NEGATIVE_INFINITY;
            }
            return m_normal.logDensity(Math.log(fX)) - Math.log(fX);
        }
    } // class LogNormalImpl

    @Override
    public double getMean() {
    	if (hasMeanInRealSpace) {
    		if (MParameterInput.get() != null) {
    			return offsetInput.get() + MParameterInput.get().getValue();
    		} else {
    			return offsetInput.get();
    		}
    	} else {
    		throw new RuntimeException("Not implemented yet");
    	}
    }
}
