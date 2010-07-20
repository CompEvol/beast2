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


import beast.core.*;
import beast.core.parameter.Parameter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;


@Description("Tree (the T in BEAST) representing gene beast.tree, species beast.tree, language history, or " +
        "other time-beast.tree relationships among sequence data.")
public class Tree extends StateNode implements Loggable {

    @SuppressWarnings("unchecked")
    public Input<List<Parameter>> treeTraitsInput = new Input<List<Parameter>>("trait", "Traits on this tree, i.e. properties associated with nodes like population size, date, location, etc.", new ArrayList<Parameter>());


    @Override
    public void initAndValidate() throws Exception {
    }

    public static final int IS_CLEAN = 0, IS_DIRTY = 1, IS_FILTHY = 2;

    int nodeCount = -1;

    /**
     * node representation of the beast.tree *
     */
    protected beast.evolution.tree.Node root;

    // dead code
    // private boolean isStochastic = true;

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
    @Override
    public Tree copy() {
        Tree tree = new Tree();
        tree.m_sID = m_sID;
        tree.index = index;
        tree.root = root.copy();
        tree.nodeCount = nodeCount;
        tree.treeTraitsInput = treeTraitsInput;
//        tree.m_state = m_state;
        return tree;
    }

    @Override
    public void assignTo(StateNode other) {
        Tree tree = (Tree) other;
        Node[] nodes = new Node[nodeCount];
        listNodes(tree.root, nodes);
        tree.m_sID = m_sID;
        tree.index = index;
        root.assignTo(nodes);
        tree.root = nodes[root.getNr()];
        tree.nodeCount = nodeCount;
        tree.treeTraitsInput = treeTraitsInput;
        //tree.m_state = m_state;
    }

    void listNodes(Node node, Node[] nodes) {
        nodes[node.getNr()] = node;
        if (!node.isLeaf()) {
            listNodes(node.m_left, nodes);
            listNodes(node.m_right, nodes);
        }
    }

    public void makeDirty(int nDirt) {
        root.makeAllDirty(nDirt);
    }

    public String toString() {
        return root.toString();
    }

    /**
     * synchronise tree nodes with its traits stored in an array *
     */
    public void syncTreeWithTraitsInState() {
        boolean bSyncNeeded = false;
        for (Parameter<?> p : treeTraitsInput.get()) {
            p = (Parameter<?>) m_state.getStateNode(p.getIndex(m_state));
            if (p.isDirty()) {
                bSyncNeeded = true;
            }
        }
        if (bSyncNeeded) {
            syncTreeWithTraits(getRoot());
        }
    } // syncTreeWithTraitsInState

    void syncTreeWithTraits(Node node) {
        for (Parameter<?> p : treeTraitsInput.get()) {
            p = (Parameter<?>) m_state.getStateNode(p.getIndex(m_state));
            int iNode = Math.abs(node.getNr());
            if (p.isDirty(iNode)) {
                node.setMetaData(p.getID(), p.getValue(iNode));
            }
        }
        if (!node.isLeaf()) {
            syncTreeWithTraits(node.m_left);
            syncTreeWithTraits(node.m_right);
        }
    } // syncTreeWithTraits

    /** Loggable interface implementation follows **/
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

    /**
     * Loggable implementation follows *
     */
    public void init(State state, PrintStream out) throws Exception {
        out.println("#NEXUS\n");
        out.println("Begin trees");
        Node node = getRoot();
        out.println("\tTranslate");
        printTranslate(node, out, getNodeCount() / 2);
        out.print(";");
    }

    public void log(int nSample, State state, PrintStream out) {
        Tree tree = (Tree) getCurrent();//(Tree) state.getStateNode(m_sID);
        out.print("tree STATE_" + nSample + " = ");
        out.print(tree.getRoot().toString());
        out.print(";");
    }

    public void close(PrintStream out) {
        out.print("End;");
    }
} // class Tree
