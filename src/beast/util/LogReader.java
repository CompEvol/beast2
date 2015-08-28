package beast.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Walter Xie
 */
public abstract class LogReader {
    public static final int BURN_IN_PERCENTAGE = 10; // default
    final String SPACE = OutputUtils.SPACE;

    protected String fileName;
    protected int burninPercentage; // TODO necessary?
    // determine init and next
    protected boolean lowMemory = false;
    // current data flag
    protected int current = 0;
    // total lines of data
    protected long total;
    // number of lines in the file
//    int lineNr = 0;  // TODO necessary?
    // state1 - state0 in log
    protected int sampleInterval = 1;

    // used for storing comments before the actual log file commences
    protected String m_sPreAmble;

    final protected static String BAR = "|---------|---------|---------|---------|---------|---------|---------|---------|";


    //+++++++++ abstract method ++++++
    // initialize
    public abstract void init(String fileName, int burninPercentage) throws Exception;
    // has next state
    public abstract boolean hasNext();
    // process the line
//    protected abstract Object next() throws Exception;
    // process the line with conditional
//    protected abstract Object next(boolean process) throws Exception;
    // process the line with conditional
    public abstract Object next(BufferedReader fin, boolean process) throws Exception;
    // reset initialize flag for full analysis
    public abstract void reset() throws Exception;


    //+++++++++ static method ++++++
    protected static void printUsageAndExit(String name) {
        System.out.println(name + " [-b <burninPercentage] [file1] ... [filen]");
        System.out.println("-burnin <burninPercentage>");
        System.out.println("--burnin <burninPercentage>");
        System.out.println("-b <burninPercentage> percentage of log file to disregard, default " + BURN_IN_PERCENTAGE);
        System.out.println("-help");
        System.out.println("--help");
        System.out.println("-h print this message");
        System.out.println("[fileX] log file to analyse. Multiple files are allowed, each is analysed separately");
        System.exit(0);
    }

    //+++++++++ public methods ++++++
    public long getNBurnIn(int burninPercentage) {
        return total * burninPercentage / 100;
    }

    public long initNData(long nBurnIn) {
        return -nBurnIn - 1;
    }

    public int getSampleInterval() {
        return sampleInterval;
    }

    public long getTotal() {
        return total;
    }

    //+++++++++ protected methods ++++++
    // read line
    protected String readLine(BufferedReader fin) throws IOException {
        if (!fin.ready()) {
            return null;
        }
        return fin.readLine();
    }
    // read line and sum up lineNr
    protected String readLine(BufferedReader fin, long lineNr) throws IOException {
        if (!fin.ready()) {
            return null;
        }
        lineNr++;
        return fin.readLine();
    }

    protected void log(String s) {
        System.err.print(s);
    }

    protected void logln(String s) {
        System.err.println(s);
    }


}
