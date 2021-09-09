package beast.app.inputeditor;


import java.io.File;

import beast.app.inputeditor.BeautiDoc;
import beast.app.util.LogFile;
import beast.base.BEASTInterface;
import beast.base.Input;

public class LogFileInputEditor extends FileInputEditor {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Class<?> type() {
		return LogFile.class;
	}

	public LogFileInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
		init(input, plugin, itemNr, bExpandOption, bAddButtons, "trace files", "log");
	}

	protected File newFile(File file) {
		return new LogFile(file.getPath());
	}

}
