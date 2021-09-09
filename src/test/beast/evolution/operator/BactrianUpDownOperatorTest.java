package test.beast.evolution.operator;


import org.apache.commons.math3.stat.StatUtils;
import org.junit.Test;
import org.xml.sax.SAXException;

import beast.base.Function;
import beast.evolution.operator.kernel.BactrianScaleOperator;
import beast.inference.MCMC;
import beast.inference.Operator;
import beast.inference.State;
import beast.inference.StateNode;
import beast.inference.distribution.LogNormalDistributionModel;
import beast.inference.distribution.ParametricDistribution;
import beast.inference.distribution.Prior;
import beast.inference.operator.kernel.BactrianUpDownOperator;
import beast.inference.parameter.RealParameter;
import beast.inference.util.RPNcalculator;
import beast.util.Randomizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;


public class BactrianUpDownOperatorTest extends BactrianRandomWalkOperatorTest {


	@Test
	@Override
	public void testNormalDistribution() throws Exception {
		// suppress test from super class
	}
	
	@Test
	public void testLogNormalDistribution() throws Exception {

		// Set up operator:
		RealParameter param1 = new RealParameter("10.0");
		param1.setID("param1");
		param1.setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		RealParameter param2 = new RealParameter("1.0");
		param2.setID("param2");
		param2.setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		BactrianUpDownOperator bactrianOperator = new BactrianUpDownOperator();
		bactrianOperator.initByName("weight", "1", "up", param1, "down", param2, "scaleFactor", 0.025, "optimise", false);
		
		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", param1, "parameter", param2, "expression", "param1 param2 -");

		BactrianScaleOperator scaleOperator1 = new BactrianScaleOperator();
		scaleOperator1.initByName("weight", "0.5", "parameter", param1, "scaleFactor", 0.5, "optimise", false);
		BactrianScaleOperator scaleOperator2 = new BactrianScaleOperator();
		scaleOperator2.initByName("weight", "0.5", "parameter", param2, "scaleFactor", 0.5, "optimise", false);
		
		List<Operator> operators = new ArrayList<>();
		operators.add(bactrianOperator);
		operators.add(scaleOperator1);
		operators.add(scaleOperator2);
		
		doMCMCrun(calculator, operators);
	}
	


	

	
	void doMCMCrun(Function param, List<Operator> operators) throws IOException, SAXException, ParserConfigurationException {
		// Fix seed: will hopefully ensure success of test unless something
		// goes terribly wrong.
		Randomizer.setSeed(127);


		ParametricDistribution p = new LogNormalDistributionModel();
		p.initByName("M", "1.0", "S", "1.0", "meanInRealSpace", true);
		Prior prior = new Prior();
		prior.initByName("x", param, "distr", p);

		
		// Set up state:
		State state = new State();
		Set<StateNode> stateNodes = new HashSet<>();
		for (Operator op : operators) {
			stateNodes.addAll(op.listStateNodes());
		}
		List<StateNode> list = new ArrayList<>();
		list.addAll(stateNodes);
		state.initByName("stateNode", list);


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
				"chainLength", "5000000",
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
		double median = StatUtils.percentile(v, 50);
		double s = StatUtils.variance(v, 50);
		assertEquals(1.0, m, 5e-3);
		assertEquals(Math.exp(-0.5), median, 5e-3);
		assertEquals(Math.exp(1)-1, s, 1e-1);
		assertEquals(0.0854, StatUtils.percentile(v, 2.5), 5e-3);
		assertEquals(0.117, StatUtils.percentile(v, 5), 5e-2);
		assertEquals(3.14, StatUtils.percentile(v, 95), 1e-1);
		assertEquals(4.31, StatUtils.percentile(v, 97.5), 1e-1);
	}
}
