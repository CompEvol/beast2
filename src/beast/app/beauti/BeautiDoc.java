package beast.app.beauti;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringReader;
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

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import beast.app.draw.BEASTObjectPanel;
import beast.app.draw.InputEditor;
import beast.app.draw.InputEditorFactory;
import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.MCMC;
import beast.core.Operator;
import beast.core.StateNode;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.core.util.CompoundDistribution;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.FilteredAlignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.branchratemodel.StrictClockModel;
import beast.evolution.likelihood.GenericTreeLikelihood;
import beast.evolution.operators.TipDatesRandomWalker;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.ParametricDistribution;
import beast.math.distributions.Uniform;
import beast.util.JSONProducer;
import beast.util.NexusParser;
import beast.util.XMLParser;
import beast.util.XMLParser.RequiredInputProvider;
import beast.util.XMLParserException;
import beast.util.XMLProducer;


@Description("Beauti document in doc-view pattern, not useful in models")
public class BeautiDoc extends BEASTObject implements RequiredInputProvider {
    final static String STANDARD_TEMPLATE = "templates/Standard.xml";

    final static int ALIGNMENT_PARTITION = 3;
    final static int SITEMODEL_PARTITION = 0;
    final static int CLOCKMODEL_PARTITION = 1;
    final static int TREEMODEL_PARTITION = 2;

    public enum ActionOnExit {
        UNKNOWN, SHOW_DETAILS_USE_TEMPLATE, SHOW_DETAILS_USE_XML_SPEC, WRITE_XML, MERGE_AND_WRITE_XML
    }

    public List<Alignment> alignments = new ArrayList<>();

    final public Input<beast.core.Runnable> mcmc = new Input<>("runnable", "main entry of analysis",
            Validate.REQUIRED);

    protected List<BranchRateModel> clockModels;
    // protected List<TreeDistribution> treePriors;
    /**
     * contains all loggers from the template *
     */

    public boolean autoSetClockRate = true;

    public boolean autoUpdateOperatorWeights = true;

    public boolean autoUpdateFixMeanSubstRate = true;
    /**
     * flags for whether parameters can be linked.
     * Once a parameter is linked, (un)linking in the alignment editor should be disabled
     */
    public boolean allowLinking = false;
    public boolean hasLinkedAtLeastOnce = false;

    /**
     * [0] = sitemodel [1] = clock model [2] = tree *
     */
    List<BEASTInterface>[] pPartitionByAlignments;
    List<BEASTInterface>[] pPartition;
    private List<Integer>[] currentPartitions;
    // partition names
    List<PartitionContext> partitionNames = new ArrayList<>();
    Set<PartitionContext> possibleContexts = new HashSet<>();

    public BeautiConfig beautiConfig;
    Beauti beauti;

    private String templateName = null;
    private String templateFileName = STANDARD_TEMPLATE;

    Map<String, String> tipTextMap = new HashMap<>();

    /**
     * list of all beastObjects in the model, mapped by its ID *
     */
    public HashMap<String, BEASTInterface> pluginmap = null;
    private Map<BEASTInterface, String> reversePluginmap;
    /**
     * list of all beastObjects in the model that have an impact on the posterior
     */
    List<BEASTInterface> posteriorPredecessors = null;
    List<BEASTInterface> likelihoodPredecessors = null;

    /**
     * set of all taxa in the model *
     */
    public Map<String,Taxon> taxaset = null;

    private boolean isExpertMode = false;

    public Set<InputEditor> currentInputEditors = new HashSet<>();

    /**
     * name of current file, used for saving (as opposed to saveAs) *
     */
    private String fileName = "";


    public Set<Input<?>> linked;

    InputEditorFactory inputEditorFactory;

    /** used to capture Stdout and Stderr **/
    static ByteArrayOutputStream baos = null;


    public InputEditorFactory getInputEditorFactory() {
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

    public ActionOnExit parseArgs(String[] args) throws XMLParserException, SAXException, IOException, ParserConfigurationException  {
        ActionOnExit endState = ActionOnExit.UNKNOWN;
        String outputFileName = "beast.xml";
        String xml = null;
        String templateXML = null;
        TraitSet traitset = null;

        int i = 0;
        try {
            while (i < args.length) {
                int old = i;
                if (args[i].equals("")) {
                    i += 1;
                } else if (args[i].equals("-capture")) {
                	// capture stderr and stdout
                	// already done in beast.app.beauti.Beauti
                	i += 1;
                } else if (args[i].equals("-xml")) {
                    String fileName = args[i + 1];
                    xml = load(fileName);
                    // XMLParser parser = new XMLParser();
                    // m_doc.m_mcmc.setValue(parser.parseFile(fileName),
                    // m_doc);
                    this.fileName = nameFromFile(fileName);
                    i += 2;
                } else if (args[i].equals("-template")) {
                    String fileName = args[i + 1];
                    templateXML = processTemplate(fileName);
                    templateFileName = fileName;
                    templateName = nameFromFile(fileName);
                    i += 2;
                } else if (args[i].equals("-nex")) {
                    // NB: multiple -nex/-xmldata commands can be processed!
                    String fileName = args[i + 1];
                    NexusParser parser = new NexusParser();
                    parser.parseFile(new File(fileName));
                    if (parser.filteredAlignments.size() > 0) {
                        for (Alignment data : parser.filteredAlignments) {
                            alignments.add(data);
                        }
                    } else {
                        alignments.add(parser.m_alignment);
                    }
                    i += 2;
                    traitset = parser.traitSet;
                } else if (args[i].equals("-xmldata")) {
                    // NB: multiple -xmldata/-nex commands can be processed!
                    String fileName = args[i + 1];
                    Alignment alignment = (Alignment) BeautiAlignmentProvider.getXMLData(new File(fileName));
                    alignments.add(alignment);
                    i += 2;
                } else if (args[i].equals("-exitaction")) {
                    if (args[i + 1].equals("writexml")) {
                        endState = ActionOnExit.WRITE_XML;
                    } else if (args[i + 1].equals("usetemplate")) {
                        endState = ActionOnExit.SHOW_DETAILS_USE_TEMPLATE;
                    } else if (args[i + 1].equals("usexml")) {
                        endState = ActionOnExit.SHOW_DETAILS_USE_XML_SPEC;
                    } else if (args[i + 1].equals("merge")) {
                        endState = ActionOnExit.MERGE_AND_WRITE_XML;
                    } else {
                        throw new IllegalArgumentException("Expected one of 'writexml','usetemplate' or 'usexml', not " + args[i + 1]);
                    }
                    i += 2;
                } else if (args[i].equals("-out")) {
                    outputFileName = args[i + 1];
                    i += 2;
                } else if (args[i].equals("-noerr")) {
                    System.setErr(new PrintStream(new OutputStream() {
                        @Override
						public void write(int b) {
                        }
                    }));
                    i += 1;
                }

                if (i == old) {
                    throw new IllegalArgumentException("Wrong argument: " + args[i]);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        initialize(endState, xml, templateXML, outputFileName);
        addTraitSet(traitset);
        return endState;
    } // parseArgs

    String nameFromFile(String fileName) {
        if (fileName.contains("/")) {
            return fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length() - 4);
        } else if (fileName.contains("\\")) {
            return fileName.substring(fileName.lastIndexOf("\\") + 1, fileName.length() - 4);
        }
        return fileName.substring(0, fileName.length() - 4);
    }



    private Set<BeautiDocListener> listeners = new HashSet<>();

    public void addBeautiDocListener(BeautiDocListener listener) {
        listeners.add(listener);
    }

    @SuppressWarnings("unchecked")
    void clear() {
        clockModels = new ArrayList<>();
        // treePriors = new ArrayList<>();
        alignments = new ArrayList<>();

        pPartitionByAlignments = new List[3];
        pPartition = new List[3];
        currentPartitions = new List[3];
        partitionNames = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            pPartitionByAlignments[i] = new ArrayList<>();
            pPartition[i] = new ArrayList<>();
            currentPartitions[i] = new ArrayList<>();
        }
        tipTextMap = new HashMap<>();

        pluginmap = new HashMap<>();
        reversePluginmap = new HashMap<>();
        taxaset = new HashMap<>();
        fileName = "";
        linked = new HashSet<>();
    }

    public void registerPlugin(BEASTInterface beastObject) {
        // first make sure to remove plug-ins when the id of a beastObject changed
        unregisterPlugin(beastObject);

        pluginmap.put(beastObject.getID(), beastObject);
        reversePluginmap.put(beastObject, beastObject.getID());
        if (beastObject instanceof Taxon) {
        	Taxon taxon = (Taxon) beastObject;
            taxaset.put(taxon.getID(), taxon);
        }
    }

    public void unregisterPlugin(BEASTInterface beastObject) {
        taxaset.remove(beastObject.getID());
        // directly remove beast object from HashMap
        // relies on hashes of String being unique (which they should be).
        // is much more efficient (O(1)) than lookup in keySet (O(n)),
        // which matter when a lot of partitions are loaded
        // but less reliable since ID may have changed.
        String id = reversePluginmap.get(beastObject);
        if (id != null && pluginmap.containsKey(id)) {
            pluginmap.remove(id);
        }

//        String oldID = null;
//        for (String id : pluginmap.keySet()) {
//            if (pluginmap.get(id).equals(beastObject)) {
//                oldID = id;
//                break;
//            }
//        }
//        if (oldID != null) {
//            pluginmap.remove(oldID);
//        }
    }

    /**
     * remove all alignment data and model, and reload Standard template *
     */
    public void newAnalysis() {
        try {
            clear();
            BEASTObjectPanel.init();
            beautiConfig.clear();
            String xml = processTemplate(templateFileName);
            loadTemplate(xml);

            for (BeautiDocListener listener : listeners) {
                listener.docHasChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadXML(File file) throws IOException, XMLParserException, SAXException, ParserConfigurationException  {
        String xml = load(file);
        extractSequences(xml);
        scrubAll(true, false);
        fireDocHasChanged();
    }

    public void loadNewTemplate(String fileName)  {
        templateFileName = fileName;
        newAnalysis();
    }

    public void importNexus(File file) throws IOException  {
        NexusParser parser = new NexusParser();
        parser.parseFile(file);
        if (parser.filteredAlignments.size() > 0) {
            for (Alignment data : parser.filteredAlignments) {
                addAlignmentWithSubnet(data, beautiConfig.partitionTemplate.get());
            }
        } else {
            addAlignmentWithSubnet(parser.m_alignment, beautiConfig.partitionTemplate.get());
        }
//      connectModel();
        addTraitSet(parser.traitSet);
//      fireDocHasChanged();
    }

    public void importXMLAlignment(File file)  {
        Alignment data = (Alignment) BeautiAlignmentProvider.getXMLData(file);
        data.initAndValidate();
        addAlignmentWithSubnet(data, beautiConfig.partitionTemplate.get());
//      connectModel();
//      fireDocHasChanged();
    }

    void fireDocHasChanged()  {
        for (BeautiDocListener listener : listeners) {
            try {
				listener.docHasChanged();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
    }

    void initialize(ActionOnExit endState, String xml, String template, String fileName) throws XMLParserException, SAXException, IOException, ParserConfigurationException {
        // beautiConfig.clear();
        switch (endState) {
            case UNKNOWN:
            case SHOW_DETAILS_USE_TEMPLATE: {
                mergeSequences(template);
                // scrubAll(true, );
                connectModel();
                break;
            }
            case SHOW_DETAILS_USE_XML_SPEC: {
                if (template == null) {
                    template = processTemplate(STANDARD_TEMPLATE);
                }
                loadTemplate(template);
                extractSequences(xml);
                connectModel();
                break;
            }
            case WRITE_XML: {
                mergeSequences(template);
                connectModel();
                save(fileName);
                break;
            }
            case MERGE_AND_WRITE_XML: {
                // merge alignment with XML
//	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//	        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
//	        doc.normalize();
//	        NodeList nodes = doc.getElementsByTagName("data");
//
//			XMLProducer producer = new XMLProducer();
//			producer.toRawXML(alignments.get(0));
//
//			Pplugin beastObject =  parser.parseFragment(xml, false);
//			int i = xml.indexOf("<data");
//			for (BEASTObject beastObject : pluginmap.values()) {
//				if (beastObject instanceof Alignment) {
//
//				}
//			}
//			save(fileName);
                System.exit(1);
                break;
            }
            // // load standard template
            // String templateXML =
            // BeautiInitDlg.processTemplate(STANDARD_TEMPLATE);
            // loadTemplate(templateXML);
            // connectModel();
        }
    }

    /**
     * public to allow access for unit test
     * @throws IOException *
     */
    public String processTemplate(String fileName) throws IOException {
        final String MERGE_ELEMENT = "mergepoint";
        // first gather the set of potential directories with templates
        Set<String> dirs = new HashSet<>();// AddOnManager.getBeastDirectories();
        String pathSep = System.getProperty("path.separator");
        String classpath = System.getProperty("java.class.path");
        String fileSep = System.getProperty("file.separator");
        if (fileSep.equals("\\")) {
            fileSep = "\\\\";
        }
        dirs.add(".");
        for (String path : classpath.split(pathSep)) {
            path = path.replaceAll(fileSep, "/");
            if (path.endsWith(".jar")) {
                path = path.substring(0, path.lastIndexOf("/"));
            }
            if (path.indexOf("/") >= 0) {
                path = path.substring(0, path.lastIndexOf("/"));
            }
            if (!dirs.contains(path)) {
                dirs.add(path);
            }
        }

        // read main template, try all template directories if necessary
        File mainTemplate = new File(fileName);
        for (String dirName : dirs) {
            if (!mainTemplate.exists()) {
                mainTemplate = new File(dirName + fileSep + fileName);
            }
            if (!mainTemplate.exists()) {
                mainTemplate = new File(dirName + fileSep + "templates" + fileSep + fileName);
            }
        }
        Log.warning.println("Loading template " + mainTemplate.getAbsolutePath());
        String templateXML = load(mainTemplate.getAbsolutePath());

        // find merge points
        int i = 0;
        HashMap<String, String> mergePoints = new HashMap<>();
        while (i >= 0) {
            i = templateXML.indexOf("<" + MERGE_ELEMENT, i + 1);
            if (i > 0) {
                int j = templateXML.indexOf('>', i);
                String str = templateXML.substring(i, j);
                str = str.replaceAll(".*id=", "");
                char c = str.charAt(0);
                str = str.replaceAll(c + "[^" + c + "]*$", "");
                str = str.substring(1);
                mergePoints.put(str, "");
            }
        }

        // find XML to merge
        // ensure processed templates are unique in name.
        // This prevents loading templates twice, once from the development area
        // and once from .beast2-addon area
        Set<String> loadedTemplates = new HashSet<>();
        for (String dirName : dirs) {
            Log.info.println("Investigating " + dirName);
            File templates = new File(dirName + fileSep + "templates");
            File[] files = templates.listFiles();
            if (files != null) {
                for (File template : files) {
                    if (!template.getAbsolutePath().equals(mainTemplate.getAbsolutePath())
                            && template.getName().toLowerCase().endsWith(".xml")) {
                        if (!loadedTemplates.contains(template.getName())) {
                        	Log.warning.println("Processing " + template.getAbsolutePath());
                            loadedTemplates.add(template.getName());
                            String xml2 = load(template.getAbsolutePath());
                            if (!xml2.contains("<mergepoint ")) {
                                try {

                                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                                    // factory.setValidating(true);
                                    Document doc = factory.newDocumentBuilder().parse(template);
                                    doc.normalize();

                                    processBeautiConfig(doc);

                                    // find mergewith elements
                                    NodeList nodes = doc.getElementsByTagName("mergewith");
                                    for (int mergeElementIndex = 0; mergeElementIndex < nodes.getLength(); mergeElementIndex++) {
                                        Node mergeElement = nodes.item(mergeElementIndex);
                                        String mergePoint = mergeElement.getAttributes().getNamedItem("point")
                                                .getNodeValue();
                                        if (!mergePoints.containsKey(mergePoint)) {
                                        	Log.warning.println("Cannot find merge point named " + mergePoint
                                                    + " from " + template.getName()
                                                    + " in template. MergeWith ignored.");
                                        } else {
                                            String xml = "";
                                            NodeList children = mergeElement.getChildNodes();
                                            for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                                                xml += nodeToString(children.item(childIndex));
                                            }
                                            String str = mergePoints.get(mergePoint);
                                            str += xml;
                                            mergePoints.put(mergePoint, str);
                                        }

                                    }
                                } catch (Exception e) {
                                    if (!e.getMessage().contains("beast.app.beauti.InputConstraint")) {
                                    	Log.warning.println(e.getMessage());
                                    }
                                }
                            }
                        } else {
                        	Log.warning.println("Skipping " + template.getAbsolutePath() + " since "
                                    + template.getName() + " is already processed");
                        }

                    }
                }
            }
        }

        // merge XML
        i = 0;
        while (i >= 0) {
            i = templateXML.indexOf("<" + MERGE_ELEMENT, i + 1);
            if (i > 0) {
                int j = templateXML.indexOf('>', i);
                String str = templateXML.substring(i, j);
                str = str.replaceAll(".*id=", "");
                char c = str.charAt(0);
                str = str.replaceAll(c + "[^" + c + "]*$", "");
                str = str.substring(1);
                String xml = mergePoints.get(str);
                templateXML = templateXML.substring(0, i) + xml + templateXML.substring(j + 1);
            }
        }
        templateName = nameFromFile(fileName);

        if (Boolean.valueOf(System.getProperty("beast.debug"))) {
            Writer out = new OutputStreamWriter(new FileOutputStream("/tmp/beast.xml"));
            try {
                out.write(templateXML);
            } finally {
                out.close();
            }
        }

        return templateXML;
    }

    void processBeautiConfig(Document doc) throws XMLParserException, TransformerException  {
        // find configuration elements, process and remove
        NodeList nodes = doc.getElementsByTagName("beauticonfig");
        Node topNode = doc.getElementsByTagName("*").item(0);
        String nameSpaceStr = XMLParser.getAttribute(topNode, "namespace");
        for (int configElementIndex = 0; configElementIndex < nodes.getLength(); configElementIndex++) {
            Node configElement = nodes.item(configElementIndex);
            String xml = nodeToString(configElement);
            XMLParser parser = new XMLParser();
            parser.setNameSpace(nameSpaceStr);
            parser.parseBareFragment(xml, true);
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
        String str = null;
        while (fin.ready()) {
            str = fin.readLine();
            buf.append(str);
            buf.append('\n');
        }
        fin.close();
        return buf.toString();
    }

    Alignment getPartition(BEASTInterface beastObject) {
        String partition = beastObject.getID();
        partition = parsePartition(partition);
        for (Alignment data : alignments) {
            if (data.getID().equals(partition)) {
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
        if (mcmc == null || (mcmc.get().hasPartitions() && partitionNames.size() == 0)) {
            return DOC_STATUS.NO_DOCUMENT;
        }
        try {
            // check if file is already saved and not changed wrt file on disk
            if (fileName != null && fileName.length() > 0) {
                String fileXML = load(fileName);
                String xml = toXML();
                if (fileXML.equals(xml)) {
                    return DOC_STATUS.SAVED;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return DOC_STATUS.DIRTY;
    } // validateModel

    /**
     * save specification in file
     * @throws IOException *
     */
    public void save(String fileName) throws IOException  {
        save(new File(fileName));
    } // save

    /**
     * save specification in file
     * @throws IOException *
     */
    public void save(File file) throws IOException  {
        determinePartitions();
        scrubAll(false, false);
        // String xml = new XMLProducer().toXML(mcmc.get(), );
        String spec = null;
        if (file.getPath().toLowerCase().endsWith(".json")) {
            spec = toJSON();
        } else {
            spec = toXML();
        }
        FileWriter outfile = new FileWriter(file);
        outfile.write(spec);
        outfile.close();
    } // save

    private String toJSON() {
        Set<BEASTInterface> beastObjects = new HashSet<>();
        String json = new JSONProducer().toJSON(mcmc.get(), beastObjects);

        json = json.replaceFirst("\\{", "{ beautitemplate:\"" + templateName + "\", beautistatus:\"" + getBeautiStatus() + "\", ");
        return json + "\n";
    }

    public String toXML() {
        Set<BEASTInterface> beastObjects = new HashSet<>();
//		for (BEASTObject beastObject : pluginmap.values()) {
//			String name = beastObject.getClass().getName();
//			if (!name.startsWith("beast.app.beauti")) {
//				beastObjects.add(beastObject);
//			}
//		}
        String xml = new XMLProducer().toXML(mcmc.get(), beastObjects);

        xml = xml.replaceFirst("<beast ", "<beast beautitemplate='" + templateName + "' beautistatus='" + getBeautiStatus() + "' ");
        return xml + "\n";
    }

    /** get status of mode-flags in BEAUti, so these can be restored when reloading an XML file **/
    String getBeautiStatus() {
	    String beautiStatus = "";
	    if (!autoSetClockRate) {
	        beautiStatus = "noAutoSetClockRate";
	    }
	    if (allowLinking) {
	        beautiStatus += (beautiStatus.length() > 0 ? "|" : "") + "allowLinking";
	    }
	    if (!autoUpdateOperatorWeights) {
	        beautiStatus += (beautiStatus.length() > 0 ? "|" : "") + "noAutoUpdateOperatorWeights";
	    }
	    if (!autoUpdateFixMeanSubstRate) {
	        beautiStatus += (beautiStatus.length() > 0 ? "|" : "") + "noAutoUpdateFixMeanSubstRate";
	    }
	    return beautiStatus;
    }

    void extractSequences(String xml) throws XMLParserException, SAXException, IOException, ParserConfigurationException  {

        // parse the XML fragment into a DOM document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        doc.normalize();
        // find top level beast element
        NodeList nodes = doc.getElementsByTagName("*");
        Node topNode = nodes.item(0);
        String beautiTemplate = XMLParser.getAttribute(topNode, "beautitemplate");
        if (beautiTemplate == null) {
            int choice = JOptionPane.showConfirmDialog(getFrame(), "This file does not appear to be generated by BEAUti. If you load it, unexpected behaviour may follow");
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }
            // load standard template
            if (beautiConfig == null) {
                String templateXML = processTemplate(STANDARD_TEMPLATE);
                loadTemplate(templateXML);
            }
        } else {
            String templateXML = processTemplate(beautiTemplate + ".xml");
            loadTemplate(templateXML);
        }

        String beautiStatus = XMLParser.getAttribute(topNode, "beautistatus");
        if (beautiStatus == null) {
            beautiStatus = "";
        }
        autoSetClockRate = !beautiStatus.contains("noAutoSetClockRate");
        beauti.autoSetClockRate.setSelected(autoSetClockRate);
        allowLinking = beautiStatus.contains("allowLinking");
        beauti.allowLinking.setSelected(allowLinking);
        autoUpdateOperatorWeights = !beautiStatus.contains("noAutoUpdateOperatorWeights");
        beauti.autoUpdateOperatorWeights.setSelected(autoUpdateOperatorWeights);
        autoUpdateFixMeanSubstRate = !beautiStatus.contains("noAutoUpdateFixMeanSubstRate");
        beauti.autoUpdateFixMeanSubstRate.setSelected(autoUpdateFixMeanSubstRate);

        // parse file
        XMLParser parser = new XMLParser();
        BEASTInterface MCMC = parser.parseFragment(xml, true);
        mcmc.setValue(MCMC, this);
        BEASTObjectPanel.addPluginToMap(MCMC, this);

        // reconstruct all objects from templates
        try {
            CompoundDistribution posterior = (CompoundDistribution) ((beast.core.MCMC) mcmc.get()).posteriorInput.get();
            for (Distribution distr : posterior.pDistributions.get()) {
                if (distr.getID().equals("likelihood")) {
                    for (Distribution likelihood : ((CompoundDistribution) distr).pDistributions.get()) {
                        if (likelihood instanceof GenericTreeLikelihood) {
                            GenericTreeLikelihood treeLikelihood = (GenericTreeLikelihood) likelihood;
                            PartitionContext context = new PartitionContext(treeLikelihood);
                            try {
                                beautiConfig.partitionTemplate.get().createSubNet(context, false);
                            } catch (Exception e) {
                                //e.printStackTrace();
                            }
                            for (BeautiSubTemplate subTemplate : beautiConfig.subTemplates) {
                                try {
                                    subTemplate.createSubNet(context, false);
                                } catch (Exception e) {
                                    //e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }

//		MCMC = parser.parseFragment(xml, true);
//		mcmc.setValue(MCMC, this);
//		PluginPanel.addPluginToMap(MCMC, this);

//		if (xml.indexOf(XMLProducer.DO_NOT_EDIT_WARNING) > 0) {
//			int start = xml.indexOf(XMLProducer.DO_NOT_EDIT_WARNING);
//			int end = xml.lastIndexOf("-->");
//			xml = xml.substring(start, end);
//			xml = xml.replaceAll(XMLProducer.DO_NOT_EDIT_WARNING, "");
//			xml = "<beast namespace='" + XMLProducer.DEFAULT_NAMESPACE + "'>" + xml + "</beast>";
//			List<BEASTObject> beastObjects = parser.parseBareFragments(xml, true);
//			for (BEASTObject beastObject : beastObjects) {
//				PluginPanel.addPluginToMap(beastObject, this);
//			}
//		}

        // extract alignments
        determinePartitions();
    }

    /**
     * Merge sequence data with xml specification.
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws XMLParserException
     */
    void mergeSequences(String xml) throws XMLParserException, SAXException, IOException, ParserConfigurationException {
        if (xml == null) {
            xml = processTemplate(STANDARD_TEMPLATE);
        }
        loadTemplate(xml);
        // create XML for alignments
        if (beautiConfig != null) {
            for (Alignment alignment : alignments) {
                beautiConfig.partitionTemplate.get().createSubNet(alignment, this, true);
            }
        } else {
            // replace alignment
            for (BEASTInterface beastObject : pluginmap.values()) {
                if (beastObject instanceof Alignment) {
                	// use toArray to prevent ConcurrentModificationException
                    for (Object output : beastObject.getOutputs().toArray()) {
                        replaceInputs((BEASTInterface) output, beastObject, alignments.get(0));
                    }
                }
            }
            return;
        }
        determinePartitions();

    } // mergeSequences

    private void replaceInputs(BEASTInterface beastObject, BEASTInterface original, BEASTInterface replacement) {
        try {
            for (Input<?> input : beastObject.listInputs()) {
                if (input.get() != null) {
                    if (input.get() instanceof List) {
                        @SuppressWarnings("unchecked")
						List<BEASTInterface> list = (List<BEASTInterface>) input.get();
                        if (list.contains(original)) {
                            list.remove(original);
                            list.add(replacement);
                        }
                    } else {
                        if (input.get().equals(original)) {
                            input.setValue(replacement, beastObject);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void loadTemplate(String xml) throws XMLParserException, SAXException, IOException, ParserConfigurationException  {
        // load the template and its beauti configuration parts
        XMLParser parser = new XMLParser();
        BEASTObjectPanel.init();
        List<BEASTInterface> beastObjects = parser.parseTemplate(xml, new HashMap<>(), true);
        for (BEASTInterface beastObject : beastObjects) {
            if (beastObject instanceof beast.core.Runnable) {
                mcmc.setValue(beastObject, this);
            } else if (beastObject instanceof BeautiConfig) {
                beautiConfig = (BeautiConfig) beastObject;
                beautiConfig.setDoc(this);
            } else {
            	Log.warning.println("template item " + beastObject.getID() + " is ignored");
            }
            BEASTObjectPanel.addPluginToMap(beastObject, this);
        }
    }

    /**
     * assigns trait to first available tree *
     */
    void addTraitSet(TraitSet trait) {
        if (trait != null) {
            CompoundDistribution likelihood = (CompoundDistribution) pluginmap.get("likelihood");
            for (Distribution d : likelihood.pDistributions.get()) {
                if (d instanceof GenericTreeLikelihood) {
                    try {
                        // TODO: this might not be a valid type conversion from TreeInterface to Tree
                        Tree tree = (Tree) ((GenericTreeLikelihood) d).treeInput.get();
                        tree.m_traitList.setValue(trait, tree);
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
     * @ *
     */
    void connectModel()  {
        scrubAll(true, true);
    }

    private void collectClockModels() {
        // collect branch rate models from model
        CompoundDistribution likelihood = (CompoundDistribution) pluginmap.get("likelihood");
        while (clockModels.size() < partitionNames.size()) {
            try {
                GenericTreeLikelihood treelikelihood = new GenericTreeLikelihood();
                treelikelihood.branchRateModelInput.setValue(new StrictClockModel(), treelikelihood);
                List<BeautiSubTemplate> availableBEASTObjects = inputEditorFactory.getAvailableTemplates(
                        treelikelihood.branchRateModelInput, treelikelihood, null, this);
                BEASTInterface beastObject = availableBEASTObjects.get(0).createSubNet(partitionNames.get(clockModels.size()), true);
                clockModels.add((BranchRateModel.Base) beastObject);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        int k = 0;
        for (Distribution d : likelihood.pDistributions.get()) {
            BranchRateModel clockModel = ((GenericTreeLikelihood) d).branchRateModelInput.get();
            // sanity check
            Tree tree = null;
            try {
                for (Input<?> input : ((BEASTInterface) clockModel).listInputs()) {
                    if (input.getName().equals("tree")) {
                        tree = (Tree) input.get();
                    }

                }
                if (tree != null && tree != ((GenericTreeLikelihood) d).treeInput.get()) {
                    clockModel = clockModels.get(k);
                    Log.warning.println("WARNING: unlinking clock model for " + d.getID());
                    JOptionPane.showMessageDialog(beauti.getSelectedComponent(),
                            "Cannot link all clock model(s) except strict clock with different trees !");
                    ((GenericTreeLikelihood) d).branchRateModelInput.setValue(clockModel, d);
                }
            } catch (Exception e) {
                // ignore
            }

            if (clockModel != null) {
                String id = ((BEASTInterface) clockModel).getID();
                id = parsePartition(id);
                String partition = alignments.get(k).getID();
                if (id.equals(partition)) {
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
    // while (treePriors.size() < partitionNames.size()) {
    // try {
    // CompoundDistribution distr = new CompoundDistribution();
    // distr.pDistributions.setValue(new YuleModel(), distr);
    // List<BeautiSubTemplate> availableBEASTObjects =
    // inputEditorFactory.getAvailableTemplates(distr.pDistributions, distr,
    // null, this);
    // for (int i = availableBEASTObjects.size() - 1; i >= 0; i--) {
    // if
    // (!TreeDistribution.class.isAssignableFrom(availableBEASTObjects.get(i)._class))
    // {
    // availableBEASTObjects.remove(i);
    // }
    // }
    // if (availableBEASTObjects.size() > 0) {
    // BEASTObject beastObject =
    // availableBEASTObjects.get(0).createSubNet(partitionNames.get(treePriors.size()));
    // treePriors.add((TreeDistribution) beastObject);
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
    // for (BEASTObject beastObject : tree.outputs) {
    // if (beastObject instanceof TreeDistribution &&
    // posteriorPredecessors.contains(beastObject)) {
    // treePriors.set(partition, (TreeDistribution) beastObject);
    // }
    // }
    // }
    // }
    // }

    BranchRateModel getClockModel(String partition) {
        int k = 0;
        for (Alignment data : alignments) {
            if (data.getID().equals(partition)) {
                return clockModels.get(k);
            }
            k++;
        }
        return null;
    }

    // TreeDistribution getTreePrior(String partition) {
    // int k = 0;
    // for (Alignment data : alignments) {
    // if (data.getID().equals(partition)) {
    // return treePriors.get(k);
    // }
    // k++;
    // }
    // return null;
    // }

    synchronized public void scrubAll(boolean useNotEstimatedStateNodes, boolean isInitial) {
        try {
            if (autoSetClockRate) {
                setClockRate();
            }
            if (autoUpdateFixMeanSubstRate) {
            	SiteModelInputEditor.customConnector(this);
            }

            // set estimate flag on tree, only if tree occurs in a partition
//            for (BEASTInterface beastObject : pluginmap.values()) {
//                if (beastObject instanceof Tree) {
//                    Tree tree = (Tree) beastObject;
//                    tree.isEstimatedInput.setValue(false, tree);
//                }
//            }
//            for (BEASTInterface beastObject : pPartition[2]) {
//                // TODO: this might not be a valid type conversion from TreeInterface to Tree
//                Tree tree = (Tree) ((GenericTreeLikelihood) beastObject).treeInput.get();
//                tree.isEstimatedInput.setValue(true, tree);
//            }
            if (pluginmap.containsKey("Tree.t:Species")) {
                Tree tree = (Tree) pluginmap.get("Tree.t:Species");
                tree.isEstimatedInput.setValue(true, tree);
            }

            // go through all templates, and process connectors in relevant ones
            boolean progress = true;
            while (progress) {
                warning("============================ start scrubbing ===========================");
                progress = false;
                setUpActivePlugins();

                // process MRCA priors
                for (String id : pluginmap.keySet()) {
                    if (id.endsWith(".prior")) {
                    	BEASTInterface beastObject = pluginmap.get(id);
                        if (beastObject instanceof MRCAPrior) {
                            MRCAPrior prior = (MRCAPrior) beastObject;
                            if (prior.treeInput.get().isEstimatedInput.get() == false) {
                                // disconnect
                                disconnect(beastObject, "prior", "distribution");
                            } else {
                                // connect
                                connect(beastObject, "prior", "distribution");
                            }
                        }
                    }
                }

                List<BeautiSubTemplate> templates = new ArrayList<>();
                templates.add(beautiConfig.partitionTemplate.get());
                templates.addAll(beautiConfig.subTemplates);

                for (PartitionContext context : possibleContexts) {
                    applyBeautiRules(templates, isInitial, context);
                }
                // add 'Species' as special partition name
                applyBeautiRules(templates, isInitial, new PartitionContext("Species"));

                // if the model changed, some rules that use inposterior() may
                // not have been triggered properly
                // so we need to check that the model changed, and if so,
                // revisit the BeautiConnectors
                List<BEASTInterface> posteriorPredecessors2 = new ArrayList<>();
                collectPredecessors(((MCMC) mcmc.get()).posteriorInput.get(), posteriorPredecessors2);
                if (posteriorPredecessors.size() != posteriorPredecessors2.size()) {
                    progress = true;
                } else {
                    for (BEASTInterface beastObject : posteriorPredecessors2) {
                        if (!posteriorPredecessors.contains(beastObject)) {
                            progress = true;
                            break;
                        }
                    }
                }
            }

            List<BeautiSubTemplate> templates = new ArrayList<>();
            templates.add(beautiConfig.hyperPriorTemplate);
            for (BEASTInterface beastObject : pluginmap.values()) {
                if (beastObject instanceof RealParameter) {
                    if (beastObject.getID().startsWith("parameter.")) {
                        PartitionContext context = new PartitionContext(beastObject.getID().substring("parameter.".length()));
                        applyBeautiRules(templates, isInitial, context);
                    }
                }
            }


            collectClockModels();
            // collectTreePriors();

            Log.warning.println("PARTITIONS:\n");
            Log.warning.println(Arrays.toString(currentPartitions));

            determineLinks();
        } catch (Exception e) {
            Log.err.println(e.getMessage());
        }

        if (autoUpdateOperatorWeights) {
        	reweightSpeciesPartitionOperators();
        }
    } // scrubAll

    protected void setUpActivePlugins() {
        posteriorPredecessors = new ArrayList<>();
        collectPredecessors(((MCMC) mcmc.get()).posteriorInput.get(), posteriorPredecessors);
        likelihoodPredecessors = new ArrayList<>();
        if (pluginmap.containsKey("likelihood")) {
            collectPredecessors(pluginmap.get("likelihood"), likelihoodPredecessors);
        }


        Log.trace.print("InPosterior=");
        for (BEASTInterface o : posteriorPredecessors) {
        	pluginmap.put(o.getID(), o);
        	Log.trace.print(o.getID() + " ");
        	//if (!pluginmap.containsKey(o)) {
        	//	System.err.println("MISSING: " + o.getID());
        	//}
        }
        Log.trace.println();
    }

    public static String translatePartitionNames(String str, PartitionContext partition) {
//        str = str.replaceAll(".s:\\$\\(n\\)", ".s:" + partition.siteModel);
//        str = str.replaceAll(".c:\\$\\(n\\)", ".c:" + partition.clockModel);
//        str = str.replaceAll(".t:\\$\\(n\\)", ".t:" + partition.tree);
//        str = str.replaceAll("\\$\\(n\\)", partition.partition);
        // optimised code, based on (probably incorrect) profiler output
		StringBuilder sb = new StringBuilder();
		int len = str.length();
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			if (c == '.' && i < len - 6) {
				if (str.charAt(i + 2) == ':' && str.charAt(i + 3) == '$' &&
						str.charAt(i + 4) == '(' && str.charAt(i + 5) == 'n' && str.charAt(i + 6) == ')') {
					switch (str.charAt(i+1)) {
					case 's': // .s:$(n)
						sb.append(".s:").append(partition.siteModel);
						i += 6;
						break;
					case 'c':
						sb.append(".c:").append(partition.clockModel);
						i += 6;
						break;
					case 't':
						sb.append(".t:").append(partition.tree);
						i += 6;
						break;
					default:
						sb.append('.');
					}
				} else {
					sb.append('.');
				}
			} else if (c == '$' && i < len - 3) {
				if (str.charAt(i + 1) == '(' && str.charAt(i + 2) == 'n' && str.charAt(i + 3) == ')') {
					sb.append(partition.partition);
					i+= 3;
				} else {
					sb.append(c);
				}
			} else {
				sb.append(c);
			}
		}
        return sb.toString();
    }

    void applyBeautiRules(List<BeautiSubTemplate> templates, boolean isInitial, PartitionContext context) {
        for (BeautiSubTemplate template : templates) {
            String templateID = translatePartitionNames(template.getMainID(), context);
            BEASTInterface beastObject = pluginmap.get(templateID);

            // check if template is in use
            if (beastObject != null) {
                // if so, run through all connectors
                for (BeautiConnector connector : template.connectors) {

                    if (connector.atInitialisationOnly()) {
                        if (isInitial) {
                            warning("connect: " + connector.toString(context) + "\n");
                            connect(connector, context);
                        }
                    } else if (connector.isActivated(context, posteriorPredecessors,
                            likelihoodPredecessors, this)) {
                        warning("connect: " + connector.toString(context) + "\n");
                        try {
                            connect(connector, context);
                        } catch (Exception e) {
                            warning(e.getMessage());
                        }

                    } else {
                        warning("DISconnect: " + connector.toString(context) + "\n");
                        try {
                            disconnect(connector, context);
                        } catch (Exception e) {
                            warning(e.getMessage() + "\n");
                        }
                    }
                }
            }
        }
    }

    void setClockRate()  {
    	boolean needsEstimationBySPTree = false;
        if (pluginmap.containsKey("Tree.t:Species")) {
        	Tree sptree = (Tree) pluginmap.get("Tree.t:Species");
	        // check whether there is a calibration
	        for (Object beastObject : sptree.getOutputs()) {
	            if (beastObject instanceof MRCAPrior) {
	                MRCAPrior prior = (MRCAPrior) beastObject;
	                if (prior.distInput.get() != null) {
	                    needsEstimationBySPTree = true;
	                }
	            }
	        }
        }

        BEASTInterface likelihood = pluginmap.get("likelihood");
        if (likelihood instanceof CompoundDistribution) {
            int i = 0;
            RealParameter firstClock = null;
            for (Distribution distr : ((CompoundDistribution) likelihood).pDistributions.get()) {
                if (distr instanceof GenericTreeLikelihood) {
                    GenericTreeLikelihood treeLikelihood = (GenericTreeLikelihood) distr;
                    boolean needsEstimation = needsEstimationBySPTree;
                    if (i > 0) {
                        BranchRateModel.Base model = treeLikelihood.branchRateModelInput.get();
                        needsEstimation = (model.meanRateInput.get() != firstClock) || firstClock.isEstimatedInput.get();
                    } else {
                        // TODO: this might not be a valid type conversion from TreeInterface to Tree
                        Tree tree = (Tree) treeLikelihood.treeInput.get();
                        // check whether there are tip dates
                        if (tree.hasDateTrait()) {
                            needsEstimation = true;
                        }
                        // check whether there is a calibration
                        for (Object beastObject : tree.getOutputs()) {
                            if (beastObject instanceof MRCAPrior) {
                                MRCAPrior prior = (MRCAPrior) beastObject;
                                if (prior.distInput.get() != null) {
                                    needsEstimation = true;
                                }
                            }
                        }
                    }
                    BranchRateModel.Base model = treeLikelihood.branchRateModelInput.get();
                    if (model != null) {
                        RealParameter clockRate = model.meanRateInput.get();
                        clockRate.isEstimatedInput.setValue(needsEstimation, clockRate);
                        if (firstClock == null) {
                            firstClock = clockRate;
                        }
                    }
                    i++;
                }
            }
        }
    }

    public void addPlugin(final BEASTInterface beastObject) { //  {
        // SwingUtilities.invokeLater(new Runnable() {
        // @Override
        // public void run() {
        //
        BEASTObjectPanel.addPluginToMap(beastObject, this);
        try {
            for (Input<?> input : beastObject.listInputs()) {
                if (input.get() != null) {
                    if (input.get() instanceof BEASTInterface) {
                        BEASTObjectPanel.addPluginToMap((BEASTInterface) input.get(), this);
                    }
                    if (input.get() instanceof List<?>) {
                        for (Object o : (List<?>) input.get()) {
                            if (o instanceof BEASTInterface) {
                                BEASTObjectPanel.addPluginToMap((BEASTInterface) o, this);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        	Log.warning.println(e.getClass().getName() + " " + e.getMessage());
        }

        // }});

        // m_sIDMap.put(beastObject.getID(), beastObject);
        // for (BEASTObject beastObject2 : beastObject.listActivePlugins()) {
        // addPlugin(beastObject2);
        // }
    }

    /**
     * connect source beastObject with target beastObject
     *
     * @ *
     */
    public void connect(BeautiConnector connector, PartitionContext context) {
        if (!connector.isRegularConnector) {
            return;
        }
        String srcID = translatePartitionNames(connector.sourceID, context);
        BEASTInterface srcBEASTObject = pluginmap.get(srcID);
        if (srcBEASTObject == null) {
            throw new IllegalArgumentException("Could not find beastObject with id " + srcID + ". Typo in template perhaps?\n");
        }
        String targetID = translatePartitionNames(connector.targetID, context);
        connect(srcBEASTObject, targetID, connector.targetInput);
    }

    public void connect(BEASTInterface srcBEASTObject, String targetID, String inputName) {
        try {
        	BEASTInterface target = pluginmap.get(targetID);
            if (target == null) {
            	Log.trace.println("BeautiDoc: Could not find object " + targetID);
            	return;
            }
            // prevent duplication inserts in list
            Object o = target.getInputValue(inputName);
            if (o instanceof List) {
                // System.err.println("   " + ((List)o).size());
                if (((List<?>) o).contains(srcBEASTObject)) {
                    warning("   " + targetID + "/" + inputName + " already contains " + (srcBEASTObject == null ? "nulls" : srcBEASTObject.getID()) + "\n");
                    return;
                }
            }

            target.setInputValue(inputName, srcBEASTObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * disconnect source beastObject with target beastObject *
     */
    public void disconnect(BeautiConnector connector, PartitionContext context) {
        if (!connector.isRegularConnector) {
            return;
        }
        BEASTInterface srcBEASTObject = pluginmap.get(translatePartitionNames(connector.sourceID, context));
        String targetID = translatePartitionNames(connector.targetID, context);
        disconnect(srcBEASTObject, targetID, connector.targetInput);
    }

    public void disconnect(BEASTInterface srcBEASTObject, String targetID, String inputName) {
        try {
        	BEASTInterface target = pluginmap.get(targetID);
            if (target == null) {
                return;
            }
            final Input<?> input = target.getInput(inputName);
            Object o = input.get();
            if (o instanceof List) {
                List<?> list = (List<?>) o;
                // System.err.println("   " + ((List)o).size());
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) == srcBEASTObject) {
                        warning("  DEL " + targetID + "/" + inputName + " contains " + (srcBEASTObject == null ? "null" : srcBEASTObject.getID()) + "\n");
                        list.remove(i);
                    }
                }
                if (srcBEASTObject != null && srcBEASTObject.getOutputs() != null) {
                    srcBEASTObject.getOutputs().remove(target);
                }
            } else {
                if (input.get() != null && input.get() instanceof BEASTInterface &&
                		input.get() == srcBEASTObject) {
                        //((BEASTInterface) input.get()).getID().equals(targetID)) {
                    input.setValue(null, target);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addAlignmentWithSubnet(Alignment data, BeautiSubTemplate template) {
        alignments.add(data);
        template.createSubNet(data, this, true);
        // re-determine partitions
        determinePartitions();
    }

    /**
      * Reweight total weight of operators that work on the Species partition to 20%
      * of total operator weights. This helps *BEAST analyses in convergence. For non
      * *BEAST analyses, this bit of code has no effect.
      */
    private void reweightSpeciesPartitionOperators() {
    	if (!(mcmc.get() instanceof MCMC)) {
    		return;
    	}
    	List<Operator> speciesOperators = new ArrayList<>();
    	double totalWeight = 0;
    	double speciesWeight = 0;
    	for (Operator operator : ((MCMC)mcmc.get()).operatorsInput.get()) {
			if (operator.getID().endsWith("Species")) {
				speciesOperators.add(operator);
				speciesWeight += operator.getWeight();
			}
			totalWeight += operator.getWeight();
    	}

    	if (speciesWeight > 0 && speciesWeight < totalWeight) {
    		// we have a Species-related operator AND an alignment
    		// rescale weights so that 20% of operator weights is dedicated to Species operators
    		final double fraction = 0.2;
    		//double scale = fraction/(1.0 - fraction) / (speciesWeight / (totalWeight - speciesWeight));
    		double scale = fraction /(1-fraction) * ((totalWeight-speciesWeight) / speciesWeight);
    		for (Operator operator : speciesOperators) {
    			operator.m_pWeight.setValue(scale * operator.getWeight(), operator);
    		}
    	}

	}

	public BEASTInterface addAlignmentWithSubnet(PartitionContext context, BeautiSubTemplate template)  {
        BEASTInterface data = template.createSubNet(context, true);
        alignments.add((Alignment) data);
        // re-determine partitions
        determinePartitions();
        return data;
    }

    public void delAlignmentWithSubnet(Alignment data) {
        alignments.remove(data);
        try {
            PartitionContext context = null;
            for (PartitionContext context2 : partitionNames) {
                if (context2.partition.equals(data.getID())) {
                    context = context2;
                    break;
                }
            }
            BeautiSubTemplate template = beautiConfig.partitionTemplate.get();
            template.removeSubNet(template, context);
            for (BeautiSubTemplate template2 : beautiConfig.subTemplates) {
                template2.removeSubNet(template2, context);
            }

            // remove from possible contexts
            PartitionContext[] contexts = possibleContexts.toArray(new PartitionContext[0]);
            determinePartitions();
            scrubAll(true, false);
            for (PartitionContext context2 : contexts) {
                if (context2.equals(context)) {
                    possibleContexts.remove(context2);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // re-determine partitions
        determinePartitions();
        scrubAll(true, true);
    }

    public void determinePartitions() {
        CompoundDistribution likelihood = (CompoundDistribution) pluginmap.get("likelihood");
        if (likelihood == null) {
            return;
        }
        partitionNames.clear();
        possibleContexts.clear();
        for (Distribution distr : likelihood.pDistributions.get()) {
            if (distr instanceof GenericTreeLikelihood) {
                GenericTreeLikelihood treeLikelihood = (GenericTreeLikelihood) distr;
                alignments.add(treeLikelihood.dataInput.get());
                PartitionContext context = new PartitionContext(treeLikelihood);
                partitionNames.add(context);
                boolean found = false;
                for (PartitionContext context2 : possibleContexts) {
                    if (context.equals(context2)) {
                        found = true;
                    }
                }
                if (!found) {
                    possibleContexts.add(context);
                }
            }
        }

        alignments.clear();
        for (int i = 0; i < 3; i++) {
            pPartitionByAlignments[i].clear();
            pPartition[i].clear();
            currentPartitions[i].clear();
        }
        List<GenericTreeLikelihood> treeLikelihoods = new ArrayList<>();
        for (Distribution distr : likelihood.pDistributions.get()) {
            if (distr instanceof GenericTreeLikelihood) {
                GenericTreeLikelihood treeLikelihood = (GenericTreeLikelihood) distr;
                alignments.add(treeLikelihood.dataInput.get());
                treeLikelihoods.add(treeLikelihood);
            }
        }
        for (Distribution distr : likelihood.pDistributions.get()) {
            if (distr instanceof GenericTreeLikelihood) {
                GenericTreeLikelihood treeLikelihood = (GenericTreeLikelihood) distr;
                try {
                    // sync SiteModel, ClockModel and Tree to any changes that
                    // may have occurred
                    // this should only affect the clock model in practice
                    int partition = getPartitionNr((BEASTInterface) treeLikelihood.siteModelInput.get());
                    GenericTreeLikelihood treeLikelihood2 = treeLikelihoods.get(partition);
                    treeLikelihood.siteModelInput.setValue(treeLikelihood2.siteModelInput.get(), treeLikelihood);
                    currentPartitions[0].add(partition);

                    BranchRateModel rateModel = treeLikelihood.branchRateModelInput.get();
                    if (rateModel != null) {
                        partition = getPartitionNr((BEASTInterface) rateModel);
                        treeLikelihood2 = treeLikelihoods.get(partition);
                        treeLikelihood.branchRateModelInput.setValue(treeLikelihood2.branchRateModelInput.get(),
                                treeLikelihood);
                        currentPartitions[1].add(partition);
                    } else {
                        currentPartitions[1].add(0);
                    }

                    partition = getPartitionNr((BEASTInterface) treeLikelihood.treeInput.get());
                    treeLikelihood2 = treeLikelihoods.get(partition);
                    treeLikelihood.treeInput.setValue(treeLikelihood2.treeInput.get(), treeLikelihood);
                    currentPartitions[2].add(partition);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                pPartitionByAlignments[0].add(treeLikelihood);
                pPartitionByAlignments[1].add(treeLikelihood);
                pPartitionByAlignments[2].add(treeLikelihood);
            }
        }

        int partitionCount = partitionNames.size();
        for (int i = 0; i < 3; i++) {
            boolean[] usedPartition = new boolean[partitionCount];
            for (int j = 0; j < partitionCount; j++) {
                int partitionIndex = currentPartitions[i].get(j);// getPartitionNr(m_pPartitionByAlignments[i].get(j));
                usedPartition[partitionIndex] = true;
            }
            for (int j = 0; j < partitionCount; j++) {
                if (usedPartition[j]) {
                    pPartition[i].add(pPartitionByAlignments[i].get(j));
                }
            }
        }
        Log.warning.println("PARTITIONS0:\n");
        Log.warning.println(Arrays.toString(currentPartitions));
    }

    int getPartitionNr(String partition, int partitionID) {
        for (int i = 0; i < partitionNames.size(); i++) {
            PartitionContext context = partitionNames.get(i);
            switch (partitionID) {
                case ALIGNMENT_PARTITION:
                    if (context.partition.equals(partition)) {
                        return i;
                    }
                    break;
                case SITEMODEL_PARTITION:
                    if (context.siteModel.equals(partition)) {
                        return i;
                    }
                    break;
                case CLOCKMODEL_PARTITION:
                    if (context.clockModel.equals(partition)) {
                        return i;
                    }
                    break;
                case TREEMODEL_PARTITION:
                    if (context.tree.equals(partition)) {
                        return i;
                    }
                    break;
            }
        }
        return -1;
    }

    int getPartitionNr(BEASTInterface beastObject) {
        String ID = beastObject.getID();
        String partition = ID;
        if (ID.indexOf('.') >= 0) {
            partition = ID.substring(ID.indexOf('.') + 1);
        }
        int partitionID = ALIGNMENT_PARTITION;
        if (ID.indexOf(':') >= 0) {
            char c = ID.charAt(ID.length() - partition.length());
            switch (c) {
                case 's':
                    partitionID = SITEMODEL_PARTITION;
                    break;
                case 'c':
                    partitionID = CLOCKMODEL_PARTITION;
                    break;
                case 't':
                    partitionID = TREEMODEL_PARTITION;
                    break;
            }
            partition = partition.substring(partition.indexOf(':') + 1);
        }
        return getPartitionNr(partition, partitionID);
    }

    public List<BEASTInterface> getPartitions(String typeName) {
        if (typeName == null) {
            return pPartition[2];
        }
        if (typeName.contains("Partitions")) {
            List<BEASTInterface> beastObjects = new ArrayList<>();
            beastObjects.addAll(alignments);
            return beastObjects;
        }
        if (typeName.contains("SiteModel")) {
            return pPartition[0];
        }
        if (typeName.contains("ClockModel")) {
            return pPartition[1];
        }
        return pPartition[2];
    }

    public void setCurrentPartition(int colNr, int rowNr, String partition) {
        int currentPartion = getPartitionNr(partition, colNr);
        currentPartitions[colNr].set(rowNr, currentPartion);
    }

    @Override
    public Object createInput(BEASTInterface beastObject, Input<?> input, PartitionContext context) {
        for (BeautiSubTemplate template : beautiConfig.subTemplates) {
            try {
                if (input.canSetValue(template.instance, beastObject)) {
                    String partition = beastObject.getID();
                    partition = parsePartition(partition);
                    Object o = template.createSubNet(context, beastObject, input, true);
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
        	Log.warning.print(s);
        }
    }

    public boolean isExpertMode() {
        return isExpertMode;
    }

    public void setExpertMode(boolean expertMode) {
        isExpertMode = expertMode;
    }

    static public String parsePartition(String id) {
        String partition = id.substring(id.indexOf('.') + 1);
        if (partition.indexOf(':') >= 0) {
            partition = partition.substring(partition.indexOf(':') + 1);
        }
        return partition;
    }

    /**
     * Create a deep copy of a beastObject, but in a different partition context
     * First, find all beastObjects that are predecesors of the beastObject to be copied
     * that are ancestors of statenodes
     *
     * @param beastObject
     * @param parent
     * @return
     */
    static public BEASTInterface deepCopyPlugin(BEASTInterface beastObject, BEASTInterface parent, MCMC mcmc,
    										PartitionContext oldContext,
                                            PartitionContext newContext, BeautiDoc doc, List<BEASTInterface> tabooList)
          {
        /** taboo = list of beastObjects that should not be copied **/
        Set<BEASTInterface> taboo = new HashSet<>();
        taboo.add(parent);
        // add state
        taboo.add(mcmc.startStateInput.get());
        // add likelihood and prior
        if (mcmc.posteriorInput.get() instanceof CompoundDistribution) {
            for (Distribution distr : ((CompoundDistribution) mcmc.posteriorInput.get()).pDistributions.get()) {
                if (distr instanceof CompoundDistribution) {
                    taboo.add(distr);
                }
            }
        }
        // add posterior
        taboo.add(mcmc.posteriorInput.get());
        // parent of operators
        taboo.add(mcmc);
        // add loggers
        taboo.addAll(mcmc.loggersInput.get());
        // add exception for *BEAST logger (perhaps need to be generalised?)
        if (doc.pluginmap.containsKey("SpeciesTreeLoggerX")) {
        	taboo.add(doc.pluginmap.get("SpeciesTreeLoggerX"));
        }
        // add trees
        for (StateNode node : mcmc.startStateInput.get().stateNodeInput.get()) {
            if (node instanceof Tree) {
                taboo.add(node);
            }
        }
        // add MRCAPriors
		for (String id : doc.pluginmap.keySet()) {
			BEASTInterface o = doc.pluginmap.get(id);
			if (o instanceof MRCAPrior) {
				taboo.add(o);
			}
		}
        if (tabooList != null) {
            taboo.addAll(tabooList);
        }

        // find predecessors of beastObject to be copied
        List<BEASTInterface> predecessors = new ArrayList<>();
        collectPredecessors(beastObject, predecessors);

        // find ancestors of StateNodes that are predecessors + the beastObject
        // itself
        Set<BEASTInterface> ancestors = new HashSet<>();
        collectAncestors(beastObject, ancestors, taboo);
		Log.info.print(Arrays.toString(ancestors.toArray()));
        for (BEASTInterface beastObject2 : predecessors) {
            if (beastObject2 instanceof StateNode) {
                Set<BEASTInterface> ancestors2 = new HashSet<>();
                collectAncestors(beastObject2, ancestors2, taboo);
                ancestors.addAll(ancestors2);
            } else if (beastObject2 instanceof Alignment || beastObject2 instanceof FilteredAlignment) {
                for (Object output : beastObject2.getOutputs()) {
                    if (!taboo.contains(output)) {
                        Set<BEASTInterface> ancestors2 = new HashSet<>();
                        collectAncestors((BEASTInterface)output, ancestors2, taboo);
                        ancestors.addAll(ancestors2);
                    }
                }
            }
        }

//		Log.info.print(Arrays.toString(predecessors.toArray()));
//		for (BEASTObject p : ancestors) {
//			Log.info.print("(");
//			for (BEASTObject p2 : p.listActivePlugins()) {
//				if (ancestors.contains(p2)) {
//					Log.info.print(p2.getID()+ " ");
//				}
//			}
//			Log.info.print(") ");
//			Log.info.println(p.getID());
//		}

        // now the ancestors contain all beastObjects to be copied
        // make a copy of all individual BEASTObjects, before connecting them up
        Map<String, BEASTInterface> copySet = new HashMap<>();
        for (BEASTInterface beastObject2 : ancestors) {
            String id = beastObject2.getID();
            String copyID = renameId(id, oldContext, newContext);
            if (!id.equals(copyID)) {
	            if (doc.pluginmap.containsKey(copyID)) {
	            	BEASTInterface org = doc.pluginmap.get(copyID);
	                copySet.put(id, org);
	            } else {
	            	BEASTInterface copy;
					try {
						copy = beastObject2.getClass().newInstance();
		            	copy.setID(copyID);
		                copySet.put(id, copy);
					} catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
						throw new RuntimeException("Programmer error: every object in the model should have a default constructor that is publicly accessible");
					}
	            }
            }
            Log.warning.println("Copy: " + id + " -> " + copyID);
        }

        // set all inputs of copied beastObjects + outputs to taboo
		for (BEASTInterface beastObject2 : ancestors) {
            String id = beastObject2.getID();
            BEASTInterface copy = copySet.get(id);
            if (copy != null) {
            	Log.warning.println("Processing: " + id + " -> " + copy.getID());
            // set inputs
            for (Input<?> input : beastObject2.listInputs()) {
                if (input.get() != null) {
                    if (input.get() instanceof List) {
                        // handle lists
                    	//((List)copy.getInput(input.getName())).clear();
                        for (Object o : (List<?>) input.get()) {
                            if (o instanceof BEASTInterface) {
                            	BEASTInterface value = getCopyValue((BEASTInterface) o, copySet, oldContext, newContext, doc);
                            	// make sure it is not already in the list
                            	Object o2 = copy.getInput(input.getName()).get();
                            	boolean alreadyInList = false;
                            	if (o2 instanceof List) {
                            		List<?> currentList = (List<?>) o2;
	                            	for (Object v : currentList) {
	                            		if (v == value) {
	                            			alreadyInList = true;
	                            			break;
	                            		}
	                            	}
                            	}
                            	if (!alreadyInList) {
                            		// add to the list
                            		copy.setInputValue(input.getName(), value);
                            	}
                            } else {
                                // it is a primitive value
                            	if (copy instanceof Parameter.Base && input.getName().equals("value")) {
                            	//	// prevent appending to parameter values
                            		Parameter.Base<?> p = ((Parameter.Base<?>) copy);
                            		((List<?>) p.valuesInput.get()).clear();
                            	}
                                copy.setInputValue(input.getName(), input.get());
                            }
                        }
                    } else if (input.get() instanceof BEASTInterface) {
                        // handle BEASTObject
                    	BEASTInterface value = getCopyValue((BEASTInterface) input.get(), copySet, oldContext, newContext, doc);
                        copy.setInputValue(input.getName(), value);
                    } else if (input.get() instanceof String) {
                		// may need to replace partition info
                		String s = (String) input.get();
                		s = s.replaceAll("\\.c:[a-zA-Z0-9_]*", ".c:" + newContext.clockModel);
                		s = s.replaceAll("\\.s:[a-zA-Z0-9_]*", ".s:" + newContext.siteModel);
                		s = s.replaceAll("\\.t:[a-zA-Z0-9_]*", ".t:" + newContext.tree);
                		copy.setInputValue(input.getName(), s);
                	} else {
                        // it is a primitive value
                		copy.setInputValue(input.getName(), input.get());
                	}
                }
            }

            // set outputs
            for (Object output : beastObject2.getOutputs()) {
                if (taboo.contains(output) && output != parent) {
                	BEASTInterface output2 = getCopyValue((BEASTInterface)output, copySet, oldContext, newContext, doc);
                    for (Input<?> input : ((BEASTInterface)output).listInputs()) {
                        // do not add state node initialisers automatically
                        if (input.get() instanceof List &&
                                // do not update state node initialisers
                                !(taboo.contains(output2) && input.getName().equals("init"))) {
                            List<?> list = (List<?>) input.get();
                            if (list.contains(beastObject2)) {
                            	List<?> list2 = (List<?>)output2.getInput(input.getName()).get();
                            	if (!list2.contains(copy)) {
                            		output2.setInputValue(input.getName(), copy);
                            	}
                            }
                        }
                    }

                }
            }

            copySet.put(id, copy);
    		//Log.warning.println(base.operatorsAsString());
            }
        }

		// deep copy must be obtained from copyset, before sorting
        // since the sorting changes (deletes items) from the copySet map
        BEASTInterface deepCopy = copySet.get(beastObject.getID());

        // first need to sort copySet by topology, before we can initAndValidate
        // them
        List<BEASTInterface> sorted = new ArrayList<>();
        Collection<BEASTInterface> values = copySet.values();
        while (values.size() > 0) {
            for (BEASTInterface copy : values) {
                boolean found = false;
                for (BEASTInterface beastObject2 : copy.listActiveBEASTObjects()) {
                    if (values.contains(beastObject2)) {
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
        // initialise copied beastObjects
        Set<BEASTInterface> done = new HashSet<>();
        for (BEASTInterface copy : sorted) {
            try {
                if (!done.contains(copy)) {
                    copy.initAndValidate();
                    done.add(copy);
                }
            } catch (Exception e) {
                // ignore
            	Log.warning.print(e.getMessage());
            }
            if (doc != null) {
                doc.addPlugin(copy);
            }
        }

        doc.scrubAll(true, false);
        return deepCopy;
    } // deepCopyPlugin

    private static BEASTInterface getCopyValue(BEASTInterface value, Map<String, BEASTInterface> copySet, PartitionContext oldContext, PartitionContext partitionContext, BeautiDoc doc) {
        if (copySet.containsKey(value.getID())) {
            value = copySet.get(value.getID());
            return value;
        }
        String valueID = value.getID();
        if (valueID == null) {
            return value;
        }
        if (valueID.indexOf('.') >= 0) {
            String valueCopyID = renameId(valueID, oldContext, partitionContext);
            if (doc.pluginmap.containsKey(valueCopyID)) {
                value = doc.pluginmap.get(valueCopyID);
            }
        } else if (doc.pluginmap.get(valueID) instanceof Alignment || doc.pluginmap.get(valueID) instanceof FilteredAlignment) {
            return doc.pluginmap.get(partitionContext.partition);
        }
        return value;
    }

    public static String renameId(String id, PartitionContext oldContext, PartitionContext newContext) {
        String oldPartition = id.substring(id.indexOf('.') + 1);
        String newPartition = null;
        if (oldPartition.indexOf(':') >= 0) {
            char c = oldPartition.charAt(0);
            switch (c) {
                case 's':
                    newPartition = newContext.siteModel;
                    oldPartition  = oldContext.siteModel;
                    break;
                case 'c':
                    newPartition = newContext.clockModel;
                    oldPartition  = oldContext.clockModel;
                    break;
                case 't':
                    newPartition = newContext.tree;
                    oldPartition  = oldContext.tree;
                    break;
            }
            //oldPartition = oldPartition.substring(oldPartition.indexOf(':') + 1);
        } else {
            newPartition = newContext.partition;
            oldPartition = oldContext.partition;
        }
        if (id.indexOf('.') < 0 || !(id.endsWith(oldPartition))) {
        	// original id does not contain partition info
        	return id;
        }
        id = id.substring(0, id.length() - oldPartition.length()) + newPartition;
        return id;
    }

    static public void collectPredecessors(BEASTInterface beastObject, List<BEASTInterface> predecessors) {
        predecessors.add(beastObject);
        if (beastObject instanceof Alignment || beastObject instanceof FilteredAlignment) {
            return;
        }
        try {
            for (BEASTInterface beastObject2 : beastObject.listActiveBEASTObjects()) {
                if (!predecessors.contains(beastObject2)) {
                    collectPredecessors(beastObject2, predecessors);
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    static public void collectAncestors(BEASTInterface beastObject, Set<BEASTInterface> ancestors, Set<BEASTInterface> tabu) {
        if ((beastObject instanceof GenericTreeLikelihood) || (beastObject instanceof BeautiPanelConfig)) {
            return;
        }
        ancestors.add(beastObject);
        try {
            for (Object beastObject2 : beastObject.getOutputs()) {
                if (!ancestors.contains(beastObject2) && !tabu.contains(beastObject2)) {
                    collectAncestors((BEASTInterface)beastObject2, ancestors, tabu);
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void renamePartition(int partitionID, String oldName, String newName)  {
    	Log.warning.println("renamePartition: " + partitionID + " " + oldName + " " + newName);
        // sanity check: make sure newName is not already in use by another partition
        String newsuffix = null;
        switch (partitionID) {
            case ALIGNMENT_PARTITION:
                newsuffix = "." + newName;
                break;
            case SITEMODEL_PARTITION:
                newsuffix = ".s:" + newName;
                break;
            case CLOCKMODEL_PARTITION:
                newsuffix = ".c:" + newName;
                break;
            case TREEMODEL_PARTITION:
                newsuffix = ".t:" + newName;
                break;
            default:
                throw new IllegalArgumentException();
        }
        for (BEASTInterface beastObject : pluginmap.values()) {
            if (beastObject.getID().endsWith(newsuffix)) {
                throw new IllegalArgumentException("Name " + newName + " is already in use");
            }
        }

        // do the renaming
        String oldsuffix = null;
        switch (partitionID) {
            case ALIGNMENT_PARTITION:
                oldsuffix = "." + oldName;
                break;
            case SITEMODEL_PARTITION:
                oldsuffix = ".s:" + oldName;
                break;
            case CLOCKMODEL_PARTITION:
                oldsuffix = ".c:" + oldName;
                break;
            case TREEMODEL_PARTITION:
                oldsuffix = ".t:" + oldName;
                break;
            default:
                throw new IllegalArgumentException();
        }
        for (BEASTInterface beastObject : pluginmap.values()) {
            if (beastObject.getID().endsWith(oldsuffix)) {
                String id = beastObject.getID();
                id = id.substring(0, id.indexOf(oldsuffix)) + newsuffix;
                beastObject.setID(id);
            }
        }
        if (partitionID == ALIGNMENT_PARTITION) {
            // make exception for renaming alignment: its ID does not contain a dot
            for (BEASTInterface beastObject : pluginmap.values()) {
                if (beastObject.getID().equals(oldName)) {
                    beastObject.setID(newName);
                }
            }
        }

        // update beastObject map
        String[] keyset = pluginmap.keySet().toArray(new String[0]);
        for (String key : keyset) {
            if (key.endsWith(oldsuffix)) {
            	BEASTInterface beastObject = pluginmap.remove(key);
                key = key.substring(0, key.indexOf(oldsuffix)) + newsuffix;
                pluginmap.put(key, beastObject);
            }
        }

        // update tip text map
        keyset = tipTextMap.keySet().toArray(new String[0]);
        for (String key : keyset) {
            if (key.endsWith(oldsuffix)) {
                String tip = tipTextMap.remove(key);
                key = key.substring(0, key.indexOf(oldsuffix)) + newsuffix;
                tip = tip.replaceAll(oldsuffix, newsuffix);
                tipTextMap.put(key, tip);
            }
        }

        // update partition name table
        determinePartitions();
    } // renamePartition

    public PartitionContext getContextFor(BEASTInterface beastObject) {
        String id = beastObject.getID();
        String partition = id.substring(id.indexOf('.') + 1);

        int partitionID = ALIGNMENT_PARTITION;
        if (partition.indexOf(':') >= 0) {
            char c = partition.charAt(0);
            switch (c) {
                case 's':
                    partitionID = SITEMODEL_PARTITION;
                    break;
                case 'c':
                    partitionID = CLOCKMODEL_PARTITION;
                    break;
                case 't':
                    partitionID = TREEMODEL_PARTITION;
                    break;
            }
            partition = parsePartition(id);
        }

        for (PartitionContext context : partitionNames) {
            switch (partitionID) {
                case ALIGNMENT_PARTITION:
                    if (context.partition.equals(partition)) {
                        return context;
                    }
                    break;
                case SITEMODEL_PARTITION:
                    if (context.siteModel.equals(partition)) {
                        return context;
                    }
                    break;
                case CLOCKMODEL_PARTITION:
                    if (context.clockModel.equals(partition)) {
                        return context;
                    }
                    break;
                case TREEMODEL_PARTITION:
                    if (context.tree.equals(partition)) {
                        return context;
                    }
                    break;
                default:
                    // should never get here, unless template contains .X$(n) where X is not 'c', 't', or 's'
                    return null;
            }
        }
        return new PartitionContext(partition);
    }


    // methods for dealing with linking
    void determineLinks() {
        if (!allowLinking) {
            return;
        }
        linked.clear();
        for (BEASTInterface beastObject : posteriorPredecessors) {
            Map<String, Integer> outputIDs = new HashMap<>();
            for (Object output : beastObject.getOutputs()) {
                if (posteriorPredecessors.contains(output)) {
                    String id = ((BEASTInterface)output).getID();
                    if (id.indexOf('.') >= 0) {
                        id = id.substring(0, id.indexOf('.'));
                        if (outputIDs.containsKey(id)) {
                            outputIDs.put(id, outputIDs.get(id) + 1);
                        } else {
                            outputIDs.put(id, 1);
                        }
                    }
                }
            }
            for (Object output : beastObject.getOutputs()) {
                if (posteriorPredecessors.contains(output)) {
                    String id = ((BEASTInterface)output).getID();
                    if (id.indexOf('.') >= 0) {
                        id = id.substring(0, id.indexOf('.'));
                        if (outputIDs.get(id) > 1) {
                            addLink(beastObject, (BEASTInterface)output);
                        }
                    }
                }
            }
            // add parameters that have more than 1 outputs into susbtitution models
            if (beastObject instanceof Parameter<?>) {
                for (Object output : beastObject.getOutputs()) {
                    if (posteriorPredecessors.contains(output)) {
                        if (output instanceof SubstitutionModel) {
                            int nrOfSubstModelsInOutput = 0;
                            try {
                                for (Input<?> input : ((BEASTInterface)output).listInputs()) {
                                    if (input.get() != null && input.get().equals(beastObject)) {
                                        nrOfSubstModelsInOutput++;
                                    }
                                }
                            } catch (Exception e) {
                                // ignore
                            }
                            if (nrOfSubstModelsInOutput > 1) {
                                addLink(beastObject, (BEASTInterface)output);
                            }
                        }
                    }
                }
            }
        }

        hasLinkedAtLeastOnce = false;
        for (Input<?> input : linked) {
            if (input.getType().isAssignableFrom(RealParameter.class)) {
                hasLinkedAtLeastOnce = true;
                break;
            }
        }
    }

    void addLink(BEASTInterface from, BEASTInterface to) {
        try {
            for (Input<?> input : to.listInputs()) {
                if (input.get() instanceof BEASTInterface) {
                    if (input.get() == from) {
                        linked.add(input);
                        return;
                    }
                }
                // does it make sense to link list inputs?
//				if (input.get() instanceof List<?>) {
//					for (Object o : (List<?>) input.get()) {
//						if (o instanceof BEASTObject) {
//							if (o == from) {
//								addLink(input);
//								return;
//							}
//						}
//					}
//				}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addLink(Input<?> input) {
        linked.add(input);
        hasLinkedAtLeastOnce = true;
    }

    public void deLink(Input<?> input) {
        linked.remove(input);
    }

    public boolean isLinked(Input<?> input) {
        return linked.contains(input);
    }

    /**
     * return all RealParameters that have
     * the same ID in another partition, or
     * the same partition with the same substitution model as output
     *
     * @param beastObject
     * @return
     */
    public List<BEASTInterface> suggestedLinks(BEASTInterface beastObject) {
        String id = beastObject.getID();
        List<BEASTInterface> list = new ArrayList<>();
        String partitionID = null;
        if (id.indexOf('.') >= 0) {
            partitionID = id.substring(id.indexOf('.') + 1);
            id = id.substring(0, id.indexOf('.'));
        } else {
            return list;
        }
        for (BEASTInterface candidate : posteriorPredecessors) {
            String id2 = candidate.getID();
            if (id2.indexOf('.') >= 0) {
                String partitionID2 = id2.substring(id2.indexOf('.') + 1);
                id2 = id2.substring(0, id2.indexOf('.'));
                if (id2.equals(id)) {
                    list.add(candidate);
                }
                if (beastObject instanceof Parameter<?> &&
                        partitionID2.equals(partitionID) &&
                        candidate.getClass().equals(beastObject.getClass())) {
                    boolean dimensionMatches = true;
                    if (((Parameter<?>) beastObject).getDimension() != ((Parameter<?>) candidate).getDimension()) {
                        dimensionMatches = false;
                    }
                    // ensure they share an output
                    boolean foundCommonOutput = false;
                    for (Object out1 : beastObject.getOutputs()) {
                        for (Object out2 : candidate.getOutputs()) {
                            if (out1 == out2 && out1 instanceof SubstitutionModel) {
                                foundCommonOutput = true;
                                break;
                            }
                        }
                    }
                    if (dimensionMatches && foundCommonOutput) {
                        list.add(candidate);
                    }
                }
            }
        }
        list.remove(beastObject);
        return list;
    }

    public BEASTInterface getUnlinkCandidate(Input<?> input, BEASTInterface parent) {
        PartitionContext oldContext = getContextFor((BEASTInterface)input.get());
        PartitionContext newContext = getContextFor(parent);
        BEASTInterface beastObject = deepCopyPlugin((BEASTInterface) input.get(), parent, (MCMC) mcmc.get(), oldContext, newContext, this, null);
        return beastObject;
    }

    public void setBeauti(Beauti beauti) {
        this.beauti = beauti;
    }

    public JFrame getFrame() {
        return beauti.frame;
    }

    @Override
    public void initAndValidate() {
    }

    /** create taxonset, one taxon for each sequence in the alignment
     * and assign taxonset to the alignment
     * **/
    static void createTaxonSet(Alignment a, BeautiDoc doc) {
		List<String> taxaNames = a.getTaxaNames();
		TaxonSet taxonset = new TaxonSet();
        for (final String taxaName : taxaNames) {
        	taxonset.taxonsetInput.get().add(doc.getTaxon(taxaName));
        }
        taxonset.setID("TaxonSet0." + a.getID());
        try {
			taxonset.initAndValidate();
		} catch (Exception e) {
			e.printStackTrace();
		}
		doc.registerPlugin(taxonset);
	}

    /** create Taxon with given name, reuse if a taxon
     *  with this name already exists
     **/
	public Taxon getTaxon(String taxaName) {
    	if (taxaset.keySet().contains(taxaName)) {
    		return taxaset.get(taxaName);
    	} else {
    		try {
    			Taxon taxon = new Taxon(taxaName);
    			registerPlugin(taxon);
    			return taxon;
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
		return null;
	}

	public void addMRCAPrior(MRCAPrior mrcaPrior) {
			Tree tree = (Tree) pluginmap.get("Tree.t:" + alignments.get(0).getID());
			// TODO: make sure we have the appropriate tree
			CompoundDistribution prior = (CompoundDistribution) pluginmap.get("prior");
			mrcaPrior.treeInput.setValue(tree, mrcaPrior);
			ParametricDistribution distr = mrcaPrior.distInput.get();

			TaxonSet t = mrcaPrior.taxonsetInput.get();
			if (taxaset.keySet().contains(t.getID())) {
				Log.warning.println("taxonset " + t.getID() + " already exists: MRCAPrior " + mrcaPrior.getID() + " can not be added");
			} else {
				taxaset.put(t.getID(), t);
				// ensure TaxonSets are not duplicated
				List<Taxon> taxa = t.taxonsetInput.get();
				for (int i = 0; i< taxa.size(); i++) {
					if (taxaset.containsKey(taxa.get(i).getID())) {
						taxa.set(i, taxaset.get(taxa.get(i).getID()));
					} else {
						taxaset.put(taxa.get(i).getID(), taxa.get(i));
					}
				}
				if (distr instanceof Uniform && ((Uniform)distr).lowerInput.get() == ((Uniform)distr).upperInput.get()) {
					// it is a 'fixed' calibration, no need to add a distribution
				} else {
					prior.pDistributions.setValue(mrcaPrior, prior);
				}
			}
			if (t.taxonsetInput.get().size() == 1) {
				// it is a calibration on a tip -- better start sampling that tip
		        TipDatesRandomWalker operator = new TipDatesRandomWalker();
		        t.initAndValidate();
		        operator.initByName("taxonset", t, "weight", 1.0, "tree", tree, "windowSize", 1.0);
		        operator.setID("TipDatesRandomWalker." + t.getID());
		        MCMC mcmc = (MCMC) this.mcmc.get();
		        mcmc.operatorsInput.setValue(operator, mcmc);
		        
		        // set up date trait
		        double date = distr.getMean();
		        TraitSet dateTrait = null;
		        for (TraitSet ts : tree.m_traitList.get()) {
		        	if (ts.isDateTrait()) {
		        		dateTrait = ts;
		        	}
		        }
		        if (dateTrait == null) {
		        	dateTrait = new TraitSet();
		        	dateTrait.initByName("traitname", TraitSet.DATE_BACKWARD_TRAIT, "taxa", tree.getTaxonset(), 
		        			"value", t.taxonsetInput.get().get(0).getID() + "=" + date);
		        	tree.m_traitList.setValue(dateTrait, tree);
		        	tree.initAndValidate();
		        } else {
		        	dateTrait.traitsInput.setValue(dateTrait.traitsInput.get() + ",\n" +
		        			t.taxonsetInput.get().get(0).getID() + "=" + date
		        			, dateTrait);	
		        }
			}
	}


} // class BeautiDoc
