package beast.core.parameter;

import beast.core.Description;
import beast.core.Input;
import beast.core.State;

/**
 * @author Alexei Drummond
 */

@Description("A real-valued parameter represents a value in the state space that can be changed " +
        "by operators.")
public class RealParameter extends Parameter<Double> {
    public Input<Double> m_pValues = new Input<Double>("value", "start value for this parameter");
    public Input<Double> lowerValueInput = new Input<Double>("lower", "lower value for this parameter");
    public Input<Double> upperValueInput = new Input<Double>("upper", "upper value for this parameter");


    public Double getValue() {
        return values[0];
    }

    @Override
    public void initAndValidate(State state) throws Exception {
        m_fUpper = lowerValueInput.get();
        m_fLower = upperValueInput.get();
        values = new java.lang.Double[m_nDimension.get()];
        for (int i = 0; i < values.length; i++) {
            values[i] = m_pValues.get();
        }
    }

    /**
     * deep copy *
     */
    public Parameter copy() {
        Parameter<java.lang.Double> copy = new RealParameter();
        copy.setID(getID());
        copy.values = new java.lang.Double[values.length];
        System.arraycopy(values, 0, copy.values, 0, values.length);
        copy.m_fLower = m_fLower;
        copy.m_fUpper = m_fUpper;
        return copy;
    }
}


