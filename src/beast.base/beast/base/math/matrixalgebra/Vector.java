/*
 * Vector.java
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
 * Vector implementation
 *
 * @author Didier H. Besset
 */
public class Vector {
	protected double[] components;

	/**
	 * Create a vector of given dimension.
	 * NOTE: The supplied array of components must not be changed.
	 *
	 * @param comp double[]
	 */
	public Vector(double comp[]) throws NegativeArraySizeException {
		int n = comp.length;
		if (n <= 0)
			throw new NegativeArraySizeException(
					"Vector components cannot be empty");
		components = new double[n];
		System.arraycopy(comp, 0, components, 0, n);
	}

	public Vector(int comp[]) throws NegativeArraySizeException {
		int n = comp.length;
		if (n <= 0)
			throw new NegativeArraySizeException(
					"Vector components cannot be empty");
		components = new double[n];
//	System.arraycopy( comp, 0, components, 0, n);
		for (int i = 0; i < n; i++)
			components[i] = comp[i];


	}

	/**
	 * Create a vector of given dimension.
	 *
	 * @param dimension int dimension of the vector; must be positive.
	 */
	public Vector(int dimension) throws NegativeArraySizeException {
		if (dimension <= 0)
			throw new NegativeArraySizeException(
					"Requested vector size: " + dimension);
		components = new double[dimension];
		clear();
	}

	/**
	 * @param v DHBmatrixAlgebra.DhbVector
	 * @throws DHBmatrixAlgebra.DhbIllegalDimension
	 *          if the vector
	 *          and supplied vector do not have the same dimension.
	 */
	public void accumulate(double[] x) throws IllegalDimension {
		if (this.dimension() != x.length)
			throw new IllegalDimension("Attempt to add a "
					+ this.dimension() + "-dimension vector to a "
					+ x.length + "-dimension array");
		for (int i = 0; i < this.dimension(); i++)
			components[i] += x[i];
	}

	/**
	 * @param v DHBmatrixAlgebra.DhbVector
	 * @throws DHBmatrixAlgebra.DhbIllegalDimension
	 *          if the vector
	 *          and supplied vector do not have the same dimension.
	 */
	public void accumulate(Vector v) throws IllegalDimension {
		if (this.dimension() != v.dimension())
			throw new IllegalDimension("Attempt to add a "
					+ this.dimension() + "-dimension vector to a "
					+ v.dimension() + "-dimension vector");
		for (int i = 0; i < this.dimension(); i++)
			components[i] += v.components[i];
	}

	/**
	 * @param v DHBmatrixAlgebra.DhbVector
	 * @throws DHBmatrixAlgebra.DhbIllegalDimension
	 *          if the vector
	 *          and supplied vector do not have the same dimension.
	 */
	public void accumulateNegated(double[] x) throws IllegalDimension {
		if (this.dimension() != x.length)
			throw new IllegalDimension("Attempt to add a "
					+ this.dimension() + "-dimension vector to a "
					+ x.length + "-dimension array");
		for (int i = 0; i < this.dimension(); i++)
			components[i] -= x[i];
	}

	/**
	 * @param v DHBmatrixAlgebra.DhbVector
	 * @throws DHBmatrixAlgebra.DhbIllegalDimension
	 *          if the vector
	 *          and supplied vector do not have the same dimension.
	 */
	public void accumulateNegated(Vector v) throws IllegalDimension {
		if (this.dimension() != v.dimension())
			throw new IllegalDimension("Attempt to add a "
					+ this.dimension() + "-dimension vector to a "
					+ v.dimension() + "-dimension vector");
		for (int i = 0; i < this.dimension(); i++)
			components[i] -= v.components[i];
	}

	/**
	 * @param v DHBmatrixAlgebra.DhbVector
	 * @return DHBmatrixAlgebra.DhbVector sum of the vector with
	 *         the supplied vector
	 * @throws DHBmatrixAlgebra.DhbIllegalDimension
	 *          if the vector
	 *          and supplied vector do not have the same dimension.
	 */
	public Vector add(Vector v) throws IllegalDimension {
		if (this.dimension() != v.dimension())
			throw new IllegalDimension("Attempt to add a "
					+ this.dimension() + "-dimension vector to a "
					+ v.dimension() + "-dimension vector");
		double[] newComponents = new double[this.dimension()];
		for (int i = 0; i < this.dimension(); i++)
			newComponents[i] = components[i] + v.components[i];
		return new Vector(newComponents);
	}

	/**
	 * Sets all components of the receiver to 0.
	 */
	public void clear() {
		for (int i = 0; i < components.length; i++) components[i] = 0;
	}

	/**
	 * @param n int
	 * @return double
	 */
	public double component(int n) {
		return components[n];
	}

	/**
	 * Returns the dimension of the vector.
	 *
	 * @return int
	 */
	public int dimension() {
		return components.length;
	}

	/**
	 * @param v DHBmatrixAlgebra.DhbVector
	 * @return true if the supplied vector is equal to the receiver
	 */
	public boolean equals(Vector v) {
		int n = this.dimension();
		if (v.dimension() != n)
			return false;
		for (int i = 0; i < n; i++) {
			if (v.components[i] != components[i])
				return false;
		}
		return true;
	}

	/**
	 * Computes the norm of a vector.
	 */
	public double norm() {
		double sum = 0;
		for (int i = 0; i < components.length; i++)
			sum += components[i] * components[i];
		return Math.sqrt(sum);
	}

	/**
	 * @param x double
	 */
	public Vector normalizedBy(double x) {
		for (int i = 0; i < this.dimension(); i++)
			components[i] /= x;
		return this;
	}

	/**
	 * Computes the product of the vector by a number.
	 *
	 * @param d double
	 * @return DHBmatrixAlgebra.DhbVector
	 */
	public Vector product(double d) {
		double newComponents[] = new double[components.length];
		for (int i = 0; i < components.length; i++)
			newComponents[i] = d * components[i];
		return new Vector(newComponents);
	}

	/**
	 * Compute the scalar product (or dot product) of two vectors.
	 *
	 * @param v DHBmatrixAlgebra.DhbVector
	 * @return double the scalar product of the receiver with the argument
	 * @throws DHBmatrixAlgebra.DhbIllegalDimension
	 *          if the dimension
	 *          of v is not the same.
	 */
	public double product(Vector v) throws IllegalDimension {
		int n = v.dimension();
		if (components.length != n)
			throw new IllegalDimension(
					"Dot product with mismatched dimensions: "
							+ components.length + ", " + n);
		return secureProduct(v);
	}

	/**
	 * Computes the product of the transposed vector with a matrix
	 *
	 * @param a MatrixAlgebra.Matrix
	 * @return MatrixAlgebra.DhbVector
	 */
	public Vector product(Matrix a) throws IllegalDimension {
		int n = a.rows();
		int m = a.columns();
		if (this.dimension() != n)
			throw new IllegalDimension(
					"Product error: transposed of a " + this.dimension()
							+ "-dimension vector cannot be multiplied with a "
							+ n + " by " + m + " matrix");
		return secureProduct(a);
	}

	/**
	 * @param x double
	 */
	public Vector scaledBy(double x) {
		for (int i = 0; i < this.dimension(); i++)
			components[i] *= x;
		return this;
	}

	/**
	 * Compute the scalar product (or dot product) of two vectors.
	 * No dimension checking is made.
	 *
	 * @param v DHBmatrixAlgebra.DhbVector
	 * @return double the scalar product of the receiver with the argument
	 */
	protected double secureProduct(Vector v) {
		double sum = 0;
		for (int i = 0; i < v.dimension(); i++)
			sum += components[i] * v.components[i];
		return sum;
	}

	/**
	 * Computes the product of the transposed vector with a matrix
	 *
	 * @param a MatrixAlgebra.Matrix
	 * @return MatrixAlgebra.DhbVector
	 */
	protected Vector secureProduct(Matrix a) {
		int n = a.rows();
		int m = a.columns();
		double[] vectorComponents = new double[m];
		for (int j = 0; j < m; j++) {
			vectorComponents[j] = 0;
			for (int i = 0; i < n; i++)
				vectorComponents[j] += components[i] * a.components[i][j];
		}
		return new Vector(vectorComponents);
	}

	/**
	 * @param v DHBmatrixAlgebra.DhbVector
	 * @return DHBmatrixAlgebra.DhbVector	subtract the supplied vector
	 *         to the receiver
	 * @throws DHBmatrixAlgebra.DhbIllegalDimension
	 *          if the vector
	 *          and supplied vector do not have the same dimension.
	 */
	public Vector subtract(Vector v) throws IllegalDimension {
		if (this.dimension() != v.dimension())
			throw new IllegalDimension("Attempt to add a "
					+ this.dimension() + "-dimension vector to a "
					+ v.dimension() + "-dimension vector");
		double[] newComponents = new double[this.dimension()];
		for (int i = 0; i < this.dimension(); i++)
			newComponents[i] = components[i] - v.components[i];
		return new Vector(newComponents);
	}

	/**
	 * @param v MatrixAlgebra.DhbVector	second vector to build tensor
	 *          product with.
	 * @return MatrixAlgebra.Matrix	tensor product with the specified
	 *         vector
	 */
	public Matrix tensorProduct(Vector v) {
		int n = dimension();
		int m = v.dimension();
		double[][] newComponents = new double[n][m];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++)
				newComponents[i][j] = components[i] * v.components[j];
		}
		return n == m ? new SymmetricMatrix(newComponents)
				: new Matrix(newComponents);
	}

	/**
	 * @return double[]	a copy of the components of the receiver.
	 */
	public double[] toComponents() {
		int n = dimension();
		double[] answer = new double[n];
		System.arraycopy(components, 0, answer, 0, n);
		return answer;
	}

	/**
	 * Returns a string representation of the vector.
	 *
	 * @return java.lang.String
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		char[] separator = {'[', ' '};
		for (int i = 0; i < components.length; i++) {
			sb.append(separator);
			sb.append(components[i]);
			separator[0] = ',';
		}
		sb.append(']');
		return sb.toString();
	}
}