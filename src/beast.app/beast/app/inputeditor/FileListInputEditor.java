package beast.app.inputeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import beast.app.inputeditor.BeautiDoc;
import beast.app.inputeditor.ListInputEditor;
import beast.app.treeannotator.FileDrop;
import beast.app.util.Utils;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.ProgramStatus;
import jam.panels.ActionPanel;
import jam.table.TableRenderer;


/** for opening files for reading
 * use OutFile when you need a file for writing
 */
public class FileListInputEditor extends ListInputEditor {

	final static String SEPARATOR = Utils.isWindows() ? "\\\\" : "/";
	
	private static final long serialVersionUID = 1L;

	JTable filesTable = null;
    private FilesTableModel filesTableModel = null;
    private List<File> files;

	@Override
	public Class<?> type() {
		return List.class;
	}
	
    @Override
    public Class<?> baseType() {
		return File.class;
    }


	public FileListInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_bExpandOption = isExpandOption;
        m_input = input;
        m_beastObject = beastObject;
		// super.init(input, beastObject, itemNr, isExpandOption, addButtons);
		
		Object o = input.get();
		if (o instanceof List) {
			files = (List<File>) o;
		}
		add(fileListPanel());
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
		m_input.setValue(file, m_beastObject);	
   	}
	

	static File getDefaultFile(File file) {
		File defaultFile;
		if (file.exists()) {
			defaultFile = file;
			if (defaultFile.getParent() == null) {
				defaultFile = new File(ProgramStatus.g_sDir);
				if (defaultFile.isDirectory()) {
					defaultFile = new File(ProgramStatus.g_sDir + FileListInputEditor.SEPARATOR + file.getName());
				} else {
					defaultFile = new File(new File(ProgramStatus.g_sDir).getParent() + FileListInputEditor.SEPARATOR + file.getName());
				}
			}
		} else {
			defaultFile = new File(ProgramStatus.g_sDir);
			if (defaultFile.isDirectory()) {
				defaultFile = new File(ProgramStatus.g_sDir + FileListInputEditor.SEPARATOR + file.getName());
			} else {
				defaultFile = new File(new File(ProgramStatus.g_sDir).getParent() + FileListInputEditor.SEPARATOR + file.getName());
			}
		}
		return defaultFile;
	}
	
	/** to be overridded by file editors that produce specific file types **/
	protected File newFile(File file) {
		return file;
	}

	
	


    public JPanel fileListPanel() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // Taxon Sets
        filesTableModel = new FilesTableModel();
        filesTable = new JTable(filesTableModel);

        filesTable.getColumnModel().getColumn(0).setCellRenderer(
                new TableRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));
        filesTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        //filesTable.getColumnModel().getColumn(0).setPreferredWidth(80);

        // This causes superfluous TabelModel.setValue events to fire.
        // Is this still needed?  I guess we'll see...
        //TableEditorStopper.ensureEditingStopWhenTableLosesFocus(filesTable);

        filesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
			public void valueChanged(ListSelectionEvent evt) {
                filesTableSelectionChanged();
            }
        });

        JScrollPane scrollPane1 = new JScrollPane(filesTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        //scrollPane1.setMaximumSize(new Dimension(10000, 10));
        scrollPane1.setPreferredSize(new Dimension(500, 285));

        ActionPanel actionPanel1 = new ActionPanel(false);
        actionPanel1.setAddAction(addFileAction);
        actionPanel1.setRemoveAction(removeFileAction);
        removeFileAction.setEnabled(false);

        JPanel controlPanel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel1.add(actionPanel1);

        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JLabel label = new JLabel(formatName(m_input.getName()) + ":");
        label.setToolTipText(m_input.getTipText());
        panel.add(label, BorderLayout.NORTH);
        panel.add(scrollPane1, BorderLayout.CENTER);
        panel.add(actionPanel1, BorderLayout.SOUTH);
        panel.setToolTipText(m_input.getTipText());

        Color focusColor = UIManager.getColor("Focus.color");
        Border focusBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, focusColor);
        new FileDrop(null, scrollPane1, focusBorder, new FileDrop.Listener() {
            @Override
			public void filesDropped(java.io.File[] files) {
                addFiles(files);
            }   // end filesDropped
        }); // end FileDrop.Listener
        
        return panel;
    }


    private void filesTableSelectionChanged() {
        if (filesTable.getSelectedRowCount() == 0) {
            removeFileAction.setEnabled(false);
        } else {
            removeFileAction.setEnabled(true);
        }
    }

    private void addFiles(File[] fileArray) {
        int sel1 = files.size();
        for (File file : fileArray) {        	
			try {
	        	File File = (File) baseType().getConstructor(String.class).newInstance(file.getAbsolutePath());
	            files.add(File);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			};

            String fileName = file.getAbsolutePath();
            if (fileName.lastIndexOf(File.separator) > 0) {
            	ProgramStatus.setCurrentDir(fileName.substring(0, fileName.lastIndexOf(File.separator)));
            }
        }

        filesTableModel.fireTableDataChanged();

        int sel2 = files.size() - 1;
        filesTable.setRowSelectionInterval(sel1, sel2);

    }

    Action addFileAction = new AbstractAction("+") {
        private static final long serialVersionUID = 7602227478402204088L;

        @Override
		public void actionPerformed(ActionEvent ae) {
            File[] files = Utils.getLoadFiles("Select file", new File(ProgramStatus.g_sDir), "XML, trace or tree log files", "log", "trees", "xml");
            if (files != null) {
                addFiles(files);
            }
        }
    };

    Action removeFileAction = new AbstractAction("-") {
        private static final long serialVersionUID = 5934278375005327047L;

        @Override
		public void actionPerformed(ActionEvent ae) {
            int row = filesTable.getSelectedRow();
            if (row != -1) {
                files.remove(row);
            }

            filesTableModel.fireTableDataChanged();

            if (row >= files.size()) row = files.size() - 1;
            if (row >= 0) {
                filesTable.setRowSelectionInterval(row, row);
            }
        }
    };


    class FilesTableModel extends AbstractTableModel {
        /**
         *
         */
        private static final long serialVersionUID = 4153326364833213013L;
        private final String[] columns = {"File"};//, "Burnin (percentage)"};

        public FilesTableModel() {
        }

        @Override
		public int getColumnCount() {
            return columns.length;
        }

        @Override
		public int getRowCount() {
        	if (files == null) {
        		return 0;
        	}
            return files.size();
        }

        @Override
		public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
            	if (rowIndex < files.size()) {
                    return files.get(rowIndex).getName();
            	}
            }
            return null;
        }

        @Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
            return (columnIndex == 1);
        }

        /**
         * This empty implementation is provided so users don't have to implement
         * this method if their data model is not editable.
         *
         * @param aValue      value to assign to cell
         * @param rowIndex    row of cell
         * @param columnIndex column of cell
         */
        @Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }

        @Override
		public String getColumnName(int columnIndex) {
            return columns[columnIndex];
        }

        @Override
		public Class<?> getColumnClass(int columnIndex) {
            return baseType(); // getValueAt(0, columnIndex).getClass();
        }
    }

//    class FileInfo {
//        File file;
//        Integer burnin;
//    }

}
