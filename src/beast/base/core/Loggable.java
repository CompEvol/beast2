package beast.base.core;

import java.io.PrintStream;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */

/**
 * interface for items that can be logged through a Logger *
 */
public interface Loggable {

    /**
     * write header information, e.g. labels of a parameter,
     * or Nexus tree preamble
     *
     * @param out log stream
     */
    void init(PrintStream out);

    /**
     * log this sample for current state to PrintStream,
     * e.g. value of a parameter, list of parameters or Newick tree
     *
     * @param sample chain sample number
     * @param out     log stream
     */
    default void log(long sample, PrintStream out) {
    	if (sample < Integer.MAX_VALUE) {
    		log((int) sample, out);
    	} else {
    		throw new IllegalArgumentException("Loggable::log(long,Prinstream) was not implemented: cannot log samples larger than " + Integer.MAX_VALUE);
    	}
    }
    
    /** For backward compatibility only: the int-version of log().
     * 
     * Please use log(long sample, PrintStream out) instead of log(int, PrintStream)
     */
    @Deprecated 
    default void log(int sample, PrintStream out) {
    	// if either int or long version is not implemented
    	// this ensure one of them get called
		log((long) sample, out);    	
    }
    
    /**
     * close log. An end of log message can be left (as in End; for Nexus trees)
     *
     * @param out log stream
     */
    void close(PrintStream out);
}
