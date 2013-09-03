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


//import beast.core.parameter.IntegerParameter;
//import beast.core.parameter.BooleanParameter;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import beast.app.beauti.PartitionContext;
import beast.core.*;
import beast.core.Input.Validate;
import beast.core.Runnable;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.tree.Tree;


import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * XMLParser process Beast 2 XML and constructs an MCMC object
 * <p/>
 * <p/>
 * Reserved elements (present in element2class map)
 * <distribution>
 * <operator>
 * <logger>
 * <data>
 * <sequence>
 * <state>
 * <parameter>
 * <tree>
 * <beast version='2.0' namespace='x.y.z:'>
 * <map name='elementName'>x.y.z.Class</map>
 * <run>
 * <plate>
 * <input>
 * <p/>
 * Reserved attributes:
 * <input id='myId' idRef='otherId' name='inputName' spec='x.y.z.MyClass'/>
 * <p/>
 * Reserved attribute formats:
 * shortcut for idref inputs
 * <input xyz='@ref'/>
 * ==
 * <input>
 * <input name='xyz' idref='ref'/>
 * </input>
 * <p/>
 * plate notations
 * <plate var='n' range='1,2,3'><xyz id='id$(n)'/></plate>
 * ==
 * <xyz id='id1'/>
 * <xyz id='id2'/>
 * <xyz id='id3'/>
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


public class XMLParser {
    final static String DATA_CLASS = Alignment.class.getName();
    final static String SEQUENCE_CLASS = Sequence.class.getName();
    final static String STATE_CLASS = State.class.getName();
    final static String LIKELIHOOD_CLASS = Distribution.class.getName();
    final static String LOG_CLASS = Logger.class.getName();
    final static String OPERATOR_CLASS = Operator.class.getName();
    final static String REAL_PARAMETER_CLASS = RealParameter.class.getName();
    final static String PLUGIN_CLASS = BEASTObject.class.getName();
    final static String INPUT_CLASS = Input.class.getName();
    final static String TREE_CLASS = Tree.class.getName();
    final static String RUNNABLE_CLASS = Runnable.class.getName();


    /* This is the set of keywords in XML.
* This list should not be added to unless there
* is a very very good reason. */
    final static String BEAST_ELEMENT = "beast";
    final static String MAP_ELEMENT = "map";
    final static String DISTRIBUTION_ELEMENT = "distribution";
    final static String OPERATOR_ELEMENT = "operator";
    final static String INPUT_ELEMENT = "input";
    final static String LOG_ELEMENT = "logger";
    final static String DATA_ELEMENT = "data";
    final static String SEQUENCE_ELEMENT = "sequence";
    final static String STATE_ELEMENT = "state";
    final static String TREE_ELEMENT = "tree";
    final static String REAL_PARAMETER_ELEMENT = "parameter";
    final static String RUN_ELEMENT = "run";
    final static String PLATE_ELEMENT = "plate";

    Runnable m_runnable;
    State m_state;
    /**
     * DOM document representation of XML file *
     */
    Document doc;

    /**
     * maps sequence data onto integer value *
     */
    String m_sDataMap;

    HashMap<String, BEASTObject> IDMap;
    HashMap<String, Integer[]> likelihoodMap;
    HashMap<String, Node> IDNodeMap;

    static HashMap<String, String> element2ClassMap;
    static Set<String> reservedElements;
    static {
        element2ClassMap = new HashMap<String, String>();
        element2ClassMap.put(DISTRIBUTION_ELEMENT, LIKELIHOOD_CLASS);
        element2ClassMap.put(OPERATOR_ELEMENT, OPERATOR_CLASS);
        element2ClassMap.put(INPUT_ELEMENT, INPUT_CLASS);
        element2ClassMap.put(LOG_ELEMENT, LOG_CLASS);
        element2ClassMap.put(DATA_ELEMENT, DATA_CLASS);
        element2ClassMap.put(STATE_ELEMENT, STATE_CLASS);
        element2ClassMap.put(SEQUENCE_ELEMENT, SEQUENCE_CLASS);
        element2ClassMap.put(TREE_ELEMENT, TREE_CLASS);
        element2ClassMap.put(REAL_PARAMETER_ELEMENT, REAL_PARAMETER_CLASS);
        reservedElements = new HashSet<String>();
        for (String element : element2ClassMap.keySet()) {
        	reservedElements.add(element);
        }
    }
    
    List<BEASTObject> pluginsWaitingToInit;
    List<Node> nodesWaitingToInit;

    public HashMap<String, String> getElement2ClassMap() {
        return element2ClassMap;
    }


    String[] m_sNameSpaces;

    /**
     * Flag to indicate initAndValidate should be called after
     * all inputs of a plugin have been parsed
     */
    boolean m_bInitialize = true;

    /**
     * when parsing XML, missing inputs can be assigned default values through
     * a RequiredInputProvider
     */
    RequiredInputProvider requiredInputProvider = null;
    PartitionContext partitionContext = null;

    public XMLParser() {
        pluginsWaitingToInit = new ArrayList<BEASTObject>();
        nodesWaitingToInit = new ArrayList<Node>();
    }

    public Runnable parseFile(File file) throws Exception {
        // parse the XML file into a DOM document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //factory.setValidating(true);
        doc = factory.newDocumentBuilder().parse(file);
        doc.normalize();
        processPlates();

        IDMap = new HashMap<String, BEASTObject>();
        likelihoodMap = new HashMap<String, Integer[]>();
        IDNodeMap = new HashMap<String, Node>();


        parse();
        //assert m_runnable == null || m_runnable instanceof Runnable;
        if (m_runnable != null)
            return m_runnable;
        else {
            throw new Exception("Run element does not point to a runnable object.");
        }
    } // parseFile

    /**
     * extract all elements (runnable or not) from an XML fragment.
     * Useful for retrieving all non-runnable elements when a template
     * is instantiated by Beauti *
     */
    public List<BEASTObject> parseTemplate(String sXML, HashMap<String, BEASTObject> sIDMap, boolean bInitialize) throws Exception {
        m_bInitialize = bInitialize;
        // parse the XML file into a DOM document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //factory.setValidating(true);
        doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(sXML)));
        doc.normalize();
        processPlates();

        IDMap = sIDMap;//new HashMap<String, Plugin>();
        likelihoodMap = new HashMap<String, Integer[]>();
        IDNodeMap = new HashMap<String, Node>();

        List<BEASTObject> plugins = new ArrayList<BEASTObject>();

        // find top level beast element
        NodeList nodes = doc.getElementsByTagName("*");
        if (nodes == null || nodes.getLength() == 0) {
            throw new Exception("Expected top level beast element in XML");
        }
        Node topNode = nodes.item(0);
        // sanity check that we are reading a beast 2 file
        double fVersion = getAttributeAsDouble(topNode, "version");
        if (!topNode.getNodeName().equals(BEAST_ELEMENT) || fVersion < 2.0 || fVersion == Double.MAX_VALUE) {
            return plugins;
        }
        // only process templates
//        String sType = getAttribute(topNode, "type");
//        if (sType == null || !sType.equals("template")) {
//        	return plugins;
//        }


        initIDNodeMap(topNode);
        parseNameSpaceAndMap(topNode);

        NodeList children = topNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Node child = children.item(i);
                System.err.println(child.getNodeName());
                if (!child.getNodeName().equals(MAP_ELEMENT)) {
                    plugins.add(createObject(child, PLUGIN_CLASS, null));
                }
            }
        }
        initPlugins();
        return plugins;
    } // parseTemplate

    private void initPlugins() throws Exception {
    	Node node = null;
        try {
        	for (int i = 0; i < pluginsWaitingToInit.size(); i++) {
        		BEASTObject plugin = pluginsWaitingToInit.get(i);
        		node = nodesWaitingToInit.get(i);
        		plugin.initAndValidate();
        	}
        } catch (Exception e) {
            // next lines for debugging only
            //plugin.validateInputs();
            //plugin.initAndValidate();
            e.printStackTrace();
            throw new XMLParserException(node, "validate and intialize error: " + e.getMessage(), 110);
        }
	}

	/**
     * Expand plates in XML by duplicating the containing XML and replacing
     * the plate variable with the appropriate value.
     */
    void processPlates() {
        // process plate elements
        NodeList nodes = doc.getElementsByTagName(PLATE_ELEMENT);
        // instead of processing all plates, process them one by one,
        // then check recursively for new plates that could have been
        // created when they are nested
        if (nodes.getLength() > 0) {
            Node node = nodes.item(0);
            String sVar = node.getAttributes().getNamedItem("var").getNodeValue();
            String sRange = node.getAttributes().getNamedItem("range").getNodeValue();
            String[] sValues = sRange.split(",");
            for (String sValue : sValues) {
                // copy children
                NodeList children = node.getChildNodes();
                for (int iChild = 0; iChild < children.getLength(); iChild++) {
                    Node child = children.item(iChild);
                    Node newChild = child.cloneNode(true);
                    replace(newChild, sVar, sValue);
                    node.getParentNode().insertBefore(newChild, node);
                }
            }
            node.getParentNode().removeChild(node);
            processPlates();
        }
    } // processPlates

    void replace(Node node, String sVar, String sValue) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            NamedNodeMap atts = element.getAttributes();
            for (int i = 0; i < atts.getLength(); i++) {
                Attr attr = (Attr) atts.item(i);
                if (attr.getValue().indexOf("$(" + sVar + ")") > -1) {
                    String sAtt = attr.getValue();
                    sAtt = sAtt.replaceAll("\\$\\(" + sVar + "\\)", sValue);
                    attr.setNodeValue(sAtt);
                }
            }
        }

        // process children
        NodeList children = node.getChildNodes();
        for (int iChild = 0; iChild < children.getLength(); iChild++) {
            Node child = children.item(iChild);
            replace(child, sVar, sValue);
        }
    } // replace

    /**
     * Parse an XML fragment representing a Plug-in
     * Only the run element or if that does not exist the last child element of
     * the top level <beast> element is considered.
     */
    public BEASTObject parseFragment(String sXML, boolean bInitialize) throws Exception {
        m_bInitialize = bInitialize;
        // parse the XML fragment into a DOM document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(sXML)));
        doc.normalize();
        processPlates();

        IDMap = new HashMap<String, BEASTObject>();
        likelihoodMap = new HashMap<String, Integer[]>();
        IDNodeMap = new HashMap<String, Node>();

        // find top level beast element
        NodeList nodes = doc.getElementsByTagName("*");
        if (nodes == null || nodes.getLength() == 0) {
            throw new Exception("Expected top level beast element in XML");
        }
        Node topNode = nodes.item(0);
        initIDNodeMap(topNode);
        parseNameSpaceAndMap(topNode);

        NodeList children = topNode.getChildNodes();
        if (children.getLength() == 0) {
            throw new Exception("Need at least one child element");
        }
        int i = children.getLength() - 1;
        while (i >= 0 && (children.item(i).getNodeType() != Node.ELEMENT_NODE ||
                !children.item(i).getNodeName().equals("run"))) {
            i--;
        }
        if (i < 0) {
            i = children.getLength() - 1;
            while (i >= 0 && children.item(i).getNodeType() != Node.ELEMENT_NODE) {
                i--;
            }
        }
        if (i < 0) {
            throw new Exception("Need at least one child element");
        }

        BEASTObject plugin = createObject(children.item(i), PLUGIN_CLASS, null);
        initPlugins();
        return plugin;
    } // parseFragment

    /**
     * Parse XML fragment that will be wrapped in a beast element
     * before parsing. This allows for ease of creating Plugin objects,
     * like this:
     * Tree tree = (Tree) new XMLParser().parseBareFragment("<tree spec='beast.util.TreeParser' newick='((1:1,3:1):1,2:2)'/>");
     * to create a simple tree.
     */
    public BEASTObject parseBareFragment(String sXML, boolean bInitialize) throws Exception {
        // get rid of XML processing instruction
        sXML = sXML.replaceAll("<\\?xml[^>]*>", "");
        if (sXML.indexOf("<beast") > -1) {
            return parseFragment(sXML, bInitialize);
        } else {
            return parseFragment("<beast>" + sXML + "</beast>", bInitialize);
        }
    }

    public List<BEASTObject> parseBareFragments(String sXML, boolean bInitialize) throws Exception {
        m_bInitialize = bInitialize;
        // parse the XML fragment into a DOM document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(sXML)));
        doc.normalize();
        processPlates();

        // find top level beast element
        NodeList nodes = doc.getElementsByTagName("*");
        if (nodes == null || nodes.getLength() == 0) {
            throw new Exception("Expected top level beast element in XML");
        }
        Node topNode = nodes.item(0);
        initIDNodeMap(topNode);
        parseNameSpaceAndMap(topNode);

        NodeList children = topNode.getChildNodes();
        List<BEASTObject> plugins = new ArrayList<BEASTObject>();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                BEASTObject plugin = createObject(children.item(i), PLUGIN_CLASS, null);
                plugins.add(plugin);
            }
        }
        initPlugins();
        return plugins;
    }

    /**
     * parse BEAST file as DOM document
     *
     * @throws Exception
     */
    public void parse() throws Exception {
        // find top level beast element
        NodeList nodes = doc.getElementsByTagName("*");
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
        parseRunElement(topNode);
        initPlugins();
    } // parse


    /**
     * Traverse DOM beast.tree and grab all nodes that have an 'id' attribute
     * Throw exception when a duplicate id is encountered
     *
     * @param node
     * @throws Exception
     */
    void initIDNodeMap(Node node) throws Exception {
        String sID = getID(node);
        if (sID != null) {
            if (IDNodeMap.containsKey(sID)) {
                throw new XMLParserException(node, "IDs should be unique. Duplicate id '" + sID + "' found", 104);
            }
            IDNodeMap.put(sID, node);
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
     * @param topNode
     * @throws XMLParserException
     */
    void parseNameSpaceAndMap(Node topNode) throws XMLParserException {
        // process namespaces
        if (hasAtt(topNode, "namespace")) {
            String sNameSpace = getAttribute(topNode, "namespace");
            setNameSpace(sNameSpace);
        } else {
            // make sure that the default namespace is in there
            if (m_sNameSpaces == null) {
                m_sNameSpaces = new String[1];
                m_sNameSpaces[0] = "";
            }
        }

        // process map elements
        NodeList nodes = doc.getElementsByTagName(MAP_ELEMENT);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node child = nodes.item(i);
            String sName = getAttribute(child, "name");
            if (sName == null) {
                throw new XMLParserException(child, "name attribute expected in map element", 300);
            }
            if (!element2ClassMap.containsKey(sName)) {
//                throw new XMLParserException(child, "name '" + sName + "' is already defined as " + m_sElement2ClassMap.get(sName), 301);
//            }

	            // get class
	            String sClass = child.getTextContent();
	            // remove spaces
	            sClass = sClass.replaceAll("\\s", "");
	            //sClass = sClass.replaceAll("beast", "yabby");
	            // go through namespaces in order they are declared to find the correct class
	            boolean bDone = false;
	            for (String sNameSpace : m_sNameSpaces) {
	                try {
	                    // sanity check: class should exist
	                    if (!bDone && Class.forName(sNameSpace + sClass) != null) {
	                        element2ClassMap.put(sName, sClass);
	                        System.err.println(sName + " => " + sNameSpace + sClass);
	                        String reserved = getAttribute(child, "reserved"); 
	                        if (reserved != null && reserved.toLowerCase().equals("true")) {
	                        	reservedElements.add(sName);
	                        }
	
	                        bDone = true;
	                    }
	                } catch (ClassNotFoundException e) {
	                    //System.err.println("Not found " + e.getMessage());
	                    // TODO: handle exception
	                }
	            }
            }
        }
    } // parseNameSpaceAndMap

    public void setNameSpace(String sNameSpaceStr) {
        String[] sNameSpaces = sNameSpaceStr.split(":");
        // append dot after every non-zero namespace
        m_sNameSpaces = new String[sNameSpaces.length + 1];
        int i = 0;
        for (String sNameSpace : sNameSpaces) {
            if (sNameSpace.length() > 0) {
                if (sNameSpace.charAt(sNameSpace.length() - 1) != '.') {
                    sNameSpace += '.';
                }
            }
            m_sNameSpaces[i++] = sNameSpace;
        }
        // make sure that the default namespace is in there
        m_sNameSpaces[i] = "";
    }

    void parseRunElement(Node topNode) throws Exception {
        // find mcmc element
        NodeList nodes = doc.getElementsByTagName(RUN_ELEMENT);
        if (nodes.getLength() == 0) {
            throw new XMLParserException(topNode, "Expected run element in file", 102);
        }
        if (nodes.getLength() > 1) {
            throw new XMLParserException(topNode, "Expected only one mcmc element in file, not " + nodes.getLength(), 103);
        }
        Node mcmc = nodes.item(0);

        m_runnable = (Runnable) createObject(mcmc, RUNNABLE_CLASS, null);
    } // parseMCMC

    /**
     * Check that plugin is a class that is assignable to class with name sClass.
     * This involves a parameter clutch to deal with non-real parameters.
     * This needs a bit of work, obviously...
     */
    boolean checkType(String sClass, BEASTObject plugin) throws Exception {
        if (sClass.equals(INPUT_CLASS) || Class.forName(sClass).isInstance(plugin)) {
            return true;
        }
        // parameter clutch
        if (sClass.equals(RealParameter.class.getName()) && plugin instanceof Parameter<?>) {
            return true;
        }
        return false;
    } // checkType

    BEASTObject createObject(Node node, String sClass, BEASTObject parent) throws Exception {
    	//sClass = sClass.replaceAll("beast", "yabby");
        // try the IDMap first
        String sID = getID(node);

        if (sID != null) {
            if (IDMap.containsKey(sID)) {
                BEASTObject plugin = IDMap.get(sID);
                if (checkType(sClass, plugin)) {
                    return plugin;
                }
                throw new XMLParserException(node, "id=" + sID + ". Expected object of type " + sClass + " instead of " + plugin.getClass().getName(), 105);
            }
        }

        String sIDRef = getIDRef(node);
        if (sIDRef != null) {
            // produce warning if there are other attributes than idref
            if (node.getAttributes().getLength() > 1) {
                // check if there are just 2 attributes and other attribute is 'name'
            	int idOffset = (getAttribute(node, "id") == null? 0: 1);
                if (node.getAttributes().getLength() > 1 + idOffset || 
                		(node.getAttributes().getLength() == 2 + idOffset && getAttribute(node, "name") == null)) {
                    Log.warning.println("Element " + node.getNodeName() + " found with idref='" + sIDRef + "'. All other attributes are ignored.\n");
                }
            }
            if (IDMap.containsKey(sIDRef)) {
                BEASTObject plugin = IDMap.get(sIDRef);
                if (checkType(sClass, plugin)) {
                    return plugin;
                }
                throw new XMLParserException(node, "id=" + sIDRef + ". Expected object of type " + sClass + " instead of " + plugin.getClass().getName(), 106);
            } else if (IDNodeMap.containsKey(sIDRef)) {
                BEASTObject plugin = createObject(IDNodeMap.get(sIDRef), sClass, parent);
                if (checkType(sClass, plugin)) {
                    return plugin;
                }
                throw new XMLParserException(node, "id=" + sIDRef + ". Expected object of type " + sClass + " instead of " + plugin.getClass().getName(), 107);
            }
            throw new XMLParserException(node, "Could not find object associated with idref " + sIDRef, 170);
        }
        // it's not in the ID map yet, so we have to create a new object
        String sSpecClass = sClass;
        String sElementName = node.getNodeName();


        if (element2ClassMap.containsKey(sElementName)) {
            sSpecClass = element2ClassMap.get(sElementName);
        }
        String sSpec = getAttribute(node, "spec");
        if (sSpec != null) {
            sSpecClass = sSpec;
        }
    	//sSpecClass = sSpecClass.replaceAll("beast", "yabby");
    	
    	if (sSpecClass.indexOf("BEASTObject") > 0) {
    		System.out.println(sSpecClass);
    	}

    	Object o = null;
        // try to create object from sSpecName, taking namespaces in account
        try {
            boolean bDone = false;
            for (String sNameSpace : m_sNameSpaces) {
                try {
                    if (!bDone) {
                    	//sNameSpace = sNameSpace.replaceAll("beast", "yabby");
                        o = Class.forName(sNameSpace + sSpecClass).newInstance();
                        bDone = true;
                    }
                } catch (InstantiationException e) {
                    // we only get here when the class exists, but cannot be created
                    // for instance because it is abstract
                    throw new Exception("Cannot instantiate class (" + sSpecClass + "). Please check the spec attribute.");
                } catch (ClassNotFoundException e) {
                    // TODO: handle exception
                }
            }
            if (!bDone) {
                throw new Exception("Class could not be found. Did you mean " + guessClass(sSpecClass) + "?");
                //throw new ClassNotFoundException(sSpecClass);
            }
            // hack required to make log-parsing easier
            if (o instanceof State) {
                m_state = (State) o;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new XMLParserException(node, "Cannot create class: " + sSpecClass + ". " + e.getMessage(), 122);
        }
        // sanity check
        if (!(o instanceof BEASTObject)) {
            if (o instanceof Input) {
                // if we got this far, it is a basic input,
                // that is, one of the form <input name='xyz'>value</input>
                String sName = getAttribute(node, "name");
                if (sName == null) {
                    sName = "value";
                }
                String sText = node.getTextContent();
                if (sText.length() > 0) {
                    setInput(node, parent, sName, sText);
                }
                return null;
            } else {
                throw new XMLParserException(node, "Expected object to be instance of Plugin", 108);
            }
        }
        // set id
        BEASTObject plugin = (BEASTObject) o;
        plugin.setID(sID);
        register(node, plugin);
        // process inputs
        parseInputs(plugin, node);
        // initialise
        if (m_bInitialize) {
            try {
                plugin.validateInputs();
                pluginsWaitingToInit.add(plugin);
                nodesWaitingToInit.add(node);
                //plugin.initAndValidate();
            } catch (Exception e) {
                // next lines for debugging only
                //plugin.validateInputs();
                //plugin.initAndValidate();
                e.printStackTrace();
                throw new XMLParserException(node, "validate and intialize error: " + e.getMessage(), 110);
            }
        }
        return plugin;
    } // createObject

    /**
     * find closest matching class to named class *
     */
    String guessClass(String sClass) {
        String sName = sClass;
        if (sClass.contains(".")) {
            sName = sClass.substring(sClass.lastIndexOf('.') + 1);
        }
        List<String> sPluginNames = AddOnManager.find(beast.core.BEASTObject.class, AddOnManager.IMPLEMENTATION_DIR);
        int nBestDistance = Integer.MAX_VALUE;
        String sClosest = null;
        for (String sPlugin : sPluginNames) {
            String sClassName = sPlugin.substring(sPlugin.lastIndexOf('.') + 1);
            int nDistance = getLevenshteinDistance(sName, sClassName);


            if (nDistance < nBestDistance) {
                nBestDistance = nDistance;
                sClosest = sPlugin;
            }
        }
        return sClosest;
    }


    /**
     * Compute edit distance between two strings = Levenshtein distance *
     */
    public static int getLevenshteinDistance(String s, String t) {
        if (s == null || t == null) {
            throw new IllegalArgumentException("Strings must not be null");
        }

        int n = s.length(); // length of s
        int m = t.length(); // length of t

        if (n == 0) {
            return m;
        } else if (m == 0) {
            return n;
        }

        int p[] = new int[n + 1]; //'previous' cost array, horizontally
        int d[] = new int[n + 1]; // cost array, horizontally
        int _d[]; //placeholder to assist in swapping p and d

        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t
        char t_j; // jth character of t
        int cost; // cost
        for (i = 0; i <= n; i++) {
            p[i] = i;
        }
        for (j = 1; j <= m; j++) {
            t_j = t.charAt(j - 1);
            d[0] = j;
            for (i = 1; i <= n; i++) {
                cost = s.charAt(i - 1) == t_j ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
            }
            // copy current distance counts to 'previous row' distance counts
            _d = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];
    }

    void parseInputs(BEASTObject parent, Node node) throws Exception {
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
                    if (sValue.startsWith("@")) {
                        String sIDRef = sValue.substring(1);
                        Element element = doc.createElement("input");
                        element.setAttribute("idref", sIDRef);
                        // add child in case things go belly up, and an XMLParserException is thrown
                        node.appendChild(element);
                        BEASTObject plugin = createObject(element, PLUGIN_CLASS, parent);
                        // it is save to remove the elment now
                        node.removeChild(element);
                        setInput(node, parent, sName, plugin);
                    } else {
                        setInput(node, parent, sName, sValue);
                    }
                }
            }
        }
        // process element nodes
        NodeList children = node.getChildNodes();
        int nChildElements = 0;
        String sText = "";
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String sElement = child.getNodeName();
                // resolve name of the input
                String sName = getAttribute(child, "name");
                if (sName == null) {
                    sName = sElement;
                }
                // resolve base class
                String sClass = PLUGIN_CLASS;
                if (element2ClassMap.containsKey(sElement)) {
                    sClass = element2ClassMap.get(sElement);
                }
                BEASTObject childItem = createObject(child, sClass, parent);
                if (childItem != null) {
                    setInput(node, parent, sName, childItem);
                }
                nChildElements++;
            } else if (child.getNodeType() == Node.CDATA_SECTION_NODE ||
                    child.getNodeType() == Node.TEXT_NODE) {
                sText += child.getTextContent();
            }
        }
        if (!sText.matches("\\s*")) {
            setInput(node, parent, "value", sText);
        }

        if (nChildElements == 0) {
            String sContent = node.getTextContent();
            if (sContent != null && sContent.length() > 0 && sContent.replaceAll("\\s", "").length() > 0) {
                try {
                    setInput(node, parent, "value", sContent);
                } catch (Exception e) {
                    //
                }
            }
        }

        // fill in missing inputs, if an input provider is available
        if (requiredInputProvider != null) {
            for (Input<?> input : parent.listInputs()) {
                if (input.get() == null && input.getRule() == Validate.REQUIRED) {
                    Object o = requiredInputProvider.createInput(parent, input, partitionContext);
                    if (o != null) {
                        input.setValue(o, parent);
                    }
                }
            }
        }
    } // setInputs

    void setInput(Node node, BEASTObject plugin, String sName, BEASTObject plugin2) throws XMLParserException {
        try {
            Input<?> input = plugin.getInput(sName);
            // test whether input was not set before, this is done by testing whether input has default value.
            // for non-list inputs, this should be true if the value was not already set before
            // for list inputs this is always true.
            if (input.get() == input.defaultValue) {
                plugin.setInputValue(sName, plugin2);
            } else {
                throw new Exception("Multiple entries for non-list input " + input.getName());
            }
            return;
        } catch (Exception e) {
        	if (sName.equals("xml:base")) {
        		// ignore xml:base attributes introduces by XML entities
        		return;
        	}
            if (e.getMessage().contains("101")) {
                String sType = "?";
                try {
                    sType = plugin.getInput(sName).getType().getName().replaceAll(".*\\.", "");
                } catch (Exception e2) {
                    // TODO: handle exception
                }
                throw new XMLParserException(node, e.getMessage() +
                        " expected '" + sType +
                        "' but got '" + plugin2.getClass().getName().replaceAll(".*\\.", "") + "'"
                        , 123);
            } else {
                throw new XMLParserException(node, e.getMessage(), 130);
            }
        }
        //throw new XMLParserException(node, "no such input '"+sName+"' for element <" + node.getNodeName() + ">", 167);
    }

    void setInput(Node node, BEASTObject plugin, String sName, String sValue) throws XMLParserException {
        try {
            plugin.setInputValue(sName, sValue);
            return;
        } catch (Exception e) {
        	if (sName.equals("xml:base")) {
        		// ignore xml:base attributes introduces by XML entities
        		return;
        	}
            try {
				plugin.setInputValue(sName, sValue);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            throw new XMLParserException(node, e.getMessage(), 124);
        }
        //throw new XMLParserException(node, "no such input '"+sName+"' for element <" + node.getNodeName() + ">", 168);
    }

    /**
     * records id in IDMap, for ease of retrieving Plugins associated with idrefs *
     */
    void register(Node node, BEASTObject plugin) {
        String sID = getID(node);
        if (sID != null) {
            IDMap.put(sID, plugin);
        }
    }

    public static String getID(Node node) { // throws Exception {
        return getAttribute(node, "id");
    } // getID

    public static String getIDRef(Node node) {// throws Exception {
        return getAttribute(node, "idref");
    } // getIDRef

    /**
     * get string value of attribute with given name
     * as opposed to double or integer value (see methods below) *
     */
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

    /**
     * get integer value of attribute with given name *
     */
    public static int getAttributeAsInt(Node node, String sAttName) { //throws Exception {
        String sAtt = getAttribute(node, sAttName);
        if (sAtt == null) {
            return -1;
        }
        return Integer.parseInt(sAtt);
    }

    /**
     * get double value of attribute with given name *
     */
    public static double getAttributeAsDouble(Node node, String sAttName) { // throws Exception {
        String sAtt = getAttribute(node, sAttName);
        if (sAtt == null) {
            return Double.MAX_VALUE;
        }
        return Double.parseDouble(sAtt);
    }

    /**
     * test whether a node contains a attribute with given name *
     */
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

    public interface RequiredInputProvider {
		Object createInput(BEASTObject plugin, Input<?> input, PartitionContext context);
    }

    public void setRequiredInputProvider(RequiredInputProvider provider, PartitionContext context) {
        requiredInputProvider = provider;
        partitionContext = context;
    }

    /**
     * parses file and formats it using the XMLProducer *
     */
    public static void main(String[] args) {
        try {
            // redirect stdout to stderr
            PrintStream out = System.out;
            System.setOut(System.err);
            // parse the file
            XMLParser parser = new XMLParser();
            BEASTObject plugin = parser.parseFile(new File(args[0]));
            // restore stdout
            System.setOut(out);
            System.out.println(new XMLProducer().toXML(plugin));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


} // classXMLParser
