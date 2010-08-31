package beast.core.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
//import beast.core.Density;
import beast.core.Input;
import beast.core.Valuable;
import beast.core.Input.Validate;
import beast.core.Loggable;
import beast.core.Plugin;
//import beast.core.parameter.RealParameter;

@Description("Report effective sample size of a parameter or log values from a distribution. " +
		"This uses the same criterion as Tracer and assumes 10% burn in.")
public class ESS extends Plugin implements Loggable {
	public Input<Valuable> m_pParam =
            new Input<Valuable>("arg","value (e.g. parameter or distribution) to report ESS for", Validate.REQUIRED);
//	public Input<Density> m_pDistribution =
//            new Input<Density>("distribution","probability distribution to report ESS for", Validate.XOR, m_pParam);

	/** values from which the ESS is calculated **/
	List<Double> m_trace;
	/** sum of trace, excluding burn-in **/
	double m_fSum = 0;
	/** keep track of sums of trace(i)*trace(i_+ lag) for all lags, excluding burn-in  **/
    List<Double> m_fSquareLaggedSums;
//	/** shadow of distribution input (if any) **/
//	Density m_distribution;
	
	@Override
	public void initAndValidate() {
//		if (m_pParam.get() == null) {
//			m_distribution = m_pDistribution.get();
//		}
		m_trace = new ArrayList<Double>();
		m_fSquareLaggedSums = new ArrayList<Double>();
	}
	
	@Override
	public void init(PrintStream out) throws Exception {
//		final String sID = (m_distribution == null? m_pParam.get().getID() : m_distribution.getID());
		final String sID = ((Plugin) m_pParam.get()).getID();
		out.print("ESS("+sID+")\t");
	}

    final static int MAX_LAG = 2000;

//  We determine the Effective Sample Size (ESS) based on the auto correlation (AC) between the sequence and the same
//  sequence delayed by some amount.  For a highly correlated sequence the AC will be high for a small delay,
//  and is expected to drop to around zero when the delay is large enough. The delay when the AC is zero is the ACT (auto
//  correlation time), and the ESS is the number of samples remaining when keeping only one sample out of every ACT.
//
//  The (squared) auto correlation between two sequences is the covariance divided by the product of the individual
//  variances. Since both sequences are essentially the same sequence we do not bother to scale.
//
//  The simplest criteria to use to find the point where the AC "gets" to zero is to take the first time it becomes
//  negative. This is deemed too simple and instead we first find the approximate point - the first time where the sum of
//  two consecutive values is negative, and then determine the ACT by assuming the AC - as a function of the delay - is
//  roughly linear and so the ACT (the point on the X axis) is approximately equal to twice the area under the curve divided
//  by the value at x=0 (the AC of the sequence). This is the reason for summing up twice the variances inside the loop - a
//  basic numerical integration technique.

    @Override
	public void log(final int nSample, PrintStream out) {
//		final Double fNewValue = (m_distribution == null? m_pParam.get().getValue() : m_distribution.getCurrentLogP());
		final Double fNewValue = m_pParam.get().getArrayValue();
		m_trace.add(fNewValue);
		m_fSum += fNewValue;
		
        final int nTotalSamples = m_trace.size();

        // take 10% burn in
        final int iStart = nTotalSamples/10;
        if (iStart != ((nTotalSamples-1)/10)) {
        	// compensate for 10% burnin
        	m_fSum -= m_trace.get((nTotalSamples-1)/10);
        }
        final int nSamples = nTotalSamples - iStart;
        final int nMaxLag = Math.min(nSamples, MAX_LAG);

        // calculate mean
        final double fMean = m_fSum / nSamples;

        if (iStart != ((nTotalSamples-1)/10)) {
        	// compensate for 10% burnin
        	int iTrace = ((nTotalSamples-1)/10);
            for (int iLag = 0; iLag < m_fSquareLaggedSums.size(); iLag++) {
                m_fSquareLaggedSums.set(iLag, m_fSquareLaggedSums.get(iLag) - m_trace.get(iTrace) * m_trace.get(iTrace + iLag));             
            }        	
        }
        
        while (m_fSquareLaggedSums.size()< nMaxLag) {
        	m_fSquareLaggedSums.add(0.0);
        }
    
        // calculate auto correlation for selected lag times
        double[] fAutoCorrelation = new double[nMaxLag];
        // fSum1 = \sum_{iStart ... nTotalSamples-iLag-1} trace
    	double fSum1 = m_fSum;
        // fSum1 = \sum_{iStart+iLag ... nTotalSamples-1} trace
    	double fSum2 = m_fSum;
        for (int iLag = 0; iLag < nMaxLag; iLag++) {
            m_fSquareLaggedSums.set(iLag, m_fSquareLaggedSums.get(iLag) + m_trace.get(nTotalSamples - iLag - 1) * m_trace.get(nTotalSamples - 1));
            // The following line is the same approximation as in Tracer 
            // (valid since fMean *(nSamples - iLag), fSum1, and fSum2 are approximately the same)
            // though a more accurate estimate would be
            // fAutoCorrelation[iLag] = m_fSquareLaggedSums.get(iLag) - fSum1 * fSum2
            fAutoCorrelation[iLag] = m_fSquareLaggedSums.get(iLag) - (fSum1 + fSum2) * fMean + fMean * fMean * (nSamples - iLag);
            fAutoCorrelation[iLag] /= ((double) (nSamples - iLag));
        	fSum1 -= m_trace.get(nTotalSamples - 1 - iLag);
        	fSum2 -= m_trace.get(iStart + iLag);
        }

        double integralOfACFunctionTimes2 = 0.0;
        for (int iLag = 0; iLag < nMaxLag; iLag++) {
            if (iLag == 0) {
                integralOfACFunctionTimes2 = fAutoCorrelation[0];
            } else if (iLag % 2 == 0) {
                // fancy stopping criterion - see main comment
                if (fAutoCorrelation[iLag - 1] + fAutoCorrelation[iLag] > 0) {
                    integralOfACFunctionTimes2 += 2.0 * (fAutoCorrelation[iLag - 1] + fAutoCorrelation[iLag]);
                } else {
                    // stop
                    break;
                }
            }
        }

        // auto correlation time
        final double fACT = integralOfACFunctionTimes2 / fAutoCorrelation[0];

        // effective sample size
        final double fESS = nSamples / fACT;
        String sStr = fESS +"";
       	sStr = sStr.substring(0, sStr.indexOf('.') + 2);
        out.print(sStr + "\t");
    } // log

	@Override
	public void close(PrintStream out) {
		// nothing to do
	}

} // class ESS
