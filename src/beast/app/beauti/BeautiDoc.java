package beast.app.beauti;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import beast.app.draw.InputEditor;
import beast.app.draw.InputEditorFactory;
import beast.app.draw.PluginPanel;
import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.MCMC;
import beast.core.Plugin;
import beast.core.StateNode;
import beast.core.parameter.RealParameter;
import beast.core.util.CompoundDistribution;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.branchratemodel.StrictClockModel;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;
import beast.math.distributions.MRCAPrior;
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

	public List<Alignment> alignments = new ArrayList<Alignment>();

	public Input<beast.core.Runnable> mcmc = new Input<beast.core.Runnable>("runnable", "main entry of analysis",
			Validate.REQUIRED);

	protected List<BranchRateModel> clockModels;
	// protected List<TreeDistribution> treePriors;
	/**
	 * contains all loggers from the template *
	 */

	public boolean bAutoSetClockRate = true;

	/**
	 * [0] = sitemodel [1] = clock model [2] = tree *
	 */
	List<Plugin>[] pPartitionByAlignments;
	List<Plugin>[] pPartition;
	private List<Integer>[] nCurrentPartitions;
	// partition names
	List<String> sPartitionNames = new ArrayList<String>();

	public BeautiConfig beautiConfig;

	private String templateName = null;
	private String templateFileName = STANDARD_TEMPLATE;

	Map<String, String> tipTextMap = new HashMap<String, String>();

	/**
	 * list of all plugins in the model, mapped by its ID *
	 */
	public HashMap<String, Plugin> pluginmap = null;
	/**
	 * list of all plugins in the model that have an impact on the posterior
	 */
	List<Plugin> posteriorPredecessors = null;
	List<Plugin> likelihoodPredecessors = null;

	/**
	 * set of all taxa in the model *
	 */
	public Set<Taxon> taxaset = null;

	private boolean isExpertMode = false;

	public Set<InputEditor> currentInputEditors = new HashSet<InputEditor>();

	/**
	 * name of current file, used for saving (as opposed to saveAs) *
	 */
	private String fileName = "";

	InputEditorFactory inputEditorFactory;

	public InputEditorFactory getInpuEditorFactory() {
		return inputEditorFactory;
	}

	public BeautiDoc() {
		// g_doc = this;
		setID("BeautiDoc");
		clear();
		inputEditorFactory = new InputEditorFactory(this);
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	public String getTemplateName() {
		return templateName;
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
					this.fileName = nameFromFile(sFileName);
					i += 2;
				} else if (args[i].equals("-template")) {
					String sFileName = args[i + 1];
					sTemplateXML = processTemplate(sFileName);
					templateFileName = sFileName;
					templateName = nameFromFile(sFileName);
					i += 2;
				} else if (args[i].equals("-nex")) {
					// NB: multiple -nex/-xmldata commands can be processed!
					String fileName = args[i + 1];
					NexusParser parser = new NexusParser();
					parser.parseFile(new File(fileName));
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
					String fileName = args[i + 1];
					Alignment alignment = (Alignment) AlignmentListInputEditor.getXMLData(new File(fileName));
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

	private Set<BeautiDocListener> listeners = new HashSet<BeautiDocListener>();

	public void addBeautiDocListener(BeautiDocListener listener) {
		listeners.add(listener);
	}

	@SuppressWarnings("unchecked")
	void clear() {
		clockModels = new ArrayList<BranchRateModel>();
		// treePriors = new ArrayList<TreeDistribution>();
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

		pluginmap = new HashMap<String, Plugin>();
		taxaset = new HashSet<Taxon>();
		fileName = "";
	}

	public void registerPlugin(Plugin plugin) {
		// first make sure to remove plug-ins when the id of a plugin changed
		unregisterPlugin(plugin);

		pluginmap.put(plugin.getID(), plugin);
		if (plugin instanceof Taxon) {
			taxaset.add((Taxon) plugin);
		}
	}

	public void unregisterPlugin(Plugin plugin) {
		String oldID = null;
		for (String id : pluginmap.keySet()) {
			if (pluginmap.get(id).equals(plugin)) {
				oldID = id;
				break;
			}
		}
		if (oldID != null) {
			pluginmap.remove(oldID);
		}
		taxaset.remove(plugin);
	}

	/**
	 * remove all alignment data and model, and reload Standard template *
	 */
	public void newAnalysis() {
		try {
			clear();
			PluginPanel.init();
			beautiConfig.clear();
			String sXML = processTemplate(templateFileName);
			loadTemplate(sXML);

			for (BeautiDocListener listener : listeners) {
				listener.docHasChanged();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadXML(File file) throws Exception {
		String sXML = load(file);
		extractSequences(sXML);
		connectModel();
		fireDocHasChanged();
	}

	public void loadNewTemplate(String fileName) throws Exception {
		templateFileName = fileName;
		newAnalysis();
	}

	public void importNexus(File file) throws Exception {
		NexusParser parser = new NexusParser();
		parser.parseFile(file);
		if (parser.m_filteredAlignments.size() > 0) {
			for (Alignment data : parser.m_filteredAlignments) {
				addAlignmentWithSubnet(data);
			}
		} else {
			addAlignmentWithSubnet(parser.m_alignment);
		}
		connectModel();
		addTraitSet(parser.m_traitSet);
		fireDocHasChanged();
	}

	public void importXMLAlignment(File file) throws Exception {
		Alignment data = (Alignment) AlignmentListInputEditor.getXMLData(file);
		data.initAndValidate();
		addAlignmentWithSubnet(data);
		connectModel();
		fireDocHasChanged();
	}

	private void fireDocHasChanged() throws Exception {
		for (BeautiDocListener listener : listeners) {
			listener.docHasChanged();
		}
	}

	void initialize(ActionOnExit endState, String sXML, String sTemplate, String sFileName) throws Exception {
		// beautiConfig.clear();
		switch (endState) {
		case UNKNOWN:
		case SHOW_DETAILS_USE_TEMPLATE: {
			mergeSequences(sTemplate);
			// scrubAll(true, );
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

	/**
	 * public to allow access for unit test *
	 */
	public String processTemplate(String sFileName) throws Exception {
		final String MERGE_ELEMENT = "mergepoint";
		// first gather the set of potential directories with templates
		Set<String> sDirs = new HashSet<String>();// AddOnManager.getBeastDirectories();
		String pathSep = System.getProperty("path.separator");
		String classpath = System.getProperty("java.class.path");
		String fileSep = System.getProperty("file.separator");
		for (String path : classpath.split(pathSep)) {
			if (path.endsWith(".jar")) {
				path = path.substring(0, path.lastIndexOf(fileSep));
			}
			if (path.indexOf(fileSep) >= 0) {
				path = path.substring(0, path.lastIndexOf(fileSep));
			}
			if (!sDirs.contains(path)) {
				sDirs.add(path);
			}
		}

		// read main template, try all template directories if necessary
		File mainTemplate = new File(sFileName);
		for (String sDir : sDirs) {
			if (!mainTemplate.exists()) {
				mainTemplate = new File(sDir + fileSep + sFileName);
			}
			if (!mainTemplate.exists()) {
				mainTemplate = new File(sDir + fileSep + "templates" + fileSep + sFileName);
			}
		}
		System.err.println("Loading template " + mainTemplate.getAbsolutePath());
		String sTemplateXML = load(mainTemplate.getAbsolutePath());

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
		// ensure processed templates are unique in name.
		// This prevents loading templates twice, once from the development area
		// and once from .beast2-addon area
		Set<String> loadedTemplates = new HashSet<String>();
		for (String sDir : sDirs) {
			File templates = new File(sDir + fileSep + "templates");
			File[] files = templates.listFiles();
			if (files != null) {
				for (File template : files) {
					if (!template.getAbsolutePath().equals(mainTemplate.getAbsolutePath())
							&& template.getName().toLowerCase().endsWith(".xml")) {
						if (!loadedTemplates.contains(template.getName())) {
							System.err.println("Processing " + template.getAbsolutePath());
							loadedTemplates.add(template.getName());
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
										String sMergePoint = mergeElement.getAttributes().getNamedItem("point")
												.getNodeValue();
										if (!sMergePoints.containsKey(sMergePoint)) {
											System.err.println("Cannot find merge point named " + sMergePoint
													+ " from " + template.getName()
													+ " in template. MergeWith ignored.");
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
						} else {
							System.err.println("Skipping " + template.getAbsolutePath() + " since "
									+ template.getName() + " is already processed");
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
		templateName = nameFromFile(sFileName);

		Writer out = new OutputStreamWriter(new FileOutputStream("/tmp/beast.xml"));
		try {
			out.write(sTemplateXML);
		} finally {
			out.close();
		}

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

	static public String load(String fileName) throws IOException {
		return load(new File(fileName));
	}

	static public String load(File file) throws IOException {
		BufferedReader fin = new BufferedReader(new FileReader(file));
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
		sPartition = parsePartition(sPartition);
		for (Alignment data : alignments) {
			if (data.getID().equals(sPartition)) {
				return data;
			}
		}
		return null;
	}

	/**
	 * see whether we have a valid model that can be saved at this point in time
	 */
	public enum DOC_STATUS {
		NO_DOCUMENT, SAVED, DIRTY
	}

	public DOC_STATUS validateModel() {
		if (mcmc == null || sPartitionNames.size() == 0) {
			return DOC_STATUS.NO_DOCUMENT;
		}
		try {
			// check if file is already saved and not changed wrt file on disk
			if (fileName != null && fileName.length() > 0) {
				String sFileXML = load(fileName);
				String sXML = toXML();
				if (sFileXML.equals(sXML)) {
					return DOC_STATUS.SAVED;
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return DOC_STATUS.DIRTY;
	} // validateModel

	/**
	 * save specification in file *
	 */
	public void save(String sFileName) throws Exception {
		save(new File(sFileName));
	} // save

	/**
	 * save specification in file *
	 */
	public void save(File file) throws Exception {
		determinePartitions();
		scrubAll(false, false);
		// String sXML = new XMLProducer().toXML(mcmc.get(), );
		String sXML = toXML();
		FileWriter outfile = new FileWriter(file);
		outfile.write(sXML);
		outfile.close();
	} // save

	public String toXML() {
		Set<Plugin> plugins = new HashSet<Plugin>();
		for (Plugin plugin : pluginmap.values()) {
			String sName = plugin.getClass().getName();
			if (!sName.startsWith("beast.app.beauti")) {
				plugins.add(plugin);
			}
		}
		String sXML = new XMLProducer().toXML(mcmc.get(), plugins);
		return sXML + "\n";
	}

	void extractSequences(String sXML) throws Exception {
		// load standard template
		if (beautiConfig == null) {
			String sTemplateXML = processTemplate(STANDARD_TEMPLATE);
			loadTemplate(sTemplateXML);
		}
		// parse file
		XMLParser parser = new XMLParser();
		Plugin MCMC = parser.parseFragment(sXML, true);
		mcmc.setValue(MCMC, this);
		PluginPanel.addPluginToMap(MCMC, this);

		if (sXML.indexOf(XMLProducer.DO_NOT_EDIT_WARNING) > 0) {
			int iStart = sXML.indexOf(XMLProducer.DO_NOT_EDIT_WARNING);
			int iEnd = sXML.lastIndexOf("-->");
			sXML = sXML.substring(iStart, iEnd);
			sXML = sXML.replaceAll(XMLProducer.DO_NOT_EDIT_WARNING, "");
			sXML = "<beast namespace='" + XMLProducer.DEFAULT_NAMESPACE + "'>" + sXML + "</beast>";
			List<Plugin> plugins = parser.parseBareFragments(sXML, true);
			for (Plugin plugin : plugins) {
				PluginPanel.addPluginToMap(plugin, this);
			}
		}

		// extract alignments
		determinePartitions();
	}

	/**
	 * Merge sequence data with sXML specification.
	 */
	void mergeSequences(String sXML) throws Exception {
		if (sXML == null) {
			sXML = processTemplate(STANDARD_TEMPLATE);
		}
		loadTemplate(sXML);
		// create XML for alignments
		for (Alignment alignment : alignments) {
			beautiConfig.partitionTemplate.get().createSubNet(alignment, this);
		}
		determinePartitions();

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
				beautiConfig = (BeautiConfig) plugin;
				beautiConfig.setDoc(this);
			} else {
				System.err.println("template item " + plugin.getID() + " is ignored");
			}
			PluginPanel.addPluginToMap(plugin, this);
		}
	}

	/**
	 * assigns trait to first available tree *
	 */
	void addTraitSet(TraitSet trait) {
		if (trait != null) {
			CompoundDistribution likelihood = (CompoundDistribution) pluginmap.get("likelihood");
			for (Distribution d : likelihood.pDistributions.get()) {
				if (d instanceof TreeLikelihood) {
					Tree tree = ((TreeLikelihood) d).m_tree.get();
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
	 *             *
	 */
	void connectModel() throws Exception {
		scrubAll(true, true);
	}

	private void collectClockModels() {
		// collect branch rate models from model
		CompoundDistribution likelihood = (CompoundDistribution) pluginmap.get("likelihood");
		while (clockModels.size() < sPartitionNames.size()) {
			try {
				TreeLikelihood treelikelihood = new TreeLikelihood();
				treelikelihood.m_pBranchRateModel.setValue(new StrictClockModel(), treelikelihood);
				List<BeautiSubTemplate> sAvailablePlugins = inputEditorFactory.getAvailableTemplates(
						treelikelihood.m_pBranchRateModel, treelikelihood, null, this);
				Plugin plugin = sAvailablePlugins.get(0).createSubNet(sPartitionNames.get(clockModels.size()));
				clockModels.add((BranchRateModel.Base) plugin);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		int k = 0;
		for (Distribution d : likelihood.pDistributions.get()) {
			BranchRateModel clockModel = ((TreeLikelihood) d).m_pBranchRateModel.get();
			// sanity check
			Tree tree = null;
			try {
				for (Input<?> input : ((Plugin) clockModel).listInputs()) {
					if (input.getName().equals("tree")) {
						tree = (Tree) input.get();
					}

				}
				if (tree != null && tree != ((TreeLikelihood) d).m_tree.get()) {
					clockModel = clockModels.get(k);
					System.err.println("WARNING: unlinking clock model for " + d.getID());
					((TreeLikelihood) d).m_pBranchRateModel.setValue(clockModel, d);
				}
			} catch (Exception e) {
				// ignore
			}

			
			
			if (clockModel != null) {
				String sID = ((Plugin) clockModel).getID();
				sID = parsePartition(sID);
				String sPartition = alignments.get(k).getID();
				if (sID.equals(sPartition)) {
					clockModels.set(k, clockModel);
				}
				k++;
			}
		}
	}

	// private void collectTreePriors() {
	// // collect tree priors from model
	// CompoundDistribution prior = (CompoundDistribution)
	// pluginmap.get("prior");
	// while (treePriors.size() < sPartitionNames.size()) {
	// try {
	// CompoundDistribution distr = new CompoundDistribution();
	// distr.pDistributions.setValue(new YuleModel(), distr);
	// List<BeautiSubTemplate> sAvailablePlugins =
	// inputEditorFactory.getAvailableTemplates(distr.pDistributions, distr,
	// null, this);
	// for (int i = sAvailablePlugins.size() - 1; i >= 0; i--) {
	// if
	// (!TreeDistribution.class.isAssignableFrom(sAvailablePlugins.get(i)._class))
	// {
	// sAvailablePlugins.remove(i);
	// }
	// }
	// if (sAvailablePlugins.size() > 0) {
	// Plugin plugin =
	// sAvailablePlugins.get(0).createSubNet(sPartitionNames.get(treePriors.size()));
	// treePriors.add((TreeDistribution) plugin);
	// } else {
	// treePriors.add(null);
	// }
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	//
	// // find tree priors that are in the posterior somewhere
	// // by inspecting outputs of *[id=likelihood]/TreeLikelihood/Tree
	// // and match tree prior to partition of tree
	// CompoundDistribution likelihood = (CompoundDistribution)
	// pluginmap.get("likelihood");
	// for (Distribution distr : likelihood.pDistributions.get()) {
	// if (distr instanceof TreeLikelihood) {
	// TreeLikelihood tl = (TreeLikelihood) distr;
	// Tree tree = tl.m_tree.get();
	// int partition = getPartitionNr(tree);
	// for (Plugin plugin : tree.outputs) {
	// if (plugin instanceof TreeDistribution &&
	// posteriorPredecessors.contains(plugin)) {
	// treePriors.set(partition, (TreeDistribution) plugin);
	// }
	// }
	// }
	// }
	// }

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

	// TreeDistribution getTreePrior(String sPartition) {
	// int k = 0;
	// for (Alignment data : alignments) {
	// if (data.getID().equals(sPartition)) {
	// return treePriors.get(k);
	// }
	// k++;
	// }
	// return null;
	// }

	void scrubAll(boolean bUseNotEstimatedStateNodes, boolean bInitial) {
		try {
			if (bAutoSetClockRate) {
				setClockRate();
			}

			// set estimate flag on tree, only if tree occurs in a partition
			for (String sPartition : sPartitionNames) {
				Tree tree = (Tree) pluginmap.get("Tree.t:" + sPartition);
				tree.m_bIsEstimated.setValue(false, tree);
			}
			for (Plugin plugin : pPartition[2]) {
				Tree tree = ((TreeLikelihood) plugin).m_tree.get();
				tree.m_bIsEstimated.setValue(true, tree);
			}

			// go through all templates, and process connectors in relevant ones
			boolean bProgress = true;
			while (bProgress) {
				bProgress = false;
				posteriorPredecessors = new ArrayList<Plugin>();
				collectPredecessors(((MCMC) mcmc.get()).posteriorInput.get(), posteriorPredecessors);
				likelihoodPredecessors = new ArrayList<Plugin>();
				if (pluginmap.containsKey("likelihood")) {
					collectPredecessors(pluginmap.get("likelihood"), likelihoodPredecessors);
				}

				// process MRCA priors
				for (String sID : pluginmap.keySet()) {
					if (sID.endsWith(".prior")) {
						Plugin plugin = pluginmap.get(sID);
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
				templates.add(beautiConfig.partitionTemplate.get());
				templates.addAll(beautiConfig.subTemplates);

				List<String> sPartitionNames2 = new ArrayList<String>();
				// add 'Species' as special partition name
				sPartitionNames2.addAll(sPartitionNames);
				sPartitionNames2.add("Species");

				for (String sPartition : sPartitionNames2) {
					for (BeautiSubTemplate template : templates) {
						String sTemplateID = template.getMainID().replaceAll("\\$\\(n\\)", sPartition);
						Plugin plugin = pluginmap.get(sTemplateID);

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
										warning("connect: " + connector + "\n");
										connect(connector, sPartition);
									}
								} else if (connector.isActivated(sPartition, posteriorPredecessors,
										likelihoodPredecessors, this)) {
									warning("connect: " + connector + "\n");
									try {
										connect(connector, sPartition);
									} catch (Exception e) {
										warning(e.getMessage());
									}

								} else {
									warning("DISconnect: " + connector + "\n");
									try {
										disconnect(connector, sPartition);
									} catch (Exception e) {
										warning(e.getMessage() + "\n");
									}
								}
							}
						}
					}
				}

				// if the model changed, some rules that use inposterior() may
				// not have been triggered properly
				// so we need to check that the model changed, and if so,
				// revisit the BeautiConnectors
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

			collectClockModels();
			// collectTreePriors();

			System.err.println("PARTITIONS:\n");
			System.err.println(Arrays.toString(nCurrentPartitions));

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	} // scrubAll

	void setClockRate() throws Exception {
		Plugin likelihood = pluginmap.get("likelihood");
		if (likelihood instanceof CompoundDistribution) {
			int i = 0;
			BranchRateModel.Base firstModel = null;
			for (Distribution distr : ((CompoundDistribution) likelihood).pDistributions.get()) {
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

	public void addPlugin(final Plugin plugin) { // throws Exception {
		// SwingUtilities.invokeLater(new Runnable() {
		// @Override
		// public void run() {
		//
		PluginPanel.addPluginToMap(plugin, this);
		try {
			for (Input<?> input : plugin.listInputs()) {
				if (input.get() != null) {
					if (input.get() instanceof Plugin) {
						PluginPanel.addPluginToMap((Plugin) input.get(), this);
					}
					if (input.get() instanceof List<?>) {
						for (Object o : (List<?>) input.get()) {
							if (o instanceof Plugin) {
								PluginPanel.addPluginToMap((Plugin) o, this);
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

	/**
	 * connect source plugin with target plugin
	 * 
	 * @throws Exception
	 *             *
	 */
	public void connect(BeautiConnector connector, String sPartition) throws Exception {
		String sSrcID = connector.sSourceID.replaceAll("\\$\\(n\\)", sPartition);
		Plugin srcPlugin = pluginmap.get(sSrcID);
		if (srcPlugin == null) {
			throw new Exception("Could not find plugin with id " + sSrcID + ". Typo in template perhaps?");
		}
		String sTargetID = connector.sTargetID.replaceAll("\\$\\(n\\)", sPartition);
		connect(srcPlugin, sTargetID, connector.sTargetInput);
	}

	public void connect(Plugin srcPlugin, String sTargetID, String sInputName) {
		try {
			Plugin target = pluginmap.get(sTargetID);
			// prevent duplication inserts in list
			Object o = target.getInputValue(sInputName);
			if (o instanceof List) {
				// System.err.println("   " + ((List)o).size());
				if (((List<?>) o).contains(srcPlugin)) {
					warning("   " + sTargetID + "/" + sInputName + " already contains " + srcPlugin.getID() + "\n");
					return;
				}
			}

			target.setInputValue(sInputName, srcPlugin);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * disconnect source plugin with target plugin *
	 */
	public void disconnect(BeautiConnector connector, String sPartition) {
		Plugin srcPlugin = pluginmap.get(connector.sSourceID.replaceAll("\\$\\(n\\)", sPartition));
		String sTargetID = connector.sTargetID.replaceAll("\\$\\(n\\)", sPartition);
		disconnect(srcPlugin, sTargetID, connector.sTargetInput);
	}

	public void disconnect(Plugin srcPlugin, String sTargetID, String sInputName) {
		try {
			Plugin target = pluginmap.get(sTargetID);
			if (target == null) {
				return;
			}
			Input<?> input = target.getInput(sInputName);
			Object o = input.get();
			if (o instanceof List) {
				List<?> list = (List<?>) o;
				// System.err.println("   " + ((List)o).size());
				for (int i = 0; i < list.size(); i++) {
					if (list.get(i) == srcPlugin) {
						warning("  DEL " + sTargetID + "/" + sInputName + " contains " + srcPlugin.getID() + "\n");
						list.remove(i);
					}
				}
				if (srcPlugin != null && srcPlugin.outputs != null) {
					srcPlugin.outputs.remove(target);
				}
			} else {
				input.setValue(null, target);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addAlignmentWithSubnet(Alignment data) {
		alignments.add(data);
		beautiConfig.partitionTemplate.get().createSubNet(data, this);
		// re-determine partitions
		determinePartitions();
	}

	public void delAlignmentWithSubnet(Alignment data) {
		alignments.remove(data);
		try {
			BeautiSubTemplate template = beautiConfig.partitionTemplate.get();
			template.removeSubNet(template, data.getID());
		} catch (Exception e) {
			e.printStackTrace();
		}
		// re-determine partitions
		determinePartitions();
		scrubAll(true, true);
	}

	public void determinePartitions() {
		CompoundDistribution likelihood = (CompoundDistribution) pluginmap.get("likelihood");
		sPartitionNames = new ArrayList<String>();
		for (Distribution distr : likelihood.pDistributions.get()) {
			if (distr instanceof TreeLikelihood) {
				TreeLikelihood treeLikelihoods = (TreeLikelihood) distr;
				alignments.add(treeLikelihoods.m_data.get());
				String sID = treeLikelihoods.m_data.get().getID();
				sID = parsePartition(sID);
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
						treeLikelihood.m_pBranchRateModel.setValue(treeLikelihood2.m_pBranchRateModel.get(),
								treeLikelihood);
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

	int getPartitionNr(String partition) {
		int partitionNr = sPartitionNames.indexOf(partition);
		return partitionNr;
	}

	int getPartitionNr(Plugin plugin) {
		String partition = plugin.getID();
		partition = parsePartition(partition);
		return getPartitionNr(partition);
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
		for (BeautiSubTemplate template : beautiConfig.subTemplates) {
			try {
				if (input.canSetValue(template.instance, plugin)) {
					String sPartition = plugin.getID();
					sPartition = parsePartition(sPartition);
					Object o = template.createSubNet(sPartition, plugin, input);
					return o;
				}
			} catch (Exception e) {
				// ignore, cannot set value
			}
		}
		return null;
	}

	private void warning(String s) {
		if (Boolean.valueOf(System.getProperty("beast.debug"))) {
			System.err.print(s);
		}
	}

	public boolean isExpertMode() {
		return isExpertMode;
	}

	public void setExpertMode(boolean expertMode) {
		isExpertMode = expertMode;
	}

	static public String parsePartition(String sID) {
		String sPartition = sID.substring(sID.indexOf('.') + 1);
		if (sPartition.indexOf(':') >= 0) {
			sPartition = sPartition.substring(sPartition.indexOf(':') + 1);
		}
		return sPartition;
	}

	/**
	 * Create a deep copy of a plugin, but in a different partition context
	 * First, find all plugins that are predecesors of the plugin to be copied
	 * that are ancestors of statenodes
	 * 
	 * @param plugin
	 * @param partition
	 * @return
	 * @throws Exception
	 */
	static public Plugin deepCopyPlugin(Plugin plugin, Plugin parent, MCMC mcmc, String partitionContext, BeautiDoc doc)
			throws Exception {
		/** tabu = list of plugins that should not be copied **/
		Set<Plugin> tabu = new HashSet<Plugin>();
		tabu.add(parent);
		// add state
		tabu.add(mcmc.m_startState.get());
		// add likelihood and prior
		if (mcmc.posteriorInput.get() instanceof CompoundDistribution) {
			for (Distribution distr : ((CompoundDistribution) mcmc.posteriorInput.get()).pDistributions.get()) {
				if (distr instanceof CompoundDistribution) {
					tabu.add(distr);
					// for (Distribution distr2 : ((CompoundDistribution)
					// distr).pDistributions.get()) {
					// if (distr2 instanceof TreeLikelihood) {
					// tabu.add(distr2);
					// }
					// }
				}
			}
		}
		// add posterior
		tabu.add(mcmc.posteriorInput.get());
		// paraent of operators
		tabu.add(mcmc);
		// add loggers
		tabu.addAll(mcmc.m_loggers.get());

		// find predecessors of plugin to be copied
		List<Plugin> predecessors = new ArrayList<Plugin>();
		collectPredecessors(plugin, predecessors);

		// find ancestors of StateNodes that are predecessors + the plugin
		// itself
		Set<Plugin> ancestors = new HashSet<Plugin>();
		collectAncestors(plugin, ancestors, tabu);
		for (Plugin plugin2 : predecessors) {
			if (plugin2 instanceof StateNode) {
				Set<Plugin> ancestors2 = new HashSet<Plugin>();
				collectAncestors(plugin2, ancestors2, tabu);
				ancestors.addAll(ancestors2);
			}
		}

		// now the ancestors contain all plugins to be copied
		// make a copy of all individual Pluings, before connecting them up
		Map<String, Plugin> copySet = new HashMap<String, Plugin>();
		for (Plugin plugin2 : ancestors) {
			Plugin copy = (Plugin) plugin2.getClass().newInstance();
			String id = plugin2.getID();
			copySet.put(id, copy);
		}

		// set all inputs of copied plugins + outputs to tabu
		for (Plugin plugin2 : ancestors) {
			String id = plugin2.getID();
			Plugin copy = copySet.get(id);
			// set inputs
			for (Input<?> input : plugin2.listInputs()) {
				if (input.get() != null) {
					if (input.get() instanceof List) {
						// handle lists
						for (Object o : (List<?>) input.get()) {
							if (o instanceof Plugin) {
								Plugin value = (Plugin) o;
								if (copySet.containsKey(value.getID())) {
									value = copySet.get(value.getID());
								}
								copy.setInputValue(input.getName(), value);
							} else {
								// it is a primitive value
								copy.setInputValue(input.getName(), input.get());
							}
						}
					} else if (input.get() instanceof Plugin) {
						// handle Plugin
						Plugin value = (Plugin) input.get();
						if (copySet.containsKey(value.getID())) {
							value = copySet.get(value.getID());
						}
						copy.setInputValue(input.getName(), value);
					} else {
						// it is a primitive value
						copy.setInputValue(input.getName(), input.get());
					}
				}
			}
			copy.setID(renameId(id, partitionContext));
			// set outputs
			for (Plugin output : plugin2.outputs) {
				if (tabu.contains(output) && output != parent) {
					for (Input<?> input : output.listInputs()) {
						if (input.get() instanceof List) {
							List<?> list = (List<?>) input.get();
							if (list.contains(plugin2)) {
								output.setInputValue(input.getName(), copy);
							}
						}
					}

				}
			}

			copySet.put(id, copy);
		}

		// deep copy must be obtained from copyset, before sorting
		// since the sorting changes (deletes items) from the copySet map
		Plugin deepCopy = copySet.get(plugin.getID());

		// first need to sort copySet by topology, before we can initAndValidate
		// them
		List<Plugin> sorted = new ArrayList<Plugin>();
		Collection<Plugin> values = copySet.values();
		while (values.size() > 0) {
			for (Plugin copy : values) {
				boolean found = false;
				for (Plugin plugin2 : copy.listActivePlugins()) {
					if (values.contains(plugin2)) {
						found = true;
						break;
					}
				}
				if (!found) {
					sorted.add(copy);
				}
			}
			values.remove(sorted.get(sorted.size() - 1));
		}
		// initialise copied plugins
		for (Plugin copy : sorted) {
			copy.initAndValidate();
			if (doc != null) {
				doc.addPlugin(copy);
			}
		}

		return deepCopy;
	} // deepCopyPlugin

	public static String renameId(String sID, String partitionContext) {
		String sOldPartition = parsePartition(sID);
		String sNewPartition = partitionContext.substring(0, partitionContext.indexOf(':'));
		sID = sID.substring(0, sID.length() - sOldPartition.length()) + sNewPartition;
		return sID;
	}

	static public void collectPredecessors(Plugin plugin, List<Plugin> predecessors) {
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

	static public void collectAncestors(Plugin plugin, Set<Plugin> ancestors, Set<Plugin> tabu) {
		if ((plugin instanceof TreeLikelihood) || (plugin instanceof BeautiPanelConfig)) {
			return;
		}
		ancestors.add(plugin);
		try {
			for (Plugin plugin2 : plugin.outputs) {
				if (!ancestors.contains(plugin2) && !tabu.contains(plugin2)) {
					collectAncestors(plugin2, ancestors, tabu);
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

} // class BeautiDoc
