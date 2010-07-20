package beast.core.parameter;


import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.core.StateNode;

import java.io.PrintStream;

/**
 * @author Alexei Drummond
 */

@Description("A real-valued parameter represents a value (or array of values if the dimension is larger than one) " +
        "in the state space that can be changed by operators.")
public class RealParameter extends Parameter<Double> {
    public Input<Double> m_pValues = new Input<Double>("value", "start value for this parameter");
    public Input<Double> lowerValueInput = new Input<Double>("lower", "lower value for this parameter");
    public Input<Double> upperValueInput = new Input<Double>("upper", "upper value for this parameter");

    public RealParameter() {
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
    public RealParameter(Double value, Double lower, Double upper, Integer dimension) throws Exception {

        m_pValues.setValue(value, this);
        lowerValueInput.setValue(lower, this);
        upperValueInput.setValue(upper, this);
        m_nDimension.setValue(dimension, this);
        initAndValidate(null);
    }


    public Double getValue() {
        return values[0];
    }

    @Override
    public void initAndValidate(State state) throws Exception {
        m_fLower = lowerValueInput.get();
        m_fUpper = upperValueInput.get();

        System.out.println("Lower = " + m_fLower);
        System.out.println("Upper = " + m_fUpper);

        values = new java.lang.Double[m_nDimension.get()];
        for (int i = 0; i < values.length; i++) {
            values[i] = m_pValues.get();
        }
        super.initAndValidate(state);
    }

//    public void setValue(Double fValue) throws Exception {
//
//        if (fValue < getLower() || fValue > getUpper()) throw new IllegalArgumentException("new value outside bounds!");
//
//        if (isStochastic()) {
//            values[0] = fValue;
//            setDirty(true);
//        } else throw new Exception("Can't set the value of a fixed parameter.");
//    }


    /**
     * deep copy *
     */
    @Override
    public Parameter<?> copy() {
        RealParameter copy = new RealParameter();
        copy.setID(getID());
        copy.index = index;
        copy.values = new java.lang.Double[values.length];
        System.arraycopy(values, 0, copy.values, 0, values.length);
        copy.m_fLower = m_fLower;
        copy.m_fUpper = m_fUpper;
        copy.m_bIsDirty = new boolean[values.length];
        return copy;
    }

    @Override
    public void assignTo(StateNode other) {
        RealParameter copy = (RealParameter) other;
        copy.setID(getID());
        copy.index = index;
        copy.values = new java.lang.Double[values.length];
        System.arraycopy(values, 0, copy.values, 0, values.length);
        copy.m_fLower = m_fLower;
        copy.m_fUpper = m_fUpper;
        copy.m_bIsDirty = new boolean[values.length];
    }

    public void log(int nSample, State state, PrintStream out) {
        RealParameter var = (RealParameter) getCurrent();//state.getStateNode(m_sID);
        int nValues = var.getDimension();
        for (int iValue = 0; iValue < nValues; iValue++) {
            out.print(var.getValue(iValue) + "\t");
        }
    }
}


