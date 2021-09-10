/*
 * LinearEquations.java
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

package beast.base.math.matrixalgebra;

/**
 * Class representing a system of linear equations.
 *
 * @author Didier H. Besset
 */
public class LinearEquations {
	/**
	 * components is a matrix build from the system's matrix and
	 * the constant vector
	 */
	private double[][] rows;
	/**
	 * Array containing the solution vectors.
	 */
	private Vector[] solutions;

	/**
	 * Construct a system of linear equation Ax = y1, y2,....
	 *
	 * @param m double[][]
	 * @param c double[][]
	 * @throws IllegalDimension if the system's matrix is not square
	 *                          if constant dimension does not match
	 *                          that of the matrix
	 */
	public LinearEquations(double[][] m, double[][] c)
			throws IllegalDimension {
		int n = m.length;
		if (m[0].length != n)
			throw new IllegalDimension("Illegal system: a" + n + " by "
					+ m[0].length + " matrix is not a square matrix");
		if (c[0].length != n)
			throw new IllegalDimension("Illegal system: a " + n + " by " + n
					+ " matrix cannot build a system with a "
					+ c[0].length + "-dimensional vector");
		rows = new double[n][n + c.length];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++)
				rows[i][j] = m[i][j];
			for (int j = 0; j < c.length; j++)
				rows[i][n + j] = c[j][i];
		}
	}

	/**
	 * Construct a system of linear equation Ax = y.
	 *
	 * @param m double[][]		components of the system's matrix
	 * @param c double[]	components of the constant vector
	 * @throws IllegalDimension if the system's matrix is not square
	 *                          if constant dimension does not match
	 *                          that of the matrix
	 */
	public LinearEquations(double[][] m, double[] c)
			throws IllegalDimension {
		int n = m.length;
		if (m[0].length != n)
			throw new IllegalDimension("Illegal system: a" + n + " by "
					+ m[0].length + " matrix is not a square matrix");
		if (c.length != n)
			throw new IllegalDimension("Illegal system: a " + n + " by " + n
					+ " matrix cannot build a system with a "
					+ c.length + "-dimensional vector");
		rows = new double[n][n + 1];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++)
				rows[i][j] = m[i][j];
			rows[i][n] = c[i];
		}
	}

	/**
	 * Construct a system of linear equation Ax = y.
	 *
	 * @param a MatrixAlgebra.Matrix	matrix A
	 * @param y MatrixAlgebra.Vector	vector y
	 * @throws IllegalDimension if the system's matrix is not square
	 *                          if vector dimension does not match
	 *                          that of the matrix
	 */
	public LinearEquations(Matrix a, Vector y)
			throws IllegalDimension {
		this(a.components, y.components);
	}

	/**
	 * Computes the solution for constant vector p applying
	 * backsubstitution.
	 *
	 * @param p int
	 * @throws java.lang.ArithmeticException if one diagonal element
	 *                                       of the triangle matrix is zero.
	 */
	private void backSubstitution(int p) throws ArithmeticException {
		int n = rows.length;
		double[] answer = new double[n];
		double x;
		for (int i = n - 1; i >= 0; i--) {
			x = rows[i][n + p];
			for (int j = i + 1; j < n; j++)
				x -= answer[j] * rows[i][j];
			answer[i] = x / rows[i][i];
		}
		solutions[p] = new Vector(answer);
		return;
	}

	/**
	 * Finds the position of the largest pivot at step p.
	 *
	 * @param p int	step of pivoting.
	 * @return int
	 */
	private int largestPivot(int p) {
		double pivot = Math.abs(rows[p][p]);
		int answer = p;
		double x;
		for (int i = p + 1; i < rows.length; i++) {
			x = Math.abs(rows[i][p]);
			if (x > pivot) {
				answer = i;
				pivot = x;
			}
		}
		return answer;
	}

	/**
	 * Perform pivot operation at location p.
	 *
	 * @param p int
	 * @throws java.lang.ArithmeticException if the pivot element
	 *                                       is zero.
	 */
	private void pivot(int p) throws ArithmeticException {
		double inversePivot = 1 / rows[p][p];
		double r;
		int n = rows.length;
		int m = rows[0].length;
		for (int i = p + 1; i < n; i++) {
			r = inversePivot * rows[i][p];
			for (int j = p; j < m; j++)
				rows[i][j] -= rows[p][j] * r;
		}
		return;
	}

	/**
	 * Perform optimum pivot operation at location p.
	 *
	 * @param p int
	 */
	private void pivotingStep(int p) {
		swapRows(p, largestPivot(p));
		pivot(p);
		return;
	}

	/**
	 * @return Vector		solution for the 1st constant vector
	 */
	public Vector solution() throws ArithmeticException {
		return solution(0);
	}

	/**
	 * Return the vector solution of constants indexed by p.
	 *
	 * @param p int	index of the constant vector fed into the system.
	 * @return matrixAlgebra.Vector
	 * @throws java.lang.ArithmeticException if the system cannot be solved.
	 */
	public Vector solution(int p) throws ArithmeticException {
		if (solutions == null)
			solve();
		if (solutions[p] == null)
			backSubstitution(p);
		return solutions[p];
	}

	/**
	 * @throws java.lang.ArithmeticException if the system cannot be solved.
	 */
	private void solve() throws ArithmeticException {
		int n = rows.length;
		for (int i = 0; i < n; i++)
			pivotingStep(i);
		solutions = new Vector[rows[0].length - n];
	}

	/**
	 * Swaps rows p and q.
	 *
	 * @param p int
	 * @param q int
	 */
	private void swapRows(int p, int q) {
		if (p != q) {
			double temp;
			int m = rows[p].length;
			for (int j = 0; j < m; j++) {
				temp = rows[p][j];
				rows[p][j] = rows[q][j];
				rows[q][j] = temp;
			}
		}
		return;
	}

	/**
	 * Returns a string representation of the system.
	 *
	 * @return java.lang.String
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		char[] separator = {'[', ' '};
		int n = rows.length;
		int m = rows[0].length;
		for (int i = 0; i < n; i++) {
			separator[0] = '(';
			for (int j = 0; j < n; j++) {
				sb.append(separator);
				sb.append(rows[i][j]);
				separator[0] = ',';
			}
			separator[0] = ':';
			for (int j = n; j < m; j++) {
				sb.append(separator);
				sb.append(rows[i][j]);
				separator[0] = ',';
			}
			sb.append(')');
			sb.append('\n');
		}
		return sb.toString();
	}
}