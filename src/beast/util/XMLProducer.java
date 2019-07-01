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





import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
import org.xml.sax.SAXException;

import beast.app.BEASTVersion2;
import beast.core.BEASTInterface;
import beast.core.BEASTObjectStore;
import beast.core.Input;

/**
 * converts MCMC plug in into XML, i.e. does the reverse of XMLParser
 * but tries to prettify the XML as well.
 */
public class XMLProducer extends XMLParser {
    /**
     * list of objects already converted to XML, so an idref suffices
     */
    HashSet<BEASTInterface> isDone;
    Map<BEASTInterface, Set<String>> isInputsDone;

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
     * representing the plug-in. This assumes beast object is Runnable
     */
    public String toXML(Object beastObject) {
        return toXML(beastObject, new ArrayList<>());
    }

    public String toXML(Object beastObject, Collection<BEASTInterface> others) {
        try {
            StringBuffer buf = new StringBuffer();
        	Set<String> requiredPacakges = ClassToPackageMap.getPackagesAndVersions(beastObject);
        	String required = requiredPacakges.toString();
        	required = required.substring(1, required.length() - 1);
        	required = required.replaceAll(", ", ":");
            buf.append("<" + XMLParser.BEAST_ELEMENT + 
            		" version='" + new BEASTVersion2().getMajorVersion() + "'" +
            		" required='" + required + "'" +
            		" namespace='" + DEFAULT_NAMESPACE + "'>\n");
            for (String element : element2ClassMap.keySet()) {
            	if (!reservedElements.contains(element)) {
            		buf.append("<map name='" + element + "'>" + element2ClassMap.get(element) +"</map>\n");
            	}
            }
            buf.append("\n\n");
            isDone = new HashSet<>();
            isInputsDone = new HashMap<>();
            IDs = new HashSet<>();
            indent = 0;
            beastObjectToXML(beastObject, buf, null, true);
            String endBeastString = "</" + XMLParser.BEAST_ELEMENT + ">";
            buf.append(endBeastString);
            //return buf.toString();
            // beautify XML hierarchy
            String xml = cleanUpXML(buf.toString(), m_sXMLBeuatifyXSL);
            // TODO: fix m_sIDRefReplacementXSL script to deal with nested taxon sets
            // String xml2 = cleanUpXML(xml, m_sIDRefReplacementXSL);
            String xml2 = xml;
            xml = findPlates(xml2);
            // beatify by applying name spaces to spec attributes
            String[] nameSpaces = DEFAULT_NAMESPACE.split(":");
            for (String nameSpace : nameSpaces) {
                xml = xml.replaceAll("spec=\"" + nameSpace + ".", "spec=\"");
            }


            buf = new StringBuffer();
            if (others.size() > 0) {
                for (BEASTInterface beastObject2 : others) {
                    if (!IDs.contains(beastObject2.getID())) {
                        beastObjectToXML(beastObject2, buf, null, false);
                    }
                }
            }
            int endIndex = xml.indexOf(endBeastString);
            String extras = buf.toString();
            // prevent double -- inside XML comment, this can happen in sequences
            extras = extras.replaceAll("--","- - ");
            xml = xml.substring(0, endIndex) //+ "\n\n<!-- " + DO_NOT_EDIT_WARNING + " \n\n" + 
            	//extras +  "\n\n-->\n\n" 
            		+ endBeastString;

            xml = xml.replaceAll("xmlns=\"http://www.w3.org/TR/xhtml1/strict\"", "");
            
            xml = dedupName(xml);
            xml = sortTags(xml);
            

            //insert newlines in alignments
            int k = xml.indexOf("<data ");
            StringBuffer buf2 = new StringBuffer(xml); 
            while (k > 0) {
            	while (xml.charAt(k) != '>') {
            		if (xml.charAt(k) == ' ' && !xml.startsWith("idref", k+1)) {
            			buf2.setCharAt(k, '\n');
            		}
            		k++;
            	}
            	k = xml.indexOf("<data ", k + 1);
            }
            

            return buf2.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    } // toXML
    
    // ensure attributes are ordered so that id goes first, then spec, then remainder of attributes
    private String sortTags(String xml) {
    	//if (true) return xml;
    	String [] strs = xml.split("<");
    	StringBuilder bf = new StringBuilder();
    	bf.append(strs[0]);
    	for (int i = 1; i < strs.length; i++) {
    		String str = strs[i];
    		String [] ss = str.split(">");
    		boolean [] isShortEnd = new  boolean[ss.length];
    		for (int j = 0; j < ss.length; j++) {
    			if (ss[j].endsWith("/")) {
    				isShortEnd[j] = true;
    				ss[j] = ss[j].substring(0,  ss[j].length() - 1);
    			}
    		}
   			String [] strs2 = split(ss[0]);
   			int spec = 0;
   			while (spec < strs2.length && !strs2[spec].startsWith("spec=")) {
   				spec++;
   			}
   			int iD = 0;
   			while (iD < strs2.length && !strs2[iD].startsWith("id=")) {
   				iD++;
   			}
			bf.append('<');
			if (strs2[0] != null)
				bf.append(strs2[0]);
			if (iD < strs2.length) {
				bf.append(' ');
				bf.append(strs2[iD]);
			}
			if (spec < strs2.length) {
				bf.append(' ');
				bf.append(strs2[spec]);
			}
			for (int j = 1; j < strs2.length; j++) {
				if (j != iD && j != spec) {
	    			bf.append(' ');
	    			if (strs2[j] != null)
	    				bf.append(strs2[j]);
				}
			}
			for (int k = 1; k < ss.length; k++) {
				if (isShortEnd[k-1]) {
					bf.append('/');
				}
				bf.append('>');
				bf.append(ss[k]);
			}
			if (ss.length == 1 && str != null && str.endsWith(">")) {
				bf.append('>');
			}
    	}
		return bf.toString();
	}
    
    // since str.split(" "): does not match trailing spaces, we need to split by hand
    // also, attributes with spaces in them should not be split, e.g. <x id="a b"/> should be split in 2, not 3
    String [] split(String str) {
    	List<String> s = new ArrayList<>();
    	StringBuilder buf = new StringBuilder();
    	int i = 0;
    	while (i < str.length()) {
    		char c = str.charAt(i);
    		if (c == ' ') {
    			String str2 = buf.toString();
    			if ((str2.contains("='") && !str2.endsWith("'")) || 
    					(str2.contains("=\"") && !str2.endsWith("\""))) {
    				buf.append(c);
    			} else {
    				s.add(str2);
    				buf = new StringBuilder();
    			}
    		} else {
    			buf.append(c);
    		}
    		i++;
    	}
		s.add(buf.toString());
    	return s.toArray(new String []{});
    }

	String dedupName(String xml) {
        // replace <$x name="$y" idref="$z"/> and <$x idref="$z" name="$y"/> 
        // with <$y idref="$z"/>
        StringBuilder sb = new StringBuilder();
        int i = -1;
        while (++i < xml.length()) {
        	char c = xml.charAt(i);
        	if (c == '<') {
                StringBuilder tag = new StringBuilder();
        		tag.append(c);
        		while (((c = xml.charAt(++i)) != ' ') && (c != '/') && c != '>') {
        			tag.append(c);
        		}
        		if (c != '/' && c != '>') {
                    StringBuilder tag2 = new StringBuilder();
            		while ((c = xml.charAt(++i)) != '=') {
            			tag2.append(c);
            		}
            		if (tag2.toString().equals("name")) {
                        ++i;
                        StringBuilder value2 = new StringBuilder();
                		while ((c = xml.charAt(++i)) != '"') {
                			value2.append(c);
                		}
                        StringBuilder tag3 = new StringBuilder();
                    	c = xml.charAt(++i);
                        if (c != '>') {
                            if (c == '/') {
                        		tag3.append(c);
                            }
                        	while (((c = xml.charAt(++i)) != '=') && (c != '/') && (c != '>')) {
                        		tag3.append(c);
                        	}
                        }
                		if (c != '/' && c != '>' && tag3.toString().equals("idref")) {
                			tag3.append(c);
                			tag3.append(xml.charAt(++i));
                    		while ((c = xml.charAt(++i)) != '"') {
                    			tag3.append(c);
                    		}
                    		sb.append('<');
                			sb.append(value2);                			
                    		sb.append('=');
                    		sb.append(tag3);
                    		sb.append("/>");
                    		while ((c = xml.charAt(++i)) != '>') {}
                		} else {
                			sb.append(tag);
                			sb.append(' ');
                			sb.append(tag2);
                			sb.append("=\"");
                			sb.append(value2);                			
                			sb.append('"');
                			sb.append(' ');
                			sb.append(tag3);
                			sb.append(c);
                		}
            		} else if (tag2.toString().equals("idref")) {
            			tag2.append(c);
            			tag2.append(xml.charAt(++i));
                		while (((c = xml.charAt(++i)) != ' ') && (c != '/') && c != '>') {
                			tag2.append(c);
                		}
                		if (c != '/' && c != '>') {
                            StringBuilder tag3 = new StringBuilder();
                    		while ((c = xml.charAt(++i)) != '=') {
                    			tag3.append(c);
                    		}
                    		if (tag3.toString().equals("name")) {
                                ++i;
                                StringBuilder value2 = new StringBuilder();
                        		while ((c = xml.charAt(++i)) != '"') {
                        			value2.append(c);
                        		}
                        		sb.append('<');
                    			sb.append(value2);                			
                        		sb.append(' ');
                        		sb.append(tag2);
                        		sb.append("/>");
                        		while ((c = xml.charAt(++i)) != '>') {}
                    		} else {
                    			sb.append(tag);
                    			sb.append(' ');
                    			sb.append(tag2);
                    			sb.append(' ');
                    			sb.append(tag3);
                    			sb.append(c);
                    		}
                		} else {
                			sb.append(tag);
                			sb.append(' ');
                			sb.append(tag2);
                			sb.append(c);
                		}
            		} else {
            			sb.append(tag);
            			sb.append(' ');
            			sb.append(tag2);                			
            			sb.append(c);
            		}
        		} else {
        			sb.append(tag);
        			sb.append(c);
        		}
        	} else {
        		sb.append(c);
        	}
        }
        return sb.toString();
	}

	/**
     * like toXML() but without the assumption that beast object is Runnable *
     */
    public String modelToXML(BEASTInterface beastObject) {
        try {
            String xML0 = toRawXML(beastObject);
            String xml = cleanUpXML(xML0, m_sSupressAlignmentXSL);
            // TODO: fix m_sIDRefReplacementXSL script to deal with nested taxon sets
            //String xml2 = cleanUpXML(xml, m_sIDRefReplacementXSL);
            String xml2 = xml;
            xml = findPlates(xml2);
            xml = xml.replaceAll("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>", "");
            xml = xml.replaceAll("\\n\\s*\\n", "\n");
            return xml;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    } // toXML

    /**
     * like modelToXML, but without the cleanup *
     */
    public String toRawXML(BEASTInterface beastObject) {
        return toRawXML(beastObject, null);
    } // toRawXML

    /**
     * like modelToXML, but without the cleanup *
     * For beast object without name
     */
    public String toRawXML(BEASTInterface beastObject, String name) {
        try {
            StringBuffer buf = new StringBuffer();
            isDone = new HashSet<>();
            isInputsDone = new HashMap<>();
            IDs = new HashSet<>();
            indent = 0;
            beastObjectToXML(beastObject, buf, name, false);
            return buf.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    } // toRawXML



    public String stateNodeToXML(BEASTInterface beastObject) {
        try {
            StringBuffer buf = new StringBuffer();
            //buf.append("<" + XMLParser.BEAST_ELEMENT + " version='2.0'>\n");
            isDone = new HashSet<>();
            IDs = new HashSet<>();
            indent = 0;
            beastObjectToXML(beastObject, buf, null, false);
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
    String cleanUpXML(String xml, String xsl) throws TransformerException {
        StringWriter strWriter = new StringWriter();
        Reader xmlInput = new StringReader(xml);
        javax.xml.transform.Source xmlSource =
                new javax.xml.transform.stream.StreamSource(xmlInput);
        Reader xslInput = new StringReader(xsl);
        javax.xml.transform.Source xsltSource =
                new javax.xml.transform.stream.StreamSource(xslInput);
        javax.xml.transform.Result result =
                new javax.xml.transform.stream.StreamResult(strWriter);
        // create an instance of TransformerFactory
        javax.xml.transform.TransformerFactory transFact = javax.xml.transform.TransformerFactory.newInstance();
        javax.xml.transform.Transformer trans = transFact.newTransformer(xsltSource);
        trans.transform(xmlSource, result);

        String xml2 = strWriter.toString();
        return xml2;
    }

    // compress parts into plates
    String findPlates(String xml) throws SAXException, IOException, ParserConfigurationException, TransformerException  {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
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
        for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
            Node child = children.item(childIndex);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                List<Node> comparables = new ArrayList<>();
                for (int siblingNr = childIndex + 1; siblingNr < children.getLength(); siblingNr++) {
                    if (children.item(siblingNr).getNodeType() == Node.ELEMENT_NODE) {
                        Node sibling = children.item(siblingNr);
                        if (comparable(child, sibling, ".p1", ".p" + (comparables.size() + 2))) {
                            comparables.add(sibling);
                        } else {
                            // break
                            siblingNr = children.getLength();
                        }
                    }
                }
                if (comparables.size() > 0) {
                	// TODO: FIX THIS SO THAT NOT AN ARBITRARY `1' is used to generate the plate
                    // we can make a plate now
//                    String rangeString = "1";
//                    int k = 2;
//                    for (Node sibling : comparables) {
//                        rangeString += "," + k++;
//                        sibling.getParentNode().removeChild(sibling);
//                    }
//                    makePlate(child, "1", "n", rangeString);
                }
            }
        }
        // recurse to lower levels
        children = node.getChildNodes();
        for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
            findPlates(children.item(childIndex));
        }
    } // findPlates

    /**
     * replace node element by a plate element with variable var and range rangeString *
     */
    void makePlate(Node node, String pattern, String var, String rangeString) {
        Element plate = doc.createElement("plate");
        plate.setAttribute("var", var);
        plate.setAttribute("range", rangeString);
        String indent = node.getPreviousSibling().getTextContent();
        replace(node, pattern, var);
        node.getParentNode().replaceChild(plate, node);
        plate.appendChild(doc.createTextNode(indent + "  "));
        plate.appendChild(node);
        plate.appendChild(doc.createTextNode(indent));
    }

    /**
     * recursively replace all attribute values containing the pattern with variable var *
     */
    void replace(Node node, String pattern, String var) {
        NamedNodeMap atts = node.getAttributes();
        if (atts != null) {
            for (int i = 0; i < atts.getLength(); i++) {
                Attr attr = (Attr) atts.item(i);
                String valueString = attr.getValue().replaceAll(pattern, "\\$\\(" + var + "\\)");
                attr.setValue(valueString);
            }
        }
        NodeList children = node.getChildNodes();
        for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
            Node child = children.item(childIndex);
            replace(child, pattern, var);
        }
    }

    /**
     * check if two XML nodes are the same, when pattern1 is replaced by pattothersern2 *
     */
    boolean comparable(Node node1, Node node2, String pattern1, String pattern2) {
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
            String name = attr.getName();
            String valueString = attr.getValue();
            Node att = atts2.getNamedItem(name);
            if (att == null) {
                return false;
            }
            String valueString2 = ((Attr) att).getValue();
            if (!valueString.equals(valueString2)) {
                valueString = valueString.replaceAll(pattern1, "\\$\\(n\\)");
                valueString2 = valueString2.replaceAll(pattern2, "\\$\\(n\\)");
                if (!valueString.equals(valueString2)) {
                    return false;
                }
            }
        }
        // compare children
        NodeList children = node1.getChildNodes();
        NodeList children2 = node2.getChildNodes();
        for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
            Node child = children.item(childIndex);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String name = child.getNodeName();
                boolean isMatch = false;
                for (int childIndex2 = 0; !isMatch && childIndex2 < children2.getLength(); childIndex2++) {
                    Node child2 = children2.item(childIndex2);
                    if (child2.getNodeType() == Node.ELEMENT_NODE && name.equals(child2.getNodeName())) {
                        isMatch = comparable(child, child2, pattern1, pattern2);
                    }
                }
                if (!isMatch) {
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
     * produce elements for a beast object with name name, putting results in buf.
     * It tries to create XML conforming to the XML transformation rules (see XMLParser)
     * that is moderately readable.
     * @throws ClassNotFoundException 
     */
    @SuppressWarnings("rawtypes")
    void beastObjectToXML(Object beastObject, StringBuffer buf, String name, boolean isTopLevel) throws ClassNotFoundException {
        // determine element name, default is input, otherwise find one of the defaults
        String elementName = "input";
        for (String key : element2ClassMap.keySet()) {
        	String className = element2ClassMap.get(key);
        	Class _class = BEASTClassLoader.forName(className);
        	if (_class.equals(beastObject.getClass())) {
        		elementName = key;
        	}
        }
        
//        if (beastObject instanceof Alignment) {
//            elementName = XMLParser.DATA_ELEMENT;
//        }
//        if (beastObject instanceof Sequence) {
//            elementName = XMLParser.SEQUENCE_ELEMENT;
//        }
//        if (beastObject instanceof State) {
//            elementName = XMLParser.STATE_ELEMENT;
//        }
//        if (beastObject instanceof Distribution) {
//            elementName = XMLParser.DISTRIBUTION_ELEMENT;
//        }
//        if (beastObject instanceof Logger) {
//            elementName = XMLParser.LOG_ELEMENT;
//        }
//        if (beastObject instanceof Operator) {
//            elementName = XMLParser.OPERATOR_ELEMENT;
//        }
//        if (beastObject instanceof RealParameter) {
//            elementName = XMLParser.REAL_PARAMETER_ELEMENT;
//        }
//        if (beastObject instanceof Tree) {
//            elementName = XMLParser.TREE_ELEMENT;
//        }

        if (isTopLevel) {
            elementName = XMLParser.RUN_ELEMENT;
        }
        for (int i = 0; i < indent; i++) {
            buf.append("    ");
        }
        indent++;

        // open element
        buf.append("<").append(elementName);
        
        if (BEASTObjectStore.getId(beastObject) == null) {
        	String id = BEASTObjectStore.getClassName(beastObject);
        	if (id.contains(".")) {
        		id = id.substring(id.lastIndexOf('.') + 1);
        	}
            if (IDs.contains(id)) {
                int k = 1;
                while (IDs.contains(id + k)) {
                    k++;
                }
                id = id + k;
            }
            BEASTObjectStore.setId(beastObject, id);
        }

        boolean skipInputs = false;
        BEASTInterface beastObject2 = BEASTObjectStore.INSTANCE.getBEASTObject(beastObject);
        // isDone.contains(beastObject) fails when BEASTObjects override equals(), so use a stream with == instead
        if (isDone.stream().anyMatch(x -> x == beastObject2)) {
            // XML is already produced, we can idref it
            buf.append(" idref='" + normalise(BEASTObjectStore.getId(beastObject)) + "'");
            skipInputs = true;
        } else {
            // see whether a reasonable id can be generated
            if (BEASTObjectStore.getId(beastObject) != null && !BEASTObjectStore.getId(beastObject).equals("")) {
                String id = BEASTObjectStore.getId(beastObject);
                // ensure ID is unique, if not add index behind
                uniqueID(id, buf);
            }
            isDone.add(BEASTObjectStore.INSTANCE.getBEASTObject(beastObject));
        }
        String className = BEASTObjectStore.getClassName(beastObject);
        if (skipInputs == false && (!element2ClassMap.containsKey(elementName) ||
                !element2ClassMap.get(elementName).equals(className)||                
                reservedElements.contains(elementName))) {
            // only add spec element if it cannot be deduced otherwise (i.e., by idref or default mapping
            buf.append(" spec='" + className + "'");
        }
        if (name != null && !name.equals(elementName)) {
            // only add name element if it differs from element = default name
            buf.append(" name='" + name + "'");
        }

        if (!skipInputs) {
            // process inputs of this beast object
            // first, collect values as attributes
            List<Input<?>> inputs = BEASTObjectStore.listInputs(beastObject);
            //Collections.sort(inputs, (a,b) -> {return a.getName().compareTo(b.getName());});
            for (Input<?> input : inputs) {
            	Object value = input.get();
            	if (value != null) {
            		inputToXML(input, value, beastObject, buf, true);
            	}
            }
            // next, collect values as input elements
            StringBuffer buf2 = new StringBuffer();
            for (Input input : inputs) {
            	Object value = input.get();
            	if (value != null) {
            		inputToXML(input, value, beastObject, buf2, false);
            	}
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
                buf.append("</" + elementName + ">\n");
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

    // ensure ID is unique, if not add index behind
    private void uniqueID(String id, StringBuffer buf) {
        if (IDs.contains(id)) {
            int k = 1;
            while (IDs.contains(id + k)) {
                k++;
            }
            id = id + k;
        }
        buf.append(" id='" + normalise(id) + "'");
        IDs.add(id);
    }

    /**
     * produce XML for an input of a beast object, both as attribute/value pairs for
     * primitive inputs (if isShort=true) and as individual elements (if isShort=false)
     *
     * @param input: name of the input
     * @param beastObject: beast object to produce this input XML for
     * @param buf:    gets XML results are appended
     * @param isShort: flag to indicate attribute/value format (true) or element format (false)
     * @throws ClassNotFoundException 
     */
    void inputToXML(Input<?> input, Object value, Object beastObject, StringBuffer buf, boolean isShort) throws ClassNotFoundException {
    	//if (input.getName().equals("*")) {
    		// this can happen with beast.core.parameter.Map
    		// and * is not a valid XML attribute name
    		//return;
    	//}
        if (value != null) {
        	String name = input.getName();
            if (value instanceof Map) {
                // distinguish between List, Map, BEASTInterface and primitive input types
            	if (isShort) {
					@SuppressWarnings("unchecked")
					Map<String,?> map = (Map<String,?>) value;
                	// determine label width
                	int whiteSpaceWidth = 0;
                	List<String> keys = new ArrayList<>();
                	keys.addAll(map.keySet());
                	Collections.sort(keys);
                	for (String key : keys) {
                		whiteSpaceWidth = Math.max(whiteSpaceWidth, key.length());
                	}
                	for (String key : map.keySet()) {
                        //buf.append("        <input name='" + key + "'>");
                		if (!name.startsWith("*")) {
	                        buf.append("\n        " + key);
	                		for (int k = key.length(); k < whiteSpaceWidth; k++) {
	                			buf.append(' ');
	                		}
	                		buf.append("=\"" + normalise(map.get(key).toString()) + "\"");
                		}
                	}
                }
            	return;
            } else if (name.startsWith("*") || name.equals("id") || name.equals("name")) {
        		// this can happen with private inputs, like in ThreadedTreeLikelihood
        		// and * is not a valid XML attribute name
        		return;
            } else if (value instanceof List) {
                if (!isShort) {
                	int k = 0;
                	List<?> list = (List<?>) value;
                    for (Object o2 : list) {
                    	if (!BEASTObjectStore.isPrimitive(o2) ) {
                    		beastObjectToXML(o2, buf, input.getName(), false);
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
            } else if (value.getClass().isArray()) {
                if (isShort) {
	            	StringBuilder buf2 = new StringBuilder();
	            	buf2.append(" " + input.getName() + "=\"");
	            	Class type = value.getClass().getComponentType();
	            	if (type.isPrimitive()) {
	            		if (type.equals(Integer.TYPE)) {
			            	for (int o : (int []) value) {
			            		buf2.append(o + " ");
			            	}
	                    } else if (type.equals(Long.TYPE)) {
			            	for (long o : (long []) value) {
			            		buf2.append(o + " ");
			            	}
	                    } else if (type.equals(Short.TYPE)) {
			            	for (short o : (short []) value) {
			            		buf2.append(o + " ");
			            	}
	                    } else if (type.equals(Float.TYPE)) {
			            	for (float o : (float []) value) {
			            		buf2.append(o + " ");
			            	}
	                    } else if (type.equals(Double.TYPE)) {
			            	for (double o : (double []) value) {
			            		buf2.append(o + " ");
			            	}
	                    } else if (type.equals(Boolean.TYPE)) {
			            	for (boolean o : (boolean []) value) {
			            		buf2.append(o + " ");
			            	}
	                    } else if (type.equals(Byte.TYPE)) {
			            	for (byte o : (byte []) value) {
			            		buf2.append(o + " ");
			            	}
	                    } else if (type.equals(Character.TYPE)) {
			            	for (char o : (char []) value) {
			            		buf2.append(o + " ");
			            	}
		            	}	            		
	            	} else {
		            	for (Object o2 : (Object []) value) {
	                    	if (BEASTObjectStore.isPrimitive(o2)) {
	                    		buf2.append(o2.toString() + " ");
	                    	} else {
	                    		return;
	                    	}
		            	}
	                }	            	
	            	buf2.deleteCharAt(buf2.length() - 1);
	            	buf2.append("\"");
	            	buf.append(buf2);
                } else {
	            	Class type = value.getClass().getComponentType();
                	if (!type.isPrimitive()) {
		            	for (Object o2 : (Object []) value) {
	                    	StringBuffer buf3 = new StringBuffer();
	                    	if (!BEASTObjectStore.isPrimitive(o2)) {
	                    		beastObjectToXML(o2, buf3, input.getName(), false);
		                        buf.append(buf3);
	                    	}
		            	}
                	}
                }
            	return;
            } else if (value instanceof BEASTInterface) {
            	if (input.defaultValue == null || !value.equals(input.defaultValue)) {
            		BEASTInterface bo2 = BEASTObjectStore.INSTANCE.getBEASTObject(beastObject);
            		BEASTInterface value2 = BEASTObjectStore.INSTANCE.getBEASTObject(value);
                    if (isShort && isDone.contains(value2)) {
                        buf.append(" " + input.getName() + "='@" + normalise( value2.getID() ) + "'");
                        if (!isInputsDone.containsKey(bo2)) {
                        	isInputsDone.put(bo2, new HashSet<>());
                        }
                        isInputsDone.get(bo2).add(input.getName());
                    }
                    if (!isShort && (!isInputsDone.containsKey(bo2) ||
                    		!isInputsDone.get(bo2).contains(input.getName()))) {
                        beastObjectToXML((BEASTInterface) value, buf, input.getName(), false);
                    }
            	}
                return;
            } else {
            	if (BEASTObjectStore.isPrimitive(value)) {             	
	            	if (!value.equals(input.defaultValue)) {
	                    // primitive type
	                    String valueString = value.toString();
	                    if (isShort) {
	                        if (valueString.indexOf('\n') < 0) {
	                            buf.append(" " + input.getName() + "='" + normalise(value.toString()) + "'");
	                        }
	                    } else {
	                        if (valueString.indexOf('\n') >= 0) {
	                            for (int j = 0; j < indent; j++) {
	                                buf.append("    ");
	                            }
	                            if (input.getName().equals("value")) {
	                                buf.append(normalise(value.toString()));
	                            } else {
	                                buf.append("<input name='" + input.getName() + "'>" + normalise(value.toString()) + "</input>\n");
	                            }
	                        }
	                    }
	            	}
            	} else {
            		inputToXML(input, BEASTObjectStore.INSTANCE.getBEASTObject(value), beastObject, buf, isShort);
            	}
                return;
            }
        } else {
            // value=null, no XML to produce
            return;
        }
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

