package beast.core.parameter;


import beast.core.Description;
import beast.core.Input;

import java.io.PrintStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

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


    /** we need this here, because the base implementation (public T getValue()) fails
     * for some reason
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

        System.out.println("Lower = " + m_fLower);
        System.out.println("Upper = " + m_fUpper);

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
    public void fromXML(Node node) {
    	NamedNodeMap atts = node.getAttributes();
    	setID(atts.getNamedItem("id").getNodeValue());
    	setLower(Double.parseDouble(atts.getNamedItem("lower").getNodeValue()));
    	setUpper(Double.parseDouble(atts.getNamedItem("upper").getNodeValue()));
    	int nDimension = Integer.parseInt(atts.getNamedItem("dimension").getNodeValue());
    	values = new Double[nDimension];
    	String sValue = node.getTextContent();
    	String [] sValues = sValue.split(",");
    	for (int i = 0; i < sValues.length; i++) {
    		values[i] = Double.parseDouble(sValues[i]);
    	}
    }
}


