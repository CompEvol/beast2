package beast.app.draw;

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
	private static final long serialVersionUID = 1L;
	/** the input to be edited **/
	Input<?> m_input;
	/** parent plugin **/
	Plugin m_plugin;
	/** text field used for primitive input editors **/
	JTextField m_entry;
	
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

		JLabel label = new JLabel(input.getName());
		label.setToolTipText(input.getTipText());
		add(label);
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
	} // init

} // class InputEditor
