
/*
 * File BeerLikelihoodCoreCached.java
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

/** likelihood beast.core that uses a cache mechanism so that only local
 * patterns for leaves need to be calculated.
 */

package beast.evolution.likelihood;

import java.util.Arrays;


public class BeerLikelihoodCoreCached extends LikelihoodCore {
	int u, v, w, v1, v2;
    protected int m_nStates;
    protected int m_nNodes;
    protected int m_nPatterns;
    protected int m_nPartialsSize;
    protected int m_nMatrixSize;
    protected int m_nMatrixCount;

    protected boolean m_bIntegrateCategories;

    protected double[][][] m_fPartials; // 2 x #nodes x (#patterns*#states*#matrices)

    protected int[][] m_iStates; // #nodes x #patterns

    protected double[][][] m_fMatrices; // 2 x #nodes x matrix size

    protected int[] m_iCurrentMatrices; // # nodes
    protected int[] m_iStoredMatrices;  // # nodes
    protected int[] m_iCurrentPartials; // # nodes
    protected int[] m_iStoredPartials;  // # nodes

    protected boolean m_bUseScaling = false;

    protected double[][][] m_fScalingFactors; // 2 x #nodes x #patters

    private double m_fScalingThreshold = 1.0E-100;


    protected int[][] m_nNrOfID; // 2 x #nodes
    protected int[][][] m_nID; // 2 x #nodes x #patterns


	// stack related variables
	final static int OPERATION_SS = 0;
	final static int OPERATION_SP = 1;
	final static int OPERATION_PP = 2;
	int m_nTopOfStack = 0;
	int [] m_nOperation; // #nodes
	int [] m_nNode1;     // #nodes
	int [] m_nNode2;     // #nodes
	int [] m_nNode3;     // #nodes
	int [][] m_nStates1; // #nodes x #patterns
	int [][] m_nStates2; // #nodes x #patterns

	// set of temporary pointers
	int [] m_pStates1;
	int [] m_pStates2;
	double[] m_pfMatrices1;
	double[] m_pfPartials1;
	double[] m_pfMatrices2;
	double[] m_pfPartials2;
	double[] m_pfPartials3;

	public BeerLikelihoodCoreCached(int nStateCount) {
		this.m_nStates = nStateCount;
	} // c'tor

	void calcAllMatrixSSP(int nNrOfID) {
		for (int i = 0; i < nNrOfID; i++) {
			for (int l = 0; l < m_nMatrixCount; l++) {
				w = l * m_nMatrixSize;
				calcSSP(m_pStates1[i], m_pStates2[i]);
			}
		}
	}

	void calcSSP(int state1, int state2) {
		for (int i = 0; i < m_nStates; i++) {
			m_pfPartials3[v] = m_pfMatrices1[w + state1] * m_pfMatrices2[w + state2];
			v++;
			w += m_nStates+1;
		}
	}

	void calcAllMatrixSPP(int nNrOfID) {
		for (int i = 0; i < nNrOfID; i++) {
		for (int l = 0; l < m_nMatrixCount; l++) {
            w = l * m_nMatrixSize;
				v = (l  + m_pStates2[i] * m_nMatrixCount) * m_nStates;
				calcSPP(m_pStates1[i]);
			}
		}
	}

	void calcSPP(int state1) {
		double tmp, sum;
		for (int i = 0; i < m_nStates; i++) {
			tmp = m_pfMatrices1[w + state1];
			sum = 0.0;
			for (int j = 0; j < m_nStates; j++) {
				sum += m_pfMatrices2[w] * m_pfPartials2[v + j];
				w++;
			}
			w++;
			m_pfPartials3[u] = tmp * sum;
			u++;
		}
		v += m_nStates;
	}

	void calcAllMatrixPPP(int nNrOfID) {
		for (int i = 0; i < nNrOfID; i++) {
			for (int l = 0; l < m_nMatrixCount; l++) {
				w = l * m_nMatrixSize;
				v1 = (l + m_pStates1[i] * m_nMatrixCount) * m_nStates;
				v2 = (l + m_pStates2[i] * m_nMatrixCount) * m_nStates;

				calcPPP();
			}
		}
	}

	void calcPPP() {
		double sum1, sum2;
		for (int i = 0; i < m_nStates; i++) {
			sum1=0;
			sum2=0;
			for (int j = 0; j < m_nStates; j++) {
				sum1 += m_pfMatrices1[w] * m_pfPartials1[v1 + j];
				sum2 += m_pfMatrices2[w] * m_pfPartials2[v2 + j];
				w++;
			}
			w++;
			m_pfPartials3[u] = sum1 * sum2;
			u++;
		}
	}

    /**
     * Calculates partial likelihoods at a node.
     *
     * @param iNode1 the 'child 1' node
     * @param iNode2 the 'child 2' node
     * @param iNode3 the 'parent' node
     */
    public void calculatePartials(int iNode1, int iNode2, int iNode3) {
        if (m_iStates[iNode1] != null) {
            if (m_iStates[iNode2] != null) {
                calculateStatesStatesPruning(iNode1, iNode2, iNode3);
            } else {
                calculateStatesPartialsPruning(iNode1, iNode2, iNode3);
            }
        } else {
            if (m_iStates[iNode2] != null) {
                calculateStatesPartialsPruning(iNode2, iNode1, iNode3);
            } else {
                calculatePartialsPartialsPruning(iNode1, iNode2, iNode3);
            }
        }

        if (m_bUseScaling) {
            scalePartials(iNode3);
        }
    }

//	int [][] initIDMap(int iNode, int nMaxID1, int nMaxID2) {
//		m_nNrOfID[m_iCurrentPartials[iNode]][iNode] = 0;
//		int [][] nIDMap = new int[nMaxID1][nMaxID2];
//		for (int i = 0; i < nMaxID1; i++) {
//			Arrays.fill (nIDMap[i], -1);
//		}
//		return nIDMap;
//	} // initIDMap

	int [] initIDMap(int iNode, int nMaxID1, int nMaxID2) {
		m_nNrOfID[m_iCurrentPartials[iNode]][iNode] = 0;
		int [] nIDMap = new int[nMaxID1*nMaxID2];
		Arrays.fill (nIDMap, -1);
		return nIDMap;
	} // initIDMap

	/**
	 * Calculates partial likelihoods at a node when both children have states.
	 */
	protected void calculateStatesStatesPruning(int iNode1, int iNode2, int iNode3) {
		int[] iStates1 = m_iStates[iNode1];
		int[] iStates2 = m_iStates[iNode2];
		int [] nID3 = m_nID[m_iCurrentPartials[iNode3]][iNode3];

		// prepare the stack
		m_nOperation[m_nTopOfStack] = OPERATION_SS;
		m_nNode1[m_nTopOfStack] = iNode1;
		m_nNode2[m_nTopOfStack] = iNode2;
		m_nNode3[m_nTopOfStack] = iNode3;
		m_pStates1 = m_nStates1[m_nTopOfStack];
		m_pStates2 = m_nStates2[m_nTopOfStack];
		m_nTopOfStack++;

		int [] nIDMap = initIDMap(iNode3, m_nStates+1, m_nStates+1);
		int nBase = m_nStates+1;
		int nNrOfID = 0;
		for (int k = 0; k < m_nPatterns; k++) {
			int state1 = iStates1[k];
			int state2 = iStates2[k];
			if (nIDMap[state1 + nBase * state2]<0) {
				m_pStates1[nNrOfID] = state1;
				m_pStates2[nNrOfID] = state2;
				nIDMap[state1 + nBase * state2] = nNrOfID++;
				//calcAllMatrixSSP(state1, state2);
				//m_innerLoopCalculator.calcAllMatrixSSP(state1, state2);
			}
			nID3[k] = nIDMap[state1 + nBase * state2];
		}
		m_nNrOfID[m_iCurrentPartials[iNode3]][iNode3] = nNrOfID;

//		v=0;
//		fMatrices1 = m_fMatrices[m_iCurrentMatrices[iNode1]][iNode1];
//		fMatrices2 = m_fMatrices[m_iCurrentMatrices[iNode2]][iNode2];
//		fPartials3 = m_fPartials[m_iCurrentPartials[iNode3]][iNode3];
//		calcAllMatrixSSP(nNrOfID);
	}

	/**
	 * Calculates partial likelihoods at a node when one child has states and one has partials.
	 */
	protected void calculateStatesPartialsPruning(int iNode1, int iNode2, int iNode3) {
		int [] iStates1 = m_iStates[iNode1];
		int [] nID2 = m_nID[m_iCurrentPartials[iNode2]][iNode2];
		int [] nID3 = m_nID[m_iCurrentPartials[iNode3]][iNode3];

		//v = 0;
		// prepare the stack
		m_nOperation[m_nTopOfStack] = OPERATION_SP;
		m_nNode1[m_nTopOfStack] = iNode1;
		m_nNode2[m_nTopOfStack] = iNode2;
		m_nNode3[m_nTopOfStack] = iNode3;
		m_pStates1 = m_nStates1[m_nTopOfStack];
		m_pStates2 = m_nStates2[m_nTopOfStack];
		m_nTopOfStack++;

		int [] nIDMap = initIDMap(iNode3, m_nStates+1, m_nNrOfID[m_iCurrentPartials[iNode2]][iNode2]);
		int nBase = m_nStates+1;
		int nNrOfID = 0;
		for (int k = 0; k < m_nPatterns; k++) {
			int state1 = iStates1[k];
			int state2 = nID2[k];
			if (nIDMap[state1 + nBase * state2]<0) {
				m_pStates1[nNrOfID] = state1;
				m_pStates2[nNrOfID] = state2;
				nIDMap[state1 + nBase * state2] = nNrOfID++;
				//m_nID[iNode3][k] = nIDMap[state1][state2];
				//calcAllMatrixSPP(state1, state2);
				//m_innerLoopCalculator.calcAllMatrixSPP(state1, state2);
			}
			nID3[k] = nIDMap[state1 + nBase * state2];
		}
		m_nNrOfID[m_iCurrentPartials[iNode3]][iNode3] = nNrOfID;

//		u = 0;
//		fMatrices1 = m_fMatrices[m_iCurrentMatrices[iNode1]][iNode1];
//		fPartials2 = m_fPartials[m_iCurrentPartials[iNode2]][iNode2];
//		fMatrices2 = m_fMatrices[m_iCurrentMatrices[iNode2]][iNode2];
//		fPartials3 = m_fPartials[m_iCurrentPartials[iNode3]][iNode3];
//		calcAllMatrixSPP(nNrOfID);
	}


	void initPartialsLeave(int iNode) {
		System.err.println("RRB: this is really inefficient and code should be replaced!!");
		m_nNrOfID[m_iCurrentPartials[iNode]][iNode] = 0;
		for (int k = 0; k < m_nPatterns; k++) {
			m_nID[m_iCurrentPartials[iNode]][iNode][k] = k;
			m_nNrOfID[m_iCurrentPartials[iNode]][iNode]++;
		}
	} // initPartialsLeave

	/**
	 * Calculates partial likelihoods at a node when both children have partials.
	 */
	protected void calculatePartialsPartialsPruning(int iNode1, int iNode2, int iNode3) {
		int [] nID1 = m_nID[m_iCurrentPartials[iNode1]][iNode1];
		int [] nID2 = m_nID[m_iCurrentPartials[iNode2]][iNode2];
		int [] nID3 = m_nID[m_iCurrentPartials[iNode3]][iNode3];

		// prepare the stack
		m_nOperation[m_nTopOfStack] = OPERATION_PP;
		m_nNode1[m_nTopOfStack] = iNode1;
		m_nNode2[m_nTopOfStack] = iNode2;
		m_nNode3[m_nTopOfStack] = iNode3;
		m_pStates1 = m_nStates1[m_nTopOfStack];
		m_pStates2 = m_nStates2[m_nTopOfStack];
		m_nTopOfStack++;

		if (m_nNrOfID[m_iCurrentPartials[iNode1]][iNode1] == 0) {
			initPartialsLeave(iNode1);
		}
		if (m_nNrOfID[m_iCurrentPartials[iNode2]][iNode2] == 0) {
			initPartialsLeave(iNode2);
		}

		int [] nIDMap = initIDMap(iNode3, m_nNrOfID[m_iCurrentPartials[iNode1]][iNode1], m_nNrOfID[m_iCurrentPartials[iNode2]][iNode2]);
		int nBase = m_nNrOfID[m_iCurrentPartials[iNode1]][iNode1];
		int nNrOfID = 0;
		for (int k = 0; k < m_nPatterns; k++) {
			int state1 = nID1[k];
			int state2 = nID2[k];
			if (nIDMap[state1 + nBase * state2]<0) {
				m_pStates1[nNrOfID] = state1;
				m_pStates2[nNrOfID] = state2;
				nIDMap[state1 + nBase * state2] = nNrOfID++;
				//calcAllMatrixPPP(state1, state2);
			}
			nID3[k] = nIDMap[state1 + nBase * state2];
		}
		m_nNrOfID[m_iCurrentPartials[iNode3]][iNode3] = nNrOfID;

//		u = 0;
//		fPartials1 = m_fPartials[m_iCurrentPartials[iNode1]][iNode1];
//		fMatrices1 = m_fMatrices[m_iCurrentMatrices[iNode1]][iNode1];
//		fPartials2 = m_fPartials[m_iCurrentPartials[iNode2]][iNode2];
//		fMatrices2 = m_fMatrices[m_iCurrentMatrices[iNode2]][iNode2];
//		fPartials3 = m_fPartials[m_iCurrentPartials[iNode3]][iNode3];
//		calcAllMatrixPPP(nNrOfID);
	}

//	/**
//	 * Calculates partial likelihoods at a node when both children have states.
//	 */
//	protected void calculateStatesStatesPruning(int[] iStates1, double[] fMatrices1,
//												int[] iStates2, double[] fMatrices2,
//												double[] fPartials3, int[] iMatrixMap)
//	{
//		int v = 0;
//		System.out.println("#matrices=" + m_nMatrixCount);
//
//		for (int k = 0; k < m_nPatternCount; k++) {
//
//			int state1 = iStates1[k];
//			int state2 = iStates2[k];
//
//			int w = iMatrixMap[k] * m_nMatrixSize;
//
//			if (state1 < m_nStateCount && state2 < m_nStateCount) {
//
//				for (int i = 0; i < m_nStateCount; i++) {
//
//					fPartials3[v] = fMatrices1[w + state1] * fMatrices2[w + state2];
//
//					v++;
//					w += m_nStateCount;
//				}
//
//			} else if (state1 < m_nStateCount) {
//				// child 2 has a gap or unknown state so treat it as unknown
//
//				for (int i = 0; i < m_nStateCount; i++) {
//
//					fPartials3[v] = fMatrices1[w + state1];
//
//					v++;
//					w += m_nStateCount;
//				}
//			} else if (state2 < m_nStateCount) {
//				// child 2 has a gap or unknown state so treat it as unknown
//
//				for (int i = 0; i < m_nStateCount; i++) {
//
//					fPartials3[v] = fMatrices2[w + state2];
//
//					v++;
//					w += m_nStateCount;
//				}
//			} else {
//				// both children have a gap or unknown state so set partials to 1
//
//				for (int j = 0; j < m_nStateCount; j++) {
//					fPartials3[v] = 1.0;
//					v++;
//				}
//			}
//		}
//	}
//
//	/**
//	 * Calculates partial likelihoods at a node when one child has states and one has partials.
//	 */
//	protected void calculateStatesPartialsPruning(	int[] iStates1, double[] fMatrices1,
//													double[] fPartials2, double[] fMatrices2,
//													double[] fPartials3, int[] iMatrixMap)
//	{
//
//		double sum, tmp;
//
//		int u = 0;
//		int v = 0;
//
//		for (int k = 0; k < m_nPatternCount; k++) {
//
//			int state1 = iStates1[k];
//
//			int w = iMatrixMap[k] * m_nMatrixSize;
//
//			if (state1 < m_nStateCount) {
//
//				for (int i = 0; i < m_nStateCount; i++) {
//
//					tmp = fMatrices1[w + state1];
//
//					sum = 0.0;
//					for (int j = 0; j < m_nStateCount; j++) {
//						sum += fMatrices2[w] * fPartials2[v + j];
//						w++;
//					}
//
//					fPartials3[u] = tmp * sum;
//					u++;
//				}
//
//				v += m_nStateCount;
//			} else {
//				// Child 1 has a gap or unknown state so don't use it
//
//				for (int i = 0; i < m_nStateCount; i++) {
//
//					sum = 0.0;
//					for (int j = 0; j < m_nStateCount; j++) {
//						sum += fMatrices2[w] * fPartials2[v + j];
//						w++;
//					}
//
//					fPartials3[u] = sum;
//					u++;
//				}
//
//				v += m_nStateCount;
//			}
//		}
//	}
//
//	/**
//	 * Calculates partial likelihoods at a node when both children have partials.
//	 */
//	protected void calculatePartialsPartialsPruning(double[] fPartials1, double[] fMatrices1,
//													double[] fPartials2, double[] fMatrices2,
//													double[] fPartials3, int[] iMatrixMap)
//	{
//		double sum1, sum2;
//
//		int u = 0;
//		int v = 0;
//
//		for (int k = 0; k < m_nPatternCount; k++) {
//
//			int w = iMatrixMap[k] * m_nMatrixSize;
//
//			for (int i = 0; i < m_nStateCount; i++) {
//
//				sum1 = sum2 = 0.0;
//
//				for (int j = 0; j < m_nStateCount; j++) {
//					sum1 += fMatrices1[w] * fPartials1[v + j];
//					sum2 += fMatrices2[w] * fPartials2[v + j];
//
//					w++;
//				}	Input<Frequencies> m_freqs = new Input<Frequencies>("frequencies", "frequencies of characters used as prior on root");

//
//				fPartials3[u] = sum1 * sum2;
//				u++;
//			}
//			v += m_nStateCount;
//		}
//	}

	/**
	 * Integrates partials across categories.
     * @param fInPartials the array of partials to be integrated
	 * @param fProportions the proportions of sites in each category
	 * @param fOutPartials an array into which the partials will go
	 */
    public void integratePartials(int iNode, double[] fProportions, double[] fOutPartials) {
    	for (int i = 0; i < m_nTopOfStack; i++) {
    		m_pStates1 = m_nStates1[i];
    		m_pStates2 = m_nStates2[i];
    		int iNode1 = m_nNode1[i];
    		int iNode2 = m_nNode2[i];
    		int iNode3 = m_nNode3[i];

			m_pfMatrices1 = m_fMatrices[m_iCurrentMatrices[iNode1]][iNode1];
			m_pfMatrices2 = m_fMatrices[m_iCurrentMatrices[iNode2]][iNode2];
			m_pfPartials3 = m_fPartials[m_iCurrentPartials[iNode3]][iNode3];

			switch (m_nOperation[i]) {
    		case OPERATION_SS:
    			v=0;
    			calcAllMatrixSSP(m_nNrOfID[m_iCurrentPartials[iNode3]][iNode3]);
    			break;
    		case OPERATION_SP:
    			u = 0;
    			//m_pfMatrices1 = m_fMatrices[m_iCurrentMatrices[iNode1]][iNode1];
    			m_pfPartials2 = m_fPartials[m_iCurrentPartials[iNode2]][iNode2];
    			//m_pfMatrices2 = m_fMatrices[m_iCurrentMatrices[iNode2]][iNode2];
    			//m_pfPartials3 = m_fPartials[m_iCurrentPartials[iNode3]][iNode3];
    			calcAllMatrixSPP(m_nNrOfID[m_iCurrentPartials[iNode3]][iNode3]);
    			break;
    		case OPERATION_PP:
    			u = 0;
    			m_pfPartials1 = m_fPartials[m_iCurrentPartials[iNode1]][iNode1];
    			//m_pfMatrices1 = m_fMatrices[m_iCurrentMatrices[iNode1]][iNode1];
    			m_pfPartials2 = m_fPartials[m_iCurrentPartials[iNode2]][iNode2];
    			//m_pfMatrices2 = m_fMatrices[m_iCurrentMatrices[iNode2]][iNode2];
    			//m_pfPartials3 = m_fPartials[m_iCurrentPartials[iNode3]][iNode3];
    			calcAllMatrixPPP(m_nNrOfID[m_iCurrentPartials[iNode3]][iNode3]);
    			break;
    		}
    	}
    	m_nTopOfStack = 0;







		double[] fInPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
		int [] nID = m_nID[m_iCurrentPartials[iNode]][iNode];
		int u = 0;
		int v = 0;
		for (int k = 0; k < m_nPatterns; k++) {
			v = 0*m_nStates + nID[k]*m_nStates * m_nMatrixCount;
			for (int i = 0; i < m_nStates; i++) {
				fOutPartials[u] = fInPartials[v] * fProportions[0];
				u++;
				v++;
			}
		}


		for (int l = 1; l < m_nMatrixCount; l++) {
			u=0;
			for (int k = 0; k < m_nPatterns; k++) {
				v = l*m_nStates + nID[k]*m_nStates * m_nMatrixCount;
				for (int i = 0; i < m_nStates; i++) {
					fOutPartials[u] += fInPartials[v] * fProportions[l];
					u++;
					v++;
				}
			}
		}
	}

	/**
	 * Calculates pattern log likelihoods at a node.
	 * @param fPartials the partials used to calculate the likelihoods
	 * @param fFrequencies an array of state frequencies
	 * @param fOutLogLikelihoods an array into which the likelihoods will go
	 */
	public void calculateLogLikelihoods(double[] fPartials, double[] fFrequencies, double[] fOutLogLikelihoods)
	{
        int v = 0;
		for (int k = 0; k < m_nPatterns; k++) {

            double sum = 0.0;
			for (int i = 0; i < m_nStates; i++) {
				sum += fFrequencies[i] * fPartials[v];
				v++;
			}
            fOutLogLikelihoods[k] = Math.log(sum) + getLogScalingFactor(k);
		}
	}



    /**
     * initializes partial likelihood arrays.
     *
     * @param nNodeCount           the number of nodes in the beast.tree
     * @param nPatternCount        the number of patterns
     * @param nMatrixCount         the number of matrices (i.e., number of categories)
     * @param bIntegrateCategories whether sites are being integrated over all matrices
     */
    public void initialize(int nNodeCount, int nPatternCount, int nMatrixCount, boolean bIntegrateCategories) {

        this.m_nNodes = nNodeCount;
        this.m_nPatterns = nPatternCount;
        this.m_nMatrixCount = nMatrixCount;

        this.m_bIntegrateCategories = bIntegrateCategories;

        if (bIntegrateCategories) {
            m_nPartialsSize = nPatternCount * m_nStates * nMatrixCount;
        } else {
            m_nPartialsSize = nPatternCount * m_nStates;
        }

        m_fPartials = new double[2][nNodeCount][];

        m_iCurrentMatrices = new int[nNodeCount];
        m_iStoredMatrices = new int[nNodeCount];

        m_iCurrentPartials = new int[nNodeCount];
        m_iStoredPartials = new int[nNodeCount];

        m_iStates = new int[nNodeCount][];

        for (int i = 0; i < nNodeCount; i++) {
            m_fPartials[0][i] = null;
            m_fPartials[1][i] = null;

            m_iStates[i] = null;
        }

        m_nMatrixSize = (m_nStates+1) * (m_nStates+1);

        m_fMatrices = new double[2][nNodeCount][nMatrixCount * m_nMatrixSize];

        m_nNrOfID = new int[2][nNodeCount];
        m_nID = new int[2][nNodeCount][nPatternCount];
        //m_nIDMap =  new int[nNodeCount][][];
    	//m_pStates1 = new int[m_nPatterns];
    	//m_pStates2 = new int[m_nPatterns];

    	m_nTopOfStack = 0;
    	m_nOperation    = new int[nNodeCount]; // #nodes
    	m_nNode1        = new int[nNodeCount];// #nodes
    	m_nNode2        = new int[nNodeCount];// #nodes
    	m_nNode3        = new int[nNodeCount];// #nodes
    	m_nStates1      = new int[nNodeCount][nPatternCount];// #nodes x #patterns
    	m_nStates2      = new int[nNodeCount][nPatternCount];// #nodes x #patterns
//        if (m_nMatrixCount == 1) {
//        	m_innerLoopCalculator = new InnerLoopCalculatorSingleMatrix();
//        } else {
//        	m_innerLoopCalculator = new InnerLoopCalculator();
//        }
    }

    /**
     * cleans up and deallocates arrays.
     */
    public void finalize() throws java.lang.Throwable  {

        m_nNodes = 0;
        m_nPatterns = 0;
        m_nMatrixCount = 0;

        m_fPartials = null;
        m_iCurrentPartials = null;
        m_iStoredPartials = null;
        m_iStates = null;
        m_fMatrices = null;
        m_iCurrentMatrices = null;
        m_iStoredMatrices = null;

        m_fScalingFactors = null;
    }

    public void setUseScaling(double fScale) {
        this.m_bUseScaling = (fScale != 1.0);

        if (m_bUseScaling) {
            m_fScalingFactors = new double[2][m_nNodes][m_nPatterns];
        }
    }

    /**
     * Allocates partials for a node
     */
    public void createNodePartials(int iNode) {
        this.m_fPartials[0][iNode] = new double[m_nPartialsSize];
        this.m_fPartials[1][iNode] = new double[m_nPartialsSize];
    }

    /**
     * Sets partials for a node
     */
    public void setNodePartials(int iNode, double[] fPartials) {
    	m_nNrOfID[m_iCurrentPartials[iNode]][iNode] = 0;

        if (this.m_fPartials[0][iNode] == null) {
            createNodePartials(iNode);
        }
        //if (fPartials.length < m_nPartialsSize) {
            for (int k = 0; k < m_nPatterns; k++) {
            	for (int l = 0; l < m_nMatrixCount; l++) {
    				int u = l*m_nStates + k*m_nStates * m_nMatrixCount;
    				int v = k*m_nStates;
            		for (int i = 0; i < m_nStates; i++) {
            			m_fPartials[0][iNode][u] = fPartials[v];
            			u++;
            			v++;
            		}
            	}
            }
                //System.arraycopy(fPartials, 0, this.m_fPartials[0][iNode], k, fPartials.length);
                //k += fPartials.length;
        //} else {
        //	System.err.println("RRB: should not get here, following line is invalid");
        //   System.arraycopy(fPartials, 0, this.m_fPartials[0][iNode], 0, fPartials.length);
        //}
    }

    /**
     * Allocates states for a node
     */
    public void createNodeStates(int iNode) {

        this.m_iStates[iNode] = new int[m_nPatterns];
    }

    /**
     * Sets states for a node
     */
    public void setNodeStates(int iNode, int[] iStates) {

        if (this.m_iStates[iNode] == null) {
            createNodeStates(iNode);
        }
        System.arraycopy(iStates, 0, this.m_iStates[iNode], 0, m_nPatterns);
        for (int i = 0; i < m_nPatterns; i++) {
        	if (iStates[i] < m_nStates) {
        		m_iStates[iNode][i] = iStates[i];
        	} else {
        		m_iStates[iNode][i] = m_nStates;
        	}
        }
    }

    /**
     * Gets states for a node
     */
    public void getNodeStates(int iNode, int[] iStates) {
        System.arraycopy(this.m_iStates[iNode], 0, iStates, 0, m_nPatterns);
    }

    public void setNodeMatrixForUpdate(int iNode) {
        m_iCurrentMatrices[iNode] = 1 - m_iCurrentMatrices[iNode];

    }


    /**
     * Sets probability matrix for a node
     */
    public void setNodeMatrix(int iNode, int iMatrixIndex, double[] fMatrix) {
    	for (int i = 0; i < m_nStates; i++) {
	        System.arraycopy(fMatrix, i*m_nStates, m_fMatrices[m_iCurrentMatrices[iNode]][iNode],
	                iMatrixIndex * m_nMatrixSize+i*(m_nStates+1), m_nStates);
	        m_fMatrices[m_iCurrentMatrices[iNode]][iNode][iMatrixIndex * m_nMatrixSize+i*(m_nStates+1)+m_nStates]=1.0;
    	}
    	for (int i = 0; i < m_nStates+1; i++) {
	        m_fMatrices[m_iCurrentMatrices[iNode]][iNode][iMatrixIndex * m_nMatrixSize+m_nStates*(m_nStates+1)+i]=1.0;
    	}
    }

    /**
     * Gets probability matrix for a node
     */
    public void getNodeMatrix(int iNode, int iMatrixIndex, double[] fMatrix) {
    	System.err.println("getNodeMatrix call is invalid");
        System.arraycopy(m_fMatrices[m_iCurrentMatrices[iNode]][iNode],
                iMatrixIndex * m_nMatrixSize, fMatrix, 0, m_nMatrixSize);
    }

    public void setNodePartialsForUpdate(int iNode) {
        m_iCurrentPartials[iNode] = 1 - m_iCurrentPartials[iNode];
    }



    /**
     * Sets the currently updating node partials for node nodeIndex. This may
     * need to repeatedly copy the partials for the different category partitions
     */
    public void setCurrentNodePartials(int iNode, double[] fPartials) {
        //if (fPartials.length < m_nPartialsSize) {
        //    int k = 0;
        //    for (int i = 0; i < m_nMatrixCount; i++) {
        //        System.arraycopy(fPartials, 0, this.m_fPartials[m_iCurrentPartialsIndices[iNode]][iNode], k, fPartials.length);
        //        k += fPartials.length;
        //    }
        //} else {
        //    System.arraycopy(fPartials, 0, this.m_fPartials[m_iCurrentPartialsIndices[iNode]][iNode], 0, fPartials.length);
        //}
        for (int k = 0; k < m_nPatterns; k++) {
        	for (int l = 0; l < m_nMatrixCount; l++) {
				int u = l*m_nStates + k*m_nStates * m_nMatrixCount;
				int v = k*m_nStates;
        		for (int i = 0; i < m_nStates; i++) {
        			m_fPartials[m_iCurrentPartials[iNode]][iNode][u] = fPartials[v];
        			u++;
        			v++;
        		}
        	}
        }
    }


    /**
     * Calculates partial likelihoods at a node.
     *
     * @param iNode1 the 'child 1' node
     * @param iNode2 the 'child 2' node
     * @param iNode3 the 'parent' node
     * @param iMatrixMap  a map of which matrix to use for each pattern (can be null if integrating over categories)
     */
  public void calculatePartials(int iNode1, int iNode2, int iNode3, int[] iMatrixMap) {
	  System.err.println("calculatePartials(int, int, int ,int[]) not implemented yet");
  }
//    public void calculatePartials(int iNode1, int iNode2, int iNode3, int[] iMatrixMap) {
//        if (m_iStates[iNode1] != null) {
//            if (m_iStates[iNode2] != null) {
//                calculateStatesStatesPruning(
//                        m_iStates[iNode1], m_fMatrices[m_iCurrentMatricesIndices[iNode1]][iNode1],
//                        m_iStates[iNode2], m_fMatrices[m_iCurrentMatricesIndices[iNode2]][iNode2],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode3]][iNode3], iMatrixMap);
//            } else {
//                calculateStatesPartialsPruning(
//                        m_iStates[iNode1], m_fMatrices[m_iCurrentMatricesIndices[iNode1]][iNode1],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode2]][iNode2], m_fMatrices[m_iCurrentMatricesIndices[iNode2]][iNode2],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode3]][iNode3], iMatrixMap);
//            }
//        } else {
//            if (m_iStates[iNode2] != null) {
//                calculateStatesPartialsPruning(
//                        m_iStates[iNode2], m_fMatrices[m_iCurrentMatricesIndices[iNode2]][iNode2],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode1]][iNode1], m_fMatrices[m_iCurrentMatricesIndices[iNode1]][iNode1],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode3]][iNode3], iMatrixMap);
//            } else {
//                calculatePartialsPartialsPruning(
//                        m_fPartials[m_iCurrentPartialsIndices[iNode1]][iNode1], m_fMatrices[m_iCurrentMatricesIndices[iNode1]][iNode1],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode2]][iNode2], m_fMatrices[m_iCurrentMatricesIndices[iNode2]][iNode2],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode3]][iNode3], iMatrixMap);
//            }
//        }
//
//        if (m_bUseScaling) {
//            scalePartials(iNode3);
//        }
//    }





    /**
     * Scale the partials at a given node. This uses a scaling suggested by Ziheng Yang in
     * Yang (2000) J. Mol. Evol. 51: 423-432
     * <p/>
     * This function looks over the partial likelihoods for each state at each pattern
     * and finds the largest. If this is less than the scalingThreshold (currently set
     * to 1E-40) then it rescales the partials for that pattern by dividing by this number
     * (i.e., normalizing to between 0, 1). It then stores the log of this scaling.
     * This is called for every internal node after the partials are calculated so provides
     * most of the performance hit. Ziheng suggests only doing this on a proportion of nodes
     * but this sounded like a headache to organize (and he doesn't use the threshold idea
     * which improves the performance quite a bit).
     *
     * @param iNode
     */
    protected void scalePartials(int iNode) {
        int u = 0;
//      for (int i = 0; i < m_nPatternCount; i++) {

        for (int i = 0; i < m_nNrOfID[m_iCurrentPartials[iNode]][iNode]; i++) {
            double scaleFactor = 0.0;
            int v = u;
            for (int k = 0; k < m_nMatrixCount; k++) {
                for (int j = 0; j < m_nStates; j++) {
                    if (m_fPartials[m_iCurrentPartials[iNode]][iNode][v] > scaleFactor) {
                        scaleFactor = m_fPartials[m_iCurrentPartials[iNode]][iNode][v];
                    }
                    v++;
                }
                v += (m_nPatterns - 1) * m_nStates;
            }

            if (scaleFactor < m_fScalingThreshold) {

                v = u;
                for (int k = 0; k < m_nMatrixCount; k++) {
                    for (int j = 0; j < m_nStates; j++) {
                        m_fPartials[m_iCurrentPartials[iNode]][iNode][v] /= scaleFactor;
                        v++;
                    }
                    v += (m_nPatterns - 1) * m_nStates;
                }
                m_fScalingFactors[m_iCurrentPartials[iNode]][iNode][i] = Math.log(scaleFactor);

            } else {
                m_fScalingFactors[m_iCurrentPartials[iNode]][iNode][i] = 0.0;
            }
            u += m_nStates;


        }
    }

    /**
     * This function returns the scaling factor for that pattern by summing over
     * the log scalings used at each node. If scaling is off then this just returns
     * a 0.
     *
     * @return the log scaling factor
     */
    public double getLogScalingFactor(int iPattern) {
        double logScalingFactor = 0.0;
        if (m_bUseScaling) {
            for (int i = 0; i < m_nNodes; i++) {
                logScalingFactor += m_fScalingFactors[m_iCurrentPartials[i]][i][iPattern];
            }
        }
        return logScalingFactor;
    }

    /**
     * Gets the partials for a particular node.
     *
     * @param iNode   the node
     * @param fOutPartials an array into which the partials will go
     */
    public void getPartials(int iNode, double[] fOutPartials) {
        double[] partials1 = m_fPartials[m_iCurrentPartials[iNode]][iNode];

        System.arraycopy(partials1, 0, fOutPartials, 0, m_nPartialsSize);
    }

    /**
     * Store current state
     */
    public void store() {

        System.arraycopy(m_iCurrentMatrices, 0, m_iStoredMatrices, 0, m_nNodes);
        System.arraycopy(m_iCurrentPartials, 0, m_iStoredPartials, 0, m_nNodes);
    }

    public void unstore() {
        System.arraycopy(m_iStoredMatrices, 0, m_iCurrentMatrices, 0, m_nNodes);
        System.arraycopy(m_iStoredPartials, 0, m_iCurrentPartials, 0, m_nNodes);
    }

    /**
     * Restore the stored state
     */
    public void restore() {
        // Rather than copying the stored stuff back, just swap the pointers...
        int[] iTmp1 = m_iCurrentMatrices;
        m_iCurrentMatrices = m_iStoredMatrices;
        m_iStoredMatrices = iTmp1;

        int[] iTmp2 = m_iCurrentPartials;
        m_iCurrentPartials = m_iStoredPartials;
        m_iStoredPartials = iTmp2;
    }

//	@Override
//	LikelihoodCore feelsGood() {
//		return null;
//	}
} // class BeerLikelihoodCore
