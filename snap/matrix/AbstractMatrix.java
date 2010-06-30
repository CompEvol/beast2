
/*
 * File AbstractMatrix.java
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


import java.util.*;

import snap.likelihood.COMPLEX;

//typedef unsigned int uint;
//typedef complex<long double> COMPLEX;
//typedef vector<vector<double> > array2d;


abstract public class AbstractMatrix {

	/**
	 Abstract matrix (or linear operator) type.
	 Used for sparse matrices.
	 **/
		//void getSize(int &n, int &m)  {};
		public int getNrOfRows() {return 0;}
		public int getNrOfCols() {return 0;}
		public double infNorm()   {return 0;}
		public double trace()  {return 0;};


		//void multiply(Vector<Double> v, Vector<Double> Av) {};
		public abstract void multiply(double[] v, double[] Av);
		//void multiplyC(Vector<Complex> v, Vector<Complex> Av)  {};
		//void solve(Vector<Double> y, double offset, Vector<Double> x)  {};
		//void solve(Vector<Complex> y, Complex offset, Vector<Complex> x) {};

		//void print(OutputStream os) {};


		
		
		
		/**
		 Computes infinity norm. pg 56 of Golub and van Loan
		 **/
		public static double infinityNorm(Array2d A) {
			double norm = 0.0;
			int n = A.getNrOfRows();
			int m = A.getNrOfCols();

			for(int i=1;i<=n;i++) {
				double sum = 0.0;
				for(int j=1;j<=m;j++)
					sum+=Math.abs(A.at(i,j));
				norm = Math.max(norm,sum);
			}
			return norm;
		}	
		
		// 0-based vector norm
		public static double vectorNorm(double [] v) {
			int n = v.length;
			double sum=0.0;
			for(int i=0;i<n;i++)
				sum+=v[i]*v[i];
			return Math.sqrt(sum);
		}
		
		// 1-based vector norm
		public static double vectorNorm(Vector<Double> v) {
			int n = v.size()-1;
			double sum=0.0;
			for(int i=1;i<=n;i++)
				sum += v.elementAt(i) * v.elementAt(i);
			return Math.sqrt(sum);
		}
		
		
		


//	static void convolution2Dfft( Array2d A,  Array2d B, Array2d result, boolean zeroRow /*=false*/, boolean reAllocate /*= false*/) {
//		slowConvolution2D(A,  B, result);
//	}
//	static void slowConvolution2D( Array2d A,  Array2d B, Array2d result) {
//
//		//int ma,mb,na,nb;
//		//getSize(A,ma,na);
//		int na = A.getNrOfRows();
//		int ma = A.getNrOfCols();
//		//getSize(B,mb,nb);
//		int nb = B.getNrOfRows();
//		int mb = B.getNrOfCols();
//
//		//Determine smallest FFT array that is larger than A and B.
//		int mr = ma+mb-1;
//		int nr = na+nb-1;
//
//		result.resize(mr+1, nr+1);
//
//		//typedef vector<double>::const_iterator DBL_PTR;
//
//		for(int i3=1;i3<=mr;i3++) {
//			for(int j3=1;j3<=nr;j3++) {
//				double sum=0.0;
//				//Sum over all i1,i2 such that i1+i2 = i3+1.
//				//and over all j1,j2 such that j1+j2 = j3+1.
//				//Note: 1<=i1<=ma  and 1<=i2<=mb
//				int min_i = (i3>mb) ? (i3+1) - mb : 1;
//				int max_i = (i3<ma) ? i3 : ma;
//
//				int min_j = (j3>nb) ? (j3+1) - nb : 1;
//				int max_j = (j3<na) ? j3 : na;
//
//				for(int i1 = min_i;i1<=max_i;i1++) {
//					int i2 = (i3 + 1) - i1;
//					for(int j1 = min_j;j1<=max_j;j1++) {
//						int j2 = (j3+1) - j1;
//						sum+=A.at(i1,j1)*B.at(i2,j2);
//					}
//				}
//				result.set(i3,j3,sum);
//			}
//		}
//	}



	public static void main(String [] args) {
		//AbstractMatrix A = new QMatrix(2, 1, 1, 0.1);
		/*
		try { // test expm
			int m = 2;
			Array2d F = new Array2d(m, m);
			Array2d smallH = new Array2d(m, m);
			smallH.set(1,1,0.5);
			smallH.set(1,2,0.2);
			smallH.set(2,1,0.7);
			smallH.set(2,2,0.3);
			MatrixExponentiator.expm(smallH, F);
			System.out.println(F.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
			*/
		try { // test expmv
			Vector<Double> v = new Vector<Double>(5+1);
			v.add(0.0);
			v.add(0.1);
			v.add(0.2);
			v.add(0.3);
			v.add(0.4);
			v.add(0.5);
			//Vector<Double> w = MatrixExponentiator.expmv(0.01, A, v);
			//System.out.println(w.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // main
	
	public abstract void solve(COMPLEX[] vc, COMPLEX offset, COMPLEX[] xc) throws Exception;
	public abstract void solve(double[] vc_r, double[] vc_i, double offset_r, double offset_i, double [] xc_r, double [] xc_i) throws Exception;

} // class AbstractMatrix 
