package beast.pkgmgmt.launcher;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.BEASTVersion;
import beast.pkgmgmt.Package;
import beast.pkgmgmt.PackageManager;
import beast.pkgmgmt.PackageVersion;
import beast.pkgmgmt.Utils6;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

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
			testCudaStatusOnMac();
			boolean useStrictVersions = false;
			for (String arg : args) {
				if (arg.equals("-strictversions")) {
					useStrictVersions = true;
				}
			}
			String classpath = getPath(useStrictVersions, args.length > 0 ? args[args.length - 1]: null);
			run(classpath, "beastfx.app.beast.BeastMain", args);
			// BeastMain.main(args);
		}
	}
	
	/**
	 * Install BEAST package, if a none is installed, or a newer version can be found 
	 **/
	static private void installBEASTPackage(String packageName, boolean copyExamplesAndTemplates) throws IOException, NoSuchMethodException, SecurityException,
			ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		BeastLauncher clu = new BeastLauncher();

		// first try beast from the package_user_dir/lib/beast.jar
		String beastUserDir = getPackageUserDir();
		pathDelimiter = isWindows() ? "\\\\" : "/";
		beastUserDir += pathDelimiter + packageName + pathDelimiter;
		String beastJar = beastUserDir + "lib";
		boolean foundJavaJarFile = checkForBEAST(new File(beastJar), packageName);

		String launcherJar = clu.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		// deal with special characters and spaces in path
		launcherJar = URLDecoder.decode(launcherJar, "UTF-8");
		//System.err.println("jardir = " + launcherJar);
		File jarDir0 = new File(launcherJar).getParentFile();
		while ((!foundJavaJarFile) && (jarDir0 != null)) {
			foundJavaJarFile = checkForBEAST(jarDir0, packageName);
			foundJavaJarFile = foundJavaJarFile
					|| checkForBEAST(new File(jarDir0.getAbsolutePath() + pathDelimiter + "lib"), packageName);

			if (foundJavaJarFile) {
				createBeastPackage(jarDir0, packageName, copyExamplesAndTemplates);
			}

			jarDir0 = jarDir0.getParentFile();
		}

		if (!foundJavaJarFile) {
			System.err.println("WARNING: could not find " + packageName + ".jar");
			// if beast.jar or its classes are not already in the class path (as
			// is when launched e.g. as developer)
			// the next line will fail
		}

		// initialise beast.jar
//		Method method = Class.forName("beast.evolution.alignment.Alignment").getMethod("findDataTypes");
//		method.invoke(null);

	}

	private static void createBeastPackage(File jarDir0, String packageName, boolean copyExamplesAndTemplates) {
		try {
			//System.err.println("jarDir0="+jarDir0.toString().toLowerCase());
			if (jarDir0.toString().toLowerCase().endsWith("lib")) {
				jarDir0 = jarDir0.getParentFile();
			}
			//System.err.println("jarDir0="+jarDir0.toString().toLowerCase());

			// create package user dir, if it not already exists
			String userDir = getPackageUserDir();
			File dir = new File(userDir + pathDelimiter + packageName + pathDelimiter + "lib");
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					// cannot create dir, let alone create a beast package
					return;
				}
			}
			File exampleDir = null, templateDir = null;
			if (copyExamplesAndTemplates) {
				exampleDir = new File(
						userDir + pathDelimiter + packageName + pathDelimiter + "examples" + pathDelimiter + "nexus");
				if (!exampleDir.exists()) {
					if (!exampleDir.mkdirs()) {
						// cannot create dir, let alone create a beast package
						return;
					}
				}
				
				templateDir = new File(userDir + pathDelimiter + packageName + pathDelimiter + "fxtemplates");
				if (!templateDir.exists()) {
					if (!templateDir.mkdirs()) {
						// cannot create dir, let alone create a beast package
						return;
					}
				}
			}

			File beastJar = new File(jarDir0 + pathDelimiter + "lib" + pathDelimiter + packageName + ".jar");
			File target = new File(dir + pathDelimiter + packageName + ".jar");
			copyFileUsingStream(beastJar, target);

			File versionFile = new File(jarDir0 + pathDelimiter + packageName + ".version.xml");
			if (!versionFile.exists()) {
				versionFile = new File(jarDir0 + pathDelimiter + packageName.toLowerCase() + ".version.xml");
			}				
			if (!versionFile.exists()) {
				System.err.println("Error: cannot find version file: " + versionFile);
			} else {
				File versionTarget = new File(userDir + pathDelimiter + packageName + pathDelimiter + "version.xml");
				copyFileUsingStream(versionFile, versionTarget);
			}

			File beastSrcJar = new File(jarDir0 + pathDelimiter + "lib" + pathDelimiter + packageName + ".src.jar");
			File srcTarget = new File(userDir + pathDelimiter + packageName + pathDelimiter + packageName + ".src.jar");
			copyFileUsingStream(beastSrcJar, srcTarget);

			if (copyExamplesAndTemplates) {
				copyFilesInDir(new File(jarDir0 + pathDelimiter + "examples"),
						new File(userDir + pathDelimiter + packageName + pathDelimiter + "examples"));
				copyFilesInDir(new File(jarDir0 + pathDelimiter + "examples" + pathDelimiter + "nexus"), exampleDir);
				copyFilesInDir(new File(jarDir0 + pathDelimiter + "fxtemplates"), templateDir);
			}

		} catch (Exception e) {
			// do net let exceptions hold up launch of BEAST & friends
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

	private static boolean checkForBEAST(File jarDir, String packageName) throws IOException {
		//System.err.println("Checking out " + jarDir.getAbsolutePath());
		boolean foundOne = false;
		if (jarDir.exists()) {
			// System.err.println(jarDir.getAbsolutePath() + "exists");
			URL url = new URL("file://" + (isWindows() ? "/" : "") + jarDir.getAbsolutePath() + "/" + packageName +".jar");
			if (new File(jarDir.getAbsoluteFile() + File.separator + packageName + ".jar").exists()) {
				File versionFile = new File(jarDir.getParent() + pathDelimiter + "version.xml");
				if (versionFile.exists()) {
					BufferedReader fin = new BufferedReader(new FileReader(versionFile));
					String str = null;
					while (fin.ready()) {
						str += fin.readLine();
					}
					fin.close();

					int start = str.indexOf("version=");
					int end = str.indexOf("\"",  start + 9);
					if (end == -1 || str.indexOf("'",  start + 9) > 0) {
						end = Math.min(end, str.indexOf("'",  start + 9));
					}
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
						+ "Quit to update Java from <br>"
						+ "<a href='https://www.java.com/en/'>https://www.java.com/</a>.</html>";
				if (!java.awt.GraphicsEnvironment.isHeadless()) {
					JOptionPane.showMessageDialog(null, JAVA_VERSION_MSG);
				} else {
					JAVA_VERSION_MSG = JAVA_VERSION_MSG.replaceAll("<br>", "\n");
					JAVA_VERSION_MSG = JAVA_VERSION_MSG.replaceAll("<[^<]*>", "");
					System.err.println(JAVA_VERSION_MSG);
				}
				return false; // if majorVersion < 8 then fail
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

	/**
	 * @param useStrictVersions
	 * @param beastFile
	 * @return Class path string for main BEAST process, enclosed in literal quotes.
	 */
	public static String getPath(boolean useStrictVersions, String beastFile) throws NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		installBEASTPackage("BEAST.base", false);
		installBEASTPackage("BEAST.app", true);
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
			buf.append(URLDecoder.decode(PackageManager.getBEASTInstallDir() + "/lib/beast.base.jar", "UTF-8"));

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
	    			System.err.println("The following packages are required, but not available: " + unavailablePackages);
	    		} else {
	    			System.err.println("The following package is required, but is not available: " + unavailablePackages);
	    		}
	    		System.err.println("See http://beast2.org/managing-packages/ for details on how to install packages.");
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
						// System.err.println.println("Loading package " +
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
				// System.err.println.println("Skipping "+jarDirName+"/version.xml
				// (unable to open file");
			}

		}
		if (PackageManager.getBEASTInstallDir() != null) {
			buf.append(File.pathSeparator);
			buf.append(URLDecoder.decode(PackageManager.getBEASTInstallDir() + "/lib/beast.base.jar", "UTF-8"));
			buf.append(File.pathSeparator);
			buf.append(URLDecoder.decode(PackageManager.getBEASTInstallDir() + "/lib/beast.app.jar", "UTF-8"));
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

	
	public static boolean testCudaStatusOnMac() {
	    String beastJar = Utils6.getPackageUserDir();
	    beastJar += "/" + "BEAST.base" + "/" + "lib" + "/" + "BEAST.base.jar";
		BeastLauncher clu = new BeastLauncher();
		String launcherJar = clu.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		return testCudaStatusOnMac(beastJar + File.pathSeparator + launcherJar, "beast.base.CudaDetector");
	}
	
	/**
	 * 
	 * @param classPath contains beast.base.CudaDetector
	 * @return
	 */
	public static boolean testCudaStatusOnMac(String classPath, String testCudaClass) {
		String cudaStatusOnMac = "<html>It appears you have CUDA installed, but your computer hardware does not support it.<br>"
				+ "You need to remove CUDA before BEAST/BEAUti can start.<br>"
				+ "To remove CUDA, delete the following folders (if they exist) by typing in a terminal:<br>"
				+ "rm -r /Library/Frameworks/CUDA.framework<br>"
				+ "rm -r /Developer/NVIDIA<br>"
				+ "rm -r /usr/local/cuda<br>"
				+ "You may need 'sudo rm' instead of 'rm'</html>";
        boolean forceJava = Boolean.valueOf(System.getProperty("java.only"));
        if (forceJava) {
        	// don't need to check if Beagle (and thus CUDA) is never loaded
        	return true;
        }
        if (isMac()) {
			// check any of these directories exist
			// /Library/Frameworks/CUDA.framework
			// /Developer/NVIDIA
			// /usr/local/cuda
			// there is evidence of CUDA being installed on this computer
			// try to create a BeagleTreeLikelihood using a separate process
			try {
			if (new File("/Library/Frameworks/CUDA.framework").exists() ||
					new File("/Developer/NVIDIA").exists() ||
					new File("/usr/local/cuda").exists() || true) {
				
					String java = null;
					// first check we can find java of the packaged JRE
	            	Utils6 clu = new Utils6();
	            	String launcherJar = clu.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();            	
	            	String jreDir = URLDecoder.decode(new File(launcherJar).getParent(), "UTF-8") + "/../jre1.8.0_161/";	            	            	
	            	if (new File(jreDir).exists()) {
		                java = jreDir + "bin/java";
	            	}
	            	if (java == null) {
					      java = System.getenv("java.home");
					      if (java == null) {
					          if (System.getenv("JAVA_HOME") != null) {
					              java = System.getenv("JAVA_HOME") + File.separatorChar
					                      + "bin" + File.separatorChar + "java";
					          } else {
					          	  java = "java";
					          }					    	  
					      } else {
					    	  java += "/bin/java";
					      }
	            	 }
	            	 for (String jarFile : classPath.split(File.pathSeparator)) {
	            		 if (!new File(jarFile).exists()) { 
	            			 System.err.println("Could not find " + jarFile + ", giving up testCudaStatusOnMac");
	            			 //TODO: first time BEAST is started, BEAST will not be installed as package yet, so beastJar does not exist
	            			 return true;
	            		 }
	            	 }
				      Process p = Runtime.getRuntime().exec(new String[]{java , "-Dbeast.user.package.dir=/NONE", "-cp" , 
				    		  classPath , testCudaClass});
				      BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			          int c;
			          while ((c = input.read()) != -1) {
			        	  System.err.print((char)c);
			          }
			          input.close();			
			          p.waitFor();
				      if (p.exitValue() != 0) {
				    	  try {
				    		  JOptionPane.showMessageDialog(null, cudaStatusOnMac);
				    	  } catch (Exception e) {
//				    	  if (GraphicsEnvironment.isHeadless()) {
				    		  cudaStatusOnMac = cudaStatusOnMac.replaceAll("<br>", "\n");
				    		  cudaStatusOnMac = cudaStatusOnMac.replaceAll("<.?html>","\n");
				    		  System.err.println("WARNING: " + cudaStatusOnMac);
//				    	  } else {
				    	  }
				    	  return false;
				      }
				    }
				}
		    catch (Exception err) {
			      err.printStackTrace();
			}
			
		}
		return true;
	}

	public static void run(String classPath, String main, String[] args) {
		try {

	        for (int i = 0; i < args.length; i++) {
	        	String arg = args[i];
	        	if (arg.startsWith("-Xmx") ||
	        		arg.startsWith("-Xms") ||
	        	    arg.startsWith("-Xss")) {
	        		// warn BEAST v2.5.x users that memory/stack allocation uses a different mechanism now
	        		System.err.println("WARNING: the -Xmx, -Xms and -Xss arguments will be ignored for setting memory/stack space");
	        		System.err.println("WARNING: If you want to use any of these arguments you must either change the script, or");
	        		System.err.println("WARNING: call `java -Xmx16g -cp /path/to/launcher.jar " + main + " arg1 arg2 ...` instead");
	        		System.err.println("WARNING: where `/path/to/` is replaced by the path to where the launcher.jar file is, and");
	        		System.err.println("WARNING: arg1 the first argument, arg2, the second, etc.");
	        	}
	        }            
           	        	
			for (String jarFileName : classPath.substring(1, classPath.length() - 1).split(File.pathSeparator)) {
				if (jarFileName.toLowerCase().endsWith("base.jar")) {
					File jarFile = new File(jarFileName);
					BEASTClassLoader.classLoader.addURL(jarFile.toURI().toURL(), "BEAST.base", null);
				}
				if (jarFileName.toLowerCase().endsWith("app.jar")) {
					File jarFile = new File(jarFileName);
					BEASTClassLoader.classLoader.addURL(jarFile.toURI().toURL(), "BEAST.app", null);
				}
			}
			BEASTClassLoader.classLoader.addParent("BEAST.app", "BEAST.base");
			BEASTClassLoader.initServices(classPath);
		
			Class<?> mainClass = BEASTClassLoader.forName(main);
			Method mainMethod = mainClass.getMethod("main", String [].class);
			mainMethod.invoke(null, (Object) args);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	


	
}
