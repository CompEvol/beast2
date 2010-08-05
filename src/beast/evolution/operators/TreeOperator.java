
/*
 * File TreeOperator.java
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
package beast.evolution.operators;


import beast.core.Description;

import beast.core.Input;
import beast.evolution.tree.Node;
import beast.core.Operator;
import beast.evolution.tree.Tree;
import beast.core.Input.Validate;

@Description("This operator changes a beast.tree.")
abstract public class TreeOperator extends Operator {
	public Input<Tree> m_tree = new Input<Tree>("tree","beast.tree on which this operation is performed", Validate.REQUIRED);


	/**
	 * @param parent the parent
	 * @param child  the child that you want the sister of
	 * @return the other child of the given parent.
	 */
    protected Node getOtherChild(Node parent, Node child) {
    	if (parent.m_left.getNr() == child.getNr()) {
    		return parent.m_right;
    	} else {
    		return parent.m_left;
    	}
    }


//    double [] m_fHeights;
//	public void calculateHeightsFromLengths(Tree beast.tree) {
//		m_fHeights = new double[beast.tree.getNodeCount()];
//		Node root = beast.tree.getRoot();
//		calculateHeightsFromLengths(root);
//	} // calculateHeightsFromLengths
//
//	void calculateHeightsFromLengths(Node node) {
//		Node parent = node.getParent();
//		if (m_fHeights[node.getNr()] != 0) {
//			System.err.println("heights double properly initialized");
//		}
//		if (node.getLength() < 0) {
//			System.err.println("negative length");
//		}
//		if (parent != null) {
//			m_fHeights[node.getNr()] = m_fHeights[parent.getNr()] - node.m_fLength;
//		}
//		if (!node.isLeaf()) {
//			calculateHeightsFromLengths(node.m_left);
//			calculateHeightsFromLengths(node.m_right);
//		}
//	} // calculateHeightsFromLengths

//	public void setNodeHeight(Node node, double fHeight) throws Exception {
//		m_fHeights[node.getNr()] = fHeight;
////		double fOldHeight = m_fHeights[node.getNr()];
////		double fDeltaLength = fHeight - fOldHeight;
////		node.addLength(-fDeltaLength);
////		if (!node.isLeaf()) {
////			node.m_left.addLength(fDeltaLength);
////			node.m_right.addLength(fDeltaLength);
////		}
////		m_fHeights[node.getNr()] += fDeltaLength;
//	} // setNodeHeight

	/** replace child with another node
     * @param node
     * @param child
     * @param replacement
     **/
	public void replace(Node node, Node child, Node replacement) {
		if (node.m_left.getNr() == child.getNr()) {
			node.m_left = replacement;
		} else {
			// it must be the right child
			node.m_right = replacement;
		}
		//child.setParent(null);
		node.makeDirty(Tree.IS_FILTHY);
		replacement.setParent(node);
		replacement.makeDirty(Tree.IS_FILTHY);
//		replacement.setLength(m_fHeights[node.getNr()] - m_fHeights[replacement.getNr()]);
	}

//	void setLengthsFromHeights(Node node) {
//		if (node.isRoot()) {
//			node.m_fLength = 0;
//		} else {
//			double fLength = m_fHeights[node.getParent().getNr()] - m_fHeights[node.getNr()];
//			if (Math.abs(node.m_fLength - fLength) > 1e-100) {
//				node.setLength(fLength);
//				if (fLength < 0) {
//					int h = 3;
//					h++;
//				}
//			}
//		}
//		if (!node.isLeaf()) {
//			setLengthsFromHeights(node.m_left);
//			setLengthsFromHeights(node.m_right);
//		}
//	} // setLengthFromHeights
//
//	double height(Node node) {
//		return m_fHeights[node.getNr()];
//	}
}
