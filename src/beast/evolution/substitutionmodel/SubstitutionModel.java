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
package beast.evolution.substitutionmodel;

import beast.core.*;
import beast.core.Input.Validate;


@Description("Specifies transition probability matrix for a given distance")
public interface SubstitutionModel {

    /**
     * get the complete transition probability matrix for the given distance
     *
     * @param substitutions the expected number of substitutions
     * @param matrix        an array to store the matrix
     */
    void getTransitionProbabilities(double substitutions, double[] matrix);
    //void getPaddedTransitionProbabilities(double substitutions, double[] matrix);

    /**
     * This function returns the Eigen decomposition of the instantaneous rate matrix if available.
     *
     * @return the EigenDecomposition, null if not available
     */
    EigenDecomposition getEigenDecomposition();

    public abstract class Base extends CalculationNode implements SubstitutionModel {
        public Input<Frequencies> frequencies =
                new Input<Frequencies>("frequencies", "substitution model equilibrium state frequencies", Validate.REQUIRED);

    }
}
