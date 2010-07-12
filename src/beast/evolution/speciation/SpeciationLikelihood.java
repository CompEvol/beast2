/*
 * SpeciationLikelihood.java
 *
 * Copyright (C) 2002-2009 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package beast.evolution.speciation;

import beast.core.Cacheable;
import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.core.Input.Validate;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.math.distributions.Distribution;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Ported from Beast 1.6
 * Parts of this class were derived from C++ code provided by Oliver Pybus.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: SpeciationLikelihood.java,v 1.10 2005/05/18 09:51:11 rambaut Exp $
 */
@Description("A likelihood function for speciation processes.")
public class SpeciationLikelihood extends beast.core.Distribution implements Cacheable {

	public Input<Tree> m_tree = new Input<Tree>("tree", "species tree over which to calculate speciation likelihood", Validate.REQUIRED);
	
//	@Override
//	public void initAndValidate(State state) throws Exception {
//		// nothing to do
//	}

   /**
     * Calculates the log likelihood of this set of coalescent intervals,
     * given a demographic model.
     *
     * @return the log likelihood
     */
	@Override
    public final double calculateLogP(State state) {
      	Tree stateTree = (Tree)state.getStateNode(m_tree);
      	
//        if (m_bIsDirty || stateTree.isDirty()) {
        if (isDirty(stateTree.getRoot())) {
//          if (exclude != null) {
//          logP = calculateTreeLogLikelihood(tree, exclude);
//          }
  		    logP = calculateTreeLogLikelihood(stateTree, state);
//            m_bIsDirty = false;
        }
        return logP;
    } // calculateLogP
	
	// this is a bit crude.
	// the tree should really know whether it is dirty all by itself.
	boolean isDirty(Node node) {
		if (node.isDirty() != Tree.IS_CLEAN) {
			return true;
		}
		if (node.isLeaf()) {
			return false;
		}
		return isDirty(node.m_left) || isDirty(node.m_right);  
	}
	
	/** 
     * Generic likelihood calculation
     * @return log-likelihood of density
     */
	double calculateTreeLogLikelihood(Tree tree, State state) {
		return 0;
	}

//    public final void makeDirty() {
//    	m_bIsDirty = false;
//    }
//
//
//    /** flag to indicate internal state needs to be recalculated **/
//    boolean m_bIsDirty = true;

    // ****************************************************************
    // Private and protected stuff
    // ****************************************************************

    private boolean storedLikelihoodKnown = false;

    /******************************/
    /** Cacheable implementation **/
    /******************************/

    /**
     * Stores the precalculated state: likelihood
     */
	@Override
    public void store(int nSample) {
		super.store(nSample);
    }

    /**
     * Restores the precalculated state: computed likelihood
     */
	@Override
    public void restore(int nSample) {
		super.restore(nSample);
    }

    /*****************************************/
    /** Distribution implementation follows **/
    /*****************************************/
    @Override
	public List<String> getArguments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getConditions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sample(State state, Random random) {
		// TODO Auto-generated method stub
		
	}
}