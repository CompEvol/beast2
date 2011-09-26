package beast.app.beauti;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import beast.util.ClassDiscovery;

import java.util.List;

/** dialog for managing Add-ons.
 * List, install and uninstall add-ons
 *  **/
public class JAddOnDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	JPanel panel;
	DefaultListModel model = new DefaultListModel();
	JList list;

	public JAddOnDialog() {
        setModal(true);
        panel = new JPanel();
        add(BorderLayout.CENTER, panel);
        setTitle("BEAST 2 Add-On Manager");
        
        
        Component pluginListBox = createList();
        panel.add(pluginListBox);
        Box buttonBox = createButtonBox();
        add(buttonBox, BorderLayout.SOUTH);
        
        Dimension dim = panel.getPreferredSize();
        Dimension dim2 = buttonBox.getPreferredSize();
        setSize(dim.width + 10, dim.height + dim2.height + 30);
	}

	class AddOn {
		String sAddOnURL;
		String sAddOnDescription;
		boolean bIsInstalled;
		AddOn(List<String> list) {
			sAddOnDescription = list.get(0);
			sAddOnURL = list.get(1);
			bIsInstalled = false;
			List<String> sBeastDirs = ClassDiscovery.getBeastDirectories();
			String sAddOnName = ClassDiscovery.URL2AddOnName(sAddOnURL); 
			for (String sDir : sBeastDirs) {
				File f = new File(sDir + "/" + sAddOnName);
				if (f.exists()) {
					bIsInstalled = true;
				}
			}
		}
		
		public String toString() {
			if (bIsInstalled) {
				return sAddOnDescription + "(installed)";
			}
			return sAddOnDescription;
		}
	}
	
	private Component createList() {
		list  = new JList(model);
		list.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		try {
			List<List<String>> addOns = ClassDiscovery.getAddOns();
			for (List<String> addOn : addOns) {
				AddOn addOnObject = new AddOn(addOn);
				model.addElement(addOnObject);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		JScrollPane pane = new JScrollPane(list);
		return pane;
	}
	
	private Box createButtonBox() {
		Box box = Box.createHorizontalBox();
		box.add(Box.createGlue());
		JButton installButton = new JButton("Install/Uninstall");
		installButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				AddOn addOn = (AddOn) list.getSelectedValue();
				if (addOn != null) {
					try {
					if (addOn.bIsInstalled) {
						ClassDiscovery.uninstallAddOn(addOn.sAddOnURL);
					} else {
						
						ClassDiscovery.installAddOn(addOn.sAddOnURL);
					}
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(null, "Install/uninstall failed because: " + ex.getMessage());
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
	
	
	
}
