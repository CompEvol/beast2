/*
* File SiteModel.java
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
package beast.evolution.sitemodel;


import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.StateNode;
import beast.core.parameter.RealParameter;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.substitutionmodel.HKY;
import beast.evolution.substitutionmodel.SubstitutionModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Site model with
 * o gamma site model,
 * o proportion of sites that are invariant
 * *
 */

@Description("Defines mutation rate " +
        "and gamma distributed rates across sites (optional) " +
        "and proportion of the sites invariant (also optional).")
public class SiteModel extends CalculationNode {
    public Input<RealParameter> muParameter = new Input<RealParameter>("mutationRate", "mutation rate (defaults to 1.0)");
    public Input<Integer> gammaCategoryCount =
            new Input<Integer>("gammaCategoryCount", "gamma category count (default=zero for no gamma)", 0);
    public Input<RealParameter> shapeParameter =
            new Input<RealParameter>("shape", "shape parameter of gamma distribution. Ignored if gammaCategoryCount 1 or less");
    public Input<RealParameter> invarParameter =
            new Input<RealParameter>("proportionInvariant", "proportion of sites that is invariant: should be between 0 (default) and 1");
    public Input<SubstitutionModel> m_pSubstModel =
            new Input<SubstitutionModel>("substModel", "substitution model along branches in the beast.tree", new HKY(), Validate.REQUIRED);
    public Input<Frequencies> m_pFreqs =
            new Input<Frequencies>("frequencies", "frequencies of characters used as prior on root", Validate.REQUIRED);


    @Override
    public void initAndValidate() throws Exception {

        if (muParameter.get() != null) {
            muParameter.get().setBounds(0.0, Double.POSITIVE_INFINITY);
        }

        if (shapeParameter.get() != null) {
            categoryCount = gammaCategoryCount.get();

            // The quantile calculator fails when the shape parameter goes much below
            // 1E-3 so we have put a hard lower bound on it. If this is not there then
            // the category rates can go to 0 and cause a -Inf likelihood (whilst this
            // is not a problem as the state will be rejected, it could mask other issues
            // and this seems the better approach.
            shapeParameter.get().setBounds(1.0E-3, 1.0E3);
        } else {
            categoryCount = 1;
        }

        if (invarParameter.get() != null && invarParameter.get().getValue() > 0) {
            categoryCount += 1;
            invarParameter.get().setBounds(0.0, 1.0);
        } else if (invarParameter.get() != null && invarParameter.get().getValue() <= 0) {
            invarParameter.setValue(null, this);
        }

        categoryRates = new double[categoryCount];
        categoryProportions = new double[categoryCount];

        ratesKnown = false;

        addCondition(muParameter);
        addCondition(invarParameter);
        addCondition(shapeParameter);
    }

    public List<String> getConditions() {
        return conditions;
    }

    // RRB: do we need this bit of undocumented code?

    public void addCondition(Input<? extends StateNode> stateNode) {
//        if (this instanceof StateNode) throw new RuntimeException();
        if (stateNode.get() == null) return;

        if (conditions == null) conditions = new ArrayList<String>();

        conditions.add(stateNode.get().getID());
    }

    protected List<String> conditions = null;


    @Override
    protected boolean requiresRecalculation() {
        // we only get here if something is dirty in its inputs
        ratesKnown = false;
        return true;
//        if (muParameter.isDirty()) {
//            ratesKnown = false;
//        }
//        if (muParameter.isDirty()) {
//            ratesKnown = false;
//        }
//        if (muParameter.isDirty()) {
//            ratesKnown = false;
//        }
//        return m_pSubstModel.isDirty() || !ratesKnown;
    }


    // *****************************************************************
    // Interface SiteModel
    // *****************************************************************

    public boolean integrateAcrossCategories() {
        return true;
    }

    public int getCategoryCount() {
        return categoryCount;
    }

    public int getCategoryOfSite(int site) {
        throw new IllegalArgumentException("Integrating across categories");
    }

    public double getRateForCategory(int category) {
        //synchronized (this) 
        {
            if (!ratesKnown) {
                calculateCategoryRates();
            }
        }

        RealParameter tmp = muParameter.get();
        final double mu = (tmp != null) ? tmp.getValue() : 1.0;

        return categoryRates[category] * mu;
    }

    public double[] getCategoryRates() {
        //synchronized (this) 
        {
            if (!ratesKnown) {
                calculateCategoryRates();
            }
        }

        RealParameter tmp = muParameter.get();
        final double mu = (tmp != null) ? tmp.getValue() : 1.0;

        final double[] rates = new double[categoryRates.length];
        for (int i = 0; i < rates.length; i++) {
            rates[i] = categoryRates[i] * mu;
        }

        return rates;
    }

    public SubstitutionModel getSubstitutionModel() {
        return m_pSubstModel.get();
    }

    public double[] getFrequencies() {
        return m_pFreqs.get().getFreqs();
    }

    /**
     * get the complete transition probability matrix for the given distance
     *
     * @param substitutions the expected number of substitutions
     * @param matrix        an array to store the matrix
     */
    public void getTransitionProbabilities(double substitutions, double[] matrix) {
        m_pSubstModel.get().getTransitionProbabilities(substitutions, matrix);
    }

    /**
     * Get the expected proportion of sites in this category.
     *
     * @param category the category number
     * @return the proportion.
     */
    public double getProportionForCategory(int category) {
        //synchronized (this) 
        {
            if (!ratesKnown) {
                calculateCategoryRates();
            }
        }

        return categoryProportions[category];
    }

    /**
     * Get an array of the expected proportion of sites in this category.
     *
     * @return an array of the proportion.
     */
    public double[] getCategoryProportions() {
        //synchronized (this) 
        {
            if (!ratesKnown) {
                calculateCategoryRates();
            }
        }

        return categoryProportions;
    }

    /**
     * discretization of gamma distribution with equal proportions in each
     * category
     */
    private void calculateCategoryRates() {
        double propVariable = 1.0;
        int cat = 0;

        if (invarParameter.get() != null) {
            categoryRates[0] = 0.0;
            categoryProportions[0] = invarParameter.get().getValue();

            propVariable = 1.0 - categoryProportions[0];
            cat = 1;
        }

        if (shapeParameter.get() != null) {

            final double a = shapeParameter.get().getValue();
            double mean = 0.0;
            final int gammaCatCount = categoryCount - cat;

            for (int i = 0; i < gammaCatCount; i++) {

                try {
                    categoryRates[i + cat] = GammaDistributionQuantile((2.0 * i + 1.0) / (2.0 * gammaCatCount), a, 1.0 / a);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Something went wrong with the gamma distribution calculation");
                    System.exit(-1);
                }
                mean += categoryRates[i + cat];

                categoryProportions[i + cat] = propVariable / gammaCatCount;
            }

            mean = (propVariable * mean) / gammaCatCount;

            for (int i = 0; i < gammaCatCount; i++) {

                categoryRates[i + cat] /= mean;
            }
        } else {
            categoryRates[cat] = 1.0 / propVariable;
            categoryProportions[cat] = propVariable;
        }


        ratesKnown = true;
    }


    @Override
    public void store() {
        //m_pSubstModel.get().store(nSample);
    } // no additional state needs storing

    @Override
    public void restore() {
        //m_pSubstModel.get().restore(nSample);
        ratesKnown = false;
    }


    private boolean ratesKnown;

    private int categoryCount;

    private double[] categoryRates;

    private double[] categoryProportions;


    /**
     * quantile (inverse cumulative density function) of the Gamma distribution
     *
     * @param y     argument
     * @param shape shape parameter
     * @param scale scale parameter
     * @return icdf value
     * @throws Exception if arguments out of range
     */
    double GammaDistributionQuantile(double y, double shape, double scale) throws Exception {
        return 0.5 * scale * pointChi2(y, 2.0 * shape);
    }

    double pointChi2(double prob, double v) throws Exception {
        // Returns z so that Prob{x<z}=prob where x is Chi2 distributed with df
        // = v
        // RATNEST FORTRAN by
        // Best DJ & Roberts DE (1975) The percentage points of the
        // Chi2 distribution. Applied Statistics 24: 385-388. (AS91)

        double e = 0.5e-6, aa = 0.6931471805, g;
        double xx, c, ch, a, q, p1, p2, t, x, b, s1, s2, s3, s4, s5, s6;

        if (prob < 0.000002 || prob > 0.999998 || v <= 0) {
            throw new Exception("Error SiteModel 102: Arguments out of range");
        }
        g = GammaFunctionlnGamma(v / 2);
        xx = v / 2;
        c = xx - 1;
        if (v < -1.24 * Math.log(prob)) {
            ch = Math.pow((prob * xx * Math.exp(g + xx * aa)), 1 / xx);
            if (ch - e < 0) {
                return ch;
            }
        } else {
            if (v > 0.32) {
                x = NormalDistributionQuantile(prob, 0, 1);
                p1 = 0.222222 / v;
                ch = v * Math.pow((x * Math.sqrt(p1) + 1 - p1), 3.0);
                if (ch > 2.2 * v + 6) {
                    ch = -2 * (Math.log(1 - prob) - c * Math.log(.5 * ch) + g);
                }
            } else {
                ch = 0.4;
                a = Math.log(1 - prob);

                do {
                    q = ch;
                    p1 = 1 + ch * (4.67 + ch);
                    p2 = ch * (6.73 + ch * (6.66 + ch));
                    t = -0.5 + (4.67 + 2 * ch) / p1
                            - (6.73 + ch * (13.32 + 3 * ch)) / p2;
                    ch -= (1 - Math.exp(a + g + .5 * ch + c * aa) * p2 / p1)
                            / t;
                } while (Math.abs(q / ch - 1) - .01 > 0);
            }
        }
        do {
            q = ch;
            p1 = 0.5 * ch;
            if ((t = GammaFunctionincompleteGammaP(xx, p1, g)) < 0) {
                throw new Exception("Error SiteModel 101: Arguments out of range: t < 0");
            }
            p2 = prob - t;
            t = p2 * Math.exp(xx * aa + g + p1 - c * Math.log(ch));
            b = t / ch;
            a = 0.5 * t - b * c;

            s1 = (210 + a * (140 + a * (105 + a * (84 + a * (70 + 60 * a))))) / 420;
            s2 = (420 + a * (735 + a * (966 + a * (1141 + 1278 * a)))) / 2520;
            s3 = (210 + a * (462 + a * (707 + 932 * a))) / 2520;
            s4 = (252 + a * (672 + 1182 * a) + c * (294 + a * (889 + 1740 * a))) / 5040;
            s5 = (84 + 264 * a + c * (175 + 606 * a)) / 2520;
            s6 = (120 + c * (346 + 127 * c)) / 5040;
            ch += t
                    * (1 + 0.5 * t * s1 - b
                    * c
                    * (s1 - b
                    * (s2 - b
                    * (s3 - b
                    * (s4 - b * (s5 - b * s6))))));
        } while (Math.abs(q / ch - 1) > e);

        return (ch);
    }

    /**
     * log Gamma function: ln(gamma(alpha)) for alpha>0, accurate to 10 decimal places
     *
     * @param alpha argument
     * @return the log of the gamma function of the given alpha
     */
    double GammaFunctionlnGamma(double alpha) {
        // Pike MC & Hill ID (1966) Algorithm 291: Logarithm of the gamma function.
        // Communications of the Association for Computing Machinery, 9:684

        double x = alpha, f = 0.0, z;

        if (x < 7) {
            f = 1;
            z = x - 1;
            while (++z < 7) {
                f *= z;
            }
            x = z;
            f = -Math.log(f);
        }
        z = 1 / (x * x);

        return
                f + (x - 0.5) * Math.log(x) - x + 0.918938533204673 +
                        (((-0.000595238095238 * z + 0.000793650793651) *
                                z - 0.002777777777778) * z + 0.083333333333333) / x;
    }

    /**
     * Incomplete Gamma function P(a,x) = 1-Q(a,x)
     * (a cleanroom implementation of Numerical Recipes gammp(a,x);
     * in Mathematica this function is 1-GammaRegularized)
     *
     * @param a        parameter
     * @param x        argument
     * @param lnGammaA precomputed lnGamma(a)
     * @return function value
     * @throws Exception if illegal arguments given
     */
    double GammaFunctionincompleteGammaP(double a, double x, double lnGammaA) throws Exception {
        return incompleteGamma(x, a, lnGammaA);
    }


    /**
     * Returns the incomplete gamma ratio I(x,alpha) where x is the upper
     * limit of the integration and alpha is the shape parameter.
     *
     * @param x              upper limit of integration
     * @param alpha          shape parameter
     * @param ln_gamma_alpha the log gamma function for alpha
     * @return the incomplete gamma ratio
     * @throws Exception if illegal arguments given
     */
    double incompleteGamma(double x, double alpha, double ln_gamma_alpha) throws Exception {
        // (1) series expansion     if (alpha>x || x<=1)
        // (2) continued fraction   otherwise
        // RATNEST FORTRAN by
        // Bhattacharjee GP (1970) The incomplete gamma integral.  Applied Statistics,
        // 19: 285-287 (AS32)

        double accurate = 1e-8, overflow = 1e30;
        double factor, gin, rn, a, b, an, dif, term;
        double pn0, pn1, pn2, pn3, pn4, pn5;

        if (x == 0.0) {
            return 0.0;
        }
        if (x < 0.0 || alpha <= 0.0) {
            throw new Exception("Error SiteModel 103: Arguments out of bounds");
        }

        factor = Math.exp(alpha * Math.log(x) - x - ln_gamma_alpha);

        if (x > 1 && x >= alpha) {
            // continued fraction
            a = 1 - alpha;
            b = a + x + 1;
            term = 0;
            pn0 = 1;
            pn1 = x;
            pn2 = x + 1;
            pn3 = x * b;
            gin = pn2 / pn3;

            do {
                a++;
                b += 2;
                term++;
                an = a * term;
                pn4 = b * pn2 - an * pn0;
                pn5 = b * pn3 - an * pn1;

                if (pn5 != 0) {
                    rn = pn4 / pn5;
                    dif = Math.abs(gin - rn);
                    if (dif <= accurate) {
                        if (dif <= accurate * rn) {
                            break;
                        }
                    }

                    gin = rn;
                }
                pn0 = pn2;
                pn1 = pn3;
                pn2 = pn4;
                pn3 = pn5;
                if (Math.abs(pn4) >= overflow) {
                    pn0 /= overflow;
                    pn1 /= overflow;
                    pn2 /= overflow;
                    pn3 /= overflow;
                }
            } while (true);
            gin = 1 - factor * gin;
        } else {
            // series expansion
            gin = 1;
            term = 1;
            rn = alpha;
            do {
                rn++;
                term *= x / rn;
                gin += term;
            }
            while (term > accurate);
            gin *= factor / alpha;
        }
        return gin;
    }

    double NormalDistributionQuantile(double z, double m, double sd) throws Exception {
        return m + Math.sqrt(2.0) * sd * ErrorFunctionInverseErf(2.0 * z - 1.0);
    }

    /**
     * inverse error function
     *
     * @param z argument
     * @return function value
     * @throws Exception argument out of range
     */
    double ErrorFunctionInverseErf(double z) throws Exception {
        return ErrorFunctionPointNormal(0.5 * z + 0.5) / Math.sqrt(2.0);
    }


    // Private

    // Returns z so that Prob{x<z}=prob where x ~ N(0,1) and (1e-12) < prob<1-(1e-12)

    double ErrorFunctionPointNormal(double prob) throws Exception {
        // Odeh RE & Evans JO (1974) The percentage points of the normal distribution.
        // Applied Statistics 22: 96-97 (AS70)

        // Newer methods:
        // Wichura MJ (1988) Algorithm AS 241: the percentage points of the
        // normal distribution.  37: 477-484.
        // Beasley JD & Springer SG  (1977).  Algorithm AS 111: the percentage
        // points of the normal distribution.  26: 118-121.

        double a0 = -0.322232431088, a1 = -1, a2 = -0.342242088547, a3 = -0.0204231210245;
        double a4 = -0.453642210148e-4, b0 = 0.0993484626060, b1 = 0.588581570495;
        double b2 = 0.531103462366, b3 = 0.103537752850, b4 = 0.0038560700634;
        double y, z, p1;

        p1 = (prob < 0.5 ? prob : 1 - prob);
        if (p1 < 1e-20) {
            throw new Exception("Error SiteModel 104: Argument prob out of range");
        }

        y = Math.sqrt(Math.log(1 / (p1 * p1)));
        z = y + ((((y * a4 + a3) * y + a2) * y + a1) * y + a0) / ((((y * b4 + b3) * y + b2) * y + b1) * y + b0);
        return (prob < 0.5 ? -z : z);
    }
} // class SiteModel
