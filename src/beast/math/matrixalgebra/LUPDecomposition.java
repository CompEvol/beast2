/*
 * LUPDecomposition.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package beast.math.matrixalgebra;

/**
 * Lower Upper Permutation (LUP) decomposition
 *
 * @author Didier H. Besset
 */
public class LUPDecomposition {
	/**
	 * Rows of the system
	 */
	private double[][] rows;
	/**
	 * Permutation
	 */
	private int[] permutation = null;
	/**
	 * Permutation's parity
	 */
	private int parity = 1;

	/**
	 * Constructor method
	 *
	 * @param components double[][]
	 * @throws DhbMatrixAlgebra.DhbIllegalDimension
	 *          the supplied matrix is not square
	 */
	public LUPDecomposition(double[][] components)
			throws IllegalDimension {
		int n = components.length;
		if (components[0].length != n)
			throw new IllegalDimension("Illegal system: a" + n + " by "
					+ components[0].length + " matrix is not a square matrix");
		rows = components;
		initialize();
	}

	/**
	 * Constructor method.
	 *
	 * @param m DhbMatrixAlgebra.Matrix
	 * @throws DhbMatrixAlgebra.DhbIllegalDimension
	 *          the supplied matrix is not square
	 */
	public LUPDecomposition(Matrix m) throws IllegalDimension {
		if (!m.isSquare())
			throw new IllegalDimension(
					"Supplied matrix is not a square matrix");
		initialize(m.components);
	}

	/**
	 * Constructor method.
	 *
	 * @param m DhbMatrixAlgebra.DhbSymmetricMatrix
	 */
	public LUPDecomposition(SymmetricMatrix m) {
		initialize(m.components);
	}

	/**
	 * @param xTilde double[]
	 * @return double[]
	 */
	private double[] backwardSubstitution(double[] xTilde) {
		int n = rows.length;
		double[] answer = new double[n];
		for (int i = n - 1; i >= 0; i--) {
			answer[i] = xTilde[i];
			for (int j = i + 1; j < n; j++)
				answer[i] -= rows[i][j] * answer[j];
			answer[i] /= rows[i][i];
		}
		return answer;
	}

	private void decompose() {
		int n = rows.length;
		permutation = new int[n];
		for (int i = 0; i < n; i++)
			permutation[i] = i;
		parity = 1;
		try {
			for (int i = 0; i < n; i++) {
				swapRows(i, largestPivot(i));
				pivot(i);
			}
		} catch (ArithmeticException e) {
			parity = 0;
		}
    }

	/**
	 * @return boolean	true if decomposition was done already
	 */
	private boolean decomposed() {
		if (parity == 1 && permutation == null)
			decompose();
		return parity != 0;
	}

	/**
	 * @param c double[]
	 * @return double[]
	 */
	public double determinant() {
		if (!decomposed())
			return Double.NaN;
		double determinant = parity;
		for (int i = 0; i < rows.length; i++)
			determinant *= rows[i][i];
		return determinant;
	}

	public boolean isPD() {
		for (int i = 0; i < rows.length; i++) {
			if (rows[i][i] <= 0)
				return false;
		}
		return true;
	}


	/**
	 * @param c double[]
	 * @return double[]
	 */
	private double[] forwardSubstitution(double[] c) {
		int n = rows.length;
		double[] answer = new double[n];
		for (int i = 0; i < n; i++) {
			answer[i] = c[permutation[i]];
			for (int j = 0; j <= i - 1; j++)
				answer[i] -= rows[i][j] * answer[j];
		}
		return answer;
	}

	private void initialize() {
		permutation = null;
		parity = 1;
	}

	/**
	 * @param components double[][]  components obtained from constructor methods.
	 */
	private void initialize(double[][] components) {
		int n = components.length;
		rows = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++)
				rows[i][j] = components[i][j];
		}
		initialize();
	}

	/**
	 * @param c double[]
	 * @return double[]
	 */
	public double[][] inverseMatrixComponents() {
		if (!decomposed())
			return null;
		int n = rows.length;
		double[][] inverseRows = new double[n][n];
		double[] column = new double[n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++)
				column[j] = 0;
			column[i] = 1;
			column = solve(column);
			for (int j = 0; j < n; j++)
				inverseRows[i][j] = column[j];
		}
		return inverseRows;
	}

	/**
	 * @param k int
	 * @return int
	 */
	private int largestPivot(int k) {
		double maximum = Math.abs(rows[k][k]);
		double abs;
		int index = k;
		for (int i = k + 1; i < rows.length; i++) {
			abs = Math.abs(rows[i][k]);
			if (abs > maximum) {
				maximum = abs;
				index = i;
			}
		}
		return index;
	}

	/**
	 * @param k int
	 */
	private void pivot(int k) {
		double inversePivot = 1 / rows[k][k];
		int k1 = k + 1;
		int n = rows.length;
		for (int i = k1; i < n; i++) {
			rows[i][k] *= inversePivot;
			for (int j = k1; j < n; j++)
				rows[i][j] -= rows[i][k] * rows[k][j];
		}
	}

	/**
	 * @param c double[]
	 * @return double[]
	 */
	public double[] solve(double[] c) {
		return decomposed()
				? backwardSubstitution(forwardSubstitution(c))
				: null;
	}

	/**
	 * @param c double[]
	 * @return double[]
	 */
	public Vector solve(Vector c) {
		double[] components = solve(c.components);
		if (components == null)
			return null;
		return components == null ? null : new Vector(components);
	}

	/**
	 * @param i int
	 * @param k int
	 */
	private void swapRows(int i, int k) {
		if (i != k) {
			double temp;
			for (int j = 0; j < rows.length; j++) {
				temp = rows[i][j];
				rows[i][j] = rows[k][j];
				rows[k][j] = temp;
			}
			int nTemp;
			nTemp = permutation[i];
			permutation[i] = permutation[k];
			permutation[k] = nTemp;
			parity = -parity;
		}
	}

	/**
	 * Make sure the supplied matrix components are those of
	 * a symmetric matrix
	 *
	 * @param components double
	 */
	public static void symmetrizeComponents(double[][] components) {
		for (int i = 0; i < components.length; i++) {
			for (int j = i + 1; j < components.length; j++) {
				components[i][j] += components[j][i];
				components[i][j] *= 0.5;
				components[j][i] = components[i][j];
			}
		}
	}

	/**
	 * Returns a String that represents the value of this object.
	 *
	 * @return a string representation of the receiver
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		char[] separator = {'[', ' '};
		int n = rows.length;
		for (int i = 0; i < n; i++) {
			separator[0] = '{';
			for (int j = 0; j < n; j++) {
				sb.append(separator);
				sb.append(rows[i][j]);
				separator[0] = ' ';
			}
			sb.append('}');
			sb.append('\n');
		}
		if (permutation != null) {
			sb.append(parity == 1 ? '+' : '-');
			sb.append("( " + permutation[0]);
			for (int i = 1; i < n; i++)
				sb.append(", " + permutation[i]);
			sb.append(')');
			sb.append('\n');
		}
		return sb.toString();
	}
}