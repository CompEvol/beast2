
/*
 * File SnAPTreeLikelihood.java
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



import beast.app.BeastMCMC;
import beast.core.Data;
import beast.core.Description;
import beast.core.Input;
import beast.core.Uncertainty;
import beast.core.Parameter;
import beast.core.State;
import beast.core.Tree;

import snap.NodeData;


@Description("Implements a beast.tree Likelihood Function for Single Site Sorted-sequences on a beast.tree.")
public class SnAPTreeLikelihood extends Uncertainty {

//	int m_nU;
//	int m_nV;
	Data m_data;
	int[] m_nSampleSizes;
	SnAPLikelihoodCore m_core;

	public Input<Data> m_pData = new Input<Data>("data", "set of alignments");
	public Input<Tree> m_pTree = new Input<Tree>("tree", "beast.tree with phylogenetic relations");
	public Input<Parameter> m_pU = new Input<Parameter>("mutationRateU", "mutation rate from red to green?");
	public Input<Parameter> m_pV = new Input<Parameter>("mutationRateV", "mutation rate from green to red?");


    /**
     * Constructor.
     */
	public SnAPTreeLikelihood() {
    }


    //public SSSTreeLikelihood(Data patternList, State state) {
    @Override
    public void initAndValidate(State state) {
    	m_data = m_pData.get();
    	if ( BeastMCMC.m_nThreads == 1) {
    		m_core = new SnAPLikelihoodCore(m_pTree.get().getRoot(), m_pData.get());
    	} else {
    		m_core = new SnAPLikelihoodCoreT(m_pTree.get().getRoot(), m_pData.get());
    	}
    	Integer [] nSampleSizes = m_data.m_nStateCounts.toArray(new Integer[0]);
    	m_nSampleSizes = new int[nSampleSizes.length];
    	for (int i = 0; i < nSampleSizes.length; i++) {
    		m_nSampleSizes[i] = nSampleSizes[i];
    	}

//    	try {
//    		m_nU = state.getParameterIndex("u");
//    		m_nV = state.getParameterIndex("v");
//    	} catch (Exception e) {
//			e.printStackTrace();
//		}
    }

   int m_nTreeID = -1;
    /**
     * Calculate the log likelihood of the current state.
     *
     * @return the log likelihood.
     */
    public double calculateLogP(State state) {
    	if (m_nTreeID < 0) {
			m_nTreeID = state.getTreeIndex(m_pTree.get().getID());
    	}
    	try {
	    	NodeData root = (NodeData) state.m_trees[m_nTreeID].getRoot();
	    	double u = state.getValue(m_pU);
	    	double v  = state.getValue(m_pV);
			boolean useCache = true;
			boolean dprint = false;
			m_fLogP = m_core.computeLogLikelihood(root, u, v,
	    			m_nSampleSizes,
	    			m_data,
	    			useCache,
	    			dprint /*= false*/);
			return m_fLogP;
    	} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
    } // calculateLogLikelihood

	@Override
	public void store(int nSample) {
    	super.store(nSample);
    	m_core.m_bReuseCache = true;
    }

	@Override
    public void restore(int nSample) {
    	super.restore(nSample);
    	m_core.m_bReuseCache = false;
    }

	@Override
	public String getCitation() {
		return "David Bryant, Remco Bouckaert, Noah Rosenberg. Inferring species trees directly from SNP and AFLP data: full coalescent analysis without those pesky gene trees. arXiv:0910.4193v1. http://arxiv.org/abs/0910.4193";
	}

} // class SSSTreeLikelihood
