package beast.app.draw;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import beast.core.Input;
import beast.core.Plugin;

public abstract class InputEditor extends Box {
	final static String NO_VALUE = "<none>";
	private static final long serialVersionUID = 1L;
	/** the input to be edited **/
	Input<?> m_input;
	/** parent plugin **/
	Plugin m_plugin;
	/** text field used for primitive input editors **/
	JTextField m_entry;

	/** label that shows up when validation fails **/
	SmallLabel m_validateLabel;
	
	
	public InputEditor() {
		super(BoxLayout.X_AXIS);
		setAlignmentX(LEFT_ALIGNMENT);
	} // c'tor
	
	
	/** return class the editor is suitable for **/
	abstract public Class<?> type();
	
	/** construct an editor consisting of a label and input entry **/
	public void init(Input<?> input, Plugin plugin) {
		m_input = input;
		m_plugin = plugin;

		addInputLabel();
		m_entry = new JTextField();
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
					JOptionPane.showMessageDialog(null, "Error while setting " + m_input.getName() + ": " + ex.getMessage());
				}
			}
		});
		add(m_entry);
		addValidationLabel();
	} // init

	
	protected void addInputLabel() {
		JLabel label = new JLabel(m_input.getName());
		label.setToolTipText(m_input.getTipText());
		Dimension size = new Dimension(150,15);
		label.setMaximumSize(size);
		label.setMinimumSize(size);
		label.setPreferredSize(size);
		add(label);
	}
	
	protected void addValidationLabel() {
		m_validateLabel = new SmallLabel("x", new Color(200,0,0));
		try {
			m_input.validate();
			m_validateLabel.setVisible(false);
		} catch (Exception e) {
			m_validateLabel.setToolTipText(e.getMessage());
			m_validateLabel.setVisible(true);
		}
		add(m_validateLabel);
	}
	protected void checkValidation() {
		try {
			m_input.validate();
			m_validateLabel.setVisible(false);
		} catch (Exception e) {
			m_validateLabel.setToolTipText(e.getMessage());
			m_validateLabel.setVisible(true);
		}
	}
	
} // class InputEditor
