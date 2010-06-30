
/*
 * File SnAPPrior.java
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
package snap.likelihood;




import beast.core.Description;
import beast.core.Input;
import beast.core.Uncertainty;
import beast.core.Parameter;
import beast.core.State;
import beast.core.Tree;
import beast.core.Node;

@Description("Standard prior for SnAP analysis, consisting of a Yule prior on the beast.tree " +
		"(parameterized by lambda) " +
		"and gamma distribution over the theta values " +
		"(with parameters alpha and beta). " +
		"Thetas are represented by the gamma parameter where values are theta=2/gamma")
public class SnAPPrior extends Uncertainty {
//	public Input<Parameter> m_pU = new Input<Parameter>("mutationRateU", "mutation rate from red to green?");
//	public Input<Parameter> m_pV = new Input<Parameter>("mutationRateV", "mutation rate from green to red?");
	public Input<Parameter> m_pAlpha = new Input<Parameter>("alpha", "prior parameter -- see docs for details");
	public Input<Parameter> m_pBeta = new Input<Parameter>("beta", "prior parameter -- see docs for details");
	public Input<Parameter> m_pLambda = new Input<Parameter>("lambda", "parameter for Yule birth process");
	public Input<Parameter> m_pGamma = new Input<Parameter>("gamma", "Populations sizes for the nodes in the beast.tree");
	public Input<Tree> m_pTree = new Input<Tree>("tree", "beast.tree with phylogenetic relations");

	public SnAPPrior() {
	}

	int m_nTreeID = -1;
	public void initAndValidate(State state) {
		if (m_nTreeID < 0) {
			m_nTreeID = state.getTreeIndex(m_pTree.get().getID());
		}
	}


	@Override
	public double calculateLogP(State state) throws Exception {
		double logP = 0.0;

		//Mutation rates
		//	uniform pi_0 on [0,1]. Rate equals one.
//		if (state.getValue(m_pU) < 0.0 || state.getValue(m_pV) < 0.0 )
//			return Double.NEGATIVE_INFINITY; //zero prior probability.


		//branch lengths / beast.tree height
		//Assume a yule prior with given birthrate lambda.
		//computeHeights(state.beast.tree);
		//state.m_trees[m_nTreeID].calculateHeightsFromLengths();
		double heightsum = state.m_trees[m_nTreeID].getRoot().getHeight();
		heightsum += heightSum(state.m_trees[m_nTreeID].getRoot());
		int nspecies = state.m_trees[m_nTreeID].getNodeCount()/2+1;

		double lambda = state.getValue(m_pLambda);
		double alpha = state.getValue(m_pAlpha);
		double beta = state.getValue(m_pBeta);
		logP += (nspecies-1)*Math.log(lambda) - lambda*heightsum;

		//Gamma values in beast.tree
		Parameter gamma = state.getParameter(m_pGamma);
		//double [] gamma = state.getParameter(m_pGamma).getValues();
		//	We assume that theta has a gamma (alpha,beta) distribution, so that
		//the gamma parameter has 2/gamma(alpha,beta) distribution
		for (int iNode = 0; iNode < gamma.getDimension(); iNode++) {
//			if (gamma[iNode] < 5.0) {
//				return Double.NEGATIVE_INFINITY;
//			}
			double x = 2.0/gamma.getValue(iNode);
			logP += (alpha - 1.0)*Math.log(x) - (beta * x);
		}
		m_fLogP = logP;
		return logP;
	} // calculateLogLikelihood

	double heightSum(Node node) throws Exception {
		if (node.isLeaf()) {
			return 0;
		} else {
			double h = node.getHeight();
			h +=
				heightSum(node.m_left) +
				heightSum(node.m_right);
			return h;
		}
	} // heightSum


} // class SSSPrior
