package beast.app.tools;

import beast.app.beastapp.BeastLauncher;

public class AppStoreLauncher extends BeastLauncher {
	/**
	 * Loads beast.jar and launches AppStore 
	 * 
	 * This class should be compiled against 1.6 and packaged by itself. The
	 * remained of BEAST can be compiled against Java 1.7 or higher
	 * **/
	public static void main(String[] args) throws Exception {
		if (javaVersionCheck("AppStore")) {
			loadBEASTJars();
			AppStore.main(args);
		}
	}

}
