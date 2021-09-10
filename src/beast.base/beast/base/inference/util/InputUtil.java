package beast.base.inference.util;

import java.util.List;

import beast.base.core.Input;
import beast.base.inference.CalculationNode;
import beast.base.inference.Operator;
import beast.base.inference.StateNode;

public class InputUtil {

	/**
     * As Input.get() but with this difference that the State can manage
     * whether to make a copy and register the operator.
     * <p/>
     * Only Operators should call this method.
     * Also Operators should never call Input.get(), always Input.get(operator).
     *
     * @param operator
     * @return
     */
    static public StateNode get(Input<?> input, final Operator operator) {
        return ((StateNode) input.get()).getCurrentEditable(operator);
    }

    /**
     * Return the dirtiness state for this input.
     * For a StateNode or list of StateNodes, report whether for any something is dirty,
     * for a CalcationNode or list of CalculationNodes, report whether any is dirty.
     * Otherwise, return false.
     * *
     */
    static public boolean isDirty(Input<?> input) {
        final Object value = input.get();

        if (value == null) {
            return false;
        }

        if (value instanceof StateNode) {
            return ((StateNode) value).somethingIsDirty();
        }

        if (value instanceof CalculationNode) {
            return ((CalculationNode) value).isDirtyCalculation();
        }

        if (value instanceof List<?>) {
            for (final Object obj : (List<?>) value) {
                if (obj instanceof CalculationNode && ((CalculationNode) obj).isDirtyCalculation()) {
                    return true;
                } else if (obj instanceof StateNode && ((StateNode) obj).somethingIsDirty()) {
                    return true;
                }
            }
        }

        return false;
    }
 
}
