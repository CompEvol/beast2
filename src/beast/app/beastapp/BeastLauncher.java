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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import beast.app.BEASTVersion;
import beast.app.util.Utils6;
import beast.core.util.Log;
import beast.util.PackageManager;
import beast.util.Package;
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
			boolean useStrictVersions = false;
			for (String arg : args) {
				if (arg.equals("-strictversions")) {
					useStrictVersions = true;
				}
			}
			String classpath = getPath(useStrictVersions, args.length > 0 ? args[args.length - 1]: null);
			run(classpath, "beast.app.beastapp.BeastMain", args);
			// BeastMain.main(args);
		}
	}
	
	/**
	 * Install BEAST package, if a none is installed, or a newer version can be found 
	 **/
	static private void installBEASTPackage() throws IOException, NoSuchMethodException, SecurityException,
			ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		BeastLauncher clu = new BeastLauncher();

		// first try beast from the package_user_dir/lib/beast.jar
		String beastUserDir = getPackageUserDir();
		pathDelimiter = isWindows() ? "\\\\" : "/";
		beastUserDir += pathDelimiter + "BEAST" + pathDelimiter;
		String beastJar = beastUserDir + "lib";
		boolean foundJavaJarFile = checkForBEAST(new File(beastJar));

		String launcherJar = clu.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		// deal with special characters and spaces in path
		launcherJar = URLDecoder.decode(launcherJar, "UTF-8");
		//System.err.println("jardir = " + launcherJar);
		File jarDir0 = new File(launcherJar).getParentFile();
		while ((!foundJavaJarFile) && (jarDir0 != null)) {
			foundJavaJarFile = checkForBEAST(jarDir0);
			foundJavaJarFile = foundJavaJarFile
					|| checkForBEAST(new File(jarDir0.getAbsolutePath() + pathDelimiter + "lib"));

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
//		Method method = Class.forName("beast.evolution.alignment.Alignment").getMethod("findDataTypes");
//		method.invoke(null);

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

			String version = "<package name='BEAST' version='" + BEASTVersion.INSTANCE.getVersion() + "'>\n" + "</package>";
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

	private static boolean checkForBEAST(File jarDir) throws IOException {
		//System.err.println("Checking out " + jarDir.getAbsolutePath());
		boolean foundOne = false;
		if (jarDir.exists()) {
			// System.err.println(jarDir.getAbsolutePath() + "exists");
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

//				URLClassLoader sysLoader = (URLClassLoader) clu.getClass().getClassLoader();
//				Class<?> sysclass = URLClassLoader.class;
//				try {
//					// Parameters
//					Class<?>[] parameters = new Class[] { URL.class };
//					Method method = sysclass.getDeclaredMethod("addURL", parameters);
//					method.setAccessible(true);
//					method.invoke(sysLoader, new Object[] { url });
//					System.err.println("Loaded URL " + url);
//					foundOne = true;
//				} catch (Throwable t) {
//					t.printStackTrace();
//					throw new IOException("Error, could not add URL to system classloader");
//				}
				foundOne = true;
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
		try {
			int majorVersion = Utils6.getMajorJavaVersion();
			if (majorVersion < 8) {
				String JAVA_VERSION_MSG = "<html>" + app + " requires Java version at least 8<br>"
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

	protected static String getPath(boolean useStrictVersions, String beastFile) throws NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		installBEASTPackage();
		PackageManager.initialise();


        if (useStrictVersions) {
        	// grab "required" attribute from beast spec
            if (beastFile.endsWith(".json")) {
            	throw new IllegalArgumentException("The -strictversions flag is not implemented for JSON files yet (only XML files are supported).");
            } else {
            	try {
	                BufferedReader fin = new BufferedReader(new FileReader(beastFile));
	                StringBuilder buf = new StringBuilder();
	                String str = null;
	                int lineCount = 0;
	                while (fin.ready() && lineCount < 100) {
	                    str = fin.readLine();
	                    buf.append(str);
	                    buf.append(' ');
	                }
	                fin.close();
	                str = buf.toString();
	                int start = str.indexOf("required=");
	                if (start < 0) {
	                	throw new IllegalArgumentException("Could not find a 'required' attribute in the XML. Add the required attribute, or run without the -strictversions flag");
	                }
	                char c = str.charAt(start + 9);
	                start += 10;
	                int end = str.indexOf(c, start);
	                String packages = str.substring(start, end);

	                buf = new StringBuilder();
	        		buf.append("\"");
	        		buf.append(sanitise(System.getProperty("java.library.path")));
	        		String packagePath = determinePackagePath("BEAST " + BEASTVersion.INSTANCE.getVersion() + ":" + packages);
	        		buf.append(packagePath);
	        		buf.append("\"");
	        		return buf.toString();
            	} catch (IOException e) {
            		e.printStackTrace();
            	}
            }
        } 

        PackageManager.checkInstalledDependencies();

        // just load all packages
        StringBuilder buf = new StringBuilder();
		buf.append("\"");
//		buf.append(sanitise(System.getProperty("java.library.path")));
		String packagePath = Utils6.getBeautiProperty("package.path");
		if (packagePath == null || packagePath.length() == 0) {
			packagePath = determinePackagePath();
			Utils6.saveBeautiProperty("package.path", packagePath);
		}
		buf.append(File.pathSeparator);
		buf.append(packagePath);
		buf.append("\"");
		return buf.toString();
	}

	
	private static String determinePackagePath(String packagesString) throws UnsupportedEncodingException {
		StringBuilder buf = new StringBuilder();
		if (PackageManager.getBEASTInstallDir() != null) {
			buf.append(File.pathSeparator);
			buf.append(URLDecoder.decode(PackageManager.getBEASTInstallDir() + "/lib/beast.jar", "UTF-8"));
		}
	    if (packagesString != null && packagesString.trim().length() > 0) {
	    	Map<String, Package> packages = new HashMap<String, Package>();
	    	PackageManager.addInstalledPackages(packages);
	    	
	    	String unavailablePackages = "";
	    	String [] packageAndVersions = packagesString.split(":");
			Set<String> classes = new HashSet<String>();
	    	for (String s : packageAndVersions) {
	    		s = s.trim();
	    		int i = s.lastIndexOf(" ");
	    		if (i > 0) {
	    			String pkgname = s.substring(0, i);
	    			String pkgversion = s.substring(i+1).trim().replaceAll("v", "");
	    			Package pkg = new Package(pkgname);
	    			PackageVersion version = new PackageVersion(pkgversion);
	    			PackageManager.useArchive(true);
	    			String dirName = PackageManager.getPackageDir(pkg, version, false, PackageManager.getBeastPacakgePathProperty());
	    			if (new File(dirName).exists()) {
	    				buf.append(addJarsToPath(dirName, classes));
	    			} else {
	    				// check the latest installed version
	    				Package pkg2 = packages.get(pkgname);
	    				if (pkg2 == null || !pkg2.isInstalled() || !pkg2.getInstalledVersion().equals(version)) {
	        				unavailablePackages += s +", ";
	    				} else {
	    					PackageManager.useArchive(false);
	            			dirName = PackageManager.getPackageDir(pkg, version, false, PackageManager.getBeastPacakgePathProperty());
	            			if (new File(dirName).exists()) {
	            				buf.append(addJarsToPath(dirName, classes));
	            			} else {
	            				unavailablePackages += s +", ";
	            			}
	    				}
	    			}
	    		}
	    	}
	    	if (unavailablePackages.length() > 1) {
	    		unavailablePackages = unavailablePackages.substring(0, unavailablePackages.length() - 2);
	    		if (unavailablePackages.contains(",")) {
	    			Log.warning("The following packages are required, but not available: " + unavailablePackages);
	    		} else {
	    			Log.warning("The following package is required, but is not available: " + unavailablePackages);
	    		}
	    		Log.warning("See http://beast2.org/managing-packages/ for details on how to install packages.");
	    		throw new IllegalArgumentException("The following package(s) are required, but not available: " + unavailablePackages);
	    	}
	    }
	    return buf.toString();
    }

	private static String determinePackagePath() throws UnsupportedEncodingException {
		StringBuilder buf = new StringBuilder();
		Set<String> classes = new HashSet<String>();
		for (String jarDirName : PackageManager.getBeastDirectories()) {
			try {
				File versionFile = new File(jarDirName + "/version.xml");
				String packageNameAndVersion = null;
				if (versionFile.exists()) {
					try {
						// print name and version of package
						DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
						Document doc = factory.newDocumentBuilder().parse(versionFile);
						Element packageElement = doc.getDocumentElement();
						packageNameAndVersion = packageElement.getAttribute("name") + " v" + packageElement.getAttribute("version");
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
				buf.append(addJarsToPath(jarDirName, classes));
				

			} catch (Exception e) {
				// File exists, but cannot open the file for some reason
				// Log.debug.println("Skipping "+jarDirName+"/version.xml
				// (unable to open file");
				// Log.warning.println("Skipping "+jarDirName+"/version.xml
				// (unable to open file");
			}

		}
		if (PackageManager.getBEASTInstallDir() != null) {
			buf.append(File.pathSeparator);
			buf.append(URLDecoder.decode(PackageManager.getBEASTInstallDir() + "/lib/beast.jar", "UTF-8"));
		}
		return buf.toString();
	}

	private static String addJarsToPath(String packageDirName, Set<String> classes) throws UnsupportedEncodingException {
		StringBuilder buf = new StringBuilder();
		File jarDir = new File(packageDirName + "/lib");
		if (!jarDir.exists()) {
			jarDir = new File(packageDirName + "\\lib");
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
						buf.append(File.pathSeparator);
						buf.append(URLDecoder.decode(jarDir.getAbsolutePath() + "/" + fileName, "UTF-8"));
						classes.addAll(jarClasses);
					}
				}
			}
		}
		return buf.toString();
	}


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

            if (Utils6.isMac()) {
            	// check whether a bundled jre is present
            	BeastLauncher clu = new BeastLauncher();
            	String launcherJar = clu.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();            	
            	String jreDir = URLDecoder.decode(new File(launcherJar).getParent(), "UTF-8") + "/../jre1.8.0_161/";
            	            	
            	if (new File(jreDir).exists()) {
	                cmd.add(jreDir + "bin/java");
            	}
			}

            if (cmd.size() == 0) {
	            if (System.getenv("JAVA_HOME") != null) {
	                cmd.add(System.getenv("JAVA_HOME") + File.separatorChar
	                        + "bin" + File.separatorChar + "java");
	            } else
	                cmd.add("java");
            }

            setJavaHeapAndStackSize(cmd, args);

            if (System.getProperty("java.library.path") != null && System.getProperty("java.library.path").length() > 0) {
            	cmd.add("-Djava.library.path=" + sanitise(System.getProperty("java.library.path")));
            }            
            
            cmd.add("-cp");
            cmd.add(classPath.substring(1, classPath.length() - 1));
            cmd.add(main);

            for (String arg : args) {
            	if (arg != null) {
            		cmd.add(arg);
            	}
            }

            final ProcessBuilder pb = new ProcessBuilder(cmd);

            Map<String, String> environment = pb.environment();
            System.err.println(pb.command());

            //File log = new File("log");
            pb.redirectErrorStream(true);
            
            // Start the process and wait for it to finish.
            final Process process = pb.start();

            Thread inputThread = null;
            boolean waitToExit = System.getProperty("launcher.wait.for.exit") != null; 
            if (main.equals("beast.app.beastapp.BeastMain") && waitToExit) {
            	inputThread = new Thread() {
	            	public void run() {
	                    OutputStream out = process.getOutputStream();
	                    byte [] buf = new byte[2048];
	            		while (true) {
	            			try {
			                    int ni = System.in.available();
			                    if (ni > 0) {
			                      int c = System.in.read(buf, 0, Math.min(ni, buf.length));
			                      System.err.print((char) c);
			                      out.write(buf, 0, c);
			                      out.flush();
			                    }	 
	            			} catch (IOException e) {
	            				e.printStackTrace();
	            			}
	                		try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
	             		}
	            	}
	            };
	            inputThread.start();
            }

            if (waitToExit) {
	            int c;
	            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
	            while ((c = input.read()) != -1) {
	                System.out.print((char)c);
	            }
	            input.close();
	
	            final int exitStatus = process.waitFor();
	            if (exitStatus != 0) {
	            	System.err.println(process.getErrorStream());
	            }
	            if (main.equals("beast.app.beastapp.BeastMain")) {
	            	inputThread.stop();
	            }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

	private static void setJavaHeapAndStackSize(List<String> cmd, String[] args) {
        boolean heapSizeSet = false;
        boolean stackSizeSet = false;
        
        // see if we have a cmd line argument for heap or stack size
        for (int i = 0; i < args.length; i++) {
        	String arg = args[i];
        	if (arg.startsWith("-Xmx")) {
        		cmd.add(arg);
        		args[i] = null;
        		heapSizeSet = true;
        	} else if (arg.startsWith("-Xms")) {
        		cmd.add(arg);
        		args[i] = null;
        		stackSizeSet = true;
        	}
        }            

        // force default values if nothing is specified
        if (!heapSizeSet) {
        	cmd.add("-Xms8g");
        }
        if (!stackSizeSet) {
        	cmd.add("-Xms256m");
        }
    }
	
}
