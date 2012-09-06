package beast.app.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import beast.util.AddOnManager;


/** launch applications specific to add-ons installed, for example
 * utilities for post-processing add-on specific data. 
 */
public class AddOnAppLauncher extends JDialog {
    private static final long serialVersionUID = 1L;

    JPanel panel;
    DefaultListModel model = new DefaultListModel();
    JList list;
	
	public AddOnAppLauncher() {
		
        panel = new JPanel();
        add(BorderLayout.CENTER, panel);
        setTitle("BEAST 2 Add-On Application Launcher");
        Component pluginListBox = createList();
        panel.add(pluginListBox);
        Box buttonBox = createButtonBox();
        add(buttonBox, BorderLayout.SOUTH);

        Dimension dim = panel.getPreferredSize();
        Dimension dim2 = buttonBox.getPreferredSize();
        setSize(dim.width + 10, dim.height + dim2.height + 30);
	}

    private Component createList() {
        Box box = Box.createVerticalBox();
        box.add(new JLabel("List of available Add-on applications"));
        list = new JList(model);
        list.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        resetList();

        JScrollPane pane = new JScrollPane(list);
        box.add(pane);
        return box;
    }
    
    private void resetList() {
        model.clear();
        try {
            List<List<String>> addOns = getAddOnApps();
            for (List<String> addOn : addOns) {
            	AddOnApp addOnApp= new AddOnApp(addOn);
                model.addElement(addOnApp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        list.setSelectedIndex(0);
    }
    
    private Box createButtonBox() {
        Box box = Box.createHorizontalBox();
        box.add(Box.createGlue());
        JButton installButton = new JButton("Install/Uninstall");
        installButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                AddOnApp addOnApp = (AddOnApp) list.getSelectedValue();
                if (addOnApp != null) {
                    try {
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Launch failed because: " + ex.getMessage());
                    }
                }
            }
        });
        box.add(installButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        box.add(Box.createGlue());
        box.add(closeButton);
        box.add(Box.createGlue());
        return box;
    }
    
    /**
	 * @param args
	 */
	public static void main(String[] args) {
		AddOnAppLauncher dlg = new AddOnAppLauncher();
		dlg.setVisible(true);

	}

	List<List<String>> getAddOnApps() {
		List<List<String>> addons = new ArrayList<List<String>>();
		List<String> dirs = AddOnManager.getBeastDirectories();
        for (String sJarDir : dirs) {
            File versionFile = new File(sJarDir + "/version.xml");
            if (versionFile.exists() && versionFile.isFile()) {
            	// TODO: get app info from version file
            	// TODO: add app info to addons list
            }
        }
		
		return addons;
	}

	
	class AddOnApp {
		AddOnApp(List<String> addon) {
			className = addon.get(0);
			defaultArguments = addon.get(1);
			icon = (addon.size() > 2? addon.get(2) : null);
		}
		String className;
		String defaultArguments;
		String icon;
	}
}
