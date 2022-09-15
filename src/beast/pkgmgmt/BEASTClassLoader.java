package beast.pkgmgmt;




import java.util.*;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;

/**
 * This class helps dynamically load BEAST packages
 * using the URLClassLoader mechanism used to be the default
 * class loader in Java 8, but isn't in Java 9+ any more.
 * 
 * It requires package developers to use 
 * BEASTClassLoader.forName() instead of Class.forName().
 * and BEASTClassLoader.classLoader.getResource to access 
 * resources (like images) from jar files instead of
 * ClassLoader.getResource
 */
public class BEASTClassLoader extends URLClassLoader {
		// maps package name (String) to class loader (MultiParentURLClassLoader)
	    // used to find class loader for a package
		static Map<String, MultiParentURLClassLoader> package2classLoaderMap = new HashMap<>();
	
		// singleton class loader
		static final public BEASTClassLoader classLoader = new BEASTClassLoader(new URL[0], BEASTClassLoader.class.getClassLoader());

		// maps service (=class name) to service providers (=set of class names)
		static private Map<String, Set<String>> services = new HashMap<>();
		
		// maps service provider (=class name) to class loader for that provider (=class) 
		static private Map<String, ClassLoader> class2loaderMap = new HashMap<>();
		
		static private Set<String> namespaces = new HashSet<>();
		/**
		 * Class loader should only be created by the singleton BEASTClassLoader.classLoader
		 * so keep this private
		 */
	    private BEASTClassLoader(URL[] urls, ClassLoader parent) {
	            super(urls, parent);
	    }

	    /** dynamically load jars **/
	    @Override
	    // TODO: purge use of addURL(jarFile)
	    @Deprecated // use addURL(jarFile, packageName) instead
	    public void addURL(URL url) {
	    	super.addURL(url);
	    }

	    public void addURL(URL url, String packageName, Map<String, Set<String>> services) {
	    	MultiParentURLClassLoader loader = getClassLoader(packageName);
	    	loader.addURL(url);

	    	if (services != null) {
	    		addServices(packageName, services);
	    	}
	    	
	    }

	    public void addParent(String packageName, String parentPackage) {
    		if (parentPackage.equals(packageName)) {
    			return;
    		}
	    	MultiParentURLClassLoader loader = getClassLoader(packageName);
	    	MultiParentURLClassLoader parentLoader = getClassLoader(parentPackage);
	    	loader.addParentLoader(parentLoader);	    	
	    }
	    
	    /** dynamically load jars **/
	    // TODO: purge use of addJar(jarFile)
	    @Deprecated // use addJar(jarFile, packageName) instead
	    public void addJar(String jarFile) {
	        File file = new File(jarFile);
	        if (file.exists()) {
	        	//System.err.println("found file " + jarFile);
	            try {
	                URL url = file.toURI().toURL();
	                super.addURL(url);
	                //System.err.println("Loaded " + url);
	            } catch (MalformedURLException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	    
	    public void addJar(String jarFile, String packageName) {
	    	//System.err.println("Attempting to load " + jarFile);
	    	MultiParentURLClassLoader loader = getClassLoader(packageName);
	    	loader.addURL(jarFile);
	    } 	
	   
	    /**
	     *  The BEAST package alternative for Class.forName().
	     *  The latter won't work for loading classes from packages from BEAST v2.6.0 onwards. 
	     * **/
		public static Class<?> forName(String className) throws ClassNotFoundException {
			// System.err.println("forName: " + className);
			
			if (class2loaderMap.containsKey(className)) {
				ClassLoader loader = class2loaderMap.get(className);
				// System.err.println("class2loaderMap " + loader.toString());
				return Class.forName(className, false, loader);
			}
			
			// System.err.println("Loading non-service: " + className);
			for (MultiParentURLClassLoader loader : package2classLoaderMap.values()) {
				try { 
					// System.err.println("Trying to load "+className+" using " + loader.name);
					return Class.forName(className, false, loader);
				} catch (NoClassDefFoundError | java.lang.ClassNotFoundException e) {
					// ignore -- assume another loader contains the class
				}
			}
			
			try { 
				// System.err.println("Trying to load using BEASTClassLoader.classLoader");
				Class<?> c = Class.forName(className, false, BEASTClassLoader.classLoader);
				// System.err.println("Loading from " + c.getProtectionDomain().getCodeSource().getLocation());
				return c;
			} catch (NoClassDefFoundError e) {
				throw new ClassNotFoundException(e.getMessage());
			}
		}	
		
		public static Class<?> forName(String className, String service) throws ClassNotFoundException {
			// System.err.println("forName: " + className + " " + service);

			if (!services.containsKey(service)) {
				if (services.size() == 0) {
					services.put(service, new HashSet<>());
					initServices();
					return forName(className, service);
				} else {
					throw new IllegalArgumentException("Could not find service " + service + " while trying to forName class " + className);
				}
			}
			if (!services.get(service).contains(className)) {
				throw new ClassNotFoundException("Could not find class " + className + " as service " + service + "\n"
						+ "Perhaps the package is missing or the package is not correctly configured by the developer "
						+ "(Developer: check by running beastfx.app.tools.PackageHealthChecker on the package)");
			}
			ClassLoader loader = class2loaderMap.get(className);
			return Class.forName(className, false, loader);
		}

		
		/**
		 * Return set of services provided by all packages
		 * @param service: class identifying the service
		 * @return set of services found
		 */
		public static Set<String> loadService(Class<?> service) {
			Set<String> providers = services.get(service.getName());
			if (providers == null) {
				if (services.size() == 0) {
					initServices();
				} else {
					services.put(service.getName(), new HashSet<>());
				}
				providers = services.get(service.getName());
			}
			return providers;
		}

		public static void initServices() {
			// no services loaded at all. Should only get here when running
			// junit tests or from an IDE
			// Try to find version.xml files
			String classPath = System.getProperty("java.class.path");
			try {
				// deal with special characters and spaces in path
				classPath = URLDecoder.decode(classPath, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// ignore
			}
			initServices("/" + classPath + "/");
		}

		public static void initServices(String classPath) {
			// try to find version.xml files in source path
			for (String jarFileName : classPath.substring(1, classPath.length() - 1).split(File.pathSeparator)) {
				File jarFile = new File(jarFileName);
				try {
					String parentDir = jarFile.isDirectory() ?							
							(jarFile.getParentFile() == null ? File.pathSeparator :jarFile.getParentFile().getPath()) :
							(jarFile.getParentFile() == null  || jarFile.getParentFile().getParentFile() == null ? File.pathSeparator :
							jarFile.getParentFile().getParentFile().getPath());
					if (new File(parentDir + File.separator + "version.xml").exists()) {
						addServices(parentDir + File.separator + "version.xml");
					}
					if (new File(parentDir + File.separator + "beast.base.version.xml").exists()) {
						addServices(parentDir + File.separator + "beast.base.version.xml");
					}  else if (new File(parentDir + File.separator + "beast.base" + File.separator + "version.xml").exists()) {
						addServices(parentDir + File.separator + "beast.base" + File.separator + "version.xml");
					}
					if (new File(parentDir + File.separator + "beast.app.version.xml").exists()) {
						addServices(parentDir + File.separator + "beast.app.version.xml");
					} else if (new File(parentDir + File.separator + "beast.app" + File.separator + "version.xml").exists()) {
						addServices(parentDir + File.separator + "beast.app" + File.separator + "version.xml");
					}
				} catch (Throwable e) {
					// ignore
				}
			}		
		}
		
		
		public static void addServices(String versionFile) {
			try {
				Map<String,Set<String>> services = null;
		        // print name and version of package
		        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		        Document doc = factory.newDocumentBuilder().parse(versionFile);
		        services = PackageManager.parseServices(doc);
		        Element packageElement = doc.getDocumentElement();
                String packageName = packageElement.getAttribute("name"); 
				BEASTClassLoader.classLoader.addServices(packageName, services);
			} catch (Throwable e) {
				// ignore
			}
		}	
		
		public static Map<String, Set<String>> getServices() {			
			return services;
		}
		
		public void addServices(String packageName, Map<String, Set<String>> services) {
			ClassLoader loader = getClassLoader(packageName);

	    	for (String service : services.keySet()) {
	    		if (!BEASTClassLoader.services.containsKey(service)) {	    			
	    			BEASTClassLoader.services.put(service, new HashSet<>());
	    		}
	    		Set<String> providers = BEASTClassLoader.services.get(service);
	    		providers.addAll(services.get(service));
	    		for (String provider : services.get(service)) {
	    			class2loaderMap.put(provider, loader);
					if (provider.contains(".")) {
						String namespace = provider.substring(0, provider.lastIndexOf('.'));
						namespaces.add(namespace);
					}
	    		}
	    	}	    	
			
		}

		/*
		 * get deepest level class loader associated with a specific package
		 * identified by the package name
		 */
		private static MultiParentURLClassLoader getClassLoader(String packageName) {
	    	if (!package2classLoaderMap.containsKey(packageName)) {
		    	package2classLoaderMap.put(packageName, new MultiParentURLClassLoader(new URL[0], packageName));
		    	// System.err.println("Created classloader >>" + packageName + "<<");
	    	}
	    		
	    	MultiParentURLClassLoader loader = package2classLoaderMap.get(packageName);
	    	return loader;
		}

		
		/**
		 * add service with specified class name -- useful for testing
		 * @param service: name of the service to add
		 * @param className: name of the service provider
		 */
		public static void addService(String service, String className, String packageName) {
    		if (!BEASTClassLoader.services.containsKey(service)) {    			
    			if (BEASTClassLoader.services.size() == 0) {
    				initServices();
    			}
        		if (!BEASTClassLoader.services.containsKey(service)) {    			
        			BEASTClassLoader.services.put(service, new HashSet<>());    			
        		}
    		}
    		BEASTClassLoader.services.get(service).add(className);
    		
    		class2loaderMap.put(className, getClassLoader(packageName));
		}

		/**
		 * delete services with specified class name -- useful for testing
		 * @param service: name of the service to add
		 * @param className: name of the service provider
		 */
		public static void delService(Map<String,Set<String>> serviceMap, String packageName) {
			for (String service : serviceMap.keySet()) {
				Set<String> classNames = serviceMap.get(service);
				for (String provider : classNames) {
					// release service
					BEASTClassLoader.services.get(service).remove(provider);
				
					// release name space
					String namespace = provider.substring(0, provider.lastIndexOf('.'));
					namespaces.remove(namespace);
				}
			}
			
			if (package2classLoaderMap.containsKey(packageName)) {
				package2classLoaderMap.remove(packageName);
			}
		}

		public static String usesExistingNamespaces(Set<String> services) {
			for (String service : services) {
				if (service.contains(".")) {
					String namespace = service.substring(0, service.lastIndexOf('.'));
					if (namespaces.contains(namespace)) {
						return namespace;
					}
				}
			}
			return null;
		}

		/** get the resource for a BEAST package
		 * This replaces <Class>.class.getResource(resourceName)
		 * and is required because every BEAST package has its own class loader now 
		 * @return
		 */
		public static URL getResource(String packageName, String resourceName) {
			ClassLoader classLoader = package2classLoaderMap.get(packageName);
			if (classLoader == null) {
				return null;				
			}
			URL url = classLoader.getResource(resourceName);
			return url;
		}
}
