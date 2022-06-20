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

package org.apache.commons.math.analysis;

import org.apache.commons.math.FunctionEvaluationException;

/**
 * An interface representing a multivariate real function.
 *
 * @version $Revision: 811685 $ $Date: 2009-09-05 13:36:48 -0400 (Sat, 05 Sep 2009) $
 * @since 2.0
 */
public interface MultivariateRealFunction {

    /**
     * Compute the value for the function at the given point.
     *
     * @param point point at which the function must be evaluated
     * @return function value for the given point
     * @throws FunctionEvaluationException if the function evaluation fails
     * @throws IllegalArgumentException    if points dimension is wrong
     */
    double value(double[] point)
            throws FunctionEvaluationException, IllegalArgumentException;

}
