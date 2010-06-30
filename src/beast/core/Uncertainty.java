
/*
 * File Uncertainty.java
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
package beast.core;

@Description("Probabilistic representation that can produce " +
		"a loglikelihood/log probability/log uncertainty for running an MCMC chain.")
public class Uncertainty extends Plugin {

	/** current and stored log probability/log likelihood/log uncertainty **/
	protected double m_fLogP = 0;
	private double m_fStoredLogP = 0;

	/** do the actual calculation **/
	public double calculateLogP(State state) throws Exception {
		m_fLogP = 0;
		return m_fLogP;
	}

	/** get result from last known calculation **/
	public double getCurrentLogP() {
		return m_fLogP;
	}

	@Override
	public void initAndValidate(State state) throws Exception {
		// nothing to do
	}

	@Override
	public void store(int nSample) {
		super.store(nSample);
		m_fStoredLogP = m_fLogP;
	}
	@Override
	public void restore(int nSample) {
		super.restore(nSample);
		m_fLogP = m_fStoredLogP;
	}

} // class Uncertainty
