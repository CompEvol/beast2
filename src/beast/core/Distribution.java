/*
* File Distribution.java
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

import java.io.PrintStream;
import java.util.List;
import java.util.Random;

@Description("Probabilistic representation that can produce " +
        "a log probability for instance for running an MCMC chain.")
public abstract class Distribution extends CalculationNode implements Loggable, Function {

    /**
     * current and stored log probability/log likelihood/log distribution *
     */
    protected double logP = Double.NaN;
    protected double storedLogP = Double.NaN;

    /**
     * @return the normalised probability (density) for this distribution.
     *         Note that some efficiency can be gained by testing whether the
     *         Distribution is dirty, and if not, call getCurrentLogP() instead
     *         of recalculating.
     */
    public double calculateLogP() {
        logP = 0;
        return logP;
    }

    /** The beastObject implements f( arguments | conditionals) **/

    /**
     * @return a list of unique ids for the state nodes that form the argument
     */
    public abstract List<String> getArguments();

    /**
     * @return a list of unique ids for the state nodes that make up the conditions
     */
    public abstract List<String> getConditions();

    /**
     * This method draws new values for the arguments conditional on the current value(s) of the conditionals.
     * <p/>
     * The new values are overwrite the argument values in the provided state.
     *
     * @param state  the state
     * @param random random number generator
     */
    public abstract void sample(State state, Random random);

    /**
     * Sample input parameter from its distribution (if one exists)
     *
     * @param inputName name of input
     * @param inputValue BEASTInterface value of input
     * @param state state object (needed for call to sample)
     * @param random random object (needed for call to sample)
     * @return Distribution object or null if none found.
     */
    public void sampleInputDistribution(String inputName, StateNode inputValue, State state, Random random) {
        for (BEASTInterface output : inputValue.getOutputs()) {
            if (output instanceof Distribution) {
                Distribution distrib = (Distribution) output;
                List<String> distribArgs = distrib.getArguments();
                if (distribArgs != null && distribArgs.contains(inputName)) {
                    distrib.sample(state, random);
                    break;
                }
            }
        }
    }

    /**
     * get result from last known calculation, useful for logging
     *
     * @return log probability
     */
    public double getCurrentLogP() {
        return logP;
    }

    @Override
    public void initAndValidate() {
        // nothing to do
    }

    /**
     * CalculationNode methods *
     */
    @Override
    public void store() {
        storedLogP = logP;
        super.store();
    }

    @Override
    public void restore() {
        logP = storedLogP;
        super.restore();
    }

    /**
     * Loggable interface implementation follows *
     */
    @Override
    public void init(final PrintStream out) {
        out.print(getID() + "\t");
    }

    @Override
    public void log(final int sample, final PrintStream out) {
        out.print(getCurrentLogP() + "\t");
    }

    @Override
    public void close(final PrintStream out) {
        // nothing to do
    }

    /**
     * Valuable interface implementation follows *
     */
    @Override
    public int getDimension() {
        return 1;
    }

    @Override
    public double getArrayValue() {
        return logP;
    }

    @Override
    public double getArrayValue(final int dim) {
        if (dim == 0) return getArrayValue();
        return 0;
    }

    /**
     * Intended to be overridden by stochastically estimated distributions.
     * Used to disable target distribution consistency checks implemented in
     * the MCMC class which do not apply to stochastic distributions.
     *
     * @return true if stochastic.
     */
    public boolean isStochastic() {
        return false;
    }


    /**
     * Return non-stochastic part of a distribution recalculate, if required. 
     * This can be used for debugging purposes to verify that the non-stochastic 
     * part of a distribution is calculated correctly e.g. inside the MCMC loop
     *
     * @return logP if not stochastic, zero otherwise
     */
    public double getNonStochasticLogP() {
        if (isStochastic()) {
            return 0;
        } else {
            if (isDirtyCalculation()) {
                return calculateLogP();
            } else {
                return getCurrentLogP();
            }
        }
    }

} // class Distribution
