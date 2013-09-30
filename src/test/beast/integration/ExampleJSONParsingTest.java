package test.beast.integration;


import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import beast.core.Logger;
import beast.core.MCMC;
import beast.util.JSONParser;
import beast.util.Randomizer;


import junit.framework.TestCase;

/**
 * check whether all example files parse *
 */
public class ExampleJSONParsingTest extends TestCase {

    @Test
    public void test_ThatXmlExamplesParse() {
        String dir = System.getProperty("user.dir") + "/examples";
    	test_ThatJSONExamplesParse(dir);
    }
    
    public void test_ThatJSONExamplesParse(String dir) {
        try {
            Randomizer.setSeed(127);
            Logger.FILE_MODE = Logger.LogFileMode.overwrite;
            System.out.println("Test JSON Examples in " + dir);
            File sExampleDir = new File(dir);
            String[] sExampleFiles = sExampleDir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".json");
                }
            });

            List<String> sFailedFiles = new ArrayList<String>();
            for (String fileName : sExampleFiles) {
                System.out.println("Processing " + fileName);
                JSONParser parser = new JSONParser();
                try {
                    parser.parseFile(new File(dir + "/" + fileName));
                } catch (Exception e) {
                    System.out.println("ExampleJSONParsing::Failed for " + fileName
                            + ": " + e.getMessage());
                    sFailedFiles.add(fileName);
                }
                System.out.println("Done " + fileName);
            }
            if (sFailedFiles.size() > 0) {
                System.out.println("\ntest_ThatJSONExamplesParse::Failed for : " + sFailedFiles.toString());
            } else {
                System.out.println("\ntest_ThatJSONExamplesParse::Success");
            }
            assertTrue(sFailedFiles.toString(), sFailedFiles.size() == 0);
        } catch (Exception e) {
            System.out.println("exception thrown ");
            System.out.println(e.getMessage());
        }
    } // test_JSONExamples

    @Test
    public void test_ThatJSONExamplesRun() {
        String dir = System.getProperty("user.dir") + "/examples";
        test_ThatJSONExamplesRun(dir);
    }
    
    public void test_ThatJSONExamplesRun(String dir) {
        try {
            Logger.FILE_MODE = Logger.LogFileMode.overwrite;
            System.out.println("Test that JSON Examples run in " + dir);
            File sExampleDir = new File(dir);
            String[] sExampleFiles = sExampleDir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".json");
                }
            });

            List<String> sFailedFiles = new ArrayList<String>();
            int nSeed = 127;
            for (String fileName : sExampleFiles) {
                Randomizer.setSeed(nSeed);
                nSeed += 10; // need more than one to prevent trouble with multiMCMC logs
                System.out.println("Processing " + fileName);
                JSONParser parser = new JSONParser();
                try {
                    beast.core.Runnable runable = parser.parseFile(new File(dir + "/" + fileName));
                    if (runable instanceof MCMC) {
                        MCMC mcmc = (MCMC) runable;
                        mcmc.setInputValue("preBurnin", 0);
                        mcmc.setInputValue("chainLength", 1000);
                        mcmc.run();
                    }
                } catch (Exception e) {
                    System.out.println("ExampleJSONParsing::Failed for " + fileName
                            + ": " + e.getMessage());
                    sFailedFiles.add(fileName);
                }
                System.out.println("Done " + fileName);
            }
            if (sFailedFiles.size() > 0) {
                System.out.println("\ntest_ThatJSONExamplesRun::Failed for : " + sFailedFiles.toString());
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
        org.junit.runner.JUnitCore.main("test.beast.integration.ExampleJSONParsingTest");
    }

} // ExampleJSONParsingTest
