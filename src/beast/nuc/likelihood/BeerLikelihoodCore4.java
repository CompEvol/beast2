
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


package beast.nuc.likelihood;


public class BeerLikelihoodCore4 extends BeerLikelihoodCore {

	public BeerLikelihoodCore4() {
		super(4);
	} // c'tor

	int calcSSP(int state1, int state2, double [] pfMatrices1, double [] pfMatrices2, double [] pfPartials3, int w, int v) {
			pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
			v++;
			w += m_nStates+1;

			pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
			v++;
			w += m_nStates+1;

			pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
			v++;
			w += m_nStates+1;

			pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
			v++;
			w += m_nStates+1;
			return v;
	}

	int calcSPP(int state1, double [] pfMatrices1, double [] pfMatrices2, double [] pfPartials2, double [] pfPartials3, int w, int v, int u) {
		double tmp, sum;
			tmp = pfMatrices1[w + state1];
			sum = 0.0;
				sum += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
			w++;
			pfPartials3[u] = tmp * sum;
			u++;

			tmp = pfMatrices1[w + state1];
			sum = 0.0;
				sum += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
			w++;
			pfPartials3[u] = tmp * sum;
			u++;

			tmp = pfMatrices1[w + state1];
			sum = 0.0;
				sum += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
			w++;
			pfPartials3[u] = tmp * sum;
			u++;

			tmp = pfMatrices1[w + state1];
			sum = 0.0;
				sum += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
			w++;
			pfPartials3[u] = tmp * sum;
			u++;

		//v += m_nStates;
		return u;
	}

	int calcPPP(double [] pfMatrices1, double [] pfPartials1, double [] pfMatrices2, double [] pfPartials2, double [] pfPartials3, int w, int v1, int v2, int u) {
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
			w++;
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
			w++;
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
			w++;
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
			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;
			return u;
	}

} // class BeerLikelihoodCore
