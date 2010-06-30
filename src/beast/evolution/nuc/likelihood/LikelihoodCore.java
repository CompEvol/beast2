
/*
 * File LikelihoodCore.java
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
package beast.evolution.nuc.likelihood;

abstract public class LikelihoodCore {
	boolean m_bUseScaling = false;

	abstract public void initialize(int nNodeCount, int nPatternCount, int nMatrixCount, boolean bIntegrateCategories);
	abstract public void finalize() throws java.lang.Throwable;

	abstract public void integratePartials(int iNode, double[] fProportions, double[] fOutPartials);
	abstract public void calculateLogLikelihoods(double[] fPartials, double[] fFrequencies, double[] fOutLogLikelihoods);

	abstract public void setUseScaling(boolean bUseScaling);
	public boolean getUseScaling() {return m_bUseScaling;}
	abstract public void createNodePartials(int iNode);
	abstract public void setNodePartials(int iNode, double[] fPartials);
	abstract public void createNodeStates(int iNode);
	abstract public void setNodeStates(int iNode, int[] iStates);
	abstract public void setNodeMatrixForUpdate(int iNode);
	abstract public void setNodeMatrix(int iNode, int iMatrixIndex, double[] fMatrix);
    abstract public void setNodePartialsForUpdate(int iNode);
    public void setNodeStatesForUpdate(int iNode) {};
    abstract public void setCurrentNodePartials(int iNode, double[] fPartials);
    abstract public void calculatePartials(int iNode1, int iNode2, int iNode3);
    abstract public void calculatePartials(int iNode1, int iNode2, int iNode3, int[] iMatrixMap);
    abstract public double getLogScalingFactor(int iPattern);
    abstract public void store();
    abstract public void restore();
}
