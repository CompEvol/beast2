package beast.core.parameter;



import beast.core.Description;
import beast.core.Input;

import java.io.PrintStream;


/**
 * @author Alexei Drummond
 */

@Description("An integer-valued parameter represents a value (or array of values if the dimension is larger than one) " +
        "in the state space that can be changed by operators.")
public class IntegerParameter extends Parameter<java.lang.Integer> {
    public Input<Integer> lowerValueInput = new Input<Integer>("lower", "lower value for this parameter");
    public Input<Integer> upperValueInput = new Input<Integer>("upper", "upper value for this parameter");

    public IntegerParameter() {
    }

    /** Constructor used by Input.setValue(String) **/
    public IntegerParameter(String sValue) throws Exception {
    	init(0, 0, sValue, 1);
    }
    /**
     * Constructor for testing.
     */
    public IntegerParameter(String value, Integer lower, Integer upper, Integer dimension) throws Exception {
    	init(lower, upper, value, dimension);
    }

    @Override
    public void initAndValidate() throws Exception {
    	if (lowerValueInput.get() != null) {
    		m_fLower = lowerValueInput.get();
    	} else {
    		m_fLower = Integer.MIN_VALUE+1;
    	}
    	if (upperValueInput.get() != null) {
    		m_fUpper = upperValueInput.get();
    	} else {
    		m_fUpper = Integer.MAX_VALUE-1;
    	}
    	String sValue = m_pValues.get();
    	// remove start and end spaces
    	sValue = sValue.replaceAll("^\\s+", "");
    	sValue = sValue.replaceAll("\\s+$", "");
    	// split into space-separated bits
    	String [] sValues = sValue.split("\\s+");
    	int nDimension = Math.max(m_nDimension.get(), sValues.length);
        values = new java.lang.Integer[nDimension];
        for (int i = 0; i < values.length; i++) {
            values[i] = new Integer(sValues[i % sValues.length]);
        }
        super.initAndValidate();
    }


    /** Valuable implementation follows **/
    /** we need this here, because the base implementation (public T getValue()) fails
     * for some reason
     */
    @Override
    public Integer getValue() {
        return values[0];
    }
    @Override public double getArrayValue() {return (double) values[0];}
    @Override public double getArrayValue(int iValue) {return (double) values[iValue];};

    /** Loggable implementation follows **/
    @Override
    public void log(int nSample, PrintStream out) {
        IntegerParameter var = (IntegerParameter) getCurrent();
        int nValues = var.getDimension();
        for (int iValue = 0; iValue < nValues; iValue++) {
            out.print(var.getValue(iValue) + "\t");
        }
    }

    /** StateNode methods **/
	@Override
	public int scale(double fScale) {
		// nothing to do
		System.err.println("Attempt to scale Integer parameter " + getID() + "  has no effect");
		return 0;
	}

	@Override
    void fromXML(int nDimension, String sLower, String sUpper, String [] sValues) {
    	setLower(Integer.parseInt(sLower));
    	setUpper(Integer.parseInt(sUpper));
    	values = new Integer[nDimension];
    	for (int i = 0; i < sValues.length; i++) {
    		values[i] = Integer.parseInt(sValues[i]);
    	}
    }
}
