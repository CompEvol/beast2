/*
* File GeneralSubstitutionModel.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is not copyright Remco! It is copied from BEAST 1.
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

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.State;
import beast.core.parameter.RealParameter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Description("Specifies transition probability matrix with no restrictions on the rates other " +
        "than that one of the is equal to one and the others are specified relative to " +
        "this unit rate. Works for any number of states.")
public class GeneralSubstitutionModel extends SubstitutionModel.Base {
    public Input<RealParameter> rates =
            new Input<RealParameter>("rates", "rate parameter which defines the transition rate matrix", Validate.REQUIRED);

    @Override
    public void initAndValidate() throws Exception {
        updateMatrix = true;

        eigenSystem = getDefaultEigenSystem(frequencies.get().getFreqs().length);
    } // initAndValidate

    private double[] relativeRates;
    private double[] storedRelativeRates;

    private EigenSystem eigenSystem;
    
    private EigenDecomposition eigenDecomposition;
    private EigenDecomposition storedEigenDecomposition;

    private boolean updateMatrix = true;
    private boolean storedUpdateMatrix = true;

    public void getTransitionProbabilities(double distance, double[] matrix) {
    }

    public EigenDecomposition getEigenDecomposition() {
        return null;
    }

    protected EigenSystem getDefaultEigenSystem(int stateCount) {
        return new DefaultEigenSystem(stateCount);
    }

    public void store(int stateNumber) {

        storedUpdateMatrix = updateMatrix;

//        System.arraycopy(relativeRates, 0, storedRelativeRates, 0, rateCount);

        storedEigenDecomposition = eigenDecomposition.copy();
    }

    /**
     * Restore the additional stored state
     */
    public void restore(int stateNumber) {

        updateMatrix = storedUpdateMatrix;

        // To restore all this stuff just swap the pointers...
        double[] tmp1 = storedRelativeRates;
        storedRelativeRates = relativeRates;
        relativeRates = tmp1;

        EigenDecomposition tmp = storedEigenDecomposition;
        storedEigenDecomposition = eigenDecomposition;
        eigenDecomposition = tmp;

    }

}
