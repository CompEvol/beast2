package beast.app.beauti;






import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import beast.app.beastapp.BeastVersion;
import beast.app.beauti.BeautiDoc.ActionOnExit;
import beast.app.draw.HelpBrowser;
import beast.app.draw.ModelBuilder;
import beast.app.draw.MyAction;
import beast.app.draw.ExtensionFileFilter;
import beast.app.draw.PluginPanel;
import beast.app.util.Utils;
import beast.util.AddOnManager;


public class Beauti extends JTabbedPane {
	private static final long serialVersionUID = 1L;
	
    ExtensionFileFilter ef0 = new ExtensionFileFilter(".nex", "Nexus files");
    ExtensionFileFilter ef1 = new ExtensionFileFilter(".xml", "BEAST files");
	
    /**
     * name of current file, used for saving (as opposed to saveAs) *
     */
    String sFileName = "";
    /**
     * current directory for opening files *
     */
    public static String g_sDir = System.getProperty("user.dir");
    /**
     * File extension for Beast specifications
     */
    static public final String FILE_EXT = ".xml";

	/** document in document-view pattern. BTW this class is the view */
    BeautiDoc doc;
    JFrame frame;
    
    /** currently selected tab **/
    BeautiPanel currentTab;
    
	boolean [] bPaneIsVisible;
	BeautiPanel [] panels;

	/** menu for switching templates **/
    JMenu templateMenu;
	/** menu for making showing/hiding tabs **/
    JMenu viewMenu;

	
	public Beauti(BeautiDoc doc) {
		bPaneIsVisible = new boolean[ BeautiConfig.g_panels.size()];
		Arrays.fill(bPaneIsVisible, true);
		//m_panels = new BeautiPanel[NR_OF_PANELS];
		this.doc = doc;
		this.doc.setBeauti(this);
	}
	
	void setTitle() {
		frame.setTitle("Beauti II: " + this.doc.sTemplateName + " " + sFileName);
	}
	
	void toggleVisible(int nPanelNr) {
		if (bPaneIsVisible[nPanelNr]) {
			bPaneIsVisible[nPanelNr] = false;
			int nTabNr = tabNrForPanel(nPanelNr);
			removeTabAt(nTabNr);
		} else {
			bPaneIsVisible[nPanelNr] = true;
			int nTabNr = tabNrForPanel(nPanelNr);
				BeautiPanelConfig panel = BeautiConfig.g_panels.get(nPanelNr);
				insertTab(BeautiConfig.getButtonLabel(this, panel.sNameInput.get()), null, panels[nPanelNr], panel.sTipTextInput.get(), nTabNr);
//			}
			setSelectedIndex(nTabNr);
		}
	}
	
	int tabNrForPanel(int nPanelNr) {
		int k = 0;
		for (int i = 0; i < nPanelNr; i++) {
			if (bPaneIsVisible[i]) {
				k++;
			}
		}
		return k;
	}

	
    Action a_new = new ActionNew();
    Action a_load = new ActionLoad();
    Action a_template = new ActionTemplate();
    Action a_addOn = new ActionAddOn();
    Action a_import = new ActionImport();
    Action a_save = new ActionSave();
    Action a_saveas = new ActionSaveAs();
    Action a_quit = new ActionQuit();
    Action a_viewall = new ActionViewAllPanels();
    
    Action a_help = new ActionHelp();
    Action a_citation = new ActionCitation();
    Action a_about = new ActionAbout();
    Action a_viewModel = new ActionViewModel();

    class ActionSave extends MyAction {
        private static final long serialVersionUID = 1;

        public ActionSave() {
            super("Save", "Save Model", "save", "ctrl S");
            setEnabled(false);
        } // c'tor

        public ActionSave(String sName, String sToolTipText, String sIcon,
                          String sAcceleratorKey) {
            super(sName, sToolTipText, sIcon, sAcceleratorKey);
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
            if (!sFileName.equals("")) {
                if (!doc.validateModel()) {
                    return;
                }
                saveFile(sFileName);
//                m_doc.isSaved();
            } else {
                if (saveAs()) {
//                    m_doc.isSaved();
                }
            }
        } // actionPerformed

        boolean saveAs() {
            if (!doc.validateModel()) {
                return false;
            }
            File file = beast.app.util.Utils.getSaveFile("Save Model As", new File(sFileName), null, (String[]) null);
//            JFileChooser fc = new JFileChooser(g_sDir);
//            fc.addChoosableFileFilter(ef1);
//            fc.setDialogTitle("Save Model As");
//            if (!sFileName.equals("")) {
//                // can happen on actionQuit
//                fc.setSelectedFile(new File(sFileName));
//            }
//            int rval = fc.showSaveDialog(null);
//
//            if (rval == JFileChooser.APPROVE_OPTION) {
            if (file != null) {
                // System.out.println("Saving to file \""+
                // f.getAbsoluteFile().toString()+"\"");
                sFileName = file.getAbsolutePath();//fc.getSelectedFile().toString();
                if (sFileName.lastIndexOf('/') > 0) {
                    g_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
                }
                if (!sFileName.endsWith(FILE_EXT))
                	sFileName = sFileName.concat(FILE_EXT);
                saveFile(sFileName);
                setTitle();
                return true;
            }
            return false;
        } // saveAs    

        protected void saveFile(String sFileName) {
            try {
            	if (currentTab != null) {
            		currentTab.config.sync(currentTab.iPartition);
            	} else {
            		panels[0].config.sync(0);
            	}
                doc.save(sFileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } // saveFile
        
        
    } // class ActionSave
    class ActionSaveAs extends ActionSave {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -20389110859354L;

        public ActionSaveAs() {
            super("Save As", "Save Model As", "saveas", "");
            setEnabled(false);
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
            saveAs();
        } // actionPerformed
    } // class ActionSaveAs    

    class ActionNew extends MyAction {
        private static final long serialVersionUID = 1;

        public ActionNew() {
            super("New", "Start new analysis", "new", "ctrl N");
        } // c'tor
        
        public void actionPerformed(ActionEvent ae) {
        	doc.newAnalysis();
			a_save.setEnabled(false);
			a_saveas.setEnabled(false);
        }
    }
    
    class ActionLoad extends MyAction {
        private static final long serialVersionUID = 1;

        public ActionLoad() {
            super("Load", "Load Beast File", "open", "ctrl O");
        } // c'tor

        public ActionLoad(String sName, String sToolTipText, String sIcon,
                          String sAcceleratorKey) {
            super(sName, sToolTipText, sIcon, sAcceleratorKey);
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
        	File file = beast.app.util.Utils.getLoadFile("Load Beast XML File");
//    		JFileChooser fileChooser = new JFileChooser(g_sDir);
//    		fileChooser.addChoosableFileFilter(ef1);
//    		fileChooser.setDialogTitle("Load Beast XML File");
//    		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//        		sFileName = fileChooser.getSelectedFile().toString();
        	if (file != null) {
        		sFileName = file.getAbsolutePath();
                if (sFileName.lastIndexOf('/') > 0) {
                    g_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
                }
    			try {
    				doc.loadXML(sFileName);
    				a_save.setEnabled(true);
    				a_saveas.setEnabled(true);
    				setTitle();
    			} catch (Exception e) {
    				e.printStackTrace();
					JOptionPane.showMessageDialog(null, "Something went wrong loading the file: " + e.getMessage());
				}
            }
        } // actionPerformed
    }

    class ActionTemplate extends MyAction {
        private static final long serialVersionUID = 1;

        public ActionTemplate() {
            super("Other Template", "Load Beast Analysis Template From File", "template", "");
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
        	File file = beast.app.util.Utils.getLoadFile("Load Template XML File");
//    		JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir")+"/templates");
//    		fileChooser.addChoosableFileFilter(ef1);
//    		fileChooser.setDialogTitle("Load Template XML File");
//    		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//    			String sFileName = fileChooser.getSelectedFile().toString();
        	if (file != null) {
        		String sFileName = file.getAbsolutePath();
    			try {
    				doc.loadNewTemplate(sFileName);
    			} catch (Exception e) {
    				e.printStackTrace();
					JOptionPane.showMessageDialog(null, "Something went wrong loading the template: " + e.getMessage());
				}
            }
        } // actionPerformed
    } // ActionTemplate

    
    class ActionAddOn extends MyAction {
        private static final long serialVersionUID = 1;

        public ActionAddOn() {
            super("Manage Add-ons", "Manage Add-ons", "addon", "");
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
        	JAddOnDialog dlg = new JAddOnDialog(frame);
        	dlg.setVisible(true);
        	// refresh template menu item
        	templateMenu.removeAll();
    		List<AbstractAction> templateActions = getTemplateActions();
    		for (AbstractAction a: templateActions) {
    			templateMenu.add(a);
    		}
    		templateMenu.addSeparator();
    		templateMenu.add(a_template);

        } // actionPerformed
    }

    class ActionImport extends MyAction {
        private static final long serialVersionUID = 1;

        public ActionImport() {
            super("Import Alignment", "Import Alignment File", "import", "ctrl I");
        } // c'tor

        public ActionImport(String sName, String sToolTipText, String sIcon,
                          String sAcceleratorKey) {
            super(sName, sToolTipText, sIcon, sAcceleratorKey);
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
    		JFileChooser fileChooser = new JFileChooser(g_sDir);
    		fileChooser.addChoosableFileFilter(ef1);
    		fileChooser.addChoosableFileFilter(ef0);
    		fileChooser.setMultiSelectionEnabled(true);
    		fileChooser.setDialogTitle("Load Beast XML File");

    		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
    			try {
        			File[] files = fileChooser.getSelectedFiles();
        			for (int i = 0; i < files.length; i++) {
        				String sFileName = files[i].getAbsolutePath();
        				if (sFileName.lastIndexOf('/') > 0) {
        					Beauti.g_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
        				}
        				if (sFileName.toLowerCase().endsWith(".nex") || sFileName.toLowerCase().endsWith(".nxs")) {
                			doc.importNexus(sFileName);
        				}
        				if (sFileName.toLowerCase().endsWith(".xml")) {
                			doc.importXMLAlignment(sFileName);
        				}
        			}
    				a_save.setEnabled(true);
    				a_saveas.setEnabled(true);
    			} catch (Exception e) {
    				e.printStackTrace();
					JOptionPane.showMessageDialog(null, "Something went wrong importing the alignment: " + e.getMessage());
				}
            }
        } // actionPerformed
    }
    
    class ActionQuit extends ActionSave {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -2038911085935515L;

        public ActionQuit() {
            super("Exit", "Exit Program", "exit", "alt F4");
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
//            if (!m_doc.m_bIsSaved) {
        	if (doc.validateModel()) {
                int result = JOptionPane.showConfirmDialog(null,
                        "Do you want to save the Beast specification?",
                        "Save before closing?",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.CANCEL_OPTION) {
                    return;
                }
                if (result == JOptionPane.YES_OPTION) {
                    if (!saveAs()) {
                        return;
                    }
                }
            }
            System.exit(0);
        }
    } // class ActionQuit

    ViewPanelCheckBoxMenuItem [] m_viewPanelCheckBoxMenuItems;
    
    class ViewPanelCheckBoxMenuItem extends JCheckBoxMenuItem{
		private static final long serialVersionUID = 1L;
		int m_iPanel;
		
    	ViewPanelCheckBoxMenuItem(int iPanel) {
    		super("Show " +	BeautiConfig.g_panels.get(iPanel).sNameInput.get() + " panel", 
    				BeautiConfig.g_panels.get(iPanel).bIsVisibleInput.get());
    		m_iPanel = iPanel;
    		if (m_viewPanelCheckBoxMenuItems == null) {
    			m_viewPanelCheckBoxMenuItems = new ViewPanelCheckBoxMenuItem[ BeautiConfig.g_panels.size()];
    		}
    		m_viewPanelCheckBoxMenuItems[iPanel] = this;
    	} // c'tor
    	
    	void doAction() {
    		toggleVisible(m_iPanel);
    	}
    };

    /** makes all panels visible **/
    class ActionViewAllPanels extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionViewAllPanels() {
            super("View all", "View all panels", "viewall", "");
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
        	for(int nPanelNr = 0; nPanelNr < bPaneIsVisible.length; nPanelNr++) {
        		if (!bPaneIsVisible[nPanelNr]) {
        			toggleVisible(nPanelNr);
        			m_viewPanelCheckBoxMenuItems[nPanelNr].setState(true);
        		}
        	}
        } // actionPerformed
    } // class ActionViewAllPanels

    class ActionAbout extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionAbout() {
            super("About", "Help about", "about", "");
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
        	BeastVersion version = new BeastVersion();
            JOptionPane.showMessageDialog(null, version.getCredits(), "About Beauti 2", JOptionPane.PLAIN_MESSAGE, BeautiPanel.getIcon(0, null));
        }
    } // class ActionAbout

    class ActionHelp extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionHelp() {
            super("Help", "Help on current panel", "help", "");
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            HelpBrowser b = new HelpBrowser(currentTab.config.getType());
            b.setSize(800, 800);
            b.setVisible(true);
            b.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    } // class ActionHelp

    class ActionCitation extends MyAction implements ClipboardOwner {
        private static final long serialVersionUID = -1;

        public ActionCitation() {
            super("Citation", "Show appropriate citations and copy to clipboard", "citation", "");
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
        	String sCitations = doc.mcmc.get().getCitations();
        	try {
	            StringSelection stringSelection = new StringSelection( sCitations );
	            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	            clipboard.setContents(stringSelection, this);
        	} catch (Exception e) {
        		e.printStackTrace();
			}
            JOptionPane.showMessageDialog(null, sCitations + "\nCitations copied to clipboard", "Citation(s) applicable to this model:", JOptionPane.INFORMATION_MESSAGE);
        	
        } // getCitations

		@Override
		public void lostOwnership(Clipboard clipboard, Transferable contents) {
			// do nothing
		}
     } // class ActionAbout

    class ActionViewModel extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionViewModel() {
            super("View model", "View model graph", "model", "");
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
            JFrame frame = new JFrame("Model Builder");
            ModelBuilder modelBuilder = new ModelBuilder();
            modelBuilder.init();
            frame.add(modelBuilder, BorderLayout.CENTER);
            frame.add(modelBuilder.m_jTbTools2, BorderLayout.NORTH);
            modelBuilder.setEditable(false);
            modelBuilder.m_doc.init(doc.mcmc.get());
            modelBuilder.setDrawingFlag();
            frame.setSize(600, 800);
            frame.setVisible(true);
        }
    } // class ActionViewModel
    
    
    void refreshPanel() {
		try {
			BeautiPanel panel = (BeautiPanel) getSelectedComponent();
			if (panel != null) {
				this.doc.determinePartitions();
				panel.updateList();
				panel.refreshPanel();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
    }
    
    public JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);
        fileMenu.add(a_new);
        fileMenu.add(a_load);
        fileMenu.add(a_import);
        fileMenu.addSeparator();
		templateMenu = new JMenu("Template");
		fileMenu.add(templateMenu);
		List<AbstractAction> templateActions = getTemplateActions();
		for (AbstractAction a: templateActions) {
			templateMenu.add(a);
		}
		templateMenu.addSeparator();
		templateMenu.add(a_template);
		fileMenu.add(a_addOn);
        fileMenu.addSeparator();
        fileMenu.add(a_save);
        fileMenu.add(a_saveas);
        fileMenu.addSeparator();
        fileMenu.add(a_quit);
        
        JMenu modeMenu = new JMenu("Mode");
        menuBar.add(modeMenu);
        modeMenu.setMnemonic('M');
        
//		final JCheckBoxMenuItem viewEditTree = new JCheckBoxMenuItem("Expert mode", InputEditor.g_bExpertMode);
//		viewEditTree.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				InputEditor.g_bExpertMode = viewEditTree.getState();
//				refreshPanel();
//			}
//		});
//		modeMenu.add(viewEditTree);
//		modeMenu.addSeparator();
//
//		final JCheckBoxMenuItem autoScrubPriors = new JCheckBoxMenuItem("Automatic scrub priors", this.doc.bAutoScrubPriors);
//		autoScrubPriors.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				doc.bAutoScrubPriors = autoScrubPriors.getState();
//				refreshPanel();
//			}
//		});
//		modeMenu.add(autoScrubPriors);
//		final JCheckBoxMenuItem autoScrubState = new JCheckBoxMenuItem("Automatic scrub state", this.doc.bAutoScrubState);
//		autoScrubState.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				doc.bAutoScrubState = autoScrubState.getState();
//				refreshPanel();
//			}
//		});
//		modeMenu.add(autoScrubState);
//		final JCheckBoxMenuItem autoScrubOperators = new JCheckBoxMenuItem("Automatic scrub operators", this.doc.bAutoScrubOperators);
//		autoScrubOperators.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				doc.bAutoScrubOperators = autoScrubOperators.getState();
//				refreshPanel();
//			}
//		});
//		modeMenu.add(autoScrubOperators);
//		final JCheckBoxMenuItem autoScrubLoggers = new JCheckBoxMenuItem("Automatic scrub loggers", this.doc.bAutoScrubLoggers);
//		autoScrubLoggers.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				doc.bAutoScrubLoggers = autoScrubLoggers.getState();
//				refreshPanel();
//			}
//		});
//		modeMenu.add(autoScrubLoggers);

        
		final JCheckBoxMenuItem autoSetClockRate = new JCheckBoxMenuItem("Automatic set clock rate", this.doc.bAutoSetClockRate);
		autoSetClockRate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				doc.bAutoSetClockRate = autoSetClockRate.getState();
				refreshPanel();
			}
		});
		modeMenu.add(autoSetClockRate);

		final JCheckBoxMenuItem muteSound = new JCheckBoxMenuItem("Mute sound", false);
		muteSound.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				BeautiPanel.soundIsPlaying = muteSound.getState();
				refreshPanel();
			}
		});
		modeMenu.add(muteSound);
        
        
        viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        viewMenu.setMnemonic('V');
        setUpViewMenu();
        

        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        helpMenu.setMnemonic('H');
        helpMenu.add(a_help);
        helpMenu.add(a_citation);
        helpMenu.add(a_viewModel);
        helpMenu.add(a_about);
    	
    	setMenuVisibiliy("", menuBar);
    	
        return menuBar;
    } // makeMenuBar
	
    
    void setUpViewMenu() {
    	viewMenu.removeAll();
		for (int iPanel = 0; iPanel < BeautiConfig.g_panels.size(); iPanel++) {
        	final ViewPanelCheckBoxMenuItem viewPanelAction = new ViewPanelCheckBoxMenuItem( iPanel);
        	viewPanelAction.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                	viewPanelAction.doAction();
                }
            });
        	viewMenu.add(viewPanelAction);
		}
        viewMenu.addSeparator();
    	viewMenu.add(a_viewall);
	}


	class TemplateAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		
		String m_sFileName;
		
    	public TemplateAction(File file) {
    		super("xx");
    		m_sFileName = file.getAbsolutePath();
    		String sName = m_sFileName.substring(m_sFileName.lastIndexOf("/") + 1, m_sFileName.length() - 4);
    		putValue(Action.NAME, sName);
    	}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if (JOptionPane.showConfirmDialog(frame, "Changing templates means the information input so far will be lost. " +
						"Are you sure you want to change templates?", "Are you sure?", JOptionPane.YES_NO_CANCEL_OPTION) == 
							JOptionPane.YES_OPTION) {
    				doc.loadNewTemplate(m_sFileName);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(null, "Something went wrong loading the template: " + ex.getMessage());
			}
		}
    }
    
    private List<AbstractAction> getTemplateActions() {
    	List<AbstractAction> actions = new ArrayList<AbstractAction>();
    	List<String> sBeastDirectories = AddOnManager.getBeastDirectories();
    	for (String sDir : sBeastDirectories) {
    		File dir = new File(sDir + "/templates");
    		getTemplateActionForDir(dir, actions);
    	}
    	return actions;
	}

	private void getTemplateActionForDir(File dir, List<AbstractAction> actions) {
		if (dir.exists() && dir.isDirectory()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File template : files) {
					if (template.getName().toLowerCase().endsWith(".xml")) {
						try {
						String sXML2 = BeautiDoc.load(template.getAbsolutePath());
						if (sXML2.contains("<mergepoint ")) {
							actions.add(new TemplateAction(template));
						}
						} catch (Exception e) {
							System.err.println(e.getMessage());
						}
					}
				}
			}
		}
	}

	void setMenuVisibiliy(String sParentName, Component c) {
    	String sName = "";
    	if (c instanceof JMenu) {
    		sName = ((JMenu)c).getText();
    	} else if (c instanceof JMenuItem) {
    		sName = ((JMenuItem)c).getText();
    	}
   		if (sName.length() > 0 && BeautiConfig.menuIsInvisible(sParentName+sName)) {
   			c.setVisible(false);
   		}
       	if (c instanceof JMenu) {
       		for (Component x: ((JMenu)c).getMenuComponents()){
        		setMenuVisibiliy(sParentName + sName + (sName.length() > 0 ? ".":""),  x);
       		}
       	} else if (c instanceof Container) {
        	for (int i =0 ; i < ((Container)c).getComponentCount(); i++) {
        		setMenuVisibiliy(sParentName,  ((Container)c).getComponent(i));
        	}
       	}
   	}

    
    // hide panels as indicated in the hidepanels attribute in the XML template,
    // or use default tabs to hide otherwise.
	void hidePanels() {
//		for (int iPanel = 0; iPanel < BeautiConfig.g_panels.size(); iPanel++) {
//			BeautiPanelConfig panelConfig = BeautiConfig.g_panels.get(iPanel);
//			if (!panelConfig.m_bIsVisibleInput.get()) {
//				toggleVisible(iPanel);
//			}
//		}
	} // hidePanels
	
    void setUpPanels() throws Exception {
    	// remove any existing tabs
    	if (getTabCount() > 0) {
	    	while (getTabCount() > 0) {
	    		removeTabAt(0);
	    	}
    	} else {
    		for (int iPanel = 0; iPanel < BeautiConfig.g_panels.size(); iPanel++) {
    			BeautiPanelConfig panelConfig = BeautiConfig.g_panels.get(iPanel);
    			bPaneIsVisible[iPanel] = panelConfig.bIsVisibleInput.get();
    		}    		
    	}
    	// add panels according to BeautiConfig 
		panels = new BeautiPanel[ BeautiConfig.g_panels.size()];
		for (int iPanel = 0; iPanel < BeautiConfig.g_panels.size(); iPanel++) {
			BeautiPanelConfig panelConfig = BeautiConfig.g_panels.get(iPanel);
			panels[ iPanel] = new BeautiPanel( iPanel, this.doc, panelConfig);
			addTab(BeautiConfig.getButtonLabel(this, panelConfig.getName()), null, panels[ iPanel], panelConfig.getTipText());
		}
		
		for (int iPanel = BeautiConfig.g_panels.size() - 1; iPanel >= 0; iPanel--) {
			if (!bPaneIsVisible[iPanel]) {
				removeTabAt(iPanel);
			}
		}
    }


	
	public static void main(String[] args) {
		try {
			AddOnManager.loadExternalJars();
			Utils.loadUIManager();
			PluginPanel.init();
			
	        BeautiDoc doc = new BeautiDoc();
	        if (doc.parseArgs(args) == ActionOnExit.WRITE_XML) {
               	return;
            }
	        
	        final Beauti beauti = new Beauti(doc);

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
						beauti.currentTab.config.sync(beauti.currentTab.iPartition);
						BeautiPanel panel = (BeautiPanel) beauti.getSelectedComponent();
						beauti.currentTab = panel;
						beauti.refreshPanel();
					}
				}
			});
			
			
			beauti.setVisible(true);
			beauti.refreshPanel();
			JFrame frame = new JFrame("Beauti II: " + doc.sTemplateName + " " + beauti.sFileName);
			beauti.frame = frame;
			frame.setIconImage(BeautiPanel.getIcon(0, null).getImage());

			JMenuBar menuBar = beauti.makeMenuBar(); 
			frame.setJMenuBar(menuBar);
			
			if (doc.sFileName != null || doc.alignments.size()> 0) {
				beauti.a_save.setEnabled(true);
				beauti.a_saveas.setEnabled(true);
			}
			
			frame.add(beauti);
	        frame.setSize(1024, 768);
	        frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		} catch (Exception e) {
			e.printStackTrace();
		}
    } // main

} // class Beauti
