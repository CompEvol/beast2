package beast.app.util;



import jam.framework.Application;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import beast.base.core.Log;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.likelihood.BeagleTreeLikelihood;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.JukesCantor;
import beast.base.evolution.tree.TreeParser;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.Utils6;

import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
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

        java.net.URL url = BEASTClassLoader.classLoader.getResource(name);
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
                    Class<?> qm = BEASTClassLoader.forName("ch.randelshofer.quaqua.QuaquaManager");
                    Method method = qm.getMethod("setExcludedUIs", Set.class);

                    Set<String> excludes = new HashSet<>();
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
            	Log.warning.println(e.getMessage());
                try {
                    UIManager.setLookAndFeel(laf);
                } catch (UnsupportedLookAndFeelException e1) {
                    e1.printStackTrace();
                }
            }

        } else {
            try {
                // Set System L&F
            	// this is supposed to look OK on high res screens
            	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            } catch (UnsupportedLookAndFeelException |ClassNotFoundException | InstantiationException |IllegalAccessException e) {
            	Log.warning.println(e.getMessage());
            }
        }
        
    	// change font size, if specified in beauti.properties file
    	String fontsize = getBeautiProperty("fontsize");
    	if (fontsize != null) {
    		try {
    			setFontSize(Integer.parseInt(fontsize));
    		} catch (NumberFormatException e) {
    			// ignore if fontsize is improperly formatted.
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

    public static File[] getFile(String message, boolean isLoadNotSave, File defaultFileOrDir, boolean allowMultipleSelection, String description, final String... extensions) {
        if (isMac()) {
            java.awt.Frame frame = new java.awt.Frame();
            java.awt.FileDialog chooser = new java.awt.FileDialog(frame, message,
                    (isLoadNotSave ? java.awt.FileDialog.LOAD : java.awt.FileDialog.SAVE));
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

            chooser.setMultipleMode(allowMultipleSelection);
            chooser.setVisible(true);
            if (chooser.getFile() == null) return null;
            if (allowMultipleSelection) {
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
            chooser.setMultiSelectionEnabled(allowMultipleSelection);
            //chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            if (description != null && extensions.length > 1 && extensions[0].length() > 0) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(description, extensions);
                chooser.setFileFilter(filter);
            }

            if (isLoadNotSave) {
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    frame.dispose();
                    if (allowMultipleSelection) {
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
                    if (allowMultipleSelection) {
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
    
//	public static ImageIcon getIcon(int panelIndex, BeautiPanelConfig config) {
//	    String iconLocation = BeautiPanel.ICONPATH + panelIndex + ".png";
//	    if (config != null) {
//	        iconLocation = BeautiPanel.ICONPATH + config.getIcon();
//	    }
//	    return Utils.getIcon(iconLocation);
//	}

    /**
     * Retrieve icon.
     *
     * @param iconLocation location of icon
     * @return icon or null if no icon found
     */
	public static ImageIcon getIcon(String iconLocation) {
	    try {
	        URL url = BEASTClassLoader.classLoader.getResource(iconLocation);
	        if (url == null) {
//	            System.err.println("Cannot find icon " + iconLocation);
	            return null;
	        }
	        ImageIcon icon = new ImageIcon(url);
	        return icon;
	    } catch (Exception e) {
	    	Log.warning.println("Cannot load icon " + iconLocation + " " + e.getMessage());
	        return null;
	    }
	
	}

    /**
     * Used to detect whether CUDA with BEAGLE is installed on OS X in {@link Utils6#testCudaStatusOnMac()},
     * which is used by {@link beast.app.util.BeastLauncher#main(String[])}.
     * @see <a href="https://github.com/CompEvol/beast2/issues/500">issues 500</a>.
     */
    public static void main(String[] args) {
		try {
			Sequence a = new Sequence("A", "A");
	        Sequence b = new Sequence("B", "A");
	        Sequence c = new Sequence("C", "A");
	        Sequence d = new Sequence("D", "A");

	        Alignment data = new Alignment();
	        data.initByName("sequence", a, "sequence", b, "sequence", c, "sequence", d, "dataType", "nucleotide");

	        TreeParser tree = new TreeParser();
	        tree.initByName("taxa", data,
	                "newick", "(((A:1,B:1):1,C:2):1,D:3)",
	                "IsLabelledNewick", true);

	        JukesCantor JC = new JukesCantor();
	        JC.initAndValidate();

	        SiteModel siteModel = new SiteModel();
	        siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", JC);

	    	BeagleTreeLikelihood likelihood = new BeagleTreeLikelihood();
	        likelihood.initByName("data", data, "tree", tree, "siteModel", siteModel);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


    	System.out.println("Success");
    	// if we got this far, exit with status 0
		System.exit(0);
	}

	static public void setFontSize(int fontSize) {
	     // Setup font size based on screen size
		for (String item : new String[]{"Button.font", "ToggleButton.font", "RadioButton.font", 
				"ColorChooser.font", "List.font", "MenuBar.font", "MenuItem.font", 
				"RadioButtonMenuItem.font", "CheckBoxMenuItem.font", "Menu.font", "PopupMenu.font", "OptionPane.font", 
				"Panel.font", "ProgressBar.font", "ScrollPane.font", "Viewport.font", "TabbedPane.font",  
				"TableHeader.font", "PasswordField.font", 
				"EditorPane.font", "TitledBorder.font", "ToolBar.font", "ToolTip.font", "Tree.font",
				"ComboBox.font", "CheckBox.font", "Label.font", "Table.font", "TextField.font", "TextArea.font", "TextPane.font"}) {
			Font font = UIManager.getFont(item);
			UIManager.put(item, new Font(font.getName(), font.getStyle(), fontSize));
		}
	    Log.debug.println("Font is now at size " + fontSize);
	}

	//++++++ dependency on Utils6

	/**
	 * Get value from beauti.properties file
	 */
	static public String getBeautiProperty(String key) {
		return Utils6.getBeautiProperty(key);
	}
	
	/**
	 * Set property value in beauti.properties file
	 * if value == null, the property will be removed
	 */
	static public void saveBeautiProperty(String key, String value) {
		Utils6.saveBeautiProperty(key, value);
	}
	
    public static void logToSplashScreen(String msg) {
    	Utils6.logToSplashScreen(msg);
    }


    //++++++ Mac OS only

    public static void macOSXRegistration(Application application) {
        if (isMac()) {
            NewOSXAdapter newOSXAdapter = new NewOSXAdapter(application);
            try {
                newOSXAdapter.registerMacOSXApplication(application);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                System.err.println("Exception while loading the OSXAdapter:");
                e.printStackTrace();
            }
        }

    }


}
