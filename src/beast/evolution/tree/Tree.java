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
import beast.core.StateNode;
import beast.util.TreeParser;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Description("Tree (the T in BEAST) representing gene beast.tree, species beast.tree, language history, or " +
        "other time-beast.tree relationships among sequence data.")
public class Tree extends StateNode {
	
	public Input<TraitSet> m_trait = new Input<TraitSet>("trait", "trait information for initializing traits (like node dates) in the tree");

    @Override
    public void initAndValidate() throws Exception {
    	if (m_trait.get() != null) {
    		adjustTreeToNodeHeights(root);
    	}
    }

    
    public Tree() {}
    
    /** Constructor used by Input.setValue(String) **/
    public Tree(String sNewick) throws Exception {
    	TreeParser parser = new TreeParser();
    	setRoot(parser.parseNewick(sNewick));
    }
    
	/** process m_nodeDates, moving taxon heights to match the m_nodeHeights if necessary.
     * If this leads to internal branch lengths becoming negative, the internal nodes are
     * moved as well.
     */
	protected void adjustTreeToNodeHeights(Node node) {
		if (node.isLeaf()) {
			node.setMetaData(m_trait.get().getTraitName(), m_trait.get().getValue(node.getNr()));
		} else {
			adjustTreeToNodeHeights(node.m_left);
			adjustTreeToNodeHeights(node.m_right);
			if (node.m_fHeight < node.m_left.getHeight()) {
				node.m_fHeight = node.m_left.getHeight() + 0.000001;
			}
			if (node.m_fHeight < node.m_right.getHeight()) {
				node.m_fHeight = node.m_right.getHeight() + 0.000001;
			}
		}
	}

    
    
    public static final int IS_CLEAN = 0, IS_DIRTY = 1, IS_FILTHY = 2;

    int nodeCount = -1;

    /**
     * node representation of the beast.tree *
     */
    protected beast.evolution.tree.Node root;

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
//        tree.treeTraitsInput = treeTraitsInput;
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
//        tree.treeTraitsInput = treeTraitsInput;
        //tree.m_state = m_state;
    }

    public Node [] getNodesAsArray() {
        if (m_nodes == null) {
        	m_nodes = new Node[nodeCount];
        	listNodes(root, m_nodes);
        }    	
        return m_nodes;
    }
    
    @Override
    public void assignFrom(StateNode other) {
        Tree tree = (Tree) other;
        Node [] nodes = getNodesAsArray();
        m_sID = tree.m_sID;
        index = tree.index;
        root = nodes[tree.root.getNr()];
        root.assignFrom(nodes, tree.root);
        root.m_Parent = null;
        nodeCount = tree.nodeCount;
//      tree.treeTraitsInput = treeTraitsInput;
        //m_state = tree.m_state;
    }

    Node[] m_nodes = null;
    @Override
    public void assignFromFragile(StateNode other) {
        Tree tree = (Tree) other;
        if (m_nodes == null) {
        	m_nodes = new Node[nodeCount];
        	listNodes(root, m_nodes);
        	if (tree.m_nodes == null) {
        		tree.m_nodes = new Node[nodeCount];
            	listNodes(tree.root, tree.m_nodes);
        	}
        }
        
        root = m_nodes[tree.root.getNr()];
        Node [] otherNodes = tree.m_nodes;
        int iRoot = root.getNr();
        assignFrom(0, iRoot, otherNodes);
        root.m_fHeight = otherNodes[iRoot].m_fHeight;
        root.m_Parent = null;
    	root.m_left = m_nodes[otherNodes[iRoot].m_left.getNr()];
    	root.m_right = m_nodes[otherNodes[iRoot].m_right.getNr()];
//    	root.assignFromFragile(otherNodes[iRoot]);
        assignFrom(iRoot + 1, nodeCount, otherNodes);
    }
    
    void assignFrom(int iStart, int iEnd, Node [] otherNodes) {
        for (int i = iStart; i < iEnd; i++) {
        	Node sink = m_nodes[i];
        	Node src = otherNodes[i];
        	sink.m_fHeight = src.m_fHeight;
        	sink.m_Parent = m_nodes[src.m_Parent.getNr()];
        	if (src.m_left != null) {
        		sink.m_left = m_nodes[src.m_left.getNr()];
        		sink.m_right = m_nodes[src.m_right.getNr()];
        	}
//        	sink.assignFromFragile(src);
        }
    }
    /** convert tree to array representation **/
    void listNodes(Node node, Node[] nodes) {
        nodes[node.getNr()] = node;
        if (!node.isLeaf()) {
            listNodes(node.m_left, nodes);
            listNodes(node.m_right, nodes);
        }
    }
    
    @Override
    public void setEverythingDirty(boolean bDirty) {
    	setSomethingIsDirty(bDirty);
    	if ( !bDirty ) {
    		root.makeAllDirty(IS_CLEAN);
    	} else {
    		root.makeAllDirty(IS_FILTHY);
    	}
    }

    public String toString() {
        return root.toString();
    }


    /* Loggable interface implementation follows */

    /**
     * print translate block for NEXUS beast.tree file
     */
    public static void printTranslate(Node node, PrintStream out, int nNodeCount) {
    	List<String> translateLines = new ArrayList<String>();
        printTranslate(node, translateLines, nNodeCount);
        Collections.sort(translateLines);
        for (String sLine : translateLines) {
        	out.println(sLine);
        }
    }

    /** need this helper so that we can sort list of entries **/ 
    static void printTranslate(Node node, List<String> translateLines, int nNodeCount) {
        if (node.isLeaf()) {
        	String sNr = node.getNr() +"";
        	String sLine = "\t\t" + "    ".substring(sNr.length()) + sNr + " " + node.getID();
            if (node.getNr() < nNodeCount) {
            	sLine += ",";
            }
            translateLines.add(sLine);
        } else {
            printTranslate(node.m_left, translateLines, nNodeCount);
            printTranslate(node.m_right, translateLines, nNodeCount);
        }
    }

    /**
     * Loggable implementation follows *
     */
    @Override
    public void init(PrintStream out) throws Exception {
        out.println("#NEXUS\n");
        out.println("Begin trees");
        Node node = getRoot();
        out.println("\tTranslate");
        printTranslate(node, out, getNodeCount() / 2);
        out.print(";");
    }

    @Override
    public void log(int nSample, PrintStream out) {
        Tree tree = (Tree) getCurrent();//(Tree) state.getStateNode(m_sID);
        out.print("tree STATE_" + nSample + " = ");
		tree.getRoot().sort();
        out.print(tree.getRoot().toString());
        out.print(";");
    }

    @Override
    /** @see Loggable.close(PrintStream) **/
    public void close(PrintStream out) {
        out.print("End;");
    }

	@Override
	public int scale(double fScale) throws Exception {
		root.scale(fScale);
		return nodeCount/2;
	}

	@Override
	public void fromXML(org.w3c.dom.Node node) {
		String sNewick = node.getTextContent();
		TreeParser parser = new TreeParser();
		try {
			parser.m_nOffset.setValue(0, parser);
			setRoot(parser.parseNewick(sNewick));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override public int getDimension() {return nodeCount;}
    @Override public double getArrayValue() {return (double) root.m_fHeight;}
    @Override public double getArrayValue(int iValue) {
        if (m_nodes == null) {
        	m_nodes = new Node[nodeCount];
        	listNodes(root, m_nodes);
        }
    	return (double) m_nodes[iValue].m_fHeight;
    }
} // class Tree
