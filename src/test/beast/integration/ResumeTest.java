package test.beast.integration;

import java.io.File;

import org.junit.Test;

import beast.core.Logger;
import beast.core.MCMC;
import beast.util.Randomizer;
import beast.util.XMLParser;
import junit.framework.TestCase;

/**
 * check that a chain can be resumed after termination *
 */
public class ResumeTest extends TestCase {

    final static String XML_FILE = "testHKY.xml";
	{
		ExampleXmlParsingTest.setUpTestDir();
	}

    
    @Test
    public void test_ThatXmlExampleResumes() throws Exception {
        Randomizer.setSeed(127);
        Logger.FILE_MODE = Logger.LogFileMode.overwrite;
        String dir = System.getProperty("user.dir") + "/examples";
        String fileName = dir + "/" + XML_FILE;

        System.out.println("Processing " + fileName);
        XMLParser parser = new XMLParser();
        beast.core.Runnable runable = parser.parseFile(new File(fileName));
        runable.setStateFile("tmp.state", false);
        if (runable instanceof MCMC) {
            MCMC mcmc = (MCMC) runable;
            mcmc.setInputValue("preBurnin", 0);
            mcmc.setInputValue("chainLength", 1000l);
            mcmc.run();
        }
        System.out.println("Done " + fileName);

        System.out.println("Resuming " + fileName);
        Logger.FILE_MODE = Logger.LogFileMode.resume;
        parser = new XMLParser();
        runable = parser.parseFile(new File(fileName));
        runable.setStateFile("tmp.state", true);
        if (runable instanceof MCMC) {
            MCMC mcmc = (MCMC) runable;
            mcmc.setInputValue("preBurnin", 0);
            mcmc.setInputValue("chainLength", 1000l);
            mcmc.run();
        }
        System.out.println("Done " + fileName);
    } // test_ThatXmlExampleResumes

} // class ResumeTest
