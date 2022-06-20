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
package org.apache.commons.math.analysis.solvers;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;

/**
 * Implements the <a href="http://mathworld.wolfram.com/Bisection.html">
 * bisection algorithm</a> for finding zeros of univariate real functions.
 * <p>
 * The function should be continuous but not necessarily smooth.</p>
 *
 * @version $Revision: 811685 $ $Date: 2009-09-05 13:36:48 -0400 (Sat, 05 Sep 2009) $
 */
public class BisectionSolver extends UnivariateRealSolverImpl {

    /**
     * Construct a solver for the given function.
     *
     * @param f function to solve.
     * @deprecated as of 2.0 the function to solve is passed as an argument
     *             to the {@link #solve(UnivariateRealFunction, double, double)} or
     *             {@link UnivariateRealSolverImpl#solve(UnivariateRealFunction, double, double, double)}
     *             method.
     */
    @Deprecated
    public BisectionSolver(UnivariateRealFunction f) {
        super(f, 100, 1E-6);
    }

    /**
     * Construct a solver.
     */
    public BisectionSolver() {
        super(100, 1E-6);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	@Deprecated
    public double solve(double min, double max, double initial)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	@Deprecated
    public double solve(double min, double max)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public double solve(final UnivariateRealFunction f, double min, double max, double initial)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public double solve(final UnivariateRealFunction f, double min, double max)
            throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min, max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
            fmin = f.value(min);
            fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
}
