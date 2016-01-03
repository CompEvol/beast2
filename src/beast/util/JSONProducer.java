package beast.util;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import beast.app.BEASTVersion;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.State;
import beast.core.parameter.Parameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.tree.TraitSet;





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
    Set<BEASTInterface> isDone;
    Map<BEASTInterface, Set<String>> isInputsDone;
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
    public String toJSON(BEASTInterface beastObject) {
    	return toJSON(beastObject, new ArrayList<>());
    }

    public String toJSON(BEASTInterface beastObject, Collection<BEASTInterface> others) {
        try {
            StringBuffer buf = new StringBuffer();
            //buf.append("{\"version\": \"2.0\",\n\"namespace\": \"" + DEFAULT_NAMESPACE + "\",\n\n" +
            //		"\"" + JSONParser.ANALYSIS_ELEMENT + "\": [\n");
            buf.append("{version: \"" + (new BEASTVersion()).getMajorVersion() + "\",\nnamespace: \"" + DEFAULT_NAMESPACE + "\",\n\n" +
            		XMLParser.BEAST_ELEMENT + ": [\n");
            //buf.append("\n\n");
            isDone = new HashSet<>();
            isInputsDone = new HashMap<>();
            IDs = new HashSet<>();
            indentCount = 1;
            
            List<BEASTInterface> priorityBeastObjects = new ArrayList<>();
            findPriorityBeastObjects(beastObject, priorityBeastObjects);
            for (BEASTInterface beastObject2 : priorityBeastObjects) {
            	if (!isDone.contains(beastObject2)) {
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

    private void findPriorityBeastObjects(BEASTInterface beastObject, List<BEASTInterface> priorityBeastObjects) throws IllegalArgumentException, IllegalAccessException {
    	if (beastObject.getClass().equals(Alignment.class)) {
    		priorityBeastObjects.add(beastObject);
    	}
    	if (beastObject instanceof TraitSet) {
    		priorityBeastObjects.add(beastObject);
    	}
		for (BEASTInterface beastObject2 : beastObject.listActivePlugins()) {
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
    void beastObjectToJSON(BEASTInterface beastObject, Class<?> defaultType, StringBuffer buf, String name, boolean isTopLevel) throws Exception {
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
        if (isDone.contains(beastObject)) {
            // JSON is already produced, we can idref it
        	buf.append((needsComma == true) ? ",\n" + indent + " " : ""); 
            buf.append("idref: \"" + beastObject.getID() + "\" ");
            needsComma = true;
            skipInputs = true;
        } else {
            // see whether a reasonable id can be generated
            if (beastObject.getID() != null && !beastObject.getID().equals("")) {
                String sID = beastObject.getID();
                // ensure ID is unique
                if (IDs.contains(sID)) {
                    int k = 1;
                    while (IDs.contains(sID + k)) {
                        k++;
                    }
                    sID = sID + k;
                }
            	buf.append((needsComma == true) ? ",\n" + indent + " " : ""); 
                buf.append("id: \"" + normalise(null, sID) + "\"");
                needsComma = true;
                IDs.add(sID);
            }
            isDone.add(beastObject);
        }
        String sClassName = beastObject.getClass().getName();
        if (skipInputs == false) {
            // only add spec element if it cannot be deduced otherwise (i.e., by idref)
        	if (defaultType != null && !defaultType.getName().equals(sClassName)) {
	        	buf.append((needsComma == true) ? ",\n" + indent + " " : ""); 
	            //buf.append("\"spec\": \"" + sClassName + "\"");
	            buf.append("spec: \"" + sClassName + "\"");
	            needsComma = true;
        	}
        }

        if (!skipInputs) {
            // process inputs of this beastObject
            // first, collect values as attributes
            List<Input<?>> inputs = beastObject.listInputs();
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
     * primitive inputs (if isShort=true) and as individual elements (if bShort=false)
     *
     * @param input0: name of the input
     * @param beastObject: beastObject to produce this input JSON for
     * @param buf:    gets JSON results are appended
     * @param isShort: flag to indicate attribute/value format (true) or element format (false)
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void inputToJSON(Input input, Object value, BEASTInterface beastObject, StringBuffer buf, boolean isShort, String indent) throws Exception {
        if (value != null) {
        	
            // distinguish between Map, List, BEASTObject and primitive input types
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
                    	if (o2 instanceof BEASTInterface) {
                    		beastObjectToJSON((BEASTInterface) o2, input.getType(), buf3, null, false);
                    	} else {
                    		buf2.append(o2.toString());
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
            } else if (value instanceof BEASTInterface) {
            	if (!value.equals(input.defaultValue)) {
            		
            		// Parameters can use short hand notation if they are not in the state 
            		// Note this means lower and upper bounds are lost -- no problem for BEAST, but maybe for BEAUti
            		if (value instanceof Parameter.Base) {
            			Parameter.Base parameter = (Parameter.Base) value;
            			boolean isInState = false;
            			for (Object o : parameter.getOutputs()) {
            				if (o instanceof State) {
            					isInState = true;
            					break;
            				}
            			}
            			if (!isInState) {
            				if (isShort) {
                                buf.append(" " + input.getName() + ": \"" + parameter.getValue() + "\"");
            				} else {
            					return;
            				}
            			}
            		}
            		
                    if (isShort && isDone.contains(value)) {
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
                // primitive type

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
    

	
	public static void main(String[] args) throws Exception {
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


