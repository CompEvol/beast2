package beast.app.draw;


import beast.app.beauti.BeautiSubTemplate;
import beast.core.Input;
import beast.core.Plugin;
import beast.core.Input.Validate;
import beast.util.AddOnManager;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class PluginInputEditor extends InputEditor implements ValidateListener {
    private static final long serialVersionUID = 1L;
    JComboBox m_selectPluginBox;
    SmallButton m_editPluginButton;

    PluginInputEditor _this;
    
    public PluginInputEditor() {
        super();
        _this = this;
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
    public void init(Input<?> input, Plugin plugin, EXPAND bExpand, boolean bAddButtons) {
		m_bAddButtons = bAddButtons;
        m_input = input;
        m_plugin = plugin;
    	if (bExpand == EXPAND.FALSE) {
    		simpleInit(input, plugin);
    	} else {
    		expandedInit(input, plugin);
    	}
    } // init
    
    /** add combobox with available plugins 
     * a button to edit that plugin +
     * a validation icon
     * **/
	void simpleInit(Input <?> input, Plugin plugin) {

        addInputLabel();

        addComboBox(this, input, plugin);

        if (m_bAddButtons) {
        	if (PluginPanel.countInputs((Plugin)m_input.get()) > 0) {
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
		                	try {
		                		dlg.accept((Plugin) m_input.get());
		                	} catch (Exception ex) {
								ex.printStackTrace();
							}
		                }
		                refresh();
		                checkValidation();
		            }
		        });
		        add(m_editPluginButton);
        	}
        }
        addValidationLabel();
    } // init

    void refresh() {
        String sOldID = (String) m_selectPluginBox.getSelectedItem();
        String sID = ((Plugin)m_input.get()).getID();
        if (!sID.equals(sOldID)) {
            m_selectPluginBox.addItem(sID);
            m_selectPluginBox.setSelectedItem(sID);
            m_selectPluginBox.removeItem(sOldID);
        }
        super.refreshPanel();
//        Component c = this;
//        while (((Component) c).getParent() != null) {
//        	c = ((Component) c).getParent();
//        	if (c instanceof ListSelectionListener) {
//        		((ListSelectionListener) c).valueChanged(null);
//        	}
//        }
    }

    void initSelectPluginBox() {
        List<String> sAvailablePlugins = PluginPanel.getAvailablePlugins(m_input, m_plugin, null);
        if (sAvailablePlugins.size() > 0) {
            sAvailablePlugins.add(NO_VALUE);
        	for (int i = 0; i < sAvailablePlugins.size(); i++) {
        		String sPlugin = sAvailablePlugins.get(i);
        		if (sPlugin.startsWith("new ")) {
        			sPlugin = sPlugin.substring(sPlugin.lastIndexOf('.'));
        			sAvailablePlugins.set(i, sPlugin);
        		}

        	}
            m_selectPluginBox.removeAllItems();
            for (String sStr : sAvailablePlugins.toArray(new String[0])) {
            	m_selectPluginBox.addItem(sStr);
            }
            m_selectPluginBox.setSelectedItem(m_plugin.getID());
        }		
	}
	
	Box m_expansionBox = null;
	
    void expandedInit(Input<?> input, Plugin plugin) {
        addInputLabel();
        Box box = Box.createVerticalBox();
        // add horizontal box with combobox of Plugins to select from
        Box combobox = Box.createHorizontalBox();
        addComboBox(combobox, input, plugin);
        box.add(combobox);
       
    	PluginPanel.addInputs(box, (Plugin) input.get(), this, this);

        box.setBorder(new EtchedBorder());
        add(box);
        m_expansionBox = box;
    } // expandedInit
    
    
    /** add combobox with Plugins to choose from
     * On choosing a new value, create plugin (if is not already an object)
     * Furthermore, if expanded, update expanded inputs 
     */
    protected void addComboBox(Box box, Input <?> input, Plugin plugin) {
        List<BeautiSubTemplate> availableTemplates = PluginPanel.getAvailableTemplates(m_input, m_plugin, null);
        if (availableTemplates.size() > 0) {
//        	if (m_input.getRule() != Validate.REQUIRED || plugin == null) {
//        		sAvailablePlugins.add(NO_VALUE);
//        	}
//        	for (int i = 0; i < sAvailablePlugins.size(); i++) {
//        		String sPlugin = sAvailablePlugins.get(i);
//        		if (sPlugin.startsWith("new ")) {
//        			sPlugin = sPlugin.substring(sPlugin.lastIndexOf('.'));
//        			sAvailablePlugins.set(i, sPlugin);
//        		}
//
//        	}
            m_selectPluginBox = new JComboBox(availableTemplates.toArray());
            
            Object o = input.get();
            String sID;
            if (o == null) {
            	sID = plugin.getID();
            } else {
            	sID = ((Plugin) o).getID();
            }
            sID = sID.substring(0, sID.indexOf('.'));
            for (BeautiSubTemplate template : availableTemplates) {
            	if (template.matchesName(sID)) {
            		m_selectPluginBox.setSelectedItem(template);
            	}
            }

            m_selectPluginBox.addActionListener(new ActionListener() {
                // implements ActionListener
                public void actionPerformed(ActionEvent e) {
                	
//                	SwingUtilities.invokeLater(new Runnable() {
//						
//						@Override
//						public void run() {
                	
                	// get a handle of the selected plugin
                	BeautiSubTemplate sSelected = (BeautiSubTemplate) m_selectPluginBox.getSelectedItem();
                    Plugin plugin = (Plugin) m_input.get();
                    String sID = plugin.getID();
                    String sPartition = sID.substring(sID.indexOf('.') + 1);
                    String sNewID = sSelected.getMainID().replaceAll("\\$\\(n\\)", sPartition);

                    if (sSelected.equals(NO_VALUE)) {
                        plugin = null;
//                    } else if (PluginPanel.g_plugins.containsKey(sNewID)) {
//                    	plugin = PluginPanel.g_plugins.get(sNewID);
                    } else {
                        try {
                        	plugin = sSelected.createSubNet(sPartition, m_plugin, m_input);
                            //PluginPanel.addPluginToMap(plugin);
                            // tricky: try to connect up new inputs with old inputs of existing name
//                            Plugin oldPlugin = (Plugin) m_input.get();
//                            for (Input<?> oldInput: oldPlugin.listInputs()) {
//                            	String sName = oldInput.getName();
//                            	try {
//                            		Input<?> newInput = plugin.getInput(sName);
//                            		if (newInput.get() instanceof List) {
//                            			List<?> values = (List<?>) oldInput.get();
//                            			for (Object value: values) {
//                                			newInput.setValue(value, plugin);
//                            			}
//                            		} else {
//                            			newInput.setValue(oldInput.get(), plugin);
//                            		}
//                            	} catch (Exception ex) {
//									// ignore
//								}
//                            }
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
                            	if (m_bAddButtons) {
                            		m_editPluginButton.setEnabled(false);
                            	}
                            }
                        } else {
                            if (!m_input.canSetValue(plugin, m_plugin)) {
                            	throw new Exception("Cannot set input to this value");
                            }
//                        	// get handle on ID of the plugin, and add to combobox if necessary
//                            String sID = plugin.getID();
//                            // TODO RRB: have to remove ID first, then add it
//                            // The addition is necessary to make the items in the expansionBox scale and show up
//                            // Is there another way?
//                            m_selectPluginBox.removeItem(sID);
//                            m_selectPluginBox.addItem(sID);
//                            m_selectPluginBox.setSelectedItem(sID);
                            sID = plugin.getID();
                            sID = sID.substring(0, sID.indexOf('.'));
        					for (int i = 0; i < m_selectPluginBox.getItemCount(); i++) {
        						BeautiSubTemplate template = (BeautiSubTemplate) m_selectPluginBox.getItemAt(i);
                            	if (template.getMainID().replaceAll(".\\$\\(n\\)", "").equals(sID)) {
                            		m_selectPluginBox.setSelectedItem(template);
                            	}
                            }
                        }
                        
                        m_input.setValue(plugin, m_plugin);
                        
                        if (m_expansionBox != null) {
                        	// remove items from Expansion Box
                        	for (int i = 1; i < m_expansionBox.getComponentCount(); ) {
                        		m_expansionBox.remove(i);
                        	}
                        	// add new items to Expansion Box
                        	if (plugin != null) {
                        		PluginPanel.addInputs(m_expansionBox, plugin, _this, _this);
                        	}
                        } else {
                        	// it is not expanded, enable the edit button
                        	if (m_bAddButtons) {
                        		m_editPluginButton.setEnabled(true);
                        	}
                            checkValidation();
                        }
                        sync();
                        refreshPanel();
                    } catch (Exception ex) {
                        sID = ((Plugin)m_input.get()).getID();
                        m_selectPluginBox.setSelectedItem(sID);
                    	//ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Could not change plugin: " +
                                ex.getClass().getName() + " " +
                                ex.getMessage()
                        );
                    }
                }
						
					}
				);
//            }
//            });
            m_selectPluginBox.setToolTipText(input.getTipText());
            m_selectPluginBox.setMaximumSize(new Dimension(1024, 20));
            box.add(m_selectPluginBox);
        }
    }

//    protected void addComboBox2(Box box, Input <?> input, Plugin plugin) {
//        List<String> sAvailablePlugins = PluginPanel.getAvailablePlugins(m_input, m_plugin, null);
//        if (sAvailablePlugins.size() > 0) {
//        	if (m_input.getRule() != Validate.REQUIRED || plugin == null) {
//        		sAvailablePlugins.add(NO_VALUE);
//        	}
//        	for (int i = 0; i < sAvailablePlugins.size(); i++) {
//        		String sPlugin = sAvailablePlugins.get(i);
//        		if (sPlugin.startsWith("new ")) {
//        			sPlugin = sPlugin.substring(sPlugin.lastIndexOf('.'));
//        			sAvailablePlugins.set(i, sPlugin);
//        		}
//
//        	}
//            m_selectPluginBox = new JComboBox(sAvailablePlugins.toArray(new String[0]));
//            String sSelectString = NO_VALUE;
//            if (input.get() != null) {
//                sSelectString = ((Plugin) input.get()).getID();
//            }
//            m_selectPluginBox.setSelectedItem(sSelectString);
//
//            m_selectPluginBox.addActionListener(new ActionListener() {
//                // implements ActionListener
//                public void actionPerformed(ActionEvent e) {
//                	
//                	// get a handle of the selected plugin
//                    String sSelected = (String) m_selectPluginBox.getSelectedItem();
//                    Plugin plugin = (Plugin) m_input.get();
//                    if (sSelected.equals(NO_VALUE)) {
//                        plugin = null;
//                    } else if (!sSelected.startsWith(".")) {
//                        plugin = PluginPanel.g_plugins.get(sSelected);
//                    } else {
//                        List<String> sAvailablePlugins = PluginPanel.getAvailablePlugins(m_input, m_plugin, null);
//                        int i = 0;                     
//                        while (!sAvailablePlugins.get(i).matches(".*\\"+sSelected+"$")) {
//                        	i++;
//                        }
//                    	sSelected = sAvailablePlugins.get(i);                       
//                        /* create new plugin */
//                        try {
//                            plugin = (Plugin) Class.forName(sSelected.substring(4)).newInstance();
//                            PluginPanel.addPluginToMap(plugin);
//                            // tricky: try to connect up new inputs with old inputs of existing name
//                            Plugin oldPlugin = (Plugin) m_input.get();
//                            for (Input<?> oldInput: oldPlugin.listInputs()) {
//                            	String sName = oldInput.getName();
//                            	try {
//                            		Input<?> newInput = plugin.getInput(sName);
//                            		if (newInput.get() instanceof List) {
//                            			List<?> values = (List<?>) oldInput.get();
//                            			for (Object value: values) {
//                                			newInput.setValue(value, plugin);
//                            			}
//                            		} else {
//                            			newInput.setValue(oldInput.get(), plugin);
//                            		}
//                            	} catch (Exception ex) {
//									// ignore
//								}
//                            }
//                            
//                        } catch (Exception ex) {
//                            JOptionPane.showMessageDialog(null, "Could not select plugin: " +
//                                    ex.getClass().getName() + " " +
//                                    ex.getMessage()
//                            );
//                        }
//                    }
//
//                    
//                    try {
//                        if (plugin == null) {
//                            m_selectPluginBox.setSelectedItem(NO_VALUE);
//                            // is this input expanded?
//                            if (m_expansionBox != null) {
//                            	// remove items from Expansion Box, if any
//                            	for (int i = 1; i < m_expansionBox.getComponentCount(); i++) {
//                            		m_expansionBox.remove(i);
//                            	}
//                            } else { // not expanded
//                            	if (m_bAddButtons) {
//                            		m_editPluginButton.setEnabled(false);
//                            	}
//                            }
//                        } else {
//                            if (!m_input.canSetValue(plugin, m_plugin)) {
//                            	throw new Exception("Cannot set input to this value");
//                            }
//                        	// get handle on ID of the plugin, and add to combobox if necessary
//                            String sID = plugin.getID();
//                            // TODO RRB: have to remove ID first, then add it
//                            // The addition is necessary to make the items in the expansionBox scale and show up
//                            // Is there another way?
//                            m_selectPluginBox.removeItem(sID);
//                            m_selectPluginBox.addItem(sID);
//                            m_selectPluginBox.setSelectedItem(sID);
//                        }
//                        
//                        m_input.setValue(plugin, m_plugin);
//                        
//                        if (m_expansionBox != null) {
//                        	// remove items from Expansion Box
//                        	for (int i = 1; i < m_expansionBox.getComponentCount(); ) {
//                        		m_expansionBox.remove(i);
//                        	}
//                        	// add new items to Expansion Box
//                        	if (plugin != null) {
//                        		PluginPanel.addInputs(m_expansionBox, plugin, _this, _this);
//                        	}
//                        } else {
//                        	// it is not expanded, enable the edit button
//                        	if (m_bAddButtons) {
//                        		m_editPluginButton.setEnabled(true);
//                        	}
//                            checkValidation();
//                        }
//                        
//                    } catch (Exception ex) {
//                        String sID = ((Plugin)m_input.get()).getID();
//                        m_selectPluginBox.setSelectedItem(sID);
//                    	//ex.printStackTrace();
//                        JOptionPane.showMessageDialog(null, "Could not change plugin: " +
//                                ex.getClass().getName() + " " +
//                                ex.getMessage()
//                        );
//                    }
//                }
//            });
//            m_selectPluginBox.setToolTipText(input.getTipText());
//            m_selectPluginBox.setMaximumSize(new Dimension(1024, 20));
//            box.add(m_selectPluginBox);
//        }
//    }

    String[] getAvailablePlugins() {
        List<String> sPlugins = AddOnManager.find(m_input.getType(), "beast");
        return sPlugins.toArray(new String[0]);
    } // getAvailablePlugins

} // class PluginInputEditor
