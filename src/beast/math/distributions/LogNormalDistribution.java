/*
 * LogNormalDistribution.java
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

package beast.math.distributions;


import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
/**
 * log normal distribution (pdf, cdf, quantile)
 *
 * @author Korbinian Strimmer
 * @version $Id: LogNormalDistribution.java,v 1.3 2005/06/21 16:25:15 beth Exp $
 */
public class LogNormalDistribution implements Distribution {
    //
    // Public stuff
    //

    /**
     * @param M the mean of the random variable's natural logarithm
     * @param S the stdev of the random variable's natural logarithm
     */
    public LogNormalDistribution(double M, double S) {
        this.M = M;
        this.S = S;
    }

    public final double getM() {
        return M;
    }

    public final void setM(double M) {
        this.M = M;
    }

    public final double getS() {
        return S;
    }

    public final void setS(double S) {
        this.S = S;
    }

    public double pdf(double x) {
        return pdf(x, M, S);
    }

    public double logPdf(double x) {
        return logPdf(x, M, S);
    }

    public double cdf(double x) {
        return cdf(x, M, S);
    }

    public double quantile(double y) {
        return quantile(y, M, S);
    }

    public double mean() {
        return mean(M, S);
    }

    public double variance() {
        return variance(M, S);
    }

    public double mode() {
        return mode(M, S);
    }

    public final UnivariateRealFunction getProbabilityDensityFunction() {
        return pdfFunction;
    }

    private final UnivariateRealFunction pdfFunction = new UnivariateRealFunction() {
        public final double value(double x) {
            return pdf(x);
        }
    };

    /**
     * probability density function
     *
     * @param x argument
     * @param M log mean
     * @param S log standard deviation
     * @return pdf at x
     */
    public static double pdf(double x, double M, double S) {

        normal.setMean(M);
        normal.setMean(S);
        return normal.density(Math.log(x)) / x;
    }

    /**
     * the natural log of the probability density function of the distribution
     *
     * @param x argument
     * @param M log mean
     * @param S log standard deviation
     * @return log pdf at x
     */
    public static double logPdf(double x, double M, double S) {
        return Math.log(pdf(x,M,S));
    }

    /**
     * cumulative density function
     *
     * @param x argument
     * @param M log mean
     * @param S log standard deviation
     * @return cdf at x
     */
    public static double cdf(double x, double M, double S) {

        normal.setMean(M);
        normal.setMean(S);
        try {
            return normal.cumulativeProbability(Math.log(x));
        } catch (MathException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return Double.NaN;
        }
        //return NormalDistribution.cdf(Math.log(x), M, S);
    }

    /**
     * quantiles (=inverse cumulative density function)
     *
     * @param z argument
     * @param M log mean
     * @param S log standard deviation
     * @return icdf at z
     */
    public static double quantile(double z, double M, double S) {


        try {
            return Math.exp(normal.inverseCumulativeProbability(z));
        } catch (MathException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return Double.NaN;
        }
    }

    /**
     * mean
     *
     * @param M log mean
     * @param S log standard deviation
     * @return mean
     */
    public static double mean(double M, double S) {
        return Math.exp(M + (S * S / 2));
    }

    /**
     * mode
     *
     * @param M log mean
     *
     * @param S log standard deviation
     * @return mode
     */
    public static double mode(double M, double S) {
        return Math.exp(M - S * S);
    }

    /**
     * variance
     *
     * @param M log mean
     * @param S log standard deviation
     * @return variance
     */
    public static double variance(double M, double S) {
        final double S2 = S * S;

        return Math.exp(S2 + 2 * M) * (Math.exp(S2) - 1);
    }

    // Private

    protected double M, S;

    static NormalDistributionImpl normal = new NormalDistributionImpl(0, 1);
}
