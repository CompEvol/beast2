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

import beast.app.BEASTVersion2;
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
     * @param burnInPercentage  burnInPercentage typical = 10; percentage of data that can be ignored
     * @throws IOException 
     */
    public LogAnalyser(String[] args, int burnInPercentage) throws IOException {
    	this(args, burnInPercentage, false, true);
    }

    public LogAnalyser(String[] args, int burnInPercentage, boolean quiet, boolean calcStats) throws IOException {
        fileName = args[args.length - 1];
        readLogFile(fileName, burnInPercentage);
        this.quiet = quiet;
        if (calcStats) {
        	calcStats();
        }
    }

    public LogAnalyser(String[] args) throws IOException {
        this(args, BURN_IN_PERCENTAGE, false, true);
    }

    public LogAnalyser(String fileName, int burnInPercentage) throws IOException {
    	this(fileName, burnInPercentage, false, true);
    }

    public LogAnalyser(String fileName, int burnInPercentage, boolean quiet) throws IOException {
    	this(fileName, burnInPercentage, quiet, true);
    }
    
    public LogAnalyser(String fileName) throws IOException {
        this(fileName, BURN_IN_PERCENTAGE);
    }

    public LogAnalyser(String fileName, int burnInPercentage, boolean quiet, boolean calcStats) throws IOException {
        this.fileName = fileName;
        this.quiet = quiet;
        readLogFile(fileName, burnInPercentage);
        if (calcStats) {
        	calcStats();
        }
    }

    @SuppressWarnings("unchecked")
	protected void readLogFile(String fileName, int burnInPercentage) throws IOException {
        log("\nLoading " + fileName);
        BufferedReader fin = new BufferedReader(new FileReader(fileName));
        String str;
        m_sPreAmble = "";
        m_sLabels = null;
        int data = 0;
        // first, sweep through the log file to determine size of the log
        while (fin.ready()) {
            str = fin.readLine();
            if (str.indexOf('#') < 0 && str.matches(".*[0-9a-zA-Z].*")) {
                if (m_sLabels == null)
                    m_sLabels = str.split("\\s");
                else
                    data++;
            } else {
                m_sPreAmble += str + "\n";
            }
        }
        int lines = Math.max(1, data / 80);
        // reserve memory
        int items = m_sLabels.length;
        m_ranges = new List[items];
        int burnIn = data * burnInPercentage / 100;
        int total = data - burnIn;
        m_fTraces = new Double[items][data - burnIn];
        fin.close();
        fin = new BufferedReader(new FileReader(fileName));
        data = -burnIn - 1;
        logln(", burnin " + burnInPercentage + "%, skipping " + burnIn + " log lines\n\n" + BAR);
        // grab data from the log, ignoring burn in samples
        m_types = new type[items];
        Arrays.fill(m_types, type.INTEGER);
        int reported = 0; 
        while (fin.ready()) {
            str = fin.readLine();
            int i = 0;
            if (str.indexOf('#') < 0 && str.matches("[-0-9].*")) // {
                //data++;
                if (++data >= 0) //{
                    for (String str2 : str.split("\\s")) {
                        try {
                            if (str2.indexOf('.') >= 0) {
                                m_types[i] = type.REAL;
                            }
                            m_fTraces[i][data] = Double.parseDouble(str2);
                        } catch (Exception e) {
                            if (m_ranges[i] == null) {
                                m_ranges[i] = new ArrayList<>();
                            }
                            if (!m_ranges[i].contains(str2)) {
                                m_ranges[i].add(str2);
                            }
                            m_fTraces[i][data] = 1.0 * m_ranges[i].indexOf(str2);
                        }
                        i++;
                    }
            //}
            //}
            if (data > 0 && data % lines == 0 && reported < 81) {
				while (10000 * reported < 810000 * (data + 1)/ total) {
	                log("*");
	                reported++;
        	    }
            }
        }
        logln("");
        // determine types
        for (int i = 0; i < items; i++)
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
    public void calcStats() {
        logln("\nCalculating statistics\n\n" + BAR);
        int stars = 0;
        int items = m_sLabels.length;
        m_fMean = new Double[items];
        m_fStdError = new Double[items];
        m_fStdDev = new Double[items];
        m_fMedian = new Double[items];
        m_f95HPDlow = new Double[items];
        m_f95HPDup = new Double[items];
        m_fESS = new Double[items];
        m_fACT = new Double[items];
        m_fGeometricMean = new Double[items];
        int sampleInterval = (int) (m_fTraces[0][1] - m_fTraces[0][0]);
        for (int i = 1; i < items; i++) {
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
                m_fACT[i] = ESS.ACT(m_fTraces[i], sampleInterval);
                m_fStdError[i] = ESS.stdErrorOfMean(trace, sampleInterval);
                m_fESS[i] = trace.length / (m_fACT[i] / sampleInterval);

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
            while (stars < 80 * (i + 1) / items) {
                log("*");
                stars++;
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

    public void setData(Double[] trace, int sampleStep) {
        Double[][] traces = new Double[2][];
        traces[0] = new Double[trace.length];
        for (int i = 0; i < trace.length; i++) {
            traces[0][i] = (double) i * sampleStep;
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

    public double getMean(int column) {
        return m_fMean[column];
    }

    public double getStdDev(int column) {
        return m_fStdDev[column];
    }

    public double getStdError(int column) {
        return m_fStdError[column];
    }

    public double getMedian(int column) {
        return m_fMedian[column];
    }

    public double get95HPDup(int column) {
        return m_f95HPDup[column];
    }

    public double get95HPDlow(int column) {
        return m_f95HPDlow[column];
    }

    public double getESS(int column) {
        return m_fESS[column];
    }

    public double getACT(int column) {
        return m_fACT[column];
    }

    public double getGeometricMean(int column) {
        return m_fGeometricMean[column];
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

    public double getACT(Double[] trace, int sampleStep) {
        setData(trace, sampleStep);
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
        int max = 0;
        for (int i = 1; i < m_sLabels.length; i++)
            max = Math.max(m_sLabels[i].length(), max);
        String space = "";
        for (int i = 0; i < max; i++)
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

        out.print("sample\tfilename\t");
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
                boolean quiet = false;
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
	                BEASTVersion2 version = new BEASTVersion2();
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

                            System.out.print(idx + "\t" + files.get(idx) + "\t");
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
