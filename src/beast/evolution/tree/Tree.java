/*
* File Tree.java
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


import beast.core.Description;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.State;
import beast.core.StateNode;
import beast.core.parameter.Parameter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Description("Tree (the T in BEAST) representing gene beast.tree, species beast.tree, language history, or " +
        "other time-beast.tree relationships among sequence data.")
public class Tree extends StateNode implements Loggable {
    
	@SuppressWarnings("unchecked")
	public Input<List<Parameter>> treeTraitsInput = new Input<List<Parameter>>("trait", "Traits on this tree, i.e. properties associated with nodes like population size, date, location, etc.", new ArrayList<Parameter>());
	
	public static final int IS_CLEAN = 0, IS_DIRTY = 1, IS_FILTHY = 2;

    int nodeCount = -1;

    /**
     * node representation of the beast.tree *
     */
    protected beast.evolution.tree.Node root;

    private boolean isStochastic = true;

    /**
     * getters and setters
     *
     * @return the number of nodes in the beast.tree
     */
    public int getNodeCount() {
        return nodeCount;
    }

    public Node getRoot() {
        return root;
    }

    public void setRoot(Node root) {
        this.root = root;
        nodeCount = this.root.getNodeCount();
    }

    public Node getNode(int iNodeNr) {
        return getNode(iNodeNr, root);
    }

    public Node getNode(int iNodeNr, Node node) {
        if (node.getNr() == iNodeNr) {
            return node;
        }
        if (node.isLeaf()) {
            return null;
        } else {
            Node child = getNode(iNodeNr, node.m_left);
            if (child != null) {
                return child;
            }
            return getNode(iNodeNr, node.m_right);
        }
    } // getNode
    

    /**
     * copy meta data matching sPattern to double array
     *
     * @param node     the node
     * @param fT       the double array to be filled with meta data
     * @param sPattern the name of the meta data
     */
    public void getMetaData(Node node, Double[] fT, String sPattern) {
        fT[Math.abs(node.getNr())] = node.getMetaData(sPattern);
        if (!node.isLeaf()) {
            getMetaData(node.m_left, fT, sPattern);
            getMetaData(node.m_right, fT, sPattern);
        }
    }

    public void setMetaData(Node node, Double[] fT, String sPattern) {
        node.setMetaData(sPattern, fT[Math.abs(node.getNr())]);
        if (!node.isLeaf()) {
            setMetaData(node.m_left, fT, sPattern);
            setMetaData(node.m_right, fT, sPattern);
        }
    }

    /**
     * deep copy
     *
     * @return a deep copy of this beast.tree.
     */
    public Tree copy() {
        Tree tree = new Tree();
        tree.m_sID = m_sID;
        tree.root = root.copy();
        tree.nodeCount = nodeCount;
        return tree;
    }

    /**
     * validation for debugging only.
     * This code should be removed probably...
     *
     * @throws java.lang.Exception if the beast.tree is not valid
     */
    public void validate() throws Exception {
        // check
        // 1. all nodes have (correct) parents, except root
        // 2. nr of nodes adds up to m_nNodeCount
        // 3. branch lengths positive
        if (root.getParent() != null) {
            throw new Exception("v1: root has a parent!!!");
        }
        int[] nParents = new int[nodeCount];
        Arrays.fill(nParents, -1);
        int[] nLeft = new int[nodeCount];
        Arrays.fill(nLeft, -1);
        int[] nRight = new int[nodeCount];
        Arrays.fill(nRight, -1);

        int nNodes = validateNode(root, nParents, nLeft, nRight);
        if (nNodes != nodeCount) {
            System.err.println("v2: Lost some nodes " + (nodeCount - nNodes) + " out of " + nodeCount + " to be exact");
            System.err.println(root.toString());
            throw new Exception("v2: Lost some nodes " + (nodeCount - nNodes) + " out of " + nodeCount + " to be exact");
        }

        // check that leaf heights have are the same
//		double [] fHeights = new double[getNodeCount()];
//		calculateHeightsFromLengths(m_root, fHeights);
//		for (int i = 0; i < m_nNrOfNodes/2 + 1; i++) {
//			if (Math.abs(fHeights[i]-fHeights[0]) > 1e-8) {
//				throw new Exception("leaf heights have changed\n" + Arrays.toString(fHeights));
//			}
//		}
        checkLeafHeights(root);
    } // validate

    void checkLeafHeights(Node node) throws Exception {
        if (node.isLeaf()) {
            if (node.getHeight() != 0) {
                throw new Exception("leaf node is non-zero\n" + node.toString());
            }
        } else {
            checkLeafHeights(node.m_left);
            checkLeafHeights(node.m_right);
        }
        if (node.getLength() < 0) {
            throw new Exception("Negative branch length found" + node.toString());
        }
    }

//	void calculateHeightsFromLengths(Node node, double [] fHeights) {
//		Node parent = node.getParent();
//		if (parent != null) {
//			fHeights[node.getNr()] = fHeights[parent.getNr()] - node.m_fLength;
//		}
//		if (!node.isLeaf()) {
//			calculateHeightsFromLengths(node.m_left, fHeights);
//			calculateHeightsFromLengths(node.m_right, fHeights);
//		}
//	} // calculateHeightsFromLengths

    /**
     * traverse through beast.tree and check beast.tree on internal consistency
     *
     * @param node     the node
     * @param nParents the parents
     * @param nLeft    the left children
     * @param nRight   the right children
     * @return the total number of nodes below the given node, including the given node
     * @throws Exception if the beast.tree is not valid
     */
    int validateNode(Node node, int[] nParents, int[] nLeft, int[] nRight) throws Exception {
//		if (node.m_fLength < 0) {
//			throw new Exception("v3: Negative branch length for node " + node.getNr() + ": " + node.getID());
//		}
        if (!node.isRoot()) {
            // check consistency of parent-child link
            int iParent = node.getParent().getNr();
            int iNode = node.getNr();
            if (nLeft[iParent] != iNode && nRight[iParent] != iNode) {
                throw new Exception("v4: parent and child relation is broken");
            }
        }
        if (node.isLeaf()) {
            if (node.getParent() == null) {
                throw new Exception("v5: Suspect: single node which is leaf and root at the same time... (" + node.getID() + ")");
            }
            if (nParents[node.getNr()] != -1) {
                throw new Exception("v6: duplicate node number " + node.getNr());
            }
            nParents[node.getNr()] = node.getParent().getNr();
            return 1;
        } else {
            // check for cycles
            if (nLeft[node.getNr()] != -1) {
                throw new Exception("v7: duplicate left node number " + node.getNr());
            }
            if (nRight[node.getNr()] != -1) {
                throw new Exception("v8: duplicate left node number " + node.getNr());
            }
            nLeft[node.getNr()] = node.m_left.getNr();
            nRight[node.getNr()] = node.m_right.getNr();
            int nNodeCount = validateNode(node.m_left, nParents, nLeft, nRight);
            nNodeCount += validateNode(node.m_right, nParents, nLeft, nRight);
            return nNodeCount + 1;
        }
    } // validateNode

    public void makeDirty(int nDirt) {
        root.makeAllDirty(nDirt);
    }

    public String toString() {
        return root.toString();
    }

    
    /** synchronise tree nodes with its traits stored in an array **/
	public void syncTreeWithTraitsInState(State state) {
		boolean bSyncNeeded = false;
		for (Parameter<?> p : treeTraitsInput.get()) {
			p = (Parameter<?>) state.getStateNode(p.getIndex(state));
			if (p.isDirty()) {
				bSyncNeeded = true;
			}
		}	
		if (bSyncNeeded) {
			syncTreeWithTraits(getRoot(), state);
		}
	} // syncTreeWithTraitsInState

	void syncTreeWithTraits(Node node, State state) {
		for (Parameter<?> p : treeTraitsInput.get()) {
			p = (Parameter<?>) state.getStateNode(p.getIndex(state));
			int iNode = Math.abs(node.getNr());
			if (p.isDirty(iNode)) {
				node.setMetaData(p.getID(), p.getValue(iNode));
			}
		}
		if (!node.isLeaf()) {
			syncTreeWithTraits(node.m_left, state);
			syncTreeWithTraits(node.m_right, state);
		}
	} // syncTreeWithTraits
	
    /** Loggable interface implementation follows **/
    /** implementation for Loggable interface follows **/
    /**
     * print translate block for NEXUS beast.tree file *
     */
    void printTranslate(Node node, PrintStream out, int nNodeCount) {
        if (node.isLeaf()) {
            out.print("\t\t" + node.getNr() + " " + node.getID());
            if (node.getNr() < nNodeCount) {
                out.println(",");
            } else {
                out.println();
            }
        } else {
            printTranslate(node.m_left, out, nNodeCount);
            printTranslate(node.m_right, out, nNodeCount);
        }

    }
    
	@Override
	public void init(State state, PrintStream out) throws Exception {
		out.println("#NEXUS\n");
		out.println("Begin trees");
		Node node = getRoot();
		out.println("\tTranslate");
		printTranslate(node, out, getNodeCount() / 2);
		out.print(";");
	}

	@Override
	public void log(int nSample, State state, PrintStream out) {
		Tree tree = (Tree) state.getStateNode(m_sID);
		out.print("tree STATE_" + nSample + " = ");
		out.print(tree.getRoot().toString());
		out.print(";");
	}

	@Override
	public void close(PrintStream out) {
      out.print("End;");
	}
} // class Tree
