package beast.app.beastapp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;

import javax.swing.JOptionPane;

import beast.core.util.Log;


/**
 * Loads beast.jar and launches BEAST through the BEASTMain class
 * 
 * This class should be compiled against 1.6 and packaged by itself. The
 * remained of BEAST can be compiled against Java 1.8
 * **/
public class BeastLauncher {

	public static void main(String[] args) throws Exception {
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
		String launcherJar = clu.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		// deal with special characters and spaces in path
		launcherJar = URLDecoder.decode(launcherJar, "UTF-8");
		Log.warning.println("jardir = " + launcherJar);
		File jarDir0 = new File(launcherJar).getParentFile();
		boolean foundOne = false;
		while ((!foundOne) && (jarDir0 != null)) { // && jarDir0.exists() &&
											// jarDir0.isDirectory()) {
			foundOne = checkForBEAST(jarDir0, clu);
			foundOne = foundOne ||
			    checkForBEAST((isWindows() ? new File(jarDir0.getAbsolutePath() + "\\lib") : new File(jarDir0.getAbsolutePath() + "/lib")), clu);
			
			jarDir0 = jarDir0.getParentFile();
		}
		
		if (!foundOne) {
			Log.warning.println("WARNING: could not find beast.jar");
			// if beast.jar or its classes are not already in the class path (as is when launched e.g. as developer)
			// the next line will fail
		}
		
		// initialise beast.jar
        Method method = Class.forName("beast.evolution.alignment.Alignment").getMethod("findDataTypes");
        method.invoke(null);

	}

	private static boolean checkForBEAST(File jarDir, Object clu) throws IOException {
		Log.warning.println("Checking out " + jarDir.getAbsolutePath());
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
					Log.warning.println("Loaded URL " + url);
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
						Log.warning.println(JAVA_VERSION_MSG);
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

}
