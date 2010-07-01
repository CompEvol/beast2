/*
* File GeneralSubstitutionModel.java
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
package beast.evolution.nuc.substitutionmodel;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Parameter;
import beast.core.State;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Description("Specifies transition probability matrix with no restrictions on the rates other " +
        "than that one of the is equal to one and the others are specified relative to " +
        "this unit rate. Works for any number of states.")
public class GeneralSubstitutionModel extends SubstitutionModel {
    public Input<Parameter> m_pRateParameter = new Input<Parameter>("rates", "rate parameter which defines the transition rate matrix", Validate.REQUIRED);
    public Input<Integer> m_pRelativeTo = new Input<Integer>("relativeto", "index of rate which equals 1 while other are rates relative to this one", new Integer(0));

    @Override
    public void initAndValidate(State state) throws Exception {
        //setStateCount(m_pData.get().getMaxStateCount());
        setStateCount(m_pFreqs.get().getFreqs().length);

        if (m_pFreqs.get() != null) {
            checkFrequencies();
        }

        updateMatrix = true;
    } // initAndValidate

    /**
     * machine accuracy constant
     */
    public static double EPSILON = 2.220446049250313E-16;

    double[] m_freqs;

    protected double[] relativeRates;
    protected double[] storedRelativeRates;

    protected int stateCount;
    protected int rateCount;

    protected boolean eigenInitialised = false;
    protected boolean updateMatrix = true;
    protected boolean storedUpdateMatrix = true;

    @Override
    public boolean isDirty(State state) {
        if (m_pFreqs.get().isDirty(state)) {
            checkFrequencies();
            updateMatrix = true;
            return true;
        }
        if (state.isDirty(m_pRateParameter)) {
            updateMatrix = true;
            return true;
        }
        return false;
    } // isDirty

    public void store(int nSample) {

        storedUpdateMatrix = updateMatrix;

        System.arraycopy(relativeRates, 0, storedRelativeRates, 0, rateCount);

        System.arraycopy(Eval, 0, storedEval, 0, stateCount);
        for (int i = 0; i < stateCount; i++) {
            System.arraycopy(Ievc[i], 0, storedIevc[i], 0, stateCount);
            System.arraycopy(Evec[i], 0, storedEvec[i], 0, stateCount);
        }

    }

    /**
     * Restore the additional stored state
     */
    public void restore(int nSample) {

        updateMatrix = storedUpdateMatrix;

        // To restore all this stuff just swap the pointers...
        double[] tmp1 = storedRelativeRates;
        storedRelativeRates = relativeRates;
        relativeRates = tmp1;

        tmp1 = storedEval;
        storedEval = Eval;
        Eval = tmp1;

        double[][] tmp2 = storedIevc;
        storedIevc = Ievc;
        Ievc = tmp2;

        tmp2 = storedEvec;
        storedEvec = Evec;
        Evec = tmp2;

    }

    /**
     * sets up relative rate matrix *
     */
    public void setupRelativeRates(State state) {
        int nRelativeTo = m_pRelativeTo.get();
        Parameter pRates = state.getParameter(m_pRateParameter);
        for (int i = 0; i < relativeRates.length; i++) {
            if (i == nRelativeTo) {
                relativeRates[i] = 1.0;
            } else if (i < nRelativeTo) {
                relativeRates[i] = pRates.getValue(i);
            } else {
                relativeRates[i] = pRates.getValue(i - 1);
            }
        }
    } // setupRelativeRates


    private void setStateCount(int stateCount) {
        eigenInitialised = false;

        this.stateCount = stateCount;
        rateCount = ((stateCount - 1) * stateCount) / 2;

        relativeRates = new double[rateCount];
        storedRelativeRates = new double[rateCount];
        Arrays.fill(relativeRates, 1.0);
    }


    /**
     * get the complete transition probability matrix for the given distance
     *
     * @param distance the expected number of substitutions
     * @param matrix   an array to store the matrix
     */
    public void getTransitionProbabilities(double distance, double[] matrix, State state) {
        int i, j, k;
        double temp;

        // this must be synchronized to avoid being called simultaneously by
        // two different likelihood threads - AJD
        synchronized (this) {
            if (updateMatrix) {
                setupMatrix(state);
            }
        }

        // implemented a pool of iexp matrices to support multiple threads
        // without creating a new matrix each call. - AJD
        double[][] iexp = popiexp();
        for (i = 0; i < stateCount; i++) {
            temp = Math.exp(distance * Eval[i]);
            for (j = 0; j < stateCount; j++) {
                iexp[i][j] = Ievc[i][j] * temp;
            }
        }

        int u = 0;
        for (i = 0; i < stateCount; i++) {
            for (j = 0; j < stateCount; j++) {
                temp = 0.0;
                for (k = 0; k < stateCount; k++) {
                    temp += Evec[i][k] * iexp[k][j];
                }

                matrix[u] = Math.abs(temp);
                u++;
            }
        }
        pushiexp(iexp);
    }

    /**
     * This function returns the Eigen vectors.
     *
     * @return the array
     */
    public double[][] getEigenVectors(State state) {
        synchronized (this) {
            if (updateMatrix) {
                setupMatrix(state);
            }
        }
        return Evec;
    }

    /**
     * This function returns the inverse Eigen vectors.
     *
     * @return the array
     */
    public double[][] getInverseEigenVectors(State state) {
        synchronized (this) {
            if (updateMatrix) {
                setupMatrix(state);
            }
        }
        return Ievc;
    }

    /**
     * This function returns the Eigen values.
     */
    public double[] getEigenValues(State state) {
        synchronized (this) {
            if (updateMatrix) {
                setupMatrix(state);
            }
        }
        return Eval;
    }

    /**
     * setup substitution matrix
     */
    public void setupMatrix(State state) {
        setupRelativeRates(state);

        if (!eigenInitialised)
            initialiseEigen();

        int i, j, k = 0;

        // Set the instantaneous rate matrix
        for (i = 0; i < stateCount; i++) {
            for (j = i + 1; j < stateCount; j++) {
                amat[i][j] = relativeRates[k] * m_freqs[j];
                amat[j][i] = relativeRates[k] * m_freqs[i];
                k += 1;
            }
        }
        makeValid(amat, stateCount);
        normalize(amat, m_freqs);

        // copy q matrix for unit testing
        for (i = 0; i < amat.length; i++) {
            System.arraycopy(amat[i], 0, q[i], 0, amat[i].length);
        }

        // compute eigenvalues and eigenvectors
        elmhes(amat, ordr, stateCount);
        eltran(amat, Evec, ordr, stateCount);
        hqr2(stateCount, 1, stateCount, amat, Evec, Eval, evali);
        luinverse(Evec, Ievc, stateCount);

        updateMatrix = false;
    }

    // Make it a valid rate matrix (make sum of rows = 0)

    void makeValid(double[][] matrix, int dimension) {
        for (int i = 0; i < dimension; i++) {
            double sum = 0.0;
            for (int j = 0; j < dimension; j++) {
                if (i != j)
                    sum += matrix[i][j];
            }
            matrix[i][i] = -sum;
        }
    }

    /**
     * Normalize rate matrix to one expected substitution per unit time
     *
     * @param matrix the matrix to normalize to one expected substitution
     * @param pi     the equilibrium distribution of states
     */
    void normalize(double[][] matrix, double[] pi) {
        double subst = 0.0;
        int dimension = pi.length;

        for (int i = 0; i < dimension; i++)
            subst += -matrix[i][i] * pi[i];

        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                matrix[i][j] = matrix[i][j] / subst;
            }
        }
    }

    /**
     * Ensures that frequencies are not smaller than MINFREQ and
     * that two frequencies differ by at least 2*MINFDIFF.
     * This avoids potential problems later when eigenvalues
     * are computed.
     */
    private void checkFrequencies() {
        // required frequency difference
        double MINFDIFF = 1.0E-10;

        // lower limit on frequency
        double MINFREQ = 1.0E-10;

        int maxi = 0;
        double sum = 0.0;
        double maxfreq = 0.0;
        m_freqs = m_pFreqs.get().getFreqs();
        for (int i = 0; i < stateCount; i++) {
            double freq = m_freqs[i];
            if (freq < MINFREQ) m_freqs[i] = MINFREQ;
            if (freq > maxfreq) {
                maxfreq = freq;
                maxi = i;
            }
            sum += m_freqs[i];
        }
        double diff = 1.0 - sum;
        m_freqs[maxi] += diff;

        for (int i = 0; i < stateCount - 1; i++) {
            for (int j = i + 1; j < stateCount; j++) {
                if (m_freqs[i] == m_freqs[j]) {
                    m_freqs[i] += MINFDIFF;
                    m_freqs[j] += MINFDIFF;
                }
            }
        }
    }

    /**
     * allocate memory for the Eigen routines
     */
    protected void initialiseEigen() {

        Eval = new double[stateCount];
        Evec = new double[stateCount][stateCount];
        Ievc = new double[stateCount][stateCount];

        storedEval = new double[stateCount];
        storedEvec = new double[stateCount][stateCount];
        storedIevc = new double[stateCount][stateCount];

        amat = new double[stateCount][stateCount];
        q = new double[stateCount][stateCount];

        ordr = new int[stateCount];
        evali = new double[stateCount];

        eigenInitialised = true;
        updateMatrix = true;
    }

    // Eigenvalues, eigenvectors, and inverse eigenvectors
    protected double[] Eval;
    protected double[] storedEval;
    protected double[][] Evec;
    protected double[][] storedEvec;
    protected double[][] Ievc;
    protected double[][] storedIevc;

    List<double[][]> iexpPool = new LinkedList<double[][]>();

    private int[] ordr;
    private double[] evali;
    double amat[][];
    private double q[][];

    public double[][] getQ() {
        return q;
    }

    protected synchronized double[][] popiexp() {

        if (iexpPool.size() == 0) {
            iexpPool.add(new double[stateCount][stateCount]);
        }
        return iexpPool.remove(0);
    }

    protected synchronized void pushiexp(double[][] iexp) {
        iexpPool.add(0, iexp);
    }

    private void elmhes(double[][] a, int[] ordr, int n) {
        int m, j, i;
        double y, x;

        for (i = 0; i < n; i++) {
            ordr[i] = 0;
        }
        for (m = 2; m < n; m++) {
            x = 0.0;
            i = m;
            for (j = m; j <= n; j++) {
                if (Math.abs(a[j - 1][m - 2]) > Math.abs(x)) {
                    x = a[j - 1][m - 2];
                    i = j;
                }
            }
            ordr[m - 1] = i;
            if (i != m) {
                for (j = m - 2; j < n; j++) {
                    y = a[i - 1][j];
                    a[i - 1][j] = a[m - 1][j];
                    a[m - 1][j] = y;
                }
                for (j = 0; j < n; j++) {
                    y = a[j][i - 1];
                    a[j][i - 1] = a[j][m - 1];
                    a[j][m - 1] = y;
                }
            }
            if (x != 0.0) {
                for (i = m; i < n; i++) {
                    y = a[i][m - 2];
                    if (y != 0.0) {
                        y /= x;
                        a[i][m - 2] = y;
                        for (j = m - 1; j < n; j++) {
                            a[i][j] -= y * a[m - 1][j];
                        }
                        for (j = 0; j < n; j++) {
                            a[j][m - 1] += y * a[j][i];
                        }
                    }
                }
            }
        }
    }

    // Helper variables for mcdiv
    private double cr, ci;

    private void mcdiv(double ar, double ai, double br, double bi) {
        double s, ars, ais, brs, bis;

        s = Math.abs(br) + Math.abs(bi);
        ars = ar / s;
        ais = ai / s;
        brs = br / s;
        bis = bi / s;
        s = brs * brs + bis * bis;
        cr = (ars * brs + ais * bis) / s;
        ci = (ais * brs - ars * bis) / s;
    }

    void hqr2(int n, int low, int hgh, double[][] h, double[][] zz,
              double[] wr, double[] wi) throws ArithmeticException {
        int i, j, k, l = 0, m, en, na, itn, its;
        double p = 0, q = 0, r = 0, s = 0, t, w, x = 0, y, ra, sa, vi, vr, z = 0, norm, tst1, tst2;
        boolean notLast;


        norm = 0.0;
        k = 1;
        /* store isolated roots and compute matrix norm */
        for (i = 0; i < n; i++) {
            for (j = k - 1; j < n; j++) {
                norm += Math.abs(h[i][j]);
            }
            k = i + 1;
            if (i + 1 < low || i + 1 > hgh) {
                wr[i] = h[i][i];
                wi[i] = 0.0;
            }
        }
        en = hgh;
        t = 0.0;
        itn = n * 30;
        while (en >= low) {    /* search for next eigenvalues */
            its = 0;
            na = en - 1;
            while (en >= 1) {
                /* look for single small sub-diagonal element */
                boolean fullLoop = true;
                for (l = en; l > low; l--) {
                    s = Math.abs(h[l - 2][l - 2]) + Math.abs(h[l - 1][l - 1]);
                    if (s == 0.0) {
                        s = norm;
                    }
                    tst1 = s;
                    tst2 = tst1 + Math.abs(h[l - 1][l - 2]);
                    if (tst2 == tst1) {
                        fullLoop = false;
                        break;
                    }
                }
                if (fullLoop) {
                    l = low;
                }

                x = h[en - 1][en - 1];    /* form shift */
                if (l == en || l == na) {
                    break;
                }
                if (itn == 0) {
                    /* eigenvalues have not converged */
                    throw new ArithmeticException();
                }
                y = h[na - 1][na - 1];
                w = h[en - 1][na - 1] * h[na - 1][en - 1];
                /* form exceptional shift */
                if (its == 10 || its == 20) {
                    t += x;
                    for (i = low - 1; i < en; i++) {
                        h[i][i] -= x;
                    }
                    s = Math.abs(h[en - 1][na - 1]) + Math.abs(h[na - 1][en - 3]);
                    x = 0.75 * s;
                    y = x;
                    w = -0.4375 * s * s;
                }
                its++;
                itn--;
                /* look for two consecutive small sub-diagonal elements */
                for (m = en - 2; m >= l; m--) {
                    z = h[m - 1][m - 1];
                    r = x - z;
                    s = y - z;
                    p = (r * s - w) / h[m][m - 1] + h[m - 1][m];
                    q = h[m][m] - z - r - s;
                    r = h[m + 1][m];
                    s = Math.abs(p) + Math.abs(q) + Math.abs(r);
                    p /= s;
                    q /= s;
                    r /= s;
                    if (m == l) {
                        break;
                    }
                    tst1 = Math.abs(p) * (Math.abs(h[m - 2][m - 2]) + Math.abs(z) + Math.abs(h[m][m]));
                    tst2 = tst1 + Math.abs(h[m - 1][m - 2]) * (Math.abs(q) + Math.abs(r));
                    if (tst2 == tst1) {
                        break;
                    }
                }
                for (i = m + 2; i <= en; i++) {
                    h[i - 1][i - 3] = 0.0;
                    if (i != m + 2) {
                        h[i - 1][i - 4] = 0.0;
                    }
                }
                for (k = m; k <= na; k++) {
                    notLast = k != na;
                    if (k != m) {
                        p = h[k - 1][k - 2];
                        q = h[k][k - 2];
                        r = 0.0;
                        if (notLast) {
                            r = h[k + 1][k - 2];
                        }
                        x = Math.abs(p) + Math.abs(q) + Math.abs(r);
                        if (x != 0.0) {
                            p /= x;
                            q /= x;
                            r /= x;
                        }
                    }
                    if (x != 0.0) {
                        if (p < 0.0) {    /* sign */
                            s = -Math.sqrt(p * p + q * q + r * r);
                        } else {
                            s = Math.sqrt(p * p + q * q + r * r);
                        }
                        if (k != m) {
                            h[k - 1][k - 2] = -s * x;
                        } else if (l != m) {
                            h[k - 1][k - 2] = -h[k - 1][k - 2];
                        }
                        p += s;
                        x = p / s;
                        y = q / s;
                        z = r / s;
                        q /= p;
                        r /= p;
                        if (!notLast) {
                            for (j = k - 1; j < n; j++) {    /* row modification */
                                p = h[k - 1][j] + q * h[k][j];
                                h[k - 1][j] -= p * x;
                                h[k][j] -= p * y;
                            }
                            j = (en < (k + 3)) ? en : (k + 3); /* min */
                            for (i = 0; i < j; i++) {    /* column modification */
                                p = x * h[i][k - 1] + y * h[i][k];
                                h[i][k - 1] -= p;
                                h[i][k] -= p * q;
                            }
                            /* accumulate transformations */
                            for (i = low - 1; i < hgh; i++) {
                                p = x * zz[i][k - 1] + y * zz[i][k];
                                zz[i][k - 1] -= p;
                                zz[i][k] -= p * q;
                            }
                        } else {
                            for (j = k - 1; j < n; j++) {    /* row modification */
                                p = h[k - 1][j] + q * h[k][j] + r * h[k + 1][j];
                                h[k - 1][j] -= p * x;
                                h[k][j] -= p * y;
                                h[k + 1][j] -= p * z;
                            }
                            j = (en < (k + 3)) ? en : (k + 3); /* min */
                            for (i = 0; i < j; i++) {    /* column modification */
                                p = x * h[i][k - 1] + y * h[i][k] + z * h[i][k + 1];
                                h[i][k - 1] -= p;
                                h[i][k] -= p * q;
                                h[i][k + 1] -= p * r;
                            }
                            /* accumulate transformations */
                            for (i = low - 1; i < hgh; i++) {
                                p = x * zz[i][k - 1] + y * zz[i][k] +
                                        z * zz[i][k + 1];
                                zz[i][k - 1] -= p;
                                zz[i][k] -= p * q;
                                zz[i][k + 1] -= p * r;
                            }
                        }
                    }
                }                 /* for k */
            }                     /* while infinite loop */
            if (l == en) {                 /* one root found */
                h[en - 1][en - 1] = x + t;
                wr[en - 1] = h[en - 1][en - 1];
                wi[en - 1] = 0.0;
                en = na;
                continue;
            }
            y = h[na - 1][na - 1];
            w = h[en - 1][na - 1] * h[na - 1][en - 1];
            p = (y - x) / 2.0;
            q = p * p + w;
            z = Math.sqrt(Math.abs(q));
            h[en - 1][en - 1] = x + t;
            x = h[en - 1][en - 1];
            h[na - 1][na - 1] = y + t;
            if (q >= 0.0) {     /* real pair */
                if (p < 0.0) {    /* sign */
                    z = p - Math.abs(z);
                } else {
                    z = p + Math.abs(z);
                }
                wr[na - 1] = x + z;
                wr[en - 1] = wr[na - 1];
                if (z != 0.0) {
                    wr[en - 1] = x - w / z;
                }
                wi[na - 1] = 0.0;
                wi[en - 1] = 0.0;
                x = h[en - 1][na - 1];
                s = Math.abs(x) + Math.abs(z);
                p = x / s;
                q = z / s;
                r = Math.sqrt(p * p + q * q);
                p /= r;
                q /= r;
                for (j = na - 1; j < n; j++) {    /* row modification */
                    z = h[na - 1][j];
                    h[na - 1][j] = q * z + p * h[en - 1][j];
                    h[en - 1][j] = q * h[en - 1][j] - p * z;
                }
                for (i = 0; i < en; i++) {    /* column modification */
                    z = h[i][na - 1];
                    h[i][na - 1] = q * z + p * h[i][en - 1];
                    h[i][en - 1] = q * h[i][en - 1] - p * z;
                }
                /* accumulate transformations */
                for (i = low - 1; i < hgh; i++) {
                    z = zz[i][na - 1];
                    zz[i][na - 1] = q * z + p * zz[i][en - 1];
                    zz[i][en - 1] = q * zz[i][en - 1] - p * z;
                }
            } else {    /* complex pair */
                wr[na - 1] = x + p;
                wr[en - 1] = x + p;
                wi[na - 1] = z;
                wi[en - 1] = -z;
            }
            en -= 2;
        } /* while en >= low */
        /* backsubstitute to find vectors of upper triangular form */
        if (norm != 0.0) {
            for (en = n; en >= 1; en--) {
                p = wr[en - 1];
                q = wi[en - 1];
                na = en - 1;
                if (q == 0.0) {/* real vector */
                    m = en;
                    h[en - 1][en - 1] = 1.0;
                    if (na != 0) {
                        for (i = en - 2; i >= 0; i--) {
                            w = h[i][i] - p;
                            r = 0.0;
                            for (j = m - 1; j < en; j++) {
                                r += h[i][j] * h[j][en - 1];
                            }
                            if (wi[i] < 0.0) {
                                z = w;
                                s = r;
                            } else {
                                m = i + 1;
                                if (wi[i] == 0.0) {
                                    t = w;
                                    if (t == 0.0) {
                                        tst1 = norm;
                                        t = tst1;
                                        do {
                                            t = 0.01 * t;
                                            tst2 = norm + t;
                                        }
                                        while (tst2 > tst1);
                                    }
                                    h[i][en - 1] = -(r / t);
                                } else {    /* solve real equations */
                                    x = h[i][i + 1];
                                    y = h[i + 1][i];
                                    q = (wr[i] - p) * (wr[i] - p) + wi[i] * wi[i];
                                    t = (x * s - z * r) / q;
                                    h[i][en - 1] = t;
                                    if (Math.abs(x) > Math.abs(z))
                                        h[i + 1][en - 1] = (-r - w * t) / x;
                                    else
                                        h[i + 1][en - 1] = (-s - y * t) / z;
                                }
                                /* overflow control */
                                t = Math.abs(h[i][en - 1]);
                                if (t != 0.0) {
                                    tst1 = t;
                                    tst2 = tst1 + 1.0 / tst1;
                                    if (tst2 <= tst1) {
                                        for (j = i; j < en; j++) {
                                            h[j][en - 1] /= t;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (q > 0.0) {
                    m = na;
                    if (Math.abs(h[en - 1][na - 1]) > Math.abs(h[na - 1][en - 1])) {
                        h[na - 1][na - 1] = q / h[en - 1][na - 1];
                        h[na - 1][en - 1] = (p - h[en - 1][en - 1]) / h[en - 1][na - 1];
                    } else {
                        mcdiv(0.0, -h[na - 1][en - 1], h[na - 1][na - 1] - p, q);
                        h[na - 1][na - 1] = cr;
                        h[na - 1][en - 1] = ci;
                    }
                    h[en - 1][na - 1] = 0.0;
                    h[en - 1][en - 1] = 1.0;
                    if (en != 2) {
                        for (i = en - 3; i >= 0; i--) {
                            w = h[i][i] - p;
                            ra = 0.0;
                            sa = 0.0;
                            for (j = m - 1; j < en; j++) {
                                ra += h[i][j] * h[j][na - 1];
                                sa += h[i][j] * h[j][en - 1];
                            }
                            if (wi[i] < 0.0) {
                                z = w;
                                r = ra;
                                s = sa;
                            } else {
                                m = i + 1;
                                if (wi[i] == 0.0) {
                                    mcdiv(-ra, -sa, w, q);
                                    h[i][na - 1] = cr;
                                    h[i][en - 1] = ci;
                                } else {    /* solve complex equations */
                                    x = h[i][i + 1];
                                    y = h[i + 1][i];
                                    vr = (wr[i] - p) * (wr[i] - p);
                                    vr = vr + wi[i] * wi[i] - q * q;
                                    vi = (wr[i] - p) * 2.0 * q;
                                    if (vr == 0.0 && vi == 0.0) {
                                        tst1 = norm * (Math.abs(w) + Math.abs(q) + Math.abs(x) +
                                                Math.abs(y) + Math.abs(z));
                                        vr = tst1;
                                        do {
                                            vr = 0.01 * vr;
                                            tst2 = tst1 + vr;
                                        }
                                        while (tst2 > tst1);
                                    }
                                    mcdiv(x * r - z * ra + q * sa, x * s - z * sa - q * ra, vr, vi);
                                    h[i][na - 1] = cr;
                                    h[i][en - 1] = ci;
                                    if (Math.abs(x) > Math.abs(z) + Math.abs(q)) {
                                        h[i + 1]
                                                [na - 1] = (q * h[i][en - 1] -
                                                w * h[i][na - 1] - ra) / x;
                                        h[i + 1][en - 1] = (-sa - w * h[i][en - 1] -
                                                q * h[i][na - 1]) / x;
                                    } else {
                                        mcdiv(-r - y * h[i][na - 1], -s - y * h[i][en - 1], z, q);
                                        h[i + 1][na - 1] = cr;
                                        h[i + 1][en - 1] = ci;
                                    }
                                }
                                /* overflow control */
                                t = (Math.abs(h[i][na - 1]) > Math.abs(h[i][en - 1])) ?
                                        Math.abs(h[i][na - 1]) : Math.abs(h[i][en - 1]);
                                if (t != 0.0) {
                                    tst1 = t;
                                    tst2 = tst1 + 1.0 / tst1;
                                    if (tst2 <= tst1) {
                                        for (j = i; j < en; j++) {
                                            h[j][na - 1] /= t;
                                            h[j][en - 1] /= t;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            /* end back substitution. vectors of isolated roots */
            for (i = 0; i < n; i++) {
                if (i + 1 < low || i + 1 > hgh) {
                    for (j = i; j < n; j++) {
                        zz[i][j] = h[i][j];
                    }
                }
            }
            /* multiply by transformation matrix to give vectors of
                               * original full matrix. */
            for (j = n - 1; j >= low - 1; j--) {
                m = ((j + 1) < hgh) ? (j + 1) : hgh; /* min */
                for (i = low - 1; i < hgh; i++) {
                    z = 0.0;
                    for (k = low - 1; k < m; k++) {
                        z += zz[i][k] * h[k][j];
                    }
                    zz[i][j] = z;
                }
            }
        }
    }

    private void eltran(double[][] a, double[][] zz, int[] ordr, int n) {
        int i, j, m;

        for (i = 0; i < n; i++) {
            for (j = i + 1; j < n; j++) {
                zz[i][j] = 0.0;
                zz[j][i] = 0.0;
            }
            zz[i][i] = 1.0;
        }
        if (n <= 2) {
            return;
        }
        for (m = n - 1; m >= 2; m--) {
            for (i = m; i < n; i++) {
                zz[i][m - 1] = a[i][m - 2];
            }
            i = ordr[m - 1];
            if (i != m) {
                for (j = m - 1; j < n; j++) {
                    zz[m - 1][j] = zz[i - 1][j];
                    zz[i - 1][j] = 0.0;
                }
                zz[i - 1][m - 1] = 1.0;
            }
        }
    }

    void luinverse(double[][] inmat, double[][] imtrx, int size) throws IllegalArgumentException {
        int i, j, k, l, maxi = 0, idx, ix, jx;
        double sum, tmp, maxb, aw;
        int[] index;
        double[] wk;
        double[][] omtrx;


        index = new int[size];
        omtrx = new double[size][size];

        /* copy inmat to omtrx */
        for (i = 0; i < size; i++) {
            for (j = 0; j < size; j++) {
                omtrx[i][j] = inmat[i][j];
            }
        }

        wk = new double[size];
        aw = 1.0;
        for (i = 0; i < size; i++) {
            maxb = 0.0;
            for (j = 0; j < size; j++) {
                if (Math.abs(omtrx[i][j]) > maxb) {
                    maxb = Math.abs(omtrx[i][j]);
                }
            }
            if (maxb == 0.0) {
                /* Singular matrix */
                System.out.println("Singular matrix encountered");
                throw new IllegalArgumentException("Singular matrix");
            }
            wk[i] = 1.0 / maxb;
        }
        for (j = 0; j < size; j++) {
            for (i = 0; i < j; i++) {
                sum = omtrx[i][j];
                for (k = 0; k < i; k++) {
                    sum -= omtrx[i][k] * omtrx[k][j];
                }
                omtrx[i][j] = sum;
            }
            maxb = 0.0;
            for (i = j; i < size; i++) {
                sum = omtrx[i][j];
                for (k = 0; k < j; k++) {
                    sum -= omtrx[i][k] * omtrx[k][j];
                }
                omtrx[i][j] = sum;
                tmp = wk[i] * Math.abs(sum);
                if (tmp >= maxb) {
                    maxb = tmp;
                    maxi = i;
                }
            }
            if (j != maxi) {
                for (k = 0; k < size; k++) {
                    tmp = omtrx[maxi][k];
                    omtrx[maxi][k] = omtrx[j][k];
                    omtrx[j][k] = tmp;
                }
                aw = -aw;
                wk[maxi] = wk[j];
            }
            index[j] = maxi;
            if (omtrx[j][j] == 0.0) {
                omtrx[j][j] = EPSILON;
            }
            if (j != size - 1) {
                tmp = 1.0 / omtrx[j][j];
                for (i = j + 1; i < size; i++) {
                    omtrx[i][j] *= tmp;
                }
            }
        }
        for (jx = 0; jx < size; jx++) {
            for (ix = 0; ix < size; ix++) {
                wk[ix] = 0.0;
            }
            wk[jx] = 1.0;
            l = -1;
            for (i = 0; i < size; i++) {
                idx = index[i];
                sum = wk[idx];
                wk[idx] = wk[i];
                if (l != -1) {
                    for (j = l; j < i; j++) {
                        sum -= omtrx[i][j] * wk[j];
                    }
                } else if (sum != 0.0) {
                    l = i;
                }
                wk[i] = sum;
            }
            for (i = size - 1; i >= 0; i--) {
                sum = wk[i];
                for (j = i + 1; j < size; j++) {
                    sum -= omtrx[i][j] * wk[j];
                }
                wk[i] = sum / omtrx[i][i];
            }
            for (ix = 0; ix < size; ix++) {
                imtrx[ix][jx] = wk[ix];
            }
        }
        wk = null;
        index = null;
        omtrx = null;
    }

}
