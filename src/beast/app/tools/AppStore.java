package beast.app.tools;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import beast.app.util.Utils;
import beast.core.util.Log;
import beast.util.AddOnManager;


/**
 * launch applications specific to add-ons installed, for example utilities for
 * post-processing add-on specific data.
 *
 * @author  Remco Bouckaert
 * @author  Walter Xie
 */
public class AppStore {
    public static final String DEFAULT_ICON = "beast/app/tools/images/utility.png";

    private final String ALL = "-all-";
    JComboBox<String> packageComboBox;
    DefaultListModel<PackageApp> model = new DefaultListModel<>();
    JList<PackageApp> listApps;
    JButton launchButton = new JButton("Launch");
    JDialog mainDialog;

    public AppStore() {
    }

    public JDialog launchGUI() {

        mainDialog = new JDialog();
        mainDialog.setTitle("BEAST 2 Package Application Launcher");

        Box top = Box.createHorizontalBox();
        JLabel label = new JLabel("Filter: ");
        packageComboBox = new JComboBox<>(new String[]{ALL});
        packageComboBox.setToolTipText("Show application of the installed package(s)");
        packageComboBox.addActionListener(e -> {
                JComboBox<?> cb = (JComboBox<?>) e.getSource();
                if (cb.getSelectedItem() != null) {
                    resetAppList(cb.getSelectedItem().toString());
                }
            });
        label.setLabelFor(packageComboBox);
        top.add(label);
        top.add(packageComboBox);
        mainDialog.getContentPane().add(BorderLayout.NORTH, top);

        Component beastObjectListBox = createList();
        mainDialog.getContentPane().add(BorderLayout.CENTER, beastObjectListBox);

        Box buttonBox = createButtonBox();
        mainDialog.getContentPane().add(buttonBox, BorderLayout.SOUTH);

//      Dimension dim = panel.getPreferredSize();
//      Dimension dim2 = buttonBox.getPreferredSize();
//		setSize(dim.width + 10, dim.height + dim2.height + 30);
        mainDialog.setSize(new Dimension(660, 400));
        mainDialog.setLocationRelativeTo(null);

        return mainDialog;
    }

    private Component createList() {
        Box box = Box.createVerticalBox();
        box.add(Box.createGlue());

        listApps = new JList<PackageApp>(model) {
 			private static final long serialVersionUID = 1L;

			//Subclass JList to workaround bug 4832765, which can cause the
            //scroll pane to not let the user easily scroll up to the beginning
            //of the list.  An alternative would be to set the unitIncrement
            //of the JScrollBar to a fixed value. You wouldn't get the nice
            //aligned scrolling, but it should work.
            @Override
			public int getScrollableUnitIncrement(Rectangle visibleRect,
                                                  int orientation,
                                                  int direction) {
                int row;
                if (orientation == SwingConstants.VERTICAL &&
                        direction < 0 && (row = getFirstVisibleIndex()) != -1) {
                    Rectangle r = getCellBounds(row, row);
                    if ((r.y == visibleRect.y) && (row != 0))  {
                        Point loc = r.getLocation();
                        loc.y--;
                        int prevIndex = locationToIndex(loc);
                        Rectangle prevR = getCellBounds(prevIndex, prevIndex);

                        if (prevR == null || prevR.y >= r.y) {
                            return 0;
                        }
                        return prevR.height;
                    }
                }
                return super.getScrollableUnitIncrement(
                        visibleRect, orientation, direction);
            }
        };
        listApps.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listApps.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                JLabel label = (JLabel) super
                        .getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setIcon(((PackageApp) value).icon);
                label.setHorizontalTextPosition(SwingConstants.CENTER);
                label.setVerticalTextPosition(SwingConstants.BOTTOM);
                return label;
            }
        });
        listApps.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listApps.setVisibleRowCount(-1);
        listApps.addMouseListener(new MouseAdapter() {
            @Override
			public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    launchButton.doClick();
                }
            }
        });

//        if (model.getSize() > 0) { // TODO not working
//            listApps.setPrototypeCellValue(model.firstElement()); //get extra space
//        }

        resetAppList();

        JScrollPane listScroller = new JScrollPane(listApps);
        listScroller.setPreferredSize(new Dimension(660, 400));
        listScroller.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel("List of available package applications");
        label.setLabelFor(listApps);

        box.add(label);
        box.add(listScroller);

        return box;
    }

    private void resetAppList() {
        Set<String> packages = new TreeSet<>();
        model.clear();
        try {
            List<PackageApp> packageApps = getPackageApps();
            for (PackageApp packageApp : packageApps) {
                model.addElement(packageApp);
                packages.add(packageApp.packageName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        listApps.setSelectedIndex(0);

        packageComboBox.removeAllItems();
        packageComboBox.addItem(ALL);
        for (String p : packages) {
            packageComboBox.addItem(p);
        }
    }


    private void resetAppList(String packageName) {
        model.clear();
        try {
            List<PackageApp> packageApps = getPackageApps();
            for (PackageApp packageApp : packageApps) {
                if (packageName.equals(ALL) || packageName.equalsIgnoreCase(packageApp.packageName))
                    model.addElement(packageApp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        listApps.setSelectedIndex(0);
    }


    private Box createButtonBox() {
        Box box = Box.createHorizontalBox();
        box.add(Box.createGlue());

        launchButton.addActionListener(e -> {
                PackageApp packageApp = listApps.getSelectedValue();
                if (packageApp != null) {
                    try {
                        new PackageAppThread(packageApp).start();

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Launch failed because: " + ex.getMessage());
                    }
                }
            });
        box.add(launchButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
//				setVisible(false);
                mainDialog.dispose();
            });
        box.add(Box.createGlue());
        box.add(closeButton);
        box.add(Box.createGlue());
        return box;
    }

    List<PackageApp> getPackageApps() {
        List<PackageApp> packageApps = new ArrayList<>();
        List<String> dirs = AddOnManager.getBeastDirectories();
        for (String jarDirName : dirs) {
            File versionFile = new File(jarDirName + "/version.xml");
            if (versionFile.exists() && versionFile.isFile()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document doc;
                try {
                    doc = factory.newDocumentBuilder().parse(versionFile);
                    doc.normalize();
                    // get addonapp info from version.xml
                    Element addon = doc.getDocumentElement();
                    NodeList nodes = doc.getElementsByTagName("addonapp");
                    for (int j = 0; j < nodes.getLength(); j++) {
                        Element addOnAppElement = (Element) nodes.item(j);
                        PackageApp packageApp = new PackageApp();
                        packageApp.packageName = addon.getAttribute("name");
                        packageApp.jarDir = jarDirName;
                        packageApp.className = addOnAppElement.getAttribute("class");
                        packageApp.description = addOnAppElement.getAttribute("description");
                        packageApp.argumentsString = addOnAppElement.getAttribute("args");

                        String iconLocation = addOnAppElement.getAttribute("icon");
                        packageApp.icon = Utils.getIcon(iconLocation);
                        if (packageApp.icon == null || iconLocation.trim().isEmpty())
                            packageApp.icon = Utils.getIcon(DEFAULT_ICON);

                        packageApps.add(packageApp);
                    }
                } catch (Exception e) {
                    // ignore
                    System.err.println(e.getMessage());
                }
            }
        }
        return packageApps;
    }

    /**
     * package application information required for launching the app and
     * displaying in list box
     **/
    class PackageApp {
        String packageName;
        String jarDir;
        String description;
        String className;
        String argumentsString;
        ImageIcon icon;

        public String[] getArgs() {
            if (argumentsString == null || argumentsString.trim().isEmpty()) {
                return new String[]{};
            } else {
                String[] args = argumentsString.split(" ", -1);
//                System.out.println("package = " + packageName + ", class = " + className + ", args = " + Arrays.toString(args));
                return args;
            }
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /** thread for launching add on application **/
    class PackageAppThread extends Thread {
        PackageApp packageApp;

        PackageAppThread(PackageApp packageApp) {
            this.packageApp = packageApp;
        }

        @Override
        public void run() {
            // invoke package application
//            AppStore.runAppFromJar(packageApp.className, packageApp.getArgs());
            runAppFromCMD(packageApp, null);
        }
    }

    public void runAppFromCMD(PackageApp packageApp, String[] additionalArgs) {
        try {
            AddOnManager.loadExternalJars();

            List<String> cmd = new ArrayList<>();
            if (System.getenv("JAVA_HOME") != null) {
                cmd.add(System.getenv("JAVA_HOME") + File.separatorChar
                        + "bin" + File.separatorChar + "java");
            } else
                cmd.add("java");
            // TODO: deal with java directives like -Xmx -Xms here

            if (System.getProperty("java.library.path") != null && System.getProperty("java.library.path").length() > 0) {
            	cmd.add("-Djava.library.path=" + sanitise(System.getProperty("java.library.path")));
            }
            cmd.add("-cp");
            final String strClassPath = sanitise(System.getProperty("java.class.path"));
            cmd.add(strClassPath);
            cmd.add(packageApp.className);

            for (String arg : packageApp.getArgs()) {
                cmd.add(arg);
            }

            if (additionalArgs != null) {
                for (String arg : additionalArgs)
                    cmd.add(arg);
            }


            final ProcessBuilder pb = new ProcessBuilder(cmd);

            System.err.println(pb.command());

            //File log = new File("log");
            pb.redirectErrorStream(true);
            
            // Start the process and wait for it to finish.
            final Process process = pb.start();
            int c;
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((c = input.read()) != -1) {
                Log.info.print((char)c);
            }
            input.close();
            final int exitStatus = process.waitFor();

            if (exitStatus != 0) {
                Log.err.println(Utils.toString(process.getErrorStream()));
            } else {
//                System.out.println(Utils.toString(process.getInputStream()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
	private String sanitise(String property) {
		// sanitise for windows
		if (beast.app.util.Utils.isWindows()) {
			String cwd = System.getProperty("user.dir");
			cwd = cwd.replace("\\", "/");
			property = property.replaceAll(";\\.", ";" +  cwd + ".");
			property = property.replace("\\", "/");
		}
		return property;
	}

    private void printUsage(PrintStream ps) {
        ps.println("\nAppStore: Run installed BEAST 2 package apps.\n" +
                        "\n" +
                        "Usage:\n" +
                        "\tappstore\n" +
                        "\tappstore -help\n" +
                        "\tappstore -list [package_name]\n" +
                        "\tappstore <app_class_name|app_description>");
    }

    private void printAppList(List<PackageApp> appList, PrintStream ps) {
        ps.println("Package         | Class Name      | Description");
        ps.println("----------------|-----------------|-----------------");
        for (PackageApp app : appList) {
            String[] fullClassName = app.className.split("\\.");
            String className = fullClassName[fullClassName.length-1];
            ps.format("%-15.15s | %-15.15s | %s\n",
                    app.packageName, className, app.description);
        }
    }

    public static void main(String[] args) {
        AppStore appStore = new AppStore();

        if (args.length == 0) {
        	Utils.loadUIManager();
            SwingUtilities.invokeLater(() -> appStore.launchGUI().setVisible(true));
        } else {

            if (args[0].startsWith("-")) {
                switch(args[0]) {
                    case "-help":
                        appStore.printUsage(System.out);
                        System.exit(0);

                    case "-list":
                        System.out.println("\nAvailable package apps:\n");
                        if (args.length>1) {
                            String packageNameFilter = args[1].toLowerCase();

                            List<PackageApp> filteredAppList = new ArrayList<>();
                            for (PackageApp app : appStore.getPackageApps()) {
                                if (!app.packageName.toLowerCase().contains(packageNameFilter))
                                    filteredAppList.add(app);
                            }
                            appStore.printAppList(filteredAppList, System.out);
                        } else {
                            appStore.printAppList(appStore.getPackageApps(), System.out);
                        }
                        System.exit(0);

                    default:
                        System.err.print("\nUnsupported option.");
                        appStore.printUsage(System.err);
                        System.exit(1);
                }
            } else {

                // Find apps with class name or description that matches
                // command line.
                List<PackageApp> partialMatchingApps = new ArrayList<>();
                PackageApp exactMatchApp = null;
                for (PackageApp app : appStore.getPackageApps()) {
                    if (app.className.equals(args[0]) || app.description.equals(args[0]))
                        exactMatchApp = app;
                    else {
                        if (app.className.toLowerCase().contains(args[0].toLowerCase())
                                || app.description.toLowerCase().contains(args[0].toLowerCase()))
                            partialMatchingApps.add(app);
                    }
                }

                String[] packageArgs = Arrays.copyOfRange(args, 1, args.length);

                if (exactMatchApp != null)
                    appStore.runAppFromCMD(exactMatchApp, packageArgs);
                else {
                    if (partialMatchingApps.size()==1) {
                        appStore.runAppFromCMD(partialMatchingApps.get(0), packageArgs);
                    } else {
                        if (partialMatchingApps.isEmpty()) {
                            System.err.println("\nNo apps match.");
                            appStore.printUsage(System.err);
                            System.exit(1);
                        } else {
                            System.err.println("\nMultiple apps match:\n");
                            appStore.printAppList(partialMatchingApps, System.err);
                        }
                        System.exit(1);
                    }
                }
            }
        }
    }
}
