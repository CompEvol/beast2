package beast.core;

@Description("Entry point for running a Beast task, for instance an MCMC or other probabilistic " +
        "analysis, a simulation, etc.")
public abstract class Runnable extends Plugin {
    public void run() throws Exception {
    }

    /**
     * Set up information related to the file for (re)storing the State.
     * The Runnable implementation is responsible for making its
     * State synchronising with the file *
     * @param sFileName
     * @param bRestoreFromFile
     */
    public void setStateFile(final String sFileName, final boolean bRestoreFromFile) {
    	if (System.getProperty("state.file.name") != null) {
    		m_sStateFile = System.getProperty("state.file.name");
    	} else {
            if (System.getProperty("file.name.prefix") != null) {
            	m_sStateFile = System.getProperty("file.name.prefix") + "/" + sFileName;
            } else {
            	m_sStateFile = sFileName;
            }
    	}
        m_bRestoreFromFile = bRestoreFromFile;
    }

    /**
     * flag to indicate that the State should be restored from File at the start of the analysis *
     */
    protected boolean m_bRestoreFromFile = false;

    /**
     * name of the file store the state in *
     */
    protected String m_sStateFile = "state.backup.xml";
}
