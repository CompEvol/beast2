package beast.pkgmgmt.launcher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class AppLauncherLauncher extends BeastLauncher {
	/**
	 * Loads beast.jar and launches AppStore 
	 * 
	 * This class should be compiled against 1.6 and packaged by itself. The
	 * remained of BEAST can be compiled against Java 1.7 or higher
	 * @throws IOException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * **/
	public static void main(String[] args) throws NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		if (javaVersionCheck("AppLauncher")) {
			String classpath = getPath(false, null);
			run(classpath, "beast.app.tools.AppLauncher", args);
		}
	}

}
