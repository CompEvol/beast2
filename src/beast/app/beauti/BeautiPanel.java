package beast.app.beauti;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import beast.app.beauti.BeautiPanelConfig.Partition;
import beast.app.draw.InputEditor;
import beast.app.draw.InputEditor.ExpandOption;
import beast.app.util.Utils;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.MCMC;
import beast.core.util.CompoundDistribution;
import beast.evolution.alignment.Taxon;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.likelihood.GenericTreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.sitemodel.SiteModelInterface;
import beast.evolution.tree.TreeInterface;

/**
 * panel making up each of the tabs in Beauti *
 */
public class BeautiPanel extends JPanel implements ListSelectionListener {

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

        setLayout(new BorderLayout());

        this.config = config;
        if (this.config.hasPartition() != Partition.none &&
                doc.getPartitions(config.bHasPartitionsInput.get().toString()).size() > 1) {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            add(splitPane,BorderLayout.CENTER);
        } else {
            splitPane = null;
        }

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
        box.add(new JLabel(Utils.getIcon(iPanel, config)));

        splitPane.add(box, JSplitPane.LEFT);
        if (listOfPartitions != null) {
            listOfPartitions.setSelectedIndex(iPartition);
        }
    }

    /**
     * Create a list of partitions and return as a JComponent;
     * @return
     */
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

        // AJD: This is unnecessary and not appropriate for Mac OS X look and feel
        //listOfPartitions.setBorder(new BevelBorder(BevelBorder.RAISED));

        JScrollPane listPane = new JScrollPane(listOfPartitions);
        partitionComponent.add(listPane, BorderLayout.CENTER);
        // AJD: This is unnecessary and not appropriate for Mac OS X look and feel
        //partitionComponent.setBorder(new EtchedBorder());
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
        for (BEASTInterface partition : doc.getPartitions(type)) {
        	if (type.equals("SiteModel")) {
        		partition = (BEASTInterface) ((GenericTreeLikelihood) partition).siteModelInput.get();
        	} else if (type.equals("ClockModel")) {
        		partition = (BEASTInterface) ((GenericTreeLikelihood) partition).branchRateModelInput.get();
        	} else if (type.equals("Tree")) {
        		partition = (BEASTInterface) ((GenericTreeLikelihood) partition).treeInput.get();
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
    
    void refreshInputPanel(BEASTInterface plugin, Input<?> input, boolean bAddButtons, InputEditor.ExpandOption bForceExpansion) throws Exception {
        if (centralComponent != null) {
            remove(centralComponent);
        }
        if (input != null && input.get() != null && input.getType() != null) {
            InputEditor.ButtonStatus bs = config.buttonStatusInput.get();
            InputEditor inputEditor = doc.getInputEditorFactory().createInputEditor(input, plugin, bAddButtons, bForceExpansion, bs, null, doc);

            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            if (isToClone()) {
                ClonePartitionPanel clonePartitionPanel = new ClonePartitionPanel(this);
                p.add(clonePartitionPanel, BorderLayout.NORTH);
            } else {
                p.add(inputEditor.getComponent(), BorderLayout.CENTER);
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
            centralComponent = new JLabel("No input editors.");
        }
        if (splitPane != null) {
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.add(centralComponent, BorderLayout.NORTH);
            splitPane.add(panel, JSplitPane.RIGHT);
        } else {
            add(centralComponent);
        }
    }

    void refreshInputPanel() throws Exception {
        doc.currentInputEditors.clear();
        InputEditor.Base.g_nLabelWidth = config.nLabelWidthInput.get();
        BEASTInterface plugin = config;
        final Input<?> input = config.resolveInput(doc, iPartition);

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
    	java.util.List<BEASTInterface> list = doc.getPartitions(type);
    	int iSource = -1, iTarget = -1;
        for (int i = 0; i < list.size(); i++) {
        	BEASTInterface partition = list.get(i);
        	if (type.equals("SiteModel")) {
        		partition = (BEASTInterface) ((GenericTreeLikelihood) partition).siteModelInput.get();
        	} else if (type.equals("ClockModel")) {
        		partition = (BEASTInterface) ((GenericTreeLikelihood) partition).branchRateModelInput.get();
        	} else if (type.equals("Tree")) {
        		partition = (BEASTInterface) ((GenericTreeLikelihood) partition).treeInput.get();
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
				siteModel = (SiteModel.Base) BeautiDoc.deepCopyPlugin((BEASTInterface) siteModelSource,
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
				clockModel = (BranchRateModel) BeautiDoc.deepCopyPlugin((BEASTInterface) clockModelSource,
						likelihood, (MCMC) doc.mcmc.get(), context, doc, null);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "Could not clone " + sourceID + " to " + targetID + " " + e.getMessage());
				return;
			}
			// make sure that *if* the clock model has a tree as input, it is
			// the same as for the likelihood
			TreeInterface tree = null;
			try {
				for (Input<?> input : ((BEASTInterface) clockModel).listInputs()) {
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
			if (tree != null && tree != likelihood.treeInput.get()) {
				//likelihood.treeInput.setValue(tree, likelihood);
				JOptionPane.showMessageDialog(null, "Cannot clone clock model with different trees");
				return;
			}

			likelihood.branchRateModelInput.setValue(clockModel, likelihood);
			return;
    	} else if (type.equals("Tree")) {
			TreeInterface tree = null;
			TreeInterface treeSource = likelihoodSource.treeInput.get();
			try {
			tree = (TreeInterface) BeautiDoc.deepCopyPlugin((BEASTInterface) treeSource, likelihood,
							(MCMC) doc.mcmc.get(), context, doc, null);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(this, "Could not clone " + sourceID + " to " + targetID + " " + e.getMessage());
					return;
			}
			// sanity check: make sure taxon sets are compatible
            Taxon.assertSameTaxa(tree.getID(), tree.getTaxonset().getTaxaNames(),
                    likelihood.dataInput.get().getID(), likelihood.dataInput.get().getTaxaNames());

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
