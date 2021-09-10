package beast.base.parser;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObjectStore;
import beast.base.core.Log;
import beast.pkgmgmt.PackageManager;
import beast.pkgmgmt.Utils6;

public class ClassToPackageMap {

	/** keep track of which class comes from a particular package.
	 * It maps a full class name onto a package name + " v" + package version
	 * e.g. "bModelTest v0.3.2"
	 */
	static Map<String, String> classToPackageMap = new HashMap<String, String>();

	public static Map<String, String> getRawClassToPackageMap() {return classToPackageMap;}

	public static Map<String, String> getClassToPackageMap() {
		if (classToPackageMap.size() == 0) {
	        for (String jarDirName : PackageManager.getBeastDirectories()) {
	        	ClassToPackageMap.initPackageMap(jarDirName);
	        }
		}
		return classToPackageMap;
	}

	/** traverse model graph starting at o, and collect packageAndVersion strings
	     * along the way.
	     */
	    static void getPackagesAndVersions(Object o, Set<String> packagesAndVersions) {
	    	String packageAndVersion = classToPackageMap.get(o.getClass().getName());
	    	if (packageAndVersion != null) {
	    		packagesAndVersions.add(packageAndVersion);
	    	}
	    	for (BEASTInterface o2 : BEASTObjectStore.listActiveBEASTObjects(o)) {
	    		try {
	    			getPackagesAndVersions(o2, packagesAndVersions);
	    		} catch (NoClassDefFoundError e) {
	    			// ignore
	    		}
	    	}
		}

	/** return set of Strings in the format of classToPackageMap (like "bModelTest v0.3.2")
	 * for all packages used by o and its predecessors in the model graph.
	 */
	public static Set<String> getPackagesAndVersions(Object o) {
		Set<String> packagesAndVersions = new LinkedHashSet<String>();
		getPackagesAndVersions(o, packagesAndVersions);
		return packagesAndVersions;
	}

	public static void initPackageMap(String jarDirName) {
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

}
