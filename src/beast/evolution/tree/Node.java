
/*
 * File Node.java
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
package beast.evolution.tree;

import beast.core.*;

import java.util.List;

@Description("Nodes in building binary beast.tree data structure.")
public class Node extends Plugin {
	/** label nr of node, only used when this is a leaf **/
	protected int m_iLabel;
	/** height of this node. */
	protected double m_fHeight = Double.MAX_VALUE;
	/** list of children of this node **/
	public Node m_left;
	public Node m_right;
	/** parent node in the beast.tree, null if root **/
	Node m_Parent = null;
	/** status of this node after an operation is performed on the state **/
	int m_bIsDirty = Tree.IS_CLEAN;
	/** meta-data contained in square brackets in Newick **/
	public String m_sMetaData;

	public int getNr() {return m_iLabel;}
	public void setNr(int iLabel) {m_iLabel = iLabel;}

	public double getHeight() {return m_fHeight;}
	public void setHeight(double fHeight) {
		m_fHeight = fHeight;
		m_bIsDirty |= Tree.IS_DIRTY;
		if (!isLeaf()) {
			m_left.m_bIsDirty |= Tree.IS_DIRTY;
			m_right.m_bIsDirty |= Tree.IS_DIRTY;
		}
	}

	/** @return length of branch in the beast.tree **/
	public double getLength() {
		if (isRoot()) {
			return 0;
		} else {
			return getParent().m_fHeight - m_fHeight;
		}
	}

	public int isDirty() {return m_bIsDirty;}
	public void makeDirty(int nDirty) {
		m_bIsDirty |= nDirty;
	}
	public void makeAllDirty(int nDirty) {
		m_bIsDirty = nDirty;
		if (!isLeaf()) {
			m_left.makeAllDirty(nDirty);
			m_right.makeAllDirty(nDirty);
		}
	}


	/** @return parent node, or null if this is root **/
	public Node getParent() {
		return m_Parent;
	}
	public void setParent(Node parent) {
		m_Parent = parent;
	}

	/** @return true if current node is root node **/
	public boolean isRoot() {
		return m_Parent == null;
	}

	/** @return  true if current node is a leaf node **/
	public boolean isLeaf() {
		return m_left == null;
	}

	/** @return count number of nodes in beast.tree, starting with current node **/
	public int getNodeCount() {
		if (isLeaf()) {
			return 1;
		}
		return 1 + m_left.getNodeCount() + m_right.getNodeCount();
	}

	 /**
	 * @return beast.tree in Newick format, without any length or meta data
	 * information.
	 **/
	public String toShortNewick() {
		StringBuffer buf = new StringBuffer();
		if (m_left != null) {
			buf.append("(");
			buf.append(m_left.toShortNewick());
			buf.append(',');
			buf.append(m_right.toShortNewick());
			buf.append(")");
		} else {
//			if (m_sID != null) {
//				buf.append(m_sID);
//			} else {
				buf.append(m_iLabel);
//			}
		}
//		buf.append("["+m_iLabel+"]");
         buf.append(":").append(String.format("%3.3f", getLength()));
//		for (int i = 0; i < m_bIsDirty; i++) {
//			buf.append("X");
//		}
		return buf.toString();
	}

	/**
	 * @param sLabels
     * @return  beast.tree in long Newick format, with all length and meta data
	 * information
	 **/
	public String toNewick(List<String> sLabels) {
		StringBuffer buf = new StringBuffer();
		if (m_left != null) {
			buf.append("(");
			buf.append(m_left.toNewick(sLabels));
			buf.append(',');
			buf.append(m_right.toNewick(sLabels));
			buf.append(")");
		} else {
			if (sLabels == null) {
				buf.append(m_iLabel);
			} else {
				buf.append(sLabels.get(m_iLabel));
			}
		}
		buf.append(getNewickMetaData());
        buf.append(":").append(getLength());
		return buf.toString();
	}

	public String getNewickMetaData() {
		if (m_sMetaData != null) {
			return '[' + m_sMetaData + ']';
		}
		return "";
	}

	/**
	 * @param sLabels
     * @return beast.tree in long Newick format, with all length and meta data
	 * information, but with leafs labelled with their names
	 **/
	public String toString(List<String> sLabels) {
		StringBuffer buf = new StringBuffer();
		if (m_left != null) {
			buf.append("(");
			buf.append(m_left.toString(sLabels));
			buf.append(',');
			buf.append(m_right.toString(sLabels));
			buf.append(")");
		} else {
			buf.append(sLabels.get(m_iLabel));
		}
		if (m_sMetaData != null) {
			buf.append('[');
			buf.append(m_sMetaData);
			buf.append(']');
		}
        buf.append(":").append(getLength());
		return buf.toString();
	}

	public String toString() {
		return toShortNewick();
	}
	
	/**
	 * sorts nodes in children according to lowest numbered label in subtree
	 *
     * @return
     **/
	public int sort() {
		if (m_left != null) {
			int iChild1 = m_left.sort();
			int iChild2 = m_right.sort();
			if (iChild1 > iChild2) {
				Node tmp = m_left;
				m_left = m_right;
				m_right = tmp;
				return iChild2;
			}
			return iChild1;
		}
		// this is a leaf node, just return the label nr
		return m_iLabel;
	} // sort

	/** during parsing, leaf nodes are numbered 0...m_nNrOfLabels-1
	 * but internal nodes are left to zero. After labeling internal
	 * nodes, m_iLabel uniquely identifies a node in a beast.tree.
     * @param iLabel
     * @return
     */
	public int labelInternalNodes(int iLabel) {
		if (isLeaf()) {
			return iLabel;
		} else {
			iLabel = m_left.labelInternalNodes(iLabel);
			iLabel = m_right.labelInternalNodes(iLabel);
			m_iLabel = iLabel++;
		}
		return iLabel;
	} // labelInternalNodes

	/**
     * @return (deep) copy of node
     **/
	public Node copy() {
		Node node = new Node();
		node.m_fHeight = m_fHeight;
		node.m_iLabel = m_iLabel;
		node.m_sMetaData = m_sMetaData;
		node.m_Parent = null;
		node.m_sID = m_sID;
		if (m_left != null) {
			node.m_left = m_left.copy();
			node.m_right = m_right.copy();
			node.m_left.m_Parent = node;
			node.m_right.m_Parent = node;
		}
		return node;
	} // copy

	public void assignTo(Node [] nodes) {
		Node node = nodes[getNr()];
		node.m_fHeight = m_fHeight;
		node.m_iLabel = m_iLabel;
		node.m_sMetaData = m_sMetaData;
		node.m_Parent = null;
		node.m_sID = m_sID;
		if (m_left != null) {
			node.m_left = nodes[m_left.getNr()];
			m_left.assignTo(nodes);
			node.m_right = nodes[m_right.getNr()];
			m_right.assignTo(nodes);
			node.m_left.m_Parent = node;
			node.m_right.m_Parent = node;
		}
	}
	
	public void assignFrom(Node [] nodes) {
		Node node = nodes[getNr()];
		m_fHeight = node.m_fHeight;
		m_iLabel = node.m_iLabel;
		m_sMetaData = node.m_sMetaData;
		m_Parent = null;
		m_sID = node.m_sID;
		if (node.m_left != null) {
			m_left = nodes[node.m_left.getNr()];
			m_left.assignFrom(nodes);
			m_right = nodes[node.m_right.getNr()];
			m_right.assignFrom(nodes);
			m_left.m_Parent = node;
			m_right.m_Parent = node;
		}
	}
	
	public void setMetaData(String sPattern, Object fValue) {
		if (sPattern.equals(TraitSet.DATE_TRAIT) || 
				sPattern.equals(TraitSet.DATE_FORWARD_TRAIT) || 
				sPattern.equals(TraitSet.DATE_BACKWARD_TRAIT)) {
			m_fHeight = (Double) fValue;
		}
		m_bIsDirty |= Tree.IS_DIRTY;
	}
	
	public double getMetaData(String sPattern) {
		return 0;
	}

	/** scale height of this node and all its descendants
     * @param fScale scale factor
     **/
	public void scale(double fScale) throws Exception {
		m_bIsDirty |= Tree.IS_DIRTY;
		if (!isLeaf()) {
			m_fHeight *= fScale;
			m_left.scale(fScale);
			m_right.scale(fScale);
			if (m_fHeight < m_left.m_fHeight || m_fHeight < m_right.m_fHeight) {
				throw new Exception("Scale gives negative branch length");
			}
		}
	}

	@Override
	public void initAndValidate() throws Exception {
		// do nothing
	}

} // class Node
