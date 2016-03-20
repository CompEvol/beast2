/**
 * 
 */
package test.beast.evolution.operator;

import org.junit.Test;

import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.DeltaExchangeOperator;

/**
 * @author gereon
 *
 */
public class TestDeltaExchangeOperatorWithZeroWeights extends TestOperator {
	@Override
	public void setUp() {
		register(new DeltaExchangeOperator(),
				"weightvector", new IntegerParameter(new Integer[] {0, 1, 2, 1}),
				"parameter", new RealParameter(new Double[] {1., 1., 1., 1.}));
	}
	
	@Test
	public void testKeepsWeightedSum() {
		Double[] p = ((RealParameter) m_operands.get("parameter")).getValues();
		assertEquals("The DeltaExchangeOperator should not change the sum of a parameter",
				0*p[1]+1*p[1]+2*p[2]+1*p[3], 4, 0.00001);
	}
}
