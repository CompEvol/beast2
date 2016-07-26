package beast.app.beauti;

import beast.core.Description;
import beast.util.AddOnManager;
import beast.util.Package;
import beast.util.PackageVersion;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static beast.util.AddOnManager.*;

/**
 * dialog for managing Package.
 * List, install and uninstall Package
 *
 * @author  Remco Bouckaert
 * @author  Walter Xie
 */
@Description("BEAUti package manager")
public class JPackageDialog extends JPanel {
    private static final long serialVersionUID = 1L;
    JScrollPane scrollPane;
    JLabel jLabel;
    Box buttonBox;
    JFrame frame;
    PackageTable dataTable = null;

    TreeMap<String, Package> packageMap = new TreeMap<>((s1,s2)->{
    	if (s1.equals(AddOnManager.BEAST_PACKAGE_NAME)) {
    		if (s2.equals(AddOnManager.BEAST_PACKAGE_NAME)) {
    			return 0;
    		}
    		return -1;
    	}
    	if (s2.equals(AddOnManager.BEAST_PACKAGE_NAME)) {
    		return 1;
    	}
    	return s1.compareToIgnoreCase(s2);
    });

    List<Package> packageList = null;

    boolean isRunning;
    Thread t;
    
    public JPackageDialog() {
        jLabel = new JLabel("List of available packages for BEAST v" + beastVersion.getMajorVersion() + ".*");
        frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        setLayout(new BorderLayout());

		createTable();
        // update packages using a 30 second time out
        isRunning = true;
        t = new Thread() {
        	@Override
			public void run() {
                resetPackages();
        		isRunning = false;
        	}
        };
        t.start();
    	Thread t2 = new Thread() {
    		@Override
			public void run() {
    			try {
    				// wait 30 seconds
					sleep(30000);
	    			if (isRunning) {
	    				t.interrupt();
	    				JOptionPane.showMessageDialog(frame, "<html>Download of file " +
	    						AddOnManager.PACKAGES_XML + " timed out.<br>" +
	    								"Perhaps this is due to lack of internet access</br>" +
	    								"or some security settings not allowing internet access.</html>"
	    						);
	    			}
				} catch (InterruptedException e) {
				}
    		}
    	};
    	t2.start();
        
        try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        	
        scrollPane = new JScrollPane(dataTable);
        /*getContentPane().*/add(BorderLayout.CENTER, scrollPane);

        buttonBox = createButtonBox();
        /*getContentPane().*/add(buttonBox, BorderLayout.SOUTH);

        scrollPane.setPreferredSize(new Dimension(660, 400));
        Dimension dim = scrollPane.getPreferredSize();
        Dimension dim2 = buttonBox.getPreferredSize();
        setSize(dim.width + 30, dim.height + dim2.height + 30);
    }


    private void createTable() {
        DataTableModel dataTableModel = new DataTableModel();
        dataTable = new PackageTable(dataTableModel);
        
        double [] widths = new double[dataTable.getColumnCount()];
        //double total = 0;
        for (int i = 0; i < dataTable.getColumnCount(); i++) {
        	widths[i] = dataTable.getColumnModel().getColumn(i).getWidth();
        	//total += widths[i]; 
        }
        widths[2] /= 4.0;
        dataTable.getColumnModel().getColumn(2).setPreferredWidth((int) widths[2]);
        dataTable.getColumnModel().getColumn(2).setMinWidth((int) widths[2]);
        widths[3] /= 2.0; 
        dataTable.getColumnModel().getColumn(3).setPreferredWidth((int) widths[3]);
        widths[4] *= 2.0; 
        dataTable.getColumnModel().getColumn(4).setPreferredWidth((int) widths[4]);
        
        
        // TODO:
        // The following would work ...
        //dataTable.setAutoCreateRowSorter(true);
        // ...if all processing was done based on the data in the table, 
        // instead of the row number alone.
        dataTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        dataTable.addMouseListener(new MouseAdapter() {
            @Override
			public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Package selPackage = getSelectedPackage(dataTable.getSelectedRow());
                    showDetail(selPackage);
                }
            }
        });
		int size = dataTable.getFont().getSize();
		dataTable.setRowHeight(20 * size/13);
    }

    private void resetPackages() {
        packageMap.clear();
        try {
            addAvailablePackages(packageMap);
            addInstalledPackages(packageMap);

            // Create list of packages excluding beast2
            packageList = new ArrayList<>();
            for (Package pkg : packageMap.values())
                if (!pkg.getName().equals("beast2"))
                    packageList.add(pkg);

        } catch (AddOnManager.PackageListRetrievalException e) {
        	StringBuilder msgBuilder = new StringBuilder("<html>" + e.getMessage() + "<br>");
            if (e.getCause() instanceof IOException)
                msgBuilder.append(NO_CONNECTION_MESSAGE.replaceAll("\\.", ".<br>"));
            msgBuilder.append("</html>");

        	try {
        	SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, msgBuilder));
        	} catch (Exception e0) {
        		e0.printStackTrace();
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }

        dataTable.tableChanged(new TableModelEvent(dataTable.getModel()));

        if (dataTable.getRowCount() > 0)
            dataTable.setRowSelectionInterval(0, 0);

    }

    private Package getSelectedPackage(int selectedRow) {
        if (packageList.size() <= selectedRow)
            throw new IllegalArgumentException("Incorrect row " + selectedRow +
                    " is selected from package list, size = " + packageMap.size());
        return packageList.get(selectedRow);
    }

    private void showDetail(Package aPackage) {
        //custom title, no icon
        JOptionPane.showMessageDialog(null,
                aPackage.toHTML(),
                aPackage.getName(),
                JOptionPane.PLAIN_MESSAGE);
    }

    private Box createButtonBox() {
        Box box = Box.createHorizontalBox();
        JButton installButton = new JButton("Install/Upgrade");
        installButton.addActionListener(e -> {
            // first get rid of existing packages
            int[] selectedRows = dataTable.getSelectedRows();
            String installedPackageNames = "";

            setCursor(new Cursor(Cursor.WAIT_CURSOR));

            Map<Package, PackageVersion> packagesToInstall = new HashMap<>();
            for (int selRow : selectedRows) {
                Package selPackage = getSelectedPackage(selRow);
                if (selPackage != null)
                    packagesToInstall.put(selPackage, selPackage.getLatestVersion());
            }

            try {
                populatePackagesToInstall(packageMap, packagesToInstall);

                prepareForInstall(packagesToInstall, false, null);

                if (getToDeleteListFile().exists()) {
                    JOptionPane.showMessageDialog(frame,
                            "<html><body><p style='width: 200px'>Upgrading packages on your machine requires BEAUti " +
                                    "to restart. Shutting down now.</p></body></html>");
                    System.exit(0);
                }

                installPackages(packagesToInstall, false, null);

                // Refresh classes:
                loadExternalJars();

                installedPackageNames = String.join(",",
                        packagesToInstall.keySet().stream()
                                .map(Package::toString)
                                .collect(Collectors.toList()));

                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

            } catch (DependencyResolutionException | IOException ex) {
                JOptionPane.showMessageDialog(null, "Install failed because: " + ex.getMessage());
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }

            resetPackages();
            dataTable.setRowSelectionInterval(selectedRows[0], selectedRows[0]);

            if (installedPackageNames.length()>0)
                JOptionPane.showMessageDialog(null, "Package(s) "
                        + installedPackageNames + " installed. "
                        + "Note that any changes to the BEAUti "
                        + "interface will\n not appear until a "
                        + "new document is created or BEAUti is "
                        + "restarted.");
        });
        box.add(installButton);

        JButton uninstallButton = new JButton("Uninstall");
        uninstallButton.addActionListener(e -> {
            StringBuilder removedPackageNames = new StringBuilder();
            int[] selectedRows = dataTable.getSelectedRows();

            for (int selRow : selectedRows) {
                Package selPackage = getSelectedPackage(selRow);
                if (selPackage != null) {
                    try {
                        if (selPackage.isInstalled()) {
                            setCursor(new Cursor(Cursor.WAIT_CURSOR));
                            List<String> deps = getInstalledDependencyNames(selPackage, packageMap);

                            if (deps.isEmpty()) {
                                String result = uninstallPackage(selPackage, false, null);

                                if (result != null) {
                                    if (removedPackageNames.length() > 0)
                                        removedPackageNames.append(", ");
                                    removedPackageNames.append("'")
                                            .append(selPackage.getName())
                                            .append("'");
                                }
                            } else {
                                throw new DependencyResolutionException("package " + selPackage
                                        + " is used by the following packages: "
                                + String.join(", ", deps) + "\n"
                                + "Remove those packages first.");
                            }

                            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        }

                        resetPackages();
                        dataTable.setRowSelectionInterval(selectedRows[0], selectedRows[0]);
                    } catch (IOException | DependencyResolutionException ex) {
                        JOptionPane.showMessageDialog(null, "Uninstall failed because: " + ex.getMessage());
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            }

            if (getToDeleteListFile().exists()) {
                JOptionPane.showMessageDialog(frame,
                        "<html><body><p style='width: 200px'>Removing packages on your machine requires BEAUti " +
                                "to restart. Shutting down now.</p></body></html>");
                System.exit(0);
            }

            if (removedPackageNames.length()>0)
                JOptionPane.showMessageDialog(null, "Package(s) "
                        + removedPackageNames.toString() + " removed. "
                        + "Note that any changes to the BEAUti "
                        + "interface will\n not appear until a "
                        + "new document is created or BEAUti is "
                        + "restarted.");
        });
        box.add(uninstallButton);

        box.add(Box.createHorizontalGlue());

        JButton packageRepoButton = new JButton("Package repositories");
        packageRepoButton.addActionListener(e -> {
                JPackageRepositoryDialog dlg = new JPackageRepositoryDialog(frame);
                dlg.setVisible(true);
                resetPackages();
            });
        box.add(packageRepoButton);

        box.add(Box.createGlue());

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            	if (dlg != null) {
            		dlg.setVisible(false);
            	} else {
            		setVisible(false);
            	}
            });
        box.add(closeButton);

        JButton button = new JButton("?");
        button.setToolTipText(getPackageUserDir() + " " + getPackageSystemDir());
        button.addActionListener(e -> {
                JOptionPane.showMessageDialog(scrollPane, "<html>By default, packages are installed in <br><br><em>" + getPackageUserDir() +
                        "</em><br><br>and are available only to you.<br>" +
                        "<br>Packages can also be moved manually to <br><br><em>" + getPackageSystemDir() +
                        "</em><br><br>which makes them available to all users<br>"
                        + "on your system.</html>");
            });
        box.add(button);
        return box;
    }

	class DataTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		String[] columnNames = {"Name", "Status/Version", "Latest", "Dependencies", "Detail"};

        @Override
		public int getColumnCount() {
            return columnNames.length;
        }

        @Override
		public int getRowCount() {
            return packageList.size();
        }

        @Override
		public Object getValueAt(int row, int col) {
            Package aPackage = packageList.get(row);
            switch (col) {
                case 0:
                    return aPackage.getName();
                case 1:
                    return aPackage.getStatusString();
                case 2:
                    return aPackage.isAvailable() ? aPackage.getLatestVersion() : "not available";
                case 3:
                    return aPackage.getDependenciesString();
                case 4:
                    return aPackage.getDescription();
                default:
                    throw new IllegalArgumentException("unknown column, " + col);
            }
        }

        @Override
		public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
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



	public JDialog asDialog(JFrame frame) {
		if (frame == null) {
	        frame = (JFrame) SwingUtilities.getWindowAncestor(this);
		}
		this.frame = frame;
    	dlg = new JDialog(frame, "BEAST 2 Package Manager", true);
		dlg.getContentPane().add(scrollPane, BorderLayout.CENTER);  
		dlg.getContentPane().add(jLabel, BorderLayout.NORTH);  
		dlg.getContentPane().add(buttonBox, BorderLayout.SOUTH);  
		dlg.pack();  
        Point frameLocation = frame.getLocation();
        Dimension frameSize = frame.getSize();
        Dimension dim = getPreferredSize();
        int size = UIManager.getFont("Label.font").getSize();
        dlg.setSize(690 * size / 13, 430 * size / 13);
        dlg.setLocation(frameLocation.x + frameSize.width / 2 - dim.width / 2, frameLocation.y + frameSize.height / 2 - dim.height / 2);

        frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        return dlg;
	}


	JDialog dlg = null;
	@Override
	public void setCursor(Cursor cursor) {
		if (dlg != null) {
			dlg.setCursor(cursor);
		} else {
			super.setCursor(cursor);
		}
	}

    class PackageTable extends JTable {
		private static final long serialVersionUID = 1L;

        Map<Package, PackageVersion> packagesToInstall = new HashMap<>();

        public PackageTable(TableModel dm) {
            super(dm);
        }

        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c =  super.prepareRenderer(renderer, row, column);

            Font font = c.getFont();
            font.getFamily();
            Font boldFont = new Font(font.getName(), Font.BOLD | Font.ITALIC, font.getSize());

            Package pkg = packageList.get(row);

            if (! isRowSelected(row)) {
                if (pkg.newVersionAvailable()) {
                    if (pkg.isInstalled())
                        c.setFont(boldFont);

                    if (column == 2) {
                        packagesToInstall.clear();
                        packagesToInstall.put(pkg, pkg.getLatestVersion());
                        try {
                            populatePackagesToInstall(packageMap, packagesToInstall);
                            c.setForeground(new Color(0, 150, 0));
                        } catch (DependencyResolutionException ex) {
                            c.setForeground(new Color(150, 0, 0));
                        }
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                } else {
                    c.setForeground(Color.BLACK);
                }
            }

            return c;
        }
    }
}
