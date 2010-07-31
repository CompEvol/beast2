package beast.evolution.substitutionmodel;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public interface EigenSystem {
    /**
     * Set the instantaneous rate matrix
     * @param matrix
     */
    EigenDecomposition decomposeMatrix(double[][] matrix);
}
