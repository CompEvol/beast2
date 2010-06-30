
/*
 * File NodeSwapper.java
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


import java.util.Vector;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.State;
import beast.core.Tree;
import beast.core.Node;
import beast.util.Randomizer;

@Description("Randomly selects two nodes and swap them in the beast.tree.")
public class NodeSwapper extends Operator {
	public Input<Tree> m_pTree = new Input<Tree>("tree", "beast.tree with phylogenetic relations");

	int m_nNodeCount = -1;
	int m_nTreeID = -1;

	@Override
	public void initAndValidate(State state) {
	}

	void registerNodes(Node [] nodes, Node node) {
		nodes[node.getNr()] = node;
		if (!node.isLeaf()) {
			registerNodes(nodes, node.m_left);
			registerNodes(nodes, node.m_right);
		}
	}
	@Override
	public double proposal(State state) throws Exception {
		double hastingsRatio = 1.0;
		if (m_nNodeCount < 0) {
			m_nTreeID = state.getTreeIndex(m_pTree.get().getID());
			m_nNodeCount = state.m_trees[m_nTreeID].getNodeCount();
		}
		Node [] nodes = new Node[m_nNodeCount];
		registerNodes(nodes, state.m_trees[m_nTreeID].getRoot());

		//First select a triple (x,y,m) where x and y are a random pair of leaves and m is the mrca of x and y.


		//choose two random leaves.
		int ntax = m_nNodeCount/2+1;
		int xid = Randomizer.nextInt(ntax);
		int yid = Randomizer.nextInt(ntax);

		Node x = nodes[xid];
		Node y = nodes[yid];

		//Check that x and y have different parents
		if (x.getParent().getNr() == y.getParent().getNr())
			return Double.NEGATIVE_INFINITY; //No move.

		//Find the MRCA
		Vector<Node> ancerstorsx = new Vector<Node>();
		Node xa = x;
		while (!xa.isRoot()) {
			ancerstorsx.add(xa);
			xa = xa.getParent();
		}
		ancerstorsx.add(xa);
		Vector<Node> ancerstorsy = new Vector<Node>();
		Node ya = y;
		while (!ya.isRoot()) {
			ancerstorsy.add(ya);
			ya = ya.getParent();
		}
		ancerstorsy.add(ya);
		Node mrca = null;
		int nSizeX = ancerstorsx.size();
		int nSizeY = ancerstorsy.size();
		int i = 0;
		mrca = ancerstorsx.elementAt(nSizeX - 1);
		while (ancerstorsx.elementAt(nSizeX - i - 1).getNr() == ancerstorsy.elementAt(nSizeY - i - 1).getNr() && i < nSizeX && i < nSizeY) {
			mrca = ancerstorsx.elementAt(nSizeX - i - 1);
			i++;
		}

//		throw new Exception("nspecies field is increasing up the beast.tree" is no longer true!!! FIX THIS);
//		Node xa=x, ya=y;
//		double mheight = 0.0; //length of path up to mrca
//		while(xa.getNr() != ya.getNr()) {
//			if (xa.getNr() <= ya.getNr()) {
//				mheight += xa.getLength();
//				xa=xa.getParent();
//			} else {
//				ya = ya.getParent();
//			}
//		}
//		Node mrca = xa;

		double mheight = mrca.getHeight() - x.getHeight(); //length of path up to mrca


		//Find a cut-off. No effect if choose a height between mrca and its children
		double minb = 10e10;
		minb = Math.min(mrca.m_left.getLength(), mrca.m_right.getLength());
		double height = Randomizer.nextDouble()*(mheight-minb);

		//Want to cut paths from x and y to MRCA at this height.
		xa = x;
		double xheight = 0.0;
		while(xheight+(xa.getLength())<height) {
			xheight+=(xa.getLength());
			xa = xa.getParent();
		}

		ya = y;
		double yheight = 0.0;
		while(yheight+(ya.getLength())<height) {
			yheight+=ya.getLength();
			ya = ya.getParent();
		}

		//Swap
		double xlength = ya.getLength() + (yheight - xheight);
		double ylength = xa.getLength() + (xheight - yheight);
		//double xgamma, ygamma;

		swap(nodes, xa, ya);

		//xa.m_fLength = (float) xlength;
		xa.setHeight(xa.getParent().getHeight() - xlength);
		//ya.m_fLength = (float) ylength;
		ya.setHeight(ya.getParent().getHeight() - ylength);
		return Math.log(hastingsRatio);
	}

	void swap(Node [] nodes, Node x, Node y) {
		int ix = x.getNr();
		int iy = y.getNr();
		int ixp = x.getParent().getNr();
		int iyp = y.getParent().getNr();
		if (nodes[ixp].m_left.getNr() == ix) {
			nodes[ixp].m_left = y;
		} else {
			nodes[ixp].m_right = y;
		}
		if (nodes[iyp].m_left.getNr() == iy) {
			nodes[iyp].m_left = x;
		} else {
			nodes[iyp].m_right = x;
		}
		y.setParent(nodes[ixp]);
		x.setParent(nodes[iyp]);
	}

}
