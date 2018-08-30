package beast.app.beauti;


import beast.app.BEASTVersion2;
import beast.app.beauti.BeautiDoc.ActionOnExit;
import beast.app.beauti.BeautiDoc.DOC_STATUS;
import beast.app.draw.BEASTObjectPanel;
import beast.app.draw.HelpBrowser;
import beast.app.draw.ModelBuilder;
import beast.app.draw.MyAction;
import beast.app.tools.AppLauncher;
import beast.app.util.Utils;
import beast.app.util.Utils6;
import beast.core.BEASTInterface;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.math.distributions.MRCAPrior;
import beast.util.PackageManager;
import jam.framework.DocumentFrame;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.List;


public class Beauti extends JTabbedPane implements BeautiDocListener {
    private static final long serialVersionUID = 1L;
    static final String BEAUTI_ICON = "beast/app/draw/icons/beauti.png";

    // ExtensionFileFilter ef0 = new ExtensionFileFilter(".nex", "Nexus files");
    // ExtensionFileFilter ef1 = new ExtensionFileFilter(".xml", "BEAST files");

    /**
     * current directory for opening files *
     */
    public static String g_sDir = System.getProperty("user.dir");

	/**
     * File extension for Beast specifications
     */
    static public final String FILE_EXT = ".xml";
    static public final String FILE_EXT2 = ".json";
    static final String fileSep = System.getProperty("file.separator");

    /**
     * document in document-view pattern. BTW this class is the view
     */
    public BeautiDoc doc;
    public JFrame frame;

    /**
     * currently selected tab *
     */
    public BeautiPanel currentTab;

    public boolean[] isPaneIsVisible;
    public BeautiPanel[] panels;

    /**
     * menu for file handling, importing partitions, etc.
     */
	JMenu fileMenu;
    /**
     * menu for switching templates *
     */
    JMenu templateMenu;
    /**
     * menu for making showing/hiding tabs *
     */
    JMenu viewMenu;

    JCheckBoxMenuItem autoSetClockRate;
    JCheckBoxMenuItem allowLinking;
    JCheckBoxMenuItem autoUpdateFixMeanSubstRate;

    /**
     * flag indicating beauti is in the process of being set up and panels
     * should not sync with current model *
     */
    public boolean isInitialising = true;

    public Beauti(BeautiDoc doc) {
        isPaneIsVisible = new boolean[doc.beautiConfig.panels.size()];
        Arrays.fill(isPaneIsVisible, true);
        // m_panels = new BeautiPanel[NR_OF_PANELS];
        this.doc = doc;
        this.doc.addBeautiDocListener(this);
        doc.setBeauti(this);
    }

    void setTitle() {
        frame.setTitle("BEAUti 2: " + this.doc.getTemplateName() + " "
                + doc.getFileName());
    }

    void toggleVisible(int panelNr) {
        if (isPaneIsVisible[panelNr]) {
            isPaneIsVisible[panelNr] = false;
            int tabNr = tabNrForPanel(panelNr);
            removeTabAt(tabNr);
        } else {
            isPaneIsVisible[panelNr] = true;
            int tabNr = tabNrForPanel(panelNr);
            BeautiPanelConfig panel = doc.beautiConfig.panels.get(panelNr);
            insertTab(
                    doc.beautiConfig.getButtonLabel(this,
                            panel.nameInput.get()), null, panels[panelNr],
                    panel.tipTextInput.get(), tabNr);
            // }
            setSelectedIndex(tabNr);
        }
    }

    int tabNrForPanel(int panelNr) {
        int k = 0;
        for (int i = 0; i < panelNr; i++) {
            if (isPaneIsVisible[i]) {
                k++;
            }
        }
        return k;
    }

    Action a_new = new ActionNew();
    public Action a_load = new ActionLoad();
    Action a_template = new ActionTemplate();
    Action a_managePackages = new ActionManagePacakges();
    Action a_clearClassPath = new ActionClearClassPath();
    Action a_appLauncher = new ActionAppLauncher();
//    public Action a_import = new ActionImport();
    public Action a_save = new ActionSave();
    Action a_saveas = new ActionSaveAs();
    Action a_close = new ActionClose();
    Action a_quit = new ActionQuit();
    Action a_viewall = new ActionViewAllPanels();

    Action a_help = new ActionHelp();
    Action a_msgs = new ActionMsgs();
    Action a_citation = new ActionCitation();
    Action a_about = new ActionAbout();
    Action a_viewModel = new ActionViewModel();

    @Override
    public void docHasChanged() throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        setUpPanels();
        setUpViewMenu();
        setTitle();
    }

    class ActionSave extends MyAction {
        private static final long serialVersionUID = 1;

        public ActionSave() {
            super("Save", "Save Model", "save", KeyEvent.VK_S);
            setEnabled(false);
        } // c'tor

        public ActionSave(String name, String toolTipText, String icon,
                          int acceleratorKey) {
            super(name, toolTipText, icon, acceleratorKey);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            DOC_STATUS docStatus = doc.validateModel();
            if (docStatus != DOC_STATUS.DIRTY) {
                if (docStatus == DOC_STATUS.NO_DOCUMENT)
                    JOptionPane.showMessageDialog(null,
                            "There is no data to save to file");

                return;
            }

            if (!doc.getFileName().equals("")) {
                saveFile(doc.getFileName());
                // m_doc.isSaved();
            } else {
                if (saveAs()) {
                    // m_doc.isSaved();
                }
            }
        } // actionPerformed

    } // class ActionSave

    class ActionSaveAs extends ActionSave {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -20389110859354L;

        public ActionSaveAs() {
            super("Save As", "Save Model As", "saveas", -1);
            setEnabled(false);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            saveAs();
        } // actionPerformed
    } // class ActionSaveAs

    boolean saveAs() {
        if (doc.validateModel() == DOC_STATUS.NO_DOCUMENT) {
            JOptionPane.showMessageDialog(null,
                    "There is no data to save to file");
            return false;
        }
        String fileSep = System.getProperty("file.separator");
        if (fileSep.equals("\\")) {
            fileSep = "\\\\";
        }
        String defaultFile = g_sDir + (doc.getFileName().equals("") ? "" : fileSep + new File(doc.getFileName()).getName());
        File file = beast.app.util.Utils.getSaveFile("Save Model As", new File(
                defaultFile), null, FILE_EXT, FILE_EXT2);
        if (file != null) {
            if (file.exists() && !Utils.isMac()) {
                if (JOptionPane.showConfirmDialog(null,
                        "File " + file.getName()
                                + " already exists. Do you want to overwrite?",
                        "Overwrite file?", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
            // System.out.println("Saving to file \""+
            // f.getAbsoluteFile().toString()+"\"");
            doc.setFileName(file.getAbsolutePath());// fc.getSelectedFile().toString();
            if (doc.getFileName().lastIndexOf(fileSep) > 0) {
                g_sDir = doc.getFileName().substring(0,
                        doc.getFileName().lastIndexOf(fileSep));
            }
            if (!doc.getFileName().toLowerCase().endsWith(FILE_EXT) && !doc.getFileName().toLowerCase().endsWith(FILE_EXT2))
                doc.setFileName(doc.getFileName().concat(FILE_EXT));
            saveFile(doc.getFileName());
            setTitle();
            return true;
        }
        return false;
    } // saveAs

    public void saveFile(String fileName) {
        try {
            if (currentTab != null) {
                currentTab.config.sync(currentTab.partitionIndex);
            } else {
                panels[0].config.sync(0);
            }
            doc.save(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // saveFile

    class ActionNew extends MyAction {
        private static final long serialVersionUID = 1;

        public ActionNew() {
            super("New", "Start new analysis", "new", KeyEvent.VK_N);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            main2(new String[0]);
            // doc.newAnalysis();
            // a_save.setEnabled(false);
            // a_saveas.setEnabled(false);
            // setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    class ActionLoad extends MyAction {
        private static final long serialVersionUID = 1;

        public ActionLoad() {
            super("Load", "Load Beast File", "open", KeyEvent.VK_O);
        }

        public ActionLoad(String name, String toolTipText, String icon,
                          int acceleratorKey) {
            super(name, toolTipText, icon, acceleratorKey);
        }

        @Override
		public void actionPerformed(ActionEvent ae) {
            File file = beast.app.util.Utils.getLoadFile("Load Beast XML File",
                    new File(g_sDir), "Beast XML files", "xml");//, "BEAST json file", "json");
            // JFileChooser fileChooser = new JFileChooser(g_sDir);
            // fileChooser.addChoosableFileFilter(ef1);
            // fileChooser.setDialogTitle("Load Beast XML File");
            // if (fileChooser.showOpenDialog(null) ==
            // JFileChooser.APPROVE_OPTION) {
            // fileName = fileChooser.getSelectedFile().toString();
            if (file != null) {
                setCursor(new Cursor(Cursor.WAIT_CURSOR));
                doc.newAnalysis();
                doc.setFileName(file.getAbsolutePath());
                String fileSep = System.getProperty("file.separator");
                if (fileSep.equals("\\")) {
                    fileSep = "\\\\";
                }
                if (doc.getFileName().lastIndexOf(fileSep) > 0) {
                    g_sDir = doc.getFileName().substring(0,
                            doc.getFileName().lastIndexOf(fileSep));
                }
                try {
                	// TODO: deal with json files
                    doc.loadXML(new File(doc.getFileName()));
                    a_save.setEnabled(true);
                    a_saveas.setEnabled(true);
                    setTitle();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(
                            null,
                            "Something went wrong loading the file: "
                                    + e.getMessage());
                }
            }
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        } // actionPerformed
    }

    class ActionTemplate extends MyAction {
        private static final long serialVersionUID = 1;

        public ActionTemplate() {
            super("Other Template", "Load Beast Analysis Template From File",
                    "template", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            File file = beast.app.util.Utils
                    .getLoadFile("Load Template XML File");
            // JFileChooser fileChooser = new
            // JFileChooser(System.getProperty("user.dir")+"/templates");
            // fileChooser.addChoosableFileFilter(ef1);
            // fileChooser.setDialogTitle("Load Template XML File");
            // if (fileChooser.showOpenDialog(null) ==
            // JFileChooser.APPROVE_OPTION) {
            // String fileName = fileChooser.getSelectedFile().toString();
            if (file != null) {
                String fileName = file.getAbsolutePath();
                try {
                    doc.loadNewTemplate(fileName);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(
                            null,
                            "Something went wrong loading the template: "
                                    + e.getMessage());
                }
            }
            createFileMenu();
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        } // actionPerformed
    } // ActionTemplate

    class ActionManagePacakges extends MyAction {
        private static final long serialVersionUID = 1;

        public ActionManagePacakges() {
            super("Manage Packages", "Manage Packages", "package", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
        	JPackageDialog panel = new JPackageDialog();
        	JDialog dlg = panel.asDialog(frame);
            dlg.setVisible(true);
            // refresh template menu item
            templateMenu.removeAll();
            List<AbstractAction> templateActions = getTemplateActions();
            for (AbstractAction a : templateActions) {
                templateMenu.add(a);
            }
            templateMenu.addSeparator();
            templateMenu.add(a_template);
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        } // actionPerformed
    }
    
    class ActionClearClassPath extends MyAction {
        private static final long serialVersionUID = 1;

        public ActionClearClassPath() {
            super("Clear Class Path", "Clear class path, so it will be refreshed next time BEAUti starts. Only useful when installing packages by hand.", "ccp", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
        	Utils6.saveBeautiProperty("package.path", null);
        	JOptionPane.showMessageDialog(null, "The class path was cleared.\n"
        			+ "Next time you start BEAUti, the class path will be re-established.\n"
        			+ "This is only useful when you install packages by han.d\n"
        			+ "Otherwise, this is harmless, but onlys potentially slows restarting BEAUti.");
        } // actionPerformed
    }
    
    class ActionAppLauncher extends MyAction {
        private static final long serialVersionUID = 1;

        public ActionAppLauncher() {
            super("Launch Apps", "Launch BEAST Apps supplied by packages", "launch", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
        	AppLauncher.main(new String[]{});
        } // actionPerformed
    }

    //    class ActionImport extends MyAction {
//        private static final long serialVersionUID = 1;
//
//        public ActionImport() {
//            super("Import Alignment", "Import Alignment File", "import",
//                    KeyEvent.VK_I);
//        }
//
//        public ActionImport(String name, String toolTipText, String icon,
//                            int acceleratorKey) {
//            super(name, toolTipText, icon, acceleratorKey);
//        }
//
//        public void actionPerformed(ActionEvent ae) {
//
//            try {
//                setCursor(new Cursor(Cursor.WAIT_CURSOR));
//
//                // get user-specified alignments
//                doc.beautiConfig.selectAlignments(doc,Beauti.this);
//
//                doc.connectModel();
//                doc.fireDocHasChanged();
//                a_save.setEnabled(true);
//                a_saveas.setEnabled(true);
//            } catch (Exception e) {
//                e.printStackTrace();
//
//                String text = "Something went wrong importing the alignment:\n";
//                JTextArea textArea = new JTextArea(text);
//                textArea.setColumns(30);
//                textArea.setLineWrap(true);
//                textArea.setWrapStyleWord(true);
//                textArea.append(e.getMessage());
//                textArea.setSize(textArea.getPreferredSize().width, 1);
//                textArea.setOpaque(false);
//                JOptionPane.showMessageDialog(null, textArea,
//                        "Error importing alignment",
//                        JOptionPane.WARNING_MESSAGE);
//            }
//            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
//            // }
//        } // actionPerformed
//    }

    class ActionClose extends ActionSave {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -2038911085935515L;

        public ActionClose() {
            super("Close", "Close Window", "close", KeyEvent.VK_W);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            // if (!m_doc.m_bIsSaved) {
            if (!quit()) {
                return;
            }

            JMenuItem menuItem = (JMenuItem) ae.getSource();
            JPopupMenu popupMenu = (JPopupMenu) menuItem.getParent();
            Component invoker = popupMenu.getInvoker();
            JComponent invokerAsJComponent = (JComponent) invoker;
            Container topLevel = invokerAsJComponent.getTopLevelAncestor();
            if (topLevel != null) {
                ((JFrame) topLevel).dispose();
            }
        }
    } // class ActionClose

    class ActionQuit extends ActionSave {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -2038911085935515L;

        public ActionQuit() {
            super("Exit", "Exit Program", "exit", KeyEvent.VK_F4);
            putValue(Action.MNEMONIC_KEY, new Integer('x'));
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            // if (!m_doc.m_bIsSaved) {
            if (!quit()) {
                return;
            }
            System.exit(0);
        }
    } // class ActionQuit

    boolean quit() {
        if (doc.validateModel() == DOC_STATUS.DIRTY) {
            int result = JOptionPane.showConfirmDialog(null,
                    "Do you want to save the Beast specification?",
                    "Save before closing?", JOptionPane.YES_NO_CANCEL_OPTION);
            Log.err.println("result=" + result);
            if (result == JOptionPane.CANCEL_OPTION) {
                return false;
            }
            if (result == JOptionPane.YES_OPTION) {
                if (!saveAs()) {
                    return false;
                }
            }
        }
        return true;
    }

    ViewPanelCheckBoxMenuItem[] m_viewPanelCheckBoxMenuItems;

    class ViewPanelCheckBoxMenuItem extends JCheckBoxMenuItem {
        private static final long serialVersionUID = 1L;
        int m_iPanel;

        ViewPanelCheckBoxMenuItem(int panelIndex) {
            super("Show "
                    + doc.beautiConfig.panels.get(panelIndex).nameInput.get()
                    + " panel",
                    doc.beautiConfig.panels.get(panelIndex).isVisibleInput.get());
            m_iPanel = panelIndex;
            if (m_viewPanelCheckBoxMenuItems == null) {
                m_viewPanelCheckBoxMenuItems = new ViewPanelCheckBoxMenuItem[doc.beautiConfig.panels
                        .size()];
            }
            m_viewPanelCheckBoxMenuItems[panelIndex] = this;
        } // c'tor

        void doAction() {
            toggleVisible(m_iPanel);
        }
    }

    ;

    /**
     * makes all panels visible *
     */
    class ActionViewAllPanels extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionViewAllPanels() {
            super("View all", "View all panels", "viewall", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            for (int panelNr = 0; panelNr < isPaneIsVisible.length; panelNr++) {
                if (!isPaneIsVisible[panelNr]) {
                    toggleVisible(panelNr);
                    m_viewPanelCheckBoxMenuItems[panelNr].setState(true);
                }
            }
        } // actionPerformed
    } // class ActionViewAllPanels

    class ActionAbout extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionAbout() {
            super("About", "Help about", "about", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            BEASTVersion2 version = new BEASTVersion2();
            JOptionPane.showMessageDialog(null, version.getCredits(),
                    "About Beauti " + version.getVersionString() + 
                    " Java version " + System.getProperty("java.version"), JOptionPane.PLAIN_MESSAGE,
                    Utils.getIcon(BEAUTI_ICON));
        }
    } // class ActionAbout

    class ActionHelp extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionHelp() {
            super("Help", "Help on current panel", "help", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            HelpBrowser b = new HelpBrowser(currentTab.config.getType());
            int size = UIManager.getFont("Label.font").getSize();
            b.setSize(800 * size / 13, 800 * size / 13);
            b.setVisible(true);
            b.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    } // class ActionHelp

    class ActionMsgs extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionMsgs() {
            super("Messages", "Show information, warning and error messages", "msgs", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
        	if (BeautiDoc.baos == null) {
        		JOptionPane.showMessageDialog(frame, "<html>Error and warning messages are printed to Stdout and Stderr<br>" +
        				"To show them here, start BEAUti with the -capture argument.</html>");
        	} else {
	        	String msgs = BeautiDoc.baos.toString();
	        	JTextArea textArea = new JTextArea(msgs);
	        	textArea.setRows(40);
	        	textArea.setColumns(50);
	        	textArea.setEditable(true);
	        	JScrollPane scroller = new JScrollPane(textArea);
	        	JOptionPane.showMessageDialog(frame, scroller);
        	}
        }
    }

    class ActionCitation extends MyAction implements ClipboardOwner {
        private static final long serialVersionUID = -1;

        public ActionCitation() {
            super("Citation",
                    "Show appropriate citations and copy to clipboard",
                    "citation", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            String citations = doc.mcmc.get().getCitations();
            try {
                StringSelection stringSelection = new StringSelection(
                        citations);
                Clipboard clipboard = Toolkit.getDefaultToolkit()
                        .getSystemClipboard();
                clipboard.setContents(stringSelection, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            JOptionPane.showMessageDialog(null, citations
                    + "\nCitations copied to clipboard",
                    "Citation(s) applicable to this model:",
                    JOptionPane.INFORMATION_MESSAGE);

        } // getCitations

        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            // do nothing
        }
    } // class ActionAbout

    class ActionViewModel extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionViewModel() {
            super("View model", "View model graph", "model", -1);
        } // c'tor

        @Override
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

    public void refreshPanel() {
        try {
            BeautiPanel panel = (BeautiPanel) getSelectedComponent();
            if (panel != null) {
                this.doc.determinePartitions();
                panel.updateList();
                panel.refreshPanel();
            }
            requestFocus();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);
        createFileMenu();

        JMenu modeMenu = new JMenu("Mode");
        menuBar.add(modeMenu);
        modeMenu.setMnemonic('M');

        autoSetClockRate = new JCheckBoxMenuItem(
                "Automatic set clock rate", this.doc.autoSetClockRate);
        autoSetClockRate.addActionListener(ae -> {
                doc.autoSetClockRate = autoSetClockRate.getState();
                refreshPanel();
            });
        modeMenu.add(autoSetClockRate);

        allowLinking = new JCheckBoxMenuItem(
                "Allow parameter linking", this.doc.allowLinking);
        allowLinking.addActionListener(ae -> {
                doc.allowLinking = allowLinking.getState();
                doc.determineLinks();
                refreshPanel();
            });
        modeMenu.add(allowLinking);

        autoUpdateFixMeanSubstRate = new JCheckBoxMenuItem(
                "Automatic set fix mean substitution rate flag", this.doc.autoUpdateFixMeanSubstRate);
        autoUpdateFixMeanSubstRate.addActionListener(ae -> {
                doc.autoUpdateFixMeanSubstRate = autoUpdateFixMeanSubstRate.getState();
                refreshPanel();
            });
        modeMenu.add(autoUpdateFixMeanSubstRate);

        // final JCheckBoxMenuItem muteSound = new
        // JCheckBoxMenuItem("Mute sound", false);
        // muteSound.addActionListener(new ActionListener() {
        // public void actionPerformed(ActionEvent ae) {
        // BeautiPanel.soundIsPlaying = !muteSound.getState();
        // refreshPanel();
        // }
        // });
        // modeMenu.add(muteSound);

        viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        viewMenu.setMnemonic('V');
        setUpViewMenu();

        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        helpMenu.setMnemonic('H');
        helpMenu.add(a_help);
        helpMenu.add(a_msgs);
        helpMenu.add(a_citation);
        helpMenu.add(a_viewModel);
        if (!Utils.isMac() || Utils6.isMajorLower(Utils6.JAVA_1_8)) {
            helpMenu.add(a_about);
        }

        setMenuVisibiliy("", menuBar);

        return menuBar;
    } // makeMenuBar

    private void createFileMenu() {
    	// first clear menu
   		fileMenu.removeAll();

        fileMenu.add(a_new);
        fileMenu.add(a_load);
        fileMenu.addSeparator();
        addAlignmentProviderMenus(fileMenu);
        fileMenu.addSeparator();
        templateMenu = new JMenu("Template");
        fileMenu.add(templateMenu);
        List<AbstractAction> templateActions = getTemplateActions();
        for (AbstractAction a : templateActions) {
            templateMenu.add(a);
        }
        JMenu workDirMenu = new JMenu("Set working dir");
        fileMenu.add(workDirMenu);
        List<AbstractAction> workDirMenuActions = getWorkDirActions();
        for (AbstractAction a : workDirMenuActions) {
        	workDirMenu.add(a);
        }
        templateMenu.addSeparator();
        templateMenu.add(a_template);
        fileMenu.add(a_managePackages);
        fileMenu.add(a_clearClassPath);
        fileMenu.add(a_appLauncher);
        fileMenu.addSeparator();
        fileMenu.add(a_save);
        fileMenu.add(a_saveas);
        if (!Utils.isMac()) {
            fileMenu.addSeparator();
            fileMenu.add(a_close);
            fileMenu.add(a_quit);
        }
	}

	private void addAlignmentProviderMenus(JMenu fileMenu) {
        List<BeautiAlignmentProvider> providers = doc.beautiConfig.alignmentProvider;
        for (BeautiAlignmentProvider provider : providers) {
        	AbstractAction action = new AbstractAction() {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
		            try {
		                setCursor(new Cursor(Cursor.WAIT_CURSOR));

		                // get user-specified alignments
				        List<BEASTInterface> beastObjects = provider.getAlignments(doc);
				        if (beastObjects != null) {
					        for (BEASTInterface o : beastObjects) {
					        	if (o instanceof Alignment) {
					        		try {
					        			BeautiDoc.createTaxonSet((Alignment) o, doc);
					        		} catch(Exception ex) {
					        			ex.printStackTrace();
					        		}
					        	}
					        }
				        }

		                doc.connectModel();
		                doc.fireDocHasChanged();
		                
				        if (beastObjects != null) {
					        for (BEASTInterface o : beastObjects) {
					        	if (o instanceof MRCAPrior) {
				        			doc.addMRCAPrior((MRCAPrior) o);
					        	}
					        }
				        }
		                a_save.setEnabled(true);
		                a_saveas.setEnabled(true);
		            } catch (Exception exx) {
		                exx.printStackTrace();

		                String text = "Something went wrong importing the alignment:\n";
		                JTextArea textArea = new JTextArea(text);
		                textArea.setColumns(30);
		                textArea.setLineWrap(true);
		                textArea.setWrapStyleWord(true);
		                textArea.append(exx.getMessage());
		                textArea.setSize(textArea.getPreferredSize().width, 1);
		                textArea.setOpaque(false);
		                JOptionPane.showMessageDialog(null, textArea,
		                        "Error importing alignment",
		                        JOptionPane.WARNING_MESSAGE);
		            }
		            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			};
            String providerInfo = provider.toString().replaceAll("Add ", "Add partition for ");
            action.putValue(Action.SHORT_DESCRIPTION, providerInfo);
            action.putValue(Action.LONG_DESCRIPTION, providerInfo);
            action.putValue(Action.NAME, provider.toString());
        	fileMenu.add(action);
        }
	}

	
	void setUpViewMenu() {
        m_viewPanelCheckBoxMenuItems = null;
        viewMenu.removeAll();
        for (int panelIndex = 0; panelIndex < doc.beautiConfig.panels.size(); panelIndex++) {
            final ViewPanelCheckBoxMenuItem viewPanelAction = new ViewPanelCheckBoxMenuItem(
                    panelIndex);
            viewPanelAction.addActionListener(ae -> {
                    viewPanelAction.doAction();
                });
            viewMenu.add(viewPanelAction);
        }
        viewMenu.addSeparator();
        viewMenu.add(a_viewall);
        
        viewMenu.addSeparator();
        MyAction zoomIn = new MyAction("Zoom in", "Increase font size of all components", null, KeyEvent.VK_EQUALS) {
 			private static final long serialVersionUID = 1L;

			@Override
        	public void actionPerformed(ActionEvent ae) {
				int size = UIManager.getFont("Label.font").getSize();
            	Utils.setFontSize(size + 1);
            	Utils.saveBeautiProperty("fontsize", (size + 1) + "");
        		refreshPanel();
        		repaint();
        	}
        };
        MyAction zoomOut = new MyAction("Zoom out", "Decrease font size of all components", null, KeyEvent.VK_MINUS) {
			private static final long serialVersionUID = 1L;

			@Override
        	public void actionPerformed(ActionEvent ae) {
				int size = UIManager.getFont("Label.font").getSize();
            	Utils.setFontSize(Math.max(size - 1, 4));
            	Utils.saveBeautiProperty("fontsize", Math.max(size - 1, 4) + "");
        		refreshPanel();
        		repaint();
        	}
        };
        viewMenu.add(zoomIn);
        viewMenu.add(zoomOut);

    }

    class TemplateAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        String m_sFileName;
        String templateInfo;

        public TemplateAction(File file) {
            super("xx");
            m_sFileName = file.getAbsolutePath();
            String fileSep = System.getProperty("file.separator");
            if (fileSep.equals("\\")) {
                fileSep = "\\";
            }
            int i = m_sFileName.lastIndexOf(fileSep) + 1;
            String name = m_sFileName.substring(
                    i, m_sFileName.length() - 4);
            putValue(Action.NAME, name);
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory
                        .newInstance();
                Document doc = factory.newDocumentBuilder().parse(file);
                doc.normalize();
                // get name and version of add-on
                Element template = doc.getDocumentElement();
                templateInfo = template.getAttribute("templateinfo");
                if (templateInfo == null || templateInfo.length() == 0) {
                    templateInfo = "switch to " + name + " template";
                }
                //templateInfo = "<html>" + templateInfo + "</html>";
                putValue(Action.SHORT_DESCRIPTION, templateInfo);
                putValue(Action.LONG_DESCRIPTION, templateInfo);
            } catch (Exception e) {
                // ignore
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (doc.validateModel() == DOC_STATUS.NO_DOCUMENT) {
                    doc.loadNewTemplate(m_sFileName);
                } else if (JOptionPane.showConfirmDialog(frame,
                        "Changing templates means the information input so far will be lost. "
                                + "Are you sure you want to change templates?",
                        "Are you sure?", JOptionPane.YES_NO_CANCEL_OPTION) == JOptionPane.YES_OPTION) {
                    doc.loadNewTemplate(m_sFileName);
                }
                createFileMenu();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Something went wrong loading the template: "
                                + ex.getMessage());
            }
        }

    }

	private List<AbstractAction> getTemplateActions() {
        List<AbstractAction> actions = new ArrayList<>();
        List<String> beastDirectories = PackageManager.getBeastDirectories();
        for (String dirName : beastDirectories) {
            File dir = new File(dirName + "/templates");
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
                            String xml2 = BeautiDoc.load(template.getAbsolutePath());
                            if (xml2.contains("templateinfo=")) {
                            	String fileName = template.getName();
                                fileName = fileName.substring(0, fileName.length() - 4);
                                boolean duplicate = false;
                            	for (AbstractAction action : actions) {
                            		String name = action.getValue(Action.NAME).toString();
                            		if (name.equals(fileName)) {
                            			duplicate = true;
                            		}
                            	}
                            	if (!duplicate) {
                            		actions.add(new TemplateAction(template));
                            	}
                            }
                        } catch (Exception e) {
                        	Log.warning.println(e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private List<AbstractAction> getWorkDirActions() {
        List<AbstractAction> actions = new ArrayList<>();
        List<String> beastDirectories = PackageManager.getBeastDirectories();
        Set<String> doneDirs = new HashSet<>();
        for (String dir : beastDirectories) {
        	if (!doneDirs.contains(dir)) {
	        	doneDirs.add(dir);
	        	String exampledir = dir + File.separator+ "examples";
	        	if (new File(exampledir).exists()) {
		        	AbstractAction action = new AbstractAction() {
						private static final long serialVersionUID = 1L;
	
						@Override
						public void actionPerformed(ActionEvent e) {
							g_sDir = dir;
						}
	
		            };
		            String workDirInfo = "Set working directory to " + dir;
		            String name = dir;
		            if (name.indexOf(File.separator) >= 0) {
		            	name = dir.substring(dir.lastIndexOf(File.separator) + 1);
		            }
		            action.putValue(Action.SHORT_DESCRIPTION, workDirInfo);
		            action.putValue(Action.LONG_DESCRIPTION, workDirInfo);
		            action.putValue(Action.NAME, name);
		            actions.add(action);
	        	}
        	}
        }
        return actions;
    }

    void setMenuVisibiliy(String parentName, Component c) {
        String name = "";
        if (c instanceof JMenu) {
            name = ((JMenu) c).getText();
        } else if (c instanceof JMenuItem) {
            name = ((JMenuItem) c).getText();
        }
        if (name.length() > 0
                && doc.beautiConfig.menuIsInvisible(parentName + name)) {
            c.setVisible(false);
        }
        if (c instanceof JMenu) {
            for (Component x : ((JMenu) c).getMenuComponents()) {
                setMenuVisibiliy(parentName + name
                        + (name.length() > 0 ? "." : ""), x);
            }
        } else if (c instanceof Container) {
            for (int i = 0; i < ((Container) c).getComponentCount(); i++) {
                setMenuVisibiliy(parentName, ((Container) c).getComponent(i));
            }
        }
    }

    // hide panels as indicated in the hidepanels attribute in the XML template,
    // or use default tabs to hide otherwise.
    public void hidePanels() {
        // for (int panelIndex = 0; panelIndex < BeautiConfig.g_panels.size(); panelIndex++)
        // {
        // BeautiPanelConfig panelConfig = BeautiConfig.g_panels.get(panelIndex);
        // if (!panelConfig.m_bIsVisibleInput.get()) {
        // toggleVisible(panelIndex);
        // }
        // }
    } // hidePanels

    public void setUpPanels() throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        isInitialising = true;
        // remove any existing tabs
        if (getTabCount() > 0) {
            while (getTabCount() > 0) {
                removeTabAt(0);
            }
            isPaneIsVisible = new boolean[doc.beautiConfig.panels.size()];
            Arrays.fill(isPaneIsVisible, true);
        }
        for (int panelIndex = 0; panelIndex < doc.beautiConfig.panels.size(); panelIndex++) {
            BeautiPanelConfig panelConfig = doc.beautiConfig.panels.get(panelIndex);
            isPaneIsVisible[panelIndex] = panelConfig.isVisibleInput.get();
        }
        // add panels according to BeautiConfig
        panels = new BeautiPanel[doc.beautiConfig.panels.size()];
        for (int panelIndex = 0; panelIndex < doc.beautiConfig.panels.size(); panelIndex++) {
            BeautiPanelConfig panelConfig = doc.beautiConfig.panels.get(panelIndex);
            panels[panelIndex] = new BeautiPanel(panelIndex, this.doc, panelConfig);
            addTab(doc.beautiConfig.getButtonLabel(this, panelConfig.getName()),
                    null, panels[panelIndex], panelConfig.getTipText());
        }

        for (int panelIndex = doc.beautiConfig.panels.size() - 1; panelIndex >= 0; panelIndex--) {
            if (!isPaneIsVisible[panelIndex]) {
                removeTabAt(panelIndex);
            }
        }
        isInitialising = false;
    }

    /**
     * record number of frames. If the last frame is closed, exit the app. *
     */
    static int BEAUtiIntances = 0;
    static public boolean isInBeauti() {
    	return BEAUtiIntances > 0;
    }

    private static String usage() {
        return "java Beauti [options]\n" + "where options can be one of the following:\n"
                + "-template [template file] : BEAUti template to be used. Default templates/Standard.xml\n"
        		+ "-nex [nexus data file] : nexus file to be read using template, multiple -nex arguments are allowed\n"
                + "-xmldat [beast xml file] : as -nex but with BEAST 1 or 2 xml file instead of nexus file\n"
                + "-xml [beast file] : BEAST 2 XML file to be loaded\n"
                + "-exitaction [writexml|usetemplate|usexml] : what to do after processing arguments\n"
                + "-out [output file name] : file to be written\n"
                + "-capture : captures stdout and stderr and make them available under Help/Messages menu\n"
                + "-v, -version : print version\n"
                + "-h, -help : print this help message\n";
    }

   

    public static Beauti main2(String[] args) {
    	Utils6.startSplashScreen();
    	Utils6.logToSplashScreen("Initialising BEAUti");
        try {
        	ByteArrayOutputStream baos = null;
            for (String arg : args) {
            	if (arg.equals("-v") || arg.equals("-version")) {
                    System.out.println((new BEASTVersion2()).getVersionString());
                    System.exit(0);
            	}
            	if (arg.equals("-h") || arg.equals("-help")) {
                    System.out.println(usage());
                    System.exit(0);
            	}
            	if (arg.equals("-capture")) {
            		final PrintStream beautiLog = System.err;
                	baos = new ByteArrayOutputStream() {
                		@Override
                		public synchronized void write(byte[] b, int off, int len) {
                			super.write(b, off, len);
                			beautiLog.write(b, off, len);
                		};

                		@Override
                		public synchronized void write(int b) {
                			super.write(b);
                			beautiLog.write(b);
                		};

                		@Override
                		public void write(byte[] b) throws java.io.IOException {
                			super.write(b);
                			beautiLog.write(b);
                		};

                		@Override
                		public void flush() throws java.io.IOException {
                			super.flush();
                			beautiLog.flush();
                		};

                		@Override
                		public void close() throws IOException {
                			super.close();
                			beautiLog.close();
                		}
                	};

                	PrintStream p = new PrintStream(baos);
                	System.setOut(p);
                	System.setErr(p);
                	Log.err = p;
                	Log.warning = p;
                	Log.info = p;
                	Log.debug = p;
                	Log.trace = p;
            	}
            }

            PackageManager.loadExternalJars();
            //if (!Utils.isMac()) {
            	Utils.loadUIManager();
            //}
            BEASTObjectPanel.init();

            BeautiDoc doc = new BeautiDoc();
            BeautiDoc.baos = baos;
            if (doc.parseArgs(args) == ActionOnExit.WRITE_XML) {
                return null;
            }

            final Beauti beauti = new Beauti(doc);

            if (Utils.isMac() && Utils6.isMajorAtLeast(Utils6.JAVA_1_8)) {
                // set up application about-menu for Mac
                // Mac-only stuff
                try {
                    URL url = ClassLoader.getSystemResource(ModelBuilder.ICONPATH + "beauti.png");
                    Icon icon = null;
                    if (url != null) {
                        icon = new ImageIcon(url);
                    } else {
                    	Log.warning.println("Unable to find image: " + ModelBuilder.ICONPATH + "beauti.png");
                    }
                    jam.framework.Application application = new jam.framework.MultiDocApplication(null, "BEAUti", "about", icon) {

                        @Override
                        protected JFrame getDefaultFrame() {
                            return null;
                        }

                        @Override
                        public void doQuit() {
                            beauti.a_quit.actionPerformed(null);
                        }

                        @Override
                        public void doAbout() {
                            beauti.a_about.actionPerformed(null);
                        }

                        @Override
                        public DocumentFrame doOpenFile(File file) {
                            return null;
                        }

                        @Override
                        public DocumentFrame doNew() {
                            return null;
                        }
                    };

                    // https://github.com/CompEvol/beast2/issues/805
                    if (Utils6.isMajorAtLeast(Utils6.JAVA_9)) // >= Java 9
                        beast.app.util.Utils.macOSXRegistration(application);
                    else // <= Java 8
                        jam.mac.Utils.macOSXRegistration(application);
                } catch (Exception e) {
                    // ignore
                }
                if (Utils6.isMajorLower(Utils6.JAVA_9)) {
                    try {
                        Class<?> class_ = Class.forName("jam.maconly.OSXAdapter");
                        Method method = class_.getMethod("enablePrefs", boolean.class);
                        method.invoke(null, false);
                    } catch (java.lang.NoSuchMethodException e) {
                        // ignore
                    }
                }
            }
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
                            beauti.currentTab.config
                                    .sync(beauti.currentTab.partitionIndex);
                        }
                        BeautiPanel panel = (BeautiPanel) beauti
                                .getSelectedComponent();
                        beauti.currentTab = panel;
                        beauti.refreshPanel();
                    }
                }
            });

            beauti.setVisible(true);
            beauti.refreshPanel();
            JFrame frame = new JFrame("BEAUti 2: " + doc.getTemplateName()
                    + " " + doc.getFileName());
            beauti.frame = frame;
            ImageIcon icon = Utils.getIcon(BEAUTI_ICON);
            if (icon != null) {
                frame.setIconImage(icon.getImage());
            }

            JMenuBar menuBar = beauti.makeMenuBar();
            frame.setJMenuBar(menuBar);

            if (doc.getFileName() != null || doc.alignments.size() > 0) {
                beauti.a_save.setEnabled(true);
                beauti.a_saveas.setEnabled(true);
            }

            frame.add(beauti);
            int size = UIManager.getFont("Label.font").getSize();
            frame.setSize(1024 * size / 13, 768 * size / 13);
            frame.setLocation(BEAUtiIntances * 10, BEAUtiIntances * 10);
            frame.setVisible(true);

            // check file needs to be save on closing main frame
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            BEAUtiIntances++;
            frame.addWindowListener(new WindowAdapter() {
                @Override
				public void windowClosing(WindowEvent e) {
                    if (!beauti.quit()) {
                        return;
                    }
                    JFrame frame = (JFrame) e.getSource();
                    frame.dispose();
                    BEAUtiIntances--;
                    if (BEAUtiIntances == 0) {
                        System.exit(0);
                    }
                }
            });

            // Toolkit toolkit = Toolkit.getDefaultToolkit();
            // // PropertyChangeListener plistener = new
            // PropertyChangeListener() {
            // // @Override
            // // public void propertyChange(PropertyChangeEvent event) {
            // // Object o = event.getSource();
            // // Object o2 = event.getNewValue();
            // // event.getPropertyName();
            // // System.err.println(">>> " + event.getPropertyName() + " " +
            // o.getClass().getName() + "\n" + o2.getClass().getName());
            // // }
            // // };
            // AWTEventListener listener = new AWTEventListener() {
            // @Override
            // public void eventDispatched(AWTEvent event) {
            // Object o = event.getSource();
            // String label = "";
            // try {
            // Method method = o.getClass().getMethod("getText", Object.class);
            // label = (String) method.invoke(o);
            // } catch (Exception e) {
            // // TODO: handle exception
            // }
            // if (event.paramString().matches(".*\\([0-9]*,[0-9]*\\).*")) {
            // String s = event.paramString();
            // String sx = s.substring(s.indexOf('(') + 1);
            // String sy = sx;
            // sx = sx.substring(0, sx.indexOf(','));
            // sy = sy.substring(sy.indexOf(',') + 1, sy.indexOf(')'));
            // int x = Integer.parseInt(sx);
            // int y = Integer.parseInt(sy);
            // Component c = beauti.findComponentAt(x, y);
            // if (c != null) {
            // System.err.println(c.getClass().getName());
            // }
            // }
            //
            // System.err.println(label + " " + event.paramString() + " " +
            // o.getClass().getName());
            //
            // }
            // };
            // toolkit.addAWTEventListener(listener,
            // AWTEvent.ACTION_EVENT_MASK|AWTEvent.ITEM_EVENT_MASK|AWTEvent.MOUSE_EVENT_MASK);
            // // beauti.addPropertyChangeListener(plistener);

        	Utils6.endSplashScreen();
            return beauti;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    } // main2

    public static void main(String[] args) {
        main2(args);

        // check for new packages in the background
        new Thread() {
        	public void run() {
        		String statuString = Utils.getBeautiProperty("package.update.status");
        		if (statuString == null) {
        			statuString = PackageManager.UpdateStatus.AUTO_CHECK_AND_ASK.toString(); 
        		}
        		PackageManager.UpdateStatus updateStatus = PackageManager.UpdateStatus.valueOf(statuString);
        		PackageManager.updatePackages(updateStatus, true);
        	};
        }.start();
    }

} // class Beauti

