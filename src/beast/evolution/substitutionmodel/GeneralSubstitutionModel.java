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
import beast.core.Valuable;
import beast.core.Input.Validate;

@Description("Specifies transition probability matrix with no restrictions on the rates other " +
        "than that one of the is equal to one and the others are specified relative to " +
        "this unit rate. Works for any number of states.")
public class GeneralSubstitutionModel extends SubstitutionModel.Base {
    public Input<Valuable> m_rates =
            new Input<Valuable>("rates", "Rate parameter which defines the transition rate matrix. " +
            		"Only the off-diagonal entries need to be specified (diagonal makes row sum to zero in a " +
            		"rate matrix). Entry i specifies the rate of from i%n to floor(i/(n-1)) where " +
            		"n is the number of states.", Validate.REQUIRED);

    int m_nStates;
    double [][] m_rateMatrix;
    
    @Override
    public void initAndValidate() throws Exception {
        updateMatrix = true;
        m_nStates = frequencies.get().getFreqs().length;
        if (m_rates.get().getDimension() != m_nStates * (m_nStates-1)) {
        	throw new Exception("Dimension of input 'rates' is " + m_rates.get().getDimension() + " but a " +
        			"rate matrix of dimension " + m_nStates + "x" + (m_nStates -1) + "=" + m_nStates * (m_nStates -1) + " was " +
        			"expected");
        }
        eigenSystem = new DefaultEigenSystem(m_nStates);
        m_rateMatrix = new double[m_nStates][m_nStates];
    } // initAndValidate

    private double[] relativeRates;
    private double[] storedRelativeRates;

	private EigenSystem eigenSystem;
    
    private EigenDecomposition eigenDecomposition;
    private EigenDecomposition storedEigenDecomposition;

    private boolean updateMatrix = true;
    private boolean storedUpdateMatrix = true;

    @Override
    public void getTransitionProbabilities(double distance, double[] matrix) {
        int i, j, k;
        double temp;

        // this must be synchronized to avoid being called simultaneously by
        // two different likelihood threads - AJD
        synchronized (this) {
            if (updateMatrix) {
            	setupRateMatrix();
            	eigenDecomposition = eigenSystem.decomposeMatrix(m_rateMatrix);
            	updateMatrix = false;
            }
        }

        // TODO: is the following really necessary?
        // TODO: implemented a pool of iexp matrices to support multiple threads
        // TODO: without creating a new matrix each call. - AJD
        double[] iexp = new double[m_nStates * m_nStates];
        // Eigen vectors
        double[] Evec = eigenDecomposition.getEigenVectors();
        // inverse Eigen vectors
        double[] Ievc = eigenDecomposition.getInverseEigenVectors();
        // Eigen values
        double[] Eval = eigenDecomposition.getEigenValues();
        for (i = 0; i < m_nStates; i++) {
            temp = Math.exp(distance * Eval[i]);
            for (j = 0; j < m_nStates; j++) {
                iexp[i * m_nStates + j] = Ievc[i * m_nStates + j] * temp;
            }
        }

        int u = 0;
        for (i = 0; i < m_nStates; i++) {
            for (j = 0; j < m_nStates; j++) {
                temp = 0.0;
                for (k = 0; k < m_nStates; k++) {
                    temp += Evec[i * m_nStates + k] * iexp[k * m_nStates + j];
                }

                matrix[u] = Math.abs(temp);
                u++;
            }
        }
    } // getTransitionProbabilities

    /** sets up rate matrix **/
    public void setupRateMatrix() {
    	Valuable rates = m_rates.get();
    	double [] fFreqs = frequencies.get().getFreqs();
	    for (int i = 0; i < m_nStates; i++) {
	    	m_rateMatrix[i][i] = 0;
		    for (int j = 0; j < i; j++) {
		    	m_rateMatrix[i][j] = rates.getArrayValue(i * (m_nStates -1) + j);
		    }
		    for (int j = i+1; j < m_nStates; j++) {
		    	m_rateMatrix[i][j] = rates.getArrayValue(i * (m_nStates -1) + j-1);
		    }
	    }
	    // bring in frequencies
        for (int i = 0; i < m_nStates; i++) {
            for (int j = i + 1; j < m_nStates; j++) {
            	m_rateMatrix[i][j] *= fFreqs[j];
            	m_rateMatrix[j][i] *= fFreqs[i];
            }
        }
        // set up diagonal
        for (int i = 0; i < m_nStates; i++) {
            double fSum = 0.0;
            for (int j = 0; j < m_nStates; j++) {
                if (i != j)
                    fSum += m_rateMatrix[i][j];
            }
            m_rateMatrix[i][i] = -fSum;
        }
        // normalise rate matrix to one expected substitution per unit time
        double fSubst = 0.0;
        for (int i = 0; i < m_nStates; i++)
            fSubst += -m_rateMatrix[i][i] * fFreqs[i];

        for (int i = 0; i < m_nStates; i++) {
            for (int j = 0; j < m_nStates; j++) {
            	m_rateMatrix[i][j] = m_rateMatrix[i][j] / fSubst;
            }
        }        
	} // setupRelativeRates

    
    @Override
    public EigenDecomposition getEigenDecomposition() {
        return null;
    }

    @Override
    public void store() {
        storedUpdateMatrix = updateMatrix;
        storedEigenDecomposition = eigenDecomposition.copy();
    }

    /**
     * Restore the additional stored state
     */
    @Override
    public void restore() {

        updateMatrix = storedUpdateMatrix;

        // To restore all this stuff just swap the pointers...
        double[] tmp1 = storedRelativeRates;
        storedRelativeRates = relativeRates;
        relativeRates = tmp1;

        EigenDecomposition tmp = storedEigenDecomposition;
        storedEigenDecomposition = eigenDecomposition;
        eigenDecomposition = tmp;

    }

    @Override
    protected boolean requiresRecalculation() {
    	// we only get here if something is dirty
        updateMatrix = true;
    	return true;
    }
    
} // class GeneralSubstitutionModel
