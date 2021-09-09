package beast.app.util;

import java.io.File;

public class TreeFile extends File {

	public TreeFile(File parent, String child) {
		super(parent, child);
	}

	public TreeFile(String string) {
		super(string);
	}

}
