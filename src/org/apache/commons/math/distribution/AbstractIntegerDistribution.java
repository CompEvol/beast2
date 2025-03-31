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

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.MathRuntimeException;


/**
 * Base class for integer-valued discrete distributions.  Default
 * implementations are provided for some of the methods that do not vary
 * from distribution to distribution.
 *
 * @version $Revision: 920558 $ $Date: 2010-03-08 17:57:32 -0500 (Mon, 08 Mar 2010) $
 */
public abstract class AbstractIntegerDistribution extends AbstractDistribution
        implements IntegerDistribution, Serializable {

    /**
     * Message for endpoints in wrong order.
     */
    private static final String WRONG_ORDER_ENDPOINTS_MESSAGE =
            "lower endpoint ({0}) must be less than or equal to upper endpoint ({1})";

    /**
     * Message for out of range point.
     */
    private static final String OUT_OF_RANGE_POINT =
            "{0} out of [{1}, {2}] range";

    /**
     * Serializable version identifier
     */
    private static final long serialVersionUID = -1146319659338487221L;

    /**
     * Default constructor.
     */
    protected AbstractIntegerDistribution() {
        super();
    }

    /**
     * For a random variable X whose values are distributed according
     * to this distribution, this method returns P(X &le; x).  In other words,
     * this method represents the  (cumulative) distribution function, or
     * CDF, for this distribution.
     * <p/>
     * If <code>x</code> does not represent an integer value, the CDF is
     * evaluated at the greatest integer less than x.
     *
     * @param x the value at which the distribution function is evaluated.
     * @return cumulative probability that a random variable with this
     *         distribution takes a value less than or equal to <code>x</code>
     * @throws MathException if the cumulative probability can not be
     *                       computed due to convergence or other numerical errors.
     */
    @Override
	public double cumulativeProbability(double x) throws MathException {
        return cumulativeProbability((int) Math.floor(x));
    }

    /**
     * For a random variable X whose values are distributed according
     * to this distribution, this method returns P(x0 &le; X &le; x1).
     *
     * @param x0 the (inclusive) lower bound
     * @param x1 the (inclusive) upper bound
     * @return the probability that a random variable with this distribution
     *         will take a value between <code>x0</code> and <code>x1</code>,
     *         including the endpoints.
     * @throws MathException            if the cumulative probability can not be
     *                                  computed due to convergence or other numerical errors.
     * @throws IllegalArgumentException if <code>x0 > x1</code>
     */
    @Override
    public double cumulativeProbability(double x0, double x1)
            throws MathException {
        if (x0 > x1) {
            throw MathRuntimeException.createIllegalArgumentException(
                    WRONG_ORDER_ENDPOINTS_MESSAGE, x0, x1);
        }
        if (Math.floor(x0) < x0) {
            return cumulativeProbability(((int) Math.floor(x0)) + 1,
                    (int) Math.floor(x1)); // don't want to count mass below x0
        } else { // x0 is mathematical integer, so use as is
            return cumulativeProbability((int) Math.floor(x0),
                    (int) Math.floor(x1));
        }
    }

    /**
     * For a random variable X whose values are distributed according
     * to this distribution, this method returns P(X &le; x).  In other words,
     * this method represents the probability distribution function, or PDF,
     * for this distribution.
     *
     * @param x the value at which the PDF is evaluated.
     * @return PDF for this distribution.
     * @throws MathException if the cumulative probability can not be
     *                       computed due to convergence or other numerical errors.
     */
    @Override
	public abstract double cumulativeProbability(int x) throws MathException;

    /**
     * For a random variable X whose values are distributed according
     * to this distribution, this method returns P(X = x). In other words, this
     * method represents the probability mass function,  or PMF, for the distribution.
     * <p/>
     * If <code>x</code> does not represent an integer value, 0 is returned.
     *
     * @param x the value at which the probability density function is evaluated
     * @return the value of the probability density function at x
     */
    @Override
	public double probability(double x) {
        double fl = Math.floor(x);
        if (fl == x) {
            return this.probability((int) x);
        } else {
            return 0;
        }
    }

    /**
     * For a random variable X whose values are distributed according
     * to this distribution, this method returns P(x0 &le; X &le; x1).
     *
     * @param x0 the inclusive, lower bound
     * @param x1 the inclusive, upper bound
     * @return the cumulative probability.
     * @throws MathException            if the cumulative probability can not be
     *                                  computed due to convergence or other numerical errors.
     * @throws IllegalArgumentException if x0 > x1
     */
    @Override
	public double cumulativeProbability(int x0, int x1) throws MathException {
        if (x0 > x1) {
            throw MathRuntimeException.createIllegalArgumentException(
                    WRONG_ORDER_ENDPOINTS_MESSAGE, x0, x1);
        }
        return cumulativeProbability(x1) - cumulativeProbability(x0 - 1);
    }

    /**
     * Computes the quantile function of this distribution.
     * For a random variable {@code X} distributed according to this distribution,
     * the returned value is     
     * <ul>
     * <li>{@link #getDomainLowerBound()} for {@code p = 0},</li>
     * <li>{@link #getDomainUpperBound()} for {@code p = 1}, and</li>
     * <li>{@link #solveInverseCumulativeProbability(double, int, int)} for
     *     {@code 0 < p < 1}.</li>
     * </ul>
     *
     * @param p the desired probability
     * @return the smallest {@code p}-quantile of this distribution
     * (largest 0-quantile for {@code p = 0})
     * @throws MathException            if the inverse cumulative probability can not be
     *                                  computed due to convergence or other numerical errors.
     * @throws IllegalArgumentException if p < 0 or p > 1
     */
    @Override
    public int inverseCumulativeProbability(final double p) throws MathException {
        if (p < 0.0 || p > 1.0) {
            throw MathRuntimeException.createIllegalArgumentException(
                    OUT_OF_RANGE_POINT, p, 0.0, 1.0);
        }

        // by default, do simple bisection.
        // subclasses can override if there is a better method.
        int lower = getDomainLowerBound(p);
        if (p == 0.0) {
            return lower;
        }
        if (lower == Integer.MIN_VALUE) {
            if (checkedCumulativeProbability(lower) >= p) {
                return lower;
            }
        } else {
            lower -= 1; // this ensures cumulativeProbability(lower) < p, which
                        // is important for the solving step
        }
        int upper = getDomainUpperBound(p);
        if (p == 1.0) {
            return upper;
        }

        upper = solveInverseCumulativeProbability(p, lower, upper);

        // ensure upper lies within domain bounds
        upper = upper>=getDomainLowerBound(p) ? upper : getDomainLowerBound(p);
        upper = upper<=getDomainUpperBound(p) ? upper : getDomainUpperBound(p);
        
        return upper;
    }
    

    /**
     * This is a utility function used by {@link
     * #inverseCumulativeProbability(double)}. It assumes {@code 0 < p < 1} and
     * that the inverse cumulative probability lies in the bracket {@code
     * (lower, upper]}. The implementation does simple bisection to find the
     * smallest {@code p}-quantile <code>inf{x in Z | P(X<=x) >= p}</code>.
     *
     * @param p the cumulative probability
     * @param lower a value satisfying {@code cumulativeProbability(lower) < p}
     * @param upper a value satisfying {@code p <= cumulativeProbability(upper)}
     * @return the smallest {@code p}-quantile of this distribution
     * @throws FunctionEvaluationException 
     */
    protected int solveInverseCumulativeProbability(final double p, int lower, int upper) throws FunctionEvaluationException {
        while (lower + 1 < upper) {
            int xm = (lower + upper) / 2;
            if (xm < lower || xm > upper) {
                /*
                 * Overflow.
                 * There will never be an overflow in both calculation methods
                 * for xm at the same time
                 */
                xm = lower + (upper - lower) / 2;
            }

            double pm = checkedCumulativeProbability(xm);
            if (pm >= p) {
                upper = xm;
            } else {
                lower = xm;
            }
        }
        return upper;
    }
       


    /**
     * Computes the cumulative probablity function and checks for NaN values returned.
     * Throws MathException if the value is NaN. Wraps and rethrows any MathException encountered
     * evaluating the cumulative probability function in a FunctionEvaluationException. Throws
     * FunctionEvaluationException of the cumulative probability function returns NaN.
     *
     * @param argument input value
     * @return cumulative probability
     * @throws FunctionEvaluationException if a MathException occurs computing the cumulative probability
     */
    private double checkedCumulativeProbability(int argument) throws FunctionEvaluationException {
        double result = Double.NaN;
        try {
            result = cumulativeProbability(argument);
        } catch (MathException ex) {
            throw new FunctionEvaluationException(ex, argument, ex.getPattern(), ex.getArguments());
        }
        if (Double.isNaN(result)) {
            throw new FunctionEvaluationException(argument,
                    "Discrete cumulative probability function returned NaN for argument {0}", argument);
        }
        return result;
    }

    /**
     * Access the domain value lower bound, based on <code>p</code>, used to
     * bracket a PDF root.  This method is used by
     * {@link #inverseCumulativeProbability(double)} to find critical values.
     *
     * @param p the desired probability for the critical value
     * @return domain value lower bound, i.e.
     *         P(X &lt; <i>lower bound</i>) &lt; <code>p</code>
     */
    protected abstract int getDomainLowerBound(double p);

    /**
     * Access the domain value upper bound, based on <code>p</code>, used to
     * bracket a PDF root.  This method is used by
     * {@link #inverseCumulativeProbability(double)} to find critical values.
     *
     * @param p the desired probability for the critical value
     * @return domain value upper bound, i.e.
     *         P(X &lt; <i>upper bound</i>) &gt; <code>p</code>
     */
    protected abstract int getDomainUpperBound(double p);
}
