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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

import beast.app.BeastMCMC;
import beast.app.draw.HelpBrowser;
import beast.app.draw.InputEditor;
import beast.app.draw.ModelBuilder;
import beast.app.draw.MyAction;
import beast.app.draw.ExtensionFileFilter;
import beast.app.draw.PluginPanel;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;

public class Beauti extends JTabbedPane {
	private static final long serialVersionUID = 1L;
	
	
    /**
     * name of current file, used for saving (as opposed to saveAs) *
     */
    String m_sFileName = "";
    /**
     * current directory for opening files *
     */
    static String m_sDir = System.getProperty("user.dir");
    /**
     * File extension for Beast specifications
     */
    static public final String FILE_EXT = ".xml";

	/** document in document-view pattern. BTW this class is the view */
    BeautiDoc m_doc;
    
    /** currently selected tab **/
    BeautiPanel m_currentTab;
    
	boolean [] m_bPaneIsVisible;
	BeautiPanel [] m_panels;

	public Beauti(BeautiDoc doc) {
		m_bPaneIsVisible = new boolean[ BeautiConfig.g_panels.size()];
		Arrays.fill(m_bPaneIsVisible, true);
		//m_panels = new BeautiPanel[NR_OF_PANELS];
		m_doc = doc;
	}

	
	

	
	
	void toggleVisible(int nPanelNr) {
		if (m_bPaneIsVisible[nPanelNr]) {
			m_bPaneIsVisible[nPanelNr] = false;
			int nTabNr = tabNrForPanel(nPanelNr);
			removeTabAt(nTabNr);
		} else {
			m_bPaneIsVisible[nPanelNr] = true;
			int nTabNr = tabNrForPanel(nPanelNr);
				BeautiPanelConfig panel = BeautiConfig.g_panels.get(nPanelNr);
				insertTab(BeautiConfig.getButtonLabel(this, panel.m_sNameInput.get()), null, m_panels[nPanelNr], panel.m_sTipTextInput.get(), nTabNr);
//			}
			setSelectedIndex(nTabNr);
		}
	}
	
	int tabNrForPanel(int nPanelNr) {
		int k = 0;
		for (int i = 0; i < nPanelNr; i++) {
			if (m_bPaneIsVisible[i]) {
				k++;
			}
		}
		return k;
	}

	
    Action a_save = new ActionSave();
    Action a_saveas = new ActionSaveAs();
    Action a_quit = new ActionQuit();
    Action a_viewall = new ActionViewAllPanels();
    
    Action a_help = new ActionHelp();
    Action a_citation = new ActionCitation();
    Action a_about = new ActionAbout();
    Action a_viewModel = new ActionViewModel();

    class ActionSave extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -20389110859355156L;

        public ActionSave() {
            super("Save", "Save Graph", "save", "ctrl S");
        } // c'tor

        public ActionSave(String sName, String sToolTipText, String sIcon,
                          String sAcceleratorKey) {
            super(sName, sToolTipText, sIcon, sAcceleratorKey);
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
            if (!m_sFileName.equals("")) {
                if (!m_doc.validateModel()) {
                    return;
                }
                saveFile(m_sFileName);
//                m_doc.isSaved();
            } else {
                if (saveAs()) {
//                    m_doc.isSaved();
                }
            }
        } // actionPerformed

        ExtensionFileFilter ef1 = new ExtensionFileFilter(".xml", "BEAST files");

        boolean saveAs() {
            if (!m_doc.validateModel()) {
                return false;
            }
            JFileChooser fc = new JFileChooser(m_sDir);
            fc.addChoosableFileFilter(ef1);
            fc.setDialogTitle("Save Model As");
            if (!m_sFileName.equals("")) {
                // can happen on actionQuit
                fc.setSelectedFile(new File(m_sFileName));
            }
            int rval = fc.showSaveDialog(null);

            if (rval == JFileChooser.APPROVE_OPTION) {
                // System.out.println("Saving to file \""+
                // f.getAbsoluteFile().toString()+"\"");
                m_sFileName = fc.getSelectedFile().toString();
                if (m_sFileName.lastIndexOf('/') > 0) {
                    m_sDir = m_sFileName.substring(0, m_sFileName.lastIndexOf('/'));
                }
                if (!m_sFileName.endsWith(FILE_EXT))
                	m_sFileName = m_sFileName.concat(FILE_EXT);
                saveFile(m_sFileName);
                return true;
            }
            return false;
        } // saveAs    

        protected void saveFile(String sFileName) {
            try {
            	m_currentTab.m_config.sync(m_currentTab.m_iPartition);
                m_doc.save(sFileName);
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
            super("Save As", "Save Graph As", "saveas", "");
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
            saveAs();
        } // actionPerformed
    } // class ActionSaveAs    
    
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
        	if (m_doc.validateModel()) {
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
    		super("Show " +	BeautiConfig.g_panels.get(iPanel).m_sNameInput.get() + " panel", 
    				BeautiConfig.g_panels.get(iPanel).m_bIsVisibleInput.get());
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
        	for(int nPanelNr = 0; nPanelNr < m_bPaneIsVisible.length; nPanelNr++) {
        		if (!m_bPaneIsVisible[nPanelNr]) {
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
            JOptionPane.showMessageDialog(null, "Beauti 2\n\n2011", "About Message", JOptionPane.PLAIN_MESSAGE);
        }
    } // class ActionAbout

    class ActionHelp extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionHelp() {
            super("Help", "Help on current panel", "help", "");
        } // c'tor

        public void actionPerformed(ActionEvent ae) {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            HelpBrowser b = new HelpBrowser(m_currentTab.m_config.getType());
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
        	String sCitations = m_doc.m_mcmc.get().getCitations();
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
            modelBuilder.m_doc.init(m_doc.m_mcmc.get());
            modelBuilder.setDrawingFlag();
            frame.setSize(600, 800);
            frame.setVisible(true);
        }
    } // class ActionViewModel
    
    
    void refreshPanel() {
		try {
			BeautiPanel panel = (BeautiPanel) getSelectedComponent();
//			int i = 0;
//			while (m_panels[i] != panel) {
//				i++;
//			}
			panel.refreshPanel();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
    }
    
    public JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);
        fileMenu.add(a_save);
        fileMenu.add(a_saveas);
        fileMenu.addSeparator();
        fileMenu.add(a_quit);
        
        JMenu modeMenu = new JMenu("Mode");
        menuBar.add(modeMenu);
        modeMenu.setMnemonic('M');
        
		final JCheckBoxMenuItem viewEditTree = new JCheckBoxMenuItem("Expert mode", InputEditor.g_bExpertMode);
		viewEditTree.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				InputEditor.g_bExpertMode = viewEditTree.getState();
				refreshPanel();
			}
		});
		modeMenu.add(viewEditTree);
		modeMenu.addSeparator();

		final JCheckBoxMenuItem autoScrubPriors = new JCheckBoxMenuItem("Automatic scrub priors", m_doc.m_bAutoScrubPriors);
		autoScrubPriors.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				m_doc.m_bAutoScrubPriors = autoScrubPriors.getState();
				refreshPanel();
			}
		});
		modeMenu.add(autoScrubPriors);
		final JCheckBoxMenuItem autoScrubState = new JCheckBoxMenuItem("Automatic scrub state", m_doc.m_bAutoScrubState);
		autoScrubState.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				m_doc.m_bAutoScrubState = autoScrubState.getState();
				refreshPanel();
			}
		});
		modeMenu.add(autoScrubState);
		final JCheckBoxMenuItem autoScrubOperators = new JCheckBoxMenuItem("Automatic scrub operators", m_doc.m_bAutoScrubOperators);
		autoScrubOperators.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				m_doc.m_bAutoScrubOperators = autoScrubOperators.getState();
				refreshPanel();
			}
		});
		modeMenu.add(autoScrubOperators);
		final JCheckBoxMenuItem autoScrubLoggers = new JCheckBoxMenuItem("Automatic scrub loggers", m_doc.m_bAutoScrubLoggers);
		autoScrubLoggers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				m_doc.m_bAutoScrubLoggers = autoScrubLoggers.getState();
				refreshPanel();
			}
		});
		modeMenu.add(autoScrubLoggers);
        
        
        
        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        viewMenu.setMnemonic('V');
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
		for (int iPanel = 0; iPanel < BeautiConfig.g_panels.size(); iPanel++) {
			BeautiPanelConfig panelConfig = BeautiConfig.g_panels.get(iPanel);
			if (!panelConfig.m_bIsVisibleInput.get()) {
				toggleVisible(iPanel);
			}
		}
	} // hidePanels
	
	public static void main(String[] args) {
		try {
        	BeastMCMC.loadExternalJars();
			
			PluginPanel.init();
			
	        BeautiInitDlg dlg = null;
	        BeautiDoc doc = new BeautiDoc();
	        dlg = new BeautiInitDlg(args, doc);

			if (dlg.m_sTemplateXML.indexOf("spec='beast.evolution.alignment.TaxonSet'/>") > 0) {
				Set<String> candidates = new HashSet<String>();
				for (Alignment alignment : doc.m_alignments.get()) {
					for (String sName : alignment.getTaxaNames()) {
						candidates.add(sName);
					}
				}
				String sTaxonSet = "\n    <taxon spec='TaxonSet' id='all'>\n";
				for (String sName : candidates) {
					sTaxonSet += "        <taxon spec='Taxon' id='" + sName +"'/>\n";
				}
				sTaxonSet += "    </taxon>\n";
				dlg.m_sTemplateXML = dlg.m_sTemplateXML.replace("spec='beast.evolution.alignment.TaxonSet'/>", 
						"spec='beast.evolution.alignment.TaxonSet'>" + sTaxonSet + "</taxonset>");
			}
	        
	        
	        doc.initialize(dlg.m_endState, dlg.m_sXML, dlg.m_sTemplateXML, dlg.m_sOutputFileName);
	
	        final Beauti beauti = new Beauti(doc);

			beauti.m_panels = new BeautiPanel[ BeautiConfig.g_panels.size()];
			for (int iPanel = 0; iPanel < BeautiConfig.g_panels.size(); iPanel++) {
				BeautiPanelConfig panelConfig = BeautiConfig.g_panels.get(iPanel);
				beauti.m_panels[ iPanel] = new BeautiPanel( iPanel, doc, panelConfig);
				beauti.addTab(BeautiConfig.getButtonLabel(beauti, panelConfig.getName()), null, beauti.m_panels[ iPanel], panelConfig.getTipText());
			}
			beauti.m_currentTab = beauti.m_panels[0];
			beauti.hidePanels();

			beauti.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					beauti.m_currentTab.m_config.sync(beauti.m_currentTab.m_iPartition);
					BeautiPanel panel = (BeautiPanel) beauti.getSelectedComponent();
					beauti.m_currentTab = panel;
					beauti.refreshPanel();
				}
			});
			
			
			beauti.setVisible(true);
			beauti.refreshPanel();
			JFrame frame = new JFrame("Beauti II");
			frame.setIconImage(BeautiPanel.getIcon(6, null).getImage());

			JMenuBar menuBar = beauti.makeMenuBar(); 
			frame.setJMenuBar(menuBar);
			
			frame.add(beauti);
	        frame.setSize(800, 600);
	        frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		} catch (Exception e) {
			e.printStackTrace();
		}
    } // main

} // class Beauti
