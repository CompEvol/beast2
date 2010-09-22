package beast.app.draw;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

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
public abstract class InputEditor extends Box {
	final static String NO_VALUE = "<none>";
	/** list of inputs for which the input editor should be expanded inline in a dialog 
	 * in the format <className>.<inputName>, e.g. beast.core.MCMC.state  
	 */
	static Set<String> m_inlinePlugins;
	
	static {
		// load m_inlinePlugins from properties file
		Properties props = new Properties();
		try {
			String sPropFile = "beast/app/draw/" + "inputeditor.properties";
			InputStream in = InputEditor.class.getClassLoader().getResourceAsStream(sPropFile);
			props.load(in);
			String sInlinePlugins = props.getProperty("inlinePlugins");
			m_inlinePlugins = new HashSet<String>();
			for (String sInlinePlugin: sInlinePlugins.split("\\s+")) {
				m_inlinePlugins.add(sInlinePlugin);
			}
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + " " + e.getMessage());
		}
	}
	
	private static final long serialVersionUID = 1L;
	/** the input to be edited **/
	Input<?> m_input;
	/** parent plugin **/
	Plugin m_plugin;
	/** text field used for primitive input editors **/
	JTextField m_entry;

	JLabel m_inputLabel;
	/** label that shows up when validation fails **/
	SmallLabel m_validateLabel;
	
	
	public InputEditor() {
		super(BoxLayout.X_AXIS);
		setAlignmentX(LEFT_ALIGNMENT);
	} // c'tor
	
	
	/** return class the editor is suitable for **/
	abstract public Class<?> type();
	
	/** construct an editor consisting of a label and input entry **/
	public void init(Input<?> input, Plugin plugin, boolean bExpand) {
		m_input = input;
		m_plugin = plugin;

		addInputLabel();
		m_entry = new JTextField();
		m_entry.setMinimumSize(new Dimension(100,16));
		if (input.get()!= null) {
			m_entry.setText(input.get().toString());
		}
		m_entry.setToolTipText(input.getTipText());
		m_entry.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					m_input.setValue(m_entry.getText(), m_plugin);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, "Error while setting " + m_input.getName() + ": " + ex.getMessage() +
							" Leaving value at " + m_input.get());
					m_entry.setText(m_input.get() + "");
				}
				checkValidation();
			}
		});
		add(m_entry);
		addValidationLabel();
	} // init

	
	protected void addInputLabel() {
		String sName = m_input.getName();
		sName = sName.replaceAll("([a-z])([A-Z])", "$1 $2");
		sName = sName.substring(0,1).toUpperCase() + sName.substring(1);
		m_inputLabel = new JLabel(sName);
		m_inputLabel.setToolTipText(m_input.getTipText());
		Dimension size = new Dimension(150,15);
		m_inputLabel.setMaximumSize(size);
		m_inputLabel.setMinimumSize(size);
		m_inputLabel.setPreferredSize(size);
		add(m_inputLabel);
	}
	
	protected void addValidationLabel() {
		m_validateLabel = new SmallLabel("x", new Color(200,0,0));
		add(m_validateLabel);
		m_validateLabel.setVisible(true);
		checkValidation();
	}
	
	/* check the input is valid, continue checking recursively */
	protected void checkValidation() {
		try {
			m_input.validate();
			// recurse
			try {
				validateRecursively(m_input);
			} catch (Exception e) {
				m_validateLabel.setVisible(true);
				m_validateLabel.setToolTipText("<html><p>Recursive error in " + e.getMessage() + "</p></html>");
				m_validateLabel.m_circleColor = Color.orange;
				repaint();
				return;
			}
			m_validateLabel.setVisible(false);
		} catch (Exception e) {
			m_validateLabel.setToolTipText(e.getMessage());
			m_validateLabel.m_circleColor = Color.red;
			m_validateLabel.setVisible(true);
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
	
} // class InputEditor
