/*
 * ParametricDistributionModel.java
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

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.IntegerDistribution;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.util.Randomizer;

/**
 * A class that describes a parametric distribution
 *
 * * (FIXME) cumulative functions disregard offset. Serious bug if they are used.
 *
 * @author Alexei Drummond
 * @version $Id: ParametricDistributionModel.java,v 1.4 2005/05/24 20:25:59 rambaut Exp $
 */

@Description("A class that describes a parametric distribution, that is, a distribution that takes some " +
        "parameters/valuables as inputs and can produce (cumulative) densities and inverse " +
        "cumulative densities.")
public abstract class ParametricDistribution extends CalculationNode implements ContinuousDistribution {
    public final Input<Double> offsetInput = new Input<>("offset", "offset of origin (defaults to 0)", 0.0);

    abstract public org.apache.commons.math.distribution.Distribution getDistribution();

    /**
     * Calculate log probability of a valuable x for this distribution.
     * If x is multidimensional, the components of x are assumed to be independent,
     * so the sum of log probabilities of all elements of x is returned as the prior.
     */
    public double calcLogP(final Function fun) {
        final double offset = offsetInput.get();
        double logP = 0;
        for (int i = 0; i < fun.getDimension(); i++) {
            final double x = fun.getArrayValue(i);
            logP += logDensity(x, offset);
        }
        return logP;
    }

    /*
     * This implementation is only suitable for univariate distributions.
     * Must be overwritten for multivariate ones.
     */
    public Double[][] sample(final int size) throws MathException {
        final Double[][] sample = new Double[size][];
        for (int i = 0; i < sample.length; i++) {
            final double p = Randomizer.nextDouble();
            sample[i] = new Double[]{inverseCumulativeProbability(p)};
        }
        return sample;

    }

    /**
     * For this distribution, X, this method returns x such that P(X &lt; x) = p.
     *
     * @param p the cumulative probability.
     * @return x.
     * @throws MathException if the inverse cumulative probability can not be
     *                       computed due to convergence or other numerical errors.
     */
    //@Override
    @Override
	public double inverseCumulativeProbability(final double p) throws MathException {
        final org.apache.commons.math.distribution.Distribution dist = getDistribution();
        double offset = getOffset();
        if (dist instanceof ContinuousDistribution) {
            return offset + ((ContinuousDistribution) dist).inverseCumulativeProbability(p);
        } else if (dist instanceof IntegerDistribution) {
            return offset + ((IntegerDistribution)dist).inverseCumulativeProbability(p);
        }
        return 0.0;
    }

    /**
     * Return the probability density for a particular point.
     * NB this does not take offset in account
     *
     * @param x The point at which the density should be computed.
     * @return The pdf at point x.
     */
    //@Override
    @Override
	public double density(double x) {
        final double offset = getOffset();
 //       if( x >= offset ) {
            x -= offset;
            final org.apache.commons.math.distribution.Distribution dist = getDistribution();
            if (dist instanceof ContinuousDistribution) {
                return ((ContinuousDistribution) dist).density(x);
            } else if (dist instanceof IntegerDistribution) {
                return ((IntegerDistribution) dist).probability(x);
            }
   //     }
        return 0.0;
    }
    
    private double logDensity(double x, final double offset) {
   //     if( x >= offset ) {
            x -= offset;
            final org.apache.commons.math.distribution.Distribution dist = getDistribution();
            if (dist instanceof ContinuousDistribution) {
                return ((ContinuousDistribution) dist).logDensity(x);
            } else if (dist instanceof IntegerDistribution) {
                final double probability = ((IntegerDistribution) dist).probability(x);
                if( probability > 0 ) {
                    return Math.log(probability);
                }
            }
  //      }
        return Double.NEGATIVE_INFINITY;
    }

    //@Override
    @Override
	public double logDensity(final double x) {
        return logDensity(x, getOffset());
    }

    /**
     * For a random variable X whose values are distributed according
     * to this distribution, this method returns P(X &le; x).  In other words,
     * this method represents the  (cumulative) distribution function, or
     * CDF, for this distribution.
     *
     * @param x the value at which the distribution function is evaluated.
     * @return the probability that a random variable with this
     *         distribution takes a value less than or equal to <code>x</code>
     * @throws MathException if the cumulative probability can not be
     *                       computed due to convergence or other numerical errors.
     */
    //@Override
    @Override
	public double cumulativeProbability(final double x) throws MathException {
        return getDistribution().cumulativeProbability(x);
    }

    /**
     * For a random variable X whose values are distributed according
     * to this distribution, this method returns P(x0 &le; X &le; x1).
     *
     * @param x0 the (inclusive) lower bound
     * @param x1 the (inclusive) upper bound
     * @return the probability that a random variable with this distribution
     *         will take a value between <code>x0</code> and <code>x1</code>,
     *         including the endpoints
     * @throws MathException            if the cumulative probability can not be
     *                                  computed due to convergence or other numerical errors.
     * @throws IllegalArgumentException if <code>x0 > x1</code>
     */
    //@Override
    @Override
	public double cumulativeProbability(final double x0, final double x1) throws MathException {
        return getDistribution().cumulativeProbability(x0, x1);
    }

    /**
     * @return  offset of distribution.
     */
    public double getOffset() {
        return offsetInput.get();
    }

    protected double getMeanWithoutOffset() {
        throw new RuntimeException("Not implemented yet");
    }

    /** returns mean of distribution, if implemented **/
    public double getMean() {
        return getMeanWithoutOffset() + getOffset();
    }
    
    /**
     * @return true if the distribution is an integer distribution
     */
    public boolean isIntegerDistribution() {
        return getDistribution() instanceof IntegerDistribution;
    }
}