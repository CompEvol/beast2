package beast.core.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Loggable;
import beast.core.Plugin;
import beast.core.parameter.RealParameter;

@Description("Report effective sample size of a parameter or log values from a distribution. " +
		"This uses the same criterion as Tracer and assumes 10% burn in.")
public class ESS extends Plugin implements Loggable {
	public Input<RealParameter> m_pParam =
            new Input<RealParameter>("parameter","real valued parameter to report ESS for");
	public Input<Distribution> m_pDistribution =
            new Input<Distribution>("distribution","probability distribution to report ESS for", Validate.XOR, m_pParam);

	Distribution m_distribution;
	List<Double> m_trace;
	
	@Override
	public void initAndValidate() {
		if (m_pParam.get() == null) {
			m_distribution = m_pDistribution.get();
		}
		m_trace = new ArrayList<Double>();
	}
	
	@Override
	public void init(PrintStream out) throws Exception {
		final String sID = (m_distribution == null? m_pParam.get().getID() : m_distribution.getID());
		out.print("ESS("+sID+")\t");
	}

    final static int MAX_LAG = 2000;

//  We determine the Effective Sample Size (ESS) based on the auto correlation (AC) between the sequence and the same
//  sequence delayed by some amount.  For a highly correlated sequence the AC will be high for a small delay,
//  and is expected to drop to around zero when the delay is large enough. The delay when the AC is zero is the ACT (auto
//  correlation time), and the ESS is the number of samples ramining when keeping only one sample out of every ACT.
//
//  The (squared) auto correlation between two sequences is the covariance divided by the product of the individual
//  variances. Since both sequences are essentially the same sequence we do not bother to scale.
//
//  The simplest criteria to use to find the point where the AC "gets" to zero is to take the first time it becomes
//  negative. This is deemed too simple and instead we first find the approximate point - the first time where the sum of
//  two consecutive values is negative, and then determine the ACT by assuming the AC - as a function of the delay - is
//  roughly linear and so the ACT (the point on the X axis) is approximetly equal to twice the area under the curve divided
//  by the value at x=0 (the AC of the sequence). This is the reason for summing up twice the variances inside the loop - a
//  basic numerical integration technique.

    @Override
	public void log(final int nSample, PrintStream out) {
		final Double fNewValue = (m_distribution == null? m_pParam.get().getValue() : m_distribution.getCurrentLogP());
		m_trace.add(fNewValue);
		
        final int nTotalSamples = m_trace.size();

        if (nTotalSamples < 5) {
        	// don't bother if we only have 5 samples
            out.print(0 + "\t");
            return;
        }

        // take 10% burn in
        final int iStart = nTotalSamples/10;
        final int nSamples = nTotalSamples - iStart;
        final int nMaxLag = Math.min(nSamples, MAX_LAG);

        // calculate mean
        double fSum = 0;
        for (int i = iStart; i < nTotalSamples; i++) {
            fSum += m_trace.get(i);
        }
        final double fMean = fSum / nSamples;
    
        // calculate auto correlation for selected lag times
        double[] fAutoCorrelation = new double[nMaxLag];
        for (int iLag = 0; iLag < nMaxLag; iLag++) {
            for (int j = iStart; j < nTotalSamples - iLag; j++) {
                final double del1 = m_trace.get(j) - fMean;
                final double del2 = m_trace.get(j + iLag) - fMean;
                fAutoCorrelation[iLag] += (del1 * del2);
            }
            fAutoCorrelation[iLag] /= ((double) (nSamples - iLag));
        }

        // calculate the magical variable 'varStat', RRB: what is this doing exactly?
        double varStat = 0.0;
        for (int iLag = 0; iLag < nMaxLag; iLag++) {
            if (iLag == 0) {
                varStat = fAutoCorrelation[0];
            } else if (iLag % 2 == 0) {
                // fancy stopping criterion :)
            	// RRB: this gets me confused. I thought it would be a matter of just adding autocorelations...
                if (fAutoCorrelation[iLag - 1] + fAutoCorrelation[iLag] > 0) {
                    varStat += 2.0 * (fAutoCorrelation[iLag - 1] + fAutoCorrelation[iLag]);
                } else {
                    // stop
                    break;
                }
            }
        }

        // auto correlation time
        final double fACT = varStat / fAutoCorrelation[0];

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

}
