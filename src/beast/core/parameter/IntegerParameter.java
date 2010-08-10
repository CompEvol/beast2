package beast.core.parameter;


import beast.core.Description;
import beast.core.Input;

import java.io.PrintStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


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
     */
    public IntegerParameter(Integer value, Integer lower, Integer upper, Integer dimension) throws Exception {
    	init(value, lower, upper, dimension);
    }

    @Override
    public void initAndValidate() throws Exception {
    	if (lowerValueInput.get() != null) {
    		m_fLower = lowerValueInput.get();
    	} else {
    		m_fLower = Integer.MIN_VALUE;
    	}
    	if (upperValueInput.get() != null) {
    		m_fUpper = upperValueInput.get();
    	} else {
    		m_fUpper = Integer.MAX_VALUE;
    	}
        values = new Integer[m_nDimension.get()];
        for (int i = 0; i < values.length; i++) {
            values[i] = m_pValues.get();
        }
        super.initAndValidate();
    }


    @Override
    public void log(int nSample, PrintStream out) {
        IntegerParameter var = (IntegerParameter) getCurrent();
        int nValues = var.getDimension();
        for (int iValue = 0; iValue < nValues; iValue++) {
            out.print(var.getValue(iValue) + "\t");
        }
    }


	@Override
	public int scale(double fScale) {
		// nothing to do
		return 0;
	}

    @Override
    public void fromXML(Node node) {
    	NamedNodeMap atts = node.getAttributes();
    	setID(atts.getNamedItem("id").getNodeValue());
    	setLower(Integer.parseInt(atts.getNamedItem("lower").getNodeValue()));
    	setUpper(Integer.parseInt(atts.getNamedItem("upper").getNodeValue()));
    	int nDimension = Integer.parseInt(atts.getNamedItem("dimension").getNodeValue());
    	values = new Integer[nDimension];
    	String sValue = node.getTextContent();
    	String [] sValues = sValue.split(",");
    	for (int i = 0; i < sValues.length; i++) {
    		values[i] = Integer.parseInt(sValues[i]);
    	}
    }
}
