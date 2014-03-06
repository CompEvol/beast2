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



import java.util.List;
import java.util.Random;

import beast.core.Description;
import beast.core.State;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeDistribution;
import beast.evolution.tree.TreeInterface;


/**
 * Ported from Beast 1.6
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: SpeciationLikelihood.java,v 1.10 2005/05/18 09:51:11 rambaut Exp $
 */
@Description("A likelihood function for speciation processes.")
abstract public class SpeciesTreeDistribution extends TreeDistribution {
//	SpeciesTreeDistribution extends TreeDistribution

    /**
     * Calculates the log likelihood of this set of coalescent intervals,
     * given a demographic model.
     *
     * @return the log likelihood
     */
    @Override
    public double calculateLogP() {
        // (Q2R): what if tree intervals?
        // (Q2R): always the same tree, no? so why pass in argument
        final TreeInterface tree = treeInput.get();
        logP = calculateTreeLogLikelihood(tree);
        return logP;
    } // calculateLogP


    /**
     * Generic likelihood calculation
     *
     * @param tree
     * @return log-likelihood of density
     */
    public abstract double calculateTreeLogLikelihood(TreeInterface tree);

    // ****************************************************************
    // Private and protected stuff
    // ****************************************************************


    /*****************************************/
    /** Distribution implementation follows **/
    /**
     * *************************************
     */
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