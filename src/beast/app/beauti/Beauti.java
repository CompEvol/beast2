package beast.app.beauti;




import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;

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
import beast.app.draw.InputEditor;
import beast.app.draw.MyAction;
import beast.app.draw.ExtensionFileFilter;
import beast.app.draw.PluginPanel;

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
//	static final int NR_OF_PANELS = 0;
//	static String [] TAB_NAME = {
//			"Data", 
//			"Taxon sets", 
//			"Tip Dates", 
//			"Site Model", 
//			"Clock Model", 
//			"Tree prior", 
//			"Initialization",
//			"Priors", 
//			"Operators", 
//			"MCMC"}; 
	
//	final static public int DATA_PANEL = 0;
//	final static public int TAXON_SETS_PANEL = 1;
//	final static public int TIP_DATES_PANEL = 2;
//	final static public int SITE_MODEL_PANEL = 3;
//	final static public int CLOCK_MODEL_PANEL = 4;
//	final static public int TREE_PRIOR_PANEL = 5;
//	final static public int STATE_PANEL = 6;
//	final static public int PRIORS_PANEL = 7;
//	final static public int OPERATORS_PANEL = 8;
//	final static public int MCMC_PANEL = 9;
//
//	static String [] TAB_CONST = {
//		"DATA_PANEL",
//		"TAXON_SETS_PANEL",
//		"TIP_DATES_PANEL",
//		"SITE_MODEL_PANEL",
//		"CLOCK_MODEL_PANEL",
//		"TREE_PRIOR_PANEL",
//		"STATE_PANEL",
//		"PRIORS_PANEL",
//		"OPERATORS_PANEL",
//		"MCMC_PANEL"
//	};
//
//	static String [] TAB_TIPTEXT = {
//			"Aligned sequence data",
//			"Taxon sets",
//			 "Allows to specify data that a taxan was sampled",
//			 "Site model",
//			 "Clock model",
//			 "Tree prior",
//			 "Initial state",
//			 "Other priors",
//			 "MCMC Operator details",
//			 "MCMC parameters"};
//	
//	static boolean [] HAS_PARTITIONS = {
//		false,//"Data", 
//		false,//"Taxon sets", 
//		true,//"Tip Dates", 
//		true,//"Site Model", 
//		true,//"Clock Model", 
//		false,//"Tree prior", 
//		false,//Initial
//		false,//"Priors", 
//		false,//"Operators", 
//		false//"MCMC"
//	}; 
//	

	public Beauti(BeautiDoc doc) {
		m_bPaneIsVisible = new boolean[/*NR_OF_PANELS*/ + BeautiConfig.g_panels.size()];
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
//			if (nPanelNr < NR_OF_PANELS) {
//				insertTab(BeautiConfig.getButtonLabel(this, TAB_NAME[nPanelNr]), null, m_panels[nPanelNr], TAB_TIPTEXT[nPanelNr], nTabNr);
//			} else {
				BeautiPanelConfig panel = BeautiConfig.g_panels.get(nPanelNr /*- NR_OF_PANELS*/);
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
            	m_currentTab.m_config.sync();
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
    		super("Show " + 
    				//(iPanel < NR_OF_PANELS ? BeautiConfig.getButtonLabel(Beauti.class.getName(), TAB_NAME[iPanel]) :
    					BeautiConfig.g_panels.get(iPanel /*- NR_OF_PANELS*/).m_sNameInput.get()
    				//	) 
    				+ " panel", 
    				//(iPanel < NR_OF_PANELS ? m_bPaneIsVisible[iPanel] : )
    				BeautiConfig.g_panels.get(iPanel /*- NR_OF_PANELS*/).m_bIsVisibleInput.get());
    		m_iPanel = iPanel;
    		if (m_viewPanelCheckBoxMenuItems == null) {
    			m_viewPanelCheckBoxMenuItems = new ViewPanelCheckBoxMenuItem[/*NR_OF_PANELS*/ + BeautiConfig.g_panels.size()];
    		}
    		m_viewPanelCheckBoxMenuItems[iPanel] = this;
    	}
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
        		if (!m_bPaneIsVisible[nPanelNr] 
        		    //&& !BeautiConfig.g_sDisabledMenus.contains("View.Show "+TAB_NAME[nPanelNr] + " panel")
        		                      ) {
        			toggleVisible(nPanelNr);
        			m_viewPanelCheckBoxMenuItems[nPanelNr].setState(true);
        		}
        	}
        } // actionPerformed
    } // class ActionViewAllPanels
    
    
    void refreshPanel() {
		try {
			BeautiPanel panel = (BeautiPanel) getSelectedComponent();
			int i = 0;
			while (m_panels[i] != panel) {
				i++;
			}
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
//        fileMenu.add(a_new);
//        fileMenu.add(a_load);
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
//        for (int i = 0; i < NR_OF_PANELS; i++) {
//        	final ViewPanelCheckBoxMenuItem viewPanelAction = new ViewPanelCheckBoxMenuItem(i);
//        	viewPanelAction.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent ae) {
//                	viewPanelAction.doAction();
//                }
//            });
//        	viewMenu.add(viewPanelAction);
//        }
		for (int iPanel = 0; iPanel < BeautiConfig.g_panels.size(); iPanel++) {
        	final ViewPanelCheckBoxMenuItem viewPanelAction = new ViewPanelCheckBoxMenuItem(/*NR_OF_PANELS*/ + iPanel);
        	viewPanelAction.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                	viewPanelAction.doAction();
                }
            });
        	viewMenu.add(viewPanelAction);
		}
        
        
        viewMenu.addSeparator();
    	viewMenu.add(a_viewall);
        
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
		
//		for(int nPanelNr = 0; nPanelNr < NR_OF_PANELS; nPanelNr++) {
//    		if (BeautiConfig.g_sHidePanels.contains(TAB_CONST[nPanelNr])) {
//    			toggleVisible(nPanelNr);
//    		}
//    	}
	} // hidePanels
	
	public static void main(String[] args) {
		try {
        	BeastMCMC.loadExternalJars();
			
			PluginPanel.init();
			
			InputEditor.g_bExpertMode = true;
	        BeautiInitDlg dlg = null;
	        BeautiDoc doc = new BeautiDoc();
	        dlg = new BeautiInitDlg(args, doc);
			InputEditor.g_bExpertMode = false;

	        doc.initialize(dlg.m_endState, dlg.m_sXML, dlg.m_sTemplateXML, dlg.m_sOutputFileName);
	
	        final Beauti beauti = new Beauti(doc);

			beauti.m_panels = new BeautiPanel[/*NR_OF_PANELS*/ + BeautiConfig.g_panels.size()];
//			for (int iPanel = 0; iPanel < NR_OF_PANELS; iPanel++) {
//				beauti.m_panels[iPanel] = new BeautiPanel(iPanel, doc, HAS_PARTITIONS[iPanel]);
//				beauti.addTab(BeautiConfig.getButtonLabel(beauti, TAB_NAME[iPanel]), null, beauti.m_panels[iPanel], TAB_TIPTEXT[iPanel]);
//			}
			for (int iPanel = 0; iPanel < BeautiConfig.g_panels.size(); iPanel++) {
				BeautiPanelConfig panelConfig = BeautiConfig.g_panels.get(iPanel);
				beauti.m_panels[/*NR_OF_PANELS*/ + iPanel] = new BeautiPanel(/*NR_OF_PANELS*/ + iPanel, doc, panelConfig);
				beauti.addTab(BeautiConfig.getButtonLabel(beauti, panelConfig.getName()), null, beauti.m_panels[/*NR_OF_PANELS*/ + iPanel], panelConfig.getTipText());
			}
			beauti.m_currentTab = beauti.m_panels[0];
			beauti.hidePanels();

			beauti.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					//beauti.m_doc.sync(beauti.m_currentTab.m_iPanel);
					beauti.m_currentTab.m_config.sync();
					BeautiPanel panel = (BeautiPanel) beauti.getSelectedComponent();
					//panel.m_config.sync();
					beauti.m_currentTab = panel;
					//beauti.m_doc.syncTo(panel.m_iPanel, panel.m_iPartition);
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
