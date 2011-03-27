package beast.app.draw;

import beast.core.Input;
import beast.core.Plugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ListInputEditorOrig extends InputEditor {
    private static final long serialVersionUID = 1L;
    /**
     * for handling list of inputs *
     */
    protected JList m_list;
    protected DefaultListModel m_listModel;

    /**
     * buttons for manipulating the list of inputs *
     */
    protected SmallButton m_addButton;
    protected SmallButton m_delButton;
    protected SmallButton m_editButton;


    public ListInputEditorOrig() {
        super();
    }

    @Override
    public Class<?> type() {
        return ArrayList.class;
    }
    
    /** return type of the list **/
    public Class<?> baseType() {
        return Plugin.class;
    }

    /**
     * construct an editor consisting of
     * o a label
     * o a button for selecting another plug-in
     * o a set of buttons for adding, deleting, editing items in the list
     */
    @Override
    public void init(Input<?> input, Plugin plugin, EXPAND bExpand, boolean bAddButtons) {
		m_bAddButtons = bAddButtons;
        m_input = input;
        m_plugin = plugin;
        addInputLabel();

        /** list of inputs **/
        m_listModel = new DefaultListModel();
        for (Object o : (List<?>) input.get()) {
            if (o instanceof Plugin) {
                Plugin plugin2 = (Plugin) o;
                String sName = plugin2.getID();
                if (sName == null || sName.length() == 0) {
                    sName = plugin2.getClass().getName();
                    sName = sName.substring(sName.lastIndexOf('.') + 1);
                }
                m_listModel.addElement(sName);
            }
        }
        if (m_listModel.isEmpty()) {
            m_listModel.addElement(NO_VALUE);
        }
        m_list = new JList(m_listModel);
        m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_list.setSelectedIndex(0);

        m_list.setLayoutOrientation(JList.VERTICAL);
        m_list.setVisibleRowCount(-1);
        JScrollPane listScroller = new JScrollPane(m_list);
        listScroller.setPreferredSize(new Dimension(250, 50));
        listScroller.setAlignmentX(LEFT_ALIGNMENT);
        add(listScroller);

        /** add, delete, edit buttons to manipulate list of inputs **/

        Box buttonBox = Box.createVerticalBox();
        m_addButton = new SmallButton("+", true);
        m_addButton.setToolTipText("Add item to the list");
        m_addButton.addActionListener(new ActionListener() {
            // implements ActionListener
            public void actionPerformed(ActionEvent e) {
            	addItem();
            }
        });


        m_editButton = new SmallButton("e", false);
        m_editButton.setToolTipText("Edit item in the list");
        m_editButton.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            // implements ActionListener
            public void actionPerformed(ActionEvent e) {
            	editItem();
            }
        });
        m_delButton = new SmallButton("-", false);
        m_delButton.setToolTipText("Delete item from the list");
        m_delButton.addActionListener(new ActionListener() {
            // implements ActionListener
            public void actionPerformed(ActionEvent e) {
            	deleteItem();
            }
        });

        buttonBox.add(m_addButton);
        buttonBox.add(m_editButton);
        buttonBox.add(m_delButton);
        add(buttonBox);
        m_list.setSize(100, 15);
        addValidationLabel();
        updateState();
    } // init

	protected void addItem() {
        List<String> sTabuList = new ArrayList<String>();
        for (int i = 0; i < m_list.getModel().getSize(); i++) {
            sTabuList.add((String) m_list.getModel().getElementAt(i));
        }
        Plugin plugin = pluginSelector(m_input, m_plugin, sTabuList);
        if (plugin != null) {
            try {
                m_input.setValue(plugin, m_plugin);
            } catch (Exception ex) {
                System.err.println(ex.getClass().getName() + " " + ex.getMessage());
            }
            if (m_listModel.size() == 1 && m_listModel.elementAt(0).equals(NO_VALUE)) {
                m_listModel.remove(0);
            }
            m_listModel.addElement(plugin.getID());
            m_list.setSelectedIndex(m_listModel.size() - 1);
            checkValidation();
            updateState();
        }
	} // addItem
	
	@SuppressWarnings("unchecked")
	protected void editItem() {
        int iSelected = m_list.getSelectedIndex();
        String sID = (String) m_listModel.get(iSelected);
        Plugin plugin = PluginPanel.g_plugins.get(sID);
        PluginDialog dlg = new PluginDialog(plugin, m_input.getType());
        dlg.setVisible(true);
        if (dlg.getOK()) {
            ((List<Plugin>) m_input.get()).set(iSelected, dlg.m_panel.m_plugin);
            m_listModel.set(iSelected, dlg.m_panel.m_plugin.getID());
        }
        PluginPanel.m_position.x -= 20;
        PluginPanel.m_position.y -= 20;
        checkValidation();
        updateState();
	} // editItem
    
	protected void deleteItem() {
        int iSelected = m_list.getSelectedIndex();
        ((List<?>) m_input.get()).remove(iSelected);
        m_listModel.remove(iSelected);
        if (m_listModel.size() == 0) {
            m_listModel.addElement(NO_VALUE);
        }
        m_list.setSelectedIndex(Math.max(iSelected - 1, 0));
        checkValidation();
        updateState();
	} // deleteItem
	
    /**
     * Select existing plug-in, or create a new one.
     * Suppress existing plug-ins with IDs from the tabu list.
     * Return null if nothing is selected.
     */
    public Plugin pluginSelector(Input<?> input, Plugin parent, List<String> sTabuList) {
        List<String> sPlugins = PluginPanel.getAvailablePlugins(input, parent, sTabuList);
        /* select a plugin **/
        String sClassName = null;
        if (sPlugins.size() == 1) {
            // if there is only one candidate, select that one
            sClassName = sPlugins.get(0);
        } else {
            // otherwise, pop up a list box
            sClassName = (String) JOptionPane.showInputDialog(null,
                    "Select a constant", "select",
                    JOptionPane.PLAIN_MESSAGE, null,
                    sPlugins.toArray(new String[0]),
                    null);
            if (sClassName == null) {
                return null;
            }
        }
        if (!sClassName.startsWith("new ")) {
            /* return existing plugin */
            return (PluginPanel.g_plugins.get(sClassName));
        }
        /* create new plugin */
        try {
            Plugin plugin = (Plugin) Class.forName(sClassName.substring(4)).newInstance();
            PluginPanel.addPluginToMap(plugin);
            return plugin;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Could not select plugin: " +
                    ex.getClass().getName() + " " +
                    ex.getMessage()
            );
            return null;
        }
    } // pluginSelector

    protected void updateState() {
        boolean bValidSelection = false;
        if (!m_list.isSelectionEmpty()) {
            String sStr = (String) m_list.getSelectedValue();
            if (!sStr.equals(NO_VALUE)) {
                bValidSelection = true;
            }
        }
        m_editButton.setEnabled(bValidSelection);
        m_delButton.setEnabled(bValidSelection);
    } // updateState

} // class ListPluginInputEditor
