package beast.app.beauti;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

	public enum ActionOnExit {
		UNKNOWN, SHOW_DETAILS_USE_TEMPLATE, SHOW_DETAILS_USE_XML_SPEC, WRITE_XML
	}

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
	
	/** [0] = sitemodel [1] = clock model [2] = tree **/
	List<Plugin>[] m_pPartitionByAlignments;
	List<Plugin>[] m_pPartition;
	private List<Integer>[] m_nCurrentPartitions;
	// partition names
	List<String> m_sPartitionNames = new ArrayList<String>();
	
	// RRB: hack to pass info to BeautiSubTemplate TODO: beautify this, since it prevents haveing multiple windows open
	// All globals need to be removed to make multiple document view work on Macs
	static BeautiDoc g_doc;
	Beauti m_beauti;
	
	public BeautiDoc() {
		g_doc = this;
		setID("BeautiDoc");
		clear();
	}
	
	
	String m_sTemplateName = null;
	String m_sFileName = null;

	public ActionOnExit parseArgs(String[] args) throws Exception {
		ActionOnExit m_endState = ActionOnExit.UNKNOWN;
		String m_sOutputFileName = "beast.xml";
		String m_sXML = null;
		String m_sTemplateXML = null;
		String m_sTemplateFileName;

		int i = 0;
		try {
			while (i < args.length) {
				int iOld = i;
				if (args[i].equals("")) {
					i += 1;
				} else if (args[i].equals("-h") || args[i].equals("-help")) {
					showUsageAndExit();
				} else if (args[i].equals("-xml")) {
					String sFileName = args[i + 1];
					m_sXML = load(sFileName);
					// XMLParser parser = new XMLParser();
					// m_doc.m_mcmc.setValue(parser.parseFile(sFileName),
					// m_doc);
					m_sFileName = nameFromFile(sFileName);
					i += 2;
				} else if (args[i].equals("-template")) {
					String sFileName = args[i + 1];
					m_sTemplateXML = processTemplate(sFileName);
					m_sTemplateFileName = sFileName;
					m_sTemplateName = nameFromFile(sFileName);
					i += 2;
				} else if (args[i].equals("-nex")) {
					// NB: multiple -nex/-xmldata commands can be processed!
					String sFileName = args[i + 1];
					NexusParser parser = new NexusParser();
					parser.parseFile(sFileName);
					if (parser.m_filteredAlignments.size() > 0) {
						for (Alignment data : parser.m_filteredAlignments) {
							m_alignments.add(data);
						}
					} else {
						m_alignments.add(parser.m_alignment);
					}
					i += 2;
				} else if (args[i].equals("-xmldata")) {
					// NB: multiple -xmldata/-nex commands can be processed!
					String sFileName = args[i + 1];
					Alignment alignment = (Alignment) AlignmentListInputEditor.getXMLData(sFileName);
					m_alignments.add(alignment);
					i += 2;
				} else if (args[i].equals("-exitaction")) {
					if (args[i + 1].equals("writexml")) {
						m_endState = ActionOnExit.WRITE_XML;
					} else if (args[i + 1].equals("usetemplate")) {
						m_endState = ActionOnExit.SHOW_DETAILS_USE_TEMPLATE;
					} else if (args[i + 1].equals("usexml")) {
						m_endState = ActionOnExit.SHOW_DETAILS_USE_XML_SPEC;
					} else {
						throw new Exception("Expected one of 'writexml','usetemplate' or 'usexml', not " + args[i + 1]);
					}
					i += 2;
				} else if (args[i].equals("-out")) {
					m_sOutputFileName = args[i + 1];
					i += 2;
				}
				if (i == iOld) {
					throw new Exception("Wrong argument: " + args[i]);
				}
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		
        initialize(m_endState, m_sXML, m_sTemplateXML, m_sOutputFileName);
        return m_endState;
	} // parseArgs


	String nameFromFile(String sFileName) {
		if (sFileName.contains("/")) {
			return sFileName.substring(sFileName.lastIndexOf("/") + 1, sFileName.length() - 4);
		} else if (sFileName.contains("\\")) {
			return sFileName.substring(sFileName.lastIndexOf("\\") + 1, sFileName.length() - 4);
		}
		return sFileName.substring(0, sFileName.length() - 4);
	}

	void showUsageAndExit() {
		System.out.println(usage());
		System.exit(0);
	}
	
	String usage() {
		return "java Beauti [options]\n" + "where options can be one of the following:\n"
				+ "-template [template file]\n" + "-nex [nexus data file]\n" + "-xmldat [beast xml file]\n"
				+ "-xml [beast file]\n" + "-out [output file name]\n" + "-exitaction [writexml|usetemplate|usexml]\n";
	}
	
	public void setBeauti(Beauti beauti) {
		m_beauti = beauti;		
	}
	
	void clear() {
		m_potentialPriors = new ArrayList<Distribution>();
		m_clockModels = new ArrayList<BranchRateModel>();
		m_alignments = new ArrayList<Alignment>();

		m_pPartitionByAlignments = new List[3];
		m_pPartition = new List[3];
		m_nCurrentPartitions = new List[3];
		m_sPartitionNames = new ArrayList<String>();
		for (int i = 0; i < 3; i++) {
			m_pPartitionByAlignments[i] = new ArrayList();
			m_pPartition[i] = new ArrayList();
			m_nCurrentPartitions[i] = new ArrayList<Integer>();
		}
	}
	
	/** remove all alignment data and model, and reload Standard template **/
	public void newAnalysis() {
		try {
			clear();
			PluginPanel.init();
			BeautiConfig.clear();
			String sXML = processTemplate(STANDARD_TEMPLATE);
			loadTemplate(sXML);
	    	m_beauti.setUpPanels();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadXML(String sFileName) throws Exception {
		String sXML = load(sFileName);
		extractSequences(sXML);
    	connectModel();
    	m_beauti.setUpPanels();
	}

	public void loadNewTemplate(String sFileName) throws Exception {
		String sXML = processTemplate(sFileName);
		extractSequences(sXML);
    	connectModel();
    	m_beauti.setUpPanels();
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
    	m_beauti.setUpPanels();
	}

	public void importXMLAlignment(String sFileName) throws Exception {
		Alignment data = (Alignment) AlignmentListInputEditor.getXMLData(sFileName);
		addAlignmentWithSubnet(data);
    	connectModel();
    	m_beauti.setUpPanels();
	}
	
    void initialize(ActionOnExit endState, String sXML, String sTemplate, String sFileName) throws Exception {
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

    
	String processTemplate(String sFileName) throws Exception {
		final String MERGE_ELEMENT = "mergepoint";
		File mainTemplate = new File(sFileName);
		String sTemplateXML = load(sFileName);
		// find merge points
		int i = 0;
		HashMap<String, String> sMergePoints = new HashMap<String, String>();
		while (i >= 0) {
			i = sTemplateXML.indexOf("<" + MERGE_ELEMENT, i + 1);
			if (i > 0) {
				int j = sTemplateXML.indexOf('>', i);
				String sStr = sTemplateXML.substring(i, j);
				sStr = sStr.replaceAll(".*id=", "");
				char c = sStr.charAt(0);
				sStr = sStr.replaceAll(c + "[^" + c + "]*$", "");
				sStr = sStr.substring(1);
				sMergePoints.put(sStr, "");
			}
		}

		// find XML to merge

		File templates = new File("templates");
		File[] files = templates.listFiles();
		if (files != null) {
			for (File template : files) {
				if (!template.getAbsolutePath().equals(mainTemplate.getAbsolutePath()) && 
						template.getName().toLowerCase().endsWith(".xml")) {
					try {

						DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
						// factory.setValidating(true);
						Document doc = factory.newDocumentBuilder().parse(template);
						doc.normalize();

						processBeautiConfig(doc);

						// find mergewith elements
						NodeList nodes = doc.getElementsByTagName("mergewith");
						for (int iMergeElement = 0; iMergeElement < nodes.getLength(); iMergeElement++) {
							Node mergeElement = nodes.item(iMergeElement);
							String sMergePoint = mergeElement.getAttributes().getNamedItem("point").getNodeValue();
							if (!sMergePoints.containsKey(sMergePoint)) {
								System.err.println("Cannot find merge point named " + sMergePoint + " from "
										+ template.getName() + " in template. MergeWith ignored.");
							} else {
								String sXML = "";
								NodeList children = mergeElement.getChildNodes();
								for (int iChild = 0; iChild < children.getLength(); iChild++) {
									sXML += nodeToString(children.item(iChild));
								}
								String sStr = sMergePoints.get(sMergePoint);
								sStr += sXML;
								sMergePoints.put(sMergePoint, sStr);
							}

						}
					} catch (Exception e) {
						if (!e.getMessage().contains("beast.app.beauti.InputConstraint")) {
							System.err.println(e.getMessage());
						}
					}
				}
			}

			// merge XML
			i = 0;
			while (i >= 0) {
				i = sTemplateXML.indexOf("<" + MERGE_ELEMENT, i + 1);
				if (i > 0) {
					int j = sTemplateXML.indexOf('>', i);
					String sStr = sTemplateXML.substring(i, j);
					sStr = sStr.replaceAll(".*id=", "");
					char c = sStr.charAt(0);
					sStr = sStr.replaceAll(c + "[^" + c + "]*$", "");
					sStr = sStr.substring(1);
					String sXML = sMergePoints.get(sStr);
					sTemplateXML = sTemplateXML.substring(0, i) + sXML + sTemplateXML.substring(j + 1);
				}
			}
		}
		m_sTemplateName = nameFromFile(sFileName);
		return sTemplateXML;
	}

	void processBeautiConfig(Document doc) throws Exception {
		// find configuration elements, process and remove
		NodeList nodes = doc.getElementsByTagName("beauticonfig");
        Node topNode = doc.getElementsByTagName("*").item(0);
        String sNameSpaceStr = XMLParser.getAttribute(topNode, "namespace");
        for (int iConfigElement = 0; iConfigElement < nodes.getLength(); iConfigElement++) {
			Node configElement = nodes.item(iConfigElement);
			String sXML = nodeToString(configElement);
			XMLParser parser = new XMLParser();
			parser.setNameSpace(sNameSpaceStr);
			parser.parseBareFragment(sXML, true);
			configElement.getParentNode().removeChild(configElement);
		}
	}

	String nodeToString(Node node) throws TransformerException {
		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer transformer = transFactory.newTransformer();
		StringWriter buffer = new StringWriter();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(node), new StreamResult(buffer));
		return buffer.toString();
	}

	String load(String sFileName) throws IOException {
		BufferedReader fin = new BufferedReader(new FileReader(sFileName));
		StringBuffer buf = new StringBuffer();
		String sStr = null;
		while (fin.ready()) {
			sStr = fin.readLine();
			buf.append(sStr);
			buf.append('\n');
		}
		fin.close();

		String sXML = buf.toString();
		if (sXML.indexOf(XMLProducer.DO_NOT_EDIT_WARNING) > 0) {
			sXML = sXML.replaceAll("<!--\\s*" + XMLProducer.DO_NOT_EDIT_WARNING, "");
			int i = sXML.lastIndexOf("-->");
			sXML = sXML.substring(0, i) + sXML.substring(i + 3);
		}
		return sXML;
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
		determinePartitions();
		//String sXML = new XMLProducer().toXML(m_mcmc.get(), PluginPanel.g_plugins.values());
		String sXML = new XMLProducer().toXML(m_mcmc.get(), new HashSet<Plugin>());
		FileWriter outfile = new FileWriter(sFileName);
		outfile.write(sXML);
		outfile.close();
	} // save

	void extractSequences(String sXML) throws  Exception {
		// load standard template
		String sTemplateXML = processTemplate(STANDARD_TEMPLATE);
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
			sXML = processTemplate(STANDARD_TEMPLATE);
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
			
			System.err.println("PARTITIONS:\n");
			System.err.println(Arrays.toString(m_nCurrentPartitions));
			
			
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
		determinePartitions();
	}

	void determinePartitions() {
		CompoundDistribution likelihood = (CompoundDistribution) PluginPanel.g_plugins.get("likelihood");
		m_sPartitionNames = new ArrayList<String>();
		for (Distribution distr : likelihood.pDistributions.get()) {
			if (distr instanceof TreeLikelihood) {
				TreeLikelihood treeLikelihoods = (TreeLikelihood) distr;
				m_alignments.add(treeLikelihoods.m_data.get());
				m_sPartitionNames.add(treeLikelihoods.m_data.get().getID());
			}
		}
		
		m_alignments.clear();
		for (int i = 0; i < 3; i++) {
			m_pPartitionByAlignments[i].clear();
			m_pPartition[i].clear();
			m_nCurrentPartitions[i].clear();
		}
		List<TreeLikelihood> treeLikelihoods = new ArrayList<TreeLikelihood>();
		for (Distribution distr : likelihood.pDistributions.get()) {
			if (distr instanceof TreeLikelihood) {
				TreeLikelihood treeLikelihood = (TreeLikelihood) distr;
				m_alignments.add(treeLikelihood.m_data.get());
				treeLikelihoods.add(treeLikelihood);
			}
		}
		for (Distribution distr : likelihood.pDistributions.get()) {
			if (distr instanceof TreeLikelihood) {
				TreeLikelihood treeLikelihood = (TreeLikelihood) distr;
				try {
					// sync SiteModel, ClockModel and Tree to any changes that may have occurred
					// this should only affect the clock model in practice
					int nPartition = getPartitionNr(treeLikelihood.m_pSiteModel.get());
					TreeLikelihood treeLikelihood2 = treeLikelihoods.get(nPartition);
					treeLikelihood.m_pSiteModel.setValue(treeLikelihood2.m_pSiteModel.get(), treeLikelihood);
					m_nCurrentPartitions[0].add(nPartition);

					nPartition = getPartitionNr(treeLikelihood.m_pBranchRateModel.get());
					treeLikelihood2 = treeLikelihoods.get(nPartition);
					treeLikelihood.m_pBranchRateModel.setValue(treeLikelihood2.m_pBranchRateModel.get(), treeLikelihood);
					m_nCurrentPartitions[1].add(nPartition);

					nPartition = getPartitionNr(treeLikelihood.m_tree.get());
					treeLikelihood2 = treeLikelihoods.get(nPartition);
					treeLikelihood.m_tree.setValue(treeLikelihood2.m_tree.get(), treeLikelihood);
					m_nCurrentPartitions[2].add(nPartition);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
//				m_pPartitionByAlignments[0].add(treeLikelihood.m_pSiteModel.get());
//				m_pPartitionByAlignments[1].add(treeLikelihood.m_pBranchRateModel.get());
//				m_pPartitionByAlignments[2].add(treeLikelihood.m_tree.get());
				m_pPartitionByAlignments[0].add(treeLikelihood);
				m_pPartitionByAlignments[1].add(treeLikelihood);
				m_pPartitionByAlignments[2].add(treeLikelihood);
			}
		}
		
		int nPartitions = m_sPartitionNames.size();
		for (int i = 0; i < 3; i++) {
			boolean [] bUsedPartition = new boolean[nPartitions];
			for (int j = 0; j < nPartitions; j++) {
				int iPartition = m_nCurrentPartitions[i].get(j);//getPartitionNr(m_pPartitionByAlignments[i].get(j));
				bUsedPartition[iPartition] = true;
			}
			for (int j = 0; j < nPartitions; j++) {
				if (bUsedPartition[j]) {
					m_pPartition[i].add(m_pPartitionByAlignments[i].get(j));
				}
			}			
		}
	}

	int getPartitionNr(String sPartition) {
		int nPartition = m_sPartitionNames.indexOf(sPartition);
		return nPartition;
	}
	
	int getPartitionNr(Plugin plugin) {
		String sPartition = plugin.getID();
		sPartition = sPartition.substring(sPartition.lastIndexOf('.')+1);
		int nPartition = m_sPartitionNames.indexOf(sPartition);
		return nPartition;
	}

	public List<Plugin> getPartitions(String sType) {
		if (sType == null) {
			return m_pPartition[2];
		}
		if (sType.contains("SiteModel")) {
			return m_pPartition[0];
		}
		if (sType.contains("ClockModel")) {
			return m_pPartition[1];
		}
		return m_pPartition[2];
	}

	public void setCurrentPartition(int iCol, int iRow, String sPartition) {
		int nCurrentPartion = m_sPartitionNames.indexOf(sPartition);
		m_nCurrentPartitions[iCol].set(iRow, nCurrentPartion);
	}


} // class BeautiDoc
