package beast.app.beastapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import beast.app.BEASTVersion;
import beast.app.util.Utils6;
import beast.util.PackageVersion;

/**
 * Loads beast.jar and launches BEAST through the BEASTMain class
 * 
 * This class should be compiled against 1.6 and packaged by itself. The
 * remainder of BEAST can be compiled against Java 1.8
 **/
public class BeastLauncher {
	public final static String ARCHIVE_DIR = "archive";

	private static String pathDelimiter;

	public static void main(String[] args) throws NoSuchMethodException, SecurityException, ClassNotFoundException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		if (javaVersionCheck("BEAST")) {
			// loadBEASTJars();
			Utils6.testCudaStatusOnMac();
			String classpath = getPath();
			run(classpath, "beast.app.beastapp.BeastMain", args);
			// BeastMain.main(args);
		}
	}

	/**
	 * Load jars. The path is relative to the parent directory of the jar
	 * containing this class, taking the lib directory. This is meant only to
	 * load beast.jar and perhaps some other libraries, not all packages.
	 **/
	static protected void loadBEASTJars() throws IOException, NoSuchMethodException, SecurityException,
			ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		BeastLauncher clu = new BeastLauncher();

		// first try beast from the package_user_dir/lib/beast.jar
		String beastUserDir = getPackageUserDir();
		pathDelimiter = isWindows() ? "\\\\" : "/";
		beastUserDir += pathDelimiter + "BEAST" + pathDelimiter;
		String beastJar = beastUserDir + "lib";
		boolean foundJavaJarFile = checkForBEAST(new File(beastJar), clu);

		String launcherJar = clu.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		// deal with special characters and spaces in path
		launcherJar = URLDecoder.decode(launcherJar, "UTF-8");
		System.err.println("jardir = " + launcherJar);
		File jarDir0 = new File(launcherJar).getParentFile();
		while ((!foundJavaJarFile) && (jarDir0 != null)) {
			foundJavaJarFile = checkForBEAST(jarDir0, clu);
			foundJavaJarFile = foundJavaJarFile
					|| checkForBEAST(new File(jarDir0.getAbsolutePath() + pathDelimiter + "lib"), clu);

			if (foundJavaJarFile) {
				createBeastPackage(jarDir0);
			}

			jarDir0 = jarDir0.getParentFile();
		}

		if (!foundJavaJarFile) {
			System.err.println("WARNING: could not find beast.jar");
			// if beast.jar or its classes are not already in the class path (as
			// is when launched e.g. as developer)
			// the next line will fail
		}

		// initialise beast.jar
		Method method = Class.forName("beast.evolution.alignment.Alignment").getMethod("findDataTypes");
		method.invoke(null);

	}

	private static void createBeastPackage(File jarDir0) {
		try {
			if (jarDir0.toString().toLowerCase().endsWith("lib")) {
				jarDir0 = jarDir0.getParentFile();
			}

			// create package user dir, if it not already exists
			String userDir = getPackageUserDir();
			File dir = new File(userDir + pathDelimiter + "BEAST" + pathDelimiter + "lib");
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					// cannot create dir, let alone create a beast package
					return;
				}
			}
			File exampleDir = new File(
					userDir + pathDelimiter + "BEAST" + pathDelimiter + "examples" + pathDelimiter + "nexus");
			if (!exampleDir.exists()) {
				if (!exampleDir.mkdirs()) {
					// cannot create dir, let alone create a beast package
					return;
				}
			}
			File templateDir = new File(userDir + pathDelimiter + "BEAST" + pathDelimiter + "templates");
			if (!templateDir.exists()) {
				if (!templateDir.mkdirs()) {
					// cannot create dir, let alone create a beast package
					return;
				}
			}

			File beastJar = new File(jarDir0 + pathDelimiter + "lib" + pathDelimiter + "beast.jar");
			File target = new File(dir + pathDelimiter + "beast.jar");
			copyFileUsingStream(beastJar, target);

			String version = "<addon name='BEAST' version='" + BEASTVersion.INSTANCE.getVersion() + "'>\n" + "</addon>";
			FileWriter outfile = new FileWriter(userDir + pathDelimiter + "BEAST" + pathDelimiter + "version.xml");
			outfile.write(version);
			outfile.close();

			File beastSrcJar = new File(jarDir0 + pathDelimiter + "lib" + pathDelimiter + "beast.src.jar");
			File srcTarget = new File(dir + pathDelimiter + "beast.src.jar");
			copyFileUsingStream(beastSrcJar, srcTarget);

			copyFilesInDir(new File(jarDir0 + pathDelimiter + "examples"),
					new File(userDir + pathDelimiter + "BEAST" + pathDelimiter + "examples"));
			copyFilesInDir(new File(jarDir0 + pathDelimiter + "examples" + pathDelimiter + "nexus"), exampleDir);
			copyFilesInDir(new File(jarDir0 + pathDelimiter + "templates"), templateDir);

			// TODO: include templates?
			// if so, how to prevent clashes with templates in package and in
			// installation dir?
			// TODO: what about examples?
		} catch (Exception e) {
			// do net let exceptions hold up launch of beast & friends
			e.printStackTrace();
		}

	}

	private static void copyFilesInDir(File srcDir, File targetDir) throws IOException {
		String targetDirName = targetDir.getAbsolutePath();
		for (File src : srcDir.listFiles()) {
			if (src.isFile()) {
				copyFileUsingStream(src, new File(targetDirName + pathDelimiter + src.getName()));
			}
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
			if (new File(jarDir.getAbsoluteFile() + File.separator + "beast.jar").exists()) {
				File versionFile = new File(jarDir.getParent() + pathDelimiter + "version.xml");
				if (versionFile.exists()) {
					BufferedReader fin = new BufferedReader(new FileReader(versionFile));
					String str = null;
					while (fin.ready()) {
						str += fin.readLine();
					}
					fin.close();

					int start = str.indexOf("version=");
					int end = str.indexOf("'", start + 9);
					String version = str.substring(start + 9, end);
					double localVersion = parseVersion(version);
					double desiredVersion = parseVersion(BEASTVersion.INSTANCE.getVersion());
					if (localVersion < desiredVersion) {
						return false;
					}
				}

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
				int majorVersion = Integer.parseInt(version[0]);
				if (majorVersion == 1) {
					majorVersion = Integer.parseInt(version[1]);
				}
				if (majorVersion != 8) {
					String JAVA_VERSION_MSG = "<html>" + app + " requires Java version 8,<br>"
							+ "but the current version is " + majorVersion + ".<br><br>"
							+ "You can get Java from <a href='https://www.java.com/en/'>https://www.java.com/</a>.<br><br> "
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
			return System.getProperty("user.home") + "\\BEAST\\" + BEASTVersion.INSTANCE.getMajorVersion();
		}
		if (Utils6.isMac()) {
			return System.getProperty("user.home") + "/Library/Application Support/BEAST/"
					+ BEASTVersion.INSTANCE.getMajorVersion();
		}
		// Linux and unices
		return System.getProperty("user.home") + "/.beast/" + BEASTVersion.INSTANCE.getMajorVersion();
	}

	/**
	 * Parse version string, assume it is of the form 1.2.3 returns version
	 * where each sub-version is divided by 100, so 2.0 -> return 2 2.1 return
	 * 2.01 2.2.3 return 2.0103 Letters are ignored, so 2.0.e -> 2.0 2.x.1 ->
	 * 2.0001
	 * 
	 * @return
	 */
	private static double parseVersion(String versionString) {
		// is of the form 1.2.3
		String[] strs = versionString.split("\\.");
		double version = 0;
		double divider = 1.0;
		for (int i = 0; i < strs.length; i++) {
			try {
				version += Double.parseDouble(strs[i]) / divider;
				divider = divider * 100.0;
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return version;
	}

	private static String getPath() {
		StringBuilder buf = new StringBuilder();
		buf.append("\"");
		buf.append(sanitise(System.getProperty("java.library.path")));
		String packagePath = Utils6.getBeautiProperty("package.path");
		if (packagePath == null || packagePath.length() == 0) {
			packagePath = determinePackagePath();
			Utils6.saveBeautiProperty("package.path", packagePath);
		}
		buf.append(packagePath);
		buf.append("\"");
		return buf.toString();
	}

	private static String determinePackagePath() {
		StringBuilder buf = new StringBuilder();
		Set<String> classes = new HashSet<String>();
		for (String jarDirName : getBeastDirectories()) {
			try {
				File versionFile = new File(jarDirName + "/version.xml");
				String packageNameAndVersion = null;
				if (versionFile.exists()) {
					try {
						// print name and version of package
						DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
						Document doc = factory.newDocumentBuilder().parse(versionFile);
						Element addon = doc.getDocumentElement();
						packageNameAndVersion = addon.getAttribute("name") + " v" + addon.getAttribute("version");
						// Log.warning.println("Loading package " +
						// packageNameAndVersion);
						// Utils.logToSplashScreen("Loading package " +
						// packageNameAndVersion);
					} catch (Exception e) {
						// too bad, won't print out any info

						// File is called version.xml, but is not a Beast2
						// version file
						// Log.debug.print("Skipping "+jarDirName+" (not a
						// Beast2 package)");
					}
				}
				File jarDir = new File(jarDirName + "/lib");
				if (!jarDir.exists()) {
					jarDir = new File(jarDirName + "\\lib");
				}
				if (jarDir.exists() && jarDir.isDirectory()) {
					for (String fileName : jarDir.list()) {
						if (fileName.endsWith(".jar")) {
							// Log.debug.print("Probing: " + fileName + " ");
							// check that we are not reload existing classes
							boolean alreadyLoaded = false;
							Set<String> jarClasses = new HashSet<String>();
							try {
								JarInputStream jarFile = new JarInputStream(
										new FileInputStream(jarDir.getAbsolutePath() + "/" + fileName));
								JarEntry jarEntry;

								while (!alreadyLoaded) {
									jarEntry = jarFile.getNextJarEntry();
									if (jarEntry == null) {
										break;
									}
									if ((jarEntry.getName().endsWith(".class"))) {
										String className = jarEntry.getName().replaceAll("/", "\\.");
										className = className.substring(0, className.lastIndexOf('.'));
										if (classes.contains(className)) {
											alreadyLoaded = true;
										} else {
											jarClasses.add(className);
										}
									}
								}
								jarFile.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
							alreadyLoaded = false;
							if (!alreadyLoaded) {
								buf.append(":" + jarDir.getAbsolutePath() + "/" + fileName);
								classes.addAll(jarClasses);
							}
						}
					}
				}
			} catch (Exception e) {
				// File exists, but cannot open the file for some reason
				// Log.debug.println("Skipping "+jarDirName+"/version.xml
				// (unable to open file");
				// Log.warning.println("Skipping "+jarDirName+"/version.xml
				// (unable to open file");
			}

		}
		return buf.toString();
	}

	/**
	 * return list of directories that may contain packages *
	 */
	public static List<String> getBeastDirectories() {

		List<String> dirs = new ArrayList<String>();
		// check if there is the BEAST environment variable is set
		if (System.getProperty("BEAST_ADDON_PATH") != null) {
			String BEAST = System.getProperty("BEAST_ADDON_PATH");
			for (String dirName : BEAST.split(":")) {
				dirs.add(dirName);
			}
		}
		if (System.getenv("BEAST_ADDON_PATH") != null) {
			String BEAST = System.getenv("BEAST_ADDON_PATH");
			for (String dirName : BEAST.split(":")) {
				dirs.add(dirName);
			}
		}

		// add user package directory
		dirs.add(getPackageUserDir());

		// add application package directory
		dirs.add(Utils6.getPackageSystemDir());

		// add BEAST installation directory
		if (getBEASTInstallDir() != null)
			dirs.add(getBEASTInstallDir());

		// pick up directories in class path, useful when running in an IDE
		String strClassPath = System.getProperty("java.class.path");
		String[] paths = strClassPath.split(":");
		for (String path : paths) {
			if (!path.endsWith(".jar")) {
				path = path.replaceAll("\\\\", "/");
				if (path.contains("/")) {
					path = path.substring(0, path.lastIndexOf("/"));
					// deal with the way Mac's appbundler sets up paths
					path = path.replaceAll("/[^/]*/Contents/Java", "");
					// exclude Intellij build path out/production
					if (!dirs.contains(path) && !path.contains("production")) {
						dirs.add(path);
					}
				}
			}
		}

		// subdirectories that look like they may contain an package
		// this is detected by checking the subdirectory contains a lib or
		// templates directory
		List<String> subDirs = new ArrayList<String>();
		for (String dirName : dirs) {
			File dir = new File(dirName);
			if (dir.isDirectory()) {
				File[] files = dir.listFiles();
				if (files == null)
					continue;

				for (File file : files) {
					if (file.isDirectory()) {
						File versionFile = new File(file, "version.xml");
						if (versionFile.exists())
							subDirs.add(file.getAbsolutePath());
					}
				}
			}
		}

		dirs.addAll(subDirs);
		dirs.addAll(getLatestBeastArchiveDirectories(dirs));
		return dirs;
	}

	/**
	 * Returns directory where BEAST installation resides, based on the location
	 * of the jar containing the BeastMain class file. This assumes that the
	 * parent directory of the beast.jar is the base install directory.
	 *
	 * @return string representation of BEAST install directory or null if this
	 *         directory cannot be identified.
	 */
	public static String getBEASTInstallDir() {

		// Allow users to explicitly set install directory - handy for
		// programmers
		if (System.getProperty("beast.install.dir") != null)
			return System.getProperty("beast.install.dir");

		URL u = BeastMain.class.getProtectionDomain().getCodeSource().getLocation();
		String s = u.getPath();
		File beastJar = new File(s);
		// Log.trace.println("BeastMain found in " + beastJar.getPath());
		if (!beastJar.getName().toLowerCase().endsWith(".jar")) {
			return null;
		}

		if (beastJar.getParentFile() != null) {
			return beastJar.getParentFile().getParent();
		} else {
			return null;
		}
	}

	/*
	 * Get directories from archive, if not already loaded when traversing
	 * visitedDirs. Only add the latest version from the archive.
	 */
	private static List<String> getLatestBeastArchiveDirectories(List<String> visitedDirs) {
		List<String> dirs = new ArrayList<String>();
		String FILESEPARATOR = "/"; // (Utils.isWindows() ? "\\" : "/");

		String dir = getPackageUserDir() + FILESEPARATOR + ARCHIVE_DIR;
		File archiveDir = new File(dir);
		if (archiveDir.exists()) {

			// determine which packages will already be loaded
			Set<String> alreadyLoaded = new HashSet<String>();
			for (String d : visitedDirs) {
				File dir2 = new File(d);
				if (dir2.isDirectory()) {
					File versionFile = new File(dir2 + "/version.xml");
					if (versionFile.exists()) {
						try {
							// find name of package
							DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
							Document doc = factory.newDocumentBuilder().parse(versionFile);
							Element addon = doc.getDocumentElement();
							alreadyLoaded.add(addon.getAttribute("name"));
						} catch (Exception e) {
							// too bad, won't print out any info
						}
					}

					alreadyLoaded.add(dir2.getName());
					for (String f : dir2.list()) {
						File dir3 = new File(f);
						if (dir3.isDirectory()) {
							versionFile = new File(dir3 + "/version.xml");
							if (versionFile.exists()) {
								try {
									// find name of package
									DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
									Document doc = factory.newDocumentBuilder().parse(versionFile);
									Element addon = doc.getDocumentElement();
									alreadyLoaded.add(addon.getAttribute("name"));
								} catch (Exception e) {
									// too bad, won't print out any info
								}
							}
						}
					}
				}
			}

			for (String f : archiveDir.list()) {
				File f2 = new File(dir + FILESEPARATOR + f);
				if (f2.isDirectory()) {
					// this may be a package directory -- pick the latest
					// directory
					String[] versionDirs = f2.list();
					Arrays.sort(versionDirs, new Comparator<String>() {
						public int compare(String v1, String v2) {
						PackageVersion pv1 = new PackageVersion(v1);
						PackageVersion pv2 = new PackageVersion(v2);
						return (pv1.compareTo(pv2));
						}
					});
					int k = versionDirs.length - 1;
					while (k >= 0) {
						String versionDir = versionDirs[k];
						File vDir = new File(f2.getPath() + FILESEPARATOR + versionDir);
						if (vDir.exists() && new File(vDir.getPath() + FILESEPARATOR + "version.xml").exists()) {
							// check it is not already loaded
							if (!alreadyLoaded.contains(f)) {
								dirs.add(vDir.getPath());
							}
							break;
						}
						k--;
					}
				}
			}
		}
		return dirs;
	} // getBeastDirectories

	private static String sanitise(String property) {
		// make absolute paths from relative paths
		String pathSeparator = System.getProperty("path.separator");
		String[] paths = property.split(pathSeparator);
		StringBuilder b = new StringBuilder();
		for (String path : paths) {
			File f = new File(path);
			b.append(f.getAbsolutePath());
			b.append(pathSeparator);
		}
		// chop off last pathSeparator
		property = b.substring(0, b.length() - 1);

		// sanitise for windows
		if (Utils6.isWindows()) {
			String cwd = System.getProperty("user.dir");
			cwd = cwd.replace("\\", "/");
			property = property.replaceAll(";\\.", ";" + cwd + ".");
			property = property.replace("\\", "/");
		}
		return property;
	}

	public static void run(String classPath, String main, String[] args) {
		try {
        

            List<String> cmd = new ArrayList<String>();
            if (System.getenv("JAVA_HOME") != null) {
                cmd.add(System.getenv("JAVA_HOME") + File.separatorChar
                        + "bin" + File.separatorChar + "java");
            } else
                cmd.add("java");

            if (System.getProperty("java.library.path") != null && System.getProperty("java.library.path").length() > 0) {
            	cmd.add("-Djava.library.path=" + sanitise(System.getProperty("java.library.path")));
            }
            cmd.add("-cp");
            cmd.add(classPath);
            cmd.add(main);

            for (String arg : args) {
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
                System.out.print((char)c);
            }
            input.close();
            final int exitStatus = process.waitFor();

            if (exitStatus != 0) {
            	System.err.println(process.getErrorStream());
                // Log.err.println(Utils.toString());
            } else {
//                System.out.println(Utils.toString(process.getInputStream()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
