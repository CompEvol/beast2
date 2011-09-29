package beast.app.beauti;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import beast.core.Logger.LOGMODE;
import beast.core.MCMC;
import beast.core.Operator;
import beast.core.Plugin;
import beast.core.State;
import beast.core.StateNode;
import beast.core.StateNodeInitialiser;
import beast.core.parameter.RealParameter;
import beast.core.util.CompoundDistribution;
import beast.evolution.alignment.Alignment;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.operators.TipDatesScaler;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeDistribution;
import beast.evolution.tree.coalescent.TreeIntervals;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.Prior;
import beast.util.NexusParser;
import beast.util.XMLParser;
import beast.util.XMLProducer;
import beast.util.XMLParser.RequiredInputProvider;

@Description("Beauti document in doc-view pattern, not useful in models")
public class BeautiDoc extends Plugin implements RequiredInputProvider {
	final static String STANDARD_TEMPLATE = "templates/Standard.xml";

	public enum ActionOnExit {
		UNKNOWN, SHOW_DETAILS_USE_TEMPLATE, SHOW_DETAILS_USE_XML_SPEC, WRITE_XML
	}

	// public Input<List<Alignment>> m_alignments = new
	// Input<List<Alignment>>("alignment", "list of alignments or partitions",
	// new ArrayList<Alignment>(), Validate.REQUIRED);
	public List<Alignment> alignments = new ArrayList<Alignment>();

	public Input<beast.core.Runnable> mcmc = new Input<beast.core.Runnable>("runnable", "main entry of analysis",
			Validate.REQUIRED);

	/** points to input that contains prior distribution, if any **/
	Input<List<Distribution>> priors;
	/** contains all Priors from the template **/
	protected List<Distribution> potentialPriors;
	List<StateNodeInitialiser> potentitalInits;
	List<Logger> potentitalLoggers;

	protected List<BranchRateModel> clockModels;
	/** contains all loggers from the template **/
	List<List<Plugin>> loggerInputs;

	boolean bAutoScrubOperators = true;
	boolean bAutoScrubLoggers = true;
	boolean bAutoScrubPriors = true;
	boolean bAutoScrubState = true;

	/** [0] = sitemodel [1] = clock model [2] = tree **/
	List<Plugin>[] pPartitionByAlignments;
	List<Plugin>[] pPartition;
	private List<Integer>[] nCurrentPartitions;
	// partition names
	List<String> sPartitionNames = new ArrayList<String>();

	// RRB: hack to pass info to BeautiSubTemplate TODO: beautify this, since it
	// prevents haveing multiple windows open
	// All globals need to be removed to make multiple document view work on
	// Macs
	static BeautiDoc g_doc;
	Beauti beauti;

	String sTemplateName = null;
	String m_sTemplateFileName = STANDARD_TEMPLATE;
	String sFileName = null;

	public BeautiDoc() {
		g_doc = this;
		setID("BeautiDoc");
		clear();
	}

	public ActionOnExit parseArgs(String[] args) throws Exception {
		ActionOnExit m_endState = ActionOnExit.UNKNOWN;
		String m_sOutputFileName = "beast.xml";
		String m_sXML = null;
		String m_sTemplateXML = null;

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
					this.sFileName = nameFromFile(sFileName);
					i += 2;
				} else if (args[i].equals("-template")) {
					String sFileName = args[i + 1];
					m_sTemplateXML = processTemplate(sFileName);
					m_sTemplateFileName = sFileName;
					sTemplateName = nameFromFile(sFileName);
					i += 2;
				} else if (args[i].equals("-nex")) {
					// NB: multiple -nex/-xmldata commands can be processed!
					String sFileName = args[i + 1];
					NexusParser parser = new NexusParser();
					parser.parseFile(sFileName);
					if (parser.m_filteredAlignments.size() > 0) {
						for (Alignment data : parser.m_filteredAlignments) {
							alignments.add(data);
						}
					} else {
						alignments.add(parser.m_alignment);
					}
					i += 2;
				} else if (args[i].equals("-xmldata")) {
					// NB: multiple -xmldata/-nex commands can be processed!
					String sFileName = args[i + 1];
					Alignment alignment = (Alignment) AlignmentListInputEditor.getXMLData(sFileName);
					alignments.add(alignment);
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
		this.beauti = beauti;
	}

	@SuppressWarnings("unchecked")
	void clear() {
		potentialPriors = new ArrayList<Distribution>();
		potentitalInits = new ArrayList<StateNodeInitialiser>();
		potentitalLoggers = new ArrayList<Logger>();
		clockModels = new ArrayList<BranchRateModel>();
		alignments = new ArrayList<Alignment>();

		pPartitionByAlignments = new List[3];
		pPartition = new List[3];
		nCurrentPartitions = new List[3];
		sPartitionNames = new ArrayList<String>();
		for (int i = 0; i < 3; i++) {
			pPartitionByAlignments[i] = new ArrayList<Plugin>();
			pPartition[i] = new ArrayList<Plugin>();
			nCurrentPartitions[i] = new ArrayList<Integer>();
		}
	}

	/** remove all alignment data and model, and reload Standard template **/
	public void newAnalysis() {
		try {
			clear();
			PluginPanel.init();
			BeautiConfig.clear();
			String sXML = processTemplate(m_sTemplateFileName);
			loadTemplate(sXML);
			beauti.setUpPanels();
			beauti.setUpViewMenu();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadXML(String sFileName) throws Exception {
		String sXML = load(sFileName);
		extractSequences(sXML);
		connectModel();
		beauti.setUpPanels();
	}

	public void loadNewTemplate(String sFileName) throws Exception {
		m_sTemplateFileName = sFileName;
		newAnalysis();
//		BeautiConfig.clear();
//		PluginPanel.init();
//		String sXML = processTemplate(sFileName);
//		extractSequences(sXML);
//		connectModel();
//		beauti.setUpPanels();
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
		beauti.setUpPanels();
	}

	public void importXMLAlignment(String sFileName) throws Exception {
		Alignment data = (Alignment) AlignmentListInputEditor.getXMLData(sFileName);
		addAlignmentWithSubnet(data);
		connectModel();
		beauti.setUpPanels();
	}

	void initialize(ActionOnExit endState, String sXML, String sTemplate, String sFileName) throws Exception {
		BeautiConfig.clear();
		switch (endState) {
		case UNKNOWN:
		case SHOW_DETAILS_USE_TEMPLATE: {
			mergeSequences(sTemplate);
			scrubAll(true);
			connectModel();
			break;
		}
		case SHOW_DETAILS_USE_XML_SPEC: {
			if (sTemplate == null) {
				sTemplate = processTemplate(STANDARD_TEMPLATE);
			}
			loadTemplate(sTemplate);
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
			// // load standard template
			// String sTemplateXML =
			// BeautiInitDlg.processTemplate(STANDARD_TEMPLATE);
			// loadTemplate(sTemplateXML);
			// connectModel();
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
				if (!template.getAbsolutePath().equals(mainTemplate.getAbsolutePath())
						&& template.getName().toLowerCase().endsWith(".xml")) {
					String sXML2 = load(template.getAbsolutePath());
					if (!sXML2.contains("<mergepoint ")) {
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
		sTemplateName = nameFromFile(sFileName);
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

	static public String load(String sFileName) throws IOException {
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
		for (Alignment data : alignments) {
			if (data.getID().equals(sPartition)) {
				return data;
			}
		}
		return null;
	}

	/**
	 * see whether we have a valid model that can be saved at this point in time
	 **/
	public boolean validateModel() {
		if (mcmc == null) {
			return false;
		}
		return true;
	} // validateModel

	/** save specification in file **/
	public void save(String sFileName) throws Exception {
		scrubAll(false);
		determinePartitions();
		// String sXML = new XMLProducer().toXML(m_mcmc.get(),
		// PluginPanel.g_plugins.values());
		String sXML = new XMLProducer().toXML(mcmc.get(), new HashSet<Plugin>());
		FileWriter outfile = new FileWriter(sFileName);
		outfile.write(sXML);
		outfile.close();
	} // save

	void extractSequences(String sXML) throws Exception {
		// load standard template
		if (m_beautiConfig == null) {
			String sTemplateXML = processTemplate(STANDARD_TEMPLATE);
			loadTemplate(sTemplateXML);
		}
		// parse file
		XMLParser parser = new XMLParser();
		Plugin MCMC = parser.parseFragment(sXML, true);
		mcmc.setValue(MCMC, this);
		PluginPanel.addPluginToMap(MCMC);
		// extract alignments
		determinePartitions();
	}

	BeautiConfig m_beautiConfig;

	/**
	 * Merge sequence data with sXML specification.
	 **/
	void mergeSequences(String sXML) throws Exception {
		if (sXML == null) {
			sXML = processTemplate(STANDARD_TEMPLATE);
		}
		loadTemplate(sXML);
		// create XML for alignments
		for (Alignment alignment : alignments) {
			m_beautiConfig.partitionTemplate.get().createSubNet(alignment, this);
		}
		determinePartitions();

		// // create XML for alignments
		// String sAlignments = "";
		// int n = 1;
		// String sRange="range='";
		// List<String> sAlignmentNames = new ArrayList<String>();
		// for (Alignment alignment : m_alignments.get()) {
		// sAlignmentNames.add(alignment.getID());
		// alignment.setID(/*"alignment" + */alignment.getID());
		// sRange += alignment.getID() +",";
		// n++;
		// }
		//
		// for (Alignment alignment : m_alignments.get()) {
		// if (!(alignment instanceof FilteredAlignment)) {
		// sAlignments += new XMLProducer().toRawXML(alignment);
		// }
		// }
		// List<String> sDone = new ArrayList<String>();
		// for (Alignment alignment : m_alignments.get()) {
		// if (alignment instanceof FilteredAlignment) {
		// FilteredAlignment data = (FilteredAlignment) alignment;
		// Alignment baseData = data.m_alignmentInput.get();
		// String sBaseID = baseData.getID();
		// if (sDone.indexOf(sBaseID) < 0) {
		// sAlignments += new XMLProducer().toRawXML(baseData);
		// sDone.add(sBaseID);
		// }
		// // suppress alinmentInput
		// data.m_alignmentInput.setValue(null, data);
		// String sData = new XMLProducer().toRawXML(data);
		// // restore alinmentInput
		// data.m_alignmentInput.setValue(baseData, data);
		// sData = sData.replaceFirst("<data ", "<data data='@" + sBaseID
		// +"' ");
		// sAlignments += sData;
		// }
		// }
		//
		// sRange = sRange.substring(0, sRange.length()-1)+"'";
		//
		// // process plates in template
		// int i = -1;
		// do {
		// i = sXML.indexOf("<plate", i);
		// if (i >= 0) {
		// int j = sXML.indexOf("range='#alignments'");
		// int j2 = sXML.indexOf("range=\"#alignments\"");
		// if (j < 0 || (j2>=0 && j2 < j)) {
		// j = j2;
		// }
		// // sanity check: no close of elements encountered underway...
		// for (int k = i; k < j; k++) {
		// if (sXML.charAt(k) == '>') {
		// j = -1;
		// }
		// }
		// if ( j >= 0) {
		// sXML = sXML.substring(0, j) + sRange + sXML.substring(j+19);
		// }
		// i++;
		// }
		// } while (i >= 0);
		//
		//
		// if (sAlignmentNames.size() == 1) {
		// // process plates in template
		// i = -1;
		// do {
		// i = sXML.indexOf("$(n).", i);
		// if ( i >= 0) {
		// sXML = sXML.substring(0, i) + sXML.substring(i+5);
		// }
		// } while (i >= 0);
		// }
		//
		// // merge in the alignments
		// i = sXML.indexOf("<data");
		// int j = sXML.indexOf("/>",i);
		// int k = sXML.indexOf("</data>",i);
		// if (j < 0 || (k > 0 && k < j)) {
		// j = k + 7;
		// }
		// sXML = sXML.substring(0, i) + sAlignments + sXML.substring(j);
		//
		// // parse the resulting XML
		// FileWriter outfile = new FileWriter("beast.xml");
		// outfile.write(sXML);
		// outfile.close();
		//
		// XMLParser parser = new XMLParser();
		// List<Plugin> plugins = parser.parseTemplate(sXML, new HashMap<String,
		// Plugin>());
		// for (Plugin plugin: plugins) {
		// PluginPanel.addPluginToMap(plugin);
		// if (plugin instanceof beast.core.Runnable) {
		// m_mcmc.setValue(plugin, this);
		// }
		// }
		// return sAlignmentNames;
	} // mergeSequences

	void loadTemplate(String sXML) throws Exception {
		// load the template and its beauti configuration parts
		XMLParser parser = new XMLParser();
		PluginPanel.init();
		List<Plugin> plugins = parser.parseTemplate(sXML, new HashMap<String, Plugin>(), true);
		for (Plugin plugin : plugins) {
			if (plugin instanceof beast.core.Runnable) {
				mcmc.setValue(plugin, this);
			} else if (plugin instanceof BeautiConfig) {
				m_beautiConfig = (BeautiConfig) plugin;
			} else {
				System.err.println("template item " + plugin.getID() + " is ignored");
			}
			PluginPanel.addPluginToMap(plugin);
		}
	}

	/**
	 * Connect all inputs to the relevant ancestors of m_runnable.
	 * 
	 * @throws Exception
	 * **/
	void connectModel() throws Exception {
		try {
			// MCMC mcmc = (MCMC) m_mcmc.get();
			// build global list of loggers
			loggerInputs = new ArrayList<List<Plugin>>();
			for (Logger logger : ((MCMC) mcmc.get()).m_loggers.get()) {
				List<Plugin> loggers = new ArrayList<Plugin>();
				for (Plugin plugin : logger.m_pLoggers.get()) {
					loggers.add(plugin);
				}
				loggerInputs.add(loggers);
			}

			// collect priors from template
			CompoundDistribution prior = (CompoundDistribution) PluginPanel.g_plugins.get("prior");
			this.priors = prior.pDistributions;
			List<Distribution> list = this.priors.get();
			for (Distribution prior2 : list) {
				if (!(prior2 instanceof TreeDistribution)) {
					potentialPriors.add(prior2);
				}
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
			BranchRateModel.Base clockModel = ((TreeLikelihood) d).m_pBranchRateModel.get();
			if (clockModel != null) {
				String sID = clockModel.getID();
				sID = sID.substring(sID.indexOf('.') + 1);
				String sPartition = alignments.get(k).getID();
				if (sID.equals(sPartition)) {
					if (clockModels.size() <= k) {
						clockModels.add(clockModel);
					} else {
						clockModels.set(k, clockModel);
					}
				}
				k++;
			}
		}
	}

	BranchRateModel getClockModel(String sPartition) {
		int k = 0;
		for (Alignment data : alignments) {
			if (data.getID().equals(sPartition)) {
				return clockModels.get(k);
			}
			k++;
		}
		return null;
	}

	void scrubAll(boolean bUseNotEstimatedStateNodes) {
		try {
			setClockRate();
			
			// go through all templates, and process connectors in relevant ones 
//			List<Plugin> mcmcPredecessors = new ArrayList<Plugin>();
//			collectPredecessors(((MCMC) mcmc.get()), mcmcPredecessors);
			
//			List<Plugin> getPosteriorPredecessors() {
				List<Plugin> posteriorPredecessors = new ArrayList<Plugin>();
				collectPredecessors(((MCMC) mcmc.get()).posteriorInput.get(), posteriorPredecessors);
//				return posteriorPredecessors;
//			}
			
//			List<Plugin> posteriorPredecessors = getPosteriorPredecessors();
			List<BeautiSubTemplate> templates = new ArrayList<BeautiSubTemplate>();
			templates.add(m_beautiConfig.partitionTemplate.get());
			templates.addAll(BeautiConfig.g_subTemplates);
			
			for (String sPartition : sPartitionNames) {
				for (BeautiSubTemplate template : templates) {
					String sTemplateID = template.getMainID().replaceAll("\\$\\(n\\)", sPartition);
					Plugin plugin = PluginPanel.g_plugins.get(sTemplateID);

					// check if template is in use
					if (plugin != null) {
						
						// if so, run through all connectors
						for (BeautiConnector connector : template.connectors) {
							if (connector.isActivated(sPartition, posteriorPredecessors)) {
								System.err.println("connect: " + connector);
								connect(connector, sPartition);
							} else {
								disconnect(connector, sPartition);
								System.err.println("DISconnect: " + connector);
							}
						}
					}
				}
			}
			
			
//			if (bAutoScrubState) {
//				scrubState(bUseNotEstimatedStateNodes);
//			}
//			if (bAutoScrubPriors) {
//				scrubPriors();
//			}
//			if (bAutoScrubOperators) {
//				scrubOperators();
//			}
//			if (bAutoScrubLoggers) {
//				scrubLoggers();
//			}
//			scrubInits();
			collectClockModels();

			System.err.println("PARTITIONS:\n");
			System.err.println(Arrays.toString(nCurrentPartitions));

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	} // scrubAll

	void setClockRate() throws Exception {
		Plugin likelihood = PluginPanel.g_plugins.get("likelihood");
		if (likelihood instanceof CompoundDistribution) {
			int i = 0;
			for (Distribution distr : ((CompoundDistribution)likelihood).pDistributions.get()) {
				if (distr instanceof TreeLikelihood) {
					TreeLikelihood treeLikelihood = (TreeLikelihood) distr;
					boolean bNeedsEstimation = false;
					if (i > 0) {
						bNeedsEstimation = true;
					} else {
						Tree tree = treeLikelihood.m_tree.get();
						// check whether there are tip dates
						TraitSet trait = tree.m_trait.get();
						if (trait != null) {
							bNeedsEstimation = true;
						}
						// check whether there is a calibration
						for (Plugin plugin : tree.outputs) {
							if (plugin instanceof MRCAPrior) {
								MRCAPrior prior = (MRCAPrior) plugin;
								if (prior.m_distInput.get() != null) {
									bNeedsEstimation = true;
								}
							}
						}
					}
					BranchRateModel.Base model = (BranchRateModel.Base) treeLikelihood.m_pBranchRateModel.get();
					if (model != null) {
						RealParameter clockRate = model.meanRateInput.get(); 
						clockRate.m_bIsEstimated.setValue(bNeedsEstimation, clockRate);
					}
					i++;
				}
			}
		}
	}
	
//	/** remove operators on StateNodesthat have no impact on the posterior **/
//	void scrubOperators() {
//		List<Plugin> posteriorPredecessors = new ArrayList<Plugin>();
//		collectPredecessors(((MCMC) mcmc.get()).posteriorInput.get(), posteriorPredecessors);
//		// clear operatorsInput & add to global list of operators if not already
//		// there
//		List<Operator> operators0 = ((MCMC) mcmc.get()).operatorsInput.get();
//		operators0.clear();
//
//		List<StateNode> stateNodes = ((MCMC) mcmc.get()).m_startState.get().stateNodeInput.get();
//
//		// add operators that have predecessors in posteriorPredecessors
//		for (Operator operator : PluginPanel.g_operators) {
//			List<Plugin> operatorPredecessors = new ArrayList<Plugin>();
//			collectPredecessors(operator, operatorPredecessors);
//			for (Plugin plugin : operatorPredecessors) {
//				if (posteriorPredecessors.contains(plugin)) {
//					// test at least one of the inputs is a StateNode that needs
//					// to be estimated
//					try {
//						for (Plugin plugin2 : operator.listActivePlugins()) {
//							if (plugin2 instanceof StateNode) {
//								if (((StateNode) plugin2).m_bIsEstimated.get() && stateNodes.contains(plugin2)) {
//									operators0.add(operator);
//									break;
//								}
//							}
//						}
//					} catch (Exception e) {
//						// TODO: handle exception
//					}
//					break;
//				}
//			}
//		}
//		// sort potential priors by ID
//		final Map<String, String> map = new HashMap<String, String>();
//		for (Operator operator : operators0) {
//			String sStateNodes = "";
//	        try {
//	        	for (Plugin plugin2 : operator.listActivePlugins()) {
//	        		if (plugin2 instanceof StateNode && ((StateNode) plugin2).m_bIsEstimated.get()) {
//	        			sStateNodes += plugin2.getID() + " "; 
//	        		}
//	        	}
//	        } catch (Exception e) {
//				// ignore
//			}
//	        map.put(operator.getID(), sStateNodes+operator.getID());
//		}
//		
//		
//		Collections.sort(operators0, new Comparator<Operator>() {
//			@Override
//			public int compare(Operator o1, Operator o2) {
//				return map.get(o1.getID()).compareTo(map.get(o2.getID()));
//			}
//		});
//
//	}
//
//
//	
//	/** remove loggers of StateNodes that have no impact on the posterior **/
//	void scrubLoggers() {
////		List<Plugin> posteriorPredecessors = new ArrayList<Plugin>();
////		collectPredecessors(((MCMC) mcmc.get()).posteriorInput.get(), posteriorPredecessors);
//
//		List<StateNode> stateNodes = ((State) PluginPanel.g_plugins.get("state")).stateNodeInput.get();
//		List<Logger> loggers = ((MCMC) mcmc.get()).m_loggers.get();
//		for (int k = 0; k < loggers.size(); k++) {
//			Logger logger = loggers.get(k);
//			if (!logger.m_sMode.get().equals(LOGMODE.tree)) {
//				List<Plugin> loggerInput = loggerInputs.get(k);
//				// clear logger & add to global list of loggers if not already there
//				List<Plugin> loggers0 = logger.m_pLoggers.get();
//				for (int i = loggers0.size() - 1; i >= 0; i--) {
//					Plugin o = loggers0.remove(i);
//					if (!loggerInput.contains(o)) {
//						loggerInput.add(o);
//					}
//				}
////			// add loggers that have predecessors in posteriorPredecessors
////			for (Plugin newlogger : loggerInput) {
////				List<Plugin> loggerPredecessors = new ArrayList<Plugin>();
////				collectPredecessors(newlogger, loggerPredecessors);
////				for (Plugin plugin : loggerPredecessors) {
////					if (posteriorPredecessors.contains(plugin)) {
////						loggers0.add(newlogger);
////						break;
////					}
////				}
////			}
//
//				// add loggers that have all StateNode predecessors in the State
//	            HashSet<StateNode> operatorStateNodes = new HashSet<StateNode>();
//	            try {
//		            for (Operator op : ((MCMC)mcmc.get()).operatorsInput.get()) {
//		            	for (Plugin o : op.listActivePlugins()) {
//		            		if (o instanceof StateNode) {
//		            			operatorStateNodes.add((StateNode) o);
//		            		}
//		            	}
//		            }
//	            } catch (Exception e) {
//					// TODO: handle exception
//				}
//	            
//	            for (Plugin newlogger : loggerInput) {
//					if (newlogger instanceof StateNode) {
//						if (stateNodes.contains(newlogger) && operatorStateNodes.contains(newlogger)) {
//							// check there is an operator that operates on this StateNode
//							loggers0.add(newlogger);
//						}
//					} else {
//						List<Plugin> loggerPredecessors = new ArrayList<Plugin>();
//						collectPredecessors(newlogger, loggerPredecessors);
//						boolean bMatch = false;
//						for (Plugin plugin : loggerPredecessors) {
//							if (plugin instanceof StateNode) { // && ! stateNodes.contains(plugin)) {
//								bMatch = true;
//								break;
//							}
//						}
//						if (bMatch) {
//							loggers0.add(newlogger);
//						}
//					}
//				}
//			}		
//		}
//
//		// find obsolete tree loggers
//		for (int i = loggers.size() - 1; i >= 0; i--) {
//			Logger logger = loggers.get(i);
//			if (logger.m_sMode.get().equals(LOGMODE.tree)) {
//				Object tree = logger.m_pLoggers.get().get(0);
//				if (tree instanceof StateNode) {
//					if (!stateNodes.contains(tree)) {
//						loggers.remove(i);
//						potentitalLoggers.add(logger);
//					}
//				}
//			}
//		}
//		// check whether any potential logger is needed
//		for (int i = potentitalLoggers.size() - 1; i >= 0; i--) {
//			Logger logger = potentitalLoggers.get(i);
//			if (logger.m_sMode.get().equals(LOGMODE.tree)) {
//				Object tree = logger.m_pLoggers.get().get(0);
//				if (stateNodes.contains(tree)) {
//					loggers.add(logger);
//					potentitalLoggers.remove(i);
//				}
//			}
//		}
//
//	}

//	private void scrubInits() {
//		List<StateNodeInitialiser> inits = ((MCMC) mcmc.get()).m_initilisers.get();
//		List<StateNode> stateNodes = ((State) PluginPanel.g_plugins.get("state")).stateNodeInput.get();
//		// check whether all initialisers are still needed
//		for (int i = inits.size() - 1; i >= 0; i--) {
//			List<Plugin> initPredecessors = new ArrayList<Plugin>();
//			collectPredecessors((Plugin) inits.get(i), initPredecessors);
//			boolean bFound = false;
//			for (Plugin plugin : initPredecessors) {
//				if (stateNodes.contains(plugin)) {
//					bFound = true;
//					break;
//				}
//			}
//			if (!bFound) {
//				StateNodeInitialiser init = inits.get(i);
//				inits.remove(i);
//				potentitalInits.add(init);
//			}
//		}
//		// check whether any potential initialiser is needed
//		for (int i = potentitalInits.size() - 1; i >= 0; i--) {
//			List<Plugin> initPredecessors = new ArrayList<Plugin>();
//			collectPredecessors((Plugin) potentitalInits.get(i), initPredecessors);
//			for (Plugin plugin : initPredecessors) {
//				if (stateNodes.contains(plugin)) {
//					StateNodeInitialiser init = potentitalInits.get(i);
//					inits.add(init);
//					potentitalInits.remove(i);
//					break;
//				}
//			}
//		}
//
//	}

//	/**
//	 * remove StateNodes that are not estimated or have no impact on the
//	 * posterior
//	 **/
//	void scrubState(boolean bUseNotEstimatedStateNodes) {
//		bUseNotEstimatedStateNodes = false;
//		try {
//
//			State state = ((MCMC) mcmc.get()).m_startState.get();
//			List<StateNode> stateNodes = state.stateNodeInput.get();
//			stateNodes.clear();
//
//			// grab all statenodes that impact the posterior
//			List<Plugin> posteriorPredecessors = new ArrayList<Plugin>();
//			Plugin posterior = ((MCMC) mcmc.get()).posteriorInput.get();
//			collectPredecessors(posterior, posteriorPredecessors);
//			for (Plugin plugin : posteriorPredecessors) {
//				if ((plugin instanceof StateNode)
//						&& (((StateNode) plugin).m_bIsEstimated.get() || bUseNotEstimatedStateNodes)) {
//					if (!stateNodes.contains(plugin)) {
//						stateNodes.add((StateNode) plugin);
//					}
//					// System.err.println(stateNode.getID());
//				}
//			}
//			for (int i = stateNodes.size() - 1; i >= 0; i--) {
//				Plugin stateNode = stateNodes.get(i);
//				List<Plugin> ancestors = new ArrayList<Plugin>();
//				collectNonTrivialAncestors(stateNode, ancestors);
//				if (!ancestors.contains(posterior)) {
//					stateNodes.remove(i);
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	/**
//	 * collect priors that have predecessors in the State, i.e. a StateNode that
//	 * is estimated
//	 **/
//	void scrubPriors() {
//		if (this.priors == null) {
//			return;
//		}
//		try {
//			List<Plugin> likelihoodPredecessors = new ArrayList<Plugin>();
//			Plugin likelihood = PluginPanel.g_plugins.get("likelihood");
//			collectPredecessors(likelihood, likelihoodPredecessors);
//
//			List<Distribution> priors = this.priors.get();
//			for (int i = priors.size() - 1; i >= 0; i--) {
//				Distribution prior = priors.get(i);
//				if (!potentialPriors.contains(prior) && !(prior instanceof TreeDistribution)) {
//					potentialPriors.add(prior);
//				}
//				if (prior instanceof MRCAPrior) {
//					if (((MRCAPrior) prior).m_bOnlyUseTipsInput.get()) {
//						priors.remove(i);
//					}
//				} else if (prior instanceof TreeDistribution) {
//					TreeDistribution distr = (TreeDistribution) prior;
//					Tree tree = distr.m_tree.get();
//					if (tree == null) {
//						TreeIntervals intervals = distr.treeIntervals.get();
//						tree = intervals.m_tree.get();
//					}
//					if (tree != null && !likelihoodPredecessors.contains(tree)) {
//						priors.remove(i);
//						if (!potentialPriors.contains(prior)) {
//							potentialPriors.add(prior);
//						}
//					}
//				} else if (!(prior instanceof TreeDistribution)) {
//					priors.remove(i);
//				}
//			}
//			// priors.clear();
//			List<Plugin> posteriorPredecessors = new ArrayList<Plugin>();
//			collectPredecessors(((MCMC) mcmc.get()).posteriorInput.get(), posteriorPredecessors);
//
//			List<StateNode> stateNodes = ((MCMC) mcmc.get()).m_startState.get().stateNodeInput.get();
//
//			for (int k = potentialPriors.size() - 1; k >= 0; k--) {
//				Distribution prior = potentialPriors.get(k);
//				List<Plugin> priorPredecessors = new ArrayList<Plugin>();
//				collectPredecessors(prior, priorPredecessors);
//				for (Plugin plugin : priorPredecessors) {
//					if (// posteriorPredecessors.contains(plugin) &&
//					plugin instanceof StateNode && ((StateNode) plugin).m_bIsEstimated.get()) {
//						if (prior instanceof MRCAPrior) {
//							if (((MRCAPrior) prior).m_bOnlyUseTipsInput.get()) {
//								// It is a tip dates prior. Check there is a tip
//								// dates operator.
//								for (Plugin plugin2 : ((MRCAPrior) prior).m_treeInput.get().outputs) {
//									if (plugin2 instanceof TipDatesScaler
//											&& ((TipDatesScaler) plugin2).m_pWeight.get() > 0) {
//										priors.add(prior);
//									}
//								}
//							}
//						} else if (prior instanceof TreeDistribution) {
//							TreeDistribution distr = (TreeDistribution) prior;
//							Tree tree = distr.m_tree.get();
//							if (tree == null) {
//								TreeIntervals intervals = distr.treeIntervals.get();
//								tree = intervals.m_tree.get();
//							}
//							if (tree != null && likelihoodPredecessors.contains(tree)) {
//								priors.add(prior);
//								potentialPriors.remove(prior);
//							}
//						} else if (!(prior instanceof Prior)) {
//							priors.add(prior);
//							potentialPriors.remove(prior);
//						} else if (!(prior instanceof MRCAPrior) && stateNodes.contains(plugin)) {
//							// priors.add(prior);
//						}
//						break;
//					}
//				}
//			}
//
//			// sort potential priors by ID
//			Collections.sort(potentialPriors, new Comparator<Distribution>() {
//				@Override
//				public int compare(Distribution o1, Distribution o2) {
//					return o1.getID().compareTo(o2.getID());
//				}
//			});
//
//			// add priors on parameters
//			List<Plugin> nonTrivialPosteriorPredecessors = new ArrayList<Plugin>();
//			collectNonTrivialPredecesors(((MCMC) mcmc.get()).posteriorInput.get(), nonTrivialPosteriorPredecessors);
//			
//			for (Distribution prior : potentialPriors) {
//				List<Plugin> priorPredecessors = new ArrayList<Plugin>();
//				collectPredecessors(prior, priorPredecessors);
//				for (Plugin plugin : priorPredecessors) {
//					if (plugin instanceof StateNode && ((StateNode) plugin).m_bIsEstimated.get()) {
//						if (prior instanceof MRCAPrior) {
//						} else if (prior instanceof TreeDistribution) {
//						} else if (stateNodes.contains(plugin) && nonTrivialPosteriorPredecessors.contains(plugin)) {
//							priors.add(prior);
//						}
//						break;
//					}
//				}
//			}
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//	}

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

//	/** collect all ancestors (outputs, their outputs, etc) that are not Priors **/
//	private void collectNonTrivialAncestors(Plugin plugin, List<Plugin> ancestors) {
//		if (ancestors.contains(plugin)) {
//			return;
//		}
//		ancestors.add(plugin);
//		int nSize = ancestors.size();
//		for (int i = 0; i < nSize; i++) {
//			Plugin plugin2 = ancestors.get(i);
//			for (Plugin output : plugin2.outputs) {
//				if (!(output instanceof Prior)) {
//					collectNonTrivialAncestors(output, ancestors);
//				}
//			}
//		}
//	}
//
//	private void collectNonTrivialPredecesors(Plugin plugin, List<Plugin> predecessors) {
//		predecessors.add(plugin);
//		try {
//			for (Plugin plugin2 : plugin.listActivePlugins()) {
//				if (!predecessors.contains(plugin2) && !(plugin2 instanceof Prior)) {
//					collectNonTrivialPredecesors(plugin2, predecessors);
//				}
//			}
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		}
//	}

	public void addPlugin(final Plugin plugin) throws Exception {
		// SwingUtilities.invokeLater(new Runnable() {
		// @Override
		// public void run() {
		//
		PluginPanel.addPluginToMap(plugin);
		try {
			for (Input<?> input : plugin.listInputs()) {
				if (input.get() != null) {
					if (input.get() instanceof Plugin) {
						PluginPanel.addPluginToMap((Plugin) input.get());
					}
					if (input.get() instanceof List<?>) {
						for (Object o : (List<?>) input.get()) {
							if (o instanceof Plugin) {
								PluginPanel.addPluginToMap((Plugin) o);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// ignore
			System.err.println(e.getClass().getName() + " " + e.getMessage());
		}

		// }});

		// m_sIDMap.put(plugin.getID(), plugin);
		// for (Plugin plugin2 : plugin.listActivePlugins()) {
		// addPlugin(plugin2);
		// }
	}

	/** connect source plugin with target plugin 
	 * @throws Exception **/
	public void connect(BeautiConnector connector, String sPartition) throws Exception {
		String sSrcID = connector.sSourceID.replaceAll("\\$\\(n\\)", sPartition);
		Plugin srcPlugin = PluginPanel.g_plugins.get(sSrcID);
		if (srcPlugin == null) {
			throw new Exception("Could not find plugin with id " + sSrcID + ". Typo in template perhaps?");
		}
		String sTargetID = connector.sTargetID.replaceAll("\\$\\(n\\)", sPartition);
//
//	}
//	
//	public void connect(Plugin srcPlugin, String sTargetID, String sInputName) throws Exception {
		try {
			Plugin target = PluginPanel.g_plugins.get(sTargetID);
			// prevent duplication inserts in list
			Object o = target.getInputValue(connector.sTargetInput);
			if (o instanceof List) {
				System.err.println("   " + ((List)o).size());
				if (((List<?>) o).contains(srcPlugin)) {
					System.err.println("   " + sTargetID + "/"  + connector.sTargetInput +  " already contains " + connector.sSourceID);
					return;
				}
			}
			
			target.setInputValue(connector.sTargetInput, srcPlugin);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** disconnect source plugin with target plugin **/
	public void disconnect(BeautiConnector connector, String sPartition) {
		Plugin srcPlugin = PluginPanel.g_plugins.get(connector.sSourceID.replaceAll("\\$\\(n\\)", sPartition));
		String sTargetID = connector.sTargetID.replaceAll("\\$\\(n\\)", sPartition);
//	public void disconnect(Plugin srcPlugin, String sTargetID, String sInputName) throws Exception {
		try {
			Plugin target = PluginPanel.g_plugins.get(sTargetID);
			Input<?> input = target.getInput(connector.sTargetInput);
			Object o = input.get();
			if (o instanceof List) {
				List<?> list = (List<?>) o;
				System.err.println("   " + ((List)o).size());
				for (int i = 0; i < list.size(); i++) {
					if (list.get(i) == srcPlugin) {
						System.err.println("  DEL "  + sTargetID + "/"  + connector.sTargetInput +  " already contains " + connector.sSourceID);
						list.remove(i);
					}
				}
				srcPlugin.outputs.remove(target);
			} else {
				input.setValue(null, target);
			}
			
//			potentialPriors.remove(srcPlugin);
//			potentitalInits.remove(srcPlugin);
//			if (srcPlugin instanceof Logger) {
//				potentitalLoggers.remove((Logger) srcPlugin);
//			}
//			PluginPanel.g_operators.remove(srcPlugin);
//			PluginPanel.g_distributions.remove(srcPlugin);
//			PluginPanel.g_loggers.remove(srcPlugin);
//			PluginPanel.g_taxa.remove(srcPlugin);
//			for (List<Plugin> logger : loggerInputs) {
//				logger.remove(srcPlugin);
//			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addAlignmentWithSubnet(Alignment data) {
		alignments.add(data);
		m_beautiConfig.partitionTemplate.get().createSubNet(data, this);
		// re-determine partitions
		determinePartitions();
	}

	void determinePartitions() {
		CompoundDistribution likelihood = (CompoundDistribution) PluginPanel.g_plugins.get("likelihood");
		sPartitionNames = new ArrayList<String>();
		for (Distribution distr : likelihood.pDistributions.get()) {
			if (distr instanceof TreeLikelihood) {
				TreeLikelihood treeLikelihoods = (TreeLikelihood) distr;
				alignments.add(treeLikelihoods.m_data.get());
				String sID = treeLikelihoods.m_data.get().getID();
				sID = sID.substring(sID.lastIndexOf(".")+1);
				sPartitionNames.add(sID);
			}
		}

		alignments.clear();
		for (int i = 0; i < 3; i++) {
			pPartitionByAlignments[i].clear();
			pPartition[i].clear();
			nCurrentPartitions[i].clear();
		}
		List<TreeLikelihood> treeLikelihoods = new ArrayList<TreeLikelihood>();
		for (Distribution distr : likelihood.pDistributions.get()) {
			if (distr instanceof TreeLikelihood) {
				TreeLikelihood treeLikelihood = (TreeLikelihood) distr;
				alignments.add(treeLikelihood.m_data.get());
				treeLikelihoods.add(treeLikelihood);
			}
		}
		for (Distribution distr : likelihood.pDistributions.get()) {
			if (distr instanceof TreeLikelihood) {
				TreeLikelihood treeLikelihood = (TreeLikelihood) distr;
				try {
					// sync SiteModel, ClockModel and Tree to any changes that
					// may have occurred
					// this should only affect the clock model in practice
					int nPartition = getPartitionNr(treeLikelihood.m_pSiteModel.get());
					TreeLikelihood treeLikelihood2 = treeLikelihoods.get(nPartition);
					treeLikelihood.m_pSiteModel.setValue(treeLikelihood2.m_pSiteModel.get(), treeLikelihood);
					nCurrentPartitions[0].add(nPartition);

					BranchRateModel rateModel = treeLikelihood.m_pBranchRateModel.get();
					if (rateModel != null) {
						nPartition = getPartitionNr((Plugin) rateModel);
						treeLikelihood2 = treeLikelihoods.get(nPartition);
						treeLikelihood.m_pBranchRateModel
								.setValue(treeLikelihood2.m_pBranchRateModel.get(), treeLikelihood);
						nCurrentPartitions[1].add(nPartition);
					} else {
						nCurrentPartitions[1].add(0);
					}
					
					nPartition = getPartitionNr(treeLikelihood.m_tree.get());
					treeLikelihood2 = treeLikelihoods.get(nPartition);
					treeLikelihood.m_tree.setValue(treeLikelihood2.m_tree.get(), treeLikelihood);
					nCurrentPartitions[2].add(nPartition);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// m_pPartitionByAlignments[0].add(treeLikelihood.m_pSiteModel.get());
				// m_pPartitionByAlignments[1].add(treeLikelihood.m_pBranchRateModel.get());
				// m_pPartitionByAlignments[2].add(treeLikelihood.m_tree.get());
				pPartitionByAlignments[0].add(treeLikelihood);
				pPartitionByAlignments[1].add(treeLikelihood);
				pPartitionByAlignments[2].add(treeLikelihood);
			}
		}

		int nPartitions = sPartitionNames.size();
		for (int i = 0; i < 3; i++) {
			boolean[] bUsedPartition = new boolean[nPartitions];
			for (int j = 0; j < nPartitions; j++) {
				int iPartition = nCurrentPartitions[i].get(j);// getPartitionNr(m_pPartitionByAlignments[i].get(j));
				bUsedPartition[iPartition] = true;
			}
			for (int j = 0; j < nPartitions; j++) {
				if (bUsedPartition[j]) {
					pPartition[i].add(pPartitionByAlignments[i].get(j));
				}
			}
		}

		System.err.println("PARTITIONS0:\n");
		System.err.println(Arrays.toString(nCurrentPartitions));
	}

	int getPartitionNr(String sPartition) {
		int nPartition = sPartitionNames.indexOf(sPartition);
		return nPartition;
	}

	int getPartitionNr(Plugin plugin) {
		String sPartition = plugin.getID();
		sPartition = sPartition.substring(sPartition.lastIndexOf('.') + 1);
		int nPartition = sPartitionNames.indexOf(sPartition);
		return nPartition;
	}

	public List<Plugin> getPartitions(String sType) {
		if (sType == null) {
			return pPartition[2];
		}
		if (sType.contains("SiteModel")) {
			return pPartition[0];
		}
		if (sType.contains("ClockModel")) {
			return pPartition[1];
		}
		return pPartition[2];
	}

	public void setCurrentPartition(int iCol, int iRow, String sPartition) {
		int nCurrentPartion = sPartitionNames.indexOf(sPartition);
		nCurrentPartitions[iCol].set(iRow, nCurrentPartion);
	}

	@Override
	public Object createInput(Plugin plugin, Input<?> input) {
		for (BeautiSubTemplate template : BeautiConfig.g_subTemplates) {
			try {
				if (input.canSetValue(template.instance, plugin)) {
					String sPartition = plugin.getID();
					sPartition = sPartition.substring(sPartition.indexOf('.') + 1);
					Object o = template.createSubNet(sPartition, plugin, input);
					return o;
				}
			} catch (Exception e) {
				// ignore, cannot set value
			}
		}
		// TODO Auto-generated method stub
		return null;
	}

} // class BeautiDoc
