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
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.Nucleotide;
import beast.evolution.tree.Node;


/**
 * Tamura and Nei model of nucleotide evolution.
 * <p/>
 * <p/>
 * <p/>
 * pr = p[0]+p[1]
 * py = 1 - pr
 * <p/>
 * eigen values
 * <p/>
 * [0, -1, -(k[0]*pr + py), -(k[1]*py + pr)]
 * <p/>
 * unnormalized eigen vectors
 * [1,1,1,1],
 * [1,1,-pr/py,-pr/py],
 * [1, -p[0]/p[1], 0, 0],
 * [0, 0, 1,-p[2]/p[3]]
 *
 * @author Joseph Heled
 * @author Alexei Drummond
 * @author Marc Suchard
 */

@Description("TN93 (Tamura and Nei, 1993) substitution model of nucleotide evolution.")
@Citation(value = "Tamura, K., & Nei, M. (1993). Estimation of the number of nucleotide substitutions in the control region " +
        "of mitochondrial DNA in humans and chimpanzees. Molecular Biology and Evolution, 10(3), 512-526.", DOI = "", year = 1994, firstAuthorSurname = "tamura")
public class TN93 extends SubstitutionModel.NucleotideBase {
    final public Input<RealParameter> kappa1Variable = new Input<>("kappa1", "rate of A<->G transitions", Validate.REQUIRED);
    final public Input<RealParameter> kappa2Variable = new Input<>("kappa2", "rate of C<->T transitions", Validate.REQUIRED);

    private boolean updateIntermediates = true;

    /**
     * Used for precalculations
     */
    private double p1a;
    private double p0a;
    private double p3b;
    private double p2b;
    private double a;
    private double b;
    private double p1aa;
    private double p0aa;
    private double p3bb;
    private double p2bb;
    private double p1aIsa;
    private double p0aIsa;
    private double p3bIsb;
    private double p2bIsb;
    private double k1g;
    private double k1a;
    private double k2t;
    private double k2c;
    private double subrateScale;

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


    @Override
    public void initAndValidate() {
        super.initAndValidate();

        kappa1Variable.get().setBounds(Math.max(0.0, kappa1Variable.get().getLower()), kappa1Variable.get().getUpper());
        kappa2Variable.get().setBounds(Math.max(0.0, kappa2Variable.get().getLower()), kappa2Variable.get().getUpper());

        nrOfStates = STATE_COUNT;

        updateIntermediates = true;
    }

    /**
     * @return kappa1
     */
    public final double getKappa1() {
        return kappa1Variable.get().getValue(0);
    }

    /**
     * @return kappa2
     */
    public final double getKappa2() {
        return kappa2Variable.get().getValue(0);
    }

    /**
     * get the complete transition probability matrix for the given distance.
     * <p/>
     * Based on work Joseph Heled did during his 691 project.
     *
     * @param matrix an array to store the matrix
     */
    @Override
	public void getTransitionProbabilities(Node node, double startTime, double endTime, double rate, double[] matrix) {

        double distance = (startTime - endTime) * rate;

        synchronized (this) {
            if (updateIntermediates) {
                calculateIntermediates();
            }
        }

        distance /= subrateScale;

        double[] q = {
                0, k1g, freqC, freqT,
                k1a, 0, freqC, freqT,
                freqA, freqG, 0, k2t,
                freqA, freqG, k2c, 0
        };

        q[0] = -(q[1] + q[2] + q[3]);
        q[5] = -(q[4] + q[6] + q[7]);
        q[10] = -(q[8] + q[9] + q[11]);
        q[15] = -(q[12] + q[13] + q[14]);

        double[] fa0 = {
                1 + q[0] - p1aa, q[1] + p1aa, q[2], q[3],
                q[4] + p0aa, 1 + q[5] - p0aa, q[6], q[7],
                q[8], q[9], 1 + q[10] - p3bb, q[11] + p3bb,
                q[12], q[13], q[14] + p2bb, 1 + q[15] - p2bb
        };


        double[] fa1 = {
                -q[0] + p1aIsa, -q[1] - p1aIsa, -q[2], -q[3],
                -q[4] - p0aIsa, -q[5] + p0aIsa, -q[6], -q[7],
                -q[8], -q[9], -q[10] + p3bIsb, -q[11] - p3bIsb,
                -q[12], -q[13], -q[14] - p2bIsb, -q[15] + p2bIsb};

        double et = Math.exp(-distance);

        for (int k = 0; k < 16; ++k) {
            fa1[k] = fa1[k] * et + fa0[k];
        }

        final double eta = Math.exp(distance * a);
        final double etb = Math.exp(distance * b);

        double za = eta / (a * (1 + a));
        double zb = etb / (b * (1 + b));
        double u0 = p1a * za;
        double u1 = p0a * za;
        double u2 = p3b * zb;
        double u3 = p2b * zb;

        fa1[0] += u0;
        fa1[1] -= u0;
        fa1[4] -= u1;
        fa1[5] += u1;

        fa1[10] += u2;
        fa1[11] -= u2;
        fa1[14] -= u3;
        fa1[15] += u3;

        // transpose 2 middle rows and columns
        matrix[0] = fa1[0];
        matrix[1] = fa1[2];
        matrix[2] = fa1[1];
        matrix[3] = fa1[3];
        matrix[4] = fa1[8];
        matrix[5] = fa1[10];
        matrix[6] = fa1[9];
        matrix[7] = fa1[11];
        matrix[8] = fa1[4];
        matrix[9] = fa1[6];
        matrix[10] = fa1[5];
        matrix[11] = fa1[7];
        matrix[12] = fa1[12];
        matrix[13] = fa1[14];
        matrix[14] = fa1[13];
        matrix[15] = fa1[15];

        //System.arraycopy(fa1, 0, matrix, 0, 16);
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

            final double kappa1 = getKappa1();
            final double kappa2 = getKappa2();
            final double beta = -1.0 / (2.0 * (piR * piY + kappa1 * pi[0] * pi[2] + kappa2 * pi[1] * pi[3]));
            final double A_R = 1.0 + piR * (kappa1 - 1);
            final double A_Y = 1.0 + piY * (kappa2 - 1);

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
    protected double beta;

    private void calculateIntermediates() {

        calculateFreqRY();

        double k1 = getKappa1();
        double k2 = getKappa2();

//        System.out.println(getModelName() + " Using " + k1 + " " + k2);
        // A hack until I get right this boundary case. gives results accurate to 1e-8 in the P matrix
        // so should be OK even like this.
        if (k1 == 1) {
            k1 += 1E-10;
        }
        if (k2 == 1) {
            k2 += 1e-10;
        }

        double l1 = k1 * k1 * freqR + k1 * (2 * freqY - 1) - freqY;
        double l2 = k2 * k2 * freqY + k2 * (2 * freqR - 1) - freqR;

        p1a = freqG * l1;
        p0a = freqA * l1;
        p3b = freqT * l2;
        p2b = freqC * l2;

        a = -(k1 * freqR + freqY);
        b = -(k2 * freqY + freqR);

        p1aa = p1a / a;
        p0aa = p0a / a;
        p3bb = p3b / b;
        p2bb = p2b / b;

        p1aIsa = p1a / (1 + a);
        p0aIsa = p0a / (1 + a);
        p3bIsb = p3b / (1 + b);
        p2bIsb = p2b / (1 + b);

        k1g = k1 * freqG;
        k1a = k1 * freqA;
        k2t = k2 * freqT;
        k2c = k2 * freqC;

        subrateScale = 2 * (k1 * freqA * freqG + k2 * freqC * freqT + freqR * freqY);
        updateIntermediates = false;
    }

    /**
     * CalculationNode implementations *
     */
    @Override
    protected boolean requiresRecalculation() {
        // we only get here if something is dirty
        updateEigen = true;
        updateIntermediates = true;
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
        updateEigen = true;
        updateIntermediates = true;
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