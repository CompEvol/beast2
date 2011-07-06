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
import beast.core.StateNodeInitialiser;
import beast.evolution.alignment.TaxonSet;
import beast.util.TreeParser;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
* Note that leaf nodes are always numbered 0,...,nodeCount-1
* Internal nodes are numbered higher, but the root has no guaranteed 
* number.
*/

@Description("Tree (the T in BEAST) representing gene beast.tree, species beast.tree, language history, or " +
        "other time-beast.tree relationships among sequence data.")
public class Tree extends StateNode {
	public Input<Tree> m_initial = new Input<Tree>("initial","tree to start with");
	public Input<TraitSet> m_trait = new Input<TraitSet>("trait", "trait information for initializing traits (like node dates) in the tree");
	public Input<TaxonSet> m_taxonset = new Input<TaxonSet>("taxonset", "set of taxa that correspond to the leafs in the tree");
	
    @Override
    public void initAndValidate() throws Exception {
    	if (m_initial.get() != null && !(this instanceof StateNodeInitialiser)) {
    		Tree other = m_initial.get();
            root = other.root.copy();
            nodeCount = other.nodeCount;
            internalNodeCount = other.internalNodeCount;
            leafNodeCount = other.leafNodeCount;
    	}
    	
    	if (m_trait.get() != null) {
    		adjustTreeToNodeHeights(root);
    	}
    	if (nodeCount < 0) {
    		// make dummy tree with a single root node
    		root = new Node();
    		root.m_iLabel = 0;
    		root.m_fHeight = 0;
    		root.m_tree = this;
    		nodeCount = 1;
    		internalNodeCount = 0;
    		leafNodeCount = 1;
    	}
    	
    	if (nodeCount >= 0) {
    		initArrays();
    	}
    }

    void initArrays() {
    	// initialise tree-as-array representation + its stored variant
    	m_nodes = new Node[nodeCount];
    	listNodes(root, m_nodes);
    	m_storedNodes = new Node[nodeCount];
        Node copy = root.copy();
    	listNodes(copy, m_storedNodes);
    }

    
    public Tree() {}
    
    /** Constructor used by Input.setValue(String) **/
    public Tree(String sNewick) throws Exception {
    	TreeParser parser = new TreeParser();
    	setRoot(parser.parseNewick(sNewick));
    	initArrays();
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

    
    /** state of dirtiness of a node in the tree
     * DIRTY means a property on the node has changed, but not the topology
     * FILTHY means the nodes' parent or child has changed.
     */
    public static final int IS_CLEAN = 0, IS_DIRTY = 1, IS_FILTHY = 2;

    /** counters of number of nodes, nodeCount = internalNodeCount + leafNodeCount **/
    int nodeCount = -1;
    int internalNodeCount = -1;
    int leafNodeCount = -1;

    /**
     * node representation of the beast.tree *
     */
    protected beast.evolution.tree.Node root;
    protected beast.evolution.tree.Node storedRoot;

    /** array of all nodes in the tree **/
    Node[] m_nodes = null;
    Node[] m_storedNodes = null;
    
    /** array of taxa names for the nodes in the tree 
     * such that m_sTaxaNames[node.getNr()] == node.getID()**/
    String [] m_sTaxaNames = null;
    
    
    /**
     * getters and setters
     *
     * @return the number of nodes in the beast.tree
     */
    public int getNodeCount() {
    	if (nodeCount < 0) {
    		nodeCount = this.root.getNodeCount();
    	}
        return nodeCount;
    }
    public int getInternalNodeCount() {
    	if (internalNodeCount < 0) {
    		internalNodeCount = root.getInternalNodeCount();
    	}
        return internalNodeCount;
    }
    public int getLeafNodeCount() {
    	if (leafNodeCount < 0) {
    		leafNodeCount = root.getLeafNodeCount();
    	}
        return leafNodeCount;
    }

    public Node getRoot() {
        return root;
    }

    public void setRoot(Node root) {
        this.root = root;
       	nodeCount = this.root.getNodeCount();
    }

    public Node getNode(int iNodeNr) {
    	return m_nodes[iNodeNr];
        //return getNode(iNodeNr, root);
    }

    public String [] getTaxaNames() {
    	if (m_sTaxaNames == null) {
    		if (m_taxonset.get() != null) {
    			m_sTaxaNames = m_taxonset.get().asStringList().toArray(new String[0]);
    		} else {
    			m_sTaxaNames = new String[getLeafNodeCount()];
    			collectTaxaNames(getRoot());
    		}
    	}
    	return m_sTaxaNames;
    }
    
	void collectTaxaNames(Node node) {
		if (node.isLeaf()) {
			m_sTaxaNames[node.getNr()] = node.getID();
		} else {
			collectTaxaNames(node.m_left);
			collectTaxaNames(node.m_right);
		}
	}


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
            if (node.m_right != null) {
            	getMetaData(node.m_right, fT, sPattern);
            }
        }
    }
    
    /** traverse tree and assign meta-data values in fT to nodes in the
     * tree to the meta-data field represented by the given pattern. 
     * This only has an effect when setMetadata() in a subclass
     * of Node know how to process such value.
     */
    public void setMetaData(Node node, Double[] fT, String sPattern) {
        node.setMetaData(sPattern, fT[Math.abs(node.getNr())]);
        if (!node.isLeaf()) {
            setMetaData(node.m_left, fT, sPattern);
            if (node.m_right != null) {
            	setMetaData(node.m_right, fT, sPattern);
            }
        }
    }


    /** convert tree to array representation **/
    void listNodes(Node node, Node[] nodes) {
        nodes[node.getNr()] = node;
        node.m_tree = this;
        if (!node.isLeaf()) {
            listNodes(node.m_left, nodes);
            if (node.m_right != null) {
            	listNodes(node.m_right, nodes);
            }
        }
    }

    /** returns list of nodes in array format.
     *  **/
    public Node [] getNodesAsArray() {
        return m_nodes;
    }


    /**
     * deep copy, returns a completely new tree
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
        tree.internalNodeCount = internalNodeCount;
        tree.leafNodeCount = leafNodeCount;
        return tree;
    }

    /** copy of all values into existing tree **/
    @Override
    public void assignTo(StateNode other) {
        Tree tree = (Tree) other;
        Node[] nodes = new Node[nodeCount];
        listNodes(tree.root, nodes);
        tree.m_sID = m_sID;
        //tree.index = index;
        root.assignTo(nodes);
        tree.root = nodes[root.getNr()];
        tree.nodeCount = nodeCount;
        tree.internalNodeCount = internalNodeCount;
        tree.leafNodeCount = leafNodeCount;
    }

    /** copy of all values from existing tree **/
    @Override
    public void assignFrom(StateNode other) {
        Tree tree = (Tree) other;
        Node [] nodes = new Node[tree.getNodeCount()];//tree.getNodesAsArray();
        for (int i = 0; i < tree.getNodeCount(); i++) {
        	nodes[i] = new Node();
        }
        m_sID = tree.m_sID;
        //index = tree.index;
        root = nodes[tree.root.getNr()];
        root.assignFrom(nodes, tree.root);
        root.m_Parent = null;
        nodeCount = tree.nodeCount;
        internalNodeCount = tree.internalNodeCount;
        leafNodeCount = tree.leafNodeCount;
        initArrays();
    }

    /** as assignFrom, but only copy tree structure **/
    @Override
    public void assignFromFragile(StateNode other) {
        Tree tree = (Tree) other;
        if (m_nodes == null) {
        	initArrays();
        }
        root = m_nodes[tree.root.getNr()];
        Node [] otherNodes = tree.m_nodes;
        int iRoot = root.getNr();
        assignFrom(0, iRoot, otherNodes);
        root.m_fHeight = otherNodes[iRoot].m_fHeight;
        root.m_Parent = null;
    	if (otherNodes[iRoot].m_left != null) {
    		root.m_left = m_nodes[otherNodes[iRoot].m_left.getNr()];
    	} else {
    		root.m_left = null;
    	}
    	if (otherNodes[iRoot].m_right != null) {
    		root.m_right = m_nodes[otherNodes[iRoot].m_right.getNr()];
    	} else {
    		root.m_right = null;
    	}
        assignFrom(iRoot + 1, nodeCount, otherNodes);
    }
    /** helper to assignFromFragile **/
    private void assignFrom(int iStart, int iEnd, Node [] otherNodes) {
        for (int i = iStart; i < iEnd; i++) {
        	Node sink = m_nodes[i];
        	Node src = otherNodes[i];
        	sink.m_fHeight = src.m_fHeight;
        	sink.m_Parent = m_nodes[src.m_Parent.getNr()];
        	if (src.m_left != null) {
        		sink.m_left = m_nodes[src.m_left.getNr()];
        		if (src.m_right != null) {
        			sink.m_right = m_nodes[src.m_right.getNr()];
        		} else {
        			sink.m_right = null;
        		}
        	}
        }
    }
    

    public String toString() {
        return root.toString();
    }


	/**
     * StateNode implementation 
     */
    @Override
    public void setEverythingDirty(boolean bDirty) {
    	setSomethingIsDirty(bDirty);
    	if ( !bDirty ) {
    		root.makeAllDirty(IS_CLEAN);
    	} else {
    		root.makeAllDirty(IS_FILTHY);
    	}
    }

    @Override
	public int scale(double fScale) throws Exception {
		root.scale(fScale);
		return getInternalNodeCount();
	}

	/** Loggable interface implementation follows **/

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
            if (node.m_right != null) {
            	printTranslate(node.m_right, translateLines, nNodeCount);
            }
        }
    }

    public static void printTaxa(Node node, PrintStream out, int nNodeCount) {
    	List<String> translateLines = new ArrayList<String>();
    	printTranslate(node, translateLines, nNodeCount);
        Collections.sort(translateLines);
        for (String sLine : translateLines) {
        	sLine = sLine.split("\\s+")[2];
        	out.println("\t\t\t"+sLine.replace(',', ' '));
        }
    }

    @Override
    public void init(PrintStream out) throws Exception {
        Node node = getRoot();
        out.println("#NEXUS\n");
        out.println("Begin taxa;");
        out.println("\tDimensions ntax=" + getLeafNodeCount() + ";");
        out.println("\t\tTaxlabels");
        printTaxa(node, out, getNodeCount() / 2);
        out.println("\t\t\t;");
		out.println("End;");
        
        out.println("Begin trees;");
        out.println("\tTranslate");
        printTranslate(node, out, getNodeCount() / 2);
        out.print(";");
    }

    @Override
    public void log(int nSample, PrintStream out) {
        Tree tree = (Tree) getCurrent();
        out.print("tree STATE_" + nSample + " = ");
        // Don't sort, this can confuse CalculationNodes relying on the tree
		//tree.getRoot().sort();
		int [] dummy = new int[1];
		String sNewick = tree.getRoot().toSortedNewick(dummy);
        out.print(sNewick);
        out.print(";");
    }

    @Override
    /** @see Loggable.close(PrintStream) **/
    public void close(PrintStream out) {
        out.print("End;");
    }

	/** reconstruct tree from XML fragment in the form of a DOM node **/
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
		initArrays();
	}

	/** Valuable implementation **/
	@Override public int getDimension() {return getNodeCount();}
    @Override public double getArrayValue() {return (double) root.m_fHeight;}
    @Override public double getArrayValue(int iValue) {
    	return (double) m_nodes[iValue].m_fHeight;
    }

	/** StateNode implementation **/
    @Override
    protected void store() {
        storedRoot = m_storedNodes[root.getNr()];
        int iRoot = root.getNr();
        storeNodes(0, iRoot);
        storedRoot.m_fHeight = m_nodes[iRoot].m_fHeight;
        storedRoot.m_Parent = null;
    	if (root.m_left != null) {
    		storedRoot.m_left = m_storedNodes[root.m_left.getNr()];
    	} else {
    		storedRoot.m_left = null;
    	}
    	if (root.m_right != null) {
    		storedRoot.m_right = m_storedNodes[root.m_right.getNr()];
    	} else {
    		storedRoot.m_right = null;
    	}
    	storeNodes(iRoot + 1, nodeCount);
    }
    /** helper to store **/
    private void storeNodes(int iStart, int iEnd) {
        for (int i = iStart; i < iEnd; i++) {
        	Node sink = m_storedNodes[i];
        	Node src = m_nodes[i];
        	sink.m_fHeight = src.m_fHeight;
        	sink.m_Parent = m_storedNodes[src.m_Parent.getNr()];
        	if (src.m_left != null) {
        		sink.m_left = m_storedNodes[src.m_left.getNr()];
        		if (src.m_right != null) {
        			sink.m_right = m_storedNodes[src.m_right.getNr()];
        		} else {
        			sink.m_right = null;
        		}
        	}
        }
    }

    @Override
    public void restore() {
    	Node [] tmp = m_storedNodes;
    	m_storedNodes = m_nodes;
    	m_nodes = tmp;
    	root = m_nodes[storedRoot.getNr()];
    	m_bHasStartedEditing = false;
    }
    
} // class Tree
