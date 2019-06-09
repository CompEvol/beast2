package beast.util;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beast.app.BEASTVersion2;
import beast.core.BEASTInterface;
import beast.core.BEASTObjectStore;
import beast.core.Input;
import beast.core.parameter.Parameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.operators.ScaleOperator;





/*
 * Why JSON:
 *
 * JSON vs XML: http://www.json.org/fatfree.html
 * + JSON more readable
 * + JSON less to type
 * 
 * JSON vs YAML: http://en.wikipedia.org/wiki/YAML
 * 
 * + JSON has editor support -- gedit, any Javascript editor
 * + JSON validation -- http://jsonlint.com/ for validation
 * + JSON = Javascript, useful format for GUIs
 * - JSON bracket matching is not well supported in editors
 * 
 * + YAML no issues with brackets
 * - YAML little editor support
*/

public class JSONProducer {

    /**
     * list of objects already converted to JSON, so an idref suffices
     */
    Set<Object> isDone;
    Map<Object, Set<String>> isInputsDone;
    /**
     * list of IDs of elements produces, used to prevent duplicate ID generation
     */
    HashSet<String> IDs;
    /**
     * #spaces before elements in JSON *
     */
    int indentCount;
    
    final public static String DEFAULT_NAMESPACE = "beast.core:beast.evolution.alignment:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood";

    public JSONProducer() {
        super();
    }

    /**
     * Main entry point for this class
     * Given a plug-in, produces the JSON in BEAST 2 format
     * representing the plug-in. This assumes beastObject is Runnable
     */
    public String toJSON(Object beastObject) {
    	return toJSON(beastObject, new ArrayList<>());
    }

    public String toJSON(Object beastObject, Collection<BEASTInterface> others) {
        try {
            StringBuffer buf = new StringBuffer();
            //buf.append("{\"version\": \"2.0\",\n\"namespace\": \"" + DEFAULT_NAMESPACE + "\",\n\n" +
            //		"\"" + JSONParser.ANALYSIS_ELEMENT + "\": [\n");
            buf.append("{version: \"" + (new BEASTVersion2()).getMajorVersion() + "\",\nnamespace: \"" + DEFAULT_NAMESPACE + "\",\n\n" +
            		XMLParser.BEAST_ELEMENT + ": [\n");
            //buf.append("\n\n");
            isDone = new HashSet<>();
            isInputsDone = new HashMap<>();
            IDs = new HashSet<>();
            indentCount = 1;
            
            List<BEASTInterface> priorityBeastObjects = new ArrayList<>();
            findPriorityBeastObjects(beastObject, priorityBeastObjects);
            for (BEASTInterface beastObject2 : priorityBeastObjects) {
            	if (!isDone.contains(BEASTObjectStore.INSTANCE.getBEASTObject(beastObject2))) {
            		//name = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
            		beastObjectToJSON(beastObject2, BEASTInterface.class, buf, null, true);
            		buf.append(",");
            	}
            }
            buf.append("\n\n");
            
            beastObjectToJSON(beastObject, BEASTInterface.class, buf, null, true);
            String end = "\n]\n}";
            buf.append(end);

            String JSON = buf.toString();
            String[] nameSpaces = DEFAULT_NAMESPACE.split(":");
            for (String nameSpace : nameSpaces) {
                //JSON = JSON.replaceAll("\"spec\": \"" + nameSpace + ".", "\"spec\": \"");
                JSON = JSON.replaceAll("spec: \"" + nameSpace + ".", "spec: \"");
            }
            return JSON;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    } // toJSON

    private void findPriorityBeastObjects(Object beastObject, List<BEASTInterface> priorityBeastObjects) throws IllegalArgumentException, IllegalAccessException {
    	if (beastObject.getClass().equals(Alignment.class)) {
    		priorityBeastObjects.add((Alignment) beastObject);
    	}
//    	if (beastObject instanceof TraitSet) {
//    		priorityBeastObjects.add(beastObject);
//    	}
		for (Object beastObject2 : BEASTObjectStore.listActiveBEASTObjects(beastObject)) {
			findPriorityBeastObjects(beastObject2, priorityBeastObjects);
		}
	}



    public String stateNodeToJSON(BEASTInterface beastObject) {
        try {
            StringBuffer buf = new StringBuffer();
            isDone = new HashSet<>();
            IDs = new HashSet<>();
            indentCount = 1;
            beastObjectToJSON(beastObject, null, buf, null, false);
            return buf.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    /**
     * Produce JSON fragment for a beast object with given name, putting results in buf.
     * It tries to create JSON conforming to the JSON transformation rules (see JSONParser)
     * that is moderately readable.
     */
    void beastObjectToJSON(Object beastObject, Class<?> defaultType, StringBuffer buf, String name, boolean isTopLevel) {
        // determine element name, default is input, otherwise find one of the defaults

    	String indent = "";
        for (int i = 0; i < indentCount; i++) {
        	indent += "\t";
            //buf.append("    ");
        }
        indentCount++;

        // open element
        boolean needsComma = false;
        if (name != null) {
            //buf.append((indentCount == 1 ? "" : indent.substring(1)) + " \"" + name + "\": {");
            buf.append((indentCount == 1 ? "" : indent.substring(1)) + " " + name + ": {");
        } else {
        	buf.append(indent + "{");
        }

        boolean skipInputs = false;
        BEASTInterface beastObject2 = BEASTObjectStore.INSTANCE.getBEASTObject(beastObject);
        // isDone.contains(beastObject2) fails when BEASTObjects override equals(), so use a stream with == instead
        if (isDone.stream().anyMatch(x -> x == beastObject2)) {
            // JSON is already produced, we can idref it
        	buf.append((needsComma == true) ? ",\n" + indent + " " : ""); 
            buf.append("idref: \"" + BEASTObjectStore.getId(beastObject) + "\" ");
            needsComma = true;
            skipInputs = true;
        } else {
            // see whether a reasonable id can be generated
            if (BEASTObjectStore.getId(beastObject) != null && !BEASTObjectStore.getId(beastObject).equals("")) {
                String id = BEASTObjectStore.getId(beastObject);
                // ensure ID is unique
                if (IDs.contains(id)) {
                    int k = 1;
                    while (IDs.contains(id + k)) {
                        k++;
                    }
                    id = id + k;
                }
            	buf.append((needsComma == true) ? ",\n" + indent + " " : ""); 
                buf.append("id: \"" + normalise(null, id) + "\"");
                needsComma = true;
                IDs.add(id);
                BEASTObjectStore.setId(beastObject, id);
            }
            //isDone.add(beastObject);
            isDone.add(BEASTObjectStore.INSTANCE.getBEASTObject(beastObject));
        }
        String className = BEASTObjectStore.getClassName(beastObject);
        
        if (skipInputs == false) {
            // only add spec element if it cannot be deduced otherwise (i.e., by idref)
        	//if (defaultType != null && !defaultType.getName().equals(className)) {
	        	buf.append((needsComma == true) ? ",\n" + indent + " " : ""); 
	            //buf.append("\"spec\": \"" + className + "\"");
	            buf.append("spec: \"" + className + "\"");
	            needsComma = true;
        	//}
        }

        
        if (beastObject instanceof ScaleOperator) {
        	int h = 3;
        	h++;
        }
        if (!skipInputs) {
            // process inputs of this beastObject
            // first, collect values as attributes
            List<Input<?>> inputs = BEASTObjectStore.listInputs(beastObject);
            //Collections.sort(inputs, (a,b) -> {return a.getName().compareTo(b.getName());});
            //List<InputType> inputs = XMLParserUtils.listInputs(beastObject.getClass(), beastObject);
            for (Input<?> input : inputs) {
            	StringBuffer buf2 = new StringBuffer();
            	Object value = input.get();
                inputToJSON(input, value, beastObject, buf2, true, indent);
                if (buf2.length() > 0) {
                	buf.append((needsComma == true) ? "," : "");
                	buf.append(buf2);
                    needsComma = true;
                }
            }
            // next, collect values as input elements
            StringBuffer buf2 = new StringBuffer();
            for (Input<?> input : inputs) {
            	StringBuffer buf3 = new StringBuffer();
            	Object value = input.get();
                inputToJSON(input, value, beastObject, buf3, false, indent);
                if (buf3.length() > 0) {
                	buf2.append((needsComma == true) ? ",\n" : "\n");
                	buf2.append(buf3);
                    needsComma = true;
                }
            }
            if (buf2.length() != 0) {
                buf.append(buf2);
            }
            indentCount--;
            if (needsComma) {
                buf.append("\n"+indent);
            }
            needsComma = true;
        } else {
            // close element
            indentCount--;
            buf.append("");
            needsComma = true;
        }
        //if (m_nIndent < 2) {
        // collapse newlines if there are no sub-objects
        String str = buf.toString();
        if (str.indexOf('}') < 0 && str.length() < 1024) {
        	str = str.replaceAll("\\s+", " ");
        	buf.delete(0, buf.length());
        	buf.append(indent);
        	buf.append(str);
        }
        
        buf.append("}");
        //}
    } // beastObjectToJSON


	/**
     * produce JSON for an input of a beastObject, both as attribute/value pairs for
     * primitive inputs (if isShort=true) and as individual elements (if isShort=false)
     *
     * @param input0: name of the input
     * @param beastObject: beastObject to produce this input JSON for
     * @param buf:    gets JSON results are appended
     * @param isShort: flag to indicate attribute/value format (true) or element format (false)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void inputToJSON(Input input, Object value, Object beastObject, StringBuffer buf, boolean isShort, String indent) {
        if (value != null) {
        	
            // distinguish between Map, List, BEASTObject and primitive input types
        	String name = input.getName();
            if (value instanceof Map) {
                if (!isShort) {
                	Map<String,?> map = (Map<String,?>) value;
                	StringBuffer buf2 = new StringBuffer();
                	
                	// determine label width
                	int whiteSpaceWidth = 0;
                	for (String key : map.keySet()) {
                		whiteSpaceWidth = Math.max(whiteSpaceWidth, key.length());
                	}
                	boolean needsComma = false;
                	List<String> keys = new ArrayList<>();
                	keys.addAll(map.keySet());
                	Collections.sort(keys);
                	for (String key : keys) {
                    	if (needsComma) {
                    		buf2.append(",\n");
                    	}
                		buf2.append(indent + " " + key);
                		for (int k = key.length(); k < whiteSpaceWidth; k++) {
                			buf2.append(' ');
                		}
                		buf2.append(" :\"" + map.get(key) +"\"");
                		needsComma = true;
                	}
                	buf.append(buf2);
                }
            	return;
            } else if (name.startsWith("*") || name.equals("id") || name.equals("name")) {
        		// this can happen with private inputs, like in ThreadedTreeLikelihood
        		// and * is not a valid XML attribute name
            	return;
            } else if (value instanceof List) {
                if (!isShort) {
                	StringBuffer buf2 = new StringBuffer();
                	//buf2.append(indent + " \"" + input0 + "\": [\n");
                	buf2.append(indent + " " + input.getName() + ": [\n");
                	boolean needsComma = false;
                	int oldLen = buf2.length();
                    for (Object o2 : (List) value) {
                    	if (needsComma) {
                    		buf2.append(",\n");
                    	}
                    	StringBuffer buf3 = new StringBuffer();
                    	if (BEASTObjectStore.isPrimitive(o2)) {
                    		buf2.append(o2.toString());
                    	} else {
                    		beastObjectToJSON(o2, input.getType(), buf3, null, false);
                    	}
                        buf2.append(buf3);
                        needsComma = oldLen < buf2.length();
                    }
                    if (buf2.length() != oldLen) {
                    	buf.append(buf2);
                    	buf.append("\n" + indent + "  ]");
                    }
                }
                return;
            } else if (value.getClass().isArray()) {
                if (!isShort) {
	            	StringBuilder buf2 = new StringBuilder();
	            	buf2.append(indent + " " + input.getName() + ": [");
	            	Class type = value.getClass().getComponentType();
	            	if (type.isPrimitive()) {
	            		if (type.equals(Integer.TYPE)) {
			            	for (int o : (int []) value) {
			            		buf2.append(o + ", ");
			            	}
	                    } else if (type.equals(Long.TYPE)) {
			            	for (long o : (long []) value) {
			            		buf2.append(o + ", ");
			            	}
	                    } else if (type.equals(Short.TYPE)) {
			            	for (short o : (short []) value) {
			            		buf2.append(o + ", ");
			            	}
	                    } else if (type.equals(Float.TYPE)) {
			            	for (float o : (float []) value) {
			            		buf2.append(o + ", ");
			            	}
	                    } else if (type.equals(Double.TYPE)) {
			            	for (double o : (double []) value) {
			            		buf2.append(o + ", ");
			            	}
	                    } else if (type.equals(Boolean.TYPE)) {
			            	for (boolean o : (boolean []) value) {
			            		buf2.append(o + ", ");
			            	}
	                    } else if (type.equals(Byte.TYPE)) {
			            	for (byte o : (byte []) value) {
			            		buf2.append(o + ", ");
			            	}
	                    } else if (type.equals(Character.TYPE)) {
			            	for (char o : (char []) value) {
			            		buf2.append(o + ", ");
			            	}
		            	}	            		
	            	} else {
	                    for (Object o2 : (Object []) value) {
	                    	StringBuffer buf3 = new StringBuffer();
	                    	if (BEASTObjectStore.isPrimitive(o2)) {
	                    	    buf2.append(o2.toString() + ", ");
	                    	} else {
	                    		beastObjectToJSON(o2, input.getType(), buf3, null, false);
		                        buf2.append("\n" + buf3 + ", ");
	                    	}
	                    }
	                }
	            	buf2.deleteCharAt(buf2.length() - 1);
	            	buf2.deleteCharAt(buf2.length() - 1);
	            	if (BEASTInterface.class.isAssignableFrom(type)) {
                    	buf2.append("\n" + indent + "    ]");
	            	} else {
	            		buf2.append("]");
	            	}
	            	buf.append(buf2);
                }
            	return;
            } else if (value instanceof BEASTInterface) {
            	if (input.defaultValue == null || !value.equals(input.defaultValue)) {
            		
            		// Parameters can use short hand notation if they are not in the state 
            		// Note this means lower and upper bounds are lost -- no problem for BEAST, but maybe for BEAUti
//            		if (value instanceof BooleanParameter || value instanceof IntegerParameter || value instanceof RealParameter) {
                	if (value instanceof Parameter<?>) {
            			Parameter.Base parameter = (Parameter.Base) value;
            			boolean isInState = false;
            			for (Object o : parameter.getOutputs()) {
            				if (o.getClass().getSimpleName().equals("State")) {
            					isInState = true;
            					break;
            				}
            			}
            			if (!isInState && parameter.getDimension() == 1 && parameter.getMinorDimension1() == 1) {
            				// if not in state, bounds do not matter
            				//if ((parameter instanceof RealParameter && parameter.getLower().equals(Double.NEGATIVE_INFINITY) && parameter.getUpper().equals(Double.POSITIVE_INFINITY)) ||
            				//	(parameter instanceof IntegerParameter && parameter.getLower().equals(Integer.MIN_VALUE + 1) && parameter.getUpper().equals(Integer.MAX_VALUE - 1))) {
	            				if (isShort) {
	                                buf.append(" " + input.getName() + ": \"" + parameter.getValue() + "\"");
	            				} else {
	            					return;
	            				}
            				//}
            			}
            		}
            		
                    if (isShort && isDone.contains(BEASTObjectStore.INSTANCE.getBEASTObject(value))) {
                        buf.append(" " + input.getName() + ": \"@" + ((BEASTInterface) value).getID() + "\"");
                        if (!isInputsDone.containsKey(beastObject)) {
                        	isInputsDone.put(beastObject, new HashSet<>());
                        }
                        isInputsDone.get(beastObject).add(input.getName());
                    }
                    if (!isShort && (!isInputsDone.containsKey(beastObject) ||
                    		!isInputsDone.get(beastObject).contains(input.getName()))) {
                        beastObjectToJSON((BEASTInterface) value, input.getType(), buf, input.getName(), false);
                    }
            	}
                return;
            } else {
            	if (BEASTObjectStore.isPrimitive(value)) {             	
	            	if (!value.equals(input.defaultValue)) {
	            		
	                    String valueString = value.toString();
	                    if (isShort) {
	                        if (valueString.indexOf('\n') < 0) {
	                            buf.append(" " + input.getName() + ": " + normalise(input, value.toString()) + "");
	                        }
	                    } else {
	                        if (valueString.indexOf('\n') >= 0) {
	                                buf.append(indent + "" + input.getName() + ": " + normalise(input, value.toString()) + "");
	                        }
	                    }
	            	}
            	} else {
            		inputToJSON(input, BEASTObjectStore.INSTANCE.getBEASTObject(value), beastObject, buf, isShort, indent);
            	}
                return;
            }
        } else {
            // value=null, no JSON to produce
            return;
        }
    } // inputToJSON

    
   /** convert plain text string to JSON string, replacing some entities **/
    private String normalise(Input<?> input, String str) {
    	str = str.replaceAll("\\\\", "\\\\\\\\");
    	str = str.replaceAll("/", "\\\\/");
    	str = str.replaceAll("\b", "\\\\b");
    	str = str.replaceAll("\f", "\\\\f");
    	str = str.replaceAll("\t", "\\\\t");
    	str = str.replaceAll("\\r", "\\\\r");
    	str = str.replaceAll("\"", "\\\\\"");
    	str = str.replaceAll("\n", "\\\\n");
    	if (input != null && !input.getType().equals(Double.class) &&
    			!input.getType().equals(Integer.class)&&
    			!input.getType().equals(Boolean.class)) {
    		str = "\"" + str + "\"";
    	}
    	return str;
    }
    

	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, XMLParserException {
		// convert BEAST 2 XML to BEAST JSON file
		XMLParser parser = new XMLParser();
		BEASTInterface beastObject = parser.parseFile(new File(args[0]));

		String JSONFile = args[0].replace(".xml", ".json");
		PrintStream out;
		if (JSONFile.endsWith(".json")) {
			out = new PrintStream(new File(JSONFile));
		} else {
			out = System.out;
		}
		
		JSONProducer writer = new JSONProducer();
		String JSON = writer.toJSON(beastObject);
		out.println(JSON);
		out.close();
		
		
	}

}


