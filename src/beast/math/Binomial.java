/*
 * Binomial.java
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
 * Binomial coefficients
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Korbinian Strimmer
 * @version $Id: Binomial.java,v 1.11 2005/05/24 20:26:00 rambaut Exp $
 */
public class Binomial {
    //
    // Public stuff
    //

    public static double logChoose(final int n, final int k) {
        return GammaFunction.lnGamma(n + 1.0) - GammaFunction.lnGamma(k + 1.0)
                - GammaFunction.lnGamma(n - k + 1.0);
    }


    /**
     * @param n total elements
     * @param k chosen elements
     * @return Binomial coefficient n choose k
     */
    public static double choose(double n, double k) {
        n = Math.floor(n + 0.5);
        k = Math.floor(k + 0.5);

        final double lchoose = GammaFunction.lnGamma(n + 1.0) -
                GammaFunction.lnGamma(k + 1.0) - GammaFunction.lnGamma(n - k + 1.0);

        return Math.floor(Math.exp(lchoose) + 0.5);
    }

    /**
     * @param n # elements
     * @return n choose 2 (number of distinct ways to choose a pair from n elements)
     */
    public static double choose2(final int n) {
        // not sure how much overhead there is with try-catch blocks
        // i.e. would an if statement be better?

        try {
            return choose2LUT[n];
        } catch (ArrayIndexOutOfBoundsException e) {
            if (n < 0) {
                return 0;
            }

            while (maxN < n) {
                maxN += 1000;
            }

            initialize();
            return choose2LUT[n];
        }
    }

    private static void initialize() {
        choose2LUT = new double[maxN + 1];
        choose2LUT[0] = 0;
        choose2LUT[1] = 0;
        choose2LUT[2] = 1;
        for (int i = 3; i <= maxN; i++) {
            choose2LUT[i] = (i * (i - 1)) * 0.5;
        }
    }

    private static int maxN = 5000;
    private static double[] choose2LUT;

    static {
        //
        initialize();
    }
}
