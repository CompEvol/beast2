
/*
 * File BeerLikelihoodCoreCnG4.java
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

	void calcSSP(int state1, int state2) {
			m_pfPartials3[v] = m_pfMatrices1[w + state1] * m_pfMatrices2[w + state2];
			v++;
			w += m_nStates+1;

			m_pfPartials3[v] = m_pfMatrices1[w + state1] * m_pfMatrices2[w + state2];
			v++;
			w += m_nStates+1;

			m_pfPartials3[v] = m_pfMatrices1[w + state1] * m_pfMatrices2[w + state2];
			v++;
			w += m_nStates+1;

			m_pfPartials3[v] = m_pfMatrices1[w + state1] * m_pfMatrices2[w + state2];
			v++;
			w += m_nStates+1;
	}

	void calcSPP(int state1) {
		double tmp, sum;
			tmp = m_pfMatrices1[w + state1];
			sum = 0.0;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 0];
				w++;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 1];
				w++;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 2];
				w++;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 3];
				w++;
			w++;
			m_pfPartials3[u] = tmp * sum;
			u++;

			tmp = m_pfMatrices1[w + state1];
			sum = 0.0;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 0];
				w++;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 1];
				w++;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 2];
				w++;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 3];
				w++;
			w++;
			m_pfPartials3[u] = tmp * sum;
			u++;

			tmp = m_pfMatrices1[w + state1];
			sum = 0.0;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 0];
				w++;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 1];
				w++;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 2];
				w++;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 3];
				w++;
			w++;
			m_pfPartials3[u] = tmp * sum;
			u++;

			tmp = m_pfMatrices1[w + state1];
			sum = 0.0;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 0];
				w++;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 1];
				w++;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 2];
				w++;
				sum += m_pfMatrices2[w] * m_pfPartials2[v + 3];
				w++;
			w++;
			m_pfPartials3[u] = tmp * sum;
			u++;

		v += m_nStates;
	}

	void calcPPP() {
		double sum1, sum2;
			sum1=0;
			sum2=0;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 0];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 0];
				w++;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 1];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 1];
				w++;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 2];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 2];
				w++;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 3];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 3];
				w++;
			w++;
			m_pfPartials3[u] = sum1 * sum2;
			u++;

			sum1=0;
			sum2=0;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 0];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 0];
				w++;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 1];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 1];
				w++;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 2];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 2];
				w++;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 3];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 3];
				w++;
			w++;
			m_pfPartials3[u] = sum1 * sum2;
			u++;

			sum1=0;
			sum2=0;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 0];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 0];
				w++;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 1];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 1];
				w++;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 2];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 2];
				w++;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 3];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 3];
				w++;
			w++;
			m_pfPartials3[u] = sum1 * sum2;
			u++;

			sum1=0;
			sum2=0;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 0];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 0];
				w++;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 1];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 1];
				w++;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 2];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 2];
				w++;
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + 3];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + 3];
				w++;
			w++;
			m_pfPartials3[u] = sum1 * sum2;
			u++;
	}

} // class BeerLikelihoodCore
