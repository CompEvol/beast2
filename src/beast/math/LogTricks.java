/*
 * LogTricks.java
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

package beast.math;

/**
 * @author Marc Suchard
 */
public class LogTricks {

    public static final double maxFloat = Double.MAX_VALUE; //3.40282347E+38;
    public static final double logLimit = -maxFloat / 100;
    public static final double logZero = -maxFloat;
    public static final double NATS =  400; //40;

    public static double logSumNoCheck(double x, double y) {
        double temp = y - x;
        if (Math.abs(temp) > NATS)
            return (x > y) ? x : y;
        else
            return x + StrictMath.log1p(StrictMath.exp(temp));
    }

    public static double logSum(double[] x) {
        double sum = x[0];
        final int len = x.length;
        for(int i=1; i<len; i++)
            sum = logSumNoCheck(sum,x[i]);
        return sum;
    }

    public static double logSum(double x, double y) {
        final double temp = y - x;
        if (temp > NATS || x < logLimit)
            return y;
        if (temp < -NATS || y < logLimit)
            return x;
        if (temp < 0)
            return x + StrictMath.log1p(StrictMath.exp(temp));
        return y + StrictMath.log1p(StrictMath.exp(-temp));
    }

    public static void logInc(Double x, double y) {
        double temp = y - x;
        if (temp > NATS || x < logLimit)
            x = y;
        else if (temp < -NATS || y < logLimit)
            ;
        else
            x += StrictMath.log1p(StrictMath.exp(temp));
    }

    public static double logDiff(double x, double y) {
        assert x > y;
        double temp = y - x;
        if (temp < -NATS || y < logLimit)
            return x;
        return x + StrictMath.log1p(-Math.exp(temp));
    }

}
