package beast.app.util;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileNameExtensionFilter;

import beast.app.beauti.BeautiPanel;
import beast.app.beauti.BeautiPanelConfig;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public class Utils {

    /**
     * This function takes a file name and an array of extensions (specified
     * without the leading '.'). If the file name ends with one of the extensions
     * then it is returned with this trimmed off. Otherwise the file name is
     * return as it is.
     *
     * @param fileName   String
     * @param extensions String[]
     * @return the trimmed filename
     */
    public static String trimExtensions(String fileName, String[] extensions) {

        String newName = null;

        for (String extension : extensions) {
            final String ext = "." + extension;
            if (fileName.toUpperCase().endsWith(ext.toUpperCase())) {
                newName = fileName.substring(0, fileName.length() - ext.length());
            }
        }

        return (newName != null) ? newName : fileName;
    }

    /**
     * @param caller Object
     * @param name   String
     * @return a named image from file or resource bundle.
     */
    public static Image getImage(Object caller, String name) {

        java.net.URL url = caller.getClass().getResource(name);
        if (url != null) {
            return Toolkit.getDefaultToolkit().createImage(url);
        } else {
            if (caller instanceof Component) {
                Component c = (Component) caller;
                Image i = c.createImage(100, 20);
                Graphics g = c.getGraphics();
                g.drawString("Not found!", 1, 15);
                return i;
            } else return null;
        }
    }

    public static File getCWD() {
        final String f = System.getProperty("user.dir");
        return new File(f);
    }


    public static void loadUIManager() {

        if (isMac()) {
            System.setProperty("apple.awt.graphics.UseQuartz", "true");
            System.setProperty("apple.awt.antialiasing", "true");
            System.setProperty("apple.awt.rendering", "VALUE_RENDER_QUALITY");

            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.draggableWindowBackground", "true");
            System.setProperty("apple.awt.showGrowBox", "true");

            LookAndFeel laf = UIManager.getLookAndFeel();

            try {

                try {
                    // We need to do this using dynamic class loading to avoid other platforms
                    // having to link to this class. If the Quaqua library is not on the classpath
                    // it simply won't be used.
                    Class<?> qm = Class.forName("ch.randelshofer.quaqua.QuaquaManager");
                    Method method = qm.getMethod("setExcludedUIs", Set.class);

                    Set<String> excludes = new HashSet<String>();
                    excludes.add("Button");
                    excludes.add("ToolBar");
                    method.invoke(null, excludes);

                } catch (Throwable e) {
                }

                //set the Quaqua Look and Feel in the UIManager
                UIManager.setLookAndFeel(
                        "ch.randelshofer.quaqua.QuaquaLookAndFeel"
                );

                UIManager.put("SystemFont", new Font("Lucida Grande", Font.PLAIN, 13));
                UIManager.put("SmallSystemFont", new Font("Lucida Grande", Font.PLAIN, 11));


            } catch (Exception e) {
                System.err.println(e.getMessage());
                try {
                    UIManager.setLookAndFeel(laf);
                } catch (UnsupportedLookAndFeelException e1) {
                    e1.printStackTrace();
                }
            }

        }

// APART FROM THE ABOVE CODE FOR OLD MAC OS X, WE SHOULD LEAVE THE UIManager to the defaults, rather than mess it up
// DEFAULT is almost always the most appropriate thing to use!
//        try {
//
//            if (!lafLoaded) {
//            	if (System.getProperty("beast.laf") != null && !System.getProperty("beast.laf").equals("")) {
//                    UIManager.setLookAndFeel(System.getProperty("beast.laf"));
//            	} else if (isMac()) {
//                   	UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
//                } else { // If Windows or Linux
//                    try {
//                        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
//                    } catch (Exception e) {
//                        UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
//                    }
//                }
//            }
//        } catch (Exception e) {
//        }
    }

    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac");
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().startsWith("linux");
    }

    public static File getLoadFile(String message) {
        return getLoadFile(message, null, null, (String[]) null);
    }

    public static File getSaveFile(String message) {
        return getSaveFile(message, null, null, (String[]) null);
    }

    public static File getLoadFile(String message, File defaultFileOrDir, String description, final String... extensions) {
        File[] files = getFile(message, true, defaultFileOrDir, false, description, extensions);
        if (files == null) {
            return null;
        } else {
            return files[0];
        }
    }

    public static File getSaveFile(String message, File defaultFileOrDir, String description, final String... extensions) {
        File[] files = getFile(message, false, defaultFileOrDir, false, description, extensions);
        if (files == null) {
            return null;
        } else {
            return files[0];
        }
    }

    public static File[] getLoadFiles(String message, File defaultFileOrDir, String description, final String... extensions) {
        return getFile(message, true, defaultFileOrDir, true, description, extensions);
    }

    public static File[] getSaveFiles(String message, File defaultFileOrDir, String description, final String... extensions) {
        return getFile(message, false, defaultFileOrDir, true, description, extensions);
    }

    public static File[] getFile(String message, boolean bLoadNotSave, File defaultFileOrDir, boolean bAllowMultipleSelection, String description, final String... extensions) {
        if (isMac()) {
            java.awt.Frame frame = new java.awt.Frame();
            java.awt.FileDialog chooser = new java.awt.FileDialog(frame, message,
                    (bLoadNotSave ? java.awt.FileDialog.LOAD : java.awt.FileDialog.SAVE));
            if (defaultFileOrDir != null) {
                if (defaultFileOrDir.isDirectory()) {
                    chooser.setDirectory(defaultFileOrDir.getAbsolutePath());
                } else {
                    chooser.setDirectory(defaultFileOrDir.getParentFile().getAbsolutePath());
                    chooser.setFile(defaultFileOrDir.getName());
                }
            }
            if (description != null) {
                FilenameFilter filter = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        for (int i = 0; i < extensions.length; i++) {
                            if (name.toLowerCase().endsWith(extensions[i].toLowerCase())) {
                                return true;
                            }
                        }
                        return false;
                    }
                };
                chooser.setFilenameFilter(filter);
            }

            chooser.setMultipleMode(bAllowMultipleSelection);
            chooser.setVisible(true);
            if (chooser.getFile() == null) return null;
            if (bAllowMultipleSelection) {
            	return chooser.getFiles();
            }
            File file = new java.io.File(chooser.getDirectory(), chooser.getFile());
            chooser.dispose();
            frame.dispose();
            return new File[]{file};
        } else {
            // No file name in the arguments so throw up a dialog box...
            java.awt.Frame frame = new java.awt.Frame();
            frame.setTitle(message);
            final JFileChooser chooser = new JFileChooser(defaultFileOrDir);
            chooser.setMultiSelectionEnabled(bAllowMultipleSelection);
            //chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            if (description != null) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(description, extensions);
                chooser.setFileFilter(filter);
            }

            if (bLoadNotSave) {
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    frame.dispose();
                    if (bAllowMultipleSelection) {
                        return chooser.getSelectedFiles();
                    } else {
                        if (chooser.getSelectedFile() == null) {
                            return null;
                        }
                        return new File[]{chooser.getSelectedFile()};
                    }
                }
            } else {
                if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    frame.dispose();
                    if (bAllowMultipleSelection) {
                        return chooser.getSelectedFiles();
                    } else {
                        if (chooser.getSelectedFile() == null) {
                            return null;
                        }
                        return new File[]{chooser.getSelectedFile()};
                    }
                }
            }
        }
        return null;
    }

    public static String toString(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        reader.close();
        return out.toString();
    }
	public static ImageIcon getIcon(int iPanel, BeautiPanelConfig config) {
	    String sIconLocation = BeautiPanel.ICONPATH + iPanel + ".png";
	    if (config != null) {
	        sIconLocation = BeautiPanel.ICONPATH + config.getIcon();
	    }
	    return Utils.getIcon(sIconLocation);
	}

    /**
     * Retrieve icon.
     *
     * @param sIconLocation location of icon
     * @return icon or null if no icon found
     */
	public static ImageIcon getIcon(String sIconLocation) {
	    try {
	        URL url = (URL) ClassLoader.getSystemResource(sIconLocation);
	        if (url == null) {
//	            System.err.println("Cannot find icon " + sIconLocation);
	            return null;
	        }
	        ImageIcon icon = new ImageIcon(url);
	        return icon;
	    } catch (Exception e) {
	        System.err.println("Cannot load icon " + sIconLocation + " " + e.getMessage());
	        return null;
	    }
	
	}
}
