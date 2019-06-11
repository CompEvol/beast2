package beast.util;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import beast.core.util.Log;

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

	// singleton class loader
	static final public BEASTClassLoader classLoader = new BEASTClassLoader(new URL[0], BEASTClassLoader.class.getClassLoader());

	/**
	 * Class loader should only be created by the singleton BEASTClassLoader.classLoader
	 * so keep this private
	 */
    private BEASTClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
    }

    /** dynamically load jars **/
    @Override
    public void addURL(URL url) {
    	super.addURL(url);
    }

    /** dynamically load jars **/
    public void addJar(String jarFile) {
    	Log.debug.println("Attempting to load " + jarFile);
        File file = new File(jarFile);
        if (file.exists()) {
        	Log.debug.println("found file " + jarFile);
                try {
                    URL url = file.toURI().toURL();
                    classLoader.addURL(url);
                    Log.debug.println("Loaded " + url);
                } catch (MalformedURLException e) {
                        e.printStackTrace();
                }
        }
    }
   
    /**
     *  The BEAST package alternative for Class.forName().
     *  The latter won't work for loading classes from packages from BEAST v2.6.0 onwards. 
     * **/
	public static Class<?> forName(String className) throws ClassNotFoundException {
		// System.err.println("Loading: " + className);
		try { 
			return Class.forName(className, true, BEASTClassLoader.classLoader);
		} catch (Throwable e) {
			try { 
				return Class.forName(className, false, BEASTClassLoader.classLoader);
			} catch (NoClassDefFoundError e2) {
				throw new ClassNotFoundException(e2.getMessage());
			}
			
		}	
	}
}
