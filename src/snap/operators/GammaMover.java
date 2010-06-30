
/*
 * File GammaMover.java
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
import beast.util.Randomizer;

@Description("Scales single value in gamma parameter.")
public class GammaMover extends Operator {
	public Input<Parameter> m_pGamma = new Input<Parameter>("gamma", "population sizes");
	public Input<Double> m_pScaleGamma = new Input<Double>("pGammaMove", "scale of move");

	double m_fScale;
	int m_nGamma = -1;

	@Override
	public void initAndValidate(State state) {
		m_nGamma = state.getParameterIndex(m_pGamma.get().getID());
		m_fScale = m_pScaleGamma.get();
	}


	@Override
	public double proposal(State state) throws Exception {
		Parameter gamma = (Parameter) state.getParameter(m_nGamma);
		int whichNode = Randomizer.nextInt(gamma.getDimension());

		double scale = Math.exp(m_fScale*(2.0*Randomizer.nextDouble() - 1.0));
		state.mulValue(whichNode, (1.0/scale), gamma);
		//gamma.mulValue(whichNode, (1.0/scale));
		return Math.log(scale);
	}

	/** automatic parameter tuning **/
	@Override
	public void optimize(double logAlpha) {
		Double fDelta = calcDelta(logAlpha);
		fDelta += Math.log(m_fScale);
		m_fScale = Math.exp(fDelta);
    }
}
