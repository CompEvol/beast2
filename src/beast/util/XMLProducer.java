/*
* File XMLProducer.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is part of BEAST 2.
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




import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import beast.core.*;


import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * converts MCMC plug in into XML, i.e. does the reverse of XMLParser
 * but tries to prettify the XML as well.
 */
public class XMLProducer extends XMLParser {
    /**
     * list of objects already converted to XML, so an idref suffices
     */
    HashSet<BEASTInterface> isDone;
    @SuppressWarnings("rawtypes")
    HashSet<Input> inputsDone;
    /**
     * list of IDs of elements produces, used to prevent duplicate ID generation
     */
    HashSet<String> IDs;
    /**
     * #spaces before elements in XML *
     */
    int indent;

    final public static String DEFAULT_NAMESPACE = "beast.core:beast.evolution.alignment:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood";
    //final public static String DO_NOT_EDIT_WARNING = "DO NOT EDIT the following machine generated text, they are used in Beauti";

    public XMLProducer() {
        super();
    }

    /**
     * Main entry point for this class
     * Given a plug-in, produces the XML in BEAST 2.0 format
     * representing the plug-in. This assumes plugin is Runnable
     */
    @SuppressWarnings("rawtypes")
    public String toXML(BEASTInterface plugin) {
        return toXML(plugin, new ArrayList<BEASTInterface>());
    }

    public String toXML(BEASTInterface plugin, Collection<BEASTInterface> others) {
        try {
            StringBuffer buf = new StringBuffer();
            buf.append("<" + XMLParser.BEAST_ELEMENT + " version='2.0' namespace='" + DEFAULT_NAMESPACE + "'>\n");
            for (String element : element2ClassMap.keySet()) {
            	if (!reservedElements.contains(element)) {
            		buf.append("<map name='" + element + "'>" + element2ClassMap.get(element) +"</map>\n");
            	}
            }
            buf.append("\n\n");
            isDone = new HashSet<BEASTInterface>();
            inputsDone = new HashSet<Input>();
            IDs = new HashSet<String>();
            indent = 0;
            pluginToXML(plugin, buf, null, true);
            String sEndBeast = "</" + XMLParser.BEAST_ELEMENT + ">";
            buf.append(sEndBeast);
            //return buf.toString();
            // beautify XML hierarchy
            String sXML = cleanUpXML(buf.toString(), m_sXMLBeuatifyXSL);
            // TODO: fix m_sIDRefReplacementXSL script to deal with nested taxon sets
            // String sXML2 = cleanUpXML(sXML, m_sIDRefReplacementXSL);
            String sXML2 = sXML;
            sXML = findPlates(sXML2);
            // beatify by applying name spaces to spec attributes
            String[] sNameSpaces = DEFAULT_NAMESPACE.split(":");
            for (String sNameSpace : sNameSpaces) {
                sXML = sXML.replaceAll("spec=\"" + sNameSpace + ".", "spec=\"");
            }


            buf = new StringBuffer();
            if (others.size() > 0) {
                for (BEASTInterface plugin2 : others) {
                    if (!IDs.contains(plugin2.getID())) {
                        pluginToXML(plugin2, buf, null, false);
                    }
                }
            }
            int iEnd = sXML.indexOf(sEndBeast);
            String extras = buf.toString();
            // prevent double -- inside XML comment, this can happen in sequences
            extras = extras.replaceAll("--","- - ");
            sXML = sXML.substring(0, iEnd) //+ "\n\n<!-- " + DO_NOT_EDIT_WARNING + " \n\n" + 
            	//extras +  "\n\n-->\n\n" 
            		+ sEndBeast;

            sXML = sXML.replaceAll("xmlns=\"http://www.w3.org/TR/xhtml1/strict\"", "");

            //insert newlines in alignments
            int k = sXML.indexOf("<data ");
            StringBuffer buf2 = new StringBuffer(sXML); 
            while (k > 0) {
            	while (sXML.charAt(k) != '>') {
            		if (sXML.charAt(k) == ' ') {
            			buf2.setCharAt(k, '\n');
            		}
            		k++;
            	}
            	k = sXML.indexOf("<data ", k + 1);
            }
            

            return buf2.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    } // toXML

    /**
     * like toXML() but without the assumption that plugin is Runnable *
     */
    public String modelToXML(BEASTInterface plugin) {
        try {
            String sXML0 = toRawXML(plugin);
            String sXML = cleanUpXML(sXML0, m_sSupressAlignmentXSL);
            // TODO: fix m_sIDRefReplacementXSL script to deal with nested taxon sets
            //String sXML2 = cleanUpXML(sXML, m_sIDRefReplacementXSL);
            String sXML2 = sXML;
            sXML = findPlates(sXML2);
            sXML = sXML.replaceAll("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>", "");
            sXML = sXML.replaceAll("\\n\\s*\\n", "\n");
            return sXML;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    } // toXML

    /**
     * like modelToXML, but without the cleanup *
     */
    @SuppressWarnings("rawtypes")
    public String toRawXML(BEASTInterface plugin) {
        return toRawXML(plugin, null);
    } // toRawXML

    /**
     * like modelToXML, but without the cleanup *
     * For plugin without name
     */
    @SuppressWarnings("rawtypes")
    public String toRawXML(BEASTInterface plugin, String sName) {
        try {
            StringBuffer buf = new StringBuffer();
            isDone = new HashSet<BEASTInterface>();
            inputsDone = new HashSet<Input>();
            IDs = new HashSet<String>();
            indent = 0;
            pluginToXML(plugin, buf, sName, false);
            return buf.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    } // toRawXML



    public String stateNodeToXML(BEASTInterface plugin) {
        try {
            StringBuffer buf = new StringBuffer();
            //buf.append("<" + XMLParser.BEAST_ELEMENT + " version='2.0'>\n");
            isDone = new HashSet<BEASTInterface>();
            IDs = new HashSet<String>();
            indent = 0;
            pluginToXML(plugin, buf, null, false);
            return buf.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Applies XSL script (specified in m_sXSL) to make XML a bit
     * nicer by removing unused IDs and moving data, beast.tree and likelihood
     * outside MCMC element.
     * Tries to compress common parts into plates.
     */
    String cleanUpXML(String sXML, String sXSL) throws TransformerException {
        StringWriter strWriter = new StringWriter();
        Reader xmlInput = new StringReader(sXML);
        javax.xml.transform.Source xmlSource =
                new javax.xml.transform.stream.StreamSource(xmlInput);
        Reader xslInput = new StringReader(sXSL);
        javax.xml.transform.Source xsltSource =
                new javax.xml.transform.stream.StreamSource(xslInput);
        javax.xml.transform.Result result =
                new javax.xml.transform.stream.StreamResult(strWriter);
        // create an instance of TransformerFactory
        javax.xml.transform.TransformerFactory transFact = javax.xml.transform.TransformerFactory.newInstance();
        javax.xml.transform.Transformer trans = transFact.newTransformer(xsltSource);
        trans.transform(xmlSource, result);

        String sXML2 = strWriter.toString();
        return sXML2;
    }

    // compress parts into plates
    String findPlates(String sXML) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(sXML)));
        doc.normalize();

        Node topNode = doc.getElementsByTagName("*").item(0);
        findPlates(topNode);

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        TransformerFactory factory2 = TransformerFactory.newInstance();
        Transformer transformer = factory2.newTransformer();
        transformer.transform(source, result);

        return sw.toString();
    }

    /**
     * tries to compress XML into plates *
     */
    void findPlates(Node node) {
        NodeList children = node.getChildNodes();
        for (int iChild = 0; iChild < children.getLength(); iChild++) {
            Node child = children.item(iChild);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                List<Node> comparables = new ArrayList<Node>();
                for (int iSibling = iChild + 1; iSibling < children.getLength(); iSibling++) {
                    if (children.item(iSibling).getNodeType() == Node.ELEMENT_NODE) {
                        Node sibling = children.item(iSibling);
                        if (comparable(child, sibling, ".p1", ".p" + (comparables.size() + 2))) {
                            comparables.add(sibling);
                        } else {
                            // break
                            iSibling = children.getLength();
                        }
                    }
                }
                if (comparables.size() > 0) {
                	// TODO: FIX THIS SO THAT NOT AN ARBITRARY `1' is used to generate the plate
                    // we can make a plate now
//                    String sRange = "1";
//                    int k = 2;
//                    for (Node sibling : comparables) {
//                        sRange += "," + k++;
//                        sibling.getParentNode().removeChild(sibling);
//                    }
//                    makePlate(child, "1", "n", sRange);
                }
            }
        }
        // recurse to lower levels
        children = node.getChildNodes();
        for (int iChild = 0; iChild < children.getLength(); iChild++) {
            findPlates(children.item(iChild));
        }
    } // findPlates

    /**
     * replace node element by a plate element with variable sVar and range sRange *
     */
    void makePlate(Node node, String sPattern, String sVar, String sRange) {
        Element plate = doc.createElement("plate");
        plate.setAttribute("var", sVar);
        plate.setAttribute("range", sRange);
        String sIndent = node.getPreviousSibling().getTextContent();
        replace(node, sPattern, sVar);
        node.getParentNode().replaceChild(plate, node);
        plate.appendChild(doc.createTextNode(sIndent + "  "));
        plate.appendChild(node);
        plate.appendChild(doc.createTextNode(sIndent));
    }

    /**
     * recursively replace all attribute values containing the pattern with variable sVar *
     */
    void replace(Node node, String sPattern, String sVar) {
        NamedNodeMap atts = node.getAttributes();
        if (atts != null) {
            for (int i = 0; i < atts.getLength(); i++) {
                Attr attr = (Attr) atts.item(i);
                String sValue = attr.getValue().replaceAll(sPattern, "\\$\\(" + sVar + "\\)");
                ;
                attr.setValue(sValue);
            }
        }
        NodeList children = node.getChildNodes();
        for (int iChild = 0; iChild < children.getLength(); iChild++) {
            Node child = children.item(iChild);
            replace(child, sPattern, sVar);
        }
    }

    /**
     * check if two XML nodes are the same, when sPattern1 is replaced by sPattothersern2 *
     */
    boolean comparable(Node node1, Node node2, String sPattern1, String sPattern2) {
        // compare name
        if (!node1.getNodeName().equals(node2.getNodeName())) {
            return false;
        }
        // compare text
        if (!node1.getTextContent().trim().equals(node2.getTextContent().trim())) {
            return false;
        }
        // compare attributes
        NamedNodeMap atts = node1.getAttributes();
        NamedNodeMap atts2 = node2.getAttributes();
        if (atts.getLength() != atts2.getLength()) {
            return false;
        }
        for (int i = 0; i < atts.getLength(); i++) {
            Attr attr = (Attr) atts.item(i);
            String sName = attr.getName();
            String sValue = attr.getValue();
            Node att = atts2.getNamedItem(sName);
            if (att == null) {
                return false;
            }
            String sValue2 = ((Attr) att).getValue();
            if (!sValue.equals(sValue2)) {
                sValue = sValue.replaceAll(sPattern1, "\\$\\(n\\)");
                sValue2 = sValue2.replaceAll(sPattern2, "\\$\\(n\\)");
                if (!sValue.equals(sValue2)) {
                    return false;
                }
            }
        }
        // compare children
        NodeList children = node1.getChildNodes();
        NodeList children2 = node2.getChildNodes();
        for (int iChild = 0; iChild < children.getLength(); iChild++) {
            Node child = children.item(iChild);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String sName = child.getNodeName();
                boolean bMatch = false;
                for (int iChild2 = 0; !bMatch && iChild2 < children2.getLength(); iChild2++) {
                    Node child2 = children2.item(iChild2);
                    if (child2.getNodeType() == Node.ELEMENT_NODE && sName.equals(child2.getNodeName())) {
                        bMatch = comparable(child, child2, sPattern1, sPattern2);
                    }
                }
                if (!bMatch) {
                    return false;
                }
            }
        }
        return true;
    } // comparable


    /**
     * XSL stylesheet for cleaning up bits and pieces of the vanilla XML
     * in order to make it more readable *
     */
    String m_sXMLBeuatifyXSL = "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns='http://www.w3.org/TR/xhtml1/strict'>\n" +
            "\n" +
            "<xsl:output method='xml' indent='yes'/>\n" +
            "\n" +
            "<xsl:template match='beast'>\n" +
            "    <xsl:copy>\n" +
            "        <xsl:apply-templates select='@*'/>\n" +
            "        <xsl:text>&#x0a;&#x0a;&#x0a;    </xsl:text>\n" +
            "        <xsl:apply-templates  select='//data[not(@idref)]' mode='copy'/>\n" +
            "        <xsl:text>&#x0a;&#x0a;&#x0a;    </xsl:text>\n" +
            "        <xsl:apply-templates select='//beast.tree[not(@idref)]' mode='copy'/>\n" +
            "        <xsl:text>&#x0a;&#x0a;&#x0a;    </xsl:text>\n" +
            "        <xsl:apply-templates select='//distribution[not(@idref) and not(ancestor::distribution)]' mode='copy'/>\n" +
            "        <xsl:text>&#x0a;&#x0a;&#x0a;    </xsl:text>\n" +
            "        <xsl:apply-templates select='node()'/>    \n" +
            "    </xsl:copy>\n" +
            "</xsl:template>\n" +
            "\n" +
            "<xsl:template match='*' mode='copy'>\n" +
            "  <xsl:copy>\n" +
            "    <xsl:attribute name='id'>\n" +
            "         <xsl:value-of select='@id'/>\n" +
            "    </xsl:attribute>\n" +
            "    <xsl:apply-templates select='@*|node()'/>\n" +
            "  </xsl:copy>\n" +
            "</xsl:template>\n" +
            "\n" +
            "<xsl:template match='data|beast.tree|distribution[not(ancestor::distribution)]'>\n" +
            "    <xsl:copy>\n" +
            "        <xsl:attribute name='idref'>\n" +
            "            <xsl:choose>\n" +
            "                <xsl:when test='@idref!=\"\"'><xsl:value-of select='@idref'/></xsl:when>\n" +
            "                <xsl:otherwise><xsl:value-of select='@id'/></xsl:otherwise>\n" +
            "            </xsl:choose>\n" +
            "        </xsl:attribute>\n" +
            "    <xsl:apply-templates select='@name'/>\n" +
            "    </xsl:copy>\n" +
            "</xsl:template>\n" +
            "\n" +
            "<xsl:template match='input'>\n" +
            "    <xsl:element name='{@name}'>" +
            "		<xsl:apply-templates select='node()|@*[name()!=\"name\"]'/>" +
            "	</xsl:element>\n" +
            "</xsl:template>\n" +

            "<xsl:template match='log/log'>\n" +
            "    <xsl:copy><xsl:apply-templates select='*[@*!=\"\"]'/> </xsl:copy>\n" +
            "</xsl:template>\n" +
            "\n" +
            // Better not suppress unused id's; used for example in reporting Operators
            //"<xsl:template match='@id'>\n" +
            //"    <xsl:if test='//@idref=. or not(contains(../@spec,substring(.,string-length(.)-2)))'>\n" +
            //"        <xsl:copy/>\n" +
            //"    </xsl:if>\n" +
            //"</xsl:template>\n" +
            "\n" +
            "<xsl:template match='@*|node()'>\n" +
            "  <xsl:copy>\n" +
            "    <xsl:apply-templates select='@*|node()'/>\n" +
            "  </xsl:copy>\n" +
            "</xsl:template>\n" +
            "\n" +
            "</xsl:stylesheet>\n";


    /**
     * script to reduce elements of the form <name idref='xyz'/> to name='@xyz' attributes *
     */
    String m_sIDRefReplacementXSL =
            "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns='http://www.w3.org/TR/xhtml1/strict'>\n" +
                    "\n" +
                    "<xsl:output method='xml' indent='yes'/>\n" +
                    "\n" +
                    "<xsl:template match='beast'>\n" +
                    "  <xsl:copy>\n" +
                    "    <xsl:apply-templates select='@*|node()'/>\n" +
                    "  </xsl:copy>\n" +
                    "</xsl:template>\n" +
                    "\n" +
                    "<xsl:template match='node()'>\n" +
                    "    <xsl:choose>\n" +
                    "    <xsl:when test='count(@idref)=1 and count(@name)=1 and count(@*)=2'>\n" +
                    "        <xsl:element name='{@name}'>\n" +
                    "            <xsl:attribute name='idref'>\n" +
                    "                <xsl:value-of select='@idref'/>\n" +
                    "            </xsl:attribute>\n" +
                    "        </xsl:element>\n" +
                    "    </xsl:when>\n" +
                    "    <xsl:when test='not(count(@idref)=1 and count(@*)=1)'>\n" +
                    "        <xsl:copy>\n" +
                    "           <xsl:apply-templates select='@*'/>\n" +
                    "		    <xsl:for-each select='*'>\n" +
                    "                <xsl:if test='count(@idref)=1 and count(@*)=1'>\n" +
                    "                    <xsl:attribute name='{name()}'>@<xsl:value-of select='@idref'/></xsl:attribute>\n" +
                    "                </xsl:if>\n" +
                    "		    </xsl:for-each>\n" +
                    "           <xsl:apply-templates/>\n" +
                    "        </xsl:copy>\n" +
                    "    </xsl:when>\n" +
                    "    </xsl:choose>\n" +
                    "</xsl:template>\n" +
                    "\n" +
                    "<xsl:template match='@*'>\n" +
                    "  <xsl:copy>\n" +
                    "    <xsl:apply-templates select='@*|node()'/>\n" +
                    "  </xsl:copy>\n" +
                    "</xsl:template>\n" +
                    "\n" +
                    "</xsl:stylesheet>";

    String s = "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns='http://www.w3.org/TR/xhtml1/strict'>\n" +
            "\n" +
            "<xsl:output method='xml' indent='yes'/>\n" +
            "\n" +
            "<xsl:template match='beast'>\n" +
            "  <xsl:copy>\n" +
            "    <xsl:apply-templates select='@*|node()'/>\n" +
            "  </xsl:copy>\n" +
            "</xsl:template>\n" +
            "\n" +
            "<xsl:template match='node()'>\n" +
            "    <xsl:if test='not(count(@idref)=1 and count(@*)=1)'>\n" +
            "    <xsl:copy>\n" +
            "       <xsl:apply-templates select='@*'/>\n" +
            "		<xsl:for-each select='*'>\n" +
            "            <xsl:if test='count(@idref)=1 and count(@*)=1'>\n" +
            "                <xsl:attribute name='{name()}'>@<xsl:value-of select='@idref'/></xsl:attribute>\n" +
            "            </xsl:if>\n" +
            "		</xsl:for-each>\n" +
            "       <xsl:apply-templates/>\n" +
            "    </xsl:copy>\n" +
            "    </xsl:if>\n" +
            "</xsl:template>\n" +
            "\n" +
            "<xsl:template match='@*'>\n" +
            "  <xsl:copy>\n" +
            "    <xsl:apply-templates select='@*|node()'/>\n" +
            "  </xsl:copy>\n" +
            "</xsl:template>\n" +
            "\n" +
            "</xsl:stylesheet>\n";


    /**
     * XSL stylesheet for suppressing alignment*
     */
    String m_sSupressAlignmentXSL = "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns='http://www.w3.org/TR/xhtml1/strict'>\n" +
            "\n" +
            "<xsl:output method='xml'/>\n" +
            "\n" +
            "<xsl:template match='data'/>\n" +
            "\n" +
            "<xsl:template match='input[@name]'>\n" +
            "    <xsl:element name='{@name}'>" +
            "		<xsl:apply-templates select='node()|@*[name()!=\"name\"]'/>" +
            "	</xsl:element>\n" +
            "</xsl:template>\n" +
            "\n" +
            "<xsl:template match='@*|node()'>\n" +
            "  <xsl:copy>\n" +
            "    <xsl:apply-templates select='@*|node()'/>\n" +
            "  </xsl:copy>\n" +
            "</xsl:template>\n" +
            "\n" +
            "</xsl:stylesheet>\n";

    /**
     * produce elements for a plugin with name sName, putting results in buf.
     * It tries to create XML conforming to the XML transformation rules (see XMLParser)
     * that is moderately readable.
     */
    @SuppressWarnings("rawtypes")
    void pluginToXML(BEASTInterface plugin, StringBuffer buf, String sName, boolean bIsTopLevel) throws Exception {
        // determine element name, default is input, otherswise find one of the defaults
        String sElementName = "input";
        for (String key : element2ClassMap.keySet()) {
        	String className = element2ClassMap.get(key);
        	Class _class = Class.forName(className);
        	if (_class.equals(plugin.getClass())) {
        		sElementName = key;
        	}
        }
        
//        if (plugin instanceof Alignment) {
//            sElementName = XMLParser.DATA_ELEMENT;
//        }
//        if (plugin instanceof Sequence) {
//            sElementName = XMLParser.SEQUENCE_ELEMENT;
//        }
//        if (plugin instanceof State) {
//            sElementName = XMLParser.STATE_ELEMENT;
//        }
//        if (plugin instanceof Distribution) {
//            sElementName = XMLParser.DISTRIBUTION_ELEMENT;
//        }
//        if (plugin instanceof Logger) {
//            sElementName = XMLParser.LOG_ELEMENT;
//        }
//        if (plugin instanceof Operator) {
//            sElementName = XMLParser.OPERATOR_ELEMENT;
//        }
//        if (plugin instanceof RealParameter) {
//            sElementName = XMLParser.REAL_PARAMETER_ELEMENT;
//        }
//        if (plugin instanceof Tree) {
//            sElementName = XMLParser.TREE_ELEMENT;
//        }

        if (bIsTopLevel) {
            sElementName = XMLParser.RUN_ELEMENT;
        }
        for (int i = 0; i < indent; i++) {
            buf.append("    ");
        }
        indent++;

        // open element
        buf.append("<").append(sElementName);

        boolean bSkipInputs = false;
        if (isDone.contains(plugin)) {
            // XML is already produced, we can idref it
            buf.append(" idref='" + plugin.getID() + "'");
            bSkipInputs = true;
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
                buf.append(" id='" + sID + "'");
                IDs.add(sID);
            }
            isDone.add(plugin);
        }
        String sClassName = plugin.getClass().getName();
        if (bSkipInputs == false && (!element2ClassMap.containsKey(sElementName) ||
                !element2ClassMap.get(sElementName).equals(sClassName))) {
            // only add spec element if it cannot be deduced otherwise (i.e., by idref or default mapping
            buf.append(" spec='" + sClassName + "'");
        }
        if (sName != null && !sName.equals(sElementName)) {
            // only add name element if it differs from element = default name
            buf.append(" name='" + sName + "'");
        }

        if (!bSkipInputs) {
            // process inputs of this plugin
            // first, collect values as attributes
            List<Input<?>> sInputs = BEASTObject.listInputs(plugin);
            for (Input sInput : sInputs) {
                inputToXML(sInput.getName(), plugin, buf, true);
            }
            // next, collect values as input elements
            StringBuffer buf2 = new StringBuffer();
            for (Input sInput : sInputs) {
                inputToXML(sInput.getName(), plugin, buf2, false);
            }
            if (buf2.length() == 0) {
                // if nothing was added by the inputs, close element
                indent--;
                buf.append("/>\n");
            } else {
                // add contribution of inputs
            	if (buf2.indexOf("<") >= 0) {
                    buf.append(">\n");
                    buf.append(buf2);
                    indent--;
                    for (int i = 0; i < indent; i++) {
                        buf.append("    ");
                    }
            	} else {
                    buf.append(">");
            		buf.append(buf2.toString().trim());
                    indent--;
            	}
                // add closing element
                buf.append("</" + sElementName + ">\n");
            }
        } else {
            // close element
            indent--;
            buf.append("/>\n");
        }
        if (indent < 2) {
            buf.append("\n");
        }
    } // pluginToXML


    /**
     * produce XML for an input of a plugin, both as attribute/value pairs for
     * primitive inputs (if bShort=true) and as individual elements (if bShort=false)
     *
     * @param sInput: name of the input
     * @param plugin: plugin to produce this input XML for
     * @param buf:    gets XML results are appended
     * @param bShort: flag to indicate attribute/value format (true) or element format (false)
     * @throws Exception 
     */
    @SuppressWarnings("rawtypes")
    void inputToXML(String sInput, BEASTInterface plugin, StringBuffer buf, boolean isShort) throws Exception {
        Field[] fields = plugin.getClass().getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType().isAssignableFrom(Input.class)) {
                Input input = (Input) fields[i].get(plugin);
                if (input.getName().equals(sInput)) {
                    // found the input with name sInput
                    if (input.get() != null) {
                        if (input.get() instanceof Map) {
                            // distinguish between List, Plugin and primitive input types
                        	if (isShort) {
	                        	Map<String,?> map = (Map<String,?>) input.get();
	                        	// determine label width
	                        	int whiteSpaceWidth = 0;
	                        	List<String> keys = new ArrayList<String>();
	                        	keys.addAll(map.keySet());
	                        	Collections.sort(keys);
	                        	for (String key : keys) {
	                        		whiteSpaceWidth = Math.max(whiteSpaceWidth, key.length());
	                        	}
	                        	for (String key : map.keySet()) {
                                    //buf.append("        <input name='" + key + "'>");
                                    buf.append("\n        " + key);
	                        		for (int k = key.length(); k < whiteSpaceWidth; k++) {
	                        			buf.append(' ');
	                        		}
	                        		buf.append("=\"" + normalise(map.get(key).toString()) + "\"");
	                        	}
                            }
                        	return;
                        } else if (input.get() instanceof List) {
                            if (!isShort) {
                            	int k = 0;
                            	List list = (List) input.get();
                                for (Object o2 : list) {
                                	if (o2 instanceof BEASTInterface) {
                                		pluginToXML((BEASTInterface) o2, buf, sInput, false);
                                	} else {
                                		k++;
                                		buf.append(o2.toString());
                                		if (k < list.size()) {
                                			buf.append(' ');
                                		}
                                	}
                                }
                            }
                            return;
                        } else if (input.get() instanceof BEASTInterface) {
                        	if (!input.get().equals(input.defaultValue)) {
	                            if (isShort && isDone.contains((BEASTInterface) input.get())) {
	                                buf.append(" " + sInput + "='@" + ((BEASTInterface) input.get()).getID() + "'");
	                                inputsDone.add(input);
	                            }
	                            if (!isShort && !inputsDone.contains(input)) {
	                                pluginToXML((BEASTInterface) input.get(), buf, sInput, false);
	                            }
                        	}
                            return;
                        } else {
                        	if (!input.get().equals(input.defaultValue)) {
	                            // primitive type, see if
	                            String sValue = input.get().toString();
	                            if (isShort) {
	                                if (sValue.indexOf('\n') < 0) {
	                                    buf.append(" " + sInput + "='" + normalise(input.get().toString()) + "'");
	                                }
	                            } else {
	                                if (sValue.indexOf('\n') >= 0) {
	                                    for (int j = 0; j < indent; j++) {
	                                        buf.append("    ");
	                                    }
	                                    if (sInput.equals("value")) {
	                                        buf.append(input.get().toString());
	                                    } else {
	                                        buf.append("<input name='" + sInput + "'>" + normalise(input.get().toString()) + "</input>\n");
	                                    }
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
        throw new Exception("Could not find input " + sInput + " in plugin " + plugin.getID() + " " + plugin.getClass().getName());
    } // inputToXML

    
   /** convert plain text string to XML string, replacing some entities **/
    String normalise(String str) {
    	str = str.replaceAll("&", "&amp;");    	
    	str = str.replaceAll("'", "&apos;");
    	str = str.replaceAll("\"", "&quot;");
    	str = str.replaceAll("<", "&lt;");
    	str = str.replaceAll(">", "&gt;");
    	return str;
    }

    
    
} // class XMLProducer

