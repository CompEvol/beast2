/*
 * BeautiFrame.java
 *
 * (c) 2002-2005 BEAST Development Core Team
 *
 * This package may be distributed under the
 * Lesser Gnu Public Licence (LGPL)
 */
package beast.app.beauti2;

import beast.app.beauti.*;
import beast.app.beauti2.menus.BeautiFileMenuHandler;
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
import java.util.Arrays;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: BeautiFrame.java,v 1.22 2006/09/09 16:07:06 rambaut Exp $
 */
public class BeautiFrame extends DocumentFrame implements BeautiDocListener, BeautiFileMenuHandler {

    private static final long serialVersionUID = 1L;

    private JFileChooser importChooser; // make JFileChooser chooser remember previous path

    /**
     * File extension for Beast specifications
     */
    static public final String FILE_EXT = ".xml";

    /** document in document-view pattern. BTW this class is the view */
    private BeautiDoc doc;

    private JTabbedPane tabbedPane =  new JTabbedPane();
    private TemplatePanel templatePanel;

    private boolean [] isPaneVisible;
    private BeautiPanel [] panels;

    /** flag indicating beauti is in the process of being set up and panels should not sync with current model **/
    private boolean isInitialising = true;

    public BeautiFrame(String title, BeautiDoc doc) throws Exception {
        super();

        setTitle(title);

		isPaneVisible = new boolean[ BeautiConfig.g_panels.size()];
		Arrays.fill(isPaneVisible, true);
		//m_panels = new BeautiPanel[NR_OF_PANELS];
		this.doc = doc;
		this.doc.addBeautiDocListener(this);

	    // set the import action (this will mean an 'import' menu option will be created in the File menu).
        setImportAction(importAction);

        getFindAction().setEnabled(false);
        // probably some other actions to disable

        templatePanel = new TemplatePanel(this, doc);

        setUpPanels();

		tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Component comp = tabbedPane.getSelectedComponent();
                if (comp != null && comp instanceof BeautiPanel) {
                    if (!isInitialising) {
                        ((BeautiPanel)comp).config.sync(((BeautiPanel)comp).iPartition);
                    }
                    refreshPanel();
                }
            }
        });
		
		refreshPanel();

		setIconImage(BeautiPanel.getIcon(0, null).getImage());

		//JMenuBar menuBar = beauti.makeMenuBar(); 
		//frame.setJMenuBar(menuBar);
		
//		if (doc.sFileName != null || doc.alignments.size()> 0) {
//			beauti.a_save.setEnabled(true);
//			beauti.a_saveas.setEnabled(true);
//		}
		
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
        basePanel.add(tabbedPane, BorderLayout.CENTER);

        add(basePanel, BorderLayout.CENTER);

        importChooser = new JFileChooser(Utils.getCWD());

        importChooser.setMultiSelectionEnabled(true);
        importChooser.setFileFilter(new FileNameExtensionFilter(
                "NEXUS (*.nex) & BEAST (*.xml) Files", "nex", "nexus", "nx", "xml", "beast", "fa", "fasta", "afa"));
        importChooser.setDialogTitle("Import Data...");

        setSize(1024, 768);
    }

    public void setUpPanels() throws Exception {
    	isInitialising = true;
    	// remove any existing tabs
    	if (tabbedPane.getTabCount() > 0) {
	    	while (tabbedPane.getTabCount() > 0) {
	    		tabbedPane.removeTabAt(0);
	    	}
			isPaneVisible = new boolean[ BeautiConfig.g_panels.size()];
			Arrays.fill(isPaneVisible, true);
    	}
		for (int iPanel = 0; iPanel < BeautiConfig.g_panels.size(); iPanel++) {
			BeautiPanelConfig panelConfig = BeautiConfig.g_panels.get(iPanel);
			isPaneVisible[iPanel] = panelConfig.bIsVisibleInput.get();
		}
        // add the special Template panel:
        tabbedPane.addTab("Templates", templatePanel);
        tabbedPane.setToolTipTextAt(0, "Select from available analysis templates");

    	// add panels according to BeautiConfig 
		panels = new BeautiPanel[ BeautiConfig.g_panels.size()];
		for (int iPanel = 0; iPanel < BeautiConfig.g_panels.size(); iPanel++) {
			BeautiPanelConfig panelConfig = BeautiConfig.g_panels.get(iPanel);
			panels[ iPanel] = new BeautiPanel( iPanel, this.doc, panelConfig);
			tabbedPane.addTab(BeautiConfig.getButtonLabel(this, panelConfig.getName()), null, panels[ iPanel], panelConfig.getTipText());
		}
		
		for (int iPanel = BeautiConfig.g_panels.size() - 1; iPanel >= 0; iPanel--) {
			if (!isPaneVisible[iPanel]) {
				tabbedPane.removeTabAt(iPanel);
			}
		}
    	isInitialising = false;
    }

    public void refreshPanel() {
		try {
			Component panel = tabbedPane.getSelectedComponent();
			if (panel != null && panel instanceof BeautiPanel) {
				this.doc.determinePartitions();
				((BeautiPanel)panel).updateList();
				((BeautiPanel)panel).refreshPanel();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
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
    	doc.sFileName = file.getAbsolutePath();
		try {
			doc.loadXML(doc.sFileName);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
        return true;
    }

    // Write the document data to the specified file (return true if successfully saved)
    protected boolean writeToFile(File file) throws IOException {
        if (!doc.validateModel()) {
            return false;
        }
        try {
            Component comp = tabbedPane.getSelectedComponent();
            if (comp != null && comp instanceof BeautiPanel) {
                ((BeautiPanel)comp).config.sync(((BeautiPanel)comp).iPartition);
        	} else {
        		panels[0].config.sync(0);
        	}
            doc.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
							doc.importNexus(sFileName);
					}
					if (sFileName.toLowerCase().endsWith(".xml")) {
						doc.importXMLAlignment(sFileName);
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

    @Override
    public void docHasChanged() throws Exception{
        setUpPanels();
//        setUpViewMenu();
//        setTitle();
    }

    @Override
    public Action getAddonManagerAction() {
        return addonManagerAction;
    }

    protected AbstractAction addonManagerAction = new AbstractAction(BeautiFileMenuHandler.ADD_ON_MANAGER) {
        public void actionPerformed(java.awt.event.ActionEvent ae) {
        	JAddOnDialog dlg = new JAddOnDialog(BeautiFrame.this);
        	dlg.setVisible(true);

        	// refresh template menu item
//        	templateMenu.removeAll();
//    		List<AbstractAction> templateActions = getTemplateActions();
//    		for (AbstractAction a: templateActions) {
//    			templateMenu.add(a);
//    		}
//    		templateMenu.addSeparator();
//    		templateMenu.add(a_template);

        } // actionPerformed
    };

}
