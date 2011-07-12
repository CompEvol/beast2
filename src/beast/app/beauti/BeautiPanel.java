package beast.app.beauti;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;

import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import beast.app.draw.HelpBrowser;
import beast.app.draw.InputEditor;
import beast.app.draw.InputEditor.BUTTONSTATUS;
import beast.app.draw.PluginPanel;
import beast.app.draw.SmallButton;
import beast.app.draw.InputEditor.EXPAND;
import beast.core.Input;
import beast.core.Plugin;
import beast.evolution.alignment.Alignment;

/** panel making up each of the tabs in Beauti **/
public class BeautiPanel extends JPanel implements ListSelectionListener {
	private static final long serialVersionUID = 1L;
	
    /** document that this panel applies to **/
    BeautiDoc m_doc;
    /** configuration for this panel **/
    BeautiPanelConfig m_config;
    
    /** panel number **/
    int m_iPanel;
    
    /** partition currently on display **/
    int m_iPartition = 0;

    /** box containing the list of partitions, to make (in)visible on update **/
	Box m_listBox;
	/** list of partitions in m_listBox **/
	JList m_listOfPartitions;
	/** model for m_listOfPartitions **/
    DefaultListModel m_listModel;
    
    
    /** component containing main input editor **/ 
	Component m_centralComponent = null;

    public BeautiPanel(int iPanel, BeautiDoc doc, BeautiPanelConfig config) throws Exception {
		m_doc = doc;
		m_iPanel = iPanel;
		
        SmallButton helpButton2 = new SmallButton("?", true);
        helpButton2.setToolTipText("Show help for this plugin");
        helpButton2.addActionListener(new ActionListener() {
            // implementation ActionListener
            public void actionPerformed(ActionEvent e) {
                setCursor(new Cursor(Cursor.WAIT_CURSOR));
                HelpBrowser b = new HelpBrowser(m_config.getType());
                b.setSize(800, 800);
                b.setVisible(true);
                b.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    	add(helpButton2);
		
		
	    setLayout(new BorderLayout());
	    m_config = config;
	    refreshPanel();
	    addParitionPanel(m_config.hasPartition(), iPanel);
	} // c'tor
    
	
    void addParitionPanel(boolean bHasPartion, int iPanel) {
		Box box = Box.createVerticalBox();
    	if (bHasPartion) {
    		m_listBox = createList(); 
			box.add(m_listBox);
    	}
		box.add(Box.createVerticalGlue());
		box.add(new JLabel(getIcon(iPanel, m_config)));
    	add(box, BorderLayout.WEST);
	}
	
    Box createList() {
		Box partitionBox = Box.createVerticalBox();
		partitionBox.setAlignmentX(LEFT_ALIGNMENT);
		partitionBox.add(new JLabel("partition"));
        m_listModel = new DefaultListModel();
    	m_listOfPartitions = new JList(m_listModel);

    	Dimension size = new Dimension(100,300);
    	m_listOfPartitions.setFixedCellWidth(100);
//    	m_listOfPartitions.setSize(size);
    	m_listOfPartitions.setPreferredSize(size);
//    	m_listOfPartitions.setMinimumSize(size);
//    	m_listOfPartitions.setBounds(0, 0, 100, 100);
    	
    	m_listOfPartitions.addListSelectionListener(this);
    	for (Alignment data : m_doc.m_alignments.get()) {
    		m_listModel.addElement(data);
    	}
    	m_listOfPartitions.setBorder(new BevelBorder(BevelBorder.RAISED));
    	partitionBox.add(m_listOfPartitions);
    	partitionBox.setBorder(new EtchedBorder());
    	return partitionBox;
    }

	static ImageIcon getIcon(int iPanel, BeautiPanelConfig config) {
        String sIconLocation = BeautiInitDlg.ICONPATH + iPanel +".png";
        if (config != null) {
        	sIconLocation = BeautiInitDlg.ICONPATH + config.getIcon();
        }
        try {
	        URL url = (URL)ClassLoader.getSystemResource(sIconLocation);
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
	
	
	static BeautiPanel g_currentPanel = null;
	
	void refreshPanel() throws Exception {
//		if (g_currentPanel != null) {
//			g_currentPanel.m_config.sync(m_iPartition);
//		}
		m_doc.scrubAll(true);
		refreshInputPanel();
		if (m_listBox != null) {
			m_listBox.setVisible(m_doc.m_alignments.get().size() > 1);
		}
		g_currentPanel = this;
	}
	
	void refreshInputPanel(Plugin plugin, Input<?> input, boolean bAddButtons, EXPAND bForceExpansion) throws Exception {
		if (m_centralComponent != null) {
			remove(m_centralComponent);
		}
	    if (input != null && input.get() != null) {
	    	BUTTONSTATUS bs = m_config.m_buttonStatusInput.get();
	        InputEditor inputEditor = PluginPanel.createInputEditor(input, plugin, bAddButtons, bForceExpansion, bs, null);
	        Box box = Box.createVerticalBox();
	        box.add(inputEditor);
	        // RRB: is there a better way than just pooring in glue at the bottom?
	        for (int i = 0; i < 30; i++) {
	        	box.add(Box.createGlue());
	        }
	        JScrollPane scroller = new JScrollPane(box);
	        m_centralComponent = scroller;
	    } else {
	        m_centralComponent = new JLabel("Nothing to be specified");
	    }
        add(m_centralComponent, BorderLayout.CENTER);
	}

	void refreshInputPanel() throws Exception {
		InputEditor.g_currentInputEditors.clear();
		InputEditor.g_nLabelWidth = m_config.m_nLabelWidthInput.get();
		Plugin plugin = m_config;
		Input<?> input = m_config.resolveInput(m_doc, m_iPartition);
		
		boolean bAddButtons = m_config.addButtons();
		EXPAND bForceExpansion = m_config.forceExpansion();
		refreshInputPanel(plugin, input, bAddButtons, bForceExpansion);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		System.err.print("BeautiPanel::valueChanged " + m_iPartition + " => ");
		if (e != null) {
			m_config.sync(m_iPartition);
			if (m_listOfPartitions != null) {
				m_iPartition = m_listOfPartitions.getSelectedIndex();
			}
		}
		System.err.println(m_iPartition);
		try {
			refreshPanel();

			m_centralComponent.repaint();
			repaint();
			
			// hack to ensure m_centralComponent is repainted RRB: is there a better way???
			Frame frame = Frame.getFrames()[Frame.getFrames().length - 1];
			frame.setSize(frame.getSize());
			//Frame frame = frames[frames.length - 1];
//			Dimension size = frames[frames.length-1].getSize();
//			frames[frames.length-1].setSize(size);

//			m_centralComponent.repaint();
//			m_centralComponent.requestFocusInWindow();
			m_centralComponent.requestFocus();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

} // class BeautiPanel
