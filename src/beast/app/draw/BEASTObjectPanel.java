package beast.app.draw;





import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import beast.app.beauti.BeautiDoc;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.MCMC;
import beast.core.util.Log;
import beast.util.BEASTClassLoader;
import beast.util.XMLProducer;

/**
 * Panel for editing BEASTObjects.
 * <p/>
 * This dynamically creates a Panel consisting of
 * InputEditors associated with the inputs of a BEASTObject.
 * *
 */

public class BEASTObjectPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    /**
     * plug in to be edited *
     */
    public BEASTInterface m_beastObject;
    /**
     * (super) class of plug-in, this determines the super-class
     * that is allowable if the beastObject class is changed.
     */
    Class<?> m_beastObjectClass;
    JLabel m_beastObjectButton;
    JTextField m_identry;

    private boolean m_bOK = false;
    /* Set of beastObjects in the system.
      * These are the beastObjects that an input can be connected to **/
    static public HashMap<String, BEASTInterface> g_plugins = null;
    //    static public Set<Operator> g_operators = null;
//    static public Set<StateNode> g_stateNodes = null;
//    static public Set<Loggable> g_loggers = null;
//    static public Set<Distribution> g_distributions = null;

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
//        g_inputEditorMap = new HashMap<>, String>();
//        g_listInputEditorMap = new HashMap<>, String>();
//
////        String [] knownEditors = new String [] {"beast.app.draw.DataInputEditor","beast.app.beauti.AlignmentListInputEditor", "beast.app.beauti.FrequenciesInputEditor", "beast.app.beauti.OperatorListInputEditor", "beast.app.beauti.ParametricDistributionInputEditor", "beast.app.beauti.PriorListInputEditor", "beast.app.beauti.SiteModelInputEditor", "beast.app.beauti.TaxonSetInputEditor", "beast.app.beauti.TipDatesInputEditor", "beast.app.draw.BooleanInputEditor", "beast.app.draw.DoubleInputEditor", "beast.app.draw.EnumInputEditor", "beast.app.draw.IntegerInputEditor", "beast.app.draw.ListInputEditor", 
////        		"beast.app.draw.ParameterInputEditor", "beast.app.draw.PluginInputEditor", "beast.app.draw.StringInputEditor"};
////        registerInputEditors(knownEditors);
//        String[] PACKAGE_DIRS = {"beast.app",};
//        for (String packageName : PACKAGE_DIRS) {
//            List<String> inputEditors = AddOnManager.find("beast.app.draw.InputEditor", packageName);
//            registerInputEditors(inputEditors.toArray(new String[0]));
//        }

        m_position = new Point(0, 0);
        g_plugins = new HashMap<>();
//        g_operators = new HashSet<>();
//        g_stateNodes = new HashSet<>();
//        g_loggers = new HashSet<>();
//        g_distributions = new HashSet<>();
    }


    public BEASTObjectPanel(BEASTInterface beastObject, Class<?> _pluginClass, List<BEASTInterface> beastObjects, BeautiDoc doc) {
        //g_plugins = new HashMap<>();
        for (BEASTInterface beastObject2 : beastObjects) {
            String id = getID(beastObject2);
            // ensure IDs are unique
            if (g_plugins.containsKey(id)) {
                beastObject2.setID(null);
                id = getID(beastObject2);
            }
            registerPlugin(getID(beastObject2), beastObject2, doc);
        }
        init(beastObject, _pluginClass, true, doc);
    }

    /**
     * add beastObject to beastObject map and update related maps
     *
     * @return true if it was already registered *
     */
    static public boolean registerPlugin(String id, BEASTInterface beastObject, BeautiDoc doc) {
        if (doc != null) {
            doc.registerPlugin(beastObject);
        }
//    	if (beastObject instanceof Operator) {
//    		g_operators.add((Operator)beastObject);
//    	}
//    	if (beastObject instanceof StateNode) {
//    		g_stateNodes.add((StateNode)beastObject);
//    	}
//    	if (beastObject instanceof Loggable) {
//    		g_loggers.add((Loggable)beastObject);
//    	}
//    	if (beastObject instanceof Distribution) {
//    		g_distributions.add((Distribution)beastObject);
//    	}
        if (g_plugins.containsKey(id) && g_plugins.get(id) == beastObject) {
            return true;
        }
        g_plugins.put(id, beastObject);
        return false;
    }

    public static void renamePluginID(BEASTInterface beastObject, String oldID, String id, BeautiDoc doc) {
        if (doc != null) {
            doc.unregisterPlugin(beastObject);
        }
        g_plugins.remove(oldID);
//		g_operators.remove(oldID);
//		g_stateNodes.remove(oldID);
//		g_loggers.remove(oldID);
//		g_distributions.remove(oldID);
        registerPlugin(id, beastObject, doc);
    }

    public BEASTObjectPanel(BEASTInterface beastObject, Class<?> _pluginClass, BeautiDoc doc) {
        this(beastObject, _pluginClass, true, doc);
    }

    public BEASTObjectPanel(BEASTInterface beastObject, Class<?> _pluginClass, boolean isShowHeader, BeautiDoc doc) {
        initPlugins(beastObject, doc);
        init(beastObject, _pluginClass, isShowHeader, doc);
    }

    void init(BEASTInterface beastObject, Class<?> _pluginClass, boolean showHeader, BeautiDoc doc) {
        try {
            m_beastObject = beastObject.getClass().newInstance();
            for (Input<?> input : beastObject.listInputs()) {
                m_beastObject.setInputValue(input.getName(), input.get());
            }
            m_beastObject.setID(beastObject.getID());
        } catch (Exception e) {
            e.printStackTrace();
        }


        //setModal(true);
        //m_beastObject = beastObject;
        m_beastObjectClass = _pluginClass;
        //setTitle(m_beastObject.getID() + " Editor");

        Box mainBox = Box.createVerticalBox();
        mainBox.add(Box.createVerticalStrut(5));

        if (showHeader) {
            /* add beastObject + help button at the top */
            Box pluginBox = createPluginBox();
            mainBox.add(pluginBox);
            mainBox.add(Box.createVerticalStrut(5));
            if (doc != null && m_identry != null) {
            	// we are in Beauti, do not edit IDs
            	m_identry.setEnabled(false);
            }
        }

        doc.getInputEditorFactory().addInputs(mainBox, m_beastObject, null, null, doc);


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
     * add all inputs of a beastObject to a box *
     */
    public static int countInputs(BEASTInterface beastObject, BeautiDoc doc) {
        int inputCount = 0;
        try {
        	if (beastObject == null) {
        		return 0;
        	}
            List<Input<?>> inputs = beastObject.listInputs();
            for (Input<?> input : inputs) {
                String fullInputName = beastObject.getClass().getName() + "." + input.getName();
                if (!doc.beautiConfig.suppressBEASTObjects.contains(fullInputName)) {
                    inputCount++;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return inputCount;
    } // addInputs



    /**
     * create box for manipulating the beastObject, or ask for help *
     */
    Box createPluginBox() {
        Box box = Box.createHorizontalBox();
        //jLabel icon = new JLabel();
        box.add(Box.createHorizontalGlue());

        JLabel label = new JLabel(m_beastObjectClass.getName().replaceAll(".*\\.", "") + ":");
        box.add(label);

//        m_pluginButton = new JLabel(m_beastObject.getID());
//        m_pluginButton.setToolTipText(m_beastObject.getID() + " is of type " + m_beastObject.getClass().getName() + " Click to change.");
        label.setToolTipText(m_beastObject.getID() + " is of type " + m_beastObject.getClass().getName() + " Click to change.");

//		m_pluginButton.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				List<String> classes = ClassDiscovery.find(m_pluginClass, "beast"); 
//				String className = (String) JOptionPane.showInputDialog(null,
//						"Select another type of " + m_pluginClass.getName().replaceAll(".*\\.", ""), 
//						"Select",
//						JOptionPane.PLAIN_MESSAGE, null,
//						classes.toArray(new String[0]),
//						null);
//				if (className.equals(m_beastObject.getClass().getName())) {
//					return;
//				}
//				try {
//					m_beastObject = (BEASTObject) BEASTClassLoader.forName(className).newInstance();
//					m_pluginButton.setText(className.replaceAll(".*\\.", ""));
//					// TODO: replace InputEditors where appropriate.
//					
//				} catch (Exception ex) {
//					JOptionPane.showMessageDialog(null, "Could not change beastObject: " +
//							ex.getClass().getName() + " " +
//							ex.getMessage()
//							);
//				}
//			}
//		});
//        box.add(Box.createHorizontalStrut(10));
//        box.add(m_pluginButton);


        box.add(new JLabel(" " + m_beastObject.getID()));
        
//        m_identry = new JTextField();
//        m_identry.setText(m_beastObject.getID());
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
//		PluginPanel.g_plugins.remove(m_beastObject.getID());
//		m_beastObject.setID(m_identry.getText());
//		PluginPanel.g_plugins.put(m_beastObject.getID(), m_beastObject);
    }



    /**
     * collect all beastObjects that can reach this input (actually, it's parent)
     * and add them to the tabu list.
     */
    static List<BEASTInterface> listAscendants(BEASTInterface parent, Collection<BEASTInterface> beastObjects) {
        /* First, calculate outputs for each beastObject */
        HashMap<BEASTInterface, List<BEASTInterface>> outputs = getOutputs(beastObjects);
        /* process outputs */
        List<BEASTInterface> ascendants = new ArrayList<>();
        ascendants.add(parent);
        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < ascendants.size(); i++) {
                BEASTInterface ascendant = ascendants.get(i);
                if (outputs.containsKey(ascendant)) {
                    for (BEASTInterface parent2 : outputs.get(ascendant)) {
                        if (!ascendants.contains(parent2)) {
                            ascendants.add(parent2);
                            progress = true;
                        }
                    }
                }
            }
        }
        return ascendants;
    }

    /* calculate outputs for each beastObject
      * and put them as ArrayLists in a Map
      * so they can be retrieved indexed by beastObject like this:
      * ArrayList<BEASTObject> output = outputs.get(beastObject)*/

    static HashMap<BEASTInterface, List<BEASTInterface>> getOutputs(Collection<BEASTInterface> beastObjects) {
        HashMap<BEASTInterface, List<BEASTInterface>> outputs = new HashMap<>();
        for (BEASTInterface beastObject : beastObjects) {
            outputs.put(beastObject, new ArrayList<>());
        }
        for (BEASTInterface beastObject : beastObjects) {
            try {
                for (Input<?> input2 : beastObject.listInputs()) {
                    Object o = input2.get();
                    if (o != null && o instanceof BEASTInterface) {
                        List<BEASTInterface> list = outputs.get(o);
//                    	if (list == null) {
//                    		int h = 3;
//                    		h++;
//                    	} else {
                        list.add(beastObject);
//                    	}
                    }
                    if (o != null && o instanceof List<?>) {
                        for (Object o2 : (List<?>) o) {
                            if (o2 != null && o2 instanceof BEASTInterface) {
                                List<BEASTInterface> list = outputs.get(o2);
                                if (list != null) {
                                    list.add(beastObject);
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

    public void initPlugins(BEASTInterface beastObject, BeautiDoc doc) {
        //g_plugins = new HashMap<>();
        addPluginToMap(beastObject, doc);
    }

    static public void addPluginToMap(BEASTInterface beastObject, BeautiDoc doc) {
        if (registerPlugin(getID(beastObject), beastObject, doc)) {
            return;
        }
        try {
            for (Input<?> input : beastObject.listInputs()) {
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
            Log.warning.println(e.getClass().getName() + " " + e.getMessage());
        }
    } // addPluginToMap

    /**
     * return ID of beastObject, if no ID is specified, generate an appropriate ID first
     */
    public static String getID(BEASTInterface beastObject) {
        if (beastObject.getID() == null || beastObject.getID().length() == 0) {
            String id = beastObject.getClass().getName().replaceAll(".*\\.", "");
            int i = 0;
            while (g_plugins.containsKey(id + "." + i)) {
                i++;
            }
            beastObject.setID(id + "." + i);
        }
        return beastObject.getID();
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
                BEASTInterface beastObject = new beast.util.XMLParser().parseBareFragment(text.toString(), false);
                pluginPanel = new BEASTObjectPanel(beastObject, beastObject.getClass(), null);
            } else if (args.length == 1) {
                pluginPanel = new BEASTObjectPanel((BEASTInterface) BEASTClassLoader.forName(args[0]).newInstance(), BEASTClassLoader.forName(args[0]), null);
            } else if (args.length == 2) {
                pluginPanel = new BEASTObjectPanel((BEASTInterface) BEASTClassLoader.forName(args[0]).newInstance(), BEASTClassLoader.forName(args[1]), null);
            } else {
                throw new IllegalArgumentException("Incorrect number of arguments");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Usage: " + BEASTObjectPanel.class.getName() + " [-x file ] [class [type]]\n" +
                    "where [class] (optional, default MCMC) is a BEASTObject to edit\n" +
                    "and [type] (optional only if class is specified, default Runnable) the type of the BEASTObject.\n" +
                    "for example\n" +
                    "");
            System.exit(1);
        }
        pluginPanel.setVisible(true);
        if (pluginPanel.m_bOK) {
            BEASTInterface beastObject = pluginPanel.m_beastObject;
            String xml = new XMLProducer().modelToXML(beastObject);
            System.out.println(xml);
        }
    } // main

} // class PluginDialog

