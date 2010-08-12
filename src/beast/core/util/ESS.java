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

@Description("Report effective sample size of a parameter or distribution. " +
		"This uses the same criterion as Tracer and assumes 10% burn in.")
public class ESS extends Plugin implements Loggable {
	public Input<RealParameter> m_pParam = new Input<RealParameter>("parameter","real valued parameter to report ESS for");
	public Input<Distribution> m_pDistribution = new Input<Distribution>("distribution","probability distribution to report ESS for", Validate.XOR, m_pParam);

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
		String sID = (m_distribution == null? m_pParam.get().getID() : m_distribution.getID());
		out.print("ESS("+sID+")\t");
	}

    final static int MAX_LAG = 2000;

    @Override
	public void log(int nSample, PrintStream out) {
		Double fNewValue = (m_distribution == null? m_pParam.get().getValue() : m_distribution.getCurrentLogP());
		m_trace.add(fNewValue);
		
        int nSamples = m_trace.size();
        double fESS = 0;
        if (nSamples < 5) {
        	// don't bother if we only have 5 samples
            fESS = 0;
            out.print(fESS + "\t");
            return;
        }

        // take 10% burn in
        int iStart = nSamples/10;
        int nMaxLag = Math.min(nSamples - iStart, MAX_LAG);

        // calculate mean
        double fMean = 0;
        for (int i = iStart; i < nSamples; i++) {
            fMean += m_trace.get(i);
        }
        fMean /= (nSamples -iStart);
    
        // calculate auto correlation for selected lag times
        double[] fAutoCorrelation = new double[nMaxLag];
        for (int iLag = 0; iLag < nMaxLag; iLag++) {
            for (int j = iStart; j < nSamples - iLag; j++) {
                final double del1 = m_trace.get(j) - fMean;
                final double del2 = m_trace.get(j + iLag) - fMean;
                fAutoCorrelation[iLag] += (del1 * del2);
            }
            fAutoCorrelation[iLag] /= ((double) (nSamples - iStart - iLag));
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
                    nMaxLag = iLag;
                }
            }
        }

        // auto correlation time
        double fACT = varStat / fAutoCorrelation[0];

        // effective sample size
        fESS = (nSamples - iStart) / fACT;
        String sStr = fESS +"";
       	sStr = sStr.substring(0, sStr.indexOf('.') + 2);
        out.print(sStr + "\t");
    } // log

	@Override
	public void close(PrintStream out) {
		// nothing to do
	}

}
