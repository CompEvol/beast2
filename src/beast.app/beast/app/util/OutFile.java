package beast.app.util;

import java.io.File;

public class OutFile extends File {

	public OutFile(File parent, String child) {
		super(parent, child);
	}

	public OutFile(String string) {
		super(string);
	}

}
