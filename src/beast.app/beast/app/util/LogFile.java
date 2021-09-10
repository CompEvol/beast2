package beast.app.util;

import java.io.File;

public class LogFile extends File {

	public LogFile(File parent, String child) {
		super(parent, child);
	}

	public LogFile(String pathname) {
		super(pathname);
	}

}
