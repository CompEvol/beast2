package beast.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import beast.app.beastapp.BeastVersion;
import beast.app.util.Utils;
import beast.core.util.ESS;

public class LogAnalyser {

    /**
     * column labels in log file *
     */
    protected String[] m_sLabels;

    /**
     * distinguish various column types *
     */
    protected enum type {
        REAL, INTEGER, BOOL, NOMINAL
    }

    ;
    protected type[] m_types;
    /**
     * range of a column, if it is not a REAL *
     */
    protected List<String>[] m_ranges;

    /**
     * data from log file with burn-in removed *
     */
    protected Double[][] m_fTraces;

    /**
     * statistics on the data, one per column. First column (sample nr) is not set *
     */
    Double[] m_fMean, m_fStdError, m_fStdDev, m_fMedian, m_f95HPDup, m_f95HPDlow, m_fESS, m_fACT, m_fGeometricMean;

    /**
     * used for storing comments before the actual log file commences *
     */
    protected String m_sPreAmble;

    final protected static String BAR = "|---------|---------|---------|---------|---------|---------|---------|---------|";

    public LogAnalyser() {
    }

    /**
     * MAX_LAG typical = 2000; = maximum lag for ESS
     * nBurnInPercentage typical = 10; percentage of data that can be ignored
     * *
     */
    public LogAnalyser(String[] args, int MAX_LAG, int nBurnInPercentage) throws Exception {
        String sFile = args[args.length - 1];
        readLogFile(sFile, nBurnInPercentage);
        calcStats(MAX_LAG);
    }

    protected void readLogFile(String sFile, int nBurnInPercentage) throws Exception {
        log("\nLoading " + sFile);
        BufferedReader fin = new BufferedReader(new FileReader(sFile));
        String sStr = null;
        m_sPreAmble = "";
        m_sLabels = null;
        int nData = 0;
        // first, sweep through the log file to determine size of the log
        while (fin.ready()) {
            sStr = fin.readLine();
            if (sStr.indexOf('#') < 0 && sStr.matches(".*[0-9a-zA-Z].*")) {
                if (m_sLabels == null)
                    m_sLabels = sStr.split("\\s");
                else
                    nData++;
            } else {
                m_sPreAmble += sStr + "\n";
            }
        }
        int nLines = Math.max(1, nData / 80);
        // reserve memory
        int nItems = m_sLabels.length;
        m_ranges = new List[nItems];
        int nBurnIn = nData * nBurnInPercentage / 100;
        m_fTraces = new Double[nItems][nData - nBurnIn];
        fin = new BufferedReader(new FileReader(sFile));
        nData = -nBurnIn - 1;
        logln(" skipping " + nBurnIn + " log lines\n\n" + BAR);
        // grab data from the log, ignoring burn in samples
        m_types = new type[nItems];
        Arrays.fill(m_types, type.INTEGER);
        while (fin.ready()) {
            sStr = fin.readLine();
            int i = 0;
            if (sStr.indexOf('#') < 0 && sStr.matches("[0-9].*")) // {
                //nData++;
                if (++nData >= 0) //{
                    for (String sStr2 : sStr.split("\\s")) {
                        try {
                            if (sStr2.indexOf('.') >= 0) {
                                m_types[i] = type.REAL;
                            }
                            m_fTraces[i][nData] = Double.parseDouble(sStr2);
                        } catch (Exception e) {
                            if (m_ranges[i] == null) {
                                m_ranges[i] = new ArrayList<String>();
                            }
                            if (!m_ranges[i].contains(sStr2)) {
                                m_ranges[i].add(sStr2);
                            }
                            m_fTraces[i][nData] = 1.0 * m_ranges[i].indexOf(sStr2);
                        }
                        i++;
                    }
            //}
            //}
            if (nData % nLines == 0) {
                log("*");
            }
        }
        logln("");
        // determine types
        for (int i = 0; i < nItems; i++)
            if (m_ranges[i] != null)
                if (m_ranges[i].size() == 2 && m_ranges[i].contains("true") && m_ranges[i].contains("false") ||
                        m_ranges[i].size() == 1 && (m_ranges[i].contains("true") || m_ranges[i].contains("false")))
                    m_types[i] = type.BOOL;
                else
                    m_types[i] = type.NOMINAL;

    } // readLogFile

    /**
     * calculate statistics on the data, one per column.
     * First column (sample nr) is not set *
     */
    void calcStats(int MAX_LAG) {
        logln("\nCalculating statistics\n\n" + BAR);
        int nStars = 0;
        int nItems = m_sLabels.length;
        m_fMean = new Double[nItems];
        m_fStdError = new Double[nItems];
        m_fStdDev = new Double[nItems];
        m_fMedian = new Double[nItems];
        m_f95HPDlow = new Double[nItems];
        m_f95HPDup = new Double[nItems];
        m_fESS = new Double[nItems];
        m_fACT = new Double[nItems];
        m_fGeometricMean = new Double[nItems];
        int nSampleInterval = (int) (m_fTraces[0][1] - m_fTraces[0][0]);
        for (int i = 1; i < nItems; i++) {
            // calc mean and standard deviation
            Double[] fTrace = m_fTraces[i];
            double fSum = 0, fSum2 = 0;
            for (double f : fTrace) {
                fSum += f;
                fSum2 += f * f;
            }
            if (m_types[i] != type.NOMINAL) {
                m_fMean[i] = fSum / fTrace.length;
                m_fStdDev[i] = Math.sqrt(fSum2 / fTrace.length - m_fMean[i] * m_fMean[i]);
            } else {
                m_fMean[i] = Double.NaN;
                m_fStdDev[i] = Double.NaN;
            }

            if (m_types[i] == type.REAL || m_types[i] == type.INTEGER) {
                // calc median, and 95% HPD interval
                Double[] fSorted = fTrace.clone();
                Arrays.sort(fSorted);
                m_fMedian[i] = fSorted[fTrace.length / 2];
                // n instances cover 95% of the trace, reduced down by 1 to match Tracer
                int n = (int) ((fSorted.length - 1) * 95.0 / 100.0);
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
                m_f95HPDup[i] = fSorted[hpdIndex + n];

                // calc effective sample size
                m_fACT[i] = ESS.ACT(m_fTraces[i], nSampleInterval);
                m_fStdError[i] = ESS.stdErrorOfMean(fTrace, nSampleInterval);
                m_fESS[i] = fTrace.length / (m_fACT[i] / nSampleInterval);

                // calc geometric mean
                if (fSorted[0] > 0) {
                    // geometric mean is only defined when all elements are positive
                    double gm = 0;
                    for (double f : fTrace)
                        gm += Math.log(f);
                    m_fGeometricMean[i] = Math.exp(gm / (double) fTrace.length);
                } else
                    m_fGeometricMean[i] = Double.NaN;
            } else {
                m_fMedian[i] = Double.NaN;
                m_f95HPDlow[i] = Double.NaN;
                m_f95HPDup[i] = Double.NaN;
                ;
                m_fACT[i] = Double.NaN;
                ;
                m_fESS[i] = Double.NaN;
                ;
                m_fGeometricMean[i] = Double.NaN;
            }
            while (nStars < 80 * (i + 1) / nItems) {
                log("*");
                nStars++;
            }
        }
        logln("\n");
    } // calcStats

    public void setData(Double[][] fTraces, String[] sLabels, type[] types, int MAX_LAG) {
        m_fTraces = fTraces.clone();
        m_sLabels = sLabels.clone();
        m_types = types.clone();
        calcStats(MAX_LAG);
    }

    public void setData(Double[] fTrace, int nSampleStep) {
        Double[][] fTraces = new Double[2][];
        fTraces[0] = new Double[fTrace.length];
        for (int i = 0; i < fTrace.length; i++) {
            fTraces[0][i] = (double) i * nSampleStep;
        }
        fTraces[1] = fTrace.clone();
        setData(fTraces, new String[]{"column", "data"}, new type[]{type.REAL, type.REAL}, 2000);
    }

    public double getMean(int iColumn) {
        return m_fMean[iColumn];
    }

    public double getStdDev(int iColumn) {
        return m_fStdDev[iColumn];
    }

    public double getMedian(int iColumn) {
        return m_fMedian[iColumn];
    }

    public double get95HPDup(int iColumn) {
        return m_f95HPDup[iColumn];
    }

    public double get95HPDlow(int iColumn) {
        return m_f95HPDlow[iColumn];
    }

    public double getESS(int iColumn) {
        return m_fESS[iColumn];
    }

    public double getACT(int iColumn) {
        return m_fACT[iColumn];
    }

    public double getGeometricMean(int iColumn) {
        return m_fGeometricMean[iColumn];
    }

    public double getMean(Double[] fTrace) {
        setData(fTrace, 1);
        return m_fMean[1];
    }

    public double getStdDev(Double[] fTrace) {
        setData(fTrace, 1);
        return m_fStdDev[1];
    }

    public double getMedian(Double[] fTrace) {
        setData(fTrace, 1);
        return m_fMedian[1];
    }

    public double get95HPDup(Double[] fTrace) {
        setData(fTrace, 1);
        return m_f95HPDup[1];
    }

    public double get95HPDlow(Double[] fTrace) {
        setData(fTrace, 1);
        return m_f95HPDlow[1];
    }

    public double getESS(Double[] fTrace) {
        setData(fTrace, 1);
        return m_fESS[1];
    }

    public double getACT(Double[] fTrace, int nSampleStep) {
        setData(fTrace, nSampleStep);
        return m_fACT[1];
    }

    public double getGeometricMean(Double[] fTrace) {
        setData(fTrace, 1);
        return m_fGeometricMean[1];
    }

    /**
     * print statistics for each column except first column (sample nr). *
     */
    final static String SPACE = " ";
    public void print(PrintStream out) {
        try {
            // delay so that stars can be flushed from stderr
            Thread.sleep(100);
        } catch (Exception e) {
        }
        int nMax = 0;
        for (int i = 1; i < m_sLabels.length; i++)
            nMax = Math.max(m_sLabels[i].length(), nMax);
        String sSpace = "";
        for (int i = 0; i < nMax; i++)
            sSpace += " ";

        out.println("item" + sSpace.substring(4) + " " + format("mean") + format("stderr")  + format("stddev")  + format("median")  + format("95%HPDlo")  + format("95%HPDup")  + format("ACT")  + format("ESS")  + format("geometric-mean"));
        for (int i = 1; i < m_sLabels.length; i++) {
            out.println(m_sLabels[i] + sSpace.substring(m_sLabels[i].length()) + SPACE +
                    format(m_fMean[i]) + SPACE + format(m_fStdError[i]) + SPACE + format(m_fStdDev[i]) +
                    SPACE + format(m_fMedian[i]) + SPACE + format(m_f95HPDlow[i]) + SPACE + format(m_f95HPDup[i]) +
                    SPACE + format(m_fACT[i]) + SPACE + format(m_fESS[i]) + SPACE + format(m_fGeometricMean[i]));
        }
    }

    protected void log(String s) {
        System.err.print(s);
    }

    protected void logln(String s) {
        System.err.println(s);
    }

    String format(String s) {
        while (s.length() < 8) {
            s += " ";
        }
    	return s + SPACE;
    }
    
    String format(Double d) {
        if (Double.isNaN(d)) {
            return "NaN     ";
        }
        if (Math.abs(d) > 1e-4 || d == 0) {
	        DecimalFormat f = new DecimalFormat("#0.######", new DecimalFormatSymbols(Locale.US));
	        String sStr = f.format(d);
	        if (sStr.length() > 8) {
	            sStr = sStr.substring(0, 8);
	        }
	        while (sStr.length() < 8) {
	            sStr += " ";
	        }
	        return sStr;
        } else {
	        DecimalFormat f = new DecimalFormat("0.##E0", new DecimalFormatSymbols(Locale.US));
	        String sStr = f.format(d);
	        if (sStr.length() > 8) {
		        String [] sStrs = sStr.split("E");
	            sStr =  sStrs[0].substring(0, 8 - sStrs[1].length() - 1) + "E" + sStrs[1];
	        }
	        while (sStr.length() < 8) {
	            sStr += " ";
	        }
	        return sStr;        	
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            LogAnalyser analyser;
            if (args.length == 0) {
                BeastVersion version = new BeastVersion();
                File file = Utils.getLoadFile("LogAnalyser " + version.getVersionString() + " - Select log file to analyse",
                        null, "BEAST log (*.log) Files", "log", "txt");
                if (file == null) {
                    return;
                }
                analyser = new LogAnalyser(new String[]{file.getAbsolutePath()}, 2000, 10);
                analyser.print(System.out);
            } else {
                analyser = new LogAnalyser(args, 2000, 10);
            }
            analyser.print(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
