package beast.app.draw;

import beast.core.Input;
import beast.core.MCMC;
import beast.core.Plugin;
import beast.util.ClassDiscovery;
import beast.util.XMLProducer;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * Dialog for editing Plugins.
 * <p/>
 * This dynamically creates a dialog consisting of
 * InputEditors associated with the inputs of a Plugin.
 * *
 */

public class PluginPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    /**
     * plug in to be edited *
     */
    public Plugin m_plugin;
    /**
     * (super) class of plug-in, this determines the super-class
     * that is allowable if the plugin class is changed.
     */
    Class<?> m_pluginClass;
    JLabel m_pluginButton;
    private boolean m_bOK = false;
    /* Set of plugins in the system.
      * These are the plugins that an input can be connected to **/
    static public Map<String, Plugin> g_plugins = null;

    static Point m_position;

    /**
     * map that identifies the InputEditor to use for a particular type of Input *
     */
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
        m_position = new Point(0, 0);
    } // finished registering input editors


    public PluginPanel(Plugin plugin, Class<?> _pluginClass, List<Plugin> plugins) {
        g_plugins = new HashMap<String, Plugin>();
        for (Plugin plugin2 : plugins) {
            String sID = getID(plugin2);
            // ensure IDs are unique
            if (g_plugins.containsKey(sID)) {
                plugin2.setID(null);
                sID = getID(plugin2);
            }
            g_plugins.put(getID(plugin2), plugin2);
        }
        init(plugin, _pluginClass);
    }

    public PluginPanel(Plugin plugin, Class<?> _pluginClass) {
        if (g_plugins == null) {
            initPlugins(plugin);
        }
        init(plugin, _pluginClass);
    }

    void init(Plugin plugin, Class<?> _pluginClass) {

        //setModal(true);
        m_plugin = plugin;
        m_pluginClass = _pluginClass;
        //setTitle(m_plugin.getID() + " Editor");

        Box mainBox = Box.createVerticalBox();
        mainBox.add(Box.createVerticalStrut(5));
        /* add plugin + help button at the top */
        Box pluginBox = createPluginBox();
        mainBox.add(pluginBox);
        mainBox.add(Box.createVerticalStrut(5));

        /* add individual inputs **/
        List<Input<?>> inputs = null;
        try {
            inputs = plugin.listInputs();
        } catch (Exception e) {
            // TODO: handle exception
        }
        for (Input<?> input : inputs) {
            try {
                if (input.getType() == null) {
                    input.determineClass(m_plugin);
                }
                Class<?> inputClass = input.getType();

                InputEditor inputEditor;
                if (g_inputEditorMap.containsKey(inputClass)) {
                    String sInputEditor = g_inputEditorMap.get(inputClass);
                    inputEditor = (InputEditor) Class.forName(sInputEditor).newInstance();

                } else if (List.class.isAssignableFrom(inputClass) ||
                        (input.get() != null && input.get() instanceof List<?>)) {
                    inputEditor = new ListInputEditor();
                } else {
                    // assume it is a general Plugin, so create a Plugin class
                    inputEditor = new PluginInputEditor();
                }
                inputEditor.init(input, m_plugin);
                inputEditor.setBorder(new EtchedBorder());
                mainBox.add(inputEditor);
                mainBox.add(Box.createVerticalStrut(5));
            } catch (Exception e) {
                // ignore
                System.err.println(e.getClass().getName() + ": " + e.getMessage() + "\n" +
                        "input " + input.getName() + " could not be added.");
            }
        }

        mainBox.add(Box.createVerticalStrut(5));

        this.add(mainBox);
        Dimension dim = mainBox.getPreferredSize();
        setSize(dim.width + 10, dim.height + 30);

        PluginPanel.m_position.x += 30;
        PluginPanel.m_position.y += 30;
        setLocation(PluginPanel.m_position);
    } // c'tor

    public boolean getOK() {
        PluginPanel.m_position.x -= 30;
        PluginPanel.m_position.y -= 30;
        return m_bOK;
    }

    /**
     * create box for manipulating the plugin, or ask for help *
     */
    Box createPluginBox() {
        Box box = Box.createHorizontalBox();
        JLabel icon = new JLabel();
        URL url = ClassLoader.getSystemResource(ModelBuilder.ICONPATH + "/beast.png");
        Icon _icon = new ImageIcon(url);
        icon.setIcon(_icon);
        box.add(icon);
        box.add(Box.createHorizontalGlue());

        JLabel label = new JLabel(m_pluginClass.getName().replaceAll(".*\\.", "") + ":");
        box.add(label);

        m_pluginButton = new JLabel(m_plugin.getID());
        m_pluginButton.setToolTipText(m_plugin.getID() + " is of type " + m_plugin.getClass().getName() + " Click to change.");
//		m_pluginButton.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				List<String> sClasses = ClassDiscovery.find(m_pluginClass, "beast"); 
//				String sClassName = (String) JOptionPane.showInputDialog(null,
//						"Select another type of " + m_pluginClass.getName().replaceAll(".*\\.", ""), 
//						"Select",
//						JOptionPane.PLAIN_MESSAGE, null,
//						sClasses.toArray(new String[0]),
//						null);
//				if (sClassName.equals(m_plugin.getClass().getName())) {
//					return;
//				}
//				try {
//					m_plugin = (Plugin) Class.forName(sClassName).newInstance();
//					m_pluginButton.setText(sClassName.replaceAll(".*\\.", ""));
//					// TODO: replace InputEditors where appropriate.
//					
//				} catch (Exception ex) {
//					JOptionPane.showMessageDialog(null, "Could not change plugin: " +
//							ex.getClass().getName() + " " +
//							ex.getMessage()
//							);
//				}
//			}
//		});
        box.add(Box.createHorizontalStrut(10));
        box.add(m_pluginButton);


        SmallButton helpButton2 = new SmallButton("?", true);
        helpButton2.setToolTipText("Show help for this plugin");
        helpButton2.addActionListener(new ActionListener() {

            // implementation ActionListener
            public void actionPerformed(ActionEvent e) {
                setCursor(new Cursor(Cursor.WAIT_CURSOR));
                HelpBrowser b = new HelpBrowser(m_plugin.getClass().getName());
                b.setSize(800, 800);
                b.setVisible(true);
                b.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
        box.add(Box.createHorizontalStrut(10));
        box.add(helpButton2);
        box.add(Box.createHorizontalGlue());

        Box vbox = Box.createVerticalBox();
        vbox.setBorder(new EtchedBorder());
        vbox.add(Box.createVerticalStrut(10));
        vbox.add(box);
        vbox.add(Box.createVerticalStrut(10));

        return vbox;
    } // createPluginBox

    static List<String> getAvailablePlugins(Input<?> input, Plugin parent, List<String> sTabuList) {
        /* add ascendants to tabu list */
        if (sTabuList == null) {
            sTabuList = new ArrayList<String>();
        }
        for (Plugin plugin : listAscendants(parent, g_plugins.values())) {
            sTabuList.add(plugin.getID());
        }
        System.err.println(sTabuList);

        /* collect all plugins in the system, that are not in the tabu list*/
        List<String> sPlugins = new ArrayList<String>();
        for (Plugin plugin : g_plugins.values()) {
            if (input.getType().isAssignableFrom(plugin.getClass())) {
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
        /* add all plugin-classes of type assignable to the input */
        for (String sClass : ClassDiscovery.find(input.getType(), "beast")) {
            sPlugins.add("new " + sClass);
        }
        return sPlugins;
    } // getAvailablePlugins

    /**
     * collect all plugins that can reach this input (actually, it's parent)
     * and add them to the tabu list.
     */
    static List<Plugin> listAscendants(Plugin parent, Collection<Plugin> plugins) {
        /* First, calculate outputs for each plugin */
        HashMap<Plugin, List<Plugin>> outputs = getOutputs(plugins);
        /* process outputs */
        List<Plugin> ascendants = new ArrayList<Plugin>();
        ascendants.add(parent);
        boolean bProgress = true;
        while (bProgress) {
            bProgress = false;
            for (int i = 0; i < ascendants.size(); i++) {
                Plugin ascendant = ascendants.get(i);
                for (Plugin parent2 : outputs.get(ascendant)) {
                    if (!ascendants.contains(parent2)) {
                        ascendants.add(parent2);
                        bProgress = true;
                    }
                }
            }
        }
        return ascendants;
    }

    /* calculate outputs for each plugin
      * and put them as ArrayLists in a Map
      * so they can be retrieved indexed by plugin like this:
      * ArrayList<Plugin> output = outputs.get(plugin)*/

    static HashMap<Plugin, List<Plugin>> getOutputs(Collection<Plugin> plugins) {
        HashMap<Plugin, List<Plugin>> outputs = new HashMap<Plugin, List<Plugin>>();
        for (Plugin plugin : plugins) {
            outputs.put(plugin, new ArrayList<Plugin>());
        }
        for (Plugin plugin : plugins) {
            try {
                for (Input<?> input2 : plugin.listInputs()) {
                    Object o = input2.get();
                    if (o != null && o instanceof Plugin) {
                        outputs.get(o).add(plugin);
                    }
                    if (o != null && o instanceof List<?>) {
                        for (Object o2 : (List<?>) o) {
                            if (o2 != null && o2 instanceof Plugin) {
                                outputs.get(o2).add(plugin);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return outputs;
    } // getOutputs

    public void initPlugins(Plugin plugin) {
        g_plugins = new HashMap<String, Plugin>();
        addPluginToMap(plugin);
    }

    static void addPluginToMap(Plugin plugin) {
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
                                addPluginToMap((Plugin) o);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
            System.err.println(e.getClass().getName() + " " + e.getMessage());
        }
    } // addPluginToMap

    /**
     * return ID of plugin, if no ID is specified, generate an appropriate ID first
     */
    static String getID(Plugin plugin) {
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

    /**
     * rudimentary test *
     */
    public static void main(String[] args) {
        PluginPanel pluginPanel = null;
        try {
            if (args.length == 0) {
                pluginPanel = new PluginPanel(new MCMC(), Runnable.class);
            } else if (args[0].equals("-x")) {
                StringBuilder text = new StringBuilder();
                String NL = System.getProperty("line.separator");
                Scanner scanner = new Scanner(new File(args[1]));
                try {
                    while (scanner.hasNextLine()) {
                        text.append(scanner.nextLine() + NL);
                    }
                }
                finally {
                    scanner.close();
                }
                Plugin plugin = new beast.util.XMLParser().parseBareFragment(text.toString(), false);
                pluginPanel = new PluginPanel(plugin, plugin.getClass());
            } else if (args.length == 1) {
                pluginPanel = new PluginPanel((Plugin) Class.forName(args[0]).newInstance(), Class.forName(args[0]));
            } else if (args.length == 2) {
                pluginPanel = new PluginPanel((Plugin) Class.forName(args[0]).newInstance(), Class.forName(args[1]));
            } else {
                throw new Exception("Incorrect number of arguments");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Usage: " + PluginPanel.class.getName() + " [-x file ] [class [type]]\n" +
                    "where [class] (optional, default MCMC) is a Plugin to edit\n" +
                    "and [type] (optional only if class is specified, default Runnable) the type of the Plugin.\n" +
                    "for example\n" +
                    "");
            System.exit(0);
        }
        pluginPanel.setVisible(true);
        if (pluginPanel.m_bOK) {
            Plugin plugin = pluginPanel.m_plugin;
            String sXML = new XMLProducer().modelToXML(plugin);
            System.out.println(sXML);
        }
    } // main
} // class PluginDialog

