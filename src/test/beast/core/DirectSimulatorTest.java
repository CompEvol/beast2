package test.beast.core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import beast.core.DirectSimulator;
import beast.core.Logger;
import beast.core.Logger.LogFileMode;
import beast.core.parameter.RealParameter;
import beast.math.distributions.Normal;
import beast.math.distributions.Prior;
import beast.util.LogAnalyser;
import junit.framework.TestCase;

public class DirectSimulatorTest extends TestCase {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	
	@Test
	public void testBoundedPrior() throws Exception {
		RealParameter p = new RealParameter("0.0");
		p.setID("p");
		p.setUpper(0.0);
		p.setLower(Double.NEGATIVE_INFINITY);
		
		Normal normal = new Normal();
		normal.initByName("mean", "0.0", "sigma", "1.0");
		Prior prior = new Prior();
		prior.initByName("x", p, "distr", normal);
		
		Logger logger = new Logger();
		Logger.FILE_MODE = LogFileMode.overwrite;
		String tracefile = "/tmp/trace.log";
		logger.initByName("log", p, "fileName", tracefile);
				
		
		DirectSimulator simulator = new DirectSimulator();
		simulator.initByName("distribution", prior, "logger", logger, "nSamples", 1000);
		
		simulator.run();

		LogAnalyser trace = new LogAnalyser(tracefile);
		double upper = trace.get95HPDup("p");
		assertEquals(true, upper <= 0);
		
		// clean up
		// new File(tracefile).delete();
		System.err.println("done");
		
	}

	@Test
	public void testMultiDimensionalBoundedPrior() throws Exception {
		RealParameter x = new RealParameter();
		x.initByName("dimension", 3, "value", "0.0", "upper", 0.0, "lower", Double.NEGATIVE_INFINITY);
		x.setID("x");
		
		Normal normal = new Normal();
		normal.initByName("mean", "0.0", "sigma", "1.0");
		Prior prior = new Prior();
		prior.initByName("x", x, "distr", normal);
		
		Logger logger = new Logger();
		Logger.FILE_MODE = LogFileMode.overwrite;
		String tracefile = "/tmp/trace.log";
		logger.initByName("log", x, "fileName", tracefile);
				
		
		DirectSimulator simulator = new DirectSimulator();
		simulator.initByName("distribution", prior, "logger", logger, "nSamples", 1000);
		
		simulator.run();

		LogAnalyser trace = new LogAnalyser(tracefile);
		double upper = trace.get95HPDup("x.1");
		assertEquals(true, upper <= 0);
		upper = trace.get95HPDup("x.2");
		assertEquals(true, upper <= 0);
		upper = trace.get95HPDup("x.3");
		assertEquals(true, upper <= 0);
		
		// clean up
		// new File(tracefile).delete();
		System.err.println("done");
		
	}
}
