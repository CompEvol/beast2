package beast.pkgmgmt;


import java.io.File;
import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ServiceLoader;
import java.util.Set;

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
	static public BEASTClassLoader classLoader;// = new BEASTClassLoader(new URL[0], BEASTClassLoader.class.getClassLoader());

    private BEASTClassLoader(ClassLoader parent) {
        super(new URL[]{}, parent);
    }
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
    	System.err.println("Attempting to load " + jarFile);
    	// TODO: fix this
        File file = new File(jarFile);
        if (file.exists()) {
        	System.err.println("found file " + jarFile);
                try {
                    URL url = file.toURI().toURL();
                    classLoader.addURL(url);
                    System.err.println("Loaded " + url);
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
		if (classLoader ==  null) {
			Path [] paths = new Path[]{
					Paths.get("build/dist/beast.base.jar"),
					Paths.get("build/dist/beast.app.jar"),
					Paths.get("build/dist/json.jar"),
					Paths.get("build/dist/commons-math.jar"),
			};
			ModuleFinder moduleFinder = ModuleFinder.of(paths);
			
	        //Create a new Configuration for a new module layer deriving from the boot configuration, and resolving
	        //the "my.implementation" module.
	        var cfg = ModuleLayer.boot().configuration().resolve(moduleFinder ,ModuleFinder.of(),Set.of("beast.base", "beast.app"));
	        
	        //Create classloader
	        // var mcl = new URLClassLoader(new URL[] {new URL("file:///tmp/mymodule.jar")});        
	        var mcl = new URLClassLoader(new URL[] {});
	        
	        // make the module layer, using the configuration and classloader.        
	        ModuleLayer ml = ModuleLayer.boot().defineModulesWithOneLoader(cfg, mcl);
	        classLoader = new BEASTClassLoader(ml.findLoader("beast.base"));
		}
		
		// System.err.println("Loading: " + className);
		try { 
			return Class.forName(className, false, classLoader);
		} catch (NoClassDefFoundError e2) {
			throw new ClassNotFoundException(e2.getMessage());
		}
	}

	/** 
	 * load service like DataType from all available modules 
	 * **/	
	public static Iterable<?> load(Class<?> clazz) {
		return ServiceLoader.load(clazz);
	}
}
