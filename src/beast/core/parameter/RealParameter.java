package beast.core.parameter;


import java.io.PrintStream;

import beast.core.Description;
import beast.core.Input;


/**
 * @author Alexei Drummond
 */

@Description("A real-valued parameter represents a value (or array of values if the dimension is larger than one) " +
        "in the state space that can be changed by operators.")
public class RealParameter extends Parameter.Base<Double> {
    final public Input<Double> lowerValueInput = new Input<>("lower", "lower value for this parameter (default -infinity)");
    final public Input<Double> upperValueInput = new Input<>("upper", "upper value for this parameter (default +infinity)");

    public RealParameter() {
    }

    public RealParameter(final Double[] values) {
        super(values);
    }

    /**
     * Constructor used by Input.setValue(String) *
     */
    public RealParameter(final String value) throws Exception {
        init(0.0, 0.0, value, 1);
    }

    @Override
    public void initAndValidate() throws Exception {
        if (lowerValueInput.get() != null) {
            m_fLower = lowerValueInput.get();
        } else {
            m_fLower = Double.NEGATIVE_INFINITY;
        }
        if (upperValueInput.get() != null) {
            m_fUpper = upperValueInput.get();
        } else {
            m_fUpper = Double.POSITIVE_INFINITY;
        }
        super.initAndValidate();
    }

    @Override
    Double getMax() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    Double getMin() {
        return Double.NEGATIVE_INFINITY;
    }
    /** Valuable implementation follows **/

    /**
     * RRB: we need this here, because the base implementation (public T getValue()) fails
     * for some reason. Why?
     */
    @Override
    public Double getValue() {
        return values[0];
    }

    @Override
    public double getArrayValue() {
        return values[0];
    }

    @Override
    public double getArrayValue(final int index) {
        return values[index];
    }

    /**
     * Loggable implementation *
     */
    @Override
    public void log(final int nSample, final PrintStream out) {
        final RealParameter var = (RealParameter) getCurrent();
        final int nValues = var.getDimension();
        for (int iValue = 0; iValue < nValues; iValue++) {
            out.print(var.getValue(iValue) + "\t");
        }
    }

    /**
     * StateNode methods *
     */
    @Override
    public int scale(final double fScale) throws Exception {
        for (int i = 0; i < values.length; i++) {
            values[i] *= fScale;
            if (values[i] < m_fLower || values[i] > m_fUpper) {
                throw new IllegalArgumentException("parameter scaled our of range");
            }
        }
        return values.length;
    }


    @Override
    void fromXML(final int nDimension, final String sLower, final String sUpper, final String[] sValues) {
        setLower(Double.parseDouble(sLower));
        setUpper(Double.parseDouble(sUpper));
        values = new Double[nDimension];
        for (int i = 0; i < sValues.length; i++) {
            values[i] = Double.parseDouble(sValues[i]);
        }
    }


}


