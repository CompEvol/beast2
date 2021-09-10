package beast.app.util;

import java.io.File;

public class XMLFile extends File {

	public XMLFile(File parent, String child) {
		super(parent, child);
	}

	public XMLFile(String pathname) {
		super(pathname);
	}

}
