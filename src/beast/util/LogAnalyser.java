package beast.util;

import static beast.util.OutputUtils.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.app.BEASTVersion;
import beast.app.util.Utils;
import beast.core.util.ESS;
import beast.core.util.Log;


public class LogAnalyser {
    public static final int BURN_IN_PERCENTAGE = 10; // default

    protected final String fileName;

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

    /**
     * If set, analyzer works in "quiet" mode.
     */
    protected boolean quiet = false;

    final protected static String BAR = "|---------|---------|---------|---------|---------|---------|---------|---------|";

    public LogAnalyser() {
        fileName = null;
    }

    /**
     *
     * @param args
     * @param nBurnInPercentage  nBurnInPercentage typical = 10; percentage of data that can be ignored
     * @throws Exception
     */
    public LogAnalyser(String[] args, int nBurnInPercentage) throws Exception {
        fileName = args[args.length - 1];
        readLogFile(fileName, nBurnInPercentage);
        calcStats();
    }

    public LogAnalyser(String[] args) throws Exception {
        this(args, BURN_IN_PERCENTAGE);
    }

    public LogAnalyser(String fileName, int nBurnInPercentage) throws Exception {
        this.fileName = fileName;
        readLogFile(fileName, nBurnInPercentage);
        calcStats();
    }

    public LogAnalyser(String fileName, int nBurnInPercentage, boolean quiet) throws Exception {
        this.fileName = fileName;
        this.quiet = quiet;
        readLogFile(fileName, nBurnInPercentage);
        calcStats();
    }

    public LogAnalyser(String fileName) throws Exception {
        this(fileName, BURN_IN_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
	protected void readLogFile(String fileName, int nBurnInPercentage) throws IOException {
        log("\nLoading " + fileName);
        BufferedReader fin = new BufferedReader(new FileReader(fileName));
        String str;
        m_sPreAmble = "";
        m_sLabels = null;
        int nData = 0;
        // first, sweep through the log file to determine size of the log
        while (fin.ready()) {
            str = fin.readLine();
            if (str.indexOf('#') < 0 && str.matches(".*[0-9a-zA-Z].*")) {
                if (m_sLabels == null)
                    m_sLabels = str.split("\\s");
                else
                    nData++;
            } else {
                m_sPreAmble += str + "\n";
            }
        }
        int nLines = Math.max(1, nData / 80);
        // reserve memory
        int nItems = m_sLabels.length;
        m_ranges = new List[nItems];
        int nBurnIn = nData * nBurnInPercentage / 100;
        m_fTraces = new Double[nItems][nData - nBurnIn];
        fin.close();
        fin = new BufferedReader(new FileReader(fileName));
        nData = -nBurnIn - 1;
        logln(", burnin " + nBurnInPercentage + "%, skipping " + nBurnIn + " log lines\n\n" + BAR);
        // grab data from the log, ignoring burn in samples
        m_types = new type[nItems];
        Arrays.fill(m_types, type.INTEGER);
        while (fin.ready()) {
            str = fin.readLine();
            int i = 0;
            if (str.indexOf('#') < 0 && str.matches("[0-9].*")) // {
                //nData++;
                if (++nData >= 0) //{
                    for (String str2 : str.split("\\s")) {
                        try {
                            if (str2.indexOf('.') >= 0) {
                                m_types[i] = type.REAL;
                            }
                            m_fTraces[i][nData] = Double.parseDouble(str2);
                        } catch (Exception e) {
                            if (m_ranges[i] == null) {
                                m_ranges[i] = new ArrayList<>();
                            }
                            if (!m_ranges[i].contains(str2)) {
                                m_ranges[i].add(str2);
                            }
                            m_fTraces[i][nData] = 1.0 * m_ranges[i].indexOf(str2);
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
        
        fin.close();
    } // readLogFile

    /**
     * calculate statistics on the data, one per column.
     * First column (sample nr) is not set *
     */
    void calcStats() {
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
            Double[] trace = m_fTraces[i];
            double sum = 0, sum2 = 0;
            for (double f : trace) {
                sum += f;
                sum2 += f * f;
            }
            if (m_types[i] != type.NOMINAL) {
                m_fMean[i] = sum / trace.length;
                m_fStdDev[i] = Math.sqrt(sum2 / trace.length - m_fMean[i] * m_fMean[i]);
            } else {
                m_fMean[i] = Double.NaN;
                m_fStdDev[i] = Double.NaN;
            }

            if (m_types[i] == type.REAL || m_types[i] == type.INTEGER) {
                // calc median, and 95% HPD interval
                Double[] sorted = trace.clone();
                Arrays.sort(sorted);
                m_fMedian[i] = sorted[trace.length / 2];
                // n instances cover 95% of the trace, reduced down by 1 to match Tracer
                int n = (int) ((sorted.length - 1) * 95.0 / 100.0);
                double minRange = Double.MAX_VALUE;
                int hpdIndex = 0;
                for (int k = 0; k < sorted.length - n; k++) {
                    double range = sorted[k + n] - sorted[k];
                    if (range < minRange) {
                        minRange = range;
                        hpdIndex = k;
                    }
                }
                m_f95HPDlow[i] = sorted[hpdIndex];
                m_f95HPDup[i] = sorted[hpdIndex + n];

                // calc effective sample size
                m_fACT[i] = ESS.ACT(m_fTraces[i], nSampleInterval);
                m_fStdError[i] = ESS.stdErrorOfMean(trace, nSampleInterval);
                m_fESS[i] = trace.length / (m_fACT[i] / nSampleInterval);

                // calc geometric mean
                if (sorted[0] > 0) {
                    // geometric mean is only defined when all elements are positive
                    double gm = 0;
                    for (double f : trace)
                        gm += Math.log(f);
                    m_fGeometricMean[i] = Math.exp(gm / trace.length);
                } else
                    m_fGeometricMean[i] = Double.NaN;
            } else {
                m_fMedian[i] = Double.NaN;
                m_f95HPDlow[i] = Double.NaN;
                m_f95HPDup[i] = Double.NaN;
                m_fACT[i] = Double.NaN;
                m_fESS[i] = Double.NaN;
                m_fGeometricMean[i] = Double.NaN;
            }
            while (nStars < 80 * (i + 1) / nItems) {
                log("*");
                nStars++;
            }
        }
        logln("\n");
    } // calcStats

    public void setData(Double[][] traces, String[] labels, type[] types) {
        m_fTraces = traces.clone();
        m_sLabels = labels.clone();
        m_types = types.clone();
        calcStats();
    }

    public void setData(Double[] trace, int nSampleStep) {
        Double[][] traces = new Double[2][];
        traces[0] = new Double[trace.length];
        for (int i = 0; i < trace.length; i++) {
            traces[0][i] = (double) i * nSampleStep;
        }
        traces[1] = trace.clone();
        setData(traces, new String[]{"column", "data"}, new type[]{type.REAL, type.REAL});
    }

    public int indexof(String label) {
        return CollectionUtils.indexof(label, m_sLabels);
	}

    /**
     * First column "Sample" (sample nr) needs to be removed
     * @return
     */
    public List<String> getLabels() {
        if (m_sLabels.length < 2)
            return new ArrayList<>();
        return CollectionUtils.toList(m_sLabels, 1, m_sLabels.length);
    }

    public Double [] getTrace(int index) {
    	return m_fTraces[index].clone();
    }

    public Double [] getTrace(String label) {
    	return m_fTraces[indexof(label)].clone();
    }

    public double getMean(String label) {
        return getMean(indexof(label));
    }

    public double getStdError(String label) {
        return getStdError(indexof(label));
    }

    public double getStdDev(String label) {
        return getStdDev(indexof(label));
    }

    public double getMedian(String label) {
        return getMedian(indexof(label));
    }

    public double get95HPDup(String label) {
        return get95HPDup(indexof(label));
    }

    public double get95HPDlow(String label) {
        return get95HPDlow(indexof(label));
    }

    public double getESS(String label) {
        return getESS(indexof(label));
    }

    public double getACT(String label) {
        return getACT(indexof(label));
    }

    public double getGeometricMean(String label) {
        return getGeometricMean(indexof(label));
    }

    public double getMean(int iColumn) {
        return m_fMean[iColumn];
    }

    public double getStdDev(int iColumn) {
        return m_fStdDev[iColumn];
    }

    public double getStdError(int iColumn) {
        return m_fStdError[iColumn];
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

    public double getMean(Double[] trace) {
        setData(trace, 1);
        return m_fMean[1];
    }

    public double getStdDev(Double[] trace) {
        setData(trace, 1);
        return m_fStdDev[1];
    }

    public double getMedian(Double[] trace) {
        setData(trace, 1);
        return m_fMedian[1];
    }

    public double get95HPDup(Double[] trace) {
        setData(trace, 1);
        return m_f95HPDup[1];
    }

    public double get95HPDlow(Double[] trace) {
        setData(trace, 1);
        return m_f95HPDlow[1];
    }

    public double getESS(Double[] trace) {
        setData(trace, 1);
        return m_fESS[1];
    }

    public double getACT(Double[] trace, int nSampleStep) {
        setData(trace, nSampleStep);
        return m_fACT[1];
    }

    public double getGeometricMean(Double[] trace) {
        setData(trace, 1);
        return m_fGeometricMean[1];
    }

    public String getLogFile() {
        return fileName;
    }

    /**
     * print statistics for each column except first column (sample nr). *
     */
    final String SPACE = OutputUtils.SPACE;
    public void print(PrintStream out) {
    	// set up header for prefix, if any is specified
    	String prefix = System.getProperty("prefix");
    	String prefixHead = (prefix == null ? "" : "prefix ");
    	if (prefix != null) {
	    	String [] p = prefix.trim().split("\\s+");
	    	if (p.length > 1) {
	    		prefixHead = "";
	    		for (int i = 0; i < p.length; i++) {
	    			prefixHead += "prefix" + i + " ";
	    		}
	    	}
    	}
    	
        try {
            // delay so that stars can be flushed from stderr
            Thread.sleep(100);
        } catch (Exception e) {
        }
        int nMax = 0;
        for (int i = 1; i < m_sLabels.length; i++)
            nMax = Math.max(m_sLabels[i].length(), nMax);
        String space = "";
        for (int i = 0; i < nMax; i++)
            space += " ";

        out.println("item" + space.substring(4) + " " + prefixHead +
        		format("mean") + format("stderr")  + format("stddev")  + format("median")  + format("95%HPDlo")  + format("95%HPDup")  + format("ACT")  + format("ESS")  + format("geometric-mean"));
        for (int i = 1; i < m_sLabels.length; i++) {
            out.println(m_sLabels[i] + space.substring(m_sLabels[i].length()) + SPACE + (prefix == null ? "" : prefix + SPACE) +
                    format(m_fMean[i]) + SPACE + format(m_fStdError[i]) + SPACE + format(m_fStdDev[i]) +
                    SPACE + format(m_fMedian[i]) + SPACE + format(m_f95HPDlow[i]) + SPACE + format(m_f95HPDup[i]) +
                    SPACE + format(m_fACT[i]) + SPACE + format(m_fESS[i]) + SPACE + format(m_fGeometricMean[i]));
        }
    }

    /**
     * Display header used in one-line mode.
     *
     * @param out output stream
     */
    public void printOneLineHeader(PrintStream out) {

        String[] postFix = {
                "mean", "stderr", "stddev",
                "median", "95%HPDlo", "95%HPDup",
                "ACT", "ESS", "geometric-mean"
        };

        for (int paramIdx=1; paramIdx<m_sLabels.length; paramIdx++) {
            for (int i=0; i<postFix.length; i++) {
                if (paramIdx> 1 || i>0)
                    out.print("\t");

                out.print(m_sLabels[paramIdx] + "." + postFix[i]);
            }
        }

        out.println();
    }

    /**
     * Display results for single log on one line.
     *
     * @param out output stream
     */
    public void printOneLine(PrintStream out) {

        for (int paramIdx=1; paramIdx<m_sLabels.length; paramIdx++) {
            if (paramIdx>1)
                out.print("\t");

            out.print(m_fMean[paramIdx] + "\t");
            out.print(m_fStdError[paramIdx] + "\t");
            out.print(m_fStdDev[paramIdx] + "\t");
            out.print(m_fMedian[paramIdx] + "\t");
            out.print(m_f95HPDlow[paramIdx] + "\t");
            out.print(m_f95HPDup[paramIdx] + "\t");
            out.print(m_fACT[paramIdx] + "\t");
            out.print(m_fESS[paramIdx] + "\t");
            out.print(m_fGeometricMean[paramIdx]);
        }

        out.println();
    }

    protected void log(String s) {
        if (!quiet)
            Log.warning.print(s);
    }

    protected void logln(String s) {
        if (!quiet)
        	Log.warning.println(s);
    }

    static void printUsageAndExit() {
    	System.out.println("LogAnalyser [-b <burninPercentage] [file1] ... [filen]");
    	System.out.println("-burnin <burninPercentage>");
    	System.out.println("--burnin <burninPercentage>");
    	System.out.println("-b <burninPercentage> percentage of log file to disregard, default " + BURN_IN_PERCENTAGE);
        System.out.println("-oneline Display only one line of output per file.\n" +
                "         Header is generated from the first file only.\n" +
                "         (Implies quiet mode.)");
        System.out.println("-quiet Quiet mode.  Avoid printing status updates to stderr.");
    	System.out.println("-help");
    	System.out.println("--help");
    	System.out.println("-h print this message");
    	System.out.println("[fileX] log file to analyse. Multiple files are allowed, each is analysed separately");
    	System.exit(0);
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            LogAnalyser analyser;
            	// process args
            	int burninPercentage = BURN_IN_PERCENTAGE;
                boolean oneLine = false;
                boolean quiet = true;
            	List<String> files = new ArrayList<>();
            	int i = 0;
            	while (i < args.length) {
            		String arg = args[i];
                    switch (arg) {
            		case "-b":
            		case "-burnin":
            		case "--burnin":
            			if (i+1 >= args.length) {
            				Log.warning.println("-b argument requires another argument");
            				printUsageAndExit();
            			}
            			burninPercentage = Integer.parseInt(args[i+1]);
            			i += 2;
            			break;

                    case "-oneline":
                        oneLine = true;
                        i += 1;
                        break;

                    case "-quiet":
                        quiet = true;
                        i += 1;
                        break;

            		case "-h":
            		case "-help":
            		case "--help":
            			printUsageAndExit();
            			break;
            		default:
            			if (arg.startsWith("-")) {
            				Log.warning.println("unrecognised command " + arg);
            				printUsageAndExit();
            			}
            			files.add(arg);
            			i++;
            		}
            	}
            	if (files.size() == 0) {
            		// no file specified, open file dialog to select one
	                BEASTVersion version = new BEASTVersion();
	                File file = Utils.getLoadFile("LogAnalyser " + version.getVersionString() + " - Select log file to analyse",
	                        null, "BEAST log (*.log) Files", "log", "txt");
	                if (file == null) {
	                    return;
	                }
	                analyser = new LogAnalyser(file.getAbsolutePath(), burninPercentage, quiet);
	                analyser.print(System.out);
            	} else {
            		// process files
                    if (oneLine) {
                        for (int idx=0; idx<files.size(); idx++) {
                            analyser = new LogAnalyser(files.get(idx), burninPercentage, true);

                            if (idx == 0)
                                analyser.printOneLineHeader(System.out);

                            analyser.printOneLine(System.out);
                        }

                    } else {
                        for (String file : files) {
                            analyser = new LogAnalyser(file, burninPercentage, quiet);
                            analyser.print(System.out);
                        }
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
