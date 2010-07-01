package beast.evolution.tree.coalescent;

import beast.core.Input;
import beast.core.Parameter;
import beast.core.State;

import java.util.Collections;
import java.util.List;
/*
 * ConstantPopulation.java
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

/**
 * This class models coalescent intervals for a constant population
 * (parameter: N0=present-day population size). <BR>
 * If time units are set to Units.EXPECTED_SUBSTITUTIONS then
 * the N0 parameter will be interpreted as N0 * mu. <BR>
 * Also note that if you are dealing with a diploid population
 * N0 will be out by a factor of 2.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: ConstantPopulation.java,v 1.9 2005/05/24 20:25:55 rambaut Exp $
 */
public class ExponentialGrowth extends PopulationFunction.Abstract {
    public Input<Parameter> popSizeParameter = new Input<Parameter>("popSize", "the current popSize parameter; defaults to 1.0)");
    public Input<Parameter> growthRateParameter = new Input<Parameter>("growthRate", "popSize parameter; defaults to 0.01)");

    //
    // Public stuff
    //

    public void initAndValidate(State state) throws Exception {
        Parameter popParameter = state.getParameter(popSizeParameter);
        N0 = popParameter.getValue();

        Parameter growthParameter = state.getParameter(growthRateParameter);
        r = growthParameter.getValue();
    }

    public void setState(State state) {
        super.setState(state);
        N0 = state.getParameter(popSizeParameter).getValue();
        r = state.getParameter(growthRateParameter).getValue();
    }

    /**
     * @return initial population size.
     */
    public double getN0() {
        return N0;
    }

    /**
     * sets initial population size.
     *
     * @param N0 new size
     */
    public void setN0(double N0) {
        this.N0 = N0;
    }

    /**
     * @return growth rate.
     */
    public final double getGrowthRate() {
        return r;
    }

    /**
     * sets growth rate to r.
     *
     * @param r
     */
    public void setGrowthRate(double r) {
        this.r = r;
    }

    /**
     * An alternative parameterization of this model. This
     * function sets growth rate for a given doubling time.
     *
     * @param doublingTime
     */
    public void setDoublingTime(double doublingTime) {
        setGrowthRate(Math.log(2) / doublingTime);
    }

    // Implementation of abstract methods

    public double getPopSize(double t) {

        double r = getGrowthRate();
        if (r == 0) {
            return getN0();
        } else {
            return getN0() * Math.exp(-t * r);
        }
    }

    /**
     * Calculates the integral 1/N(x) dx between start and finish.
     */
    @Override
    public double getIntegral(double start, double finish) {
        double r = getGrowthRate();
        if (r == 0.0) {
            return (finish - start) / getN0();
        } else {
            return (Math.exp(finish * r) - Math.exp(start * r)) / getN0() / r;
        }
    }

    public double getIntensity(double t) {
        double r = getGrowthRate();
        if (r == 0.0) {
            return t / getN0();
        } else {
            return (Math.exp(t * r) - 1.0) / getN0() / r;
        }
    }

    public double getInverseIntensity(double x) {

        double r = getGrowthRate();
        if (r == 0.0) {
            return getN0() * x;
        } else {
            return Math.log(1.0 + getN0() * x * r) / r;
        }
    }


    // Implementation of abstract methods

    public List<String> getParameterIds() {
        return Collections.singletonList(popSizeParameter.get().getID());
    }

    public int getNumArguments() {
        return 2;
    }

    public String getArgumentName(int n) {
        if (n == 0) return "N0";
        else return "r";
    }

    public double getArgument(int n) {
        if (n == 0) return getN0();
        else return r;
    }

    public void setArgument(int n, double value) {
        if (n == 0) setN0(value);
        else r = value;
    }

    public double getLowerBound(int n) {
        if (n == 0) return 0.0;
        else return Double.NEGATIVE_INFINITY;
    }

    public double getUpperBound(int n) {
        return Double.POSITIVE_INFINITY;
    }

    public PopulationFunction getCopy() {
        ExponentialGrowth eg = new ExponentialGrowth();
        eg.setN0(N0);
        eg.r = r;
        return eg;
    }

    //
    // private stuff
    //

    /**
     * The current day population size
     */
    private double N0;

    /**
     * The exponential growth rate
     */
    private double r;
}