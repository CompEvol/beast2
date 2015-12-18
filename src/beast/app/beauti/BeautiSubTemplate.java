package beast.app.beauti;


import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import beast.core.Description;
import beast.core.Input;
import beast.core.Logger;
import beast.core.BEASTObject;
import beast.core.BEASTInterface;
import beast.core.Input.Validate;
import beast.core.util.Log;
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
    final public Input<String> sClassInput = new Input<>("class", "name of the class (with full class path) to be created", Validate.REQUIRED);
    final public Input<String> sMainInput = new Input<>("mainid", "specifies id of the main plugin to be created by the template", Validate.REQUIRED);
    //public Input<XML> sXMLInput = new Input<>("value","collection of objects to be created in Beast2 xml format", Validate.REQUIRED);
    final public Input<String> sXMLInput = new Input<>("value", "collection of objects to be created in Beast2 xml format", Validate.REQUIRED);
    final public Input<List<BeautiConnector>> connectorsInput = new Input<>("connect", "Specifies which part of the template get connected to the main network", new ArrayList<>());
    final public Input<String> suppressedInputs = new Input<>("suppressInputs", "comma separated list of inputs that should not be shown");
    final public Input<String> inlineInput = new Input<>("inlineInputs", "comma separated list of inputs that should " +
            "go inline, e.g. beast.evolution.sitemodel.SiteModel.substModel");
    final public Input<String> collapsedInput = new Input<>("collapsedInputs", "comma separated list of inputs that should " +
            "go inline, but are initially collapsed, e.g. beast.core.MCMC.logger");

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
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader("<beast xmlns:beauti='http://beast2.org'>" + sXML + "</beast>")));
        sXML = processDoc(doc);
        
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

    /* go through DOM document
     * pick up items that should be translated to BeautiConnectors
     * Remove any connector related code from DOM and return resulting XML as String
     */
    private String processDoc(Document doc) throws Exception {
        // find top level beast element
        final NodeList nodes = doc.getElementsByTagName("*");
        if (nodes == null || nodes.getLength() == 0) {
            throw new Exception("Expected top level beast element in XML");
        }
        final Node topNode = nodes.item(0);
        // process top level elements
        NodeList toplevels = topNode.getChildNodes();
        for (int i = 0; i < toplevels.getLength(); i++) {
        	Node node = toplevels.item(i);
        	// find elements with an idref attribute
        	if (node.getNodeType() == Node.ELEMENT_NODE) {
        		if (node.getAttributes().getNamedItem("idref") != null) {
        			String targetID = XMLParser.getAttribute(node, "idref");
        			topNode.removeChild(node);
        			i--;

        			// top-level elements with idref either have an if element containing a condition, like so:
        			// <logger idref="tracelog">
					//     <if cond="inposterior(HKY) and kappa/estimate=true">
					//        <log idref="kappa"/>
					//        <log idref="alpha"/>
					//     </if>
					// </logger>
        			//
        			// or contain elements, each with their own conditions, like so
        			//
					// <logger idref="tracelog">
					//    <log idref="kappa" beauti:if="inposterior(HKY) and kappa/estimate=true"/>
					//    <log idref="alpha" beauti:if="inposterior(HKY) and alpha/estimate=true"/>
					// </logger>
        			//
        			// tedious DOM parsing distinguishing these cases follows...
        			
        			NodeList children = node.getChildNodes();
        			for (int j = 0; j < children.getLength(); j++) {
        				Node child = children.item(j);
        				if (child.getNodeType() == Node.ELEMENT_NODE) {
        					// determine target input name
        					String inputName = child.getNodeName();
        					String name = XMLParser.getAttribute(child, "name");
        					if (name != null) {
        						inputName = name;
        					}
        					if (inputName.equals("if")) {
        						// process if-element e.g.
        						String condition = XMLParser.getAttribute(child, "cond");
        						NodeList childrenOfIf = child.getChildNodes();
        						for (int k = 0; k < childrenOfIf.getLength(); k++) {
        							Node child2 = childrenOfIf.item(k);
        							if (child2.getNodeType() == Node.ELEMENT_NODE) {
        	        					// determine source ID
        	        					boolean hasIDRef = true;
        	        					String sourceID = XMLParser.getAttribute(child2, "idref");
        	        					if (sourceID == null) {
        	        						sourceID = XMLParser.getAttribute(child2, "id");
        	        						hasIDRef = false;
        	        					}
        	        					if (sourceID == null) {
        	        						throw new Exception("idref and id not specified on element with name '" + name +"'");
        	        					}
        	        					inputName = child2.getNodeName();
        	        					String name2 = XMLParser.getAttribute(child2, "name");
        	        					if (name2 != null) {
        	        						inputName = name2;
        	        					}
        	        					BeautiConnector connector = new BeautiConnector(sourceID, targetID, inputName, condition);
        	        					connectorsInput.get().add(connector);

        	        					if (!hasIDRef) {
        	            					topNode.appendChild(child2);
        	            					k--;
        	        					}
        								
        							} else {
    	            					topNode.appendChild(child2);
    	            					k--;
        							}
        							
        						}
        					} else {
	        					// determine source ID
	        					boolean hasIDRef = true;
	        					String sourceID = XMLParser.getAttribute(child, "idref");
	        					if (sourceID == null) {
	        						sourceID = XMLParser.getAttribute(child, "id");
	        						hasIDRef = false;
	        					}
	        					if (sourceID == null) {
	        						throw new Exception("idref and id not specified on element with name '" + name +"'");
	        					}
	        					String condition = XMLParser.getAttribute(child, "beauti:if");
	        					if (condition != null) {
	        						Node ifNode = child.getAttributes().removeNamedItem("beauti:if");
	        					}
	
	        					BeautiConnector connector = new BeautiConnector(sourceID, targetID, inputName, condition);
	        					connectorsInput.get().add(connector);
	        					if (!hasIDRef) {
	            					topNode.appendChild(child);
	            					j--;
	        					}
        					}
        				} else {
        					topNode.appendChild(children.item(j));
        					j--;
        				}
        			}
        		}
        	}
        }
        
    	// translate DOM back to String
        // TODO: move to XMLParserUtils
	    DOMSource domSource = new DOMSource(doc);
	    StringWriter writer = new StringWriter();
	    StreamResult result = new StreamResult(writer);
	    TransformerFactory tf = TransformerFactory.newInstance();
	    Transformer transformer = tf.newTransformer();
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    transformer.transform(domSource, result);
	    String sXML = writer.toString();
	    sXML = sXML.substring(sXML.indexOf("<beast xmlns:beauti=\"http://beast2.org\">") + 40, sXML.lastIndexOf("</beast>"));
	    return sXML;
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
        BEASTInterface plugin = null;
        if (o instanceof BEASTInterface) {
            plugin = (BEASTInterface) o;
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

    public BEASTInterface createSubNet(PartitionContext partition, BEASTInterface plugin, Input<?> input, boolean init) throws Exception {
        removeSubNet(input.get());
        if (sXML == null) {
            // this is the NULL_TEMPLATE
            input.setValue(null, plugin);
            return null;
        }
        BEASTInterface o = createSubNet(partition, doc.pluginmap, init);
        input.setValue(o, plugin);
        return o;
    }

    public BEASTInterface createSubNet(PartitionContext partition, List<BEASTInterface> list, int iItem, boolean init) throws Exception {
        removeSubNet(list.get(iItem));
        if (sXML == null) {
            // this is the NULL_TEMPLATE
            list.set(iItem, null);
            return null;
        }
        BEASTInterface o = createSubNet(partition, doc.pluginmap, init);
        list.set(iItem, o);
        return o;
    }

    public BEASTInterface createSubNet(PartitionContext partition, boolean init) throws Exception {
        if (sXML == null) {
            // this is the NULL_TEMPLATE
            return null;
        }
        BEASTInterface o = createSubNet(partition, doc.pluginmap, init);
        return o;
    }


    BEASTInterface createSubNet(Alignment data, BeautiDoc doc, boolean init) {
        String sPartition = data.getID();
        HashMap<String, BEASTInterface> sIDMap = doc.pluginmap;//new HashMap<>();
        sIDMap.put(sPartition, data);
        return createSubNet(new PartitionContext(sPartition), sIDMap, init);
    }

    private BEASTInterface createSubNet(PartitionContext context, /*BeautiDoc doc,*/ HashMap<String, BEASTInterface> sIDMap, boolean init) {
        // wrap in a beast element with appropriate name spaces
        String _sXML = "<beast version='2.0' \n" +
                "namespace='beast.app.beauti:beast.core:beast.evolution.branchratemodel:beast.evolution.speciation:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood:beast.evolution:beast.math.distributions'>\n" +
                sXML +
                "</beast>\n";

        // resolve alignment references
        _sXML = _sXML.replaceAll("idref=[\"']data['\"]", "idref='" + context.partition + "'");
        _sXML = _sXML.replaceAll("[\"']@data['\"]", "'@" + context.partition + "'");
        // ensure uniqueness of IDs
        _sXML = BeautiDoc.translatePartitionNames(_sXML, context);//_sXML.replaceAll("\\$\\(n\\)", sPartition);

        XMLParser parser = new XMLParser();
        parser.setRequiredInputProvider(doc, context);
        List<BEASTInterface> plugins = null;
        try {
            plugins = parser.parseTemplate(_sXML, sIDMap, true);
            for (BEASTInterface plugin : plugins) {
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
                //System.out.println(connector.sSourceID + " == " + connector.sTargetID);
                if (connector.sTargetID != null && connector.sTargetID.equals("prior")) {
                	Log.warning.println(">>> No description for connector " + connector.sSourceID + " == " + connector.sTargetID);
                }
                if (connector.getTipText() != null) {
                	String ID = BeautiDoc.translatePartitionNames(connector.sSourceID, context);
                	String tipText = BeautiDoc.translatePartitionNames(connector.getTipText(), context).trim().replaceAll("\\s+", " ");
                	//System.out.println(ID + " -> " + tipText);
                    doc.tipTextMap.put(ID, tipText);
                }
            }

            if (suppressedInputs.get() != null) {
                String[] inputs = suppressedInputs.get().split(",");
                for (String input : inputs) {
                    input = input.trim();
                    doc.beautiConfig.suppressPlugins.add(input);
                }
            }

            if (inlineInput.get() != null) {
                String[] inputs = inlineInput.get().split(",");
                for (String input : inputs) {
                    input = input.trim();
                    doc.beautiConfig.inlinePlugins.add(input);
                }
            }

            if (collapsedInput.get() != null) {
                String[] inputs = collapsedInput.get().split(",");
                for (String input : inputs) {
                    input = input.trim();
                    doc.beautiConfig.collapsedPlugins.add(input);
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
        BEASTInterface plugin = doc.pluginmap.get(sID);

        if (this == doc.beautiConfig.partitionTemplate.get()) {
            // HACK: need to make sure the subst model is of the correct type
            BEASTInterface treeLikelihood = doc.pluginmap.get("treeLikelihood." + context.partition);
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
