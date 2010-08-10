package beast.core;

@Description("Entry point for running an Beast task, for instance an MCMC or other probabilistic " +
		"analysis, a simulation, etc.")
public abstract class Runnable extends Plugin {
    public void run() throws Exception {};
    
    public void restoreFromFile() {
    	m_bRestoreFromFile = true;
    }
    /** flag to indicate that the State should be restored from File at the start of the analysis **/
    boolean m_bRestoreFromFile = false;
}
