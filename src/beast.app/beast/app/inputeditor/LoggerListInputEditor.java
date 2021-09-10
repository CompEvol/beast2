package beast.app.inputeditor;

import java.awt.Dimension;
import java.util.List;

import javax.swing.JTextField;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.Logger;



public class LoggerListInputEditor extends ListInputEditor {
    private static final long serialVersionUID = 1L;

	public LoggerListInputEditor(BeautiDoc doc) {
		super(doc);
	}

    @Override
    public Class<?> type() {
        return List.class;
    }

    @Override
    public Class<?> baseType() {
        return Logger.class;
    }
    

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
    	super.init(input, beastObject, itemNr, isExpandOption, addButtons);
    }
    
    @Override
    protected void addSingleItem(BEASTInterface beastObject) {
    	currentLogger = (Logger) beastObject;
    	super.addSingleItem(beastObject);
    }
    
    public InputEditor createFileNameEditor() {
        final Input<?> input = currentLogger.fileNameInput;
        StringInputEditor fileNameEditor = new StringInputEditor(doc);
        fileNameEditor.init(input, currentLogger, -1, ExpandOption.FALSE, true);

        // ensure file name entry has larger size than the standard size
        JTextField fileNameEntry = fileNameEditor.getEntry();
        Dimension size = new Dimension(400, fileNameEntry.getPreferredSize().height);
        fileNameEntry.setMinimumSize(size);
        fileNameEntry.setPreferredSize(size);
        return fileNameEditor;
    }
    
    Logger currentLogger;
}
