package beast.evolution.substitutionmodel;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public interface EigenSystem {
    /**
     * Set the instantaneous rate matrix
     * This changes the values in matrix as side effect
     * @param matrix
     */
    EigenDecomposition decomposeMatrix(double[][] matrix);
}
