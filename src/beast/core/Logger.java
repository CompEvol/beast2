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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

@Description("Logs results of calculation processes.")
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

    /** Compound loggers get a sample number printed at the beginning of the line,
     * while tree loggers don't.
     */
    public final static int COMPOUND_LOGGER = 0, TREE_LOGGER = 2;
    public int m_mode = COMPOUND_LOGGER;
    
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
        String sFileName = m_pFileName.get();
        if (sFileName == null || sFileName.length() == 0) {
            m_out = System.out;
        } else {
            if (sFileName.contains("$(seed)")) {
                sFileName = sFileName.replace("$(seed)", Randomizer.getSeed() + "");
                m_pFileName.setValue(sFileName, this);
            }
            m_out = new PrintStream(sFileName);
        }
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
    } // init


    
    /** log the state for given sample nr
     **/
    public void log(int nSample) {
        if ((nSample < 0) || (nSample % m_nEvery > 0)) {
            return;
        }
        if (m_mode == COMPOUND_LOGGER) {
            m_out.print(nSample + "\t");
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
