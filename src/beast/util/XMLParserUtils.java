package beast.util;

import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.List;

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
            final Node node = nodes.item(0);
            final String sVar = node.getAttributes().getNamedItem("var").getNodeValue();
            final String sRange = node.getAttributes().getNamedItem("range").getNodeValue();

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

    /**
     * @param node the node to do variable replacement in
     * @param sVar the variable name to replace
     * @param sValue the value to replace the variable name with
     */
    public static void replaceVariable(final Node node, final String sVar, final String sValue) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
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

        // process children
        final NodeList children = node.getChildNodes();
        for (int iChild = 0; iChild < children.getLength(); iChild++) {
            final Node child = children.item(iChild);
            replaceVariable(child, sVar, sValue);
        }
    } // replace



}
