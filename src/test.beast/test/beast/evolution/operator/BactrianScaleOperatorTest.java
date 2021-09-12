package test.beast.evolution.operator;


import org.apache.commons.math3.stat.StatUtils;
import org.junit.Test;
import org.xml.sax.SAXException;

import beast.base.evolution.operator.kernel.BactrianScaleOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;


public class BactrianScaleOperatorTest extends BactrianRandomWalkOperatorTest {


	@Test
	@Override
	public void testNormalDistribution() throws Exception {
		// suppress test from super class
	}
	
	@Test
	public void testLogNormalDistribution() throws Exception {

		// Set up operator:
		RealParameter param = new RealParameter("10.0");
		BactrianScaleOperator bactrianOperator = new BactrianScaleOperator();
		bactrianOperator.initByName("weight", "1", "parameter", param);

//		ScaleOperator bactrianOperator = new ScaleOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param, "scaleFactor", 0.75);
		
		doMCMCrun(param, bactrianOperator);
	}
	


	
	@Test
	public void testTwoDimensionalDistribution() throws Exception {
		// Set up operator:
		RealParameter param = new RealParameter("10.0 10.0");
		BactrianScaleOperator bactrianOperator = new BactrianScaleOperator();
		bactrianOperator.initByName("weight", "1", "parameter", param);

//		ScaleOperator bactrianOperator = new ScaleOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param, "scaleFactor", 0.75);

		doMCMCrun(param, bactrianOperator);
		
	 /* Results:
		Effective sample size (ESS):	
		BactrianScaleOperator 109202.7	114451.9	
		ScaleOperator          24587.3	 22223.3
	 */
	}
	
	
	@Test
	public void testScaleAllIndependentlylDistribution() throws Exception {
		// Set up operator:
		RealParameter param = new RealParameter("10.0 10.0 10.0");

		// Set up operator:
		BactrianScaleOperator bactrianOperator = new BactrianScaleOperator();
		bactrianOperator.initByName("weight", "1", "parameter", param, "scaleFactor", 0.75, "scaleAllIndependently", true);

//		ScaleOperator bactrianOperator = new ScaleOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param, "scaleFactor", 0.75, "scaleAllIndependently", true);

		doMCMCrun(param, bactrianOperator);

		
	 /* Results:
		Effective sample size (ESS):	
		BactrianScaleOperator  143614.1	144363.9	142207.5	 	
		ScaleOperator           30736.7	 29254.7	 30813.9
	 */
	}
	
	@Test
	public void testScaleAllDistribution() throws Exception {

		// Set up operator:
		RealParameter param = new RealParameter("10.0 10.0 10.0");

		// Set up operator:
		BactrianScaleOperator bactrianOperator = new BactrianScaleOperator();
		bactrianOperator.initByName("weight", "1", "parameter", param, "scaleAll", true);

//		ScaleOperator bactrianOperator = new ScaleOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param, "scaleFactor", 0.75, "scaleAll", true);
		
		// requires second scale operator to assure dimensions are independent (which the first operator assumes)
		BactrianScaleOperator bactrianOperator2 = new BactrianScaleOperator();
		bactrianOperator2.initByName("weight", "1", "parameter", param);

		doMCMCrun(param, bactrianOperator, bactrianOperator2);
		
	 /* Results:
		Effective sample size (ESS):	
		BactrianScaleOperator  67413.3	65668.3	67986.6	 	
		ScaleOperator          58268	56123.4	51362.7
	 */
	}

	final static double EPSILON = 1e-10;

	@Test
	public void testTreeScaling() {
        String newick = "((0:1.0,1:1.0)4:1.0,(2:1.0,3:1.0)5:0.5)6:0.0;";

        TreeParser tree = new TreeParser(newick, false, false, false, 0);

        Node [] node = tree.getNodesAsArray();
        
        BactrianScaleOperator operator = new BactrianScaleOperator();
        operator.initByName("tree", tree, "weight", 1.0);
        operator.proposal();
        
        // leaf node
        node = tree.getNodesAsArray();
        assertEquals(0.0, node[0].getHeight(), EPSILON);
        assertEquals(0.0, node[1].getHeight(), EPSILON);
        // leaf node, not scaled
        assertEquals(0.5, node[2].getHeight(), EPSILON);
        assertEquals(0.5, node[3].getHeight(), EPSILON);
        
        // internal nodes, all scaled
        // first determine scale factor
        double scale = node[4].getHeight() / 1.0;
        assertEquals(1.0 * scale, node[4].getHeight(), EPSILON);
        assertEquals(1.5 * scale, node[5].getHeight(), EPSILON);
        assertEquals(2.0 * scale, node[6].getHeight(), EPSILON);
	}

	
	void doMCMCrun(RealParameter param, Operator bactrianOperator) throws IOException, SAXException, ParserConfigurationException {
		doMCMCrun(param, bactrianOperator, null);
	}

	void doMCMCrun(RealParameter param, Operator bactrianOperator, Operator optionOperator) throws IOException, SAXException, ParserConfigurationException {
		// Fix seed: will hopefully ensure success of test unless something
		// goes terribly wrong.
		Randomizer.setSeed(127);

		param.setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

		ParametricDistribution p = new LogNormalDistributionModel();
		p.initByName("M", "1.0", "S", "1.0", "meanInRealSpace", true);
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
		if (optionOperator == null) {
			mcmc.initByName(
					"chainLength", "1000000",
					"state", state,
					"distribution", prior,
					"operator", bactrianOperator,
					"logger", traceReport
					);
		} else {
			mcmc.initByName(
					"chainLength", "1000000",
					"state", state,
					"distribution", prior,
					"operator", bactrianOperator,
					"operator", optionOperator,
					"logger", traceReport
					);			
		}

		// Run MCMC:
		mcmc.run();

		List<double[]> values = traceReport.getAnalysis2();
		for (int k = 0; k < values.get(0).length; k++) {
			double[] v = new double[values.size()];
			for (int i = 0; i < v.length; i++) {
				v[i] = values.get(i)[k];
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
	}
}
