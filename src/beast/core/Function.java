package beast.core;

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
    public int getDimension();

    /**
     * @return main value. For a 1 dimensional Function, this is the only
     *         value, but for a Tree this can be the root height, while the individual
     *         values obtained from getValue(dim) return the node heights.
     */
    public double getArrayValue();

    /**
     * @param dim requested dimension
     * @return dim'th value (if any)
     */
    public double getArrayValue(int dim);

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
}
