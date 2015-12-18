package test.beast.integration;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import beast.core.BEASTInterface;
import beast.core.Logger;
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
            File sExampleDir = new File(dir);
            String[] sExampleFiles = sExampleDir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".xml");
                }
            });

            List<String> sFailedFiles = new ArrayList<String>();
            for (String fileName : sExampleFiles) {
            	if (exceptions.contains(fileName)) {
                    System.out.println("Skipping exception " + fileName);
            	} else {
	                System.out.println("Processing " + fileName);
	                XMLProducer parser = new XMLProducer();
	                BEASTInterface o = null;
	                try {
	                    o = parser.parseFile(new File(dir + "/" + fileName));
	                } catch (Exception e) {
	                	o = null;
	                }
	                if (o != null) {
	                	String xml = parser.toXML(o);
	                	XMLParser parser2 = new XMLParser();
	                    try {
	                    	parser2.parseFragment(xml, false);
	                    } catch (Exception e) {
	                        System.out.println("test_ThatXmlExamplesProduces::Failed for " + fileName
	                                + ": " + e.getMessage());
	                        sFailedFiles.add(fileName);
	                    }
	                }
	                System.out.println("Done " + fileName);
            	}
            }
            if (sFailedFiles.size() > 0) {
                System.out.println("\ntest_ThatXmlExamplesProduces::Failed for : " + sFailedFiles.toString());
            } else {
                System.out.println("\ntest_ThatXmlExamplesProduces::Success");
            }
            assertTrue(sFailedFiles.toString(), sFailedFiles.size() == 0);
        } catch (Exception e) {
            System.out.println("exception thrown ");
            System.out.println(e.getMessage());
        }
    } // test_XmlExamples

}
