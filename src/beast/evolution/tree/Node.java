
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

import java.util.ArrayList;
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
	
	/** The Tree that this node is a part of.
	 * This allows e.g. access to the State containing the Tree **/ 
	protected Tree m_tree;
	public Tree getTree() {
		return m_tree;
	}

	@Override
	public void initAndValidate() throws Exception {
		// do nothing
	}

	/** number uniquely identifying the node in the tree.
	 * This is a number between 0 and the total number of nodes in the tree
	 * Leaf nodes are number 0 to #leaf nodes -1
	 * Internal nodes are numbered  #leaf nodes  up to #nodes-1
	 * The root node is not guaranteed a number. **/
	public int getNr() {return m_iLabel;}
	public void setNr(int iLabel) {m_iLabel = iLabel;}

	public double getHeight() {return m_fHeight;}

	public double getDate() {
		return m_tree.getDate(m_fHeight);
	}

	public void setHeight(double fHeight) {
		startEditing();
		m_fHeight = fHeight;
		m_bIsDirty |= Tree.IS_DIRTY;
		if (!isLeaf()) {
			m_left.m_bIsDirty |= Tree.IS_DIRTY;
			if (m_right != null) {
				m_right.m_bIsDirty |= Tree.IS_DIRTY;
			}
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

	/** methods for accessing the dirtiness state of the Node.
	 * A Node is Tree.IS_DIRTY if its value (like height) has changed
	 * A Node Tree.IS_if FILTHY if its parent or child has changed.
	 * Otherwise the node is Tree.IS_CLEAN **/
	public int isDirty() {return m_bIsDirty;}
	public void makeDirty(int nDirty) {
		m_bIsDirty |= nDirty;
	}
	public void makeAllDirty(int nDirty) {
		m_bIsDirty = nDirty;
		if (!isLeaf()) {
			m_left.makeAllDirty(nDirty);
			if (m_right != null) {
				m_right.makeAllDirty(nDirty);
			}
		}
	}


	/** @return parent node, or null if this is root **/
	public Node getParent() {
		return m_Parent;
	}
	public void setParent(Node parent) {
		startEditing();
		if (m_Parent != parent) {
			m_Parent = parent;
			m_bIsDirty = Tree.IS_FILTHY;
		}
	}
	
	/**
	 * get all child node under this node, if this node is leaf then list.szie() = 0.
	 * @return
	 */
	public List<Node> getAllChildNodes() {
	    List<Node> childNodes = new ArrayList<Node>();
	    if (!this.isLeaf()) getAllChildNodes(childNodes);
	    return childNodes;
	}
	// recursive
	public void getAllChildNodes(List<Node> childNodes) {
	    childNodes.add(this);
	    if (!this.isLeaf()) {	     
	        m_right.getAllChildNodes(childNodes);
	        m_left.getAllChildNodes(childNodes);
	    }
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
		if (m_right != null) {
			return 1 + m_left.getNodeCount() + m_right.getNodeCount();
		} else {
			return 1 + m_left.getNodeCount();
		}
	}
	public int getLeafNodeCount() {
		if (isLeaf()) {
			return 1;
		}
		int nCount = m_left.getLeafNodeCount();
		if (m_right != null) {
			nCount +=  m_right.getLeafNodeCount();
		}
		return nCount;
	}
	public int getInternalNodeCount() {
		if (isLeaf()) {
			return 0;
		}
		int nCount = 1 + m_left.getInternalNodeCount();
		if (m_right != null) {
			nCount +=  m_right.getInternalNodeCount();
		}
		return nCount;
	}

	 /**
	 * @return beast.tree in Newick format, with length and meta data
	 * information. Unlike toNewick(), here Nodes are numbered, instead of 
	 * using the taxon names. 
	 * Also, internal nodes are labelled if bPrintInternalNodeLabels
	 * is set true. This is useful for example when storing a State to file
	 * so that it can be restored.
	 **/
	public String toShortNewick(boolean bPrintInternalNodeLabels) {
		StringBuffer buf = new StringBuffer();
		if (m_left != null) {
			if (bPrintInternalNodeLabels) {
				buf.append(m_iLabel + ":");
			}
			buf.append("(");
			buf.append(m_left.toShortNewick(bPrintInternalNodeLabels));
			if (m_right != null) {
				buf.append(',');
				buf.append(m_right.toShortNewick(bPrintInternalNodeLabels));
			}
			buf.append(")");
		} else {
			buf.append(m_iLabel);
		}
		buf.append(getNewickMetaData());
        buf.append(":").append(getLength());
		return buf.toString();
	}

	/** prints newick string where it orders by highest leaf number
	 * in a clade
	 */
	String toSortedNewick(int [] iMaxNodeInClade) {
		StringBuffer buf = new StringBuffer();
		if (m_left != null) {
			buf.append("(");
			String sChild1 = m_left.toSortedNewick(iMaxNodeInClade);
			int iChild1 = iMaxNodeInClade[0];
			if (m_right != null) {
				String sChild2 = m_right.toSortedNewick(iMaxNodeInClade);
				int iChild2 = iMaxNodeInClade[0];
				if (iChild1 > iChild2) {
					buf.append(sChild2);
					buf.append(",");
					buf.append(sChild1);
				} else {
					buf.append(sChild1);
					buf.append(",");
					buf.append(sChild2);
					iMaxNodeInClade[0] = iChild1;
				}
			} else {
				buf.append(sChild1);
			}
			buf.append(")");
		} else {
			iMaxNodeInClade[0] = m_iLabel;
			buf.append(m_iLabel);
		}			
        buf.append(":").append(getLength());
		return buf.toString();
	}
	
	/**
	 * @param sLabels names of the taxa
     * @return  beast.tree in Newick format with taxon labels for leafs.
	 **/
	public String toNewick(List<String> sLabels) {
		StringBuffer buf = new StringBuffer();
		if (m_left != null) {
			buf.append("(");
			buf.append(m_left.toNewick(sLabels));
			if (m_right != null) {
				buf.append(',');
				buf.append(m_right.toNewick(sLabels));
			}
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
			if (m_right != null) {
				buf.append(',');
				buf.append(m_right.toString(sLabels));
			}
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
		return toShortNewick(true);
	}
	
	/**
	 * sorts nodes in children according to lowest numbered label in subtree
	 *
     * @return
     **/
	public int sort() {
		if (m_left != null) {
			int iChild1 = m_left.sort();
			if (m_right != null) {
				int iChild2 = m_right.sort();
				if (iChild1 > iChild2) {
					Node tmp = m_left;
					m_left = m_right;
					m_right = tmp;
					return iChild2;
				}
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
			if (m_right != null) {
				iLabel = m_right.labelInternalNodes(iLabel);
			}
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
			node.m_left.m_Parent = node;
			if (m_right != null) {
				node.m_right = m_right.copy();
				node.m_right.m_Parent = node;
			}
		}
		return node;
	} // copy

	/** assign values to a tree in array representation **/
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
			node.m_left.m_Parent = node;
			if (m_right != null) {
				node.m_right = nodes[m_right.getNr()];
				m_right.assignTo(nodes);
				node.m_right.m_Parent = node;
			}
		}
	}
	
	/** assign values from a tree in array representation **/
	public void assignFrom(Node [] nodes, Node node) {
		m_fHeight = node.m_fHeight;
		m_iLabel = node.m_iLabel;
		m_sMetaData = node.m_sMetaData;
		m_Parent = null;
		m_sID = node.m_sID;
		if (node.m_left != null) {
			m_left = nodes[node.m_left.getNr()];
			m_left.assignFrom(nodes, node.m_left);
			m_left.m_Parent = this;
			if (node.m_right != null) {
				m_right = nodes[node.m_right.getNr()];
				m_right.assignFrom(nodes, node.m_right);
				m_right.m_Parent = this;
			}
		}
	}

	/** set meta-data according to pattern.
	 * Only heights are recognised, but derived classes could deal with
	 * richer meta data pattersn.
	 */
	public void setMetaData(String sPattern, Object fValue) {
		startEditing();
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
		startEditing();
		m_bIsDirty |= Tree.IS_DIRTY;
		if (!isLeaf()) {
			m_fHeight *= fScale;
			m_left.scale(fScale);
			if (m_right != null) {
				m_right.scale(fScale);
			}
			if (m_fHeight < m_left.m_fHeight || m_fHeight < m_right.m_fHeight) {
				throw new Exception("Scale gives negative branch length");
			}
		}
	}

	private void startEditing() {
		if (m_tree != null && m_tree.getState() != null) {
			m_tree.startEditing(null);
		}
	}

	/** some methods that are usefule for porting from BEAST 1 **/
	public int getChildCount() {
		if (isLeaf()) {
			return 0;
		} else {
			return 2;
		}
	}
	
	public Node getChild(int iChild) {
		if (iChild == 0) {
			return m_left;
		} else {
			return m_right;
		}
	}
	
	
} // class Node
