package beast.app.beauti2;

import beast.util.AddOnManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

/** dialog for managing Add-ons.
 * List, install and uninstall add-ons
 *  **/
public class AddOnManagerDialog {
    private static final long serialVersionUID = 1L;

    private final JFrame frame;

    public AddOnManagerDialog(JFrame frame) {
        this.frame = frame;
    }

    public int showDialog() {
        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));


        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("List of available Add-ons"));

        final DefaultListModel model = new DefaultListModel();
        model.addElement("Fetching...");

        final JList list  = new JList(model);
        list.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(list);

        panel.add(scrollPane, BorderLayout.CENTER);

        final JButton installButton = new JButton("Install/Uninstall");
        JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel1.add(installButton);

        panel.add(panel1, BorderLayout.SOUTH);

        Object[] buttons = {"Done"};
        JOptionPane optionPane = new JOptionPane(panel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_OPTION,
                null,
                buttons,
                buttons[0]);
        optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

        final JDialog dialog = optionPane.createDialog(frame, "Add-On Manager");
        dialog.setModal(true);
        dialog.setResizable(true);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(320, 240));
        dialog.setSize(640, 480);

        installButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddOn addOn = (AddOn) list.getSelectedValue();
                if (addOn != null) {
                    try {
                        if (addOn.bIsInstalled) {
                            if (JOptionPane.showConfirmDialog(null, "Are you sure you want to uninstall " + AddOnManager.URL2AddOnName(addOn.sAddOnURL) + "?", "Uninstall Add On", JOptionPane.YES_NO_OPTION) ==
                                    JOptionPane.YES_OPTION) {
                                frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                                AddOnManager.uninstallAddOn(addOn.sAddOnURL);
                                frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                            }
                        } else {
                            frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                            AddOnManager.installAddOn(addOn.sAddOnURL);
                            frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        }
                        populateList(model);
                        list.setSelectedIndex(0);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Install/uninstall failed because: " + ex.getMessage());
                    }
                }
            }
        });

        // disable these until table is populated
        list.setEnabled(false);
        installButton.setEnabled(false);

        // show dialog and then populate list
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
              populateList(model);
              list.setEnabled(true);
              list.setSelectedIndex(0);
              installButton.setEnabled(true);
          }
        });

        dialog.setVisible(true);

        return JOptionPane.OK_OPTION;
    }

    class AddOn {
        String sAddOnURL;
        String sAddOnDescription;
        boolean bIsInstalled;
        AddOn(List<String> list) {
            sAddOnDescription = list.get(0);
            sAddOnURL = list.get(1);
            bIsInstalled = false;
            List<String> sBeastDirs = AddOnManager.getBeastDirectories();
            String sAddOnName = AddOnManager.URL2AddOnName(sAddOnURL);
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

    private void populateList(DefaultListModel model) {
        model.clear();
        try {
            List<List<String>> addOns = AddOnManager.getAddOns();
            for (List<String> addOn : addOns) {
                AddOn addOnObject = new AddOn(addOn);
                model.addElement(addOnObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
