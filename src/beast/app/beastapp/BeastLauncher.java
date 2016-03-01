package beast.app.beastapp;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;

import javax.swing.JOptionPane;

import beast.app.BEASTVersion;
import beast.app.util.Utils6;


/**
 * Loads beast.jar and launches BEAST through the BEASTMain class
 * 
 * This class should be compiled against 1.6 and packaged by itself. The
 * remainder of BEAST can be compiled against Java 1.8
 * **/
public class BeastLauncher {

	public static void main(String[] args) throws NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		if (javaVersionCheck("BEAST")) {
			loadBEASTJars();
			BeastMain.main(args);
		}
	}

	/**
	 * Load jars. The path is relative to the parent directory of the jar
	 * containing this class, taking the lib directory. This is meant only to
	 * load beast.jar and perhaps some other libraries, not all packages.
	 **/
	static protected void loadBEASTJars() throws IOException, NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		BeastLauncher clu = new BeastLauncher();

		// first try beast from the package_user_dir/lib/beast.jar
		String beastUserDir = getPackageUserDir();
		String pathDelimiter = isWindows() ? "\\\\" : "/";
		beastUserDir +=  pathDelimiter + "BEAST" + pathDelimiter;
		String beastJar = beastUserDir + "lib";
		boolean foundOne = checkForBEAST(new File(beastJar), clu);
		
		String launcherJar = clu.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		// deal with special characters and spaces in path
		launcherJar = URLDecoder.decode(launcherJar, "UTF-8");
		System.err.println("jardir = " + launcherJar);
		File jarDir0 = new File(launcherJar).getParentFile();
		while ((!foundOne) && (jarDir0 != null)) { // && jarDir0.exists() &&
											// jarDir0.isDirectory()) {
			foundOne = checkForBEAST(jarDir0, clu);
			foundOne = foundOne ||
			    checkForBEAST((isWindows() ? new File(jarDir0.getAbsolutePath() + "\\lib") : new File(jarDir0.getAbsolutePath() + "/lib")), clu);
			
			if (foundOne) {
				createBeastPackage(jarDir0, pathDelimiter);
			}
			
			jarDir0 = jarDir0.getParentFile();
		}
		
		if (!foundOne) {
			System.err.println("WARNING: could not find beast.jar");
			// if beast.jar or its classes are not already in the class path (as is when launched e.g. as developer)
			// the next line will fail
		}
		
		// initialise beast.jar
        Method method = Class.forName("beast.evolution.alignment.Alignment").getMethod("findDataTypes");
        method.invoke(null);

	}

	private static void createBeastPackage(File jarDir0, String pathDelimiter) {
		try {
			// create package user dir, if it not already exists
	        File dir = new File(getPackageUserDir() + pathDelimiter + "BEAST" + pathDelimiter + "lib");
	        if (!dir.exists()) {
	            if (!dir.mkdirs()) {
	            	// cannot create dir, let alone create a beast package
	            	return;
	            }
	        }
	        
	        File beastJar = new File(jarDir0 + pathDelimiter + "lib" + pathDelimiter + "beast.jar");
	        File target = new File(dir + pathDelimiter + "beast.jar");
	        copyFileUsingStream(beastJar, target);
	        
	        String version = "<addon name='BEAST' version='" + (new BEASTVersion()).getVersion() + "'>\n" +
	        		"</addon>";
	        FileWriter outfile = new FileWriter(getPackageUserDir() + pathDelimiter + "beast" + pathDelimiter + "version.xml");
	        outfile.write(version);
	        outfile.close();

	        File beastSrcJar = new File(jarDir0 + pathDelimiter + "beast.src.jar");
	        File srcTarget = new File(dir + pathDelimiter + "beast.src.jar");
	        copyFileUsingStream(beastSrcJar, srcTarget);

	        // TODO: include templates?
	        // if so, how to prevent clashes with templates in package and in installation dir?
	        // TODO: what about examples?
		} catch (Exception e) {
			// do net let exceptions hold up launch of beast & friends
			e.printStackTrace();
		}

	}
	
	// copy files using Java 6 code
	private static void copyFileUsingStream(File source, File dest) throws IOException {
	    InputStream is = null;
	    OutputStream os = null;
	    try {
	        is = new FileInputStream(source);
	        os = new FileOutputStream(dest);
	        byte[] buffer = new byte[1024];
	        int length;
	        while ((length = is.read(buffer)) > 0) {
	            os.write(buffer, 0, length);
	        }
	    } finally {
	        is.close();
	        os.close();
	    }
	}
	private static boolean checkForBEAST(File jarDir, Object clu) throws IOException {
		System.err.println("Checking out " + jarDir.getAbsolutePath());
		boolean foundOne = false;
		if (jarDir.exists()) {
			URL url = new URL("file://" + (isWindows() ? "/" : "") + jarDir.getAbsolutePath() + "/beast.jar");
			if (new File(jarDir.getAbsoluteFile()+File.separator+"beast.jar").exists()) {
				URLClassLoader sysLoader = (URLClassLoader) clu.getClass().getClassLoader();
				Class<?> sysclass = URLClassLoader.class;
				try {
					// Parameters
					Class<?>[] parameters = new Class[] { URL.class };
					Method method = sysclass.getDeclaredMethod("addURL", parameters);
					method.setAccessible(true);
					method.invoke(sysLoader, new Object[] { url });
					System.err.println("Loaded URL " + url);
					foundOne = true;
				} catch (Throwable t) {
					t.printStackTrace();
					throw new IOException("Error, could not add URL to system classloader");
				}
		        String classpath = System.getProperty("java.class.path");
		        String jar = url + "";
		        classpath += System.getProperty("path.separator") + jar.substring(5);
		        System.setProperty("java.class.path", classpath);
			}
		}
		return foundOne;
	}

	static boolean isMac() {
		return System.getProperty("os.name").toLowerCase().startsWith("mac");
	}

	static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("windows");
	}

	static boolean isLinux() {
		return System.getProperty("os.name").toLowerCase().startsWith("linux");
	}

	/** make sure we run Java version 8 or better **/
	static protected boolean javaVersionCheck(String app) {
		String javaVersion = System.getProperty("java.version");
		// javaVersion should be something like "1.7.0_25"
		String[] version = javaVersion.split("\\.");
		if (version.length > 2) {
			try {
				int majorVersion = Integer.parseInt(version[1]);
				if (majorVersion <= 7) {
					String JAVA_VERSION_MSG = "<html>" + app + " requires Java version 8,<br>" + "but the current version is " + majorVersion
							+ ".<br><br>" + "You can get Java from <a href='https://www.java.com/en/'>https://www.java.com/</a>.<br><br> "
							+ "Continuing, but expect the unexpected.</html>";
					if (!java.awt.GraphicsEnvironment.isHeadless()) {
						JOptionPane.showMessageDialog(null, JAVA_VERSION_MSG);
					} else {
						JAVA_VERSION_MSG = JAVA_VERSION_MSG.replaceAll("<br>", "\n");
						JAVA_VERSION_MSG = JAVA_VERSION_MSG.replaceAll("<[^<]*>", "");
						System.err.println(JAVA_VERSION_MSG);
					}
					return true;
				}
			} catch (NumberFormatException e) {
				// We only get here if the JVM does not return the expected
				// string format when asked for java.version.
				// hope for the best
			}
			return true;
		}
		// We only get here if the JVM does not return the expected
		// string format when asked for java.version.
		// hope for the best
		return true;
	}

    public static String getPackageUserDir() {
        if (System.getProperty("beast.user.package.dir") != null)
            return System.getProperty("beast.user.package.dir");
        
        if (Utils6.isWindows()) {
            return System.getProperty("user.home") + "\\BEAST\\" + (new BEASTVersion()).getMajorVersion();
        }
        if (Utils6.isMac()) {
            return System.getProperty("user.home") + "/Library/Application Support/BEAST/" + (new BEASTVersion()).getMajorVersion();
        }
        // Linux and unices
        return System.getProperty("user.home") + "/.beast/" + (new BEASTVersion()).getMajorVersion();
    }
}
