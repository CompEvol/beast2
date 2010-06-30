
/*
 * File Array2d.java
 *
 * Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
 *
 * This file is part of BEAST2.
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
package snap.matrix;
/** 
 * class for storing dense matrix
 * with 1-base, so a 2x2 matrix Array2d A can
 * be accessed using A.at(1,1) A.at(2,2) etc.  
 * 
 *
 */
public class Array2d {

	/** constructor that initialises as zero matrix of size nRows x nCols **/
	public Array2d(int nRows, int nCols) {
		m_nRows = nRows;
		m_nCols = nCols;
		m_values = new double[nRows*nCols];
	} // c'tor
	public Array2d(int nRows, int nCols, double [] values) {
		m_nRows = nRows;
		m_nCols = nCols;
		m_values = new double[nRows*nCols];
		System.arraycopy(values, 0, m_values, 0, m_values.length);
	} // c'tor
	
	/** constructor that initialises as square identity matrix **/
	public Array2d(int nRows) {
		m_nRows = nRows;
		m_nCols = nRows;
		m_values = new double[nRows*nRows];
		for (int i = 1; i <= m_nRows; i++) {
			set(i,i,1.0);
		}
	} // c'tor

	/** nr of rows in array **/
	int m_nRows = 0;
	public int getNrOfRows() {return m_nRows;}
	/** nr of columns in array **/
	int m_nCols = 0;
	public int getNrOfCols() {return m_nCols;}
	/** 2-dimensional zero based array for storing matrix values **/
	double [] m_values;
	/** return value at position (i,j) in matrix **/
	public double at(int i, int j) {return m_values[(i-1)*m_nCols+j-1];}
	/** assign value v to position (i,j) in matrix **/
	public void set(int i, int j, double v) {m_values[(i-1)*m_nCols+j-1] = v;}
	/** divide value at position (i,j) by v **/
	public void div(int i, int j, double v) {m_values[(i-1)*m_nCols+j-1] /= v;}
	/** multiply value at position (i,j) by v **/
	public void mul(int i, int j, double v) {m_values[(i-1)*m_nCols+j-1] *= v;}
	/** subtract v from value at position (i,j) **/
	public void min(int i, int j, double v) {m_values[(i-1)*m_nCols+j-1] -= v;}
	/** add v to value at position (i,j) **/
	public void add(int i, int j, double v) {m_values[(i-1)*m_nCols+j-1] += v;}
	public void add(double multiplier, double [] X) {
		for (int i = 0; i <m_values.length; i++) {
			m_values[i] += multiplier*X[i];
		}
	}
	/** return fresh copy of current matrix **/
	public Array2d copy() {
		Array2d copy = new Array2d(m_nRows, m_nCols, m_values);
		/*
		for (int iRow = 1; iRow <= m_nRows; iRow++) {
			for (int iCol = 1; iCol <= m_nCols; iCol++) {
				copy.set(iRow, iCol, at(iRow, iCol));
			}
		}
		*/
		return copy;
	} // copy

	/** return transpose of current matrix **/
	public Array2d transpose() {
		Array2d transpose = new Array2d(m_nRows, m_nCols);
		for (int iRow = 1; iRow <= m_nRows; iRow++) {
			for (int iCol = 1; iCol <= m_nCols; iCol++) {
				transpose.set(iCol, iRow, at(iRow, iCol));
			}
		}
		return transpose;
	} // transpose

	/** return matrix as zero-based vector, so that matrix item (i,j) 
	 * is at position (i-1)*nCol+j-1 in the vector
	 **/
	public double [] asZeroBasedArray() {
		return m_values;
		
		//double [] array = new double[m_nRows*m_nCols];
		//System.arraycopy(m_values, 0, array, 0, m_values.length);
		/*
		for (int iRow = 1; iRow <= m_nRows; iRow++) {
			for (int iCol = 1; iCol <= m_nCols; iCol++) {
				array[(iRow-1)*m_nCols + iCol-1] = at(iCol, iRow);
			}
		}
		*/
//		return array;
	} // asArray

	//public void assign(double [] zeroBasedArray) { 
	//	System.arraycopy(zeroBasedArray, 0, m_values, 0, m_nRows * m_nCols);
	//}
	
	/** get column of matrix in 1-based vector **/ 
	public void getColumn(int iCol, double [] column) {
		//double [] column = new double [m_nRows + 1];
		for (int iRow = 1; iRow <= m_nRows; iRow++) {
			column[iRow] = at(iRow, iCol);
		}
		//return column;
	} // getColumn

	public void set(double [] zeroBasedArray, int nRows) {
		m_nRows = nRows;
		m_nCols = nRows;
		m_values = new double[nRows*nRows];
		System.arraycopy(zeroBasedArray, 0, m_values, 0, m_values.length);
		/*
		for (int iRow = 1; iRow <= m_nRows; iRow++) {
			for (int iCol = 1; iCol <= m_nCols; iCol++) {
				set(iRow, iCol, zeroBasedArray[(iRow-1)*m_nCols + iCol-1]);
			}
		}
		*/
	} // set

	/**
	 Multiples each entry of A by lambda
	 **/
	public void scale(double lambda) {
		for (int i = 0; i < m_values.length; i++) {
			m_values[i] *= lambda;
		}
		/*
		for (int iRow = 1; iRow <= m_nRows; iRow++) {
			for (int iCol = 1; iCol <= m_nCols; iCol++) {
				mul(iRow, iCol,lambda);
			}
		}
		*/
	}		
	
	public void resize(int nRows, int nCols) {
		if (nRows == m_nRows && nCols == m_nCols) {
			return;
		}
		double [] values = new double[nRows*nCols];
		if (m_nRows > nRows) {
			m_nRows = nRows;
		}
		for (int iRow = 0; iRow < m_nRows; iRow++) {
			System.arraycopy(m_values, iRow*m_nCols, values, iRow*nCols, Math.min(m_nCols, nCols));
		}
		m_values = values;
		m_nRows = nRows;
		m_nCols = nCols;
	} // resize
/*
	public void multiplyA(Array2d B, Array2d X) throws Exception {
		int qB = B.getNrOfRows();
		int nCols = B.getNrOfCols();
		if (m_nCols!=qB) {
			throw new Exception("Non-matching dimensions in matrix multiplication");
		}

		X.resize(m_nRows,nCols);
		for (int iRow = 1; iRow <= m_nRows; iRow++) {
			for (int iCol = 1; iCol <= m_nCols; iCol++) {
				double sum = 0.0;
				for(int k = 1; k <= m_nCols; k++) {
					sum += at(iRow,k)*B.at(k,iCol);
				}
				//TODO reimplement matrices as one dimensional arrays. BUT check convolution code!
				X.set(iRow, iCol, sum);
			}
		}
	}
	public void multiplyV(double[] b, double[] x) throws Exception {
		int mb = b.length;
		if (m_nCols!=mb)
			throw new Exception("Non-matching dimensions in matrix-vector multiplication");
		//x.resize(m_nRows+1);
		x = new double[m_nRows];
		for(int i=0;i<m_nRows;i++) {
			double sum = 0.0;
			for(int j=0;j<m_nCols;j++) {
				sum += at(i+1,j+1)*b[j];
			}
			x[i] = sum;
		}
	}
*/	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (int iRow = 1; iRow <= m_nRows; iRow++) {
			buf.append('[');
			for (int iCol = 1; iCol <= m_nCols; iCol++) {
				buf.append(m_values[(iRow-1)*m_nCols+iCol-1] + " ");
			}
			buf.append("]\n");
		}
		return buf.toString();
	}
/*
	void multiply(Vector<Double> b, Vector<Double> x) throws Exception {
		int mb = b.size()-1;
		if (m_nCols!=mb)
			throw new Exception("Non-matching dimensions in matrix-vector multiplication");
		//x.resize(m_nRows+1);
		for(int i=0;i<m_nRows;i++) {
			double sum = 0.0;
			for(int j=0;j<m_nCols;j++) {
				sum += at(i,j)*b.elementAt(j);
			}
			x.set(i, sum);
		}
	}
*/	
}
