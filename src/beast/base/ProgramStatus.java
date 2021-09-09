package beast.base;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import beast.pkgmgmt.Utils6;
import beast.pkgmgmt.Version;


/*
 * Contains information about the currently running program.
 */
public class ProgramStatus {

	/** program name, e.g. BEAUti or BEAST **/
	static public String name = "unknown";
	
    /** program version information **/
	static public Version version;

	/**
     * number of threads used to run the likelihood beast.core *
     */
    static public int m_nThreads = 1;

    /**
     * thread pool *
     */
    public static ExecutorService g_exec = Executors.newFixedThreadPool(m_nThreads);

	/**
	 * current directory for opening files *
	 */
	public static String g_sDir = System.getProperty("user.dir");

	public static void setCurrentDir(String currentDir) {
		g_sDir = currentDir;
		Utils6.saveBeautiProperty("currentDir", currentDir);
	}
    
}
