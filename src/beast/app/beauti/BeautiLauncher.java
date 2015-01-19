package beast.app.beauti;


import beast.app.beastapp.BeastLauncher;
//import beast.app.util.Utils;

/** 
 * Loads beast.jar and launches BEAUti through the Beauti class
 * 
 * This class should be compiled against 1.6 and packaged by itself. 
 * The remained of BEAST can be compiled against Java 1.7
 * **/
public class BeautiLauncher extends BeastLauncher {

	public static void main(String[] args) throws Exception {
		//Utils.startSplashScreen();
		if (javaVersionCheck("BEAUti")) {
			loadBEASTJars();
			Beauti.main(args);
		}
        //Utils.endSplashScreen();
	}
}
