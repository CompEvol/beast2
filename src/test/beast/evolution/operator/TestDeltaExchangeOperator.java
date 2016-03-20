/**
 * 
 */
package test.beast.evolution.operator;

import org.junit.Test;

import beast.core.parameter.RealParameter;
import beast.evolution.operators.DeltaExchangeOperator;

/**
 * @author gereon
 *
 */
public class TestDeltaExchangeOperator extends TestOperator {
	@Override
	public void setUp() {
		register(new DeltaExchangeOperator(),
				"parameter", new RealParameter(new Double[] {1., 1., 1., 1.}));
	}
	
	@Test
	public void testKeepsSum() {
		m_operator.proposal();
		double i = 0;
		for (Double p : ((RealParameter) m_operands.get("parameter")).getValues()) {
			i += p;
		}
		assertEquals("The DeltaExchangeOperator should not change the sum of a parameter", i, 4, 0.00001);
	}
}
