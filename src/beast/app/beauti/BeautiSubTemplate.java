package beast.app.beauti;


import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;

import beast.app.draw.PluginPanel;
import beast.core.Description;
import beast.core.Input;
import beast.core.Logger;
import beast.core.BEASTObject;
import beast.core.Input.Validate;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.FilteredAlignment;
import beast.evolution.likelihood.GenericTreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.util.XMLParser;


//import beast.app.beauti.BeautiConnector.ConnectCondition;

@Description("Template that specifies which sub-net needs to be created when " +
        "a plugin of a paricular class is created.")
public class BeautiSubTemplate extends BEASTObject {
    public Input<String> sClassInput = new Input<String>("class", "name of the class (with full class path) to be created", Validate.REQUIRED);
    public Input<String> sMainInput = new Input<String>("mainid", "specifies id of the main plugin to be created by the template", Validate.REQUIRED);
    //public Input<XML> sXMLInput = new Input<XML>("value","collection of objects to be created in Beast2 xml format", Validate.REQUIRED);
    public Input<String> sXMLInput = new Input<String>("value", "collection of objects to be created in Beast2 xml format", Validate.REQUIRED);
    public Input<List<BeautiConnector>> connectorsInput = new Input<List<BeautiConnector>>("connect", "Specifies which part of the template get connected to the main network", new ArrayList<BeautiConnector>());
    public Input<String> suppressedInputs = new Input<String>("suppressInputs", "comma separated list of inputs that should not be shown");

    Class<?> _class = null;
    Object instance;
    String sXML = null;
    List<BeautiConnector> connectors;

    BeautiDoc doc;

    //	String [] sSrcIDs;
//	String [] sTargetIDs;
//	String [] sTargetInputs;
//	ConnectCondition [] conditions;
    String sMainID = "";
    String sShortClassName;

    @Override
    public void initAndValidate() throws Exception {
        _class = Class.forName(sClassInput.get());
        sShortClassName = sClassInput.get().substring(sClassInput.get().lastIndexOf('.') + 1);
        instance = _class.newInstance();
        sXML = sXMLInput.get();//.m_sValue.get();
        sMainID = sMainInput.get();
        // sanity check: make sure the XML is parseable
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.newDocumentBuilder().parse(new InputSource(new StringReader("<beast>" + sXML + "</beast>")));
        // make sure there are no comments in the XML: this screws up any XML when saved to file
        if (sXML.contains("<!--")) {
            while (sXML.contains("<!--")) {
                int iStart = sXML.indexOf("<!--");
                // next line is guaranteed to find something, things we already checked this is valid XML
                int iEnd = sXML.indexOf("-->", iStart);
                sXML = sXML.substring(0, iStart) + sXML.substring(iEnd + 3);
            }
        }
        //m_sXMLInput.get().m_sValue.setValue("<![CDATA[" + m_sXML + "]]>", m_sXMLInput.get());
        sXMLInput.setValue("<![CDATA[" + sXML + "]]>", this);

        connectors = connectorsInput.get();
//		int nConnectors = connections.get().size();
//		sSrcIDs = new String[nConnectors];
//		sTargetIDs = new String[nConnectors];
//		sTargetInputs = new String[nConnectors];
////		conditions = new ConnectCondition[nConnectors];
//
//		for (int i = 0; i < nConnectors; i++) {
//			BeautiConnector connector = connections.get().get(i);
//			sSrcIDs[i] = connector.sSourceID.get();
//			sTargetIDs[i] = connector.sTargetID.get();
//			sTargetInputs[i] = connector.sInputName.get();
////			conditions[i] = connector.connectCondition.get(); 
//		}
    }

    public void setDoc(BeautiDoc doc) {
        this.doc = doc;
    }

    void removeSubNet(BeautiSubTemplate template, PartitionContext context) throws Exception {
        // disconnect all connection points in the template
        for (BeautiConnector connector : template.connectors) {
            doc.disconnect(connector, context);
        }
    }
    
    void removeSubNet(Object o) throws Exception {
        if (o == null) {
            // nothing to do
            return;
        }
        BEASTObject plugin = null;
        if (o instanceof BEASTObject) {
            plugin = (BEASTObject) o;
        }

        // find template that created this plugin
        String sID = plugin.getID();
        //String sPartition = BeautiDoc.parsePartition(sID);
        sID = sID.substring(0, sID.indexOf("."));
        BeautiSubTemplate template = null;
        for (BeautiSubTemplate template2 : doc.beautiConfig.subTemplatesInput.get()) {
            if (template2.matchesName(sID)) {
                template = template2;
                break;
            }
        }
        if (template == null) {
            throw new Exception("Cannot find template for removing " + plugin.getID());
        }
        PartitionContext context = doc.getContextFor(plugin);
        removeSubNet(template, context);
    }

    public BEASTObject createSubNet(PartitionContext partition, BEASTObject plugin, Input<?> input, boolean init) throws Exception {
        removeSubNet(input.get());
        if (sXML == null) {
            // this is the NULL_TEMPLATE
            input.setValue(null, plugin);
            return null;
        }
        BEASTObject o = createSubNet(partition, doc.pluginmap, init);
        input.setValue(o, plugin);
        return o;
    }

    public BEASTObject createSubNet(PartitionContext partition, List<BEASTObject> list, int iItem, boolean init) throws Exception {
        removeSubNet(list.get(iItem));
        if (sXML == null) {
            // this is the NULL_TEMPLATE
            list.set(iItem, null);
            return null;
        }
        BEASTObject o = createSubNet(partition, doc.pluginmap, init);
        list.set(iItem, o);
        return o;
    }

    public BEASTObject createSubNet(PartitionContext partition, boolean init) throws Exception {
        if (sXML == null) {
            // this is the NULL_TEMPLATE
            return null;
        }
        BEASTObject o = createSubNet(partition, doc.pluginmap, init);
        return o;
    }


    BEASTObject createSubNet(Alignment data, BeautiDoc doc, boolean init) {
        String sPartition = data.getID();
        HashMap<String, BEASTObject> sIDMap = doc.pluginmap;//new HashMap<String, Plugin>();
        sIDMap.put(sPartition, data);
        return createSubNet(new PartitionContext(sPartition), sIDMap, init);
    }

    private BEASTObject createSubNet(PartitionContext context, /*BeautiDoc doc,*/ HashMap<String, BEASTObject> sIDMap, boolean init) {
        // wrap in a beast element with appropriate name spaces
        String _sXML = "<beast version='2.0' \n" +
                "namespace='beast.app.beauti:beast.core:beast.evolution.branchratemodel:beast.evolution.speciation:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood:beast.evolution:beast.math.distributions'>\n" +
                sXML +
                "</beast>\n";

        // resolve alignment references
        _sXML = _sXML.replaceAll("idref=[\"']data['\"]", "idref='" + context.partition + "'");
        // ensure uniqueness of IDs
        _sXML = BeautiDoc.translatePartitionNames(_sXML, context);//_sXML.replaceAll("\\$\\(n\\)", sPartition);

        XMLParser parser = new XMLParser();
        parser.setRequiredInputProvider(doc, context);
        List<BEASTObject> plugins = null;
        try {
            plugins = parser.parseTemplate(_sXML, sIDMap, true);
            for (BEASTObject plugin : plugins) {
                doc.addPlugin(plugin);
                try {
                	System.err.println("Adding " + plugin.getClass().getName() + " " + plugin);
                } catch (Exception e) {
                	System.err.println("Adding " + plugin.getClass().getName());
				}
            }

            for (BeautiConnector connector : connectors) {
                if (init && connector.atInitialisationOnly()) {// ||
                    doc.connect(connector, context);
                }
                if (connector.getTipText() != null) {
                    doc.tipTextMap.put(BeautiDoc.translatePartitionNames(connector.sSourceID, context), //.replaceAll("\\$\\(n\\)", sPartition),
                    		BeautiDoc.translatePartitionNames(connector.getTipText(), context)); //.replaceAll("\\$\\(n\\)", sPartition));
                }
            }
            if (suppressedInputs.get() != null) {
                String[] inputs = suppressedInputs.get().split(",");
                for (String input : inputs) {
                    input = input.trim();
                    doc.beautiConfig.suppressPlugins.add(input);
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (sMainID.equals("[top]")) {
            return plugins.get(0);
        }

        String sID = sMainID;
        sID = BeautiDoc.translatePartitionNames(sID, context); //sID.replaceAll("\\$\\(n\\)", sPartition);
        BEASTObject plugin = doc.pluginmap.get(sID);

        if (this == doc.beautiConfig.partitionTemplate.get()) {
            // HACK: need to make sure the subst model is of the correct type
            BEASTObject treeLikelihood = doc.pluginmap.get("treeLikelihood." + context.partition);
            if (treeLikelihood != null && ((GenericTreeLikelihood) treeLikelihood).siteModelInput.get() instanceof SiteModel.Base) {
	            SiteModel.Base siteModel = (SiteModel.Base) ((GenericTreeLikelihood) treeLikelihood).siteModelInput.get();
	            SubstitutionModel substModel = siteModel.substModelInput.get();
	            try {
	                siteModel.canSetSubstModel(substModel);
	            } catch (Exception e) {
	                Object o = doc.createInput(siteModel, siteModel.substModelInput, context);
	                try {
	                    siteModel.substModelInput.setValue(o, siteModel);
	                } catch (Exception ex) {
	                    ex.printStackTrace();
	                }
	            }
            }

            // HACK2: rename file name for trace log if it has the default value
            Logger logger = (Logger) doc.pluginmap.get("tracelog");
            if (logger != null) {
	            String fileName = logger.fileNameInput.get();
	            if (fileName.startsWith("beast.") && treeLikelihood != null) {
	            	Alignment data = ((GenericTreeLikelihood)treeLikelihood).dataInput.get();
	            	while (data instanceof FilteredAlignment) {
	            		data = ((FilteredAlignment) data).alignmentInput.get();
	            	}
	            	fileName = data.getID() + fileName.substring(5);
	            	try {
						logger.fileNameInput.setValue(fileName, logger);
					} catch (Exception e) {
						e.printStackTrace();
					}
	            }
            }
        }

        //System.err.println(new XMLProducer().toXML(plugin));
        return plugin;
    }

    public String getMainID() {
        return sMainID;
    }


    @Override
    public String toString() {
        String sID = getID();
        sID = sID.replaceAll("([a-z])([A-Z])", "$1 $2");
        return sID;
    }


    public boolean matchesName(String sID) {
        if (getMainID().replaceAll(".\\$\\(n\\)", "").equals(sID)) {
            return true;
        }
        if (getMainID().replaceAll("..:\\$\\(n\\)", "").equals(sID)) {
            return true;
        }
        if (sShortClassName != null && sShortClassName.equals(sID)) {
            return true;
        }
        return false;
    }
}
