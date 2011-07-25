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

import beast.app.draw.ListInputEditor;
import beast.app.draw.PluginPanel;
import beast.app.draw.SmallButton;
import beast.app.draw.InputEditor.BUTTONSTATUS;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Logger;
import beast.core.Plugin;
import beast.core.State;
import beast.core.StateNode;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeDistribution;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.OneOnX;
import beast.math.distributions.Prior;

public class PriorListInputEditor extends ListInputEditor {
	private static final long serialVersionUID = 1L;

	List<JComboBox> m_comboBox;
	List<JButton> m_taxonButton;
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
		m_comboBox = new ArrayList<JComboBox>();
		m_taxonButton = new ArrayList<JButton>();
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
        if (plugin instanceof Prior) {
        	Prior prior = (Prior) plugin;
        	String sText = /*plugin.getID() + ": " +*/ ((Plugin)prior.m_x.get()).getID();
        	JLabel label = new JLabel(sText);
        	label.setMinimumSize(new Dimension(200,20));
        	label.setPreferredSize(new Dimension(200,20));
        	itemBox.add(label);

            //List<String> sAvailablePlugins = PluginPanel.getAvailablePlugins(prior.m_distInput, prior, null);
            List<BeautiSubTemplate> sAvailablePlugins = PluginPanel.getAvailableTemplates(prior.m_distInput, prior, null);
            comboBox = new JComboBox(sAvailablePlugins.toArray());
            
            String sID = prior.m_distInput.get().getID();
            sID = sID.substring(0, sID.indexOf('.'));
            for (BeautiSubTemplate template : sAvailablePlugins) {
        		if (template.m_sClassInput.get() != null && template.m_sClassInput.get().contains(sID)) {
            		comboBox.setSelectedItem(template);
            	}
            }
           	comboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JComboBox comboBox = (JComboBox) e.getSource();

	        		List list = (List) m_input.get();
	        		int iItem = 0;
	        		while (m_comboBox.get(iItem) != comboBox) {
	        			iItem++;
	        		}
					BeautiSubTemplate template = (BeautiSubTemplate) comboBox.getSelectedItem();
					String sID = ((Plugin)list.get(iItem)).getID();
                    String sPartition = sID.substring(sID.indexOf('.') + 1);
					Plugin plugin2 = template.createSubNet(sPartition);

					
					//System.err.println(" " + plugin2);
	            	Prior prior = (Prior) list.get(iItem);
	            	try {
						prior.m_distInput.setValue(plugin2, prior);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					sync();
					refreshPanel();
					
//					JComboBox comboBox = (JComboBox) e.getSource();
//                    String sSelected = (String) comboBox.getSelectedItem();xx
//	            	Plugin plugin2 = PluginPanel.g_plugins.get(sSelected);
////System.err.println("PRIOR" + sSelected + " " + plugin2);
//	        		List list = (List) m_input.get();
//	        		int iItem = 0;
//	        		while (m_comboBox.get(iItem) != comboBox) {
//	        			iItem++;
//	        		}
//	            	Prior prior = (Prior) list.get(iItem);
//	            	try {
//						prior.m_distInput.setValue(plugin2, prior);
//					} catch (Exception e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
//					refreshPanel();
				}
			});
        	itemBox.add(comboBox);
        } else if (plugin instanceof TreeDistribution) {
        	TreeDistribution distr= (TreeDistribution) plugin;
        	String sText = ""/*plugin.getID() + ": "*/;
        	if (distr.m_tree.get() != null) {
        		sText += distr.m_tree.get().getID();
        	} else {
        		sText += distr.treeIntervals.get().m_tree.get().getID();
        	}
        	JLabel label = new JLabel(sText);
        	label.setMinimumSize(new Dimension(200,20));
        	label.setPreferredSize(new Dimension(200,20));
        	itemBox.add(label);
//            List<String> sAvailablePlugins = PluginPanel.getAvailablePlugins(m_input, m_plugin, null);
            
            List<BeautiSubTemplate> sAvailablePlugins = PluginPanel.getAvailableTemplates(m_input, m_plugin, null);
            comboBox = new JComboBox(sAvailablePlugins.toArray());

            for (int i = sAvailablePlugins.size()-1; i >= 0; i--) {
            	if (!TreeDistribution.class.isAssignableFrom(sAvailablePlugins.get(i).m_class)) {
					sAvailablePlugins.remove(i);
				}
            	
            }

            String sID = distr.getID();
            sID = sID.substring(0, sID.indexOf('.'));
            for (BeautiSubTemplate template : sAvailablePlugins) {
            	if (template.getMainID().replaceAll(".\\$\\(n\\)", "").equals(sID)) {
            		comboBox.setSelectedItem(template);
            	}
            }
            
        	comboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JComboBox comboBox = (JComboBox) e.getSource();

	        		List list = (List) m_input.get();
	        		int iItem = 0;
	        		while (m_comboBox.get(iItem) != comboBox) {
	        			iItem++;
	        		}
					BeautiSubTemplate template = (BeautiSubTemplate) comboBox.getSelectedItem();
					String sID = ((Plugin)list.get(iItem)).getID();
                    String sPartition = sID.substring(sID.indexOf('.') + 1);
					Plugin plugin2 = template.createSubNet(sPartition);
//System.err.println(" " + plugin2);
	            	list.set(iItem, plugin2);
//System.err.println(iItem + " " +list.get(iItem)+ " " + plugin2 + " " + list);
					sync();
					refreshPanel();
				}
			});
        	itemBox.add(comboBox);
        } else if (plugin instanceof MRCAPrior) {
        	MRCAPrior prior = (MRCAPrior) plugin;
        	String sText = prior.getID();
        	
        	taxonButton = new JButton(sText);
        	taxonButton.setMinimumSize(new Dimension(200,20));
        	taxonButton.setPreferredSize(new Dimension(200,20));
        	itemBox.add(taxonButton);
        	taxonButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JButton comboBox = (JButton) e.getSource();
	        		List list = (List) m_input.get();
	        		int iItem = 0;
	        		while (m_taxonButton.get(iItem) != comboBox) {
	        			iItem++;
	        		}
	        		MRCAPrior prior = (MRCAPrior) list.get(iItem);
	            	try {
	            		TaxonSet taxonset = prior.m_taxonset.get();
	            		Set<Taxon> candidates = getTaxonCandidates(prior);
	            		TaxonSetDialog dlg = new TaxonSetDialog(taxonset, candidates);
	                    dlg.setVisible(true);
	                    if (dlg.m_bOK) {
	                    	prior.setID(dlg.m_taxonSet.getID());
	                    	prior.m_taxonset.setValue(dlg.m_taxonSet, prior);
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
        	
            List<BeautiSubTemplate> sAvailablePlugins = PluginPanel.getAvailableTemplates(prior.m_distInput, prior, null);
            comboBox = new JComboBox(sAvailablePlugins.toArray());
            
            if (prior.m_distInput.get() != null) {
            	String sID = prior.m_distInput.get().getID();
            	sID = sID.substring(0, sID.indexOf('.'));
            	for (BeautiSubTemplate template : sAvailablePlugins) {
            		if (template.m_sClassInput.get() != null && template.m_sClassInput.get().contains(sID)) {
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
					Plugin plugin2 = template.createSubNet("");
//System.err.println("PRIOR" + plugin2);
	        		List list = (List) m_input.get();
	        		int iItem = 0;
	        		while (m_comboBox.get(iItem) != comboBox) {
	        			iItem++;
	        		}
	        		MRCAPrior prior = (MRCAPrior) list.get(iItem);
	            	try {
						prior.m_distInput.setValue(plugin2, prior);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					refreshPanel();
				}
			});
        	itemBox.add(comboBox);
        
        	JCheckBox isEstimatedBox = new JCheckBox(BeautiConfig.getInputLabel(prior, prior.m_bIsMonophyleticInput.getName()));
			isEstimatedBox.setSelected(prior.m_bIsMonophyleticInput.get());
			isEstimatedBox.setToolTipText(prior.m_bIsMonophyleticInput.getTipText());
			isEstimatedBox.addActionListener(new MRCAPriorActionListener(prior));
			itemBox.add(isEstimatedBox);
        }
        if (!(plugin instanceof MRCAPrior)) {
        	m_delButton.get(m_delButton.size() - 1).setVisible(false);
        }
    	comboBox.setMaximumSize(new Dimension(1024, 24));
    	m_comboBox.add(comboBox);
    	m_taxonButton.add(taxonButton);
    	itemBox.add(createGlue());
    } // addPluginItem
	
	
	
	Set<Taxon> getTaxonCandidates(MRCAPrior prior) {
		Set<Taxon> candidates = new HashSet<Taxon>();
		for (String sTaxon : prior.m_treeInput.get().getTaxaNames()) {
			Taxon taxon = null;
			for (Taxon taxon2 : PluginPanel.g_taxa) {
				if (taxon2.getID().equals(sTaxon)) {
					taxon = taxon2;
					break;
				}
			}
			if (taxon == null) {
				taxon = new Taxon();
				taxon.setID(sTaxon);
				PluginPanel.g_taxa.add(taxon);
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
            BeautiDoc.g_doc.scrubState(true);
            State state = (State) PluginPanel.g_plugins.get("state");
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
	        if (!dlg.m_bOK) {
	        	return null;
	        }
        	taxonSet = dlg.m_taxonSet;
	    	PluginPanel.addPluginToMap(taxonSet);
	    	prior.m_taxonset.setValue(taxonSet, prior);
	    	prior.setID(taxonSet.getID() + ".prior");
	    	// this sets up the type
	    	prior.m_distInput.setValue(new OneOnX(), prior);
	    	// this removes the parametric distribution
    		prior.m_distInput.setValue(null, prior);
    		
    		Logger logger = (Logger) PluginPanel.g_plugins.get("tracelog");
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
