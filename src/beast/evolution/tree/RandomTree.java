/*
 * CoalescentSimulator.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.math.MathException;

import beast.core.BEASTInterface;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.StateNode;
import beast.core.StateNodeInitialiser;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.coalescent.PopulationFunction;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.ParametricDistribution;
import beast.util.HeapSort;
import beast.util.Randomizer;




@Description("This class provides the basic engine for coalescent simulation of a given demographic model over a given time period. ")
public class RandomTree extends Tree implements StateNodeInitialiser {
    final public Input<Alignment> taxaInput = new Input<>("taxa", "set of taxa to initialise tree specified by alignment");

    final public Input<PopulationFunction> populationFunctionInput = new Input<>("populationModel", "population function for generating coalescent???", Validate.REQUIRED);
    final public Input<List<MRCAPrior>> calibrationsInput = new Input<>("constraint", "specifies (monophyletic or height distribution) constraints on internal nodes", new ArrayList<>());
    final public Input<Double> rootHeightInput = new Input<>("rootHeight", "If specified the tree will be scaled to match the root height, if constraints allow this");

    // total nr of taxa
    int nrOfTaxa;

    class Bound {
        Double upper = Double.POSITIVE_INFINITY;
        Double lower = Double.NEGATIVE_INFINITY;

        @Override
		public String toString() {
            return "[" + lower + "," + upper + "]";
        }
    }

    // Location of last monophyletic clade in the lists below, which are grouped together at the start.
    // (i.e. the first isMonophyletic of the TaxonSets are monophyletic, while the remainder are not).
    int lastMonophyletic;

    // taxonSets,distributions, m_bounds and taxonSetIDs are indexed together (four values associated with this clade, a set of taxa.

    // taxon sets of clades that has a constraint of calibrations. Monophyletic constraints may be nested, and are sorted by the code to be at a
    // higher index, i.e iterating from zero up does post-order (descendants before parent).
    List<Set<String>> taxonSets;

    // list of parametric distribution constraining the MRCA of taxon sets, null if not present
    List<ParametricDistribution> distributions;

    // hard bound for the set, if any
    List<Bound> m_bounds;

    // The prior element involved, if any
    List<String> taxonSetIDs;

    List<Integer>[] children;

    Set<String> taxa;

    // number of the next internal node, used when creating new internal nodes
    int nextNodeNr;

    // used to indicate one of the MRCA constraints could not be met
    protected class ConstraintViolatedException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    @Override
    public void initAndValidate() {

        taxa = new LinkedHashSet<>();
        if (taxaInput.get() != null) {
            taxa.addAll(taxaInput.get().getTaxaNames());
        } else {
            taxa.addAll(m_taxonset.get().asStringList());
        }

        nrOfTaxa = taxa.size();

        initStateNodes();
        super.initAndValidate();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void swap(final List list, final int i, final int j) {
        final Object tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    // taxonset intersection test
//    private boolean intersects(final BitSet bitSet, final BitSet bitSet2) {
//        for (int k = bitSet.nextSetBit(0); k >= 0; k = bitSet.nextSetBit(k + 1)) {
//            if (bitSet2.get(k)) {
//                return true;
//            }
//        }
//        return false;
//    }

    // returns true if bitSet is a subset of bitSet2
//    private boolean isSubset(final BitSet bitSet, final BitSet bitSet2) {
//        boolean isSubset = true;
//        for (int k = bitSet.nextSetBit(0); isSubset && k >= 0; k = bitSet.nextSetBit(k + 1)) {
//            isSubset = bitSet2.get(k);
//        }
//        return isSubset;
//    }

    @SuppressWarnings("unchecked")
	@Override
    public void initStateNodes() {
        // find taxon sets we are dealing with
        taxonSets = new ArrayList<>();
        m_bounds = new ArrayList<>();
        distributions = new ArrayList<>();
        taxonSetIDs = new ArrayList<>();
        lastMonophyletic = 0;

        if (taxaInput.get() != null) {
            taxa.addAll(taxaInput.get().getTaxaNames());
        } else {
            taxa.addAll(m_taxonset.get().asStringList());
        }

        // pick up constraints from outputs, m_inititial input tree and output tree, if any
        List<MRCAPrior> calibrations = new ArrayList<>();
        calibrations.addAll(calibrationsInput.get());
//    	for (BEASTObject beastObject : outputs) {
//    	// pick up constraints in outputs
//		if (beastObject instanceof MRCAPrior && !calibrations.contains(beastObject)) {
//			calibrations.add((MRCAPrior) beastObject);
//		} else  if (beastObject instanceof Tree) {
//        	// pick up constraints in outputs if output tree
//			Tree tree = (Tree) beastObject;
//			if (tree.m_initial.get() == this) {
//            	for (BEASTObject beastObject2 : tree.outputs) {
//            		if (beastObject2 instanceof MRCAPrior && !calibrations.contains(beastObject2)) {
//            			calibrations.add((MRCAPrior) beastObject2);
//            		}                		
//            	}
//			}
//		}
//		
//	}
        // pick up constraints in m_initial tree
        for (final Object beastObject : getOutputs()) {
            if (beastObject instanceof MRCAPrior && !calibrations.contains(beastObject) ) {
                calibrations.add((MRCAPrior) beastObject);
            }
        }
        if (m_initial.get() != null) {
            for (final Object beastObject : m_initial.get().getOutputs()) {
                if (beastObject instanceof MRCAPrior && !calibrations.contains(beastObject)) {
                    calibrations.add((MRCAPrior) beastObject);
                }
            }
        }

        for (final MRCAPrior prior : calibrations) {
            final TaxonSet taxonSet = prior.taxonsetInput.get();
            if (taxonSet != null && !prior.onlyUseTipsInput.get()) {
	            final Set<String> usedTaxa = new LinkedHashSet<>();
	        	if (taxonSet.asStringList() == null) {
	        		taxonSet.initAndValidate();
	        	}
	            for (final String taxonID : taxonSet.asStringList()) {
	                if (!taxa.contains(taxonID)) {
	                    throw new IllegalArgumentException("Taxon <" + taxonID + "> could not be found in list of taxa. Choose one of " + taxa);
	                }
	                usedTaxa.add(taxonID);
	            }
	            final ParametricDistribution distr = prior.distInput.get();
	            final Bound bounds = new Bound();
	            if (distr != null) {
	        		List<BEASTInterface> beastObjects = new ArrayList<>();
	        		distr.getPredecessors(beastObjects);
	        		for (int i = beastObjects.size() - 1; i >= 0 ; i--) {
	        			beastObjects.get(i).initAndValidate();
	        		}
	                try {
						bounds.lower = distr.inverseCumulativeProbability(0.0) + distr.offsetInput.get();
		                bounds.upper = distr.inverseCumulativeProbability(1.0) + distr.offsetInput.get();
					} catch (MathException e) {
						Log.warning.println("At RandomTree::initStateNodes, bound on MRCAPrior could not be set " + e.getMessage());
					}
	            }
	
	            if (prior.isMonophyleticInput.get()) {
	                // add any monophyletic constraint
	                taxonSets.add(lastMonophyletic, usedTaxa);
	                distributions.add(lastMonophyletic, distr);
	                m_bounds.add(lastMonophyletic, bounds);
	                taxonSetIDs.add(prior.getID());
	                lastMonophyletic++;
	            } else {
	                // only calibrations with finite bounds are added
	                if (!Double.isInfinite(bounds.lower) || !Double.isInfinite(bounds.upper)) {
	                    taxonSets.add(usedTaxa);
	                    distributions.add(distr);
	                    m_bounds.add(bounds);
	                    taxonSetIDs.add(prior.getID());
	                }
	            }
            }
        }

        // assume all calibration constraints are MonoPhyletic
        // TODO: verify that this is a reasonable assumption
        lastMonophyletic = taxonSets.size();


        // sort constraints such that if taxon set i is subset of taxon set j, then i < j
        for (int i = 0; i < lastMonophyletic; i++) {
            for (int j = i + 1; j < lastMonophyletic; j++) {

                Set<String> intersection = new LinkedHashSet<>(taxonSets.get(i));
                intersection.retainAll(taxonSets.get(j));

                if (intersection.size() > 0) {
                    final boolean isSubset = taxonSets.get(i).containsAll(taxonSets.get(j));
                    final boolean isSubset2 = taxonSets.get(j).containsAll(taxonSets.get(i));
                    // sanity check: make sure either
                    // o taxonset1 is subset of taxonset2 OR
                    // o taxonset1 is superset of taxonset2 OR
                    // o taxonset1 does not intersect taxonset2
                    if (!(isSubset || isSubset2)) {
                        throw new IllegalArgumentException("333: Don't know how to generate a Random Tree for taxon sets that intersect, " +
                                "but are not inclusive. Taxonset " + taxonSetIDs.get(i) + " and " + taxonSetIDs.get(j));
                    }
                    // swap i & j if b1 subset of b2
                    if (isSubset) {
                        swap(taxonSets, i, j);
                        swap(distributions, i, j);
                        swap(m_bounds, i, j);
                        swap(taxonSetIDs, i, j);
                    }
                }
            }
        }

        // build tree of mono constraints such that j is parent of i if i is a subset of j but i+1,i+2,...,j-1 are not.
        // The last one, standing for the virtual "root" of all monophyletic clades is not associated with an actual clade
        final int[] parent = new int[lastMonophyletic];
        children = new List[lastMonophyletic + 1];
        for (int i = 0; i < lastMonophyletic + 1; i++) {
            children[i] = new ArrayList<>();
        }
        for (int i = 0; i < lastMonophyletic; i++) {
            int j = i + 1;
            while (j < lastMonophyletic && !taxonSets.get(j).containsAll(taxonSets.get(i))) {
                j++;
            }
            parent[i] = j;
            children[j].add(i);
        }

        // make sure upper bounds of a child does not exceed the upper bound of its parent
        for (int i = lastMonophyletic-1; i >= 0 ;--i) {
            if (parent[i] < lastMonophyletic ) {
                if (m_bounds.get(i).upper > m_bounds.get(parent[i]).upper) {
                    m_bounds.get(i).upper = m_bounds.get(parent[i]).upper - 1e-100;
                }
            }
        }


        final PopulationFunction popFunction = populationFunctionInput.get();

        simulateTree(taxa, popFunction);
        if (rootHeightInput.get() != null) {
        	scaleToFit(rootHeightInput.get() / root.getHeight(), root);
        }

        nodeCount = 2 * taxa.size() - 1;
        internalNodeCount = taxa.size() - 1;
        leafNodeCount = taxa.size();

        HashMap<String,Integer> taxonToNR = null;
        // preserve node numbers where possible
        if (m_initial.get() != null) {
            if( leafNodeCount == m_initial.get().getLeafNodeCount() ) {
                // dont ask me how the initial tree is rubbish  (i.e. 0:0.0)
                taxonToNR = new HashMap<>();
                for (Node n : m_initial.get().getExternalNodes()) {
                    taxonToNR.put(n.getID(), n.getNr());
                }
            }
        } else {
            taxonToNR = new HashMap<>();
            String[] taxa = getTaxaNames();
            for(int k = 0; k < taxa.length; ++k) {
                taxonToNR.put(taxa[k], k);
            }
        }
        // multiple simulation tries may produce an excess of nodes with invalid nr's. reset those.
        setNodesNrs(root, 0, new int[1], taxonToNR);

        initArrays();

        if (m_initial.get() != null) {
            m_initial.get().assignFromWithoutID(this);
        }
        for(int k = 0; k < lastMonophyletic; ++k) {
            final MRCAPrior p = calibrations.get(k);
            if( p.isMonophyleticInput.get() ) {
                final TaxonSet taxonSet = p.taxonsetInput.get();
                if (taxonSet == null) {
                	throw new IllegalArgumentException("Something is wrong with constraint " + p.getID() + " -- a taxonset must be specified if a monophyletic constraint is enforced.");
                }
                final Set<String> usedTaxa = new LinkedHashSet<>();
                if (taxonSet.asStringList()  == null) {
                	taxonSet.initAndValidate();
                }
                usedTaxa.addAll(taxonSet.asStringList());
                /* int c = */ traverse(root, usedTaxa, taxonSet.getTaxonCount(), new int[1]);
                // boolean b = c == nrOfTaxa + 127;
            }
        }
    }

    private int setNodesNrs(final Node node, int internalNodeCount, int[] n, Map<String,Integer> initial) {
        if( node.isLeaf() )  {
            if( initial != null ) {
                node.setNr(initial.get(node.getID()));
            } else {
                node.setNr(n[0]);
                n[0] += 1;
            }
        } else {
            for (final Node child : node.getChildren()) {
                internalNodeCount = setNodesNrs(child, internalNodeCount, n, initial);
            }
            node.setNr(nrOfTaxa + internalNodeCount);
            internalNodeCount += 1;
        }
        return internalNodeCount;
    }

    private void scaleToFit(double scale, Node node) {
        if (!node.isLeaf()) {
	    	double oldHeight = node.getHeight();
	    	node.height *= scale;
	        final Integer constraint = getDistrConstraint(node);
	        if (constraint != null) {
	            if (node.height < m_bounds.get(constraint).lower || node.height > m_bounds.get(constraint).upper) {
	            	//revert scaling
	            	node.height = oldHeight;
	            	return;
	            }
	        }
	        scaleToFit(scale, node.getLeft());
	        scaleToFit(scale, node.getRight());
	        if (node.height < Math.max(node.getLeft().getHeight(), node.getRight().getHeight())) {
	        	// this can happen if a child node is constrained and the default tree is higher than desired
	        	node.height = 1.0000001 * Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
	        }
        }
	}

	//@Override
    @Override
	public void getInitialisedStateNodes(final List<StateNode> stateNodes) {
        stateNodes.add(m_initial.get());
    }

    /**
     * Simulates a coalescent tree, given a taxon list.
     *
     * @param taxa         the set of taxa to simulate a coalescent tree between
     * @param demoFunction the demographic function to use
     */
    public void simulateTree(final Set<String> taxa, final PopulationFunction demoFunction) {
        if (taxa.size() == 0)
            return;

        String msg = "Failed to generate a random tree (probably a bug).";
        for (int attempts = 0; attempts < 1000; ++attempts) {
            try {
                nextNodeNr = nrOfTaxa;
                final Set<Node> candidates = new LinkedHashSet<>();
                int i = 0;
                for (String taxon : taxa) {
                    final Node node = newNode();
                    node.setNr(i);
                    node.setID(taxon);
                    node.setHeight(0.0);
                    candidates.add(node);
                    i += 1;
                }

                if (m_initial.get() != null) {
                    processCandidateTraits(candidates, m_initial.get().m_traitList.get());
                } else {
                    processCandidateTraits(candidates, m_traitList.get());
                }

                final Map<String,Node> allCandidates = new TreeMap<>();
                for (Node node: candidates) {
                    allCandidates.put(node.getID(),node);
                }
                root = simulateCoalescent(lastMonophyletic, allCandidates, candidates, demoFunction);
                return;
            } catch (ConstraintViolatedException e) {
                // need to generate another tree
            	msg = "\nWARNING: Generating a random tree did not succeed. The most common reasons are:\n";
            	msg += "1. there are conflicting monophyletic constraints, for example if both (A,B) \n"
            			+ "and (B,C) must be monophyletic no tree will be able to meet these constraints at the same \n"
            			+ "time. To fix this, carefully check all clade sets, especially the ones that are expected to \n"
            			+ "be nested clades.\n";
            	msg += "2. clade heights are constrained by an upper and lower bound, but the population size \n"
            			+ "is too large, so it is very unlikely a generated treed does not violate these constraints. To \n"
            			+ "fix this you can try to reduce the population size of the population model.\n";
            	msg += "Expect BEAST to crash if this is not fixed.\n"; 
            	Log.err.println(msg);
            }
        }
        throw new RuntimeException(msg);
    }
    
    /**
     * Apply traits to a set of nodes.
     * @param candidates List of nodes
     * @param traitSets List of TraitSets to apply
     */
    private void processCandidateTraits(Set<Node> candidates, List<TraitSet> traitSets) {
        for (TraitSet traitSet : traitSets) {
            for (Node node : candidates) {
                node.setMetaData(traitSet.getTraitName(), traitSet.getValue(node.getID()));
            }
        }
    }


    private Node simulateCoalescent(final int isMonophyleticNode, final Map<String,Node> allCandidates, final Set<Node> candidates, final PopulationFunction demoFunction)
            throws ConstraintViolatedException {
        final List<Node> remainingCandidates = new ArrayList<>();
        final Set<String> taxaDone = new TreeSet<>();
        for (final int monoNode : children[isMonophyleticNode]) {
            // create list of leaf nodes for this monophyletic MRCA
            final Set<Node> candidates2 = new LinkedHashSet<>();
            final Set<String> isTaxonSet = taxonSets.get(monoNode);
            for (String taxon : isTaxonSet) {
                candidates2.add(allCandidates.get(taxon));
            }

            final Node MRCA = simulateCoalescent(monoNode, allCandidates, candidates2, demoFunction);
            remainingCandidates.add(MRCA);

            taxaDone.addAll(isTaxonSet);
        }

        for (final Node node : candidates) {
            if (!taxaDone.contains(node.getID())) {
                remainingCandidates.add(node);
            }
        }

        final double upper = isMonophyleticNode < m_bounds.size() ?  m_bounds.get(isMonophyleticNode).upper : Double.POSITIVE_INFINITY;
        final Node MRCA = simulateCoalescentWithMax(remainingCandidates, demoFunction, upper);
        return MRCA;
    }

//    /**
//     * @param id the id to match
//     * @param nodes a list of nodes
//     * @return the node with the matching id;
//     */
//    private Node getNodeById(String id, List<Node> nodes) {
//        for (Node node : nodes) {
//            if (node.getID().equals(id)) return node;
//        }
//        return null;
//    }

    /**
     * @param nodes
     * @param demographic
     * @return the root node of the given array of nodes after simulation of the
     *         coalescent under the given demographic model.
     * @throws beast.evolution.tree.RandomTree.ConstraintViolatedException
     */
//    public Node simulateCoalescent(final List<Node> nodes, final PopulationFunction demographic) throws ConstraintViolatedException {
//        return simulateCoalescentWithMax(nodes, demographic, Double.POSITIVE_INFINITY);
//    }

    /**
     * @param nodes
     * @param demographic
     * @return the root node of the given array of nodes after simulation of the
     *         coalescent under the given demographic model.
     * @throws beast.evolution.tree.RandomTree.ConstraintViolatedException
     */
    public Node simulateCoalescentWithMax(final List<Node> nodes, final PopulationFunction demographic,
                                          final double maxHeight) throws ConstraintViolatedException {
        // sanity check - disjoint trees

        // if( ! Tree.Utils.allDisjoint(nodes) ) {
        // throw new RuntimeException("non disjoint trees");
        // }

        if (nodes.size() == 0) {
            throw new IllegalArgumentException("empty nodes set");
        }

        for (int attempts = 0; attempts < 1000; ++attempts) {
            final List<Node> rootNode = simulateCoalescent(nodes, demographic, 0.0, maxHeight);
            if (rootNode.size() == 1) {
                return rootNode.get(0);
            }
        }

        if( Double.isFinite(maxHeight) ){
            double h = -1;

            for( Node n : nodeList ) {
                h = Math.max(h, n.getHeight());
            }
            assert h < maxHeight;
            double dt = (maxHeight - h)/ (nodeList.size() + 1);
            while (nodeList.size() > 1) {
                int k = nodeList.size() - 1;
                final Node left = nodeList.remove(k);
                final Node right = nodeList.get(k-1);
                final Node newNode = newNode();
                newNode.setNr(nextNodeNr++);   // multiple tries may generate an excess of nodes assert(nextNodeNr <= nrOfTaxa*2-1);
                newNode.setHeight(h + dt);
                newNode.setLeft(left);
                left.setParent(newNode);
                newNode.setRight(right);
                right.setParent(newNode);
                nodeList.set(k-1, newNode);
            }
            assert (nodeList.size() == 1);
            return nodeList.get(0);
        }
        throw new RuntimeException("failed to merge trees after 1000 tries!");
    }

    public List<Node> simulateCoalescent(final List<Node> nodes, final PopulationFunction demographic, double currentHeight,
                                         final double maxHeight) throws ConstraintViolatedException {
        // If only one node, return it
        // continuing results in an infinite loop
        if (nodes.size() == 1)
            return nodes;

        final double[] heights = new double[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            heights[i] = nodes.get(i).getHeight();
        }
        final int[] indices = new int[nodes.size()];
        HeapSort.sort(heights, indices);

        // node list
        nodeList.clear();
        activeNodeCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            nodeList.add(nodes.get(indices[i]));
        }
        setCurrentHeight(currentHeight);

        // get at least two tips
        while (getActiveNodeCount() < 2) {
            currentHeight = getMinimumInactiveHeight();
            setCurrentHeight(currentHeight);
        }

        // simulate coalescent events
        double nextCoalescentHeight = currentHeight
                + PopulationFunction.Utils.getSimulatedInterval(demographic, getActiveNodeCount(), currentHeight);

        // while (nextCoalescentHeight < maxHeight && (getNodeCount() > 1)) {
        while (nextCoalescentHeight < maxHeight && (nodeList.size() > 1)) {

            if (nextCoalescentHeight >= getMinimumInactiveHeight()) {
                currentHeight = getMinimumInactiveHeight();
                setCurrentHeight(currentHeight);
            } else {
                currentHeight = coalesceTwoActiveNodes(currentHeight, nextCoalescentHeight);
            }

            // if (getNodeCount() > 1) {
            if (nodeList.size() > 1) {
                // get at least two tips
                while (getActiveNodeCount() < 2) {
                    currentHeight = getMinimumInactiveHeight();
                    setCurrentHeight(currentHeight);
                }

                // nextCoalescentHeight = currentHeight +
                // DemographicFunction.Utils.getMedianInterval(demographic,
                // getActiveNodeCount(), currentHeight);
                nextCoalescentHeight = currentHeight
                        + PopulationFunction.Utils.getSimulatedInterval(demographic, getActiveNodeCount(),
                        currentHeight);
            }
        }

        return nodeList;
    }

    /**
     * @return the height of youngest inactive node.
     */
    private double getMinimumInactiveHeight() {
        if (activeNodeCount < nodeList.size()) {
            return (nodeList.get(activeNodeCount)).getHeight();
        } else
            return Double.POSITIVE_INFINITY;
    }

    /**
     * Set the current height.
     * @param height
     */
    private void setCurrentHeight(final double height) {
        while (getMinimumInactiveHeight() <= height) {
            activeNodeCount += 1;
        }
    }

    /**
     * @return the number of active nodes (equate to lineages)
     */
    private int getActiveNodeCount() {
        return activeNodeCount;
    }

    //
    // /**
    // * @return the total number of nodes both active and inactive
    // */
    // private int getNodeCount() {
    // return nodeList.size();
    // }
    //

    /**
     * Coalesce two nodes in the active list. This method removes the two
     * (randomly selected) active nodes and replaces them with the new node at
     * the top of the active list.
     * @param minHeight
     * @param height
     * @return
     */
    private double coalesceTwoActiveNodes(final double minHeight, double height) throws ConstraintViolatedException {
        final int node1 = Randomizer.nextInt(activeNodeCount);
        int node2 = node1;
        while (node2 == node1) {
            node2 = Randomizer.nextInt(activeNodeCount);
        }

        final Node left = nodeList.get(node1);
        final Node right = nodeList.get(node2);

        final Node newNode = newNode();
//		System.err.println(2 * m_taxa.get().getNrTaxa() - nodeList.size());
        newNode.setNr(nextNodeNr++);   // multiple tries may generate an excess of nodes assert(nextNodeNr <= nrOfTaxa*2-1);
        newNode.setHeight(height);
        newNode.setLeft(left);
        left.setParent(newNode);
        newNode.setRight(right);
        right.setParent(newNode);

        nodeList.remove(left);
        nodeList.remove(right);

        activeNodeCount -= 2;

        nodeList.add(activeNodeCount, newNode);

        activeNodeCount += 1;

        // check if there is a calibration on this node
        final Integer constraint = getDistrConstraint(newNode);
        if (constraint != null) {
//			for (int i = 0; i < 1000; i++) {
//				try {
//					height = distr.sample(1)[0][0];
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				if (height > minHeight) {
//					break;
//				}
//			} 
            final double min = Math.max(m_bounds.get(constraint).lower, minHeight);
            final double max = m_bounds.get(constraint).upper;
            if (max < min) {
                // failed to draw a matching height from the MRCA distribution
                // TODO: try to scale rest of tree down
                throw new ConstraintViolatedException();
            }
            if (height < min || height > max) {
            	if (max == Double.POSITIVE_INFINITY) {
            		height = min + 0.1;
            	} else {
            		height = min + Randomizer.nextDouble() * (max - min);
            	}
                newNode.setHeight(height);
            }
        }


        if (getMinimumInactiveHeight() < height) {
            throw new RuntimeException(
                    "This should never happen! Somehow the current active node is older than the next inactive node!\n"
            		+ "One possible solution you can try is to increase the population size of the population model.");
        }
        return height;
    }

    private Integer getDistrConstraint(final Node node) {
        for (int i = 0; i < distributions.size(); i++) {
            if (distributions.get(i) != null) {
                final Set<String> taxonSet = taxonSets.get(i);
                if (traverse(node, taxonSet, taxonSet.size(), new int[1]) == nrOfTaxa + 127) {
                    return i;
                }
            }
        }
        return null;
    }

    int traverse(final Node node, final Set<String> MRCATaxonSet, final int nrOfMRCATaxa, final int[] taxonCount) {
        if (node.isLeaf()) {
            taxonCount[0]++;
            if (MRCATaxonSet.contains(node.getID())) {
                return 1;
            } else {
                return 0;
            }
        } else {
            int taxons = traverse(node.getLeft(), MRCATaxonSet, nrOfMRCATaxa, taxonCount);
            final int leftTaxa = taxonCount[0];
            taxonCount[0] = 0;
            if (node.getRight() != null) {
                taxons += traverse(node.getRight(), MRCATaxonSet, nrOfMRCATaxa, taxonCount);
                final int rightTaxa = taxonCount[0];
                taxonCount[0] = leftTaxa + rightTaxa;
            }
            if (taxons == nrOfTaxa + 127) {
                taxons++;
            }
            if (taxons == nrOfMRCATaxa) {
                // we are at the MRCA, return magic nr
                return nrOfTaxa + 127;
            }
            return taxons;
        }
    }


    @Override
    public String[] getTaxaNames() {
        if (m_sTaxaNames == null) {
            final List<String> taxa;
            if (taxaInput.get() != null) {
                taxa = taxaInput.get().getTaxaNames();
            } else {
                taxa = m_taxonset.get().asStringList();
            }
            m_sTaxaNames = taxa.toArray(new String[taxa.size()]);
        }
        return m_sTaxaNames;
    }

    final private ArrayList<Node> nodeList = new ArrayList<>();
    private int activeNodeCount = 0;


}