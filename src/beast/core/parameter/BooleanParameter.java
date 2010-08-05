package beast.core.parameter;

import beast.core.Description;
import beast.core.Input;
import beast.core.StateNode;

import java.io.PrintStream;

/**
 * @author Joseph Heled
 *
 */
@Description("A Boolean-valued parameter represents a value (or array of values if the dimension is larger than one) " +
        "in the state space that can be changed by operators.")
public class BooleanParameter extends Parameter<java.lang.Boolean> {

    public Input<Boolean> m_pValues = new Input<Boolean>("value", "start value for this parameter");

    public BooleanParameter() {}

    /**
     * Constructor for testing.
     *
     * @param value
     * @param dimension
     * @throws Exception
     */
    public BooleanParameter(Integer value, Integer dimension) throws Exception {

        m_pValues.setValue(value, this);
        m_nDimension.setValue(dimension, this);
        initAndValidate();
    }


//    public Boolean getValue() {
//        return values[0];
//    }

    @Override
    public void initAndValidate() throws Exception {

        values = new Boolean[m_nDimension.get()];
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
        Parameter<Boolean> copy = new BooleanParameter();
        copy.setID(getID());
        copy.index = index;
        copy.values = new Boolean[values.length];
        System.arraycopy(values, 0, copy.values, 0, values.length);
        copy.m_bIsDirty = new boolean[values.length];
        return copy;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void assignTo(StateNode other) {
        Parameter<Boolean> copy = (Parameter<Boolean>) other;
        copy.setID(getID());
        copy.index = index;
        copy.values = new Boolean[values.length];
        System.arraycopy(values, 0, copy.values, 0, values.length);
        copy.m_fLower = m_fLower;
        copy.m_fUpper = m_fUpper;
        copy.m_bIsDirty = new boolean[values.length];
    }

    public void log(int nSample, PrintStream out) {
        BooleanParameter var = (BooleanParameter) getCurrent();//state.getStateNode(m_sID);
        int nValues = var.getDimension();
        for (int iValue = 0; iValue < nValues; iValue++) {
            out.print(var.getValue(iValue) + "\t");
        }
    }
}
