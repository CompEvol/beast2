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
@SuppressWarnings("serial")
public class XMLParserException extends Exception {
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

    public XMLParserException(String sMsg) {
    	super(sMsg);
    }
    
    public XMLParserException(Node node, String sMsg, int nErrorNr) {
        super(sMsg);
        _node = node;
        msg = "";
        errorNr = nErrorNr;
    }

    // format message and resolve parent
    public String getMessage() {
        String sMsg = "\nError " + errorNr + " parsing the xml input file\n\n" + msg + super.getMessage();
        if (_node == null) {
            return "NULL NODE\n" + sMsg;
        }
        String path = "";
        Node node = _node;
        while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
            String sID;
            sID = getAttribute(node, "id");
            if (sID != null) {
                sID = " id='" + sID + "'";
            } else {
                sID = "";
            }

            String sName;
            sName = getAttribute(node, "name");
            if (sName != null) {
                sName = " name='" + sName + "'";
            } else {
                sName = "";
            }

            String sSpec;
            sSpec = getAttribute(node, "spec");
            if (sSpec != null) {
                sSpec = " spec='" + sSpec + "'";
            } else {
                sSpec = "";
            }
            path = path.replaceAll("  <", "      <");
            path = "  <" + node.getNodeName() + sID + sName + sSpec + ">\n" + path;
            node = node.getParentNode();
        }
        sMsg += "\n\nError detected about here:\n" + path;
        return sMsg;
    } // getMessage

    String getAttribute(Node node, String sTarget) {
        NamedNodeMap atts = node.getAttributes();
        if (atts == null) {
            return null;
        }
        for (int i = 0; i < atts.getLength(); i++) {
            String sName = atts.item(i).getNodeName();
            if (sName.equals(sTarget)) {
                String sValue = atts.item(i).getNodeValue();
                return sValue;
            }
        }
        return null;
    } // getID

	public String getOriginalMessage() {
		return super.getMessage();
	}

} // XMLParserException
