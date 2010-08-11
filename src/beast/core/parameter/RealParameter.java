package beast.core.parameter;



import beast.core.Description;
import beast.core.Input;

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
     */
    public RealParameter(Double value, Double lower, Double upper, Integer dimension) throws Exception {
    	init(value, lower, upper, dimension);
    }


    /** RRB: we need this here, because the base implementation (public T getValue()) fails
     * for some reason. Why?
     */
    @Override
    public Double getValue() {
        return values[0];
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

        values = new java.lang.Double[m_nDimension.get()];
        for (int i = 0; i < values.length; i++) {
            values[i] = m_pValues.get();
        }
        super.initAndValidate();
    }

    // RRB: if you remove next line, please document properly!
    @Override
    public void log(int nSample, PrintStream out) {
        RealParameter var = (RealParameter) getCurrent();
        int nValues = var.getDimension();
        for (int iValue = 0; iValue < nValues; iValue++) {
            out.print(var.getValue(iValue) + "\t");
        }
    }

    @Override
	public int scale(double fScale) throws Exception {
    	for (int i = 0; i < values.length; i++) {
    		values[i] *= fScale;
    		if (values[i] < m_fLower || values[i] > m_fUpper) {
    			throw new Exception("parameter scaled our of range");
    		}
    	}
		return values.length;
	}


    @Override
    void fromXML(int nDimension, String sLower, String sUpper, String [] sValues) {
    	setLower(Double.parseDouble(sLower));
    	setUpper(Double.parseDouble(sUpper));
    	values = new Double[nDimension];
    	for (int i = 0; i < sValues.length; i++) {
    		values[i] = Double.parseDouble(sValues[i]);
    	}
    }
}


