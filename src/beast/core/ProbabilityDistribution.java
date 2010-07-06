/*
* File Uncertainty.java
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
package beast.core;

import java.util.List;
import java.util.Random;

@Description("Probabilistic representation that can produce " +
        "a log probability for running an MCMC chain.")
public abstract class ProbabilityDistribution extends Plugin implements Cacheable {

    /**
     * current and stored log probability/log likelihood/log uncertainty *
     */
    protected double logP = 0;
    private double storedLogP = 0;

    /**
     * @param state a state
     * @return the normalized probability (density) for this distribution
     * @throws Exception an exception
     */
    public double calculateLogP(State state) throws Exception {
        logP = 0;
        return logP;
    }

    /**
     * This method modifies the portion of the provided state that corresponds to the
     * probability distributions arguments, by drawing new values conditional on the
     * parameters conditioned on
     *
     * @param state the state
     */
    public abstract void sample(State state, Random random);

    /**
     * @return a list of unique ids for the state nodes that form the argument
     */
    public abstract List<String> getArguments();

    /**
     * @return a list of unique ids for the state nodes that make up the conditions
     */
    public abstract List<String> getConditions();

    /**
     * get result from last known calculation *
     *
     * @return log probability
     */
    public double getCurrentLogP() {
        return logP;
    }

    @Override
    public void initAndValidate(State state) throws Exception {
        // nothing to do
    }

    public void store(int nSample) {
        //super.store(nSample);
        storedLogP = logP;
    }

    public void restore(int nSample) {
        //super.restore(nSample);
        logP = storedLogP;
    }

    public void prepare(State state) {
    }


} // class ProbabilityDistribution
