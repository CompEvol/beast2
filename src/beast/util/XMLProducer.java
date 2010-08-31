/*
* File XMLProducer.java
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


import beast.core.*;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Tree;

import javax.xml.transform.TransformerException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;

/**
 * converts MCMC plug in into XML, i.e. does the reverse of XMLParser *
 */
public class XMLProducer extends XMLParser {
    /**
     * list of objects already converted to XML, so an idref suffices
     */
    HashSet<Plugin> m_bDone;
    /**
     * list of IDs of elements produces, used to prevent duplicate ID generation
     */
    HashSet<String> m_sIDs;
    /**
     * #spaces before elements in XML *
     */
    int m_nIndent;

    final static String DEFAULT_NAMESPACE="beast.core:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood";
    public XMLProducer() {
        super();
    }

    /**
     * Main entry point for this class
     * Given a plug-in, produces the XML in BEAST 2.0 format
     * representing the plug-in. This assumes plugin is Runnable
     */
    public String toXML(Plugin plugin) {
        try {
            StringBuffer buf = new StringBuffer();
            buf.append("<" + XMLParser.BEAST_ELEMENT + " version='2.0' namespace='" + DEFAULT_NAMESPACE + "'>\n");
            m_bDone = new HashSet<Plugin>();
            m_sIDs = new HashSet<String>();
            m_nIndent = 0;
            pluginToXML(plugin, buf, null, true);
            buf.append("</" + XMLParser.BEAST_ELEMENT + ">");
            //return buf.toString();
            // beautify XML hierarchy
            String sXML = cleanUpXML(buf.toString(), m_sXSL);
            // beatify by applying name spaces to spec attributes
            String [] sNameSpaces = DEFAULT_NAMESPACE.split(":");
            for (String sNameSpace : sNameSpaces) {
            	sXML = sXML.replaceAll("spec=\"" + sNameSpace+".", "spec=\"");
            }
            return sXML;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    } // toXML

    /** like toXML() but without the assumption that plugin is Runnable **/
    public String modelToXML(Plugin plugin) {
        try {
            StringBuffer buf = new StringBuffer();
            //buf.append("<" + XMLParser.BEAST_ELEMENT + " version='2.0'>\n");
            m_bDone = new HashSet<Plugin>();
            m_sIDs = new HashSet<String>();
            m_nIndent = 0;
            pluginToXML(plugin, buf, null, false);
            //buf.append("</" + XMLParser.BEAST_ELEMENT + ">");
            String sXML = cleanUpXML(buf.toString(), m_sXSL2);
            sXML = sXML.replaceAll("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>", "");
            sXML = sXML.replaceAll("\\n\\s*\\n", "\n");
            return sXML;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    } // toXML

    public String stateNodeToXML(Plugin plugin) {
        try {
            StringBuffer buf = new StringBuffer();
            //buf.append("<" + XMLParser.BEAST_ELEMENT + " version='2.0'>\n");
            m_bDone = new HashSet<Plugin>();
            m_sIDs = new HashSet<String>();
            m_nIndent = 0;
            pluginToXML(plugin, buf, null, false);
            return buf.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * applies XSL script (specified in m_sXSL) to make XML a bit
     * nicer by removing unused IDs and moving data, beast.tree and likelihood
     * outside MCMC element.
     */
    String cleanUpXML(String sXML, String sXSL) throws TransformerException {
//if(true) return sXML;
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

        return strWriter.toString();
    }

    /**
     * XSL stylesheet for cleaning up bits and pieces of the vanilla XML
     * in order to make it more readable *
     */
    String m_sXSL = "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns='http://www.w3.org/TR/xhtml1/strict'>\n" +
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
            "<xsl:template match='@id'>\n" +
            "    <xsl:if test='//@idref=. or not(contains(../@spec,substring(.,string-length(.)-2)))'>\n" +
            "        <xsl:copy/>\n" +
            "    </xsl:if>\n" +
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
     * XSL stylesheet for suppressing alignment*
     */
    String m_sXSL2 = "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns='http://www.w3.org/TR/xhtml1/strict'>\n" +
            "\n" +
            "<xsl:output method='xml'/>\n" +
            "\n" +
            "<xsl:template match='data'/>\n" +
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
    void pluginToXML(Plugin plugin, StringBuffer buf, String sName, boolean bIsTopLevel) throws Exception {
        // determine element name, default is input, otherswise find one of the defaults
        String sElementName = "input";
        if (plugin instanceof Alignment) {
            sElementName = XMLParser.DATA_ELEMENT;
        }
        if (plugin instanceof Sequence) {
            sElementName = XMLParser.SEQUENCE_ELEMENT;
        }
        if (plugin instanceof State) {
            sElementName = XMLParser.STATE_ELEMENT;
        }
        if (plugin instanceof Density ) {
            sElementName = XMLParser.DISTRIBUTION_ELEMENT;
        }
        if (plugin instanceof Logger) {
            sElementName = XMLParser.LOG_ELEMENT;
        }
//        if (plugin instanceof MCMC) {
//            sElementName = XMLParser.MCMC_ELEMENT;
//        }
        if (plugin instanceof Operator) {
            sElementName = XMLParser.OPERATOR_ELEMENT;
        }
        if (plugin instanceof RealParameter) {
            sElementName = XMLParser.REAL_PARAMETER_ELEMENT;
        }
//        if (plugin instanceof IntegerParameter) {
//            sElementName = XMLParser.INT_PARAMETER_ELEMENT;
//        }
//        if (plugin instanceof BooleanParameter) {
//            sElementName = XMLParser.BOOL_PARAMETER_ELEMENT;
//        }
        if (plugin instanceof Tree) {
            sElementName = XMLParser.TREE_ELEMENT;
        }
	     
        if (bIsTopLevel) {
            sElementName = XMLParser.RUN_ELEMENT;
        }
        for (int i = 0; i < m_nIndent; i++) {
            buf.append("    ");
        }
        m_nIndent++;

        // open element
        buf.append("<").append(sElementName);

        boolean bSkipInputs = false;
        if (m_bDone.contains(plugin)) {
            // XML is already produced, we can idref it
            buf.append(" idref='" + plugin.getID() + "'");
            bSkipInputs = true;
        } else {
            // see whether a reasonable id can be generated
            if (plugin.getID() != null && !plugin.getID().equals("")) {
                String sID = plugin.getID();
                // ensure ID is unique
                if (m_sIDs.contains(sID)) {
                    int k = 1;
                    while (m_sIDs.contains(sID + k)) {
                        k++;
                    }
                    sID = sID + k;
                }
                buf.append(" id='" + sID + "'");
                m_sIDs.add(sID);
            }
            m_bDone.add(plugin);
        }
        String sClassName = plugin.getClass().getName();
        if (bSkipInputs == false && (!m_sElement2ClassMap.containsKey(sElementName) ||
                !m_sElement2ClassMap.get(sElementName).equals(sClassName))) {
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
            List<Input<?>> sInputs = plugin.listInputs();
            for (Input sInput : sInputs) {
                inputToXML(sInput.getName(), plugin, buf, true);
            }
            buf.append(">\n");
            // next, collect values as input elements
            for (Input sInput : sInputs) {
                inputToXML(sInput.getName(), plugin, buf, false);
            }
            m_nIndent--;
            for (int i = 0; i < m_nIndent; i++) {
                buf.append("    ");
            }
            // add closing elment
            buf.append("</" + sElementName + ">\n");
        } else {
            // close element
            m_nIndent--;
            buf.append("/>\n");
        }
        if (m_nIndent < 2) {
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
	void inputToXML(String sInput, Plugin plugin, StringBuffer buf, boolean bShort) throws Exception {
        Field[] fields = plugin.getClass().getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType().isAssignableFrom(Input.class)) {
				Input input = (Input) fields[i].get(plugin);
                if (input.getName().equals(sInput)) {
                    // found the input with name sInput
                    if (input.get() != null) {
                        // distinguish between List, Plugin and primitive input types
                        if (input.get() instanceof List) {
                            if (!bShort) {
                                for (Object o2 : (List) input.get()) {
                                    pluginToXML((Plugin) o2, buf, sInput, false);
                                }
                            }
                            return;
                        } else if (input.get() instanceof Plugin) {
                            if (!bShort) {
                                pluginToXML((Plugin) input.get(), buf, sInput, false);
                            }
                            return;
                        } else {
                            // primitive type, see if
                            String sValue = input.get().toString();
                            if (bShort) {
                                if (sValue.indexOf('\n') < 0) {
                                    buf.append(" " + sInput + "='" + input.get().toString() + "'");
                                }
                            } else {
                                if (sValue.indexOf('\n') >= 0) {
                                    for (int j = 0; j < m_nIndent; j++) {
                                        buf.append("    ");
                                    }
                                    if (sInput.equals("value")) {
                                        buf.append(input.get().toString());
                                    } else {
                                        buf.append("<input name='" + sInput + "'>" + input.get().toString() + "</input>\n");
                                    }
                                }
                            }
                            return;
                        }
                    } else {
                        // value=null, so XML to produce
                        return;
                    }
                }
            }
        }
        // should never get here
        throw new Exception("Could not find input " + sInput + " in plugin " + plugin.getID() + " " + plugin.getClass().getName());
    } // inputToXML

} // class XMLProducer

