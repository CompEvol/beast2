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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
     * Recursively sample the state nodes that this distribution is conditional on.
     * @param state the state to pass to distribution sample methods that this distribution is based on.
     * @param random random number generator
     */
    public final void sampleConditions(State state, Random random) {

        ArrayList<Input> conditionInputs = new ArrayList<>();

        // get all the inputs this distribution is conditional on
        for (String id : getConditions()) {
            for (Input input : getInputs().values()) {
                Object value = input.get();
                if (value != null && value instanceof BEASTInterface) {
                    String valueID = ((BEASTInterface) value).getID();
                    if (valueID != null && valueID.equals(id)) {
                        conditionInputs.add(input);
                    }
                }
            }
        }

        // for each calculation node or state node that this distribution is conditional on, sample the condition.
        for (Input inputCondition : conditionInputs) {
            Object value = inputCondition.get();

            if (value instanceof StateNode) {
                sampleStateNode((StateNode) value, state, random);
            } else if (value instanceof CalculationNode) {
                sampleCalculationNode((CalculationNode)value, state, random);
            }
        }
    }

    /**
     * Given a calculation node this method recursively finds all calculation node inputs that this calculation node is
     * dependent on and calls sampleStateNode or sampleCalculationNode on such inputs recursively.
     *
     * @param calculationNode the state node to be sampled
     * @param state the state to record the new sample value in
     * @param random a random number generator
     */
    private void sampleCalculationNode(CalculationNode calculationNode, State state, Random random) {

        // recursively sample conditions and state nodes
        for (Input input : calculationNode.getInputs().values()) {
            Object value = input.get();
            if (value != null && value instanceof StateNode) {
                sampleStateNode((StateNode) value, state, random);
            } else if (value != null && value instanceof CalculationNode) {
                sampleCalculationNode((CalculationNode) value, state, random);
            }
        }
    }

    /**
     * Given a state node this method finds the first distribution whose argument matches the ID of this state node
     * and calls sample(state, random) on that distribution.
     *
     * @param stateNode the state node to be sampled
     * @param state the state to record the new sample value in
     * @param random a random number generator
     */
    private void sampleStateNode(StateNode stateNode, State state, Random random) {
        // find distribution governing this state node and re-sample.
        for (BEASTInterface output : stateNode.getOutputs()) {
            if (output instanceof Distribution) {
                Distribution distrib = (Distribution) output;
                List<String> distribArgs = distrib.getArguments();
                if (distribArgs != null && distribArgs.contains(stateNode.getID())) {
                    distrib.sample(state, random);
                    break;
                }
            }
        }
    }

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
     * Field for keeping track of whether a sample has been drawn from this distribution
     * during this sample run.
     */
    public boolean sampledFlag = false;

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
