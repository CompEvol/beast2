package beast.app.util;


import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

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
        boolean lafLoaded = false;

        if (jam.mac.Utils.isMacOSX()) {
            System.setProperty("apple.awt.graphics.UseQuartz", "true");
            System.setProperty("apple.awt.antialiasing","true");
            System.setProperty("apple.awt.rendering","VALUE_RENDER_QUALITY");

            System.setProperty("apple.laf.useScreenMenuBar","true");
            System.setProperty("apple.awt.draggableWindowBackground","true");
            System.setProperty("apple.awt.showGrowBox","true");

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

                }
                catch (Throwable e) {
                }

                //set the Quaqua Look and Feel in the UIManager
                UIManager.setLookAndFeel(
                        "ch.randelshofer.quaqua.QuaquaLookAndFeel"
                );
                lafLoaded = true;

            } catch (Exception e) {

            }

            UIManager.put("SystemFont", new Font("Lucida Grande", Font.PLAIN, 13));
            UIManager.put("SmallSystemFont", new Font("Lucida Grande", Font.PLAIN, 11));
        }
        
        try {

            if (!lafLoaded) {
                UIManager.setLookAndFeel("javax.swing.plaf.metal");
                		//UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception e) {
        }
    }

    public static boolean isMac() {
        return jam.mac.Utils.isMacOSX();//System.getProperty("os.name").startsWith("Windows");
     }
    public static boolean isWindows() {
       return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }
    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().startsWith("linux");
     }

}
