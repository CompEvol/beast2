package beast.core.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/** class used for logging messages from programs 
 * Comes with 5 levels:
 * error, 
 * warning, 
 * info, 
 * debug, 
 * trace
 **/ 
public class Log {
	static PrintStream nullStream = new PrintStream(new OutputStream() {
		@Override
		public void write(int b) throws IOException {
		}
	});
	
    public enum Level {
        error, warning, info, debug, trace
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

	static {
		setLevel(level);
	}
	
	static public PrintStream err= System.err;
	static public PrintStream warning= System.err;
	static public PrintStream info = System.out;
	static public PrintStream debug = System.out;
	static public PrintStream trace = System.out;

	static private PrintStream errIfOpen = System.err;
	static private PrintStream warningIfOpen = System.err;
	static private PrintStream infoIfOpen = System.out;
	static private PrintStream debugIfOpen = System.out;
	static private PrintStream traceIfOpen = System.out;

	
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
		}
	}
	
} // Log
