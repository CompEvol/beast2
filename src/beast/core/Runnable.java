package beast.core;

@Description("Entry point for running an Beast task, for instance an MCMC or other probabilistic " +
		"analysis, a simulation, etc.")
public abstract class Runnable extends Plugin {
    public void run() throws Exception {}
    
    public void setStateFile(String sFileName, boolean bRestoreFromFile) {
    	m_sStateFile = sFileName;
    	m_bRestoreFromFile = bRestoreFromFile;
    }
    
    /** flag to indicate that the State should be restored from File at the start of the analysis **/
    protected boolean m_bRestoreFromFile = false;
    /** name of the file store the state in **/
    protected String m_sStateFile;
}
