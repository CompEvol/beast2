package beast.app.beauti;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
//import java.awt.Panel;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import beast.app.beauti.BeautiPanelConfig.Partition;
import beast.app.draw.InputEditor;
import beast.app.draw.InputEditor.ExpandOption;
import beast.core.Input;
import beast.core.Plugin;

/**
 * panel making up each of the tabs in Beauti *
 */
public class BeautiPanel extends JPanel implements ListSelectionListener {
    private static final long serialVersionUID = 1L;
    public final static String ICONPATH = "beast/app/beauti/";

    /**
     * document that this panel applies to *
     */
    BeautiDoc doc;

    public BeautiDoc getDoc() {
        return doc;
    }

    /**
     * configuration for this panel *
     */
    public BeautiPanelConfig config;

    /**
     * panel number *
     */
    int iPanel;

    /**
     * partition currently on display *
     */
    public int iPartition = 0;

    /**
     * box containing the list of partitions, to make (in)visible on update *
     */
    Box partitionBox;
    /**
     * list of partitions in m_listBox *
     */
    JList listOfPartitions;
    /**
     * model for m_listOfPartitions *
     */
    DefaultListModel listModel;


    /**
     * component containing main input editor *
     */
    Component centralComponent = null;

    public BeautiPanel() {
    }

    public BeautiPanel(int iPanel, BeautiDoc doc, BeautiPanelConfig config) throws Exception {
        this.doc = doc;
        this.iPanel = iPanel;

//        SmallButton helpButton2 = new SmallButton("?", true);
//        helpButton2.setToolTipText("Show help for this plugin");
//        helpButton2.addActionListener(new ActionListener() {
//            // implementation ActionListener
//            public void actionPerformed(ActionEvent e) {
//                setCursor(new Cursor(Cursor.WAIT_CURSOR));
//                HelpBrowser b = new HelpBrowser(m_config.getType());
//                b.setSize(800, 800);
//                b.setVisible(true);
//                b.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
//            }
//        });
//    	add(helpButton2);


        setLayout(new BorderLayout());
        this.config = config;
        refreshPanel();
        addParitionPanel(this.config.hasPartition(), iPanel);

        setOpaque(false);
    } // c'tor

    void addParitionPanel(Partition bHasPartion, int iPanel) {
        Box box = Box.createVerticalBox();
        if (bHasPartion != Partition.none) {
            box.add(createList());
        }
        box.add(Box.createVerticalGlue());
        box.add(new JLabel(getIcon(iPanel, config)));
        add(box, BorderLayout.WEST);
        if (listOfPartitions != null) {
            listOfPartitions.setSelectedIndex(iPartition);
        }
    }

    Box createList() {
        partitionBox = Box.createVerticalBox();
        partitionBox.setAlignmentX(LEFT_ALIGNMENT);
        partitionBox.add(new JLabel("partition"));
        listModel = new DefaultListModel();
        listOfPartitions = new JList(listModel);
        listOfPartitions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        Dimension size = new Dimension(100, 300);
        listOfPartitions.setFixedCellWidth(100);
//    	m_listOfPartitions.setSize(size);
        listOfPartitions.setPreferredSize(size);
//    	m_listOfPartitions.setMinimumSize(size);
//    	m_listOfPartitions.setBounds(0, 0, 100, 100);

        listOfPartitions.addListSelectionListener(this);
        updateList();
        listOfPartitions.setBorder(new BevelBorder(BevelBorder.RAISED));
        partitionBox.add(listOfPartitions);
        partitionBox.setBorder(new EtchedBorder());
        return partitionBox;
    }

    public void updateList() {
        if (listModel == null) {
            return;
        }
        listModel.clear();
        for (Plugin partition : doc.getPartitions(config.bHasPartitionsInput.get().toString())) {
            String sPartition = partition.getID();
            sPartition = sPartition.substring(sPartition.lastIndexOf('.') + 1);
            listModel.addElement(sPartition);
        }
        if (iPartition >= 0 && listModel.size() > 0)
            listOfPartitions.setSelectedIndex(iPartition);
    }

    public static ImageIcon getIcon(int iPanel, BeautiPanelConfig config) {
        String sIconLocation = ICONPATH + iPanel + ".png";
        if (config != null) {
            sIconLocation = ICONPATH + config.getIcon();
        }
        try {
            URL url = (URL) ClassLoader.getSystemResource(sIconLocation);
            if (url == null) {
                System.err.println("Cannot find icon " + sIconLocation);
                return null;
            }
            ImageIcon icon = new ImageIcon(url);
            return icon;
        } catch (Exception e) {
            System.err.println("Cannot load icon " + sIconLocation + " " + e.getMessage());
            return null;
        }

    }

    // AR remove globals (doesn't seem to be used anywhere)...
//	static BeautiPanel g_currentPanel = null;

    public void refreshPanel() throws Exception {
        if (doc.alignments.size() == 0) {
            return;
        }
        doc.scrubAll(true, false);

        refreshInputPanel();
        if (partitionBox != null && config.getType() != null) {
            partitionBox.setVisible(doc.getPartitions(config.getType()).size() > 1);
        }
//		g_currentPanel = this;
    }

    void refreshInputPanel(Plugin plugin, Input<?> input, boolean bAddButtons, InputEditor.ExpandOption bForceExpansion) throws Exception {
        if (centralComponent != null) {
            remove(centralComponent);
        }
        if (input != null && input.get() != null) {
            InputEditor.ButtonStatus bs = config.buttonStatusInput.get();
            InputEditor inputEditor = doc.getInpuEditorFactory().createInputEditor(input, plugin, bAddButtons, bForceExpansion, bs, null, doc);
            
            //Box box = Box.createVerticalBox();
            //box.add(inputEditor.getComponent());
            // RRB: is there a better way than just pooring in glue at the bottom?
            //for (int i = 0; i < 30; i++) {

            //box.add(Box.createVerticalStrut(1024 - ((Component)inputEditor).getPreferredSize().height));
            //}
            //JScrollPane scroller = new JScrollPane(box);
            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.add(inputEditor.getComponent(), BorderLayout.CENTER);
            p.add(Box.createVerticalStrut(1024 - inputEditor.getComponent().getPreferredSize().height), BorderLayout.SOUTH);

            
            //p.setPreferredSize(new Dimension(1024,1024));
            JScrollPane scroller = new JScrollPane(p);
            centralComponent = scroller;
        } else {
            centralComponent = new JLabel("Nothing to be specified");
        }
        add(centralComponent, BorderLayout.CENTER);
    }

    void refreshInputPanel() throws Exception {
    	doc.currentInputEditors.clear();
        InputEditor.Base.g_nLabelWidth = config.nLabelWidthInput.get();
        Plugin plugin = config;
        Input<?> input = config.resolveInput(doc, iPartition);

        boolean bAddButtons = config.addButtons();
        ExpandOption bForceExpansion = config.forceExpansion();
        refreshInputPanel(plugin, input, bAddButtons, bForceExpansion);
    }


    public static boolean soundIsPlaying = false;

    public static synchronized void playSound(final String url) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    synchronized (this) {
                        if (soundIsPlaying) {
                            return;
                        }
                        soundIsPlaying = true;
                    }
                    Clip clip = AudioSystem.getClip();
                    AudioInputStream inputStream = AudioSystem.getAudioInputStream(getClass().getResourceAsStream("/beast/app/beauti/" + url));
                    clip.open(inputStream);
                    clip.start();
                    Thread.sleep(500);
                    synchronized (this) {
                        soundIsPlaying = false;
                    }
                } catch (Exception e) {
                    soundIsPlaying = false;
                    System.err.println(e.getMessage());
                }
            }
        }).start();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        //System.err.print("BeautiPanel::valueChanged " + m_iPartition + " => ");
        if (e != null) {
            config.sync(iPartition);
            if (listOfPartitions != null) {
                iPartition = Math.max(0, listOfPartitions.getSelectedIndex());
            }
        }
        BeautiPanel.playSound("woosh.wav");
        //System.err.println(m_iPartition);
        try {
            refreshPanel();

            centralComponent.repaint();
            repaint();

            // hack to ensure m_centralComponent is repainted RRB: is there a better way???
            if (Frame.getFrames().length == 0) {
                // happens at startup
                return;
            }
            Frame frame = Frame.getFrames()[Frame.getFrames().length - 1];
            frame.setSize(frame.getSize());
            //Frame frame = frames[frames.length - 1];
//			Dimension size = frames[frames.length-1].getSize();
//			frames[frames.length-1].setSize(size);

//			m_centralComponent.repaint();
//			m_centralComponent.requestFocusInWindow();
            centralComponent.requestFocus();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

} // class BeautiPanel
