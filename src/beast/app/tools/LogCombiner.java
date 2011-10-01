package beast.app.tools;

import jam.console.ConsoleApplication;

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

import javax.swing.JFrame;

import beast.app.beastapp.BeastVersion;
import beast.app.tools.LogCombinerDialog;
import beast.util.LogAnalyser;

/** combines log files produced by a ParticleFilter for 
 * combined analysis**/
public class LogCombiner extends LogAnalyser {

	List<String> m_sLogFileName = new ArrayList<String>();
	String m_sParticleDir;
	int m_nParticles = -1;
	String m_sFileOut;
	PrintStream m_out = System.out;
	int m_nBurninPercentage = 10;

	boolean m_bIsTreeLog = false;
	List<String> m_sTrees;
	// Sample interval as it appears in the combined log file.
	// To use the interval of the log files, use the -renumber option 
	int m_nSampleInterval = 1;
	DecimalFormat format = new DecimalFormat("#.############E0", new DecimalFormatSymbols(Locale.US));
	
	// resample the log files to this frequency (the original sampling frequency must be a factor of this value)
	int m_nResample = 1;
	
	private void parseArgs(String[] args) throws Exception {
		int i = 0;
		format = new DecimalFormat("#.############E0", new DecimalFormatSymbols(Locale.US));
		m_sLogFileName = new ArrayList<String>();
		try {
			while (i < args.length) {
				int iOld = i;
				if (i < args.length) {
					if (args[i].equals("")) {
						i += 1;
					} else if (args[i].equals("-help") || args[i].equals("-h") || args[i].equals("--help")) {
						System.out.println(getUsage());
						System.exit(0);
					} else if (args[i].equals("-o")) {
						m_sFileOut = args[i+1];
		                m_out = new PrintStream(m_sFileOut);
						i += 2;
					} else if (args[i].equals("-b")) {
						m_nBurninPercentage = Integer.parseInt(args[i+1]);
						i += 2;
					} else if (args[i].equals("-n")) {
		                m_nParticles = Integer.parseInt(args[i+1]);
						i += 2;
					} else if (args[i].equals("-log")) {
						m_sLogFileName.add(args[i+1]);
						i += 2;
					} else if (args[i].equals("-dir")) {
		                m_sParticleDir = args[i+1];
						i += 2;
					} else if (args[i].equals("-decimal")) {
						format = new DecimalFormat("#.############", new DecimalFormatSymbols(Locale.US));
						i++;
					} else if (args[i].equals("-resample")) {
		                m_nResample = Integer.parseInt(args[i+1]);
						i += 2;
					} else if (args[i].equals("-renumber")) {
						m_nSampleInterval = -1;
						i++;
					}
					if (i == iOld) {
						throw new Exception("Unrecognised argument");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Error parsing command line arguments: " + Arrays.toString(args) + "\nArguments ignored\n\n" + getUsage());
		}
	}

	
    /** data from log file with burn-in removed **/
    Double [][] m_fCombinedTraces;

	private void combineParticleLogs() throws Exception {
		List<String> sLogs = new ArrayList<String>();
		for (int i = 0; i < m_nParticles; i++) {
			String sDir = m_sParticleDir + "/particle" + i;
			File dir = new File(sDir);
			if (!dir.exists() || !dir.isDirectory()) {
				throw new Exception ("Could not process particle " +i +". Expected " + sDir + " to be a directory, but it is not.");
			}
			sLogs.add(sDir + "/" + m_sLogFileName.get(0));
		}
		int [] nBurnIns = new int[sLogs.size()];
		Arrays.fill(nBurnIns, m_nBurninPercentage);
		combineLogs(sLogs.toArray(new String[0]), nBurnIns);
	}

	private void combineLogs(String [] sLogs, int [] nBurbIns) throws Exception {
		m_fCombinedTraces = null;
		// read logs
		int nColumns = 0;
		int k = 0;
		for (String sFile : sLogs) {
			BufferedReader fin = new BufferedReader(new FileReader(sFile));
			String sStr = fin.readLine();
			if (sStr.toUpperCase().startsWith("#NEXUS")) {
				m_bIsTreeLog = true;
				readTreeLogFile(sFile, nBurbIns[k]);
			} else {
				readLogFile(sFile, nBurbIns[k]);
			}

			if (m_fCombinedTraces == null) {
				m_fCombinedTraces = m_fTraces;
				if (!m_bIsTreeLog) {
					nColumns = m_sLabels.length;
				}
			} else {
				if (nColumns != m_sLabels.length) {
					throw new Exception("ERROR: The number of columns in file " + sFile + " does not match that of the first file");
				}
				for (int i = 0; i < m_fTraces.length; i++) {
					Double [] logLine = m_fTraces[i];
					Double [] oldTrace = m_fCombinedTraces[i];
					Double [] newTrace = new Double[oldTrace.length + logLine.length];
					System.arraycopy(oldTrace, 0, newTrace, 0, oldTrace.length);
					System.arraycopy(logLine, 0, newTrace, oldTrace.length, logLine.length);
					m_fCombinedTraces[i] = newTrace;
				}
			}
			k++;
		}
		if (!m_bIsTreeLog) {
			// reset sample column
			if (m_fCombinedTraces[0].length > 2) {
				if (m_nSampleInterval < 0) {
					// need to renumber
					m_nSampleInterval = (int) (m_fCombinedTraces[0][1] - m_fCombinedTraces[0][0]);
				}
				for (int i = 0; i < m_fCombinedTraces[0].length; i++) {
					m_fCombinedTraces[0][i] = (double) (m_nSampleInterval * i);
				}
			}
		}
	}

	protected void readTreeLogFile(String sFile, int nBurnInPercentage) throws Exception {
		log("\nLoading " + sFile);
		BufferedReader fin = new BufferedReader(new FileReader(sFile));
		String sStr = null;
		m_sPreAmble = "";
		int nData = 0;
		// first, sweep through the log file to determine size of the log
		while (fin.ready()) {
			sStr = fin.readLine();
			if (sStr.matches("^tree STATE.*")) {
				nData++;
			} else {
				if (nData == 0) {
					m_sPreAmble += sStr + "\n";
				}
			}
		}
		int nLines = nData / 80;
		// reserve memory
		int nBurnIn = nData * nBurnInPercentage / 100;
		logln(" skipping " + nBurnIn + " trees\n\n" + BAR);
		if (m_sTrees == null) {
			m_sTrees = new ArrayList<String>();
		}
		fin = new BufferedReader(new FileReader(sFile));
		nData = -nBurnIn - 1;
		// grab data from the log, ignoring burn in samples
		int nSample0 = -1;

		while (fin.ready()) {
			sStr = fin.readLine();
			if (sStr.matches("^tree STATE_.*")) {
				if (++nData >= 0) {
					if (m_nSampleInterval < 0) {
						String sStr2 = sStr.substring(11, sStr.indexOf("="));
						if (nSample0 < 0) {
							nSample0 = Integer.parseInt(sStr2.replaceAll("\\s", ""));
						} else {
							m_nSampleInterval = Integer.parseInt(sStr2.replaceAll("\\s", "")) - nSample0;
						}
						
					}
					m_sTrees.add(sStr.substring(sStr.indexOf("=") + 1));
				}
			}
			if (nData % nLines == 0) {
				log("*");
			}
		}
		logln("");
	} // readTreeLogFile

	private void printCombinedLogs() {
		logln("Collected " + (m_bIsTreeLog ? m_sTrees.size(): m_fCombinedTraces[0].length) + " lines in combined log");
		if (m_sFileOut != null){
			log("Writing to file " + m_sFileOut);
		}
		// preamble
		m_out.println(m_sPreAmble);

		int nLines = 0;
		if (m_bIsTreeLog) {
			for (int i = 0; i < m_sTrees.size(); i++) {
				if ((m_nSampleInterval * i) % m_nResample == 0) {
					m_out.println("tree STATE_" + (m_nSampleInterval * i) + " = " + m_sTrees.get(i));
					nLines++;
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
							m_out.print((int) (double) m_fCombinedTraces[j][i]+"\t");
							break;
						case REAL:
							m_out.print(format.format(m_fCombinedTraces[j][i])+"\t");
							break;
						case NOMINAL:
						case BOOL:
							m_out.print(m_ranges[(int)(double)m_fCombinedTraces[j][i]] + "\t");
							break;
						}
					}
					m_out.print("\n");
					nLines++;
				}
			}
		}
		logln(" " + nLines + " lines in combined log");
	}

	private static String getUsage() {
		return "Usage: LogCombiner -log <file> -n <int> [<options>]\n" +
		"combines multiple (trace or tree) log files into a single log file.\n" +
		"options:\n" +
		"-log <file>      specify the name of the log file, each log file must be specified with separate -log option\n" +
		"-o <output.log>  specify log file to write into (default output is stdout)\n" +
		"-b <burnin>      specify the number PERCANTAGE of lines in the log file considered to be burnin (default 10)\n" +
		"-dir <directory> specify particle directory, if defined only one log must be specified and the -n option\n" +
		"-n <int>         specify the number of particles, ignored if -dir is not defined\n" +
		"-help            print this message\n";
	}

	private void printTitle(String aboutString) {
		aboutString = "LogCombiner" + aboutString.replaceAll("</p>", "\n\n");
		aboutString = aboutString.replaceAll("<br>", "\n");
		aboutString = aboutString.replaceAll("<[^>]*>", " ");
		String [] sStrs = aboutString.split("\n");
		for (String sStr : sStrs) {
	        int n = 80 - sStr.length();
	        int n1 = n / 2;
	        for (int i = 0; i < n1; i++) {
	            log(" ");
	        }
	        logln(sStr);
		}
	}

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
        BeastVersion version = new BeastVersion();
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
//	            java.net.URL url = LogCombiner.class.getResource("/images/utility.png");
	            javax.swing.Icon icon = null;

//	            if (url != null) {
//	                icon = new javax.swing.ImageIcon(url);
//	            }


	            //ConsoleApplication consoleApp = 
	           	new ConsoleApplication(nameString, aboutString, icon, true);

	            combiner.printTitle(aboutString);

	            LogCombinerDialog dialog = new LogCombinerDialog(new JFrame());

	            if (!dialog.showDialog("LogCombiner " + versionString)) {
	                return;
	            }

	            combiner.m_bIsTreeLog = dialog.isTreeFiles();
	            if (dialog.convertToDecimal()) {
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

	            String outputFileName = dialog.getOutputFileName();

	            if (outputFileName == null) {
	                System.err.println("No output file specified");
	            }

	            try {
					combiner.combineLogs(inputFiles, burnins);
					combiner.printCombinedLogs();

	            } catch (Exception ex) {
	                System.err.println("Exception: " + ex.getMessage());
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
					String [] sLogFiles = combiner.m_sLogFileName.toArray(new String[0]); 
					int [] nBurnIns = new int[sLogFiles.length];
					Arrays.fill(nBurnIns, combiner.m_nBurninPercentage);
					combiner.combineLogs(sLogFiles, nBurnIns);

				} else {
					// particle log combiner
					combiner.combineParticleLogs();
				}
				combiner.printCombinedLogs();
	        }
		} catch (Exception e) {
			System.out.println(getUsage());
			e.printStackTrace();
		}
	} // main




}
