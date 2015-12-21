package test.beast.util;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import beast.core.BEASTInterface;
import beast.evolution.alignment.Taxon;
import beast.util.XMLParser;
import beast.util.XMLProducer;
import junit.framework.TestCase;

public class XMLTest extends TestCase {

    @Test
    public void testAnnotatedConstructor2() throws Exception {
    	List<Taxon> taxa = new ArrayList<>();
    	taxa.add(new Taxon("first one"));
    	taxa.add(new Taxon("second one"));

    	AnnotatedRunnableTestClass t = new AnnotatedRunnableTestClass(3, taxa);
    	
    	XMLProducer producer = new XMLProducer();
    	String xml = producer.toXML(t);

    	assertEquals(3, t.getParam1());

    	
        FileWriter outfile = new FileWriter(new File("/tmp/XMLTest.xml"));
        outfile.write(xml);
        outfile.close();

    	
    	XMLParser parser = new XMLParser();
    	BEASTInterface b = parser.parseFile(new File("/tmp/XMLTest.xml"));
    	assertEquals(3, ((AnnotatedRunnableTestClass) b).getParam1());
    	assertEquals(2, ((AnnotatedRunnableTestClass) b).getTaxon().size());
    }

}
