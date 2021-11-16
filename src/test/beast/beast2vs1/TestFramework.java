package test.beast.beast2vs1;

import beagle.BeagleFlag;
import beast.core.Logger;
import beast.util.Randomizer;
import beast.util.XMLParser;
import junit.framework.Assert;
import junit.framework.TestCase;
import test.beast.beast2vs1.trace.Expectation;
import test.beast.beast2vs1.trace.LogAnalyser;
import test.beast.beast2vs1.trace.TraceStatistics;

import java.io.File;
import java.util.List;

public abstract class TestFramework extends TestCase {
    protected long SEED = 128;
    private String[] xmls;

    protected abstract List<Expectation> giveExpectations(int index_XML) throws Exception;

    public String dirName;
    public String logDir;
    public String testFile = "/test.";
    public boolean useSeed = true;
    public boolean checkESS = true;
    
    public TestFramework() {
    	dirName = System.getProperty("user.dir") + "/examples/beast2vs1/";
    	logDir = System.getProperty("user.dir");
    }
    
    protected void setUp(String[] xmls) throws InterruptedException { // throws Exception {
        this.xmls = new String[xmls.length];
        for (int i = 0; i < xmls.length; i++) {
            this.xmls[i] = xmls[i];
        }
        Thread.sleep(1000); // ensure log file is processed between test suits
    }
//    protected abstract void analyse() throws Exception;

    public void analyse(int index_XML) throws Exception {
//        for (int i = 0; i < xmls.length; i++) {
//            if (giveExpectations(i).size() > 0) {
        // quick fix to ensure log file names are different
        SEED = SEED + index_XML;
        Randomizer.setSeed(SEED);
        Logger.FILE_MODE = Logger.LogFileMode.overwrite;
        
        long beagleFlags = BeagleFlag.PROCESSOR_CPU.getMask() | BeagleFlag.VECTOR_SSE.getMask();
        System.setProperty("beagle.preferred.flags", Long.toString(beagleFlags));

        String fileName = dirName + xmls[index_XML];

        System.out.println("Processing " + fileName);
        XMLParser parser = new XMLParser();
        beast.core.Runnable runable = parser.parseFile(new File(fileName));
        runable.setStateFile("tmp.state", false);
//		   runable.setInputValue("preBurnin", 0);
//		   runable.setInputValue("chainLength", 1000);
        runable.run();

        String logFile = logDir + testFile + (useSeed ? SEED : "") + ".log";
        System.out.println("\nAnalysing log " + logFile);
        LogAnalyser logAnalyser = new LogAnalyser(logFile, giveExpectations(index_XML)); // burnIn = 0.1 * maxState

        for (Expectation expectation : logAnalyser.m_pExpectations.get()) {
            TraceStatistics stats = expectation.getTraceStatistics();
            if (stats == null) {
                System.err.println("Null trace at " + expectation.traceName.get()
                        + "\nPlease check log for " + xmls[index_XML] );
            } else {
                double m = stats.getMean();
                double stderr = stats.getStdErrorOfMean();

                Assert.assertTrue(xmls[index_XML] + ": Expected " + expectation.traceName.get() + " delta mean: "
                        + expectation.expValue.get() + " - " + m + " <= delta stdErr: 2*(" + expectation.getStdError()
                        + " + " + stderr + ")", expectation.isPassed());

                if (checkESS)
                    Assert.assertTrue(xmls[index_XML] + ":  has very low effective sample sizes (ESS) "
                            + stats.getESS(), expectation.isValid());
            }
        }

        System.out.println("\nSucceed " + fileName);
        System.out.println("\n***************************************\n");
//            }
//        }
    }

    protected void addExpIntoList(List<Expectation> expList, String traceName, Double expValue, Double stdError) throws Exception {
        Expectation exp = new Expectation(traceName, expValue, stdError);
        expList.add(exp);
    }

}