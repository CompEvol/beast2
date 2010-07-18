package beast.app.draw;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
	/** plug in to be edited **/
	Plugin m_plugin;
	/** (super) class of plug-in, this determines the super-class
	 * that is allowable if the plugin class is changed.
	 */
	Class<?> m_pluginClass;
	JButton m_pluginButton; 
	public boolean m_bOK = false;
	/* Set of plugins in the system. 
	 * These are the plugins that an input can be connected to **/
	static public Map<String, Plugin> g_plugins = null; 
	//static public String [] g_sPlugInNames;
	
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
		if (g_plugins == null) {
			initPlugins(plugin);
		}
		
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
				inputEditor.setBorder(new EtchedBorder());
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
		
		JLabel label = new JLabel(m_pluginClass.getName().replaceAll(".*\\.", "")+ ":");
		box.add(label);
		
		m_pluginButton = new JButton(m_plugin.getID());
		m_pluginButton.setToolTipText(m_plugin.getID() + " is of type " + m_plugin.getClass().getName() + " Click to change.");
		m_pluginButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String sClassName = (String) JOptionPane.showInputDialog(null,
						"Select a constant", "select",
						JOptionPane.PLAIN_MESSAGE, null,
						ClassDiscovery.find("beast.core.Plugin", "beast").toArray(new String[0]),
						null);
				if (sClassName.equals(m_plugin.getClass().getName())) {
					return;
				}
				try {
					m_plugin = (Plugin) Class.forName(sClassName).newInstance();
					m_pluginButton.setText(sClassName.replaceAll(".*\\.", ""));
					// TODO: replace InputEditors where appropriate.
					
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, "Could not change plugin: " +
							ex.getClass().getName() + " " +
							ex.getMessage()
							);
				}
			}
		});
		box.add(Box.createHorizontalStrut(10));
		box.add(m_pluginButton);
		
		
		//box.add(Box.createHorizontalGlue());
		
//		JButton helpButton = new JButton("Help");
//		helpButton.setToolTipText("Show help for this plugin");
//		helpButton.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				setCursor(new Cursor(Cursor.WAIT_CURSOR));
//				HelpBrowser b = new HelpBrowser(m_plugin.getClass().getName());
//				b.setSize(800,800);
//				b.setVisible(true);
//				b.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
//			}
//		});
//		box.add(helpButton);

		SmallButton helpButton2 = new SmallButton("?", true);
		helpButton2.setToolTipText("Show help for this plugin");
		helpButton2.addActionListener(new ActionListener() {
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
		box.add(Box.createHorizontalStrut(10));
		box.add(helpButton2);
		
		Box vbox = Box.createVerticalBox();
		vbox.setBorder(new EtchedBorder());
		vbox.add(Box.createVerticalStrut(10));
		vbox.add(box);
		vbox.add(Box.createVerticalStrut(10));
		
		return vbox;
	} // createPluginBox
	
	
	/** Select existing plug-in, or create a new one.
	 * Suppress existing plug-ins with IDs from the tabu list. 
	 * Return null if nothing is selected.
	 */
	public static Plugin pluginSelector(Input<?> input, List<String> sTabuList) {
		List<String> sPlugins = new ArrayList<String>();
		Class<?> _class = input.type();
		for (Plugin plugin : g_plugins.values()) {
			if (input.type().isAssignableFrom(plugin.getClass())) {
				boolean bIsTabu = false;
				if (sTabuList != null) {
					for (String sTabu : sTabuList) {
						if (sTabu.equals(plugin.getID())) {
							bIsTabu = true;
						}
					}
				}
				if (!bIsTabu) {
					sPlugins.add(plugin.getID());
				}
			}
		}
		for(String sClass: ClassDiscovery.find(input.type(), "beast")) {
			sPlugins.add("new " + sClass);
		}
		String sClassName = null;
		if (sPlugins.size() == 1) {
			sClassName = sPlugins.get(0);
		} else {
			sClassName = (String) JOptionPane.showInputDialog(null,
				"Select a constant", "select",
				JOptionPane.PLAIN_MESSAGE, null,
				sPlugins.toArray(new String[0]),
				null);
		}
		
		if (sClassName == null) {
			return null;
		}
		if (!sClassName.startsWith("new ")) {
			return (g_plugins.get(sClassName));
		}
		try {
			Plugin plugin = (Plugin) Class.forName(sClassName.substring(4)).newInstance();
			g_plugins.put(getID(plugin), plugin);
			return plugin;
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Could not select plugin: " +
					ex.getClass().getName() + " " +
					ex.getMessage()
					);
			return null;
		}
	} // pluginSelector
	
	/** rudimentary test **/
	public static void main(String [] args) {
		PluginDialog dlg = new PluginDialog(new MCMC(), Runnable.class);
		dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		dlg.setVisible(true);
	}

	public void initPlugins(Plugin plugin) {
		g_plugins = new HashMap<String, Plugin>();
		addPluginToMap(plugin);
	}
	void addPluginToMap(Plugin plugin) {
		g_plugins.put(getID(plugin), plugin);
		try {
		for (Input<?> input : plugin.listInputs()) {
			if (input.get() != null) {
				if (input.get() instanceof Plugin) {
					addPluginToMap((Plugin) input.get());
				}
				if (input.get() instanceof List<?>) {
					for (Object o : (List<?>) input.get()) {
						if (o instanceof Plugin) {
							addPluginToMap((Plugin)o);
						}
					}
				}
			}
		}
		} catch (Exception e) {
			// ignore
			System.err.println(e.getClass().getName() + " " + e.getMessage());
		}
	}
	/** return ID of plugin, if no ID is specified, generate an appropriate ID first */
	static 
	String getID(Plugin plugin) {
		if (plugin.getID() == null || plugin.getID().length() == 0) {
			String sID = plugin.getClass().getName().replaceAll(".*\\.", "");
			int i = 0;
			while (g_plugins.containsKey(sID + i)) {
				i++;
			}
			plugin.setID(sID + i);
		}
		return plugin.getID();
	}
} // class PluginDialog

