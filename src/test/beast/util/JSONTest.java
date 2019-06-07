package test.beast.util;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import beast.app.beauti.BeautiDoc;
import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Taxon;
import beast.util.JSONParser;
import beast.util.JSONProducer;
import junit.framework.TestCase;

public class JSONTest extends TestCase {
	public static String JSON_FILE = "examples/testUCLNclock.json";

    @Test
    public void testJSONtoXMLtoJSON() throws Exception {
    	JSONParser parser = new JSONParser();
		BEASTObject beastObject = parser.parseFile(new File(JSON_FILE));
		JSONProducer producer = new JSONProducer();
		String actual = producer.toJSON(beastObject).trim();//.replaceAll("\\s+", " ");

		FileWriter outfile = new FileWriter(new File("/tmp/testUCLNclock.json"));
		outfile.write(actual);
		outfile.close();

		String expected = BeautiDoc.load(JSON_FILE).trim();//.replaceAll("\\s+", " ");
		assertEquals("Produced JSON differs from original", 
				expected, 
				actual);
    }

    @Test
    public void testJSONFragmentParsing() throws Exception {
    	JSONParser parser = new JSONParser();
    	String json = "{version: \"2.5\",\n" + 
    			"\n" + 
    			"beast: [\n" + 
    			"{spec:\"beast.core.parameter.RealParameter\",\n" +
    			" value:\"2.345\"\n" +
    			"}\n" +
    			"]\n" +
    			"}\n";
    			;
		List<Object> objects = parser.parseFragment(json, true);
		assertEquals(1, objects.size());
		RealParameter p = (RealParameter) objects.get(0);
		assertEquals(2.345, p.getValue(), 1e-13);
    }
    	
    @Test
    public void testAnnotatedConstructor() throws Exception {
    	List<Taxon> taxa = new ArrayList<>();
    	taxa.add(new Taxon("first one"));
    	taxa.add(new Taxon("second one"));
    			
    	AnnotatedRunnableTestClass t = new AnnotatedRunnableTestClass(3, taxa);
    	
    	JSONProducer producer = new JSONProducer();
    	String json = producer.toJSON(t);

    	assertEquals(3, (int) t.getParam1());

    	
        FileWriter outfile = new FileWriter(new File("/tmp/JSONTest.json"));
        outfile.write(json);
        outfile.close();

    	
    	JSONParser parser = new JSONParser();
    	BEASTInterface b;
    	b = parser.parseFile(new File("/tmp/JSONTest.json"));
    	assertEquals(3, (int) ((AnnotatedRunnableTestClass) b).getParam1());
    	assertEquals(2, ((AnnotatedRunnableTestClass) b).getTaxon().size());
    	
    	
    	// test that default value for param1 comes through
    	String json2 = "{version: \"2.5\",\n" + 
    			"namespace: \"beast.core:beast.evolution.alignment:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood\",\n" + 
    			"\n" + 
    			"beast: [\n" + 
    			"\n" + 
    			"\n" + 
    			"        {id: \"JSONTest\",\n" + 
    			"         spec: \"test.beast.util.AnnotatedRunnableTestClass\",\n" + 
    			"         taxon: [\n" + 
    			"                 {id: \"first one\", spec: \"Taxon\" },\n" + 
    			"                 {id: \"second one\", spec: \"Taxon\" }\n" + 
//    			"                 {id: \"first one\" },\n" + 
//    			"                 {id: \"second one\" }\n" + 
    			"          ]\n" + 
    			"        }\n" + 
    			"]\n" + 
    			"}";
    	
        outfile = new FileWriter(new File("/tmp/JSONTest2.json"));
        outfile.write(json2);
        outfile.close();

        parser = new JSONParser();
    	b = parser.parseFile(new File("/tmp/JSONTest2.json"));
    	assertEquals(10, (int) ((AnnotatedRunnableTestClass) b).getParam1());
    	assertEquals(2, ((AnnotatedRunnableTestClass) b).getTaxon().size());
    	

    	// test that array of doubles comes through in second constructor
    	String json3 = "{version: \"2.5\",\n" + 
    			"namespace: \"beast.core:beast.evolution.alignment:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood\",\n" + 
    			"\n" + 
    			"beast: [\n" + 
    			"\n" + 
    			"\n" + 
    			"        {id: \"JSONTest\",\n" + 
    			"         spec: \"test.beast.util.AnnotatedRunnableTestClass\",\n" + 
    			"         array: [1.0, 2.0, 3.0]\n" + 
    			"        }\n" + 
    			"]\n" + 
    			"}";
    	
        outfile = new FileWriter(new File("/tmp/JSONTest3.json"));
        outfile.write(json3);
        outfile.close();

        parser = new JSONParser();
    	b = parser.parseFile(new File("/tmp/JSONTest3.json"));
    	assertEquals(3, ((AnnotatedRunnableTestClass) b).getArray().size());
    }

}
