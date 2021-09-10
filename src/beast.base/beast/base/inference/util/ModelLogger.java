package beast.base.inference.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;


/** log model at the start of a trace log **/ 
public class ModelLogger {

	static Set
	<ModelLogger> modelLoggers = new HashSet<>();

	
    static {
    	findModelLoggers();
    }

    static public void findModelLoggers() {
        // build up list of ModelLoggers
    	modelLoggers = new HashSet<>();
        List<String> loggers = PackageManager.find(ModelLogger.class, "beast");
        for (String loggerName : loggers) {
            try {
            	ModelLogger logger = (ModelLogger) BEASTClassLoader.forName(loggerName).getConstructors()[0].newInstance();
                modelLoggers.add(logger);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
		
	/**
	 * How well this model logger can log this object
	 * @param o object to be logged
	 * @return less than 0 if it cannot be logged, higher values for better matches
	 */
	protected int canHandleObject(Object o) {
		return -1;
	}
	
	protected String modelToStringImp(Object o) {
		return null;
	}
	
	public static String modelToString(Object o) {
		int i = -1;
		ModelLogger bestLogger = null;
		for (ModelLogger m : modelLoggers) {
			int score = m.canHandleObject(o);
			if (score > i) {
				i = score;
				bestLogger = m;
			}
		}
		if (bestLogger == null) {
			return null;
		}
		return bestLogger.modelToStringImp(o);
	}
}
