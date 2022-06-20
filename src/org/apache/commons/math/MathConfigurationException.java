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

import java.io.Serializable;

/**
 * Signals a configuration problem with any of the factory methods.
 *
 * @version $Revision: 811685 $ $Date: 2009-09-05 13:36:48 -0400 (Sat, 05 Sep 2009) $
 */
public class MathConfigurationException extends MathException implements Serializable {

    /**
     * Serializable version identifier
     */
    private static final long serialVersionUID = 5261476508226103366L;

    /**
     * Default constructor.
     */
    public MathConfigurationException() {
        super();
    }

    /**
     * Constructs an exception with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     *
     * @param pattern   format specifier
     * @param arguments format arguments
     * @since 1.2
     */
    public MathConfigurationException(String pattern, Object... arguments) {
        super(pattern, arguments);
    }

    /**
     * Create an exception with a given root cause.
     *
     * @param cause the exception or error that caused this exception to be thrown
     */
    public MathConfigurationException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an exception with specified formatted detail message and root cause.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     *
     * @param cause     the exception or error that caused this exception to be thrown
     * @param pattern   format specifier
     * @param arguments format arguments
     * @since 1.2
     */
    public MathConfigurationException(Throwable cause, String pattern, Object... arguments) {
        super(cause, pattern, arguments);
    }

}
