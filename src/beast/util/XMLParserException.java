
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
	/** DOM Node where the anomaly was in the vicinity **/
	Node m_node;
	/** short description of the anomaly **/
	String m_sMsg;
	/** number of the anomaly, for ease of finding in the code **/
	int m_nErrorNr;

	public XMLParserException(Node node, String sMsg, int nErrorNr) {
		super(sMsg);
		m_node = node;
		m_sMsg = "";
		m_nErrorNr = nErrorNr;
	}

	// format message and resolve parent
	public String getMessage() {
		String sMsg = "\nError " + m_nErrorNr + " parsing the xml input file\n\n" + m_sMsg + super.getMessage();
		if (m_node==null) {
			return "NULL NODE\n" + sMsg;
		}
		String sPath = "";
		Node node = m_node;
		while (node.getNodeType() == Node.ELEMENT_NODE) {
			String sID;
			sID = getAttribute(node, "id");
			if (sID != null) {
				sID = " id='"+sID+"'";
			} else {
				sID = "";
			}

			String sName;
			sName = getAttribute(node, "name");
			if (sName != null) {
				sName = " name='"+sName+"'";
			} else {
				sName = "";
			}

			String sSpec;
			sSpec = getAttribute(node, "spec");
			if (sSpec != null) {
				sSpec = " spec='"+sSpec+"'";
			} else {
				sSpec = "";
			}
			sPath = sPath.replaceAll("  <", "      <");
			sPath = "  <" + node.getNodeName() + sID + sName + sSpec + ">\n" + sPath;
			node = node.getParentNode();
		}
		sMsg += "\n\nError detected about here:\n" + sPath;
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

} // XMLParserException
