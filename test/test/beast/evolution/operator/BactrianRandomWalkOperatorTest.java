package test.beast.evolution.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.jupiter.api.Test;

import beast.base.core.BEASTObject;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.inference.State;
import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.distribution.Normal;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.operator.RealRandomWalkOperator;
import beast.base.inference.operator.kernel.BactrianRandomWalkOperator;
import beast.base.inference.operator.kernel.KernelDistribution;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.util.ESS;
import beast.base.util.Randomizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;


public class BactrianRandomWalkOperatorTest  {

	@Test
	public void testNormalDistribution() throws Exception {

		// Fix seed: will hopefully ensure success of test unless something
		// goes terribly wrong.
		Randomizer.setSeed(127);

		// Assemble model:
		RealParameter param = new RealParameter("0.0");
		param.setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		ParametricDistribution p = new Normal();
		p.initByName("mean", "1.0", "sigma", "1.0");
		Prior prior = new Prior();
		prior.initByName("x", param, "distr", p);

		
		// Set up state:
		State state = new State();
		state.initByName("stateNode", param);

// ESS 
// Mirror       196397.20218992446
// Bactrian		198525.37485263616
// Gaussian     177970.32054462744	
// non Gaussian 185569.35975056374		
		
		// Set up operator:
		BactrianRandomWalkOperator bactrianOperator = new BactrianRandomWalkOperator();
		KernelDistribution.Mirror kdist = new KernelDistribution.Mirror();
//		KernelDistribution.Bactrian kdist = new KernelDistribution.Bactrian();
		kdist.initByName("initial",500, "burnin", 500);
		bactrianOperator.initByName("weight", "1", "parameter", param, "kernelDistribution", kdist, "scaleFactor", 1.0, "optimise", true);

//		RealRandomWalkOperator bactrianOperator = new RealRandomWalkOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param, "windowSize", 1.0, "useGaussian", false);

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

		List<Double> values = traceReport.getAnalysis();
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i);
		}
		double m = StatUtils.mean(v);
		double s = StatUtils.variance(v);
		assertEquals(1.0, m, 5e-3);
		assertEquals(1.0, s, 5e-3);


	}

	
	
	@Test
	public void testLogNormalDistribution() throws Exception {

		// Fix seed: will hopefully ensure success of test unless something
		// goes terribly wrong.
		Randomizer.setSeed(127);

		// Assemble model:
		RealParameter param = new RealParameter("10.0");
		param.setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		ParametricDistribution p = new LogNormalDistributionModel();
		p.initByName("M", "1.0", "S", "1.0", "meanInRealSpace", true);
		Prior prior = new Prior();
		prior.initByName("x", param, "distr", p);

		
		// Set up state:
		State state = new State();
		state.initByName("stateNode", param);

// ESS 
// Mirror       101392.25428944119 
// Bactrian		 40484.42378045924 Mean: 0.9936652025880692 variance: 1.6192894315845616
// Gaussian      58971.221113274	
// Uniform       79520.29358602439		
		
		// Set up operator:
		BactrianRandomWalkOperator bactrianOperator = new BactrianRandomWalkOperator();
// 		KernelDistribution.MirrorDistribution kdist = new KernelDistribution.MirrorDistribution();
		KernelDistribution.Bactrian kdist = new KernelDistribution.Bactrian();
		kdist.initAndValidate();
		bactrianOperator.initByName("weight", "1", "parameter", param, "kernelDistribution", kdist, "scaleFactor", 1.0, "optimise", true);

//		RealRandomWalkOperator bactrianOperator = new RealRandomWalkOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param, "windowSize", 1.0, "useGaussian", true);

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

		List<Double> values = traceReport.getAnalysis();
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i);
		}
		double m = StatUtils.mean(v);
		double median = StatUtils.percentile(v, 50);
		double s = StatUtils.variance(v, 50);
		assertEquals(1.0, m, 9e-3);
		assertEquals(Math.exp(-0.5), median, 9e-3);
		assertEquals(Math.exp(1)-1, s, 1e-1);
		assertEquals(0.0854, StatUtils.percentile(v, 2.5), 5e-3);
		assertEquals(0.117, StatUtils.percentile(v, 5), 5e-3);
		assertEquals(3.14, StatUtils.percentile(v, 95), 1e-1);
		assertEquals(4.31, StatUtils.percentile(v, 97.5), 1e-1);

	}
	/**
	 * Modified logger which analyses a sequence of tree states generated
	 * by an MCMC run.
	 */
	public class TraceReport extends Logger {

		public Input<Integer> burninInput = new Input<Integer>("burnin",
				"Number of samples to skip (burn in)", Input.Validate.REQUIRED);

		public Input<Boolean> silentInput = new Input<Boolean>("silent",
				"Don't display final report.", false);

		Function paramToTrack;

		int m_nEvery = 1;
		int burnin;
		boolean silent = false;

		List<Double> values;
		List<double[]> values2;

		@Override
		public void initAndValidate() {

			List<BEASTObject> loggers = loggersInput.get();
			final int nLoggers = loggers.size();
			if (nLoggers == 0) {
				throw new IllegalArgumentException("Logger with nothing to log specified");
			}

			if (everyInput.get() != null)
				m_nEvery = everyInput.get();

			burnin = burninInput.get();

			if (silentInput.get() != null)
				silent = silentInput.get();

			paramToTrack = (Function)loggers.get(0);
			values = new ArrayList<>();
			values2 = new ArrayList<>();
		}

		@Override
		public void init() { }

		@Override
		public void log(long nSample) {

			if ((nSample % m_nEvery > 0) || nSample<burnin)
				return;

			values.add(paramToTrack.getArrayValue());
			values2.add(paramToTrack.getDoubleValues());
		}

		@Override
		public void close() {

			if (!silent) {
				System.out.println("\n----- Tree trace analysis -----------------------");
				double[] v = new double[values.size()];
				for (int i = 0; i < v.length; i++) {
					v[i] = values.get(i);
				}
				double m = StatUtils.mean(v);
				double s = StatUtils.variance(v);
				double ess = ESS.calcESS(values);
				System.out.println("Mean: " + m + " variance: " + s + " ESS: " + ess);
				System.out.println("-------------------------------------------------");
				System.out.println();
				
				try {
					PrintStream log = new PrintStream(new File("/tmp/bactrian.log"));
					log.print("Sample\t");
					int n = values2.get(0).length;
					for (int j = 0; j < n; j++) {
						log.print("param" + (j+1) + "\t");
					}
					log.println();
					for (int i = 0; i < v.length; i++) {
						log.print(i + "\t");
						for (int j = 0; j < n; j++) {
							log.print(values2.get(i)[j] + "\t");
						}
						log.println();
					}
					log.close();
					System.out.println("trace log written to /tmp/bactrian.log");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		/**
		 * Obtain completed analysis.
		 *
		 * @return trace.
		 */
		public List<Double> getAnalysis() {
			return values;
		}
		
		public List<double[]> getAnalysis2() {
			return values2;
		}

	}
}
