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
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

@Description("Logs results of a calculation processes on regular intervals.")
public class Logger extends Plugin {

    public Input<List<Plugin>> m_pLoggers = new Input<List<Plugin>>("log",
                    "Element in a log. This can be any plug in that is Loggable.",
                    new ArrayList<Plugin>(), Validate.REQUIRED, Loggable.class);
    
    public Input<Integer> m_pEvery = new Input<Integer>("logEvery", "Number of the samples logged", 1);
    public Input<String> m_pFileName = new Input<String>("fileName", "Name of the file, or stdout if left blank");
    public Input<Plugin> m_pModelPlugin = new Input<Plugin>("model", "Model to log at the top of the log. " +
    		"If specified, XML will be produced for the model, commented out by # at the start of a line. " +
    		"Alignments are suppressed. This way, the log file documents itself. ");

    /** list of loggers, if any */
    Loggable m_loggers[];
    public final static int FILE_ONLY_NEW = 0, FILE_OVERWRITE = 1, FILE_APPEND = 2;
    public static int FILE_MODE = FILE_ONLY_NEW;
    /** Compound loggers get a sample number printed at the beginning of the line,
     * while tree loggers don't.
     */
    public final static int COMPOUND_LOGGER = 0, TREE_LOGGER = 2;
    public int m_mode = COMPOUND_LOGGER;
    /** offset for the sample number, which is non-zero when a chain is resumed **/
    static int m_nSampleOffset = 0;
    
    /** number of samples between logs **/
    int m_nEvery = 1;
    
    /** stream to log to */
    PrintStream m_out;

    /** keep track of time taken between logs to estimate speed **/
    long m_nStartLogTime;

    
    @Override
    public void initAndValidate() throws Exception {
        List<Plugin> loggers = m_pLoggers.get();
        final int nLoggers = loggers.size();
        if ( nLoggers == 0) {
            throw new Exception("Logger with nothing to log specified");
        }

        m_loggers = new Loggable[nLoggers];
        for(int k = 0; k < nLoggers; ++k) {
            m_loggers[k] = (Loggable)loggers.get(k);
        }

        // determine logging mode
        m_mode = COMPOUND_LOGGER;
        if ( nLoggers==1 && m_loggers[0] instanceof Tree) {
            m_mode = TREE_LOGGER;
        }

        if (m_pEvery.get() != null) {
            m_nEvery = m_pEvery.get();
        }

        m_nStartLogTime = System.currentTimeMillis();
    } // initAndValidate

    
    
    /** initialise log, open file (if necessary) and produce header of log
     **/
    public void init() throws Exception {
    	boolean bNeedsHeader = openLogFile();
    	if (bNeedsHeader) {
	        if (m_pModelPlugin.get() != null) {
	        	// print model at top of log
	        	String sXML = new XMLProducer().modelToXML(m_pModelPlugin.get());
	        	sXML = "#" + sXML.replaceAll("\\n", "\n#");
	        	m_out.println("\n#model:\n#");
	        	m_out.println(sXML);
	        	m_out.println("#");
	        }
	        if (m_mode == COMPOUND_LOGGER) {
	            m_out.print("Sample\t");
	        }
	        for(Loggable m_logger : m_loggers) {
	            m_logger.init(m_out);
	        }
	        m_out.println();
    	}
    } // init

    boolean openLogFile() throws Exception {
        String sFileName = m_pFileName.get();
        if (sFileName == null || sFileName.length() == 0) {
            m_out = System.out;
            return true;
        } else {
            if (sFileName.contains("$(seed)")) {
                sFileName = sFileName.replace("$(seed)", Randomizer.getSeed() + "");
                m_pFileName.setValue(sFileName, this);
            }
            switch (FILE_MODE) {
            case FILE_ONLY_NEW :// only open file if the file does not already exists
            {
            	File file = new File(sFileName);
            	if (file.exists()) {
            		throw new Exception("Trying to write file " + sFileName + " but the file already exists (perhaps use the -overwrite flag?).");
            	}
                m_out = new PrintStream(sFileName);
                System.out.println("Writing file " + sFileName);
                return true;
            }
            case FILE_OVERWRITE :// (over)write log file
            {
            	String sMsg = "Writing";
            	if (new File(sFileName).exists()) {
            		sMsg = "Warning: Overwriting";
            	}
                m_out = new PrintStream(sFileName);
                System.out.println(sMsg + " file " + sFileName);
                return true;
            }
            case FILE_APPEND :// append log file, pick up SampleOffset by reading existing log
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
	            		m_nSampleOffset = Integer.parseInt(sStr.split("\\s")[0]);
	                	// open the file for appending
	                	FileOutputStream out2 = new FileOutputStream(sFileName, true);
	                    m_out = new PrintStream(out2);
	                } else {
	                	// it is a tree logger, we need to get rid of the last line!
	            		BufferedReader fin = new BufferedReader(new FileReader(sFileName));
	            		StringBuffer buf = new StringBuffer();
	            		String sStrLast = null;
	            		String sStr = null;
	            		while (fin.ready()) {
	            			sStrLast = sStr;
	            			buf.append(sStrLast);
	            			buf.append('\n');
	            			sStr = fin.readLine();
	            		}
	            		fin.close();
	            		// determine number of the last sample
	            		sStr = sStrLast.split("\\s+")[1];
	            		m_nSampleOffset = Integer.parseInt(sStr.substring(6));
	                	// open the file and write back all but the last line
	                	FileOutputStream out2 = new FileOutputStream(sFileName);
	                    m_out = new PrintStream(out2);
	                    m_out.print(buf.toString());
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
    
    /** log the state for given sample nr
     **/
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
        if (m_mode == COMPOUND_LOGGER) {
            m_out.print((nSample)+ "\t");
        }
        for(Loggable m_logger : m_loggers) {
            m_logger.log(nSample, m_out);
        }
        if (m_out == System.out) {
            long nLogTime = System.currentTimeMillis();
            int nSecondsPerMSamples = (int) ((nLogTime - m_nStartLogTime) * 1000.0 / (nSample + 1.0));
            String sTimePerMSamples =
                    (nSecondsPerMSamples >= 3600 ? nSecondsPerMSamples / 3600 + "h" : "") +
                            (nSecondsPerMSamples >= 60 ? (nSecondsPerMSamples % 3600) / 60 + "m" : "") +
                            (nSecondsPerMSamples % 60 + "s");
//            if (nSecondsPerMSamples < 600) {
//            	sTimePerMSamples += (nLogTime - m_nStartLogTime) % 1000; 
//            }
            m_out.print(sTimePerMSamples + "/Msamples");
        }
        m_out.println();
    } // log


    
    /** stop logging, produce end of log message and close file (if necessary) **/
    public void close() {
        for(Loggable m_logger : m_loggers) {
            m_logger.close(m_out);
        }

        if( m_out != System.out )  {
        	// close all file, except stdout
            m_out.close();
        }
    } // close

} // class Logger
