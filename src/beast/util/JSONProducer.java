package beast.util;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import beast.core.Input;
import beast.core.BEASTObject;
import beast.core.BEASTInterface;
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
 * + JSON has editor support -- gedit, any javascript editor
 * + JSON validation -- http://jsonlint.com/ for validation
 * + JSON = javascript, useful format for GUIs
 * - JSON bracket mathcing is not well supported in editors
 * 
 * + YAML no issues with brackets
 * - YAML litte editor support
*/

public class JSONProducer {

    /**
     * list of objects already converted to XML, so an idref suffices
     */
    HashSet<BEASTInterface> isDone;
    @SuppressWarnings("rawtypes")
    HashSet<Input> isInputsDone;
    /**
     * list of IDs of elements produces, used to prevent duplicate ID generation
     */
    HashSet<String> IDs;
    /**
     * #spaces before elements in XML *
     */
    int indentCount;
    
    final public static String DEFAULT_NAMESPACE = "beast.core:beast.evolution.alignment:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood";

    public JSONProducer() {
        super();
    }

    /**
     * Main entry point for this class
     * Given a plug-in, produces the XML in BEAST 2 format
     * representing the plug-in. This assumes plugin is Runnable
     */
    public String toJSON(BEASTInterface plugin) {
    	return toJSON(plugin, new ArrayList<BEASTInterface>());
    }

	@SuppressWarnings("rawtypes")
    public String toJSON(BEASTInterface plugin, Collection<BEASTInterface> others) {
        try {
            StringBuffer buf = new StringBuffer();
            //buf.append("{\"version\": \"2.0\",\n\"namespace\": \"" + DEFAULT_NAMESPACE + "\",\n\n" +
            //		"\"" + JSONParser.ANALYSIS_ELEMENT + "\": [\n");
            buf.append("{version: \"2.0\",\nnamespace: \"" + DEFAULT_NAMESPACE + "\",\n\n" +
            		JSONParser.ANALYSIS_ELEMENT + ": [\n");
            //buf.append("\n\n");
            isDone = new HashSet<BEASTInterface>();
            isInputsDone = new HashSet<Input>();
            IDs = new HashSet<String>();
            indentCount = 1;
            
            List<BEASTInterface> priorityPlugins = new ArrayList<BEASTInterface>();
            findPriorityPlugins(plugin, priorityPlugins);
            for (BEASTInterface plugin2 : priorityPlugins) {
            	if (!isDone.contains(plugin2)) {
            		//String name = plugin2.getClass().getName();
            		//name = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
            		pluginToJSON(plugin2, BEASTInterface.class, buf, null, true);
            		buf.append(",");
            	}
            }
            buf.append("\n\n");
            
            pluginToJSON(plugin, BEASTInterface.class, buf, null, true);
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
    } // toXML

    private void findPriorityPlugins(BEASTInterface plugin, List<BEASTInterface> priorityPlugins) throws IllegalArgumentException, IllegalAccessException {
    	if (plugin.getClass().equals(Alignment.class)) {
    		priorityPlugins.add(plugin);
    	}
    	if (plugin instanceof TraitSet) {
    		priorityPlugins.add(plugin);
    	}
		for (BEASTInterface plugin2 : plugin.listActivePlugins()) {
			findPriorityPlugins(plugin2, priorityPlugins);
		}
	}

	/**
     * like modelToXML, but without the cleanup *
     * For plugin without name
     */
//    @SuppressWarnings("rawtypes")
//    public String toJSON(Plugin plugin, String sName) {
//        try {
//            StringBuffer buf = new StringBuffer();
//            isDone = new HashSet<Plugin>();
//            isInputsDone = new HashSet<Input>();
//            ids = new HashSet<String>();
//            indentCount = 0;
//            pluginToJSON(plugin, Plugin.class, buf, sName, false);
//            return buf.toString();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    } // toRawJSON



    public String stateNodeToJSON(BEASTInterface plugin) {
        try {
            StringBuffer buf = new StringBuffer();
            //buf.append("<" + XMLParser.beast_ELEMENT + " version='2.0'>\n");
            isDone = new HashSet<BEASTInterface>();
            IDs = new HashSet<String>();
            indentCount = 1;
            pluginToJSON(plugin, null, buf, null, false);
            return buf.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    /**
     * produce elements for a plugin with name sName, putting results in buf.
     * It tries to create XML conforming to the XML transformation rules (see XMLParser)
     * that is moderately readable.
     */
//    @SuppressWarnings("rawtypes")
    void pluginToJSON(BEASTInterface plugin, Class<?> defaultType, StringBuffer buf, String name, boolean bIsTopLevel) throws Exception {
        // determine element name, default is input, otherswise find one of the defaults

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
        if (isDone.contains(plugin)) {
            // XML is already produced, we can idref it
        	buf.append((needsComma == true) ? ",\n" + indent + " " : ""); 
            //buf.append("\"idref\": \"" + plugin.getID() + "\" ");
            buf.append("idref: \"" + plugin.getID() + "\" ");
            needsComma = true;
            skipInputs = true;
        } else {
            // see whether a reasonable id can be generated
            if (plugin.getID() != null && !plugin.getID().equals("")) {
                String sID = plugin.getID();
                // ensure ID is unique
                if (IDs.contains(sID)) {
                    int k = 1;
                    while (IDs.contains(sID + k)) {
                        k++;
                    }
                    sID = sID + k;
                }
            	buf.append((needsComma == true) ? ",\n" + indent + " " : ""); 
                //buf.append("\"id\": \"" + normalise(null, sID) + "\"");
                buf.append("id: \"" + normalise(null, sID) + "\"");
                needsComma = true;
                IDs.add(sID);
            }
            isDone.add(plugin);
        }
        String sClassName = plugin.getClass().getName();
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
            // process inputs of this plugin
            // first, collect values as attributes
            List<Input<?>> inputs = plugin.listInputs();
            for (Input<?> input : inputs) {
            	StringBuffer buf2 = new StringBuffer();
                inputToJSON(input.getName(), plugin, buf2, true, indent);
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
                inputToJSON(input.getName(), plugin, buf3, false, indent);
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
    } // pluginToJSON


    /**
     * produce JSON for an input of a plugin, both as attribute/value pairs for
     * primitive inputs (if isShort=true) and as individual elements (if bShort=false)
     *
     * @param sInput: name of the input
     * @param plugin: plugin to produce this input XML for
     * @param buf:    gets XML results are appended
     * @param isShort: flag to indicate attribute/value format (true) or element format (false)
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void inputToJSON(String input0, BEASTInterface plugin, StringBuffer buf, boolean isShort, String indent) throws Exception {
        Field[] fields = plugin.getClass().getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType().isAssignableFrom(Input.class)) {
                Input input = (Input) fields[i].get(plugin);
                if (input.getName().equals(input0)) {
                    // found the input with name sInput
                    if (input.get() != null) {
                        // distinguish between Map, List, Plugin and primitive input types
                        if (input.get() instanceof Map) {
                            if (!isShort) {
	                        	Map<String,?> map = (Map<String,?>) input.get();
	                        	StringBuffer buf2 = new StringBuffer();
	                        	// determine label widith
	                        	int whiteSpaceWidth = 0;
	                        	for (String key : map.keySet()) {
	                        		whiteSpaceWidth = Math.max(whiteSpaceWidth, key.length());
	                        	}
	                        	boolean needsComma = false;
	                        	List<String> keys = new ArrayList<String>();
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
                        } else if (input.get() instanceof List) {
                            if (!isShort) {
                            	StringBuffer buf2 = new StringBuffer();
                            	//buf2.append(indent + " \"" + input0 + "\": [\n");
                            	buf2.append(indent + " " + input0 + ": [\n");
                            	boolean needsComma = false;
                            	int oldLen = buf2.length();
                                for (Object o2 : (List) input.get()) {
                                	if (needsComma) {
                                		buf2.append(",\n");
                                	}
                                	StringBuffer buf3 = new StringBuffer();
                                	if (o2 instanceof BEASTInterface) {
                                		pluginToJSON((BEASTInterface) o2, input.getType(), buf3, null, false);
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
                        } else if (input.get() instanceof BEASTInterface) {
                        	if (!input.get().equals(input.defaultValue)) {
	                            if (isShort && isDone.contains((BEASTInterface) input.get())) {
	                                buf.append(" " + input0 + ": \"@" + ((BEASTInterface) input.get()).getID() + "\"");
	                                isInputsDone.add(input);
	                            }
	                            if (!isShort && !isInputsDone.contains(input)) {
	                                pluginToJSON((BEASTInterface) input.get(), input.getType(), buf, input0, false);
	                            }
                        	}
                            return;
                        } else {
                        	if (!input.get().equals(input.defaultValue)) {
	                            // primitive type, see if
	                            String sValue = input.get().toString();
	                            if (isShort) {
	                                if (sValue.indexOf('\n') < 0) {
	                                    //buf.append(" \"" + input0 + "\": " + normalise(input, input.get().toString()) + "");
	                                    buf.append(" " + input0 + ": " + normalise(input, input.get().toString()) + "");
	                                }
	                            } else {
	                                if (sValue.indexOf('\n') >= 0) {
//	                                    for (int j = 0; j < m_nIndent; j++) {
//	                                        buf.append("    ");
//	                                    }
	                                    //if (sInput.equals("value")) {
	                                    //    buf.append(input.get().toString());
	                                    //} else {
	                                        //buf.append(indent + "\"" + input0 + "\": " + normalise(input, input.get().toString()) + "");
	                                        buf.append(indent + "" + input0 + ": " + normalise(input, input.get().toString()) + "");
	                                    //}
	                                }
	                            }
                        	}
                            return;
                        }
                    } else {
                        // value=null, no XML to produce
                        return;
                    }
                }
            }
        }
        // should never get here
        throw new Exception("Could not find input " + input0 + " in plugin " + plugin.getID() + " " + plugin.getClass().getName());
    } // inputToJSON

    
   /** convert plain text string to XML string, replacing some entities **/
    String normalise(Input<?> input, String str) {
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
    

	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		// convert BEAST 2 XML to BEAST json file
		XMLParser parser = new XMLParser();
		BEASTInterface plugin = parser.parseFile(new File(args[0]));

		String JSONFile = args[0].replace(".xml", ".json");
		PrintStream out;
		if (JSONFile.endsWith(".json")) {
			out = new PrintStream(new File(JSONFile));
		} else {
			out = System.out;
		}
		
		JSONProducer writer = new JSONProducer();
		String JSON = writer.toJSON(plugin);
		out.println(JSON);
		out.close();
		
		
	}

}


