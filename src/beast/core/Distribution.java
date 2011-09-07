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
public abstract class Distribution extends CalculationNode implements Loggable, Valuable {

    /**
     * current and stored log probability/log likelihood/log distribution *
     */
    protected double logP = Double.NaN;
    protected double storedLogP = Double.NaN;

    /**
     * @return the normalised probability (density) for this distribution.
     * Note that some efficiency can be gained by testing whether the
     * Distribution is dirty, and if not, call getCurrentLogP() instead
     * of recalculating.
     * @throws Exception an exception
     */
    public double calculateLogP() throws Exception {
        logP = 0;
        return logP;
    }

    /** The plugin implements f( arguments | conditionals) **/

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
     *
     * The new values are overwrite the argument values in the provided state.
     *
     * @param state the state
     * @param random  random number generator
     */
    public abstract void sample(State state, Random random);

    /**
     * get result from last known calculation, useful for logging
     *
     * @return log probability
     */
    public double getCurrentLogP() {
        return logP;
    }

    @Override
    public void initAndValidate() throws Exception {
        // nothing to do
    }

    /** CalculationNode methods **/
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

    /** Loggable interface implementation follows **/
    @Override
	public void init(PrintStream out) throws Exception {
		out.print(getID() + "\t");
	}

    @Override
	public void log(int nSample, PrintStream out) {
		out.print(getCurrentLogP() + "\t");
	}

    @Override
	public void close(PrintStream out) {
		// nothing to do
	}
    
    /** Valuable interface implementation follows **/
    @Override
	public int getDimension() {return 1;}
    @Override
	public double getArrayValue() {return logP;}
    @Override
	public double getArrayValue(int iDim) {if (iDim == 0) return getArrayValue(); return 0;}

} // class ProbabilityDistribution
