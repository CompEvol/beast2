package beast.app.util;

import java.util.logging.*;

public class MessageLogHandler extends StreamHandler {

	public MessageLogHandler() {
		setOutputStream(System.out);
		setFormatter(new MessageLogFormatter());
	}


	public void publish(LogRecord record) {
		super.publish(record);
		flush();
	}

	public void close() {
		flush();
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
		 * @param record the log record to be formatted.
		 * @return a formatted log record
		 */
		public synchronized String format(LogRecord record) {
			final StringBuffer sb = new StringBuffer();
            sb.append(formatMessage(record));
			sb.append(lineSeparator);
			return sb.toString();
		}
	}
}

