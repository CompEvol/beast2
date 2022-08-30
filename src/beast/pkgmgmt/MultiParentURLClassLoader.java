package beast.pkgmgmt;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/**
 * Class loader that first tries its parent class loaders before attempting to
 * load the class from the implemented URLClassLoader.
 * This allows packages to have their own versions of libraries, provided the
 * packages they rely on not already contain the same library.
 */
public class MultiParentURLClassLoader extends URLClassLoader {

	List<ClassLoader> parentLoaders;
	Set<ClassLoader> childLoaders;
	String name;
	
	public MultiParentURLClassLoader(URL[] urls, String packageName) {
		super(urls);
		childLoaders = new HashSet<>();
		this.parentLoaders = new ArrayList<>();
		this.name = packageName;
	}
	
	public MultiParentURLClassLoader(URL[] urls, ClassLoader [] parentLoaders) {
		super(urls);
		childLoaders = new HashSet<>();
		this.parentLoaders = new ArrayList<>();
		
		for (ClassLoader parentLoader: parentLoaders) {
			this.parentLoaders.add(parentLoader);
			if (parentLoader instanceof  MultiParentURLClassLoader) {
				((MultiParentURLClassLoader)parentLoader).addChildLoader(this);
			}
		}
	}

    private void addChildLoader(MultiParentURLClassLoader parentLoader) {
    	childLoaders.add(parentLoader);		
	}

	public void addParentLoader(ClassLoader parentLoader) {
		parentLoaders.add(parentLoader);
		if (parentLoader instanceof MultiParentURLClassLoader) {
			// System.err.println(((MultiParentURLClassLoader)parentLoader).name + " -> " + name);
		}
 		if (parentLoader instanceof  MultiParentURLClassLoader) {
			((MultiParentURLClassLoader)parentLoader).addChildLoader(this);
		}
    }
	
    public void addURL(String jarFile) {
        File file = new File(jarFile);
        if (file.exists()) {
        	System.err.println("found file " + jarFile);
            try {
                URL url = file.toURI().toURL();
                super.addURL(url);
                System.err.println("Loaded " + url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    public void addURL(URL url) {
    	super.addURL(url);
    }
	    
    public Class<?> forName(String className) throws ClassNotFoundException {
		
		// first try to resolve from all parent class loaders,
		// each is supposed to be associated with its own package
		// managed by the PackageManager
		for (ClassLoader parentLoader : parentLoaders) {			
			try {
				Class<?> c =  Class.forName(className, false, parentLoader);
				if (c != null) {
					return c;
				}
			} catch (NoClassDefFoundError e) {
				// ignore -- try the next class loader instead
			}			
		}

		// no luck trying the parents -- try the local package instead
		try {
			return Class.forName(className, false, this);
		} catch (NoClassDefFoundError e2) {
			throw new ClassNotFoundException(e2.getMessage());
		}
	}
    
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
    	// System.err.println("MPCL: load class " + name + " " + this.name);
		try {
			return loadClass(name, false);
		} catch (NoClassDefFoundError | ClassNotFoundException e) {
			// ignore -- try the next class loader instead
		}

		// no luck trying the parents
		for (ClassLoader parentLoader : parentLoaders) {			
			try {
				Class<?> c =  parentLoader.loadClass(name);
				if (c != null) {
					return c;
				}
			} catch (NoClassDefFoundError e) {
				// ignore -- try the next class loader instead
			}			
		}

		// System.err.println("MPCL: giving up " + name + " " + this.name);
				
		throw new NoClassDefFoundError(name);
    }
    
    @Override
    public String toString() {
    	return super.toString() +"["+ name + "]";
    }
}
