package test.beast.util;

import junit.framework.TestCase;
import org.junit.Test;

import beast.base.BEASTInterface;
import beast.evolution.alignment.Taxon;
import beast.parser.XMLParser;
import beast.parser.XMLParserUtils;
import beast.parser.XMLProducer;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLTest extends TestCase {

    @Test
    public void testAnnotatedConstructor2() throws Exception {
    	List<Taxon> taxa = new ArrayList<>();
    	taxa.add(new Taxon("first one"));
    	taxa.add(new Taxon("second one"));

    	AnnotatedRunnableTestClass t = new AnnotatedRunnableTestClass(3, taxa);
    	
    	XMLProducer producer = new XMLProducer();
    	String xml = producer.toXML(t);

    	assertEquals(3, (int) t.getParam1());

    	
        FileWriter outfile = new FileWriter(new File("/tmp/XMLTest.xml"));
        outfile.write(xml);
        outfile.close();

    	
    	XMLParser parser = new XMLParser();
    	BEASTInterface b = parser.parseFile(new File("/tmp/XMLTest.xml"));
    	assertEquals(3, (int) ((AnnotatedRunnableTestClass) b).getParam1());
    	assertEquals(2, ((AnnotatedRunnableTestClass) b).getTaxon().size());
    }

    @Test
    public void testVariableReplacement() {

        String stringWithVariables = "$(one) $(two) $(three=3)";

        Map<String,String> variableDefs = new HashMap<>();

        XMLParserUtils.extractVariableDefaultsFromString(stringWithVariables, variableDefs);

        assertEquals(1, variableDefs.size());
        assertTrue(variableDefs.containsKey("three"));
        assertTrue(variableDefs.containsValue("3"));

        String modifiedString = XMLParserUtils.replaceVariablesInString(stringWithVariables, variableDefs);
        assertEquals("$(one) $(two) 3", modifiedString);
	}
}
