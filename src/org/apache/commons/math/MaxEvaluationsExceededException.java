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
 * Error thrown when a numerical computation exceeds its allowed
 * number of functions evaluations.
 *
 * @version $Revision: 779273 $ $Date: 2009-05-27 14:54:48 -0400 (Wed, 27 May 2009) $
 * @since 2.0
 */
public class MaxEvaluationsExceededException extends ConvergenceException {

    /**
     * Serializable version identifier.
     */
    private static final long serialVersionUID = -5921271447220129118L;

    /**
     * Maximal number of evaluations allowed.
     */
    private final int maxEvaluations;

    /**
     * Constructs an exception with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     *
     * @param maxEvaluations maximal number of evaluations allowed
     */
    public MaxEvaluationsExceededException(final int maxEvaluations) {
        super("Maximal number of evaluations ({0}) exceeded", maxEvaluations);
        this.maxEvaluations = maxEvaluations;
    }

    /**
     * Constructs an exception with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     *
     * @param maxEvaluations the exceeded maximal number of evaluations
     * @param pattern        format specifier
     * @param arguments      format arguments
     */
    public MaxEvaluationsExceededException(final int maxEvaluations,
                                           final String pattern, final Object... arguments) {
        super(pattern, arguments);
        this.maxEvaluations = maxEvaluations;
    }

    /**
     * Get the maximal number of evaluations allowed.
     *
     * @return maximal number of evaluations allowed
     */
    public int getMaxEvaluations() {
        return maxEvaluations;
    }

}
