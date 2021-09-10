package beast.app.util;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class ErrorLogHandler extends StreamHandler {

    public ErrorLogHandler(int maxErrorCount) {
        setOutputStream(System.err);
        setFormatter(new MessageLogFormatter());

        this.maxErrorCount = maxErrorCount;
    }


    @Override
	public void publish(LogRecord record) {
        super.publish(record);
        flush();

        if (record.getLevel() == Level.SEVERE) {
            errorCount++;

            if (errorCount > maxErrorCount) {
                if (errorCount > 1) {
                    throw new RuntimeException("ErrorLog: Maximum number of errors (" + (maxErrorCount + 1) + ") reached. Terminating BEAST");
                } else {
                    throw new RuntimeException("An error was encounted. Terminating BEAST");
                }
            }
        }
    }

    @Override
	public void close() {
        flush();
    }

    public int getErrorCount() {
        return errorCount;
    }

    private class MessageLogFormatter extends Formatter {

        // Line separator string.  This is the value of the line.separator
        // property at the moment that the SimpleFormatter was created.
        private final String lineSeparator = System.getProperty("line.separator");

        // AR - is there a reason why this was used? It causes warnings at compile
//        private final String lineSeparator = (String) java.security.AccessController.doPrivileged(
//                new sun.security.action.GetPropertyAction("line.separator"));

        /**
         * Format the given LogRecord.
         *
         * @param record the log record to be formatted.
         * @return a formatted log record
         */
        @Override
		public synchronized String format(LogRecord record) {
            StringBuffer sb = new StringBuffer();
            String message = formatMessage(record);
            sb.append(message);
            sb.append(lineSeparator);
            return sb.toString();
        }
    }


    private final int maxErrorCount;
    private int errorCount = 0;
}