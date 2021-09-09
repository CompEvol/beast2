package beast.app.inputeditor;




import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;

import beast.app.inputeditor.BeautiDoc;
import beast.app.inputeditor.InputEditor;
import beast.app.util.OutFile;
import beast.app.util.Utils;
import beast.base.BEASTInterface;
import beast.base.Input;
import beast.base.ProgramStatus;

public class OutFileInputEditor extends InputEditor.Base {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Class<?> type() {
		return OutFile.class;
	}

	public OutFileInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
		super.init(input, plugin, itemNr, bExpandOption, bAddButtons);
		if (input.get() == null) {
			m_entry.setText("[[none]]");
		} else {
			m_entry.setText(((File) m_input.get()).getName());
		}
		
		JButton button = new JButton("browse");
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				File defaultFile = FileInputEditor.getDefaultFile((File) m_input.get());
				File file = Utils.getSaveFile(m_input.getTipText(), defaultFile, "All files", Utils.isWindows() ? "*" : "");
				if (file != null) 
					file = new OutFile(file.getPath());
				try {
					m_entry.setText(file.getName());
					m_input.setValue(new OutFile(file.getPath()), m_beastObject);
					String path = file.getPath();
					ProgramStatus.setCurrentDir(path);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		add(button);
	}
	
}
