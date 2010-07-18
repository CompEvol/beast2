package beast.app.draw;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import beast.core.Input;
import beast.core.Plugin;
import beast.util.ClassDiscovery;

public class PluginInputEditor extends InputEditor {
	private static final long serialVersionUID = 1L;
	JButton m_selectPluginBox;
	JButton m_editPluginButton;
	
	public PluginInputEditor() {
		super();
	}
	@Override
	public Class<?> type() {return Plugin.class;}

	/** construct an editor consisting of 
	 * o a label
	 * o a button for selecting another plug-in
	 * o a button for editing the plug-in 
	 * o validation label -- optional, if input is not valid
	 **/
	@Override
	public void init(Input<?> input, Plugin plugin) {
		m_input = input;
		m_plugin = plugin;

		addInputLabel();
		
		String [] sAvailablePlugins = getAvailablePlugins();
		if (sAvailablePlugins.length > 1) {
			m_selectPluginBox = new JButton();
			if (input.get() != null) {
				m_selectPluginBox.setText(((Plugin)input.get()).getID());
			} else {
				m_selectPluginBox.setText(NO_VALUE);
			}
			m_selectPluginBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Plugin plugin = PluginDialog.pluginSelector(m_input, null);
					if (plugin == m_input.get()) {
						// nothing changed
						return;
					}
					if (plugin == null) {
						// selection cancelled
						return;
					}
					
					try {
						m_input.setValue(plugin, m_plugin);
						m_selectPluginBox.setText(plugin.getID());
						m_editPluginButton.setEnabled(true);
						checkValidation();
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(null, "Could not change plugin: " +
								ex.getClass().getName() + " " +
								ex.getMessage()
								);
					}
				}
			});
			add(m_selectPluginBox);
		}
		
		m_editPluginButton = new JButton();
		if (input.get() == null) {
			m_editPluginButton.setText("...");
			m_editPluginButton.setEnabled(false);
		} else {
			m_editPluginButton.setText(((Plugin)input.get()).getID());
		}
		m_editPluginButton.setToolTipText(input.getTipText());
		
		m_editPluginButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				PluginDialog dlg = new PluginDialog((Plugin) m_input.get(), m_input.type());
				dlg.setVisible(true);
				if (dlg.m_bOK) {
					m_plugin = dlg.m_plugin;
				}
			}
		});
		add(m_editPluginButton);

		addValidationLabel();
	} // init
	
	String [] getAvailablePlugins() {
		List<String> sPlugins = ClassDiscovery.find(m_input.type(), "beast");
		return sPlugins.toArray(new String[0]);
	} // getAvailablePlugins
} // class PluginInputEditor
