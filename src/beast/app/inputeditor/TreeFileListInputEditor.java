package beast.app.inputeditor;

import beast.app.inputeditor.BeautiDoc;
import beast.app.util.TreeFile;

public class TreeFileListInputEditor extends FileListInputEditor {
	private static final long serialVersionUID = 1L;

	public TreeFileListInputEditor(BeautiDoc doc) {
		super(doc);
	}
	
    @Override
    public Class<?> baseType() {
		return TreeFile.class;
    }
}
