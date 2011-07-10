package beast.app.beauti;


import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import beast.app.draw.PluginPanel;
import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Logger;
import beast.core.MCMC;
import beast.core.Operator;
import beast.core.Plugin;
import beast.core.State;
import beast.core.StateNode;
import beast.core.util.CompoundDistribution;
import beast.evolution.alignment.Alignment;
import beast.evolution.tree.TreeDistribution;
import beast.util.XMLParser;
import beast.util.XMLProducer;

@Description("Beauti document in doc-view pattern, not useful in models")
public class BeautiDoc extends Plugin {
	public Input<List<Alignment>> m_alignments = new Input<List<Alignment>>("alignment", "list of alignments or partitions", new ArrayList<Alignment>(), Validate.REQUIRED);
	public Input<beast.core.Runnable> m_mcmc = new Input<beast.core.Runnable>("runnable", "main entry of analysis", Validate.REQUIRED);

	/** points to input that contains prior distribution, if any **/
	Input<List<Distribution>> m_priors; 
	/** contains all Priors from the template **/
	protected List<Distribution> m_potentialPriors;

	/** contains all loggers from the template **/
	List<List<Plugin>> m_loggerInputs;
	
	boolean m_bAutoScrubOperators = true;
	boolean m_bAutoScrubLoggers = true;
	boolean m_bAutoScrubPriors = true;
	boolean m_bAutoScrubState = true;
	
	
	public BeautiDoc() {
		setID("BeautiDoc");
		m_potentialPriors = new ArrayList<Distribution>();
	}
	
    void initialize(BeautiInitDlg.ActionOnExit endState, String sXML, String sTemplate, String sFileName) throws Exception {
    	switch (endState) {
    	case SHOW_DETAILS_USE_TEMPLATE: {
    		mergeSequences(sTemplate);
        	connectModel();
    		break;
    	}
    	case SHOW_DETAILS_USE_XML_SPEC: {
    		extractSequences(sXML);
        	connectModel();
    		break;
    	}
    	case WRITE_XML: {
    		mergeSequences(sTemplate);
        	connectModel();
        	save(sFileName);
        	break;
    	}
    	case UNKNOWN:
    		System.exit(0);
    	}
    }

	
	/** see whether we have a valid model that can be saved at this point in time **/
	public boolean validateModel() {
		if (m_mcmc == null) {
			return false;
		}
		return true;
	} // validateModel
	
	/** save specification in file **/
	public void save(String sFileName) throws Exception {
		scrubAll(false);
		String sXML = new XMLProducer().toXML(m_mcmc.get(), PluginPanel.g_plugins.values());
		FileWriter outfile = new FileWriter(sFileName);
		outfile.write(sXML);
		outfile.close();
	} // save

	List<String> extractSequences(String sXML) throws  Exception {
		XMLParser parser = new XMLParser();
		List<Plugin> plugins = parser.parseTemplate(sXML);
		List<String> sAlignmentNames = new ArrayList<String>();
		for (Plugin plugin: plugins) {
			PluginPanel.addPluginToMap(plugin);
			if (plugin instanceof beast.core.Runnable) {
				m_mcmc.setValue(plugin, this);
			}
			if (plugin instanceof beast.evolution.alignment.Alignment) {
				sAlignmentNames.add(plugin.getID());
			}
		}
		return sAlignmentNames;
	}

	/** Merge sequence data with sXML specification. 
	 **/
	List<String> mergeSequences(String sXML) throws  Exception {
		// create XML for alignments
		String sAlignments = "";
		int n = 1;
		String sRange="range='";
		List<String> sAlignmentNames = new ArrayList<String>();
		for (Alignment alignment : m_alignments.get()) {
			sAlignmentNames.add(alignment.getID());
			alignment.setID(/*"alignment" + */alignment.getID());
			sRange += alignment.getID() +",";
			sAlignments += new XMLProducer().toRawXML(alignment);
			n++;
		}
		sRange = sRange.substring(0, sRange.length()-1)+"'";
		
		// process plates in template
		int i  = -1;
		do {
			i = sXML.indexOf("<plate", i);
			if (i >= 0) {
				int j = sXML.indexOf("range='#alignments'");
				int j2 = sXML.indexOf("range=\"#alignments\"");
				if (j < 0 || (j2>=0 && j2 < j)) {
					j = j2;
				}
				// sanity check: no close of elements encountered underway...
				for (int k = i; k < j; k++) {
					if (sXML.charAt(k) == '>') {
						j = -1;
					}
				}
				if ( j >= 0) {
					sXML = sXML.substring(0, j) + sRange + sXML.substring(j+19);
				}
				i++;
			}
		} while (i >= 0);
		
		
		if (sAlignmentNames.size() == 1) {
			// process plates in template
			i  = -1;
			do {
				i = sXML.indexOf("$(n).", i);
				if ( i >= 0) {
					sXML = sXML.substring(0, i) + sXML.substring(i+5);
				}
			} while (i >= 0);
		}
		
		// merge in the alignments
		i = sXML.indexOf("<data");
		int j = sXML.indexOf("/>",i);
		int k = sXML.indexOf("</data>",i);
		if (j < 0 || (k > 0 && k < j)) {
			j = k + 7;
		}
		sXML = sXML.substring(0, i) + sAlignments + sXML.substring(j);
		
		// parse the resulting XML
		FileWriter outfile = new FileWriter("beast.xml");
		outfile.write(sXML);
		outfile.close();
		
		XMLParser parser = new XMLParser();
		List<Plugin> plugins = parser.parseTemplate(sXML);
		for (Plugin plugin: plugins) {
			PluginPanel.addPluginToMap(plugin);
			if (plugin instanceof beast.core.Runnable) {
				m_mcmc.setValue(plugin, this);
			}
		}
		return sAlignmentNames;
	} // mergeSequences
	
	
	/** Connect all inputs to the relevant ancestors of m_runnable.
	 * @throws Exception 
	 *  **/ 
	void connectModel() throws Exception {
		try {
		MCMC mcmc = (MCMC) m_mcmc.get();
		// build global list of loggers
		m_loggerInputs = new ArrayList<List<Plugin>>();
		for (Logger logger : ((MCMC)m_mcmc.get()).m_loggers.get()) {
			List<Plugin> loggers = new ArrayList<Plugin>();
			for (Plugin plugin : logger.m_pLoggers.get()) {
				loggers.add(plugin);
			}
			m_loggerInputs.add(loggers);
		}
		
		// collect priors from template
		CompoundDistribution posteror = (CompoundDistribution) mcmc.posteriorInput.get();
		m_priors = ((CompoundDistribution)posteror.pDistributions.get().get(0)).pDistributions;
		List<Distribution> list = m_priors.get();
		for (Distribution d : list) {
			m_potentialPriors.add(d);
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	void scrubAll(boolean bUseNotEstimatedStateNodes) {
		try {
			if (m_bAutoScrubPriors) {
				scrubPriors();
			}
			if (m_bAutoScrubState) {
				scrubState(bUseNotEstimatedStateNodes);
			}
			if (m_bAutoScrubLoggers) {
				scrubLoggers();
			}
			if (m_bAutoScrubOperators) {
				scrubOperators();
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	} // scrubAll
	
	/** remove operators on StateNodesthat have no impact on the posterior **/
	void scrubOperators() {
		List<Plugin> posteriorPredecessors = new ArrayList<Plugin>();
		collectPredecessors(((MCMC)m_mcmc.get()).posteriorInput.get(), posteriorPredecessors);
		// clear operatorsInput & add to global list of operators if not already there
		List<Operator> operators0 = ((MCMC)m_mcmc.get()).operatorsInput.get();
		operators0.clear();
		// add operators that have predecessors in posteriorPredecessors
		//for (Operator operator : m_operators) {
		for (Operator operator : PluginPanel.g_operators) {
			List<Plugin> operatorPredecessors = new ArrayList<Plugin>();
			collectPredecessors(operator, operatorPredecessors);
			for (Plugin plugin : operatorPredecessors) {
				if (posteriorPredecessors.contains(plugin)) {
					// test at least one of the inputs is a StateNode that needs to be estimated
					try {
						for (Plugin plugin2 : operator.listActivePlugins()) {
							if (plugin2 instanceof StateNode) {
								if (((StateNode)plugin2).m_bIsEstimated.get()) {
									operators0.add(operator);
									break;
								}
							}
						}
					} catch (Exception e) {
						// TODO: handle exception
					}
					break;
				}
			}
		}
	}
	
	/** remove loggers of StateNodes that have no impact on the posterior **/
	void scrubLoggers() {
		List<Plugin> posteriorPredecessors = new ArrayList<Plugin>();
		collectPredecessors(((MCMC)m_mcmc.get()).posteriorInput.get(), posteriorPredecessors);
		List<Logger> loggers = ((MCMC)m_mcmc.get()).m_loggers.get();
		for (int k = 0; k < loggers.size(); k++) {
			Logger logger = loggers.get(k);
			List<Plugin> loggerInput = m_loggerInputs.get(k);
			// clear logger & add to global list of loggers if not already there
			List<Plugin> loggers0 = logger.m_pLoggers.get();
			for (int i = loggers0.size()-1; i>=0;i--) {
				Plugin o = loggers0.remove(i);
				if (!loggerInput.contains(o)) {
					loggerInput.add(o);
				}
			}
			// add loggers that have predecessors in posteriorPredecessors
			for (Plugin newlogger : loggerInput) {
				List<Plugin> loggerPredecessors = new ArrayList<Plugin>();
				collectPredecessors(newlogger, loggerPredecessors);
				for (Plugin plugin : loggerPredecessors) {
					if (posteriorPredecessors.contains(plugin)) {
						loggers0.add(newlogger);
						break;
					}
				}
			}
		}
	}


	/** remove StateNodes that are not estimated or have no impact on the posterior **/
	void scrubState(boolean bUseNotEstimatedStateNodes) {
		try {
			List<Plugin> posteriorPredecessors = new ArrayList<Plugin>();
			collectPredecessors(((MCMC)m_mcmc.get()).posteriorInput.get(), posteriorPredecessors);
			
			State state = ((MCMC)m_mcmc.get()).m_startState.get(); 
			List<StateNode> stateNodes = state.stateNodeInput.get();
			stateNodes.clear();
			for (StateNode stateNode :  PluginPanel.g_stateNodes) {
				if (posteriorPredecessors.contains(stateNode) && (stateNode.m_bIsEstimated.get() || bUseNotEstimatedStateNodes)) {
					stateNodes.add(stateNode);
					System.err.println(stateNode.getID());
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** collect priors that have predecessors in the State, i.e. a StateNode that is estimated **/
	void scrubPriors() {
		List<Distribution> priors = m_priors.get();
		for (int i = priors.size()-1; i>=0; i--) {
			if (!(priors.get(i) instanceof TreeDistribution)) {
				priors.remove(i);
			}
		}
		//priors.clear();
		List<Plugin> posteriorPredecessors = new ArrayList<Plugin>();
		collectPredecessors(((MCMC)m_mcmc.get()).posteriorInput.get(), posteriorPredecessors);

		for (Distribution prior : m_potentialPriors) {
			List<Plugin> priorPredecessors = new ArrayList<Plugin>();
			collectPredecessors(prior, priorPredecessors);
			for (Plugin plugin : priorPredecessors) {
				if (//posteriorPredecessors.contains(plugin) && 
						plugin instanceof StateNode && ((StateNode) plugin).m_bIsEstimated.get()) {
					if (!(prior instanceof TreeDistribution)) {
						priors.add(prior);
					}
					break;
				}
			}
		}
	}
	
	void collectPredecessors(Plugin plugin, List<Plugin> predecessors) {
		predecessors.add(plugin);
		try {
			for (Plugin plugin2 : plugin.listActivePlugins()) {
				collectPredecessors(plugin2, predecessors);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
} // class BeautiDoc
