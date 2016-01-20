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


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
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
    //configuration file
    public final static String PACKAGES_XML = "https://raw.githubusercontent.com/CompEvol/CBAN/master/packages" + beastVersion.getVersion() + ".xml";

    public static final String INSTALLED = "installed";
    public static final String NOT_INSTALLED = "un-installed";
    
    public static final String NO_CONNECTION_MESSAGE = "Could not get an internet connection. "
    		+ "The BEAST Package Manager needs internet access in order to list available packages and download them for installation. "
    		+ "Possibly, some software (like security software, or a firewall) blocks the BEAST Package Manager.  "
    		+ "If so, you need to reconfigure such software to allow access.";


    /**
     * Exception thrown when reading a package repository fails.
     */
    public static class PackageListRetrievalException extends Exception {

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
    public static List<String> getPackagesURL() throws MalformedURLException {
        // Java 7 introduced SNI support which is enabled by default.
        // http://stackoverflow.com/questions/7615645/ssl-handshake-alert-unrecognized-name-error-since-upgrade-to-java-1-7-0
        System.setProperty("jsse.enableSNIExtension", "false");

        List<String> URLs = new ArrayList<>();
        // hack for v2.3.2 only -- to share repository file, saving lots of edits for package releases
        // next version should have single package file to cover all version
        URLs.add(PACKAGES_XML.replaceAll("2.3.2", "2.3.1"));

        File beastProps = new File(getPackageUserDir() + "/beauti.properties");
        // check beast.properties file exists in package directory
        if (beastProps.exists()) {
            Properties prop = new Properties();

            try {
                //load a properties file
                prop.load(new FileInputStream(beastProps));

                //# url
                //packages.url=http://...
                if (prop.containsKey("packages.url")) {
                    for (String userURL : prop.getProperty("packages.url").split(","))
                        URLs.add(userURL.trim());
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
     * @param URLs List of URLs.  The first is assumed to be the central
     * package repository and is thus ignored.
     */
    public static void savePackageURLs(List<String> URLs) {
        if (URLs.size()<1)
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
        if (URLs.size()>1) {
            StringBuilder sb = new StringBuilder("");
            for (int i=1; i<URLs.size(); i++) {
                if (i>1)
                    sb.append(",");
                sb.append(URLs.get(i));
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
     * create list of packages. The list is downloaded from a beast2 wiki page and
     * parsed.
     *
     * @return list of packages, encoded as pairs of description, urls.
     * @throws PackageListRetrievalException
     * @throws  MalformedURLException
     */
    public static List<Package> getPackages() throws PackageListRetrievalException, MalformedURLException {

        List<Package> packages = new ArrayList<>();
        List<String> uRLs = getPackagesURL();


        List<String> brokenPackageRepositories = new ArrayList<>();
        Exception firstException = null;

        for (String sURL : uRLs) {
            URL url = new URL(sURL);

            try (InputStream is = url.openStream()) {

                if (sURL.endsWith(".xml")) {
                    addPackages(is, packages);
                }

            } catch (IOException | ParserConfigurationException | SAXException e) {
                if (brokenPackageRepositories.isEmpty())
                    firstException = e;

                brokenPackageRepositories.add(sURL);
            }
//            write package xml page, if received from internet
        }

        if (!brokenPackageRepositories.isEmpty()) {
            String message = "Error reading the following package repository URLs:";
            for (String url : brokenPackageRepositories)
                message += " " + url;

            throw new PackageListRetrievalException(message, firstException);
        }

        // Ensure package list is in alphabetical order
        Collections.sort(packages, (p1, p2) -> p1.packageName.toLowerCase().compareTo(p2.packageName.toLowerCase()));
        
        return packages;
    }

    public static void addPackages(InputStream is, List<Package> packages) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(is));

        Element rootElement = document.getDocumentElement(); // <packages>
        NodeList nodes = rootElement.getChildNodes();

        for(int i = 0; i < nodes.getLength(); i++){
            Node node = nodes.item(i);

            if(node instanceof Element){
                Package aPackage = new Package((Element) node);
                if (!containsPackage(aPackage.packageName, packages))
                    packages.add(aPackage);
                else
                    message("Multiple packages with name "
                            + "'" + aPackage.packageName + "' "
                            + "found in repositories.  Selecting first (version "
                            + getPackage(aPackage.packageName, packages).latestVersion
                            + ").");
            }

        } // end for i

    }

    public static Package getPackage(String packageName, List<Package> packages) {
        for (Package aPackage : packages) {
            if (packageName.equalsIgnoreCase(aPackage.packageName))
                return aPackage;
        }
        return null;
    }

    public static boolean containsPackage(String packageName, List<Package> packages) {
        for (Package aPackage : packages) {
            if (packageName.equalsIgnoreCase(aPackage.packageName))
                return true;
        }
        return false;
    }

    /**
     * download and unzip package from URL provided It is assumed the package
     * consists of a zip file containing directories /lib with jars used by the
     * add on /templates with beauti XML templates
     *
     *
     * @param aPackage
     * @param useAppDir if false, use user directory, otherwise use application directory
     * @param customDir
     * @param packages  if not null, install all dependent packages of aPackage, but not work for customDir != null
     * @throws Exception
     */
    public static String installPackage(Package aPackage, boolean useAppDir, String customDir, List<Package> packages) throws IOException {
        if (!aPackage.url.toLowerCase().endsWith(".zip")) {
            throw new IOException("Package should be packaged in a zip file");
        }
//        String name = URL2PackageName(uRL); // not safe to use
        String name = aPackage.packageName;

        // install all dependent packages
        if (customDir == null && packages != null) {
            for (PackageDependency packageDependency : aPackage.dependencies) {
                String s = packageDependency.dependson;
                if (!s.equals("beast2")) {
                    Package pDent = getPackage(s, packages);
                    installPackage(pDent, useAppDir, null, packages);
                }
            }
        }

        // create directory
        URL templateURL = new URL(aPackage.url);
        ReadableByteChannel rbc = Channels.newChannel(templateURL.openStream());
        String dirName = (useAppDir ? getPackageSystemDir() : getPackageUserDir()) + "/" + name;
        if (customDir != null) {
            dirName = customDir + "/" + name;
        }
        File dir = new File(dirName);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Could not create template directory " + dirName);
            }
        }
        // grab file from URL
        String zipFile = dirName + "/" + name + ".zip";
        FileOutputStream fos = new FileOutputStream(zipFile);
        fos.getChannel().transferFrom(rbc, 0, 1 << 24);

        // unzip archive
        doUnzip(zipFile, dirName);
        // refresh classes
        loadExternalJars();
        fos.close();
        return dirName;
    }

    public static String uninstallPackage(Package aPackage, boolean useAppDir, String customDir, List<Package> packages, boolean autoUninstall) throws IOException {
        if (!aPackage.url.toLowerCase().endsWith(".zip")) {
            throw new IOException("Package should be packaged in a zip file");
        }
//        String name = URL2PackageName(uRL);
        String name = aPackage.packageName;

        // uninstall all dependent packages
        if (customDir == null && packages != null) {
            for (Package p : packages) {
                if (p.dependsOn(aPackage.packageName) && p.isInstalled()) {
                    if (autoUninstall) {
                        uninstallPackage(p, useAppDir, null, packages, true);
                    } else {
                        warning("Installed package (" + p.packageName + ") depends on the package (" + aPackage.packageName + "),\n" +
                                "which will not work if " + aPackage.packageName + "is uninstalled.");
                        return null;
                    }
                }
            }
        }

        String dirName = (useAppDir ? getPackageSystemDir() : getPackageUserDir()) + "/" + name;
        if (customDir != null) {
            dirName = customDir + "/" + name;
        }
        File dir = new File(dirName);
        List<File> deleteFailed = new ArrayList<>();
        deleteRecursively(dir, deleteFailed);

        // write deleteFailed to file
        if (deleteFailed.size() > 0) {
            File toDeleteList = getToDeleteListFile();
            FileWriter outfile= new FileWriter(toDeleteList, true);
            for (File file : deleteFailed) {
                outfile.write(file.getAbsolutePath() + "\n");
            }
            outfile.close();
        }
        return dirName;
    }

    public static boolean checkIsInstalled(String packageName) {
        boolean isInstalled = false;
        List<String> beastDirs = getBeastDirectories();
        for (String dirName : beastDirs) {
            File f = new File(dirName + "/" + packageName);
            if (f.exists()) {
                isInstalled = true;
            }
        }
        return isInstalled;
    }

    /** pretty format aPackage information in list of string form as produced by getAddOns() **/
    public static String formatPackageInfo(Package aPackage) {
        StringBuilder buf = new StringBuilder();
        buf.append(aPackage.packageName);
        if (aPackage.packageName.length() < 12) {
            buf.append("             ".substring(aPackage.packageName.length()));
        }
        buf.append(" (");
        if (aPackage.isInstalled()) {
            buf.append("v");
        }
        buf.append(aPackage.getStatus());
        buf.append(") : latest version " + aPackage.latestVersion);
        buf.append((aPackage.getDependenciesString().length() > 0 ? " : depends on " + aPackage.getDependenciesString() : ""));
        buf.append(" : " + aPackage.description.trim());
        return buf.toString();
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

        List<String> dirs = getBeastDirectories();
        checkDependencies(dirs);
        for (String jarDirName : dirs) {
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


    /** try to delete files that could not be deleted earlier **/
    private static void processDeleteList() {
        File toDeleteLisFile = getToDeleteListFile();
        if (toDeleteLisFile.exists()) {
            try {
                BufferedReader fin = new BufferedReader(new FileReader(toDeleteLisFile));
                while (fin.ready()) {
                    String str = fin.readLine();
                    File file = new File(str);
                    file.delete();
                }
                fin.close();
                toDeleteLisFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * go through list of directories collecting version and dependency information for
     * all packages. Version and dependency info is stored in a file
     *
     * @param dirs
     */
    private static void checkDependencies(List<String> dirs) {
        HashMap<String, Double> packageVersion = new HashMap<>();
        packageVersion.put("beast2", beastVersion.parseVersion(beastVersion.getVersion()));
        List<PackageDependency> dependencies = new ArrayList<>();

        // gather version and dependency info for all packages
        for (String dirName : dirs) {
            File version = new File(dirName + "/version.xml");
            if (version.exists()) {
                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    Document doc = factory.newDocumentBuilder().parse(version);
                    doc.normalize();
                    // get name and version of package
                    Element addon = doc.getDocumentElement();
                    String addonName = addon.getAttribute("name");
                    String addonVersion = addon.getAttribute("version");
                    packageVersion.put(addonName, beastVersion.parseVersion(addonVersion));

                    // get dependencies of add-n
                    NodeList nodes = doc.getElementsByTagName("depends");
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Element dependson = (Element) nodes.item(i);
                        PackageDependency dep = new PackageDependency();
                        dep.packageName = addonName;
                        dep.dependson = dependson.getAttribute("on");
                        String atLeastString = dependson.getAttribute("atleast");
                        dep.setAtLest(atLeastString);
                        String atMostString = dependson.getAttribute("atmost");
                        dep.setAtMost(atMostString);
                        dependencies.add(dep);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // check dependencies
        for (PackageDependency dep : dependencies) {
            Double version = packageVersion.get(dep.dependson);
            if (version == null) {
                warning("Package " + dep.packageName + " requires another package (" + dep.dependson + ") which is not installed.\n" +
                        "Either uninstall " + dep.packageName + " or install the " + dep.dependson + " add on.");
            } else if (version > dep.atMost || version < dep.atLeast) {
                warning("Package " + dep.packageName + " requires another package (" + dep.dependson + ") with version in range " +
                        beastVersion.formatVersion(dep.atLeast) + " to " + beastVersion.formatVersion(dep.atMost) + " but " + dep.dependson + " has version " + beastVersion.formatVersion(version) + "\n" +
                        "Either uninstall " + dep.packageName + " or install the correct version of " + dep.dependson + ".");
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
            method.invoke(sysLoader, new Object[]{u});
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
        Collections.sort(result); //, new StringCompare());

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
        Collections.sort(result); //, new StringCompare());
        // remove duplicates
        for (int i = result.size() - 1; i > 0; i--) {
            if (result.get(i).equals(result.get(i - 1))) {
                result.remove(i);
            }
        }

        return result;
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

            List<String> uRLs = getPackagesURL();
            Log.debug.println("Packages user path : " + getPackageUserDir());
            for (String uRL : uRLs) {
                Log.debug.println("Access URL : " + uRL);
            }
            Log.debug.print("Getting list of packages ...");
            List<Package> packages = null;
            try {
            	packages = AddOnManager.getPackages();
            } catch (PackageListRetrievalException e) {
            	Log.warning.println(e.getMessage());
                if (e.getCause() instanceof IOException)
                    Log.warning.println(NO_CONNECTION_MESSAGE);
            	return;
            }
            Log.debug.println("Done!\n");

            if (arguments.hasOption("list")) {
                Log.info.println("Name : status : Description ");
                for (Package aPackage : packages) {
                    Log.info.println(formatPackageInfo(aPackage));
                }
            }

            if (arguments.hasOption("add")) {
                String name = arguments.getStringOption("add");
                boolean processed = false;
                for (Package aPackage : packages) {
                    if (aPackage.packageName.equals(name)) {
                        processed = true;
                        if (!aPackage.isInstalled()) {
                            Log.debug.println("Start installation");
                            String dir = installPackage(aPackage, useAppDir, customDir, null);
                            Log.info.println("Package " + name + " is installed in " + dir + ".");
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
                for (Package aPackage : packages) {
                    if (aPackage.packageName.equals(name)) {
                        processed = true;
                        if (aPackage.isInstalled()) {
                            Log.debug.println("Start un-installation");
                            String dir = uninstallPackage(aPackage, useAppDir, customDir, null, false);
                            Log.info.println("Package " + name + " is uninstalled from " + dir + ".");
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
