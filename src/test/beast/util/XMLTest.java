package test.beast.util;

import java.io.File;
import java.io.FileWriter;

import org.junit.Test;

import beast.core.BEASTInterface;
import beast.util.XMLParser;
import beast.util.XMLProducer;
import junit.framework.TestCase;

public class XMLTest extends TestCase {

    @Test
    public void testAnnotatedConstructor2() throws Exception {
    	AnnotatedRunnableTestClass t = new AnnotatedRunnableTestClass(3);
    	
    	XMLProducer producer = new XMLProducer();
    	String xml = producer.toXML(t);

    	assertEquals(3, t.getParam1());

    	
        FileWriter outfile = new FileWriter(new File("/tmp/XMLTest.xml"));
        outfile.write(xml);
        outfile.close();

    	
    	XMLParser parser = new XMLParser();
    	BEASTInterface b = parser.parseFile(new File("/tmp/XMLTest.xml"));
    	assertEquals(3, ((AnnotatedRunnableTestClass) b).getParam1());
    }

}
