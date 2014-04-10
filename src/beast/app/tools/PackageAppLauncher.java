package beast.app.tools;

import beast.app.beauti.BeautiPanel;
import beast.util.AddOnManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;



/**
 * launch applications specific to add-ons installed, for example utilities for
 * post-processing add-on specific data.
 *
 * @author  Remco Bouckaert
 * @author  Walter Xie
 */
public class PackageAppLauncher extends JDialog {
    private static final long serialVersionUID = 1L;
    public static final String DEFAULT_ICON = "beast/app/tools/images/utility.png";

    JComboBox packageComboBox = new JComboBox(new String[]{"-all-"});
    DefaultListModel model = new DefaultListModel();
    JList list;
    JButton launchButton = new JButton("Launch");

    public PackageAppLauncher() {
        try {
            AddOnManager.loadExternalJars();
        } catch (Exception e) {
            // ignore
        }
        setTitle("BEAST 2 Package Application Launcher");

        Box top = Box.createHorizontalBox();
        JLabel label = new JLabel("Select a package:");
        label.setLabelFor(packageComboBox);
        top.add(label);
        top.add(packageComboBox);
        add(BorderLayout.NORTH, top);

        Component pluginListBox = createList();
        add(BorderLayout.CENTER, pluginListBox);

        Box buttonBox = createButtonBox();
        add(buttonBox, BorderLayout.SOUTH);

//        Dimension dim = panel.getPreferredSize();
//        Dimension dim2 = buttonBox.getPreferredSize();
//		setSize(dim.width + 10, dim.height + dim2.height + 30);
        setSize(new Dimension(660, 400));
    }

    private Component createList() {
        Box box = Box.createVerticalBox();
        box.add(Box.createGlue());

        list = new JList(model) {
            //Subclass JList to workaround bug 4832765, which can cause the
            //scroll pane to not let the user easily scroll up to the beginning
            //of the list.  An alternative would be to set the unitIncrement
            //of the JScrollBar to a fixed value. You wouldn't get the nice
            //aligned scrolling, but it should work.
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
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                JLabel label = (JLabel) super
                        .getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setIcon(((PackageApp) value).icon);
                label.setHorizontalTextPosition(JLabel.CENTER);
                label.setVerticalTextPosition(JLabel.BOTTOM);
                return label;
            }
        });
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(-1);
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    launchButton.doClick();
                }
            }
        });

//        if (model.getSize() > 0) { // TODO not working
//            list.setPrototypeCellValue(model.firstElement()); //get extra space
//        }

        resetList();

        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(660, 400));
        listScroller.setAlignmentX(LEFT_ALIGNMENT);

        JLabel label = new JLabel("List of available package applications");
        label.setLabelFor(list);

        box.add(label);
        box.add(listScroller);

        return box;
    }

    private void resetList() {
        model.clear();
        try {
            List<PackageApp> packageApps = getPackageApps();
            for (PackageApp packageApp : packageApps) {
                model.addElement(packageApp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        list.setSelectedIndex(0);
    }

    private Box createButtonBox() {
        Box box = Box.createHorizontalBox();
        box.add(Box.createGlue());

        launchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PackageApp packageApp = (PackageApp) list.getSelectedValue();
                if (packageApp != null) {
                    try {
                        new PackageAppThread(packageApp).start();

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Launch failed because: " + ex.getMessage());
                    }
                }
            }
        });
        box.add(launchButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//				setVisible(false);
                dispose();
            }
        });
        box.add(Box.createGlue());
        box.add(closeButton);
        box.add(Box.createGlue());
        return box;
    }

    List<PackageApp> getPackageApps() {
        List<PackageApp> packageApps = new ArrayList<PackageApp>();
        List<String> dirs = AddOnManager.getBeastDirectories();
        for (String sJarDir : dirs) {
            File versionFile = new File(sJarDir + "/version.xml");
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
                        packageApp.jarDir = sJarDir;
                        packageApp.className = addOnAppElement.getAttribute("class");
                        packageApp.description = addOnAppElement.getAttribute("description");
                        packageApp.defaultArguments = addOnAppElement.getAttribute("args");

                        String iconLocation = addOnAppElement.getAttribute("icon");
                        packageApp.icon = BeautiPanel.getIcon(iconLocation);
                        if (packageApp.icon == null || iconLocation.trim().isEmpty())
                            packageApp.icon = BeautiPanel.getIcon(DEFAULT_ICON);

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
     * add on application information reguired for launching the app and
     * displaying in list box
     **/
    class PackageApp {
        String packageName;
        String jarDir;
        String description;
        String className;
        String defaultArguments;
        ImageIcon icon;

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
            try {
                String command = "java -classpath " +  //TODO
                        System.getProperty("java.class.path") +
//                        " " + packageApp.jarDir + File.separator + "lib" + File.separator + packageApp.packageName + ".addon.jar" +
                        " " + packageApp.className + " " + packageApp.defaultArguments;
                System.out.println(command);
                Process p = Runtime.getRuntime().exec(command);
                BufferedReader pout = new BufferedReader((new InputStreamReader(p.getInputStream())));
                BufferedReader perr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                String line;
                while ((line = pout.readLine()) != null) {
                    System.out.println(line);
                }
                pout.close();
                while ((line = perr.readLine()) != null) {
                    System.err.println(line);
                }
                perr.close();
                p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            PackageAppLauncher dlg = new PackageAppLauncher();
            dlg.setVisible(true);
        } else {
            // invoke add on application
            try {
                AddOnManager.loadExternalJars();

                // call main method through reflection
                // with default arguments
                String className = args[0];
                Class<?> c = Class.forName(className);
                Class<?>[] argTypes = new Class[] { String[].class };
                Method main = c.getDeclaredMethod("main", argTypes);
                String[] args2 = new String[args.length-1];
                for (int i = 1; i < args.length; i++) {
                    args2[i-1] = args[i];
                }
                main.invoke(null, (Object) args2);
            } catch (Exception err) {
                err.printStackTrace();
            }

        }
    }
}
