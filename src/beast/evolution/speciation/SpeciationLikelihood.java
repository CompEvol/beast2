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

import beast.core.Description;
import beast.core.State;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreePrior;

import java.util.List;
import java.util.Random;

/**
 * Ported from Beast 1.6
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: SpeciationLikelihood.java,v 1.10 2005/05/18 09:51:11 rambaut Exp $
 */
@Description("A likelihood function for speciation processes.")
abstract public class SpeciationLikelihood extends TreePrior {
//	SpeciesTreeDistribution extends TreeDistribution
	

   /**
     * Calculates the log likelihood of this set of coalescent intervals,
     * given a demographic model.
     *
     * @return the log likelihood
     */
	@Override
    public double calculateLogP() {
      	if (isDirtyCalculation()) {
          	Tree tree = m_tree.get();
  		    logP = calculateTreeLogLikelihood(tree);
        }
        return logP;
    } // calculateLogP
	
	
	/** 
     * Generic likelihood calculation
     * @return log-likelihood of density
     */
	abstract double calculateTreeLogLikelihood(Tree tree);

    // ****************************************************************
    // Private and protected stuff
    // ****************************************************************



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
        throw new UnsupportedOperationException("This should eventually sample a tree conditional on provided speciation model.");
	}
}