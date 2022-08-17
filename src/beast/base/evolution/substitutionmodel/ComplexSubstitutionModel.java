/*
 * ComplexSubstitutionModel.java
 *
 * Copyright (c) 2002-2015 Alexei Drummond, Andrew Rambaut and Marc Suchard
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

package beast.base.evolution.substitutionmodel;


import java.util.Arrays;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.tree.Node;

/**
 * <b>A general irreversible class for any
 * data type; allows complex eigenstructures.</b>
 *
 * @author Marc Suchard
 */

@Description("Complex-diagonalizable, irreversible substitution model")
@Citation(value = "Edwards, C. J., Suchard, M. A., Lemey, P., ... & Valdiosera, C. E. (2011).\n" +
        "Ancient hybridization and an Irish origin for the modern polar bear matriline.\n" +
        "Current Biology, 21(15), 1251-1258.",
        year = 2011, firstAuthorSurname = "Edwards", DOI="10.1016/j.cub.2011.05.058")
public class ComplexSubstitutionModel extends GeneralSubstitutionModel {
	
	@Override
	public void initAndValidate() {
        updateMatrix = true;
        frequencies = frequenciesInput.get();
        nrOfStates = frequencies.getFreqs().length;
        
        try {
			eigenSystem = createEigenSystem();
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e.getMessage());
		}

        rateMatrix = new double[nrOfStates][nrOfStates];
        relativeRates = new double[ratesInput.get().getDimension()];
        storedRelativeRates = new double[ratesInput.get().getDimension()];
	}
	
	
	@Override
	protected EigenSystem createEigenSystem() {
		int stateCount = getStateCount();
        return new ComplexColtEigenSystem(stateCount);
    }

	
    /**
     * get the complete transition probability matrix for the given distance
     *
     * @param distance the expected number of substitutions
     * @param matrix   an array to store the matrix
     */
    @Override
    public void getTransitionProbabilities(Node node, double startTime, double endTime, double rate, double[] matrix) {
        double distance = (startTime - endTime) * rate;

        synchronized (this) {
            if (updateMatrix) {
                setupRelativeRates();
                setupRateMatrix();
                eigenDecomposition = eigenSystem.decomposeMatrix(rateMatrix);
                updateMatrix = false;
            }
        }
        int stateCount = getStateCount();

        if (eigenDecomposition == null) {
            Arrays.fill(matrix, 0.0);
            return;
        }

        double[] Evec = eigenDecomposition.getEigenVectors();
        double[] Eval = eigenDecomposition.getEigenValues();
        double[] EvalImag = new double[stateCount];
        System.arraycopy(Eval, stateCount, EvalImag, 0, stateCount);
        double[] Ievc = eigenDecomposition.getInverseEigenVectors();

        double[][] iexp = new double[stateCount][stateCount];
        
        double temp;

// Eigenvalues and eigenvectors of a real matrix A.
//
// If A is symmetric, then A = V*D*V' where the eigenvalue matrix D is diagonal
// and the eigenvector matrix V is orthogonal. I.e. A = V D V^t and V V^t equals
// the identity matrix.
//
// If A is not symmetric, then the eigenvalue matrix D is block diagonal with
// the real eigenvalues in 1-by-1 blocks and any complex eigenvalues,
// lambda + i*mu, in 2-by-2 blocks, [lambda, mu; -mu, lambda]. The columns
// of V represent the eigenvectors in the sense that A*V = V*D. The matrix
// V may be badly conditioned, or even singular, so the validity of the
// equation A = V D V^{-1} depends on the conditioning of V.

        for (int i = 0; i < stateCount; i++) {

            if (EvalImag[i] == 0) {
                // 1x1 block
                temp = Math.exp(distance * Eval[i]);
                for (int j = 0; j < stateCount; j++) {
                    iexp[i][j] = Ievc[i * stateCount + j] * temp;
                }
            } else {
                // 2x2 conjugate block
                // If A is 2x2 with complex conjugate pair eigenvalues a +/- bi, then
                // exp(At) = exp(at)*( cos(bt)I + \frac{sin(bt)}{b}(A - aI)).
                int i2 = i + 1;
                double b = EvalImag[i];
                double expat = Math.exp(distance * Eval[i]);
                double expatcosbt = expat * Math.cos(distance * b);
                double expatsinbt = expat * Math.sin(distance * b);

                for (int j = 0; j < stateCount; j++) {
                    iexp[i][j] = expatcosbt * Ievc[i * stateCount + j] +
                            expatsinbt * Ievc[i2 * stateCount + j];
                    iexp[i2][j] = expatcosbt * Ievc[i2 * stateCount + j] -
                            expatsinbt * Ievc[i * stateCount + j];
                }
                i++; // processed two conjugate rows
            }
        }

        int u = 0;
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < stateCount; j++) {
                temp = 0.0;
                for (int k = 0; k < stateCount; k++) {
                    temp += Evec[i * stateCount + k] * iexp[k][j];
                }
                matrix[u] = Math.abs(temp);
                u++;
            }
        }
    }

//    protected int getRateCount(int stateCount) {
//        return (stateCount - 1) * stateCount;
//    }

    protected void setupRelativeRates(double[] rates) {
        for (int i = 0; i < rates.length; i++)
            rates[i] = ratesInput.get().getArrayValue(i);
    }

//    protected void setupQMatrix(double[] rates, double[] pi, double[][] matrix) {
//        int i, j, k = 0;
//        int stateCount = getStateCount();
//
//        for (i = 0; i < stateCount; i++) {
//            for (j = i + 1; j < stateCount; j++) {
//                double thisRate = rates[k++];
//                if (thisRate < 0.0) thisRate = 0.0;
//                matrix[i][j] = thisRate * pi[j];
//            }
//        }
//        // Copy lower triangle in column-order form (transposed)
//        for (j = 0; j < stateCount; j++) {
//            for (i = j + 1; i < stateCount; i++) {
//                double thisRate = rates[k++];
//                if (thisRate < 0.0) thisRate = 0.0;
//                matrix[i][j] = thisRate * pi[j];
//            }
//        }
//    }
//
//    public boolean canReturnComplexDiagonalization() {
//        return true;
//    }
//
//    protected double getNormalizationValue(double[][] matrix, double[] pi) {
//        double norm = 1.0;
//        if (doNormalization) {
//            norm = super.getNormalizationValue(matrix, pi);
//        }
////            return super.getNormalizationValue(matrix, pi);
////        } else {
////            return 1.0;
////        }
////        System.err.println("norm = " + doNormalization + " " + norm);
////        System.err.println(new Matrix(matrix));
//        return norm;
//    }
//
//    public double getLogLikelihood() {
//        if (BayesianStochasticSearchVariableSelection.Utils.connectedAndWellConditioned(probability, this))
//            return 0;
//        return Double.NEGATIVE_INFINITY;
//    }

    /**
     * Needs to be evaluated before the corresponding data likelihood.
     *
     * @return
     */
//    public boolean evaluateEarly() {
//        return true;
//    }
//
//    public void setNormalization(boolean doNormalization) {
//        this.doNormalization = doNormalization;
//    }
//
//    public void makeDirty() {
//
//    }
//
//    private double[] probability;
//
//
//    void setDoNormalization(boolean normalize) {
//        this.doNormalization = normalize;
//    }
//
//    private boolean doNormalization = true;

	@Override
	public boolean canHandleDataType(DataType dataType) {
		return true;
	}

}