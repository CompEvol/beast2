package beast.app.tools;

import beast.app.beastapp.BeastLauncher;

/** Launches log-combiner 
 * @see BeastLauncher 
 * **/
public class LogCombinerLauncher extends BeastLauncher {

	public static void main(String[] args) throws Exception {
		if (javaVersionCheck("LogCombiner")) {
			loadBEASTJars();
			LogCombiner.main(args);
		}
	}
}
