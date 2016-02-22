package beast.core.parameter;


import java.io.PrintStream;

import beast.core.Description;
import beast.core.Input;
import beast.core.util.Log;


/**
 * @author Alexei Drummond
 */

@Description("An integer-valued parameter represents a value (or array of values if the dimension is larger than one) " +
        "in the state space that can be changed by operators.")
public class IntegerParameter extends Parameter.Base<java.lang.Integer> {
    final public Input<Integer> lowerValueInput = new Input<>("lower", "lower value for this parameter (default -infinity)");
    final public Input<Integer> upperValueInput = new Input<>("upper", "upper value for this parameter  (default +infinity)");

    public IntegerParameter() {
    }

    public IntegerParameter(Integer[] values) {
        super(values);
    }

    /**
     * Constructor used by Input.setValue(String) *
     */
    public IntegerParameter(String value) throws Exception {
        init(0, 0, value, 1);
    }

    @Override
    public void initAndValidate() {
        if (lowerValueInput.get() != null) {
            m_fLower = lowerValueInput.get();
        } else {
            m_fLower = Integer.MIN_VALUE + 1;
        }
        if (upperValueInput.get() != null) {
            m_fUpper = upperValueInput.get();
        } else {
            m_fUpper = Integer.MAX_VALUE - 1;
        }
        super.initAndValidate();
    }

    @Override
	Integer getMax() {
        return Integer.MAX_VALUE - 1;
    }

    @Override
	Integer getMin() {
        return Integer.MIN_VALUE + 1;
    }

    /** Valuable implementation follows **/
    /**
     * we need this here, because the base implementation (public T getValue()) fails
     * for some reason
     */
    @Override
    public Integer getValue() {
        return values[0];
    }

    @Override
    public double getArrayValue() {
        return values[0];
    }

    public int getNativeValue(final int value) {
        return values[value];
    }

    @Override
    public double getArrayValue(int value) {
        return values[value];
    }

    /**
     * Loggable implementation follows *
     */
    @Override
    public void log(int sampleNr, PrintStream out) {
        IntegerParameter var = (IntegerParameter) getCurrent();
        int valueCount = var.getDimension();
        for (int i = 0; i < valueCount; i++) {
            out.print(var.getValue(i) + "\t");
        }
    }

    /**
     * StateNode methods *
     */
    @Override
    public int scale(double scale) {
        // nothing to do
        Log.warning.println("Attempt to scale Integer parameter " + getID() + "  has no effect");
        return 0;
    }

    @Override
    void fromXML(int dimension, String lower, String upper, String[] valueStrings) {
        setLower(Integer.parseInt(lower));
        setUpper(Integer.parseInt(upper));
        values = new Integer[dimension];
        for (int i = 0; i < values.length; i++) {
            values[i] = Integer.parseInt(valueStrings[i]);
        }
    }
}
