package beast.app.draw;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import beast.app.beauti.BeautiDoc;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.util.Log;

public class ListInputEditor extends InputEditor.Base {

    private static final long serialVersionUID = 1L;
    static Image DOWN_ICON;
    static Image RIGHT_ICON;

    {
        try {
            java.net.URL downURL = ClassLoader.getSystemResource(ModelBuilder.ICONPATH + "down.png");
            DOWN_ICON = ImageIO.read(downURL); 
            java.net.URL leftURL = ClassLoader.getSystemResource(ModelBuilder.ICONPATH + "right.png");
            RIGHT_ICON = ImageIO.read(leftURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected ButtonStatus m_buttonStatus = ButtonStatus.ALL;

    /**
     * buttons for manipulating the list of inputs *
     */
    protected SmallButton addButton;
    protected List<JTextField> m_entries;
    protected List<SmallButton> delButtonList;
    protected List<SmallButton> m_editButton;
    protected List<SmallLabel> m_validateLabels;
    protected Box m_listBox;
    protected ExpandOption m_bExpandOption;

    // the box containing any buttons
    protected Box buttonBox;

    static protected Set<String> g_collapsedIDs = new HashSet<>();
    static Set<String> g_initiallyCollapsedIDs = new HashSet<>();

    public abstract class ActionListenerObject implements ActionListener {
        public Object m_o;

        public ActionListenerObject(Object o) {
            super();
            m_o = o;
        }
    }

    public abstract class ExpandActionListener implements ActionListener {
        Box m_box;
        BEASTInterface m_beastObject;

        public ExpandActionListener(Box box, BEASTInterface beastObject) {
            super();
            m_box = box;
            m_beastObject = beastObject;
        }
    }

    //public ListInputEditor() {}
    public ListInputEditor(BeautiDoc doc) {
        super(doc);
        m_entries = new ArrayList<>();
        delButtonList = new ArrayList<>();
        m_editButton = new ArrayList<>();
        m_validateLabels = new ArrayList<>();
        m_bExpandOption = ExpandOption.FALSE;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    @Override
    public Class<?> type() {
        return ArrayList.class;
    }

    /**
     * return type of the list *
     */
    public Class<?> baseType() {
        return BEASTInterface.class;
    }

    /**
     * construct an editor consisting of
     * o a label
     * o a button for selecting another plug-in
     * o a set of buttons for adding, deleting, editing items in the list
     */
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_bExpandOption = isExpandOption;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr = -1;
        addInputLabel();
        if (m_inputLabel != null) {
            m_inputLabel.setMaximumSize(new Dimension(m_inputLabel.getSize().width, 1000));
            m_inputLabel.setAlignmentY(1.0f);
            m_inputLabel.setVerticalAlignment(SwingConstants.TOP);
            m_inputLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }

        m_listBox = Box.createVerticalBox();
        // list of inputs 
        for (Object o : (List<?>) input.get()) {
            if (o instanceof BEASTInterface) {
                BEASTInterface beastObject2 = (BEASTInterface) o;
                addSingleItem(beastObject2);
            }
        }

        setLayout(new BorderLayout());
        add(m_listBox, BorderLayout.NORTH);

        buttonBox = Box.createHorizontalBox();
        if (m_buttonStatus == ButtonStatus.ALL || m_buttonStatus == ButtonStatus.ADD_ONLY) {
            addButton = new SmallButton("+", true);
            addButton.setName("+");
            addButton.setToolTipText("Add item to the list");
            addButton.addActionListener(e -> addItem());
            buttonBox.add(addButton);
            if (!doc.isExpertMode()) {
                // if nothing can be added, make add button invisible
                List<String> tabuList = new ArrayList<>();
                for (int i = 0; i < m_entries.size(); i++) {
                    tabuList.add(m_entries.get(i).getText());
                }
                List<String> beastObjectNames = doc.getInputEditorFactory().getAvailablePlugins(m_input, m_beastObject, tabuList, doc);
                if (beastObjectNames.size() == 0) {
                    addButton.setVisible(false);
                }
            }
        }

        // add validation label at the end of a list
        m_validateLabel = new SmallLabel("x", new Color(200, 0, 0));
        if (m_bAddButtons) {
            buttonBox.add(m_validateLabel);
            m_validateLabel.setVisible(true);
            validateInput();
        }
        buttonBox.add(Box.createHorizontalGlue());
        m_listBox.add(buttonBox);

        updateState();
        
//        // RRB: is there a better way to ensure lists are not spaced out across all available space?
//    	JFrame frame = doc.getFrame();
//    	if (frame != null) {
//    		m_listBox.add(Box.createVerticalStrut(frame.getHeight() - 150));
//    	}

    } // init

    protected void addSingleItem(BEASTInterface beastObject) {
        Box itemBox = Box.createHorizontalBox();

        InputEditor editor = addPluginItem(itemBox, beastObject);
        
        SmallButton editButton = new SmallButton("e", true, SmallButton.ButtonType.square);
        editButton.setName(beastObject.getID() + ".editButton");
        if (m_bExpandOption == ExpandOption.FALSE || m_bExpandOption == ExpandOption.IF_ONE_ITEM && ((List<?>) m_input.get()).size() > 1) {
            editButton.setToolTipText("Edit item in the list");
            editButton.addActionListener(new ActionListenerObject(beastObject) {
                @Override
				public void actionPerformed(ActionEvent e) {
                    m_o = editItem(m_o);
                }
            });
        } else {
            editButton.setText("");
            editButton.setToolTipText("Expand/collapse item in the list");
            editButton.setButtonType(SmallButton.ButtonType.toolbar);
        }
        m_editButton.add(editButton);
        itemBox.add(editButton);


        SmallLabel validateLabel = new SmallLabel("x", new Color(200, 0, 0));
        itemBox.add(validateLabel);
        validateLabel.setVisible(true);
        m_validateLabels.add(validateLabel);

        // AJD: This is not consistent with Mac OS X look and feel, and its not necessary
        //itemBox.setBorder(BorderFactory.createEtchedBorder());

        if (m_bExpandOption == ExpandOption.TRUE || m_bExpandOption == ExpandOption.TRUE_START_COLLAPSED ||
                (m_bExpandOption == ExpandOption.IF_ONE_ITEM && ((List<?>) m_input.get()).size() == 1)) {
            Box expandBox = Box.createVerticalBox();
            //box.add(itemBox);
            doc.getInputEditorFactory().addInputs(expandBox, beastObject, editor, null, doc);
            //System.err.print(expandBox.getComponentCount());
            if (expandBox.getComponentCount() > 1) {
                // only go here if it is worth showing expanded box
                //expandBox.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.gray));
                //itemBox = box;
                Box box2 = Box.createVerticalBox();
                box2.add(itemBox);
                itemBox.add(editButton, 0);
                box2.add(expandBox);
//        		expandBox.setVisible(false);
//        		//itemBox.remove(editButton);
//        		editButton.setVisible(false);
//        	} else {
                itemBox = box2;
            } else {
                editButton.setVisible(false);
            }
            editButton.addActionListener(new ExpandActionListener(expandBox, beastObject) {
                @Override
				public void actionPerformed(ActionEvent e) {
                    SmallButton editButton = (SmallButton) e.getSource();
                    m_box.setVisible(!m_box.isVisible());
                    if (m_box.isVisible()) {
                        try {
                        editButton.setImg(DOWN_ICON);
                        }catch (Exception e2) {
							// TODO: handle exception
						}
                        g_collapsedIDs.remove(m_beastObject.getID());
                    } else {
                    	try {
                        editButton.setImg(RIGHT_ICON);
	                    }catch (Exception e2) {
							// TODO: handle exception
						}
                        g_collapsedIDs.add(m_beastObject.getID());
                    }
                }
            });
            String id = beastObject.getID();
            expandBox.setVisible(!g_collapsedIDs.contains(id));
            try {
            if (expandBox.isVisible()) {
                editButton.setImg(DOWN_ICON);
            } else {
                editButton.setImg(RIGHT_ICON);
            }
            } catch (Exception e) {
				// TODO: handle exception
			}


        } else {
            if (BEASTObjectPanel.countInputs(beastObject, doc) == 0) {
                editButton.setVisible(false);
            }
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

    /**
     * add components to box that are specific for the beastObject.
     * By default, this just inserts a label with the beastObject ID
     *
     * @param itemBox box to add components to
     * @param beastObject  beastObject to add
     */
    protected InputEditor addPluginItem(Box itemBox, BEASTInterface beastObject) {
        String name = beastObject.getID();
        if (name == null || name.length() == 0) {
            name = beastObject.getClass().getName();
            name = name.substring(name.lastIndexOf('.') + 1);
        }
        JLabel label = new JLabel(name);

        itemBox.add(Box.createRigidArea(new Dimension(5, 1)));
        itemBox.add(label);
        itemBox.add(Box.createHorizontalGlue());
        return this;
    }

    class IDDocumentListener implements DocumentListener {
        BEASTInterface m_beastObject;
        JTextField m_entry;

        IDDocumentListener(BEASTInterface beastObject, JTextField entry) {
            m_beastObject = beastObject;
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
            String oldID = m_beastObject.getID();
            m_beastObject.setID(m_entry.getText());
            BEASTObjectPanel.renamePluginID(m_beastObject, oldID, m_beastObject.getID(), doc);
            validateAllEditors();
            m_entry.requestFocusInWindow();
        }
    }

    protected void addItem() {
        List<String> tabuList = new ArrayList<>();
        for (int i = 0; i < m_entries.size(); i++) {
            tabuList.add(m_entries.get(i).getText());
        }
        List<BEASTInterface> beastObjects = pluginSelector(m_input, m_beastObject, tabuList);
        if (beastObjects != null) {
            for (BEASTInterface beastObject : beastObjects) {
                try {
                	setValue(beastObject);
                    //m_input.setValue(beastObject, m_beastObject);
                } catch (Exception ex) {
                    Log.err.println(ex.getClass().getName() + " " + ex.getMessage());
                }
                addSingleItem(beastObject);
                getDoc().addPlugin(beastObject);
            }
            validateInput();
            updateState();
            repaint();
        }
    } // addItem

    protected Object editItem(Object o) {
        int i = ((List<?>) m_input.get()).indexOf(o);
        BEASTInterface beastObject = (BEASTInterface) ((List<?>) m_input.get()).get(i);
        BEASTObjectDialog dlg = new BEASTObjectDialog(beastObject, m_input.getType(), doc);
        if (dlg.showDialog()) {
            //m_labels.get(i).setText(dlg.m_panel.m_beastObject.getID());
        	if (m_entries.size() > i) {
        		m_entries.get(i).setText(dlg.m_panel.m_beastObject.getID());
        	}
            //o = dlg.m_panel.m_beastObject;
            dlg.accept((BEASTInterface) o, doc);
            refreshPanel();
        }
        BEASTObjectPanel.m_position.x -= 20;
        BEASTObjectPanel.m_position.y -= 20;
        //checkValidation();
        validateAllEditors();
        updateState();
        doLayout();
        return o;
    } // editItem

    protected void deleteItem(Object o) {
        int i = ((List<?>) m_input.get()).indexOf(o);
        m_listBox.remove(i);
        ((List<?>) m_input.get()).remove(i);
        //safeRemove(m_labels, i);
        safeRemove(m_entries, i);
        safeRemove(delButtonList, i);
        safeRemove(m_editButton, i);
        safeRemove(m_validateLabels, i);
        validateInput();
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
     * Suppress existing plug-ins with IDs from the taboo list.
     * Return null if nothing is selected.
     */
    protected List<BEASTInterface> pluginSelector(Input<?> input, BEASTInterface parent, List<String> tabooList) {
        List<BEASTInterface> selectedPlugins = new ArrayList<>();
        List<String> beastObjectNames = doc.getInputEditorFactory().getAvailablePlugins(input, parent, tabooList, doc);
        /* select a beastObject **/
        String className = null;
        if (beastObjectNames.size() == 1) {
            // if there is only one candidate, select that one
            className = beastObjectNames.get(0);
        } else if (beastObjectNames.size() == 0) {
            // no candidate => we cannot be in expert mode
            // create a new BEASTObject
            doc.setExpertMode(true);
            beastObjectNames = doc.getInputEditorFactory().getAvailablePlugins(input, parent, tabooList, doc);
            doc.setExpertMode(false);
            className = beastObjectNames.get(0);
        } else {
            // otherwise, pop up a list box
            className = (String) JOptionPane.showInputDialog(null,
                    "Select a constant", "select",
                    JOptionPane.PLAIN_MESSAGE, null,
                    beastObjectNames.toArray(new String[0]),
                    null);
            if (className == null) {
                return null;
            }
        }
        if (!className.startsWith("new ")) {
            /* return existing beastObject */
            selectedPlugins.add(doc.pluginmap.get(className));
            return selectedPlugins;
        }
        /* create new beastObject */
        try {
            BEASTInterface beastObject = (BEASTInterface) Class.forName(className.substring(4)).newInstance();
            BEASTObjectPanel.addPluginToMap(beastObject, doc);
            selectedPlugins.add(beastObject);
            return selectedPlugins;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Could not select beastObject: " +
                    ex.getClass().getName() + " " +
                    ex.getMessage()
            );
            return null;
        }
    } // pluginSelector

    protected void updateState() {
        for (int i = 0; i < ((List<?>) m_input.get()).size(); i++) {
            try {
                BEASTInterface beastObject = (BEASTInterface) ((List<?>) m_input.get()).get(i);
                beastObject.validateInputs();
                m_validateLabels.get(i).setVisible(false);
            } catch (IndexOutOfBoundsException e) {
            	// happens when m_validateLabels is not large enough, so there is nothing to show 
            } catch (Exception e) {
            	// something went wrong, so show label if available
                if (m_validateLabels.size() > i) {
                    m_validateLabels.get(i).setToolTipText(e.getMessage());
                    m_validateLabels.get(i).setVisible(true);
                }
            }
        }
        validateInput();
        // this triggers properly re-layouting after an edit action
        setVisible(false);
        setVisible(true);
    } // updateState

    @Override
    public void startValidating(ValidationStatus state) {
        updateState();
    }

    public void setButtonStatus(ButtonStatus buttonStatus) {
        m_buttonStatus = buttonStatus;
    }

} // class ListPluginInputEditor
