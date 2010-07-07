/*
* File CompoundProbabilityDistribution.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is part of BEAST2.
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
package beast.core.util;

import beast.core.Description;
import beast.core.Input;
import beast.core.Distribution;
import beast.core.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Description("Takes a collection of uncertainties, typically a number of likelihoods " +
        "and priors and combines them into the compound of these uncertainties " +
        "typically interpreted as the posterior.")
public class CompoundDistribution extends Distribution {
    public Input<List<Distribution>> pDistributions = new Input<List<Distribution>>("distribution", "individual probability distributions, e.g. the likelihood and prior making up a posterior", new ArrayList<Distribution>());

    @Override
    public double calculateLogP(State state) throws Exception {
        logP = 0;
        for (int i = 0; i < pDistributions.get().size(); i++) {
            double f = pDistributions.get().get(i).calculateLogP(state);
            logP += f;
        }
        return logP;
    }

    @Override
    public void sample(State state, Random random) {

        for (int i = 0; i < pDistributions.get().size(); i++) {
            pDistributions.get().get(i).sample(state, random);
        }
    }

    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<String>();
        for (int i = 0; i < pDistributions.get().size(); i++) {
            arguments.addAll(pDistributions.get().get(i).getArguments());
        }
        return arguments;
    }

    @Override
    public List<String> getConditions() {
        List<String> conditions = new ArrayList<String>();
        for (int i = 0; i < pDistributions.get().size(); i++) {
            conditions.addAll(pDistributions.get().get(i).getConditions());
        }
        return conditions;
    }

//    @Override
//    public void restore(int nSample) {
//        super.restore(nSample);
//        for (Distribution likelihood : pDistributions.get()) {
//            likelihood.restore(nSample);
//        }
//    }
//
//    @Override
//    public void store(int nSample) {
//        super.store(nSample);
//        for (Distribution likelihood : pDistributions.get()) {
//            likelihood.store(nSample);
//        }
//    }
//
//    @Override
//    public void prepare(State state) {
//        super.prepare(state);
//        for (Distribution likelihood : pDistributions.get()) {
//            likelihood.prepare(state);
//        }
//    }
} // class CompoundProbabilityDistribution
