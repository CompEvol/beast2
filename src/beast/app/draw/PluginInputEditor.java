package beast.app.draw;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import beast.core.Input;
import beast.core.Plugin;
import beast.util.ClassDiscovery;

public class PluginInputEditor extends InputEditor {
	private static final long serialVersionUID = 1L;
	JComboBox m_selectPluginComboBox;
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
	 **/
	@Override
	public void init(Input<?> input, Plugin plugin) {
		m_input = input;
		m_plugin = plugin;

		JLabel label = new JLabel(input.getName());
		label.setToolTipText(input.getTipText());
		add(label);
		
		String [] sAvailablePlugins = getAvailablePlugins();
		if (sAvailablePlugins.length > 1) {
			m_selectPluginComboBox = new JComboBox(sAvailablePlugins);
			m_selectPluginComboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// do something
				}
			});
			add(m_selectPluginComboBox);
		}
		
		m_editPluginButton = new JButton();
		if (input.get() == null) {
			m_editPluginButton.setText("...");
		} else {
			String sName = input.get().getClass().getName();
			sName = sName.substring(sName.lastIndexOf('.') + 1);
			m_editPluginButton.setText(sName);
		}
		m_editPluginButton.setToolTipText(input.getTipText());
		
		m_editPluginButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				PluginDialog dlg = new PluginDialog(m_plugin, m_input.type());
				dlg.setVisible(true);
				if (dlg.m_bOK) {
					m_plugin = dlg.m_plugin;
				}
			}
		});
		add(m_editPluginButton);
	} // init
	
	String [] getAvailablePlugins() {
		List<String> sPlugins = ClassDiscovery.find(m_input.type(), "beast");
		return sPlugins.toArray(new String[0]);
	} // getAvailablePlugins
} // class PluginInputEditor
