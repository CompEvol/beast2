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
import beast.core.ProbabilityDistribution;
import beast.core.State;

import java.util.ArrayList;
import java.util.List;

@Description("Takes a collection of uncertainties, typically a number of likelihoods " +
        "and priors and combines them into the compound of these uncertainties " +
        "typically interpreted as the posterior.")
public class CompoundProbabilityDistribution extends ProbabilityDistribution {
    public Input<List<ProbabilityDistribution>> m_uncertainties = new Input<List<ProbabilityDistribution>>("uncertainty", "individual uncertainties, e.g. making up a posterior", new ArrayList<ProbabilityDistribution>());

    @Override
    public double calculateLogP(State state) throws Exception {
        m_fLogP = 0;
        for (int i = 0; i < m_uncertainties.get().size(); i++) {
            double f = m_uncertainties.get().get(i).calculateLogP(state);
            m_fLogP += f;
        }
        return m_fLogP;
    }

    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<String>();
        for (int i = 0; i < m_uncertainties.get().size(); i++) {
            arguments.addAll(m_uncertainties.get().get(i).getArguments());
        }
        return arguments;
    }

    @Override
    public List<String> getConditions() {
        List<String> conditions = new ArrayList<String>();
        for (int i = 0; i < m_uncertainties.get().size(); i++) {
            conditions.addAll(m_uncertainties.get().get(i).getConditions());
        }
        return conditions;
    }

    @Override
    public void restore(int nSample) {
        for (ProbabilityDistribution likelihood : m_uncertainties.get()) {
            likelihood.restore(nSample);
        }
    }

    @Override
    public void store(int nSample) {
        for (ProbabilityDistribution likelihood : m_uncertainties.get()) {
            likelihood.store(nSample);
        }
    }

} // class CompoundProbabilityDistribution
