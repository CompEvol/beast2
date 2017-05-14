/*
 * File JSONParser.java
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





import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import beast.app.beauti.PartitionContext;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Param;
import beast.core.Runnable;
import beast.core.State;
import beast.core.parameter.Map;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
import beast.util.XMLParser.NameValuePair;


/** parses BEAST JSON file into a set of BEAST objects **/
public class JSONParser {
	final static String INPUT_CLASS = Input.class.getName();
	final static String BEAST_OBJECT_CLASS = BEASTInterface.class.getName();
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
		element2ClassMap = new HashMap<>();
		reservedElements = new HashSet<>();
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
	 * beastObject have been parsed
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

	public Runnable parseFile(File file) throws IOException, JSONException, JSONParserException {
		// parse the JSON file into a JSONObject
		
		// first get rid of comments: remove all text on lines starting with space followed by //
		// keep line breaks so that error reporting indicates the correct line.
		BufferedReader fin = new BufferedReader(new FileReader(file));
		StringBuffer buf = new StringBuffer();
		String str = null;
		while (fin.ready()) {
			str = fin.readLine();
			if (!str.matches("^\\s*//.*")) {
				buf.append(str);
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

		
		IDMap = new HashMap<>();
		likelihoodMap = new HashMap<>();
		IDNodeMap = new HashMap<>();

		parse();
		// assert m_runnable == null || m_runnable instanceof Runnable;
		if (runnable != null)
			return runnable;
		else {
			throw new IOException("Run element does not point to a runnable object.");
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
	                		String att = (String) attr;
	                		att = att.replaceAll("\\$\\(" + var + "\\)", value);
	                		jsonobject.put(key, att);
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
	// public List<BEASTObject> parseTemplate(String xml, HashMap<String, BEASTObject>
	// idMap, boolean initialise) {
	// m_bInitialize = initialise;
	// // parse the XML file into a DOM document
	// DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	// //factory.setValidating(true);
	// doc = factory.newDocumentBuilder().parse(new InputSource(new
	// StringReader(xml)));
	// processPlates();
	//
	// IDMap = idMap;//new HashMap<>();
	// likelihoodMap = new HashMap<>();
	// IDNodeMap = new HashMap<>();
	//
	// List<BEASTObject> beastObjects = new ArrayList<>();
	//
	// // find top level beast element
	// NodeList nodes = doc.getElementsByTagName("*");
	// if (nodes == null || nodes.getLength() == 0) {
	// throw new Exception("Expected top level beast element in XML");
	// }
	// Node topNode = nodes.item(0);
	// // sanity check that we are reading a beast 2 file
	// double version = getAttributeAsDouble(topNode, "version");
	// if (!topNode.getNodeName().equals(BEAST_ELEMENT) || version < 2.0 ||
	// version == Double.MAX_VALUE) {
	// return beastObjects;
	// }
	// // only process templates
	// // String typeName = getAttribute(topNode, "type");
	// // if (typeName == null || !typeName.equals("template")) {
	// // return beastObjects;
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
	// Log.warning.println(child.getNodeName());
	// if (!child.getNodeName().equals(MAP_ELEMENT)) {
	// beastObjects.add(createObject(child, PLUGIN_CLASS, null));
	// }
	// }
	// }
	// initPlugins();
	// return beastObjects;
	// } // parseTemplate

	private void initBEASTObjects() throws JSONParserException {
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
			// beastObject.validateInputs();
			// beastObject.initAndValidate();
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
	 * 
	 */
	void processPlates(JSONObject node) throws IOException, JSONException, JSONParserException {
		for (String key : node.keySet()) {
			Object o = node.get(key);
			if (o instanceof JSONObject) {
				JSONObject child = (JSONObject) o;
				processPlates(child);
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

	private void unrollPlate(JSONArray list, JSONObject plate) throws IOException, JSONParserException, JSONException {
		int index = list.indexOf(plate);
		if (index < 0) {
			throw new RuntimeException("Programmer error: plate should be in list");
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
					// T O D O Auto-generated catch block
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
					// T O D O Auto-generated catch block
					e.printStackTrace();
				}
			}
			return copy;			
		}
		throw new RuntimeException("How did we get here?");
	} // unrollPlate
	

	 /**
	 * Parse an JSON fragment representing a list of BEASTObjects
	 */
    public List<Object> parseFragment(final String json, final boolean initialise) throws JSONParserException, JSONException {
        this.initialise = initialise;
		doc = new JSONObject(json);

		// find top level beast element
		JSONObject nodes = doc;
		if (nodes == null || nodes.keySet().size() == 0) {
			throw new JSONParserException(doc, "Expected top level 'beast' element in JSON fragment", 1001);
		}
		double version = getAttributeAsDouble(nodes, "version");
		if (version < 2.0 || version == Double.MAX_VALUE) {
			throw new JSONParserException(nodes, "Wrong version: only versions > 2.0 are supported", 101);
		}

		initIDNodeMap(doc);

		parseNameSpaceAndMap(doc);
  
		List<Object> objects = new ArrayList<>();
		try {
			// find beast element
			Object o = doc.get(XMLParser.BEAST_ELEMENT);
			if (o == null) {
				throw new JSONParserException(nodes, "Expected " + XMLParser.BEAST_ELEMENT + " top level object in file", 102);
			}
			if (!(o instanceof JSONArray)) {
				throw new JSONParserException(nodes, "Expected " + XMLParser.BEAST_ELEMENT + " to be a list", 1020);
			}
			JSONArray analysis = (JSONArray) o;
			for (int i = 0; i < analysis.length(); i++) {
				o = analysis.get(i);
				if (!(o instanceof JSONObject)) {
					throw new JSONParserException(nodes, XMLParser.BEAST_ELEMENT + " should only contain objects", 1021);
				}
				JSONObject node = (JSONObject) o;
				o = createObject(node, Object.class.getName());
				objects.add(o);
			}
		} catch (JSONException e) {
			throw new JSONParserException(nodes, e.getMessage(), 1004);
		}
		
		if (initialise) {
			initBEASTObjects();
		}
        return objects;
    } // parseFragment

    //
	// /**
	// * Parse XML fragment that will be wrapped in a beast element
	// * before parsing. This allows for ease of creating BEASTObject objects,
	// * like this:
	// * Tree tree = (Tree) new
	// XMLParser().parseBareFragment("<tree spec='beast.util.TreeParser' newick='((1:1,3:1):1,2:2)'/>");
	// * to create a simple tree.
	// */
	// public BEASTObject parseBareFragment(String xml, boolean initialise) throws
	// Exception {
	// // get rid of XML processing instruction
	// xml = xml.replaceAll("<\\?xml[^>]*>", "");
	// if (xml.indexOf("<beast") > -1) {
	// return parseFragment(xml, initialise);
	// } else {
	// return parseFragment("<beast>" + xml + "</beast>", initialise);
	// }
	// }
	//
	// public List<BEASTObject> parseBareFragments(String xml, boolean initialise)
    //{
	// m_bInitialize = initialise;
	// // parse the XML fragment into a DOM document
	// DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	// doc = factory.newDocumentBuilder().parse(new InputSource(new
	// StringReader(xml)));
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
	// List<BEASTObject> beastObjects = new ArrayList<>();
	// for (int i = 0; i < children.getLength(); i++) {
	// if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
	// BEASTObject beastObject = createObject(children.item(i), PLUGIN_CLASS, null);
	// beastObjects.add(beastObject);
	// }
	// }
	// initPlugins();
	// return beastObjects;
	// }

	/**
	 * parse BEAST file as DOM document
	 * 
	 * @throws JSONParserException
	 */
	public void parse() throws JSONParserException {
		// find top level beast element
		JSONObject nodes = doc;
		if (nodes == null || nodes.keySet().size() == 0) {
			throw new JSONParserException(doc, "Expected top level beast element in JSON", 1001);
		}
		double version = getAttributeAsDouble(nodes, "version");
		if (version < 2.0 || version == Double.MAX_VALUE) {
			throw new JSONParserException(nodes, "Wrong version: only versions > 2.0 are supported", 101);
		}

		initIDNodeMap(doc);

		parseNameSpaceAndMap(doc);

		// parseState();
		parseRunElement(doc);
		initBEASTObjects();
	} // parse

	/**
	 * Traverse DOM beast.tree and grab all nodes that have an 'id' attribute
	 * Throw exception when a duplicate id is encountered
	 * 
	 * @param node
	 * @throws JSONParserException
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

		// process map elements
		if (topNode.has("map")) {
			try {
				Object o = topNode.get("map");
				if (o instanceof JSONArray) {
					JSONArray maps = (JSONArray) o;
					for (int i = 0; i < maps.length(); i++) {
						Object o2 = maps.get(i);
						if (o2 instanceof JSONObject) {
							JSONObject map = (JSONObject) o2;
							if (map.has("name") && map.has("value") && map.length()==2) {
								String mapName = map.getString("name");
								String clazz = map.getString("value");
								 // remove spaces
								 clazz = clazz.replaceAll("\\s", "");
								 // go through namespaces in order they are declared to find the
								 // correct class
								 boolean done = false;
								 for (String nameSpace : nameSpaces) {
									 // sanity check: class should exist
									 try {
										if (!done && Class.forName(nameSpace + clazz) != null) {
											 element2ClassMap.put(mapName, clazz);
											 Log.warning.println(mapName + " => " + nameSpace + clazz);
											 done = true;
											 //String reserved = getAttribute(child, "reserved");
											 //if (reserved != null && reserved.toLowerCase().equals("true")) {
											 //	 reservedElements.add(name);
											 //}
										 }
									} catch (ClassNotFoundException e) {
										// ignore -- it may be in another namespace
										// there appears to be no good way to check a class exists other than to try and create one
										// and test whether no exception is thrown.
									}
								 }
								 if (!done) {
									 Log.warning.println("WARNING: no class could be found for map " + mapName + " => " + clazz +". This map is ignored.");
								 }

							} else { 
								throw new JSONParserException(map, "Expected a name and a value and nothing else", 1016);
							}
						} else {
							throw new JSONParserException(topNode, "map should be a list of JSONObjects. Use for example map:[{name:\"OneOnX\", value:\"beast.math.distributions.OneOnX\"}] for a single map", 1013);
						}
					}
				} else {
					throw new JSONParserException(topNode, "map should be a list. Use for example map:[{name:\"OneOnX\", value:\"beast.math.distributions.OneOnX\"}] for a single map", 1014);
				}
			} catch (JSONException e) {
				// should never get here, unless something is really wrong
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}		
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
			o = createObject(node, RUNNABLE_CLASS);
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
	 * Check that BEASTObject is a class that is assignable to class with name
	 * className. This involves a parameter clutch to deal with non-real
	 * parameters. This needs a bit of work, obviously...
	 */
	boolean checkType(String className, BEASTInterface beastObject) throws JSONParserException {
		// parameter clutch
		if (beastObject instanceof Parameter<?>) {
			for (String nameSpace : nameSpaces) {
				if ((nameSpace + className).equals(RealParameter.class.getName())) {
					return true;
				}
			}
		}
		if (className.equals(INPUT_CLASS)) {
			return true;
		}
		for (String nameSpace : nameSpaces) {
			try {
				if (Class.forName(nameSpace + className).isInstance(beastObject)) {
					return true;
				}
			} catch (Exception e) {
				// ignore
			}
		}
		return false;
	} // checkType

	/** create a BEAST object based on the info in the node.
	 * @param node
	 * @param className default class name -- this means a spec attribute does not need to be 
	 * specified if all outputs of this BEAST object have as Input type the class of this object.
	 * @return
	 * @throws JSONParserException
	 */
	BEASTInterface createObject(JSONObject node, String className) throws JSONParserException {
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
				Log.warning.println("Element " + getAttribute((JSONObject) node.getParent(), "name") + " found with idref='" + IDRef
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
				BEASTInterface beastObject = createObject(IDNodeMap.get(IDRef), className);
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
		String elementName = getElementName(node);
		if (element2ClassMap.containsKey(elementName)) {
			specClass = element2ClassMap.get(elementName);
		}
		String spec = getAttribute(node, "spec");
		if (spec != null) {
			specClass = spec;
		}

		String clazzName = null;
		// determine clazzName from specName, taking name spaces in account
		for (String nameSpace : nameSpaces) {
			if (clazzName == null) {
				if (XMLParserUtils.beastObjectNames.contains(nameSpace + specClass)) {
					clazzName = nameSpace + specClass;
					break;
				}
			}
		}
		if (clazzName == null) {
			// try to create the old-fashioned way by creating the class
            boolean isDone = false;
            for (final String nameSpace : nameSpaces) {
                try {
                    if (!isDone) {
                        Class.forName(nameSpace + specClass);
                        clazzName = nameSpace + specClass;
                        isDone = true;
                    }
                } catch (ClassNotFoundException e) {
                    // class does not exist -- try another namespace
                }
            }
		}
		if (clazzName == null) {
			throw new JSONParserException(node, "Class could not be found. Did you mean " + XMLParserUtils.guessClass(specClass) + "?", 1017);
			// throw new ClassNotFoundException(specClass);
		}
				
		// sanity check		
		try {
			Class<?> clazz = Class.forName(clazzName);
			if (!BEASTInterface.class.isAssignableFrom(clazz)) {
				// if (o instanceof Input) {
				// // if we got this far, it is a basic input,
				// // that is, one of the form <input name='xyz'>value</input>
				// String name = getAttribute(node, "name");
				// if (name == null) {
				// name = "value";
				// }
				// String text = node.getTextContent();
				// if (text.length() > 0) {
				// setInput(node, parent, name, text);
				// }
				// return null;
				// } else {
				throw new JSONParserException(node, "Expected object to be instance of BEASTObject", 108);
				// }
			}
		} catch (ClassNotFoundException e1) {
			// should never happen since clazzName is in the list of classes collected by the AddOnManager
			e1.printStackTrace();
			throw new RuntimeException(e1);
		}
		
		// process inputs
		List<NameValuePair> inputInfo = parseInputs(node, clazzName);
		BEASTInterface beastObject = createBeastObject(node, ID, clazzName, inputInfo);
		// initialise
		if (initialise) {
			try {
            	beastObject.determindClassOfInputs();
				beastObject.validateInputs();
				objectsWaitingToInit.add(new BEASTObjectWrapper(beastObject, node));
				// beastObject.initAndValidate();
			} catch (IllegalArgumentException e) {
				// next lines for debugging only
				// beastObject.validateInputs();
				// beastObject.initAndValidate();
				e.printStackTrace();
				throw new JSONParserException(node, "validate and intialize error: " + e.getMessage(), 110);
			}
		}
		return beastObject;
	} // createObject
	
	private String getElementName(JSONObject node) {
		Object o = node.getParent();
		if (o == null) {
			return null;
		}
		if (o instanceof JSONObject) {
			JSONObject parent = ((JSONObject) o);
			for (String s : parent.keySet()) {
				try {
					if (parent.get(s) == node) {
						return s;
					}
				} catch (JSONException e) {
					// should not get here
					e.printStackTrace();
				}
			}
		}
		if (o instanceof JSONArray) {
			JSONArray parent = ((JSONArray) o);
			Object o2 = parent.getParent();
			if (o2 == null) {
				return null;
			}
			if (o2 instanceof JSONObject) {
				JSONObject gparent = ((JSONObject) o2);
				for (String s : gparent.keySet()) {
					try {
						if (gparent.get(s) == parent) {
							return s;
						}
					} catch (JSONException e) {
						// should not get here
						e.printStackTrace();
					}
				}
			}
			
		}
		return null;
	}

	/** create BEASTInterface either using Inputs, or using annotated constructor **/
	private BEASTInterface createBeastObject(JSONObject node, String ID, String clazzName, List<NameValuePair> inputInfo) throws JSONParserException {
		BEASTInterface beastObject = useAnnotatedConstructor(node, ID, clazzName, inputInfo);
		if (beastObject != null) {
			return beastObject;
		}
		
		// create new instance using class name
		Object o = null;
		try {
			Class<?> c = Class.forName(clazzName); 
				o = c.newInstance();
		} catch (InstantiationException e) {
			// we only get here when the class exists, but cannot be
			// created for instance because it is abstract
			throw new JSONParserException(node, "Cannot instantiate class. Please check the spec attribute.", 1006);
		} catch (ClassNotFoundException e) {
			// ignore -- class was found in beastObjectNames before
		} catch (IllegalAccessException e) {
			// T O D O Auto-generated catch block
			e.printStackTrace();
			throw new JSONParserException(node, "Cannot access class. Please check the spec attribute.", 1011);
		}
		
		// set id
		beastObject = (BEASTInterface) o;
		beastObject.setID(ID);

		// hack required to make log-parsing easier
		if (o instanceof State) {
			state = (State) o;
		}

		// process inputs for annotated constructors
		for (NameValuePair pair : inputInfo) {
			setInput(node, beastObject, pair.name, pair.value);
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
			throw new JSONParserException(node, e.getMessage(), 1008);			
		}
		
		// sanity check: all attributes should be valid input names
		if (!(beastObject instanceof Map)) {
			for (String name : node.keySet()) {
				if (!(name.equals("id") || name.equals("idref") || name.equals("spec") || name.equals("name"))) {
					try {
						beastObject.getInput(name);
					} catch (Exception e) {
						throw new JSONParserException(node, e.getMessage(), 1009);
					}
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
	private BEASTInterface useAnnotatedConstructor(JSONObject node, String _id, String clazzName, List<NameValuePair> inputInfo) throws JSONParserException {
		Class<?> clazz = null;
		try {
			clazz = Class.forName(clazzName);
		} catch (ClassNotFoundException e) {
			// cannot get here, since we checked the class existed before
			e.printStackTrace();
		}
	    Constructor<?>[] allConstructors = clazz.getDeclaredConstructors();
	    for (Constructor<?> ctor : allConstructors) {
	    	// collect Param annotations on constructor parameters
	    	Annotation[][] annotations = ctor.getParameterAnnotations();
	    	List<Param> paramAnnotations = new ArrayList<>();
	    	for (Annotation [] a0 : annotations) {
		    	for (Annotation a : a0) {
		    		if (a instanceof Param) {
		    			paramAnnotations.add((Param) a);
		    		}
	    		}
	    	}
	    	
	    	for (NameValuePair pair : inputInfo) {
	    		pair.processed = false;
	    	}
	    	
	    	Class<?>[] types  = ctor.getParameterTypes();	    	
	    	if (types.length > 0 && paramAnnotations.size() == types.length) {
				try {
			    	// if all constructor parameters have Param annotations, try to call constructor
		    		// first, build up argument list, then create object
		    		Object [] args = new Object[types.length];
		    		for (int i = 0; i < types.length; i++) {
		    			Param param = paramAnnotations.get(i);
		    			Type type = types[i];
		    			if (type.getTypeName().equals("java.util.List")) {
		    				if (args[i] == null) {
		    					// no need to parameterise list due to type erasure
		    					args[i] = new ArrayList();
		    				}
		    				List<Object> values = XMLParser.getListOfValues(param, inputInfo);
		    				((List<Object>) args[i]).addAll(values);
		    			} else {
		    				args[i] = getValue(param, (Class<?>) type, inputInfo);
		    			}
		    		}
		    		
		    		// ensure all inputs are used
		    		boolean allUsed = true;
			    	for (NameValuePair pair : inputInfo) {
			    		if (!pair.processed) {
			    			allUsed= false;
			    		}
			    	}
	
			    	// if all inputs are used, call the constructor, otherwise, look for another constructor
			    	if (allUsed) {
			    		try {
							Object o = ctor.newInstance(args);
							BEASTInterface beastObject = (BEASTInterface) o;
							register(node, beastObject);
							return beastObject;
						} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							throw new JSONParserException(node, "Could not create object: " + e.getMessage(), 1012);
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

	/** get value from inputInfo, but use default if the Param name cannot be found in inputInfo
	 *  RRB: would like to combine with XMLParser.getValue, but this may get ugly due to incompatible XML/JSON types
	 */
	private Object getValue(Param param, Class<?> clazz, List<NameValuePair> inputInfo) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for (NameValuePair pair : inputInfo) {
			if (pair.name.equals(param.name())) {
				pair.processed = true;
				return pair.value;
			}
		}
		
		// could not find Param entry in inputInfo
		
		// check if this parameter is required or optional
		if (!param.optional()) {
			throw new IllegalArgumentException();
		}

		// try using a String constructor of the default value
        Constructor<?> ctor;
        String value = param.defaultValue();
        Object v = value; 
        try {
        	ctor = clazz.getDeclaredConstructor(String.class);
        } catch (NoSuchMethodException e) {
        	// we get here if there is not String constructor
        	// try integer constructor instead
        	try {
        		if (value.startsWith("0x")) {
        			v = Integer.parseInt(value.substring(2), 16);
        		} else {
        			v = Integer.parseInt(value);
        		}
            	ctor = clazz.getDeclaredConstructor(int.class);
            	
        	} catch (NumberFormatException e2) {
            	// could not parse as integer, try double instead
        		v = Double.parseDouble(value);
            	ctor = clazz.getDeclaredConstructor(double.class);
        	}
        }
        ctor.setAccessible(true);
        final Object o = ctor.newInstance(v);
        return o;
	}

	List<NameValuePair> parseInputs(JSONObject node, String className) throws JSONParserException {
		List<NameValuePair> inputInfo = new ArrayList<>();

		if (node.keySet() != null) {
			try {
				// parse inputs in occurrence of inputs in the parent object
				// this determines the order in which initAndValidate is called
				List<InputType> inputs = XMLParserUtils.listInputs(Class.forName(className), null);
				Set<String> done = new HashSet<>();
				for (InputType input : inputs) {
					String name = input.name;
					processInput(name, node, inputInfo, inputs);
					done.add(name);
				}
				
				for (String name : node.keySet()) {
					if (!done.contains(name)) {
						// this can happen with Maps
						processInput(name, node, inputInfo, inputs);
					}
				}
			} catch (JSONParserException e) {
				throw e;
			} catch (Exception e) {
				throw new JSONParserException(node, e.getMessage(), 1005);
			}
		}
		
		return inputInfo;
	} // setInputs

	
	private void processInput(String name, JSONObject node, List<NameValuePair> map, List<InputType> inputs) throws JSONParserException, JSONException {
		if (node.has(name)) {
			if (!(name.equals("id") || name.equals("idref") || name.equals("spec") || name.equals("name"))) {
				Object o = node.get(name);
				if (o instanceof String) {
					String value = (String) o;
					if (value.startsWith("@")) {
						String IDRef = value.substring(1);
						JSONObject element = new JSONObject();
						element.put("idref", IDRef);
						BEASTInterface beastObject = createObject(element, BEAST_OBJECT_CLASS);
						map.add(new NameValuePair(name, beastObject));
						//setInput(node, parent, name, beastObject);
					} else {
						map.add(new NameValuePair(name, value));
						//setInput(node, parent, name, value);
					}
				} else if (o instanceof Number) {
					map.add(new NameValuePair(name, o));
					//parent.setInputValue(name, o);
				} else if (o instanceof Boolean) {
					map.add(new NameValuePair(name, o));
					//parent.setInputValue(name, o);
				} else if (o instanceof JSONObject) {
					JSONObject child = (JSONObject) o;
					String className = getClassName(child, name, inputs);
					BEASTInterface childItem = createObject(child, className);
					if (childItem != null) {
						map.add(new NameValuePair(name, childItem));
						//setInput(node, parent, name, childItem);
					}
					// childElements++;
				} else if (o instanceof JSONArray) {
					JSONArray list = (JSONArray) o;
					for (int i = 0; i < list.length(); i++) {
						Object o2 = list.get(i);
						if (o2 instanceof JSONObject) {
							JSONObject child = (JSONObject) o2;
							String className = getClassName(child, name, inputs);
							BEASTInterface childItem = createObject(child, className);
							if (childItem != null) {
								map.add(new NameValuePair(name, childItem));
								//setInput(node, parent, name, childItem);
							}
						} else {
							map.add(new NameValuePair(name, o2));
							//parent.setInputValue(name, o2);									
						}
					}
				} else {
					throw new RuntimeException("Developer error: Don't know how to handle this JSON construction");
				}
			}
		}		
	}

	private void setInput(JSONObject node, BEASTInterface beastObject, String name, Object value) throws JSONParserException {
		try {
			final Input<?> input = beastObject.getInput(name);
			// test whether input was not set before, this is done by testing
			// whether input has default value.
			// for non-list inputs, this should be true if the value was not
			// already set before
			// for list inputs this is always true.
			if (input.get() == input.defaultValue) {
				beastObject.setInputValue(name, value);
			} else {
				throw new IOException("Multiple entries for non-list input " + input.getName());
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
					// T O D O: handle exception
				}
				throw new JSONParserException(node, e.getMessage() + " expected '" + type + "' but got '"
						+ value.getClass().getName().replaceAll(".*\\.", "") + "'", 123);
			} else {
				throw new JSONParserException(node, e.getMessage(), 130);
			}
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

	String getClassName(JSONObject child, String name, BEASTInterface parent) {
		String className = getAttribute(child, "spec");
		if (className == null) {
			final Input<?> input = parent.getInput(name);
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

	private String getClassName(JSONObject child, String name, List<InputType> inputs) {
		String className = getAttribute(child, "spec");
		if (className == null) {
			// derive type from Input
			for (InputType input : inputs) {
				if (input.name.equals(name)) {
					Class<?> type = input.type;
					if (type == null) {
						throw new RuntimeException("Programmer error: inputs should have their type set");
					}
					//if (type.isAssignableFrom(List.class)) {
					//	System.err.println("XX");
					//}
					className = type.getName();
				}
			}
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
			if (args.length > 1) {
		        FileWriter outfile = new FileWriter(args[1]);
		        outfile.write(new XMLProducer().toXML(beastObject));
		        outfile.close();
			} else {
				System.out.println(new XMLProducer().toXML(beastObject));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

} // class JSONParser
