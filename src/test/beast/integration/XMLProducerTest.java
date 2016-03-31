package test.beast.integration;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import beast.core.BEASTInterface;
import beast.core.Logger;
import beast.evolution.datatype.Nucleotide;
import beast.util.Randomizer;
import beast.util.XMLParser;
import beast.util.XMLProducer;
import junit.framework.TestCase;

public class XMLProducerTest extends TestCase {

    @Test
    public void test_ThatXmlExamplesProduces() {
    	System.setProperty("java.only", "true");
        String dir = System.getProperty("user.dir") + "/examples";
        //String dir = "/tmp";
        List<String> exceptions = new ArrayList<String>();
        exceptions.add("testExponentialGrowth.xml");
    	test_ThatXmlExamplesProduces(dir, exceptions);
    }
    
    /** parse all XML files in the given directory, then produce XML from it, and see if the produced XML still parses **/
    public void test_ThatXmlExamplesProduces(String dir, List<String> exceptions) {
        try {
            Randomizer.setSeed(127);
            Logger.FILE_MODE = Logger.LogFileMode.overwrite;
            System.out.println("Test XML Examples in " + dir);
            File exampleDir = new File(dir);
            String[] exampleFiles = exampleDir.list(new FilenameFilter() {
                @Override
				public boolean accept(File dir, String name) {
                    return name.endsWith(".xml");
                }
            });

            List<String> failedFiles = new ArrayList<String>();
            for (String fileName : exampleFiles) {
            	if (exceptions.contains(fileName)) {
                    System.out.println("Skipping exception " + fileName);
            	} else {
	                System.out.println("Processing " + fileName);
	                XMLProducer producer = new XMLProducer();
	                BEASTInterface o = null;
	                try {
	                    o = producer.parseFile(new File(dir + "/" + fileName));
	                } catch (Exception e) {
	                	o = null;
	                }
	                if (o != null) {
	                	String xml = producer.toXML(o);
	                	
	                    //FileWriter outfile = new FileWriter(new File("/tmp/XMLProducerTest.xml"));
	                    //outfile.write(xml);
	                    //outfile.close();

	                	XMLParser parser2 = new XMLParser();
	                    try {
	                    	parser2.parseFragment(xml, false);
	                    } catch (Exception e) {
	                        System.out.println("test_ThatXmlExamplesProduces::Failed for " + fileName
	                                + ": " + e.getMessage());
	                        failedFiles.add(fileName);
	                    }
	                }
	                System.out.println("Done " + fileName);
            	}
            }
            if (failedFiles.size() > 0) {
                System.out.println("\ntest_ThatXmlExamplesProduces::Failed for : " + failedFiles.toString());
            } else {
                System.out.println("\ntest_ThatXmlExamplesProduces::Success");
            }
            assertTrue(failedFiles.toString(), failedFiles.size() == 0);
        } catch (Exception e) {
            System.out.println("exception thrown ");
            System.out.println(e.getMessage());
        }
    } // test_XmlExamples

    
    @Test
    public void testObjectWithoutID() {
    	BEASTInterface o = new Nucleotide();
    	XMLProducer producer = new XMLProducer();
    	System.out.println(producer.toXML(o));
    }
}
