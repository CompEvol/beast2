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

/** check whether all example files parse **/
public class ExampleXmlParsingTest extends TestCase {

	@Test
	public void test_ThatXmlExamplesParse() {
		try {
			Randomizer.setSeed(127);
			Logger.FILE_MODE = Logger.FILE_OVERWRITE;
			String sDir = System.getProperty("user.dir") + "/examples";
			System.out.println("Test XML Examples in " + sDir);
			File sExampleDir = new File(sDir);
			String[] sExampleFiles = sExampleDir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			});
	
			List<String> sFailedFiles = new ArrayList<String>();
			for (String sFileName : sExampleFiles) {
				System.out.println("Processing " + sFileName);
				XMLParser parser = new XMLParser();
				try {
					parser.parseFile(sDir + "/" + sFileName);
				} catch (Exception e) {
					System.out.println("ExampleXmlParsing::Failed for " + sFileName
							+ ": " + e.getMessage());
					sFailedFiles.add(sFileName);
				}
				System.out.println("Done " + sFileName);
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
		try {
			Logger.FILE_MODE = Logger.FILE_OVERWRITE;
			String sDir = System.getProperty("user.dir") + "/examples";
			System.out.println("Test that XML Examples run in " + sDir);
			File sExampleDir = new File(sDir);
			String[] sExampleFiles = sExampleDir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			});
	
			List<String> sFailedFiles = new ArrayList<String>();
			int nSeed = 127;
			for (String sFileName : sExampleFiles) {
				Randomizer.setSeed(nSeed);
				nSeed += 10; // need more than one to prevent trouble with multiMCMC logs
				System.out.println("Processing " + sFileName);
				XMLParser parser = new XMLParser();
				try {
					beast.core.Runnable runable = parser.parseFile(sDir + "/" + sFileName);
					if (runable instanceof MCMC) {
						MCMC mcmc = (MCMC) runable;
						mcmc.setInputValue("preBurnin", 0);
						mcmc.setInputValue("chainLength", 1000);
						mcmc.run();
					}
				} catch (Exception e) {
					System.out.println("ExampleXmlParsing::Failed for " + sFileName
							+ ": " + e.getMessage());
					sFailedFiles.add(sFileName);
				}
				System.out.println("Done " + sFileName);
			}
			if (sFailedFiles.size() > 0) {
				System.out.println("\ntest_ThatXmlExamplesRun::Failed for : " + sFailedFiles.toString());
			} else {
				System.out.println("SUCCESS!!!");
			}
			assertTrue(sFailedFiles.toString(), sFailedFiles.size() == 0);
		} catch (Exception e) {
			System.out.println("exception thrown ");
			System.out.println(e.getMessage());;
		}
	} // test_ThatXmlExamplesRun


    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main("test.beast.integration.ExampleXmlParsingTest");
      }

} // ExampleXmlParsingTest
