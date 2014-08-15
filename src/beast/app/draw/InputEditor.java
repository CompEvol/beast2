package beast.app.draw;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;

import beast.app.beauti.BeautiDoc;
import beast.app.beauti.BeautiPanel;
import beast.app.beauti.BeautiPanelConfig;
import beast.core.Input;
import beast.core.BEASTInterface;

/**
 * Base class for editors that provide a GUI for manipulating an Input for a Plugin.
 * The idea is that for every type of Input there will be a dedicated editor, e.g.
 * for a String Input, there will be an edit field, for a Boolean Input, there will
 * be a checkbox in the editor.
 * <p/>
 * The default just provides an edit field and uses toString() on Input to get its value.
 * To change the behaviour, override
 * public void init(Input<?> input, Plugin plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons)
 */
/** note that it is assumed that any InputEditor is a java.awt.Component **/
public interface InputEditor {

    final public static String NO_VALUE = "<none>";

    public enum ExpandOption {TRUE, TRUE_START_COLLAPSED, FALSE, IF_ONE_ITEM}

    public enum ButtonStatus {ALL, NONE, DELETE_ONLY, ADD_ONLY}
    
    public enum ValidationStatus {
        IS_VALID,
        IS_INVALID,
        HAS_INVALIDMEMBERS
    }


    /** type of Plugin to which this editor can be used **/ 
    Class<?> type();

    /** list of types of Plugins to which this editor can be used **/ 
    Class<?>[] types();

    /** initialise InputEditor
     * @param input to be edited
     * @param plugin parent plugin containing the input
     * @param itemNr if the input is a list, itemNr indicates which item to edit in the list
     * @param bExpandOption start state of input editor
     * @param bAddButtons button status of input editor
     */
    void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons);

    /** set document with the model containing the input **/
    void setDoc(BeautiDoc doc);

    /** set decoration **/
    void setBorder(Border border);

    /** prepare to validate input **/
    void startValidating(ValidationStatus state);
    
    /** validate input and update status of input editor if necessary **/
    void validateInput();
    
    /** add input editor to listen for changes **/
    void addValidationListener(InputEditor validateListener);
    
    /** propagate status of predecessor inputs through list of plugins **/
    void notifyValidationListeners(ValidationStatus state);
    
    Component getComponent();

public abstract class Base extends JPanel implements InputEditor {

    private static final long serialVersionUID = 1L;
    /**
     * the input to be edited *
     */
    protected Input<?> m_input;
    /**
     * parent plugin *
     */
    protected BEASTInterface m_plugin;
    /**
     * text field used for primitive input editors *
     */
    protected JTextField m_entry;
    
    protected int itemNr;

    public JTextField getEntry() {
        return m_entry;
    }

    JLabel m_inputLabel;
    protected static Dimension PREFERRED_SIZE = new Dimension(200, 25);
    protected static Dimension MAX_SIZE = new Dimension(1024, 25);

    /**
     * flag to indicate label, edit and validate buttons/labels should be added *
     */
    protected boolean m_bAddButtons = true;

    /**
     * label that shows up when validation fails *
     */
    protected SmallLabel m_validateLabel;

    /**
     * document that we are editing *
     */
    protected BeautiDoc doc;

    /**
     * list of objects that want to be notified of the validation state when it changes *
     */
    List<InputEditor> m_validateListeners;

    public void addValidationListener(InputEditor validateListener) {
        if (m_validateListeners == null) {
            m_validateListeners = new ArrayList<InputEditor>();
        }
        m_validateListeners.add(validateListener);
    }

    public void notifyValidationListeners(ValidationStatus state) {
        if (m_validateListeners != null) {
            for (InputEditor listener : m_validateListeners) {
                listener.startValidating(state);
            }
        }
    }


    public static Integer g_nLabelWidth = 150;

//    Box box;
    
//    public Base() {
////    	box = Box.createHorizontalBox();
//        super(BoxLayout.X_AXIS);
//        //g_currentInputEditors.add(this);
////    	super.add(box);
//    }

//    @Override
//   	public Component add(Component comp) {
//   		return box.add(comp);
//   	}

	public Base(BeautiDoc doc) {
		//super(BoxLayout.X_AXIS);
		//box = Box.createHorizontalBox();
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		//setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		//setLayout(new GridLayout());
		this.doc = doc;
		if (doc != null) {
			doc.currentInputEditors.add(this);
		}
		//super.add(box);
	} // c'tor

	protected BeautiDoc getDoc() {
        if (doc == null) {
            Component c = this;
            while (c.getParent() != null) {
                c = c.getParent();
                if (c instanceof BeautiPanel) {
                    doc = ((BeautiPanel) c).getDoc();
                }
            }
        }
        return doc;
    }


    /**
     * return class the editor is suitable for.
     * Either implement type() or types() if multiple
     * types are supported *
     */
    abstract public Class<?> type();

    public Class<?>[] types() {
        Class<?>[] types = new Class<?>[1];
        types[0] = type();
        return types;
    }

    /**
     * construct an editor consisting of a label and input entry *
     */
    public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
        m_bAddButtons = bAddButtons;
        m_input = input;
        m_plugin = plugin;
        this.itemNr= itemNr;
        
        addInputLabel();

        setUpEntry();

        add(m_entry);
        add(Box.createHorizontalGlue());
        addValidationLabel();
    } // init


    void setUpEntry() {
        m_entry = new JTextField();
        m_entry.setName(m_input.getName());
        m_entry.setMinimumSize(PREFERRED_SIZE);
        m_entry.setPreferredSize(PREFERRED_SIZE);
        m_entry.setSize(PREFERRED_SIZE);
        initEntry();
        m_entry.setToolTipText(m_input.getHTMLTipText());
        m_entry.setMaximumSize(MAX_SIZE);

        m_entry.getDocument().addDocumentListener(new DocumentListener() {
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
        });
    }

    protected void initEntry() {
        if (m_input.get() != null) {
            m_entry.setText(m_input.get().toString());
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	protected void setValue(Object o) throws Exception {
    	if (itemNr < 0) {
    		m_input.setValue(o, m_plugin);
    	} else {
    		// set value of an item in a list
    		List list = (List) m_input.get();
    		Object other = list.get(itemNr);
    		if (other != o) {
    			if (other instanceof BEASTInterface) {
    				BEASTInterface.getOutputs(other).remove(m_plugin);
    			}
    			list.set(itemNr, o);
    			if (o instanceof BEASTInterface) {
    				BEASTInterface.getOutputs(o).add(m_plugin);
    			}
    		}
    	}
    }
    
    protected void processEntry() {
        try {
        	setValue(m_entry.getText());
            validateInput();
            m_entry.requestFocusInWindow();
        } catch (Exception ex) {
//			JOptionPane.showMessageDialog(null, "Error while setting " + m_input.getName() + ": " + ex.getMessage() +
//					" Leaving value at " + m_input.get());
//			m_entry.setText(m_input.get() + "");
            if (m_validateLabel != null) {
                m_validateLabel.setVisible(true);
                m_validateLabel.setToolTipText("<html><p>Parsing error: " + ex.getMessage() + ". Value was left at " + m_input.get() + ".</p></html>");
                m_validateLabel.m_circleColor = Color.orange;
            }
            repaint();
        }
    }

    protected void addInputLabel() {
        if (m_bAddButtons) {
            String sName = formatName(m_input.getName());
            addInputLabel(sName, m_input.getHTMLTipText());
        }
    }

    protected String formatName(String sName) {
	    if (doc.beautiConfig.inputLabelMap.containsKey(m_plugin.getClass().getName() + "." + sName)) {
	        sName = doc.beautiConfig.inputLabelMap.get(m_plugin.getClass().getName() + "." + sName);
	    } else {
	        sName = sName.replaceAll("([a-z])([A-Z])", "$1 $2");
	        sName = sName.substring(0, 1).toUpperCase() + sName.substring(1);
	    }
	    return sName;
    }

    protected void addInputLabel(String sLabel, String sTipText) {
        if (m_bAddButtons) {
            m_inputLabel = new JLabel(sLabel);
            m_inputLabel.setToolTipText(sTipText);
            m_inputLabel.setHorizontalTextPosition(JLabel.RIGHT);
            //Dimension size = new Dimension(g_nLabelWidth, 20);
            Dimension size = new Dimension(200, 20);
            m_inputLabel.setMaximumSize(size);
            m_inputLabel.setMinimumSize(size);
            m_inputLabel.setPreferredSize(size);
            m_inputLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

//            m_inputLabel.setSize(size);
//            m_inputLabel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            // RRB: temporary
            //m_inputLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            if (sLabel.equals("Subst Model")) {
            	int h = 3;
            	h++;
            }
            add(m_inputLabel);
        }
    }

    protected void addValidationLabel() {
        if (m_bAddButtons) {
            m_validateLabel = new SmallLabel("x", new Color(200, 0, 0));
            add(m_validateLabel);
            m_validateLabel.setVisible(true);
            validateInput();
        }
    }

    /* check the input is valid, continue checking recursively */
    protected void validateAllEditors() {
        for (InputEditor editor : doc.currentInputEditors) {
            editor.validateInput();
        }
    }

    @Override
    public void validateInput() {
        try {
            m_input.validate();
            if (m_entry != null && !m_input.canSetValue(m_entry.getText(), m_plugin)) {
                throw new Exception("invalid value");
            }
            // recurse
            try {
                validateRecursively(m_input, new HashSet<Input<?>>());
            } catch (Exception e) {
                notifyValidationListeners(ValidationStatus.HAS_INVALIDMEMBERS);
                if (m_validateLabel != null) {
                    m_validateLabel.setVisible(true);
                    m_validateLabel.setToolTipText("<html><p>Recursive error in " + e.getMessage() + "</p></html>");
                    m_validateLabel.m_circleColor = Color.orange;
                }
                repaint();
                return;
            }
            if (m_validateLabel != null) {
                m_validateLabel.setVisible(false);
            }
            notifyValidationListeners(ValidationStatus.IS_VALID);
        } catch (Exception e) {
            System.err.println("Validation message: " + e.getMessage());
            if (m_validateLabel != null) {
                m_validateLabel.setToolTipText(e.getMessage());
                m_validateLabel.m_circleColor = Color.red;
                m_validateLabel.setVisible(true);
            }
            notifyValidationListeners(ValidationStatus.IS_INVALID);
        }
        repaint();
    }

    /* Recurse in any of the input plugins
      * and validate its inputs */
    void validateRecursively(Input<?> input, Set<Input<?>> done) throws Exception {
        if (done.contains(input)) {
            // this prevent cycles to lock up validation
            return;
        } else {
            done.add(input);
        }
        if (input.get() != null) {
            if (input.get() instanceof BEASTInterface) {
                BEASTInterface plugin = ((BEASTInterface) input.get());
                for (Input<?> input2 : plugin.listInputs()) {
                    try {
                        input2.validate();
                    } catch (Exception e) {
                        throw new Exception(((BEASTInterface) input.get()).getID() + "</p><p> " + e.getMessage());
                    }
                    validateRecursively(input2, done);
                }
            }
            if (input.get() instanceof List<?>) {
                for (Object o : (List<?>) input.get()) {
                    if (o != null && o instanceof BEASTInterface) {
                        BEASTInterface plugin = (BEASTInterface) o;
                        for (Input<?> input2 : plugin.listInputs()) {
                            try {
                                input2.validate();
                            } catch (Exception e) {
                                throw new Exception(((BEASTInterface) o).getID() + " " + e.getMessage());
                            }
                            validateRecursively(input2, done);
                        }
                    }
                }
            }
        }
    } // validateRecursively

    @Override
    public void startValidating(ValidationStatus state) {
        validateInput();
    }


    public void refreshPanel() {
        Component c = this;
        while (c.getParent() != null) {
            c = c.getParent();
            if (c instanceof ListSelectionListener) {
                ((ListSelectionListener) c).valueChanged(null);
            }
        }
    }

    /**
     * synchronise values in panel with current network *
     */
    protected void sync() {
        Component c = this;
        while (c.getParent() != null) {
            c = c.getParent();
            if (c instanceof BeautiPanel) {
                BeautiPanel panel = (BeautiPanel) c;
                BeautiPanelConfig cfgPanel = panel.config;
                cfgPanel.sync(panel.iPartition);
            }
        }
    }

    // we should leave it to the component to set its own border
    @Deprecated
    public void setBorder(Border border) {
		super.setBorder(border);
    }

    @Override
    public void setDoc(BeautiDoc doc) {
    	this.doc = doc;
    }

    // what is this method for? We should leave repainting to the standard mechanism
    @Deprecated
	public void repaint() {
	this.repaint(0);
		super.repaint();
	}

	public Component getComponent() {
		return this;
	}

} // class InputEditor.Base

} // InputEditor interface
