
/*
 * File BeerLikelihoodCore4.java
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

// TODO: unroll loops
 
// TODO: separate inner loops to small methods

// TODO: remove all loops on matrices if matrixcount==1

// TODO: why do partials not sum to 1???
// TODO: ensure matrices are normalised so that the last item can be calculated as (1 - rest) 

// TODO: efficient gamma distribution handling

// TODO: buffered calculation (ie. save up all partials/partials calculations and do them in 1 go

// TODO: CUDA support


package beast.evolution.likelihood;


public class BeerLikelihoodCoreCnG4 extends BeerLikelihoodCoreCnG {
	
	public BeerLikelihoodCoreCnG4() {
		super(4);
	} // c'tor

	
	void calcSSP(int state1, int state2, double [] pfMatrices1, double [] pfMatrices2, double [] pfPartials3, int w, int v) {
		if (state1 < 4) {
			if (state2 < 4) {
				pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
				v++;	w += 4;
				pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
				v++;	w += 4;
				pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
				v++;	w += 4;
				pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
				//v++;	w += 4;
			} else {
			// child 2 has a gap or unknown state so don't use it
				pfPartials3[v] = pfMatrices1[w + state1];
				v++;	w += 4;
				pfPartials3[v] = pfMatrices1[w + state1];
				v++;	w += 4;
				pfPartials3[v] = pfMatrices1[w + state1];
				v++;	w += 4;
				pfPartials3[v] = pfMatrices1[w + state1];
				//v++;	w += 4;
			}
		} else if (state2 < 4) {
			// child 2 has a gap or unknown state so don't use it
			pfPartials3[v] = pfMatrices2[w + state2];
			v++;	w += 4;
			pfPartials3[v] = pfMatrices2[w + state2];
			v++;	w += 4;
			pfPartials3[v] = pfMatrices2[w + state2];
			v++;	w += 4;
			pfPartials3[v] = pfMatrices2[w + state2];
			//v++;	w += 4;

		} else {
			// both children have a gap or unknown state so set partials to 1
			pfPartials3[v] = 1.0;
			v++;
			pfPartials3[v] = 1.0;
			v++;
			pfPartials3[v] = 1.0;
			v++;
			pfPartials3[v] = 1.0;
			//v++;
		}

		
//		pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
//			v++;
//			w += m_nStates+1;
//
//			pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
//			v++;
//			w += m_nStates+1;
//
//			pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
//			v++;
//			w += m_nStates+1;
//
//			pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
//			//v++;
//			w += m_nStates+1;
//			//return v;
	}
	
	void calcSPP(int state1, double [] fMatrices1, double [] fMatrices2, double [] fPartials2, double [] fPartials3, int w, int v, int u) {
		double tmp, sum;
		sum = 0.0;
		if (state1 < 4) {
			sum =	fMatrices2[w] * fPartials2[v];
			sum +=	fMatrices2[w + 1] * fPartials2[v + 1];
			sum +=	fMatrices2[w + 2] * fPartials2[v + 2];
			sum +=	fMatrices2[w + 3] * fPartials2[v + 3];
			fPartials3[u] = fMatrices1[w + state1] * sum;	u++;

			w += 4;
			sum =	fMatrices2[w] * fPartials2[v];
			sum +=	fMatrices2[w + 1] * fPartials2[v + 1];
			sum +=	fMatrices2[w + 2] * fPartials2[v + 2];
			sum +=	fMatrices2[w + 3] * fPartials2[v + 3];
			fPartials3[u] = fMatrices1[w + state1] * sum;	u++;

			w += 4;
			sum =	fMatrices2[w] * fPartials2[v];
			sum +=	fMatrices2[w + 1] * fPartials2[v + 1];
			sum +=	fMatrices2[w + 2] * fPartials2[v + 2];
			sum +=	fMatrices2[w + 3] * fPartials2[v + 3];
			fPartials3[u] = fMatrices1[w + state1] * sum;	u++;

			w += 4;
			sum =	fMatrices2[w] * fPartials2[v];
			sum +=	fMatrices2[w + 1] * fPartials2[v + 1];
			sum +=	fMatrices2[w + 2] * fPartials2[v + 2];
			sum +=	fMatrices2[w + 3] * fPartials2[v + 3];
			fPartials3[u] = fMatrices1[w + state1] * sum;//	u++;
		} else {
			// Child 1 has a gap or unknown state so don't use it
			sum =	fMatrices2[w] * fPartials2[v];
			sum +=	fMatrices2[w + 1] * fPartials2[v + 1];
			sum +=	fMatrices2[w + 2] * fPartials2[v + 2];
			sum +=	fMatrices2[w + 3] * fPartials2[v + 3];
			fPartials3[u] = sum;	u++;

			w += 4;
			sum =	fMatrices2[w] * fPartials2[v];
			sum +=	fMatrices2[w + 1] * fPartials2[v + 1];
			sum +=	fMatrices2[w + 2] * fPartials2[v + 2];
			sum +=	fMatrices2[w + 3] * fPartials2[v + 3];
			fPartials3[u] = sum;	u++;

			w += 4;
			sum =	fMatrices2[w] * fPartials2[v];
			sum +=	fMatrices2[w + 1] * fPartials2[v + 1];
			sum +=	fMatrices2[w + 2] * fPartials2[v + 2];
			sum +=	fMatrices2[w + 3] * fPartials2[v + 3];
			fPartials3[u] = sum;	u++;

			w += 4;
			sum =	fMatrices2[w] * fPartials2[v];
			sum +=	fMatrices2[w + 1] * fPartials2[v + 1];
			sum +=	fMatrices2[w + 2] * fPartials2[v + 2];
			sum +=	fMatrices2[w + 3] * fPartials2[v + 3];
			fPartials3[u] = sum;//	u++;
			//v += 4;
		}
	}

	void calcPPP(double [] pfMatrices1, double [] pfPartials1, double [] pfMatrices2, double [] pfPartials2, double [] pfPartials3, int w, int v1, int v2, int u) {
		double sum1, sum2;
			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 3];
				w++;
//			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;

			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 3];
				w++;
//			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;
			
			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 3];
				w++;
//			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;

			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 3];
				w++;
//			w++;
			pfPartials3[u] = sum1 * sum2;
			//u++;
			//return u;
	}
	
	void calcPPP2(double [] pfMatrices1, double [] pfPartials1, double [] pfMatrices2, double [] pfPartials2, double [] pfPartials3, int w, int v, int u) {
		double sum1, sum2;
			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
//			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;

			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
//			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;
			
			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
//			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;

			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
//			w++;
			pfPartials3[u] = sum1 * sum2;
			//u++;
			//return u;
	}
	
//	@Override
//	LikelihoodCore getAlternativeCore() {
//    	return new BeerLikelihoodCoreJava4();
//    }
} // class BeerLikelihoodCore
