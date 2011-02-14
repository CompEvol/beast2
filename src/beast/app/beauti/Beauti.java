package beast.app.beauti;



import java.awt.Font;
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
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
	static final int NR_OF_PANELS = 10;
	static String [] TAB_NAME = {
			"Data", 
			"Taxon sets", 
			"Tip Dates", 
			"Site Model", 
			"Clock Model", 
			"Tree prior", 
			"Initialization",
			"Priors", 
			"Operators", 
			"MCMC"}; 
	
	final static public int DATA_PANEL = 0;
	final static public int TAXON_SETS_PANEL = 1;
	final static public int TIP_DATES_PANEL = 2;
	final static public int SITE_MODEL_PANEL = 3;
	final static public int CLOCK_MODEL_PANEL = 4;
	final static public int TREE_PRIOR_PANEL = 5;
	final static public int STATE_PANEL = 6;
	final static public int PRIORS_PANEL = 7;
	final static public int OPERATORS_PANEL = 8;
	final static public int MCMC_PANEL = 9;

	static String [] TAB_CONST = {
		"DATA_PANEL",
		"TAXON_SETS_PANEL",
		"TIP_DATES_PANEL",
		"SITE_MODEL_PANEL",
		"CLOCK_MODEL_PANEL",
		"TREE_PRIOR_PANEL",
		"STATE_PANEL",
		"PRIORS_PANEL",
		"OPERATORS_PANEL",
		"MCMC_PANEL"
	};

	static String [] TAB_TIPTEXT = {
			"Aligned sequence data",
			"Taxon sets",
			 "Allows to specify data that a taxan was sampled",
			 "Site model",
			 "Clock model",
			 "Tree prior",
			 "Initial state",
			 "Other priors",
			 "MCMC Operator details",
			 "MCMC parameters"};
	
	static boolean [] HAS_PARTITIONS = {
		false,//"Data", 
		false,//"Taxon sets", 
		true,//"Tip Dates", 
		true,//"Site Model", 
		true,//"Clock Model", 
		false,//"Tree prior", 
		false,//Initial
		false,//"Priors", 
		false,//"Operators", 
		false//"MCMC"
	}; 
	

	public Beauti(BeautiDoc doc) {
		m_bPaneIsVisible = new boolean[NR_OF_PANELS];
		Arrays.fill(m_bPaneIsVisible, true);
		m_panels = new BeautiPanel[NR_OF_PANELS];
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
			insertTab(TAB_NAME[nPanelNr], null, m_panels[nPanelNr], TAB_TIPTEXT[nPanelNr], nTabNr);
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

	
//    Action a_new = new ActionNew();
//    Action a_load = new ActionLoad();
    Action a_save = new ActionSave();
    Action a_saveas = new ActionSaveAs();
    Action a_quit = new ActionQuit();

    
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
                String sFileName = fc.getSelectedFile().toString();
                if (sFileName.lastIndexOf('/') > 0) {
                    m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
                }
                if (!sFileName.endsWith(FILE_EXT))
                    sFileName = sFileName.concat(FILE_EXT);
                saveFile(sFileName);
                return true;
            }
            return false;
        } // saveAs    

        protected void saveFile(String sFileName) {
            try {
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

    
    class ViewPanelCheckBoxMenuItem extends JCheckBoxMenuItem{
		private static final long serialVersionUID = 1L;
		int m_iPanel;
    	ViewPanelCheckBoxMenuItem(int iPanel) {
    		super("Show " + TAB_NAME[iPanel] + " panel", m_bPaneIsVisible[iPanel]);
    		m_iPanel = iPanel;
    	}
    	void doAction() {
    		toggleVisible(m_iPanel);
    	}
    };
    
    
//    boolean m_bExpertMode = false;
    void refreshPanel() {
		try {
			BeautiPanel panel = (BeautiPanel) getSelectedComponent();
			int i = 0;
			while (m_panels[i] != panel) {
				i++;
			}
			System.err.println(TAB_NAME[i]);
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
        
		final JCheckBoxMenuItem viewEditTree = new JCheckBoxMenuItem("Expert mode", InputEditor.m_bExpertMode);
		viewEditTree.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				InputEditor.m_bExpertMode = viewEditTree.getState();
				refreshPanel();
			}
		});
		modeMenu.add(viewEditTree);
		modeMenu.addSeparator();

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
        for (int i = 0; i < NR_OF_PANELS; i++) {
        	final ViewPanelCheckBoxMenuItem viewPanelAction = new ViewPanelCheckBoxMenuItem(i);
        	viewPanelAction.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                	viewPanelAction.doAction();
                }
            });
        	viewMenu.add(viewPanelAction);
        }
        
        return menuBar;
    } // makeMenuBar
	
    
    // hide panels as indicated in the hidepanels attribute in the XML template,
    // or use default tabs to hide otherwise.
	void hidePanels(String sXML) {			
		String sHidePanel = "TAXON_SETS_PANEL|TIP_DATES_PANEL|PRIORS_PANEL|OPERATORS_PANEL";
		if (sXML != null) {
			// grab sHidePanels flags from template
			int i = sXML.indexOf("hidepanes=");
			if (i >= 0) {
				i += 10;
				char separator = sXML.charAt(i);
				sHidePanel = "";
				i++;
				while (sXML.charAt(i) != separator) {
					sHidePanel += sXML.charAt(i++);
				}
			}
			
		}
		String [] sHidePanels = sHidePanel.split("\\|");
		for (String sPanel : sHidePanels) {
			int iPanel = 0;
			while (!TAB_CONST[iPanel].equals(sPanel)) {
				iPanel++;
			}
			toggleVisible(iPanel);
		}

	//	beauti.toggleVisible(TAXON_SETS_PANEL);
	//	beauti.toggleVisible(TIP_DATES_PANEL);
	//	beauti.toggleVisible(PRIORS_PANEL);
	//	beauti.toggleVisible(OPERATORS_PANEL);
	} // hidePanels
	
	public static void main(String[] args) {
		try {
		    // sets the default font for all Swing components.
			javax.swing.plaf.FontUIResource f = new javax.swing.plaf.FontUIResource("Serif", Font.PLAIN, 14);
		    java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
		    while (keys.hasMoreElements()) {
		      Object key = keys.nextElement();
		      Object value = UIManager.get (key);
		      if (value instanceof javax.swing.plaf.FontUIResource) {
		        UIManager.put (key, f);
		      }
		    }   			
			
			
			PluginPanel.init();
			
			InputEditor.m_bExpertMode = true;
	        BeautiInitDlg dlg = null;
	        BeautiDoc doc = new BeautiDoc();
	        dlg = new BeautiInitDlg(args, doc);
			InputEditor.m_bExpertMode = false;

	        doc.initialize(dlg.m_endState, dlg.m_sXML, dlg.m_sTemplateXML, dlg.m_sOutputFileName);
	        //dlg.setVisible(true);
	
	        final Beauti beauti = new Beauti(doc);
	
			for (int iPanel = 0; iPanel < NR_OF_PANELS; iPanel++) {
				beauti.m_panels[iPanel] = new BeautiPanel(iPanel, doc, HAS_PARTITIONS[iPanel]);
				beauti.addTab(TAB_NAME[iPanel], null, beauti.m_panels[iPanel], TAB_TIPTEXT[iPanel]);
				
			}
			beauti.m_currentTab = beauti.m_panels[0];
			beauti.hidePanels(dlg.m_sTemplateXML);

			beauti.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					beauti.m_doc.sync(beauti.m_currentTab.m_iPanel);
					BeautiPanel panel = (BeautiPanel) beauti.getSelectedComponent();
					beauti.m_currentTab = panel;
					beauti.m_doc.syncTo(panel.m_iPanel, panel.m_iPartition);
					beauti.refreshPanel();
				}
			});
			
			
			beauti.setVisible(true);
			JFrame frame = new JFrame("Beauti II");
			frame.setIconImage(BeautiPanel.getIcon(6).getImage());

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
