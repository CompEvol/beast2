/*
* File XMLParser.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is part of BEAST2.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BEAST is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BEAST is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BEAST; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/
package beast.util;


import beast.core.*;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.tree.Tree;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;


/**
 * XMLParser process Beast 2 XML and constructs an MCMC object
 * <p/>
 * <p/>
 * Reserved elements (present in element2class map)
 * <mcmc>
 * <uncertainty>
 * <operator>
 * <log>
 * <data>
 * <sequence>
 * <state>
 * <parameter>
 * <beast.tree>
 * <beast version='2.0' namespace='x.y.z:'>
 * <map name='elementName'>x.y.z.Class</map>
 * <p/>
 * Reserved attributes:
 * <input id='myId' idRef='otherId' name='inputName' spec='x.y.z.MyClass'/>
 * <p/>
 * Resolving class:
 * 1. specified in spec attribute
 * 2. if not, get from element2class map
 * 3. if not, use element name (and hope it shows up in the namespace somewhere).
 * <p/>
 * Resolving name:
 * 1. specified in name attribute
 * 2. if not, use (non-reserved) attribute name
 * 3. if not, use element name
 * 4. if input, use 'value' when there is text content, but no element content
 * <p/>
 * Resolving value:
 * 0. if idref is specified, use the referred object
 * 1. specified in value attribute
 * 2. if not, use value of (non-reserved) attribute
 * 3. if not, use text content when there is text content, but no element content
 * <p/>
 * Parsing rules:
 * <p/>
 * Processing non reserved attributes
 * <input otherAttribute="xyz"/>
 * equals
 * <input>
 * <input name='otherAttribute' value='xyz'/>
 * </input>
 * <p/>
 * Processing non reserved element names
 * <myElement/>
 * ==
 * <input spec='myElement' name='myElement'/>
 * unless 'spec' is a specified attribute, then that overrides, likewise for 'name'
 * <p/>
 * Processing of text content (only when there are no enclosing elements)
 * <input name='data'>xyz</input>
 * ==
 * <input name='data' value='xyz/>
 *
 * @author rrb
 */


public class XMLParserAnalyze {
    final static String DATA_CLASS = Alignment.class.getName();
    final static String SEQUENCE_CLASS = Sequence.class.getName();
    final static String STATE_CLASS = State.class.getName();
    final static String LIKELIHOOD_CLASS = ProbabilityDistribution.class.getName();
    final static String LOG_CLASS = Logger.class.getName();
    final static String MCMC_CLASS = MCMC.class.getName();
    final static String OPERATOR_CLASS = Operator.class.getName();
    final static String PARAMETER_CLASS = Parameter.class.getName();
    final static String PLUGIN_CLASS = Plugin.class.getName();
    final static String INPUT_CLASS = Input.class.getName();
    final static String TREE_CLASS = Tree.class.getName();


    final static String BEAST_ELEMENT = "beast";
    final static String MAP_ELEMENT = "map";
    final static String MCMC_ELEMENT = "mcmc";
    final static String PROBABILITY_ELEMENT = "probabilityDistribution";
    final static String OPERATOR_ELEMENT = "operator";
    final static String INPUT_ELEMENT = "input";
    final static String LOG_ELEMENT = "log";
    final static String DATA_ELEMENT = "data";
    final static String SEQUENCE_ELEMENT = "sequence";
    final static String STATE_ELEMENT = "state";
    final static String TREE_ELEMENT = "tree";
    final static String PARAMETER_ELEMENT = "parameter";
    final static String ANALYZE_ELEMENT = "analyze";


    Plugin m_runnable;
    State m_state;
    /**
     * DOM document representation of XML file *
     */
    Document m_doc;

    /**
     * maps sequence data onto integer value *
     */
    String m_sDataMap;

    HashMap<String, Plugin> m_sIDMap;
    HashMap<String, Integer[]> m_LikelihoodMap;
    HashMap<String, Node> m_sIDNodeMap;

    HashMap<String, String> m_sElement2ClassMap;

    String[] m_sNameSpaces;

    public XMLParserAnalyze() {
        m_sElement2ClassMap = new HashMap<String, String>();
        m_sElement2ClassMap.put(MCMC_ELEMENT, MCMC_CLASS);
        m_sElement2ClassMap.put(PROBABILITY_ELEMENT, LIKELIHOOD_CLASS);
        m_sElement2ClassMap.put(OPERATOR_ELEMENT, OPERATOR_CLASS);
        m_sElement2ClassMap.put(INPUT_ELEMENT, INPUT_CLASS);
        m_sElement2ClassMap.put(LOG_ELEMENT, LOG_CLASS);
        m_sElement2ClassMap.put(DATA_ELEMENT, DATA_CLASS);
        m_sElement2ClassMap.put(STATE_ELEMENT, STATE_CLASS);
        m_sElement2ClassMap.put(SEQUENCE_ELEMENT, SEQUENCE_CLASS);
        m_sElement2ClassMap.put(TREE_ELEMENT, TREE_CLASS);
        m_sElement2ClassMap.put(PARAMETER_ELEMENT, PARAMETER_CLASS);
    }

    public RunnablePlugin parseFile(String sFileName) throws Exception {
        //m_runnable = new MCMC();
        // parse the XML file into a DOM document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //factory.setValidating(true);
        m_doc = factory.newDocumentBuilder().parse(new File(sFileName));
        m_doc.normalize();
        m_sIDMap = new HashMap<String, Plugin>();
        m_LikelihoodMap = new HashMap<String, Integer[]>();
        m_sIDNodeMap = new HashMap<String, Node>();


        parse();
        if(m_runnable instanceof RunnablePlugin)
            return (RunnablePlugin)m_runnable;
        else{
            throw new Exception("Analyze element does not point to a runnable object.");
        }
    } // parseFile


    /**
     * parse BEAST file as DOM document
     */
    public void parse() throws Exception {
        // find top level beast element
        NodeList nodes = m_doc.getElementsByTagName("*");
        if (nodes == null || nodes.getLength() == 0) {
            throw new Exception("Expected top level beast element in XML");
        }
        Node topNode = nodes.item(0);
        double fVersion = getAttributeAsDouble(topNode, "version");
        if (fVersion < 2.0 || fVersion == Double.MAX_VALUE) {
            throw new XMLParserException(topNode, "Wrong version: only versions > 2.0 are supported", 101);
        }

        initIDNodeMap(topNode);
        parseNameSpaceAndMap(topNode);

        //parseState();
        parseAnalyze(topNode);
    } // parse


    /**
     * Traverse DOM beast.tree and grab all nodes that have an 'id' attribute
     * Throw exception when a duplicate id is encountered
     */
    void initIDNodeMap(Node node) throws Exception {
        String sID = getID(node);
        if (sID != null) {
            if (m_sIDNodeMap.containsKey(sID)) {
                throw new XMLParserException(node, "IDs should be unique. Duplicate id '" + sID + "' found", 104);
            }
            m_sIDNodeMap.put(sID, node);
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            initIDNodeMap(children.item(i));
        }
    }

    /**
     * find out namespaces (beast/@namespace attribute)
     * and element to class maps, which reside in beast/map elements
     * <beast version='2.0' namespace='snap:beast.util'>
     * <map name='snapprior'>snap.likelihood.SnAPPrior</map>
     * <map name='snaplikelihood'>snap.likelihood.SnAPTreeLikelihood</map>
     *
     * @throws beast.util.XMLParserException
     */
    void parseNameSpaceAndMap(Node topNode) throws XMLParserException {
        // process namespaces
        if (hasAtt(topNode, "namespace")) {
            String sNameSpce = getAttribute(topNode, "namespace");
            String[] sNameSpaces = sNameSpce.split(":");
            // append dot after every non-zero namespace
            m_sNameSpaces = new String[sNameSpaces.length + 2];
            int i = 0;
            for (String sNameSpace : sNameSpaces) {
                if (sNameSpace.length() > 0) {
                    if (sNameSpace.charAt(sNameSpace.length() - 1) != '.') {
                        sNameSpace += '.';
                    }
                }
                m_sNameSpaces[i++] = sNameSpace;
            }
        } else {
            m_sNameSpaces = new String[2];
        }
        // make sure that the default namespace and beast.core are in there
        m_sNameSpaces[m_sNameSpaces.length -2] = "beast.core.";
        m_sNameSpaces[m_sNameSpaces.length-1] = "";

        // process map elements
        NodeList nodes = m_doc.getElementsByTagName(MAP_ELEMENT);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node child = nodes.item(i);
            String sName = getAttribute(child, "name");
            if (sName == null) {
                throw new XMLParserException(child, "name attribute expected in map element", 300);
            }
            if (m_sElement2ClassMap.containsKey(sName)) {
                throw new XMLParserException(child, "name '" + sName + "' is already defined as " + m_sElement2ClassMap.get(sName), 301);
            }

            // get class
            String sClass = child.getTextContent();
            // remove spaces
            sClass = sClass.replaceAll("\\s", "");
            // go through namespaces in order they are declared to find the correct class
            boolean bDone = false;
            for (String sNameSpace : m_sNameSpaces) {
                try {
                    // sanity check: class should exist
                    if (!bDone && Class.forName(sNameSpace + sClass) != null) {
                        m_sElement2ClassMap.put(sName, sClass);
                        System.err.println(sName + " => " + sNameSpace + sClass);
                        bDone = true;
                    }
                } catch (ClassNotFoundException e) {
                    //System.err.println("Not found " + e.getMessage());
                    // TODO: handle exception
                }
            }
        }
    } // parseNameSpaceAndMap

    void parseAnalyze(Node topNode) throws Exception {
        // find mcmc element
        NodeList nodes = m_doc.getElementsByTagName(ANALYZE_ELEMENT);
        if (nodes.getLength() == 0) {
            throw new XMLParserException(topNode, "Expected analyze element in file", 102);
        }
        if (nodes.getLength() > 1) {
            throw new XMLParserException(topNode, "Expected only one analyze element in file, not " + nodes.getLength(), 103);
        }
        Node analyze = nodes.item(0);

        //if(analyze.getAttributes().getNamedItem("idref")!=null){
        //    analyze = m_sIDNodeMap.get(analyze.getAttributes().getNamedItem("idref").getNodeValue());
        //}
                
        m_runnable = (Plugin) createObject(analyze, analyze.getAttributes().getNamedItem("spec").getNodeValue(), null);

    } // parseRunnable

    @SuppressWarnings("unchecked")
    Plugin createObject(Node node, String sClass, Plugin parent) throws Exception {
        // try the IDMap first
        String sID = getID(node);
        if (sID != null) {
            if (m_sIDMap.containsKey(sID)) {
                Plugin plugin = m_sIDMap.get(sID);
                if (sClass.equals(INPUT_CLASS) || Class.forName(sClass).isInstance(plugin)) {
                    //if ( plugin.getClass().isInstance(Class.forName(sClass))) {
                    return plugin;
                }
                throw new XMLParserException(node, "id=" + sID + ". Expected object of type " + sClass + " instead of " + plugin.getClass().getName(), 105);
            }
        }
        String sIDRef = getIDRef(node);
        if (sIDRef != null) {
            if (m_sIDMap.containsKey(sIDRef)) {
                Plugin plugin = m_sIDMap.get(sIDRef);
                if (sClass.equals(INPUT_CLASS) || Class.forName(sClass).isInstance(plugin)) {
                    return plugin;
                }
                throw new XMLParserException(node, "id=" + sIDRef + ". Expected object of type " + sClass + " instead of " + plugin.getClass().getName(), 106);
            } else if (m_sIDNodeMap.containsKey(sIDRef)) {
                Plugin plugin = createObject(m_sIDNodeMap.get(sIDRef), sClass, parent);
                if (sClass.equals(INPUT_CLASS) || Class.forName(sClass).isInstance(plugin)) {
                    return plugin;
                }
                throw new XMLParserException(node, "id=" + sIDRef + ". Expected object of type " + sClass + " instead of " + plugin.getClass().getName(), 107);
            }
            throw new XMLParserException(node, "Could not find object associated with idref " + sIDRef, 170);
        }
        // it's not in the ID map yet, so we have to create a new object
        String sSpecClass = sClass;
        String sElementName = node.getNodeName();


        if (m_sElement2ClassMap.containsKey(sElementName)) {
            sSpecClass = m_sElement2ClassMap.get(sElementName);
        }
        String sSpec = getAttribute(node, "spec");
        if (sSpec != null) {
            sSpecClass = sSpec;
        }
        Object o = null;
        // try to create object from sSpecName, taking namespaces in account
        try {
            boolean bDone = false;
            for (String sNameSpace : m_sNameSpaces) {
                try {
                    if (!bDone) {
                        o = Class.forName(sNameSpace + sSpecClass).newInstance();
                        bDone = true;
                    }
                } catch (ClassNotFoundException e) {
                    // TODO: handle exception
                }
            }
            if (!bDone) {
                throw new ClassNotFoundException();
            }
            // hack required to make log-parsing easier
            if (o instanceof State) {
                m_state = (State) o;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new XMLParserException(node, "Cannot create class: " + sSpecClass + " " + e.getMessage(), 122);
        }
        // sanity check
        if (!(o instanceof Plugin)) {
            if (o instanceof Input) {
                // if we got this far, it is a basic input,
                // that is, one of the form <input name='xyz'>value</input>
                String sName = getAttribute(node, "name");
                if (sName == null) {
                    sName = "value";
                }
                setInput(node, parent, sName, node.getTextContent());
                return null;
            } else {
                throw new XMLParserException(node, "Expected object to be instance of Plugin", 108);
            }
        }
        // set id
        Plugin plugin = (Plugin) o;
        plugin.setID(sID);
//		if (!Class.forName(sClass).isInstance(plugin)) {
//			throw new XMLParserException(node, "id=" + sID + ". Expected object of type " + sClass + " instead of " + plugin.getClass().getName(), 109);
//		}
        register(node, plugin);
        // process inputs
        parseInputs(plugin, node);
        // initialise
        try {
            plugin.validateInputs();
            plugin.initAndValidate(m_state);
        } catch (Exception e) {
            e.printStackTrace();
            throw new XMLParserException(node, "validate and intialize error: " + e.getMessage(), 110);
        }
        return plugin;
    } // createObject


    void parseInputs(Plugin parent, Node node) throws Exception {
        // first, process attributes
        NamedNodeMap atts = node.getAttributes();
        if (atts != null) {
            for (int i = 0; i < atts.getLength(); i++) {
                String sName = atts.item(i).getNodeName();
                if (!(sName.equals("id") ||
                        sName.equals("idref") ||
                        sName.equals("spec") ||
                        sName.equals("name"))) {
                    String sValue = atts.item(i).getNodeValue();
                    setInput(node, parent, sName, sValue);
                }
            }
        }
        // process element nodes
        NodeList children = node.getChildNodes();
        int nChildElements = 0;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String sElement = child.getNodeName();
//				if (sElement.equals("log")) {
//					parseLog(parent, child);
//				} else {
                // resolve name of the input
                String sName = getAttribute(child, "name");
                if (sName == null) {
                    sName = sElement;
                }
                // resolve base class
                String sClass = PLUGIN_CLASS;
                if (m_sElement2ClassMap.containsKey(sElement)) {
                    sClass = m_sElement2ClassMap.get(sElement);
                }
                Plugin childItem = createObject(child, sClass, parent);
                if (childItem != null) {
                    setInput(node, parent, sName, childItem);
                }
//				}
                nChildElements++;
            }
        }

        if (nChildElements == 0) {
            String sContent = node.getTextContent();
            if (sContent != null && sContent.length() > 0) {
                try {
                    setInput(node, parent, "value", sContent);
                } catch (Exception e) {
                }
            }
        }
    } // setInputs

    void setInput(Node node, Plugin plugin, String sName, Plugin plugin2) throws XMLParserException {
        try {
            plugin.setInputValue(sName, plugin2);
            return;
        } catch (Exception e) {
            throw new XMLParserException(node, e.getMessage(), 123);
        }
        //throw new XMLParserException(node, "no such input '"+sName+"' for element <" + node.getNodeName() + ">", 167);
    }

    void setInput(Node node, Plugin plugin, String sName, String sValue) throws XMLParserException {
        try {

            plugin.setInputValue(sName, sValue);
            return;
        } catch (Exception e) {
            throw new XMLParserException(node, e.getMessage(), 124);
        }
        //throw new XMLParserException(node, "no such input '"+sName+"' for element <" + node.getNodeName() + ">", 168);
    }

    void register(Node node, Plugin plugin) {
        String sID = getID(node);
        if (sID != null) {
            m_sIDMap.put(sID, plugin);
        }
    }

//	/*****************************************************/
//	void parseLog(Plugin parent, Node log) throws Exception  {
//			if (log.getNodeType() == Node.ELEMENT_NODE &&
//					log.getNodeName().equals("log")) {
//				int nEvery = getAttributeAsInt(log, "logEvery");
//				beast.core.Logger pLogger;
//				String sFile = getAttribute(log, "fileName");
//				if (sFile != null) {
//					if (sFile.indexOf("$(seed)") >= 0) {
//						int k = sFile.indexOf("$(seed)");
//						sFile = sFile.substring(0,k) + Randomizer.getSeed() + sFile.substring(k+7);
//					}
//					pLogger = new beast.core.Logger(sFile, nEvery);
//				} else {
//					pLogger = new beast.core.Logger(nEvery);
//				}
//				register(log, pLogger);
//				NodeList loggers = log.getChildNodes();
//				setInput(log, parent, "log", pLogger);
//				for (int j = 0; j < loggers.getLength(); j++) {
//					Node logger = loggers.item(j);
//					if (logger.getNodeType() == Node.ELEMENT_NODE) {
//						String sName = logger.getNodeName();
//						beast.core.Logger newLogger = null;
//						if (sName.equals(PARAMETER_ELEMENT)) {
//							String sIDRef = getIDRef(logger);
//							int iVar = m_state.getStateNodeIndex(sIDRef);
//							newLogger = pLogger.new VarLogger(iVar);
//							pLogger.addLogger(newLogger);
//						} else if (sName.equals(TREE_ELEMENT)) {
//							String sIDRef = getIDRef(logger);
//							int iID = m_state.getTreeIndex(sIDRef);
//							newLogger = pLogger.new TreeLogger(iID);
//							pLogger.addLogger(newLogger);
//						} else if (sName.equals(PROBABILITY_ELEMENT)) {
//							String sIDRef = getIDRef(logger);
//							Likelihood likelihood = null;
//							if (!m_sIDMap.containsKey(sIDRef)) {
//								if (!m_sIDNodeMap.containsKey(sIDRef)) {
//									throw new XMLParserException(logger, "Cannot resolve IDRef " + sIDRef, 166);
//								}
//								likelihood = (Likelihood) createObject(m_sIDNodeMap.get(sIDRef), LIKELIHOOD_CLASS, parent);
//							} else {
//								likelihood = (Likelihood) m_sIDMap.get(sIDRef);
//							}
//							newLogger = pLogger.new LikelihoodLogger(likelihood, sIDRef);
//							pLogger.addLogger(newLogger);
//						} else {
//							// it is a plugin of some sort
//							newLogger = (beast.core.Logger) createObject(logger, LOG_CLASS, pLogger);
//							pLogger.addLogger(newLogger);
//						}
//					}
//				}
//			}
//	} // parseLog

    public static String getID(Node node) { // throws Exception {
        return getAttribute(node, "id");
    } // getID

    public static String getIDRef(Node node) {// throws Exception {
        return getAttribute(node, "idref");
    } // getIDRef

    public static String getAttribute(Node node, String sAttName) { // throws Exception {
        NamedNodeMap atts = node.getAttributes();
        if (atts == null) {
            return null;
        }
        for (int i = 0; i < atts.getLength(); i++) {
            String sName = atts.item(i).getNodeName();
            if (sName.equals(sAttName)) {
                String sValue = atts.item(i).getNodeValue();
                return sValue;
            }
        }
        return null;
    } // getAttribute

    public static int getAttributeAsInt(Node node, String sAttName) { //throws Exception {
        String sAtt = getAttribute(node, sAttName);
        if (sAtt == null) {
            return -1;
        }
        return Integer.parseInt(sAtt);
    }

    public static double getAttributeAsDouble(Node node, String sAttName) { // throws Exception {
        String sAtt = getAttribute(node, sAttName);
        if (sAtt == null) {
            return Double.MAX_VALUE;
        }
        return Double.parseDouble(sAtt);
    }

    boolean hasAtt(Node node, String sAttributeName) {
        NamedNodeMap atts = node.getAttributes();
        if (atts != null) {
            for (int i = 0; i < atts.getLength(); i++) {
                String sName = atts.item(i).getNodeName();
                if (sName.equals(sAttributeName)) {
                    return true;
                }
            }
        }
        return false;
    }

} // Parser