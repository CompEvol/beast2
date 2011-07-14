package beast.app.beauti;



import java.awt.Component;
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
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Plugin;
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

            List<String> sAvailablePlugins = PluginPanel.getAvailablePlugins(prior.m_distInput, prior, null);
            comboBox = new JComboBox(sAvailablePlugins.toArray(new String[0]));
        	comboBox.setSelectedItem(prior.m_distInput.get().getID());
           	comboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JComboBox comboBox = (JComboBox) e.getSource();
                    String sSelected = (String) comboBox.getSelectedItem();
	            	Plugin plugin2 = PluginPanel.g_plugins.get(sSelected);
System.err.println("PRIOR" + sSelected + " " + plugin2);
	        		List list = (List) m_input.get();
	        		int iItem = 0;
	        		while (m_comboBox.get(iItem) != comboBox) {
	        			iItem++;
	        		}
	            	Prior prior = (Prior) list.get(iItem);
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
            List<String> sAvailablePlugins = PluginPanel.getAvailablePlugins(m_input, m_plugin, null);
            for (int i = sAvailablePlugins.size()-1; i >= 0; i--) {
            	Plugin plugin2 = PluginPanel.g_plugins.get(sAvailablePlugins.get(i));
				if (!(plugin2 instanceof TreeDistribution)) {
					sAvailablePlugins.remove(i);
				}
            	
            }
            comboBox = new JComboBox(sAvailablePlugins.toArray(new String[0]));
        	comboBox.setSelectedItem(plugin.getID());
        	comboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JComboBox comboBox = (JComboBox) e.getSource();
                    String sSelected = (String) comboBox.getSelectedItem();
	            	Plugin plugin2 = PluginPanel.g_plugins.get(sSelected);
System.err.println(sSelected + " " + plugin2);
	        		List list = (List) m_input.get();
	        		int iItem = 0;
	        		while (m_comboBox.get(iItem) != comboBox) {
	        			iItem++;
	        		}
	            	list.set(iItem, plugin2);
System.err.println(iItem + " " +list.get(iItem)+ " " + plugin2 + " " + list);
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
        	
            List<String> sAvailablePlugins = PluginPanel.getAvailablePlugins(prior.m_distInput, prior, null);
            sAvailablePlugins.add(NO_VALUE);
//            JLabel label2 = new JLabel("calibration: ");
//            itemBox.add(label2);
            comboBox = new JComboBox(sAvailablePlugins.toArray(new String[0]));
            
            if (prior.m_distInput.get() != null) {
            	comboBox.setSelectedItem(prior.m_distInput.get().getID());
            } else {
            	comboBox.setSelectedItem(NO_VALUE);
            }
           	comboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JComboBox comboBox = (JComboBox) e.getSource();
                    String sSelected = (String) comboBox.getSelectedItem();
	            	Plugin plugin2 = PluginPanel.g_plugins.get(sSelected);
System.err.println("PRIOR" + sSelected + " " + plugin2);
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

	    Component c = this;
	    while (((Component) c).getParent() != null) {
	      	c = ((Component) c).getParent();
	      	if (c instanceof BeautiPanel) {
	      		BeautiPanel panel = (BeautiPanel) c;
	      		BeautiPanelConfig cfgPanel = panel.m_config;
	      		cfgPanel.sync(0);
	      	}
        }

	} // addItem

	@Override
    public List<Plugin> pluginSelector(Input<?> input, Plugin parent, List<String> sTabuList) {
    	MRCAPrior prior = new MRCAPrior();
    	try {

            List<Tree> trees = new ArrayList<Tree>();
	    	for (StateNode node : PluginPanel.g_stateNodes) {
	    		if (node instanceof Tree && ((Tree) node).m_initial.get() != null) {
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
	    	prior.m_treeInput.setValue(trees.get(iTree), prior);
	    	TaxonSet taxonset = new TaxonSet();
	    	taxonset.setID("MRCA(???)");
	    	prior.m_taxonset.setValue(taxonset, prior);
	    	prior.setID("MRCA(.)");
	    	// this sets up the type
	    	prior.m_distInput.setValue(new OneOnX(), prior);
	    	// this removes the parametric distribution
    		prior.m_distInput.setValue(null, prior);
    	} catch (Exception e) {
			// TODO: handle exception
		}
    	List<Plugin> selectedPlugins = new ArrayList<Plugin>();
    	selectedPlugins.add(prior);
    	return selectedPlugins;
    }

}
