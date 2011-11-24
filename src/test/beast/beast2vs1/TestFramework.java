package test.beast.beast2vs1;

import beast.core.Logger;
import beast.util.Randomizer;
import beast.util.XMLParser;
import junit.framework.TestCase;

import java.util.List;

import test.beast.beast2vs1.trace.Expectation;
import test.beast.beast2vs1.trace.LogAnalyser;

public abstract class TestFramework extends TestCase {
    protected static long SEED = 128;
    private String[] xmls;

    protected abstract List<Expectation> giveExpectations(int index_XML) throws Exception;

    protected void setUp(String[] xmls) throws Exception {
        this.xmls = new String[xmls.length];
        for (int i = 0; i < xmls.length; i++) {
            this.xmls[i] = xmls[i];
        }
    }
//    protected abstract void analyse() throws Exception;
    
    protected void analyse(int index_XML) throws Exception {
//        for (int i = 0; i < xmls.length; i++) {
//            if (giveExpectations(i).size() > 0) {
                Randomizer.setSeed(SEED);
                Logger.FILE_MODE = Logger.FILE_OVERWRITE;
                String sDir = System.getProperty("user.dir");

                String sFileName = sDir + "/examples/beast2vs1/" + xmls[index_XML];

                System.out.println("Processing " + sFileName);
                XMLParser parser = new XMLParser();
                beast.core.Runnable runable = parser.parseFile(sFileName);
                runable.setStateFile("tmp.state", false);
//		   runable.setInputValue("preBurnin", 0);
//		   runable.setInputValue("chainLength", 1000);
                runable.run();

                String logFile = sDir + "/test." + SEED + ".log";
                System.out.println("\nAnalysing log " + logFile);
                LogAnalyser logAnalyser = new LogAnalyser(logFile, giveExpectations(index_XML)); // burnIn = 0.1 * maxState

                for (Expectation expectation : logAnalyser.m_pExpectations.get()) {
                    TestCase.assertTrue(xmls[index_XML] + ": Expected " + expectation.traceName.get() + " delta mean: "
                            + expectation.expValue.get() + " - " + expectation.getTraceStatistics().getMean()
                            + " <= delta stdErr: 2*(" + expectation.getStdError() + " + "
                            + expectation.getTraceStatistics().getStdErrorOfMean() + ")", expectation.isPassed());

                    TestCase.assertTrue(xmls[index_XML] + ":  has very low effective sample sizes (ESS) "
                            + expectation.getTraceStatistics().getESS(), expectation.isValid());
                }

                System.out.println("\nSucceed " + sFileName);
                System.out.println("\n***************************************\n");
//            }
//        }
    }

    protected void addExpIntoList(List<Expectation> expList, String traceName, Double expValue, Double stdError) throws Exception {
        Expectation exp = new Expectation(traceName, expValue, stdError);
        expList.add(exp);
    }

}