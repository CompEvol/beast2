package beast.app.beauti;


import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.operators.TipDatesScaler;
import beast.evolution.tree.TreeDistribution;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.Prior;
import beast.util.NexusParser;
import beast.util.XMLParser;
import beast.util.XMLProducer;

@Description("Beauti document in doc-view pattern, not useful in models")
public class BeautiDoc extends Plugin {
	final static String STANDARD_TEMPLATE = "templates/Standard.xml";
	
	//public Input<List<Alignment>> m_alignments = new Input<List<Alignment>>("alignment", "list of alignments or partitions", new ArrayList<Alignment>(), Validate.REQUIRED);
	public List<Alignment> m_alignments = new ArrayList<Alignment>();

	public Input<beast.core.Runnable> m_mcmc = new Input<beast.core.Runnable>("runnable", "main entry of analysis", Validate.REQUIRED);

	/** points to input that contains prior distribution, if any **/
	Input<List<Distribution>> m_priors; 
	/** contains all Priors from the template **/
	protected List<Distribution> m_potentialPriors;

	protected List<BranchRateModel> m_clockModels;
	/** contains all loggers from the template **/
	List<List<Plugin>> m_loggerInputs;
	
	boolean m_bAutoScrubOperators = true;
	boolean m_bAutoScrubLoggers = true;
	boolean m_bAutoScrubPriors = true;
	boolean m_bAutoScrubState = true;
	
	
	// RRB: hack to pass info to BeautiSubTemplate TODO: beautify this, since it prevents haveing multiple windows open
	static BeautiDoc g_doc;
	
	public BeautiDoc() {
		g_doc = this;
		setID("BeautiDoc");
		m_potentialPriors = new ArrayList<Distribution>();
		m_clockModels = new ArrayList<BranchRateModel>();

		m_pPartitions = new List[3];
		m_pPartitions[0] = new ArrayList();
		m_pPartitions[1] = new ArrayList();
		m_pPartitions[2] = new ArrayList();
	}
	
	public void load(String sFileName) throws Exception {
		String sXML = BeautiInitDlg.load(sFileName);
		extractSequences(sXML);
    	connectModel();
	}

	public void importNexus(String sFileName) throws Exception {
		NexusParser parser = new NexusParser();
		parser.parseFile(sFileName);
		if (parser.m_filteredAlignments.size() > 0) {
			for (Alignment data : parser.m_filteredAlignments) {
				addAlignmentWithSubnet(data);
			}
		} else {
			addAlignmentWithSubnet(parser.m_alignment);
		}
    	connectModel();
	}

	public void importXMLAlignment(String sFileName) throws Exception {
		Alignment data = (Alignment) AlignmentListInputEditor.getXMLData(sFileName);
		addAlignmentWithSubnet(data);
    	connectModel();
	}
	
    void initialize(BeautiInitDlg.ActionOnExit endState, String sXML, String sTemplate, String sFileName) throws Exception {
    	BeautiConfig.clear();
    	switch (endState) {
    	case UNKNOWN:
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
//    		// load standard template
//    		String sTemplateXML = BeautiInitDlg.processTemplate(STANDARD_TEMPLATE);
//    		loadTemplate(sTemplateXML);
//    		connectModel();
    	}
    }

    Alignment getPartition(Plugin plugin) {
		String sPartition = plugin.getID();
		sPartition = sPartition.substring(sPartition.indexOf('.') + 1);
		for (Alignment data : m_alignments) {
			if (data.getID().equals(sPartition)) {
				return data;
			}
		}
		return null;
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
		//String sXML = new XMLProducer().toXML(m_mcmc.get(), PluginPanel.g_plugins.values());
		String sXML = new XMLProducer().toXML(m_mcmc.get(), new HashSet<Plugin>());
		FileWriter outfile = new FileWriter(sFileName);
		outfile.write(sXML);
		outfile.close();
	} // save

	void extractSequences(String sXML) throws  Exception {
		// load standard template
		String sTemplateXML = BeautiInitDlg.processTemplate(STANDARD_TEMPLATE);
		loadTemplate(sTemplateXML);
		// parse file
		XMLParser parser = new XMLParser();
		Plugin MCMC = parser.parseFragment(sXML, true);
		m_mcmc.setValue(MCMC, this);
		PluginPanel.addPluginToMap(MCMC);
		// extract alignments
		determinePartitions();
	}

	
	BeautiConfig m_beautiConfig;
	
	/** Merge sequence data with sXML specification. 
	 **/
	void mergeSequences(String sXML) throws  Exception {
		if (sXML == null) {
			sXML = BeautiInitDlg.processTemplate(STANDARD_TEMPLATE);
		}
		loadTemplate(sXML);
		// create XML for alignments
		for (Alignment alignment : m_alignments) {
			m_beautiConfig.m_partitionTemplate.get().createSubNet(alignment, this);
		}
		determinePartitions();
		
//		// create XML for alignments
//		String sAlignments = "";
//		int n = 1;
//		String sRange="range='";
//		List<String> sAlignmentNames = new ArrayList<String>();
//		for (Alignment alignment : m_alignments.get()) {
//			sAlignmentNames.add(alignment.getID());
//			alignment.setID(/*"alignment" + */alignment.getID());
//			sRange += alignment.getID() +",";
//			n++;
//		}
//
//		for (Alignment alignment : m_alignments.get()) {
//			if (!(alignment instanceof FilteredAlignment)) {
//				sAlignments += new XMLProducer().toRawXML(alignment);
//			}
//		}
//		List<String> sDone = new ArrayList<String>();
//		for (Alignment alignment : m_alignments.get()) {
//			if (alignment instanceof FilteredAlignment) {
//				FilteredAlignment data = (FilteredAlignment) alignment;
//				Alignment baseData = data.m_alignmentInput.get();
//				String sBaseID = baseData.getID();
//				if (sDone.indexOf(sBaseID) < 0) {
//					sAlignments += new XMLProducer().toRawXML(baseData);
//					sDone.add(sBaseID);
//				}
//				// suppress alinmentInput
//				data.m_alignmentInput.setValue(null, data);
//				String sData = new XMLProducer().toRawXML(data);
//				// restore alinmentInput
//				data.m_alignmentInput.setValue(baseData, data);
//				sData = sData.replaceFirst("<data ", "<data data='@" + sBaseID +"' ");
//				sAlignments += sData;
//			}
//		}
//		
//		sRange = sRange.substring(0, sRange.length()-1)+"'";
//		
//		// process plates in template
//		int i  = -1;
//		do {
//			i = sXML.indexOf("<plate", i);
//			if (i >= 0) {
//				int j = sXML.indexOf("range='#alignments'");
//				int j2 = sXML.indexOf("range=\"#alignments\"");
//				if (j < 0 || (j2>=0 && j2 < j)) {
//					j = j2;
//				}
//				// sanity check: no close of elements encountered underway...
//				for (int k = i; k < j; k++) {
//					if (sXML.charAt(k) == '>') {
//						j = -1;
//					}
//				}
//				if ( j >= 0) {
//					sXML = sXML.substring(0, j) + sRange + sXML.substring(j+19);
//				}
//				i++;
//			}
//		} while (i >= 0);
//		
//		
//		if (sAlignmentNames.size() == 1) {
//			// process plates in template
//			i  = -1;
//			do {
//				i = sXML.indexOf("$(n).", i);
//				if ( i >= 0) {
//					sXML = sXML.substring(0, i) + sXML.substring(i+5);
//				}
//			} while (i >= 0);
//		}
//		
//		// merge in the alignments
//		i = sXML.indexOf("<data");
//		int j = sXML.indexOf("/>",i);
//		int k = sXML.indexOf("</data>",i);
//		if (j < 0 || (k > 0 && k < j)) {
//			j = k + 7;
//		}
//		sXML = sXML.substring(0, i) + sAlignments + sXML.substring(j);
//		
//		// parse the resulting XML
//		FileWriter outfile = new FileWriter("beast.xml");
//		outfile.write(sXML);
//		outfile.close();
//		
//		XMLParser parser = new XMLParser();
//		List<Plugin> plugins = parser.parseTemplate(sXML, new HashMap<String, Plugin>());
//		for (Plugin plugin: plugins) {
//			PluginPanel.addPluginToMap(plugin);
//			if (plugin instanceof beast.core.Runnable) {
//				m_mcmc.setValue(plugin, this);
//			}
//		}
//		return sAlignmentNames;
	} // mergeSequences
	
	void loadTemplate(String sXML) throws Exception {
		// load the template and its beauti configuration parts
		XMLParser parser = new XMLParser();
		List<Plugin> plugins = parser.parseTemplate(sXML, new HashMap<String, Plugin>());
		for (Plugin plugin : plugins) {
			if (plugin instanceof beast.core.Runnable) {
				m_mcmc.setValue(plugin, this);
			} else if (plugin instanceof BeautiConfig) {
				m_beautiConfig = (BeautiConfig) plugin;
			} else {
				System.err.println("template item " + plugin.getID() + " is ignored");
			}
			PluginPanel.addPluginToMap(plugin);
		}
	}

	private void determinePartitions() {
		CompoundDistribution likelihood = (CompoundDistribution) PluginPanel.g_plugins.get("likelihood");
		m_alignments.clear();
		m_pPartitions[0].clear();
		m_pPartitions[1].clear();
		m_pPartitions[2].clear();
		for (Distribution distr : likelihood.pDistributions.get()) {
			if (distr instanceof TreeLikelihood) {
				TreeLikelihood treeLikelihoods = (TreeLikelihood) distr;
				m_alignments.add(treeLikelihoods.m_data.get());
				m_pPartitions[0].add(treeLikelihoods.m_pSiteModel.get());
				m_pPartitions[1].add(treeLikelihoods.m_pBranchRateModel.get());
				m_pPartitions[2].add(treeLikelihoods.m_tree.get());
			}
		}
	}

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
		CompoundDistribution prior = (CompoundDistribution) PluginPanel.g_plugins.get("prior");
		m_priors = prior.pDistributions;
		List<Distribution> list = m_priors.get();
		for (Distribution d : list) {
			m_potentialPriors.add(d);
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		collectClockModels();
	}	
	
	private void collectClockModels() {
		// collect branch rate models from template
		CompoundDistribution likelihood = (CompoundDistribution) PluginPanel.g_plugins.get("likelihood");
		int k = 0;
		for (Distribution d : likelihood.pDistributions.get()) {
			BranchRateModel.Base clockModel = ((TreeLikelihood)d).m_pBranchRateModel.get();
			String sID = clockModel.getID();
			sID = sID.substring(sID.indexOf('.')+1);
			String sPartition = m_alignments.get(k).getID();
			if (sID.equals(sPartition)) {
				if (m_clockModels.size() <= k) {
					m_clockModels.add(clockModel);
				} else {
					m_clockModels.set(k, clockModel);
				}
			}
			k++;
		}
	}

	BranchRateModel getClockModel(String sPartition) {
		int k = 0;
		for (Alignment data : m_alignments) {
			if (data.getID().equals(sPartition)) {
				return m_clockModels.get(k);
			}
			k++;
		}
		return null;
	}
	
	void scrubAll(boolean bUseNotEstimatedStateNodes) {
		try {
			if (m_bAutoScrubState) {
				scrubState(bUseNotEstimatedStateNodes);
			}
			if (m_bAutoScrubPriors) {
				scrubPriors();
			}
			if (m_bAutoScrubLoggers) {
				scrubLoggers();
			}
			if (m_bAutoScrubOperators) {
				scrubOperators();
			}
			collectClockModels();
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
		
		List<StateNode> stateNodes = ((MCMC)m_mcmc.get()).m_startState.get().stateNodeInput.get();

		// add operators that have predecessors in posteriorPredecessors
		for (Operator operator : PluginPanel.g_operators) {
			List<Plugin> operatorPredecessors = new ArrayList<Plugin>();
			collectPredecessors(operator, operatorPredecessors);
			for (Plugin plugin : operatorPredecessors) {
				if (posteriorPredecessors.contains(plugin)) {
					// test at least one of the inputs is a StateNode that needs to be estimated
					try {
						for (Plugin plugin2 : operator.listActivePlugins()) {
							if (plugin2 instanceof StateNode) {
								if (((StateNode)plugin2).m_bIsEstimated.get() && stateNodes.contains(plugin2)) {
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
			
			State state = ((MCMC)m_mcmc.get()).m_startState.get(); 
			List<StateNode> stateNodes = state.stateNodeInput.get();
			stateNodes.clear();
			
			// grab all statenodes that impact the posterior
			List<Plugin> posteriorPredecessors = new ArrayList<Plugin>();
			Plugin posterior = ((MCMC)m_mcmc.get()).posteriorInput.get();
			collectPredecessors(posterior, posteriorPredecessors);
			for (StateNode stateNode :  PluginPanel.g_stateNodes) {
				if (posteriorPredecessors.contains(stateNode) && (stateNode.m_bIsEstimated.get() || bUseNotEstimatedStateNodes)) {
					stateNodes.add(stateNode);
					//System.err.println(stateNode.getID());
				}
			}
			for (int i = stateNodes.size() - 1; i >= 0; i--) {
				Plugin stateNode = stateNodes.get(i);
				List<Plugin> ancestors = new ArrayList<Plugin>();
				collectNonTrivialAncestors(stateNode, ancestors);
				if (!ancestors.contains(posterior)) {
					stateNodes.remove(i);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	/** collect priors that have predecessors in the State, i.e. a StateNode that is estimated **/
	void scrubPriors() {
		if (m_priors == null) {
			return;
		}
		try {
			List<Distribution> priors = m_priors.get();
			for (int i = priors.size()-1; i>=0; i--) {
				Distribution prior = priors.get(i);
				if (!m_potentialPriors.contains(prior)) {
					m_potentialPriors.add(prior);
				}
				if (prior instanceof MRCAPrior) {
					if (((MRCAPrior) prior).m_bOnlyUseTipsInput.get()) {
						priors.remove(i);
					}
				} else if (!(prior instanceof TreeDistribution)) {
					priors.remove(i);
				}
			}
			//priors.clear();
			List<Plugin> posteriorPredecessors = new ArrayList<Plugin>();
			collectPredecessors(((MCMC)m_mcmc.get()).posteriorInput.get(), posteriorPredecessors);
	
			List<StateNode> stateNodes = ((MCMC)m_mcmc.get()).m_startState.get().stateNodeInput.get();
	
			for (Distribution prior : m_potentialPriors) {
				List<Plugin> priorPredecessors = new ArrayList<Plugin>();
				collectPredecessors(prior, priorPredecessors);
				for (Plugin plugin : priorPredecessors) {
					if (//posteriorPredecessors.contains(plugin) && 
							plugin instanceof StateNode && ((StateNode) plugin).m_bIsEstimated.get()) {
						if (prior instanceof MRCAPrior) {
							if (((MRCAPrior) prior).m_bOnlyUseTipsInput.get()) {
								// It is a tip dates prior. Check there is a tip dates operator.
								for (Plugin plugin2 : ((MRCAPrior) prior).m_treeInput.get().outputs) {
									if (plugin2 instanceof TipDatesScaler && ((TipDatesScaler) plugin2).m_pWeight.get() > 0) {
										priors.add(prior);
									}
								}
							}
						} else if (!(prior instanceof TreeDistribution) && !(prior instanceof MRCAPrior) && stateNodes.contains(plugin)) {
							priors.add(prior);
						}
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void collectPredecessors(Plugin plugin, List<Plugin> predecessors) {
		predecessors.add(plugin);
		try {
			for (Plugin plugin2 : plugin.listActivePlugins()) {
				if (!predecessors.contains(plugin2)) {
					collectPredecessors(plugin2, predecessors);
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/** collect all ancestors (outputs, their outputs, etc) that are not Priors **/
	private void collectNonTrivialAncestors(Plugin plugin, List<Plugin> ancestors) {
		if (ancestors.contains(plugin)) {
			return;
		} 
		ancestors.add(plugin);
		int nSize = ancestors.size();
		for (int i = 0; i < nSize; i++) {
			Plugin plugin2 = ancestors.get(i);
			for (Plugin output : plugin2.outputs) {
				if (!(output instanceof Prior)) {
					collectNonTrivialAncestors(output, ancestors);
				}
			}
		}		
	}

	
	/** [0] = sitemodel [1] = clock model [2] = tree **/
	List<Plugin>[] m_pPartitions;
	
	public void addPlugin(Plugin plugin) throws Exception {
		PluginPanel.addPluginToMap(plugin);
//		m_sIDMap.put(plugin.getID(), plugin);
//		for (Plugin plugin2 : plugin.listActivePlugins()) {
//			addPlugin(plugin2);
//		}
	}

	/** connect source plugin with target plugin **/ 
	public void connect(Plugin srcPlugin, String sTargetID, String sInputName) throws Exception {
		try {
			Plugin target = PluginPanel.g_plugins.get(sTargetID);
			target.setInputValue(sInputName, srcPlugin);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void addAlignmentWithSubnet(Alignment data) {
		m_alignments.add(data);
		m_beautiConfig.m_partitionTemplate.get().createSubNet(data, this);
		// re-determine partitions
		CompoundDistribution likelihood = (CompoundDistribution) PluginPanel.g_plugins.get("likelihood");
		m_pPartitions[0].clear();
		m_pPartitions[1].clear();
		m_pPartitions[2].clear();
		for (Distribution distr : likelihood.pDistributions.get()) {
			if (distr instanceof TreeLikelihood) {
				TreeLikelihood treeLikelihoods = (TreeLikelihood) distr;
				m_pPartitions[0].add(treeLikelihoods.m_pSiteModel.get());
				m_pPartitions[1].add(treeLikelihoods.m_pBranchRateModel.get());
				m_pPartitions[2].add(treeLikelihoods.m_tree.get());
			}
		}
	}

	public List<Plugin> getPartitions(String sType) {
		if (sType == null) {
			return m_pPartitions[2];
		}
		if (sType.contains("SiteModel")) {
			return m_pPartitions[0];
		}
		if (sType.contains("BranchRateModel")) {
			return m_pPartitions[1];
		}
		return m_pPartitions[2];
	}





} // class BeautiDoc
