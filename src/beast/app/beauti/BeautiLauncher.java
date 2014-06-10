package beast.app.beauti;

import beast.app.beastapp.BeastLauncher;

/** 
 * Loads beast.jar and launches BEAUti through the Beauti class
 * 
 * This class should be compiled against 1.6 and packaged by itself. 
 * The remained of BEAST can be compiled against Java 1.7
 * **/
public class BeautiLauncher extends BeastLauncher {

	public static void main(String[] args) throws Exception {
		if (javaVersionCheck("BEAUti")) {
			loadBEASTJars();
			Beauti.main(args);
		}
	}
}
