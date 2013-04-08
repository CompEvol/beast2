/*
* File Logger.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is part of BEAST2.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BEAST is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BEAST is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BEAST; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/
package beast.core;


import beast.core.Input.Validate;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;
import beast.util.XMLProducer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Description("Logs results of a calculation processes on regular intervals.")
public class Logger extends Plugin {
    /**
     * currently supported modes *
     */
    public enum LOGMODE {
        autodetect, compound, tree
    }

    public enum SORTMODE {
        none, alphabetic, smart
    }

    public Input<String> m_pFileName = new Input<String>("fileName", "Name of the file, or stdout if left blank");

    public Input<Integer> m_pEvery = new Input<Integer>("logEvery", "Number of the samples logged", 1);
    public Input<Plugin> m_pModelPlugin = new Input<Plugin>("model", "Model to log at the top of the log. " +
            "If specified, XML will be produced for the model, commented out by # at the start of a line. " +
            "Alignments are suppressed. This way, the log file documents itself. ");
    public Input<LOGMODE> m_sMode = new Input<LOGMODE>("mode", "logging mode, one of " + LOGMODE.values(), LOGMODE.autodetect, LOGMODE.values());
    public Input<SORTMODE> sortMode = new Input<SORTMODE>("sort", "sort items to be logged, one of " + SORTMODE.values(), SORTMODE.none, SORTMODE.values());
    public Input<Boolean> sanitiseHeaders = new Input<Boolean>("sanitiseHeaders", "whether to remove any clutter introduced by Beauti" , false);

    public Input<List<Plugin>> m_pLoggers = new Input<List<Plugin>>("log",
            "Element in a log. This can be any plug in that is Loggable.",
            new ArrayList<Plugin>(), Validate.REQUIRED, Loggable.class);

    /**
     * list of loggers, if any
     */
    List<Loggable> m_loggers;
    public final static int FILE_ONLY_NEW = 0, FILE_OVERWRITE = 1, FILE_APPEND = 2, FILE_ONLY_NEW_OR_EXIT = 3;
    public static int FILE_MODE = FILE_ONLY_NEW;
    /**
     * Compound loggers get a sample number printed at the beginning of the line,
     * while tree loggers don't.
     */
    public final static int COMPOUND_LOGGER = 0, TREE_LOGGER = 2;
    public int m_mode = COMPOUND_LOGGER;
    /**
     * offset for the sample number, which is non-zero when a chain is resumed *
     */
    static int m_nSampleOffset = 0;

    /**
     * number of samples between logs *
     */
    int m_nEvery = 1;

    /**
     * stream to log to
     */
    PrintStream m_out;

    /**
     * keep track of time taken between logs to estimate speed *
     */
    long m_nStartLogTime = -5;
    int m_nStartSample;


    @Override
    public void initAndValidate() throws Exception {
        List<Plugin> loggers = m_pLoggers.get();
        final int nLoggers = loggers.size();
        if (nLoggers == 0) {
            throw new Exception("Logger with nothing to log specified");
        }

        m_loggers = new ArrayList<Loggable>();
        for (int k = 0; k < nLoggers; ++k) {
            m_loggers.add((Loggable) loggers.get(k));
        }

        // determine logging mode
        LOGMODE sMode = m_sMode.get();
        if (sMode.equals(LOGMODE.autodetect)) {
            m_mode = COMPOUND_LOGGER;
            if (nLoggers == 1 && m_loggers.get(0) instanceof Tree) {
                m_mode = TREE_LOGGER;
            }
        } else if (sMode.equals(LOGMODE.tree)) {
            m_mode = TREE_LOGGER;
        } else if (sMode.equals(LOGMODE.compound)) {
            m_mode = COMPOUND_LOGGER;
        } else {
            throw new Exception("Mode '" + sMode + "' is not supported. Choose one of " + LOGMODE.values());
        }

        if (m_pEvery.get() != null) {
            m_nEvery = m_pEvery.get();
        }
        
        if (m_mode == COMPOUND_LOGGER) {
        	switch (sortMode.get()) {
        	case none:
        		// nothing to do
       			break;
        	case alphabetic:
        		// sort loggers by id
        		Collections.sort(m_loggers, new Comparator<Loggable>() {
					@Override
					public int compare(Loggable o1, Loggable o2) {
						String id1 = ((Plugin)o1).getID();
						String id2 = ((Plugin)o1).getID();
						if (id1 == null || id2 == null) {return 0;}
						return id1.compareTo(id2);
					}
				});
    			break;
        	case smart:
        		// Group loggers with same id-prefix, where the prefix of an id is
        		// defined as the part of an id before the first full stop.
        		// This way, multi-partition analysis generated by BEAUti get all  
        		// related log items together in Tracer
        		List<String> ids = new ArrayList<String>();
        		for (int i = 0; i < m_loggers.size(); i++) {
        			String id = ((Plugin)m_loggers.get(i)).getID();
        			if (id == null) {
        				id = "";
        			}
        			if (id.indexOf('.') > 0) {
        				id = id.substring(0, id.indexOf('.'));
        			}
        			ids.add(id);
        		}
        		for (int i = 0; i < m_loggers.size(); i++) {
        			int k = 1;
        			String id = ids.get(i);
        			for (int j = i + 1; j < m_loggers.size(); j++) {
        				if (ids.get(j).equals(id)) {
        					ids.remove(j);
        					ids.add(i + k, id);
        					Loggable l = m_loggers.remove(j);
        					m_loggers.add(i + k, l);
        					k++;
        				}
        			}
        		}
    			break;
        	}
        }
    } // initAndValidate


    /**
     * initialise log, open file (if necessary) and produce header of log
     */
    public void init() throws Exception {
        boolean bNeedsHeader = openLogFile();
        if (bNeedsHeader) {
            if (m_pModelPlugin.get() != null) {
                // print model at top of log
                String sXML = new XMLProducer().modelToXML(m_pModelPlugin.get());
                sXML = "#" + sXML.replaceAll("\\n", "\n#");
                m_out.println("#\n#model:\n#");
                m_out.println(sXML);
                m_out.println("#");
            }
            ByteArrayOutputStream baos = null;
            PrintStream tmp = null;
            if (m_out == System.out) {
                tmp = m_out;
                baos = new ByteArrayOutputStream();
                m_out = new PrintStream(baos);
            }
            ByteArrayOutputStream rawbaos = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(rawbaos);
            if (m_mode == COMPOUND_LOGGER) {
                out.print("Sample\t");
            }
            for (Loggable m_logger : m_loggers) {
                m_logger.init(out);
            }
            if (sanitiseHeaders.get()) {
            	m_out.print(sanitiseHeader(rawbaos.toString()));
            } else {
            	m_out.print(rawbaos.toString());
            }
            
            if ( baos != null ) {
                assert tmp == System.out;
                m_out = tmp;
                try {
                    String logContent = baos.toString("ASCII");
                    logContent = prettifyLogLine(logContent);
                    m_out.print(logContent);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            m_out.println();
        }
    } // init

    /** remove indicators of partion context from header of a log file **/
    private String sanitiseHeader(String header) {
    	// collect partitions
    	String partitionPrefix = null, clockPrefix = null, sitePrefix = null, treePrefix = null;
    	for (int i = 0; i < header.length(); i++) {
    		char c = header.charAt(i);
    		if (c == '.') {
    			if (header.charAt(i+2) == ':') {
    				char c2 = header.charAt(++i);
    				i++;
    				String prefix = "";
    				while (i < header.length() - 1 && c != '\t') {
        				c = header.charAt(++i);
        				if (c != '\t') {
        					prefix += c;
        				}
    				}
    				switch (c2) {
    				case 'c':
    					clockPrefix = getprefix(clockPrefix, prefix);
    					break;
    				case 's':
    					sitePrefix = getprefix(sitePrefix, prefix);
    					break;
    				case 't':
    					treePrefix = getprefix(treePrefix, prefix);
    					break;
    				}
    			} else {
    				String prefix = "";
    				while (i < header.length() - 1 && c != '\t') {
        				c = header.charAt(++i);
        				if (c != '\t') {
        					prefix += c;
        				}
    				}
					partitionPrefix = getprefix(partitionPrefix, prefix);
    			}
    		}
    	}

    	// remove clock/site/tree info
    	header = header.replaceAll("\\." + partitionPrefix, ".");
    	header = header.replaceAll("\\.c:" + clockPrefix, ".");
    	header = header.replaceAll("\\.t:" + treePrefix, ".");
    	header = header.replaceAll("\\.s:" + sitePrefix, ".");
    	// remove trailing dots on labels
    	header = header.replaceAll("\\.\\.", ".");
    	header = header.replaceAll("\\.\t", "\t");
		return header;
	}

    /** return longest common prefex of two strings, except when the first
     * on is null, then it returns the second string.
     */
	private String getprefix(String str1, String str2) {
		if (str1 == null) {
			return str2;
		} else {
			String prefix = "";
			int i = 0;
			while (i < str1.length() && i < str2.length() && 
					str1.charAt(i) == str2.charAt(i)) {
				prefix += str1.charAt(i++);
			}
			return prefix;
		}
	}


	boolean openLogFile() throws Exception {
        String sFileName = m_pFileName.get();
        if (sFileName == null || sFileName.length() == 0) {
            m_out = System.out;
            return true;
        } else {
            if (sFileName.contains("$(tree)")) {
            	String treeName = "tree";
            	for (Loggable logger : m_loggers) {
            		if (logger instanceof Plugin) {
            			String id = ((Plugin) logger).getID();
            			if (id.indexOf(".t:") > 0) {
            				treeName = id.substring(id.indexOf(".t:") + 3); 
            			}
            		}
            	}
                sFileName = sFileName.replace("$(tree)", treeName);
                m_pFileName.setValue(sFileName, this);
            }
            if (sFileName.contains("$(seed)")) {
                sFileName = sFileName.replace("$(seed)", Randomizer.getSeed() + "");
                m_pFileName.setValue(sFileName, this);
            }
            if (System.getProperty("file.name.prefix") != null) {
                sFileName = System.getProperty("file.name.prefix") + "/" + sFileName;
            }
            switch (FILE_MODE) {
                case FILE_ONLY_NEW:// only open file if the file does not already exists
                case FILE_ONLY_NEW_OR_EXIT: {
                    File file = new File(sFileName);
                    if (file.exists()) {
                        if (FILE_MODE == FILE_ONLY_NEW_OR_EXIT) {
                            System.out.println("Trying to write file " + sFileName + " but the file already exists. Exiting now.");
                            System.exit(0);
                        }
                        // Check with user what to do next
                        System.out.println("Trying to write file " + sFileName + " but the file already exists (perhaps use the -overwrite flag?).");
                        System.out.println("Overwrite (Y/N)?:");
                        System.out.flush();
                        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                        String sMsg = stdin.readLine();
                        if (!sMsg.toLowerCase().equals("y")) {
                            System.out.println("Exiting now.");
                            System.exit(0);
                        }
                    }
                    m_out = new PrintStream(sFileName);
                    System.out.println("Writing file " + sFileName);
                    return true;
                }
                case FILE_OVERWRITE:// (over)write log file
                {
                    String sMsg = "Writing";
                    if (new File(sFileName).exists()) {
                        sMsg = "Warning: Overwriting";
                    }
                    m_out = new PrintStream(sFileName);
                    System.out.println(sMsg + " file " + sFileName);
                    return true;
                }
                case FILE_APPEND:// append log file, pick up SampleOffset by reading existing log
                {
                    File file = new File(sFileName);
                    if (file.exists()) {
                        if (m_mode == COMPOUND_LOGGER) {
                            // first find the sample nr offset
                            BufferedReader fin = new BufferedReader(new FileReader(sFileName));
                            String sStr = null;
                            while (fin.ready()) {
                                sStr = fin.readLine();
                            }
                            fin.close();
                            int nSampleOffset = Integer.parseInt(sStr.split("\\s")[0]);
                            if (m_nSampleOffset > 0 && nSampleOffset != m_nSampleOffset) {
                                throw new Exception("Error 400: Cannot resume: log files do not end in same sample number");
                            }
                            m_nSampleOffset = nSampleOffset;
                            // open the file for appending
                            FileOutputStream out2 = new FileOutputStream(sFileName, true);
                            m_out = new PrintStream(out2);
                        } else {
                            // it is a tree logger, we may need to get rid of the last line!
                            final BufferedReader fin = new BufferedReader(new FileReader(sFileName));
                            final StringBuilder buf = new StringBuilder();
                            String sStrLast = null;
                            //String sStr = fin.readLine();
                            boolean endSeen = false;
                            while (fin.ready()) {
                                if( endSeen ) {
                                    buf.append("End;\n");
                                    endSeen = false;
                                }
                                final String sStr = fin.readLine();
                                if (!sStr.equals("End;")) {
                                    buf.append(sStr);
                                    buf.append('\n');
                                    sStrLast = sStr;
                                } else {
                                    endSeen = true;
                                }
                            }
                            fin.close();
                            // back up file in case something goes wrong (e.g. an out of memory error occurs)
                            File treeFileBackup = new File(sFileName);
                            treeFileBackup.renameTo(new File(sFileName + ".bu"));
                            // determine number of the last sample
                            if( sStrLast == null ) {
                                // empty log file?
                                 throw new Exception("Error 402: empty tree log file " + sFileName + "? (check if there is a back up file " + sFileName + ".bu)");
                            }
                            final String sStr = sStrLast.split("\\s+")[1];
                            final int nSampleOffset = Integer.parseInt(sStr.substring(6));
                            if (m_nSampleOffset > 0 && nSampleOffset != m_nSampleOffset) {
                                treeFileBackup.renameTo(new File(sFileName));
                                throw new Exception("Error 401: Cannot resume: log files do not end in same sample number");
                            }
                            m_nSampleOffset = nSampleOffset;
                            // open the file and write back all but the last line
                            FileOutputStream out2 = new FileOutputStream(sFileName);
                            m_out = new PrintStream(out2);
                            m_out.print(buf.toString());
                            // it is safe to remove the backup file now
                            new File(sFileName + ".bu").delete();
                        }
                        System.out.println("Appending file " + sFileName);
                        return false;
                    } else {
                        m_out = new PrintStream(sFileName);
                        System.out.println("Writing file " + sFileName);
                        return true;
                    }
                }
                default:
                    throw new Exception("DEVELOPER ERROR: unknown file mode for logger " + FILE_MODE);
            }
        }
    } // openLogFile

    /**
     * log the state for given sample nr
     * *
     * * @param nSample
     */
    public void log(int nSample) {
        if ((nSample < 0) || (nSample % m_nEvery > 0)) {
            return;
        }
        if (m_nSampleOffset > 0) {
            if (nSample == 0) {
                // don't need to duplicate the last line in the log
                return;
            }
            nSample += m_nSampleOffset;
        }
        ByteArrayOutputStream baos = null;
        PrintStream tmp = null;
        if (m_out == System.out) {
            tmp = m_out;
            baos = new ByteArrayOutputStream();
            m_out = new PrintStream(baos);
        }
        if (m_mode == COMPOUND_LOGGER) {
            m_out.print((nSample) + "\t");
        }
        for (Loggable m_logger : m_loggers) {
            m_logger.log(nSample, m_out);
        }
        if ( baos != null ) {
            assert tmp == System.out ;

            m_out = tmp;
            try {
                String logContent = baos.toString("ASCII");
                logContent = prettifyLogLine(logContent);
                m_out.print(logContent);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (m_nStartLogTime < 0) {
                if (nSample - m_nSampleOffset > 6000) {
                    m_nStartLogTime++;
                    if (m_nStartLogTime == 0) {
                        m_nStartLogTime = System.currentTimeMillis();
                        m_nStartSample = nSample;
                    }
                }
                m_out.print(" --");
            } else {

                long nLogTime = System.currentTimeMillis();
                int nSecondsPerMSamples = (int) ((nLogTime - m_nStartLogTime) * 1000.0 / (nSample - m_nStartSample + 1.0));
                String sTimePerMSamples =
                        (nSecondsPerMSamples >= 3600 ? nSecondsPerMSamples / 3600 + "h" : "") +
                                (nSecondsPerMSamples >= 60 ? (nSecondsPerMSamples % 3600) / 60 + "m" : "") +
                                (nSecondsPerMSamples % 60 + "s");
                m_out.print(" " + sTimePerMSamples + "/Msamples");
            }
        }
        m_out.println();
    } // log


    private String prettifyLogLine(String logContent) {
        String[] sStrs = logContent.split("\t");
        logContent = "";
        for (String sStr : sStrs) {
            logContent += prettifyLogEntry(sStr);
        }
        return logContent;
    }

    private String prettifyLogEntry(String sStr) {
        // TODO Q2R intelliJ says \\ can't be used in a range ...
        if (sStr.matches("[\\d-E]+\\.[\\d-E]+")) {
            // format as double
            if (sStr.contains("E")) {
                if (sStr.length() > 15) {
                    String[] sStrs = sStr.split("E");
                    return " " + sStrs[0].substring(0, 15 - sStrs[1].length() - 2) + "E" + sStrs[1];
                } else {
                    return "               ".substring(sStr.length()) + sStr;
                }
            }
            String s1 = sStr.substring(0, sStr.indexOf("."));
            String s2 = sStr.substring(sStr.indexOf(".") + 1);
            while (s2.length() < 4) {
                s2 = s2 + " ";
            }
            s2 = s2.substring(0, 4);
            sStr = s1 + "." + s2;
            sStr = "               ".substring(sStr.length()) + sStr;
        } else if (sStr.length() < 15) {
            // format integer, boolean
            sStr = "               ".substring(sStr.length()) + sStr;
        } else {
            sStr = " " + sStr;
        }
        int nOverShoot = sStr.length() - 15;
        while (nOverShoot > 0 && sStr.length() > 2 && sStr.charAt(1) == ' ') {
            sStr = sStr.substring(1);
            nOverShoot--;
        }
        if (nOverShoot > 0) {
            sStr = sStr.substring(0, 8) + "_" + sStr.substring(sStr.length() - 6);
        }
        return sStr;
    }


    /**
     * stop logging, produce end of log message and close file (if necessary) *
     */
    public void close() {
        for (Loggable m_logger : m_loggers) {
            m_logger.close(m_out);
        }

        if (m_out != System.out) {
            // close all file, except stdout
            m_out.close();
        }
    } // close


    public static int getSampleOffset() {
        return m_nSampleOffset;
    }

} // class Logger
