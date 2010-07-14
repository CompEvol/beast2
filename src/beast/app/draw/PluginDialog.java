package beast.app.draw;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.EtchedBorder;

import beast.core.Input;
import beast.core.MCMC;
import beast.core.Plugin;
import beast.util.ClassDiscovery;

/** Dialog for editing Plugins.
 * 
 * This dynamically creates a dialog consisting of
 * InputEditors associated with the inputs of a Plugin.
 * **/

public class PluginDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	/** plugin to be edited **/
	Plugin m_plugin;
	/** (super) class of plug-in, this determines the super-class
	 * that is allowable if the plugin class is changed.
	 */
	Class<?> m_pluginClass;
	
	public boolean m_bOK = false;
	
	/** map that identifies the InputEditor to use for a particular type of Input **/ 
	static HashMap<Class<?>, String> g_inputEditorMap;
	static {
		// register input editors
		g_inputEditorMap = new HashMap<Class<?>, String>();
		List<String> sInputEditors = ClassDiscovery.find("beast.app.draw.InputEditor", "beast.app.draw");
		for (String sInputEditor : sInputEditors) {
			try {
				Class<?> _class = Class.forName(sInputEditor);
				InputEditor editor = (InputEditor) _class.newInstance();
				Class<?> type = editor.type();
				g_inputEditorMap.put(type, sInputEditor);
			} catch (Exception e) {
				// ignore
				System.err.println(e.getClass().getName() + ": " + e.getMessage());
			}
		}
	} // finished registering input editors
		
	
	public PluginDialog(Plugin plugin, Class<?> _pluginClass) {
		setTitle("Generic Input Editor");
		setModal(true);
		m_plugin = plugin;
		m_pluginClass = _pluginClass;
		Box mainBox = Box.createVerticalBox();
		mainBox.add(Box.createVerticalStrut(5));
		Box pluginBox = createPluginBox();
		//pluginBox.setAlignmentX(LEFT_ALIGNMENT);
		mainBox.add(pluginBox);
		mainBox.add(Box.createVerticalStrut(5));
		
		try {
			Input<?> [] inputs = plugin.listInputs();
			for (Input<?> input : inputs) {
		        if (input.type() == null) {
					input.determineClass(m_plugin);
		        }
				Class<?> inputClass = input.type();
				
				InputEditor inputEditor;
				if (g_inputEditorMap.containsKey(inputClass)) {
					String sInputEditor = g_inputEditorMap.get(inputClass);
					inputEditor = (InputEditor) Class.forName(sInputEditor).newInstance();
					
				} else if (List.class.isAssignableFrom(inputClass) ||
						(input.get()!= null && input.get() instanceof List<?>)) {
					inputEditor = new ListInputEditor();
				} else {
					// assume it is a general Plugin, so create a Plugin class
					inputEditor = new PluginInputEditor();
				}
				inputEditor.init(input, m_plugin);
				mainBox.add(inputEditor);
				mainBox.add(Box.createVerticalStrut(5));
			}
		} catch (Exception e) {
			// ignore
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
		mainBox.add(Box.createVerticalStrut(5));

		Box cancelOkBox = Box.createHorizontalBox();
		cancelOkBox.setBorder(new EtchedBorder());
		JButton okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_bOK = true;
				dispose();
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_bOK = false;
				dispose();
			}
		});
		cancelOkBox.add(Box.createHorizontalGlue());
		cancelOkBox.add(okButton);
		cancelOkBox.add(Box.createHorizontalGlue());
		cancelOkBox.add(cancelButton);
		cancelOkBox.add(Box.createHorizontalGlue());
		
		mainBox.add(cancelOkBox);
		this.add(mainBox);
		Dimension dim = mainBox.getPreferredSize(); 
		setSize(dim.width + 10, dim.height + 30);
	} // c'tor

	/** create box for manipulating the plugin, or ask for help **/
	Box createPluginBox() {
		Box box = Box.createHorizontalBox();
		JLabel icon = new JLabel();
		URL url = ClassLoader.getSystemResource(ModelBuilder.ICONPATH + "/beast.png");
		Icon _icon = new ImageIcon(url);
		icon.setIcon(_icon);
		box.add(icon);
		
		String sName = m_pluginClass.getName();
		sName = sName.substring(Math.max(sName.lastIndexOf('.')+1, 0));
		JLabel label = new JLabel(sName);
		box.add(label);
		
		sName = m_plugin.getClass().getName();
		sName = sName.substring(Math.max(sName.lastIndexOf('.')+1, 0));
		JButton pluginButton = new JButton(sName);
		pluginButton.setToolTipText("Select another plugin");
		pluginButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				PluginDialog dlg = new PluginDialog(m_plugin, m_pluginClass);
				dlg.setVisible(true);
				if (dlg.m_bOK) {
					m_plugin = dlg.m_plugin;
				}
			}
		});
		box.add(pluginButton);
		
		
		box.add(Box.createHorizontalGlue());
		
		JButton helpButton = new JButton("Help");
		helpButton.setToolTipText("Show help for this plugin");
		helpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setCursor(new Cursor(Cursor.WAIT_CURSOR));
				HelpBrowser b = new HelpBrowser(m_plugin.getClass().getName());
				b.setSize(800,800);
				b.setVisible(true);
				b.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		});
		box.add(helpButton);

		Box vbox = Box.createVerticalBox();
		vbox.setBorder(new EtchedBorder());
		vbox.add(Box.createVerticalStrut(10));
		vbox.add(box);
		vbox.add(Box.createVerticalStrut(10));
		
		return vbox;
	} // createPluginBox
	
	
	/** rudimentary test **/
	public static void main(String [] args) {
		PluginDialog dlg = new PluginDialog(new MCMC(), Runnable.class);
		dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		dlg.setVisible(true);
	}
	
} // class PluginDialog

