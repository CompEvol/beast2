package beast.app.inputeditor;



import java.io.File;

import beast.app.inputeditor.BeautiDoc;
import beast.app.util.TreeFile;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;

public class TreeFileInputEditor extends FileInputEditor {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Class<?> type() {
		return TreeFile.class;
	}

	public TreeFileInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
		init(input, plugin, itemNr, bExpandOption, bAddButtons, "tree files", "trees");
	}

	protected File newFile(File file) {
		return new TreeFile(file.getPath());
	}

}
