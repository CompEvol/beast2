/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.math.distribution;

import java.io.Serializable;

import org.apache.commons.math.MathException;
import org.apache.commons.math.MathRuntimeException;
import org.apache.commons.math.special.Gamma;
import org.apache.commons.math.util.MathUtils;

/**
 * Implementation for the {@link PoissonDistribution}.
 *
 * @version $Revision: 925812 $ $Date: 2010-03-21 11:49:31 -0400 (Sun, 21 Mar 2010) $
 */
public class PoissonDistributionImpl extends AbstractIntegerDistribution
        implements PoissonDistribution, Serializable {

    /**
     * Default maximum number of iterations for cumulative probability calculations.
     *
     * @since 2.1
     */
    public static final int DEFAULT_MAX_ITERATIONS = 10000000;

    /**
     * Default convergence criterion.
     *
     * @since 2.1
     */
    public static final double DEFAULT_EPSILON = 1E-12;

    /**
     * Serializable version identifier
     */
    private static final long serialVersionUID = -3349935121172596109L;

    /**
     * Distribution used to compute normal approximation.
     */
    private NormalDistribution normal;

    /**
     * Holds the Poisson mean for the distribution.
     */
    private double mean;

    /**
     * Maximum number of iterations for cumulative probability.
     * <p/>
     * Cumulative probabilities are estimated using either Lanczos series approximation of
     * Gamma#regularizedGammaP or continued fraction approximation of Gamma#regularizedGammaQ.
     */
    private int maxIterations = DEFAULT_MAX_ITERATIONS;

    /**
     * Convergence criterion for cumulative probability.
     */
    private double epsilon = DEFAULT_EPSILON;

    /**
     * Create a new Poisson distribution with the given the mean. The mean value
     * must be positive; otherwise an <code>IllegalArgument</code> is thrown.
     *
     * @param p the Poisson mean
     * @throws IllegalArgumentException if p &le; 0
     */
    public PoissonDistributionImpl(double p) {
        this(p, new NormalDistributionImpl());
    }

    /**
     * Create a new Poisson distribution with the given mean, convergence criterion
     * and maximum number of iterations.
     *
     * @param p             the Poisson mean
     * @param epsilon       the convergence criteria for cumulative probabilites
     * @param maxIterations the maximum number of iterations for cumulative probabilites
     * @since 2.1
     */
    public PoissonDistributionImpl(double p, double epsilon, int maxIterations) {
        setMean(p);
        this.epsilon = epsilon;
        this.maxIterations = maxIterations;
    }

    /**
     * Create a new Poisson distribution with the given mean and convergence criterion.
     *
     * @param p       the Poisson mean
     * @param epsilon the convergence criteria for cumulative probabilites
     * @since 2.1
     */
    public PoissonDistributionImpl(double p, double epsilon) {
        setMean(p);
        this.epsilon = epsilon;
    }

    /**
     * Create a new Poisson distribution with the given mean and maximum number of iterations.
     *
     * @param p             the Poisson mean
     * @param maxIterations the maximum number of iterations for cumulative probabilites
     * @since 2.1
     */
    public PoissonDistributionImpl(double p, int maxIterations) {
        setMean(p);
        this.maxIterations = maxIterations;
    }


    /**
     * Create a new Poisson distribution with the given the mean. The mean value
     * must be positive; otherwise an <code>IllegalArgument</code> is thrown.
     *
     * @param p the Poisson mean
     * @param z a normal distribution used to compute normal approximations.
     * @throws IllegalArgumentException if p &le; 0
     * @since 1.2
     * @deprecated as of 2.1 (to avoid possibly inconsistent state, the
     *             "NormalDistribution" will be instantiated internally)
     */
    @Deprecated
    public PoissonDistributionImpl(double p, NormalDistribution z) {
        super();
        setNormalAndMeanInternal(z, p);
    }

    /**
     * Get the Poisson mean for the distribution.
     *
     * @return the Poisson mean for the distribution.
     */
    @Override
	public double getMean() {
        return mean;
    }

    /**
     * Set the Poisson mean for the distribution. The mean value must be
     * positive; otherwise an <code>IllegalArgument</code> is thrown.
     *
     * @param p the Poisson mean value
     * @throws IllegalArgumentException if p &le; 0
     * @deprecated as of 2.1 (class will become immutable in 3.0)
     */
    @Override
	@Deprecated
    public void setMean(double p) {
        setNormalAndMeanInternal(normal, p);
    }

    /**
     * Set the Poisson mean for the distribution. The mean value must be
     * positive; otherwise an <code>IllegalArgument</code> is thrown.
     *
     * @param z the new distribution
     * @param p the Poisson mean value
     * @throws IllegalArgumentException if p &le; 0
     */
    @SuppressWarnings("deprecation")
	private void setNormalAndMeanInternal(NormalDistribution z,
                                          double p) {
        if (p <= 0) {
            throw MathRuntimeException.createIllegalArgumentException(
                    "the Poisson mean must be positive ({0})", p);
        }
        mean = p;
        normal = z;
        normal.setMean(p);
        normal.setStandardDeviation(Math.sqrt(p));
    }

    /**
     * The probability mass function P(X = x) for a Poisson distribution.
     *
     * @param x the value at which the probability density function is
     *          evaluated.
     * @return the value of the probability mass function at x
     */
    @Override
	public double probability(int x) {
        double ret;
        if (x < 0 || x == Integer.MAX_VALUE) {
            ret = 0.0;
        } else if (x == 0) {
            ret = Math.exp(-mean);
        } else {
            ret = Math.exp(-SaddlePointExpansion.getStirlingError(x) -
                    SaddlePointExpansion.getDeviancePart(x, mean)) /
                    Math.sqrt(MathUtils.TWO_PI * x);
        }
        return ret;
    }

    /**
     * The probability distribution function P(X <= x) for a Poisson
     * distribution.
     *
     * @param x the value at which the PDF is evaluated.
     * @return Poisson distribution function evaluated at x
     * @throws MathException if the cumulative probability can not be computed
     *                       due to convergence or other numerical errors.
     */
    @Override
    public double cumulativeProbability(int x) throws MathException {
        if (x < 0) {
            return 0;
        }
        if (x == Integer.MAX_VALUE) {
            return 1;
        }
        return Gamma.regularizedGammaQ((double) x + 1, mean, epsilon, maxIterations);
    }

    /**
     * Calculates the Poisson distribution function using a normal
     * approximation. The <code>N(mean, sqrt(mean))</code> distribution is used
     * to approximate the Poisson distribution.
     * <p>
     * The computation uses "half-correction" -- evaluating the normal
     * distribution function at <code>x + 0.5</code>
     * </p>
     *
     * @param x the upper bound, inclusive
     * @return the distribution function value calculated using a normal
     *         approximation
     * @throws MathException if an error occurs computing the normal
     *                       approximation
     */
    @Override
	public double normalApproximateProbability(int x) throws MathException {
        // calculate the probability using half-correction
        return normal.cumulativeProbability(x + 0.5);
    }

    /**
     * Access the domain value lower bound, based on <code>p</code>, used to
     * bracket a CDF root. This method is used by
     * {@link #inverseCumulativeProbability(double)} to find critical values.
     *
     * @param p the desired probability for the critical value
     * @return domain lower bound
     */
    @Override
    protected int getDomainLowerBound(double p) {
        return 0;
    }

    /**
     * Access the domain value upper bound, based on <code>p</code>, used to
     * bracket a CDF root. This method is used by
     * {@link #inverseCumulativeProbability(double)} to find critical values.
     *
     * @param p the desired probability for the critical value
     * @return domain upper bound
     */
    @Override
    protected int getDomainUpperBound(double p) {
        return Integer.MAX_VALUE;
    }

    /**
     * Modify the normal distribution used to compute normal approximations. The
     * caller is responsible for insuring the normal distribution has the proper
     * parameter settings.
     *
     * @param value the new distribution
     * @since 1.2
     * @deprecated as of 2.1 (class will become immutable in 3.0)
     */
    @Deprecated
    public void setNormal(NormalDistribution value) {
        setNormalAndMeanInternal(value, mean);
    }
}
