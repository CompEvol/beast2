package beast.app.beauti2.util;

import beast.util.XMLParser;
import jebl.evolution.alignments.Alignment;
import jebl.evolution.sequences.Sequence;
import jebl.evolution.taxa.Taxon;
import org.jdom.*;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class BeastImporter {

//    private final Element root;
//    private final DateFormat dateFormat;
//    private final java.util.Date origin;
//
//    public BeastImporter(Reader reader) throws IOException, JDOMException, Importer.ImportException {
//        SAXBuilder builder = new SAXBuilder();
//
//        Document doc = builder.build(reader);
//        root = doc.getRootElement();
//        if (!root.getName().equalsIgnoreCase("beast")) {
//            throw new Importer.ImportException("Unrecognized root element in XML file");
//        }
//
//        dateFormat = DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.UK);
//        dateFormat.setLenient(true);
//
//        Calendar cal = Calendar.getInstance(Locale.getDefault());
//
//        // set uses a zero based month (Jan = 0):
//        cal.set(0000, 00, 01);
////        origin = cal.getTime();
//
//        cal.setTimeInMillis(0);
//        origin = cal.getTime();
//    }
//
//    public void importBEAST(List<TaxonList> taxonLists, List<Alignment> alignments) throws Importer.ImportException {
//
//        TaxonList taxa = null;
//
//        List children = root.getChildren();
//        for(Object aChildren : children) {
//            Element child = (Element) aChildren;
//
//            if( child.getName().equalsIgnoreCase(TaxaParser.TAXA) ) {
//                if( taxa == null ) {
//                    taxa = readTaxa(null, child);
//                    taxonLists.add(taxa);
//                } else {
//                    taxonLists.add(readTaxa(taxa, child));
//                }
//            } else if( child.getName().equalsIgnoreCase(AlignmentParser.ALIGNMENT) ) {
//                if( taxa == null ) {
//                    throw new Importer.ImportException("taxa not defined");
//                }
//                alignments.add(readAlignment(child, taxa));
//            }
//        }
//    }
//
//    private TaxonList readTaxa(TaxonList primaryTaxa, Element e) throws Importer.ImportException {
//        Taxa taxa = new Taxa();
//
//        String id = e.getAttributeValue("id");
//        if (id != null) {
//            taxa.setId(id);
//        } else {
//            taxa.setId("taxa");
//        }
//
//        List children = e.getChildren();
//        for(Object aChildren : children) {
//            Element child = (Element) aChildren;
//
//            if( child.getName().equalsIgnoreCase(TaxonParser.TAXON) ) {
//                taxa.addTaxon(readTaxon(primaryTaxa, child));
//            }
//        }
//        return taxa;
//    }
//
//    private Taxon readTaxon(TaxonList primaryTaxa, Element e) throws Importer.ImportException {
//
//        String id = e.getAttributeValue(XMLParser.ID);
//
//        Taxon taxon = null;
//
//        if (id != null) {
//             taxon = new Taxon(id);
//
//            List children = e.getChildren();
//            for(Object aChildren : children) {
//                Element child = (Element) aChildren;
//
//                if( child.getName().equalsIgnoreCase(dr.evolution.util.Date.DATE) ) {
//                    Date date = readDate(child);
//                    taxon.setAttribute(dr.evolution.util.Date.DATE, date);
//                } else if( child.getName().equalsIgnoreCase(AttributeParser.ATTRIBUTE) ) {
//                    String name = child.getAttributeValue(AttributeParser.NAME);
//                    String value = child.getAttributeValue(AttributeParser.VALUE);
//                    if (value == null) {
//                        value = child.getTextTrim();
//                    }
//                    taxon.setAttribute(name, value);
//                }
//            }
//        } else {
//            String idref = e.getAttributeValue(XMLParser.IDREF);
//            int index = primaryTaxa.getTaxonIndex(idref);
//            if (index >= 0) {
//                taxon = primaryTaxa.getTaxon(index);
//            }
//        }
//
//        return taxon;
//    }
//
//    private Alignment readAlignment(Element e, TaxonList taxa) throws Importer.ImportException {
//        SimpleAlignment alignment = new SimpleAlignment();
//
//        List children = e.getChildren();
//        for(Object aChildren : children) {
//            Element child = (Element) aChildren;
//
//            if( child.getName().equalsIgnoreCase(SequenceParser.SEQUENCE) ) {
//                alignment.addSequence(readSequence(child, taxa));
//            }
//        }
//        return alignment;
//    }
//
//    private Sequence readSequence(Element e, TaxonList taxa) throws Importer.ImportException {
//
//        String taxonID = e.getChild(TaxonParser.TAXON).getAttributeValue(XMLParser.IDREF);
//        int index = taxa.getTaxonIndex(taxonID);
//        if (index < 0) {
//            throw new Importer.ImportException("Unknown taxon, " + taxonID + ", in alignment");
//        }
//        Taxon taxon = taxa.getTaxon(index);
//
//        String seq = e.getTextTrim();
//
//        return new Sequence(taxon, seq);
//    }
//
//    private Date readDate(Element e) throws Importer.ImportException {
//
//        String value = e.getAttributeValue(DateParser.VALUE);
//        boolean backwards = true;
//        String direction = e.getAttributeValue(DateParser.DIRECTION);
//        if (direction != null && direction.equalsIgnoreCase(DateParser.FORWARDS)) {
//            backwards = false;
//        }
//        try {
//            return new Date(dateFormat.parse(value));
//        } catch (ParseException e1) {
//            // ignore the parse exception and try it just as a number
//        }
//
//        // try just parsing it as a number
//        return new Date(Double.valueOf(value), Units.Type.YEARS, backwards);
//
//    }
//

}
