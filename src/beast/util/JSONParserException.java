/*
* File JSONParserException.java
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

/**
 * Exception thrown by JSONParser
 * that keeps track of the JSONOBject that caused the anomaly.
 */
public class JSONParserException extends XMLParserException {

	private static final long serialVersionUID = 1L;

	/**
     * JSONObject where the anomaly was in the vicinity *
     */
	JSONObject node;

	/**
     * short description of the anomaly *
     */
    String m_sMsg;
    
    /**
     * number of the anomaly, for ease of finding in the code *
     */
    int m_nErrorNr;

    public JSONParserException(JSONObject node, String msg, int nErrorNr) {
        super(msg);
        this.node = node;
        m_sMsg = "";
        m_nErrorNr = nErrorNr;
    }

    // format message and resolve parent
    @Override
	public String getMessage() {
        String msg = "\nError " + m_nErrorNr + " parsing the json input file\n\n" + m_sMsg + super.getOriginalMessage();
        if (node == null) {
            return "NULL NODE\n" + msg;
        }
        String path = "";
        Object o = this.node;
        while (o != null) {
        	if (o instanceof JSONObject) {
        		JSONObject node = (JSONObject) o; 
	            String ID;
	            ID = getAttribute(node, "id");
	            if (ID != null) {
	                ID = " id: \"" + ID + "\"";
	            } else {
	                ID = "";
	            }
	
	            String name = "";
	            Object p = node.getParent();
	            if (p instanceof JSONObject) {
	            	JSONObject parent = (JSONObject) p;
	            	for (String key : parent.keySet()) {
	            		try {
							if (parent.get(key).equals(o)) {
								name = "\"" + key + "\": ";
							}
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	            	}
	            }
	
	            String spec;
	            spec = getAttribute(node, "spec");
	            if (spec != null) {
	                spec = " spec: \"" + spec + "\"";
	            } else {
	                spec = "";
	            }
	            path = path.replaceAll("  \"", "      \"");
	            path = path.replaceAll("  \\{", "      \\{");
	            path = path.replaceAll("  \\[", "      [");
	            path = path.replaceAll("  \\]", "      ]");
	            path = path.replaceAll("  \\}", "      \\}");
	            path = "  " + name + "{" + ID  + spec + "\n" + path + "  }\n";
	            
	            o = node.getParent();
        	} else if (o instanceof JSONArray) {
        		JSONArray list = (JSONArray) o;
	            Object p = list.getParent();
	            String name = "";
	            if (p instanceof JSONObject) {
	            	JSONObject parent = (JSONObject) p;
	            	for (String key : parent.keySet()) {
	            		try {
							if (parent.get(key).equals(o)) {
								name = "\"" + key + "\": ";
							}
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	            	}
	            }

	            path = path.replaceAll("  \"", "      \"");
	            path = path.replaceAll("  \\{", "      \\{");
	            path = path.replaceAll("  \\[", "      [");
	            path = path.replaceAll("  \\]", "      ]");
	            path = path.replaceAll("  \\}", "      \\}");
	            path = "  " + name + "[\n"  + path + "  ]\n";        	
	            o = list.getParent();
        	}

        }
        msg += "\n\nError detected about here:\n" + path;
        return msg;
    } // getMessage

    String getAttribute(JSONObject node, String target) {
    	if (node.has(target)) {
    		try {
				return  node.get(target).toString();
			} catch (JSONException e) {
				return null;
			}
    	}
   		return null;
    } // getAttribute

} // JSONParserException
