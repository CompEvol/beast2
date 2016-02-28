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

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import beast.app.BEASTVersion;
import beast.app.beastapp.BeastMain;
import beast.app.util.Arguments;
import beast.app.util.Utils;
import beast.core.Description;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;

/**
 * This class is used to manage beast 2 add-ons, and can
 * - install a new add on
 * - un-install an add on
 * - list directories that may contain add ons
 * - load jars from installed add ons
 * - discover classes in add ons that implement a certain interface or a derived from a certain class
 */
// TODO: on windows allow installation on drive D: and pick up add-ons in drive C:
@Description("Manage all BEAUti packages and list their dependencies")
public class AddOnManager {
    public static final BEASTVersion beastVersion = new BEASTVersion();

    public final static String[] IMPLEMENTATION_DIR = {"beast", "snap"};
    public final static String TO_DELETE_LIST_FILE = "toDeleteList";
    public final static String TO_INSTALL_LIST_FILE = "toInstallList";
    public final static String BEAST_PACKAGE = "BEAST";

    public final static String PACKAGES_XML = "https://raw.githubusercontent.com/CompEvol/CBAN/master/packages.xml";
//    public final static String PACKAGES_XML = "file:///Users/remco/workspace/beast2/packages.xml";

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

        List<URL> URLs = new ArrayList<>();
        URLs.add(new URL(PACKAGES_XML));

        // check beast.properties file exists in package directory
        File beastProps = new File(getPackageUserDir() + "/beauti.properties");
        if (beastProps.exists()) {
            Properties prop = new Properties();

            try {
                //load a properties file
                prop.load(new FileInputStream(beastProps));

                //# url
                //packages.url=http://...
                if (prop.containsKey("packages.url")) {
                    for (String userURLString : prop.getProperty("packages.url").split(",")) {
                        URLs.add(new URL(userURLString));
                    }
                }

            } catch (IOException ex) {
                ex.printStackTrace();
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
        if (urls.size()<1)
            return;
        
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
                ex.printStackTrace();
            }
        }
        
        // Modify property
        if (urls.size()>1) {
            StringBuilder sb = new StringBuilder("");
            for (int i=1; i<urls.size(); i++) {
                if (i>1)
                    sb.append(",");
                sb.append(urls.get(i));
            }
            
            prop.setProperty("packages.url", sb.toString());
        } else {
            prop.remove("packages.url");
        }

        // Write properties file
        try {
            prop.store(new FileOutputStream(propsFile),
                    "Automatically-generated by BEAUti.\n");
        } catch (IOException ex) {
            Log.err(ex.getMessage());
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
                Element addon = doc.getDocumentElement();
                String packageName = addon.getAttribute("name");
                String packageVersionString = addon.getAttribute("version");

                Package pkg;
                if (packageMap.containsKey(packageName)) {
                    pkg = packageMap.get(packageName);
                } else {
                    pkg = new Package(packageName);
                    packageMap.put(packageName, pkg);
                }

                PackageVersion installedVersion = new PackageVersion(packageVersionString);
                Set<PackageDependency> installedVersionDependencies =
                        new TreeSet<>((o1, o2) -> o1.dependencyName.compareTo(o2.dependencyName));

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

            } catch (ParserConfigurationException | SAXException | IOException e) {
                e.printStackTrace();
            }
        }

        // Manually set currently-installed BEAST 2 version (won't have to do this soon!)

//        Package beastPkg;
//        if (packageMap.containsKey(BEAST_PACKAGE))
//            beastPkg = packageMap.get(BEAST_PACKAGE);
//        else {
//            beastPkg = new Package(BEAST_PACKAGE);
//            packageMap.put(BEAST_PACKAGE, beastPkg);
//        }
//
//        PackageVersion beastPkgVersion = new PackageVersion(beastVersion.getVersion());
//        Set<PackageDependency> beastPkgDeps = new TreeSet<>();
//        beastPkg.setInstalled(beastPkgVersion, beastPkgDeps);
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

        List<URL> brokenPackageRepositories = new ArrayList<>();
        Exception firstException = null;

        for (URL url : urls) {
            try (InputStream is = url.openStream()) {
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
                            packageMap.put(packageName, pkg);
                        }
                        pkg.setDescription(element.getAttribute("description"));

                        PackageVersion packageVersion = new PackageVersion(element.getAttribute("version"));
                        Set<PackageDependency> packageDependencies = new HashSet<>();

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
                    }
                }
            } catch (IOException | ParserConfigurationException | SAXException e) {
                if (brokenPackageRepositories.isEmpty())
                    firstException = e;

                brokenPackageRepositories.add(url);
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

        Map<Package, PackageVersion> ptiCopy = new HashMap<>(packagesToInstall);
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
            try (PrintStream ps = new PrintStream(getToInstallListFile())) {
                for (Map.Entry<Package, PackageVersion> entry : packagesToInstall.entrySet()) {
                    ps.println(entry.getKey() + ":" + entry.getValue());
                }
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
        Map<String, String> dirList = new HashMap<>();

        for (Map.Entry<Package, PackageVersion> entry : packagesToInstall.entrySet()) {
            Package thisPkg = entry.getKey();
            PackageVersion thisPkgVersion = entry.getValue();

            // create directory
            URL templateURL = thisPkg.getVersionURL(thisPkgVersion);
            ReadableByteChannel rbc = Channels.newChannel(templateURL.openStream());
            String dirName = (useAppDir ? getPackageSystemDir() : getPackageUserDir()) + "/" + thisPkg.getName();
            if (customDir != null) {
                dirName = customDir + "/" + thisPkg.getName();
            }
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

        return dirList;
    }

    /**
     * Get list of installed packages that depend on pkg.
     *
     * @param pkg package for which to retrieve installed dependencies
     * @param packageMap package database
     * @return list of names of installed packages dependent on pkg.
     */
    public static List<String> getInstalledDependencyNames(Package pkg, Map<String, Package> packageMap) {

        List<String> dependencies = new ArrayList<>();

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

        String dirName = (useAppDir ? getPackageSystemDir() : getPackageUserDir()) + "/" + pkg.getName();
        if (customDir != null) {
            dirName = customDir + "/" + pkg.getName();
        }
        File dir = new File(dirName);
        List<File> deleteFailed = new ArrayList<>();
        deleteRecursively(dir, deleteFailed);

        // write deleteFailed to file
        if (deleteFailed.size() > 0) {
            File toDeleteList = getToDeleteListFile();
            FileWriter outfile = new FileWriter(toDeleteList, true);
            for (File file : deleteFailed) {
                outfile.write(file.getAbsolutePath() + "\n");
            }
            outfile.close();
        }
        return dirName;
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
        
        if (Utils.isWindows()) {
            return System.getProperty("user.home") + "\\BEAST\\" + beastVersion.getMajorVersion();
        }
        if (Utils.isMac()) {
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
        
        if (Utils.isWindows()) {
            return "\\Program Files\\BEAST\\" + beastVersion.getMajorVersion();
        }
        if (Utils.isMac()) {
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

        URL u = BeastMain.class.getProtectionDomain().getCodeSource().getLocation();
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

            Map<Package, PackageVersion>  packagesToInstall = new HashMap<>();
            try (BufferedReader fin = new BufferedReader(new FileReader(toInstallListFile))) {
                String line;
                while ((line = fin.readLine()) != null) {
                    String[] nameVerPair = line.split(":");

                    Package pkg = packageMap.get(nameVerPair[0]);
                    PackageVersion ver = new PackageVersion(nameVerPair[1]);
                    packagesToInstall.put(pkg, ver);
                }
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
        List<String> dirs = new ArrayList<>();
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
        List<String> subDirs = new ArrayList<>();
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
        // check version dependencies


        dirs.addAll(subDirs);

        return dirs;
    } // getBeastDirectories

    /**
     * load external jars in beast directories *
     */
    public static void loadExternalJars() throws IOException {
        processDeleteList();

        TreeMap<String, Package> packages = new TreeMap<>();
        addInstalledPackages(packages);

        processInstallList(packages);

        checkInstalledDependencies(packages);

        for (String jarDirName : getBeastDirectories()) {
            File versionFile = new File(jarDirName + "/version.xml");
            if (versionFile.exists()) {
                try {
                    // print name and version of package
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    Document doc = factory.newDocumentBuilder().parse(versionFile);
                    Element addon = doc.getDocumentElement();
                    Log.info.println("Loading package " + addon.getAttribute("name") + " v" + addon.getAttribute("version"));
                } catch (Exception e) {
                	// too bad, won't print out any info
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
                                        /*Object o =*/ Class.forName(className);
                                        loadedClass = className;
                                    } catch (Exception e) {
                                        // TODO: handle exception
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
        }
        externalJarsLoaded = true;
        Alignment.findDataTypes();
    } // loadExternalJars

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

        Map<Package, PackageVersion> copy = new HashMap<>(packagesToInstall);

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
        Map<PackageDependency,Package> dependencies = new HashMap<>();

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
        AddOnManager clu = new AddOnManager();
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

        all_classes = new ArrayList<>();
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

        result = new ArrayList<>();

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

        result = new ArrayList<>();

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

        result = new ArrayList<>();

        names = new HashSet<>();
        for (i = 0; i < pkgnames.length; i++) {
            names.addAll(find(cls, pkgnames[i]));
        }

        // sort result
        result.addAll(names);
        Collections.sort(result, (s1, s2) -> {
        	if (s1.equals(BEAST_PACKAGE)) {
        		return -1;
        	}
        	if (s2.equals(BEAST_PACKAGE)) {
        		return 1;
        	}
        	return s1.compareTo(s2);
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

        List<String> result = new ArrayList<>();
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
        Collections.sort(result, (s1, s2) -> {
        	if (s1.equals(BEAST_PACKAGE)) {
        		return -1;
        	}
        	if (s2.equals(BEAST_PACKAGE)) {
        		return 1;
        	}
        	return s1.compareTo(s2);
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
        String statusHeader = "Installation Status";
        String latestHeader = "Latest Version";
        String depsHeader = "Dependencies";
        String descriptionHeader = "Description";

        int maxNameWidth = nameHeader.length();
        int maxStatusWidth = statusHeader.length();
        int maxLatestWidth = latestHeader.length();
        int maxDepsWidth = depsHeader.length();

        // Assemble list of packages (excluding beast2), keeping track of maximum field widths
        List<Package> packageList = new ArrayList<>();
        for (Package pkg : packageMap.values()) {
//            if (pkg.getName().equals(BEAST_PACKAGE))
//                continue;

            packageList.add(pkg);

            maxNameWidth = Math.max(pkg.getName().length(), maxNameWidth);
            maxStatusWidth = Math.max(pkg.getStatusString().length(), maxStatusWidth);
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
        	if (pkg.getName().equals(BEAST_PACKAGE)) {
        		ps.printf(nameFormat, pkg.getName()); ps.print(sep);
		        ps.printf(statusFormat, pkg.getStatusString()); ps.print(sep);
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
        	if (!pkg.getName().equals(BEAST_PACKAGE)) {
	            ps.printf(nameFormat, pkg.getName()); ps.print(sep);
	            ps.printf(statusFormat, pkg.getStatusString()); ps.print(sep);
	            ps.printf(latestFormat, pkg.isAvailable() ? pkg.getLatestVersion() : "NA"); ps.print(sep);
	            ps.printf(depsFormat, pkg.getDependenciesString()); ps.print(sep);
	            ps.printf("%s\n", pkg.getDescription());
        	}
        }
    }


    private static void printUsageAndExit(Arguments arguments) {
        arguments.printUsage("addonmanager", "");
        Log.info.println("\nExamples:");
        Log.info.println("addonmanager -list");
        Log.info.println("addonmanager -add SNAPP");
        Log.info.println("addonmanager -useAppDir -add SNAPP");
        Log.info.println("addonmanager -del SNAPP");
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            Arguments arguments = new Arguments(
                    new Arguments.Option[]{
                            new Arguments.Option("list", "List available packages"),
                            new Arguments.StringOption("add", "NAME", "Install the <NAME> package "),
                            new Arguments.StringOption("del", "NAME", "Uninstall the <NAME> package "),
                            new Arguments.Option("useAppDir", "Use application (system wide) installation directory. Note this requires writing rights to the application directory. If not specified, the user's BEAST directory will be used."),
                            new Arguments.StringOption("dir", "DIR", "Install/uninstall package in directory <DIR>. This overrides the useAppDir option"),
                            new Arguments.Option("help", "Show help"),
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

            boolean useAppDir = arguments.hasOption("useAppDir");
            String customDir = arguments.getStringOption("dir");
            if (customDir != null) {
                String path = System.getProperty("BEAST_ADDON_PATH");
                System.setProperty("BEAST_ADDON_PATH", (path != null ? path + ":" : "") +customDir);
            }

            List<URL> urlList = getRepositoryURLs();
            Log.debug.println("Packages user path : " + getPackageUserDir());
            for (URL url : urlList) {
                Log.debug.println("Access URL : " + url);
            }
            Log.debug.print("Getting list of packages ...");
            Map<String, Package> packageMap = new TreeMap<>(String::compareToIgnoreCase);
            try {
                AddOnManager.addInstalledPackages(packageMap);
                AddOnManager.addAvailablePackages(packageMap);
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
                        if (!aPackage.isInstalled()) {
                            Log.debug.println("Determine packages to install");
                            Map<Package, PackageVersion> packagesToInstall = new HashMap<>();
                            packagesToInstall.put(aPackage, aPackage.getLatestVersion());
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
                if (!processed) {
                    Log.info.println("Could not find package '" + name + "' (typo perhaps?)");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 }
