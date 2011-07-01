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
import beast.core.Input;
import beast.core.Valuable;
import beast.util.Randomizer;

/**
 * A class that describes a parametric distribution
 *
 * @author Alexei Drummond
 * @version $Id: ParametricDistributionModel.java,v 1.4 2005/05/24 20:25:59 rambaut Exp $
 */

@Description("A class that describes a parametric distribution, that is, a distribution that takes some " +
		"parameters/valuables as inputs and can produce (cummulative) densities and inverse " +
		"cummulative densities.")
public abstract class ParametricDistribution extends CalculationNode implements ContinuousDistribution {
	public Input<Double> m_offset = new Input<Double>("offset","offset of origin (defaults to 0)", 0.0); 

    abstract public org.apache.commons.math.distribution.Distribution getDistribution();

    /** Calculate log probability of a valuable x for this distribution.
	 * If x is multidimensional, the components of x are assumed to be independent,
	 * so the sum of log probabilities of all elements of x is returned as the prior.
	**/
    public double calcLogP(Valuable x) throws Exception {
		double fOffset = m_offset.get();
		double fLogP = 0;
		for (int i = 0; i < x.getDimension(); i++) {
			double fX = x.getArrayValue(i) - fOffset;
			//fLogP += Math.log(density(fX));
			fLogP += logDensity(fX);
		}
		return fLogP;
    }

    /*
     * This implemenatation is only suitable for univariate distributions.
     * Must be overwritten for multivariate ones.
     */
    public Double[][] sample(int size) throws Exception{
        Double[][] sample = new Double[size][];
        for(int i = 0; i < sample.length; i++){
            double p = Randomizer.nextDouble();
            sample[i] = new Double[]{inverseCumulativeProbability(p)};
        }
        return sample;

    }
    
    /**
     * For this distribution, X, this method returns x such that P(X &lt; x) = p.
     * @param p the cumulative probability.
     * @return x.
     * @throws MathException if the inverse cumulative probability can not be
     *            computed due to convergence or other numerical errors.
     */
    @Override
    public double inverseCumulativeProbability(double p) throws MathException {
    	org.apache.commons.math.distribution.Distribution dist = getDistribution();
    	if (dist instanceof ContinuousDistribution) {
    		return ((ContinuousDistribution) dist).inverseCumulativeProbability(p);
    	} else if (dist instanceof IntegerDistribution) {
    		return dist.cumulativeProbability(p);
    	}
   		return 0.0;
    }
    
    /**
     * Return the probability density for a particular point.
     * @param x  The point at which the density should be computed.
     * @return  The pdf at point x.
     */    
    @Override
    public double density(double x) {
    	org.apache.commons.math.distribution.Distribution dist = getDistribution();
    	if (dist instanceof ContinuousDistribution) {
            return ((ContinuousDistribution) dist).density(x);
    	} else if (dist instanceof IntegerDistribution) {
    		return ((IntegerDistribution)dist).probability(x);
    	}
   		return 0.0;
    }

    @Override
    public double logDensity(double x) {
    	org.apache.commons.math.distribution.Distribution dist = getDistribution();
    	if (dist instanceof ContinuousDistribution) {
            return ((ContinuousDistribution) dist).logDensity(x);
    	} else if (dist instanceof IntegerDistribution) {
    		return Math.log(((IntegerDistribution)dist).probability(x));
    	}
   		return 0.0;
    }

    /**
     * For a random variable X whose values are distributed according
     * to this distribution, this method returns P(X &le; x).  In other words,
     * this method represents the  (cumulative) distribution function, or
     * CDF, for this distribution.
     *
     * @param x the value at which the distribution function is evaluated.
     * @return the probability that a random variable with this
     * distribution takes a value less than or equal to <code>x</code>
     * @throws MathException if the cumulative probability can not be
     * computed due to convergence or other numerical errors.
     */
    @Override
    public double cumulativeProbability(double x) throws MathException {
    	return getDistribution().cumulativeProbability(x);
    }

    /**
     * For a random variable X whose values are distributed according
     * to this distribution, this method returns P(x0 &le; X &le; x1).
     *
     * @param x0 the (inclusive) lower bound
     * @param x1 the (inclusive) upper bound
     * @return the probability that a random variable with this distribution
     * will take a value between <code>x0</code> and <code>x1</code>,
     * including the endpoints
     * @throws MathException if the cumulative probability can not be
     * computed due to convergence or other numerical errors.
     * @throws IllegalArgumentException if <code>x0 > x1</code>
     */
    @Override
    public double cumulativeProbability(double x0, double x1) throws MathException {
    	return getDistribution().cumulativeProbability(x0, x1);
    }
}