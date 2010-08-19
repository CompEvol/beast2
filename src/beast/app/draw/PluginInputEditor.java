package beast.app.draw;


import beast.core.Input;
import beast.core.Plugin;
import beast.util.ClassDiscovery;

import javax.swing.*;
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
    public void init(Input<?> input, Plugin plugin) {
        m_input = input;
        m_plugin = plugin;

        addInputLabel();

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
                            m_editPluginButton.setEnabled(false);
                            m_selectPluginBox.setSelectedItem(NO_VALUE);
                        } else {
                            String sID = plugin.getID();
                            boolean bContainsID = false;
                            for (int i = 0; i < m_selectPluginBox.getItemCount(); i++) {
                                String sStr = (String) m_selectPluginBox.getItemAt(i);
                                if (sStr.equals(sID)) {
                                    bContainsID = true;
                                }
                            }
                            if (!bContainsID) {
                                m_selectPluginBox.addItem(sID);
                            }
                            m_selectPluginBox.setSelectedItem(sID);
                        }
                        m_input.setValue(plugin, m_plugin);
                        m_editPluginButton.setEnabled(true);
                        checkValidation();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Could not change plugin: " +
                                ex.getClass().getName() + " " +
                                ex.getMessage()
                        );
                    }
                }
            });
            m_selectPluginBox.setToolTipText(input.getTipText());
            add(m_selectPluginBox);
        }

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

    String[] getAvailablePlugins() {
        List<String> sPlugins = ClassDiscovery.find(m_input.getType(), "beast");
        return sPlugins.toArray(new String[0]);
    } // getAvailablePlugins

} // class PluginInputEditor
