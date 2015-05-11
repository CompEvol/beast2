package beast.app.beauti;


import beast.app.beastapp.BeastLauncher;
import beast.app.util.Utils6;

/** 
 * Loads beast.jar and launches BEAUti through the Beauti class
 * 
 * This class should be compiled against 1.6 and packaged by itself. 
 * The remained of BEAST can be compiled against Java 1.8
 * **/
public class BeautiLauncher extends BeastLauncher {

	public static void main(String[] args) throws Exception {
		Utils6.startSplashScreen();
		if (javaVersionCheck("BEAUti")) {
			loadBEASTJars();
			Beauti.main(args);
		}
        Utils6.endSplashScreen();
	}
}
