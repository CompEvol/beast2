/*
 * MachineAccuracy.java
 *
 * Copyright (C) 2010 BEAST II Developer Group
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

package beast.util;


/**
 * determines machine accuracy
 *
 * @author Korbinian Strimmer
 * @author Alexei Drummond
 * @version $Id: MachineAccuracy.java,v 1.4 2005/05/24 20:26:01 rambaut Exp $
 */
public class MachineAccuracy {
    //
    // Public stuff
    //

    /**
     * machine accuracy constant
     */
    public static double EPSILON = 2.220446049250313E-16;

    public static double SQRT_EPSILON = 1.4901161193847656E-8;
    public static double SQRT_SQRT_EPSILON = 1.220703125E-4;

    /**
     * compute EPSILON from scratch
     */
    public static double computeEpsilon() {
        double eps = 1.0;

        while (eps + 1.0 != 1.0) {
            eps /= 2.0;
        }
        eps *= 2.0;

        return eps;
    }

    /**
     * @return true if the relative difference between the two parameters
     *         is smaller than SQRT_EPSILON.
     */
    public static boolean same(double a, double b) {
        return Math.abs((a / b) - 1.0) <= SQRT_EPSILON;
    }
    
    /**
     * Tests to see whether the absolute difference between a and b is
     * smaller than EPSILON.
     * 
     * @param a
     * @param b
     * @return result of test
     */
    public static boolean sameAbsolute(double a, double b) {
        return Math.abs(a-b) < EPSILON;
    }
}
