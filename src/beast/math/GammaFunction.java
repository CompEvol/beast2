/*
 * GammaFunction.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
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
package beast.math;

/**
 * gamma function
 *
 * @author Korbinian Strimmer
 * @version $Id: GammaFunction.java,v 1.3 2005/05/24 20:26:01 rambaut Exp $
 */
public class GammaFunction {
    //
    // Public stuff
    //

    // Gamma function

    /**
     * log Gamma function: ln(gamma(alpha)) for alpha>0, accurate to 10 decimal places
     *
     * @param alpha argument
     * @return the log of the gamma function of the given alpha
     */
    public static double lnGamma(double alpha) {
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
     * Incomplete Gamma function Q(a,x)
     * (a cleanroom implementation of Numerical Recipes gammq(a,x);
     * in Mathematica this function is called GammaRegularized)
     *
     * @param a parameter
     * @param x argument
     * @return function value
     */
    public static double incompleteGammaQ(double a, double x) {
        return 1.0 - incompleteGamma(x, a, lnGamma(a));
    }

    /**
     * Incomplete Gamma function P(a,x) = 1-Q(a,x)
     * (a cleanroom implementation of Numerical Recipes gammp(a,x);
     * in Mathematica this function is 1-GammaRegularized)
     *
     * @param a parameter
     * @param x argument
     * @return function value
     */
    public static double incompleteGammaP(double a, double x) {
        return incompleteGamma(x, a, lnGamma(a));
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
     */
    public static double incompleteGammaP(double a, double x, double lnGammaA) {
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
     */
    private static double incompleteGamma(double x, double alpha, double ln_gamma_alpha) {
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
            throw new IllegalArgumentException("Arguments out of bounds");
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

}
