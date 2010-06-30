
/*
 * File NodeBudger.java
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
import beast.core.State;
import beast.core.Node;
import beast.util.Randomizer;

@Description("Moves internal node height without changing the beast.tree topology. " +
		"So the range is limited by the height of the parent node and the height " +
		"of the highest child.")
public class NodeBudger extends NodeSwapper {

	public Input<Double> m_pWindowSize = new Input<Double>("size", "Relative size of the window in which to move the node");
	//public Input<Tree> m_pTree = new Input<Tree>("beast.tree", "beast.tree with phylogenetic relations");
	double m_fWindowSize;
	int m_nNodeCount = -1;
//	public NodeBudgerOperator(double w) {
//		m_fWindowSize = w;
//	}

	@Override
	public void initAndValidate(State state) {
		m_nTreeID = state.getTreeIndex(m_pTree.get().getID());
		m_nNodeCount = state.m_trees[m_nTreeID].getNodeCount();
		m_fWindowSize = m_pWindowSize.get();
	}

	int m_nTreeID = -1;

	@Override
	public double proposal(State state) throws Exception {
		double hastingsRatio = 1.0;
		Node [] nodes = new Node[m_nNodeCount];
		registerNodes(nodes, state.m_trees[m_nTreeID].getRoot());

		//Choose a random node internal node
		int whichNode = m_nNodeCount/2 + 1 + Randomizer.nextInt(m_nNodeCount/2 - 1);
		Node p = nodes[whichNode];

		if (p.isRoot()){
			// RRB: budging the root node leads to very long calculation times
			// so we reject its move. The root time can still be changed through
			// the scale operator, so the root time is not necessarily fixed.
			return Double.NEGATIVE_INFINITY;
		}



		//Find shortest branch to any child.
		double minb = 10e10;
		minb = Math.max(p.m_left.getHeight(), p.m_right.getHeight());

		double range = p.getParent().getHeight() - minb;

		double move = minb + m_fWindowSize * Randomizer.nextDouble()*range;


		//if (move<0 && (-move)>minb)
		//	return false; //Moves too far down.
		//if (!p.root() && move>0 && move>p->length)
		//	return false; //if not root---check not moving too far up

		p.setHeight(move);
		//p.m_fLength -= move;
		//p.m_left.m_fLength += move;
		//p.m_right.m_fLength += move;

		// TODO Auto-generated method stub
		return Math.log(hastingsRatio);
	}

	/** automatic parameter tuning **/
	@Override
	public void optimize(double logAlpha) {
		Double fDelta = calcDelta(logAlpha);
		fDelta += Math.log(m_fWindowSize);
		m_fWindowSize = Math.exp(fDelta);
    }
}
