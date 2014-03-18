package beast.app.beauti;


import beast.app.beauti.BeautiPanelConfig.Partition;
import beast.app.draw.InputEditor;
import beast.app.draw.InputEditor.ExpandOption;
import beast.core.BEASTObject;
import beast.core.Input;
import beast.core.MCMC;
import beast.core.util.CompoundDistribution;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.likelihood.GenericTreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.sitemodel.SiteModelInterface;
import beast.evolution.tree.TreeInterface;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.net.URL;
import java.util.List;



/**
 * panel making up each of the tabs in Beauti *
 */
public class BeautiPanel extends JPanel implements ListSelectionListener{
    private static final long serialVersionUID = 1L;
    public final static String ICONPATH = "beast/app/beauti/";

    static int partitionListPreferredWidth = 120;

    private JSplitPane splitPane;

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
    JComponent partitionComponent;
    /**
     * list of partitions in m_listBox *
     */
    JList listOfPartitions;
    /**
     * model for m_listOfPartitions *
     */
    DefaultListModel listModel;

    JScrollPane scroller;

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
        if (this.config.hasPartition() != Partition.none &&
                doc.getPartitions(config.bHasPartitionsInput.get().toString()).size() > 1) {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            add(splitPane,BorderLayout.CENTER);
        } else {
            splitPane = null;
        }

//        PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
//            public void propertyChange(PropertyChangeEvent changeEvent) {
//                JSplitPane sourceSplitPane = (JSplitPane) changeEvent.getSource();
//                String propertyName = changeEvent.getPropertyName();
//                if (propertyName.equals(JSplitPane.LAST_DIVIDER_LOCATION_PROPERTY)) {
//                    partitionListPreferredWidth = sourceSplitPane.getDividerLocation();
//
//                    Integer priorLast = (Integer) changeEvent.getOldValue();
//                    System.out.println("Prior last: " + priorLast);
//                    System.out.println("new: " + partitionListPreferredWidth);
//
//                }
//            }
//        };
//        splitPane.addPropertyChangeListener(propertyChangeListener);

        refreshPanel();
        addPartitionPanel(this.config.hasPartition(), iPanel);


        setOpaque(false);
    } // c'tor

    void addPartitionPanel(Partition bHasPartition, int iPanel) {
        Box box = Box.createVerticalBox();
        if (splitPane != null && bHasPartition != Partition.none) {
            box.add(createList());
        } else {
            return;
        }
        box.add(Box.createVerticalGlue());
        box.add(new JLabel(getIcon(iPanel, config)));

        //if (splitPane.getLeftComponent() != null) {
        //    Dimension d = splitPane.getLeftComponent().getSize();
        //}

        splitPane.add(box, JSplitPane.LEFT);
        if (listOfPartitions != null) {
            listOfPartitions.setSelectedIndex(iPartition);
        }
    }

    JComponent createList() {
        partitionComponent = new JPanel();
        partitionComponent.setLayout(new BorderLayout());
        JLabel partitionLabel = new JLabel("Partition");
        partitionLabel.setHorizontalAlignment(JLabel.CENTER);
        partitionComponent.add(partitionLabel, BorderLayout.NORTH);
        listModel = new DefaultListModel();
        listOfPartitions = new JList(listModel);
        listOfPartitions.setName("listOfPartitions");
        listOfPartitions.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        Dimension size = new Dimension(partitionListPreferredWidth, 300);
        //listOfPartitions.setFixedCellWidth(120);
//    	m_listOfPartitions.setSize(size);
        //listOfPartitions.setPreferredSize(size);
    	listOfPartitions.setMinimumSize(size);
//    	m_listOfPartitions.setBounds(0, 0, 100, 100);

        listOfPartitions.addListSelectionListener(this);
        updateList();
        listOfPartitions.setBorder(new BevelBorder(BevelBorder.RAISED));
        JScrollPane listPane = new JScrollPane(listOfPartitions);
        partitionComponent.add(listPane, BorderLayout.CENTER);
        partitionComponent.setBorder(new EtchedBorder());
        return partitionComponent;
    }

    public void updateList() {
        if (listModel == null) {
            return;
        }
        listModel.clear();
        if (listModel.size() > 0) {
            // this is a weird bit of code, since listModel.clear should ensure that size()==0, but it doesn't
            return;
        }
        String type = config.bHasPartitionsInput.get().toString();
        for (BEASTObject partition : doc.getPartitions(type)) {
        	if (type.equals("SiteModel")) {
        		partition = (BEASTObject) ((GenericTreeLikelihood) partition).siteModelInput.get();
        	} else if (type.equals("ClockModel")) {
        		partition = (BEASTObject) ((GenericTreeLikelihood) partition).branchRateModelInput.get();
        	} else if (type.equals("Tree")) {
        		partition = (BEASTObject) ((GenericTreeLikelihood) partition).treeInput.get();
        	}
            String sPartition = partition.getID();
            sPartition = sPartition.substring(sPartition.lastIndexOf('.') + 1);
            if (sPartition.length() > 1 && sPartition.charAt(1) == ':') {
            	sPartition = sPartition.substring(2);
            }
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
        return getIcon(sIconLocation);
    }

    public static ImageIcon getIcon(String sIconLocation) {
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
            refreshInputPanel();
            return;
        }
        doc.scrubAll(true, false);

        // toggle splitpane
        if (splitPane == null && config.hasPartition() != Partition.none &&
                doc.getPartitions(config.bHasPartitionsInput.get().toString()).size() > 1) {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            add(splitPane,BorderLayout.CENTER);
            addPartitionPanel(config.hasPartition(), iPanel);
        }
        if (splitPane != null && (config.hasPartition() == Partition.none ||
                doc.getPartitions(config.bHasPartitionsInput.get().toString()).size() <= 1)) {
            remove(splitPane);
            splitPane = null;
        }

        refreshInputPanel();
        if (partitionComponent != null && config.getType() != null) {
            partitionComponent.setVisible(doc.getPartitions(config.getType()).size() > 1);
        }



//		g_currentPanel = this;
    }
    
    void refreshInputPanel(BEASTObject plugin, Input<?> input, boolean bAddButtons, InputEditor.ExpandOption bForceExpansion) throws Exception {
        if (centralComponent != null) {
            remove(centralComponent);
        }
        if (input != null && input.get() != null && input.getType() != null) {
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
            if (isToClone()) {
                ClonePartitionPanel clonePartitionPanel = new ClonePartitionPanel(this);
                p.add(clonePartitionPanel, BorderLayout.NORTH);
            } else {
                p.add(inputEditor.getComponent(), BorderLayout.NORTH);
                //p.add(Box.createVerticalStrut(1024 - inputEditor.getComponent().getPreferredSize().height), BorderLayout.SOUTH);
                //p.setPreferredSize(new Dimension(1024,1024));
            }

            Rectangle bounds = new Rectangle(0,0);
            if (scroller != null) {
            	// get lastPaintPosition from viewport
            	// HACK access it through its string representation
	            JViewport v = scroller.getViewport();
	            String vs = v.toString();
	            int i = vs.indexOf("lastPaintPosition=java.awt.Point[x=");
	            if (i > -1) {
	            	i = vs.indexOf("y=", i);
	            	vs = vs.substring(i+2, vs.indexOf("]", i));
	            	i = Integer.parseInt(vs);
	            } else {
	            	i = 0;
	            }
	            bounds.y = -i;
            }
            scroller = new JScrollPane(p);
            scroller.getViewport().scrollRectToVisible(bounds);
            centralComponent = scroller;
        } else {
            centralComponent = new JLabel("Nothing to be specified");
        }
        if (splitPane != null) {
            splitPane.add(centralComponent, JSplitPane.RIGHT);
        } else {
            add(centralComponent);
        }
    }

    void refreshInputPanel() throws Exception {
        doc.currentInputEditors.clear();
        InputEditor.Base.g_nLabelWidth = config.nLabelWidthInput.get();
        BEASTObject plugin = config;
        Input<?> input = config.resolveInput(doc, iPartition);

        boolean bAddButtons = config.addButtons();
        ExpandOption bForceExpansion = config.forceExpansion();
        refreshInputPanel(plugin, input, bAddButtons, bForceExpansion);
    }

    /** 
     * Clones partition identified by sourceID to targetID and type (Site/Clock/Tree model)
     * as stored in config.
     * @param sourceID
     * @param targetID
     */
    public void cloneFrom(String sourceID, String targetID) {
    	if (sourceID.equals(targetID)) {
    		return;
    	}

    	String type = config.bHasPartitionsInput.get().toString();
    	java.util.List<BEASTObject> list = doc.getPartitions(type);
    	int iSource = -1, iTarget = -1;
        for (int i = 0; i < list.size(); i++) {
        	BEASTObject partition = list.get(i);
        	if (type.equals("SiteModel")) {
        		partition = (BEASTObject) ((GenericTreeLikelihood) partition).siteModelInput.get();
        	} else if (type.equals("ClockModel")) {
        		partition = (BEASTObject) ((GenericTreeLikelihood) partition).branchRateModelInput.get();
        	} else if (type.equals("Tree")) {
        		partition = (BEASTObject) ((GenericTreeLikelihood) partition).treeInput.get();
        	}
            String sPartition = partition.getID();
            sPartition = sPartition.substring(sPartition.lastIndexOf('.') + 1);
            if (sPartition.length() > 1 && sPartition.charAt(1) == ':') {
            	sPartition = sPartition.substring(2);
            }
            if (sPartition.equals(sourceID)) {
            	iSource = i;
            }
            if (sPartition.equals(targetID)) {
            	iTarget = i;
            }
        } 
    	if (iTarget == -1) {
    		throw new RuntimeException("Programmer error: sourceID and targetID should be in list");
    	}
    	
		CompoundDistribution likelihoods = (CompoundDistribution) doc.pluginmap.get("likelihood");
		
		GenericTreeLikelihood likelihoodSource = (GenericTreeLikelihood) likelihoods.pDistributions.get().get(iSource);
		GenericTreeLikelihood likelihood = (GenericTreeLikelihood) likelihoods.pDistributions.get().get(iTarget);
		PartitionContext context = doc.getContextFor(likelihood);
		// this ensures the config.sync does not set any input value
		config._input.setValue(null, config);

    	if (type.equals("SiteModel")) {		
			SiteModelInterface siteModelSource = likelihoodSource.siteModelInput.get();
			SiteModelInterface  siteModel = null;
			try {
				siteModel = (SiteModel.Base) BeautiDoc.deepCopyPlugin((BEASTObject) siteModelSource,
					likelihood, (MCMC) doc.mcmc.get(), context, doc, null);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "Could not clone " + sourceID + " to " + targetID + " " + e.getMessage());
				return;
			}
			likelihood.siteModelInput.setValue(siteModel, likelihood);
			return;
    	} else if (type.equals("ClockModel")) {
    		BranchRateModel clockModelSource = likelihoodSource.branchRateModelInput.get();
    		BranchRateModel clockModel = null;
			try {
				clockModel = (BranchRateModel) BeautiDoc.deepCopyPlugin((BEASTObject) clockModelSource,
						likelihood, (MCMC) doc.mcmc.get(), context, doc, null);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "Could not clone " + sourceID + " to " + targetID + " " + e.getMessage());
				return;
			}
			// make sure that *if* the clock model has a tree as input, it is
			// the same as for the likelihood
			TreeInterface tree = null;
			try {
				for (Input<?> input : ((BEASTObject) clockModel).listInputs()) {
					if (input.getName().equals("tree")) {
						tree = (TreeInterface) input.get();
					}

				}
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//if (tree != null && tree != likelihood.treeInput.get()) {
				likelihood.treeInput.setValue(tree, likelihood);
				//throw new RuntimeException("Cannot link clock model with different trees");
			//}

			likelihood.branchRateModelInput.setValue(clockModel, likelihood);
			return;
    	} else if (type.equals("Tree")) {
			TreeInterface tree = null;
			TreeInterface treeSource = likelihoodSource.treeInput.get();
			try {
			tree = (TreeInterface) BeautiDoc.deepCopyPlugin((BEASTObject) treeSource, likelihood,
							(MCMC) doc.mcmc.get(), context, doc, null);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(this, "Could not clone " + sourceID + " to " + targetID + " " + e.getMessage());
					return;
			}
			// sanity check: make sure taxon sets are compatible
			List<String> taxa = tree.getTaxonset().asStringList();
			List<String> taxa2 = likelihood.dataInput.get().getTaxaNames();
			if (taxa.size() != taxa2.size()) {
				throw new RuntimeException("Cannot link trees: incompatible taxon sets");
			}
			for (String taxon : taxa) {
				boolean found = false;
				for (String taxon2 : taxa2) {
					if (taxon.equals(taxon2)) {
						found = true;
						break;
					}
				}
				if (!found) {
					throw new RuntimeException("Cannot link trees: taxon" + taxon + "is not in alignment");
				}
			}

			likelihood.treeInput.setValue(tree, likelihood);
			return;

    	} else {
    		throw new RuntimeException("Programmer error calling cloneFrom: Should only clone Site/Clock/Tree model");
    	}
    } // cloneFrom

    private boolean isToClone() {
        return listOfPartitions != null && listOfPartitions.getSelectedIndices().length > 1;
    }

//    public static boolean soundIsPlaying = false;
//
//    public static synchronized void playSound(final String url) {
//        new Thread(new Runnable() {
//            public void run() {
//                try {
//                    synchronized (this) {
//                        if (soundIsPlaying) {
//                            return;
//                        }
//                        soundIsPlaying = true;
//                    }
//                    Clip clip = AudioSystem.getClip();
//                    AudioInputStream inputStream = AudioSystem.getAudioInputStream(getClass().getResourceAsStream("/beast/app/beauti/" + url));
//                    clip.open(inputStream);
//                    clip.start();
//                    Thread.sleep(500);
//                    synchronized (this) {
//                        soundIsPlaying = false;
//                    }
//                } catch (Exception e) {
//                    soundIsPlaying = false;
//                    System.err.println(e.getMessage());
//                }
//            }
//        }).start();
//    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        //System.err.print("BeautiPanel::valueChanged " + m_iPartition + " => ");
        if (e != null) {
            config.sync(iPartition);
            if (listOfPartitions != null) {
                iPartition = Math.max(0, listOfPartitions.getSelectedIndex());
            }
        }
//        BeautiPanel.playSound("woosh.wav");
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
