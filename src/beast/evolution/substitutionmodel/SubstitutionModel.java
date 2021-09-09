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


import beast.base.Description;
import beast.base.Input;
import beast.base.Input.Validate;
import beast.evolution.datatype.DataType;
import beast.evolution.tree.Node;
import beast.inference.CalculationNode;


@Description("Specifies substitution model from which a transition probability matrix for a given " +
        "distance can be obtained.")
public interface SubstitutionModel {

    /**
     * get the complete transition probability matrix for the given distance
     * determined as (startTime-endTime)*rate
     *
     * @param node       tree node for which to calculate the probabilities
     * @param startTime
     * @param endTime   we assume start time is larger than end time
     * @param rate      rate, includes gamma rates and branch rates
     * @param matrix     an array to store the matrix which represents the transition probability
     *                   matrix in the form of an array. So, matrix must be of size n*n where n is number of states.
     */
    void getTransitionProbabilities(Node node, double startTime, double endTime, double rate, double[] matrix);

    /**
     * @param node In most cases, the rate matrix is independent of the tree, but if it changes
     *             throughout a tree, the node can provide this information.
     * @return instantaneous rate matrix Q, where Q is flattened into an array
     *         This is a square matrix, where rows add to zero, or null when no rate
     *         matrix is available.
     */
    double[] getRateMatrix(Node node);

    /**
     * return frequencies for root distribution *
     */
    double[] getFrequencies();

    public int getStateCount();


    /**
     * This function returns the Eigen decomposition of the instantaneous rate matrix if available.
     * Such Eigen decomposition may not be available because the substitution model changes over time,
     * for example, when one HKY model applies for some time t less than threshold time T while a GTR
     * model applies when t >= T.
     *
     * @param node In most cases, the rate matrix, and thus the Eigen decomposition, is independent of the tree,
     *             but if it changes throughout a tree, the node can provide this information.
     * @return the EigenDecomposition, null if not available
     */
    EigenDecomposition getEigenDecomposition(Node node);

    /**
     * @return whether substitution model can return complex diagonalizations
     *         If so, for example, a treelikelihood needs to be able to deal with this.
     */
    boolean canReturnComplexDiagonalization();

    /**
     * return true if this substitution model is suitable for the data type
     */
    boolean canHandleDataType(DataType dataType);

    /**
     * basic implementation of a SubstitutionModel bringing together relevant super class*
     */
    @Description(value = "Base implementation of a substitution model.", isInheritable = false)
    public abstract class Base extends CalculationNode implements SubstitutionModel {
        final public Input<Frequencies> frequenciesInput =
                new Input<>("frequencies", "substitution model equilibrium state frequencies", Validate.REQUIRED);

        /**
         * shadows frequencies, or can be set by subst model *
         */
        protected Frequencies frequencies;

        /**
         * number of states *
         */
        protected int nrOfStates;

        @Override
        public void initAndValidate() {
            frequencies = frequenciesInput.get();
        }

        @Override
        public double[] getFrequencies() {
            return frequencies.getFreqs();
        }

        @Override
        public int getStateCount() {
            return nrOfStates;
        }


        @Override
        public boolean canReturnComplexDiagonalization() {
            return false;
        }

        @Override
        public double[] getRateMatrix(Node node) {
            return null;
        }

    } // class Base

    /**
     * basic implementation of a SubstitutionModel bringing together relevant super class*
     */
    @Description(value = "Base implementation of a nucleotide substitution model.", isInheritable = false)
    public abstract class NucleotideBase extends Base {

        public double freqA, freqC, freqG, freqT,
        // A+G
        freqR,
        // C+T
        freqY;


        @Override
        public int getStateCount() {
            assert nrOfStates == 4;
            return nrOfStates;
        }

        protected void calculateFreqRY() {
            double[] freqs = frequencies.getFreqs();
            freqA = freqs[0];
            freqC = freqs[1];
            freqG = freqs[2];
            freqT = freqs[3];
            freqR = freqA + freqG;
            freqY = freqC + freqT;
        }


    } // class NucleotideBase


} // class SubstitutionModel
