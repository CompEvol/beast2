package beast.base.core;

/**
 * Allows a BEASTObject, in particular a StateNode or CalculationNode to present itself as
 * an array of values. This is particular handy for generic calculations on a BEASTObject,
 * like calculation of ESS, posterior of a distribution or in the SpreadSheet interface
 * where the possibilities of calculations are limitless.
 * *
 */
public interface Function {

    /**
     * @return dimension of the Function *
     */
    int getDimension();

    /**
     * @return main value. For a 1 dimensional Function, this is the only
     *         value, but for a Tree this can be the root height, while the individual
     *         values obtained from getValue(dim) return the node heights.
     */
    default double getArrayValue() {
    	return getArrayValue(0);
    }

    /**
     * @param dim requested dimension
     * @return dim'th value (if any)
     */
    double getArrayValue(int dim);

    /**
     * @return all values as a double[]
     */
    default double[] getDoubleValues() {
        double[] values = new double[getDimension()];
        for (int i = 0; i < values.length; i++) {
            values[i] = getArrayValue(i);
        }
        return values;
    }

    @Description("Function that does not change over time")
	class Constant extends BEASTObject implements Function {
    	private double [] values;
    	private String[] names;
    	
    	public Constant() {
    		values = new double[1];
    	}
    	public Constant(@Param(name="value", description="Space delimited string of double values") String v) {
    		setValue(v);
    	}
    	
    	public void setValue(String v) {
    		String [] strs = v.trim().split("\\s+");    		
    		values = new double[strs.length];
    		for (int i = 0; i < strs.length; i++) {
    			values[i] = Double.parseDouble(strs[i]);
    		}
    	}

    	public String getValue() {
    		StringBuilder b = new StringBuilder();
    		for (int i = 0; i < values.length; i++) {
    			b.append(values[i] + " ");
    		}
    		return b.toString().trim();
    	}
    	
		@Override
		public int getDimension() {
			return values.length;
		}

		@Override
		public double getArrayValue(int dim) {
			return values[dim];
		}
		
		@Override
		public void initAndValidate() {
		}
    }
}
