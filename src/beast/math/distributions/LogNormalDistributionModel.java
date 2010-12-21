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
    public Input<RealParameter> MParameter = new Input<RealParameter>("M", "M parameter of lognormal distribution. Equal to the mean of the log-transformed distribution.");
    public Input<RealParameter> SParameter = new Input<RealParameter>("S", "S parameter of lognormal distribution. Equal to the standard deviation of the log-transformed distribution.");
    public Input<Boolean> m_bMeanInRealSpaceInput = new Input<Boolean>("meanInRealSpace", "Whether the M parameter is in real space, or in log-transformed space. Default false = log-transformed.", false);

    boolean m_bMeanInRealSpace ;
    LogNormalImpl m_dist = new LogNormalImpl(0, 1);
    
    public void initAndValidate() throws Exception {
    	m_bMeanInRealSpace = m_bMeanInRealSpaceInput.get();
        if (MParameter.get() != null) {
            if (MParameter.get().getLower() == null) {
                MParameter.get().setLower(Double.NEGATIVE_INFINITY);
            }
            if (MParameter.get().getUpper() == null) {
                MParameter.get().setUpper(Double.POSITIVE_INFINITY);
            }
        }

        if (SParameter.get() != null) {
            if (SParameter.get().getLower() == null) {
                SParameter.get().setLower(0.0);
            }
            if (SParameter.get().getUpper() == null) {
                SParameter.get().setUpper(Double.POSITIVE_INFINITY);
            }
        }
        refresh();
    }

	/** make sure internal state is up to date **/
	void refresh() {
		double fMean;
		double fSigma;
		if (SParameter.get() == null) {
			fSigma = 1;
		} else {
			fSigma = SParameter.get().getValue();
		}
		if (MParameter.get() == null) {
			fMean = 0;
		} else {
			fMean = MParameter.get().getValue();
		}
		if (m_bMeanInRealSpace) {
			fMean = Math.log(fMean) - (0.5 * fSigma * fSigma);
		}
		m_dist.setMeanAndStdDev(fMean, fSigma);
	}
	
	@Override
	public Distribution getDistribution() {
		return m_dist;
	}

	class LogNormalImpl implements ContinuousDistribution {
		double m_fMean;
		double m_fStdDev;
	    NormalDistributionImpl m_normal = new NormalDistributionImpl(0, 1);
	    LogNormalImpl(double fMean, double fStdDev) {
	    	setMeanAndStdDev(fMean, fStdDev);
	    }
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
	        return m_normal.density(Math.log(fX)) / fX;
		}
		@Override
		public double logDensity(double fX) {
	        return m_normal.logDensity(Math.log(fX)) - Math.log(fX);
		}
	} // class LogNormalImpl
    
}
