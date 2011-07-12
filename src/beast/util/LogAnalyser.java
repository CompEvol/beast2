package beast.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.core.util.ESS;

public class LogAnalyser {
    
    /** column labels in log file **/
    String [] m_sLabels;
    
    /** distinguish various column types **/
    enum type {REAL, BOOL, NOMINAL};
    type [] m_types;
    /** range of a column, if it is not a REAL **/
    List<String> [] m_ranges;
    
    /** data from log file with burn-in removed **/
    Double [][] m_fTraces;
    
    /** statistics on the data, one per column. First column (sample nr) is not set **/
    Double [] m_fMean, m_fStdDev, m_fMedian, m_f95HPDup, m_f95HPDlow, m_fESS, m_fACT, m_fGeometricMean;
    
	/** MAX_LAG typical = 2000; = maximum lag for ESS 
      * nBurnInPercentage typical = 10; percentage of data that can be ignored 
      * **/
	public LogAnalyser(String [] args, int MAX_LAG, int nBurnInPercentage) throws Exception {
		String sFile = args[args.length - 1];
		readLogFile(sFile, nBurnInPercentage);
		calcStats(MAX_LAG);
	}

	void readLogFile(String sFile, int nBurnInPercentage) throws Exception {
		BufferedReader fin = new BufferedReader(new FileReader(sFile));
		String sStr = null;
		int nData = 0;
		// first, sweep through the log file to determine size of the log
		while (fin.ready()) {
			sStr = fin.readLine();
			if (sStr.indexOf('#') < 0 && sStr.matches(".*[0-9a-zA-Z].*")) //{
				if (m_sLabels == null) 
					m_sLabels = sStr.split("\\s");
				else 
					nData++;
			//}
		}
		// reserve memory
		int nItems = m_sLabels.length;
		m_ranges = new List[nItems];
		int nBurnIn = nData * nBurnInPercentage / 100;
		m_fTraces = new Double[nItems][nData - nBurnIn];
		fin = new BufferedReader(new FileReader(sFile));
		nData = -nBurnIn - 1;
		// grab data from the log, ignoring burn in samples
		while (fin.ready()) {
			sStr = fin.readLine();
			int i = 0;
			if (sStr.indexOf('#') < 0 && sStr.matches(".*[0-9].*")) // {
				//nData++;
				if (++nData >= 0) //{
					for (String sStr2 : sStr.split("\\s")) {
						try {
							m_fTraces[i][nData] = Double.parseDouble(sStr2);
						} catch (Exception e) {
							if (m_ranges[i] == null) {
								m_ranges[i] = new ArrayList<String>(); 
							}
							if (!m_ranges[i].contains(sStr2)) {
								m_ranges[i].add(sStr2);
							}
							m_fTraces[i][nData] = 1.0* m_ranges[i].indexOf(sStr2);
						}
						i++;
					}
				//}
			//}
		}
		// determine types
		m_types = new type[nItems];
		Arrays.fill(m_types, type.REAL);
		for (int i = 0; i < nItems; i++)
			if (m_ranges[i] != null)
				if (m_ranges[i].size() == 2 && m_ranges[i].contains("true") && m_ranges[i].contains("false") ||
						m_ranges[i].size() == 1 && (m_ranges[i].contains("true") || m_ranges[i].contains("false")))
					m_types[i] = type.BOOL;
				else
					m_types[i] = type.NOMINAL;
				
	} // readLogFile

    /** calculate statistics on the data, one per column. 
     * First column (sample nr) is not set **/
	void calcStats(int MAX_LAG) {
		int nItems = m_sLabels.length;
		m_fMean     = new Double[nItems];
		m_fStdDev   = new Double[nItems];
		m_fMedian   = new Double[nItems];
		m_f95HPDlow = new Double[nItems];
		m_f95HPDup  = new Double[nItems];
		m_fESS      = new Double[nItems];
		m_fACT      = new Double[nItems];
		m_fGeometricMean = new Double[nItems];
		int nSampleInterval = (int)(m_fTraces[0][1] - m_fTraces[0][0]); 
		for (int i = 1; i < nItems; i++) {
			// calc mean and standard deviation
			Double [] fTrace = m_fTraces[i];
			double fSum = 0, fSum2 = 0;
			for (double f : fTrace) {
				fSum += f;
				fSum2 += f*f;
			}
			if (m_types[i] != type.NOMINAL) {
				m_fMean[i] = fSum/fTrace.length;
				m_fStdDev[i] = Math.sqrt(fSum2/fTrace.length - m_fMean[i]*m_fMean[i]);
			} else {
				m_fMean[i] = Double.NaN;
				m_fStdDev[i] = Double.NaN;
			}
			
			if (m_types[i] == type.REAL) {
				// calc median, and 95% HPD interval
				Double [] fSorted = fTrace.clone();
				Arrays.sort(fSorted);
				m_fMedian[i]   = fSorted[fTrace.length/2];
				// n instances cover 95% of the trace, reduced down by 1 to match Tracer
				int n = (int)((fSorted.length-1) * 95.0/100.0);
				double fMinRange = Double.MAX_VALUE;
				int hpdIndex = 0;
		        for (int k = 0; k < fSorted.length - n; k++) {
		            double fRange = fSorted[k + n] - fSorted[k];
		            if (fRange < fMinRange) {
		                fMinRange = fRange;
		                hpdIndex = k;
		            }
		        }
				m_f95HPDlow[i] = fSorted[hpdIndex];
				m_f95HPDup[i]  = fSorted[hpdIndex + n];
	
				// calc effective sample size
				m_fACT[i] = ESS.ACT(m_fTraces[i], nSampleInterval);
				m_fESS[i] = fTrace.length / (m_fACT[i] / nSampleInterval);
				
				// calc geometric mean
				if (fSorted[0] > 0) {
					// geometric mean is only defined when all elements are positive
					double gm = 0;
					for (double f : fTrace) 
			            gm += Math.log(f);
			        m_fGeometricMean[i] = Math.exp(gm/(double) fTrace.length);
				} else 
					m_fGeometricMean[i] = Double.NaN;
			} else {
				m_fMedian[i]        = Double.NaN;
				m_f95HPDlow[i]      = Double.NaN;
				m_f95HPDup[i]       = Double.NaN;;
				m_fACT[i]           = Double.NaN;;
				m_fESS[i]           = Double.NaN;;
				m_fGeometricMean[i] = Double.NaN;
			}
		}
	} // calcStats
	
    public void setData(Double[][] fTraces, String [] sLabels, type [] types, int MAX_LAG) {
    	m_fTraces = fTraces.clone();
    	m_sLabels = sLabels.clone();
    	m_types = types.clone();
    	calcStats(MAX_LAG);
    }
    public void setData(Double [] fTrace, int nSampleStep) {
    	Double [][] fTraces = new Double[2][];
    	fTraces[0] = new Double[fTrace.length];
    	for (int i = 0; i < fTrace.length; i++) {
    		fTraces[0][i] = (double) i * nSampleStep;
    	}
    	fTraces[1] = fTrace.clone();
    	setData(fTraces, new String[] {"column","data"}, new type [] {type.REAL, type.REAL}, 2000);
    }
    
    public double getMean(int iColumn) {return m_fMean[iColumn];}
    public double getStdDev(int iColumn) {return m_fStdDev[iColumn];}
    public double getMedian(int iColumn) {return m_fMedian[iColumn];}
    public double get95HPDup(int iColumn) {return m_f95HPDup[iColumn];}
    public double get95HPDlow(int iColumn) {return m_f95HPDlow[iColumn];}
    public double getESS(int iColumn) {return m_fESS[iColumn];}
    public double getACT(int iColumn) {return m_fACT[iColumn];}
    public double getGeometricMean(int iColumn) {return m_fGeometricMean[iColumn];}
	
    public double getMean(Double [] fTrace) {
    	setData(fTrace, 1);
    	return m_fMean[1];
    }
    public double getStdDev(Double [] fTrace) {
		setData(fTrace, 1);
		return m_fStdDev[1];
	}
    public double getMedian(Double [] fTrace) {
		setData(fTrace, 1);
		return m_fMedian[1];
	}
    public double get95HPDup(Double [] fTrace) {
		setData(fTrace, 1);
		return m_f95HPDup[1];
	}
    public double get95HPDlow(Double [] fTrace) {
		setData(fTrace, 1);
		return m_f95HPDlow[1];
	}
    public double getESS(Double [] fTrace) {
		setData(fTrace, 1);
		return m_fESS[1];
	}
    public double getACT(Double [] fTrace, int nSampleStep) {
		setData(fTrace, nSampleStep);
		return m_fACT[1];
	}
    public double getGeometricMean(Double [] fTrace) {
		setData(fTrace, 1);
		return m_fGeometricMean[1];
	}

    /** print statistics for each column except first column (sample nr). **/
	public void print() {
		System.out.println("item\tmean\tstddev\tmedian\t95%HPD-upper\t95%HPD-lower\tACT\tESS\tgeometric-mean");
		for (int i = 1; i < m_sLabels.length; i++) 
			System.out.println(m_sLabels[i] + "\t" + m_fMean[i] + "\t" + m_fStdDev[i] + 
					"\t" + m_fMedian[i] + "\t" + m_f95HPDlow[i]+ "\t" + m_f95HPDup[i] + 
					"\t" + m_fACT[i]+ "\t" + m_fESS[i] + "\t" + m_fGeometricMean[i]);
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			LogAnalyser analyser = new LogAnalyser(args, 2000, 10);
			analyser.print();
		} catch (Exception e) {e.printStackTrace();}
	}

}
