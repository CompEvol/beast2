/*
 * BeautiFrame.java
 *
 * (c) 2002-2005 BEAST Development Core Team
 *
 * This package may be distributed under the
 * Lesser Gnu Public Licence (LGPL)
 */
package beast.app.beauti2;

import beast.app.beauti.Beauti;
import beast.app.beauti.BeautiDoc;
import beast.app.beauti.BeautiPanel;
import beast.app.util.Utils;
import jam.framework.DocumentFrame;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.BorderUIResource;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: BeautiFrame.java,v 1.22 2006/09/09 16:07:06 rambaut Exp $
 */
public class BeautiFrame extends DocumentFrame {

    private static final long serialVersionUID = 2114148696789612509L;

    private JFileChooser importChooser; // make JFileChooser chooser remember previous path

    Beauti beauti;
    
    public BeautiFrame(String title, BeautiDoc doc) throws Exception {
        super();

        setTitle(title);

        // set the import action (this will mean an 'import' menu option will be created in the File menu).
        setImportAction(importAction);

        getFindAction().setEnabled(false);
        // probably some other actions to disable
        
        beauti = new Beauti(doc);
        beauti.setUpPanels();
		
		beauti.currentTab = beauti.panels[0];
		beauti.hidePanels();

		beauti.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (beauti.currentTab == null) {
					beauti.currentTab = beauti.panels[0];
				}
				if (beauti.currentTab != null) {
					if (!beauti.isInitialising) {
						beauti.currentTab.config.sync(beauti.currentTab.iPartition);
					}
					BeautiPanel panel = (BeautiPanel) beauti.getSelectedComponent();
					beauti.currentTab = panel;
					beauti.refreshPanel();
				}
			}
		});
		
		
		
		beauti.setVisible(true);
		beauti.refreshPanel();
		JFrame frame = this;//new JFrame("BEAUti 2: " + doc.sTemplateName + " " + doc.sFileName);
		beauti.frame = frame;
		frame.setIconImage(BeautiPanel.getIcon(0, null).getImage());

		//JMenuBar menuBar = beauti.makeMenuBar(); 
		//frame.setJMenuBar(menuBar);
		
//		if (doc.sFileName != null || doc.alignments.size()> 0) {
//			beauti.a_save.setEnabled(true);
//			beauti.a_saveas.setEnabled(true);
//		}
		
		add(beauti);
        setSize(1024, 768);
        setVisible(true);

        // check file needs to be save on closing main frame
//        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//		frame.addWindowListener(new WindowAdapter() {
//		    public void windowClosing(WindowEvent e) {
//	        	if (!beauti.quit()) {
//		    		return;
//		    	}
//	        	System.exit(0);
//		    }
//		});
    }

    /**
     * Initialize all the UI components here
     */
    public void initializeComponents() {

        JPanel basePanel = new JPanel(new BorderLayout(6, 6));
        basePanel.setBorder(new BorderUIResource.EmptyBorderUIResource(new java.awt.Insets(12, 12, 12, 12)));

        add(basePanel, BorderLayout.CENTER);

        importChooser = new JFileChooser(Utils.getCWD());

        importChooser.setMultiSelectionEnabled(true);
        importChooser.setFileFilter(new FileNameExtensionFilter(
                "NEXUS (*.nex) & BEAST (*.xml) Files", "nex", "nexus", "nx", "xml", "beast", "fa", "fasta", "afa"));
        importChooser.setDialogTitle("Import Data...");
    }

    /**
     * Called if the select all menu option or hot key is selected. Expected behaviour might be to select
     * all the rows in a currently focused table.
     */
    public void doSelectAll() {
    }

    /**
     * Called if the delete menu option or hot key is selected.
     */
    public void doDelete() {
    }

    // Read the document from the provide file (return true if successfully loaded).
    protected boolean readFromFile(File file) throws IOException {
    	beauti.doc.sFileName = file.getAbsolutePath();
		try {
			beauti.doc.loadXML(beauti.doc.sFileName);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
        return true;
    }

    // Write the document data to the specified file (return true if successfully saved)
    protected boolean writeToFile(File file) throws IOException {
    	beauti.doc.sFileName = file.getAbsolutePath();
        if (!beauti.doc.validateModel()) {
            return false;
        }
        beauti.saveFile(beauti.doc.sFileName);
        return true;
    }

    public final void doImport() {
        int returnVal = importChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = importChooser.getSelectedFiles();
            importFiles(files);
        }
    }

    /**
     * Called by the Import menu option to import non-native file formats.
     * @param files
     */
    private void importFiles(File[] files) {
        for (File file : files) {
            if (file == null || file.getName().equals("")) {
                JOptionPane.showMessageDialog(this, "Invalid file name",
                        "Invalid file name", JOptionPane.ERROR_MESSAGE);
            } else {
    			try {
					String sFileName = file.getAbsolutePath();
					if (sFileName.lastIndexOf('/') > 0) {
						Beauti.g_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
					}
					if (sFileName.toLowerCase().endsWith(".nex") || sFileName.toLowerCase().endsWith(".nxs")) {
							beauti.doc.importNexus(sFileName);
					}
					if (sFileName.toLowerCase().endsWith(".xml")) {
						beauti.doc.importXMLAlignment(sFileName);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//                try {
//                    BEAUTiImporter beautiImporter = new BEAUTiImporter(this);
//                    beautiImporter.importFromFile(file);
//
//                    setDirty();
////                    } catch (FileNotFoundException fnfe) {
////                        JOptionPane.showMessageDialog(this, "Unable to open file: File not found",
////                                "Unable to open file", JOptionPane.ERROR_MESSAGE);
//                } catch (IOException ioe) {
//                    JOptionPane.showMessageDialog(this, "File I/O Error unable to read file: " + ioe.getMessage(),
//                            "Unable to read file", JOptionPane.ERROR_MESSAGE);
//                    ioe.printStackTrace();
//                    // there may be other files in the list so don't return
////                    return;
//
//                } catch (NexusImporter.MissingBlockException ex) {
//                    JOptionPane.showMessageDialog(this, "TAXON, DATA or CHARACTERS block is missing in Nexus file: " + ex,
//                            "Missing Block in Nexus File",
//                            JOptionPane.ERROR_MESSAGE);
//                    ex.printStackTrace();
//
//                } catch (ImportException ime) {
//                    JOptionPane.showMessageDialog(this, "Error parsing imported file: " + ime,
//                            "Error reading file",
//                            JOptionPane.ERROR_MESSAGE);
//                    ime.printStackTrace();
//                    // there may be other files in the list so don't return
////                    return;
//                } catch (IllegalArgumentException illegEx) {
//                    JOptionPane.showMessageDialog(this, illegEx.getMessage(),
//                            "Illegal Argument Exception", JOptionPane.ERROR_MESSAGE);
//
//                } catch (Exception ex) {
//                    JOptionPane.showMessageDialog(this, "Fatal exception: " + ex,
//                            "Error reading file",
//                            JOptionPane.ERROR_MESSAGE);
//                    ex.printStackTrace();
//                    return;
//                }
            }
        }

//        setAllOptions();
    }

    /**
     * return the currently selected component that could be used to export data via the clipboard
     * @return
     */
    public JComponent getExportableComponent() {

        JComponent exportable = null;
//        Component comp = tabbedPane.getSelectedComponent();
//
//        if (comp instanceof Exportable) {
//            exportable = ((Exportable) comp).getExportableComponent();
//        } else if (comp instanceof JComponent) {
//            exportable = (JComponent) comp;
//        }

        return exportable;
    }

    public Action getImportAction() {
        return importAction;
    }

    protected AbstractAction importAction = new AbstractAction("Import Data...") {
        private static final long serialVersionUID = 3217702096314745005L;

        public void actionPerformed(java.awt.event.ActionEvent ae) {
            doImport();
        }
    };
}
