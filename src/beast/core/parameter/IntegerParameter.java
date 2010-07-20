package beast.core.parameter;


import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.core.StateNode;

import java.io.PrintStream;


/**
 * @author Alexei Drummond
 */

@Description("An integer-valued parameter represents a value (or array of values if the dimension is larger than one) " +
        "in the state space that can be changed by operators.")
public class IntegerParameter extends Parameter<java.lang.Integer> {
    public Input<Integer> m_pValues = new Input<Integer>("value", "start value for this parameter");
    public Input<Integer> lowerValueInput = new Input<Integer>("lower", "lower value for this parameter");
    public Input<Integer> upperValueInput = new Input<Integer>("upper", "upper value for this parameter");

    public IntegerParameter() {
    }

    /**
     * Constructor for testing.
     *
     * @param value
     * @param lower
     * @param upper
     * @param dimension
     * @throws Exception
     */
    public IntegerParameter(Integer value, Integer lower, Integer upper, Integer dimension) throws Exception {

        m_pValues.setValue(value, this);
        lowerValueInput.setValue(lower, this);
        upperValueInput.setValue(upper, this);
        m_nDimension.setValue(dimension, this);
        initAndValidate();
    }


    public Integer getValue() {
        return values[0];
    }

    @Override
    public void initAndValidate() throws Exception {
        m_fLower = lowerValueInput.get();
        m_fUpper = upperValueInput.get();
        values = new Integer[m_nDimension.get()];
        for (int i = 0; i < values.length; i++) {
            values[i] = m_pValues.get();
        }
        super.initAndValidate();
    }

    /**
     * deep copy *
     */
    @Override
    public Parameter<?> copy() {
        Parameter<Integer> copy = new IntegerParameter();
        copy.setID(getID());
        copy.index = index;
        copy.values = new Integer[values.length];
        System.arraycopy(values, 0, copy.values, 0, values.length);
        copy.m_fLower = m_fLower;
        copy.m_fUpper = m_fUpper;
        copy.m_bIsDirty = new boolean[values.length];
        return copy;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void assignTo(StateNode other) {
        Parameter<Integer> copy = (Parameter<Integer>) other;
        copy.setID(getID());
        copy.index = index;
        copy.values = new Integer[values.length];
        System.arraycopy(values, 0, copy.values, 0, values.length);
        copy.m_fLower = m_fLower;
        copy.m_fUpper = m_fUpper;
        copy.m_bIsDirty = new boolean[values.length];
    }

    public void log(int nSample, State state, PrintStream out) {
        IntegerParameter var = (IntegerParameter) getCurrent();//state.getStateNode(m_sID);
        int nValues = var.getDimension();
        for (int iValue = 0; iValue < nValues; iValue++) {
            out.print(var.getValue(iValue) + "\t");
        }
    }
}
