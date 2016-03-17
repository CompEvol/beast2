package beast.app.util;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

import beast.app.beauti.BeautiPanel;
import beast.app.beauti.BeautiPanelConfig;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.likelihood.BeagleTreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.JukesCantor;
import beast.util.AddOnManager;
import beast.util.TreeParser;

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

            if (description != null) {
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
	public static ImageIcon getIcon(int panelIndex, BeautiPanelConfig config) {
	    String iconLocation = BeautiPanel.ICONPATH + panelIndex + ".png";
	    if (config != null) {
	        iconLocation = BeautiPanel.ICONPATH + config.getIcon();
	    }
	    return Utils.getIcon(iconLocation);
	}

    /**
     * Retrieve icon.
     *
     * @param iconLocation location of icon
     * @return icon or null if no icon found
     */
	public static ImageIcon getIcon(String iconLocation) {
	    try {
	        URL url = ClassLoader.getSystemResource(iconLocation);
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

	
	public static boolean testCudaStatusOnMac() {
		String cudaStatusOnMac = "<html>It appears you have CUDA installed, but your computer hardware does not support it.<br>"
				+ "You need to remove CUDA before BEAST/BEAUti can start.<br>"
				+ "To remove CUDA, delete the following folders by typing in a terminal:<br>"
				+ "rm -r /Library/Frameworks/CUDA.framework<br>"
				+ "rm -r /Developer/NVIDIA<br>"
				+ "rm -r /usr/local/cuda<br>"
				+ "You may need 'sudo rm' instead of 'rm'</html>";
				
		if (isMac()) {
			// check any of these directories exist
			// /Library/Frameworks/CUDA.framework
			// /Developer/NVIDIA
			// /usr/local/cuda
			if (new File("/Library/Frameworks/CUDA.framework").exists() ||
					new File("/Developer/NVIDIA").exists() ||
					new File("/usr/local/cuda").exists()) {
				// there is evidence of CUDA being installed on this computer
				// try to create a BeagleTreeLikelihood using a separate process
				try {
				      String java = System.getenv("java.home");
				      if (java == null) {
				    	  java ="/usr/bin/java";
				      } else {
				    	  java += "/bin/java";
				      }
				      String beastJar = AddOnManager.getPackageUserDir();
				      beastJar += "/" + "BEAST" + "/" + "lib" + "/" + "beast.jar";
				      if (!new File(beastJar).exists()) {
				    	  Log.debug.println("Could not find beast.jar, giving up testCudaStatusOnMac");
				    	  return true;
				      }
				      //beastJar = "\"" + beastJar + "\"";
				      //beastJar = "/Users/remco/workspace/beast2/build/dist/beast.jar";
				      Process p = Runtime.getRuntime().exec(new String[]{java , "-cp" , beastJar , "beast.app.util.Utils"});
				      BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				      String line;
				      while ((line = input.readLine()) != null) {
				        Log.debug.println(line);
				      }
				      input.close();
				      if (p.exitValue() != 0) {
				    	  if (GraphicsEnvironment.isHeadless()) {
				    		  cudaStatusOnMac = cudaStatusOnMac.replaceAll("<br>", "\n");
				    		  cudaStatusOnMac = cudaStatusOnMac.replaceAll("<.?html>","\n");
				    		  Log.warning.println("WARNING: " + cudaStatusOnMac);
				    	  } else {
				    		  JOptionPane.showMessageDialog(null, cudaStatusOnMac);
				    	  }
				    	  return false;
				      }
				    }
				    catch (Exception err) {
				      err.printStackTrace();
				    }
			}
			
		}
		return true;
	}
	
	
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

}
