package beast.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import javax.swing.plaf.PanelUI;

public class BEASTClassLoader extends URLClassLoader {

	// singleton class loader
	static final public BEASTClassLoader classLoader = new BEASTClassLoader(new URL[0], BEASTClassLoader.class.getClassLoader());

    public BEASTClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
    }

    @Override
    public void addURL(URL url) {
    super.addURL(url);
}

    public void addJar(String jarFile) {
        File file = new File(jarFile);
        if (file.exists()) {
                try {
                        URL url = file.toURI().toURL();
                    classLoader.addURL(url);
                } catch (MalformedURLException e) {
                        e.printStackTrace();
                }
        }
    }

    
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
    	 //Class c = cl.loadClass("monos.GenerateLexicalConstraints");
         //System.out.println(c.getName());
    	classLoader.addJar("/home/rbou019/.beast/2.5/NS/lib/NS.addon.jar");
         System.out.println("Classloader of this class:"
                 + BEASTClassLoader.class.getClassLoader());
          
             System.out.println("Classloader of PanelUI:"
                 + Class.forName("beast.core.NSLogger", true, classLoader).getDeclaredConstructor().newInstance().getClass().getClassLoader());
          
             System.out.println("Classloader of ArrayList:"
                 + ArrayList.class.getClassLoader());    
            }
}
