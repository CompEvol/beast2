package test.beast.integration;


import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import beast.core.Logger;
import beast.core.MCMC;
import beast.util.Randomizer;
import beast.util.XMLParser;
import junit.framework.TestCase;

/**
 * check whether all example files parse *
 */
public class ExampleXmlParsingTest extends TestCase {
	public static void setUpTestDir() {
		// make sure output goes to test directory
		File testDir = 	new File("./test");
		if (!testDir.exists()) {
			testDir.mkdir();
		}
		System.setProperty("file.name.prefix","test/");
	}
	
	{
		setUpTestDir();
	}

    @Test
    public void test_ThatXmlExamplesParse() {
        String dir = System.getProperty("user.dir") + "/examples";
    	test_ThatXmlExamplesParse(dir);
    }
    
    public void test_ThatXmlExamplesParse(String dir) {
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
                System.out.println("Processing " + fileName);
                XMLParser parser = new XMLParser();
                try {
                    parser.parseFile(new File(dir + "/" + fileName));
                } catch (Exception e) {
                    System.out.println("ExampleXmlParsing::Failed for " + fileName
                            + ": " + e.getMessage());
                    sFailedFiles.add(fileName);
                }
                System.out.println("Done " + fileName);
            }
            if (sFailedFiles.size() > 0) {
                System.out.println("\ntest_ThatXmlExamplesParse::Failed for : " + sFailedFiles.toString());
            } else {
                System.out.println("\ntest_ThatXmlExamplesParse::Success");
            }
            assertTrue(sFailedFiles.toString(), sFailedFiles.size() == 0);
        } catch (Exception e) {
            System.out.println("exception thrown ");
            System.out.println(e.getMessage());
        }
    } // test_XmlExamples

    @Test
    public void test_ThatXmlExamplesRun() {
        String dir = System.getProperty("user.dir") + "/examples";
        test_ThatXmlExamplesRun(dir);
    }
    
    public void test_ThatXmlExamplesRun(String dir) {
        try {
            Logger.FILE_MODE = Logger.LogFileMode.overwrite;
            System.out.println("Test that XML Examples run in " + dir);
            File sExampleDir = new File(dir);
            String[] sExampleFiles = sExampleDir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".xml");
                }
            });

            List<String> sFailedFiles = new ArrayList<String>();
            int nSeed = 127;
            for (String fileName : sExampleFiles) {
                Randomizer.setSeed(nSeed);
                nSeed += 10; // need more than one to prevent trouble with multiMCMC logs
                System.out.println("Processing " + fileName);
                XMLParser parser = new XMLParser();
                try {
                    beast.core.Runnable runable = parser.parseFile(new File(dir + "/" + fileName));
                    if (runable instanceof MCMC) {
                        MCMC mcmc = (MCMC) runable;
                        mcmc.setInputValue("preBurnin", 0);
                        mcmc.setInputValue("chainLength", 1000);
                        mcmc.run();
                    }
                } catch (Exception e) {
                    System.out.println("ExampleXmlParsing::Failed for " + fileName
                            + ": " + e.getMessage());
                    sFailedFiles.add(fileName);
                }
                System.out.println("Done " + fileName);
            }
            if (sFailedFiles.size() > 0) {
                System.out.println("\ntest_ThatXmlExamplesRun::Failed for : " + sFailedFiles.toString());
            } else {
                System.out.println("SUCCESS!!!");
            }
            assertTrue(sFailedFiles.toString(), sFailedFiles.size() == 0);
        } catch (Exception e) {
            System.out.println("exception thrown ");
            System.out.println(e.getMessage());
            ;
        }
    } // test_ThatXmlExamplesRun


    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main("test.beast.integration.ExampleXmlParsingTest");
    }


} // ExampleXmlParsingTest
