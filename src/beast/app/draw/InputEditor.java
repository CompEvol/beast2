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
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;

import beast.app.beauti.BeautiDoc;
import beast.app.beauti.BeautiPanel;
import beast.app.beauti.BeautiPanelConfig;
import beast.core.Input;
import beast.core.Plugin;

/** Base class for editors that provide a GUI for manipulating an Input for a Plugin.
 * The idea is that for every type of Input there will be a dedicated editor, e.g.
 * for a String Input, there will be an edit field, for a Boolean Input, there will 
 * be a checkbox in the editor.
 * 
 * The default just provides an edit field and uses toString() on Input to get its value.
 * To change the behaviour, override
    public void init(Input<?> input, Plugin plugin) {
 **/ 
public abstract class InputEditor extends Box implements ValidateListener {
	final public static String NO_VALUE = "<none>";
	
	public enum ExpandOption {TRUE, TRUE_START_COLLAPSED, FALSE, IF_ONE_ITEM};
	public enum ButtonStatus {ALL, NONE, DELETE_ONLY, ADD_ONLY};

	private static boolean isExpertMode = false;

    private static final long serialVersionUID = 1L;
	/** the input to be edited **/
	protected Input<?> m_input;
	/** parent plugin **/
	protected Plugin m_plugin;
	/** text field used for primitive input editors **/
	JTextField m_entry;
	public JTextField getEntry() {return m_entry;}

	JLabel m_inputLabel;
	
	/** flag to indicate label, edit and validate buttons/labels should be added **/
	protected boolean m_bAddButtons = true;
	
	/** label that shows up when validation fails **/
	protected SmallLabel m_validateLabel;
	
	/** document that we are editing **/
	protected BeautiDoc doc;
	
	/** list of objects that want to be notified of the validation state when it changes **/
	List<ValidateListener> m_validateListeners;
	public void addValidationListener(ValidateListener validateListener) {
		if (m_validateListeners == null) {
			m_validateListeners = new ArrayList<ValidateListener>();
		}
		m_validateListeners.add(validateListener);
	}
	void notifyValidationListeners(ValidationStatus state) {
		if (m_validateListeners != null) {
			for (ValidateListener listener : m_validateListeners) {
				listener.validate(state);
			}
		}
	}

	static public Set<InputEditor> g_currentInputEditors = new HashSet<InputEditor>();

	public static Integer g_nLabelWidth = 150;
	
	public InputEditor() {
        super(BoxLayout.X_AXIS);
    }
	
//	public InputEditor(BeautiDoc doc) {
//		super(BoxLayout.X_AXIS);
//		//setAlignmentX(LEFT_ALIGNMENT);
//		g_currentInputEditors.add(this);
//	} // c'tor
	
	
	protected BeautiDoc getDoc() {
		if (doc == null) {
		    Component c = this;
		    while (((Component) c).getParent() != null) {
		      	c = ((Component) c).getParent();
		      	if (c instanceof BeautiPanel) {
		      		doc = ((BeautiPanel) c).getDoc();
		      	}
		    }
		}
		return doc;
	}

	
	/** return class the editor is suitable for.
	 * Either implement type() or types() if multiple
	 * types are supported **/
	abstract public Class<?> type();
	public Class<?>[] types() {
		Class<?>[] types = new Class<?>[1];
		types[0] = type();
		return types;
	}
	
	/** construct an editor consisting of a label and input entry **/
	public void init(Input<?> input, Plugin plugin, ExpandOption bExpandOption, boolean bAddButtons) {
		m_bAddButtons = bAddButtons;
		m_input = input;
		m_plugin = plugin;

		addInputLabel();
		
		setUpEntry();
		
		add(m_entry);
		add(Box.createHorizontalGlue());
		addValidationLabel();
	} // init


	void setUpEntry() {
		m_entry = new JTextField();
		Dimension size = new Dimension(100,20);
		m_entry.setMinimumSize(size);
		m_entry.setPreferredSize(size);
		m_entry.setSize(size);
		initEntry();
		m_entry.setToolTipText(m_input.getTipText());
		m_entry.setMaximumSize(new Dimension(1024, 20));
		
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

	void initEntry() {
		if (m_input.get()!= null) {
			m_entry.setText(m_input.get().toString());
		}
	}
	
	void processEntry() {
		try {
			m_input.setValue(m_entry.getText(), m_plugin);
			checkValidation();
			m_entry.requestFocusInWindow();
		} catch (Exception ex) {
//			JOptionPane.showMessageDialog(null, "Error while setting " + m_input.getName() + ": " + ex.getMessage() +
//					" Leaving value at " + m_input.get());
//			m_entry.setText(m_input.get() + "");
			if (m_validateLabel != null) {
				m_validateLabel.setVisible(true);
				m_validateLabel.setToolTipText("<html><p>Parsing error: " + ex.getMessage() + ". Value was left at " + m_input.get() +".</p></html>");
				m_validateLabel.m_circleColor = Color.orange;
			}
			repaint();
		}
	}
	
	protected void addInputLabel() {
		if (m_bAddButtons) {
			String sName = m_input.getName();
			if (doc.beautiConfig.inputLabelMap.containsKey(m_plugin.getClass().getName()+"."+sName)) {
				sName = doc.beautiConfig.inputLabelMap.get(m_plugin.getClass().getName()+"."+sName);
			} else {
				sName = sName.replaceAll("([a-z])([A-Z])", "$1 $2");
				sName = sName.substring(0,1).toUpperCase() + sName.substring(1);
			}
			addInputLabel(sName, m_input.getTipText());
	    }
	}

	protected void addInputLabel(String sLabel, String sTipText) {
		if (m_bAddButtons) {
			m_inputLabel = new JLabel(sLabel);
			m_inputLabel.setToolTipText(sTipText);
			Dimension size = new Dimension(g_nLabelWidth,20);
			m_inputLabel.setMaximumSize(size);
			m_inputLabel.setMinimumSize(size);
			m_inputLabel.setPreferredSize(size);
			m_inputLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
			// RRB: temporary
			//m_inputLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			add(m_inputLabel);
		}
	}
	
	protected void addValidationLabel() {
		if (m_bAddButtons) {
			m_validateLabel = new SmallLabel("x", new Color(200,0,0));
			add(m_validateLabel);
			m_validateLabel.setVisible(true);
			checkValidation();
		}
	}
	
	/* check the input is valid, continue checking recursively */
	protected void validateAllEditors() {
		for (InputEditor editor : g_currentInputEditors) {
			editor.checkValidation();
		}
	}
	protected void checkValidation() {
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
			if (input.get() instanceof Plugin) {
				Plugin plugin = ((Plugin)input.get());
				for (Input<?> input2: plugin.listInputs()) {
					try {
						input2.validate();
					} catch (Exception e) {
						throw new Exception(((Plugin)input.get()).getID() + "</p><p> " + e.getMessage());
					}
					validateRecursively(input2, done);
				}
			}
			if (input.get() instanceof List<?>) {
				for (Object o : (List<?>)input.get()) {
					if (o != null && o instanceof Plugin) {
						Plugin plugin = (Plugin) o;
						for (Input<?> input2: plugin.listInputs()) {
							try {
								input2.validate();
							} catch (Exception e) {
								throw new Exception(((Plugin)o).getID() + " " + e.getMessage());
							}
							validateRecursively(input2, done);
						}
					}
				}
			}
		}
	} // validateRecursively
	
	@Override
	public void validate(ValidationStatus state) {
		checkValidation();
	}
	

    public void refreshPanel() {
        Component c = this;
        while (((Component) c).getParent() != null) {
        	c = ((Component) c).getParent();
        	if (c instanceof ListSelectionListener) {
        		((ListSelectionListener) c).valueChanged(null);
        	}
        }
    }

	/** synchronise values in panel with current network **/
	protected void sync() {
	    Component c = this;
	    while (((Component) c).getParent() != null) {
	      	c = ((Component) c).getParent();
	      	if (c instanceof BeautiPanel) {
	      		BeautiPanel panel = (BeautiPanel) c;
	      		BeautiPanelConfig cfgPanel = panel.config;
	      		cfgPanel.sync(panel.iPartition);
	      	}
	    }
	}
	
	@Override public void setBorder(Border border) {
        // No border
    }

    // STATIC MEMBER FUNCTIONS

    public static boolean isExpertMode() {
        return isExpertMode;
    }

    public static void setExpertMode(boolean expertMode) {
        isExpertMode = expertMode;
    }


} // class InputEditor
