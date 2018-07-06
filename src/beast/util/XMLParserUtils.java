package beast.util;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.InputForAnnotatedConstructor;
import beast.core.Param;
import beast.core.util.Log;

/**
 *
 * Provides basic functions for variable substitution and plates.
 *
 * @author Remco Bouckaert
 * @author Alexei Drummond
 */
public class XMLParserUtils {
	
	final static public List<String> beastObjectNames = PackageManager.find(beast.core.BEASTInterface.class, PackageManager.IMPLEMENTATION_DIR);


    /**
     * Expand plates in XML by duplicating the containing XML and replacing
     * the plate variable with the appropriate value.
     */
    public static void processPlates(Document doc, String plateElementName) {
    	Map<String, Node> idMap = new LinkedHashMap<>();
    	
        // process plate elements
        final NodeList nodes = doc.getElementsByTagName(plateElementName);
        // instead of processing all plates, process them one by one,
        // then check recursively for new plates that could have been
        // created when they are nested
        if (nodes.getLength() > 0) {
            Node node = nodes.item(0);
            final String var = node.getAttributes().getNamedItem("var").getNodeValue();
            final String rangeString = node.getAttributes().getNamedItem("range").getNodeValue();
            
            if (node.getAttributes().getNamedItem("fragment") != null) {
            	final String fragmentID = node.getAttributes().getNamedItem("fragment").getNodeValue();
            	Node fragment = getElementById(doc, fragmentID, idMap);
            	if (fragment == null) {
            		throw new RuntimeException("plate refers to fragment with id='" + fragmentID + "' that cannot be found");
            	}
            	fragment = fragment.cloneNode(true);
                node.getParentNode().replaceChild(fragment, node);
            	node = fragment;
           }
	
            final String[] valuesString = rangeString.split(",");

            // interpret values in the range of form x:y as all numbers between x and y inclusive
            List<String> vals = new ArrayList<>();
            for (final String valueString : valuesString) {
                if (valueString.indexOf(":") > 0) {
                	try {
	                    String[] range = valueString.split(":");
	                    int min = Integer.parseInt(range[0]);
	                    int max = Integer.parseInt(range[1]);
	                    for (int i = min; i <= max; i++) {
	                        vals.add(String.valueOf(i));
	                    }
                	} catch (NumberFormatException e) {
                		Log.warning.println("plate range value '" + valueString + "'contains a ':' but does not seem to be a range, (like 1:5).");
                		Log.warning.println("interpreting it as if it were not a range");
                        vals.add(valueString);
                	}
                } else {
                    vals.add(valueString);
                }
            }

            for (final String val : vals) {
                // copy children
                final NodeList children = node.getChildNodes();
                for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                    final Node child = children.item(childIndex);
                    final Node newChild = child.cloneNode(true);
                    replaceVariable(newChild, var, val);
                    node.getParentNode().insertBefore(newChild, node);
                }
            }
            node.getParentNode().removeChild(node);
            processPlates(doc,plateElementName);
        }
    } // processPlates
    

    static  Node getElementById(Document doc, String id, Map<String, Node> idMap) {
    	if (!idMap.containsKey(id)) {
    		registerIDs(doc, doc.getDocumentElement(), idMap);
    	}
    	return idMap.get(id);
    }

    private static void registerIDs(Document doc, Node node, Map<String, Node> idMap) {
    	if (node.getNodeType() == Node.ELEMENT_NODE) {
            if (node.getAttributes().getNamedItem("id") != null) {
            	final String id = node.getAttributes().getNamedItem("id").getNodeValue();
            	idMap.put(id, (Element) node);
            }
    	}
    	NodeList children = node.getChildNodes();
    	for (int i = 0; i < children.getLength(); i++) {
    		registerIDs(doc, children.item(i), idMap);
    	}
 		
	}

    /** export DOM document to a file -- handy for debugging **/
    public static void saveDocAsXML(Document doc, String filename) {
    	try {
	    	Transformer transformer = TransformerFactory.newInstance().newTransformer();
	    	Result output = new StreamResult(new File(filename));
	    	Source input = new DOMSource(doc);
	
	    	transformer.transform(input, output);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}    	
    }

    /**
     * Pattern used to match variables in XML fragments.
     */
	private static Pattern variablePattern = Pattern.compile("\\$\\(([^\\)]+)\\)");

    /**
     * Identify variables in attribute values and CDATA contents and replace
     * with their values as defined in the variableDefs map. Variables without
     * definitions are left alone.
     *
     * @param node the node to do variable replacement in
     * @param variableDefs map from variable names to values.
     */
    public static void replaceVariables(final Node node, Map<String, String> variableDefs) {
        switch (node.getNodeType()) {
	        case Node.ELEMENT_NODE:
				final Element element = (Element) node;
				final NamedNodeMap atts = element.getAttributes();
	            for (int i = 0; i < atts.getLength(); i++) {
	                final Attr attr = (Attr) atts.item(i);
	                attr.setNodeValue(replaceVariablesInString(attr.getValue(), variableDefs));
	            }
	            break;

	        case Node.CDATA_SECTION_NODE:
	        	String content = node.getTextContent();
	        	node.setNodeValue(replaceVariablesInString(content, variableDefs));
				break;

			default:
				break;
        }

        // process children
        final NodeList children = node.getChildNodes();
        for (int childIndex = 0; childIndex < children.getLength(); childIndex++)
            replaceVariables(children.item(childIndex), variableDefs);
    }

    /**
     * Identify variables in the given string and replace with their values as
     * defined in the variableDefs map.  Variables without definitions are left
     * alone.
     *
     * @param string String to do variable replacement in.
     * @param variableDefs Map from variable names to values.
     * @return modified string
     */
	public static String replaceVariablesInString(String string, Map<String, String> variableDefs) {

    	StringBuffer replacementStringBuffer = new StringBuffer();

 		Matcher variableMatcher = variablePattern.matcher(string);
		while (variableMatcher.find()) {
		    String varString = variableMatcher.group(1).trim();
		    int eqIdx = varString.indexOf("=");

		    String varName;
		    if (eqIdx > -1)
		        varName = varString.substring(0, eqIdx).trim();
		    else
				varName = varString;

		    String replacementString = variableDefs.containsKey(varName)
                    ? variableDefs.get(varName)
                    : variableMatcher.group(0);

            variableMatcher.appendReplacement(replacementStringBuffer,
                    Matcher.quoteReplacement(replacementString));
		}
		variableMatcher.appendTail(replacementStringBuffer);

		return replacementStringBuffer.toString();
	}

    /**
     * Cached map used by replaceVariable().
     */
	private static Map<String, String> singleVarVal = new HashMap<>();

    /**
     * Identify a variable in the given DOM subtree matching the given name
     * and replace it with the chosen value.  Other variables are left alone.
     *
     * @param node root node of DOM subtree to perform replacement in
     * @param name name of variable
     * @param value value to replace name with
     */
	public static void replaceVariable(final Node node, String name, String value) {
	    singleVarVal.clear();
	    singleVarVal.put(name, value);
	    replaceVariables(node, singleVarVal);
	}

    /**
     * Extract default values of variables defined in the DOM subtree below node.
     * Default values are used to populate the variableDefs map.  Variables
     * already defined in this map will NOT be modified.
     *
     * @param node root node of DOM subtree to extract default values from.
     * @param variableDefs map which default values will be added to.
     */
	public static void extractVariableDefaults(final Node node, Map<String, String> variableDefs) {
        switch (node.getNodeType()) {
			case Node.ELEMENT_NODE:
			    final Element element = (Element) node;
			    final NamedNodeMap attrs = element.getAttributes();
			    for (int i=0; i<attrs.getLength(); i++) {
			    	Attr attr = (Attr) attrs.item(i);
			    	extractVariableDefaultsFromString(attr.getValue(), variableDefs);
				}
				break;

			case Node.CDATA_SECTION_NODE:
				String content = node.getTextContent();
				extractVariableDefaultsFromString(content, variableDefs);
				break;

			default:
				break;
		}

        final NodeList children = node.getChildNodes();
        for (int childIndex = 0; childIndex < children.getLength(); childIndex++)
            extractVariableDefaults(children.item(childIndex), variableDefs);
	}

    /**
     * Extract default values of variables defined in the given string.
     * Default values are used to populate the variableDefs map.  Variables
     * already defined in this map will NOT be modified.
     *
     * @param string string to extract default values from.
     * @param variableDefs map which default values will be added to.
     */
	public static void extractVariableDefaultsFromString(String string, Map<String,String> variableDefs) {
		Matcher variableMatcher = variablePattern.matcher(string);
		while (variableMatcher.find()) {
		    String varString = variableMatcher.group(1);

		    int eqIdx = varString.indexOf("=");

		    if (eqIdx > -1) {
		        String varName = varString.substring(0, eqIdx).trim();

		        if (!variableDefs.containsKey(varName))
					variableDefs.put(varName, varString.substring(eqIdx+1, varString.length()));
			}
		}
	}

    /** return list of input types specified by Inputs or Param annotations
     * @param clazz Class to generate the list for
     * @param beastObject instantiation of the class, or null if not available
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws SecurityException 
     * @throws NoSuchMethodException 
     * @throws IllegalArgumentException 
     */
	public static List<InputType> listInputs(Class<?> clazz, BEASTInterface beastObject) throws InstantiationException , IllegalAccessException, IllegalArgumentException, NoSuchMethodException, SecurityException {
		List<InputType> inputTypes = new ArrayList<>();

		// First, collect Input members
		try {
			if (beastObject == null) {
				beastObject = (BEASTInterface) clazz.newInstance();
			}
			List<Input<?>> inputs = null;
			inputs = beastObject.listInputs();
			for (Input<?> input : inputs) {
				if (!(input instanceof InputForAnnotatedConstructor)) {
					try {
						// force class types to be determined
						if (input.getType() == null) {
							input.determineClass(beastObject);
						}
						inputTypes.add(new InputType(input.getName(), input.getType(), true, input.defaultValue));
					} catch (Exception e) {
						// seems safe to ignore
						e.printStackTrace();
					}
				}
			}
		} catch (InstantiationException e) {
			// this can happen if there is no constructor without arguments, 
			// e.g. when there are annotated constructors only
		}

				
		// Second, collect types of annotated constructor
	    Constructor<?>[] allConstructors = clazz.getDeclaredConstructors();
	    for (Constructor<?> ctor : allConstructors) {
	    	Annotation[][] annotations = ctor.getParameterAnnotations();
	    	List<Param> paramAnnotations = new ArrayList<>();
	    	for (Annotation [] a0 : annotations) {
		    	for (Annotation a : a0) {
		    		if (a instanceof Param) {
		    			paramAnnotations.add((Param) a);
		    		}
		    	}
	    	}
	    	Class<?>[] types  = ctor.getParameterTypes();	    	
    		Type[] gtypes = ctor.getGenericParameterTypes();
	    	if (types.length > 0 && paramAnnotations.size() > 0) {
	    		int offset = 0;
	    		if (types.length == paramAnnotations.size() + 1) {
	    			offset = 1;
	    		}
	    		for (int i = 0; i < paramAnnotations.size(); i++) {
	    			Param param = paramAnnotations.get(i);
	    			Type type = types[i + offset];
	    			Class<?> clazz2 = null;
					try {
						clazz2 = Class.forName(type.getTypeName());
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    			if (clazz2.isAssignableFrom(List.class)) {
                        Type[] genericTypes2 = ((ParameterizedType) gtypes[i + offset]).getActualTypeArguments();
                        Class<?> theClass = (Class<?>) genericTypes2[0];
	    				InputType t = new InputType(param.name(), theClass, false, param.defaultValue());
	    				inputTypes.add(t);
	    			} else {
	    				InputType t = new InputType(param.name(), types[i + offset], false, param.defaultValue());
	    				inputTypes.add(t);
	    			}
	    		}
	    	}
		}
		
		return inputTypes;
	}

//	/** get value of the input of a beast object with name specified in input **/
//    static Object getValue(BEASTInterface beastObject, InputType input) {
//    	if (input.isInput()) {
//    		// input represents simple Input
//    		return beastObject.getInput(input.getName()).get();
//    	} else {
//    		// input represents Param annotation
//    		String methodName = "get" + 
//    		    	input.getName().substring(0, 1).toUpperCase() +
//    		    	input.getName().substring(1);
//    		Method method;
//			try {
//				method = beastObject.getClass().getMethod(methodName);
//				return method.invoke(beastObject);
//			} catch (NoSuchMethodException | SecurityException |IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//				Log.err.println("Programmer error: when getting here an InputType was identified, but no Input or getter for Param annotation found");
//				e.printStackTrace();
//				return null;
//			}
//    	}
//	}


    /**
     * find closest matching class to named class *
     */
    static String guessClass(final String classname) {
        String name = classname;
        if (classname.contains(".")) {
            name = classname.substring(classname.lastIndexOf('.') + 1);
        }
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

}


