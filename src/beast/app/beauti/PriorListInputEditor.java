package beast.app.beauti;





import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import beast.app.draw.ListInputEditor;
import beast.app.draw.PluginDialog;
import beast.app.draw.PluginPanel;
import beast.app.draw.SmallButton;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Logger;
import beast.core.Plugin;
import beast.core.State;
import beast.core.StateNode;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.speciation.GeneTreeForSpeciesTreeDistribution;
import beast.evolution.speciation.SpeciesTreePrior;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeDistribution;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.OneOnX;
import beast.math.distributions.Prior;

public class PriorListInputEditor extends ListInputEditor {
	private static final long serialVersionUID = 1L;
	static Dimension PREFERRED_SIZE = new Dimension(200,20);

	List<JComboBox> comboBoxes;
	List<JButton> rangeButtons;
	JComboBox currentComboBox;

	List<JButton> taxonButtons;

//	public PriorListInputEditor(BeautiDoc doc) {
//		super(doc);
//	}

	@Override
	public Class<?> type() {
		return List.class;
	}
	
	@Override
	public Class<?> baseType() {
		return Distribution.class;
	}

	@Override
	public void init(Input<?> input, Plugin plugin, EXPAND bExpand, boolean bAddButtons) {
//		if (!InputEditor.g_bExpertMode && ((List<?>) input.get()).size() > 0 && !(((List<?>) input.get()).get(0) instanceof MRCAPrior)) {
//			m_buttonStatus = BUTTONSTATUS.NONE;
//		}
		comboBoxes = new ArrayList<JComboBox>();
		rangeButtons = new ArrayList<JButton>();
		taxonButtons = new ArrayList<JButton>();
		super.init(input, plugin, bExpand, bAddButtons);

        m_addButton = new SmallButton("+", true);
        m_addButton.setToolTipText("Add item to the list");
        m_addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	addItem();
            }
        });
        add(m_addButton);
    }
	
	
    /** add components to box that are specific for the plugin.
     * By default, this just inserts a label with the plugin ID 
     * @param itemBox box to add components to
     * @param plugin plugin to add
     */
	@Override
    protected void addPluginItem(Box itemBox, Plugin plugin) {
		JComboBox comboBox = null;
    	JButton taxonButton = null;
    	JButton rangeButton = null;
        if (plugin instanceof Prior) {
        	Prior prior = (Prior) plugin;
        	String sText = /*plugin.getID() + ": " +*/ ((Plugin)prior.m_x.get()).getID();
        	JLabel label = new JLabel(sText);
        	label.setMinimumSize(PREFERRED_SIZE);
        	label.setPreferredSize(PREFERRED_SIZE);
        	itemBox.add(label);
        	

            List<BeautiSubTemplate> sAvailablePlugins = PluginPanel.getAvailableTemplates(prior.m_distInput, prior, null, doc);
            comboBox = new JComboBox(sAvailablePlugins.toArray());
            
            String sID = prior.m_distInput.get().getID();
            System.err.println("id=" + sID);
            sID = sID.substring(0, sID.indexOf('.'));
            for (BeautiSubTemplate template : sAvailablePlugins) {
        		if (template.sClassInput.get() != null && template.sShortClassName.equals(sID)) {
            		comboBox.setSelectedItem(template);
            	}
            }
           	comboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JComboBox comboBox = (JComboBox) e.getSource();

	        		List<?> list = (List<?>) m_input.get();
	        		int iItem = 0;
	        		while (comboBoxes.get(iItem) != comboBox) {
	        			iItem++;
	        		}
					BeautiSubTemplate template = (BeautiSubTemplate) comboBox.getSelectedItem();
					String sID = ((Plugin)list.get(iItem)).getID();
                    String sPartition = sID.substring(sID.indexOf('.') + 1);
					Prior prior = (Prior) list.get(iItem);
					try {
						template.createSubNet(sPartition, prior, prior.m_distInput);
					} catch (Exception e1) {
						e1.printStackTrace();
					}

					sync();
					refreshPanel();
				}
			});
        	itemBox.add(comboBox);
        	
        	if (prior.m_x.get() instanceof RealParameter) {
        		// add range button for real parameters
        		RealParameter p = (RealParameter) prior.m_x.get();
        		rangeButton = new JButton(paramToString(p));
        		rangeButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						JButton rangeButton = (JButton) e.getSource();
		        		int iItem = 0;
		        		while (rangeButtons.get(iItem) != rangeButton) {
		        			iItem++;
		        		}
		        		List<?> list = (List<?>) m_input.get();
		        		Prior prior = (Prior) list.get(iItem);
		        		RealParameter p = (RealParameter) prior.m_x.get();
		        		PluginDialog dlg = new PluginDialog(p, RealParameter.class, doc);
		        		dlg.setVisible(true);
		                if (dlg.getOK(doc)) {
		                	dlg.accept(p);
		                	rangeButton.setText(paramToString(p));
		                	refreshPanel();
		                }
					}
				});
        		itemBox.add(Box.createHorizontalStrut(10));
        		itemBox.add(rangeButton);
        	}

        } else if (plugin instanceof TreeDistribution) {
        	TreeDistribution distr= (TreeDistribution) plugin;
        	String sText = ""/*plugin.getID() + ": "*/;
        	if (distr.m_tree.get() != null) {
        		sText += distr.m_tree.get().getID();
        	} else {
        		sText += distr.treeIntervals.get().m_tree.get().getID();
        	}
        	JLabel label = new JLabel(sText);
        	label.setMinimumSize(PREFERRED_SIZE);
        	label.setPreferredSize(PREFERRED_SIZE);
        	itemBox.add(label);
//            List<String> sAvailablePlugins = PluginPanel.getAvailablePlugins(m_input, m_plugin, null);
            
            List<BeautiSubTemplate> sAvailablePlugins = PluginPanel.getAvailableTemplates(m_input, m_plugin, null, doc);
            comboBox = new JComboBox(sAvailablePlugins.toArray());

            for (int i = sAvailablePlugins.size()-1; i >= 0; i--) {
            	if (!TreeDistribution.class.isAssignableFrom(sAvailablePlugins.get(i)._class)) {
					sAvailablePlugins.remove(i);
				}
            	
            }

            String sID = distr.getID();
            try {
            	sID = sID.substring(0, sID.indexOf('.'));
            } catch (Exception e) {
				throw new RuntimeException("Improperly formatted ID: " + distr.getID());
			}
            for (BeautiSubTemplate template : sAvailablePlugins) {
            	if (template.matchesName(sID)) { //getMainID().replaceAll(".\\$\\(n\\)", "").equals(sID)) {
            		comboBox.setSelectedItem(template);
            	}
            }
            
        	comboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					currentComboBox = (JComboBox) e.getSource();
					
                	SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
                	SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
	        		@SuppressWarnings("unchecked")
					List<Plugin> list = (List<Plugin>) m_input.get();
	        		int iItem = 0;
	        		while (comboBoxes.get(iItem) != currentComboBox) {
	        			iItem++;
	        		}
					BeautiSubTemplate template = (BeautiSubTemplate) currentComboBox.getSelectedItem();
					String sID = ((Plugin)list.get(iItem)).getID();
                    String sPartition = sID.substring(sID.indexOf('.') + 1);

                    try {
						template.createSubNet(sPartition, list, iItem);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    
//                    Plugin plugin2 = template.createSubNet(sPartition);
//System.err.println("NEW SUBNET " + plugin2);
//	            	list.set(iItem, plugin2);
//System.err.println(iItem + " " +list.get(iItem)+ " " + plugin2 + " " + list);
					sync();
					refreshPanel();
                	}
                	});
					}
                	});
				}
			});
        	itemBox.add(comboBox);
        } else if (plugin instanceof MRCAPrior) {
        	MRCAPrior prior = (MRCAPrior) plugin;
        	String sText = prior.getID();
        	
        	taxonButton = new JButton(sText);
        	taxonButton.setMinimumSize(PREFERRED_SIZE);
        	taxonButton.setPreferredSize(PREFERRED_SIZE);
        	itemBox.add(taxonButton);
        	taxonButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JButton taxonButton = (JButton) e.getSource();
	        		List<?> list = (List<?>) m_input.get();
	        		int iItem = 0;
	        		while (taxonButtons.get(iItem) != taxonButton) {
	        			iItem++;
	        		}
	        		MRCAPrior prior = (MRCAPrior) list.get(iItem);
	            	try {
	            		TaxonSet taxonset = prior.m_taxonset.get();
	            		Set<Taxon> candidates = getTaxonCandidates(prior);
	            		TaxonSetDialog dlg = new TaxonSetDialog(taxonset, candidates);
	                    dlg.setVisible(true);
	                    if (dlg.isOK) {
	                    	prior.setID(dlg.taxonSet.getID());
	                    	prior.m_taxonset.setValue(dlg.taxonSet, prior);
	                    }
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					refreshPanel();
				}
			});
        	
        	
        	if (prior.m_distInput.getType() == null) {
        		try {
        			prior.m_distInput.setValue(new OneOnX(), prior);
        			prior.m_distInput.setValue(null, prior);
        		} catch (Exception e) {
					// TODO: handle exception
				}
        		
        	}
        	
            List<BeautiSubTemplate> sAvailablePlugins = PluginPanel.getAvailableTemplates(prior.m_distInput, prior, null, doc);
            comboBox = new JComboBox(sAvailablePlugins.toArray());
            
            if (prior.m_distInput.get() != null) {
            	String sID = prior.m_distInput.get().getID();
            	sID = sID.substring(0, sID.indexOf('.'));
            	for (BeautiSubTemplate template : sAvailablePlugins) {
               		if (template.sClassInput.get() != null && template.sShortClassName.equals(sID)) {
               			comboBox.setSelectedItem(template);
            		}
            	}
            } else {
            	comboBox.setSelectedItem(BeautiConfig.NULL_TEMPLATE);
            }
           	comboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JComboBox comboBox = (JComboBox) e.getSource();
					BeautiSubTemplate template = (BeautiSubTemplate) comboBox.getSelectedItem();
	        		List<?> list = (List<?>) m_input.get();
	        		int iItem = 0;
	        		while (comboBoxes.get(iItem) != comboBox) {
	        			iItem++;
	        		}
	        		MRCAPrior prior = (MRCAPrior) list.get(iItem);

//System.err.println("PRIOR" + plugin2);
//	            	try {
//						prior.m_distInput.setValue(plugin2, prior);
//					} catch (Exception e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
					try {
						//Plugin plugin2 = 
						template.createSubNet("", prior, prior.m_distInput);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					refreshPanel();
				}
			});
        	itemBox.add(comboBox);
        
        	JCheckBox isEstimatedBox = new JCheckBox(doc.beautiConfig.getInputLabel(prior, prior.m_bIsMonophyleticInput.getName()));
			isEstimatedBox.setSelected(prior.m_bIsMonophyleticInput.get());
			isEstimatedBox.setToolTipText(prior.m_bIsMonophyleticInput.getTipText());
			isEstimatedBox.addActionListener(new MRCAPriorActionListener(prior));
			itemBox.add(isEstimatedBox);
        } else {
        	String sText = plugin.getID();
        	JLabel label = new JLabel(sText);
        	label.setMinimumSize(PREFERRED_SIZE);
        	label.setPreferredSize(PREFERRED_SIZE);
        	itemBox.add(label);

        	comboBox = new JComboBox();
        	comboBox.setVisible(false);
        }
        if (!(plugin instanceof MRCAPrior) && m_buttonStatus != BUTTONSTATUS.NONE && m_buttonStatus != BUTTONSTATUS.ADDONLY) {
        	m_delButton.get(m_delButton.size() - 1).setVisible(false);
        }
        if (plugin instanceof SpeciesTreePrior || plugin instanceof GeneTreeForSpeciesTreeDistribution) {
        	comboBox.setVisible(false);
        }
    	comboBox.setMaximumSize(new Dimension(1024, 24));
    	comboBoxes.add(comboBox);
    	rangeButtons.add(rangeButton);
    	taxonButtons.add(taxonButton);
    	
        String sTipText = getDoc().tipTextMap.get(plugin.getID());
        System.out.println(plugin.getID());
        if (sTipText != null) {
        	JLabel tipTextLabel = new JLabel(" " + sTipText);
        	itemBox.add(tipTextLabel);
        }
    	
    	itemBox.add(createGlue());
    } // addPluginItem
	
	
	String paramToString(RealParameter p) {
		Double lower = p.lowerValueInput.get();
		Double upper = p.upperValueInput.get();
		return "initial = " + p.m_pValues.get() + 
			" [" +  (lower == null? "-\u221E" : lower + "") + 
			"," + (upper == null? "\u221E" : upper + "") + "]";
	}

	Set<Taxon> getTaxonCandidates(MRCAPrior prior) {
		Set<Taxon> candidates = new HashSet<Taxon>();
		for (String sTaxon : prior.m_treeInput.get().getTaxaNames()) {
			Taxon taxon = null;
			for (Taxon taxon2 : doc.taxaset) {
				if (taxon2.getID().equals(sTaxon)) {
					taxon = taxon2;
					break;
				}
			}
			if (taxon == null) {
				taxon = new Taxon();
				taxon.setID(sTaxon);
				doc.taxaset.add(taxon);
			}
			candidates.add(taxon);
		}
		return candidates;
	}
	/** class to deal with toggling monophyletic flag on an MRCAPrior **/
	class MRCAPriorActionListener implements ActionListener {
		MRCAPrior m_prior;
		MRCAPriorActionListener(MRCAPrior prior) {
			m_prior = prior;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				m_prior.m_bIsMonophyleticInput.setValue(((JCheckBox)e.getSource()).isSelected(), m_prior);
				refreshPanel();
			} catch (Exception ex) {
				System.err.println("PriorListInputEditor " + ex.getMessage());
			}
		}
	}

	
	@Override
	protected void addItem() {
		super.addItem();
		sync();
		refreshPanel();
	} // addItem
		
	@Override
    public List<Plugin> pluginSelector(Input<?> input, Plugin parent, List<String> sTabuList) {
    	MRCAPrior prior = new MRCAPrior();
    	try {

            List<Tree> trees = new ArrayList<Tree>();
            getDoc().scrubAll(true, false);
            State state = (State) doc.pluginmap.get("state");
	    	for (StateNode node : state.stateNodeInput.get()) {
	    		if (node instanceof Tree) { // && ((Tree) node).m_initial.get() != null) {
	    			trees.add((Tree) node);
	    		}
	    	}
	    	int iTree = 0;
	    	if (trees.size() > 1) {
	    		String [] sTreeIDs = new String [trees.size()];
	    		for (int j = 0; j < sTreeIDs.length; j++) {
	    			sTreeIDs[j] = trees.get(j).getID();
	    		}
    			iTree = JOptionPane.showOptionDialog(null, "Select a tree", "MRCA selector", 
    				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, 
    				sTreeIDs, trees.get(0));
	    	}
	    	if (iTree < 0) {
	    		return null;
	    	}
	    	prior.m_treeInput.setValue(trees.get(iTree), prior);
	    	TaxonSet taxonSet = new TaxonSet();

	    	TaxonSetDialog dlg = new TaxonSetDialog(taxonSet, getTaxonCandidates(prior));
	        dlg.setVisible(true);
	        if (!dlg.isOK || dlg.taxonSet.getID() == null) {
	        	return null;
	        }
        	taxonSet = dlg.taxonSet;
	    	PluginPanel.addPluginToMap(taxonSet, doc);
	    	prior.m_taxonset.setValue(taxonSet, prior);
	    	prior.setID(taxonSet.getID() + ".prior");
	    	// this sets up the type
	    	prior.m_distInput.setValue(new OneOnX(), prior);
	    	// this removes the parametric distribution
    		prior.m_distInput.setValue(null, prior);
    		
    		Logger logger = (Logger) doc.pluginmap.get("tracelog");
    		logger.m_pLoggers.setValue(prior, logger);
    	} catch (Exception e) {
			// TODO: handle exception
		}
    	List<Plugin> selectedPlugins = new ArrayList<Plugin>();
    	selectedPlugins.add(prior);
    	g_collapsedIDs.add(prior.getID());
    	return selectedPlugins;
    }

}
