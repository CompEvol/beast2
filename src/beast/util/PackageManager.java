/*
 * File PackageManager.java
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


import beast.app.BEASTVersion;
import beast.app.util.Arguments;
import beast.app.util.Utils6;
import beast.core.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//import beast.app.util.Utils;

/**
 * This class is used to manage beast 2 add-ons, and can
 * - install a new add on
 * - un-install an add on
 * - list directories that may contain add ons
 * - load jars from installed add ons
 * - discover classes in add ons that implement a certain interface or a derived from a certain class
 */
// TODO: on windows allow installation on drive D: and pick up add-ons in drive C:
//@Description("Manage all BEAUti packages and list their dependencies")
public class PackageManager {
    public static final BEASTVersion beastVersion = BEASTVersion.INSTANCE;

    public enum UpdateStatus {AUTO_CHECK_AND_ASK, AUTO_UPDATE, DO_NOT_CHECK};

    public final static String[] IMPLEMENTATION_DIR = {"beast", "snap"};
    public final static String TO_DELETE_LIST_FILE = "toDeleteList";
    public final static String TO_INSTALL_LIST_FILE = "toInstallList";
    public final static String BEAST_PACKAGE_NAME = "BEAST";

    public final static String PACKAGES_XML = "https://raw.githubusercontent.com/CompEvol/CBAN/master/packages.xml";
//    public final static String PACKAGES_XML = "file:///Users/remco/workspace/beast2/packages.xml";
    public final static String ARCHIVE_DIR = "archive";
    // flag to indicate archive directory and version numbers in directories are required
    private static boolean useArchive = false;
    
    public static void useArchive(boolean _useArchive) {
    	useArchive = _useArchive;
    }
    
    public static final String INSTALLED = "installed";
    public static final String NOT_INSTALLED = "not installed";
    
    public static final String NO_CONNECTION_MESSAGE = "Could not get an internet connection. "
    		+ "The BEAST Package Manager needs internet access in order to list available packages and download them for installation. "
    		+ "Possibly, some software (like security software, or a firewall) blocks the BEAST Package Manager.  "
    		+ "If so, you need to reconfigure such software to allow access.";


    /**
     * Exception thrown when reading a package repository fails.
     */
    public static class PackageListRetrievalException extends Exception {
		private static final long serialVersionUID = 1L;

		/**
         * Constructor for new exception.
         *
         * @param message Message explaining what went wrong
         * @param cause First exception thrown when processing package repositories
         */
        public PackageListRetrievalException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when an operation fails due to package dependency issues.
     */
    public static class DependencyResolutionException extends Exception {
		private static final long serialVersionUID = 1L;

        /**
         * Constructor for new exception
         *
         * @param message message explaining what the dependency problem was.
         */
        public DependencyResolutionException(String message) {
            super(message);
        }
    }

    /**
     * flag indicating add ons have been loaded at least once *
     */
    static boolean externalJarsLoaded = false;


    /**
     * list of all classes found in the class path *
     */
    private static List<String> all_classes;

    /**
     * @return URLs containing list of downloadable packages.
     * @throws java.net.MalformedURLException
     */
    public static List<URL> getRepositoryURLs() throws MalformedURLException {
        // Java 7 introduced SNI support which is enabled by default.
        // http://stackoverflow.com/questions/7615645/ssl-handshake-alert-unrecognized-name-error-since-upgrade-to-java-1-7-0
        System.setProperty("jsse.enableSNIExtension", "false");

        List<URL> URLs = new ArrayList<URL>();
        URLs.add(new URL(PACKAGES_XML));

	    //# url
	    //packages.url=http://...
    	String urls = Utils6.getBeautiProperty("packages.url");
    	if (urls != null) {
	        for (String userURLString : urls.split(",")) {
	            URLs.add(new URL(userURLString));
	    	}
    	}
        return URLs;
    }
    
    /**
     * Write any third-party package repository URLs to the options file.
     * 
     * @param urls List of URLs.  The first is assumed to be the central
     * package repository and is thus ignored.
     */
    public static void saveRepositoryURLs(List<URL> urls) {
    	// RRB: if all urls removed, the old urls still pop up when restarting?
        if (urls.size()<1)
            return;
        
        // Modify property
        if (urls.size()>1) {
            StringBuilder sb = new StringBuilder("");
            for (int i=1; i<urls.size(); i++) {
                if (i>1)
                    sb.append(",");
                sb.append(urls.get(i));
            }
            
            Utils6.saveBeautiProperty("packages.url", sb.toString());
        } else {
            Utils6.saveBeautiProperty("packages.url", null);
        }
    }

    /**
     * Look through BEAST directories for installed packages and add these
     * to the package database.
     *
     * @param packageMap package database
     */
    public static void addInstalledPackages(Map<String, Package> packageMap) {
        for (String dir : getBeastDirectories()) {
            File versionXML = new File(dir + "/version.xml");
            if (!versionXML.exists())
                continue;

            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document doc = factory.newDocumentBuilder().parse(versionXML);
                doc.normalize();
                // get name and version of package
                Element packageElement = doc.getDocumentElement();
                String packageName = packageElement.getAttribute("name");
                String packageVersionString = packageElement.getAttribute("version");

                Package pkg;
                if (packageMap.containsKey(packageName)) {
                    pkg = packageMap.get(packageName);
                } else {
                    pkg = new Package(packageName);
                    packageMap.put(packageName, pkg);
                }

                if (packageElement.hasAttribute("projectURL"))
                    pkg.setProjectURL(new URL(packageElement.getAttribute("projectURL")));

                PackageVersion installedVersion = new PackageVersion(packageVersionString);

                if (packageElement.hasAttribute("projectURL") &&
                        !(pkg.getLatestVersion() != null && installedVersion.compareTo(pkg.getLatestVersion())<0))
                    pkg.setProjectURL(new URL(packageElement.getAttribute("projectURL")));

                Set<PackageDependency> installedVersionDependencies =
                        new TreeSet<PackageDependency>(new Comparator<PackageDependency>() {
							@Override
							public int compare(PackageDependency o1, PackageDependency o2) {
								return o1.dependencyName.compareTo(o2.dependencyName);
							}
						});

                // get dependencies of add-n
                NodeList nodes = doc.getElementsByTagName("depends");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element dependson = (Element) nodes.item(i);
                    String dependencyName = dependson.getAttribute("on");
                    String atLeastString = dependson.getAttribute("atleast");
                    String atMostString = dependson.getAttribute("atmost");
                    PackageDependency dependency =  new PackageDependency(
                            dependencyName,
                            atLeastString.isEmpty() ? null : new PackageVersion(atLeastString),
                            atMostString.isEmpty() ? null : new PackageVersion(atMostString));

                    installedVersionDependencies.add(dependency);
                }

                pkg.setInstalled(installedVersion, installedVersionDependencies);

            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Manually set currently-installed BEAST 2 version if not already set
        // This can happen when the BEAST package is not installed (perhaps due to 
        // file access issues)
        Package beastPkg;
        if (packageMap.containsKey(BEAST_PACKAGE_NAME)) {
            beastPkg = packageMap.get(BEAST_PACKAGE_NAME);
        } else {
            beastPkg = new Package(BEAST_PACKAGE_NAME);
            packageMap.put(BEAST_PACKAGE_NAME, beastPkg);
        }

        if (!beastPkg.isInstalled()) {
            PackageVersion beastPkgVersion = new PackageVersion(beastVersion.getVersion());
            Set<PackageDependency> beastPkgDeps = new TreeSet<PackageDependency>();
            beastPkg.setInstalled(beastPkgVersion, beastPkgDeps);
        }

    }

    /**
     * Look through the packages defined in the XML files reached by the repository URLs
     * and add these packages to the package database.
     *
     * @param packageMap package database
     * @throws PackageListRetrievalException when one or more XMLs cannot be retrieved
     */
    public static void addAvailablePackages(Map<String, Package> packageMap) throws PackageListRetrievalException {

        List<URL> urls;
        try {
            urls = getRepositoryURLs();
        } catch (MalformedURLException e) {
            throw new PackageListRetrievalException("Error parsing one or more repository URLs.", e);
        }

        List<URL> brokenPackageRepositories = new ArrayList<URL>();
        Exception firstException = null;

        for (URL url : urls) {
        	InputStream is = null;
            try {            		
            	is = url.openStream();
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(is));

                Element rootElement = document.getDocumentElement(); // <packages>
                NodeList nodes = rootElement.getChildNodes();

                for(int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i);

                    if(node instanceof Element){
                        Element element = (Element) node;
                        String packageName = element.getAttribute("name");
                        Package pkg;
                        if (packageMap.containsKey(packageName)) {
                            pkg = packageMap.get(packageName);
                        } else {
                            pkg = new Package(packageName);
//                            packageMap.put(packageName, pkg); // issue 754
                        }
                        pkg.setDescription(element.getAttribute("description"));

                        PackageVersion packageVersion = new PackageVersion(element.getAttribute("version"));

                        if (element.hasAttribute("projectURL") &&
                                !(pkg.getLatestVersion() != null && packageVersion.compareTo(pkg.getLatestVersion())<0))
                            pkg.setProjectURL(new URL(element.getAttribute("projectURL")));

                        Set<PackageDependency> packageDependencies = new HashSet<PackageDependency>();
                        NodeList depNodes = element.getElementsByTagName("depends");
                        for (int j = 0; j < depNodes.getLength(); j++) {
                            Element dependson = (Element) depNodes.item(j);
                            String dependencyName = dependson.getAttribute("on");
                            String atLeastString = dependson.getAttribute("atleast");
                            String atMostString = dependson.getAttribute("atmost");
                            PackageDependency dependency =  new PackageDependency(
                                    dependencyName,
                                    atLeastString.isEmpty() ? null : new PackageVersion(atLeastString),
                                    atMostString.isEmpty() ? null : new PackageVersion(atMostString));

                            packageDependencies.add(dependency);
                        }

                        URL packageURL = new URL(element.getAttribute("url"));

                        pkg.addAvailableVersion(packageVersion, packageURL, packageDependencies);

                        // issue 754 Package manager should make project links compulsory
                        if (pkg.isValidFormat()) {
                            packageMap.put(packageName, pkg);
                        } else{
                            String urlStr = pkg.getProjectURL()==null ? "null" : pkg.getProjectURL().toString();
                            System.err.println("Warning: filter " + packageName + " from package manager " +
                                    " because of invalid project URL " + urlStr + " !");
                        }
                    }
                }
                is.close();
            } catch (IOException e) {
                if (brokenPackageRepositories.isEmpty())
                    firstException = e;

                brokenPackageRepositories.add(url);
            } catch (ParserConfigurationException e) {
                if (brokenPackageRepositories.isEmpty())
                    firstException = e;

                brokenPackageRepositories.add(url);
            } catch (SAXException e) {
                if (brokenPackageRepositories.isEmpty())
                    firstException = e;

                brokenPackageRepositories.add(url);
            } finally {
            	try {
            		if (is != null) is.close();
            	} catch (IOException e) {
            		e.printStackTrace();
            	}
            }
        }

        if (!brokenPackageRepositories.isEmpty()) {
            String message = "Error reading the following package repository URLs:";
            for (URL url : brokenPackageRepositories)
                message += " " + url;

            throw new PackageListRetrievalException(message, firstException);
        }
    }

    /**
     * Looks through packages to be installed and uninstalls any that are already installed but
     * do not match the version that is to be installed. Packages that are already installed and do
     * match the version required are removed from packagesToInstall.
     *
     * @param packagesToInstall map from packages to versions to install
     * @param useAppDir if fause, use user directory, otherwise use application directory
     * @param customDir custom installation directory.
     * @throws IOException thrown if packages cannot be deleted and delete list file cannot be written
     */
    public static void prepareForInstall(Map<Package, PackageVersion> packagesToInstall, boolean useAppDir, String customDir) throws IOException {
    	if (useArchive) {
    		return;
    	}
    	
        Map<Package, PackageVersion> ptiCopy = new HashMap<Package, PackageVersion>(packagesToInstall);
        for (Map.Entry<Package, PackageVersion> entry : ptiCopy.entrySet()) {
            Package thisPkg = entry.getKey();
            PackageVersion thisPkgVersion = entry.getValue();

            if (thisPkg.isInstalled()) {
                if (thisPkg.getInstalledVersion().equals(thisPkgVersion))
                    packagesToInstall.remove(thisPkg);
                else
                    uninstallPackage(thisPkg, useAppDir, customDir);
            }
        }

        if (getToDeleteListFile().exists()) {
            // Write to-install file

        	// RRB: what are the following two lines for?
            //File toDeleteList = getToDeleteListFile();
            //FileWriter outfile = new FileWriter(toDeleteList, true);
        	PrintStream ps = null;
            try { 
            	ps  = new PrintStream(getToInstallListFile());
                for (Map.Entry<Package, PackageVersion> entry : packagesToInstall.entrySet()) {
                    ps.println(entry.getKey() + ":" + entry.getValue());
                }
                ps.close();
            } catch (IOException ex) {
                message("Error writing to-install file: " + ex.getMessage() +
                        " Installation may not resume successfully after restart.");
            }
        }
    }

    /**
     * Download and install specified versions of packages.  Note that
     * this method does not check dependencies.  It is assumed the contents
     * of packagesToInstall has been assembled by fillOutDependencies.
     *
     * It is further assumed that the URL points to a zip file containing
     * a directory lib containing jars used by the package, as well as
     * a directory named templates containing BEAUti XML templates.
     *
     * @param packagesToInstall map from packages to versions to install
     * @param useAppDir if false, use user directory, otherwise use application directory
     * @param customDir custom installation directory.
     * @return list of strings representing directories into which packages were installed
     * @throws IOException if URL cannot be accessed for some reason
     */
    public static Map<String, String> installPackages(Map<Package, PackageVersion> packagesToInstall, boolean useAppDir, String customDir) throws IOException {
    	closeClassLoader();
    	
        Map<String, String> dirList = new HashMap<String, String>();

        for (Map.Entry<Package, PackageVersion> entry : packagesToInstall.entrySet()) {
            Package thisPkg = entry.getKey();
            PackageVersion thisPkgVersion = entry.getValue();

            // create directory
            URL templateURL = thisPkg.getVersionURL(thisPkgVersion);
            ReadableByteChannel rbc = Channels.newChannel(templateURL.openStream());
            String dirName = getPackageDir(thisPkg, thisPkgVersion, useAppDir, customDir);
            File dir = new File(dirName);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new IOException("Could not create directory " + dirName);
                }
            }
            // grab file from URL
            String zipFile = dirName + "/" + thisPkg.getName() + ".zip";
            FileOutputStream fos = new FileOutputStream(zipFile);
            fos.getChannel().transferFrom(rbc, 0, 1 << 24);

            // unzip archive
            doUnzip(zipFile, dirName);
            fos.close();

            dirList.put(thisPkg.getName(), dirName);
        }

        // make sure the class path is updated next time BEAST is started
        Utils6.saveBeautiProperty("packages.url", null);
        return dirList;
    }

    public static String getPackageDir(Package thisPkg, PackageVersion thisPkgVersion, boolean useAppDir, String customDir) {
        String dirName = (useAppDir ? getPackageSystemDir() : getPackageUserDir()) + 
        		(useArchive ? "/" + ARCHIVE_DIR : "") + 
        		"/" + thisPkg.getName() +
        		(useArchive ? "/" + thisPkgVersion.versionString : ""); 
        if (customDir != null) {
            dirName = customDir + 
            		(useArchive ? "/" + ARCHIVE_DIR : "") + 
            		"/" + thisPkg.getName() +
            		(useArchive ? "/" + thisPkgVersion.versionString : ""); 
        }
        return dirName;
	}

	/**
     * Get list of installed packages that depend on pkg.
     *
     * @param pkg package for which to retrieve installed dependencies
     * @param packageMap package database
     * @return list of names of installed packages dependent on pkg.
     */
    public static List<String> getInstalledDependencyNames(Package pkg, Map<String, Package> packageMap) {

        List<String> dependencies = new ArrayList<String>();

        for (Package thisPkg : packageMap.values()) {
            if (thisPkg.equals(pkg))
                continue;

            if (!thisPkg.isInstalled())
                continue;

            for (PackageDependency dependency : thisPkg.getInstalledVersionDependencies()) {
                if (dependency.dependencyName.equals(pkg.getName()))
                    dependencies.add(thisPkg.getName());
            }
        }

        return dependencies;
    }

    /**
     * Uninstall the given package. Like installPackages(), this method does not perform any dependency
     * checking - it just blindly removes the specified package. This is so that the method can be called
     * while an installation is in process without falling over because of broken intermediate states.
     *
     * Before using, call getInstalledDependencies() to check for potential problems.
     *
     * @param pkg package to uninstall
     * @param useAppDir if false, use user directory, otherwise use application directory
     * @param customDir custom installation directory.
     * @return name of directory package was removed from, or null if the package was not removed.
     * @throws IOException thrown if packages cannot be deleted and delete list file cannot be written
     */
    public static String uninstallPackage(Package pkg, boolean useAppDir, String customDir) throws IOException {
    	return uninstallPackage(pkg, null, useAppDir, customDir);
    }
    
    public static String uninstallPackage(Package pkg, PackageVersion pkgVersion, boolean useAppDir, String customDir) throws IOException {
    	closeClassLoader();

    	if (pkgVersion == null) {
    		pkgVersion = pkg.getInstalledVersion();
    	}
    	String dirName = getPackageDir(pkg, pkgVersion, useAppDir, customDir);    	
        File dir = new File(dirName);
        if (!dir.exists()) {
        	useArchive = !useArchive;
        	dirName = getPackageDir(pkg, pkgVersion, useAppDir, customDir);
        	dir = new File(dirName);
        	useArchive = !useArchive;
        }
        List<File> deleteFailed = new ArrayList<File>();
        deleteRecursively(dir, deleteFailed);
        
        if (useArchive) {
        	// delete package directory, if it is empty
        	File parent = dir.getParentFile();
        	if (parent.list().length == 0) {
        		parent.delete();
        	}
        }

        // write deleteFailed to file
        if (deleteFailed.size() > 0) {
            File toDeleteList = getToDeleteListFile();
            FileWriter outfile = new FileWriter(toDeleteList, true);
            for (File file : deleteFailed) {
                outfile.write(file.getAbsolutePath() + "\n");
            }
            outfile.close();
        }
        
        // make sure the class path is updated next time BEAST is started
        Utils6.saveBeautiProperty("packages.url", null);
        return dirName;
    }


    /**
     * Close class loader so that locks on jar files are released, which may prevent
     * files being replaced on Windows.
     * http://docs.oracle.com/javase/7/docs/api/java/net/URLClassLoader.html#close%28%29
     * 
     * This allows smooth upgrading of BEAST versions using the package manager. Without 
     * this, there is no way to upgrade BEAST since the PackageManager is part of the 
     * BEAST.jar file that is loaded and needs to be replaced.
     * 
     * Side effect is that after installing a package, opening a new BEAUti instance
     * will fail (Windows only).
     * 
     */
    private static void closeClassLoader() {
    	try {
    		if (Utils6.isWindows()) {
    			URLClassLoader sysLoader = (URLClassLoader) PackageManager.class.getClassLoader();
    			sysLoader.close();
    		}
		} catch (IOException e) {
			Log.warning.println("Could not close ClassLoader: " + e.getMessage());
		}
		
	}

	private static void deleteRecursively(File file, List<File> deleteFailed) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                deleteRecursively(f, deleteFailed);
            }
        }
        if (!file.delete()) {
            deleteFailed.add(file);
        }
    }


    /**
     * unzip zip archive *
     */
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

    /**
     * @return directory where to install packages for users *
     */
    public static String getPackageUserDir() {
        
        if (System.getProperty("beast.user.package.dir") != null)
            return System.getProperty("beast.user.package.dir");
        
        if (Utils6.isWindows()) {
            return System.getProperty("user.home") + "\\BEAST\\" + beastVersion.getMajorVersion();
        }
        if (Utils6.isMac()) {
            return System.getProperty("user.home") + "/Library/Application Support/BEAST/" + beastVersion.getMajorVersion();
        }
        // Linux and unices
        return System.getProperty("user.home") + "/.beast/" + beastVersion.getMajorVersion();
    }

    /**
     * @return directory where system wide packages reside *
     */
    public static String getPackageSystemDir() {
        
        if (System.getProperty("beast.system.package.dir") != null)
            return System.getProperty("beast.system.package.dir");
        
        if (Utils6.isWindows()) {
            return "\\Program Files\\BEAST\\" + beastVersion.getMajorVersion();
        }
        if (Utils6.isMac()) {
            return "/Library/Application Support/BEAST/" + beastVersion.getMajorVersion();
        }
        return "/usr/local/share/beast/" + beastVersion.getMajorVersion();
    }

    /**
     * Returns directory where BEAST installation resides, based on the location of the jar containing the
     * BeastMain class file.  This assumes that the parent directory of the beast.jar is the base install
     * directory.
     *
     * @return string representation of BEAST install directory or null if this directory cannot be identified.
     */
    public static String getBEASTInstallDir() {

        // Allow users to explicitly set install directory - handy for programmers
        if (System.getProperty("beast.install.dir") != null)
            return System.getProperty("beast.install.dir");

        
        URL u;
		try {
			u = Class.forName("beast.app.beastapp.BeastMain").getProtectionDomain().getCodeSource().getLocation();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		String s = u.getPath();
        File beastJar = new File(s);
        Log.trace.println("BeastMain found in " + beastJar.getPath());
        if (!beastJar.getName().toLowerCase().endsWith(".jar")) {
        	return null;
        }

        if (beastJar.getParentFile() != null) {
            return beastJar.getParentFile().getParent();
        } else {
            return null;
        }
    }

    /**
     * @return file containing list of files that need to be deleted
     * but could not be deleted. This can happen when uninstalling packages
     * on windows, which locks jar files loaded by java.
     */
    public static File getToDeleteListFile() {
        return new File(getPackageUserDir() + "/" + TO_DELETE_LIST_FILE);
    }

    /**
     * Delete files that could not be deleted earlier due to jar locking.
     */
    private static void processDeleteList() {
        File toDeleteListFile = getToDeleteListFile();
        if (toDeleteListFile.exists()) {
            try {
                BufferedReader fin = new BufferedReader(new FileReader(toDeleteListFile));
                while (fin.ready()) {
                    String str = fin.readLine();
                    File file = new File(str);
                    file.delete();
                }
                fin.close();
                toDeleteListFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Obtain file containing list of packages that need to be installed
     * at startup.  This file only exists when packages have failed to upgrade
     * due to jar file locking on Windows.
     *
     * @return to-install file
     */
    public static File getToInstallListFile() {
        return new File(getPackageUserDir() + "/" + TO_INSTALL_LIST_FILE);
    }

    /**
     * Completes installation procedure if packages could not be upgraded due to
     * Windows preventing the deletion of jar files.
     *
     * @param packageMap package database
     */
    private static void processInstallList(Map<String, Package> packageMap) {
        File toInstallListFile = getToInstallListFile();
        if (toInstallListFile.exists()) {
            try {
                addAvailablePackages(packageMap);
            } catch (PackageListRetrievalException e) {
                message("Failed to resume package installation due to package list retrieval error: " + e.getMessage());
                toInstallListFile.delete();
                return;
            }

            Map<Package, PackageVersion>  packagesToInstall = new HashMap<Package, PackageVersion>();
            BufferedReader fin = null;
            try {
            	fin = new BufferedReader(new FileReader(toInstallListFile));
                String line;
                while ((line = fin.readLine()) != null) {
                    String[] nameVerPair = line.split(":");

                    Package pkg = packageMap.get(nameVerPair[0]);
                    PackageVersion ver = new PackageVersion(nameVerPair[1]);
                    packagesToInstall.put(pkg, ver);
                }
                fin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                installPackages(packagesToInstall, false, null);
            } catch (IOException e) {
                message("Failed to install packages due to I/O error: " + e.getMessage());
            }

            toInstallListFile.delete();
        }
    }

    /**
     * return list of directories that may contain packages *
     */
    public static List<String> getBeastDirectories() {
    	
        List<String> dirs = new ArrayList<String>();
        // check if there is the BEAST environment variable is set
        if (PackageManager.getBeastPacakgePathProperty() != null) {
            String BEAST = PackageManager.getBeastPacakgePathProperty();
            for (String dirName : BEAST.split(":")) {
                dirs.add(dirName);
            }
        }

        // add user package directory
        dirs.add(getPackageUserDir());

        // add application package directory
        dirs.add(getPackageSystemDir());

        // add BEAST installation directory
        if (getBEASTInstallDir() != null)
            dirs.add(getBEASTInstallDir());

        // pick up directories in class path, useful when running in an IDE
        String strClassPath = System.getProperty("java.class.path");
        String [] paths = strClassPath.split(":");
        for (String path : paths) {
            if (!path.endsWith(".jar")) {
                path = path.replaceAll("\\\\","/");
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
    
    /*
     * Get directories from archive, if not already loaded when traversing visitedDirs.
     * Only add the latest version from the archive.
     */
    private static List<String> getLatestBeastArchiveDirectories(List<String> visitedDirs) {
        List<String> dirs = new ArrayList<String>();
        String FILESEPARATOR = "/"; //(Utils6.isWindows() ? "\\" : "/");

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
                            Element packageElement = doc.getDocumentElement();
                            alreadyLoaded.add(packageElement.getAttribute("name"));
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
    	                            Element packageElement = doc.getDocumentElement();
    	                            alreadyLoaded.add(packageElement.getAttribute("name"));
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
        			// this may be a package directory -- pick the latest directory
        			String [] versionDirs = f2.list();
        			Arrays.sort(versionDirs,
        					new Comparator<String>() {
								@Override
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

    
	public static void initialise() {
	    processDeleteList();
	
	    addInstalledPackages(packages);
	
	    processInstallList(packages);
	
	//    checkInstalledDependencies(packages);
	}

	/**
     * load external jars in beast directories *
     */
    public static void loadExternalJars() throws IOException {
        processDeleteList();

        addInstalledPackages(packages);

        processInstallList(packages);

        checkInstalledDependencies(packages);

        // jars will only be loaded the classical (pre v2.5.0)
        // way with java 8 when the -Dbeast.load.jars=true
        // directive is given. This can be useful for developers
        // but generally slows down application starting.
        if (System.getProperty("beast.load.jars") == null || Utils6.getMajorJavaVersion() != 8) {
            externalJarsLoaded = true;
            findDataTypes();
    		return;
    	}

        for (String jarDirName : getBeastDirectories()) {
        	loadPackage(jarDirName);
        }
        externalJarsLoaded = true;
        findDataTypes();
    } // loadExternalJars
    
	private static void findDataTypes() {
		try {
			Method findDataTypes = Class.forName("beast.evolution.alignment.Alignment").getMethod("findDataTypes");
			findDataTypes.invoke(null);
		} catch (Exception e) {
			// too bad, cannot load data types
			Log.err.print(e.getMessage());
		}
	}

	public static void loadExternalJars(String packagesString) throws IOException {
        processDeleteList();

        addInstalledPackages(packages);

        processInstallList(packages);

        if (packagesString != null && packagesString.trim().length() > 0) {
        	String unavailablePacakges = "";
        	String [] packageAndVersions = packagesString.split(":");
        	for (String s : packageAndVersions) {
        		s = s.trim();
        		int i = s.lastIndexOf(" ");
        		if (i > 0) {
        			String pkgname = s.substring(0, i);
        			String pkgversion = s.substring(i+1).trim().replaceAll("v", "");
        			Package pkg = new Package(pkgname);
        			PackageVersion version = new PackageVersion(pkgversion);
        	    	useArchive = true;
        			String dirName = getPackageDir(pkg, version, false, PackageManager.getBeastPacakgePathProperty());
        			if (new File(dirName).exists()) {
        				loadPackage(dirName);
        			} else {
        				// check the latest installed version
        				Package pkg2 = packages.get(pkgname);
        				if (pkg2 == null || !pkg2.isInstalled() || !pkg2.getInstalledVersion().equals(version)) {
            				unavailablePacakges += s +", ";
        				} else {
	            	    	useArchive = false;
	            			dirName = getPackageDir(pkg, version, false, PackageManager.getBeastPacakgePathProperty());
	            			if (new File(dirName).exists()) {
	            				loadPackage(dirName);
	            			} else {
	            				unavailablePacakges += s +", ";
	            			}
        				}
        			}
        		}
        	}
        	if (unavailablePacakges.length() > 1) {
        		unavailablePacakges = unavailablePacakges.substring(0, unavailablePacakges.length() - 2);
        		if (unavailablePacakges.contains(",")) {
        			Log.warning("The following packages are required, but not available: " + unavailablePacakges);
        		} else {
        			Log.warning("The following package is required, but is not available: " + unavailablePacakges);
        		}
        		Log.warning("See http://beast2.org/managing-packages/ for details on how to install packages.");
        	}
        }
        externalJarsLoaded = true;
        findDataTypes();
    } // loadExternalJars

    private static void loadPackage(String jarDirName) {
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
                    Log.warning.println("Loading package " + packageNameAndVersion);
                    Utils6.logToSplashScreen("Loading package " + packageNameAndVersion);
                } catch (Exception e) {
                    // too bad, won't print out any info

                    // File is called version.xml, but is not a Beast2 version file
                    // Log.debug.print("Skipping "+jarDirName+" (not a Beast2 package)");
                }
            }
            File jarDir = new File(jarDirName + "/lib");
            if (!jarDir.exists()) {
                jarDir = new File(jarDirName + "\\lib");
            }
            if (jarDir.exists() && jarDir.isDirectory()) {
                for (String fileName : jarDir.list()) {
                    if (fileName.endsWith(".jar")) {
                        Log.debug.print("Probing: " + fileName + " ");
                        // check that we are not reload existing classes
                        String loadedClass = null;
                        try {
                            JarInputStream jarFile = new JarInputStream
                                    (new FileInputStream(jarDir.getAbsolutePath() + "/" + fileName));
                            JarEntry jarEntry;

                            while (loadedClass == null) {
                                jarEntry = jarFile.getNextJarEntry();
                                if (jarEntry == null) {
                                    break;
                                }
                                if ((jarEntry.getName().endsWith(".class"))) {
                                    String className = jarEntry.getName().replaceAll("/", "\\.");
                                    className = className.substring(0, className.lastIndexOf('.'));
                                    try {
                                        /*Object o =*/
                                        Class.forName(className);
                                        loadedClass = className;
                                    } catch (Exception e) {
                                        // TODO: handle exception
                                    }
                                    if (loadedClass == null && packageNameAndVersion != null) {
                                        classToPackageMap.put(className, packageNameAndVersion);
                                    }
                                }
                            }
                            jarFile.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                        @SuppressWarnings("deprecation")
                        URL url = new File(jarDir.getAbsolutePath() + "/" + fileName).toURL();
                        if (loadedClass == null) {
                            addURL(url);
                        } else {
                            Log.debug.println("Skip loading " + url + ": contains class " + loadedClass + " that is already loaded");
                        }
                    }
                }
            }
        } catch (Exception e) {
            // File exists, but cannot open the file for some reason
            Log.debug.println("Skipping "+jarDirName+"/version.xml (unable to open file");
            Log.warning.println("Skipping "+jarDirName+"/version.xml (unable to open file");
        }
	}

    /**
     * Populate given map with versions of packages to install which satisfy dependencies
     * of those already present.
     *
     * @param packageMap database of installed and available packages
     * @param packagesToInstall map to populate with package versions requiring installation.
     * @throws DependencyResolutionException thrown when method fails to identify a consistent set of dependent packages
     */
    public static void populatePackagesToInstall(Map<String, Package> packageMap,
                                                 Map<Package, PackageVersion> packagesToInstall) throws DependencyResolutionException {

        Map<Package, PackageVersion> copy = new HashMap<Package, PackageVersion>(packagesToInstall);

        for (Map.Entry<Package, PackageVersion> entry : copy.entrySet()) {
            populatePackagesToInstall(packageMap, packagesToInstall, entry.getKey(), entry.getValue());
        }

    }

    private static void populatePackagesToInstall(Map<String, Package> packageMap,
                                             Map<Package, PackageVersion> packagesToInstall,
                                          Package rootPackage, PackageVersion rootPackageVersion) throws DependencyResolutionException {

        if (!rootPackage.getAvailableVersions().contains(rootPackageVersion))
            throw new IllegalArgumentException("Package version " + rootPackageVersion + " is not available.");

        Set<PackageDependency> dependencies = rootPackage.getDependencies(rootPackageVersion);
        for (PackageDependency dependency : dependencies) {
            if (!packageMap.containsKey(dependency.dependencyName))
                throw new DependencyResolutionException("Package " + rootPackage
                        + " depends on unknown package " + dependency.dependencyName);

            Package depPkg = packageMap.get(dependency.dependencyName);
            PackageVersion intendedVersion = packagesToInstall.get(depPkg);
            if (intendedVersion == null) {
                if (depPkg.isInstalled() && dependency.isMetBy(depPkg.getInstalledVersion()))
                    continue;
            } else {
                if (dependency.isMetBy(intendedVersion))
                    continue;
                else
                    throw new DependencyResolutionException("Package " + rootPackage + " depends on a different " +
                            "version of package " + dependency.dependencyName + " to that required by another package.");
            }

            boolean foundCompatible = false;
            for (PackageVersion depPkgVersion : depPkg.getAvailableVersions()) {
                if (dependency.isMetBy(depPkgVersion)) {
                    if (depPkg.isInstalled() && depPkgVersion.compareTo(depPkg.getInstalledVersion())<0)
                        continue; // No downgrading of installed versions

                    packagesToInstall.put(depPkg, depPkgVersion);

                    try {
                        populatePackagesToInstall(packageMap, packagesToInstall, depPkg, depPkgVersion);
                        foundCompatible = true;
                        break;
                    } catch (DependencyResolutionException ignored) { }

                    packagesToInstall.remove(depPkg);
                }
            }
            if (!foundCompatible)
                throw new DependencyResolutionException("Package " + rootPackage + " requires " + dependency + ", " +
                        "but no installable version of that package was found.");

        }
    }

    /**
     * Checks that dependencies of all installed packages are met.
     *
     * @param packageMap
     */
    private static void checkInstalledDependencies(Map<String, Package> packageMap) {
        Map<PackageDependency,Package> dependencies = new HashMap<PackageDependency,Package>();

        // Collect installed package dependencies
        for (Package pkg : packageMap.values()) {
            if (!pkg.isInstalled())
                continue;

            for (PackageDependency dep : pkg.getInstalledVersionDependencies())
                dependencies.put(dep, pkg);
        }

        // check dependencies
        for (PackageDependency dep : dependencies.keySet()) {
            Package depPackage = packageMap.get(dep.dependencyName);
            Package requiredBy = dependencies.get(dep);
            if (depPackage == null) {
                warning("Package " + requiredBy.getName()
                        + " requires another package (" + dep.dependencyName + ") which is not available.\n" +
                        "Either uninstall " + requiredBy.getName() + " or ask the package maintainer for " +
                        "information about this dependency.");
            } else if (!depPackage.isInstalled()) {
                warning("Package " + requiredBy.getName() + " requires another package (" + dep.dependencyName + ") which is not installed.\n" +
                        "Either uninstall " + requiredBy.getName() + " or install the " + dep.dependencyName + " package.");
            } else if (!dep.isMetBy(depPackage.getInstalledVersion())) {
                warning("Package " +  requiredBy.getName() + " requires another package " + dep
                        + " but the installed " + dep.dependencyName + " has version " + depPackage.getInstalledVersion() + ".\n" +
                        "Either uninstall " + requiredBy.getName() + " or install the correct version of " + dep.dependencyName + ".");
            }
        }
    }

    public static void checkInstalledDependencies() {
    	checkInstalledDependencies(packages);
    }

    /**
     * Display a warning to console or as a dialog, depending
     * on whether a GUI exists.
     * 
     * @param string warning to display
     */
    private static void warning(String string) {
        Log.warning.println(string);
        Log.warning.println("Unexpected behavior may follow!");
        if (!java.awt.GraphicsEnvironment.isHeadless() && System.getProperty("no.beast.popup") == null) {
            JOptionPane.showMessageDialog(null, string +
                    "\nUnexpected behavior may follow!");
        }
    }
    
    /**
     * Display a message to console or as a dialog, depending
     * on whether a GUI exists.
     * 
     * @param string message to display
     */
    private static void message(String string) {
    	Log.info.println(string);
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, string);
        }
    }

    /**
     * Add URL to CLASSPATH
     *
     * @param u URL
     * @throws IOException if something goes wrong when adding a url
     */
    public static void addURL(URL u) throws IOException {
        // ClassloaderUtil clu = new ClassloaderUtil();
        PackageManager clu = new PackageManager();
        // URLClassLoader sysLoader = (URLClassLoader)
        // ClassLoader.getSystemClassLoader();
        URLClassLoader sysLoader = (URLClassLoader) clu.getClass().getClassLoader();
        URL urls[] = sysLoader.getURLs();
        for (URL url : urls) {
            if (url.toString().toLowerCase().equals(u.toString().toLowerCase())) {
                Log.debug.println("URL " + u + " is already in the CLASSPATH");
                return;
            }
        }
        Class<?> sysclass = URLClassLoader.class;
        try {
            // Parameters
            Class<?>[] parameters = new Class[]{URL.class};
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(sysLoader, u);
            Log.debug.println("Loaded URL " + u);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }
        String classpath = System.getProperty("java.class.path");
        String jar = u + "";
        classpath += System.getProperty("path.separator") + jar.substring(5);
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
            //Log.debug.println("loadallclasses " + path);
            File filepath = new File(path);

            if (filepath.isDirectory()) {
                addDirContent(filepath, filepath.getAbsolutePath().length());
            } else if (path.endsWith(".jar")) {

                JarFile jar = null;
                try {
                    jar = new JarFile(filepath);
                } catch (IOException e) {
                    Log.debug.println("WARNING: " + filepath + " could not be opened!");
                    continue;
                }

                for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        all_classes.add(entry.getName());
                    }
                }
                try {
					jar.close();
				} catch (IOException e) {
                    Log.debug.println("WARNING: " + filepath + " could not be closed!");
				}
            } else if (path.endsWith(".class")) {
                all_classes.add(path);
            } else {
                Log.debug.println("Warning: corrupt classpath entry: " + path);
            }

        }

        String fileSep = System.getProperty("file.separator");
        if (fileSep.equals("\\")) {
            fileSep = "\\\\";
        }
        for (int i = 0; i < all_classes.size(); i++) {
            String str = all_classes.get(i);
            str = str.substring(0, str.length() - 6);
            str = str.replaceAll(fileSep, ".");
            if (str.startsWith(".")) {
                str = str.substring(1);
            }
            all_classes.set(i, str);
        }

    }

    private static void addDirContent(File dir, int len) {
    	try {
    	// No point in checking directories that cannot be read.
    	// Need check here since these potentially can cause exceptions
	    	if (dir.canRead()) {
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
    	} catch (Exception e) {
    		// ignore
    		// windows appears to throw exceptions on unaccessible directories
    	}
    }


    /**
     * Checks whether the "otherclass" is a subclass of the given "superclass".
     *
     * @param superclass the superclass to check against
     * @param otherclass this class is checked whether it is a subclass of the the
     *                   superclass
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
     * @param intf the interface to look for in the given class
     * @param cls  the class to check for the interface
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
     * @param classname the class/interface to look for
     * @param pkgnames  the packages to search in
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
     * @param classname the class/interface to look for
     * @param pkgname   the package to search in
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
     * @param cls      the class/interface to look for
     * @param pkgnames the packages to search in
     * @return a list with all the found classnames
     */
    public static List<String> find(Class<?> cls, String[] pkgnames) {
        List<String> result;
        int i;
        HashSet<String> names;

        result = new ArrayList<String>();

        names = new HashSet<String>();
        for (i = 0; i < pkgnames.length; i++) {
            names.addAll(find(cls, pkgnames[i]));
        }

        // sort result
        result.addAll(names);
        Collections.sort(result, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
	        	if (s1.equals(BEAST_PACKAGE_NAME)) {
	        		return -1;
	        	}
	        	if (s2.equals(BEAST_PACKAGE_NAME)) {
	        		return 1;
	        	}
	        	return s1.compareTo(s2);
			}
        }); //, new StringCompare());

        return result;
    }

    /**
     * Checks the given package for classes that inherited from the given class,
     * in case it's a class, or implement this class, in case it's an interface.
     *
     * @param cls     the class/interface to look for
     * @param pkgname the package to search in
     * @return a list with all the found classnames
     */
    public static List<String> find(Class<?> cls, String pkgname) {
        if (all_classes == null) {
            loadAllClasses();
        }

        List<String> result = new ArrayList<String>();
        for (int i = all_classes.size() - 1; i >= 0; i--) {
            String className = all_classes.get(i);
            className = className.replaceAll("/", ".");
            //Log.debug.println(className + " " + pkgname);

            // must match package
            if (className.startsWith(pkgname)) {
                //Log.debug.println(className);
                try {
                    Class<?> clsNew = Class.forName(className);

                    // no abstract classes
                    if (!Modifier.isAbstract(clsNew.getModifiers()) &&
                            // must implement interface
                            (cls.isInterface() && hasInterface(cls, clsNew)) ||
                            // must be derived from class
                            (!clsNew.isInterface() && isSubclass(cls, clsNew))) {
                        result.add(className);
                    }
                } catch (Throwable e) {
                    Log.debug.println("Checking class: " + className);
                    e.printStackTrace();
                }

            }
        }

        // sort result
        Collections.sort(result, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
	        	if (s1.equals(BEAST_PACKAGE_NAME)) {
	        		return -1;
	        	}
	        	if (s2.equals(BEAST_PACKAGE_NAME)) {
	        		return 1;
	        	}
	        	return s1.compareTo(s2);
			}
        }); //, new StringCompare());
        // remove duplicates
        for (int i = result.size() - 1; i > 0; i--) {
            if (result.get(i).equals(result.get(i - 1))) {
                result.remove(i);
            }
        }

        return result;
    }


    /*
     * Command-line interface code
     */

    /**
     * Pretty-print package information.
     *
     * @param ps print stream to which to print package info
     * @param packageMap map from package names to package objects
     */
    public static void prettyPrintPackageInfo(PrintStream ps, Map<String, Package> packageMap) {

        // Define headers here - need to know lengths
        String nameHeader = "Name";
        String statusHeader = "Installed Version";
        String latestHeader = "Latest Version";
        String depsHeader = "Dependencies";
        String descriptionHeader = "Description";

        int maxNameWidth = nameHeader.length();
        int maxStatusWidth = statusHeader.length();
        int maxLatestWidth = latestHeader.length();
        int maxDepsWidth = depsHeader.length();

        // Assemble list of packages (excluding beast2), keeping track of maximum field widths
        List<Package> packageList = new ArrayList<Package>();
        for (Package pkg : packageMap.values()) {
//            if (pkg.getName().equals(BEAST_PACKAGE))
//                continue;

            packageList.add(pkg);

            maxNameWidth = Math.max(pkg.getName().length(), maxNameWidth);
            maxStatusWidth = Math.max(pkg.isInstalled() ? pkg.getInstalledVersion().toString().length() : 2, maxStatusWidth);
            maxLatestWidth = Math.max(maxLatestWidth, pkg.isAvailable()
                            ? pkg.getLatestVersion().toString().length()
                            :  Math.max(2, maxStatusWidth));
            maxDepsWidth = Math.max(pkg.getDependenciesString().length(), maxDepsWidth);
        }

        // Assemble format strings
        String nameFormat = "%-" + (maxNameWidth) + "s";
        String statusFormat = "%-" + (maxStatusWidth) + "s";
        String latestFormat = "%-" + (maxLatestWidth) + "s";
        String depsFormat = "%-" + (maxDepsWidth) + "s";
        String sep = " | ";

        // Print headers
        ps.printf(nameFormat, nameHeader); ps.print(sep);
        ps.printf(statusFormat, statusHeader); ps.print(sep);
        ps.printf(latestFormat, latestHeader); ps.print(sep);
        ps.printf(depsFormat, depsHeader); ps.print(sep);
        ps.printf("%s\n", descriptionHeader);

        // Add horizontal rule under header
        int totalWidth = maxNameWidth + maxStatusWidth
                + maxLatestWidth + maxDepsWidth
                + descriptionHeader.length() + 4*3;
        for (int i=0; i<totalWidth; i++)
            ps.print("-");
        ps.println();


        // Print formatted package information
        for (Package pkg : packageList) {
        	if (pkg.getName().equals(BEAST_PACKAGE_NAME)) {
        		ps.printf(nameFormat, pkg.getName()); ps.print(sep);
		        ps.printf(statusFormat, pkg.isInstalled() ? pkg.getInstalledVersion() : "NA"); ps.print(sep);
		        ps.printf(latestFormat, pkg.isAvailable() ? pkg.getLatestVersion() : "NA"); ps.print(sep);
		        ps.printf(depsFormat, pkg.getDependenciesString()); ps.print(sep);
		        ps.printf("%s\n", pkg.getDescription());
        	}
        }
        for (int i=0; i<totalWidth; i++)
            ps.print("-");
        ps.println();
        
        // Print formatted package information
        for (Package pkg : packageList) {
        	if (!pkg.getName().equals(BEAST_PACKAGE_NAME)) {
	            ps.printf(nameFormat, pkg.getName()); ps.print(sep);
	            ps.printf(statusFormat, pkg.isInstalled() ? pkg.getInstalledVersion() : "NA"); ps.print(sep);
	            ps.printf(latestFormat, pkg.isAvailable() ? pkg.getLatestVersion() : "NA"); ps.print(sep);
	            ps.printf(depsFormat, pkg.getDependenciesString()); ps.print(sep);
	            ps.printf("%s\n", pkg.getDescription());
        	}
        }
    }


    private static void printUsageAndExit(Arguments arguments) {
        arguments.printUsage("packagemanager", "");
        Log.info.println("\nExamples:");
        Log.info.println("packagemanager -list");
        Log.info.println("packagemanager -add SNAPP");
        Log.info.println("packagemanager -useAppDir -add SNAPP");
        Log.info.println("packagemanager -del SNAPP");
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            Arguments arguments = new Arguments(
                    new Arguments.Option[]{
                            new Arguments.Option("list", "List available packages"),
                            new Arguments.StringOption("add", "NAME", "Install the <NAME> package"),
                            new Arguments.StringOption("del", "NAME", "Uninstall the <NAME> package"),
                            new Arguments.StringOption("version", "NAME", "Specify package version"),
                            new Arguments.Option("useAppDir", "Use application (system wide) installation directory. Note this requires writing rights to the application directory. If not specified, the user's BEAST directory will be used."),
                            new Arguments.StringOption("dir", "DIR", "Install/uninstall package in directory <DIR>. This overrides the useAppDir option"),
                            new Arguments.Option("help", "Show help"),
                            new Arguments.Option("update", "Check for updates, and ask to install if available"),
                            new Arguments.Option("updatenow", "Check for updates and install without asking"),
                    });
            try {
                arguments.parseArguments(args);
            } catch (Arguments.ArgumentException ae) {
                Log.info.println();
                Log.info.println(ae.getMessage());
                Log.info.println();
                printUsageAndExit(arguments);
            }

            if (args.length == 0 || arguments.hasOption("help")) {
                printUsageAndExit(arguments);
            }
            
            if (arguments.hasOption("update")) {
            	updatePackages(UpdateStatus.AUTO_CHECK_AND_ASK, false);
            	return;
            }

            if (arguments.hasOption("updatenow")) {
            	updatePackages(UpdateStatus.AUTO_UPDATE, false);
            	return;
            }

            boolean useAppDir = arguments.hasOption("useAppDir");
            String customDir = arguments.getStringOption("dir");
            if (customDir != null) {
                String path = PackageManager.getBeastPacakgePathProperty();
                System.setProperty("BEAST_PACKAGE_PATH", (path != null ? path + ":" : "") +customDir);
            }

            List<URL> urlList = getRepositoryURLs();
            Log.debug.println("Packages user path : " + getPackageUserDir());
            for (URL url : urlList) {
                Log.debug.println("Access URL : " + url);
            }
            Log.debug.print("Getting list of packages ...");
            Map<String, Package> packageMap = new TreeMap<String, Package>(new Comparator<String>() {
            	// String::compareToIgnoreCase
    			@Override
    			public int compare(String s1, String s2) {
    	        	return s1.toLowerCase().compareTo(s2.toLowerCase());
    			}
            });
            try {
                PackageManager.addInstalledPackages(packageMap);
                PackageManager.addAvailablePackages(packageMap);
            } catch (PackageListRetrievalException e) {
            	Log.warning.println(e.getMessage());
                if (e.getCause() instanceof IOException)
                    Log.warning.println(NO_CONNECTION_MESSAGE);
            	return;
            }
            Log.debug.println("Done!\n");

            if (arguments.hasOption("list")) {
                prettyPrintPackageInfo(Log.info, packageMap);
            }
            
            if (arguments.hasOption("add")) {
                String name = arguments.getStringOption("add");
                boolean processed = false;
                for (Package aPackage : packageMap.values()) {
                    if (aPackage.packageName.equals(name)) {
                        processed = true;
                        if (!aPackage.isInstalled() || arguments.hasOption("version")) {
                            Log.debug.println("Determine packages to install");
                            Map<Package, PackageVersion> packagesToInstall = new HashMap<Package, PackageVersion>();
                            if (arguments.hasOption("version")) {
                            	String versionString = arguments.getStringOption("version");
                            	PackageVersion version = new PackageVersion(versionString);
                            	packagesToInstall.put(aPackage, version);
                            	PackageManager.useArchive = true;
                            } else {
                            	packagesToInstall.put(aPackage, aPackage.getLatestVersion());
                            }
                            try {
                                populatePackagesToInstall(packageMap, packagesToInstall);
                            } catch (DependencyResolutionException ex) {
                                Log.err("Installation aborted: " + ex.getMessage());
                            }
                            Log.debug.println("Start installation");
                            prepareForInstall(packagesToInstall, useAppDir, customDir);
                            Map<String, String> dirs = installPackages(packagesToInstall, useAppDir, customDir);
                            for (String pkgName : dirs.keySet())
                                Log.info.println("Package " + pkgName + " is installed in " + dirs.get(pkgName) + ".");
                        } else {
                            Log.info.println("Installation aborted: " + name + " is already installed.");
                            System.exit(1);
                        }
                    }
                }
                if (!processed) {
                    Log.info.println("Could not find package '" + name + "' (typo perhaps?)");
                }
            }

            if (arguments.hasOption("del")) {
                String name = arguments.getStringOption("del");
                boolean processed = false;
                for (Package aPackage : packageMap.values()) {
                    if (aPackage.packageName.equals(name)) {
                        processed = true;
                        if (arguments.hasOption("version")) {
                        	PackageManager.useArchive = true;
                        	String versionString = arguments.getStringOption("version");
                        	PackageVersion version = new PackageVersion(versionString);
                            String dir = uninstallPackage(aPackage, version, useAppDir, customDir);
                            Log.info.println("Package " + name + " is uninstalled from " + dir + ".");
                        } else {
	                        if (aPackage.isInstalled()) {
	                            List<String> deps = getInstalledDependencyNames(aPackage, packageMap);
	                            if (deps.isEmpty()) {
	                                Log.debug.println("Start un-installation");
	                                String dir = uninstallPackage(aPackage, useAppDir, customDir);
	                                Log.info.println("Package " + name + " is uninstalled from " + dir + ".");
	                            } else {
	                                Log.info.println("Un-installation aborted: " + name + " is used by these other packages: " +
	                                        String.join(", ", deps) + ".");
	                                Log.info.println("Remove these packages first.");
	                                System.exit(1);
	                            }
	                        } else {
	                            Log.info.println("Un-installation aborted: " + name + " is not installed yet.");
	                            System.exit(1);
	                        }
                        }
                    }
                }
                if (!processed) {
                    Log.info.println("Could not find package '" + name + "' (typo perhaps?)");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** keep track of which class comes from a particular package.
     * It maps a full class name onto a package name + " v" + package version
     * e.g. "bModelTest v0.3.2"
     */
    private static Map<String, String> classToPackageMap = new HashMap<String, String>();
    
    /**  maps package name to a Package object, which contains info on whether 
     * and which version is installed. This is initialised when loadExternalJars()
     * is called, which happens at the start of BEAST, BEAUti and many utilities.
     */
    private static TreeMap<String, Package> packages = new TreeMap<String, Package>();
   
    public static Map<String, String> getClassToPackageMap() {
    	if (classToPackageMap.size() == 0) {
            for (String jarDirName : getBeastDirectories()) {
            	initPackageMap(jarDirName);
            }
    	}
    	return classToPackageMap;
    }

    private static void initPackageMap(String jarDirName) {
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
                    Log.warning.println("Loading package " + packageNameAndVersion);
                    Utils6.logToSplashScreen("Loading package " + packageNameAndVersion);
                } catch (Exception e) {
                    // too bad, won't print out any info

                    // File is called version.xml, but is not a Beast2 version file
                    // Log.debug.print("Skipping "+jarDirName+" (not a Beast2 package)");
                }
            }
            File jarDir = new File(jarDirName + "/lib");
            if (!jarDir.exists()) {
                jarDir = new File(jarDirName + "\\lib");
            }
            if (jarDir.exists() && jarDir.isDirectory()) {
                for (String fileName : jarDir.list()) {
                    if (fileName.endsWith(".jar")) {
                        Log.debug.print("Probing: " + fileName + " ");
                        // check that we are not reload existing classes
                        try {
                            JarInputStream jarFile = new JarInputStream
                                    (new FileInputStream(jarDir.getAbsolutePath() + "/" + fileName));
                            JarEntry jarEntry;
                            while ((jarEntry = jarFile.getNextJarEntry()) != null) {
                                if ((jarEntry.getName().endsWith(".class"))) {
                                    String className = jarEntry.getName().replaceAll("/", "\\.");
                                    className = className.substring(0, className.lastIndexOf('.'));
                                    if (packageNameAndVersion != null) {
                                        classToPackageMap.put(className, packageNameAndVersion);
                                    }
                                }
                            }
                            jarFile.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // File exists, but cannot open the file for some reason
            Log.debug.println("Skipping "+jarDirName+"/version.xml (unable to open file");
            Log.warning.println("Skipping "+jarDirName+"/version.xml (unable to open file");
        }
		
	}

	/** test whether a package with given name and version is available.
     * @param pkgname
     * @param pkgversion ignored for now
     * @return
     */
    // RRB: may need to return PackageStatus instead of boolean, but not sure yet how to handle
    // the case where a newer package is installed, and the old one is not available yet.
    //public static enum PackageStatus {NOT_INSTALLED, INSTALLED, INSTALLED_VERSION_NOT_AVAILABLE};
    public static boolean isInstalled(String pkgname, String pkgversion) {
		if (!packages.containsKey(pkgname)) {
			return false;
		}
		return true;
//		Package pkg = packages.get(pkgname);
//		PackageVersion version = new PackageVersion(pkgversion);
//		if (pkg.isAvailable(version)) {
//			return false;
//		}
	}
    
    /** check whether there are new packages to install, and if so install them
     * either after asking the user, or without asking (depending on updateStatus).
     * @param updateStatus
     */
    public static void updatePackages(UpdateStatus updateStatus, boolean useGUI) {
    	if (updateStatus == UpdateStatus.DO_NOT_CHECK) {
    		return;
    	}
    	
    	// find available and installed packages
        TreeMap<String, Package> packageMap = new TreeMap<String, Package>(
        		new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
	        	if (s1.equals(PackageManager.BEAST_PACKAGE_NAME)) {
	        		if (s2.equals(PackageManager.BEAST_PACKAGE_NAME)) {
	        			return 0;
	        		}
	        		return -1;
	        	}
	        	if (s2.equals(PackageManager.BEAST_PACKAGE_NAME)) {
	        		return 1;
	        	}
	        	return s1.compareToIgnoreCase(s2);
			}
        });
        try {
			addAvailablePackages(packageMap);
		} catch (PackageListRetrievalException e) {
			// cannot access list right now, so try again next time
			return;
		}
        addInstalledPackages(packageMap);

        // check whether any installed package has an update
        Map<Package, PackageVersion> packagesToInstall = new LinkedHashMap<Package, PackageVersion>();
        for (String packageName : packageMap.keySet()) {
        	Package _package = packageMap.get(packageName);
        	if (_package.isInstalled()) {
        		if (_package.getLatestVersion() != null && _package.getLatestVersion().compareTo(_package.getInstalledVersion()) > 0) {
        			packagesToInstall.put(_package, _package.getLatestVersion());
        		}
        	}
        }
        
        if (packagesToInstall.size() == 0) {
        	// nothing to install
        	return;
        }
         
        // do we need to ask before proceeding?
    	if (updateStatus != UpdateStatus.AUTO_UPDATE) {
    		if (useGUI) {
	    		StringBuilder buf = new StringBuilder();
	    		buf.append("<table><tr><td>Package name</td><td>New version</td><td>Installed</td></tr>");
	    		for (Package _package : packagesToInstall.keySet()) {
	    			buf.append("<tr><td>" + _package.packageName + "</td>"
	    					+ "<td>" + _package.getLatestVersion()+ "</td>"
	    					+ "<td>" + _package.getInstalledVersion() + "</td></tr>");
	    		}
	    		buf.append("</table>");
	    		String [] options = new String[]{"No, never check again", "Not now", "Yes", "Always install without asking"};
	    		int response = JOptionPane.showOptionDialog(null, "<html><h2>New pacakges are available to install:</h2>" +
	    				buf.toString() + 
	    				"Do you want to install?</html>", "Package Manager", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
	    		        null, options, options[2]);
	    		switch (response) {
	    		case 0: // No, never check again
	                Utils6.saveBeautiProperty("package.update.status", UpdateStatus.DO_NOT_CHECK.toString());
	    			return;
	    		case 1: // No, check later
	                Utils6.saveBeautiProperty("package.update.status", UpdateStatus.AUTO_CHECK_AND_ASK.toString());
	    			return;
	    		case 2: // Yes, ask next time
	                Utils6.saveBeautiProperty("package.update.status", UpdateStatus.AUTO_CHECK_AND_ASK.toString());
	    			break;
	    		case 3: // Always install automatically
	                Utils6.saveBeautiProperty("package.update.status", UpdateStatus.AUTO_UPDATE.toString());
	    			break;
	    		default: // e.g. escape-key gets us here
	    			return;
	    		}
    		} else {
    			Log.info("New pacakges are available to install:");
	    		Log.info("Package name\tNew version\tInstalled");
	    		for (Package _package : packagesToInstall.keySet()) {
	    			Log.info(_package.packageName + "\t" + _package.getLatestVersion()+ "\t" + _package.getInstalledVersion());
	    		}
    			Log.info("Do you want to install (y/n)?");
                Log.info.flush();
                final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));	                        
                String msg = "n";
				try {
					msg = stdin.readLine();
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
                if (!msg.toLowerCase().equals("y")) {
                	Log.info("Exiting now");
                	return;
                }
    		}
    	}
        
        // install packages that can be updated
        try {
			prepareForInstall(packagesToInstall, false, null);

	        if (getToDeleteListFile().exists()) {
	        	if (useGUI) {
	        		JOptionPane.showMessageDialog(null,
	                    "<html><body><p style='width: 200px'>Upgrading packages on your machine requires BEAUti " +
	                            "to restart. Shutting down now.</p></body></html>");
	        	} else {
                    Log.info("Upgrading packages on your machine requires BEAUti to restart.");
	        	}
	            System.exit(0);
	        }
	
	        Map<String,String> dirList = installPackages(packagesToInstall, false, null);
	        for (String packageName : dirList.keySet()) {
	        	Log.info("Installed " + packageName + " in " + dirList.get(packageName));
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public static String getBeastPacakgePathProperty() {
    	if (System.getProperty("BEAST_PACKAGE_PATH") != null) {
    		return System.getProperty("BEAST_PACKAGE_PATH");
    	}
    	if (System.getenv("BEAST_PACKAGE_PATH") != null) {
    		return System.getenv("BEAST_PACKAGE_PATH");
    	}
    	if (System.getenv("BEAST_ADDON_PATH") != null) {
    		return System.getenv("BEAST_ADDON_PATH");
    	}    	
    	return System.getenv("BEAST_ADDON_PATH");
    }
 }
