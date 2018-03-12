package beast.core;

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
    void log(long sample, PrintStream out);

    /**
     * close log. An end of log message can be left (as in End; for Nexus trees)
     *
     * @param out log stream
     */
    void close(PrintStream out);
}
