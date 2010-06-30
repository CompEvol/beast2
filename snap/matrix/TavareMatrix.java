
/*
 * File TavareMatrix.java
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

import snap.likelihood.COMPLEX;


public class TavareMatrix extends AbstractMatrix {

		private
			int maxN;
		private	double H(int n) {return (double)n*((double)n-1.0)/2.0;}
		public
			TavareMatrix(int nmax) {maxN = nmax;}
			//void getSize(int n, int& m) const {n=m=maxN;}
		public int getNrOfRows() {return maxN;}
		public int getNrOfCols() {return maxN;}
		public double infNorm() {return (double)maxN*((double)maxN - 1.0);}

		public double trace() {
				return (maxN*(1-maxN*maxN))/6;
			}
/*
			void multiply(Vector<Double> v, Vector<Double> Av) {
				 Av.resize(maxN+1);
				 for(int n=1;n<maxN;n++)
					 Av[n] = -H(n)*v[n] + H(n+1)*v[n+1];
				Av[maxN] = -H(maxN)*v[maxN];

				Av[0] = 0.0;
			 }
*/			 
			public void multiply(double[] v, double[] Av) {
				 //Av.resize(maxN+1);
				 for(int n=1;n<maxN;n++)
					 Av[n] = -H(n)*v[n] + H(n+1)*v[n+1];
				Av[maxN] = -H(maxN)*v[maxN];

				Av[0] = 0.0;
			 }
/*
			 void multiply(Vector<COMPLEX> v, Vector<COMPLEX> Av) {
				 Av.resize(maxN+1);
				 for(int n=1;n<maxN;n++)
					 Av[n] = -H(n)*v[n] + H(n+1)*v[n+1];
				 Av[maxN] = -H(maxN)*v[maxN];

				 Av[0] = 0.0;
			 }
*/
			/*
			void solve(Vector<Double> y, double offset, Vector<Double> x){
				x.resize(maxN+1);
				x[0]=0.0;
				x[maxN] = y[maxN]/(-H(maxN) + offset);
				for(int n=maxN-1;n>=1;n--) {
					x[n] = (y[n] - H(n+1) * x[n+1])/(offset - H(n));
				}
			}
			*/
/*
			void solve(const vector<COMPLEX>& y, COMPLEX offset, vector<COMPLEX>& x) const {
				x.resize(maxN+1);
				x[0]=0.0;
				x[maxN] = y[maxN]/(-H(maxN) + offset);
				for(uint n=maxN-1;n>=1;n--) {
					x[n] = (y[n] - H(n+1) * x[n+1])/(offset - H(n));
				}
			}
*/
			/*
			void print(ostream& os) const {
				os<<"[\n";
				for(uint n=1;n<=maxN;n++) {
					for(uint i=1;i<n-1;i++)
						os<<"0\t";
					if (n>1)
						os<<H(n)<<"\t";
					os<<-H(n)<<"\t";
					for(uint i = n+1;i<=maxN;i++)
						os<<"0\t";
					if (n<maxN)
						os<<";\n";
					else
						os<<"]';"<<endl;
				}
			}
		};
*/
		public void solve(COMPLEX[] vc, COMPLEX offset, COMPLEX[] xc) throws Exception {
			throw new Exception("TavareMatrix::solve not implmeneted yet");
		}
		public void solve(double[] vc_r, double[] vc_i, double offset_r, double offset_i, double [] xc_r, double [] xc_i) throws Exception {
			throw new Exception("TavareMatrix::solve not implmeneted yet");
		}
}