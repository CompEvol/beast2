/**
 * 
 */
package test.beast.evolution.operator;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;

import beast.core.Operator;
import beast.core.State;
import beast.core.StateNode;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.DeltaExchangeOperator;
import junit.framework.TestCase;

/**
 * @author gereon
 *
 */
public abstract class TestOperator extends TestCase {
	protected HashMap<String, StateNode> m_operands;
	protected Operator m_operator;

	public void register(Operator operator, final Object... operands) {
		m_operands = new HashMap<String, StateNode>();
		if (operands.length % 2 == 1) {
			throw new RuntimeException("Expected even number of arguments, name-value pairs");
		}
		for (int i = 0; i < operands.length; i += 2) {
			if (operands[i] instanceof String) {
				final String name = (String) operands[i];
				if (operands[i + 1] instanceof StateNode) {
					final StateNode node = (StateNode) operands[i + 1];
					m_operands.put(name, node);
				} else {
					throw new IllegalArgumentException("Expected a StateNode in " + (i + 1) + "th argument ");
				}
			} else {
				throw new IllegalArgumentException("Expected a String in " + i + "th argument ");
			}
		}
		State state = new State();
		state.initByName("stateNode", new ArrayList<StateNode>(m_operands.values()));
		state.initialise();
		m_operator = operator;
		Object[] operandsAndWeight = new Object[operands.length + 2];
		for (int i = 0; i < operands.length; ++i) {
			operandsAndWeight[i] = operands[i];
		}
		operandsAndWeight[operands.length] = "weight";
		operandsAndWeight[operands.length + 1] = "1";
		m_operator.initByName(operands);
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
