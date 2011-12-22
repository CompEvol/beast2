package beast.app.draw;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;

import beast.core.Input;
import beast.core.Plugin;

public class BooleanInputEditor extends InputEditor {
	private static final long serialVersionUID = 1L;
	JCheckBox m_entry;
	

	@Override
	public Class<?> type() {return Boolean.class;}
	
	/** create input editor containing a check box **/
	@Override
	public void init(Input<?> input, Plugin plugin, ExpandOption bExpandOption, boolean bAddButtons) {
		m_bAddButtons = bAddButtons;
		m_plugin = plugin;
		m_input = input;
		m_entry = new JCheckBox(doc.beautiConfig.getInputLabel(m_plugin, input.getName()));
		if (input.get() != null) {
			m_entry.setSelected((Boolean)input.get());
		}
		m_entry.setToolTipText(input.getTipText());
		m_entry.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					m_input.setValue(m_entry.isSelected(), m_plugin);
				} catch (Exception ex) {
					System.err.println("BooleanInputEditor " + ex.getMessage());
				}
			}
		});
		add(m_entry);
		add(Box.createHorizontalGlue());
	} // c'tor
	
} // class BooleanInputEditor
