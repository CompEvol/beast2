/*
* File HKY.java
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

import beast.core.Citation;
import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.Nucleotide;
import beast.evolution.tree.Node;

@Description("HKY85 (Hasegawa, Kishino & Yano, 1985) substitution model of nucleotide evolution.")
@Citation(value =
        "Hasegawa M, Kishino H, Yano T (1985) Dating the human-ape splitting by a\n"+
                "  molecular clock of mitochondrial DNA. Journal of Molecular Evolution\n" +
                "  22:160-174.", DOI = "10.1007/BF02101694", year = 1985, firstAuthorSurname = "hasegawa")
public class HKY extends SubstitutionModel.NucleotideBase {
    final public Input<Function> kappaInput = new Input<>("kappa", "kappa parameter in HKY model", Validate.REQUIRED);

    /**
     * applies to nucleotides only *
     */
    public static final int STATE_COUNT = 4;

    /**
     * Eigenvalue decomposition of rate matrix + its stored version *
     */
    private EigenDecomposition eigenDecomposition = null;
    private EigenDecomposition storedEigenDecomposition = null;

    /**
     * flag to indicate eigen decomposition is up to date *
     */
    private boolean updateEigen = true;
    /**
     * flag to indicate matrix is up to date *
     */
    protected boolean updateMatrix = true;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        if (kappaInput.get() instanceof RealParameter) {
        	RealParameter kappa = (RealParameter) kappaInput.get(); 
            kappa.setBounds(Math.max(0.0, kappa.getLower()), kappa.getUpper());
        }

        nrOfStates = STATE_COUNT;
    }

    @Override
    public void getTransitionProbabilities(Node node, double startTime, double endTime, double rate, double[] matrix) {
        double distance = (startTime - endTime) * rate;

        synchronized(this) {
        	if (updateMatrix) {
        		setupMatrix();
        	}
        }

        final double xx = beta * distance;
        final double bbR = Math.exp(xx * A_R);
        final double bbY = Math.exp(xx * A_Y);

        final double aa = Math.exp(xx);
        final double oneminusa = 1 - aa;

        final double t1Aaa = (tab1A * aa);
        matrix[0] = freqA + t1Aaa + (tab2A * bbR);

        matrix[1] = freqC * oneminusa;
        final double t1Gaa = (tab1G * aa);
        matrix[2] = freqG + t1Gaa - (tab3G * bbR);
        matrix[3] = freqT * oneminusa;

        matrix[4] = freqA * oneminusa;
        final double t1Caa = (tab1C * aa);
        matrix[5] = freqC + t1Caa + (tab2C * bbY);
        matrix[6] = freqG * oneminusa;
        final double t1Taa = (tab1T * aa);
        matrix[7] = freqT + t1Taa - (tab3T * bbY);

        matrix[8] = freqA + t1Aaa - (tab3A * bbR);
        matrix[9] = matrix[1];
        matrix[10] = freqG + t1Gaa + (tab2G * bbR);
        matrix[11] = matrix[3];

        matrix[12] = matrix[4];
        matrix[13] = freqC + t1Caa - (tab3C * bbY);
        matrix[14] = matrix[6];
        matrix[15] = freqT + t1Taa + (tab2T * bbY);
    }

    @Override
    public EigenDecomposition getEigenDecomposition(Node node) {

        if (eigenDecomposition == null) {
            double[] evec = new double[STATE_COUNT * STATE_COUNT];
            double[] ivec = new double[STATE_COUNT * STATE_COUNT];
            double[] eval = new double[STATE_COUNT];
            eigenDecomposition = new EigenDecomposition(evec, ivec, eval);

            ivec[2 * STATE_COUNT + 1] = 1; // left eigenvectors 3 = (0,1,0,-1); 4 = (1,0,-1,0)
            ivec[2 * STATE_COUNT + 3] = -1;

            ivec[3 * STATE_COUNT + 0] = 1;
            ivec[3 * STATE_COUNT + 2] = -1;

            evec[0 * STATE_COUNT + 0] = 1; // right eigenvector 1 = (1,1,1,1)'
            evec[1 * STATE_COUNT + 0] = 1;
            evec[2 * STATE_COUNT + 0] = 1;
            evec[3 * STATE_COUNT + 0] = 1;

            updateEigen = true;
        }

        if (updateEigen) {

            double[] evec = eigenDecomposition.getEigenVectors();
            double[] ivec = eigenDecomposition.getInverseEigenVectors();
            double[] pi = frequencies.getFreqs();
            double piR = pi[0] + pi[2];
            double piY = pi[1] + pi[3];

            // left eigenvector #1
            ivec[0 * STATE_COUNT + 0] = pi[0]; // or, evec[0] = pi;
            ivec[0 * STATE_COUNT + 1] = pi[1];
            ivec[0 * STATE_COUNT + 2] = pi[2];
            ivec[0 * STATE_COUNT + 3] = pi[3];

            // left eigenvector #2
            ivec[1 * STATE_COUNT + 0] = pi[0] * piY;
            ivec[1 * STATE_COUNT + 1] = -pi[1] * piR;
            ivec[1 * STATE_COUNT + 2] = pi[2] * piY;
            ivec[1 * STATE_COUNT + 3] = -pi[3] * piR;

            // right eigenvector #2
            evec[0 * STATE_COUNT + 1] = 1.0 / piR;
            evec[1 * STATE_COUNT + 1] = -1.0 / piY;
            evec[2 * STATE_COUNT + 1] = 1.0 / piR;
            evec[3 * STATE_COUNT + 1] = -1.0 / piY;

            // right eigenvector #3
            evec[1 * STATE_COUNT + 2] = pi[3] / piY;
            evec[3 * STATE_COUNT + 2] = -pi[1] / piY;

            // right eigenvector #4
            evec[0 * STATE_COUNT + 3] = pi[2] / piR;
            evec[2 * STATE_COUNT + 3] = -pi[0] / piR;

            // eigenvectors
            double[] eval = eigenDecomposition.getEigenValues();
            final double k = kappaInput.get().getArrayValue();

            final double beta = -1.0 / (2.0 * (piR * piY + k * (pi[0] * pi[2] + pi[1] * pi[3])));
            final double A_R = 1.0 + piR * (k - 1);
            final double A_Y = 1.0 + piY * (k - 1);

            eval[1] = beta;
            eval[2] = beta * A_Y;
            eval[3] = beta * A_R;

            updateEigen = false;

        }

        return eigenDecomposition;
    }

    /**
     * Used for precalculations
     */
    protected double beta, A_R, A_Y;
    protected double tab1A, tab2A, tab3A;
    protected double tab1C, tab2C, tab3C;
    protected double tab1G, tab2G, tab3G;
    protected double tab1T, tab2T, tab3T;

    protected void setupMatrix() {

        calculateFreqRY();

        // small speed up - reduce calculations. Comments show original code

        // (C+T) / (A+G)
        final double r1 = (1 / freqR) - 1;
        tab1A = freqA * r1;

        tab3A = freqA / freqR;
        tab2A = 1 - tab3A;        // (freqR-freqA)/freqR;

        final double r2 = 1 / r1; // ((1 / freqY) - 1);
        tab1C = freqC * r2;

        tab3C = freqC / freqY;
        tab2C = 1 - tab3C;       // (freqY-freqC)/freqY; assert  tab2C + tab3C == 1.0;

        tab1G = freqG * r1;
        tab3G = tab2A;            // 1 - tab3A; // freqG/freqR;
        tab2G = tab3A;            // 1 - tab3G; // (freqR-freqG)/freqR;

        tab1T = freqT * r2;

        tab3T = tab2C;            // 1 - tab3C;  // freqT/freqY;
        tab2T = tab3C;            // 1 - tab3T; // (freqY-freqT)/freqY; //assert tab2T + tab3T == 1.0 ;

        final double k = kappaInput.get().getArrayValue();
        beta = -1.0 / (2.0 * (freqR * freqY + k * (freqA * freqG + freqC * freqT)));

        A_R = 1.0 + freqR * (k - 1);
        A_Y = 1.0 + freqY * (k - 1);

        updateMatrix = false;
    }


    /**
     * CalculationNode implementations *
     */
    @Override
    protected boolean requiresRecalculation() {
        // we only get here if something is dirty
        updateMatrix = true;
        updateEigen = true;
        return true;
    }

    @Override
    protected void store() {
        if (eigenDecomposition != null) {
            storedEigenDecomposition = eigenDecomposition.copy();
        }
        super.store();
    }

    @Override
    protected void restore() {
        updateMatrix = true;
        updateEigen = true;
        if (storedEigenDecomposition != null) {
            eigenDecomposition = storedEigenDecomposition;
        }
        super.restore();
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof Nucleotide;
    }

}