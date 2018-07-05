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



import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import beast.core.Input.Validate;
import beast.core.util.Log;
import beast.evolution.tree.Tree;
import beast.util.XMLProducer;


@Description("Logs results of a calculation processes on regular intervals.")
public class Logger extends BEASTObject {
    /**
     * currently supported modes *
     */
    public enum LOGMODE {
        autodetect, compound, tree
    }

    public enum SORTMODE {
        none, alphabetic, smart
    }

    final public Input<String> fileNameInput = new Input<>("fileName", "Name of the file, or stdout if left blank");

    final public Input<Integer> everyInput = new Input<>("logEvery", "Number of the samples logged", 1);
    final public Input<BEASTObject> modelInput = new Input<>("model", "Model to log at the top of the log. " +
            "If specified, XML will be produced for the model, commented out by # at the start of a line. " +
            "Alignments are suppressed. This way, the log file documents itself. ");
    final public Input<LOGMODE> modeInput = new Input<>("mode", "logging mode, one of " + Arrays.toString(LOGMODE.values()), LOGMODE.autodetect, LOGMODE.values());
    final public Input<SORTMODE> sortModeInput = new Input<>("sort", "sort items to be logged, one of " + Arrays.toString(SORTMODE.values()), SORTMODE.none, SORTMODE.values());
    final public Input<Boolean> sanitiseHeadersInput = new Input<>("sanitiseHeaders", "whether to remove any clutter introduced by Beauti" , false);

    final public Input<List<BEASTObject>> loggersInput = new Input<>("log",
            "Element in a log. This can be any plug in that is Loggable.",
            new ArrayList<>(), Validate.REQUIRED, Loggable.class);

    // the file name to log to, or null, or "" if logging to stdout
    private String fileName;

    /**
     * list of loggers, if any
     */
    List<Loggable> loggerList;
    public enum LogFileMode {
    	only_new, overwrite, resume, only_new_or_exit
    }
    public static LogFileMode FILE_MODE = LogFileMode.only_new;
    
    /**
     * Compound loggers get a sample number printed at the beginning of the line,
     * while tree loggers don't.
     */
    public LOGMODE mode = LOGMODE.compound;
    
    /**
     * offset for the sample number, which is non-zero when a chain is resumed *
     */
    static long sampleOffset = -1;

    /**
     * number of samples between logs *
     */
    long every = 1;

    /**
     * stream to log to
     */
    PrintStream m_out;

    /**
     * keep track of time taken between logs to estimate speed *
     */
    long startLogTime = -5;
    long startSample;

    @Override
    public void initAndValidate() {

        fileName = fileNameInput.get();

        final List<BEASTObject> loggers = loggersInput.get();
        final int loggerCount = loggers.size();
        if (loggerCount == 0) {
            throw new RuntimeException("Logger with nothing to log specified");
        }

        loggerList = new ArrayList<>();
        for (final BEASTObject logger : loggers) {
            loggerList.add((Loggable) logger);
        }

        // determine logging mode
        final LOGMODE mode = modeInput.get();
        if (mode.equals(LOGMODE.autodetect)) {
            this.mode = LOGMODE.compound;
            if (loggerCount == 1 && loggerList.get(0) instanceof Tree) {
            	this.mode = LOGMODE.tree;
            }
        } else if (mode.equals(LOGMODE.tree)) {
        	this.mode = LOGMODE.tree;
        } else if (mode.equals(LOGMODE.compound)) {
        	this.mode = LOGMODE.compound;
        } else {
            throw new IllegalArgumentException("Mode '" + mode + "' is not supported. Choose one of " + Arrays.toString(LOGMODE.values()));
        }

        if (everyInput.get() != null) {
            every = everyInput.get();
        }
        
        if (this.mode == LOGMODE.compound) {
        	switch (sortModeInput.get()) {
        	case none:
        		// nothing to do
       			break;
        	case alphabetic:
        		// sort loggers by id
        		Collections.sort(loggerList, (Loggable o1, Loggable o2) -> {
						final String id1 = ((BEASTObject)o1).getID();
						final String id2 = ((BEASTObject)o2).getID();  //was o1, probably a bug, found by intelliJ
						if (id1 == null || id2 == null) {return 0;}
						return id1.compareTo(id2);
					}
				);
    			break;
        	case smart:
        		// Group loggers with same id-prefix, where the prefix of an id is
        		// defined as the part of an id before the first full stop.
        		// This way, multi-partition analysis generated by BEAUti get all  
        		// related log items together in Tracer
        		final List<String> ids = new ArrayList<>();
        		final List<String> postfix = new ArrayList<>();
                for (final Loggable aLoggerList : loggerList) {
                    String id = ((BEASTInterface) aLoggerList).getID();
                    if (id == null) {
                        id = "";
                    }
                    String post = id;
                    if (id.indexOf('.') > 0) {
                        id = id.substring(0, id.indexOf('.'));
                        post = post.substring(post.indexOf('.') + 1);
                    } else {
                    	post = "";
                    }
                    ids.add(id);
                    postfix.add(post);
                }
        		for (int i = 0; i < loggerList.size(); i++) {
        			int k = 1;
        			final String id = ids.get(i);
        			for (int j = i + 1; j < loggerList.size(); j++) {
        				if (ids.get(j).equals(id)) {
        					int m = k;
        					String post = postfix.get(j);
        					while (m >= 0 && id.equals(ids.get(i+m-1)) && postfix.get(i+m-1).compareTo(post) > 0) {
        						m--;
        					}
        					ids.remove(j);
        					ids.add(i + m, id);
        					String p = postfix.remove(j);
        					postfix.add(i + m, p);
        					final Loggable l = loggerList.remove(j);
        					loggerList.add(i + m, l);
        					k++;
        				}
        			}
        		}
    			break;
        	}
        }
    } // initAndValidate

    /**
     * @return true if this logger is logging to stdout.
     */
    public boolean isLoggingToStdout() {
        return (fileName == null || fileName.length() == 0);
    }

    /**
     * initialise log, open file (if necessary) and produce header of log
     */
    public void init() throws IOException {
        final boolean needsHeader = openLogFile();
        if (needsHeader) {
            if (modelInput.get() != null) {
                // print model at top of log
                String xml = new XMLProducer().modelToXML(modelInput.get());
                xml = "#" + xml.replaceAll("\\n", "\n#");
                m_out.println("#\n#model:\n#");
                m_out.println(xml);
                m_out.println("#");
            }
            ByteArrayOutputStream baos = null;
            PrintStream tmp = null;
            if (m_out == System.out) {
                tmp = m_out;
                baos = new ByteArrayOutputStream();
                m_out = new PrintStream(baos);
            }
            final ByteArrayOutputStream rawbaos = new ByteArrayOutputStream();
            final PrintStream out = new PrintStream(rawbaos);
            if (mode == LOGMODE.compound) {
                out.print("Sample\t");
            }
            for (final Loggable m_logger : loggerList) {
                m_logger.init(out);
            }

            // Remove trailing tab from header
            String header = rawbaos.toString().trim();

            if (sanitiseHeadersInput.get()) {
            	m_out.print(sanitiseHeader(header));
            } else {
            	m_out.print(header);
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

    /** remove indicators of partition context from header of a log file **/
    public String sanitiseHeader(String header) {
    	// collect partitions
    	String partitionPrefix = null, clockPrefix = null, sitePrefix = null, treePrefix = null;
    	for (int i = 0; i < header.length(); i++) {
    		char c = header.charAt(i);
    		if (c == '.') {
    			if (i < header.length() - 2 && header.charAt(i+2) == ':') {
    				final char c2 = header.charAt(++i);
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
					// get rid of braces
					partitionPrefix = partitionPrefix.replaceAll("[\\(\\)]","");
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
    	header = header.replaceAll("\\.$", "");
		return header;
	}

    /** return longest common prefix of two strings, except when the first
     * on is null, then it returns the second string.
     */
	private String getprefix(final String str1, final String str2) {
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


	boolean openLogFile() throws IOException {
        if (isLoggingToStdout()) {
            m_out = System.out;
            return true;
        } else {
            if (fileName.contains("$(tree)")) {
            	String treeName = "tree";
            	for (final Loggable logger : loggerList) {
            		if (logger instanceof BEASTObject) {
            			final String id = ((BEASTObject) logger).getID();
            			if (id.indexOf(".t:") > 0) {
            				treeName = id.substring(id.indexOf(".t:") + 3); 
            			}
            		}
            	}
                fileName = fileName.replace("$(tree)", treeName);
                fileNameInput.setValue(fileName, this);
            }
            if (System.getProperty("file.name.prefix") != null) {
                fileName = System.getProperty("file.name.prefix") + fileName;
            }
            switch (FILE_MODE) {
                case only_new:// only open file if the file does not already exists
                case only_new_or_exit: {
                    final File file = new File(fileName);
                    if (file.exists()) {
                        if (FILE_MODE == LogFileMode.only_new_or_exit) {
                            Log.err.println("Trying to write file " + fileName + " but the file already exists. Exiting now.");
                            throw new RuntimeException("Use overwrite or resume option, or remove the file");
                            //System.exit(0);
                        }
                        // Check with user what to do next
                        Log.info.println("Trying to write file " + fileName + " but the file already exists (perhaps use the -overwrite flag?).");
                        if (System.getProperty("beast.useWindow") != null) {
                        	// we are using the BEAST console, so no input is possible
                        	throw new IllegalArgumentException();
						}
                        Log.info.println("Overwrite (Y/N)?:");
                        Log.info.flush();
                        final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));	                        
                        final String msg = stdin.readLine();
                        if (!msg.toLowerCase().equals("y")) {
                        	Log.info.println("Exiting now.");
                            System.exit(0);
                        }
                    }
                    m_out = new PrintStream(fileName);
                    Log.info.println("Writing file " + fileName);
                    return true;
                }
                case overwrite:// (over)write log file
                {
                    String msg = "Writing";
                    if (new File(fileName).exists()) {
                        msg = "Warning: Overwriting";
                    }
                    m_out = new PrintStream(fileName);
                    Log.warning.println(msg + " file " + fileName);
                    return true;
                }
                case resume:// append log file, pick up SampleOffset by reading existing log
                {
                    final File file = new File(fileName);
                    if (file.exists()) {
                        if (mode == LOGMODE.compound) {
                            // first find the sample nr offset
                            final BufferedReader fin = new BufferedReader(new FileReader(fileName));
                            String str = null;
                            while (fin.ready()) {
                                str = fin.readLine();
                            }
                            fin.close();
                            assert str != null;
                            final long sampleOffset = Long.parseLong(str.split("\\s")[0]);
                            if (Logger.sampleOffset > 0 && sampleOffset != Logger.sampleOffset) {
                                throw new RuntimeException("Error 400: Cannot resume: log files do not end in same sample number");
                            }
                            Logger.sampleOffset = sampleOffset;
                            // open the file for appending
                            final FileOutputStream out2 = new FileOutputStream(fileName, true);
                            m_out = new PrintStream(out2);
                        } else {
                            // it is a tree logger, we may need to get rid of the last line!

                            // back up file in case something goes wrong (e.g. an out of memory error occurs)
                            final File treeFileBackup = new File(fileName);
                            
                            //final boolean ok = treeFileBackup.renameTo(new File(fileName + ".bu"));    assert ok;
                            Files.move(treeFileBackup.toPath(), new File(fileName+".bu").toPath(), StandardCopyOption.ATOMIC_MOVE);
                            // open the file and write back all but the last line
                            final BufferedReader fin = new BufferedReader(new FileReader(fileName+".bu"));

                            final FileOutputStream out2 = new FileOutputStream(fileName);
                            m_out = new PrintStream(out2);

                            //final StringBuilder buf = new StringBuilder();
                            String strLast = null;
                            //String str = fin.readLine();
                            boolean endSeen = false;
                            while (fin.ready()) {
                                if( endSeen ) {
                                    m_out.println("End;");
                                    endSeen = false;
                                }
                                final String str = fin.readLine();
                                if (!str.equals("End;")) {
                                	m_out.println(str);
                                    strLast = str;
                                } else {
                                    endSeen = true;
                                }
                            }
                            fin.close();

                            // determine number of the last sample
                            if( strLast == null ) {
                                // empty log file?
                                 throw new RuntimeException("Error 402: empty tree log file " + fileName + "? (check if there is a back up file " + fileName + ".bu)");
                            }
                            final String str = strLast.split("\\s+")[1];
                            final long sampleOffset = Long.parseLong(str.substring(6));
                            if (Logger.sampleOffset > 0 && sampleOffset != Logger.sampleOffset) {
                                //final boolean ok1 = treeFileBackup.renameTo(new File(fileName));        assert ok1;
                                Files.move(treeFileBackup.toPath(), new File(fileName).toPath(), StandardCopyOption.ATOMIC_MOVE);
                                throw new RuntimeException("Error 401: Cannot resume: log files do not end in same sample number");
                            }
                            Logger.sampleOffset = sampleOffset;
                            // it is safe to remove the backup file now
                            new File(fileName + ".bu").delete();
                        }
                        Log.info.println("Appending file " + fileName);
                        return false;
                    } else {
                        m_out = new PrintStream(fileName);
                        Log.warning.println("WARNING: Resuming, but file " + fileName + " does not exist yet (perhaps the seed number is not the same as before?).");
                        Log.info.println("Writing new file " + fileName);
                        return true;
                    }
                }
                default:
                    throw new RuntimeException("DEVELOPER ERROR: unknown file mode for logger " + FILE_MODE);
            }
        }
    } // openLogFile

    /**
     * log the state for given sample nr
     * *
     * * @param sample
     */
    public void log(long sampleNr) {
        if ((sampleNr < 0) || (sampleNr % every > 0)) {
            return;
        }
        if (sampleOffset >= 0) {
            if (sampleNr == 0) {
                // don't need to duplicate the last line in the log
                return;
            }
            sampleNr += sampleOffset;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);

        if (mode == LOGMODE.compound) {
            out.print((sampleNr) + "\t");
        }

        for (final Loggable m_logger : loggerList) {
            m_logger.log(sampleNr, out);
        }

        // Acquire log string and trim excess tab
        String logContent;
        try {
            logContent = baos.toString("ASCII").trim();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("ASCII string encoding not supported: required for logging!");
        }

        // Include calculation speed estimate if this log is going to the terminal
        if ( m_out == System.out ) {
            logContent = prettifyLogLine(logContent);
            m_out.print(logContent);

            if (startLogTime < 0) {
                if (sampleNr - sampleOffset > 6000) {
                    startLogTime++;
                    if (startLogTime == 0) {
                        startLogTime = System.currentTimeMillis();
                        startSample = sampleNr;
                    }
                }
                m_out.print(" --");
            } else {

                final long logTime = System.currentTimeMillis();
                final int secondsPerMSamples = (int) ((logTime - startLogTime) * 1000.0 / (sampleNr - startSample + 1.0));
                final String timePerMSamples =
                        (secondsPerMSamples >= 3600 ? secondsPerMSamples / 3600 + "h" : "") +
                                (secondsPerMSamples >= 60 ? (secondsPerMSamples % 3600) / 60 + "m" : "") +
                                (secondsPerMSamples % 60 + "s");
                m_out.print(" " + timePerMSamples + "/Msamples");
            }
            m_out.println();

        } else {
            m_out.println(logContent);
        }
    } // log


    private String prettifyLogLine(String logContent) {
        final String[] strs = logContent.split("\t");
        logContent = "";
        for (final String str : strs) {
            logContent += prettifyLogEntry(str);
        }
        return logContent;
    }

    private String prettifyLogEntry(String str) {
        // TODO Q2R intelliJ says \\ can't be used in a range ...
        if (str.matches("[\\d-E]+\\.[\\d-E]+")) {
            // format as double
            if (str.contains("E")) {
                if (str.length() > 15) {
                    final String[] strs = str.split("E");
                    return " " + strs[0].substring(0, 15 - strs[1].length() - 2) + "E" + strs[1];
                } else {
                    return "               ".substring(str.length()) + str;
                }
            }
            final String s1 = str.substring(0, str.indexOf("."));
            String s2 = str.substring(str.indexOf(".") + 1);
            while (s2.length() < 4) {
                s2 = s2 + " ";
            }
            s2 = s2.substring(0, 4);
            str = s1 + "." + s2;
            str = "               ".substring(str.length()) + str;
        } else if (str.length() < 15) {
            // format integer, boolean
            str = "               ".substring(str.length()) + str;
        } else {
            str = " " + str;
        }
        int overShoot = str.length() - 15;
        while (overShoot > 0 && str.length() > 2 && str.charAt(1) == ' ') {
            str = str.substring(1);
            overShoot--;
        }
        if (overShoot > 0) {
            str = str.substring(0, 8) + "_" + str.substring(str.length() - 6);
        }
        return str;
    }


    /**
     * stop logging, produce end of log message and close file (if necessary) *
     */
    public void close() {
        for (final Loggable m_logger : loggerList) {
            m_logger.close(m_out);
        }

        if (m_out != System.out) {
            // close all file, except stdout
            m_out.close();
        }
    } // close

    public PrintStream getM_out() {
        return m_out;
    }

    public static long getSampleOffset() {
        return sampleOffset < 0 ? 0 : sampleOffset;
    }

} // class Logger
