package beast.util;

import org.w3c.dom.*;

import com.sun.org.apache.xerces.internal.dom.CoreDocumentImpl;

import beast.core.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 *
 * Provides basic functions for variable substitution and plates.
 *
 * @author Remco Bouckaert
 * @author Alexei Drummond
 */
public class XMLParserUtils {

    /**
     * Expand plates in XML by duplicating the containing XML and replacing
     * the plate variable with the appropriate value.
     */
    public static void processPlates(Document doc, String plateElementName) {
        // process plate elements
        final NodeList nodes = doc.getElementsByTagName(plateElementName);
        // instead of processing all plates, process them one by one,
        // then check recursively for new plates that could have been
        // created when they are nested
        if (nodes.getLength() > 0) {
            Node node = nodes.item(0);
            final String sVar = node.getAttributes().getNamedItem("var").getNodeValue();
            final String sRange = node.getAttributes().getNamedItem("range").getNodeValue();
            
            if (node.getAttributes().getNamedItem("fragment") != null) {
            	final String fragmentID = node.getAttributes().getNamedItem("fragment").getNodeValue();
            	Node fragment = getElementById(doc, fragmentID);
            	if (fragment == null) {
            		throw new RuntimeException("plate refers to fragment with id='" + fragmentID + "' that cannot be found");
            	}
            	fragment = fragment.cloneNode(true);
                node.getParentNode().replaceChild(fragment, node);
            	node = fragment;
           }
	
            final String[] sValues = sRange.split(",");

            // interpret values in the range of form x:y as all numbers between x and y inclusive
            List<String> vals = new ArrayList<>();
            for (final String sValue : sValues) {
                if (sValue.indexOf(":") > 0) {
                    String[] range = sValue.split(":");
                    int min = Integer.parseInt(range[0]);
                    int max = Integer.parseInt(range[1]);
                    for (int i = min; i <= max; i++) {
                        vals.add(String.valueOf(i));
                    }
                } else {
                    vals.add(sValue);
                }
            }

            for (final String val : vals) {
                // copy children
                final NodeList children = node.getChildNodes();
                for (int iChild = 0; iChild < children.getLength(); iChild++) {
                    final Node child = children.item(iChild);
                    final Node newChild = child.cloneNode(true);
                    replaceVariable(newChild, sVar, val);
                    node.getParentNode().insertBefore(newChild, node);
                }
            }
            node.getParentNode().removeChild(node);
            processPlates(doc,plateElementName);
        }
    } // processPlates
    
    static  Node getElementById(Document doc, String id) {
    	if (doc.getElementById(id) == null) {
    		registerIDs(doc, doc.getDocumentElement());
    	}
    	return doc.getElementById(id);
    }

    static void registerIDs(Document doc, Node node) {
    	if (node.getNodeType() == Node.ELEMENT_NODE) {
            if (node.getAttributes().getNamedItem("id") != null) {
            	final String id = node.getAttributes().getNamedItem("id").getNodeValue();
            	((CoreDocumentImpl) doc).putIdentifier(id, (Element) node);
            }
    	}
    	NodeList children = node.getChildNodes();
    	for (int i = 0; i < children.getLength(); i++) {
    		registerIDs(doc, children.item(i));
    	}
    }

    /** export DOM document to a file -- handy for debugging **/
    public static void saveDocAsXML(Document doc, String filename) {
    	try {
	    	Transformer transformer = TransformerFactory.newInstance().newTransformer();
	    	Result output = new StreamResult(new File(filename));
	    	Source input = new DOMSource(doc);
	
	    	transformer.transform(input, output);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}    	
    }
    
    /**
     * @param node the node to do variable replacement in
     * @param sVar the variable name to replace
     * @param sValue the value to replace the variable name with
     */
    public static void replaceVariable(final Node node, final String sVar, final String sValue) {
        switch (node.getNodeType()) {
	        case Node.ELEMENT_NODE:  {
	            final Element element = (Element) node;
	            final NamedNodeMap atts = element.getAttributes();
	            for (int i = 0; i < atts.getLength(); i++) {
	                final Attr attr = (Attr) atts.item(i);
	                if (attr.getValue().contains("$(" + sVar + ")")) {
	                    String sAtt = attr.getValue();
	                    sAtt = sAtt.replaceAll("\\$\\(" + sVar + "\\)", sValue);
	                    attr.setNodeValue(sAtt);
	                }
	            }
	        }
	        case Node.CDATA_SECTION_NODE: {
	        	String content = node.getTextContent();
	        	if (content.contains("$(" + sVar + ")")) {
	        		content = content.replaceAll("\\$\\(" + sVar + "\\)", sValue);
	        		node.setNodeValue(content);
	        	}
	        }
        }

        // process children
        final NodeList children = node.getChildNodes();
        for (int iChild = 0; iChild < children.getLength(); iChild++) {
            final Node child = children.item(iChild);
            replaceVariable(child, sVar, sValue);
        }
    } // replace



}
