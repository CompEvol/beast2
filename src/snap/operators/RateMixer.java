
/*
 * File RateMixer.java
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
package snap.operators;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.Parameter;
import beast.core.State;
import beast.core.Tree;
import beast.util.Randomizer;

@Description("Moves length of branch and gamma of branch in the oposite direction.")
public class RateMixer extends Operator {

	public Input<Double> m_pScale = new Input<Double>("scaleFactors", "scaling factor: larger means more bold proposals");
	public Input<Parameter> m_pGamma = new Input<Parameter>("gamma", "population sizes");
	public Input<Tree> m_pTree = new Input<Tree>("tree", "beast.tree with phylogenetic relations");

	@Override
	public void initAndValidate(State state) {
	}

	double m_fMixGamma;
	int m_nGamma = -1;
	int m_nTreeID = -1;
//	public RateMixer(double f) {
//		m_fMixGamma = f;
//	}
	@Override
	public double proposal(State state) throws Exception {
		if (m_nGamma < 0) {
			m_nGamma = state.getParameterIndex(m_pGamma.get().getID());
			m_fMixGamma = m_pScale.get();
			m_nTreeID = state.getTreeIndex(m_pTree.get().getID());
		}
		Parameter gamma = (Parameter)state.getParameter(m_nGamma);

		double scale = Math.exp(m_fMixGamma*(2.0*Randomizer.nextDouble() - 1.0));
		state.mulValues(scale, gamma);
		//gamma.mulValues(scale);
		state.m_trees[m_nTreeID].getRoot().scale(1/scale);

		return Math.log(scale);
	}

	/** automatic parameter tuning **/
	@Override
	public void optimize(double logAlpha) {
		Double fDelta = calcDelta(logAlpha);
		fDelta += Math.log(m_fMixGamma);
		m_fMixGamma = Math.exp(fDelta);
    }
}
