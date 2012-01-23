/*
 * DiscreteStatistics.java
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

package test.beast.beast2vs1.trace;

import beast.util.HeapSort;

/**
 * simple discrete statistics (mean, variance, cumulative probability, quantiles etc.)
 *
 * @author Korbinian Strimmer
 * @author Alexei Drummond
 * @version $Id: DiscreteStatistics.java,v 1.11 2006/07/02 21:14:53 rambaut Exp $
 */
public class DiscreteStatistics {
    /**
     * compute mean
     *
     * @param x list of numbers
     * @return mean
     */
    public static double mean(double[] x) {
        double m = 0;
        int count = x.length;
        for (double aX : x) {
            if (Double.isNaN(aX)) {
                count--;
            } else {
                m += aX;
            }
        }

        return m / (double) count;
    }

    /**
     * compute median
     *
     * @param x       list of numbers
     * @param indices index sorting x
     * @return median
     */
    public static double median(double[] x, int[] indices) {

        int pos = x.length / 2;
        if (x.length % 2 == 1) {
            return x[indices[pos]];
        } else {
            return (x[indices[pos - 1]] + x[indices[pos]]) / 2.0;
        }
    }

    /**
     * compute the mean squared error
     *
     * @param x         list of numbers
     * @param trueValue truth
     * @return MSE
     */

    public static double meanSquaredError(double[] x, double trueValue) {

        if (x == null || x.length == 0) {
            throw new IllegalArgumentException();
        }

        double total = 0;
        for (double sample : x) {
            total += (sample - trueValue) * (sample - trueValue);
        }
        total /= x.length;
        return total;
    }

    /**
     * compute median
     *
     * @param x list of numbers
     * @return median
     */
    public static double median(double[] x) {

        if (x == null || x.length == 0) {
            throw new IllegalArgumentException();
        }

        int[] indices = new int[x.length];
        HeapSort.sort(x, indices);

        return median(x, indices);
    }


    /**
     * compute variance (ML estimator)
     *
     * @param x    list of numbers
     * @param mean assumed mean of x
     * @return variance of x (ML estimator)
     */
    public static double variance(double[] x, double mean) {
        double var = 0;
        int count = x.length;
        for (double aX : x) {
            if (Double.isNaN(aX)) {
                count--;
            } else {
                double diff = aX - mean;
                var += diff * diff;
            }
        }

        if (count < 2) {
            count = 1; // to avoid division by zero
        } else {
            count = count - 1; // for ML estimate
        }

        return var / (double) count;
    }


    /**
     * compute covariance
     *
     * @param x list of numbers
     * @param y list of numbers
     * @return covariance of x and y
     */
    @SuppressWarnings({"SuspiciousNameCombination"})
    public static double covariance(double[] x, double[] y) {

        return covariance(x, y, mean(x), mean(y), stdev(x), stdev(y));
    }

    /**
     * compute covariance
     *
     * @param x      list of numbers
     * @param y      list of numbers
     * @param xmean  assumed mean of x
     * @param ymean  assumed mean of y
     * @param xstdev assumed stdev of x
     * @param ystdev assumed stdev of y
     * @return covariance of x and y
     */
    public static double covariance(double[] x, double[] y, double xmean, double ymean, double xstdev, double ystdev) {

        if (x.length != y.length) throw new IllegalArgumentException("x and y arrays must be same length!");

        int count = x.length;
        double covar = 0.0;
        for (int i = 0; i < x.length; i++) {
            if (Double.isNaN(x[i]) || Double.isNaN(y[i])) {
                count--;
            } else {
                covar += (x[i] - xmean) * (y[i] - ymean);
            }
        }
        covar /= count;
        covar /= (xstdev * ystdev);
        return covar;
    }

    /**
     * compute fisher skewness
     *
     * @param x list of numbers
     * @return skewness of x
     */
    public static double skewness(double[] x) {

        double mean = mean(x);
        double stdev = stdev(x);
        double skew = 0.0;
        double len = x.length;

        for (double xv : x) {
            double diff = xv - mean;
            diff /= stdev;

            skew += (diff * diff * diff);
        }

        skew *= (len / ((len - 1) * (len - 2)));

        return skew;
    }

    /**
     * compute standard deviation
     *
     * @param x list of numbers
     * @return standard deviation of x
     */
    public static double stdev(double[] x) {
        return Math.sqrt(variance(x));
    }

    /**
     * compute variance (ML estimator)
     *
     * @param x list of numbers
     * @return variance of x (ML estimator)
     */
    public static double variance(double[] x) {
        final double m = mean(x);
        return variance(x, m);
    }


    /**
     * compute variance of sample mean (ML estimator)
     *
     * @param x    list of numbers
     * @param mean assumed mean of x
     * @return variance of x (ML estimator)
     */
    public static double varianceSampleMean(double[] x, double mean) {
        return variance(x, mean) / (double) x.length;
    }

    /**
     * compute variance of sample mean (ML estimator)
     *
     * @param x list of numbers
     * @return variance of x (ML estimator)
     */
    public static double varianceSampleMean(double[] x) {
        return variance(x) / (double) x.length;
    }


    /**
     * compute the q-th quantile for a distribution of x
     * (= inverse cdf)
     *
     * @param q       quantile (0 < q <= 1)
     * @param x       discrete distribution (an unordered list of numbers)
     * @param indices index sorting x
     * @return q-th quantile
     */
    public static double quantile(double q, double[] x, int[] indices) {
        if (q < 0.0 || q > 1.0) throw new IllegalArgumentException("Quantile out of range");

        if (q == 0.0) {
            // for q==0 we have to "invent" an entry smaller than the smallest x

            return x[indices[0]] - 1.0;
        }

        return x[indices[(int) Math.ceil(q * indices.length) - 1]];
    }

    /**
     * compute the q-th quantile for a distribution of x
     * (= inverse cdf)
     *
     * @param q quantile (0 <= q <= 1)
     * @param x discrete distribution (an unordered list of numbers)
     * @return q-th quantile
     */
    public static double quantile(double q, double[] x) {
        int[] indices = new int[x.length];
        HeapSort.sort(x, indices);

        return quantile(q, x, indices);
    }

    /**
     * compute the q-th quantile for a distribution of x
     * (= inverse cdf)
     *
     * @param q     quantile (0 <= q <= 1)
     * @param x     discrete distribution (an unordered list of numbers)
     * @param count use only first count entries in x
     * @return q-th quantile
     */
    public static double quantile(double q, double[] x, int count) {
        int[] indices = new int[count];
        HeapSort.sort(x, indices);

        return quantile(q, x, indices);
    }

    /**
     * Determine the highest posterior density for a list of values.
     * The HPD is the smallest interval containing the required amount of elements.
     *
     * @param proportion of elements inside the interval
     * @param x          values
     * @param indices    index sorting x
     * @return the interval, an array of {low, high} values.
     */
    public static double[] HPDInterval(double proportion, double[] x, int[] indices) {

        double minRange = Double.MAX_VALUE;
        int hpdIndex = 0;

        final int diff = (int) Math.round(proportion * (double) x.length);
        for (int i = 0; i <= (x.length - diff); i++) {
            final double minValue = x[indices[i]];
            final double maxValue = x[indices[i + diff - 1]];
            final double range = Math.abs(maxValue - minValue);
            if (range < minRange) {
                minRange = range;
                hpdIndex = i;
            }
        }

        return new double[]{x[indices[hpdIndex]], x[indices[hpdIndex + diff - 1]]};
    }

    /**
     * compute the cumulative probability Pr(x <= z) for a given z
     * and a distribution of x
     *
     * @param z       threshold value
     * @param x       discrete distribution (an unordered list of numbers)
     * @param indices index sorting x
     * @return cumulative probability
     */
    public static double cdf(double z, double[] x, int[] indices) {
        int i;
        for (i = 0; i < x.length; i++) {
            if (x[indices[i]] > z) break;
        }

        return (double) i / (double) x.length;
    }

    /**
     * compute the cumulative probability Pr(x <= z) for a given z
     * and a distribution of x
     *
     * @param z threshold value
     * @param x discrete distribution (an unordered list of numbers)
     * @return cumulative probability
     */
    public static double cdf(double z, double[] x) {
        int[] indices = new int[x.length];
        HeapSort.sort(x, indices);

        return cdf(z, x, indices);
    }

    public static double max(double[] x) {
        double max = x[0];
        for (int i = 1; i < x.length; i++) {
            if (x[i] > max) max = x[i];
        }
        return max;
    }

    public static double min(double[] x) {
        double min = x[0];
        for (int i = 1; i < x.length; i++) {
            if (x[i] < min) min = x[i];
        }
        return min;
    }

    public static double geometricMean(double[] x) {
        double gm = 0;
        int len = x.length;
        for (int i = 0; i < len; i++) {
            gm += Math.log(x[i]);
        }

        return Math.exp(gm / (double) len);
    }
}
