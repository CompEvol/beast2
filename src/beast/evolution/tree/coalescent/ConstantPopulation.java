package beast.evolution.tree.coalescent;


import java.util.Collections;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
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
@Description("coalescent intervals for a constant population")
public class ConstantPopulation extends PopulationFunction.Abstract {
    final public Input<RealParameter> popSizeParameter = new Input<>("popSize",
            "constant (effective) population size value.", Validate.REQUIRED);

    //
    // Public stuff
    //

    /**
     * @return initial population size.
     */
    public double getN0() {
        N0 = popSizeParameter.get().getValue();
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


    // Implementation of abstract methods

    @Override
	public List<String> getParameterIds() {
        return Collections.singletonList(popSizeParameter.get().getID());
    }

    @Override
	public double getPopSize(double t) {
        return getN0();
    }

    @Override
	public double getIntensity(double t) {
        return t / getN0();
    }

    @Override
	public double getInverseIntensity(double x) {
        return getN0() * x;
    }

    // same as abstract
//	/**
//	 * Calculates the integral 1/N(x) dx between start and finish. The
//	 * inherited function in DemographicFunction.Abstract calls a
//	 * numerical integrater which is unecessary.
//	 */
//	public double getIntegral(double start, double finish) {
//		return getIntensity(finish) - getIntensity(start);
//	}

    //

//    public int getNumArguments() {
//        return 1;
//    }
//
//    public String getArgumentName(int n) {
//        return "N0";
//    }
//
//    public double getArgument(int n) {
//        return getN0();
//    }
//
//    public void setArgument(int n, double value) {
//        setN0(value);
//    }
//
//    public double getLowerBound(int n) {
//        return 0.0;
//    }
//
//    public double getUpperBound(int n) {
//        return Double.POSITIVE_INFINITY;
//    }
//
//    public PopulationFunction getCopy() {
//        ConstantPopulation cp = new ConstantPopulation();
//        cp.setN0(N0);
//        return cp;
//    }

//    public void prepare(State state) {
//        if (popSizeParameter.get() != null) {
//            N0 = popSizeParameter.get().getValue();//state.getParameter(popSizeParameter).getValue();
//        }
//    }


    //
    // private stuff
    //

    private double N0 = 1.0;
}
