package test.beast.evolution.operator;


import test.beast.evolution.operator.TestOperator;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.Test;
import org.xml.sax.SAXException;

import beast.base.core.Function;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.inference.distribution.Dirichlet;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.operator.kernel.BactrianDeltaExchangeOperator;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;

import java.io.IOException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;


public class BactrianDeltaExchangeOperatorTest extends BactrianRandomWalkOperatorTest {


	@Test
	@Override
	public void testNormalDistribution() throws Exception {
		// suppress test from super class
	}
	
	@Test
	public void testLogNormalDistribution() throws Exception {

		// Set up operator:
		RealParameter param1 = new RealParameter("0.25 0.25 0.25 0.25");
		param1.setID("param1");
		param1.setBounds(0.0,  1.0);
		Operator bactrianOperator = new BactrianDeltaExchangeOperator();
		// gives ESS: 18886.199729294272
		//Operator bactrianOperator = new DeltaExchangeOperator();
		// gives ESS:  1008.3640684048069
		bactrianOperator.initByName("weight", "1", "parameter", param1, "delta", 0.24, "autoOptimize", true);
		
		doMCMCrun(param1, bactrianOperator);
	}
	
	void doMCMCrun(Function param, Operator operators) throws IOException, SAXException, ParserConfigurationException {
		// Fix seed: will hopefully ensure success of test unless something
		// goes terribly wrong.
		Randomizer.setSeed(127);


		ParametricDistribution p = new Dirichlet();
		p.initByName("alpha", "4.0 1.0 1.0 1.0");
		Prior prior = new Prior();
		prior.initByName("x", param, "distr", p);

		
		// Set up state:
		State state = new State();
		state.initByName("stateNode", param);

		// Set up logger:
		TraceReport traceReport = new TraceReport();
		traceReport.initByName(
				"logEvery", "10",
				"burnin", "2000",
				"log", param,
				"silent", true
				);

		// Set up MCMC:
		MCMC mcmc = new MCMC();
		mcmc.initByName(
				"chainLength", "1000000",
				"state", state,
				"distribution", prior,
				"operator", operators,
				"logger", traceReport
				);

		// Run MCMC:
		mcmc.run();

		List<double[]> values = traceReport.getAnalysis2();
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i)[0];
		}
		double m = StatUtils.mean(v);
		// double median = StatUtils.percentile(v, 50);
		double s = StatUtils.variance(v, 50);
		assertEquals(4.0/7.0, m, 5e-3);
		
		double var = 4.0/7.0 * (1.0 - 4.0/7.0) / 8.0;
		assertEquals(var, s, 5e-3);
	}

	
	/** following tests from DeltaExchangeOperatorTest **/
	@Test
	public void testKeepsSum() {
		BactrianDeltaExchangeOperator operator = new BactrianDeltaExchangeOperator(); 
		RealParameter parameter = new RealParameter(new Double[] {1., 1., 1., 1.});
		TestOperator.register(operator,
				"parameter", parameter);
		for (int i=0; i<100; ++i) {
			operator.proposal();
		}
		double i = 0;
		for (Double p : parameter.getValues()) {
			i += p;
		}
		assertEquals("The BactrianDeltaExchangeOperator should not change the sum of a parameter", i, 4, 0.00001);
	}
	
	@Test
	public void testKeepsWeightedSum() {
		RealParameter parameter = new RealParameter(new Double[] {1., 1., 1., 1.});
		TestOperator.register(new BactrianDeltaExchangeOperator(),
				"weightvector", new IntegerParameter(new Integer[] {0, 1, 2, 1}),
				"parameter", parameter);
		Double[] p = parameter.getValues();
		assertEquals("The BactrianDeltaExchangeOperator should not change the sum of a parameter",
				0*p[1]+1*p[1]+2*p[2]+1*p[3], 4, 0.00001);
	}
	
	@Test
	public void testCanOperate() {
		// Test whether a validly initialised operator may make proposals
		State state = new State();
		RealParameter parameter = new RealParameter(new Double[] { 1., 1., 1., 1. });
		state.initByName("stateNode", parameter);
		state.initialise();
		BactrianDeltaExchangeOperator d = new BactrianDeltaExchangeOperator();
		// An invalid operator should either fail in initByName or make valid
		// proposals
		try {
			d.initByName("parameter", parameter, "weight", 1.0);
		} catch (RuntimeException e) {
			return;
		}
		d.proposal();
	}

}
