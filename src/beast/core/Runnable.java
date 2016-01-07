package beast.core;


@Description("Entry point for running a Beast task, for instance an MCMC or other probabilistic " +
        "analysis, a simulation, etc.")
public abstract class Runnable extends BEASTObject {	
	
	/** entry point for anything runnable **/
	abstract public void run() throws Exception;

    /**
     * Set up information related to the file for (re)storing the State.
     * The Runnable implementation is responsible for making its
     * State synchronising with the file *
     * @param fileName
     * @param bRestoreFromFile
     */
    public void setStateFile(final String fileName, final boolean bRestoreFromFile) {
    	if (System.getProperty("state.file.name") != null) {
    		stateFileName = System.getProperty("state.file.name");
    	} else {
            if (System.getProperty("file.name.prefix") != null) {
            	stateFileName = System.getProperty("file.name.prefix") + "/" + fileName;
            } else {
            	stateFileName = fileName;
            }
    	}
        restoreFromFile = bRestoreFromFile;
    }

    /**
     * flag to indicate that the State should be restored from File at the start of the analysis *
     */
    protected boolean restoreFromFile = false;

    /**
     * name of the file store the state in *
     */
    protected String stateFileName = "state.backup.xml";
    
    /** 
     * indicate whether this runnable distinguishes partitions, like MCMC, or not 
     * **/
    public boolean hasPartitions() {return true;}
}
