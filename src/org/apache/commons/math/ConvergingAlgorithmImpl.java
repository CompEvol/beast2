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

package org.apache.commons.math;


/**
 * Provide a default implementation for several functions useful to generic
 * converging algorithms.
 *
 * @version $Revision: 811685 $ $Date: 2009-09-05 13:36:48 -0400 (Sat, 05 Sep 2009) $
 * @since 2.0
 */
public abstract class ConvergingAlgorithmImpl implements ConvergingAlgorithm {

    /**
     * Maximum absolute error.
     */
    protected double absoluteAccuracy;

    /**
     * Maximum relative error.
     */
    protected double relativeAccuracy;

    /**
     * Maximum number of iterations.
     */
    protected int maximalIterationCount;

    /**
     * Default maximum absolute error.
     */
    protected double defaultAbsoluteAccuracy;

    /**
     * Default maximum relative error.
     */
    protected double defaultRelativeAccuracy;

    /**
     * Default maximum number of iterations.
     */
    protected int defaultMaximalIterationCount;

    // Mainly for test framework.
    /**
     * The last iteration count.
     */
    protected int iterationCount;

    /**
     * Construct an algorithm with given iteration count and accuracy.
     *
     * @param defaultAbsoluteAccuracy      maximum absolute error
     * @param defaultMaximalIterationCount maximum number of iterations
     * @throws IllegalArgumentException if f is null or the
     *                                  defaultAbsoluteAccuracy is not valid
     */
    protected ConvergingAlgorithmImpl(final int defaultMaximalIterationCount,
                                      final double defaultAbsoluteAccuracy) {
        this.defaultAbsoluteAccuracy = defaultAbsoluteAccuracy;
        this.defaultRelativeAccuracy = 1.0e-14;
        this.absoluteAccuracy = defaultAbsoluteAccuracy;
        this.relativeAccuracy = defaultRelativeAccuracy;
        this.defaultMaximalIterationCount = defaultMaximalIterationCount;
        this.maximalIterationCount = defaultMaximalIterationCount;
        this.iterationCount = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public int getIterationCount() {
        return iterationCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void setAbsoluteAccuracy(double accuracy) {
        absoluteAccuracy = accuracy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public double getAbsoluteAccuracy() {
        return absoluteAccuracy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void resetAbsoluteAccuracy() {
        absoluteAccuracy = defaultAbsoluteAccuracy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void setMaximalIterationCount(int count) {
        maximalIterationCount = count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public int getMaximalIterationCount() {
        return maximalIterationCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void resetMaximalIterationCount() {
        maximalIterationCount = defaultMaximalIterationCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void setRelativeAccuracy(double accuracy) {
        relativeAccuracy = accuracy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public double getRelativeAccuracy() {
        return relativeAccuracy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void resetRelativeAccuracy() {
        relativeAccuracy = defaultRelativeAccuracy;
    }

}
