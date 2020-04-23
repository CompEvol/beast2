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


import beast.app.beauti.BeautiDoc;
import beast.app.beauti.PartitionContext;
import beast.core.*;
import beast.core.Runnable;
import beast.core.Input.Validate;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.tree.Tree;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import static beast.util.XMLParserUtils.processPlates;
import static beast.util.XMLParserUtils.replaceVariable;


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
    final static String BEAST_INTERFACE_CLASS = BEASTInterface.class.getName();
    final static String INPUT_CLASS = Input.class.getName();
	final static String TREE_CLASS = Tree.class.getName();
    final static String RUNNABLE_CLASS = Runnable.class.getName();

    public final static String BEAST_ELEMENT = "beast";
    public final static String RUN_ELEMENT = "run";
    public final static String PLATE_ELEMENT = "plate";
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
    
    Runnable m_runnable;
    //State m_state;
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
    String unavailablePacakges = "";


    static HashMap<String, String> element2ClassMap;
    static HashMap<String, String> oldClass2ClassMap;
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
        
        // Parse version.xml files
        // Encoded in version.xml as <map from="old.class.Name" to="new.class.Name"/>
        // This helps with refactoring packages while keeping old XML parseable.
        oldClass2ClassMap = new HashMap<>();
        for (String dir : PackageManager.getBeastDirectories()) {
        	File file = new File(dir + "/version.xml");
        	if (file.exists()) {
        		try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    Document doc = factory.newDocumentBuilder().parse(file);
                    doc.normalize();
                    NodeList nodes = doc.getElementsByTagName("map");
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Element map = (Element) nodes.item(i);
                        String fromClass = map.getAttribute("from");
                        String toClass = map.getAttribute("to");
                        oldClass2ClassMap.put(fromClass, toClass);
                    }
                } catch (ParserConfigurationException|SAXException|IOException e) {
                    e.printStackTrace();
                }        		
        	}
        }
    }

    public static class NameValuePair {
		String name;
		Object value;
		boolean processed;
		public NameValuePair(String name, Object value) {
			this.name = name;
			this.value = value;
			processed = false;
		}
		@Override
		public String toString() {
			return name + " "  + value;
		}
	}

    List<BEASTInterface> beastObjectsWaitingToInit = new ArrayList<>();
    List<Node> nodesWaitingToInit = new ArrayList<>();
	java.util.Map<String,String> parserDefinitions;

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
    
    /**
     * if outFile is not null, an XML file merged with parser definitions processed 
     * will be saved here
     */
    String outFile;
    

    public XMLParser() {
        this.parserDefinitions = new HashMap<>();
    }
    
	public XMLParser(java.util.Map<String,String> parserDefinitions) {
		this.parserDefinitions = parserDefinitions;
	}

	public XMLParser(java.util.Map<String,String> parserDefinitions, String outFile) {
		this.parserDefinitions = parserDefinitions;
		this.outFile = outFile;
	}


    public Runnable parseFile(final File file) throws SAXException, IOException, ParserConfigurationException, XMLParserException {
    	return parseFile(file, false);
    }
    
    public Runnable parseFile(final File file, boolean sampleFromPrior) throws SAXException, IOException, ParserConfigurationException, XMLParserException {
        // parse the XML file into a DOM document
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //factory.setValidating(true);
        String xml = BeautiDoc.load(file);
        for (String key: parserDefinitions.keySet()) {
        	xml = xml.replaceAll("\\$\\(" + key + "\\)", parserDefinitions.get(key));
        }
        
        try {
        	if (parserDefinitions != null && parserDefinitions.size() > 1) {
	    		Log.warning("Outputting merged file to " + outFile);
		        FileWriter outfile = new FileWriter(outFile);
		        outfile.write(xml);
		        outfile.close();
        	}
        } catch (IOException e) {
        	// ignore
    		Log.warning("Something went wroting outputting merged file: " + e.getMessage());
        }

        
        doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes()));
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
        
        if (parserDefinitions != null) {
        	for (String name : parserDefinitions.keySet()) {
                replaceVariable(doc.getElementsByTagName(BEAST_ELEMENT).item(0), name, 
                		parserDefinitions.get(name));
        	}
        }

		if (sampleFromPrior) {
			Element runElement = (Element) doc.getElementsByTagName(RUN_ELEMENT).item(0);
	        runElement.setAttribute("sampleFromPrior", "true");
		}

        IDMap = new HashMap<>();
        likelihoodMap = new HashMap<>();
        IDNodeMap = new HashMap<>();

        
        parse();
        //assert m_runnable == null || m_runnable instanceof Runnable;
        if (m_runnable != null)
            return m_runnable;
        else {
            throw new XMLParserException("Run element does not point to a runnable object.");
        }
    } // parseFile

    /**
     * extract all elements (runnable or not) from an XML fragment.
     * Useful for retrieving all non-runnable elements when a template
     * is instantiated by Beauti 
     * @throws ParserConfigurationException 
     * @throws IOException 
     * @throws SAXException *
     */
    public List<BEASTInterface> parseTemplate(final String xml, final HashMap<String, BEASTInterface> idMap, final boolean initialise) throws XMLParserException, SAXException, IOException, ParserConfigurationException {
        needsInitialisation = initialise;
        // parse the XML file into a DOM document
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //factory.setValidating(true);
        doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        doc.normalize();
        processPlates(doc,PLATE_ELEMENT);
        
        //XMLParserUtils.saveDocAsXML(doc, "/tmp/beast2.xml");

        IDMap = idMap;//new HashMap<>();
        likelihoodMap = new HashMap<>();
        IDNodeMap = new HashMap<>();

        final List<BEASTInterface> beastObjects = new ArrayList<>();

        // find top level beast element
        final NodeList nodes = doc.getElementsByTagName("*");
        if (nodes == null || nodes.getLength() == 0) {
            throw new XMLParserException("Expected top level beast element in XML");
        }
        final Node topNode = nodes.item(0);
        // sanity check that we are reading a beast 2 file
        final double version = getAttributeAsDouble(topNode, "version");
        if (!topNode.getNodeName().equals(BEAST_ELEMENT) || version < 2.0 || version == Double.MAX_VALUE) {
            return beastObjects;
        }
        // only process templates
//        String typeName = getAttribute(topNode, "type");
//        if (typeName == null || !typeName.equals("template")) {
//        	return beastObjects;
//        }
        // sanity check that required packages are installed

        initIDNodeMap(topNode);
        parseNameSpaceAndMap(topNode);

        final NodeList children = topNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                final Node child = children.item(i);
                Log.debug.println(child.getNodeName());
                if (!child.getNodeName().equals(MAP_ELEMENT)) {
                    beastObjects.add(createObject(child, BEAST_INTERFACE_CLASS));
                }
            }
        }
        initBEASTObjects();
        return beastObjects;
    } // parseTemplate

    private void initBEASTObjects() throws XMLParserException {
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
     * @throws XMLParserException 
     */
    public BEASTInterface parseFragment(final String xml, final boolean initialise) throws XMLParserException  {
        needsInitialisation = initialise;
        // parse the XML fragment into a DOM document
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
			doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
        doc.normalize();
        processPlates(doc,PLATE_ELEMENT);

        IDMap = new HashMap<>();
        likelihoodMap = new HashMap<>();
        IDNodeMap = new HashMap<>();

        // find top level beast element
        final NodeList nodes = doc.getElementsByTagName("*");
        if (nodes == null || nodes.getLength() == 0) {
            throw new XMLParserException("Expected top level beast element in XML");
        }
        final Node topNode = nodes.item(0);
        initIDNodeMap(topNode);
        parseNameSpaceAndMap(topNode);

        final NodeList children = topNode.getChildNodes();
        if (children.getLength() == 0) {
            throw new XMLParserException("Need at least one child element");
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
            throw new XMLParserException("Need at least one child element");
        }

        final BEASTInterface beastObject = createObject(children.item(i), BEAST_INTERFACE_CLASS);
        initBEASTObjects();
        return beastObject;
    } // parseFragment

    /**
     * Parse XML fragment that will be wrapped in a beast element
     * before parsing. This allows for ease of creating beast objects,
     * like this:
     * Tree tree = (Tree) new XMLParser().parseBareFragment("<tree spec='beast.util.TreeParser' newick='((1:1,3:1):1,2:2)'/>");
     * to create a simple tree.
     */
    public BEASTInterface parseBareFragment(String xml, final boolean initialise) throws XMLParserException {
        // get rid of XML processing instruction
        xml = xml.replaceAll("<\\?xml[^>]*>", "");
        if (xml.contains("<beast")) {
            return parseFragment(xml, initialise);
        } else {
            return parseFragment("<beast>" + xml + "</beast>", initialise);
        }
    }

    public List<BEASTInterface> parseBareFragments(final String xml, final boolean initialise) throws XMLParserException, SAXException, IOException, ParserConfigurationException {
        needsInitialisation = initialise;
        // parse the XML fragment into a DOM document
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        doc.normalize();
        processPlates(doc,PLATE_ELEMENT);

        // find top level beast element
        final NodeList nodes = doc.getElementsByTagName("*");
        if (nodes == null || nodes.getLength() == 0) {
            throw new XMLParserException("Expected top level beast element in XML");
        }
        final Node topNode = nodes.item(0);
        initIDNodeMap(topNode);
        parseNameSpaceAndMap(topNode);

        final NodeList children = topNode.getChildNodes();
        final List<BEASTInterface> beastObjects = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                final BEASTInterface beastObject = createObject(children.item(i), BEAST_INTERFACE_CLASS);
                beastObjects.add(beastObject);
            }
        }
        initBEASTObjects();
        return beastObjects;
    }

    /**
     * parse BEAST file as DOM document
     * @throws XMLParserException 
     */
    public void parse() throws XMLParserException {
        // find top level beast element
        final NodeList nodes = doc.getElementsByTagName("*");
        if (nodes == null || nodes.getLength() == 0) {
            throw new XMLParserException("Expected top level beast element in XML");
        }
        final Node topNode = nodes.item(0);
        final double version = getAttributeAsDouble(topNode, "version");
        if (version < 2.0 || version == Double.MAX_VALUE) {
            throw new XMLParserException(topNode, "Wrong version: only versions > 2.0 are supported", 101);
        }

        String required = getAttribute(topNode, "required");
        if (required != null && required.trim().length() > 0) {
        	String [] packageAndVersions = required.split(":");
        	for (String s : packageAndVersions) {
        		s = s.trim();
        		int i = s.lastIndexOf(" ");
        		if (i > 0) {
        			String pkgname = s.substring(0, i);
        			String pkgversion = s.substring(i+1);
        			if (!PackageManager.isInstalled(pkgname, pkgversion)) {
        				unavailablePacakges += s +", ";
        			}
        		}
        	}
        	if (unavailablePacakges.length() > 1) {
        		unavailablePacakges = unavailablePacakges.substring(0, unavailablePacakges.length() - 2);
        		if (unavailablePacakges.contains(",")) {
        			Log.warning("The following packages are required, but not available: " + unavailablePacakges);
        		} else {
        			Log.warning("The following package is required, but is not available: " + unavailablePacakges);
        		}
        		Log.warning("See http://beast2.org/managing-packages/ for details on how to install packages.");
        	}
        }

        initIDNodeMap(topNode);
        parseNameSpaceAndMap(topNode);

        //parseState();
        parseRunElement(topNode);
        initBEASTObjects();
    } // parse


    /**
     * Traverse DOM beast.tree and grab all nodes that have an 'id' attribute
     * Throw exception when a duplicate id is encountered
     *
     * @param node
     * @throws XMLParserException
     */
    void initIDNodeMap(final Node node) throws XMLParserException {
        final String id = getID(node);
        if (id != null) {
            if (IDNodeMap.containsKey(id)) {
                throw new XMLParserException(node, "IDs should be unique. Duplicate id '" + id + "' found", 104);
            }
            IDNodeMap.put(id, node);
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
            final String nameSpace = getAttribute(topNode, "namespace");
            setNameSpace(nameSpace);
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
//                throw new XMLParserException(child, "name '" + name + "' is already defined as " + m_sElement2ClassMap.get(name), 301);
//            }

	            // get class
	            String clazz = child.getTextContent();
	            // remove spaces
	            clazz = clazz.replaceAll("\\s", "");
	            // go through namespaces in order they are declared to find the correct class
	            boolean isDone = false;
	            for (final String nameSpace : this.nameSpaces) {
	                try {
	                    // sanity check: class should exist
	                    if (!isDone && BEASTClassLoader.forName(nameSpace + clazz) != null) {
	                        element2ClassMap.put(name, clazz);
	                        Log.debug.println(name + " => " + nameSpace + clazz);
	                        final String reserved = getAttribute(child, "reserved");
	                        if (reserved != null && reserved.toLowerCase().equals("true")) {
	                        	reservedElements.add(name);
	                        }
	
	                        isDone = true;
	                    }
	                } catch (ClassNotFoundException e) {
	                    // Log.warning.println("Not found " + e.getMessage());
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

    void parseRunElement(final Node topNode) throws XMLParserException {
        // find mcmc element
        final NodeList nodes = doc.getElementsByTagName(RUN_ELEMENT);
        if (nodes.getLength() == 0) {
            throw new XMLParserException(topNode, "Expected run element in file", 102);
        }
        if (nodes.getLength() > 1) {
            throw new XMLParserException(topNode, "Expected only one mcmc element in file, not " + nodes.getLength(), 103);
        }
        final Node mcmc = nodes.item(0);

        m_runnable = (Runnable) createObject(mcmc, RUNNABLE_CLASS);
    } // parseMCMC

    /**
     * Check that beast object is a class that is assignable to class with name className.
     * This involves a parameter clutch to deal with non-real parameters.
     * This needs a bit of work, obviously...
     * @throws ClassNotFoundException 
     */
    boolean checkType(final String className, final BEASTInterface beastObject, Node node) throws XMLParserException  {
        try {
			if (className.equals(INPUT_CLASS) || BEASTClassLoader.forName(className).isInstance(beastObject)) {
			    return true;
			}
		} catch (ClassNotFoundException e) {
			throw new XMLParserException(node, "Class not found:" + e.getMessage(), 444);
		}
        // parameter clutch
        if (className.equals("RealParameter") && beastObject instanceof Parameter<?>) {
            return true;
        }
        return false;
    } // checkType

    BEASTInterface createObject(final Node node, final String classname) throws XMLParserException {
        // try the IDMap first
        final String id = getID(node);

        if (id != null) {
            if (IDMap.containsKey(id)) {
                final BEASTInterface beastObject = IDMap.get(id);
                if (checkType(classname, beastObject, node)) {
                    return beastObject;
                }
                throw new XMLParserException(node, "id=" + id + ". Expected object of type " + classname + " instead of " + beastObject.getClass().getName(), 105);
            }
        }

        final String dRef = getIDRef(node);
        if (dRef != null) {
            // produce warning if there are other attributes than idref
            if (node.getAttributes().getLength() > 1) {
                // check if there are just 2 attributes and other attribute is 'name' and/or 'id'
            	final int offset = (getAttribute(node, "id") == null? 0: 1) + (getAttribute(node, "name") == null? 0: 1);
                if (node.getAttributes().getLength() > 1 + offset) {
                    Log.warning.println("Element " + node.getNodeName() + " found with idref='" + dRef + "'. All other attributes are ignored.\n");
                }
            }
            if (IDMap.containsKey(dRef)) {
                final BEASTInterface beastObject = IDMap.get(dRef);
                // TODO: testing for "Alignment" is a hack for a common problem
                // occurring in many templates. As long as packages are not debugged
                // this hack should stay in place, but should eventually (v2.5?) be removed.
                if (classname.equals("Alignment") || checkType(classname, beastObject, node)) {
                    return beastObject;
                }
                checkType(classname, beastObject, node);
                throw new XMLParserException(node, "id=" + dRef + ". Expected object of type " + classname + " instead of " + beastObject.getClass().getName(), 106);
            } else if (IDNodeMap.containsKey(dRef)) {
                final BEASTInterface beastObject = createObject(IDNodeMap.get(dRef), classname);
                if (checkType(classname, beastObject, node)) {
                    return beastObject;
                }
                throw new XMLParserException(node, "id=" + dRef + ". Expected object of type " + classname + " instead of " + beastObject.getClass().getName(), 107);
            }
            throw new XMLParserException(node, "Could not find object associated with idref " + dRef, 170);
        }
        // it's not in the ID map yet, so we have to create a new object
        String specClass = classname;
        final String elementName = node.getNodeName();


        if (element2ClassMap.containsKey(elementName)) {
            specClass = element2ClassMap.get(elementName);
        }
        final String spec = getAttribute(node, "spec");
        if (spec != null) {
            specClass = spec;
        }
    	
    	//if (specClass.indexOf("BEASTInterface") > 0) {
    	//	Log.info.println(specClass);
    	//}
        
		String clazzName = null;
		// determine clazzName from specName, taking name spaces in account
		clazzName = XMLParserUtils.resolveClass(specClass, nameSpaces);
		if (clazzName == null) {
			// try to create the old-fashioned way by creating the class
            boolean isDone = false;
            for (final String nameSpace : nameSpaces) {
                try {
                    if (!isDone) {
                    	BEASTClassLoader.forName(nameSpace + specClass);
                        clazzName = nameSpace + specClass;
                        isDone = true;
                    }
                } catch (ClassNotFoundException e) {
                    // class does not exist -- try another namespace
                	if (oldClass2ClassMap.containsKey(nameSpace + specClass)) {
                		clazzName = oldClass2ClassMap.get(nameSpace + specClass);
                		isDone = true;
                	}
                }
            }
		}
		if (clazzName == null) {
			if (unavailablePacakges.length() > 2) {
				String msg = "Class " + specClass + " could not be found.\n" +
		        		(unavailablePacakges.contains(",") ?
							"This XML requires the following packages that are not installed: " :
							"This XML requires the following package that is not installed: ") + unavailablePacakges + "\n" +
		        		"See http://beast2.org/managing-packages/ for details on how to install packages.\n" +
						"Or perhaps there is a typo in spec and you meant " + XMLParserUtils.guessClass(specClass) + "?";
				throw new XMLParserException(node, msg, 1018);				
			}
			throw new XMLParserException(node, "Class could not be found. Did you mean " + XMLParserUtils.guessClass(specClass) + "?\n"
					+ "Perhaps a package required for this class is not installed?", 1017);
			// throw new ClassNotFoundException(specClass);
		}
				
		// sanity check		
		try {
			Class<?> clazz = BEASTClassLoader.forName(clazzName);
			if (!BEASTInterface.class.isAssignableFrom(clazz)) {
				// throw new XMLParserException(node, "Expected object to be instance of BEASTObject", 108);
			}
		} catch (ClassNotFoundException e1) {
			// should never happen since clazzName is in the list of classes collected by the AddOnManager
			e1.printStackTrace();
			throw new RuntimeException(e1);
		}
		
		// process inputs
		List<NameValuePair> inputInfo = parseInputs(node, clazzName);
		BEASTInterface beastObject = createBeastObject(node, id, clazzName, inputInfo);

        // initialise
        if (needsInitialisation) {
            try {
            	beastObject.determindClassOfInputs();
                beastObject.validateInputs();
                beastObjectsWaitingToInit.add(beastObject);
                nodesWaitingToInit.add(node);
            } catch (IllegalArgumentException e) {
                // next lines for debugging only
                //beastObject.validateInputs();
                //beastObject.initAndValidate();
                e.printStackTrace();
                throw new XMLParserException(node, "validate and intialize error: " + e.getMessage(), 110);
            }
        }
        return beastObject;
    } // createObject

    
	/** create BEASTInterface either using Inputs, or using annotated constructor **/
	private BEASTInterface createBeastObject(Node node, String ID, String clazzName, List<NameValuePair> inputInfo) throws XMLParserException {
		BEASTInterface beastObject = useAnnotatedConstructor(node, ID, clazzName, inputInfo);
		if (beastObject != null) {
			return beastObject;
		}
		
		// create new instance using class name
		Object o = null;
		try {
			Class<?> c = BEASTClassLoader.forName(clazzName); 
				o = c.newInstance();
		} catch (InstantiationException e) {
			// we only get here when the class exists, but cannot be
			// created for instance because it is abstract
            String msg = "Class " + clazzName + " exists but cannot to be instantiated.\n" +
                    "Please check if the class has the zero-argument constructor.";
			throw new XMLParserException(node, msg, 1006);
		} catch (ClassNotFoundException e) {
			// ignore -- class was found in beastObjectNames before
		} catch (IllegalAccessException e) {
			// try to call static method clazzName.newInstance()
			Class<?> c;
			try {
				c = BEASTClassLoader.forName(clazzName);
				Method newInstance;
				newInstance = c.getDeclaredMethod("newInstance");
				if (!Modifier.isStatic(newInstance.getModifiers())) {
					throw new XMLParserException(node, "Found method newInstance() but it is not static so it cannot be accessed", 1112);					
				}
				o = newInstance.invoke(null);
			} catch (NullPointerException e1) { 
				throw new XMLParserException(node, "Cannot access newInstance(). Perhaps the methods is not defined?", 1111);
			} catch (NoSuchMethodException | SecurityException | ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				e1.printStackTrace();
				throw new XMLParserException(node, "Cannot access class. Please check the spec attribute.", 1011);
			} 
		}
		
		// set id
		beastObject = BEASTObjectStore.INSTANCE.getBEASTObject(o);
		beastObject.setID(ID);

		// hack required to make log-parsing easier
		//if (o instanceof State) {
		//	m_state = (State) o;
		//}

		// process inputs for annotated constructors
		for (NameValuePair pair : inputInfo) {
			if (pair.value instanceof BEASTInterface) {
				setInput(node, beastObject, pair.name, (BEASTInterface) pair.value);
			} else if (pair.value instanceof String) {
				setInput(node, beastObject, pair.name, (String) pair.value);
			} else {
				throw new RuntimeException("Programmer error: value should be String or BEASTInterface");
			}
		}
		
		// fill in missing inputs, if an input provider is available
		try {
			if (requiredInputProvider != null) {
				for (Input<?> input : beastObject.listInputs()) {
					if (input.get() == null && input.getRule() == Validate.REQUIRED) {
						Object o2 = requiredInputProvider.createInput(beastObject, input, partitionContext);
						if (o2 != null) {
							input.setValue(o2, beastObject);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new XMLParserException(node, e.getMessage(), 1008);			
		}
		
		// sanity check: all attributes should be valid input names
		for (NameValuePair pair : inputInfo) {
			String name = pair.name;
			if (!(name.equals("id") || name.equals("idref") || name.equals("spec") || name.equals("name"))) {
				try {
					beastObject.getInput(name);
				} catch (Exception e) {
					throw new XMLParserException(node, e.getMessage(), 1009);
				}
			}
		}
		
		// make sure object o is in outputs of inputs
		for (NameValuePair pair : inputInfo) {
			if (pair.value instanceof BEASTInterface) {
				((BEASTInterface) pair.value).getOutputs().add((BEASTInterface) o);
			}	
		}
		

		register(node, beastObject);
		return beastObject;
	}

    
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private BEASTInterface useAnnotatedConstructor(Node node, String _id, String clazzName, List<NameValuePair> inputInfo) throws XMLParserException {
		
		if (clazzName.startsWith("dr")) {
			int h =3;
			h++;
		}
		Class<?> clazz = null;
		try {
			clazz = BEASTClassLoader.forName(clazzName);
		} catch (ClassNotFoundException e) {
			// cannot get here, since we checked the class existed before
			e.printStackTrace();
		}


		// try to find a constructor that has Param annotations where all values of inputInfo can be matched
	    Constructor<?>[] allConstructors = clazz.getDeclaredConstructors();
	    boolean hasID = false;
	    boolean hasName = false;
	    for (Constructor<?> ctor : allConstructors) {
	    	Annotation[][] annotations = ctor.getParameterAnnotations();
	    	List<Param> paramAnnotations = new ArrayList<>();
	    	int optionals = 0;
	    	for (Annotation [] a0 : annotations) {
		    	for (Annotation a : a0) {
		    		if (a instanceof Param) {
		    			Param param = (Param) a;
		    			paramAnnotations.add(param);
		    			if (param.name().equals("id") && !hasID) {
		    				inputInfo.add(new NameValuePair("id", _id));
		    				hasID = true;
		    			}
		    			if (param.name().equals("name") && !hasName) {
		    				inputInfo.add(new NameValuePair("name", node.getAttributes().getNamedItem("name")));
		    				hasName = true;
		    			}
		    			if (param.optional()) {
		    				optionals++;
		    			}
		    		}
	    		}
	    	}
	    	
	    	for (NameValuePair pair : inputInfo) {
	    		pair.processed = false;
	    	}

	    	Class<?>[] types  = ctor.getParameterTypes();
    		//Type[] gtypes = ctor.getGenericParameterTypes();
    		int offset = clazz.isMemberClass() && !clazz.getEnclosingClass().isInterface() ? 1 : 0;
	    	if (types.length > 0 && paramAnnotations.size() == types.length - offset &&
	    			paramAnnotations.size() <= types.length + optionals) {
		    	try {
		    		Object [] args = new Object[types.length];
		    		if (clazz.isMemberClass()) {
						Class<?> enclosingClass = clazz.getEnclosingClass();
						if (!enclosingClass.isInterface()) {
							Object enclosingInstance = enclosingClass.newInstance();
			    			args[0] = enclosingInstance;
						}
		    		}
		    		for (int i = offset; i < types.length; i++) {
		    			Param param = paramAnnotations.get(i - offset);
		    			Type type = types[i];
		    			if (type.getTypeName().equals("java.util.List")) {
		    				if (args[i] == null) {
		    					// no need to parameterise list due to type erasure
		    					args[i] = new ArrayList();
		    				}
		    				List<Object> values = XMLParser.getListOfValues(param, inputInfo);
		    				for (Object v : values) {
		    					if (v instanceof VirtualBEASTObject) {
		    						((List)args[i]).add(((VirtualBEASTObject) v).getObject());
		    					} else {
		    						((List)args[i]).add(v);
		    					}
		    				}
		    			} else if (type.getTypeName().endsWith("[]")) {
							String typeName = type.getTypeName();
							typeName = typeName.substring(0, typeName.length() - 2);
							try {
								if (!BEASTObjectStore.isPrimitiveType(typeName)) {
									List<Object> values = XMLParser.getListOfValues(param, inputInfo);
									Object array = java.lang.reflect.Array.newInstance(BEASTClassLoader.forName(typeName), values.size());
									for (int k = 0; k < values.size(); k++) {
										Object v = values.get(k);
										if (v instanceof VirtualBEASTObject) {
											Array.set(array, k, ((VirtualBEASTObject) v).getObject());
										} else {
											Array.set(array, k, v);
										}
									}
									args[i] = array;
								} else {
									args[i] = getValue(param, types[i], inputInfo);
									args[i] = Input.fromString(args[i], types[i]);
								}
							} catch (ClassNotFoundException | NegativeArraySizeException e) {
								args[i] = getValue(param, types[i], inputInfo);
								args[i] = Input.fromString(args[i], types[i]);
							}
						} else {
							args[i] = getValue(param, types[i], inputInfo);
							args[i] = Input.fromString(args[i], types[i]);

						}
		    		}

		    		// ensure all inputs are used
		    		boolean allUsed = true;
			    	for (NameValuePair pair : inputInfo) {
			    		if (!pair.processed) {
			    			allUsed= false;
			    		}
			    	}
			    	
			    	if (allUsed) {
				    	// ensure all arguments are set
				    	for (int i = 0; i < args.length; i++) {
				    		if (args[i] == null) {
				    			allUsed= false;
				    		}
				    	}
			    	}

			    	if (allUsed) {
				    	try {
							Object o = ctor.newInstance(args);
							BEASTInterface beastObject = BEASTObjectStore.INSTANCE.getBEASTObject(o);
							beastObject.setID(_id);
							register(node, beastObject);
							return beastObject;
				    	} catch (IllegalAccessException e) {
							throw new XMLParserException(node, "Could not create object: " + e.getMessage() + "\n"
									+ "(Perhaps a programmer error: the constructor or class may not be public)", 1014);
						} catch (InstantiationException | IllegalArgumentException | InvocationTargetException e) {
							throw new XMLParserException(node, "Could not create object: " + e.getMessage(), 1012);
				    	}
			    	}
				} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					// we get here when a param value cannot be constructed from a default value
					// let's try the next constructor (if any)
				}

	    	}
		}
		return null;
	}

	private Object getValue(Param param, Class<?> type, List<NameValuePair> inputInfo) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for (NameValuePair pair : inputInfo) {
			if (pair.name.equals(param.name())) {
				pair.processed = true;
				Object value = pair.value;
				if (value instanceof VirtualBEASTObject) {
					value = ((VirtualBEASTObject) value).getObject();
				}
				return value;
			}
		}
		
		// check if this parameter is required or optional
		if (!param.optional()) {
			throw new IllegalArgumentException();
		}
		return param.defaultValue();
	}

	static List<Object> getListOfValues(Param param, List<NameValuePair> inputInfo) {
		List<Object> values = new ArrayList<>();
		for (NameValuePair pair : inputInfo) {
			if (pair.name.equals(param.name())) {
				if (pair.value instanceof List) {
					values.addAll((List) pair.value);
				} else {
					values.add(pair.value);
				}
				pair.processed = true;
			}
		}
		return values;
	}
    
    private List<NameValuePair> parseInputs(Node node, String clazzName) throws XMLParserException {
    	List<NameValuePair> inputInfo = new ArrayList<>();
        // first, process attributes
        NamedNodeMap atts = node.getAttributes();
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
                        final BEASTInterface beastObject = createObject(element, BEAST_INTERFACE_CLASS);
                        // it is save to remove the element now
                        node.removeChild(element);
                        inputInfo.add(new NameValuePair(name, beastObject));
                    } else {
                        inputInfo.add(new NameValuePair(name, value));
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
                String specClass = BEAST_INTERFACE_CLASS;
                if (element2ClassMap.containsKey(element)) {
                	specClass = element2ClassMap.get(element);
                }
                final String spec = getAttribute(child, "spec");
                if (spec != null) {
                	specClass = spec;
                }                
                String classname = null;
        		// determine clazzName from specName, taking name spaces in account
                classname = XMLParserUtils.resolveClass(specClass, nameSpaces);
        		if (classname == null) {
        			classname = specClass;
        		}
        		
                // test for special cases: <xyz>value</xyz>  and <input name="xyz">value</input>
                // where value is a string
                boolean done = false;
                atts = child.getAttributes();
                if (atts.getLength() == 0 || (element.equals("input") && atts.getLength() == 1 && name != null)) {
                	NodeList grantchildren = child.getChildNodes();
                	boolean hasElements = false;
                	for (int j = 0; j < grantchildren.getLength(); j++) {
                		if (grantchildren.item(j).getNodeType() == Node.ELEMENT_NODE) {
                			hasElements = true;
                			break;
                		}
                	}
                	if (!hasElements) {
                		String content = child.getTextContent();
                		inputInfo.add(new NameValuePair(name, content));
                		done = true;
                	}
                }
                
                // create object from element, if not already done so
                if (!done) {
                    final BEASTInterface childItem = createObject(child, classname);
                    if (childItem != null) {
                    	inputInfo.add(new NameValuePair(name, childItem));
                    }                		
            	}
                childElements++;
            } else if (child.getNodeType() == Node.CDATA_SECTION_NODE ||
                    child.getNodeType() == Node.TEXT_NODE) {
                text += child.getTextContent();
            }
        }
        if (!text.matches("\\s*")) {
        	inputInfo.add(new NameValuePair("value", text));
        }

        if (childElements == 0) {
            final String content = node.getTextContent();
            if (content != null && content.length() > 0 && content.replaceAll("\\s", "").length() > 0) {
                try {
                	inputInfo.add(new NameValuePair("value", content));
                } catch (Exception e) {
                    //
                }
            }
        }

        return inputInfo;
    } // setInputs
    
    void setInput(final Node node, final BEASTInterface beastObject, final String name, final BEASTInterface beastObject2) throws XMLParserException {
        try {
            final Input<?> input = beastObject.getInput(name);
            // test whether input was not set before, this is done by testing whether input has default value.
            // for non-list inputs, this should be true if the value was not already set before
            // for list inputs this is always true.
            if (input.get() == input.defaultValue) {
            	if (beastObject2 instanceof VirtualBEASTObject) {
            		beastObject.setInputValue(name, ((VirtualBEASTObject)beastObject2).getObject());
            	} else {
            		beastObject.setInputValue(name, beastObject2);
            	}
            } else {
                throw new XMLParserException(node, "\nMultiple entries for input \"" + input.getName() + "\" but only single entry expected "
                		+ "in element \"" + node.getNodeName() + "\"", 130);
            }
            return;
        } catch (XMLParserException e) {
        	throw e;
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
        //throw new XMLParserException(node, "no such input '"+name+"' for element <" + node.getNodeName() + ">", 167);
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
        //throw new XMLParserException(node, "no such input '"+name+"' for element <" + node.getNodeName() + ">", 168);
    }

    /**
     * records id in IDMap, for ease of retrieving beast objects associated with idrefs *
     */
    void register(final Node node, final BEASTInterface beastObject) {
        final String id = getID(node);
        if (id != null) {
            IDMap.put(id, beastObject);
        }
    }

    public static String getID(final Node node) { 
        return getAttribute(node, "id");
    } // getID

    public static String getIDRef(final Node node) {
        return getAttribute(node, "idref");
    } // getIDRef

    /**
     * get string value of attribute with given name
     * as opposed to double or integer value (see methods below) *
     */
    public static String getAttribute(final Node node, final String attName) { 
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
    public static int getAttributeAsInt(final Node node, final String attName) { 
        final String att = getAttribute(node, attName);
        if (att == null) {
            return -1;
        }
        return Integer.parseInt(att);
    }

    /**
     * get double value of attribute with given name *
     */
    public static double getAttributeAsDouble(final Node node, final String attName) { 
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
                final String name = atts.item(i).getNodeName();
                if (name.equals(attributeName)) {
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

