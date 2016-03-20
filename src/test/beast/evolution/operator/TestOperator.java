/**
 * 
 */
package test.beast.evolution.operator;

import java.util.ArrayList;
import java.util.HashMap;


import beast.core.Operator;
import beast.core.State;
import beast.core.StateNode;
import junit.framework.TestCase;

/**
 * @author gereon
 *
 */
public abstract class TestOperator extends TestCase {

	static public void register(Operator operator, final Object... operands) {
		HashMap<String, StateNode> operandsMap;
		operandsMap = new HashMap<String, StateNode>();
		if (operands.length % 2 == 1) {
			throw new RuntimeException("Expected even number of arguments, name-value pairs");
		}
		for (int i = 0; i < operands.length; i += 2) {
			if (operands[i] instanceof String) {
				final String name = (String) operands[i];
				if (operands[i + 1] instanceof StateNode) {
					final StateNode node = (StateNode) operands[i + 1];
					operandsMap.put(name, node);
				} else {
					throw new IllegalArgumentException("Expected a StateNode in " + (i + 1) + "th argument ");
				}
			} else {
				throw new IllegalArgumentException("Expected a String in " + i + "th argument ");
			}
		}
		State state = new State();
		state.initByName("stateNode", new ArrayList<StateNode>(operandsMap.values()));
		state.initialise();
		Object[] operandsAndWeight = new Object[operands.length + 2];
		for (int i = 0; i < operands.length; ++i) {
			operandsAndWeight[i] = operands[i];
		}
		operandsAndWeight[operands.length] = "weight";
		operandsAndWeight[operands.length + 1] = "1";
		operator.initByName(operandsAndWeight);
		operator.validateInputs();
	}
}
