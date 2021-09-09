package beast.app.inputeditor;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;

import beast.app.inputeditor.BeautiDoc;
import beast.app.inputeditor.InputEditor;
import beast.app.util.Utils;
import beast.base.BEASTInterface;
import beast.base.Input;
import beast.base.ProgramStatus;


/** for opening files for reading
 * use OutFile when you need a file for writing
 */
public class FileInputEditor extends InputEditor.Base {
	final static String SEPARATOR = Utils.isWindows() ? "\\\\" : "/";
	
	private static final long serialVersionUID = 1L;

	@Override
	public Class<?> type() {
		return File.class;
	}

	public FileInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
		init(input, plugin, itemNr, bExpandOption, bAddButtons, "All files", Utils.isWindows() ? "*" : "");
	}
	
	protected void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons, String fileDescription, String fileType) {
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
				File defaultFile = getDefaultFile((File) m_input.get());
				File file = Utils.getLoadFile(m_input.getTipText(), defaultFile, "All files", Utils.isWindows() ? "*" : "");
				try {
					if (file != null) {	
						m_entry.setText(file.getName());
						m_input.setValue(newFile(file), m_beastObject);
						String path = file.getPath();
						ProgramStatus.setCurrentDir(path);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		add(button);
	}

	@Override
	protected void setValue(Object o) {
		String file = o.toString();
		if (file.equals("")) {
			return;
		}
		String fileSep = System.getProperty("file.separator");
		String origFile = null;
		try {
			origFile = ((File) m_input.get()).getAbsolutePath();
		} catch (Exception e) {
			origFile = null;
		}
		if (origFile != null && origFile.indexOf(fileSep) >= 0 && file.indexOf(fileSep) < 0) {
			if (origFile.contains(origFile)) {
				file = origFile.substring(0, origFile.lastIndexOf(fileSep) + 1) + file;
			}
		}
//		if (file.equals("[[none]]")) {
//			m_input.setValue(null, m_beastObject);
//		} else {
			m_input.setValue(file, m_beastObject);	
//		}
   	}
	

	static File getDefaultFile(File file) {
		File defaultFile;
		if (file.exists()) {
			defaultFile = file;
			if (defaultFile.getParent() == null) {
				defaultFile = new File(ProgramStatus.g_sDir);
				if (defaultFile.isDirectory()) {
					defaultFile = new File(ProgramStatus.g_sDir + FileInputEditor.SEPARATOR + file.getName());
				} else {
					defaultFile = new File(new File(ProgramStatus.g_sDir).getParent() + FileInputEditor.SEPARATOR + file.getName());
				}
			}
		} else {
			defaultFile = new File(ProgramStatus.g_sDir);
			if (defaultFile.isDirectory()) {
				defaultFile = new File(ProgramStatus.g_sDir + FileInputEditor.SEPARATOR + file.getName());
			} else {
				defaultFile = new File(new File(ProgramStatus.g_sDir).getParent() + FileInputEditor.SEPARATOR + file.getName());
			}
		}
		return defaultFile;
	}
	
	/** to be overridded by file editors that produce specific file types **/
	protected File newFile(File file) {
		return file;
	}

}
