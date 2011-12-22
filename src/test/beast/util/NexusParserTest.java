package test.beast.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import beast.util.NexusParser;

import junit.framework.TestCase;

public class NexusParserTest extends TestCase {

	@Test
	public void testThatNexusExamplesParse() {
		try {
			String sDir = System.getProperty("user.dir") + "/examples/nexus";
			System.out.println("Test Nexus Examples in " + sDir);
			File sExampleDir = new File(sDir);
			String[] sExampleFiles = sExampleDir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".nex");
				}
			});
			
			List<String> sFailedFiles = new ArrayList<String>();
			for (String sFileName : sExampleFiles) {
				System.out.println("Processing " + sFileName);
				NexusParser parser = new NexusParser();
				try {
					parser.parseFile(new File(sDir + "/" + sFileName));
				} catch (Exception e) {
					System.out.println("ExampleNexusParsing::Failed for " + sFileName
							+ ": " + e.getMessage());
					sFailedFiles.add(sFileName);
				}
				System.out.println("Done " + sFileName);
			}
			if (sFailedFiles.size() > 0) {
				System.out.println("\ntest_ThatNexusExamplesParse::Failed for : " + sFailedFiles.toString());
			} else {
				System.out.println("\ntest_ThatNexusExamplesParse::Success");
			}
			assertTrue(sFailedFiles.toString(), sFailedFiles.size() == 0);
		} catch (Exception e) {
			System.out.println("exception thrown ");
			System.out.println(e.getMessage());
		}
	}
	
	
	@Test
	public void testAssumptionsParse() {
		try {
			String sFile = System.getProperty("user.dir") + "/examples/nexus/Primates.nex";
			NexusParser parser = new NexusParser();
			parser.parseFile(new File(sFile));
			assertEquals(2, parser.m_filteredAlignments.size());
		} catch (Exception e) {
			System.out.println("exception thrown ");
			System.out.println(e.getMessage());
		}
	}}
