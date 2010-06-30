
/*
 * File QMatrix.java
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
/*
 *  Qmatrix.cpp
 *  SingleSiteSorter
 *
 *  Created by David Bryant on 12/03/09.
 *  Copyright 2009 David Bryant
 *
 
 
 The matrix Q^t is block upper-bidiagonal. 
 On the diagonal are matrices M_n,  n=1...N
 
 M_n is a (n+1)x(n+1) tridiagonal matrix indexed by r=0..n
 
 M(r,r+1) = (n-r)v.    for r<n
 M(r,r-1) = ru     for r>=0
 M(r,r) = -n(n-1)/2 gamma - (n-r)v - ru + offset = K + rv - ru   
 
 where K = -n(n-1)/2 gamma - nv and all other entries are 0.
 
 To the right (for n=1,...,N-1) are the matrices R_n.
 R_n is an (n+1) x (n+2) matrix. For r=0...n we have
 
 R_n(r,r) = (n-r)(n+1)/2 * gamma
 R_n(r,r+1) = r(n+1)/2*gamma
 
 and all other entries are 0.
 */
package snap.matrix;

import java.util.Arrays;

import snap.likelihood.COMPLEX;



public class QMatrix extends AbstractMatrix {
		private	double u,v,gamma;
		private int N;
		public QMatrix(int _N, double _u, double _v, double _gamma) {
		 N = _N; 
		 u = _u;
		 v = _v;
		 gamma = _gamma;
		}

			/*
			 Return the dimensions of the matrix
			 2+3+....+(N+1)
			 */
		public int getNrOfRows() {return ((N+1)*(N+1) + N - 1)/2;}
		public int getNrOfCols() {return ((N+1)*(N+1) + N - 1)/2;}
			
			/*
			 Return the infinity norm of the matrix 
			 */
		public double infNorm() {
				//Row (n,r) has entries u(r+1), v(n-r+1), (n-1-r)n/2 g, (r-1)n/2 g, - n(n-1)/2 g - (n-r)v -ru
				//so \sum_j |M_ij| = (2u - 2v)r + 2vn + gamma n^2 for n<N and (2u-2v)r + 2vn + 1/2 gamma n(n-1) for n=N
				//if (u>v) take r = n, giving a function that is increasing in n. So max row is (N,N) and norm is max(2u(N-1) + gamma (N-1)^2, 2u(N) + gamma N(N-1)/2)
				//if (u \leq v) take r=0, giving a function that is increasing in n. So max row is (N,0) and norm is max(2v(N-1) + gamma (N-1)^2, 2v(N) + gamma N(N-1)/2)
				// if u=v then any (N,r) is fine.
				if (u>v)
					return Math.max(2.0*u*(N-1) + gamma*(N-1)*(N-1), 2.0*u*(N) + gamma*(N)*(N-1)/2.0);
				else
					return  Math.max(2.0*v*(N-1) + gamma*(N-1)*(N-1), 2.0*v*(N) + gamma*(N)*(N-1)/2.0);
			}			
			
		public double trace() {
				return -gamma*(N-1)*N*(N+1)*(N+2)/8 - N*(N+1)*(N+2)*(u+v)/6;
			}

			/*
			 Return the product of the matrix with a vector of reals.
			 x and Ax are 1-based vectors
			 */
			public void multiply(double[] x, double[] Ax) {
				/* The vector v is indexed v[1], v[2], v[3],... = (1,0), (1,1), (2,0),.... */
				/*Imagine we had partitioned x into blocks x = [x_1 x_2 x_3 .... x_N]'
				 where x_n has (n+1) entries. Then 
				 
				 Q^t x = [y_1 y_2 .... y_N]'
				 
				 where 
				 
				 y_n = M_n x_n + R_n x_{n+1}    for n<N
				 y_N = M_N x_N*/
				//Ax = new double[x.length];
				
				int index = 1;
				for(int n=1;n<=N;n++) {
					//Evaluate yn = M_n x_n + R_n x_(n+1) 
					// Note that x_(n+1) = x[index + n + 1: index + n + 1 + n+1]
					
					double sum = 0.0;
					sum +=  (- (gamma*(n*(n-1.0)))/2.0 - v*n)*x[index];
					sum+= n*v*x[index+1];
					
					//At this point, sum = (M_n x_n) [r]
					
					if (n<N) {
						sum+= (n*(n+1.0)/2.0)*gamma*x[index+n+1];
					}
					Ax[index] = sum;
					index++;
					
					for(int r=1;r<n;r++) {
						//Q(n,r+1)(n,r) = (n-r)v   r<n
						//Q(n,r-1)(n,r) = ru
						//Q(n+1,r)(n,r) = (n-r)(n+1)/2 * gamma
						//Q(n+1,r+1)(n,r) = r(n+1)/2 * gamma
						//Q(n,r)(n,r) = - n(n-1)/2*gamma -(n-r)*v - ru.
						
						sum= r*u*x[index-1];
						sum +=  (- (gamma*(n*(n-1.0)))/2.0 - v*(n-r) - u*r)*x[index];
						if (r<n)
							sum+= (n-r)*v*x[index+1];
						
						//At this point, sum = (M_n x_n) [r]
						
						if (n<N) {
							sum+= ((n-r)*(n+1.0)/2.0)*gamma*x[index+n+1];
							sum+=(r*(n+1.0)/2.0)*gamma*x[index+n+2];
						}
						Ax[index] = sum;
						index++;
					}
					// r = n
					sum= n*u*x[index-1];
					sum +=  (- (gamma*(n*(n-1.0)))/2.0 - u*n)*x[index];
				
					//At this point, sum = (M_n x_n) [r]
					
					if (n<N) {
						sum+=(n*(n+1.0)/2.0)*gamma*x[index+n+2];
					}
					Ax[index] = sum;
					index++;
				}
			}

			public void multiplyOrig(double[] x, double[] Ax) {
				/* The vector v is indexed v[1], v[2], v[3],... = (1,0), (1,1), (2,0),.... */
				/*Imagine we had partitioned x into blocks x = [x_1 x_2 x_3 .... x_N]'
				 where x_n has (n+1) entries. Then 
				 
				 Q^t x = [y_1 y_2 .... y_N]'
				 
				 where 
				 
				 y_n = M_n x_n + R_n x_{n+1}    for n<N
				 y_N = M_N x_N*/
				//Ax = new double[x.length];
				
				int index = 1;
				for(int n=1;n<=N;n++) {
					//Evaluate yn = M_n x_n + R_n x_(n+1) 
					// Note that x_(n+1) = x[index + n + 1: index + n + 1 + n+1]
					
					for(int r=0;r<=n;r++) {
						//Q(n,r+1)(n,r) = (n-r)v   r<n
						//Q(n,r-1)(n,r) = ru
						//Q(n+1,r)(n,r) = (n-r)(n+1)/2 * gamma
						//Q(n+1,r+1)(n,r) = r(n+1)/2 * gamma
						//Q(n,r)(n,r) = - n(n-1)/2*gamma -(n-r)*v - ru.
						
						double sum = 0.0;
						if (r>0)
							sum+= r*u*x[index-1];
						sum +=  (- (gamma*(n*(n-1.0)))/2.0 - v*(n-r) - u*r)*x[index];
						if (r<n)
							sum+= (n-r)*v*x[index+1];
						
						//At this point, sum = (M_n x_n) [r]
						
						if (n<N) {
							sum+= ((n-r)*(n+1.0)/2.0)*gamma*x[index+n+1];
							sum+=(r*(n+1.0)/2.0)*gamma*x[index+n+2];
						}
						Ax[index] = sum;
						index++;
					}
				}
			}

			//void multiply(const vector<COMPLEX>& x, vector<COMPLEX>& Ax) const;
			//void solve(const vector<double>& y, double offset, vector<double>& x) const ;
			//void solve(const vector<COMPLEX>& y, COMPLEX offset, vector<COMPLEX>& x) const;

			/**
			 We have a central matrix M with 
			 M(r,r+1) = (n-r)v. 
			 M(r,r-1) = ru
			 M(r,r) = -n(n-1)/2 gamma - (n-r)v - ru + offset = K + rv - ru
			 In this routine we replace x with the solution of Mx = y.
			 **/

			void checkMrr(double Mrr)  throws Exception {
				if (Mrr == 0.0)
					throw new Exception("Error in matrix solve");
			}

			/*
			 The same as before, except now solving the transposed system M'x = y.
			 
			 //TODO: Make this code more streamlined by combining options.
			 
			 M'(r+1,r) = (n-r)v.  so  M'(r,r-1) = (n-r+1)v
			 M'(r-1,r) = ru   so M'(r,r+1) = (r+1)u
			 M'(r,r) = -n(n-1)/2 gamma - (n-r)v - ru + offset = K + rv - ru
			 
			 
			 */

			double [] solveCentralBlockTransposed(double [] y, double offset, int n, 
					double u, double v, double gamma, boolean printdebug /*= false*/) throws Exception  {
				
				if (printdebug) {
					System.out.print("ylocal = [");
					for(int i=0;i<y.length;i++) {
						System.out.print(y[i]+" ");
					}
					System.out.println("]';");
				}
				
				
				double [] x = new double[n+1];
				
				double K = -(gamma*(n*(n-1.0)))/2.0 - n*v + offset;
				
				
				if (u==0.0) {
					//Lower bidagonal.
					double Mrr = K;
					checkMrr(Mrr);
					x[0] = y[0] / Mrr;
					for(int r=1;r<=n;r++) {
						Mrr = K+r*(v-u);
						checkMrr(Mrr);
						x[r] = (y[r] - ((n-r+1.0)*v)*x[r-1])/Mrr;	
					}
				} //Upper bidiagonal
				else if (v==0.0) {
					double Mrr = K + n*(v-u);
					checkMrr(Mrr);
					x[n] = y[n] / Mrr;
					for(int r=n-1;r>=0;r--) {
						Mrr = (K+r*(v-u));
						x[r] = (y[r] - ((r+1.0)*u)*x[r+1])/Mrr;
					}
				}
				else {
					double[] d = new double[n+1];
					double[] e = new double[n+1];
					d[0] = K;
					e[0] = y[0];
					for(int r=1;r<=n;r++) {
						//zero out lower triangular
						checkMrr(d[r-1]);
						double m =((n-r+1.0)*v)/d[r-1]; //m = M'(r,r-1)/M'(r-1,r-1) 
						d[r] = K+r*(v-u) - m*r*u; //d[r] = M'(r,r) - m*M'(r-1,r)
						e[r] = y[r] - m*e[r-1]; //e[r] = y[r] - m*e[r-1]
					}
					
					if (printdebug) {
						System.out.print("d = [");
						for(int i=0;i<d.length;i++) {
							System.out.print(d[i]+" ");
						}
						System.out.print("]';"+"\n");
						
						System.out.print("e = [");
						for(int i=0;i<e.length;i++) {
							System.out.print(e[i]+" ");
						}
						System.out.print("]';"+"\n");
					}
					
					//now solve the upper biadiagonal. diagonal is d, upper is same as M
					x[n] = e[n]/d[n];
					for(int r=n-1;r>=0;r--) {
						checkMrr(d[r]);
						x[r] = (e[r] - (r+1.0)*u*x[r+1])/d[r]; //x[r] = (e[r] - M'(r,r+1)*x[r+1])/d[r]
					}
				}
				
				if (printdebug) {
					System.out.print("xlocal = ");
					for(int i=0;i<x.length;i++) {
						System.out.print(x[i]+" ");
					}
					System.out.println("]';");
					
					double diff = 0.0;
					for(int r=0;r<=n;r++) {
						double sum = 0.0;
						if (r>0)
							sum+= (n+1.0-r)*v*x[r-1];
						sum +=  (- (gamma*(n*(n-1.0)))/2.0 - v*(n-r) - u*r)*x[r];
						if (r<n)
							sum+= (r+1.0)*u*x[r+1];
						
						System.err.println(sum);
						
						diff = Math.max(diff,Math.abs(sum-y[r])); //Check that it solves the system
						
					}
					if (diff>1e-10) {
						System.err.println("Error in solve");
					}
					
				}
				return x;
			}

			public double [] findOrthogonalVector(boolean printd) throws Exception { 
				//Finds non-zero vector x such that x'Q' = 0
				/* Want to find non-zero x such that x' Q' = 0.
				 Let x = [x_1,x_2,...,x_n]
				 We first solve x_1 ' M_1 = 0 manually.
				 Then use the recursion
				 
				 x_{n-1} R_{n-1} + x_n M_n = 0  for n=2,3,...,N
				 
				 */
				int nrows = getNrOfRows();
				//int ncols = getNrOfCols();
		
				double [] x = new double[nrows + 1];		
				//vector<double>::iterator xptr = x.begin();
				double [] xn = new double[N+1]; 
				double [] yn = new double[N+1];
				
				//First solve the n=1 case.
				//M_1 = [-v   v; -u u] which has solution [u  v]
				//xn.resize(2);
				xn[0] = u; xn[1]=v;
				x[1] = u; x[2] = v;
				//advance(xptr, 3); //Now xptr points to x[3]
				int xptr = 3;
				
				//Now the rest.
				for(int n =2; n<=N; n++) {
					
					/***
					 We use the recursion 
					 x_{n-1}' R_{n-1} + x_n' M_n = 0.
					 or equivalently
					 M_n' x_n = - R_{n-1}' x_{n-1}
					 
					 
					 First compute y_n = - R_{n-1}' x_{n-1}  
					 
					 Second, solve M_n' x_n = y_n
					 
					 ****/
					
					//yn.resize(n+1);
					
					if (printd) {
						System.err.print("xn = [");
						for(int r=0;r<=n-1;r++)
							System.err.print(xn[r]+" ");
						System.err.println("];");
					}
					
					yn[0] = - ((gamma*(n-1.0)*n)/2.0)*xn[0];  //yn[0] = - R_{n-1}(0,0)xn[0]
					for(int r=1;r<n;r++) {
						yn[r] = - ( (gamma*(r-1.0)*n)/2.0 )*xn[r-1] - ( (gamma*(n-1.0-r)*n)/2.0 )*xn[r];
					}		
					yn[n] = - ( (gamma*(n-1.0)*n)/2.0 )*xn[n-1];	
					
					if (printd) {
						System.err.print("yn = [");
						for(int r=0;r<=n;r++)
							System.err.print(yn[r]+" \t");
						System.err.println("];");
					}
					
					
					xn = solveCentralBlockTransposed(yn,0,n,u,v,gamma,false);
					
					//xptr = copy(xn.begin(),xn.end(),xptr);
					for (int i = 0; i <  xn.length; i++) {
						x[xptr++] = xn[i];
					}
					
					if (printd) {
						System.err.print("xn2 = [");
						for(int r=0;r<=n;r++)
							System.err.print(xn[r]+" \t");
						System.err.println("]");
					}
				}
				return x;
			}

			@Override
			public void solve(COMPLEX[] y, COMPLEX offset, COMPLEX[] x) throws Exception {
				/* Suppose that y = [y1',y2',...,yn']' as above. We solve (Q^t + offset*I) x = y.
				 This gives us the equations
				 
				 M_n x_n + R_n x_{n+1} + offset*x_n  = y_n  for n=1,2,...,N - 1
				 and
				 M_N x_N + offset x_N = y_N
				 
				 These can be solved in reverse, starting with (M_N + offset * I)x_N = y_N
				 and then
				 (M_n + offset I) x_n = y_n - R_n x_{n+1}  n = N-1,N_2,...,1
				 
				 The solution of the tridiaongal system (M_n + offset I) x_n = z 
				 is done by the routine solveCentralBlock. The computation of Rn x_{n+1} is done by MultiplyUpperBlock.
				 */
				
				//boolean CHECK_SOLVE = false;

				if (x.length != y.length) {
					throw new Exception ("Expected x & y to be of same length");
				}
				for (int i = 0; i < x.length; i++) {
					x[i].m_fRe = 0;
					x[i].m_fIm = 0;
				}
				
				COMPLEX[] xn = new COMPLEX[N + 1];
				COMPLEX[] yn = new COMPLEX[N + 1];
				for (int i = 0; i < N + 1; i++) {
					xn[i] = new COMPLEX();
					yn[i] = new COMPLEX();
				}
				
				
				//vector<COMPLEX>::iterator xptr = x.end();
				int xptr = x.length - 1 - N;
				//vector<COMPLEX>::const_iterator yptr = y.end();
				int yptr = y.length - 1 - N;
				
				//Solve (M_N + offset * I)x_N = y_N and copy solution into xn.
				//copy(yptr,y.end(),yn.begin());
				for (int i = 0; i < yn.length; i++) {
					yn[i].m_fRe = y[yptr + i].m_fRe;
					yn[i].m_fIm = y[yptr + i].m_fIm;
				}
				//cout.precision(20);
				solveCentralBlock(yn,offset,N,u,v,gamma,xn);
//				if (CHECK_SOLVE) {
//					vector<COMPLEX> yn2(yn.size());
//					multiplyCentralBlock(xn, offset, N, u, v, gamma, yn2);
//					double diff1 = euclideanNorm(yn, yn2);
//					if (diff1 > 1e-14) {
//						cerr<<"NUmerical issues arising!"<<endl;
//					
//						vector<COMPLEX> delta(xn.size());
//						for(uint i=0;i<=N;i++)
//							yn2[i] = yn[i] - yn2[i];
//						solveCentralBlock(yn2,offset,N,u,v,gamma,delta,true);
//						for(uint i=0;i<=N;i++)
//							xn[i]+=delta[i];
//						multiplyCentralBlock(xn, offset, N, u, v, gamma, yn2);
//						cerr<<"Second diff = "<<euclideanNorm(yn, yn2)<<endl;
//						cerr<<endl;
//					
//					}
//				}
				
				
				//advance(xptr,-((int)N+1));
				//copy(xn.begin(),xn.end(),xptr);
				for (int i = 0; i < xn.length; i++) {
					x[xptr+i].m_fRe = xn[i].m_fRe;
					x[xptr+i].m_fIm = xn[i].m_fIm;
				}
				
				//Solve for the rest
				for(int n=N-1;n>=1;n--) {
					//advance(xptr,-((int)n+1)); //Point xptr to the beginning of x_n
					xptr = xptr - (n+1);
					multiplyUpperBlock(xn,n,gamma,yn); //Compute yn = R_n x_{n+1}
					//advance(yptr,-((int)n+1)); //Backwind to beginning of y_n.
					yptr = yptr - (n+1);
					for(int r=0;r<=n;r++) {
						//yn[r] = (*yptr) - yn[r]; //Put yn = y_n - R_n x_{n+1}
						yn[r].m_fRe = y[yptr + r].m_fRe - yn[r].m_fRe; 
						yn[r].m_fIm = y[yptr + r].m_fIm - yn[r].m_fIm; 
					}
					//advance(yptr,-((int)n+1)); //Backwind to beginning of y_n.
					
					solveCentralBlock(yn,offset,n,u,v,gamma,xn);
					
//					if (CHECK_SOLVE) {
//						vector<COMPLEX> yn2(yn.size());
//						multiplyCentralBlock(xn, offset, n, u, v, gamma, yn2);
//						double diff1 = euclideanNorm(yn, yn2);
//						if (diff1 > 1e-14) {
//							cerr<<"NUmerical issues arising!"<<endl;
//							
//							vector<COMPLEX> delta(xn.size());
//							for(uint i=0;i<=N;i++)
//								yn2[i] = yn[i] - yn2[i];
//							solveCentralBlock(yn2,offset,n,u,v,gamma,delta,true);
//							for(uint i=0;i<=N;i++)
//								xn[i]+=delta[i];
//							multiplyCentralBlock(xn, offset, n, u, v, gamma, yn2);
//							cerr<<"Second diff = "<<euclideanNorm(yn, yn2)<<endl;
//							cerr<<endl;
//							
//						}
//					}
					
					
					//copy(xn.begin(),xn.end(),xptr);
					for (int i = 0; i <= n; i++) {
						x[xptr + i].m_fRe = xn[i].m_fRe;
						x[xptr + i].m_fIm = xn[i].m_fIm;
					}
				}
			}

			void multiplyUpperBlock(COMPLEX [] x, int n, double gamma, COMPLEX [] y) throws Exception  {
				//y.resize(n+1);
				if (y.length < n+1) {
					throw new Exception("Expected y to be of length at least " + (n+1) + " instead of " + y.length);
				}
				for(int r=0;r<=n;r++) { 
				  //y[r] = (gamma*(double)(n-r)*(n+1.0)/2.0) * x[r] + (gamma*(double)r*(n+1.0)/2.0)*x[r+1];
				  y[r].muladd((gamma*(double)(n-r)*(n+1.0)/2.0), x[r], (gamma*(double)r*(n+1.0)/2.0),x[r+1]);
				}
			}
			void checkMrr(COMPLEX Mrr)  throws Exception {
				  if (Math.abs(Mrr.m_fRe) < 1e-20 && Math.abs(Mrr.m_fIm) < 1e-20 )
				       throw new Exception("Error in matrix solve");
			}
			void solveCentralBlock(COMPLEX[] y, COMPLEX offset, int n, double u, double v, double gamma, COMPLEX[] x) throws Exception  {
				//boolean printdebug = false;
				
				  COMPLEX K = new COMPLEX((double)(-gamma*(double)n*(n-1.0)/2.0 - n*v) + offset.m_fRe, offset.m_fIm);
				  COMPLEX tmp = new COMPLEX();
//					if (printdebug) {
//						cout<<"ylocal = [";
//						for(uint i=0;i<y.size();i++) {
//							cout<<real(y[i])<<" + "<<imag(y[i])<<"i ";
//						}
//						cout<<"]';"<<endl;
//						
//						cout<<"M = [\n";
//						for(uint r=0;r<=n;r++) {
//							for(uint i=0;i<r-1;i++)
//								cout<<"0 ";
//							if (r>0)
//								cout<<r*u<<" ";
//							cout<<real(K - (ldouble)((double)r*(v-u)))<<" + "<<imag(K-(ldouble)((double)r*(v-u)))<<"i ";
//							if (r<n)
//								cout<<(n-r)*u<<" ";
//							for(uint i=r+2;i<=n;i++)
//								cout<<"0 ";
//							if (r<n)
//								cout<<";\n";
//							else
//								cout<<"];"<<endl;
//						}
//						
//					}
					
					
					//x.resize(n+1);
					if (x.length < n+1) {
						throw new Exception("Expected x to be of length at least " + (n+1) + " instead of " + x.length);
					}
					//cout<<"n = "<<n<<" gamma = "<<gamma<<" v = "<<v<<" offset = "<<offset<<endl;
					
					//cout<<"K = "<<K<<endl;
					
					COMPLEX Mrr = new COMPLEX();
					
					if (v==0.0) {
						//Lower bidagonal.
						Mrr = K;
						checkMrr(Mrr);
						//x[0] = y[0] / Mrr;
						x[0].divide(y[0], Mrr);
						for(int r=1;r<=n;r++) {
						    //Mrr = K+(double)r*(v-u);
						    Mrr.m_fRe = K.m_fRe + (double)r*(v-u);
						    Mrr.m_fIm = K.m_fIm;
							checkMrr(Mrr);
							//x[r] = (y[r] - x[r-1]*(double)(u*r))/Mrr;
							tmp.m_fRe = y[r].m_fRe - x[r-1].m_fRe*(double)(u*r);
							tmp.m_fIm = y[r].m_fIm - x[r-1].m_fIm*(double)(u*r);
							x[r].divide(tmp, Mrr);
						}
					} //Upper bidiagonal
					else if (u==0.0) {
					    //Mrr = K + (double)n*(v-u);
					    Mrr.m_fRe = K.m_fRe + (double)n*(v-u);
					    Mrr.m_fIm = K.m_fIm;
						checkMrr(Mrr);
						//x[n] = y[n] / Mrr;
						x[n].divide(y[n], Mrr);
						for(int r=n-1;r>=0;r--) {
						    //Mrr = (K+(double)((v-u)*r));
						    Mrr.m_fRe = K.m_fRe + (double)r*(v-u);
						    Mrr.m_fIm = K.m_fIm;
						    //x[r] = (y[r] - ((double)(n-r)*v*x[r+1]))/Mrr;
						    tmp.m_fRe = y[r].m_fRe - ((double)(n-r)*v*x[r+1].m_fRe);
						    tmp.m_fIm = y[r].m_fIm - ((double)(n-r)*v*x[r+1].m_fIm);
						    x[r].divide(tmp, Mrr);
						}
					}
					else {
						COMPLEX [] d = new COMPLEX[n+1];
						for (int i = 0; i < n+1; i++) {
							d[i] = new COMPLEX();
						}
						COMPLEX [] e = new COMPLEX[n+1];
						for (int i = 0; i < n+1; i++) {
							e[i] = new COMPLEX();
						}
						d[0].m_fRe = K.m_fRe;
						d[0].m_fIm = K.m_fIm;
						e[0].m_fRe = y[0].m_fRe;
						e[0].m_fIm = y[0].m_fIm;
						for(int r=1;r<=n;r++) {
							//zero out lower triangular
							checkMrr(d[r-1]);
							//COMPLEX m = (COMPLEX)(r*u)/d[r-1]; //m is M(r,r-1) / M(r-1,r-1)
						    tmp.m_fRe = r*u;
						    tmp.m_fIm = 0.0;
						    COMPLEX m = new COMPLEX();
						    m.divide(tmp, d[r-1]);
						    //d[r] = K+(COMPLEX)(r*(v-u)) - m*(((double)n-r+1.0)*v); //Subtract m* row r-1 from row r.
						    d[r].m_fRe = K.m_fRe+r*(v-u) - m.m_fRe*(n-r+1.0)*v;
						    d[r].m_fIm = K.m_fIm+        - m.m_fIm*(n-r+1.0)*v;
							//e[r] = y[r] - m*e[r-1];
							tmp.mul(m, e[r-1]);
							e[r].m_fRe = y[r].m_fRe - tmp.m_fRe;
							e[r].m_fIm = y[r].m_fIm - tmp.m_fIm;
						}
						
//						if (printdebug) {
//							cout<<"d = ";
//							for(uint i=0;i<d.size();i++) {
//								cout<<d[i]<<" ";
//							}
//							cout<<"]';"<<endl;
//							
//							cout<<"e = ";
//							for(uint i=0;i<e.size();i++) {
//								cout<<e[i]<<" ";
//							}
//							cout<<"]';"<<endl;
//						}
						
						//now solve the upper biadiagonal. diagonal is d, upper is same as M
						//x[n] = e[n]/d[n];
						x[n].divide(e[n], d[n]);
						for(int r=n-1;r>=0;r--) {
							checkMrr(d[r]);
							//x[r] = (e[r] - (double)(n-r)*v*x[r+1])/d[r];
						    tmp.m_fRe = (e[r].m_fRe - (double)(n-r)*v*x[r+1].m_fRe);
						    tmp.m_fIm = (e[r].m_fIm - (double)(n-r)*v*x[r+1].m_fIm);
						    x[r].divide(tmp, d[r]);
						}
					}
					
//					if (printdebug) {
//						cout<<"xlocal = [";
//						for(uint i=0;i<x.size();i++) {
//							cout<<real(x[i])<<" + "<<imag(x[i])<<"i ";
//						}
//						cout<<"]';"<<endl;
//					}
					
				}

	void checkMrr(double Mrr_r,double Mrr_i)  throws Exception {
		  if (Math.abs(Mrr_r) < 1e-20 && Math.abs(Mrr_i) < 1e-20 )
		       throw new Exception("Error in matrix solve");
	}
	void multiplyUpperBlock(double [] x_r, double [] x_i, int n, double gamma, double [] y_r, double [] y_i) throws Exception  {
		//y.resize(n+1);
		if (y_r.length < n+1 || y_i.length < n+1) {
			throw new Exception("Expected y to be of length at least " + (n+1) + " instead of " + Math.min(y_r.length, y_i.length));
		}
		for(int r=0;r<=n;r++) { 
		  //y[r] = (gamma*(double)(n-r)*(n+1.0)/2.0) * x[r] + (gamma*(double)r*(n+1.0)/2.0)*x[r+1];
		  //y[r].muladd((gamma*(double)(n-r)*(n+1.0)/2.0), x[r], (gamma*(double)r*(n+1.0)/2.0),x[r+1]);
		  y_r[r] = gamma*(double)(n-r)*(n+1.0)/2.0 * x_r[r] + (gamma*(double)r*(n+1.0)/2.0)*x_r[r+1];
		  y_i[r] = gamma*(double)(n-r)*(n+1.0)/2.0 * x_i[r] + (gamma*(double)r*(n+1.0)/2.0)*x_i[r+1];
		}
	}
	void solveCentralBlock(double[] y_r, double[]y_i, double offset_r, double offset_i, int n, double u, double v, double gamma, double[] x_r, double [] x_i) throws Exception  {
		//boolean printdebug = false;
		
		  double K_r = (double)(-gamma*(double)n*(n-1.0)/2.0 - n*v) + offset_r;
		  double K_i = offset_i;
		  double tmp_r;
		  double tmp_i;
			//x.resize(n+1);
//			if (x.length < n+1) {
//				throw new Exception("Expected x to be of length at least " + (n+1) + " instead of " + x.length);
//			}
			
			double Mrr_r;
			double Mrr_i;
			
			if (v==0.0) {
				//Lower bidagonal.
				Mrr_r = K_r;
				Mrr_i = K_i;
				checkMrr(Mrr_r, Mrr_i);
				//x[0] = y[0] / Mrr;
				//x[0].divide(y[0], Mrr);
				double f = Mrr_r * Mrr_r + Mrr_i * Mrr_i; 
				x_r[0] = (y_r[0] * Mrr_r + y_i[0] * Mrr_i) / f;
				x_i[0] = (y_i[0] * Mrr_r - y_r[0] * Mrr_i) / f;
				
				
				for(int r=1;r<=n;r++) {
				    //Mrr = K+(double)r*(v-u);
				    Mrr_r = K_r + (double)r*(v-u);
				    Mrr_i = K_i;
					checkMrr(Mrr_r, Mrr_i);
					//x[r] = (y[r] - x[r-1]*(double)(u*r))/Mrr;
					tmp_r = y_r[r] - x_r[r-1]*(double)(u*r);
					tmp_i = y_i[r] - x_i[r-1]*(double)(u*r);
					//x[r].divide(tmp, Mrr);
					f = Mrr_r * Mrr_r + Mrr_i * Mrr_i; 
					x_r[r] = (tmp_r * Mrr_r + tmp_i * Mrr_i) / f;
					x_i[r] = (tmp_i * Mrr_r - tmp_r * Mrr_i) / f;
				}
			} //Upper bidiagonal
			else if (u==0.0) {
			    //Mrr = K + (double)n*(v-u);
			    Mrr_r = K_r + (double)n*(v-u);
			    Mrr_i = K_i;
				checkMrr(Mrr_r, Mrr_i);
				//x[n] = y[n] / Mrr;
				//x[n].divide(y[n], Mrr);
				double f = Mrr_r * Mrr_r + Mrr_i * Mrr_i; 
				x_r[n] = (y_r[n] * Mrr_r + y_i[n] * Mrr_i) / f;
				x_i[n] = (y_i[n] * Mrr_r - y_r[n] * Mrr_i) / f;
				for(int r=n-1;r>=0;r--) {
				    //Mrr = (K+(double)((v-u)*r));
				    Mrr_r = K_r + (double)r*(v-u);
				    Mrr_i = K_i;
				    //x[r] = (y[r] - ((double)(n-r)*v*x[r+1]))/Mrr;
				    tmp_r = y_r[r] - ((double)(n-r)*v*x_r[r+1]);
				    tmp_i = y_i[r] - ((double)(n-r)*v*x_i[r+1]);
				    //x[r].divide(tmp, Mrr);
					f = Mrr_r * Mrr_r + Mrr_i * Mrr_i; 
					x_r[r] = (tmp_r * Mrr_r + tmp_i * Mrr_i) / f;
					x_i[r] = (tmp_i * Mrr_r - tmp_r * Mrr_i) / f;
				}
			}
			else {
				double [] d_r = new double[n+1];
				double [] d_i = new double[n+1];
				double [] e_r = new double[n+1];
				double [] e_i = new double[n+1];
				d_r[0] = K_r;
				d_i[0] = K_i;
				e_r[0] = y_r[0];
				e_i[0] = y_i[0];
				for(int r=1;r<=n;r++) {
					//zero out lower triangular
					checkMrr(d_r[r-1], d_i[r-1]);
					//COMPLEX m = (COMPLEX)(r*u)/d[r-1]; //m is M(r,r-1) / M(r-1,r-1)
				    tmp_r = r*u;
				    tmp_i = 0.0;
				    //COMPLEX m = new COMPLEX();
				    //m.divide(tmp, d[r-1]);
					double f = d_r[r-1] * d_r[r-1] + d_i[r-1] * d_i[r-1]; 
					double m_r = (tmp_r * d_r[r-1] + tmp_i * d_i[r-1]) / f;
					double m_i = (tmp_i * d_r[r-1] - tmp_r * d_i[r-1]) / f;
				    //d[r] = K+(COMPLEX)(r*(v-u)) - m*(((double)n-r+1.0)*v); //Subtract m* row r-1 from row r.
				    d_r[r] = K_r + r*(v-u) - m_r*(n-r+1.0)*v;
				    d_i[r] = K_i +         - m_i*(n-r+1.0)*v;
					//e[r] = y[r] - m*e[r-1];
					//tmp.mul(m, e[r-1]);
					tmp_r = m_r * e_r[r-1] - m_i * e_i[r-1];
					tmp_i = m_i * e_r[r-1] + m_r * e_i[r-1];
					e_r[r] = y_r[r] - tmp_r;
					e_i[r] = y_i[r] - tmp_i;
				}
				
				
				//now solve the upper biadiagonal. diagonal is d, upper is same as M
				//x[n] = e[n]/d[n];
				//x[n].divide(e[n], d[n]);
				double f = d_r[n] * d_r[n] + d_i[n] * d_i[n]; 
				x_r[n] = (e_r[n] * d_r[n] + e_i[n] * d_i[n]) / f;
				x_i[n] = (e_i[n] * d_r[n] - e_r[n] * d_i[n]) / f;
				for(int r=n-1;r>=0;r--) {
					checkMrr(d_r[r], d_i[r]);
					//x[r] = (e[r] - (double)(n-r)*v*x[r+1])/d[r];
				    tmp_r = (e_r[r] - (double)(n-r)*v*x_r[r+1]);
				    tmp_i = (e_i[r] - (double)(n-r)*v*x_i[r+1]);
				    //x[r].divide(tmp, d[r]);
					f = d_r[r] * d_r[r] + d_i[r] * d_i[r]; 
					x_r[r] = (tmp_r * d_r[r] + tmp_i * d_i[r]) / f;
					x_i[r] = (tmp_i * d_r[r] - tmp_r * d_i[r]) / f;
				}
			}
			
		}

	public void solve(double[] y_r, double[] y_i, double offset_r, double offset_i, double [] x_r, double [] x_i) throws Exception {
			/* Suppose that y = [y1',y2',...,yn']' as above. We solve (Q^t + offset*I) x = y.
			 This gives us the equations
			 
			 M_n x_n + R_n x_{n+1} + offset*x_n  = y_n  for n=1,2,...,N - 1
			 and
			 M_N x_N + offset x_N = y_N
			 
			 These can be solved in reverse, starting with (M_N + offset * I)x_N = y_N
			 and then
			 (M_n + offset I) x_n = y_n - R_n x_{n+1}  n = N-1,N_2,...,1
			 
			 The solution of the tridiaongal system (M_n + offset I) x_n = z 
			 is done by the routine solveCentralBlock. The computation of Rn x_{n+1} is done by MultiplyUpperBlock.
			 */
			
			//boolean CHECK_SOLVE = false;

//			if (x_r.length != y_r.length) {
//				throw new Exception ("Expected x & y to be of same length");
//			}
			Arrays.fill(x_r, 0.0);
			Arrays.fill(x_i, 0.0);
			
			double[] xn_r = new double[N + 1];
			double[] xn_i = new double[N + 1];
			double[] yn_r = new double[N + 1];
			double[] yn_i = new double[N + 1];
			
			
			//vector<COMPLEX>::iterator xptr = x.end();
			int xptr = x_r.length - 1 - N;
			//vector<COMPLEX>::const_iterator yptr = y.end();
			int yptr = y_r.length - 1 - N;
			
			//Solve (M_N + offset * I)x_N = y_N and copy solution into xn.
			//copy(yptr,y.end(),yn.begin());
			for (int i = 0; i < yn_r.length; i++) {
				yn_r[i] = y_r[yptr + i];
				yn_i[i] = y_i[yptr + i];
			}
			//cout.precision(20);
			solveCentralBlock(yn_r, yn_i, offset_r, offset_i ,N,u,v,gamma, xn_r, xn_i);
			
			
			//advance(xptr,-((int)N+1));
			//copy(xn.begin(),xn.end(),xptr);
			for (int i = 0; i < xn_r.length; i++) {
				x_r[xptr+i] = xn_r[i];
				x_i[xptr+i] = xn_i[i];
			}
			
			//Solve for the rest
			for(int n=N-1;n>=1;n--) {
				//advance(xptr,-((int)n+1)); //Point xptr to the beginning of x_n
				xptr = xptr - (n+1);
				multiplyUpperBlock(xn_r, xn_i,n,gamma, yn_r, yn_i); //Compute yn = R_n x_{n+1}
				//advance(yptr,-((int)n+1)); //Backwind to beginning of y_n.
				yptr = yptr - (n+1);
				for(int r=0;r<=n;r++) {
					//yn[r] = (*yptr) - yn[r]; //Put yn = y_n - R_n x_{n+1}
					yn_r[r] = y_r[yptr + r] - yn_r[r]; 
					yn_i[r] = y_i[yptr + r] - yn_i[r]; 
				}
				//advance(yptr,-((int)n+1)); //Backwind to beginning of y_n.
				
				solveCentralBlock(yn_r, yn_i, offset_r, offset_i,n,u,v,gamma,xn_r, xn_i);
				
				//copy(xn.begin(),xn.end(),xptr);
				for (int i = 0; i <= n; i++) {
					x_r[xptr + i] = xn_r[i];
					x_i[xptr + i] = xn_i[i];
				}
			}
		
	}

}
