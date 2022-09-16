package beast.pkgmgmt.launcher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/** Launches TreeAnnotator 
 * @see BeastLauncher
 * **/
public class TreeAnnotatorLauncher extends BeastLauncher  {

	public static void main(String[] args) throws NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		if (javaVersionCheck("TreeAnnotator")) {
			String classpath = getPath(false, null);
			run(classpath, "beastfx.app.treeannotator.TreeAnnotator", args);
		}
	}

}
