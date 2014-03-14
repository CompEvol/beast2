package beast.app.beauti;

import beast.util.Plugin;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static beast.util.AddOnManager.*;

/**
 * dialog for managing Plugins.
 * List, install and uninstall Plugins
 *
 * modified by Walter Xie
 */
public class JPluginDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    JPanel panel;
//    DefaultListModel model = new DefaultListModel();
//    JList list;

    final JFrame frame;
    JTable dataTable = null;

    List<Plugin> plugins = new ArrayList<Plugin>();

    public JPluginDialog(JFrame frame) {
        super(frame);
        this.frame = frame;
        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        setModal(true);

        panel = new JPanel();
        getContentPane().add(BorderLayout.CENTER, panel);
        setTitle("BEAST 2 Plugin Manager");


        Component pluginListBox = createTable();
        panel.add(pluginListBox);
        Box buttonBox = createButtonBox();
        getContentPane().add(buttonBox, BorderLayout.SOUTH);

        Dimension dim = panel.getPreferredSize();
        Dimension dim2 = buttonBox.getPreferredSize();
        setSize(dim.width + 10, dim.height + dim2.height + 30);
        Point frameLocation = frame.getLocation();
        Dimension frameSize = frame.getSize();
        setLocation(frameLocation.x + frameSize.width / 2 - dim.width / 2, frameLocation.y + frameSize.height / 2 - dim.height / 2);
        frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }


    private Component createTable() {
        Box box = Box.createVerticalBox();
        box.add(new JLabel("List of available plugins for BEAST v" + beastVersion.getMajorVersion() + ".* in alphabetic order"));

        DataTableModel dataTableModel = new DataTableModel();
        dataTable = new JTable(dataTableModel);
        dataTable.setSelectionMode(DefaultListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        dataTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Plugin selPlugin = getSelectedPlugin(dataTable.getSelectedRow());
                    showDetail(selPlugin);
                }
            }
        });

        resetPlugins();

        JScrollPane pane = new JScrollPane(dataTable);
        box.add(pane);
        return box;
    }

    private void resetPlugins() {
        plugins.clear();
        try {
            plugins = getPlugins();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (dataTable.getRowCount() > 0)
            dataTable.setRowSelectionInterval(0, 0);
    }

    private Plugin getSelectedPlugin(int selectedRow) {
        if (plugins.size() <= selectedRow)
            throw new IllegalArgumentException("Incorrect row " + selectedRow +
                    " is selected from plugin list, size = " + plugins.size());
        return plugins.get(selectedRow);
    }

    private void showDetail(Plugin plugin) {
        //custom title, no icon
        JOptionPane.showMessageDialog(frame,
                "Eggs are not supposed to be green.",
                plugin.pluginURL,
                JOptionPane.PLAIN_MESSAGE);
    }

//    @Deprecated
//    private Component createList() {
//        Box box = Box.createVerticalBox();
//        box.add(new JLabel("List of available Plugins"));
//        list = new JList(model);
//        list.setSelectionMode(DefaultListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
//        resetList();
//
//        JScrollPane pane = new JScrollPane(list);
//        box.add(pane);
//        return box;
//    }
//
//    @Deprecated
//    private void resetList() {
//        model.clear();
//        try {
//            List<List<String>> addOns = getAddOns();
//            for (List<String> addOn : addOns) {
//                Plugin pluginObject = new Plugin(addOn);
//                model.addElement(pluginObject);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        list.setSelectedIndex(0);
//    }

    private Box createButtonBox() {
        Box box = Box.createHorizontalBox();
        box.add(Box.createGlue());
        JButton installButton = new JButton("Install/Upgrade");
        installButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = dataTable.getSelectedRows();
                for (int selRow : selectedRows) {
                    Plugin selPlugin = getSelectedPlugin(selRow);
                    if (selPlugin != null) {
                        try {
                            if (selPlugin.isInstalled()) {
                                //TODO upgrade version
                            } else {
                                setCursor(new Cursor(Cursor.WAIT_CURSOR));
                                installAddOn(selPlugin.pluginURL, false, null);
                                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                            }
                            resetPlugins();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "Install failed because: " + ex.getMessage());
                            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            }
        });
        box.add(installButton);

        JButton uninstallButton = new JButton("Uninstall");
        uninstallButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = dataTable.getSelectedRows();

                boolean toDeleteFileExists = false;
                for (int selRow : selectedRows) {
                    Plugin selPlugin = getSelectedPlugin(selRow);
                    if (selPlugin != null) {
                        try {
                            if (selPlugin.isInstalled()) {
//                            if (JOptionPane.showConfirmDialog(null, "Are you sure you want to uninstall " +
//                            AddOnManager.URL2AddOnName(plugin.pluginURL) + "?", "Uninstall Add On",
//                                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                                setCursor(new Cursor(Cursor.WAIT_CURSOR));
                                uninstallAddOn(selPlugin.pluginURL, false, null);
                                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

                                File toDeleteFile = getToDeleteListFile();
                                if (toDeleteFile.exists()) {
                                    toDeleteFileExists = true;
                                }
//                            }
                            } else {
                                //TODO ?
                            }
                            resetPlugins();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "Uninstall failed because: " + ex.getMessage());
                            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }

                if (toDeleteFileExists) {
                    JOptionPane.showMessageDialog(null, "<html>To complete uninstalling the plugin, BEAUti need to be restarted<br><br>Exiting now.</html>");
                    System.exit(0);
                }

            }
        });
        box.add(uninstallButton);

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

        JButton button = new JButton("?");
        button.setToolTipText(getPluginUserDir() + " " + getAddOnAppDir());
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(panel, "<html>Plugin are installed in <br><br><em>" + getPluginUserDir() +
                        "</em><br><br> by you, and are available to you,<br>the user, only.<br>" +
                        "System wide plugins are installed in <br><br><em>" + getAddOnAppDir() +
                        "</em><br><br>and are available to all users." +
                        "<br>(just move the plugin there manually" +
                        "<br>to make it system wide available).</html>");
            }
        });
        box.add(button);
        return box;
    }

    class DataTableModel extends AbstractTableModel {
        String[] columnNames = {"Name", "Status/Version", "Latest", "Dependencies", "Detail"};

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return plugins.size();
        }

        public Object getValueAt(int row, int col) {
            Plugin plugin = plugins.get(row);
            switch (col) {
                case 0:
                    return plugin.pluginName;
                case 1:
                    return plugin.getStatus();
                case 2:
                    return plugin.getLatestVersion();
                case 3:
                    return plugin.getDependencies();
                case 4:
                    return plugin.pluginDescription;
                default:
                    throw new IllegalArgumentException("unknown column, " + col);
            }
        }

        public String getColumnName(int column) {
            return columnNames[column];
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();

            buffer.append(getColumnName(0));
            for (int j = 1; j < getColumnCount(); j++) {
                buffer.append("\t");
                buffer.append(getColumnName(j));
            }
            buffer.append("\n");

            for (int i = 0; i < getRowCount(); i++) {
                buffer.append(getValueAt(i, 0));
                for (int j = 1; j < getColumnCount(); j++) {
                    buffer.append("\t");
                    buffer.append(getValueAt(i, j));
                }
                buffer.append("\n");
            }

            return buffer.toString();
        }
    }


}
