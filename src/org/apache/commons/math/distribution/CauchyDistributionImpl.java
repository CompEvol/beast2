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

import org.apache.commons.math.MathRuntimeException;

/**
 * Default implementation of
 * {@link org.apache.commons.math.distribution.CauchyDistribution}.
 *
 * @version $Revision: 925900 $ $Date: 2010-03-21 17:10:07 -0400 (Sun, 21 Mar 2010) $
 * @since 1.1
 */
public class CauchyDistributionImpl extends AbstractContinuousDistribution
        implements CauchyDistribution, Serializable {

    /**
     * Default inverse cumulative probability accuracy
     *
     * @since 2.1
     */
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;

    /**
     * Serializable version identifier
     */
    private static final long serialVersionUID = 8589540077390120676L;

    /**
     * The median of this distribution.
     */
    private double median = 0;

    /**
     * The scale of this distribution.
     */
    private double scale = 1;

    /**
     * Inverse cumulative probability accuracy
     */
    private final double solverAbsoluteAccuracy;

    /**
     * Creates cauchy distribution with the medain equal to zero and scale
     * equal to one.
     */
    public CauchyDistributionImpl() {
        this(0.0, 1.0);
    }

    /**
     * Create a cauchy distribution using the given median and scale.
     *
     * @param median median for this distribution
     * @param s      scale parameter for this distribution
     */
    public CauchyDistributionImpl(double median, double s) {
        this(median, s, DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Create a cauchy distribution using the given median and scale.
     *
     * @param median             median for this distribution
     * @param s                  scale parameter for this distribution
     * @param inverseCumAccuracy the maximum absolute error in inverse cumulative probability estimates
     *                           (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY})
     * @since 2.1
     */
    public CauchyDistributionImpl(double median, double s, double inverseCumAccuracy) {
        super();
        setMedianInternal(median);
        setScaleInternal(s);
        solverAbsoluteAccuracy = inverseCumAccuracy;
    }

    /**
     * For this distribution, X, this method returns P(X &lt; <code>x</code>).
     *
     * @param x the value at which the CDF is evaluated.
     * @return CDF evaluted at <code>x</code>.
     */
    @Override
	public double cumulativeProbability(double x) {
        return 0.5 + (Math.atan((x - median) / scale) / Math.PI);
    }

    /**
     * Access the median.
     *
     * @return median for this distribution
     */
    @Override
	public double getMedian() {
        return median;
    }

    /**
     * Access the scale parameter.
     *
     * @return scale parameter for this distribution
     */
    @Override
	public double getScale() {
        return scale;
    }

    /**
     * Returns the probability density for a particular point.
     *
     * @param x The point at which the density should be computed.
     * @return The pdf at point x.
     * @since 2.1
     */
    @Override
    public double density(double x) {
        final double dev = x - median;
        return (1 / Math.PI) * (scale / (dev * dev + scale * scale));
    }

    /**
     * For this distribution, X, this method returns the critical point x, such
     * that P(X &lt; x) = <code>p</code>.
     * <p>
     * Returns <code>Double.NEGATIVE_INFINITY</code> for p=0 and
     * <code>Double.POSITIVE_INFINITY</code> for p=1.</p>
     *
     * @param p the desired probability
     * @return x, such that P(X &lt; x) = <code>p</code>
     * @throws IllegalArgumentException if <code>p</code> is not a valid
     *                                  probability.
     */
    @Override
    public double inverseCumulativeProbability(double p) {
        double ret;
        if (p < 0.0 || p > 1.0) {
            throw MathRuntimeException.createIllegalArgumentException(
                    "{0} out of [{1}, {2}] range", p, 0.0, 1.0);
        } else if (p == 0) {
            ret = Double.NEGATIVE_INFINITY;
        } else if (p == 1) {
            ret = Double.POSITIVE_INFINITY;
        } else {
            ret = median + scale * Math.tan(Math.PI * (p - .5));
        }
        return ret;
    }

    /**
     * Modify the median.
     *
     * @param median for this distribution
     * @deprecated as of 2.1 (class will become immutable in 3.0)
     */
    @Override
	@Deprecated
    public void setMedian(double median) {
        setMedianInternal(median);
    }

    /**
     * Modify the median.
     *
     * @param newMedian for this distribution
     */
    private void setMedianInternal(double newMedian) {
        this.median = newMedian;
    }

    /**
     * Modify the scale parameter.
     *
     * @param s scale parameter for this distribution
     * @throws IllegalArgumentException if <code>sd</code> is not positive.
     * @deprecated as of 2.1 (class will become immutable in 3.0)
     */
    @Override
	@Deprecated
    public void setScale(double s) {
        setScaleInternal(s);
    }

    /**
     * Modify the scale parameter.
     *
     * @param s scale parameter for this distribution
     * @throws IllegalArgumentException if <code>sd</code> is not positive.
     */
    private void setScaleInternal(double s) {
        if (s <= 0.0) {
            throw MathRuntimeException.createIllegalArgumentException(
                    "scale must be positive ({0})", s);
        }
        scale = s;
    }

    /**
     * Access the domain value lower bound, based on <code>p</code>, used to
     * bracket a CDF root.  This method is used by
     * {@link #inverseCumulativeProbability(double)} to find critical values.
     *
     * @param p the desired probability for the critical value
     * @return domain value lower bound, i.e.
     *         P(X &lt; <i>lower bound</i>) &lt; <code>p</code>
     */
    @Override
    protected double getDomainLowerBound(double p) {
        double ret;

        if (p < .5) {
            ret = -Double.MAX_VALUE;
        } else {
            ret = median;
        }

        return ret;
    }

    /**
     * Access the domain value upper bound, based on <code>p</code>, used to
     * bracket a CDF root.  This method is used by
     * {@link #inverseCumulativeProbability(double)} to find critical values.
     *
     * @param p the desired probability for the critical value
     * @return domain value upper bound, i.e.
     *         P(X &lt; <i>upper bound</i>) &gt; <code>p</code>
     */
    @Override
    protected double getDomainUpperBound(double p) {
        double ret;

        if (p < .5) {
            ret = median;
        } else {
            ret = Double.MAX_VALUE;
        }

        return ret;
    }

    /**
     * Access the initial domain value, based on <code>p</code>, used to
     * bracket a CDF root.  This method is used by
     * {@link #inverseCumulativeProbability(double)} to find critical values.
     *
     * @param p the desired probability for the critical value
     * @return initial domain value
     */
    @Override
    protected double getInitialDomain(double p) {
        double ret;

        if (p < .5) {
            ret = median - scale;
        } else if (p > .5) {
            ret = median + scale;
        } else {
            ret = median;
        }

        return ret;
    }

    /**
     * Return the absolute accuracy setting of the solver used to estimate
     * inverse cumulative probabilities.
     *
     * @return the solver absolute accuracy
     * @since 2.1
     */
    @Override
    protected double getSolverAbsoluteAccuracy() {
        return solverAbsoluteAccuracy;
    }
}
