
/*
 * File MatrixExponentiator.java
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
package snap.likelihood;

import java.util.Arrays;

import snap.FMatrix;
import snap.matrix.AbstractMatrix;
import snap.matrix.Array2d;
import snap.matrix.QMatrix;

public class MatrixExponentiator {
//	static {
//		System.loadLibrary("SSS");
//	}

	final static int EXPM_Q = 6;
	/**
	 Solves AX = B using Gauss Elimination with Partial Pivoting
	 Algorithm 3.4.1 in Golub and van Loan, pg 112.

	 Both A and B are overwritten. Upper triangle of A overwritten with U in LU decomposition.
	 B overwritten with X.
	 **/
	private static void partialPivotSolve(Array2d A, Array2d B) throws Exception {
		int n = B.getNrOfRows();
		int m = B.getNrOfCols();

		//Check dimensions
		int nA = A.getNrOfRows();
		int mA = A.getNrOfCols();
		if (nA!=mA || nA != n) {
			throw new Exception("Inconsistent matrix dimensions in partialPivotSolve");
		}

		int [] p = new int[n+1]; //Permutations

		for (int k=1; k<=n-1;k++) {
			//Find max.
			int mu = k;
			double mx = A.at(k,k);
			for (int l=k+1; l<=n;l++)
				if (A.at(l,k) > mx) {
					mx = A.at(l,k);
					mu = l;
				}
			//swap rows
			if (mu>k) {
				double tmp;
				for(int l=k;l<=n;l++) {
					tmp = A.at(k,l); A.set(k,l, A.at(mu,l)); A.set(mu,l, tmp);
				}
			}

			p[k] = mu;
			double A_kk = A.at(k,k);
			if (A_kk != 0.0) {
				//rows = k+1:n;
				//A(rows,k) = A(rows,k)/A(k,k);
				for(int l=k+1;l<=n;l++)
					A.div(l,k, A_kk);
				//A(rows,rows) = A(rows,rows) - A(rows,k)*A(k,rows);

				for(int l1=k+1;l1<=n;l1++) {
					for(int l2 = k+1;l2<=n;l2++)
						A.min(l1,l2, A.at(l1,k)*A.at(k,l2));
				}
			}
		}

		double []  b = new double[n+1];

		for(int i=1;i<=m;i++) {

			for(int k=1;k<=n;k++) //Get ith column of B
				b[k] = B.at(k,i);

			for(int k=1;k<=n-1;k++) {
				double tmp=b[k]; b[k] = b[p[k]]; b[p[k]] = tmp;
				double b_k = b[k];
				for(int l=k+1;l<=n;l++) {
					b[l] -= b_k * A.at(l,k);
				}
			}

			//Solve Ux = y
			b[n] /= A.at(n,n);
			for(int k=n-1;k>=1;k--) {
				double sum = 0.0;
				for(int l=k+1;l<=n;l++)
					sum+=A.at(k,l) * b[l];
				b[k] = (b[k] - sum)/A.at(k,k);
			}

			for(int k=1;k<=n;k++) //Put ith column of B
				B.set(k,i, b[k]);
		}

	} // partialPivotSolve
	/**
	 Solves AX = B using Gauss Elimination with Partial Pivoting
	 Algorithm 3.4.1 in Golub and van Loan, pg 112.

	 Neither A nor B are overwritten, so requires additional memory to version above.
	 **/
//	private static void partialPivotSolveX(Array2d A, Array2d B, Array2d X) throws Exception {
//		X = B.copy();
//		Array2d Acopy = A.copy();
//		partialPivotSolve(Acopy,X);
//	}
	
	
	/**************************************************************************************************/
	/**
	 expm

	 Computes the exponential of a array2d F = exp(A)

	 The array2d has rows and columns indexed 1,2,....,n and must be square.
	 @param A  square matrix
	 @param F exponential matrix returned.
	 **/

	private static void expm(Array2d A, Array2d F) throws Exception {
//		if (false) {
//			ExpQT.expM(A, F);
//			return;
//		}
		int n = A.getNrOfRows();
		int m = A.getNrOfCols();
		if (n!=m)
			throw new Exception("Error: applying expm to a non-square matrix");
//		fprintf(stderr,"%d x\n", n);

		//Array2d At = A.transpose();

		int nrSquarings =  (int) Math.max(0.0,1.0+Math.floor(Math.log(AbstractMatrix.infinityNorm(A))/Math.log(2.0)));
		//Number of squarings

		double factor = 1.0/Math.pow((double)2.0,(double)nrSquarings); 
		//In matlab, we replace A by A*factor. But A is constant, so we do this later.

//		int q = 16;

		Array2d D = new Array2d(n); // initialize as nxnidentity matrix 
		Array2d N = new Array2d(n);
		Array2d X = new Array2d(n);
		//Array2d tmp = new Array2d(n, n); // initialize as nxnidentity zero matrix
		double c=1.0;
		double [] _A = A.asZeroBasedArray();
		double [] _X = X.asZeroBasedArray();
		int n2 = n*n;
		double [] _B = new double[n2];

	


		//copy(_D, D, n);
		double neg=-1.0;
		for(int k=1;k<=EXPM_Q;k++) {
			//X = factor*AX;
			System.arraycopy(_X, 0, _B, 0, n2);
			//Dgemm.dgemm ("N", "N", n, n, n, factor,	_A, 0, n, _B, 0, n, 0.0, _X, 0, n);
			snap.matrix.Dgemm.dgemm('N', 'N', n, n, n, factor,	_A, 0, n, _B, 0, n, 0.0, _X, 0, n);
			//System.arraycopy(_X, 0, _X, 0, n2);

			c = c*(EXPM_Q-k+1.0)/((2.0*EXPM_Q - k+1.0)*k);


			//N = N+c*X;
			//daxpy(n*n, c, _X, 0, _N, 0);
			//D = D+ ((-1)^k) * c*X;
			//daxpy(n*n, neg*c, _X, 0, _D, 0);
			double negc = neg * c;
/*
			for(int i=0; i<n2;i++) {
				_N[i] += c * _X[i];
				_D[i] += negc * _X[i];
			}
*/
			//vector<double>::iterator xptr;
			//double * dptr = _D;
			/*
			for(int i=1;i<=n;i++) {
				for(int j=1;j<=n;j++) {
					N.add(i,j,c*_X[i*n+j-1-n]);
					D.add(i, j, negc *_X[i*n+j-1-n]);
				}
			}
			*/
			N.add(c,_X);
			D.add(negc ,_X);
			neg = -neg;
		}


		partialPivotSolve(D, N);
		//copy(N,F);

//		double * _F = new double[n2];
//		double * _tmp = new double[n2];
		_A = N.asZeroBasedArray();
		for(int k=1;k<=nrSquarings;k++) {
			//multiply(F, F, tmp);
			//copy(tmp,F);
			//Dgemm.dgemm ("N", "N", n, n, n, 1.0,
			//		_A, 0, n, _A, 0, n, 0.0, _X, 0, n);
			snap.matrix.Dgemm.dgemm ('N', 'N', n, n, n, 1.0,
					_A, 0, n, _A, 0, n, 0.0, _X, 0, n);
			System.arraycopy(_X, 0, _A, 0, n2);

		}
		F.set(_A, n);
	} // expm

	/**
	 %  EXPV computes an approximation of w = exp(t*A)*v for a
	 %  general matrix A using Krylov subspace  projection techniques.
	 %  It does not compute the matrix exponential in isolation but instead,
	 %  it computes directly the action of the exponential operator on the
	 %  operand vector. This way of doing so allows for addressing large
	 %  sparse problems. The matrix under consideration interacts only
	 %  via matrix-vector products (matrix-free method).
	 Roger B. Sidje (rbs@maths.uq.edu.au)
	 %  EXPOKIT: Software Package for Computing Matrix Exponentials.
	 %  ACM - Transactions On Mathematical Software, 24(1):130-156, 1998

	 Translated from matlab into C++ by David Bryant. Here the matrix A is an abstract
	 class, and we only operate on A via the two member functions
	 infNorm  (compute the infinity norm of A)
	 multiply (compute Ax for some vector x, indexed from 1,2,....)
	 **/
	public
	static double [] expmv(double time, AbstractMatrix A, double [] v) throws Exception{
		double [] w = new double[v.length];
		System.arraycopy(v, 0, w, 0, v.length);
	
		// not sure what the value of tol should be (or what it does...(
		double tol=1.0e-5;
		//tol=1.0e-9;
		int m=30;
		//	cerr<<tol<<endl;
		int n1 = A.getNrOfRows();
		int n2 = A.getNrOfCols();

		try {
			if (n1!=n2)
				throw new Exception("expmv can only be applied to square matrices");
			int n=n1;

			//cerr<<"expmv with n = "<<n<<endl;

			m = Math.min(m,n);

			if (time==0.0) {
				// already cloned at the top w = (Vector<Double>) v.clone();
				return w;
			}



			double anorm = A.infNorm();
			int mxrej = 10;
			double btol = 1.0e-7;
			double gamma = 0.9;
			double delta = 1.2;
			int mb = m;
			double t_out = Math.abs(time);
			int nstep = 0;
			double t_new = 0.0, t_now = 0.0;
			double s_error = 0.0;
			double EPSILON = 10e-16;
			double rndoff = anorm * EPSILON;
			int mx;

			int k1 = 2;
			double xm = 1.0/m;
			double normv = AbstractMatrix.vectorNorm(v);
			double beta = normv;

			double pi = 3.141592653589793238462643;
			double fact =   Math.pow(((m+1)/Math.exp(1)),(m+1.0))  *  Math.sqrt(2.0*pi*(m+1));
			t_new = (1.0/anorm)*Math.pow(((fact*tol)/(4*beta*anorm)),xm);
			double s = Math.pow(10.0,Math.floor(Math.log10(t_new))-1);
			t_new = Math.ceil(t_new/s)*s;
			int sgn = (time > 0)? 1 : -1;
			if (time==0)
				sgn = 0;


			double [] Vcol = new double[n+1];
			double [] p= new double[n+1];

			Array2d V;
			Array2d H;
			Array2d smallH;
			Array2d F;				

			double hump = normv;

			//int number_t_steps = 0;
			//int number_expm_calls = 0;
			//int number_middleH = 0;

			
			while(t_now < t_out) {

				//number_t_steps++;


				nstep++;
				double t_step = Math.min(t_out-t_now,t_new);
				V = new Array2d(n,m+1);
				H = new Array2d(m+2,m+2);

				for(int i=1;i<=n;i++) {
					V.set(i, 1,  (1.0/beta)*w[i]);
				}

				for(int j=1;j<=m;j++) {
					V.getColumn(j,Vcol);
					A.multiply(Vcol,p);

//					System.err.print("p = ");
//					for(int ii = 1;ii<=n;ii++)
//						System.err.print(p[ii]+" ");
//					System.err.println();


					for(int i=1;i<=j;i++) {
						//number_middleH+=n;

						double sum=0.0;
						for(int i2=1;i2<=n;i2++)
							sum+=V.at(i2,i)*p[i2];
						double Hij = sum;
						H.set(i,j, sum);

						for(int i2=1;i2<=n;i2++)
							p[i2] -= Hij*V.at(i2,i);

					}
					s = AbstractMatrix.vectorNorm(p);

					if (s<btol) {
						k1 = 0;
						mb = j;
						t_step = t_out - t_now;
						break;
					}
					H.set(j+1, j, s);
					for(int j2=1;j2<=n;j2++)
						V.set(j2, j+1, (1.0/s)*p[j2]);
				}

				double avnorm = 0.0;

				//print(cout,V);
				//cout<<"H = "<<endl;
				//print(cout,H);

				if (k1!=0) {
					H.set(m+2, m+1, 1.0);
					V.getColumn(m+1, Vcol);
					A.multiply(Vcol,p); //p here used as scratch vector

					avnorm = AbstractMatrix.vectorNorm(p);
				}

				int ireject = 0;
				double err_loc = 0.0;

				F = new Array2d(0,0);
				while(ireject <= mxrej) {
					mx = mb + k1;
					smallH = H.copy();
					smallH.resize(mx,mx); //smallH = H(1:mx,1:mx)
					smallH.scale(sgn*t_step);
					F = new Array2d(mx, mx);
					expm(smallH, F);

					//number_expm_calls++;

					//cout<<"F = ";
					//print(cout,F);

					if (k1==0) {
						err_loc = btol;
						break;
					} else {
						double phi1 = Math.abs(beta*F.at(m+1,1));
						double phi2 = Math.abs(beta*F.at(m+2,1) * avnorm);
						if (phi1 > 10.0*phi2) {
							err_loc = phi2;
							xm = 1.0/m;
						} else if (phi1>phi2) {
							err_loc = (phi1*phi2)/(phi1-phi2);
							xm = 1.0/m;
						} else {
							err_loc = phi1;
							xm = 1.0/(m-1);
						}
					}
					if (err_loc <= delta* t_step * tol)
						break;
					else {
						t_step = gamma * t_step * Math.pow((t_step*tol/err_loc),xm);
						s = Math.pow(10.0,(Math.floor(Math.log10(t_step))-1));
						t_step = Math.ceil(t_step/s) * s;
						if (ireject == mxrej)
							throw new Exception("The requested tolerance is too high.");
						ireject = ireject + 1;
					}
				}

				mx = mb + (int) Math.max(  0 ,(int)k1-1 );
				for(int i1 = 1;i1<=n;i1++) {
					double sum = 0.0;
					for(int j1 = 1;j1<=mx;j1++)
						sum+=V.at(i1, j1)*(beta*F.at(j1,1));
					w[i1 ]= sum;
				}
				beta = AbstractMatrix.vectorNorm( w );
				hump = Math.max(hump,beta);

				t_now = t_now + t_step;
				t_new = gamma * t_step * Math.pow((t_step*tol/err_loc),xm);
				s = Math.pow(10.0,(Math.floor(Math.log10(t_new))-1));
				t_new = Math.ceil(t_new/s) * s;

				err_loc = Math.max(err_loc,rndoff);
				s_error = s_error + err_loc;
			}

			//	cerr<<"Number of t steps = "<<number_t_steps<<endl;
			//	cerr<<"NUmber of calls to expm = "<<number_expm_calls<<endl;
			//	cerr<<"Number of small loops = "<<number_middleH<<endl;


			//double err = s_error;
			hump = hump / normv;

			//System.out.println(nstep + " nstep" + EXPM_Q);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return w;
	}
	
	/**
	 Computes Q'x for the Q matrix defined in the paper.
	 Note that x is stored internally as a 2d array, so first converts this into a single column.
	 At present, Krylov projection is used to compute the exponential times a vector.
	 * @throws Exception 
	 **/
	//static double [] g_mem = new double[100000];
	static FMatrix expQTtx(int N, double u, double v,
			double gamma, double t,
			FMatrix x) throws Exception {
		//if (false && N>400) {
//		if (false) {
//			//Get dimensions
//			//QMatrix Q= new QMatrix(N,u,v,gamma);
//			//Q.getSize(q1,q2);
//			int q1 = ((N+1)*(N+1) + N - 1)/2;//Q.getNrOfRows();
//			double[] _x = x.asVectorCopy(); 
//			//double[] _y = new double[q1]; 
//			//Load up x into a column vector
////			int index=1;
////			for(int n=1;n<=N;n++) {
////				for(int r=0;r<=n;r++) {
////					_x[index++] = x.get(n,r);
////				}
////			}
//			ExpQT.expQTtx(N, u, v, gamma, t, _x);
//			//expQTtx(N, u, v, gamma, t, _x, g_mem);
//			//System.arraycopy(g_mem, 0, _y, 0, q1);
//	
//			//Load ycol back into 2d vector.
////			double [] _y0 = new double[q1];
////			index = 1;
////			for(int n=1;n<=N;n++) {
////				for(int r=0;r<=n;r++) {
////					_y0[n*(n+1)/2-1+r] = _y[index];
////					index++;
////				}
////			}
//			FMatrix y = new FMatrix(N, _x);//double[N+1][];
//			/*
//			FMatrix y = new FMatrix(N);
//			index = 1;
//			 for(int n=1;n<=N;n++) {
//				for(int r=0;r<=n;r++) {
//					y.set(n,r, _y[index]);
//					index++;
//				}
//			}
//			*/
//			return y;
//		}		
		//Get dimensions
		QMatrix Q= new QMatrix(N,u,v,gamma);
		//Q.getSize(q1,q2);
		//int q1 = Q.getNrOfRows();

		
	//int q2 = Q.getNrOfCols();
	double [] xcol;// = new double[q1+1];
	xcol = x.asVectorCopyBase1();
	// it's a 1-based vector, so stick a zero at the start
	
	//double [] _y0;// = new double[q1];
	//for (int h = 0; h < 50; h++) {
	//Load up x into a column vector
//	int index=1;
//	for(int n=1;n<=N;n++) {
//		for(int r=0;r<=n;r++) {
//			xcol[index++] = x.get(n,r);
//		}
//	}
	
	//Solve
	 //double [] _y1 = expmv(t, Q, xcol);
	 double [] _y1 = cf_expmvCOMPLEX(t, Q, xcol);
//	 double [] _y1 = cf_expmv(t, Q, xcol);
	 
//	 double fDiff = 0;
//	 for (int i = 0; i < _y1.length; i++) {
//		 fDiff = Math.max(fDiff, Math.abs(_y1[i]-_y2[i]));
//	 }
	 
	double [] y0 = new double[_y1.length-1];
	System.arraycopy(_y1, 1, y0, 0, y0.length);
	FMatrix y = new FMatrix(N, y0);//double[N+1][];
		return y;
	} // expQTtx


	// 100 200 300 400 500 600 700 800 900 1000 1100 1200 1300 1400 1406 -23788.232953617982
	// Start likelihood:-24583.402761633304

	/**
	 Uses the Caratheodory-Fejer approximation for the exponential (on the negative real line)
	 to evaluate the exponential of A (assumed to be negative semi-definite) times a matrix.

	 Currently uses 12 degree.
	 **/
	public static int CF_DEG = 12;
	public static double [] ci_real = {
		0.000818433612497,
		-0.068571505514864,
		1.319411815998137,
		-8.238258033274786,
		18.785982629476070,
		-11.799383335697918,
		-11.799383335697890,
		18.785982629476067,
		-8.238258033274763,
		1.319411815998138,
		-0.068571505514865,
	0.000818433612497};
	public static double [] ci_imag = {
		0.000581353207069,
		-0.038419074245887,
		0.183523497750480,
		2.796192505614474,
		-20.237292093573895,
		46.411650777279597,
		-46.411650777279569,
		20.237292093573895,
		-2.796192505614448,
		-0.183523497750480,
		0.038419074245888,
	-0.000581353207069};
	public static double [] zi_real = {
		-6.998688082445778,
		-2.235968223749446,
		0.851707264834878,
		2.917868800307170,
		4.206124506834328,
		4.827493775040721,
		4.827493775040721,
		4.206124506834328,
		2.917868800307170,
		0.851707264834878,
		-2.235968223749446,
	-6.998688082445778};
	public static double [] zi_imag = {
		-13.995917029301355,
		-11.109296400461870,
		-8.503832905826961,
		-6.017345968518187,
		-3.590920783130140,
		-1.193987999180278,
		1.193987999180278,
		3.590920783130140,
		6.017345968518187,
		8.503832905826961,
		11.109296400461870,
	13.995917029301355};
	
	public static COMPLEX r_inf = new COMPLEX(0,0);
	
	public static double [] cf_expmv(double time, AbstractMatrix A, double [] v) throws Exception {
		//for each i=1..CF_DEG we solve
		// (At - z_i I) x^(i) = v

		//we then have that w  = exp(At)v is well approximated by
		//  w = \sum c_i x^{(i)}

		//Solving  (At - z_i I) x^(i) = v
		// is equivalent to solving (A - z_i/t I) x^(i) = v/t

		//In order to improve numerical accuracy, we implement log(n) steps.

		//Initially wc <- v
		// for k = 0..steps-1
		//		vc <- wc  / (t / steps)
		//		wc <- sum c_i (A - z_i (steps/t))^{-1} vc/t
		//	end


		//complex vectors xc and vc
		int n = v.length;
		double [] w = new double[n];

		if (time == 0) {
			System.arraycopy(v, 0, w, 0, v.length);
			return w;
		}

		double[] xc_r = new double[n];
		double[] xc_i = new double[n];
		double[] wc_r = new double[n];
		double[] wc_i = new double[n];
		double[] vc_r = new double[n];
		double[] vc_i = new double[n];

		System.arraycopy(v, 1, wc_r, 1, n-1);

		int steps = (int) Math.ceil(Math.log(n)/Math.log(2));
		steps = 1;

		double stepsize = time/(double)steps;

		for(int k = 0; k < steps; k++) {

			for(int i = 1; i < n; i++) {
				vc_r[i] = wc_r[i] / stepsize;
				vc_i[i] = wc_i[i] / stepsize;
			}
//			for(int i = 1; i < n; i++) {
//				//wc[i].mul(vc[i], r_inf);
//				// r_inf = (0,0)
//				wc_r[i] = 0;
//				wc_i[i] = 0;
//			}
			Arrays.fill(wc_r, 0);
			Arrays.fill(wc_i, 0);

			for(int i = 0; i < CF_DEG; i++) {
				//COMPLEX offset = new COMPLEX(-zi_real[i]/stepsize, -zi_imag[i]/stepsize);
				A.solve(vc_r, vc_i, -zi_real[i]/stepsize, -zi_imag[i]/stepsize, xc_r, xc_i);
				double ci_r = ci_real[i];
				double ci_i = ci_imag[i];
				for(int j=1;j<n;j++) {
					//wc[j]+=c_i * xc[j];
					//c_i.timesadd(, wc[j]);
					wc_r[j] += xc_r[j] * ci_r - xc_i[j] * ci_i;
					wc_i[j] += xc_r[j] * ci_i + xc_i[j] * ci_r;
				}
			}
		}

		System.arraycopy(wc_r, 1, w, 1, n-1);
		return w;
	} // cf_expmv

	public static double [] cf_expmvCOMPLEX(double time, AbstractMatrix A, double [] v) throws Exception {
		//for each i=1..CF_DEG we solve
		// (At - z_i I) x^(i) = v

		//we then have that w  = exp(At)v is well approximated by
		//  w = \sum c_i x^{(i)}

		//Solving  (At - z_i I) x^(i) = v
		// is equivalent to solving (A - z_i/t I) x^(i) = v/t

		//In order to improve numerical accuracy, we implement log(n) steps.

		//Initially wc <- v
		// for k = 0..steps-1
		//		vc <- wc  / (t / steps)
		//		wc <- sum c_i (A - z_i (steps/t))^{-1} vc/t
		//	end


		//complex vectors xc and vc
		int n = v.length;
		double [] w = new double[n];

		if (time == 0) {
			System.arraycopy(v, 0, w, 0, v.length);
			return w;
		}

		COMPLEX[] xc = new COMPLEX[n];
		COMPLEX[] wc = new COMPLEX[n];
		COMPLEX[] vc = new COMPLEX[n];

		wc[0] = new COMPLEX();
		vc[0] = new COMPLEX();
		xc[0] = new COMPLEX();
		for(int i = 1; i < n; i++) {
			wc[i] = new COMPLEX(v[i],0.0);
			vc[i] = new COMPLEX();
			xc[i] = new COMPLEX();
		}

		int steps = (int) Math.ceil(Math.log(n)/Math.log(2));
		steps = 1;

		double stepsize = time/(double)steps;

		for(int k = 0; k < steps; k++) {

			for(int i = 1; i < n; i++) {
				vc[i].m_fRe = wc[i].m_fRe / stepsize;
				vc[i].m_fIm = wc[i].m_fIm / stepsize;
				//wc[i].mul(vc[i], r_inf);
				// r_inf = (0,0)
				wc[i].m_fRe = 0;
				wc[i].m_fIm = 0;
			}

			for(int i = 0; i < CF_DEG; i++) {
				COMPLEX offset = new COMPLEX(-zi_real[i]/stepsize, -zi_imag[i]/stepsize);
				A.solve(vc, offset, xc);
				COMPLEX c_i = new COMPLEX(ci_real[i], ci_imag[i]);
				for(int j=1;j<n;j++) {
					//wc[j]+=c_i * xc[j];
					//c_i.timesadd(, wc[j]);
					wc[j].muladd(c_i, xc[j]);
				}
			}
		}

		for(int i=1;i<n;i++) {
			w[i] = wc[i].m_fRe;
		}
		return w;
	} // cf_expmv
} // class MatrixExponentiator
