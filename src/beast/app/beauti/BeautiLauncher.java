package beast.app.beauti;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import beast.app.beastapp.BeastLauncher;
import beast.app.util.Utils6;

/** 
 * Loads beast.jar and launches BEAUti through the Beauti class
 * 
 * This class should be compiled against 1.6 and packaged by itself. 
 * The remained of BEAST can be compiled against Java 1.8
 * **/
public class BeautiLauncher extends BeastLauncher {

	public static void main(String[] args) throws NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		if (runWithBundledJRE("beast.app.beauti.Beauti", args)) {
			return;
		}
		Utils6.startSplashScreen();
		if (javaVersionCheck("BEAUti")) {
			loadBEASTJars();
			Utils6.testCudaStatusOnMac();
			Beauti.main(args);
		}
        Utils6.endSplashScreen();
	}
}
