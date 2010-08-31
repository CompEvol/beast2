/*
* Density.java
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

/**
 * an interface for a distribution.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: Distribution.java,v 1.7 2005/05/24 20:26:00 rambaut Exp $
 */
public interface Distribution {
    static final double SQRT_2 = Math.sqrt(2.0);
    static final double SQRT_2PI = Math.sqrt(2.0 * Math.PI);

    /**
     * probability density function of the distribution
     *
     * @param x argument
     * @return pdf value
     */
    public double pdf(double x);

    /**
     * the natural log of the probability density function of the distribution
     *
     * @param x argument
     * @return log pdf value
     */
    public double logPdf(double x);

    /**
     * cumulative density function of the distribution
     *
     * @param x argument
     * @return cdf value
     */
    public double cdf(double x);

    /**
     * quantile (inverse cumulative density function) of the distribution
     *
     * @param y argument
     * @return icdf value
     */
    public double quantile(double y);

    /**
     * mean of the distribution
     *
     * @return mean
     */
    public double mean();

    /**
     * variance of the distribution
     *
     * @return variance
     */
    public double variance();

    /**
     * @return a probability density function representing this distribution
     */
    org.apache.commons.math.distribution.Distribution getProbabilityDensity();
}
