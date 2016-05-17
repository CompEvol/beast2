package beast.app.tools;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.table.TableCellEditor;

import beast.app.BEASTVersion2;
import beast.core.util.Log;
import beast.util.LogAnalyser;
import jam.console.ConsoleApplication;


/**
 * combines log files produced by a ParticleFilter for
 * combined analysis*
 */
public class LogCombiner extends LogAnalyser {

    List<String> m_sLogFileName = new ArrayList<>();
    String m_sParticleDir;
    int m_nParticles = -1;
    String m_sFileOut;
    PrintStream m_out = System.out;
    int m_nBurninPercentage = 10;

    private boolean preAmpleIsPrinted = false;
    private int columnCount = -1;

    boolean m_bIsTreeLog = false;
    List<String> m_sTrees;
    // Sample interval as it appears in the combined log file.
    // To use the interval of the log files, use the -renumber option
    int m_nSampleInterval = 1;
    // whether to use decimal or scientific format to print doubles
    boolean m_bUseDecimalFormat = false;
    DecimalFormat format = new DecimalFormat("#.############E0", new DecimalFormatSymbols(Locale.US));

    // resample the log files to this frequency (the original sampling frequency must be a factor of this value)
    int m_nResample = 1;

    private void parseArgs(String[] args) {
        int i = 0;
        format = new DecimalFormat("#.############E0", new DecimalFormatSymbols(Locale.US));
        m_sLogFileName = new ArrayList<>();
        try {
            while (i < args.length) {
                int old = i;
                if (i < args.length) {
                    if (args[i].equals("")) {
                        i += 1;
                    } else if (args[i].equals("-help") || args[i].equals("-h") || args[i].equals("--help")) {
                        Log.info.println(getUsage());
                        System.exit(0);
                    } else if (args[i].equals("-o")) {
                        m_sFileOut = args[i + 1];
                        m_out = new PrintStream(m_sFileOut);
                        i += 2;
                    } else if (args[i].equals("-b") || args[i].equals("-burnin") || args[i].equals("--burnin")) {
                        m_nBurninPercentage = Integer.parseInt(args[i + 1]);
                        if (m_nBurninPercentage < 0 || m_nBurninPercentage > 100) {
                        	Log.err.println("Error: Burn-in percentage must be between 0 and 100.");
                            System.exit(1);
                        }
                        i += 2;
                    } else if (args[i].equals("-n")) {
                        m_nParticles = Integer.parseInt(args[i + 1]);
                        i += 2;
                    } else if (args[i].equals("-log")) {
                        m_sLogFileName.add(args[i + 1]);
                        i += 2;
                        while (i < args.length && !args[i].startsWith("-")) {
                            m_sLogFileName.add(args[i++]);
                        }
                    } else if (args[i].equals("-dir")) {
                        m_sParticleDir = args[i + 1];
                        i += 2;
                    } else if (args[i].equals("-decimal")) {
                        m_bUseDecimalFormat = true;
                        format = new DecimalFormat("#.############", new DecimalFormatSymbols(Locale.US));
                        i++;
                    } else if (args[i].equals("-resample")) {
                        m_nResample = Integer.parseInt(args[i + 1]);
                        i += 2;
                    } else if (args[i].equals("-renumber")) {
                        m_nSampleInterval = -1;
                        i++;
                    }
                    if (i == old) {
                        throw new IllegalArgumentException("Unrecognised argument");
                    }
                }
            }
        } catch (IllegalArgumentException e) {
        	throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Error parsing command line arguments: " + Arrays.toString(args) + "\nArguments ignored\n\n" + getUsage());
        }
    }


    /**
     * data from log file with burn-in removed *
     */
    Double[][] m_fCombinedTraces;

    private void combineParticleLogs() {
        List<String> logs = new ArrayList<>();
        for (int i = 0; i < m_nParticles; i++) {
            String dirName = m_sParticleDir + "/particle" + i;
            File dir = new File(dirName);
            if (!dir.exists() || !dir.isDirectory()) {
                throw new IllegalArgumentException("Could not process particle " + i + ". Expected " + dirName + " to be a directory, but it is not.");
            }
            logs.add(dirName + "/" + m_sLogFileName.get(0));
        }
        int[] burnIns = new int[logs.size()];
        Arrays.fill(burnIns, m_nBurninPercentage);
        try {
			combineLogs(logs.toArray(new String[0]), burnIns);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e.getMessage());
		}
    }

    @SuppressWarnings("unchecked")
	protected long readLogFile(String fileName, int burnInPercentage, long state) throws IOException {
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
        if (!preAmpleIsPrinted) {
        	m_out.print(m_sPreAmble);
            // header
            for (int i = 0; i < m_sLabels.length; i++) {
                m_out.print(m_sLabels[i] + "\t");
            }
            m_out.println();
        	preAmpleIsPrinted = true;
        }
        
        int lines = Math.max(1, data / 80);
        // reserve memory
        int items = m_sLabels.length;
        m_ranges = new List[items];
        int burnIn = data * burnInPercentage / 100;
        m_fTraces = new Double[items][data - burnIn];
        fin.close();
        fin = new BufferedReader(new FileReader(fileName));
        data = -burnIn - 1;
        logln(", burnin " + burnInPercentage + "%, skipping " + burnIn + " log lines\n\n" + BAR);
        // grab data from the log, ignoring burn in samples
        long prevLogState = -1;
        while (fin.ready()) {
            str = fin.readLine();
            if (str.indexOf('#') < 0 && str.matches("[0-9].*")) {
                data++;
                if (data >= 0) {
                	String [] strs = str.split("\\s");
                	long logState = Long.parseLong(strs[0]);
                    if (m_nSampleInterval < 0 && prevLogState >= 0) {
                        // need to renumber
                        m_nSampleInterval = (int) (logState - prevLogState);
                    }
                    prevLogState = logState;
                    if (columnCount != strs.length) {
                    	if (columnCount < 0) {
                    		columnCount = strs.length;
                    	} else {
                            fin.close();
                            throw new IllegalArgumentException("ERROR: The number of columns in file " + fileName + " does not match that of the first file");
                    	}
                    }
                	
                	if (logState % m_nSampleInterval == 0 || m_nSampleInterval < 0) {
	                	if (state < 0) {
	                		state = 0;
	                	} else {
	                		state += m_nSampleInterval;
	                	}
	                	m_out.print(state + "\t");
	                	for (int k = 1; k < strs.length; k++) {
		                	if (m_bUseDecimalFormat && strs[k].indexOf('.') > 0) {
		                		double d = Double.parseDouble(strs[k]);
		                		m_out.print(format.format(d));
		                	} else {
		                		m_out.print(strs[k]);
		                	}
	                		m_out.print('\t');
	                	}
	                	m_out.println();
                	}
                }
        	}
            if (data % lines == 0) {
                log("*");
            }
        }
        logln("");
        fin.close();
        return state;
    } // readLogFile


    private void combineLogs(String[] logs, int[] burbIns) throws IOException {
    	preAmpleIsPrinted = false;
        log("Writing to file " + m_sFileOut);
        try {
            m_out = new PrintStream(new File(m_sFileOut));
        } catch (FileNotFoundException e) {
            log("Could not open file " + m_sFileOut + " for writing: " + e.getMessage());
            return;
        }

        m_fCombinedTraces = null;
        // process logs
        int k = 0;
        long state = -1;
        for (String fileName : logs) {
            BufferedReader fin = new BufferedReader(new FileReader(fileName));
            String str = fin.readLine();
            if (str.toUpperCase().startsWith("#NEXUS")) {
                m_bIsTreeLog = true;
                state = readTreeLogFile(fileName, burbIns[k], state);
            } else {
                state = readLogFile(fileName, burbIns[k], state);
            }
            k++;
            fin.close();
        }
        m_out.close();
        log("Wrote " + (state/m_nSampleInterval + 1) + " lines to " + m_sFileOut);
    }

    protected long readTreeLogFile(String fileName, int burnInPercentage, long state) throws IOException {
        log("\nLoading " + fileName);
        BufferedReader fin = new BufferedReader(new FileReader(fileName));
        String str = null;
        m_sPreAmble = "";
        int data = 0;
        // first, sweep through the log file to determine size of the log
        while (fin.ready()) {
            str = fin.readLine();
            if (str.matches("^tree STATE.*")) {
                data++;
            } else {
                if (data == 0) {
                    m_sPreAmble += str + "\n";
                }
            }
        }
        if (!preAmpleIsPrinted) {
        	m_out.println(m_sPreAmble);
        	preAmpleIsPrinted = true;
        }
        int lines = data / 80;
        // reserve memory
        int burnIn = data * burnInPercentage / 100;
        logln(" skipping " + burnIn + " trees\n\n" + BAR);
        if (m_sTrees == null) {
            m_sTrees = new ArrayList<>();
        }
        fin.close();
        fin = new BufferedReader(new FileReader(fileName));
        data = -burnIn - 1;

        // grab data from the log, ignoring burn in samples
        long prevLogState = -1;
        while (fin.ready()) {
            str = fin.readLine();
            if (str.matches("^tree STATE_.*")) {
                if (++data >= 0) {
                    String str2 = str.substring(11, str.indexOf("=")).trim();
                    str2 = str2.split("\\s")[0];
                    long logState = Long.parseLong(str2);
                    if (m_nSampleInterval < 0 && prevLogState >= 0) {
                        // need to renumber
                        m_nSampleInterval = (int) (logState - prevLogState);
                    }
                    prevLogState = logState;
                	
                	if (logState % m_nSampleInterval == 0 || m_nSampleInterval < 0) {
	                	if (state < 0) {
	                		state = 0;
	                	} else {
	                		state += m_nSampleInterval;
	                	}
	                    str = str.replaceAll("^tree STATE_[^\\s=]*", "");
	                	m_out.print("tree STATE_" + state + " =" + str);
	                	m_out.println();
                	}
                }
            }
            if (data % lines == 0) {
                log("*");
            }
        }
        logln("");
        return state;
    } // readTreeLogFile

    private void printCombinedLogs() {
        int data = (m_bIsTreeLog ? m_sTrees.size() : m_fCombinedTraces[0].length);
        logln("Collected " + data + " lines in combined log");
        if (m_sFileOut != null) {
            log("Writing to file " + m_sFileOut);
            try {
                m_out = new PrintStream(new File(m_sFileOut));
            } catch (FileNotFoundException e) {
                log("Could not open file " + m_sFileOut + " for writing: " + e.getMessage());
                return;
            }
        }
        logln("\n\n" + BAR);
        // preamble
        m_out.println(m_sPreAmble);

        int lines = 0;
        if (m_bIsTreeLog) {
            for (int i = 0; i < m_sTrees.size(); i++) {
                if ((m_nSampleInterval * i) % m_nResample == 0) {
                    String tree = m_sTrees.get(i);
                    tree = format(tree);
                    m_out.println("tree STATE_" + (m_nSampleInterval * i) + (Character.isSpaceChar(tree.charAt(0)) ? "" : " ") + tree);
                    lines++;
                }
                if (i % (data / 80) == 0) {
                    log("*");
                }
            }
            m_out.println("End;");
        } else {
            // header
            for (int i = 0; i < m_sLabels.length; i++) {
                m_out.print(m_sLabels[i] + "\t");
            }
            m_out.println();
            for (int i = 0; i < m_fCombinedTraces[0].length; i++) {
                if (((int) (double) m_fCombinedTraces[0][i]) % m_nResample == 0) {
                    for (int j = 0; j < m_types.length; j++) {
                        switch (m_types[j]) {
                            case INTEGER:
                                m_out.print((int) (double) m_fCombinedTraces[j][i] + "\t");
                                break;
                            case REAL:
                                m_out.print(format.format(m_fCombinedTraces[j][i]) + "\t");
                                break;
                            case NOMINAL:
                            case BOOL:
                                m_out.print(m_ranges[(int) (double) m_fCombinedTraces[j][i]] + "\t");
                                break;
                        }
                    }
                    m_out.print("\n");
                    lines++;
                }
                if ((data / 80 > 0) && i % (data / 80) == 0) {
                    log("*");
                }
            }
        }
        logln("\n" + lines + " lines in combined log");
    }

    protected String format(String tree) {
        if (m_bUseDecimalFormat) {
            // convert scientific to decimal format
            if (tree.matches(".*[0-9]+\\.[0-9]+[0-9-]+E[0-9-]+.*")) {
                int k = 0;
                while (k < tree.length()) {
                    char c = tree.charAt(k);
                    if (Character.isDigit(c)) {
                        int start = k;
                        while (++k < tree.length() && Character.isDigit(tree.charAt(k))) {
                        }
                        if (k < tree.length() && tree.charAt(k) == '.') {
                            while (++k < tree.length() && Character.isDigit(tree.charAt(k))) {
                            }
                            if (k < tree.length() && (tree.charAt(k) == 'E' || tree.charAt(k) == 'e')) {
                                k++;
                                if (k < tree.length() && tree.charAt(k) == '-') {
                                    k++;
                                }
                                if (k < tree.length() && Character.isDigit(tree.charAt(k))) {
                                    while (++k < tree.length() && Character.isDigit(tree.charAt(k))) {
                                    }
                                    int end = k;
                                    String number = tree.substring(start, end);
                                    double d = Double.parseDouble(number);
                                    number = format.format(d);
                                    tree = tree.substring(0, start) + number + tree.substring(end);
                                    k = start + number.length();
                                }
                            }
                        }
                    } else {
                        k++;
                    }
                }
            }
        } else {
            // convert decimal to scientific format
            if (tree.matches(".*[0-9]+\\.[0-9]+[^E-].*")) {
                int k = 0;
                while (k < tree.length()) {
                    char c = tree.charAt(k);
                    if (Character.isDigit(c)) {
                        int start = k;
                        while (++k < tree.length() && Character.isDigit(tree.charAt(k))) {
                        }
                        if (k < tree.length() && tree.charAt(k) == '.') {
                            while (++k < tree.length() && Character.isDigit(tree.charAt(k))) {
                            }
                            if (k < tree.length() && tree.charAt(k) != '-' && tree.charAt(k) != 'E' && tree.charAt(k) != 'e') {
                                int end = k;
                                String number = tree.substring(start, end);
                                double d = Double.parseDouble(number);
                                number = format.format(d);
                                tree = tree.substring(0, start) + number + tree.substring(end);
                                k = start + number.length();
                            }
                        }
                    } else {
                        k++;
                    }
                }
            }
        }
        return tree;
    }

    private static String getUsage() {
        return "Usage: LogCombiner -log <file> -n <int> [<options>]\n" +
                "combines multiple (trace or tree) log files into a single log file.\n" +
                "options:\n" +
                "-log <file>      specify the name of the log file, each log file must be specified with separate -log option\n" +
                "-o <output.log>  specify log file to write into (default output is stdout)\n" +
                "-b <burnin>      specify the number PERCENTAGE of lines in the log file considered to be burnin (default 10)\n" +
                "-dir <directory> specify particle directory -- used for particle filtering in BEASTLabs only -- if defined only one log must be specified and the -n option specified\n" +
                "-n <int>         specify the number of particles, ignored if -dir is not defined\n" +
                "-resample <int>  specify number of states to resample\n" +
                "-decimal         flag to indicate numbers should converted from scientific into decimal format\n" +
                "-renumber        flag to indicate output states should be renumbered\n" +
                "-help            print this message\n";
    }

    private void printTitle(String aboutString) {
        aboutString = "LogCombiner" + aboutString.replaceAll("</p>", "\n\n");
        aboutString = aboutString.replaceAll("<br>", "\n");
        aboutString = aboutString.replaceAll("<[^>]*>", " ");
        String[] strs = aboutString.split("\n");
        for (String str : strs) {
            int n = 80 - str.length();
            int n1 = n / 2;
            for (int i = 0; i < n1; i++) {
                log(" ");
            }
            logln(str);
        }
    }


    /**
     * @param args
     */
    public static void main(String[] args) {
        BEASTVersion2 version = new BEASTVersion2();
        final String versionString = version.getVersionString();
        String nameString = "LogCombiner " + versionString;
        String aboutString = "<html><center><p>" + versionString + ", " + version.getDateString() + "</p>" +
                "<p>by<br>" +
                "<p>Andrew Rambaut and Alexei J. Drummond</p>" +
                "<p>Institute of Evolutionary Biology, University of Edinburgh<br>" +
                "<a href=\"mailto:a.rambaut@ed.ac.uk\">a.rambaut@ed.ac.uk</a></p>" +
                "<p>Department of Computer Science, University of Auckland<br>" +
                "<a href=\"mailto:alexei@cs.auckland.ac.nz\">alexei@cs.auckland.ac.nz</a></p>" +
                "<p>Part of the BEAST 2 package:<br>" +
                "<a href=\"http://beast2.cs.auckland.ac.nz/\">http://beast2.cs.auckland.ac.nz/</a></p>" +
                "</center></html>";


        LogCombiner combiner = new LogCombiner();
        try {
            if (args.length == 0) {
                System.setProperty("com.apple.macos.useScreenMenuBar", "true");
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("apple.awt.showGrowBox", "true");

                // TODO: set up ICON
                java.net.URL url = LogCombiner.class.getResource("images/logcombiner.png");
                javax.swing.Icon icon = null;

                if (url != null) {
                    icon = new javax.swing.ImageIcon(url);
                }

                String titleString = "<html><center><p>LogCombiner<br>" +
                        "Version " + version.getVersionString() + ", " + version.getDateString() + "</p></center></html>";

                //ConsoleApplication consoleApp =
                new ConsoleApplication(nameString, aboutString, icon, true);

                combiner.printTitle(aboutString);

                LogCombinerDialog dialog = new LogCombinerDialog(new JFrame(), titleString, icon);

                if (!dialog.showDialog(nameString)) {
                    return;
                }

                // issue #437: ensure the table editor finished.
                // this way, the latest entered burn-in is captured, otherwise 
                // the last editing action may be ignored
                TableCellEditor editor = dialog.filesTable.getCellEditor();
                if (editor != null) {
                    editor.stopCellEditing();
                }

                combiner.m_bIsTreeLog = dialog.isTreeFiles();
                combiner.m_bUseDecimalFormat = dialog.convertToDecimal();
                if (combiner.m_bUseDecimalFormat) {
                    combiner.format = new DecimalFormat("#.############", new DecimalFormatSymbols(Locale.US));
                }
                if (!dialog.renumberOutputStates()) {
                    combiner.m_nSampleInterval = -1;
                }
                if (dialog.isResampling()) {
                    combiner.m_nResample = dialog.getResampleFrequency();
                }

                String[] inputFiles = dialog.getFileNames();
                int[] burnins = dialog.getBurnins();

                combiner.m_sFileOut = dialog.getOutputFileName();

                if (combiner.m_sFileOut == null) {
                	Log.warning.println("No output file specified");
                }

                try {
                    combiner.combineLogs(inputFiles, burnins);
 
                } catch (Exception ex) {
                	Log.warning.println("Exception: " + ex.getMessage());
                    ex.printStackTrace();
                }
                System.out.println("Finished - Quit program to exit.");
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                combiner.printTitle(aboutString);

                combiner.parseArgs(args);
                if (combiner.m_sParticleDir == null) {
                    // classical log combiner
                    String[] logFiles = combiner.m_sLogFileName.toArray(new String[0]);
                    int[] burnIns = new int[logFiles.length];
                    Arrays.fill(burnIns, combiner.m_nBurninPercentage);
                    combiner.combineLogs(logFiles, burnIns);

                } else {
                    // particle log combiner
                    combiner.combineParticleLogs();
                    combiner.printCombinedLogs();
                }
            }
        } catch (Exception e) {
            System.out.println(getUsage());
            e.printStackTrace();
        }
    } // main


}
