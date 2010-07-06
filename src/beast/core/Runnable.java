package beast.core;

@Description("Entry point for running an Beast task, for instance an MCMC or other probabilistic " +
		"analysis, a simulation, etc.")
public class Runnable extends Plugin {
    public void run() throws Exception {};
}
