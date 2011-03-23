package beast.app.draw;


import java.awt.Color;
import java.awt.Dimension;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
	public static boolean m_bExpertMode = false;
	/** list of inputs for which the input editor should be expanded inline in a dialog 
	 * in the format <className>.<inputName>, e.g. beast.core.MCMC.state  
	 */
	public static Set<String> m_inlinePlugins;
	/** list of inputs that should not be shown in a dialog. Same format as for m_inlinePlugins**/
	public static Set<String> m_suppressPlugins;
	
	static {
		// load m_inlinePlugins from properties file
		Properties props = new Properties();
		try {
			// load from default position in Beast
			String sPropFile = "beast/app/draw/" + "inputeditor.properties";
			InputStream in = InputEditor.class.getClassLoader().getResourceAsStream(sPropFile);
			System.err.println("Loading " + sPropFile);
			props.load(in);
			String sInlinePlugins = props.getProperty("inlinePlugins");
			String sSuppressPlugins = props.getProperty("suppressPlugins");
			// load extra specs for other packages
			sPropFile = "inputeditor.properties";
			in = InputEditor.class.getClassLoader().getResourceAsStream(sPropFile);
			if (in != null) {
				System.err.println("Loading " + sPropFile);
				props.load(in);
				sInlinePlugins += " " + props.getProperty("inlinePlugins");
				sSuppressPlugins += " " + props.getProperty("suppressPlugins");
			}
			System.err.println("inline="+sInlinePlugins);
			System.err.println("suppress="+sSuppressPlugins);
			m_inlinePlugins = new HashSet<String>();
			for (String sInlinePlugin: sInlinePlugins.split("\\s+")) {
				m_inlinePlugins.add(sInlinePlugin);
			}
			
			m_suppressPlugins = new HashSet<String>();
			for (String sSuppressPlugin: sSuppressPlugins.split("\\s+")) {
				m_suppressPlugins.add(sSuppressPlugin);
			}
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + " " + e.getMessage());
		}
	}
	
	private static final long serialVersionUID = 1L;
	/** the input to be edited **/
	protected Input<?> m_input;
	/** parent plugin **/
	protected Plugin m_plugin;
	/** text field used for primitive input editors **/
	JTextField m_entry;

	JLabel m_inputLabel;
	
	/** flag to indicate label, edit and validate buttons/labels should be added **/
	protected boolean m_bAddButtons = true;
	/** label that shows up when validation fails **/
	protected SmallLabel m_validateLabel;
	
	/** list of objects that want to be notified of the validation state when it changes **/
	List<ValidateListener> m_validateListeners;
	public void addValidationListener(ValidateListener validateListener) {
		if (m_validateListeners == null) {
			m_validateListeners = new ArrayList<ValidateListener>();
		}
		m_validateListeners.add(validateListener);
	}
	void notifyValidationListeners(State state) {
		if (m_validateListeners != null) {
			for (ValidateListener listener : m_validateListeners) {
				listener.validate(state);
			}
		}
	}
	
	
	public InputEditor() {
		super(BoxLayout.X_AXIS);
		setAlignmentX(LEFT_ALIGNMENT);
		//setAlignmentY(TOP_ALIGNMENT);
	} // c'tor
	
	
	/** return class the editor is suitable for **/
	abstract public Class<?> type();
	
	/** construct an editor consisting of a label and input entry **/
	public void init(Input<?> input, Plugin plugin, boolean bExpand, boolean bAddButtons) {
		m_bAddButtons = bAddButtons;
		m_input = input;
		m_plugin = plugin;

		addInputLabel();
		
		setUpEntry();
		
//		m_entry.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				processEntry();
//			}
//		});
		add(m_entry);
		addValidationLabel();
	} // init


	void setUpEntry() {
		m_entry = new JTextField();
		m_entry.setMinimumSize(new Dimension(100,16));
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
		} catch (Exception ex) {
//			JOptionPane.showMessageDialog(null, "Error while setting " + m_input.getName() + ": " + ex.getMessage() +
//					" Leaving value at " + m_input.get());
//			m_entry.setText(m_input.get() + "");
			m_validateLabel.setVisible(true);
			m_validateLabel.setToolTipText("<html><p>Parsing error: " + ex.getMessage() + ". Value was left at " + m_input.get() +".</p></html>");
			m_validateLabel.m_circleColor = Color.orange;
			repaint();
		}
	}
	
	protected void addInputLabel() {
		if (m_bAddButtons) {
			String sName = m_input.getName();
			sName = sName.replaceAll("([a-z])([A-Z])", "$1 $2");
			sName = sName.substring(0,1).toUpperCase() + sName.substring(1);
			addInputLabel(sName, m_input.getTipText());
		}
	}

	protected void addInputLabel(String sLabel, String sTipText) {
		if (m_bAddButtons) {
			m_inputLabel = new JLabel(sLabel);
			m_inputLabel.setToolTipText(sTipText);
			Dimension size = new Dimension(150,15);
			m_inputLabel.setMaximumSize(size);
			m_inputLabel.setMinimumSize(size);
			m_inputLabel.setPreferredSize(size);
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
	protected void checkValidation() {
			try {
				m_input.validate();
				// recurse
				try {
					validateRecursively(m_input);
				} catch (Exception e) {
					notifyValidationListeners(State.HAS_INVALIDMEMBERS);
					if (m_bAddButtons) {
						m_validateLabel.setVisible(true);
						m_validateLabel.setToolTipText("<html><p>Recursive error in " + e.getMessage() + "</p></html>");
						m_validateLabel.m_circleColor = Color.orange;
					}
					repaint();
					return;
				}
				if (m_bAddButtons) {
					m_validateLabel.setVisible(false);
				}
				notifyValidationListeners(State.IS_VALID);
			} catch (Exception e) {
				if (m_bAddButtons) {
					m_validateLabel.setToolTipText(e.getMessage());
					m_validateLabel.m_circleColor = Color.red;
					m_validateLabel.setVisible(true);
				}
				notifyValidationListeners(State.IS_INVALID);
			}
		repaint();
	}
	
	/* Recurse in any of the input plugins
	 * and validate its inputs */
	void validateRecursively(Input<?> input) throws Exception {
		if (input.get() != null) {
			if (input.get() instanceof Plugin) {
				for (Input<?> input2: ((Plugin)input.get()).listInputs()) {
					try {
						input2.validate();
					} catch (Exception e) {
						throw new Exception(((Plugin)input.get()).getID() + "</p><p> " + e.getMessage());
					}
					validateRecursively(input2);
				}
			}
			if (input.get() instanceof List<?>) {
				for (Object o : (List<?>)input.get()) {
					if (o != null && o instanceof Plugin) {
						for (Input<?> input2: ((Plugin)o).listInputs()) {
							try {
								input2.validate();
							} catch (Exception e) {
								throw new Exception(((Plugin)o).getID() + " " + e.getMessage());
							}
							validateRecursively(input2);
						}
					}
				}
			}
		}
	} // validateRecursively
	
	@Override
	public void validate(State state) {
System.err.println("InputEditor::validate " + m_plugin.getID());		
		checkValidation();
	}
} // class InputEditor
