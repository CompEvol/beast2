package beast.math.matrixalgebra;

import beast.base.Description;

/**
 * Created by IntelliJ IDEA.
 * User: msuchard
 * Date: Jan 12, 2007
 * Time: 9:05:44 PM
 * To change this template use File | Settings | File Templates.
 */
@Description("Class ported from BEAST1")
public class CholeskyDecomposition {

	/**
	 * Dimension of square matrix
	 */
	private int n;

	public boolean isSPD() {
		return isspd;
	}

	/**
	 * Symmetric and positive definite flag.
	 */
	private boolean isspd;

	public double[][] getL() {
		return L;
	}

	private double[][] L;

	public CholeskyDecomposition(double[][] A) throws IllegalDimension {

		n = A.length;
		L = new double[n][n];
		isspd = (A[0].length == n);
		if (!isspd)
			throw new IllegalDimension("Cholesky decomposition is only defined for square matrices");
		// Main loop.
		for (int j = 0; j < n; j++) {
			double[] Lrowj = L[j];
			double d = 0.0;
			for (int k = 0; k < j; k++) {
				double[] Lrowk = L[k];
				double s = 0.0;
				for (int i = 0; i < k; i++) {
					s += Lrowk[i] * Lrowj[i];
				}
				Lrowj[k] = s = (A[j][k] - s) / L[k][k];
				d = d + s * s;
				isspd = isspd & (A[k][j] == A[j][k]);
			}
			d = A[j][j] - d;
			isspd = isspd & (d > 0.0);
			L[j][j] = Math.sqrt(Math.max(d, 0.0));
			/*for (int k = j+1; k < n; k++) {
						L[j][k] = 0.0;
					 }*/
		}

	}

}
