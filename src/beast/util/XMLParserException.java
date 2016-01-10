/*
* File XMLParserException.java
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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Exception thrown by XMLParser
 * that keeps track of the DOM Node that caused the
 * anomaly.
 */
public class XMLParserException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
     * DOM Node where the anomaly was in the vicinity *
     */
    Node _node;
    /**
     * short description of the anomaly *
     */
    String msg;
    /**
     * number of the anomaly, for ease of finding in the code *
     */
    int errorNr;

    public XMLParserException(String msg) {
    	super(msg);
    }
    
    public XMLParserException(Node node, String msg, int errorNr) {
        super(msg);
        _node = node;
        msg = "";
        this.errorNr = errorNr;
    }

    // format message and resolve parent
    @Override
	public String getMessage() {
        String msg = "\nError " + errorNr + " parsing the xml input file\n\n" + this.msg + super.getMessage();
        if (_node == null) {
            return "NULL NODE\n" + msg;
        }
        String path = "";
        Node node = _node;
        while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
            String id;
            id = getAttribute(node, "id");
            if (id != null) {
                id = " id='" + id + "'";
            } else {
                id = "";
            }

            String name;
            name = getAttribute(node, "name");
            if (name != null) {
                name = " name='" + name + "'";
            } else {
                name = "";
            }

            String spec;
            spec = getAttribute(node, "spec");
            if (spec != null) {
                spec = " spec='" + spec + "'";
            } else {
                spec = "";
            }
            path = path.replaceAll("  <", "      <");
            path = "  <" + node.getNodeName() + id + name + spec + ">\n" + path;
            node = node.getParentNode();
        }
        msg += "\n\nError detected about here:\n" + path;
        return msg;
    } // getMessage

    String getAttribute(Node node, String target) {
        NamedNodeMap atts = node.getAttributes();
        if (atts == null) {
            return null;
        }
        for (int i = 0; i < atts.getLength(); i++) {
            String name = atts.item(i).getNodeName();
            if (name.equals(target)) {
                String valueString = atts.item(i).getNodeValue();
                return valueString;
            }
        }
        return null;
    } // getID

	public String getOriginalMessage() {
		return super.getMessage();
	}

} // XMLParserException
