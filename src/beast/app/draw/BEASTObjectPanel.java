package beast.app.draw;




import javax.swing.*;

import beast.app.beauti.BeautiDoc;
import beast.core.Input;
import beast.core.MCMC;
import beast.core.BEASTInterface;
import beast.evolution.alignment.Taxon;
import beast.util.XMLProducer;


import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Panel for editing Plugins.
 * <p/>
 * This dynamically creates a Panel consisting of
 * InputEditors associated with the inputs of a Plugin.
 * *
 */

public class BEASTObjectPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    /**
     * plug in to be edited *
     */
    public BEASTInterface m_plugin;
    /**
     * (super) class of plug-in, this determines the super-class
     * that is allowable if the plugin class is changed.
     */
    Class<?> m_pluginClass;
    JLabel m_pluginButton;
    JTextField m_identry;

    private boolean m_bOK = false;
    /* Set of plugins in the system.
      * These are the plugins that an input can be connected to **/
    static public HashMap<String, BEASTInterface> g_plugins = null;
    //    static public Set<Operator> g_operators = null;
//    static public Set<StateNode> g_stateNodes = null;
//    static public Set<Loggable> g_loggers = null;
//    static public Set<Distribution> g_distributions = null;
    static public Set<Taxon> g_taxa = null;

    public static Point m_position;

    /**
     * map that identifies the InputEditor to use for a particular type of Input *
     */
//    static HashMap<Class<?>, String> g_inputEditorMap;
//    static HashMap<Class<?>, String> g_listInputEditorMap;


    static {
        init();
    } // finished registering input editors

    public static void init() {
//        // register input editors
//        g_inputEditorMap = new HashMap<Class<?>, String>();
//        g_listInputEditorMap = new HashMap<Class<?>, String>();
//
////        String [] sKnownEditors = new String [] {"beast.app.draw.DataInputEditor","beast.app.beauti.AlignmentListInputEditor", "beast.app.beauti.FrequenciesInputEditor", "beast.app.beauti.OperatorListInputEditor", "beast.app.beauti.ParametricDistributionInputEditor", "beast.app.beauti.PriorListInputEditor", "beast.app.beauti.SiteModelInputEditor", "beast.app.beauti.TaxonSetInputEditor", "beast.app.beauti.TipDatesInputEditor", "beast.app.draw.BooleanInputEditor", "beast.app.draw.DoubleInputEditor", "beast.app.draw.EnumInputEditor", "beast.app.draw.IntegerInputEditor", "beast.app.draw.ListInputEditor", 
////        		"beast.app.draw.ParameterInputEditor", "beast.app.draw.PluginInputEditor", "beast.app.draw.StringInputEditor"};
////        registerInputEditors(sKnownEditors);
//        String[] PACKAGE_DIRS = {"beast.app",};
//        for (String sPackage : PACKAGE_DIRS) {
//            List<String> sInputEditors = AddOnManager.find("beast.app.draw.InputEditor", sPackage);
//            registerInputEditors(sInputEditors.toArray(new String[0]));
//        }

        m_position = new Point(0, 0);
        g_plugins = new HashMap<String, BEASTInterface>();
//        g_operators = new HashSet<Operator>();
//        g_stateNodes = new HashSet<StateNode>();
//        g_loggers = new HashSet<Loggable>();
//        g_distributions = new HashSet<Distribution>();
        g_taxa = new HashSet<Taxon>();
    }


    public BEASTObjectPanel(BEASTInterface plugin, Class<?> _pluginClass, List<BEASTInterface> plugins, BeautiDoc doc) {
        //g_plugins = new HashMap<String, Plugin>();
        for (BEASTInterface plugin2 : plugins) {
            String sID = getID(plugin2);
            // ensure IDs are unique
            if (g_plugins.containsKey(sID)) {
                plugin2.setID(null);
                sID = getID(plugin2);
            }
            registerPlugin(getID(plugin2), plugin2, doc);
        }
        init(plugin, _pluginClass, true, doc);
    }

    /**
     * add plugin to plugin map and update related maps
     *
     * @return true if it was already registered *
     */
    static public boolean registerPlugin(String sID, BEASTInterface plugin, BeautiDoc doc) {
        if (doc != null) {
            doc.registerPlugin(plugin);
        }
//    	if (plugin instanceof Operator) {
//    		g_operators.add((Operator)plugin);
//    	}
//    	if (plugin instanceof StateNode) {
//    		g_stateNodes.add((StateNode)plugin);
//    	}
//    	if (plugin instanceof Loggable) {
//    		g_loggers.add((Loggable)plugin);
//    	}
//    	if (plugin instanceof Distribution) {
//    		g_distributions.add((Distribution)plugin);
//    	}
        if (plugin instanceof Taxon) {
            g_taxa.add((Taxon) plugin);
        }
        if (g_plugins.containsKey(sID) && g_plugins.get(sID) == plugin) {
            return true;
        }
        g_plugins.put(sID, plugin);
        return false;
    }

    public static void renamePluginID(BEASTInterface plugin, String sOldID, String sID, BeautiDoc doc) {
        if (doc != null) {
            doc.unregisterPlugin(plugin);
        }
        g_plugins.remove(sOldID);
//		g_operators.remove(sOldID);
//		g_stateNodes.remove(sOldID);
//		g_loggers.remove(sOldID);
//		g_distributions.remove(sOldID);
        g_taxa.remove(sOldID);
        registerPlugin(sID, plugin, doc);
    }

    public BEASTObjectPanel(BEASTInterface plugin, Class<?> _pluginClass, BeautiDoc doc) {
        this(plugin, _pluginClass, true, doc);
    }

    public BEASTObjectPanel(BEASTInterface plugin, Class<?> _pluginClass, boolean bShowHeader, BeautiDoc doc) {
        initPlugins(plugin, doc);
        init(plugin, _pluginClass, bShowHeader, doc);
    }

    void init(BEASTInterface plugin, Class<?> _pluginClass, boolean showHeader, BeautiDoc doc) {
        try {
            m_plugin = plugin.getClass().newInstance();
            for (Input<?> input : plugin.listInputs()) {
                m_plugin.setInputValue(input.getName(), input.get());
            }
            m_plugin.setID(plugin.getID());
        } catch (Exception e) {
            e.printStackTrace();
        }


        //setModal(true);
        //m_plugin = plugin;
        m_pluginClass = _pluginClass;
        //setTitle(m_plugin.getID() + " Editor");

        Box mainBox = Box.createVerticalBox();
        mainBox.add(Box.createVerticalStrut(5));

        if (showHeader) {
            /* add plugin + help button at the top */
            Box pluginBox = createPluginBox();
            mainBox.add(pluginBox);
            mainBox.add(Box.createVerticalStrut(5));
            if (doc != null && m_identry != null) {
            	// we are in Beauti, do not edit IDs
            	m_identry.setEnabled(false);
            }
        }

        doc.getInputEditorFactory().addInputs(mainBox, m_plugin, null, null, doc);


        mainBox.add(Box.createVerticalStrut(5));

        this.add(mainBox);
        Dimension dim = mainBox.getPreferredSize();
        setSize(dim.width + 10, dim.height + 30);

        BEASTObjectPanel.m_position.x += 30;
        BEASTObjectPanel.m_position.y += 30;
        setLocation(BEASTObjectPanel.m_position);
    } // c'tor

    public boolean getOK() {
        BEASTObjectPanel.m_position.x -= 30;
        BEASTObjectPanel.m_position.y -= 30;
        return m_bOK;
    }

    /**
     * add all inputs of a plugin to a box *
     */
    public static int countInputs(BEASTInterface plugin, BeautiDoc doc) {
        int nInputs = 0;
        try {
            List<Input<?>> inputs = plugin.listInputs();
            for (Input<?> input : inputs) {
                String sFullInputName = plugin.getClass().getName() + "." + input.getName();
                if (!doc.beautiConfig.suppressPlugins.contains(sFullInputName)) {
                    nInputs++;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return nInputs;
    } // addInputs



    /**
     * create box for manipulating the plugin, or ask for help *
     */
    Box createPluginBox() {
        Box box = Box.createHorizontalBox();
        JLabel icon = new JLabel();
        box.add(Box.createHorizontalGlue());

        JLabel label = new JLabel(m_pluginClass.getName().replaceAll(".*\\.", "") + ":");
        box.add(label);

//        m_pluginButton = new JLabel(m_plugin.getID());
//        m_pluginButton.setToolTipText(m_plugin.getID() + " is of type " + m_plugin.getClass().getName() + " Click to change.");
        label.setToolTipText(m_plugin.getID() + " is of type " + m_plugin.getClass().getName() + " Click to change.");

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
//        box.add(Box.createHorizontalStrut(10));
//        box.add(m_pluginButton);


        box.add(new JLabel(" " + m_plugin.getID()));
        
//        m_identry = new JTextField();
//        m_identry.setText(m_plugin.getID());
//        m_identry.setToolTipText("Name/ID that uniquely identifies this item");
//
//        m_identry.getDocument().addDocumentListener(new DocumentListener() {
//            @Override
//            public void removeUpdate(DocumentEvent e) {
//                processID();
//            }
//
//            @Override
//            public void insertUpdate(DocumentEvent e) {
//                processID();
//            }
//
//            @Override
//            public void changedUpdate(DocumentEvent e) {
//                processID();
//            }
//        });
//        box.add(m_identry);


        Box vbox = Box.createVerticalBox();
        vbox.setBorder(BorderFactory.createEmptyBorder());
        vbox.add(Box.createVerticalStrut(10));
        vbox.add(box);
        vbox.add(Box.createVerticalStrut(10));

        return vbox;
    } // createPluginBox

    void processID() {
//		PluginPanel.g_plugins.remove(m_plugin.getID());
//		m_plugin.setID(m_identry.getText());
//		PluginPanel.g_plugins.put(m_plugin.getID(), m_plugin);
    }



    /**
     * collect all plugins that can reach this input (actually, it's parent)
     * and add them to the tabu list.
     */
    static List<BEASTInterface> listAscendants(BEASTInterface parent, Collection<BEASTInterface> plugins) {
        /* First, calculate outputs for each plugin */
        HashMap<BEASTInterface, List<BEASTInterface>> outputs = getOutputs(plugins);
        /* process outputs */
        List<BEASTInterface> ascendants = new ArrayList<BEASTInterface>();
        ascendants.add(parent);
        boolean bProgress = true;
        while (bProgress) {
            bProgress = false;
            for (int i = 0; i < ascendants.size(); i++) {
                BEASTInterface ascendant = ascendants.get(i);
                if (outputs.containsKey(ascendant)) {
                    for (BEASTInterface parent2 : outputs.get(ascendant)) {
                        if (!ascendants.contains(parent2)) {
                            ascendants.add(parent2);
                            bProgress = true;
                        }
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

    static HashMap<BEASTInterface, List<BEASTInterface>> getOutputs(Collection<BEASTInterface> plugins) {
        HashMap<BEASTInterface, List<BEASTInterface>> outputs = new HashMap<BEASTInterface, List<BEASTInterface>>();
        for (BEASTInterface plugin : plugins) {
            outputs.put(plugin, new ArrayList<BEASTInterface>());
        }
        for (BEASTInterface plugin : plugins) {
            try {
                for (Input<?> input2 : plugin.listInputs()) {
                    Object o = input2.get();
                    if (o != null && o instanceof BEASTInterface) {
                        List<BEASTInterface> list = outputs.get(o);
//                    	if (list == null) {
//                    		int h = 3;
//                    		h++;
//                    	} else {
                        list.add(plugin);
//                    	}
                    }
                    if (o != null && o instanceof List<?>) {
                        for (Object o2 : (List<?>) o) {
                            if (o2 != null && o2 instanceof BEASTInterface) {
                                List<BEASTInterface> list = outputs.get(o2);
                                if (list == null) {
                                    int h = 3;
                                    h++;
                                } else {
                                    list.add(plugin);
                                }
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

    public void initPlugins(BEASTInterface plugin, BeautiDoc doc) {
        //g_plugins = new HashMap<String, Plugin>();
        addPluginToMap(plugin, doc);
    }

    static public void addPluginToMap(BEASTInterface plugin, BeautiDoc doc) {
        if (registerPlugin(getID(plugin), plugin, doc)) {
            return;
        }
        try {
            for (Input<?> input : plugin.listInputs()) {
                if (input.get() != null) {
                    if (input.get() instanceof BEASTInterface) {
                        addPluginToMap((BEASTInterface) input.get(), doc);
                    }
                    if (input.get() instanceof List<?>) {
                        for (Object o : (List<?>) input.get()) {
                            if (o instanceof BEASTInterface) {
                                addPluginToMap((BEASTInterface) o, doc);
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
    public static String getID(BEASTInterface plugin) {
        if (plugin.getID() == null || plugin.getID().length() == 0) {
            String sID = plugin.getClass().getName().replaceAll(".*\\.", "");
            int i = 0;
            while (g_plugins.containsKey(sID + i)) {
                i++;
            }
            plugin.setID(sID + "." + i);
        }
        return plugin.getID();
    }

    /**
     * rudimentary test *
     */
    public static void main(String[] args) {
        init();
        BEASTObjectPanel pluginPanel = null;
        try {
            if (args.length == 0) {
                pluginPanel = new BEASTObjectPanel(new MCMC(), Runnable.class, null);
            } else if (args[0].equals("-x")) {
                StringBuilder text = new StringBuilder();
                String NL = System.getProperty("line.separator");
                Scanner scanner = new Scanner(new File(args[1]));
                try {
                    while (scanner.hasNextLine()) {
                        text.append(scanner.nextLine() + NL);
                    }
                } finally {
                    scanner.close();
                }
                BEASTInterface plugin = new beast.util.XMLParser().parseBareFragment(text.toString(), false);
                pluginPanel = new BEASTObjectPanel(plugin, plugin.getClass(), null);
            } else if (args.length == 1) {
                pluginPanel = new BEASTObjectPanel((BEASTInterface) Class.forName(args[0]).newInstance(), Class.forName(args[0]), null);
            } else if (args.length == 2) {
                pluginPanel = new BEASTObjectPanel((BEASTInterface) Class.forName(args[0]).newInstance(), Class.forName(args[1]), null);
            } else {
                throw new Exception("Incorrect number of arguments");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Usage: " + BEASTObjectPanel.class.getName() + " [-x file ] [class [type]]\n" +
                    "where [class] (optional, default MCMC) is a Plugin to edit\n" +
                    "and [type] (optional only if class is specified, default Runnable) the type of the Plugin.\n" +
                    "for example\n" +
                    "");
            System.exit(0);
        }
        pluginPanel.setVisible(true);
        if (pluginPanel.m_bOK) {
            BEASTInterface plugin = pluginPanel.m_plugin;
            String sXML = new XMLProducer().modelToXML(plugin);
            System.out.println(sXML);
        }
    } // main

} // class PluginDialog

