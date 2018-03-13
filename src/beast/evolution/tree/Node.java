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


import beast.core.BEASTObject;
import beast.core.Description;
import beast.util.HeapSort;

import java.util.*;


@Description("Nodes in building beast.tree data structure.")
public class Node extends BEASTObject {

    /**
     * label nr of node, used mostly when this is a leaf.
     */
    protected int labelNr;

    /**
     * height of this node.
     */
    protected double height = Double.MAX_VALUE;

    /**
     * Arbitrarily labeled metadata on this node. Not currently implemented as part of state!
     */
    protected Map<String, Object> metaData = new TreeMap<>();

    /**
     * Length metadata for the edge above this node.  Not currently implemented as part of state!
     */
    protected Map<String, Object> lengthMetaData = new TreeMap<>();

    /**
     * list of children of this node *
     * Don't use m_left and m_right directly
     * Use getChildCount() and getChild(x) or getChildren() instead
     */
    List<Node> children = new ArrayList<>();

    /**
     * parent node in the beast.tree, null if root *
     */
    Node parent = null;

    /**
     * status of this node after an operation is performed on the state *
     */
    int isDirty = Tree.IS_CLEAN;

    /**
     * meta-data contained in square brackets in Newick *
     */
    public String metaDataString, lengthMetaDataString;

    /**
     * The Tree that this node is a part of.
     * This allows e.g. access to the State containing the Tree *
     */
    protected Tree m_tree;

    public Node() {
    }

    public Node(final String id) {
        setID(id);
        initAndValidate();
    }

    public Tree getTree() {
        return m_tree;
    }

    @Override
    public void initAndValidate() {
        // do nothing
    }

    /**
     * @return number uniquely identifying the node in the tree.
     *         This is a number between 0 and the total number of nodes in the tree
     *         Leaf nodes are number 0 to #leaf nodes -1
     *         Internal nodes are numbered  #leaf nodes  up to #nodes-1
     *         The root node is always numbered #nodes-1
     */
    public int getNr() {
        return labelNr;
    }

    public void setNr(final int labelIndex) {
        labelNr = labelIndex;
    }

    public double getHeight() {
        return height;
    }

    public double getDate() {
        return m_tree.getDate(height);
    }

    public void setHeight(final double height) {
        startEditing();
        this.height = height;
        isDirty |= Tree.IS_DIRTY;
        if (!isLeaf()) {
            getLeft().isDirty |= Tree.IS_DIRTY;
            if (getRight() != null) {
                getRight().isDirty |= Tree.IS_DIRTY;
            }
        }
    }

    /**
     * @return length of branch between this node and its parent in the beast.tree
     */
    public final double getLength() {
        if (isRoot()) {
            return 0;
        } else {
            return getParent().height - height;
        }
    }

    /**
     * methods for accessing the dirtiness state of the Node.
     * A Node is Tree.IS_DIRTY if its value (like height) has changed
     * A Node Tree.IS_if FILTHY if its parent or child has changed.
     * Otherwise the node is Tree.IS_CLEAN *
     */
    public int isDirty() {
        return isDirty;
    }

    public void makeDirty(final int dirty) {
        isDirty |= dirty;
    }

    public void makeAllDirty(final int dirty) {
        isDirty = dirty;
        if (!isLeaf()) {
            getLeft().makeAllDirty(dirty);
            if (getRight() != null) {
                getRight().makeAllDirty(dirty);
            }
        }
    }


    /**
     * @return parent node, or null if this is root *
     */
    public Node getParent() {
        return parent;
    }

    /**
     * Calls setParent(parent, true)
     *
     * @param parent the new parent to be set, must be called from within an operator.
     */
    public void setParent(final Node parent) {
        setParent(parent, true);
    }

    /**
     * Sets the parent of this node
     *
     * @param parent     the node to become parent
     * @param inOperator if true, then startEditing() is called and setting the parent will make tree "filthy"
     */
    void setParent(final Node parent, final boolean inOperator) {
        if (inOperator) startEditing();
        if (this.parent != parent) {
        	this.parent = parent;
            if (inOperator) isDirty = Tree.IS_FILTHY;
        }
    }

     /**
     * Sets the parent of this node. No overhead, no side effects like setting dirty flags etc.
     *
     * @param parent     the node to become parent
     */
    void setParentImmediate(final Node parent) {
        this.parent = parent;
    }

    /**
     * @return unmodifiable list of children of this node
     */
    public List<Node> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * get all child node under this node, if this node is leaf then list.size() = 0.
     * This returns all child nodes including this node.
     *
     * @return all child nodes including this node
     */
    public List<Node> getAllChildNodes() {
        final List<Node> childNodes = new ArrayList<>();
        if (!this.isLeaf()) getAllChildNodes(childNodes);
        return childNodes;
    }

    // recursive
    public void getAllChildNodes(final List<Node> childNodes) {
        childNodes.add(this);
        for (Node child : children)
            child.getAllChildNodes(childNodes);
    }

    /**
     * get all leaf node under this node, if this node is leaf then list.size() = 0.
     *
     * @return
     */
    public List<Node> getAllLeafNodes() {
        final List<Node> leafNodes = new ArrayList<>();
        if (!this.isLeaf()) getAllLeafNodes(leafNodes);
        return leafNodes;
    }

    // recursive
    public void getAllLeafNodes(final List<Node> leafNodes) {
        if (this.isLeaf()) {
            leafNodes.add(this);
        }

        for (Node child : children)
            child.getAllLeafNodes(leafNodes);
    }

    /**
     * @return true if current node is root node *
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * @return true if current node is a leaf node *
     */
    public boolean isLeaf() {
        return children.size() == 0;
        //return getLeft() == null && getRight() == null;
    }

    public void removeChild(final Node child) {
        startEditing();
        children.remove(child);
    }

    /**
     * Removes all children from this node.
     *
     * @param inOperator if true then startEditing() is called. For operator uses, called removeAllChildren(true), otherwise
     *                   use set to false.
     */
    public void removeAllChildren(final boolean inOperator) {
        if (inOperator) startEditing();
        children.clear();
    }

    public void addChild(final Node child) {
        child.setParent(this);
        children.add(child);
    }

    /**
     * @return count number of nodes in beast.tree, starting with current node *
     */
    public int getNodeCount() {
        int nodes = 1;
        for (final Node child : children) {
            nodes += child.getNodeCount();
        }
        return nodes;
    }

    public int getLeafNodeCount() {
        if (isLeaf()) {
            return 1;
        }
        int nodes = 0;
        for (final Node child : children) {
            nodes += child.getLeafNodeCount();
        }
        return nodes;
    }

    public int getInternalNodeCount() {
        if (isLeaf()) {
            return 0;
        }
        int nodes = 1;
        for (final Node child : children) {
            nodes += child.getInternalNodeCount();
        }
        return nodes;
    }

    /**
     * @return beast.tree in Newick format, with length and meta data
     *         information. Unlike toNewick(), here Nodes are numbered, instead of
     *         using the node labels.
     *         If there are internal nodes with non-null IDs then their numbers are also printed.
     *         Also, all internal nodes are labelled if printInternalNodeNumbers
     *         is set true. This is useful for example when storing a State to file
     *         so that it can be restored.
     */
    public String toShortNewick(final boolean printInternalNodeNumbers) {
        final StringBuilder buf = new StringBuilder();

        if (!isLeaf()) {
            buf.append("(");
            boolean isFirst = true;
            for (Node child : getChildren()) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append(",");
                buf.append(child.toShortNewick(printInternalNodeNumbers));
            }
            buf.append(")");
        }

        if (isLeaf() || getID() != null || printInternalNodeNumbers) {
            buf.append(getNr());
        }

        buf.append(getNewickMetaData());
        buf.append(":").append(getNewickLengthMetaData()).append(getLength());
        return buf.toString();
    }

    /**
     * prints newick string where it orders by highest leaf number
     * in a clade. Print node numbers (m_iLabel) incremented by 1
     * for leaves and internal nodes with non-null IDs.
     */
    String toSortedNewick(final int[] maxNodeInClade) {
        return toSortedNewick(maxNodeInClade, false);
    }

    public String toSortedNewick(int[] maxNodeInClade, boolean printMetaData) {
        StringBuilder buf = new StringBuilder();

        if (!isLeaf()) {

            if (getChildCount() <= 2) {
                // Computationally cheap method for special case of <=2 children

                buf.append("(");
                String child1 = getChild(0).toSortedNewick(maxNodeInClade, printMetaData);
                int child1Index = maxNodeInClade[0];
                if (getChildCount() > 1) {
                    String child2 = getChild(1).toSortedNewick(maxNodeInClade, printMetaData);
                    int child2Index = maxNodeInClade[0];
                    if (child1Index > child2Index) {
                        buf.append(child2);
                        buf.append(",");
                        buf.append(child1);
                    } else {
                        buf.append(child1);
                        buf.append(",");
                        buf.append(child2);
                        maxNodeInClade[0] = child1Index;
                    }
                } else {
                    buf.append(child1);
                }
                buf.append(")");
                if (getID() != null) {
                    buf.append(labelNr+1);
                }

            } else {
                // General method for >2 children

                String[] childStrings = new String[getChildCount()];
                int[] maxNodeNrs = new int[getChildCount()];
                Integer[] indices = new Integer[getChildCount()];
                for (int i = 0; i < getChildCount(); i++) {
                    childStrings[i] = getChild(i).toSortedNewick(maxNodeInClade, printMetaData);
                    maxNodeNrs[i] = maxNodeInClade[0];
                    indices[i] = i;
                }

                Arrays.sort(indices, (i1, i2) -> {
                    if (maxNodeNrs[i1] < maxNodeNrs[i2])
                        return -1;

                    if (maxNodeNrs[i1] > maxNodeNrs[i2])
                        return 1;

                    return 0;
                });

                maxNodeInClade[0] = maxNodeNrs[maxNodeNrs.length - 1];

                buf.append("(");
                for (int i = 0; i < indices.length; i++) {
                    if (i > 0)
                        buf.append(",");

                    buf.append(childStrings[indices[i]]);
                }

                buf.append(")");

                if (getID() != null) {
                    buf.append(labelNr + 1);
                }
            }

        } else {
            maxNodeInClade[0] = labelNr;
            buf.append(labelNr + 1);
        }

        if (printMetaData) {
            buf.append(getNewickMetaData());
        }

        buf.append(":");
        if (printMetaData)
                buf.append(getNewickLengthMetaData());
        buf.append(getLength());

        return buf.toString();
    }

    @Deprecated
    public String toNewick(final List<String> labels) {
        throw new UnsupportedOperationException("Please use toNewick(). Labels will come from node.getId() or node.getNr().");
    }

    /**
     *
     * @param onlyTopology  if true, only print topology
     * @return
     */
    public String toNewick(boolean onlyTopology) {
        final StringBuilder buf = new StringBuilder();
        if (!isLeaf()) {
            buf.append("(");
            boolean isFirst = true;
            for (Node child : getChildren()) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append(",");
                buf.append(child.toNewick(onlyTopology));
            }
            buf.append(")");

            if (getID() != null)
                buf.append(getID());
        } else {
            if (getID() != null)
                buf.append(getID());
            else
                buf.append(labelNr);
        }

        if (!onlyTopology) {
            buf.append(getNewickMetaData());
            buf.append(":").append(getNewickLengthMetaData()).append(getLength());
        }
        return buf.toString();
    }


    /**
     * @return beast.tree in Newick format with taxon labels for labelled tip nodes
     * and labeled (having non-null ID) internal nodes.
     * If a tip node doesn't have an ID (taxon label) then node number (m_iLabel) is printed.
     */
    public String toNewick() {
        return toNewick(false);
    }

    public String getNewickMetaData() {
        if (metaDataString != null)
            return "[&" + metaDataString + ']';
        else
            return "";
    }

    public String getNewickLengthMetaData() {
        if (lengthMetaDataString != null)
            return "[&" + lengthMetaDataString + "]";
        else
            return "";
    }

    /**
     * @param labels
     * @return beast.tree in long Newick format, with all length and meta data
     *         information, but with leafs labelled with their names
     */
    public String toString(final List<String> labels) {
        final StringBuilder buf = new StringBuilder();

        if (isLeaf()) {
            buf.append(labels.get(labelNr));
        } else {
            buf.append("(");
            boolean isFirst = true;
            for (Node child : getChildren()) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append(",");
                buf.append(child.toString(labels));
            }
            buf.append(")");
        }

        if (isLeaf())
            buf.append(labels.get(labelNr));

        if (metaDataString != null) {
            buf.append('[');
            buf.append(metaDataString);
            buf.append(']');
        }
        buf.append(":");
        if (lengthMetaDataString != null) {
            buf.append('[');
            buf.append(lengthMetaDataString);
            buf.append(']');
        }
        buf.append(getLength());

        return buf.toString();
    }

    @Override
	public String toString() {
        return toShortNewick(true);
    }

    /**
     * sorts nodes in children according to lowest numbered label in subtree
     *
     * @return
     */
    public int sort() {

        if (isLeaf()) {
            return labelNr;
        }

        final int childCount = getChildCount();

        if (childCount == 1) return getChild(0).sort();

        final List<Integer> lowest = new ArrayList<>();
        final int[] indices = new int[childCount];

        // relies on this being a copy of children list
        final List<Node> children = new ArrayList<>(getChildren());

        for (final Node child : children) {
            lowest.add(child.sort());
        }
        HeapSort.sort(lowest, indices);
        for (int i = 0; i < childCount; i++) {
            setChild(i, children.get(indices[i]));
        }
        return lowest.get(indices[0]);
    } // sort

    /**
     * during parsing, leaf nodes are numbered 0...m_nNrOfLabels-1
     * but internal nodes are left to zero. After labeling internal
     * nodes, m_iLabel uniquely identifies a node in a beast.tree.
     *
     * @param labelIndex
     * @return
     */
    public int labelInternalNodes(int labelIndex) {
        if (isLeaf()) {
            return labelIndex;
        } else {
            labelIndex = getLeft().labelInternalNodes(labelIndex);
            if (getRight() != null) {
                labelIndex = getRight().labelInternalNodes(labelIndex);
            }
            labelNr = labelIndex++;
        }
        return labelIndex;
    } // labelInternalNodes

    /**
     * @return (deep) copy of node
     */
    public Node copy() {
        final Node node = new Node();
        node.height = height;
        node.labelNr = labelNr;
        node.metaDataString = metaDataString;
        node.lengthMetaDataString = lengthMetaDataString;
        node.metaData = new TreeMap<>(metaData);
        node.lengthMetaData = new TreeMap<>(lengthMetaData);
        node.parent = null;
        node.setID(getID());

        for (final Node child : getChildren()) {
            node.addChild(child.copy());
        }
        return node;
    } // copy

    /**
     * assign values to a tree in array representation *
     */
    public void assignTo(final Node[] nodes) {
        final Node node = nodes[getNr()];
        node.height = height;
        node.labelNr = labelNr;
        node.metaDataString = metaDataString;
        node.lengthMetaDataString = lengthMetaDataString;
        node.metaData = new TreeMap<>(metaData);
        node.lengthMetaData = new TreeMap<>(lengthMetaData);
        node.parent = null;
        node.setID(getID());
        if (getLeft() != null) {
            node.setLeft(nodes[getLeft().getNr()]);
            getLeft().assignTo(nodes);
            node.getLeft().parent = node;
            if (getRight() != null) {
                node.setRight(nodes[getRight().getNr()]);
                getRight().assignTo(nodes);
                node.getRight().parent = node;
            }
        }
    }

    /**
     * assign values from a tree in array representation *
     */
    public void assignFrom(final Node[] nodes, final Node node) {
        height = node.height;
        labelNr = node.labelNr;
        metaDataString = node.metaDataString;
        lengthMetaDataString = node.lengthMetaDataString;
        metaData = new TreeMap<>(node.metaData);
        lengthMetaData = new TreeMap<>(node.lengthMetaData);
        parent = null;
        setID(node.getID());
        if (node.getLeft() != null) {
            setLeft(nodes[node.getLeft().getNr()]);
            getLeft().assignFrom(nodes, node.getLeft());
            getLeft().parent = this;
            if (node.getRight() != null) {
                setRight(nodes[node.getRight().getNr()]);
                getRight().assignFrom(nodes, node.getRight());
                getRight().parent = this;
            }
        }
    }

    /**
     * set meta-data according to pattern.
     * Only heights are recognised, but derived classes could deal with
     * richer meta data patterns.
     */
    public void setMetaData(final String pattern, final Object value) {
        startEditing();
        if (pattern.equals(TraitSet.DATE_TRAIT) ||
                pattern.equals(TraitSet.DATE_FORWARD_TRAIT) ||
                pattern.equals(TraitSet.DATE_BACKWARD_TRAIT)) {
            height = (Double) value;
            isDirty |= Tree.IS_DIRTY;
        } else {
            metaData.put(pattern, value);
        }
    }

    /**
     * Removes metadata from the node for the given key.
     */
    public void removeMetaData(final String key) {
        metaData.remove(key);
    }

    /**
     * Add edge length metadata with given key and value.
     *
     * @param key key for metadata
     * @param value value of metadata for this edge length
     */
    public void setLengthMetaData(String key, Object value) {
        startEditing();
        lengthMetaData.put(key, value);
    }

    public Object getMetaData(final String pattern) {
        if (pattern.equals(TraitSet.DATE_TRAIT) ||
                pattern.equals(TraitSet.DATE_FORWARD_TRAIT) ||
                pattern.equals(TraitSet.DATE_BACKWARD_TRAIT)) {
            return height;
        } else {
            final Object d = metaData.get(pattern);
            if (d != null) return d;
        }
        return 0;
    }

    public Object getLengthMetaData(String key) {
        return lengthMetaData.get(key);
    }

    public Set<String> getMetaDataNames() {
        return metaData.keySet();
    }

    public Set<String> getLengthMetaDataNames() {
        return lengthMetaData.keySet();
    }


    /**
     * scale height of this node and all its descendants
     *
     * @param scale scale factor
     * @return degrees of freedom scaled (used for HR calculations)
     */
    public int scale(final double scale) {
        startEditing();

        int dof = 0;

        isDirty |= Tree.IS_DIRTY;
        if (!isLeaf() && !isFake()) {
            height *= scale;

            if (isRoot() || parent.getHeight() != getHeight())
                dof += 1;
        }
        if (!isLeaf()) {
            dof += getLeft().scale(scale);
            if (getRight() != null) {
                dof += getRight().scale(scale);
            }
            if (height < getLeft().height || height < getRight().height) {
                throw new IllegalArgumentException("Scale gives negative branch length");
            }
        }

        return dof;
    }

//    /**
//     * Used for sampled ancestor trees
//     * Scales this node and all its descendants (either all descendants, or only non-sampled descendants)
//     *
//     * @param scale    the scalar to multiply each scaled node age by
//     * @param scaleSNodes true if sampled nodes should be scaled as well as internal nodes, false if only non-sampled
//     *                  internal nodes should be scaled.
//     */
//    public void scale(double scale, boolean scaleSNodes) {
//        startEditing();
//        isDirty |= Tree.IS_DIRTY;
//        if (scaleSNodes || (!isLeaf() && !isFake())) {
//            height *= scale;
//        }
//        if (!isLeaf()) {
//            (getLeft()).scale(scale, scaleSNodes);
//            if (getRight() != null) {
//                (getRight()).scale(scale, scaleSNodes);
//            }
//            if (height < getLeft().height || height < getRight().height) {
//                throw new IllegalArgumentException("Scale gives negative branch length");
//            }
//        }
//    }

    protected void startEditing() {
        if (m_tree != null && m_tree.getState() != null) {
            m_tree.startEditing(null);
        }
    }

    /**
     * @return the number of children of this node.
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * This method returns the i'th child, numbering starting from 0.
     * getChild(0) returns the same node as getLeft()
     * getChild(1) returns the same node as getRight()
     *
     * This method is unprotected and will throw an ArrayOutOfBoundsException if provided an index larger than getChildCount() - 1, or smaller than 0.
     *
     * @return the i'th child of this node.
     */
    public Node getChild(final int childIndex) {
        return children.get(childIndex);
    }

    /**
     * This sets the i'th child of this node. Will pad out the children with null's if getChildCount() <= childIndex.
     */
    public void setChild(final int childIndex, final Node node) {
        while (children.size() <= childIndex) {
            children.add(null);
        }
        children.set(childIndex, node);
    }

    /**
     * This sets the zero'th (left in binary trees) child of this node.
     *
     * @param leftChild new left child
     * @deprecated trees should not be assumed to be binary. One child and more than two are both valid in some models.
     */
    @Deprecated
    public void setLeft(final Node leftChild) {
        if (children.size() == 0) {
            children.add(leftChild);
        } else {
            children.set(0, leftChild);
        }
    }

    /**
     * This method returns the zero'th child (called left child in binary trees).
     * Will return null if there are no children.
     *
     * @return left child (zero'th child), or null if this node has no children.
     * @deprecated trees should not be assumed to be binary. One child and more than two are both valid in some models.
     */
    @Deprecated
    public Node getLeft() {
        if (children.size() == 0) {
            return null;
        }
        return children.get(0);
    }

    /**
     * This sets the second child (index 1, called right child in binary trees). If this node had no children then the left
     * child will be set to null after this call.
     *
     * @param rightChild new right child
     * @deprecated trees should not be assumed to be binary. One child and more than two are both valid in some models.
     */
    @Deprecated
    public void setRight(final Node rightChild) {
        switch (children.size()) {
            case 0:
                children.add(null);
            case 1:
                children.add(rightChild);
                break;
            default:
                children.set(1, rightChild);
                break;
        }
    }

    /**
     * This method returns the second child (index 1, called right child in binary trees).
     * Will return null if there are no children or only one child.
     *
     * @return right child (child 1), or null if this node has no children, or only one child.
     * @deprecated trees should not be assumed to be binary. One child and more than two are both valid in some models.
     */
    @Deprecated
    public Node getRight() {
        if (children.size() <= 1) {
            return null;
        }
        return children.get(1);
    }

    public static Node connect(final Node left, final Node right, final double h) {
        final Node n = new Node();
        n.setHeight(h);
        n.setLeft(left);
        n.setRight(right);
        left.parent = n;
        right.parent = n;
        return n;
    }

    /**
     * @return true if this leaf actually represents a direct ancestor
     * (i.e. is on the end of a zero-length branch)
     */
    public boolean isDirectAncestor() {
        return (isLeaf() && !isRoot() && this.getParent().getHeight() == this.getHeight());
    }

    /**
     * @return true if this is a "fake" internal node (i.e. one of its children is a direct ancestor)
     */
    public boolean isFake() {
        if (this.isLeaf())
            return false;
        return ((this.getLeft()).isDirectAncestor() || (this.getRight() != null && (this.getRight()).isDirectAncestor()));
    }

    public Node getDirectAncestorChild() {
        if (!this.isFake()) {
            return null;
        }
        if (this.getLeft().isDirectAncestor()) {
            return this.getLeft();
        }
        return this.getRight();
    }

    public Node getNonDirectAncestorChild(){
        if (!this.isFake()) {
            return null;
        }
        if ((this.getLeft()).isDirectAncestor()){
            return getRight();
        }
        if  ((this.getRight()).isDirectAncestor()){
            return getLeft();
        }
        return null;
    }

    public Node getFakeChild(){

        if ((this.getLeft()).isFake()){
            return getLeft();
        }
        if ((this.getRight()).isFake()){
            return getRight();
        }
        return null;
    }

	
} // class Node
