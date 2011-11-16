package beast.app.draw;

import beast.core.Input;
import beast.core.MCMC;
import beast.core.Plugin;
import beast.util.XMLProducer;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Scanner;

/**
 * Dialog for editing Plugins.
 * <p/>
 * This dynamically creates a dialog consisting of
 * InputEditors associated with the inputs of a Plugin.
 * *
 */

public class PluginDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    /**
     * plug in to be edited *
     */

    private boolean m_bOK = false;

    public PluginPanel m_panel;

    /**
     * map that identifies the InputEditor to use for a particular type of Input *
     */


    public PluginDialog(PluginPanel panel) {
        init(panel);
    }

    public PluginDialog(Plugin plugin, Class<? extends Plugin> aClass, List<Plugin> plugins) {
        this(new PluginPanel(plugin, aClass, plugins));
    }

    public PluginDialog(Plugin plugin, Class<?> type) {
        this(new PluginPanel(plugin, type));
    }

    /* to be called when Cancel is pressed **/
    public void accept(Plugin plugin) {
    	try {
    		for (Input<?> input : m_panel.m_plugin.listInputs()) {
    			plugin.setInputValue(input.getName(), input.get());
    		}
    		plugin.setID(m_panel.m_plugin.getID());
    	} catch (Exception e) {
			e.printStackTrace();
		}
    }

    void init(PluginPanel panel) {


        this.m_panel = panel;

        setModal(true);

        add(BorderLayout.CENTER, panel);

        setTitle(panel.m_plugin.getID() + " Editor");


        /* add cancel and ok buttons at the bottom */
        Box cancelOkBox = Box.createHorizontalBox();
        cancelOkBox.setBorder(new EtchedBorder());
        JButton okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {

            // implementation ActionListener
            public void actionPerformed(ActionEvent e) {
                m_bOK = true;
                dispose();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {

            // implementation ActionListener
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

        add(BorderLayout.SOUTH, cancelOkBox);

        Dimension dim = panel.getPreferredSize();
        Dimension dim2 = cancelOkBox.getPreferredSize();
        setSize(dim.width + 10, dim.height + dim2.height + 30);

        //PluginDialog.m_position.x += 30;
        //PluginDialog.m_position.y += 30;
        //setLocation(PluginDialog.m_position);
    } // c'tor

    public boolean getOK() {
        //PluginDialog.m_position.x -= 30;
        //PluginDialog.m_position.y -= 30;
    	if (m_bOK) {
    		String sOldID = m_panel.m_plugin.getID();
			PluginPanel.g_plugins.remove(sOldID);
			m_panel.m_plugin.setID(m_panel.m_identry.getText());
			PluginPanel.registerPlugin(m_panel.m_plugin.getID(), m_panel.m_plugin);
    	}
        return m_bOK;
    }

    /**
     * rudimentary test *
     */
    public static void main(String[] args) {
        PluginDialog dlg = null;
        try {
            if (args.length == 0) {
                dlg = new PluginDialog(new PluginPanel(new MCMC(), Runnable.class));
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
                dlg = new PluginDialog(new PluginPanel(plugin, plugin.getClass()));
            } else if (args.length == 1) {
                dlg = new PluginDialog(new PluginPanel((Plugin) Class.forName(args[0]).newInstance(), Class.forName(args[0])));
            } else if (args.length == 2) {
                dlg = new PluginDialog(new PluginPanel((Plugin) Class.forName(args[0]).newInstance(), Class.forName(args[1])));
            } else {
                throw new Exception("Incorrect number of arguments");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Usage: " + PluginDialog.class.getName() + " [-x file ] [class [type]]\n" +
                    "where [class] (optional, default MCMC) is a Plugin to edit\n" +
                    "and [type] (optional only if class is specified, default Runnable) the type of the Plugin.\n" +
                    "for example\n" +
                    "");
            System.exit(0);
        }
        dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dlg.setVisible(true);
        if (dlg.m_bOK) {
            Plugin plugin = dlg.m_panel.m_plugin;
            String sXML = new XMLProducer().modelToXML(plugin);
            System.out.println(sXML);
        }
    } // main
} // class PluginDialog

