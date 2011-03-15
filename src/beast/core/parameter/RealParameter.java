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
    public Input<Double> lowerValueInput = new Input<Double>("lower", "lower value for this parameter (default -infinity)");
    public Input<Double> upperValueInput = new Input<Double>("upper", "upper value for this parameter (default +infinity)");

    public RealParameter() {
    }

    /** Constructor used by Input.setValue(String) **/
    public RealParameter(String sValue) throws Exception {
    	init(0.0, 0.0, sValue, 1);
    }
    public RealParameter(double [] fValues) throws Exception {
    	int nDimension = fValues.length;
    	values = new Double[nDimension];
    	for (int i = 0; i < nDimension; i++) {
    		values[i] = fValues[i];
    	}
		m_fLower = Double.NEGATIVE_INFINITY;
		m_fUpper = Double.POSITIVE_INFINITY;
    }
    /**
     * Constructor for testing.
     */
    public RealParameter(String value, Double lower, Double upper, Integer dimension) throws Exception {
    	init(lower, upper, value, dimension);
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

    	String sValue = m_pValues.get();
    	// remove start and end spaces
    	sValue = sValue.replaceAll("^\\s+", "");
    	sValue = sValue.replaceAll("\\s+$", "");
    	// split into space-separated bits
    	String [] sValues = sValue.split("\\s+");
    	int nDimension = Math.max(m_nDimension.get(), sValues.length);
        values = new java.lang.Double[nDimension];
        storedValues = new java.lang.Double[nDimension];
        for (int i = 0; i < values.length; i++) {
            values[i] = new Double(sValues[i % sValues.length]);
        }
        super.initAndValidate();
    }

    /** Valuable implementation follows **/
    
    /** RRB: we need this here, because the base implementation (public T getValue()) fails
     * for some reason. Why?
     */
    @Override
    public Double getValue() {
        return values[0];
    }

    @Override public double getArrayValue() {return values[0];}
    @Override public double getArrayValue(int iValue) {return values[iValue];};

    /** Loggable implementation **/
    @Override
    public void log(int nSample, PrintStream out) {
        RealParameter var = (RealParameter) getCurrent();
        int nValues = var.getDimension();
        for (int iValue = 0; iValue < nValues; iValue++) {
            out.print(var.getValue(iValue) + "\t");
        }
    }

    /** StateNode methods **/
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


