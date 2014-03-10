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


import beast.app.BEASTVersion;
import beast.app.util.Arguments;
import beast.app.util.Utils;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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

/**
 * This class is used to manage beast 2 add-ons, and can
 * - install a new add on
 * - un-install an add on
 * - list directories that may contain add ons
 * - load jars from installed add ons
 * - discover classes in add ons that implement a certain interface or a derived from a certain class
 */
// TODO: on windows allow installation on drive D: and pick up add-ons in drive C:
public class AddOnManager {
    public static final BEASTVersion beastVersion = new BEASTVersion();

    public final static String[] IMPLEMENTATION_DIR = {"beast", "snap"};
    public final static String TO_DELETE_LIST_FILE = "toDeleteList";
    public final static String PLUGINS_XML = "http://www.beast2.org/plugins" + beastVersion.getMajorVersion() + ".xml";
    @Deprecated
    public final static String PLUGINS_URL = "http://www.beast2.org/wiki/index.php/Add-ons2.1.0";

    public static final String INSTALLED = "installed";
    public static final String NOT_INSTALLED = "un-installed";
    public static final int PLUGIN_INTRO_INDEX = 0;
    public static final int PLUGIN_URL_INDEX = 1;
    public static final int PLUGIN_NAME_INDEX = 2;
    public static final int PLUGIN_STATUS_INDEX = 3;
    public static final int PLUGIN_VERSION_INDEX = 4;
    public static final int PLUGIN_DEPENDENCIES_INDEX = 5;

    /**
     * flag indicating add ons have been loaded at least once *
     */
    static boolean externalJarsLoaded = false;


    /**
     * list of all classes found in the class path *
     */
    private static List<String> all_classes;

    /**
     * return URLs containing list of downloadable plugins *
     */
    public static String[] getPluginsURL() throws MalformedURLException {
//        File localPlugins = new File(getPluginUserDir() + "/plugins.html");
//        URL localPluginsUrl = localPlugins.toURI().toURL();

        String url = PLUGINS_XML;

        File beastProps = new File(getPluginUserDir() + "/beauti.properties");
        // check beast.properties file exists in plugin directory
        if (beastProps.exists()) {
            Properties prop = new Properties();

            try {
                //load a properties file
                prop.load(new FileInputStream(beastProps));

                //# url
                //plugins.url=http://...
                url = prop.getProperty("plugins.url");

            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // if no plugins.url, assign url back to default
            if (url == null)
                url = PLUGINS_XML;
        }

        return new String[]{
                url,
//                localPluginsUrl.toString()
        };
    }

    /**
     * create list of plugins. The list is downloaded from a beast2 wiki page and
     * parsed.
     *
     * @return list of plugins, encoded as pairs of description, urls.
     * @throws Exception
     */
    public static List<Plugin> getPlugins() throws Exception {

        List<Plugin> plugins = new ArrayList<Plugin>();
        String[] sURLs = getPluginsURL();

        for (String sURL : sURLs) {
            URL url = new URL(sURL);
            InputStream is = url.openStream(); // throws an IOException

            if (sURL.endsWith(".xml")) {
                addPlugins(is, plugins);
            } else {
                //TODO from plugins.url?
            }

            is.close();

//            write plugin xml page, if received from internet
        }
        return plugins;
    }

    public static void addPlugins(InputStream is, List<Plugin> plugins) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(is));

        Element rootElement = document.getDocumentElement(); // <plugins>
        NodeList nodes = rootElement.getChildNodes();

        for(int i = 0; i < nodes.getLength(); i++){
            Node node = nodes.item(i);

            if(node instanceof Element){
                Plugin plugin = new Plugin((Element) node);
                plugins.add(plugin);
            }

        } // end for i

    }

    public static boolean containsPlugin(String pluginName, List<Plugin> plugins) throws Exception {
        for (Plugin plugin : plugins) {
            if (pluginName.equalsIgnoreCase(plugin.pluginName))
                return true;
        }
        return false;
    }

    /**
     * create list of addons. The list is downloaded from a beast2 wiki page and
     * parsed.
     *
     * @return list of addons, encoded as pairs of description, urls.
     * @throws Exception
     */
    @Deprecated
    public static List<List<String>> getAddOns() throws Exception {

        List<List<String>> addOns = new ArrayList<List<String>>();
        String[] sURLs = getPluginsURL();
        List<String> sBeastDirs = AddOnManager.getBeastDirectories();

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
            // parse WIKI xml for plugins
            String startMark = "<!-- bodytext -->";
            sText = sText.substring(sText.indexOf(startMark) + startMark.length());
            String[] sStrs = sText.split("</p>");
            for (int i = 0; i < sStrs.length - 1; i++) {
                sText = sStrs[i];
                sText = sText.replaceAll("<p>", "");
                String[] sStr2 = sText.split("<");


                List<String> addOn = new ArrayList<String>();
                addOn.add(sStr2[PLUGIN_INTRO_INDEX]);
                sStr2 = sStr2[PLUGIN_URL_INDEX].split("\"");
                addOn.add(sStr2[PLUGIN_URL_INDEX]);
                String sAddOnName = URL2AddOnName(sStr2[PLUGIN_URL_INDEX]);
                addOn.add(sAddOnName);
                addOn.add(NOT_INSTALLED);

                if (!containsAddOn(addOn.get(PLUGIN_NAME_INDEX), addOns)) {
                    for (String sDir : sBeastDirs) {
                        File f = new File(sDir + "/" + sAddOnName);
                        if (f.exists()) {
                            addOn.set(PLUGIN_STATUS_INDEX, INSTALLED);
                        }
                        f = new File(sDir + "/" + sAddOnName + "/version.xml");
                        if (f.exists()) {
                            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                            Document doc = factory.newDocumentBuilder().parse(f);
                            doc.normalize();
                            // get name and version of plugin
                            Element addon = doc.getDocumentElement();
                            String sAddonVersion = addon.getAttribute("version");
                            addOn.add(sAddonVersion);
                            NodeList nodes = doc.getElementsByTagName("depends");
                            String dependencies = "";
                            for (int j = 0; j < nodes.getLength(); j++) {
                                Element dependson = (Element) nodes.item(j);
                                String s = dependson.getAttribute("on");
                                if (!s.equals("beast2")) {
                                    dependencies +=  s + ", ";
                                }
                            }
                            if (dependencies.length() > 2) {
                                dependencies = dependencies.substring(0, dependencies.length() - 2);
                            }
                            addOn.add(dependencies);
                        }
                    }

                    addOns.add(addOn);
                }
            } // end for i
//            write plugin html page, if received from internet
        }
        return addOns;
    }
    @Deprecated
    public static boolean containsAddOn(String sAddOnName, List<List<String>> addOns) throws Exception {
        for (List<String> addOn : addOns) {
            if (sAddOnName.equalsIgnoreCase(addOn.get(PLUGIN_NAME_INDEX)))
                return true;
        }
        return false;
    }

    /**
     * download and unzip plugin from URL provided It is assumed the plugin
     * consists of a zip file containing directories /lib with jars used by the
     * add on /templates with beauti XML templates
     *
     * @param sURL
     * @param useAppDir if false, use user directory, otherwise use application directory
     * @throws Exception
     */
    public static String installAddOn(String sURL, boolean useAppDir, String customDir) throws Exception {
        if (!sURL.toLowerCase().endsWith(".zip")) {
            throw new Exception("Plugin should be packaged in a zip file");
        }
        String sName = URL2AddOnName(sURL);

        // create directory
        URL templateURL = new URL(sURL);
        ReadableByteChannel rbc = Channels.newChannel(templateURL.openStream());
        String sDir = (useAppDir ? getAddOnAppDir() : getPluginUserDir()) + "/" + sName;
        if (customDir != null) {
            sDir = customDir + "/" + sName;
        }
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
        return sDir;
    }

    public static String uninstallAddOn(String sURL, boolean useAppDir, String customDir) throws Exception {
        if (!sURL.toLowerCase().endsWith(".zip")) {
            throw new Exception("Plugin should be packaged in a zip file");
        }
        String sName = URL2AddOnName(sURL);
        String sDir = (useAppDir ? getAddOnAppDir() : getPluginUserDir()) + "/" + sName;
        if (customDir != null) {
            sDir = customDir + "/" + sName;
        }
        File dir = new File(sDir);
        List<File> deleteFailed = new ArrayList<File>();
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
        return sDir;
    }

    public static String URL2AddOnName(String sURL) {
        String sName = sURL.substring(sURL.lastIndexOf("/") + 1);
        if (sName.contains(".")) {
            sName = sName.substring(0, sName.indexOf("."));
        }
        return sName;
    }

    public static boolean checkIsInstalled(String pluginName) {
        boolean isInstalled = false;
        List<String> sBeastDirs = getBeastDirectories();
        for (String sDir : sBeastDirs) {
            File f = new File(sDir + "/" + pluginName);
            if (f.exists()) {
                isInstalled = true;
            }
        }
        return isInstalled;
    }

    /** pretty format plugin information in list of string form as produced by getAddOns() **/
    public static String formatPluginInfo(List<String> plugin) {
        StringBuffer buf = new StringBuffer();
        buf.append(plugin.get(PLUGIN_NAME_INDEX));
        if (plugin.get(PLUGIN_NAME_INDEX).length() < 12) {
            buf.append("             ".substring(plugin.get(PLUGIN_NAME_INDEX).length()));
        }
        buf.append(" (");
        if (plugin.size() > 4) {
            buf.append("v" + plugin.get(PLUGIN_VERSION_INDEX) + " " + plugin.get(PLUGIN_STATUS_INDEX));
            buf.append((plugin.get(PLUGIN_DEPENDENCIES_INDEX).length() > 0 ? " depends on " + plugin.get(PLUGIN_DEPENDENCIES_INDEX) : ""));
        } else {
            buf.append(plugin.get(PLUGIN_STATUS_INDEX));
        }
        buf.append(")" + ": " + plugin.get(PLUGIN_INTRO_INDEX).trim());
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
     * return directory where to install plugins for users *
     */
    public static String getPluginUserDir() {
        if (Utils.isWindows()) {
//            if (System.getenv("APPDATA") != null) {
//                return System.getenv("APPDATA") + "\\BEAST";
//            }
            return System.getProperty("user.home") + "\\BEAST\\" + beastVersion.getMajorVersion();
        }
        if (Utils.isMac()) {
            return System.getProperty("user.home") + "/Library/Application Support/BEAST/" + beastVersion.getMajorVersion();
        }
        // Linux and unices
        return System.getProperty("user.home") + "/.beast/" + beastVersion.getMajorVersion();
    }

    /**
     * return directory where system wide plugins reside *
     */
    public static String getAddOnAppDir() {
        if (Utils.isWindows()) {
            return "\\Program Files\\BEAST\\" + beastVersion.getMajorVersion();
        }
        if (Utils.isMac()) {
            return "/Library/Application Support/BEAST/" + beastVersion.getMajorVersion();
        }
        return "/usr/local/share/beast/" + beastVersion.getMajorVersion();
    }

    /**
     * @return file containing list of files that need to be deleted
     * but could not be deleted. This can happen when uninstalling plugins
     * on windows, which locks jar files loaded by java.
     */
    public static File getToDeleteListFile() {
        return new File(getPluginUserDir() + "/" + TO_DELETE_LIST_FILE);
    }

    /**
     * return list of directories that may contain plugins *
     */
    public static List<String> getBeastDirectories() {
        List<String> sDirs = new ArrayList<String>();
        // check if there is the BEAST environment variable is set
        if (System.getProperty("BEAST_ADDON_PATH") != null) {
            String sBEAST = System.getProperty("BEAST_ADDON_PATH");
            for (String sDir : sBEAST.split(":")) {
                sDirs.add(sDir);
            }
        }
        if (System.getenv("BEAST_ADDON_PATH") != null) {
            String sBEAST = System.getenv("BEAST_ADDON_PATH");
            for (String sDir : sBEAST.split(":")) {
                sDirs.add(sDir);
            }
        }

        // add user directory
        sDirs.add(System.getProperty("user.dir"));
        // add user plugin directory
        sDirs.add(getPluginUserDir());
        // add application plugin directory
        sDirs.add(getAddOnAppDir());

        // pick up directories in class path, useful when running in an IDE
        String strClassPath = System.getProperty("java.class.path");
        String [] paths = strClassPath.split(":");
        for (String path : paths) {
            if (!path.endsWith(".jar")) {
                path = path.replaceAll("\\\\","/");
                if (path.indexOf("/") >= 0) {
                    path = path.substring(0, path.lastIndexOf("/"));
                    // exclude Intellij build path out/production
                    if (!sDirs.contains(path) && !path.contains("production")) {
                        sDirs.add(path);
                    }
                }
            }
        }


        // subdirectories that look like they may contain an plugin
        // this is detected by checking the subdirectory contains a lib or
        // templates directory
        List<String> sSubDirs = new ArrayList<String>();
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
                                    if (sFile.endsWith("/lib") || sFile.endsWith("/templates") ||
                                            sFile.endsWith("\\lib") || sFile.endsWith("\\templates")) {
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
        // check version dependencies


        sDirs.addAll(sSubDirs);

        return sDirs;
    } // getBeastDirectories

    /**
     * load external jars in beast directories *
     */
    public static void loadExternalJars() throws Exception {
        processDeleteList();

        List<String> sDirs = getBeastDirectories();
        checkDependencies(sDirs);
        for (String sJarDir : sDirs) {
            File jarDir = new File(sJarDir + "/lib");
            if (!jarDir.exists()) {
                jarDir = new File(sJarDir + "\\lib");
            }
            if (jarDir.exists() && jarDir.isDirectory()) {
                for (String sFile : jarDir.list()) {
                    if (sFile.endsWith(".jar")) {
                        Log.debug.print("Probing: " + sFile + " ");
                        // check that we are not reload existing classes
                        String loadedClass = null;
                        try {
                            JarInputStream jarFile = new JarInputStream
                                    (new FileInputStream(jarDir.getAbsolutePath() + "/" + sFile));
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
                                        Object o = Class.forName(className);
                                        loadedClass = className;
                                    } catch (Exception e) {
                                        // TODO: handle exception
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                        @SuppressWarnings("deprecation")
                        URL url = new File(jarDir.getAbsolutePath() + "/" + sFile).toURL();
                        if (loadedClass == null) {
                            addURL(url);
                        } else {
                            Log.debug.println("Skip loading " + url + ": contains classs " + loadedClass + " that is already loaded");
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
                    String sStr = fin.readLine();
                    File file = new File(sStr);
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
     * all plugins. Version and dependency info is stored in a file
     *
     * @param sDirs
     */
    private static void checkDependencies(List<String> sDirs) {
        HashMap<String, Double> addonVersion = new HashMap<String, Double>();
        addonVersion.put("beast2", beastVersion.parseVersion(beastVersion.getVersion()));
        List<PluginDependency> dependencies = new ArrayList<PluginDependency>();

        // gather version and dependency info for all plugins
        for (String sDir : sDirs) {
            File version = new File(sDir + "/version.xml");
            if (version.exists()) {
                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    Document doc = factory.newDocumentBuilder().parse(version);
                    doc.normalize();
                    // get name and version of plugin
                    Element addon = doc.getDocumentElement();
                    String sAddon = addon.getAttribute("name");
                    String sAddonVersion = addon.getAttribute("version");
                    addonVersion.put(sAddon, beastVersion.parseVersion(sAddonVersion));

                    // get dependencies of add-n
                    NodeList nodes = doc.getElementsByTagName("depends");
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Element dependson = (Element) nodes.item(i);
                        PluginDependency dep = new PluginDependency();
                        dep.plugin = sAddon;
                        dep.dependson = dependson.getAttribute("on");
                        String sAtLeast = dependson.getAttribute("atleast");
                        dep.setAtLest(sAtLeast);
                        String sAtMost = dependson.getAttribute("atmost");
                        dep.setAtMost(sAtMost);
                        dependencies.add(dep);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // check dependencies
        for (PluginDependency dep : dependencies) {
            Double version = addonVersion.get(dep.dependson);
            if (version == null) {
                warning("Plugin " + dep.plugin + " requires another plugin (" + dep.dependson + ") which is not installed.\n" +
                        "Either uninstall " + dep.plugin + " or install the " + dep.dependson + " add on.");
            } else if (version > dep.atMost || version < dep.atLeast) {
                warning("Plugin " + dep.plugin + " requires another plugin (" + dep.dependson + ") with version in range " +
                        beastVersion.formatVersion(dep.atLeast) + " to " + beastVersion.formatVersion(dep.atMost) + " but " + dep.dependson + " has version " + beastVersion.formatVersion(version) + "\n" +
                        "Either uninstall " + dep.plugin + " or install the correct version of " + dep.dependson + ".");
            }
        }
    }

    private static void warning(String string) {
        System.out.println(string);
        System.out.println("Unexpected behavior may follow!");
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, string +
                    "\nUnexpected behavior may follow!");
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
        for (int i = 0; i < urls.length; i++) {
            if (urls[i].toString().toLowerCase().equals(u.toString().toLowerCase())) {
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
        String sJar = u + "";
        classpath += System.getProperty("path.separator") + sJar.substring(5);
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

                JarFile jar;
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
            String sStr = all_classes.get(i);
            sStr = sStr.substring(0, sStr.length() - 6);
            sStr = sStr.replaceAll(fileSep, ".");
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

        List<String> result = new ArrayList<String>();
        for (int i = all_classes.size() - 1; i >= 0; i--) {
            String sClass = all_classes.get(i);
            sClass = sClass.replaceAll("/", ".");
            //Log.debug.println(sClass + " " + pkgname);

            // must match package
            if (sClass.startsWith(pkgname)) {
                //Log.debug.println(sClass);
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
                    Log.debug.println("Checking class: " + sClass);
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
        System.out.println("\nExamples:");
        System.out.println("addonmanager -list");
        System.out.println("addonmanager -add SNAPP");
        System.out.println("addonmanager -useAppDir -add SNAPP");
        System.out.println("addonmanager -del SNAPP");
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            Arguments arguments = new Arguments(
                    new Arguments.Option[]{
                            new Arguments.Option("list", "List available plugins"),
                            new Arguments.StringOption("add", "NAME", "Install the <NAME> plugin "),
                            new Arguments.StringOption("del", "NAME", "Uninstall the <NAME> plugin "),
                            new Arguments.Option("useAppDir", "Use application (system wide) installation directory. Note this requires writing rights to the application directory. If not specified, the user's BEAST directory will be used."),
                            new Arguments.StringOption("dir", "DIR", "Install/uninstall plugin in directory <DIR>. This overrides the useAppDir option"),
                            new Arguments.Option("help", "Show help"),
                    });
            try {
                arguments.parseArguments(args);
            } catch (Arguments.ArgumentException ae) {
                System.out.println();
                System.out.println(ae.getMessage());
                System.out.println();
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

            String[] sURLs = getPluginsURL();
            Log.debug.println("Plugins user path : " + getPluginUserDir());
            for (String sURL : sURLs) {
                Log.debug.println("Access URL : " + sURL);
            }
            Log.debug.print("Getting list of plugins ...");
            List<List<String>> addOns = AddOnManager.getAddOns();
            Log.debug.println("Done!\n");

            if (arguments.hasOption("list")) {
                System.out.println("Name : status : Description ");
                for (List<String> addOn : addOns) {
                    System.out.println(formatPluginInfo(addOn));
                }
            }

            if (arguments.hasOption("add")) {
                String name = arguments.getStringOption("add");
                boolean processed = false;
                for (List<String> addOn : addOns) {
                    if (addOn.get(PLUGIN_NAME_INDEX).equals(name)) {
                        processed = true;
                        if (!addOn.get(PLUGIN_STATUS_INDEX).equals("installed")) {
                            Log.debug.println("Start installation");
                            String dir = installAddOn(addOn.get(PLUGIN_URL_INDEX), useAppDir, customDir);
                            System.out.println("Plugin " + name + " is installed in " + dir + ".");
                        } else {
                            System.out.println("Installation aborted: " + name + " is already installed.");
                            System.exit(0);
                        }
                    }
                }
                if (!processed) {
                    System.out.println("Could not find plugin '" + name + "' (typo perhaps?)");
                }
            }

            if (arguments.hasOption("del")) {
                String name = arguments.getStringOption("del");
                boolean processed = false;
                for (List<String> addOn : addOns) {
                    if (addOn.get(PLUGIN_NAME_INDEX).equals(name)) {
                        processed = true;
                        if (!addOn.get(PLUGIN_STATUS_INDEX).equals("not installed")) {
                            Log.debug.println("Start un-installation");
                            String dir = uninstallAddOn(addOn.get(PLUGIN_URL_INDEX), useAppDir, customDir);
                            System.out.println("Plugin " + name + " is uninstalled from " + dir + ".");
                        } else {
                            System.out.println("Un-installation aborted: " + name + " is not installed yet.");
                            System.exit(0);
                        }
                    }
                }
                if (!processed) {
                    System.out.println("Could not find plugin '" + name + "' (typo perhaps?)");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
