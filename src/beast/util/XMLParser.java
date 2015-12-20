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


import static beast.util.XMLParserUtils.processPlates;
import static beast.util.XMLParserUtils.replaceVariable;

import java.io.File;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import beast.app.beauti.PartitionContext;
import beast.core.BEASTInterface;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Logger;
import beast.core.Operator;
import beast.core.Runnable;
import beast.core.State;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.tree.Tree;


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
    final static String PLUGIN_CLASS = BEASTInterface.class.getName();
    final static String INPUT_CLASS = Input.class.getName();
    final static String TREE_CLASS = Tree.class.getName();
    final static String RUNNABLE_CLASS = Runnable.class.getName();


    /* This is the set of keywords in XML.
* This list should not be added to unless there
* is a very very good reason. */
    public final static String BEAST_ELEMENT = "beast";
    public final static String MAP_ELEMENT = "map";
    public final static String DISTRIBUTION_ELEMENT = "distribution";
    public final static String OPERATOR_ELEMENT = "operator";
    public final static String INPUT_ELEMENT = "input";
    public final static String LOG_ELEMENT = "logger";
    public final static String DATA_ELEMENT = "data";
    public final static String SEQUENCE_ELEMENT = "sequence";
    public final static String STATE_ELEMENT = "state";
    public final static String TREE_ELEMENT = "tree";
    public final static String REAL_PARAMETER_ELEMENT = "parameter";
    public final static String RUN_ELEMENT = "run";
    public final static String PLATE_ELEMENT = "plate";

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

    HashMap<String, BEASTInterface> IDMap;
    HashMap<String, Integer[]> likelihoodMap;
    HashMap<String, Node> IDNodeMap;

    static HashMap<String, String> element2ClassMap;
    static Set<String> reservedElements;
    static {
        element2ClassMap = new HashMap<>();
        element2ClassMap.put(DISTRIBUTION_ELEMENT, LIKELIHOOD_CLASS);
        element2ClassMap.put(OPERATOR_ELEMENT, OPERATOR_CLASS);
        element2ClassMap.put(INPUT_ELEMENT, INPUT_CLASS);
        element2ClassMap.put(LOG_ELEMENT, LOG_CLASS);
        element2ClassMap.put(DATA_ELEMENT, DATA_CLASS);
        element2ClassMap.put(STATE_ELEMENT, STATE_CLASS);
        element2ClassMap.put(SEQUENCE_ELEMENT, SEQUENCE_CLASS);
        element2ClassMap.put(TREE_ELEMENT, TREE_CLASS);
        element2ClassMap.put(REAL_PARAMETER_ELEMENT, REAL_PARAMETER_CLASS);
        reservedElements = new HashSet<>();
        for (final String element : element2ClassMap.keySet()) {
        	reservedElements.add(element);
        }
    }
    
    List<BEASTInterface> beastObjectsWaitingToInit;
    List<Node> nodesWaitingToInit;

    public HashMap<String, String> getElement2ClassMap() {
        return element2ClassMap;
    }


    String[] nameSpaces;

    /**
     * Flag to indicate initAndValidate should be called after
     * all inputs of a beast object have been parsed
     */
    boolean needsInitialisation = true;

    /**
     * when parsing XML, missing inputs can be assigned default values through
     * a RequiredInputProvider
     */
    RequiredInputProvider requiredInputProvider = null;
    PartitionContext partitionContext = null;

    public XMLParser() {
        beastObjectsWaitingToInit = new ArrayList<>();
        nodesWaitingToInit = new ArrayList<>();
    }

    public Runnable parseFile(final File file) throws Exception {
        // parse the XML file into a DOM document
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //factory.setValidating(true);
        doc = factory.newDocumentBuilder().parse(file);
        doc.normalize();
        processPlates(doc,PLATE_ELEMENT);

        // Substitute occurrences of "$(filebase)" with name of file 
        int pointIdx = file.getName().lastIndexOf('.');
        String baseName = pointIdx<0 ? file.getName() : file.getName().substring(0, pointIdx);
        if (doc.getElementsByTagName(BEAST_ELEMENT).item(0) == null) {
        	Log.err.println("Incorrect XML: Could not find 'beast' element in file " + file.getName());
        	throw new RuntimeException();
        }
        replaceVariable(doc.getElementsByTagName(BEAST_ELEMENT).item(0), "filebase", baseName);

        // Substitute occurrences of "$(seed)" with RNG seed
        replaceVariable(doc.getElementsByTagName(BEAST_ELEMENT).item(0), "seed",
                String.valueOf(Randomizer.getSeed()));
        
        IDMap = new HashMap<>();
        likelihoodMap = new HashMap<>();
        IDNodeMap = new HashMap<>();

        
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
    public List<BEASTInterface> parseTemplate(final String sXML, final HashMap<String, BEASTInterface> sIDMap, final boolean bInitialize) throws Exception {
        needsInitialisation = bInitialize;
        // parse the XML file into a DOM document
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //factory.setValidating(true);
        doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(sXML)));
        doc.normalize();
        processPlates(doc,PLATE_ELEMENT);
        
        //XMLParserUtils.saveDocAsXML(doc, "/tmp/beast2.xml");

        IDMap = sIDMap;//new HashMap<>();
        likelihoodMap = new HashMap<>();
        IDNodeMap = new HashMap<>();

        final List<BEASTInterface> beastObjects = new ArrayList<>();

        // find top level beast element
        final NodeList nodes = doc.getElementsByTagName("*");
        if (nodes == null || nodes.getLength() == 0) {
            throw new Exception("Expected top level beast element in XML");
        }
        final Node topNode = nodes.item(0);
        // sanity check that we are reading a beast 2 file
        final double fVersion = getAttributeAsDouble(topNode, "version");
        if (!topNode.getNodeName().equals(BEAST_ELEMENT) || fVersion < 2.0 || fVersion == Double.MAX_VALUE) {
            return beastObjects;
        }
        // only process templates
//        String sType = getAttribute(topNode, "type");
//        if (sType == null || !sType.equals("template")) {
//        	return beastObjects;
//        }


        initIDNodeMap(topNode);
        parseNameSpaceAndMap(topNode);

        final NodeList children = topNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                final Node child = children.item(i);
                System.err.println(child.getNodeName());
                if (!child.getNodeName().equals(MAP_ELEMENT)) {
                    beastObjects.add(createObject(child, PLUGIN_CLASS, null));
                }
            }
        }
        initPlugins();
        return beastObjects;
    } // parseTemplate

    private void initPlugins() throws Exception {
    	Node node = null;
        try {
        	for (int i = 0; i < beastObjectsWaitingToInit.size(); i++) {
        		final BEASTInterface beastObject = beastObjectsWaitingToInit.get(i);
        		node = nodesWaitingToInit.get(i);
        		beastObject.initAndValidate();
        	}
        } catch (Exception e) {
            // next lines for debugging only
            //beastObject.validateInputs();
            //beastObject.initAndValidate();
            e.printStackTrace();
            throw new XMLParserException(node, "validate and intialize error: " + e.getMessage(), 110);
        }
	}

    /**
     * Parse an XML fragment representing a Plug-in
     * Only the run element or if that does not exist the last child element of
     * the top level <beast> element is considered.
     */
    public BEASTInterface parseFragment(final String sXML, final boolean bInitialize) throws Exception {
        needsInitialisation = bInitialize;
        // parse the XML fragment into a DOM document
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(sXML)));
        doc.normalize();
        processPlates(doc,PLATE_ELEMENT);

        IDMap = new HashMap<>();
        likelihoodMap = new HashMap<>();
        IDNodeMap = new HashMap<>();

        // find top level beast element
        final NodeList nodes = doc.getElementsByTagName("*");
        if (nodes == null || nodes.getLength() == 0) {
            throw new Exception("Expected top level beast element in XML");
        }
        final Node topNode = nodes.item(0);
        initIDNodeMap(topNode);
        parseNameSpaceAndMap(topNode);

        final NodeList children = topNode.getChildNodes();
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

        final BEASTInterface beastObject = createObject(children.item(i), PLUGIN_CLASS, null);
        initPlugins();
        return beastObject;
    } // parseFragment

    /**
     * Parse XML fragment that will be wrapped in a beast element
     * before parsing. This allows for ease of creating Plugin objects,
     * like this:
     * Tree tree = (Tree) new XMLParser().parseBareFragment("<tree spec='beast.util.TreeParser' newick='((1:1,3:1):1,2:2)'/>");
     * to create a simple tree.
     */
    public BEASTInterface parseBareFragment(String sXML, final boolean bInitialize) throws Exception {
        // get rid of XML processing instruction
        sXML = sXML.replaceAll("<\\?xml[^>]*>", "");
        if (sXML.contains("<beast")) {
            return parseFragment(sXML, bInitialize);
        } else {
            return parseFragment("<beast>" + sXML + "</beast>", bInitialize);
        }
    }

    public List<BEASTInterface> parseBareFragments(final String sXML, final boolean bInitialize) throws Exception {
        needsInitialisation = bInitialize;
        // parse the XML fragment into a DOM document
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(sXML)));
        doc.normalize();
        processPlates(doc,PLATE_ELEMENT);

        // find top level beast element
        final NodeList nodes = doc.getElementsByTagName("*");
        if (nodes == null || nodes.getLength() == 0) {
            throw new Exception("Expected top level beast element in XML");
        }
        final Node topNode = nodes.item(0);
        initIDNodeMap(topNode);
        parseNameSpaceAndMap(topNode);

        final NodeList children = topNode.getChildNodes();
        final List<BEASTInterface> beastObjects = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                final BEASTInterface beastObject = createObject(children.item(i), PLUGIN_CLASS, null);
                beastObjects.add(beastObject);
            }
        }
        initPlugins();
        return beastObjects;
    }

    /**
     * parse BEAST file as DOM document
     *
     * @throws Exception
     */
    public void parse() throws Exception {
        // find top level beast element
        final NodeList nodes = doc.getElementsByTagName("*");
        if (nodes == null || nodes.getLength() == 0) {
            throw new Exception("Expected top level beast element in XML");
        }
        final Node topNode = nodes.item(0);
        final double fVersion = getAttributeAsDouble(topNode, "version");
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
    void initIDNodeMap(final Node node) throws Exception {
        final String sID = getID(node);
        if (sID != null) {
            if (IDNodeMap.containsKey(sID)) {
                throw new XMLParserException(node, "IDs should be unique. Duplicate id '" + sID + "' found", 104);
            }
            IDNodeMap.put(sID, node);
        }
        final NodeList children = node.getChildNodes();
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
    void parseNameSpaceAndMap(final Node topNode) throws XMLParserException {
        // process namespaces
        if (hasAtt(topNode, "namespace")) {
            final String sNameSpace = getAttribute(topNode, "namespace");
            setNameSpace(sNameSpace);
        } else {
            // make sure that the default namespace is in there
            if (this.nameSpaces == null) {
                this.nameSpaces = new String[1];
                this.nameSpaces[0] = "";
            }
        }

        // process map elements
        final NodeList nodes = doc.getElementsByTagName(MAP_ELEMENT);
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node child = nodes.item(i);
            final String name = getAttribute(child, "name");
            if (name == null) {
                throw new XMLParserException(child, "name attribute expected in map element", 300);
            }
            if (!element2ClassMap.containsKey(name)) {
//                throw new XMLParserException(child, "name '" + sName + "' is already defined as " + m_sElement2ClassMap.get(sName), 301);
//            }

	            // get class
	            String clazz = child.getTextContent();
	            // remove spaces
	            clazz = clazz.replaceAll("\\s", "");
	            //sClass = sClass.replaceAll("beast", "yabby");
	            // go through namespaces in order they are declared to find the correct class
	            boolean isDone = false;
	            for (final String nameSpace : this.nameSpaces) {
	                try {
	                    // sanity check: class should exist
	                    if (!isDone && Class.forName(nameSpace + clazz) != null) {
	                        element2ClassMap.put(name, clazz);
	                        Log.debug.println(name + " => " + nameSpace + clazz);
	                        final String reserved = getAttribute(child, "reserved");
	                        if (reserved != null && reserved.toLowerCase().equals("true")) {
	                        	reservedElements.add(name);
	                        }
	
	                        isDone = true;
	                    }
	                } catch (ClassNotFoundException e) {
	                    //System.err.println("Not found " + e.getMessage());
	                    // TODO: handle exception
	                }
	            }
            }
        }
    } // parseNameSpaceAndMap

    public void setNameSpace(final String nameSpaceStr) {
        final String[] nameSpaces = nameSpaceStr.split(":");
        // append dot after every non-zero namespace
        this.nameSpaces = new String[nameSpaces.length + 1];
        int i = 0;
        for (String nameSpace : nameSpaces) {
            nameSpace = nameSpace.trim();
            if (nameSpace.length() > 0) {
                if (nameSpace.charAt(nameSpace.length() - 1) != '.') {
                    nameSpace += '.';
                }
            }
            this.nameSpaces[i++] = nameSpace;
        }
        // make sure that the default namespace is in there
        this.nameSpaces[i] = "";
    }

    void parseRunElement(final Node topNode) throws Exception {
        // find mcmc element
        final NodeList nodes = doc.getElementsByTagName(RUN_ELEMENT);
        if (nodes.getLength() == 0) {
            throw new XMLParserException(topNode, "Expected run element in file", 102);
        }
        if (nodes.getLength() > 1) {
            throw new XMLParserException(topNode, "Expected only one mcmc element in file, not " + nodes.getLength(), 103);
        }
        final Node mcmc = nodes.item(0);

        m_runnable = (Runnable) createObject(mcmc, RUNNABLE_CLASS, null);
    } // parseMCMC

    /**
     * Check that beast object is a class that is assignable to class with name sClass.
     * This involves a parameter clutch to deal with non-real parameters.
     * This needs a bit of work, obviously...
     */
    boolean checkType(final String sClass, final BEASTInterface beastObject) throws Exception {
        if (sClass.equals(INPUT_CLASS) || Class.forName(sClass).isInstance(beastObject)) {
            return true;
        }
        // parameter clutch
        if (sClass.equals(RealParameter.class.getName()) && beastObject instanceof Parameter<?>) {
            return true;
        }
        return false;
    } // checkType

    BEASTInterface createObject(final Node node, final String classname, final BEASTInterface parent) throws Exception {
        // try the IDMap first
        final String id = getID(node);

        if (id != null) {
            if (IDMap.containsKey(id)) {
                final BEASTInterface beastObject = IDMap.get(id);
                if (checkType(classname, beastObject)) {
                    return beastObject;
                }
                throw new XMLParserException(node, "id=" + id + ". Expected object of type " + classname + " instead of " + beastObject.getClass().getName(), 105);
            }
        }

        final String sIDRef = getIDRef(node);
        if (sIDRef != null) {
            // produce warning if there are other attributes than idref
            if (node.getAttributes().getLength() > 1) {
                // check if there are just 2 attributes and other attribute is 'name' and/or 'id'
            	final int offset = (getAttribute(node, "id") == null? 0: 1) + (getAttribute(node, "name") == null? 0: 1);
                if (node.getAttributes().getLength() > 1 + offset) {
                    Log.warning.println("Element " + node.getNodeName() + " found with idref='" + sIDRef + "'. All other attributes are ignored.\n");
                }
            }
            if (IDMap.containsKey(sIDRef)) {
                final BEASTInterface beastObject = IDMap.get(sIDRef);
                if (checkType(classname, beastObject)) {
                    return beastObject;
                }
                throw new XMLParserException(node, "id=" + sIDRef + ". Expected object of type " + classname + " instead of " + beastObject.getClass().getName(), 106);
            } else if (IDNodeMap.containsKey(sIDRef)) {
                final BEASTInterface beastObject = createObject(IDNodeMap.get(sIDRef), classname, parent);
                if (checkType(classname, beastObject)) {
                    return beastObject;
                }
                throw new XMLParserException(node, "id=" + sIDRef + ". Expected object of type " + classname + " instead of " + beastObject.getClass().getName(), 107);
            }
            throw new XMLParserException(node, "Could not find object associated with idref " + sIDRef, 170);
        }
        // it's not in the ID map yet, so we have to create a new object
        String specClass = classname;
        final String sElementName = node.getNodeName();


        if (element2ClassMap.containsKey(sElementName)) {
            specClass = element2ClassMap.get(sElementName);
        }
        final String sSpec = getAttribute(node, "spec");
        if (sSpec != null) {
            specClass = sSpec;
        }
    	
    	if (specClass.indexOf("BEASTInterface") > 0) {
    		System.out.println(specClass);
    	}

    	Object o = null;
        // try to create object from sSpecName, taking namespaces in account
        try {
            boolean bDone = false;
            for (final String nameSpace : this.nameSpaces) {
                try {
                    if (!bDone) {
                        o = Class.forName(nameSpace + specClass).newInstance();
                        bDone = true;
                    }
                } catch (InstantiationException e) {
                    // we only get here when the class exists, but cannot be created
                    // for instance because it is abstract or an interface

                    throw new Exception("Cannot instantiate class (" + specClass + "). Please check the spec attribute.");
                } catch (ClassNotFoundException e) {
                    // TODO: handle exception
                }
            }
            if (!bDone) {
                throw new Exception("Class could not be found. Did you mean " + guessClass(specClass) + "?");
                //throw new ClassNotFoundException(sSpecClass);
            }
            // hack required to make log-parsing easier
            if (o instanceof State) {
                m_state = (State) o;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new XMLParserException(node, "Cannot create class: " + specClass + ". " + e.getMessage(), 122);
        }
        // sanity check
        if (!(o instanceof BEASTInterface)) {
            if (o instanceof Input) {
                // if we got this far, it is a basic input,
                // that is, one of the form <input name='xyz'>value</input>
                String name = getAttribute(node, "name");
                if (name == null) {
                    name = "value";
                }
                final String text = node.getTextContent();
                if (text.length() > 0) {
                    setInput(node, parent, name, text);
                }
                return null;
            } else {
                throw new XMLParserException(node, "Expected object to be instance of Plugin", 108);
            }
        }
        // set id
        final BEASTInterface beastObject = (BEASTInterface) o;
        beastObject.setID(id);
        register(node, beastObject);
        // process inputs
        parseInputs(beastObject, node);
        // initialise
        if (needsInitialisation) {
            try {
                beastObject.validateInputs();
                beastObjectsWaitingToInit.add(beastObject);
                nodesWaitingToInit.add(node);
            } catch (Exception e) {
                // next lines for debugging only
                //beastObject.validateInputs();
                //beastObject.initAndValidate();
                e.printStackTrace();
                throw new XMLParserException(node, "validate and intialize error: " + e.getMessage(), 110);
            }
        }
        return beastObject;
    } // createObject

    /**
     * find closest matching class to named class *
     */
    String guessClass(final String classname) {
        String name = classname;
        if (classname.contains(".")) {
            name = classname.substring(classname.lastIndexOf('.') + 1);
        }
        final List<String> beastObjectNames = AddOnManager.find(beast.core.BEASTInterface.class, AddOnManager.IMPLEMENTATION_DIR);
        int bestDistance = Integer.MAX_VALUE;
        String closestName = null;
        for (final String beastObject : beastObjectNames) {
            final String classname2 = beastObject.substring(beastObject.lastIndexOf('.') + 1);
            final int distance = getLevenshteinDistance(name, classname2);


            if (distance < bestDistance) {
                bestDistance = distance;
                closestName = beastObject;
            }
        }
        return closestName;
    }


    /**
     * Compute edit distance between two strings = Levenshtein distance *
     */
    public static int getLevenshteinDistance(final String s, final String t) {
        if (s == null || t == null) {
            throw new IllegalArgumentException("Strings must not be null");
        }

        final int n = s.length(); // length of s
        final int m = t.length(); // length of t

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

    void parseInputs(final BEASTInterface parent, final Node node) throws Exception {
        // first, process attributes
        final NamedNodeMap atts = node.getAttributes();
        if (atts != null) {
            for (int i = 0; i < atts.getLength(); i++) {
                final String name = atts.item(i).getNodeName();
                if (!(name.equals("id") ||
                        name.equals("idref") ||
                        name.equals("spec") ||
                        name.equals("name"))) {
                    final String value = atts.item(i).getNodeValue();
                    if (value.startsWith("@")) {
                        final String idRef = value.substring(1);
                        final Element element = doc.createElement("input");
                        element.setAttribute("idref", idRef);
                        // add child in case things go belly up, and an XMLParserException is thrown
                        node.appendChild(element);
                        final BEASTInterface beastObject = createObject(element, PLUGIN_CLASS, parent);
                        // it is save to remove the elment now
                        node.removeChild(element);
                        setInput(node, parent, name, beastObject);
                    } else {
                        setInput(node, parent, name, value);
                    }
                }
            }
        }
        // process element nodes
        final NodeList children = node.getChildNodes();
        int childElements = 0;
        String text = "";
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                final String element = child.getNodeName();
                // resolve name of the input
                String name = getAttribute(child, "name");
                if (name == null) {
                    name = element;
                }
                // resolve base class
                String classname = PLUGIN_CLASS;
                if (element2ClassMap.containsKey(element)) {
                    classname = element2ClassMap.get(element);
                }
                final BEASTInterface childItem = createObject(child, classname, parent);
                if (childItem != null) {
                    setInput(node, parent, name, childItem);
                }
                childElements++;
            } else if (child.getNodeType() == Node.CDATA_SECTION_NODE ||
                    child.getNodeType() == Node.TEXT_NODE) {
                text += child.getTextContent();
            }
        }
        if (!text.matches("\\s*")) {
            setInput(node, parent, "value", text);
        }

        if (childElements == 0) {
            final String content = node.getTextContent();
            if (content != null && content.length() > 0 && content.replaceAll("\\s", "").length() > 0) {
                try {
                    setInput(node, parent, "value", content);
                } catch (Exception e) {
                    //
                }
            }
        }

        // fill in missing inputs, if an input provider is available
        if (requiredInputProvider != null) {
            for (final Input<?> input : parent.listInputs()) {
                if (input.get() == null && input.getRule() == Validate.REQUIRED) {
                    final Object o = requiredInputProvider.createInput(parent, input, partitionContext);
                    if (o != null) {
                        input.setValue(o, parent);
                    }
                }
            }
        }
    } // setInputs

    void setInput(final Node node, final BEASTInterface beastObject, final String name, final BEASTInterface beastObject2) throws XMLParserException {
        try {
            final Input<?> input = beastObject.getInput(name);
            // test whether input was not set before, this is done by testing whether input has default value.
            // for non-list inputs, this should be true if the value was not already set before
            // for list inputs this is always true.
            if (input.get() == input.defaultValue) {
                beastObject.setInputValue(name, beastObject2);
            } else {
                throw new Exception("Multiple entries for non-list input " + input.getName());
            }
            return;
        } catch (Exception e) {
        	if (name.equals("xml:base")) {
        		// ignore xml:base attributes introduces by XML entities
        		return;
        	}
            if (e.getMessage().contains("101")) {
                String type = "?";
                try {
                    type = beastObject.getInput(name).getType().getName().replaceAll(".*\\.", "");
                } catch (Exception e2) {
                    // TODO: handle exception
                }
                throw new XMLParserException(node, e.getMessage() +
                        " expected '" + type +
                        "' but got '" + beastObject2.getClass().getName().replaceAll(".*\\.", "") + "'"
                        , 123);
            } else {
                throw new XMLParserException(node, e.getMessage(), 130);
            }
        }
        //throw new XMLParserException(node, "no such input '"+sName+"' for element <" + node.getNodeName() + ">", 167);
    }

    void setInput(final Node node, final BEASTInterface beastObject, final String name, final String value) throws XMLParserException {
        try {
			beastObject.setInputValue(name, value);
            return;
        } catch (Exception e) {
        	if (name.equals("xml:base")) {
        		// ignore xml:base attributes introduces by XML entities
        		return;
        	}
            try {
				beastObject.setInputValue(name, value);
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
    void register(final Node node, final BEASTInterface beastObject) {
        final String sID = getID(node);
        if (sID != null) {
            IDMap.put(sID, beastObject);
        }
    }

    public static String getID(final Node node) { // throws Exception {
        return getAttribute(node, "id");
    } // getID

    public static String getIDRef(final Node node) {// throws Exception {
        return getAttribute(node, "idref");
    } // getIDRef

    /**
     * get string value of attribute with given name
     * as opposed to double or integer value (see methods below) *
     */
    public static String getAttribute(final Node node, final String attName) { // throws Exception {
        final NamedNodeMap atts = node.getAttributes();
        if (atts == null) {
            return null;
        }
        for (int i = 0; i < atts.getLength(); i++) {
            final String name = atts.item(i).getNodeName();
            if (name.equals(attName)) {
                return atts.item(i).getNodeValue();
            }
        }
        return null;
    } // getAttribute

    /**
     * get integer value of attribute with given name *
     */
    public static int getAttributeAsInt(final Node node, final String attName) { //throws Exception {
        final String att = getAttribute(node, attName);
        if (att == null) {
            return -1;
        }
        return Integer.parseInt(att);
    }

    /**
     * get double value of attribute with given name *
     */
    public static double getAttributeAsDouble(final Node node, final String attName) { // throws Exception {
        final String att = getAttribute(node, attName);
        if (att == null) {
            return Double.MAX_VALUE;
        }
        return Double.parseDouble(att);
    }

    /**
     * test whether a node contains a attribute with given name *
     */
    boolean hasAtt(final Node node, final String attributeName) {
        final NamedNodeMap atts = node.getAttributes();
        if (atts != null) {
            for (int i = 0; i < atts.getLength(); i++) {
                final String sName = atts.item(i).getNodeName();
                if (sName.equals(attributeName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public interface RequiredInputProvider {
		Object createInput(BEASTInterface beastObject, Input<?> input, PartitionContext context);
    }

    public void setRequiredInputProvider(final RequiredInputProvider provider, final PartitionContext context) {
        requiredInputProvider = provider;
        partitionContext = context;
    }

    /**
     * parses file and formats it using the XMLProducer *
     */
    public static void main(final String[] args) {
        try {
            // redirect stdout to stderr
            final PrintStream out = System.out;
            System.setOut(System.err);
            // parse the file
            final XMLParser parser = new XMLParser();
            final BEASTInterface beastObject = parser.parseFile(new File(args[0]));
            // restore stdout
            System.setOut(out);
            System.out.println(new XMLProducer().toXML(beastObject));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


} // class XMLParser
