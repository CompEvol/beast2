/*
 * File AddOnManager.java
 *
 * Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
 *
 * This file is part of BEAST2.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */
/*
 * Parts copied from WEKA ClassDiscovery.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package beast.util;


import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class is used to manage beast 2 add-ons, and can
 * - install a new add on
 * - un-install an add on
 * - list directories that may contain add ons 
 * - load jars from installed add ons
 * - discover classes in add ons that implement a certain interface or a derived from a certain class
 */
public class AddOnManager {
	public final static String[] IMPLEMENTATION_DIR = { "beast", "snap" };

	/** flag indicating add ons have been loaded at least once **/
	static boolean externalJarsLoaded = false;


	/** list of all classes found in the class path **/
	private static List<String> all_classes;

	/** return URLs containing list of downloadable add-ons **/
	public static String[] getAddOnURL() {
		return new String[] { "http://beast2.cs.auckland.ac.nz/index.php/Add-ons" };
	}

	/**
	 * create list of addons. The list is downloaded from a beast2 wiki page and
	 * parsed.
	 * 
	 * @return list of addons, encoded as pairs of description, urls.
	 * @throws Exception
	 */
	public static List<List<String>> getAddOns() throws Exception {

		List<List<String>> addOns = new ArrayList<List<String>>();
		String[] sURLs = getAddOnURL();

		for (String sURL : sURLs) {
			URL url = new URL(sURL);
			InputStream is = url.openStream(); // throws an IOException

			StringBuffer buf = new StringBuffer();
			BufferedReader d = new BufferedReader(new InputStreamReader(is));

			String sLine = "";
			while ((sLine = d.readLine()) != null) {
				buf.append(sLine);
			}
			is.close();
			String sText = buf.toString();
			// parse WIKI xml for add-ons
			sText = sText.substring(sText.indexOf("<!-- bodytext -->") + 18);
			String[] sStrs = sText.split("</p>");
			for (int i = 0; i < sStrs.length - 1; i++) {
				List<String> addOn = new ArrayList<String>();
				sText = sStrs[i];
				sText = sText.replaceAll("<p>", "");
				String[] sStr2 = sText.split("<");
				addOn.add(sStr2[0]);
				sStr2 = sStr2[1].split("\"");
				addOn.add(sStr2[1]);
				addOns.add(addOn);
			}
		}
		return addOns;
	}

	/**
	 * download and unzip add-on from URL provided It is assumed the add-on
	 * consists of a zip file containing directories /lib with jars used by the
	 * add on /templates with beauti XML templates
	 * 
	 * @param sURL
	 * @throws Exception
	 */
	public static void installAddOn(String sURL) throws Exception {
		if (!sURL.toLowerCase().endsWith(".zip")) {
			throw new Exception("Add-on should be packaged in a zip file");
		}
		String sName = URL2AddOnName(sURL);

		// create directory
		URL templateURL = new URL(sURL);
		ReadableByteChannel rbc = Channels.newChannel(templateURL.openStream());
		String sDir = getAddOnDir() + "/" + sName;
		File dir = new File(sDir);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				throw new Exception("Could not create template directory " + sDir);
			}
		}
		// grab file from URL
		String sZipFile = sDir + "/" + sName + ".zip";
		FileOutputStream fos = new FileOutputStream(sZipFile);
		fos.getChannel().transferFrom(rbc, 0, 1 << 24);

		// unzip archive
		doUnzip(sZipFile, sDir);
		// refresh classes
		loadExternalJars();
	}

	public static void uninstallAddOn(String sURL) throws Exception {
		if (!sURL.toLowerCase().endsWith(".zip")) {
			throw new Exception("Add-on should be packaged in a zip file");
		}
		String sName = URL2AddOnName(sURL);
		String sDir = getAddOnDir() + "/" + sName;
		File dir = new File(sDir);
		deleteRecursively(dir);
	}

	private static void deleteRecursively(File file) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (File f : files) {
				deleteRecursively(f);
			}
		}
		file.delete();
	}

	public static String URL2AddOnName(String sURL) {
		String sName = sURL.substring(sURL.lastIndexOf("/") + 1);
		if (sName.contains(".")) {
			sName = sName.substring(0, sName.indexOf("."));
		}
		return sName;
	}

	/** unzip zip archive **/
	public static void doUnzip(String inputZip, String destinationDirectory) throws IOException {
		int BUFFER = 2048;
		File sourceZipFile = new File(inputZip);
		File unzipDestinationDirectory = new File(destinationDirectory);

		// Open Zip file for reading
		ZipFile zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);

		// Create an enumeration of the entries in the zip file
		Enumeration<?> zipFileEntries = zipFile.entries();

		// Process each entry
		while (zipFileEntries.hasMoreElements()) {
			// grab a zip file entry
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();

			String currentEntry = entry.getName();

			File destFile = new File(unzipDestinationDirectory + "/" + currentEntry);

			// grab file's parent directory structure
			File destinationParent = destFile.getParentFile();

			// create the parent directory structure if needed
			destinationParent.mkdirs();

			try {
				// extract file if not a directory
				if (!entry.isDirectory()) {
					BufferedInputStream is = new BufferedInputStream(zipFile.getInputStream(entry));
					int currentByte;
					// establish buffer for writing file
					byte data[] = new byte[BUFFER];

					// write the current file to disk
					FileOutputStream fos = new FileOutputStream(destFile);
					BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);

					// read and write until last byte is encountered
					while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
						dest.write(data, 0, currentByte);
					}
					dest.flush();
					dest.close();
					is.close();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		zipFile.close();
	}

	/** return directory where to install add-ons **/
	public static String getAddOnDir() {
		return System.getProperty("user.home") + "/.beast2";
	}

	/** return list of directories that may contain add-ons **/
	public static List<String> getBeastDirectories() {
		List<String> sDirs = new ArrayList<String>();
		// check if there is the BEAST environment variable is set
		if (System.getenv("BEAST") != null) {
			String sBEAST = System.getenv("BEAST");
			for (String sDir : sBEAST.split(":")) {
				sDirs.add(sDir);
			}
			return sDirs;
		}
		// add user directory
		sDirs.add(System.getProperty("user.dir"));
		// add home directory
		sDirs.add(System.getProperty("user.home") + "/.beast2");
		List<String> sSubDirs = new ArrayList<String>();

		// subdirectories that look like they may contain an add-on
		// this is detected by checking the subdirectory contains a lib or
		// templates directory
		for (String sDir : sDirs) {
			File dir = new File(sDir);
			if (dir.isDirectory()) {
				File[] files = dir.listFiles();
				for (File file : files) {
					if (file.isDirectory()) {
						File[] files2 = file.listFiles();
						if (files2 != null) {
							for (File file2 : files2) {
								if (file2.isDirectory()) {
									String sFile = file2.getAbsolutePath().toLowerCase();
									if (sFile.endsWith("/lib") || sFile.endsWith("/templates")) {
										sSubDirs.add(file.getAbsolutePath());
										break;
									}
								}
							}
						}
					}
				}
			}
		}
		sDirs.addAll(sSubDirs);
		return sDirs;
	} // getBeastDirectories

	/** load external jars in beast directories **/
	public static void loadExternalJars() throws Exception {
		for (String sJarDir : getBeastDirectories()) {
			File jarDir = new File(sJarDir + "/lib");
			if (jarDir.exists() && jarDir.isDirectory()) {
				for (String sFile : jarDir.list()) {
					if (sFile.endsWith(".jar")) {
						@SuppressWarnings("deprecation")
						URL url = new File(jarDir.getAbsolutePath() + "/" + sFile).toURL();
						addURL(url);
					}
				}
			}
		}
		externalJarsLoaded = true;
	} // loadExternalJars

	/**
	 * Add URL to CLASSPATH
	 * 
	 * @param u
	 *            URL
	 * @throws IOException
	 *             if something goes wrong when adding a url
	 */
	public static void addURL(URL u) throws IOException {
		// ClassloaderUtil clu = new ClassloaderUtil();
		AddOnManager clu = new AddOnManager();
		// URLClassLoader sysLoader = (URLClassLoader)
		// ClassLoader.getSystemClassLoader();
		URLClassLoader sysLoader = (URLClassLoader) clu.getClass().getClassLoader();
		URL urls[] = sysLoader.getURLs();
		for (int i = 0; i < urls.length; i++) {
			if (urls[i].toString().toLowerCase().equals(u.toString().toLowerCase())) {
				System.err.println("URL " + u + " is already in the CLASSPATH");
				return;
			}
		}
		Class<?> sysclass = URLClassLoader.class;
		try {
			// Parameters
			Class<?>[] parameters = new Class[] { URL.class };
			Method method = sysclass.getDeclaredMethod("addURL", parameters);
			method.setAccessible(true);
			method.invoke(sysLoader, new Object[] { u });
			System.err.println("Loaded URL " + u);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new IOException("Error, could not add URL to system classloader");
		}
		String classpath = System.getProperty("java.class.path");
		String sJar = u + "";
		classpath += ":" + sJar.substring(5);
		System.setProperty("java.class.path", classpath);
		all_classes = null;
	}

	
	private static void loadAllClasses() {
		if (!externalJarsLoaded) {
			try {
				loadExternalJars();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		all_classes = new ArrayList<String>();
		String pathSep = System.getProperty("path.separator");
		String classpath = System.getProperty("java.class.path");

		for (String path : classpath.split(pathSep)) {
			File filepath = new File(path);

			if (filepath.isDirectory()) {
				addDirContent(filepath, filepath.getAbsolutePath().length());
			} else if (path.endsWith(".jar")) {

				JarFile jar;
				try {
					jar = new JarFile(filepath);
				} catch (IOException e) {
					System.err.println("WARNING: " + filepath + " could not be opened!");
					continue;
				}

				for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
					JarEntry entry = entries.nextElement();
					if (entry.getName().endsWith(".class")) {
						all_classes.add(entry.getName());
					}
				}
			} else if (path.endsWith(".class")) {
				all_classes.add(path);
			} else {
				System.err.println("Warning: corrupt classpath entry: " + path);
			}

		}

		for (int i = 0; i < all_classes.size(); i++) {
			String sStr = all_classes.get(i);
			sStr = sStr.substring(0, sStr.length() - 6);
			sStr = sStr.replaceAll("/", ".");
			if (sStr.startsWith(".")) {
				sStr = sStr.substring(1);
			}
			all_classes.set(i, sStr);
		}

	}

	private static void addDirContent(File dir, int len) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				addDirContent(file, len);
			} else {
				if (file.getName().endsWith(".class")) {
					all_classes.add(file.getAbsolutePath().substring(len));
				}
			}
		}

	}


	/**
	 * Checks whether the "otherclass" is a subclass of the given "superclass".
	 * 
	 * @param superclass
	 *            the superclass to check against
	 * @param otherclass
	 *            this class is checked whether it is a subclass of the the
	 *            superclass
	 * @return TRUE if "otherclass" is a true subclass
	 */
	public static boolean isSubclass(Class<?> superclass, Class<?> otherclass) {
		Class<?> currentclass;
		boolean result;

		result = false;
		currentclass = otherclass;
		do {
			result = currentclass.equals(superclass);

			// topmost class reached?
			if (currentclass.equals(Object.class))
				break;

			if (!result)
				currentclass = currentclass.getSuperclass();
		} while (!result);

		return result;
	}


	/**
	 * Checks whether the given class implements the given interface.
	 * 
	 * @param intf
	 *            the interface to look for in the given class
	 * @param cls
	 *            the class to check for the interface
	 * @return TRUE if the class contains the interface
	 */
	public static boolean hasInterface(Class<?> intf, Class<?> cls) {
		Class<?>[] intfs;
		int i;
		boolean result;
		Class<?> currentclass;

		result = false;
		currentclass = cls;
		do {
			// check all the interfaces, this class implements
			intfs = currentclass.getInterfaces();
			for (i = 0; i < intfs.length; i++) {
				if (intfs[i].equals(intf)) {
					result = true;
					break;
				}
			}

			// get parent class
			if (!result) {
				currentclass = currentclass.getSuperclass();

				// topmost class reached or no superclass?
				if ((currentclass == null) || (currentclass.equals(Object.class)))
					break;
			}
		} while (!result);

		return result;
	}


	/**
	 * Checks the given packages for classes that inherited from the given
	 * class, in case it's a class, or implement this class, in case it's an
	 * interface.
	 * 
	 * @param classname
	 *            the class/interface to look for
	 * @param pkgnames
	 *            the packages to search in
	 * @return a list with all the found classnames
	 */
	public static List<String> find(String classname, String[] pkgnames) {
		List<String> result;
		Class<?> cls;

		result = new ArrayList<String>();

		try {
			cls = Class.forName(classname);
			result = find(cls, pkgnames);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Checks the given package for classes that inherited from the given class,
	 * in case it's a class, or implement this class, in case it's an interface.
	 * 
	 * @param classname
	 *            the class/interface to look for
	 * @param pkgname
	 *            the package to search in
	 * @return a list with all the found classnames
	 */
	public static List<String> find(String classname, String pkgname) {
		List<String> result;
		Class<?> cls;

		result = new ArrayList<String>();

		try {
			cls = Class.forName(classname);
			result = find(cls, pkgname);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Checks the given packages for classes that inherited from the given
	 * class, in case it's a class, or implement this class, in case it's an
	 * interface.
	 * 
	 * @param cls
	 *            the class/interface to look for
	 * @param pkgnames
	 *            the packages to search in
	 * @return a list with all the found classnames
	 */
	public static List<String> find(Class<?> cls, String[] pkgnames) {
		List<String> result;
		int i;
		HashSet<String> names;

		result = new ArrayList<String>();

		names = new HashSet<String>();
		for (i = 0; i < pkgnames.length; i++)
			names.addAll(find(cls, pkgnames[i]));

		// sort result
		result.addAll(names);
		Collections.sort(result); //, new StringCompare());

		return result;
	}

	/**
	 * Checks the given package for classes that inherited from the given class,
	 * in case it's a class, or implement this class, in case it's an interface.
	 * 
	 * @param cls
	 *            the class/interface to look for
	 * @param pkgname
	 *            the package to search in
	 * @return a list with all the found classnames
	 */
	public static List<String> find(Class<?> cls, String pkgname) {
		if (all_classes == null) {
			loadAllClasses();
		}

		List<String> result = new ArrayList<String>();
		for (int i = all_classes.size() - 1; i >= 0; i--) {
			String sClass = all_classes.get(i);
			
			// must match package
			if (sClass.startsWith(pkgname)) {
				try {
					Class<?> clsNew = Class.forName(sClass);

					// no abstract classes
					if (!Modifier.isAbstract(clsNew.getModifiers()) &&
					// must implement interface
					  (cls.isInterface() && hasInterface(cls, clsNew)) ||
					// must be derived from class
					  (!clsNew.isInterface() && isSubclass(cls, clsNew))) {
						result.add(sClass);
					  }
				} catch (Throwable e) {
					System.err.println("Checking class: " + sClass);
					e.printStackTrace();
				}

			}
		}
		
		// sort result
		Collections.sort(result); //, new StringCompare());
		// remove duplicates
		for (int i = result.size() - 1; i > 0; i--) {
			if (result.get(i).equals(result.get(i - 1))) {
				result.remove(i);
			}
		}

		return result;
	}

}
