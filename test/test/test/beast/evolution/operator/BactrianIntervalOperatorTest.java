package test.beast.evolution.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.jupiter.api.Test;

import beast.base.inference.MCMC;
import beast.base.inference.State;
import beast.base.inference.distribution.Normal;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.distribution.Uniform;
import beast.base.inference.operator.kernel.BactrianIntervalOperator;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;

public class BactrianIntervalOperatorTest extends BactrianRandomWalkOperatorTest {

	@Test
	@Override
	public void testNormalDistribution() throws Exception {
		// Set up prior:
		RealParameter param = new RealParameter("2.0");
		param.setBounds(1.0,3.0);
		ParametricDistribution p = new Normal();
		p.initByName("mean", "1.5", "sigma", "0.1");

		List<double[]> values = doMCMC(param, p);
		
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i)[0];
		}
		double m = StatUtils.mean(v);
		double s = StatUtils.variance(v);
		assertEquals(1.5, m, 5e-3);
		assertEquals(0.01, s, 5e-3);
		
		/*
		effective sample size (ESS)	
		Bactrian 151680.3	
		Uniform   62021.1
		*/
	}

	@Test
	public void testUniformDistribution() throws Exception {
		// Set up prior:
		RealParameter param = new RealParameter("2.0");
		param.setBounds(1.0,3.0);
		ParametricDistribution p = new Uniform();
		p.initByName("lower", "1.0", "upper", "3.0");

		List<double[]> values = doMCMC(param, p);
		
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i)[0];
		}
		double m = StatUtils.mean(v);
		double s = StatUtils.variance(v);
		// mean = (upper - lower)/ 2
		assertEquals(2.0, m, 5e-3);
		// variance = (upper - lower)^2 / 12
		assertEquals((3-1)*(3-1)/12.0, s, 5e-3);

	}
	
	@Test
	public void testUniformDistributionZeroLowerBound() throws Exception {
		// Set up prior:
		RealParameter param = new RealParameter("1.0");
		param.setBounds(0.0,2.0);
		ParametricDistribution p = new Uniform();
		p.initByName("lower", "0.0", "upper", "2.0");

		List<double[]> values = doMCMC(param, p);
		
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i)[0];
		}
		double m = StatUtils.mean(v);
		double s = StatUtils.variance(v);
		assertEquals(1.0, m, 5e-3);
		assertEquals(1.0/3.0, s, 5e-3);

	}
	
	@Test
	public void testUniformDistributionZeroUpperBound() throws Exception {
		// Set up prior:
		RealParameter param = new RealParameter("-1.0");
		param.setBounds(-2.0,0.0);
		ParametricDistribution p = new Uniform();
		p.initByName("lower", "-2.0", "upper", "0.0");

		List<double[]> values = doMCMC(param, p);
		
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i)[0];
		}
		double m = StatUtils.mean(v);
		double s = StatUtils.variance(v);
		assertEquals(-1.0, m, 5e-3);
		assertEquals(1.0/3.0, s, 5e-3);

		/**
		effective sample size (ESS)	
		Bactrian: 177235.9
	  	Uniform:  179821.0
		*/
	}
	
	private List<double[]> doMCMC(RealParameter param, ParametricDistribution p) throws Exception {
		// Fix seed: will hopefully ensure success of test unless something
		// goes terribly wrong.
		Randomizer.setSeed(127);

		Prior prior = new Prior();
		prior.initByName("x", param, "distr", p);

		
		// Set up state:
		State state = new State();
		state.initByName("stateNode", param);

		// Set up operator:
		BactrianIntervalOperator bactrianOperator = new BactrianIntervalOperator();
		bactrianOperator.initByName("weight", "1", "parameter", param);

//		UniformOperator bactrianOperator = new UniformOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param);

		// Set up logger:
		TraceReport traceReport = new TraceReport();
		traceReport.initByName(
				"logEvery", "10",
				"burnin", "2000",
				"log", param,
				"silent", false
				);

		// Set up MCMC:
		MCMC mcmc = new MCMC();
		mcmc.initByName(
				"chainLength", "2000000",
				"state", state,
				"distribution", prior,
				"operator", bactrianOperator,
				"logger", traceReport
				);

		// Run MCMC:
		mcmc.run();

		List<double[]> values = traceReport.getAnalysis2();
		return values;
	}

}
