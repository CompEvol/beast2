package beast.app.draw;


import beast.core.Input;
import beast.core.Plugin;
import beast.util.ClassDiscovery;

import javax.swing.*;
import javax.swing.border.EtchedBorder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class PluginInputEditor extends InputEditor {
    private static final long serialVersionUID = 1L;
    JComboBox m_selectPluginBox;
    SmallButton m_editPluginButton;

    public PluginInputEditor() {
        super();
    }

    @Override
    public Class<?> type() {
        return Plugin.class;
    }

    /**
     * construct an editor consisting of
     * o a label
     * o a combo box for selecting another plug-in
     * o a button for editing the plug-in
     * o validation label -- optional, if input is not valid
     */
    @Override
    public void init(Input<?> input, Plugin plugin, boolean bExpand) {
        m_input = input;
        m_plugin = plugin;
    	if (bExpand) {
    		expandedInit(input, plugin);
    	} else {
    		simpleInit(input, plugin);
    	}
    } // init
    
    /** add combobox with available plugins 
     * a button to edit that plugin +
     * a validation icon
     * **/
	void simpleInit(Input <?> input, Plugin plugin) {

        addInputLabel();

        addComboBox(this, input, plugin);

        m_editPluginButton = new SmallButton("e", true);
        if (input.get() == null) {
            m_editPluginButton.setEnabled(false);
        }
        m_editPluginButton.setToolTipText("Edit " + m_inputLabel.getText());

        m_editPluginButton.addActionListener(new ActionListener() {

            // implements ActionListener
            public void actionPerformed(ActionEvent e) {
                PluginDialog dlg = new PluginDialog((Plugin) m_input.get(), m_input.getType());
                dlg.setVisible(true);
                if (dlg.getOK()) {
                    m_plugin = dlg.panel.m_plugin;
                }
                checkValidation();
            }
        });
        add(m_editPluginButton);

        addValidationLabel();
    } // init

	Box m_expansionBox = null;
	
    void expandedInit(Input<?> input, Plugin plugin) {
        addInputLabel();
        Box box = Box.createVerticalBox();
        // add horizontal box with combobox of Plugins to select from
        Box combobox = Box.createHorizontalBox();
        addComboBox(combobox, input, plugin);
        box.add(combobox);
        PluginPanel.addInputs(box, (Plugin) input.get());
        box.setBorder(new EtchedBorder());
        add(box);
        m_expansionBox = box;
    } // expandedInit
    
    
    /** add combobox with Plugins to choose from
     * On choosing a new value, create plugin (if is not already an object)
     * Furthermore, if expanded, update expanded inputs 
     */
    
    void addComboBox(Box box, Input <?> input, Plugin plugin) {
        List<String> sAvailablePlugins = PluginPanel.getAvailablePlugins(m_input, m_plugin, null);
        sAvailablePlugins.add(NO_VALUE);
        if (sAvailablePlugins.size() > 1) {
            m_selectPluginBox = new JComboBox(sAvailablePlugins.toArray(new String[0]));
            String sSelectString = NO_VALUE;
            if (input.get() != null) {
                sSelectString = ((Plugin) input.get()).getID();
            }
            m_selectPluginBox.setSelectedItem(sSelectString);

            m_selectPluginBox.addActionListener(new ActionListener() {
                // implements ActionListener
                public void actionPerformed(ActionEvent e) {
                	
                	// get a handle of the selected plugin
                    String sSelected = (String) m_selectPluginBox.getSelectedItem();
                    Plugin plugin = (Plugin) m_input.get();
                    if (sSelected.equals(NO_VALUE)) {
                        plugin = null;
                    } else if (!sSelected.startsWith("new ")) {
                        plugin = PluginPanel.g_plugins.get(sSelected);
                    } else {
                        /* create new plugin */
                        try {
                            plugin = (Plugin) Class.forName(sSelected.substring(4)).newInstance();
                            PluginPanel.addPluginToMap(plugin);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "Could not select plugin: " +
                                    ex.getClass().getName() + " " +
                                    ex.getMessage()
                            );
                        }
                    }

                    
                    try {
                        if (plugin == null) {
                            m_selectPluginBox.setSelectedItem(NO_VALUE);
                            // is this input expanded?
                            if (m_expansionBox != null) {
                            	// remove items from Expansion Box, if any
                            	for (int i = 1; i < m_expansionBox.getComponentCount(); i++) {
                            		m_expansionBox.remove(i);
                            	}
                            } else { // not expanded
                                m_editPluginButton.setEnabled(false);
                            }
                        } else {
                        	// get handle on ID of the plugin, and add to combobox if necessary
                            String sID = plugin.getID();
                            // TODO RRB: have to remove ID first, then add it
                            // The addition is necessary to make the items in the expansionBox scale and show up
                            // Is there another way?
                            m_selectPluginBox.removeItem(sID);
                            m_selectPluginBox.addItem(sID);
                            m_selectPluginBox.setSelectedItem(sID);
                        }
                        m_input.setValue(plugin, m_plugin);
                        
                        
                        if (m_expansionBox != null) {
                        	// remove items from Expansion Box
                        	for (int i = 1; i < m_expansionBox.getComponentCount(); ) {
                        		m_expansionBox.remove(i);
                        	}
                        	// add new items to Expansion Box
                        	if (plugin != null) {
                        		PluginPanel.addInputs(m_expansionBox, plugin);
                        	}
                        } else {
                        	// it is not expanded, enable the edit button
                        	m_editPluginButton.setEnabled(true);
                            checkValidation();
                        }
                        
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Could not change plugin: " +
                                ex.getClass().getName() + " " +
                                ex.getMessage()
                        );
                    }
                }
            });
            m_selectPluginBox.setToolTipText(input.getTipText());
            box.add(m_selectPluginBox);
        }
    }
    String[] getAvailablePlugins() {
        List<String> sPlugins = ClassDiscovery.find(m_input.getType(), "beast");
        return sPlugins.toArray(new String[0]);
    } // getAvailablePlugins

} // class PluginInputEditor
