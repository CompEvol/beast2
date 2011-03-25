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
import beast.core.StateNode;
import beast.core.util.CompoundDistribution;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.sitemodel.SiteModelInterface;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreePrior;
import beast.util.XMLParser;
import beast.util.XMLProducer;

@Description("Beauti document in doc-view pattern, not useful in models")
public class BeautiDoc extends Plugin {
	public Input<List<Alignment>> m_alignments = new Input<List<Alignment>>("alignment", "list of alignments or partitions", new ArrayList<Alignment>(), Validate.REQUIRED);
	public Input<List<TreeLikelihood>> m_likelihoods = new Input<List<TreeLikelihood>>("treelikelihood", "list of tree likelihoods, at least one per alignment", new ArrayList<TreeLikelihood>());
	
//	public Input<Plugin> m_taxonset = new Input<Plugin>("taxonset", "specifies set of taxa"); 
//	public Input<List<TraitSet>> m_tipdates = new Input<List<TraitSet>>("tipdates", "specify dates of taxa", new ArrayList<TraitSet>()); 
//	public Input<List<SiteModel>> m_sitemodel = new Input<List<SiteModel>>("sitemodel", "site model, contains substitution model", new ArrayList<SiteModel>());
//	public Input<List<BranchRateModel.Base>> m_clockmodel = new Input<List<BranchRateModel.Base>>("clockmodel", "clock model", new ArrayList<BranchRateModel.Base>()); 
	public Input<List<TreePrior>> m_treeprior = new Input<List<TreePrior>>("treeprior", "prior on the tree or trees", new ArrayList<TreePrior>()); 
	public Input<List<Distribution>> m_priors = new Input<List<Distribution>>("prior", "list of prior distributions", new ArrayList<Distribution>()); 

	public Input<List<TaxonSet>> m_taxonset; // = new Input<List<TaxonSet>>("taxonset", "list of taxon sets", new ArrayList<TaxonSet>()); 

	
	/** place holders for plugins **/
	public Input<SiteModel.Base> m_siteModel = new Input<SiteModel.Base>("sitemodel", "site model, contains substitution model");
	SiteModel.Base m_siteModelOrg;
	
	public Input<BranchRateModel.Base> m_clockModel = new Input<BranchRateModel.Base>("clockmodel", "clock model");
	BranchRateModel.Base m_clockModelOrg;

	public Input<TraitSet> m_tipdates = new Input<TraitSet>("tipdates", "specify dates of taxa");
	TraitSet m_tipdatesOrg;

	/** contains all operators from the template **/
	List<Operator> m_operators;
	/** contains all loggers from the template **/
	List<List<Plugin>> m_loggerInputs;
	/** contains all taxa from the template **/
	static protected List<Taxon> m_taxa;
	
	
	boolean m_bAutoScrubOperators = true;
	boolean m_bAutoScrubLoggers = true;
	
//	public Input<List<Operator>> m_operators;
	public Input<MCMC> m_mcmc = new Input<MCMC>("runnable", "main entry of analysis", Validate.REQUIRED);
	
	public BeautiDoc() {
		setID("BeautiDoc");
	}
	class InputID {
		Input<?> m_input;
		int m_nEntryNr = -1;
		Plugin m_plugin;
	}
	
    void initialize(BeautiInitDlg.ActionOnExit endState, String sXML, String sTemplate, String sFileName) throws Exception {
    	switch (endState) {
    	case SHOW_DETAILS_USE_TEMPLATE: {
    		List<String> sAlignmentNames = mergeSequences(sTemplate);
        	connectModel(sAlignmentNames);
    		break;
    	}
    	case SHOW_DETAILS_USE_XML_SPEC: {
    		List<String> sAlignmentNames = extractSequences(sXML);
        	connectModel(sAlignmentNames);
//    		XMLParser parser = new XMLParser();
//    		m_mcmc.setValue(parser.parseFragment(sXML, true), this);
//        	connectModel(null);
    		break;
    	}
    	case WRITE_XML: {
    		List<String> sAlignmentNames = mergeSequences(sTemplate);
        	connectModel(sAlignmentNames);
        	save(sFileName);
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
		if (m_bAutoScrubLoggers) {
			scrubLoggers();
		}
		if (m_bAutoScrubOperators) {
			scrubOperators();
		}
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
		int j = sXML.indexOf(">",i);
		sXML = sXML.substring(0, i) + sAlignments + sXML.substring(j);
		
		// parse the resulting XML
		FileWriter outfile = new FileWriter("beast.xml");
		outfile.write(sXML);
		outfile.close();
		
		XMLParser parser = new XMLParser();
		//m_mcmc.setValue(parser.parseFile("beast.xml"), this);
		List<Plugin> plugins = parser.parseTemplate(sXML);
		for (Plugin plugin: plugins) {
			PluginPanel.addPluginToMap(plugin);
			if (plugin instanceof beast.core.Runnable) {
				m_mcmc.setValue(plugin, this);
			}
		}
		return sAlignmentNames;
	}
	
	
	/** Connect all inputs to the relevant ancestors of m_runnable.
	 * @throws Exception 
	 *  **/ 
	@SuppressWarnings("unchecked")
	void connectModel(List<String> sAlignmentNames) throws Exception {
		
		MCMC mcmc = m_mcmc.get();
		CompoundDistribution posterior = (CompoundDistribution) mcmc.posteriorInput.get();
		if (posterior.pDistributions.get().size() != 2) {
			throw new Exception("Expected posterior of the form prior, posterior, instead of " + posterior.pDistributions.get().size() + " distributions");
		}
		Distribution prior = posterior.pDistributions.get().get(0);
		if (prior instanceof CompoundDistribution) {
			CompoundDistribution priors = (CompoundDistribution) prior;
			for (Distribution dist: priors.pDistributions.get()) {
				connectPrior(dist);
			}
		} else {
			connectPrior(prior);
		}
		
		Distribution likelihood = posterior.pDistributions.get().get(1);
		if (likelihood instanceof CompoundDistribution) {
			CompoundDistribution likelihoods = (CompoundDistribution) likelihood;
			for (Distribution treelikelihood : likelihoods.pDistributions.get()) {
				if (treelikelihood instanceof TreeLikelihood) {
					connectTreeLikelihood (treelikelihood);
					setTreeLikelihoodID(treelikelihood, sAlignmentNames);
				} else {
					throw new Exception("Expected treelikelihood or derived distribution in compound posterior");
				}
			}
		} else if (likelihood instanceof TreeLikelihood) {
			connectTreeLikelihood (likelihood);
			setTreeLikelihoodID(likelihood, sAlignmentNames);
		} else {
			throw new Exception("Expected treelikelihood or derived distribution in posterior");
		}
		// put in some defaults, if not provided by the template
//		boolean bAddTipDates = (m_tipdates.get().size() == 0);
//		boolean bAddBranchRates = (m_clockmodel.get().size() == 0);
		if (likelihood instanceof CompoundDistribution) {
			for (Distribution treelikelihood : ((CompoundDistribution) likelihood).pDistributions.get()) {
				Tree tree = ((TreeLikelihood) treelikelihood).m_tree.get();
//				if (bAddTipDates) {
				if (tree.m_trait.get() == null) {
					TraitSet traitSet = new TraitSet();
//					m_tipdates.setValue(traitSet, this);
					tree.m_trait.setValue(traitSet, tree);
				}
//				if (bAddBranchRates) {
//					BranchRateModel.Base clockmodel = new StrictClockModel();
//					m_clockmodel.setValue(clockmodel, this);
//					((TreeLikelihood) treelikelihood).m_pBranchRateModel.setValue(clockmodel, treelikelihood);
//				}
			}
		} else {
			Tree tree = ((TreeLikelihood)likelihood).m_tree.get();
//			if (bAddTipDates) {
			if (tree.m_trait.get() == null) {
				TraitSet traitSet = new TraitSet();
//				m_tipdates.setValue(traitSet, this);
				tree.m_trait.setValue(traitSet, tree);
			}
//			if (bAddBranchRates) {
//				BranchRateModel.Base clockmodel = new StrictClockModel();
//				m_clockmodel.setValue(clockmodel, this);
//				((TreeLikelihood) likelihood).m_pBranchRateModel.setValue(clockmodel, likelihood);
//			}
		}
		
		
		PluginPanel.addPluginToMap(m_mcmc.get());

		for (Plugin plugin : PluginPanel.g_plugins.values()) {
			for (@SuppressWarnings("rawtypes") Input input : plugin.listInputs()) {
				if (input.getType() ==  TaxonSet.class && input.get() instanceof List) {
					m_taxonset = input;
				}
			}
		}
		
		
		// build global list of operators
		m_operators = new ArrayList<Operator>();
		for (Operator o : m_mcmc.get().operatorsInput.get()) {
			m_operators.add(o);
		}
		// build global list of loggers
		m_loggerInputs = new ArrayList<List<Plugin>>();
		for (Logger logger : m_mcmc.get().m_loggers.get()) {
			List<Plugin> loggers = new ArrayList<Plugin>();
			for (Plugin plugin : logger.m_pLoggers.get()) {
				loggers.add(plugin);
			}
			m_loggerInputs.add(loggers);
		}
		// build global list of taxa
		m_taxa = new ArrayList<Taxon>();
		for (Plugin plugin : PluginPanel.g_plugins.values()) {
			if (plugin instanceof Sequence) {
				Taxon taxon = new Taxon();
				taxon.setID(((Sequence)plugin).m_sTaxon.get());
				m_taxa.add(taxon);
			}
		}		
	} // connectModel
	
	void setTreeLikelihoodID(Distribution treelikelihood, List<String >sAlignmentNames) {
		if (sAlignmentNames != null && sAlignmentNames.size() > 0) {
			treelikelihood.setID(sAlignmentNames.get(0));
			sAlignmentNames.remove(0);
		}
	} // setTreeLikelihoodID

	void connectTreeLikelihood(Distribution distribution) throws Exception {
		m_likelihoods.setValue(distribution, this);
		TreeLikelihood likelihood = (TreeLikelihood) distribution;
		m_siteModel.setValue(likelihood.m_pSiteModel.get(), this);
		if (likelihood.m_pBranchRateModel.get() != null) {
			m_clockModel.setValue(likelihood.m_pBranchRateModel.get(), this);
		}
		Tree tree = likelihood.m_tree.get();
		if (tree != null) {
			if (tree.m_trait.get() != null) {
				m_tipdates.setValue(tree.m_trait.get(), this);
			}
		}
		// hacky bit to ensure SubstitutionModel can handle DataType of alignment data
		SiteModelInterface.Base siteModel = likelihood.m_pSiteModel.get();
		try {
			siteModel.canSetSubstModel(siteModel.getSubstitutionModel());
		} catch (Exception e) {
			// obviously not
	        for (Plugin plugin : PluginPanel.g_plugins.values()) {
        		try {
					if (siteModel.canSetSubstModel(plugin)) {
						siteModel.m_pSubstModel.setValue(plugin, siteModel);
						break;
					}
				} catch (Exception ex) {
					// ignore
				}
            }
        }
	}
	
	void connectPrior(Distribution distribution) throws Exception {
		if (distribution instanceof TreePrior) {
			m_treeprior.setValue(distribution, this);
		} else {
			m_priors.setValue(distribution, this);
		}
	}
	
	
	/** methods for dealing with updates **/
	void sync(int iPanel) {
		try {
		switch (iPanel) {
		case Beauti.DATA_PANEL : break;
		case Beauti.TAXON_SETS_PANEL : //refreshInputPanel(m_doc, m_doc.m_taxonset, false, true);break;
			break;
		case Beauti.TIP_DATES_PANEL : //refreshInputPanel(m_doc, m_doc.m_tipdates, false, true);break;
			if (m_tipdatesOrg != m_tipdates.get()) {
				for (TreeLikelihood likelihood: m_likelihoods.get()) {
					Tree tree = likelihood.m_tree.get();
					if (tree.m_trait.get() == m_tipdatesOrg) {
						tree.m_trait.setValue(m_tipdates.get(), tree);
					}
				}
			}
			break;
		case Beauti.SITE_MODEL_PANEL : //refreshInputPanel(m_doc, m_doc.m_sitemodel, false, true);break;
			if (m_siteModelOrg != m_siteModel.get()) {
				for (TreeLikelihood likelihood: m_likelihoods.get()) {
					if (likelihood.m_pSiteModel.get() == m_siteModelOrg) {
						likelihood.m_pSiteModel.setValue(m_siteModel.get(), likelihood);
					}
				}
			}
			break;
		case Beauti.CLOCK_MODEL_PANEL : //refreshInputPanel(m_doc, m_doc.m_clockmodel, false, true);break;
			if (m_clockModelOrg != m_clockModel.get()) {
				for (TreeLikelihood likelihood: m_likelihoods.get()) {
					if (likelihood.m_pBranchRateModel.get() == m_clockModelOrg) {
						likelihood.m_pBranchRateModel.setValue(m_clockModel.get(), likelihood);
					}
				}
			}
			break;
		case Beauti.TREE_PRIOR_PANEL : //refreshInputPanel(m_doc, m_doc.m_treeprior, true, false);break;
			break;
		case Beauti.PRIORS_PANEL : //refreshInputPanel(m_doc, m_doc.m_priors, true, false);break;
			break;
		case Beauti.OPERATORS_PANEL : break;
		case Beauti.MCMC_PANEL : break;
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void syncSiteModel() throws Exception {
	}
	
	
	void syncTo(int iPanel, int iPartition) {
		try {
		switch (iPanel) {
		case Beauti.DATA_PANEL : break;
		case Beauti.TAXON_SETS_PANEL : 
			break;
		case Beauti.TIP_DATES_PANEL : 
			m_tipdatesOrg = m_likelihoods.get().get(iPartition).m_tree.get().m_trait.get();
			m_tipdates.setValue(m_tipdatesOrg, this);
			break;
		case Beauti.SITE_MODEL_PANEL : 
			m_siteModelOrg = m_likelihoods.get().get(iPartition).m_pSiteModel.get();
			m_siteModel.setValue(m_siteModelOrg, this);
			syncSiteModel();
			break;
		case Beauti.CLOCK_MODEL_PANEL :
			m_clockModelOrg = m_likelihoods.get().get(iPartition).m_pBranchRateModel.get();
			m_clockModel.setValue(m_clockModelOrg , this);
			break;
		case Beauti.TREE_PRIOR_PANEL : 
			break;
		case Beauti.PRIORS_PANEL : 
			break;
		case Beauti.OPERATORS_PANEL : 
			if (m_bAutoScrubOperators) {
				scrubOperators();
			}
			break;
		case Beauti.MCMC_PANEL : 
			if (m_bAutoScrubLoggers) {
				scrubLoggers();
			}
			break;
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/** remove operators on StateNodesthat have no impact on the posterior **/
	void scrubOperators() {
		List<Plugin> posteriorPredecessors = new ArrayList<Plugin>();
		collectPredecessors(m_mcmc.get().posteriorInput.get(), posteriorPredecessors);
		// clear operatorsInput & add to global list of operators if not already there
		List<Operator> operators0 = m_mcmc.get().operatorsInput.get();
		for (int i = operators0.size()-1; i>=0;i--) {
			Operator o = operators0.remove(i);
			if (!m_operators.contains(o)) {
				m_operators.add(o);
			}
		}
		// add operators that have predecessors in posteriorPredecessors
		for (Operator operator : m_operators) {
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
		collectPredecessors(m_mcmc.get().posteriorInput.get(), posteriorPredecessors);
		List<Logger> loggers = m_mcmc.get().m_loggers.get();
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
