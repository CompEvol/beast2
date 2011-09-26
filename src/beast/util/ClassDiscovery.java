
/*
 * File ClassDiscovery.java
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
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * ClassDiscovery.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package beast.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class is used for discovering classes that implement a certain
 * interface or a derived from a certain class.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 5953 $
 * @see StringCompare
 */
public class ClassDiscovery {
  public final static String[] IMPLEMENTATION_DIR = {"beast","snap"};

  
  /** return URLs containing list of downloadable add-ons **/
  public static String [] getAddOnURL() {
	  return new String [] {"http://beast2.cs.auckland.ac.nz/index.php/Add-ons"};
  }
  
  /** create list of addons. 
   * The list is downloaded from a beast2 wiki page and parsed. 
   * @return list of addons, encoded as pairs of description, urls.
   * @throws Exception
   */
  public static List<List<String>> getAddOns() throws Exception {
      List<List<String>> addOns = new ArrayList<List<String>>();
      String [] sURLs = getAddOnURL();
      
      for (String sURL : sURLs) {
	      URL url = new URL(sURL);
	      InputStream is = url.openStream();  // throws an IOException
	      DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
	
	      StringBuffer buf = new StringBuffer();
	      String sLine = "";
	      while ((sLine= dis.readLine()) != null) {
	    	  buf.append(sLine);
	      }
	      is.close();
	      String sText = buf.toString();
	      // parse WIKI xml for add-ons
	      sText = sText.substring(sText.indexOf("<!-- bodytext -->") + 18);
	      String [] sStrs = sText.split("</p>");
	      for (int i = 0; i < sStrs.length - 1; i++) {
	    	  List<String> addOn = new ArrayList<String>();
	    	  sText = sStrs[i];
	    	  sText = sText.replaceAll("<p>", "");
	    	  String [] sStr2 = sText.split("<");
	    	  addOn.add(sStr2[0]);
	    	  sStr2 = sStr2[1].split("\"");
	    	  addOn.add(sStr2[1]);
	    	  addOns.add(addOn);
	      }
      }
      return addOns;
  }
  
  /** download and unzip add-on from URL provided 
   * It is assumed the add-on consists of a zip file containing directories
   * /lib with jars used by the add on
   * /templates with beauti XML templates 
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
		  File [] files = file.listFiles();
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
  public static void doUnzip(String inputZip, String destinationDirectory)
          throws IOException {
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
                  BufferedInputStream is =
                          new BufferedInputStream(zipFile.getInputStream(entry));
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
	  sDirs.add(System.getProperty("user.dir"));
	  sDirs.add(System.getProperty("user.home") + "/.beast2");
	  return sDirs;
  }

  /** whether to output some debug information. */
  public final static boolean VERBOSE = false;

  /** for caching queries (classname+packagename &lt;-&gt; List with classnames). */
  protected static Hashtable<String,List<String>> m_Cache;

  /** notify if VERBOSE is still on */
  static {
    if (VERBOSE)
      System.err.println(ClassDiscovery.class.getName() + ": VERBOSE ON");
  }

  /**
   * Checks whether the "otherclass" is a subclass of the given "superclass".
   *
   * @param superclass      the superclass to check against
   * @param otherclass      this class is checked whether it is a subclass
   *                        of the the superclass
   * @return                TRUE if "otherclass" is a true subclass
   */
  public static boolean isSubclass(String superclass, String otherclass) {
    try {
      return isSubclass(Class.forName(superclass), Class.forName(otherclass));
    }
    catch (Exception e) {
      return false;
    }
  }

  /**
   * Checks whether the "otherclass" is a subclass of the given "superclass".
   *
   * @param superclass      the superclass to check against
   * @param otherclass      this class is checked whether it is a subclass
   *                        of the the superclass
   * @return                TRUE if "otherclass" is a true subclass
   */
  public static boolean isSubclass(Class<?> superclass, Class<?> otherclass) {
    Class<?>       currentclass;
    boolean     result;

    result       = false;
    currentclass = otherclass;
    do {
      result = currentclass.equals(superclass);

      // topmost class reached?
      if (currentclass.equals(Object.class))
        break;

      if (!result)
        currentclass = currentclass.getSuperclass();
    }
    while (!result);

    return result;
  }

  /**
   * Checks whether the given class implements the given interface.
   *
   * @param intf      the interface to look for in the given class
   * @param cls       the class to check for the interface
   * @return          TRUE if the class contains the interface
   */
  public static boolean hasInterface(String intf, String cls) {
    try {
      return hasInterface(Class.forName(intf), Class.forName(cls));
    }
    catch (Exception e) {
      return false;
    }
  }

  /**
   * Checks whether the given class implements the given interface.
   *
   * @param intf      the interface to look for in the given class
   * @param cls       the class to check for the interface
   * @return          TRUE if the class contains the interface
   */
  public static boolean hasInterface(Class<?> intf, Class<?> cls) {
    Class<?>[]       intfs;
    int           i;
    boolean       result;
    Class<?>         currentclass;

    result       = false;
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
        if ( (currentclass == null) || (currentclass.equals(Object.class)) )
          break;
      }
    }
    while (!result);

    return result;
  }

  /**
   * If the given package can be found in this part of the classpath then
   * an URL object is returned, otherwise <code>null</code>.
   *
   * @param classpathPart     the part of the classpath to look for the package
   * @param pkgname           the package to look for
   * @return                  if found, the url as string, otherwise null
   */
  protected static URL getURL(String classpathPart, String pkgname) {
    String              urlStr;
    URL                 result;
    File                classpathFile;
    File                file;
    JarFile             jarfile;
    Enumeration<JarEntry>         enm;
    String              pkgnameTmp;

    result = null;
    urlStr = null;

    try {
      classpathFile = new File(classpathPart);

      // directory or jar?
      if (classpathFile.isDirectory()) {
        // does the package exist in this directory?
        file = new File(classpathPart + pkgname);
        if (file.exists())
          urlStr = "file:" + classpathPart + pkgname;
      }
      else {
        // is package actually included in jar?
        jarfile    = new JarFile(classpathPart);
        enm        = jarfile.entries();
        pkgnameTmp = pkgname.substring(1);   // remove the leading "/"
        while (enm.hasMoreElements()) {
          if (enm.nextElement().toString().startsWith(pkgnameTmp)) {
            urlStr = "jar:file:" + classpathPart + "!" + pkgname;
            break;
          }
        }
      }
    }
    catch (Exception e) {
      // ignore
    }

    // try to generate URL from url string
    if (urlStr != null) {
      try {
        result = new URL(urlStr);
      }
      catch (Exception e) {
        System.err.println(
            "Trying to create URL from '" + urlStr
            + "' generates this exception:\n" + e);
        result = null;
      }
    }

    return result;
  }

  /**
   * Checks the given packages for classes that inherited from the given class,
   * in case it's a class, or implement this class, in case it's an interface.
   *
   * @param classname       the class/interface to look for
   * @param pkgnames        the packages to search in
   * @return                a list with all the found classnames
   */
  public static List<String> find(String classname, String[] pkgnames) {
    List<String>      result;
    Class<?>       cls;

    result = new ArrayList<String>();

    try {
      cls    = Class.forName(classname);
      result = find(cls, pkgnames);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  /**
   * Checks the given package for classes that inherited from the given class,
   * in case it's a class, or implement this class, in case it's an interface.
   *
   * @param classname       the class/interface to look for
   * @param pkgname         the package to search in
   * @return                a list with all the found classnames
   */
  public static List<String> find(String classname, String pkgname) {
    List<String>      result;
    Class<?>       cls;

    result = new ArrayList<String>();

    try {
      cls    = Class.forName(classname);
      result = find(cls, pkgname);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  /**
   * Checks the given packages for classes that inherited from the given class,
   * in case it's a class, or implement this class, in case it's an interface.
   *
   * @param cls             the class/interface to look for
   * @param pkgnames        the packages to search in
   * @return                a list with all the found classnames
   */
  public static List<String> find(Class<?> cls, String[] pkgnames) {
    List<String>	result;
    int		i;
    HashSet<String>	names;

    result = new ArrayList<String>();

    names = new HashSet<String>();
    for (i = 0; i < pkgnames.length; i++)
      names.addAll(find(cls, pkgnames[i]));

    // sort result
    result.addAll(names);
    Collections.sort(result, new StringCompare());

    return result;
  }

  /**
   * Checks the given package for classes that inherited from the given class,
   * in case it's a class, or implement this class, in case it's an interface.
   *
   * @param cls             the class/interface to look for
   * @param pkgname         the package to search in
   * @return                a list with all the found classnames
   */
  public static List<String> find(Class<?> cls, String pkgname) {
    List<String>                result;
//    StringTokenizer       tok;
    String                part;
    String                pkgpath;
    File                  dir;
    File[]                files;
    URL                   url;
    int                   i;
    Class<?>                 clsNew;
    String                classname;
    JarFile               jar;
    JarEntry              entry;
    Enumeration<JarEntry>           enm;


    // already cached?
    result = getCache(cls, pkgname);

    if (result == null) {
      result = new ArrayList<String>();

      if (VERBOSE)
	System.out.println(
	    "Searching for '" + cls.getName() + "' in '" + pkgname + "':");

      // turn package into path
      pkgpath = pkgname.replaceAll("\\.", "/");

      // check all parts of the classpath, to include additional classes from
      // "parallel" directories/jars, not just the first occurence
      /*tok = new StringTokenizer(
	  System.getProperty("java.class.path"),
	  System.getProperty("path.separator")); */

      ClassloaderUtil clu = new ClassloaderUtil();
      URLClassLoader sysLoader = (URLClassLoader)clu.getClass().getClassLoader();
      URL[] cl_urls = sysLoader.getURLs();

      //while (tok.hasMoreTokens()) {
      for (i = 0; i < cl_urls.length; i++) {
	//part = tok.nextToken();
        part = cl_urls[i].toString();
        if (part.startsWith("file:")) {
          part.replace(" ", "%20");
          try {
            File temp = new File(new java.net.URI(part));
            part = temp.getAbsolutePath();
          } catch (URISyntaxException e) {
            e.printStackTrace();
          }
        }
	if (VERBOSE)
	  System.out.println("Classpath-part: " + part);

	// does package exist in this part of the classpath?
	url = getURL(part, "/" + pkgpath);
	if (VERBOSE) {
	  if (url == null)
	    System.out.println("   " + pkgpath + " NOT FOUND");
	  else
	    System.out.println("   " + pkgpath + " FOUND");
	}
	if (url == null)
	  continue;

	// find classes
	dir = new File(part + "/" + pkgpath);
	if (dir.exists()) {
	  files = dir.listFiles();
	  for (int j = 0; j < files.length; j++) {
		  // recurse into package
		  if (files[j].isDirectory()) {
			  List<String> sClasses = find(cls, pkgname + "." + files[j].getName());
			  for(String sClass : sClasses) {
				  result.add(sClass);
			  }
		  }
	    // only class files
	    if (    (!files[j].isFile())
		|| (!files[j].getName().endsWith(".class")) )
	      continue;

	    try {
	      classname =   pkgname + "."
	      + files[j].getName().replaceAll(".*/", "")
	      .replaceAll("\\.class", "");
	      result.add(classname);
	  	if (VERBOSE) {
	  		System.out.println("Adding " + classname);
	  	}
	    }
	    catch (Exception e) {
	      e.printStackTrace();
	    }
	  }
	}
	else {
	  try {
	    jar = new JarFile(part);
	    enm = jar.entries();
	    while (enm.hasMoreElements()) {
	      entry = (JarEntry) enm.nextElement();

	      // only class files
	      if (    (entry.isDirectory())
		  || (!entry.getName().endsWith(".class")) )
		continue;

	      classname = entry.getName().replaceAll("\\.class", "");

	      // only classes in the particular package
	      if (!classname.startsWith(pkgpath))
		continue;

	      // no sub-package
	      if (classname.substring(pkgpath.length() + 1).indexOf("/") > -1)
		continue;

	      result.add(classname.replaceAll("/", "."));
	    }
	  }
	  catch (Exception e) {
	    e.printStackTrace();
	  }
	}
      }

      // check classes
      i = 0;
      while (i < result.size()) {
	try {
	  clsNew = Class.forName((String) result.get(i));

	  // no abstract classes
	  if (Modifier.isAbstract(clsNew.getModifiers()))
	    result.remove(i);
	  // must implement interface
	  else if ( (cls.isInterface()) && (!hasInterface(cls, clsNew)) )
	    result.remove(i);
	  // must be derived from class
	  else if ( (!cls.isInterface()) && (!isSubclass(cls, clsNew)) )
	    result.remove(i);
	  else
	    i++;
	}
	catch (Throwable e) {
	  System.err.println("Checking class: " + result.get(i));
	  e.printStackTrace();
	  result.remove(i);
	}
      }

      // sort result
      Collections.sort(result, new StringCompare());
      // remove duplicates
      for (i = result.size()-1; i > 0; i--) {
    	  if (result.get(i).equals(result.get(i-1))) {
    		  result.remove(i);
    	  }
      }
      // add to cache
      addCache(cls, pkgname, result);
    }

    return result;
  }

  /**
   * adds all the sub-directories recursively to the list.
   *
   * @param prefix	the path prefix
   * @param dir		the directory to look in for sub-dirs
   * @param list	the current list of sub-dirs
   * @return		the new list of sub-dirs
   */
  protected static HashSet<String> getSubDirectories(String prefix, File dir, HashSet<String> list) {
    File[]	files;
    int		i;
    String 	newPrefix;

    // add directory to the list
    if (prefix == null)
      newPrefix = "";
    else if (prefix.length() == 0)
      newPrefix = dir.getName();
    else
      newPrefix = prefix + "." + dir.getName();

    if (newPrefix.length() != 0)
      list.add(newPrefix);

    // search for sub-directories
    files = dir.listFiles();
    if (files != null) {
      for (i = 0; i < files.length; i++) {
	if (files[i].isDirectory())
	  list = getSubDirectories(newPrefix, files[i], list);
      }
    }

    return list;
  }

  /**
   * Lists all packages it can find in the classpath.
   *
   * @return                a list with all the found packages
   */
  public static List<String> findPackages() {
    List<String>		result;
    StringTokenizer	tok;
    String		part;
    File		file;
    JarFile		jar;
    JarEntry		entry;
    Enumeration<JarEntry>		enm;
    HashSet<String>		set;

    result = new ArrayList<String>();
    set    = new HashSet<String>();

    // check all parts of the classpath, to include additional classes from
    // "parallel" directories/jars, not just the first occurence
    tok = new StringTokenizer(
        System.getProperty("java.class.path"),
        System.getProperty("path.separator"));

    while (tok.hasMoreTokens()) {
      part = tok.nextToken();
      if (VERBOSE)
        System.out.println("Classpath-part: " + part);

      // find classes
      file = new File(part);
      if (file.isDirectory()) {
	set = getSubDirectories(null, file, set);
      }
      else if (file.exists()) {
        try {
          jar = new JarFile(part);
          enm = jar.entries();
          while (enm.hasMoreElements()) {
            entry = (JarEntry) enm.nextElement();

            // only directories
            if (entry.isDirectory())
              set.add(entry.getName().replaceAll("/", ".").replaceAll("\\.$", ""));
          }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    // sort result
    set.remove("META-INF");
    result.addAll(set);
    Collections.sort(result, new StringCompare());

    return result;
  }

  /**
   * initializes the cache for the classnames.
   */
  protected static void initCache() {
    if (m_Cache == null)
      m_Cache = new Hashtable<String,List<String>>();
  }

  /**
   * adds the list of classnames to the cache.
   *
   * @param cls		the class to cache the classnames for
   * @param pkgname	the package name the classes were found in
   * @param classnames	the list of classnames to cache
   */
  protected static void addCache(Class<?> cls, String pkgname, List<String> classnames) {
    initCache();
    m_Cache.put(cls.getName() + "-" + pkgname, classnames);
  }

  /**
   * returns the list of classnames associated with this class and package, if
   * available, otherwise null.
   *
   * @param cls		the class to get the classnames for
   * @param pkgname	the package name for the classes
   * @return		the classnames if found, otherwise null
   */
  protected static List<String> getCache(Class<?> cls, String pkgname) {
    initCache();
    return m_Cache.get(cls.getName() + "-" + pkgname);
  }

  /**
   * clears the cache for class/classnames relation.
   */
  public static void clearCache() {
    initCache();
    m_Cache.clear();
  }


  /**
   * Possible calls:
   * <ul>
   *    <li>
   *      weka.beast.core.ClassDiscovery &lt;packages&gt;<br/>
   *      Prints all the packages in the current classpath
   *    </li>
   *    <li>
   *      weka.beast.core.ClassDiscovery &lt;classname&gt; &lt;packagename(s)&gt;<br/>
   *      Prints the classes it found.
   *    </li>
   * </ul>
   *
   * @param args	the commandline arguments
   */
  public static void main(String[] args) {
	  try {
//		  installAddOn("http://snap-mcmc.googlecode.com/files/snap.addon.zip");
//		  uninstallAddOn("http://snap-mcmc.googlecode.com/files/snap.addon.zip");
		  doUnzip("/home/remco/.beast2/snap/snap.zip","/home/remco/.beast2/snap");
	  List<List<String>> s = getAddOns();
		System.exit(0);
	  } catch (Exception e) {
		e.printStackTrace();
		System.exit(0);
	}
	  
	  
	  
	  
    List<String>      	list;
    List<String> 		packages;
    int         	i;
    StringTokenizer	tok;

    if ((args.length == 1) && (args[0].equals("packages"))) {
      list = findPackages();
      for (i = 0; i < list.size(); i++)
	System.out.println(list.get(i));
    }
    else if (args.length == 2) {
      // packages
      packages = new ArrayList<String>();
      tok = new StringTokenizer(args[1], ",");
      while (tok.hasMoreTokens())
        packages.add(tok.nextToken());

      // search
      list = ClassDiscovery.find(
  		args[0],
  		(String[]) packages.toArray(new String[packages.size()]));

      // print result, if any
      System.out.println(
          "Searching for '" + args[0] + "' in '" + args[1] + "':\n"
          + "  " + list.size() + " found.");
      for (i = 0; i < list.size(); i++)
        System.out.println("  " + (i+1) + ". " + list.get(i));
    }
    else {
      System.out.println("\nUsage:");
      System.out.println(
	  ClassDiscovery.class.getName() + " packages");
      System.out.println("\tlists all packages in the classpath");
      System.out.println(
	  ClassDiscovery.class.getName() + " <classname> <packagename(s)>");
      System.out.println("\tlists classes derived from/implementing 'classname' that");
      System.out.println("\tcan be found in 'packagename(s)' (comma-separated list");
      System.out.println();
      System.exit(1);
    }
  }

  /**
   * compares two strings. The following order is used:<br/>
   * <ul>
   *    <li>case insensitive</li>
   *    <li>german umlauts (&auml; , &ouml; etc.) or other non-ASCII letters
   *    are treated as special chars</li>
   *    <li>special chars &lt; numbers &lt; letters</li>
   * </ul>
   */
  public static class StringCompare
    implements Comparator<String> {

    /**
     * appends blanks to the string if its shorter than <code>len</code>.
     *
     * @param s		the string to pad
     * @param len	the minimum length for the string to have
     * @return		the padded string
     */
    private String fillUp(String s, int len) {
      while (s.length() < len)
        s += " ";
      return s;
    }

    /**
     * returns the group of the character: 0=special char, 1=number, 2=letter.
     *
     * @param c		the character to check
     * @return		the group
     */
    private int charGroup(char c) {
      int         result;

      result = 0;

      if ( (c >= 'a') && (c <= 'z') )
        result = 2;
      else if ( (c >= '0') && (c <= '9') )
        result = 1;

      return result;
    }

    /**
     * Compares its two arguments for order.
     *
     * @param o1	the first object
     * @param o2	the second object
     * @return		-1 if o1&lt;o2, 0 if o1=o2 and 1 if o1&;gt;o2
     */
    public int compare(String o1, String o2) {
      String        s1;
      String        s2;
      int           i;
      int           result;
      int           v1;
      int           v2;

      result = 0;   // they're equal

      // get lower case string
      s1 = o1.toString().toLowerCase();
      s2 = o2.toString().toLowerCase();

      // same length
      s1 = fillUp(s1, s2.length());
      s2 = fillUp(s2, s1.length());

      for (i = 0; i < s1.length(); i++) {
        // same char?
        if (s1.charAt(i) == s2.charAt(i)) {
          result = 0;
        }
        else {
          v1 = charGroup(s1.charAt(i));
          v2 = charGroup(s2.charAt(i));

          // different type (special, number, letter)?
          if (v1 != v2) {
            if (v1 < v2)
              result = -1;
            else
              result = 1;
          }
          else {
            if (s1.charAt(i) < s2.charAt(i))
              result = -1;
            else
              result = 1;
          }

          break;
        }
      }

      return result;
    }

    /**
     * Indicates whether some other object is "equal to" this Comparator.
     *
     * @param obj	the object to compare with this Comparator
     * @return		true if the object is a StringCompare object as well
     */
    public boolean equals(Object obj) {
      return (obj instanceof StringCompare);
    }

  }
}
