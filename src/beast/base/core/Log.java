package beast.base.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

/** class used for logging messages from programs 
 * Comes with 5 levels:
 * error, 
 * warning, 
 * info, 
 * debug, 
 * trace
 * 
 * To log a message, refer to one of the streams, e.g.
 * Log.info.println("Hello world!");
 * 
 * or pass message directly through method
 * Log.info("Hello world!");
 * 
 * The former gives better control of newlines, while the latter always adds a newline to the string.
 **/ 
public class Log {
	static public PrintStream nullStream = new PrintStream(new OutputStream() {
		@Override
		public void write(byte[] b, int off, int len) throws IOException {};
		@Override
		public void write(int b) throws IOException {
		}
	});
	
    public enum Level {
        none, error, warning, info, debug, trace
    }

    static Level level = Level.info;
    
    /** return log levels as array of Strings **/
    static public String [] values() {
    	String [] values = new String[Level.values().length];
    	for (int i = 0; i < values.length; i++) {
			values[i] = Level.values()[i].toString();
		}
    	return values;
    }
	
	static public PrintStream err;
	static public void err(String msg) {err.println(msg);}
	static public PrintStream warning;
	static public void warning(String msg) {warning.println(msg);}
	static public PrintStream info;
	static public void info(String msg) {info.println(msg);}
	static public PrintStream debug;
	static public void debug(String msg) {debug.println(msg);}
	static public PrintStream trace;
	static public void trace(String msg) {trace.println(msg);}

	static private PrintStream errIfOpen;
	static private PrintStream warningIfOpen;
	static private PrintStream infoIfOpen;
	static private PrintStream debugIfOpen;
	static private PrintStream traceIfOpen;

	static {
		// Initialise streams here instead of when declaring the variables.
		// This is a static method and these are static members. 
		//  These can suffer from different orders of initialisation, depending on JVM
		err= System.err;
		warning= System.err;
		info = System.out;
		debug = System.out;
		trace = System.out;

		errIfOpen = System.err;
		warningIfOpen = System.err;
		infoIfOpen = System.out;
		debugIfOpen = System.out;
		traceIfOpen = System.out;
		if (System.getProperty("beast.log.level") != null) {
			try {
				level = Level.valueOf(System.getProperty("beast.log.level"));
			} catch (IllegalArgumentException e) {
				System.err.println("beast.log.level is set to " + System.getProperty("beast.log.level") + " "
						+"but should be one of " + Arrays.toString(Level.values()));
			}
		}
		setLevel(level);
	}
	
	final static public int ERROR = 0;
	final static public int WARNING = 1;
	final static public int INFO = 2;
	final static public int DEBUG = 3;
	final static public int TRACE = 4;
	
	/** Determines the level of logging that actually
	 * reaches output. Only newLevel and below will 
	 * be shown, the rest is suppressed.
	 */
	static public void setLevel(Level newLevel) {
		level = newLevel;
		
		errIfOpen = (err == nullStream ? errIfOpen : err);
		warningIfOpen = (warning == nullStream ? warningIfOpen : warning);
		infoIfOpen = (info == nullStream ? infoIfOpen : info);
		debugIfOpen = (debug == nullStream ? debugIfOpen : debug);
		traceIfOpen = (trace == nullStream ? traceIfOpen : trace);

		err = nullStream;
		warning = nullStream;
		info = nullStream;
		debug = nullStream;
		trace = nullStream;
		
		switch (level) {
		case trace:
			trace  = traceIfOpen;
		case debug:
			debug = debugIfOpen; 
		case info:
			info = infoIfOpen;
		case warning:
			warning = warningIfOpen;
		case error:
			err = errIfOpen;
		case none:
			
		}
	}
	
} // Log
