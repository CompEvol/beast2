package beast.pkgmgmt;


import javax.swing.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.Properties;

/**
 * The utils that work with Java6.
 *
 * Utils6 cannot depend on Utils class, where Utils6 is Java 6 compatible,
 * but Utils works on Java 8 and later.
 **/
public class Utils6 {

    //++++++ Java version
    // Detect or compare the Java major number from a Java version string, such as "1.7.0_25" or "10.0.1".
    public static final int JAVA_1_8 = 8;
    public static final int JAVA_9 = 9;

    /**
     * Get the current Java version from "java.version".
     * @return The Java version.
     */
    public static String getCurrentJavaVersion() {
        return System.getProperty("java.version");
    }

    /**
     * Compare the current Java major version to a given version.
     * @param javaVersion an integer of major version.
     * @return True, if current >= javaVersion.
     */
    public static boolean isMajorAtLeast(int javaVersion) {
        int currentVersion = getMajorJavaVersion();
        if (currentVersion < 2 || javaVersion < 2)
            throw new IllegalArgumentException("Java major version " + currentVersion + " or " +
                    javaVersion + " is not recognised !");
        return currentVersion >= javaVersion;
    }

    /**
     * Compare the current Java major version to a given version.
     * @param javaVersion an integer of major version.
     * @return True, if current < javaVersion.
     */
    public static boolean isMajorLower(int javaVersion) {
        int currentVersion = getMajorJavaVersion();
        if (currentVersion < 2 || javaVersion < 2)
            throw new IllegalArgumentException("Java major version " + currentVersion + " or " +
                    javaVersion + " is not recognised !");
        return currentVersion < javaVersion;
    }

    /**
     * parse a Java version string to an integer of major version like 7, 8, 9, 10, ...
     */
    public static int getMajorJavaVersion() {
        String javaVersion = getCurrentJavaVersion();
        // javaVersion should be something like "1.7.0_25"
        String[] version = javaVersion.split("\\.");
        if (version.length > 2) {
            int majorVersion = Integer.parseInt(version[0]);
            if (majorVersion == 1) {
                majorVersion = Integer.parseInt(version[1]);
            }
            return majorVersion;
        } else if (javaVersion.contains("-")) {
        	version = javaVersion.split("-");
            int majorVersion = Integer.parseInt(version[0]);
            
            return majorVersion;
        }
        try {
            int majorVersion = Integer.parseInt(javaVersion);
            return majorVersion;
        } catch (NumberFormatException e) {
            // ignore
        }
        return -1;
    }

    //++++++ Graphics

    public static class Canvas extends JComponent {
		private static final long serialVersionUID = 1L;
		
		Image imageBuffer;
        public Canvas() { }

        @Override
		public void paintComponent( Graphics g ) {
            // copy buffered image
            if ( imageBuffer != null )
                g.drawImage(imageBuffer, 0,0, this);
        }

        /**
         Get a buffered (persistent) image for drawing on this component
         */
        public Graphics getBufferedGraphics() {
            Dimension dim = getSize();
            imageBuffer = createImage( dim.width, dim.height );
            return imageBuffer.getGraphics();
        }

        @Override
		public void setBounds( int x, int y, int width, int height ) {
            setPreferredSize( new Dimension(width, height) );
            setMinimumSize( new Dimension(width, height) );
            super.setBounds( x, y, width, height );
        }
    }

    //Splash
    static Window splashScreen;
    static Canvas can;
    /*
        This could live in the desktop script.
        However we'd like to get it on the screen as quickly as possible.
    */
    public static void startSplashScreen()
    {
    	
    	ImageIcon icon = getIcon("beast/pkgmgmt/icons/beauti.png");
        Image img = icon == null ? new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB): icon.getImage();
        int width=2 * img.getWidth(null), height=img.getHeight(null);
        Window win=new Window( new Frame() );
        win.pack();
        can = new Canvas();
        can.setSize( width, height ); // why is this necessary?
        Toolkit tk=Toolkit.getDefaultToolkit();
        Dimension dim=tk.getScreenSize();
        win.setBounds(
                dim.width/2-width/2, dim.height/2-height/2, width, height );
        win.add("Center", can);
//        Image img=tk.getImage(
//                Utils.class.getResource("beast.png") ); //what
        MediaTracker mt=new MediaTracker(can);
        mt.addImage(img,0);
        try { mt.waitForAll(); } catch ( Exception e ) { }
        Graphics gr=can.getBufferedGraphics();
        gr.drawImage(img, width / 4, 0, can);
        win.setVisible(true);
        win.toFront();
        splashScreen = win;
    }
    

    public static void endSplashScreen() {
        if ( splashScreen != null ) {
            splashScreen.dispose();
        	can = null;
        	splashScreen = null;
    	}
    }


	public static ImageIcon getIcon(String iconLocation) {
	    try {
	        URL url = Utils6.class.getClassLoader().getResource(iconLocation);
	        if (url == null) {
	        	System.err.println("Cannot find icon " + iconLocation);
	            return null;
	        }
	        ImageIcon icon = new ImageIcon(url);
	        return icon;
	    } catch (Exception e) {
	    	System.err.println("Cannot load icon " + iconLocation + " " + e.getMessage());
	        return null;
	    }
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
    
    
    /**
     * @return directory where to install packages for users *
     */
    public static String getPackageUserDir() {
    	return getPackageUserDir("BEAST");
    }
    
    public static String getPackageUserDir(String application) {
    	String prefix = application.toLowerCase();
        if (System.getProperty(prefix + ".user.package.dir") != null)
            return System.getProperty(prefix + ".user.package.dir");
        
        if (isWindows()) {
            return System.getProperty("user.home") + "\\" + application + "\\" + BEASTVersion.INSTANCE.getMajorVersion();
        }
        if (isMac()) {
            return System.getProperty("user.home") + "/Library/Application Support/" + application + "/" + BEASTVersion.INSTANCE.getMajorVersion();
        }
        // Linux and unices
        return System.getProperty("user.home") + "/." + prefix + "/" + BEASTVersion.INSTANCE.getMajorVersion();
    }

    /**
     * @return directory where system wide packages reside *
     */
    public static String getPackageSystemDir() {
    	return getPackageSystemDir("BEAST");
    }
    
    public static String getPackageSystemDir(String application) {
    	String prefix = application.toLowerCase();        
        if (System.getProperty(prefix + ".system.package.dir") != null)
            return System.getProperty(prefix + ".system.package.dir");
        
        if (isWindows()) {
            return "\\Program Files\\" + application + "\\" + BEASTVersion.INSTANCE.getMajorVersion();
        }
        if (isMac()) {
            return "/Library/Application Support/" + application +"/" + BEASTVersion.INSTANCE.getMajorVersion();
        }
        return "/usr/local/share/" + prefix + "/" + BEASTVersion.INSTANCE.getMajorVersion();
    }
    

	/**
	 * Get value from beauti.properties file
	 */
	static public String getBeautiProperty(String key) {
        File beastProps = new File(getPackageUserDir() + "/beauti.properties");
        if (beastProps.exists()) {
            Properties props = new Properties();

            try {
                //load a properties file
                props.load(new FileInputStream(beastProps));
                return props.getProperty(key);

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return null;
	}
	
	/**
	 * Set property value in beauti.properties file
	 * if value == null, the property will be removed
	 */
	static public void saveBeautiProperty(String key, String value) {
        File propsFile = new File(getPackageUserDir() + "/beauti.properties");
        Properties prop = new Properties();

        //Load or create properties file
        if (propsFile.exists()) {
            try {
                prop.load(new FileInputStream(propsFile));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            try {
                propsFile.createNewFile();
            } catch (IOException ex) {
                System.err.println("saveBeautiProperty " + ex.getClass().getName() + " " + ex.getMessage());
                return;
            }
        }
        
        // set or remove value
        if (value != null) {
        	prop.setProperty(key, value);
        } else {
        	prop.remove(key);
        }
        
        // Write properties file
        try {
            prop.store(new FileOutputStream(propsFile),
                    "Automatically-generated by " + BEASTVersion.INSTANCE.getProgramName() + ".\n");
        } catch (IOException ex) {
        	System.err.println(ex.getMessage());
        }
	}
	
    public static void logToSplashScreen(String msg) {
    	if (Utils6.splashScreen != null) {
    		try {
    	    	ImageIcon icon = getIcon("beast/pkgmgmt/icons/beauti.png");
    	        Image img = icon == null ? new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB): icon.getImage();
	            Graphics gr = Utils6.can.getBufferedGraphics();
	            gr.drawImage(img, Utils6.can.getWidth() / 4, 0, Utils6.can);
	            gr.drawString(msg, 1, Utils6.can.getHeight() - 3);
	            Utils6.can.repaint();
    		} catch (java.lang.NoSuchFieldError e) {
    			// we are dealing with an older (pre v2.5.6) version of the launcher
    			// so no feedback
    		}
    		
    	}
    }

    public static boolean isJUnitTest() {
  	  for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
  	    if (element.getClassName().startsWith("org.junit.")) {
  	    	// we are in a junit test
  	    	return true;
  	    }
  	    if (element.getClassName().startsWith("org.assertj.")) {
  	    	// we are in a assertj-swing unit test
  	      	return true;
  	    }           
  	    if (element.getClassName().startsWith("org.fest.")) {
  	    	// we are in a fest-swing unit test
  	      	return true;
  	    }           
  	  }
  	  return false;
  }    


}
