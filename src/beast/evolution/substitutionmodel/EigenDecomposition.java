package beast.evolution.substitutionmodel;

/**
 * A storage structure to hold an Eigen Decomposition of a rate matrix.
 * This encapsulates everything and facilitates copying for store/restore
 * mechanisms.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @Author Marc A. Suchard
 * @version $Id$
 */
public class EigenDecomposition {

    public EigenDecomposition(double[] evec, double[] ievc, double[] eval) {
        Evec = evec;
        Ievc = ievc;
        Eval = eval;
    }

    public EigenDecomposition copy() {
        double[] evec = Evec.clone();
        double[] ievc = Ievc.clone();
        double[] eval = Eval.clone();

        return new EigenDecomposition(evec, ievc, eval);
    }

    /**
     * This function returns the Eigen vectors.
     * @return the array
     */
    public final double[] getEigenVectors() {
        return Evec;
    }

    /**
     * This function returns the inverse Eigen vectors.
     * @return the array
     */
    public final double[] getInverseEigenVectors() {
        return Ievc;
    }

    /**
     * This function returns the Eigen values.
     * @return the Eigen values
     */
    public final double[] getEigenValues() {
        return Eval;
    }

    /**
     * This functions returns true if the diagonalization may be complex
     * @return bool
     */
    public boolean canReturnComplexDiagonalization() {
        return false;
    }

    /**
     * This function rescales the eigen values; this is more stable than
     * rescaling the original Q matrix, also O(stateCount) instead of O(stateCount^2)
     */
    public void normalizeEigenValues(double scale) {
        int dim = Eval.length;
        for (int i = 0; i < dim; i++)

            Eval[i] /= scale;
    }

    // Eigenvalues, eigenvectors, and inverse eigenvectors
    private final double[] Evec;
    private final double[] Ievc;
    private final double[] Eval;

}
