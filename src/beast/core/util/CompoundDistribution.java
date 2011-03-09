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

@Description("Takes a collection of distributions, typically a number of likelihoods " +
        "and priors and combines them into the compound of these distributions " +
        "typically interpreted as the posterior.")
public class CompoundDistribution extends Distribution {
    // no need to make this input REQUIRED. If no distribution input is
    // specified the class just returns probability 1.
    public Input<List<Distribution>> pDistributions =
            new Input<List<Distribution>>("distribution",
                    "individual probability distributions, e.g. the likelihood and prior making up a posterior",
                    new ArrayList<Distribution>()); 

    /** Distribution implementation follows **/
    @Override
    public double calculateLogP() throws Exception {
        logP = 0;
        for(Distribution dists : pDistributions.get()) {
        	if (dists.isDirtyCalculation()) {
        		logP += dists.calculateLogP();
        	} else {
        		logP += dists.getCurrentLogP();
        	}
            if (Double.isInfinite(logP) || Double.isNaN(logP)) {
            	return logP;
            }
        }
        return logP;
    }

    @Override
    public void sample(State state, Random random) {
        for(Distribution distribution : pDistributions.get()) {
            distribution.sample(state, random);
        }
    }

    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<String>();
        for(Distribution distribution : pDistributions.get()) {
            arguments.addAll(distribution.getArguments());
        }
        return arguments;
    }

    @Override
    public List<String> getConditions() {
        List<String> conditions = new ArrayList<String>();
        for(Distribution distribution : pDistributions.get()) {
            conditions.addAll(distribution.getConditions());
        }
        return conditions;
    }

    @Override
    protected boolean requiresRecalculation() {
      for(Distribution distribution : pDistributions.get()) {
          if( distribution.isDirtyCalculation() ) {
              return true;
          }
      }
      return false;   
    }
} // class CompoundProbabilityDistribution
