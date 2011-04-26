package beast.app.draw;

import beast.core.Input;
import beast.core.Plugin;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ListInputEditor extends InputEditor {
    private static final long serialVersionUID = 1L;
    /**
     * for handling list of inputs *
     */
//    protected JList m_list;
//    protected DefaultListModel m_listModel;

    /**
     * buttons for manipulating the list of inputs *
     */
    protected SmallButton m_addButton;
    //protected List<JLabel> m_labels;
    protected List<JTextField> m_entries;
    protected List<SmallButton> m_delButton;
    protected List<SmallButton> m_editButton;
    protected List<SmallLabel> m_validateLabels;
    protected Box m_listBox;
    protected EXPAND m_bExpand;

    public abstract class ActionListenerObject implements ActionListener {
    	public Object m_o;
    	public ActionListenerObject(Object o) {
    		super();
    		m_o = o;
    	}
    	
    }
    public ListInputEditor() {
        super();
        //m_labels = new ArrayList<JLabel>();
        m_entries = new ArrayList<JTextField>();
        m_delButton = new ArrayList<SmallButton>();
        m_editButton = new ArrayList<SmallButton>();
        m_validateLabels = new ArrayList<SmallLabel>();
        m_bExpand = EXPAND.FALSE;
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
    	m_bExpand = bExpand;
        m_input = input;
        m_plugin = plugin;
        addInputLabel();

        m_listBox = Box.createVerticalBox();
        /** list of inputs **/
        for (Object o : (List<?>) input.get()) {
            if (o instanceof Plugin) {
                Plugin plugin2 = (Plugin) o;
                addSingleItem(plugin2);
            }
        }


        Box box = Box.createHorizontalBox();
        m_addButton = new SmallButton("+", true);
        m_addButton.setToolTipText("Add item to the list");
        m_addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	addItem();
            }
        });
        box.add(m_addButton);
        add(m_listBox);
        if (!g_bExpertMode) {
        	// if nothing can be added, make add button invisible
            List<String> sTabuList = new ArrayList<String>();
//            for (int i = 0; i < m_labels.size(); i++) {
//                sTabuList.add(m_labels.get(i).getText());
//            }
            for (int i = 0; i < m_entries.size(); i++) {
                sTabuList.add(m_entries.get(i).getText());
            }
            List<String> sPlugins = PluginPanel.getAvailablePlugins(m_input, m_plugin, sTabuList);
            if (sPlugins.size() == 0) {
            	m_addButton.setVisible(false);
            }
        }
        
        
		m_validateLabel = new SmallLabel("x", new Color(200,0,0));
        if (m_bAddButtons) {
			box.add(m_validateLabel);
			m_validateLabel.setVisible(true);
			checkValidation();
        }
		box.add(Box.createHorizontalGlue());
        m_listBox.add(box);
		
        updateState();
    } // init
    
    protected void addSingleItem(Plugin plugin) {
        Box itemBox = Box.createHorizontalBox();
        
        if (m_bExpand == EXPAND.FALSE || m_bExpand == EXPAND.IF_ONE_ITEM && ((List<?>) m_input.get()).size()>1) {
	        SmallButton editButton = new SmallButton("e", true);
	        editButton.setToolTipText("Edit item in the list");
	        editButton.addActionListener(new ActionListenerObject(plugin) {
	            public void actionPerformed(ActionEvent e) {
	            	m_o = editItem(m_o);
	            }
	        });
	        m_editButton.add(editButton);
	        itemBox.add(editButton);
        }
        
        SmallButton delButton = new SmallButton("-", true);
        delButton.setToolTipText("Delete item from the list");
        delButton.addActionListener(new ActionListenerObject(plugin) {
            // implements ActionListener
            public void actionPerformed(ActionEvent e) {
            	deleteItem(m_o);
            }
        });
        m_delButton.add(delButton);
        itemBox.add(delButton);
        
        addPluginItem(itemBox, plugin);
        
        SmallLabel validateLabel = new SmallLabel("x", new Color(200,0,0));
        itemBox.add(validateLabel);
		validateLabel.setVisible(true);
		m_validateLabels.add(validateLabel);
        itemBox.setBorder(BorderFactory.createEtchedBorder());
        
        if (m_bExpand == EXPAND.TRUE || 
        		(m_bExpand == EXPAND.IF_ONE_ITEM && ((List<?>) m_input.get()).size() == 1)) {
        	Box box = Box.createVerticalBox();
        	box.add(itemBox);
        	PluginPanel.addInputs(box, plugin, this);
        	box.setBorder(BorderFactory.createEtchedBorder());
        	itemBox = box;
        }
        
        if (m_validateLabel == null) {
        	m_listBox.add(itemBox);
        } else {
        	Component c = m_listBox.getComponent(m_listBox.getComponentCount() - 1);
        	m_listBox.remove(c);
        	m_listBox.add(itemBox);
        	m_listBox.add(c);
        }
    } // addSingleItem

    /** add components to box that are specific for the plugin.
     * By default, this just inserts a label with the plugin ID 
     * @param itemBox box to add components to
     * @param plugin plugin to add
     */
    protected void addPluginItem(Box itemBox, Plugin plugin) {
        String sName = plugin.getID();
        if (sName == null || sName.length() == 0) {
            sName = plugin.getClass().getName();
            sName = sName.substring(sName.lastIndexOf('.') + 1);
        }
        JTextField entry = new JTextField(sName);
        entry.setMinimumSize(new Dimension(200, 16));
        entry.setMaximumSize(new Dimension(200, 20));
		entry.getDocument().addDocumentListener(new IDDocumentListener(plugin, entry));
				
		itemBox.add(entry);
		m_entries.add(entry);
		itemBox.add(Box.createHorizontalGlue());
//        JLabel label = new JLabel(" " + sName);
//        label.setBackground(Color.WHITE);
//        m_labels.add(label);
//        itemBox.add(label);
//        itemBox.add(Box.createHorizontalGlue());
    }

    
	class IDDocumentListener implements DocumentListener {
		Plugin m_plugin;
		JTextField m_entry;
		IDDocumentListener(Plugin plugin, JTextField entry) {
			m_plugin = plugin;
			m_entry = entry;
		}
		@Override
		public void removeUpdate(DocumentEvent e) {
			processEntry();
		}
		@Override
		public void insertUpdate(DocumentEvent e) {
			processEntry();
		}
		@Override
		public void changedUpdate(DocumentEvent e) {
			processEntry();
		}
		void processEntry() {
			String sOldID = m_plugin.getID();
			m_plugin.setID(m_entry.getText());
			PluginPanel.renamePluginID(m_plugin, sOldID, m_plugin.getID());
			validateAllEditors();
			m_entry.requestFocusInWindow();
		}
	}
    
    
	protected void addItem() {
        List<String> sTabuList = new ArrayList<String>();
//        for (int i = 0; i < m_labels.size(); i++) {
//            sTabuList.add(m_labels.get(i).getText().substring(1));
//        }
        for (int i = 0; i < m_entries.size(); i++) {
            sTabuList.add(m_entries.get(i).getText());
        }
        Plugin plugin = pluginSelector(m_input, m_plugin, sTabuList);
        if (plugin != null) {
            try {
           		m_input.setValue(plugin, m_plugin);
            } catch (Exception ex) {
                System.err.println(ex.getClass().getName() + " " + ex.getMessage());
            }
            addSingleItem(plugin);
            checkValidation();
            updateState();
            repaint();
        }
	} // addItem
	
	protected Object editItem(Object o) {
		int i = ((List<?>)m_input.get()).indexOf(o);
        Plugin plugin = (Plugin) ((List<?>)m_input.get()).get(i);
        PluginDialog dlg = new PluginDialog(plugin, m_input.getType());
        dlg.setVisible(true);
        if (dlg.getOK()) {
        	//m_labels.get(i).setText(dlg.m_panel.m_plugin.getID());
        	m_entries.get(i).setText(dlg.m_panel.m_plugin.getID());
        	//o = dlg.m_panel.m_plugin;
        	dlg.accept((Plugin) o);
            refresh();
        }
        PluginPanel.m_position.x -= 20;
        PluginPanel.m_position.y -= 20;
        //checkValidation();
        validateAllEditors();
        updateState();
        doLayout();
        return o;
	} // editItem
    
    void refresh() {
        Component c = this;
        while (((Component) c).getParent() != null) {
        	c = ((Component) c).getParent();
        	if (c instanceof ListSelectionListener) {
        		((ListSelectionListener) c).valueChanged(null);
        	}
        }
    }

    protected void deleteItem(Object o) {
		int i = ((List<?>)m_input.get()).indexOf(o);
		m_listBox.remove(i);
		((List<?>)m_input.get()).remove(i);
		//safeRemove(m_labels, i);
		safeRemove(m_entries, i);
		safeRemove(m_delButton, i);
		safeRemove(m_editButton, i);
		safeRemove(m_validateLabels, i);
        checkValidation();
        updateState();
        doLayout();
        repaint();
	} // deleteItem
		
    private void safeRemove(List<?> list, int i) {
		if (list.size() > i) {
			list.remove(i);
		}
	}

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
        } else if (sPlugins.size() == 0) {
        	// no candidate => we cannot be in expert mode
        	// create a new Plugin 
        	InputEditor.g_bExpertMode = true;
            sPlugins = PluginPanel.getAvailablePlugins(input, parent, sTabuList);
        	InputEditor.g_bExpertMode = false;
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
    	for(int i = 0; i < ((List<?>)m_input.get()).size(); i++) {
    		try {
    			Plugin plugin = (Plugin) ((List<?>)m_input.get()).get(i);
    			plugin.validateInputs();
				m_validateLabels.get(i).setVisible(false);
    		} catch (Exception e) {
    			if (m_validateLabels.size() > i) {
	    			m_validateLabels.get(i).setToolTipText(e.getMessage());
					m_validateLabels.get(i).setVisible(true);
    			}
			}
    	}
		checkValidation();
		// this triggers properly re-layouting after an edit action
        setVisible(false);
        setVisible(true);
    } // updateState

	@Override
	public void validate(ValidationStatus state) {
		updateState();
	}
} // class ListPluginInputEditor
