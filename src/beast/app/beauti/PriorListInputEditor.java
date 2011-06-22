package beast.app.beauti;


import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import beast.app.draw.InputEditor;
import beast.app.draw.ListInputEditor;
import beast.app.draw.PluginPanel;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Plugin;
import beast.evolution.tree.TreeDistribution;
import beast.math.distributions.Prior;

public class PriorListInputEditor extends ListInputEditor {
	private static final long serialVersionUID = 1L;

	List<JComboBox> m_comboBox;
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
		if (!InputEditor.g_bExpertMode) {
			m_buttonStatus = BUTTONSTATUS.NONE;
		}
		m_comboBox = new ArrayList<JComboBox>();
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
        }
    	comboBox.setMaximumSize(new Dimension(1024, 24));
    	m_comboBox.add(comboBox);
    	itemBox.add(createGlue());
    }
	
	
}
