package beast.app.draw;


import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;

import beast.app.beauti.BeautiDoc;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.MCMC;
import beast.util.XMLProducer;

/**
 * Dialog for editing Plugins.
 * <p/>
 * This dynamically creates a dialog consisting of
 * InputEditors associated with the inputs of a Plugin.
 * *
 */

public class BEASTObjectDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    /**
     * plug in to be edited *
     */

    private boolean m_bOK = false;

    public BEASTObjectPanel m_panel;

    /**
     * map that identifies the InputEditor to use for a particular type of Input *
     */


    BeautiDoc doc;
    
    public BEASTObjectDialog(BEASTObjectPanel panel, BeautiDoc doc) {
        init(panel);
        this.doc = doc;
    }

    public BEASTObjectDialog(BEASTInterface plugin, Class<? extends BEASTInterface> aClass, List<BEASTInterface> plugins, BeautiDoc doc) {
        this(new BEASTObjectPanel(plugin, aClass, plugins, doc), doc);
    }

    public BEASTObjectDialog(BEASTInterface plugin, Class<?> type, BeautiDoc doc) {
        this(new BEASTObjectPanel(plugin, type, doc), doc);
    }

    
    public boolean showDialog() {
        URL url = ClassLoader.getSystemResource(ModelBuilder.ICONPATH + "beast.png");
        Icon icon = new ImageIcon(url);
        JOptionPane optionPane = new JOptionPane(m_panel,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                icon,
                null,
                null);
        optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

        Frame frame = (doc != null ? doc.getFrame(): Frame.getFrames()[0]);
        final JDialog dialog = optionPane.createDialog(frame, this.getTitle());
        dialog.pack();

        dialog.setVisible(true);

        int result = JOptionPane.CANCEL_OPTION;
        Integer value = (Integer) optionPane.getValue();
        if (value != null && value != -1) {
            result = value;
        }
        m_bOK = (result != JOptionPane.CANCEL_OPTION);
        return m_bOK;
    }
    
    /* to be called when OK is pressed **/
    public void accept(BEASTInterface plugin, BeautiDoc doc) {
        try {
            for (Input<?> input : m_panel.m_plugin.listInputs()) {
            	if (input.get() != null && (input.get() instanceof List)) {
                    // setInpuValue (below) on lists does not lead to expected result
            		// it appends values to the list instead, so we have to clear it first
                    List<?> list = (List<?>)plugin.getInput(input.getName()).get();
                    list.clear();
            	}
            	plugin.setInputValue(input.getName(), input.get());
            }
            plugin.setID(m_panel.m_plugin.getID());
            if (doc != null) {
            	doc.addPlugin(plugin);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void init(BEASTObjectPanel panel) {
        this.m_panel = panel;

        setModal(true);

        add(BorderLayout.CENTER, panel);

        setTitle(panel.m_plugin.getID() + " Editor");


//        /* add cancel and ok buttons at the bottom */
//        Box cancelOkBox = Box.createHorizontalBox();
//        cancelOkBox.setBorder(new EtchedBorder());
//        JButton okButton = new JButton("Ok");
//        okButton.addActionListener(new ActionListener() {
//
//            // implementation ActionListener
//            public void actionPerformed(ActionEvent e) {
//                m_bOK = true;
//                dispose();
//            }
//        });
//        JButton cancelButton = new JButton("Cancel");
//        cancelButton.addActionListener(new ActionListener() {
//
//            // implementation ActionListener
//            public void actionPerformed(ActionEvent e) {
//                m_bOK = false;
//                dispose();
//            }
//        });
//        cancelOkBox.add(Box.createHorizontalGlue());
//        cancelOkBox.add(okButton);
//        cancelOkBox.add(Box.createHorizontalGlue());
//        cancelOkBox.add(cancelButton);
//        cancelOkBox.add(Box.createHorizontalGlue());
//
//        add(BorderLayout.SOUTH, cancelOkBox);
//
//        Dimension dim = panel.getPreferredSize();
//        Dimension dim2 = cancelOkBox.getPreferredSize();
//        setSize(dim.width + 10, dim.height + dim2.height + 30);
    } // c'tor

    public boolean getOK(BeautiDoc doc) {
        //PluginDialog.m_position.x -= 30;
        //PluginDialog.m_position.y -= 30;
        if (m_bOK) {
            String sOldID = m_panel.m_plugin.getID();
            BEASTObjectPanel.g_plugins.remove(sOldID);
            m_panel.m_plugin.setID(m_panel.m_identry.getText());
            BEASTObjectPanel.registerPlugin(m_panel.m_plugin.getID(), m_panel.m_plugin, doc);
        }
        return m_bOK;
    }

    /**
     * rudimentary test *
     */
    public static void main(String[] args) {
        BEASTObjectDialog dlg = null;
        try {
            if (args.length == 0) {
                dlg = new BEASTObjectDialog(new BEASTObjectPanel(new MCMC(), Runnable.class, null), null);
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
                dlg = new BEASTObjectDialog(new BEASTObjectPanel(plugin, plugin.getClass(), null), null);
            } else if (args.length == 1) {
                dlg = new BEASTObjectDialog(new BEASTObjectPanel((BEASTInterface) Class.forName(args[0]).newInstance(), Class.forName(args[0]), null), null);
            } else if (args.length == 2) {
                dlg = new BEASTObjectDialog(new BEASTObjectPanel((BEASTInterface) Class.forName(args[0]).newInstance(), Class.forName(args[1]), null), null);
            } else {
                throw new IllegalArgumentException("Incorrect number of arguments");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Usage: " + BEASTObjectDialog.class.getName() + " [-x file ] [class [type]]\n" +
                    "where [class] (optional, default MCMC) is a Plugin to edit\n" +
                    "and [type] (optional only if class is specified, default Runnable) the type of the Plugin.\n" +
                    "for example\n" +
                    "");
            System.exit(1);
        }
        dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        if (dlg.showDialog()) {
            BEASTInterface plugin = dlg.m_panel.m_plugin;
            String sXML = new XMLProducer().modelToXML(plugin);
            System.out.println(sXML);
        }
    } // main
} // class PluginDialog

