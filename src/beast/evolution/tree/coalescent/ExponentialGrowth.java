package beast.evolution.tree.coalescent;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;

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
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: ConstantPopulation.java,v 1.9 2005/05/24 20:25:55 rambaut Exp $
 */
@Description("Coalescent intervals for a exponentially growing population.")
public class ExponentialGrowth extends PopulationFunction.Abstract {
    public Input<RealParameter> popSizeParameter = new Input<RealParameter>("popSize",
    		"present-day population size (defaults to 1.0). ");
    public Input<RealParameter> growthRateParameter = new Input<RealParameter>("growthRate", 
    		"growth rate is the exponent of the exponential growth");

    //
    // Public stuff
    //

    public void initAndValidate() throws Exception {
        if (popSizeParameter.get() != null) {
            popSizeParameter.get().setBounds(0.0, Double.POSITIVE_INFINITY);
        }
        if (growthRateParameter.get() != null) {
            growthRateParameter.get().setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        }
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

//    public void prepare(State state) {
//        if (popSizeParameter.get() != null) {
//            N0 = popSizeParameter.get().getValue();//state.getParameter(popSizeParameter).getValue();
//        }
//        if (growthRateParameter.get() != null) {
//            r = growthRateParameter.get().getValue();//state.getParameter(growthRateParameter).getValue();
//        }
//    }

    //
    // private stuff
    //

    /**
     * The current day population size
     */
    private double N0 = 1.0;

    /**
     * The exponential growth rate
     */
    private double r = 0.01;
}