/**
 * 
 */
package test.beast.evolution.operator;

import org.junit.Test;

import beast.core.State;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.DeltaExchangeOperator;

/**
 * @author gereon
 *
 */
public class DeltaExchangeOperatorTest extends TestOperator {

	@Test
	public void testKeepsSum() {
		DeltaExchangeOperator operator = new DeltaExchangeOperator(); 
		RealParameter parameter = new RealParameter(new Double[] {1., 1., 1., 1.});
		register(operator,
				"parameter", parameter);
		for (int i=0; i<100; ++i) {
			operator.proposal();
		}
		double i = 0;
		for (Double p : parameter.getValues()) {
			i += p;
		}
		assertEquals("The DeltaExchangeOperator should not change the sum of a parameter", i, 4, 0.00001);
	}
	
	@Test
	public void testKeepsWeightedSum() {
		RealParameter parameter = new RealParameter(new Double[] {1., 1., 1., 1.});
		register(new DeltaExchangeOperator(),
				"weightvector", new IntegerParameter(new Integer[] {0, 1, 2, 1}),
				"parameter", parameter);
		Double[] p = parameter.getValues();
		assertEquals("The DeltaExchangeOperator should not change the sum of a parameter",
				0*p[1]+1*p[1]+2*p[2]+1*p[3], 4, 0.00001);
	}
	
	@Test
	public void testCanOperate() {
		// Test whether a validly initialised operator may make proposals
		State state = new State();
		RealParameter parameter = new RealParameter(new Double[] { 1., 1., 1., 1. });
		state.initByName("stateNode", parameter);
		state.initialise();
		DeltaExchangeOperator d = new DeltaExchangeOperator();
		// An invalid operator should either fail in initByName or make valid
		// proposals
		try {
			d.initByName("parameter", parameter);
		} catch (RuntimeException e) {
			return;
		}
		d.proposal();
	}

}
