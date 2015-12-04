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


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import beast.app.beauti.PartitionContext;
import beast.core.*;
import beast.core.Input.Validate;
import beast.core.Runnable;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** parses YABBY JSON file into a set of YABBY objects **/
public class JSONParser {
	final static String INPUT_CLASS = Input.class.getName();
	final static String YOBJECT_CLASS = BEASTInterface.class.getName();
	final static String RUNNABLE_CLASS = Runnable.class.getName();

	Runnable runnable;
	State state;
	/**
	 * JSONObject document representation of JSON file *
	 */
	JSONObject doc;

	/**
	 * maps sequence data onto integer value *
	 */
	String DataMap;

	HashMap<String, BEASTInterface> IDMap;
	HashMap<String, Integer[]> likelihoodMap;
	HashMap<String, JSONObject> IDNodeMap;

	static HashMap<String, String> element2ClassMap;
	static Set<String> reservedElements;
	static {
		element2ClassMap = new HashMap<String, String>();
		reservedElements = new HashSet<String>();
		for (String element : element2ClassMap.keySet()) {
			reservedElements.add(element);
		}
	}

	class BEASTObjectWrapper {
		public BEASTObjectWrapper(BEASTInterface object, JSONObject node) {
			this.object = object;
			this.node = node;
		}
		
		BEASTInterface object;
		JSONObject node;
	}
	List<BEASTObjectWrapper> objectsWaitingToInit;

	public HashMap<String, String> getElement2ClassMap() {
		return element2ClassMap;
	}

	String[] nameSpaces;

	/**
	 * Flag to indicate initAndValidate should be called after all inputs of a
	 * plugin have been parsed
	 */
	boolean initialise = true;

	/**
	 * when parsing JSON, missing inputs can be assigned default values through a
	 * RequiredInputProvider
	 */
	RequiredInputProvider requiredInputProvider = null;
	PartitionContext partitionContext = null;

	public JSONParser() {
		objectsWaitingToInit = new ArrayList<>();
	}

	public Runnable parseFile(File file) throws Exception {
		// parse the JSON file into a JSONObject
		
		// first get rid of comments: remove all text on lines starting with space followed by //
		// keep line breaks so that error reporting indicates the correct line.
		BufferedReader fin = new BufferedReader(new FileReader(file));
		StringBuffer buf = new StringBuffer();
		String sStr = null;
		while (fin.ready()) {
			sStr = fin.readLine();
			if (!sStr.matches("^\\s*//.*")) {
				buf.append(sStr);
			}
			buf.append('\n');
		}
		fin.close();
		
		doc = new JSONObject(buf.toString());
		processPlates(doc);

		int pointIdx = file.getName().lastIndexOf('.');
        String baseName = pointIdx<0 ? file.getName() : file.getName().substring(0, pointIdx);

        replaceVariable(doc, "filebase", baseName);

        // Substitute occurrences of "$(seed)" with RNG seed
        replaceVariable(doc, "seed", String.valueOf(Randomizer.getSeed()));

		
		IDMap = new HashMap<String, BEASTInterface>();
		likelihoodMap = new HashMap<String, Integer[]>();
		IDNodeMap = new HashMap<String, JSONObject>();

		parse();
		// assert m_runnable == null || m_runnable instanceof Runnable;
		if (runnable != null)
			return runnable;
		else {
			throw new Exception("Run element does not point to a runnable object.");
		}
	} // parseFile
	
    /**
     * @param node the node to do variable replacement in
     * @param var the variable name to replace
     * @param value the value to replace the variable name with
     */
    public static void replaceVariable(final Object json, final String var, final String value) {
    	try {
	        if (json instanceof JSONObject) {
	            final JSONObject jsonobject = (JSONObject) json;
	            for (String key : jsonobject.keySet()) {
	                final Object attr = jsonobject.get(key);
	                if (attr instanceof String) {
	                	if (((String) attr).contains("$(" + var + ")")) {
	                		String sAtt = (String) attr;
	                		sAtt = sAtt.replaceAll("\\$\\(" + var + "\\)", value);
	                		jsonobject.put(key, sAtt);
	                	}
	                } else if (attr instanceof JSONObject) {
	                	replaceVariable(attr, var, value);
	                } else if (attr instanceof JSONArray) {
	                	replaceVariable(attr, var, value);
	                }
	            }
		    } else if (json instanceof JSONArray) {
		    	JSONArray array = (JSONArray) json;
		    	for (int i = 0; i < array.length(); i++) {
		        	Object o2 = array.get(i);
	            	replaceVariable(o2, var, value);
		        }
	        } else {
	        	// ignore
	        }
    	} catch (JSONException e) {
    		// ignore?
    	}
    } // replaceVariable

	/**
	 * extract all elements (runnable or not) from an XML fragment. Useful for
	 * retrieving all non-runnable elements when a template is instantiated by
	 * Beauti *
	 */
	// public List<Plugin> parseTemplate(String sXML, HashMap<String, Plugin>
	// sIDMap, boolean bInitialize) throws Exception {
	// m_bInitialize = bInitialize;
	// // parse the XML file into a DOM document
	// DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	// //factory.setValidating(true);
	// doc = factory.newDocumentBuilder().parse(new InputSource(new
	// StringReader(sXML)));
	// processPlates();
	//
	// IDMap = sIDMap;//new HashMap<String, Plugin>();
	// likelihoodMap = new HashMap<String, Integer[]>();
	// IDNodeMap = new HashMap<String, JSONObject>();
	//
	// List<Plugin> plugins = new ArrayList<Plugin>();
	//
	// // find top level beast element
	// NodeList nodes = doc.getElementsByTagName("*");
	// if (nodes == null || nodes.getLength() == 0) {
	// throw new Exception("Expected top level beast element in XML");
	// }
	// Node topNode = nodes.item(0);
	// // sanity check that we are reading a beast 2 file
	// double fVersion = getAttributeAsDouble(topNode, "version");
	// if (!topNode.getNodeName().equals(BEAST_ELEMENT) || fVersion < 2.0 ||
	// fVersion == Double.MAX_VALUE) {
	// return plugins;
	// }
	// // only process templates
	// // String sType = getAttribute(topNode, "type");
	// // if (sType == null || !sType.equals("template")) {
	// // return plugins;
	// // }
	//
	//
	// initIDNodeMap(topNode);
	// parseNameSpaceAndMap(topNode);
	//
	// NodeList children = topNode.getChildNodes();
	// for (int i = 0; i < children.getLength(); i++) {
	// if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
	// Node child = children.item(i);
	// System.err.println(child.getNodeName());
	// if (!child.getNodeName().equals(MAP_ELEMENT)) {
	// plugins.add(createObject(child, PLUGIN_CLASS, null));
	// }
	// }
	// }
	// initPlugins();
	// return plugins;
	// } // parseTemplate

	private void initPlugins() throws JSONParserException {
		JSONObject node = null;
		try {
			for (int i = 0; i < objectsWaitingToInit.size(); i++) {
				BEASTObjectWrapper bow = objectsWaitingToInit.get(i);
				// for error handling, init node
				node = bow.node;
				bow.object.initAndValidate();
			}
		} catch (Exception e) {
			// next lines for debugging only
			// plugin.validateInputs();
			// plugin.initAndValidate();
			e.printStackTrace();
			throw new JSONParserException(node, "validate and intialize error: " + e.getMessage(), 110);
		}
	}

	/**
	 * Expand plates in JSON by duplicating the containing JSON and replacing the
	 * plate variable with the appropriate value.
	 * "plate":{"var":"n",
	 *  "range": ["CO1", "CO2", "Nuc"],
	 *  "content": [
	 *      {"part":"$(n)"}
	 *      {"otherpart":"$(n).$(m)"}
	 *      {"yetotherpart":"xyz$(n)"}
	 *  ]
	 *  }
	 *  
	 *  is replaced by
	 *  
	 *  {"part":"CO1"}
	 *  {"otherpart":"CO1.$(m)"}
	 *  {"yetotherpart":"xyzCO1"}
	 *  {"part":"CO2"}
	 *  {"otherpart":"CO2.$(m)"}
	 *  {"yetotherpart":"xyzCO2"}
	 *  {"part":"Nuc"}
	 *  {"otherpart":"Nuc.$(m)"}
	 *  {"yetotherpart":"xyzNuc"}
	 * @throws Exception 
	 * 
	 */
	void processPlates(JSONObject node) throws Exception {
		for (String key : node.keySet()) {
			Object o = node.get(key);
			if (o instanceof JSONObject) {
				JSONObject child = (JSONObject) o;
				processPlates((JSONObject) child);
			}
			if (o instanceof JSONArray) {
				JSONArray list = (JSONArray) o;
				for (int i = 0; i < list.length(); i++) {
					Object o2 = list.get(i);
					if (o2 instanceof JSONObject) {
						JSONObject child = (JSONObject) o2;
						processPlates(child);
						if (child.has("plate")) {
							unrollPlate(list, child);
						}
					}
				}
			}
		}
	} // processPlates

	private void unrollPlate(JSONArray list, JSONObject plate) throws Exception {
		int index = list.indexOf(plate);
		if (index < 0) {
			throw new Exception("Programmer error: plate should be in list");
		}
		list.remove(index);
		if (plate.keySet().size() != 3 || 
				!plate.has("plate") ||
				!plate.has("range") ||
				!plate.has("var")) {
			throw new JSONParserException(plate, "Plate should only have tree attributes: plate,  range and var", 1007);
		}
		
		Object o = plate.get("range");
		if (!(o instanceof JSONArray)) {
			throw new JSONParserException(plate, "Plate attribute range should be a list", 1008);
		}
		JSONArray range = (JSONArray) o;
		
		o = plate.get("var");
		if (!(o instanceof String)) {
			throw new JSONParserException(plate, "Plate attribute var should be a string", 1009);
		}
		String varStr = (String) o;
		
		for (int i = 0; i < range.length(); i++) {
			o = range.get(i);
			if (!(o instanceof String)) {
				throw new JSONParserException(plate, "Plate range value should be a string", 1010);
			}
			String valueStr = (String) o;
			Object copy = copyReplace(plate, varStr, valueStr);
			list.insert(index + i, copy);
		}
	} // unrollPlate

	private Object copyReplace(Object o, String varStr, String valueStr) {
		if (o instanceof Number) {
			return o;
		} else if (o instanceof Boolean) {
			return o;
		} else if (o instanceof String) {
			String str = (String) o;
			str = str.replaceAll("\\$\\(" + varStr + "\\)", valueStr);
			return str;
		} else if (o instanceof JSONObject) {
			JSONObject orig = (JSONObject) o;
			JSONObject copy = new JSONObject();
			for (String key : orig.keySet()) {
				try {
					Object value = orig.get(key);
					Object copyValue = copyReplace(value, varStr, valueStr);
					copy.put(key, copyValue);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return copy;
		} else if (o instanceof JSONArray) {
			JSONArray orig = (JSONArray) o;
			JSONArray copy = new JSONArray();
			for (int i = 0; i < orig.length(); i++) {
				Object value;
				try {
					value = orig.get(i);
					Object copyValue = copyReplace(value, varStr, valueStr);
					copy.add(copyValue);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return copy;			
		}
		throw new RuntimeException("How did we get here?");
	} // unrollPlate
	

	// /**
	// * Parse an XML fragment representing a Plug-in
	// * Only the run element or if that does not exist the last child element
	// of
	// * the top level <beast> element is considered.
	// */
	// public Plugin parseFragment(String sXML, boolean bInitialize) throws
	// Exception {
	// m_bInitialize = bInitialize;
	// // parse the XML fragment into a DOM document
	// DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	// doc = factory.newDocumentBuilder().parse(new InputSource(new
	// StringReader(sXML)));
	// doc.normalize();
	// processPlates();
	//
	// IDMap = new HashMap<String, Plugin>();
	// likelihoodMap = new HashMap<String, Integer[]>();
	// IDNodeMap = new HashMap<String, Node>();
	//
	// // find top level beast element
	// NodeList nodes = doc.getElementsByTagName("*");
	// if (nodes == null || nodes.getLength() == 0) {
	// throw new Exception("Expected top level beast element in XML");
	// }
	// Node topNode = nodes.item(0);
	// initIDNodeMap(topNode);
	// parseNameSpaceAndMap(topNode);
	//
	// NodeList children = topNode.getChildNodes();
	// if (children.getLength() == 0) {
	// throw new Exception("Need at least one child element");
	// }
	// int i = children.getLength() - 1;
	// while (i >= 0 && (children.item(i).getNodeType() != Node.ELEMENT_NODE ||
	// !children.item(i).getNodeName().equals("run"))) {
	// i--;
	// }
	// if (i < 0) {
	// i = children.getLength() - 1;
	// while (i >= 0 && children.item(i).getNodeType() != Node.ELEMENT_NODE) {
	// i--;
	// }
	// }
	// if (i < 0) {
	// throw new Exception("Need at least one child element");
	// }
	//
	// Plugin plugin = createObject(children.item(i), PLUGIN_CLASS, null);
	// initPlugins();
	// return plugin;
	// } // parseFragment
	//
	// /**
	// * Parse XML fragment that will be wrapped in a beast element
	// * before parsing. This allows for ease of creating Plugin objects,
	// * like this:
	// * Tree tree = (Tree) new
	// XMLParser().parseBareFragment("<tree spec='beast.util.TreeParser' newick='((1:1,3:1):1,2:2)'/>");
	// * to create a simple tree.
	// */
	// public Plugin parseBareFragment(String sXML, boolean bInitialize) throws
	// Exception {
	// // get rid of XML processing instruction
	// sXML = sXML.replaceAll("<\\?xml[^>]*>", "");
	// if (sXML.indexOf("<beast") > -1) {
	// return parseFragment(sXML, bInitialize);
	// } else {
	// return parseFragment("<beast>" + sXML + "</beast>", bInitialize);
	// }
	// }
	//
	// public List<Plugin> parseBareFragments(String sXML, boolean bInitialize)
	// throws Exception {
	// m_bInitialize = bInitialize;
	// // parse the XML fragment into a DOM document
	// DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	// doc = factory.newDocumentBuilder().parse(new InputSource(new
	// StringReader(sXML)));
	// doc.normalize();
	// processPlates();
	//
	// // find top level beast element
	// NodeList nodes = doc.getElementsByTagName("*");
	// if (nodes == null || nodes.getLength() == 0) {
	// throw new Exception("Expected top level beast element in XML");
	// }
	// Node topNode = nodes.item(0);
	// initIDNodeMap(topNode);
	// parseNameSpaceAndMap(topNode);
	//
	// NodeList children = topNode.getChildNodes();
	// List<Plugin> plugins = new ArrayList<Plugin>();
	// for (int i = 0; i < children.getLength(); i++) {
	// if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
	// Plugin plugin = createObject(children.item(i), PLUGIN_CLASS, null);
	// plugins.add(plugin);
	// }
	// }
	// initPlugins();
	// return plugins;
	// }

	/**
	 * parse BEAST file as DOM document
	 * 
	 * @throws Exception
	 */
	public void parse() throws JSONParserException {
		// find top level beast element
		JSONObject nodes = doc;
		if (nodes == null || nodes.keySet().size() == 0) {
			throw new JSONParserException(doc, "Expected top level beast element in XML", 1001);
		}
		double fVersion = getAttributeAsDouble(nodes, "version");
		if (fVersion < 2.0 || fVersion == Double.MAX_VALUE) {
			throw new JSONParserException(nodes, "Wrong version: only versions > 2.0 are supported", 101);
		}

		initIDNodeMap(doc);

		parseNameSpaceAndMap(doc);

		// parseState();
		parseRunElement(doc);
		initPlugins();
	} // parse

	/**
	 * Traverse DOM beast.tree and grab all nodes that have an 'id' attribute
	 * Throw exception when a duplicate id is encountered
	 * 
	 * @param node
	 * @throws Exception
	 */
	void initIDNodeMap(JSONObject node) throws JSONParserException {
		String ID = getID(node);
		if (ID != null) {
			if (IDNodeMap.containsKey(ID)) {
				throw new JSONParserException(node, "IDs should be unique. Duplicate id '" + ID + "' found", 104);
			}
			IDNodeMap.put(ID, node);
		}
		for (Object key : node.keySet()) {
			try {
				Object o = node.get((String) key);
				if (o instanceof JSONObject) {
					initIDNodeMap((JSONObject) o);
				}
				if (o instanceof JSONArray) {
					JSONArray list = (JSONArray) o;
					for (int i = 0; i < list.length(); i++) {
						Object o2 = list.get(i);
						if (o2 instanceof JSONObject) {
							initIDNodeMap((JSONObject) o2);
						}
					}
				}
			} catch (JSONException e) {
				throw new JSONParserException(node, e.getMessage(), 1002);
			}
		}
	}

	/**
	 * find out namespaces (beast/@namespace attribute) and element to class
	 * maps, which reside in beast/map elements <beast version='2.0'
	 * namespace='snap:beast.util'> <map
	 * name='snapprior'>snap.likelihood.SnAPPrior</map> <map
	 * name='snaplikelihood'>snap.likelihood.SnAPTreeLikelihood</map>
	 * 
	 * @param topNode
	 * @throws JSONParserException
	 */
	void parseNameSpaceAndMap(JSONObject topNode) throws JSONParserException {
		// process namespaces
		if (topNode.has("namespace")) {
			String nameSpace = getAttribute(topNode, "namespace");
			setNameSpace(nameSpace);
		} else {
			// make sure that the default namespace is in there
			if (nameSpaces == null) {
				nameSpaces = new String[1];
				nameSpaces[0] = "";
			}
		}

		// // process map elements
		// NodeList nodes = doc.getElementsByTagName(MAP_ELEMENT);
		// for (int i = 0; i < nodes.getLength(); i++) {
		// Node child = nodes.item(i);
		// String sName = getAttribute(child, "name");
		// if (sName == null) {
		// throw new XMLParserException(child,
		// "name attribute expected in map element", 300);
		// }
		// if (!element2ClassMap.containsKey(sName)) {
		// // throw new XMLParserException(child, "name '" + sName +
		// "' is already defined as " + m_sElement2ClassMap.get(sName), 301);
		// // }
		//
		// // get class
		// String sClass = child.getTextContent();
		// // remove spaces
		// sClass = sClass.replaceAll("\\s", "");
		// // go through namespaces in order they are declared to find the
		// correct class
		// boolean bDone = false;
		// for (String sNameSpace : m_sNameSpaces) {
		// try {
		// // sanity check: class should exist
		// if (!bDone && Class.forName(sNameSpace + sClass) != null) {
		// element2ClassMap.put(sName, sClass);
		// System.err.println(sName + " => " + sNameSpace + sClass);
		// String reserved = getAttribute(child, "reserved");
		// if (reserved != null && reserved.toLowerCase().equals("true")) {
		// reservedElements.add(sName);
		// }
		//
		// bDone = true;
		// }
		// } catch (ClassNotFoundException e) {
		// //System.err.println("Not found " + e.getMessage());
		// // TODO: handle exception
		// }
		// }
		// }
		// }
	} // parseNameSpaceAndMap

	public void setNameSpace(String nameSpaceStr) {
		String[] nameSpaces = nameSpaceStr.split(":");
		// append dot after every non-zero namespace
		this.nameSpaces = new String[nameSpaces.length + 1];
		int i = 0;
		for (String nameSpace : nameSpaces) {
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

	void parseRunElement(JSONObject topNode) throws JSONParserException {
		// find beast element
		try {
		Object o = doc.get(XMLParser.BEAST_ELEMENT);
		if (o == null) {
			throw new JSONParserException(topNode, "Expected " + XMLParser.BEAST_ELEMENT + " top level object in file", 102);
		}
		if (!(o instanceof JSONArray)) {
			throw new JSONParserException(topNode, "Expected " + XMLParser.BEAST_ELEMENT + " to be a list", 1020);
		}
		JSONArray analysis = (JSONArray) o;
		runnable = null;
		for (int i = 0; i < analysis.length(); i++) {
			o = analysis.get(i);
			if (!(o instanceof JSONObject)) {
				throw new JSONParserException(topNode, XMLParser.BEAST_ELEMENT + " should only contain objects", 1021);
			}
			JSONObject node = (JSONObject) o;
			o = createObject(node, RUNNABLE_CLASS, null);
			if (o instanceof Runnable) {
				if (runnable != null) {
					 throw new JSONParserException(node, "Expected only one runnable element in file",  103);
				}
				runnable = (Runnable) o;
			}
		}
		if (runnable == null) {
			 throw new JSONParserException(topNode, "Expected at least one runnable element in file",  1030);
		}
		} catch (JSONException e) {
			throw new JSONParserException(topNode, e.getMessage(), 1004);
		}
	} // parseRunElement

	/**
	 * Check that plugin is a class that is assignable to class with name
	 * sClass. This involves a parameter clutch to deal with non-real
	 * parameters. This needs a bit of work, obviously...
	 */
	boolean checkType(String className, BEASTInterface plugin) throws JSONParserException {
		// parameter clutch
		if (plugin instanceof Parameter<?>) {
			for (String nameSpace : nameSpaces) {
				//nameSpace = nameSpace.replaceAll("beast", "yabby");
				if ((nameSpace + className).equals(RealParameter.class.getName())) {
					return true;
				}
			}
		}
		if (className.equals(INPUT_CLASS)) {
			return true;
		}
		for (String nameSpace : nameSpaces) {
			//nameSpace = nameSpace.replaceAll("beast", "yabby");
			try {
				if (Class.forName(nameSpace + className).isInstance(plugin)) {
					return true;
				}
			} catch (Exception e) {
				// ignore
			}
		}
		return false;
	} // checkType

	BEASTInterface createObject(JSONObject node, String className, BEASTInterface parent) throws JSONParserException {
		//className = className.replaceAll("beast", "yabby");
		// try the IDMap first
		String ID = getID(node);

		if (ID != null) {
			if (IDMap.containsKey(ID)) {
				BEASTInterface beastObject = IDMap.get(ID);
				if (checkType(className, beastObject)) {
					return beastObject;
				}
				throw new JSONParserException(node, "id=" + ID + ". Expected object of type " + className + " instead of "
						+ beastObject.getClass().getName(), 105);
			}
		}

		String IDRef = getIDRef(node);
		if (IDRef != null) {
			// produce warning if there are other attributes than idref
			if (node.keySet().size() > 1) {
				// check if there is just 1 attribute
				System.err.println("Element " + getAttribute((JSONObject) node.getParent(), "name") + " found with idref='" + IDRef
						+ "'. All other attributes are ignored.\n");
			}
			if (IDMap.containsKey(IDRef)) {
				BEASTInterface beastObject = IDMap.get(IDRef);
				if (checkType(className, beastObject)) {
					return beastObject;
				}
				throw new JSONParserException(node, "id=" + IDRef + ". Expected object of type " + className + " instead of "
						+ beastObject.getClass().getName(), 106);
			} else if (IDNodeMap.containsKey(IDRef)) {
				BEASTInterface beastObject = createObject(IDNodeMap.get(IDRef), className, parent);
				if (checkType(className, beastObject)) {
					return beastObject;
				}
				throw new JSONParserException(node, "id=" + IDRef + ". Expected object of type " + className + " instead of "
						+ beastObject.getClass().getName(), 107);
			}
			throw new JSONParserException(node, "Could not find object associated with idref " + IDRef, 170);
		}
		// it's not in the ID map yet, so we have to create a new object
		String specClass = className;
		// String sElementName = node.getNodeName();

		// if (element2ClassMap.containsKey(sElementName)) {
		// sSpecClass = element2ClassMap.get(sElementName);
		// }
		String spec = getAttribute(node, "spec");
		if (spec != null) {
			specClass = spec;
		}
		//specClass = specClass.replaceAll("beast", "yabby");

		Object o = null;
		// try to create object from sSpecName, taking namespaces in account
		try {
			boolean bDone = false;
			for (String nameSpace : nameSpaces) {
				try {
					if (!bDone) {
						//nameSpace = nameSpace.replaceAll("beast", "yabby");
						Class<?> c = Class.forName(nameSpace + specClass); 
						o = c.newInstance();
						bDone = true;
						break;
					}
				} catch (InstantiationException e) {
					// we only get here when the class exists, but cannot be
					// created
					// for instance because it is abstract
					throw new Exception("Cannot instantiate class. Please check the spec attribute.");
				} catch (ClassNotFoundException e) {
					// ignore -- class is probably in another name space
				}
			}
			if (!bDone) {
				throw new Exception("Class could not be found. Did you mean " + guessClass(specClass) + "?");
				// throw new ClassNotFoundException(sSpecClass);
			}
			// hack required to make log-parsing easier
			if (o instanceof State) {
				state = (State) o;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new JSONParserException(node, "Cannot create class: " + specClass + ". " + e.getMessage(), 122);
		}
		// sanity check
		if (!(o instanceof BEASTInterface)) {
			// if (o instanceof Input) {
			// // if we got this far, it is a basic input,
			// // that is, one of the form <input name='xyz'>value</input>
			// String sName = getAttribute(node, "name");
			// if (sName == null) {
			// sName = "value";
			// }
			// String sText = node.getTextContent();
			// if (sText.length() > 0) {
			// setInput(node, parent, sName, sText);
			// }
			// return null;
			// } else {
			throw new JSONParserException(node, "Expected object to be instance of BEASTObject", 108);
			// }
		}
		// set id
		BEASTInterface beastObject = (BEASTInterface) o;
		beastObject.setID(ID);
		register(node, beastObject);
		// process inputs
		parseInputs(beastObject, node);
		// initialise
		if (initialise) {
			try {
				beastObject.validateInputs();
				objectsWaitingToInit.add(new BEASTObjectWrapper(beastObject, node));
				// plugin.initAndValidate();
			} catch (Exception e) {
				// next lines for debugging only
				// plugin.validateInputs();
				// plugin.initAndValidate();
				e.printStackTrace();
				throw new JSONParserException(node, "validate and intialize error: " + e.getMessage(), 110);
			}
		}
		return beastObject;
	} // createObject

	/**
	 * find closest matching class to named class *
	 */
	String guessClass(String className) {
		String name = className;
		if (className.contains(".")) {
			name = className.substring(className.lastIndexOf('.') + 1);
		}
		List<String> beastObjectNames = AddOnManager.find(beast.core.BEASTInterface.class, AddOnManager.IMPLEMENTATION_DIR);
		int bestDistance = Integer.MAX_VALUE;
		String closest = null;
		for (String beastObjectName : beastObjectNames) {
			String className2 = beastObjectName.substring(beastObjectName.lastIndexOf('.') + 1);
			int distance = getLevenshteinDistance(name, className2);

			if (distance < bestDistance) {
				bestDistance = distance;
				closest = beastObjectName;
			}
		}
		return closest;
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

		int p[] = new int[n + 1]; // 'previous' cost array, horizontally
		int d[] = new int[n + 1]; // cost array, horizontally
		int _d[]; // placeholder to assist in swapping p and d

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
				// minimum of cell to the left+1, to the top+1, diagonally left
				// and up +cost
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

	void parseInputs(BEASTInterface parent, JSONObject node) throws JSONParserException {
		// sanity check: all attributes should be valid input names
		for (String name : node.keySet()) {
			if (!(name.equals("id") || name.equals("idref") || name.equals("spec") || name.equals("name"))) {
				try {
					parent.getInput(name);
				} catch (Exception e) {
					throw new JSONParserException(node, e.getMessage(), 1006);
				}
			}
		}

		if (node.keySet() != null) {
			try {
				// parse inputs in occurrance of inputs in the parent object
				// this determines the order in which initAndValidate is called
				List<Input<?>> inputs = parent.listInputs();
				Set<String> done = new HashSet<String>();
				for (Input<?> input : inputs) {
					String name = input.getName();
					processInput(name, node, parent);
					done.add(name);
				}
				
				for (String name : node.keySet()) {
					if (!done.contains(name)) {
						// this can happen with Maps
						processInput(name, node, parent);
					}
				}
			} catch (JSONParserException e) {
				throw e;
			} catch (Exception e) {
				throw new JSONParserException(node, e.getMessage(), 1005);
			}
		}
		
		
		// // process element nodes
		// NodeList children = node.getChildNodes();
		// String sText = "";
		// for (int i = 0; i < children.getLength(); i++) {
		// Node child = children.item(i);
		// if (child.getNodeType() == Node.ELEMENT_NODE) {
		// String sElement = child.getNodeName();
		// // resolve name of the input
		// String sName = getAttribute(child, "name");
		// if (sName == null) {
		// sName = sElement;
		// }
		// // resolve base class
		// String sClass = PLUGIN_CLASS;
		// if (element2ClassMap.containsKey(sElement)) {
		// sClass = element2ClassMap.get(sElement);
		// }
		// Plugin childItem = createObject(child, sClass, parent);
		// if (childItem != null) {
		// setInput(node, parent, sName, childItem);
		// }
		// nChildElements++;
		// } else if (child.getNodeType() == Node.CDATA_SECTION_NODE ||
		// child.getNodeType() == Node.TEXT_NODE) {
		// sText += child.getTextContent();
		// }
		// }
		// if (!sText.matches("\\s*")) {
		// setInput(node, parent, "value", sText);
		// }
		//
		// if (nChildElements == 0) {
		// String sContent = node.getTextContent();
		// if (sContent != null && sContent.length() > 0 &&
		// sContent.replaceAll("\\s", "").length() > 0) {
		// try {
		// setInput(node, parent, "value", sContent);
		// } catch (Exception e) {
		// //
		// }
		// }
		// }

		// fill in missing inputs, if an input provider is available
		try {
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
		} catch (Exception e) {
			throw new JSONParserException(node, e.getMessage(), 1006);			
		}
	} // setInputs

	private void processInput(String name, JSONObject node, BEASTInterface parent) throws Exception {
		if (node.has(name)) {
			if (!(name.equals("id") || name.equals("idref") || name.equals("spec") || name.equals("name"))) {
				Object o = node.get(name);
				if (o instanceof String) {
					String value = (String) o;
					if (value.startsWith("@")) {
						String IDRef = value.substring(1);
						JSONObject element = new JSONObject();
						element.put("idref", IDRef);
						BEASTInterface beastObject = createObject(element, YOBJECT_CLASS, parent);
						setInput(node, parent, name, beastObject);
					} else {
						setInput(node, parent, name, value);
					}
				} else if (o instanceof Number) {
					parent.setInputValue(name, o);
				} else if (o instanceof Boolean) {
					parent.setInputValue(name, o);
				} else if (o instanceof JSONObject) {
					JSONObject child = (JSONObject) o;
					String className = getClassName(child, name, parent);
					BEASTInterface childItem = createObject(child, className, parent);
					if (childItem != null) {
						setInput(node, parent, name, childItem);
					}
					// nChildElements++;
				} else if (o instanceof JSONArray) {
					JSONArray list = (JSONArray) o;
					for (int i = 0; i < list.length(); i++) {
						Object o2 = list.get(i);
						if (o2 instanceof JSONObject) {
							JSONObject child = (JSONObject) o2;
							String className = getClassName(child, name, parent);
							BEASTInterface childItem = createObject(child, className, parent);
							if (childItem != null) {
								setInput(node, parent, name, childItem);
							}
						} else {
							parent.setInputValue(name, o2);									
						}
					}
				} else {
					throw new Exception("Developer error: Don't know how to handle this JSON construction");
				}
			}
		}		
	}

	void setInput(JSONObject node, BEASTInterface beastObject, String name, BEASTInterface beastObject2) throws JSONParserException {
		try {
			Input<?> input = beastObject.getInput(name);
			// test whether input was not set before, this is done by testing
			// whether input has default value.
			// for non-list inputs, this should be true if the value was not
			// already set before
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
				throw new JSONParserException(node, e.getMessage() + " expected '" + type + "' but got '"
						+ beastObject2.getClass().getName().replaceAll(".*\\.", "") + "'", 123);
			} else {
				throw new JSONParserException(node, e.getMessage(), 130);
			}
		}
	}

	void setInput(JSONObject node, BEASTInterface beastObject, String name, String value) throws JSONParserException {
		try {
			beastObject.setInputValue(name, value);
			return;
		} catch (Exception e) {
			try {
				beastObject.setInputValue(name, value);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			throw new JSONParserException(node, e.getMessage(), 124);
		}
	}

	/**
	 * records id in IDMap, for ease of retrieving BeastObjects associated with
	 * idrefs *
	 */
	void register(JSONObject node, BEASTInterface beastObject) {
		String ID = getID(node);
		if (ID != null) {
			IDMap.put(ID, beastObject);
		}
	}

	public static String getID(JSONObject node) {
		return getAttribute(node, "id");
	}

	public static String getIDRef(JSONObject node) {
		return getAttribute(node, "idref");
	}

	/**
	 * get string value of attribute with given name as opposed to double or
	 * integer value (see methods below) *
	 */
	public static String getAttribute(JSONObject node, String attName) {
		if (node.has(attName)) {
			try {
				return node.get(attName).toString();
			} catch (JSONException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * get integer value of attribute with given name *
	 */
	public static int getAttributeAsInt(JSONObject node, String attName) {
		String att = getAttribute(node, attName);
		if (att == null) {
			return -1;
		}
		return Integer.parseInt(att);
	}

	/**
	 * get double value of attribute with given name 
	 * @throws JSONParserException *
	 */
	public static double getAttributeAsDouble(JSONObject node, String attName) throws JSONParserException {
		String att = getAttribute(node, attName);
		if (att == null) {
			return Double.MAX_VALUE;
		}
		try {
			return Double.parseDouble(att);			
		} catch (NumberFormatException e) {
			throw new JSONParserException(node, "Could not parse number " + att, 1003);
		}
	}

	public interface RequiredInputProvider {
		Object createInput(BEASTInterface beastObject, Input<?> input, PartitionContext context);
	}

	public void setRequiredInputProvider(RequiredInputProvider provider, PartitionContext context) {
		requiredInputProvider = provider;
		partitionContext = context;
	}

	String getClassName(JSONObject child, String name, BEASTInterface parent) throws Exception {
		String className = getAttribute(child, "spec");
		if (className == null) {
			Input<?> input = parent.getInput(name);
			Class<?> type = input.getType();
			if (type == null) {
				input.determineClass(parent);
				type = input.getType();
			}
			className = type.getName();
		}
		if (element2ClassMap.containsKey(className)) {
			className = element2ClassMap.get(className);
		}
		return className;
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
			JSONParser parser = new JSONParser();
			BEASTInterface beastObject = parser.parseFile(new File(args[0]));
			// restore stdout
			System.setOut(out);
			System.out.println(new XMLProducer().toXML(beastObject));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

} // class JSONParser
