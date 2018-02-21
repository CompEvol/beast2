package beast.app.treeannotator;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import beast.app.beastapp.BeastLauncher;


/**
 * Loads beast.jar and launches TreeAnnotator 
 * 
 * This class should be compiled against 1.6 and packaged by itself. The
 * remained of BEAST can be compiled against Java 1.7 or higher
 * **/
public class TreeAnnotatorLauncher extends BeastLauncher  {

	public static void main(String[] args) throws NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		if (javaVersionCheck("TreeAnnotator")) {
			String classpath = getPath();
			run(classpath, "beast.app.treeannotator.TreeAnnotator", args);
		}
	}

}
