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
import java.util.Set;

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
import beast.core.MCMC;
import beast.core.Plugin;
import beast.core.parameter.RealParameter;
import beast.core.util.CompoundDistribution;
import beast.evolution.alignment.Alignment;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.branchratemodel.StrictClockModel;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.speciation.YuleModel;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeDistribution;
import beast.math.distributions.MRCAPrior;
import beast.util.AddOnManager;
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
//	Input<List<Distribution>> priors;
	/** contains all Priors from the template **/
//	protected List<Distribution> potentialPriors;
//	List<StateNodeInitialiser> potentitalInits;
//	List<Logger> potentitalLoggers;

	protected List<BranchRateModel> clockModels;
	protected List<TreeDistribution> treePriors;
	/** contains all loggers from the template **/
//	List<List<Plugin>> loggerInputs;

//	boolean bAutoScrubOperators = true;
//	boolean bAutoScrubLoggers = true;
//	boolean bAutoScrubPriors = true;
//	boolean bAutoScrubState = true;

	public boolean bAutoSetClockRate = true;

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
	static public BeautiDoc g_doc;
	Beauti beauti;

	String sTemplateName = null;
	String m_sTemplateFileName = STANDARD_TEMPLATE;
	
	Map<String,String> tipTextMap = new HashMap<String, String>();

    /**
     * name of current file, used for saving (as opposed to saveAs) *
     */
	String sFileName = "";
	
	public BeautiDoc() {
		g_doc = this;
		setID("BeautiDoc");
		clear();
	}

	public ActionOnExit parseArgs(String[] args) throws Exception {
		ActionOnExit endState = ActionOnExit.UNKNOWN;
		String sOutputFileName = "beast.xml";
		String sXML = null;
		String sTemplateXML = null;
		TraitSet traitset = null;

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
					sXML = load(sFileName);
					// XMLParser parser = new XMLParser();
					// m_doc.m_mcmc.setValue(parser.parseFile(sFileName),
					// m_doc);
					this.sFileName = nameFromFile(sFileName);
					i += 2;
				} else if (args[i].equals("-template")) {
					String sFileName = args[i + 1];
					sTemplateXML = processTemplate(sFileName);
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
					traitset = parser.m_traitSet;
				} else if (args[i].equals("-xmldata")) {
					// NB: multiple -xmldata/-nex commands can be processed!
					String sFileName = args[i + 1];
					Alignment alignment = (Alignment) AlignmentListInputEditor.getXMLData(sFileName);
					alignments.add(alignment);
					i += 2;
				} else if (args[i].equals("-exitaction")) {
					if (args[i + 1].equals("writexml")) {
						endState = ActionOnExit.WRITE_XML;
					} else if (args[i + 1].equals("usetemplate")) {
						endState = ActionOnExit.SHOW_DETAILS_USE_TEMPLATE;
					} else if (args[i + 1].equals("usexml")) {
						endState = ActionOnExit.SHOW_DETAILS_USE_XML_SPEC;
					} else {
						throw new Exception("Expected one of 'writexml','usetemplate' or 'usexml', not " + args[i + 1]);
					}
					i += 2;
				} else if (args[i].equals("-out")) {
					sOutputFileName = args[i + 1];
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

		initialize(endState, sXML, sTemplateXML, sOutputFileName);
		addTraitSet(traitset);
		return endState;
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
//		potentialPriors = new ArrayList<Distribution>();
//		potentitalInits = new ArrayList<StateNodeInitialiser>();
//		potentitalLoggers = new ArrayList<Logger>();
		clockModels = new ArrayList<BranchRateModel>();
		treePriors = new ArrayList<TreeDistribution>();
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
		tipTextMap = new HashMap<String, String>();
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
			beauti.setTitle();
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
		addTraitSet(parser.m_traitSet);
		beauti.setUpPanels();
	}

	public void importXMLAlignment(String sFileName) throws Exception {
		Alignment data = (Alignment) AlignmentListInputEditor.getXMLData(sFileName);
		data.initAndValidate();
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
			//scrubAll(true, );
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

		for (String sDir : AddOnManager.getBeastDirectories()) {
			File templates = new File(sDir + "/templates");
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
		return buf.toString();
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
		if (mcmc == null || sPartitionNames.size() == 0) {
			return false;
		}
		return true;
	} // validateModel

	/** save specification in file **/
	public void save(String sFileName) throws Exception {
		determinePartitions();
		scrubAll(false, false);
		//String sXML = new XMLProducer().toXML(mcmc.get(), );
		Set<Plugin> plugins = new HashSet<Plugin>();
		for (Plugin plugin : PluginPanel.g_plugins.values()) {
			String sName = plugin.getClass().getName(); 
			if (!sName.startsWith("beast.app.beauti")) {
				plugins.add(plugin);
			}
		}
		String sXML = new XMLProducer().toXML(mcmc.get(), plugins);
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
		
		if (sXML.indexOf(XMLProducer.DO_NOT_EDIT_WARNING) > 0) {
			int iStart = sXML.indexOf(XMLProducer.DO_NOT_EDIT_WARNING);
			int iEnd = sXML.lastIndexOf("-->");
			sXML = sXML.substring(iStart, iEnd);
			sXML = sXML.replaceAll(XMLProducer.DO_NOT_EDIT_WARNING, "");
	    	sXML = "<beast namespace='" + XMLProducer.DEFAULT_NAMESPACE +"'>" + sXML + "</beast>";
			List<Plugin> plugins = parser.parseBareFragments(sXML, true);
			for (Plugin plugin : plugins) {
				PluginPanel.addPluginToMap(plugin);
			}
		}
		
		
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

	
	
	/** assigns trait to first available tree **/
	void addTraitSet(TraitSet trait) {
		if (trait != null) {
			CompoundDistribution likelihood = (CompoundDistribution) PluginPanel.g_plugins.get("likelihood");
			for (Distribution d : likelihood.pDistributions.get()) {
				if (d instanceof TreeLikelihood) {
					Tree tree = ((TreeLikelihood)d).m_tree.get();
					try {
						tree.m_trait.setValue(trait, tree);
					} catch (Exception e) {
						e.printStackTrace();
					}
					scrubAll(true, false);
					return;
				}
			}
		}
	}
	
	/**
	 * Connect all inputs to the relevant ancestors of m_runnable.
	 * 
	 * @throws Exception
	 * **/
	void connectModel() throws Exception {
//		try {
			// MCMC mcmc = (MCMC) m_mcmc.get();
			// build global list of loggers
//			loggerInputs = new ArrayList<List<Plugin>>();
//			for (Logger logger : ((MCMC) mcmc.get()).m_loggers.get()) {
//				List<Plugin> loggers = new ArrayList<Plugin>();
//				for (Plugin plugin : logger.m_pLoggers.get()) {
//					loggers.add(plugin);
//				}
//				loggerInputs.add(loggers);
//			}

			// collect priors from template
//			CompoundDistribution prior = (CompoundDistribution) PluginPanel.g_plugins.get("prior");
//			this.priors = prior.pDistributions;
//			List<Distribution> list = this.priors.get();
//			for (Distribution prior2 : list) {
//				if (!(prior2 instanceof TreeDistribution)) {
//					potentialPriors.add(prior2);
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		scrubAll(true, true);
//		collectClockModels();
//		collectTreePriors();
	}

	private void collectClockModels() {
		// collect branch rate models from model
		CompoundDistribution likelihood = (CompoundDistribution) PluginPanel.g_plugins.get("likelihood");
		while (clockModels.size() <  sPartitionNames.size()) {
			try {
				TreeLikelihood treelikelihood = new TreeLikelihood();
				treelikelihood.m_pBranchRateModel.setValue(new StrictClockModel(), treelikelihood);
	            List<BeautiSubTemplate> sAvailablePlugins = PluginPanel.getAvailableTemplates(treelikelihood.m_pBranchRateModel, treelikelihood, null);
				Plugin plugin = sAvailablePlugins.get(0).createSubNet(sPartitionNames.get(clockModels.size()));
				clockModels.add((BranchRateModel.Base) plugin);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		int k = 0;
		for (Distribution d : likelihood.pDistributions.get()) {
			BranchRateModel.Base clockModel = ((TreeLikelihood) d).m_pBranchRateModel.get();
			if (clockModel != null) {
				String sID = clockModel.getID();
				sID = sID.substring(sID.indexOf('.') + 1);
				String sPartition = alignments.get(k).getID();
				if (sID.equals(sPartition)) {
					clockModels.set(k, clockModel);
				}
				k++;
			}
		}
	}

	private void collectTreePriors() {
		// collect tree priors from model
		CompoundDistribution prior = (CompoundDistribution) PluginPanel.g_plugins.get("prior");
		while (treePriors.size() <  sPartitionNames.size()) {
			try {
				CompoundDistribution distr = new CompoundDistribution();
				distr.pDistributions.setValue(new YuleModel(), distr);
	            List<BeautiSubTemplate> sAvailablePlugins = PluginPanel.getAvailableTemplates(distr.pDistributions, distr, null);
	            for (int i = sAvailablePlugins.size()-1; i >= 0; i--) {
	            	if (!TreeDistribution.class.isAssignableFrom(sAvailablePlugins.get(i)._class)) {
						sAvailablePlugins.remove(i);
					}
	            }
	            if (sAvailablePlugins.size() > 0){
	            	Plugin plugin = sAvailablePlugins.get(0).createSubNet(sPartitionNames.get(treePriors.size()));
					treePriors.add((TreeDistribution) plugin);
	            } else {
					treePriors.add(null);
	            }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		int k = 0;
		for (Distribution d : prior.pDistributions.get()) {
			if (d instanceof TreeDistribution) {
				String sID = d.getID();
				sID = sID.substring(sID.indexOf('.') + 1);
				String sPartition = alignments.get(k).getID();
				if (sID.equals(sPartition)) {
					treePriors.set(k, (TreeDistribution) d);
				}
				k++;
			}
		}
//		for (TreeDistribution d : treePriors) {
//			System.out.println(d);
//		}
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

	TreeDistribution getTreePrior(String sPartition) {
		int k = 0;
		for (Alignment data : alignments) {
			if (data.getID().equals(sPartition)) {
				return treePriors.get(k);
			}
			k++;
		}
		return null;
	}

	void scrubAll(boolean bUseNotEstimatedStateNodes, boolean bInitial) {
		try {
			if (bAutoSetClockRate) {
				setClockRate();
			}

			// set estimate flag on tree, only if tree occurs in a partition
			for (String sPartition: sPartitionNames) {
				Tree tree = (Tree) PluginPanel.g_plugins.get("Tree." + sPartition);
				tree.m_bIsEstimated.setValue(false, tree);
			}
			for (Plugin plugin : pPartition[2]) {
				Tree tree = ((TreeLikelihood) plugin).m_tree.get();
				tree.m_bIsEstimated.setValue(true, tree);
			}
			
			
			// go through all templates, and process connectors in relevant ones 
//			List<Plugin> mcmcPredecessors = new ArrayList<Plugin>();
//			collectPredecessors(((MCMC) mcmc.get()), mcmcPredecessors);
			
//			List<Plugin> getPosteriorPredecessors() {
//				return posteriorPredecessors;
//			}
			
//			List<Plugin> posteriorPredecessors = getPosteriorPredecessors();


			boolean bProgress = true;
			while (bProgress) {
				bProgress = false;
				List<Plugin> posteriorPredecessors = new ArrayList<Plugin>();
				collectPredecessors(((MCMC) mcmc.get()).posteriorInput.get(), posteriorPredecessors);

				// process MRCA priors
				for (String sID : PluginPanel.g_plugins.keySet()) {
					if (sID.endsWith(".prior")) {
						Plugin plugin = PluginPanel.g_plugins.get(sID);
						if (plugin instanceof MRCAPrior) {
							MRCAPrior prior = (MRCAPrior) plugin;
							if (prior.m_treeInput.get().m_bIsEstimated.get() == false) {
								// disconnect
								disconnect(plugin, "prior", "distribution");
							} else {
								// connect
								connect(plugin, "prior", "distribution");
							}
						}
					}
				}
	
				
				List<BeautiSubTemplate> templates = new ArrayList<BeautiSubTemplate>();
				templates.add(m_beautiConfig.partitionTemplate.get());
				templates.addAll(BeautiConfig.g_subTemplates);
			
				List<String> sPartitionNames2 = new ArrayList<String>();
				// add 'Species' as special partition name
				sPartitionNames2.addAll(sPartitionNames);
				sPartitionNames2.add("Species");
				
				for (String sPartition : sPartitionNames2) {
					for (BeautiSubTemplate template : templates) {
						String sTemplateID = template.getMainID().replaceAll("\\$\\(n\\)", sPartition);
						Plugin plugin = PluginPanel.g_plugins.get(sTemplateID);
	
						// check if template is in use
						if (plugin != null) {
							// if so, run through all connectors
							for (BeautiConnector connector : template.connectors) {
								if (connector.toString().contains("YuleModel")) {
									int h = 3;
									h++;
								}
								if (connector.atInitialisationOnly()) {
									if (bInitial) {
										System.err.println("connect: " + connector);
										connect(connector, sPartition);
									}
								} else 	if (connector.isActivated(sPartition, posteriorPredecessors)) {
									System.err.println("connect: " + connector);
									try {
										connect(connector, sPartition);
									} catch (Exception e) {
										System.err.println(e.getMessage());
									}

								} else {
									System.err.println("DISconnect: " + connector);
									try {
										disconnect(connector, sPartition);
									} catch (Exception e) {
										System.err.println(e.getMessage());
									}
								}
							}
						}
					}
				}
			
				// if the model changed, some rules that use inposterior() may not have been triggered properly
				// so we need to check that the model changed, and if so, revisit the BeautiConnectors
				List<Plugin> posteriorPredecessors2 = new ArrayList<Plugin>();
				collectPredecessors(((MCMC) mcmc.get()).posteriorInput.get(), posteriorPredecessors2);
				if (posteriorPredecessors.size() != posteriorPredecessors2.size()) {
					bProgress = true;
				} else {
					for (Plugin plugin : posteriorPredecessors2) {
						if (!posteriorPredecessors.contains(plugin)) {
							bProgress = true;
							break;
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
			collectTreePriors();

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
			BranchRateModel.Base firstModel = null;
			for (Distribution distr : ((CompoundDistribution)likelihood).pDistributions.get()) {
				if (distr instanceof TreeLikelihood) {
					TreeLikelihood treeLikelihood = (TreeLikelihood) distr;
					boolean bNeedsEstimation = false;
					if (i > 0) {
						BranchRateModel.Base model = (BranchRateModel.Base) treeLikelihood.m_pBranchRateModel.get();
						bNeedsEstimation = (model != firstModel);
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
						if (firstModel == null) {
							firstModel = model;
						}
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

	public void addPlugin(final Plugin plugin) { //throws Exception {
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
		connect(srcPlugin, sTargetID, connector.sTargetInput);
	}

	public void connect(Plugin srcPlugin, String sTargetID, String sInputName) {
		try {
			Plugin target = PluginPanel.g_plugins.get(sTargetID);
			// prevent duplication inserts in list
			Object o = target.getInputValue(sInputName);
			if (o instanceof List) {
				//System.err.println("   " + ((List)o).size());
				if (((List<?>) o).contains(srcPlugin)) {
					System.err.println("   " + sTargetID + "/"  + sInputName +  " already contains " + srcPlugin.getID());
					return;
				}
			}
			
			target.setInputValue(sInputName, srcPlugin);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** disconnect source plugin with target plugin **/
	public void disconnect(BeautiConnector connector, String sPartition) {
		Plugin srcPlugin = PluginPanel.g_plugins.get(connector.sSourceID.replaceAll("\\$\\(n\\)", sPartition));
		String sTargetID = connector.sTargetID.replaceAll("\\$\\(n\\)", sPartition);
		disconnect(srcPlugin, sTargetID, connector.sTargetInput);
	}
	
	public void disconnect(Plugin srcPlugin, String sTargetID, String sInputName) { 
		try {
			Plugin target = PluginPanel.g_plugins.get(sTargetID);
			if (target == null) {
				return;
			}
			Input<?> input = target.getInput(sInputName);
			Object o = input.get();
			if (o instanceof List) {
				List<?> list = (List<?>) o;
				//System.err.println("   " + ((List)o).size());
				for (int i = 0; i < list.size(); i++) {
					if (list.get(i) == srcPlugin) {
						System.err.println("  DEL "  + sTargetID + "/"  + sInputName +  " already contains " + srcPlugin.getID());
						list.remove(i);
					}
				}
				if (srcPlugin != null && srcPlugin.outputs != null) {
					srcPlugin.outputs.remove(target);
				}
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
