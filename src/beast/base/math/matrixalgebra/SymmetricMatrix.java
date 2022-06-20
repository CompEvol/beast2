/*
 * SymmetricMatrix.java
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
 * Symmetric matrix
 *
 * @author Didier H. Besset
 */
public class SymmetricMatrix extends Matrix {

	private static int lupCRLCriticalDimension = 36;

	/**
	 * Creates a symmetric matrix with given components.
	 *
	 * @param a double[][]
	 */
	public SymmetricMatrix(double[][] a) {
		super(a);
	}

	/**
	 * @param n int
	 * @throws java.lang.NegativeArraySizeException
	 *          if n <= 0
	 */
	public SymmetricMatrix(int n) throws NegativeArraySizeException {
		super(n, n);
	}

	/**
	 * Constructor method.
	 *
	 * @param n int
	 * @param m int
	 * @throws java.lang.NegativeArraySizeException
	 *          if n,m <= 0
	 */
	public SymmetricMatrix(int n, int m) throws NegativeArraySizeException {
		super(n, m);
	}

	/**
	 * @param a Matrix
	 * @return SymmetricMatrix	sum of the matrix with the supplied matrix.
	 * @throws IllegalDimension if the supplied matrix does not
	 *                          have the same dimensions.
	 */
	public SymmetricMatrix add(SymmetricMatrix a)
			throws IllegalDimension {
		return new SymmetricMatrix(addComponents(a));
	}

	/**
	 * Answers the inverse of the receiver computed via the CRL algorithm.
	 *
	 * @return MatrixAlgebra.SymmetricMatrix
	 * @throws java.lang.ArithmeticException if the matrix is singular.
	 */
	private SymmetricMatrix crlInverse() throws ArithmeticException {
		if (rows() == 1)
			return inverse1By1();
		else if (rows() == 2)
			return inverse2By2();
		Matrix[] splitMatrices = split();
		SymmetricMatrix b1 = (SymmetricMatrix) splitMatrices[0].inverse();
		Matrix cb1 = splitMatrices[2].secureProduct(b1);
		SymmetricMatrix cb1cT = new SymmetricMatrix(
				cb1.productWithTransposedComponents(splitMatrices[2]));
		splitMatrices[1] = ((SymmetricMatrix)
				splitMatrices[1]).secureSubtract(cb1cT).inverse();
		splitMatrices[2] = splitMatrices[1].secureProduct(cb1);
		splitMatrices[0] = b1.secureAdd(new SymmetricMatrix(
				cb1.transposedProductComponents(splitMatrices[2])));
		return SymmetricMatrix.join(splitMatrices);
	}

	/**
	 * @return MatrixAlgebra.SymmetricMatrix
	 * @throws matrixAlgebra.IllegalDimension The supplied components are not those of a square matrix.
	 * @throws matrixAlgebra.NonSymmetricComponents
	 *                                        The supplied components are not symmetric.
	 * @param	comp double[][]	components of the matrix
	 */
	public static SymmetricMatrix fromComponents(double[][] comp)
			throws IllegalDimension, NonSymmetricComponents {
		if (comp.length != comp[0].length)
			throw new IllegalDimension("Non symmetric components: a "
					+ comp.length + " by " + comp[0].length
					+ " matrix cannot be symmetric");
		for (int i = 0; i < comp.length; i++) {
			for (int j = 0; j < i; j++) {
				if (comp[i][j] != comp[j][i])
					throw new NonSymmetricComponents(
							"Non symmetric components: a[" + i + "][" + j
									+ "]= " + comp[i][j] + ", a[" + j + "]["
									+ i + "]= " + comp[j][i]);
			}
		}
		return new SymmetricMatrix(comp);
	}

	/**
	 * @param n int
	 * @return SymmetricMatrix	an identity matrix of size n
	 */
	public static SymmetricMatrix identityMatrix(int n) {
		double[][] a = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) a[i][j] = 0;
			a[i][i] = 1;
		}
		return new SymmetricMatrix(a);
	}

	/**
	 * @return Matrix		inverse of the receiver.
	 * @throws java.lang.ArithmeticException if the receiver is
	 *                                       a singular matrix.
	 */
	public Matrix inverse() throws ArithmeticException {
		return rows() < lupCRLCriticalDimension
				? new SymmetricMatrix(
				(new LUPDecomposition(this)).inverseMatrixComponents())
				: crlInverse();
	}

	/**
	 * Compute the inverse of the receiver in the case of a 1 by 1 matrix.
	 * Internal use only: no check is made.
	 *
	 * @return MatrixAlgebra.SymmetricMatrix
	 */
	private SymmetricMatrix inverse1By1() {
		double[][] newComponents = new double[1][1];
		newComponents[0][0] = 1 / components[0][0];
		return new SymmetricMatrix(newComponents);
	}

	/**
	 * Compute the inverse of the receiver in the case of a 2 by 2 matrix.
	 * Internal use only: no check is made.
	 *
	 * @return MatrixAlgebra.SymmetricMatrix
	 */
	private SymmetricMatrix inverse2By2() {
		double[][] newComponents = new double[2][2];
		double inverseDeterminant = 1 / (components[0][0] * components[1][1]
				- components[0][1] * components[1][0]);
		newComponents[0][0] = inverseDeterminant * components[1][1];
		newComponents[1][1] = inverseDeterminant * components[0][0];
		newComponents[0][1] = newComponents[1][0] =
				-inverseDeterminant * components[1][0];
		return new SymmetricMatrix(newComponents);
	}

	/**
	 * Build a matrix from 3 parts (inverse of split).
	 *
	 * @param a MatrixAlgebra.Matrix[]
	 * @return MatrixAlgebra.SymmetricMatrix
	 */
	private static SymmetricMatrix join(Matrix[] a) {
		int p = a[0].rows();
		int n = p + a[1].rows();
		double[][] newComponents = new double[n][n];
		for (int i = 0; i < p; i++) {
			for (int j = 0; j < p; j++)
				newComponents[i][j] = a[0].components[i][j];
			for (int j = p; j < n; j++)
				newComponents[i][j] = newComponents[j][i] =
						-a[2].components[j - p][i];
		}
		for (int i = p; i < n; i++) {
			for (int j = p; j < n; j++)
				newComponents[i][j] = a[1].components[i - p][j - p];
		}
		return new SymmetricMatrix(newComponents);
	}

	/**
	 * @param n int
	 * @return int
	 */
	private int largestPowerOf2SmallerThan(int n) {
		int m = 2;
		int m2;
		while (true) {
			m2 = 2 * m;
			if (m2 >= n)
				return m;
			m = m2;
		}
	}

	/**
	 * @param a double
	 * @return MatrixAlgebra.SymmetricMatrix
	 */
	public Matrix product(double a) {
		return new SymmetricMatrix(productComponents(a));
	}

	/**
	 * @param a Matrix
	 * @return Matrix		product of the receiver with the supplied matrix
	 * @throws IllegalDimension If the number of columns of
	 *                          the receivers are not equal to the number of rows
	 *                          of the supplied matrix.
	 */
	public SymmetricMatrix product(SymmetricMatrix a) throws IllegalDimension {
		return new SymmetricMatrix(productComponents(a));
	}

	/**
	 * @param a MatrixAlgebra.Matrix
	 * @return MatrixAlgebra.Matrix	product of the receiver with
	 *         the transpose of the supplied matrix
	 * @throws MatrixAlgebra.IllegalDimension If the number of
	 *                                        columns of the receiver are not equal to the number of
	 *                                        columns of the supplied matrix.
	 */
	public SymmetricMatrix productWithTransposed(SymmetricMatrix a)
			throws IllegalDimension {
		if (a.columns() != columns())
			throw new IllegalDimension(
					"Operation error: cannot multiply a "
							+ rows() + " by " + columns()
							+ " matrix with the transpose of a "
							+ a.rows() + " by " + a.columns() + " matrix");
		return new SymmetricMatrix(productWithTransposedComponents(a));
	}

	/**
	 * Same as add ( SymmetricMatrix a), but without dimension checking.
	 *
	 * @param a MatrixAlgebra.SymmetricMatrix
	 * @return MatrixAlgebra.SymmetricMatrix
	 */
	protected SymmetricMatrix secureAdd(SymmetricMatrix a) {
		return new SymmetricMatrix(addComponents(a));
	}

	/**
	 * Same as product(SymmetricMatrix a), but without dimension checking.
	 *
	 * @param a MatrixAlgebra.SymmetricMatrix
	 * @return MatrixAlgebra.SymmetricMatrix
	 */
	protected SymmetricMatrix secureProduct(SymmetricMatrix a) {
		return new SymmetricMatrix(productComponents(a));
	}

	/**
	 * Same as subtract ( SymmetricMatrix a), but without dimension checking.
	 *
	 * @param a MatrixAlgebra.SymmetricMatrix
	 * @return MatrixAlgebra.SymmetricMatrix
	 */
	protected SymmetricMatrix secureSubtract(SymmetricMatrix a) {
		return new SymmetricMatrix(subtractComponents(a));
	}

	/**
	 * Divide the receiver into 3 matrices of approximately equal dimension.
	 *
	 * @return MatrixAlgebra.Matrix[]	Array of splitted matrices
	 */
	private Matrix[] split() {
		int n = rows();
		int p = largestPowerOf2SmallerThan(n);
		int q = n - p;
		double[][] a = new double[p][p];
		double[][] b = new double[q][q];
		double[][] c = new double[q][p];
		for (int i = 0; i < p; i++) {
			for (int j = 0; j < p; j++)
				a[i][j] = components[i][j];
			for (int j = p; j < n; j++)
				c[j - p][i] = components[i][j];
		}
		for (int i = p; i < n; i++) {
			for (int j = p; j < n; j++)
				b[i - p][j - p] = components[i][j];
		}
		Matrix[] answer = new Matrix[3];
		answer[0] = new SymmetricMatrix(a);
		answer[1] = new SymmetricMatrix(b);
		answer[2] = new Matrix(c);
		return answer;
	}

	/**
	 * @param a matrixAlgebra.SymmetricMatrix
	 * @return matrixAlgebra.SymmetricMatrix
	 * @throws matrixAlgebra.IllegalDimension (from constructor).
	 */
	public SymmetricMatrix subtract(SymmetricMatrix a)
			throws IllegalDimension {
		return new SymmetricMatrix(subtractComponents(a));
	}

	/**
	 * @return matrixAlgebra.Matrix		the same matrix
	 */
	public Matrix transpose() {
		return this;
	}

	/**
	 * @param a MatrixAlgebra.SymmetricMatrix
	 * @return MatrixAlgebra.SymmetricMatrix	product of the tranpose
	 *         of the receiver with the supplied matrix
	 * @throws MatrixAlgebra.IllegalDimension If the number of
	 *                                        rows of the receiver are not equal to
	 *                                        the number of rows of the supplied matrix.
	 */
	public SymmetricMatrix transposedProduct(SymmetricMatrix a)
			throws IllegalDimension {
		if (a.rows() != rows())
			throw new IllegalDimension(
					"Operation error: cannot multiply a tranposed "
							+ rows() + " by " + columns() + " matrix with a " +
							a.rows() + " by " + a.columns() + " matrix");
		return new SymmetricMatrix(transposedProductComponents(a));
	}
}