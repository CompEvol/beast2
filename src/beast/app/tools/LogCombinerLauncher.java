package beast.app.tools;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import beast.app.beastapp.BeastLauncher;

/** Launches log-combiner 
 * @see BeastLauncher 
 * **/
public class LogCombinerLauncher extends BeastLauncher {

	public static void main(String[] args) throws NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		if (runWithBundledJRE("beast.app.tools.LogCombiner", args)) {
			return;
		}
		if (javaVersionCheck("LogCombiner")) {
			loadBEASTJars();
			LogCombiner.main(args);
		}
	}
}
