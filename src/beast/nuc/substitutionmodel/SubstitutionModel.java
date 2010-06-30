
/*
 * File SubstitutionModel.java
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
package beast.nuc.substitutionmodel;

import beast.nuc.Frequencies;
import beast.core.Description;
import beast.core.Input;
import beast.core.Plugin;
import beast.core.State;
import beast.core.Input.Validate;


@Description("Specifies transition probability matrix for a given distance")
public class SubstitutionModel extends Plugin {
	public Input<Frequencies> m_pFreqs = new Input<Frequencies>("frequencies","frequencies of characters", Validate.REQUIRED);

    /**
     * get the complete transition probability matrix for the given distance
     *
     * @param distance the expected number of substitutions
     * @param matrix   an array to store the matrix
     */
    public void getTransitionProbabilities(double substitutions, double[] matrix, State state) {
    }

    /** return true if state is changed such that
     * the internal state of this substitution model
     * needs to be recalculated. Set flag if
     * recalculation is required.
     */
    public boolean isDirty(State state) {
    	return false;
    }

}
