package beast.core;

/**
 * Allows a Plugin, in particular a StateNode or CalculationNode to present itself as
 * an array of values. This is particular handy for generic calculations on a Plugin,
 * like calculation of ESS, posterior of a distribution or in the SpreadSheet interface
 * where the possibilities of calculations are limitless.
 * *
 */
    // RRB: Should be renamed Statistic?
    // (Q2R): how come trees and other bizzare stuff are valuable's??
public interface Valuable {

    /**
     * @return dimension of the Valuable *
     */
    public int getDimension();

    /**
     * @return main value. For a 1 dimensional Valuable, this is the only
     *         value, but for a Tree this can be the root height, while the individual
     *         values obtained from getValue(iDim) return the node heights.
     */
    public double getArrayValue();

    /**
     * @param iDim requested dimention
     * @return iDim'th value (if any)
     */
    public double getArrayValue(int iDim);
}
