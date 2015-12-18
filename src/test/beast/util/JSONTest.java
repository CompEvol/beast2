package test.beast.util;

import java.io.File;

import org.junit.Test;

import beast.app.beauti.BeautiDoc;
import beast.core.BEASTObject;
import beast.util.JSONParser;
import beast.util.JSONProducer;
import junit.framework.TestCase;

public class JSONTest extends TestCase {
	public static String JSON_FILE = "examples/testUCLNclock.json";

    @Test
    public void testJSONtoXMLtoJSON() throws Exception {
    	JSONParser parser = new JSONParser();
		BEASTObject plugin = parser.parseFile(new File(JSON_FILE));
		JSONProducer producer = new JSONProducer();
		String actual = producer.toJSON(plugin).trim().replaceAll("\\s+", " ");
		
		String expected = BeautiDoc.load(JSON_FILE).trim().replaceAll("\\s+", " ");
		assertEquals("Produced JSON differs from original", 
				expected, 
				actual);
    }
}
