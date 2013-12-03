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


import java.util.*;

import beast.core.Description;
import beast.core.Input;
import beast.core.StateNode;
import beast.core.StateNodeInitialiser;
import beast.core.BEASTObject;
import beast.core.Input.Validate;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.coalescent.PopulationFunction;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.ParametricDistribution;
import beast.util.HeapSort;
import beast.util.Randomizer;





@Description("This class provides the basic engine for coalescent simulation of a given demographic model over a given time period. ")
public class RandomTree extends Tree implements StateNodeInitialiser {
    public Input<Alignment> taxaInput = new Input<Alignment>("taxa", "set of taxa to initialise tree with specified by alignment");
    //public Input<TaxonSet> m_taxonset = new Input<TaxonSet>("taxonset","set of taxa to initialise tree with specified by a taxonset", Validate.XOR, m_taxa);
    public Input<PopulationFunction> populationFunctionInput = new Input<PopulationFunction>("populationModel", "population function for generating coalescent???", Validate.REQUIRED);
    public Input<List<MRCAPrior>> calibrationsInput = new Input<List<MRCAPrior>>("constraint", "specifies (monophyletic or height distribution) constraints on internal nodes", new ArrayList<MRCAPrior>());
    public Input<Double> rootHeightInput = new Input<Double>("rootHeight", "If specified the tree will be scaled to match the root height, if constraints allow this");

    // total nr of taxa
    int nrOfTaxa;
    // list of bitset representation of the taxon sets
    List<BitSet> taxonSets;
    // the first m_nIsMonophyletic of the m_bTaxonSets are monophyletic, while the remainder are not
    int isMonophyletic;
    // list of parametric distribution constraining the MRCA of taxon sets, null if not present
    List<ParametricDistribution> distributions;

    class Bound {
        Double upper = Double.POSITIVE_INFINITY;
        Double lower = Double.NEGATIVE_INFINITY;

        public String toString() {
            return "[" + lower + "," + upper + "]";
        }
    }

    List<Bound> m_bounds;
    List<String> taxonSetIDs;

    List<Integer>[] children;

    // number of the next internal node, used when creating new internal nodes
    int nextNodeNr;

    // used to indicate one of the MRCA constraints could not be met
    protected class ConstraintViolatedException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    @Override
    public void initAndValidate() throws Exception {
        final List<String> sTaxa;
        if (taxaInput.get() != null) {
            sTaxa = taxaInput.get().getTaxaNames();
        } else {
            sTaxa = m_taxonset.get().asStringList();
        }
        nrOfTaxa = sTaxa.size();

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
    private boolean intersects(final BitSet bitSet, final BitSet bitSet2) {
        for (int k = bitSet.nextSetBit(0); k >= 0; k = bitSet.nextSetBit(k + 1)) {
            if (bitSet2.get(k)) {
                return true;
            }
        }
        return false;
    }

    // taxonset subset test
    private boolean isSubset(final BitSet bitSet, final BitSet bitSet2) {
        boolean bIsSubset = true;
        for (int k = bitSet.nextSetBit(0); bIsSubset && k >= 0; k = bitSet.nextSetBit(k + 1)) {
            bIsSubset = bitSet2.get(k);
        }
        return bIsSubset;
    }

    //@Override
    public void initStateNodes() throws Exception {
        // find taxon sets we are dealing with
        taxonSets = new ArrayList<BitSet>();
        m_bounds = new ArrayList<Bound>();
        distributions = new ArrayList<ParametricDistribution>();
        taxonSetIDs = new ArrayList<String>();
        isMonophyletic = 0;

        final List<String> sTaxa;
        if (taxaInput.get() != null) {
            sTaxa = taxaInput.get().getTaxaNames();
        } else {
            sTaxa = m_taxonset.get().asStringList();
        }

        // pick up constraints from outputs, m_inititial input tree and output tree, if any
        List<MRCAPrior> calibrations = new ArrayList<MRCAPrior>();
        calibrations.addAll(calibrationsInput.get());
//    	for (Plugin plugin : outputs) {
//    	// pick up constraints in outputs
//		if (plugin instanceof MRCAPrior && !calibrations.contains(plugin)) {
//			calibrations.add((MRCAPrior) plugin);
//		} else  if (plugin instanceof Tree) {
//        	// pick up constraints in outputs if output tree
//			Tree tree = (Tree) plugin;
//			if (tree.m_initial.get() == this) {
//            	for (Plugin plugin2 : tree.outputs) {
//            		if (plugin2 instanceof MRCAPrior && !calibrations.contains(plugin2)) {
//            			calibrations.add((MRCAPrior) plugin2);
//            		}                		
//            	}
//			}
//		}
//		
//	}
        // pick up constraints in m_initial tree
        for (final BEASTObject plugin : outputs) {
            if (plugin instanceof MRCAPrior && !calibrations.contains(plugin) ) {
                calibrations.add((MRCAPrior) plugin);
            }
        }
        if (m_initial.get() != null) {
            for (final BEASTObject plugin : m_initial.get().outputs) {
                if (plugin instanceof MRCAPrior && !calibrations.contains(plugin) ) {
                    calibrations.add((MRCAPrior) plugin);
                }
            }
        }


        for (final MRCAPrior prior : calibrations) {
            final TaxonSet taxonSet = prior.taxonsetInput.get();
            if (taxonSet != null && !prior.onlyUseTipsInput.get()) {
	            final BitSet bTaxa = new BitSet(nrOfTaxa);
	        	if (taxonSet.asStringList() == null) {
	        		taxonSet.initAndValidate();
	        	}
	            for (final String sTaxonID : taxonSet.asStringList()) {
	                final int iID = sTaxa.indexOf(sTaxonID);
	                if (iID < 0) {
	                    throw new Exception("Taxon <" + sTaxonID + "> could not be found in list of taxa. Choose one of " + sTaxa.toArray(new String[0]));
	                }
	                bTaxa.set(iID);
	            }
	            final ParametricDistribution distr = prior.distInput.get();
	            final Bound bounds = new Bound();
	            if (distr != null) {
	        		List<BEASTObject> plugins = new ArrayList<BEASTObject>();
	        		distr.getPredecessors(plugins);
	        		for (int i = plugins.size() - 1; i >= 0 ; i--) {
	        			plugins.get(i).initAndValidate();
	        		}
	                bounds.lower = distr.inverseCumulativeProbability(0.0) + distr.offsetInput.get();
	                bounds.upper = distr.inverseCumulativeProbability(1.0) + distr.offsetInput.get();
	            }
	
	            if (prior.isMonophyleticInput.get()) {
	                // add any monophyletic constraint
	                taxonSets.add(isMonophyletic, bTaxa);
	                distributions.add(isMonophyletic, distr);
	                m_bounds.add(isMonophyletic, bounds);
	                taxonSetIDs.add(prior.getID());
	                isMonophyletic++;
	            } else {
	                // only calibrations with finite bounds are added
	                if (!Double.isInfinite(bounds.lower) || !Double.isInfinite(bounds.upper)) {
	                    taxonSets.add(bTaxa);
	                    distributions.add(distr);
	                    m_bounds.add(bounds);
	                    taxonSetIDs.add(prior.getID());
	                }
	            }
            }
        }

        // assume all calibration constraints are MonoPhyletic
        // TODO: verify that this is a reasonable assumption
        isMonophyletic = taxonSets.size();


        // sort constraints such that if taxon set i is subset of taxon set j, then i < j
        for (int i = 0; i < isMonophyletic; i++) {
            for (int j = i + 1; j < isMonophyletic; j++) {
                final boolean bIntersects = intersects(taxonSets.get(i), taxonSets.get(j));
                if (bIntersects) {
                    final boolean bIsSubset = isSubset(taxonSets.get(j), taxonSets.get(i));
                    final boolean bIsSubset2 = isSubset(taxonSets.get(i), taxonSets.get(j));
                    // sanity check: make sure either
                    // o taxonset1 is subset of taxonset2 OR
                    // o taxonset1 is superset of taxonset2 OR
                    // o taxonset1 does not intersect taxonset2
                    if (!(bIsSubset || bIsSubset2)) {
                        throw new Exception("333: Don't know how to generate a Random Tree for taxon sets that intersect, " +
                                "but are not inclusive. Taxonset " + taxonSetIDs.get(i) + " and " + taxonSetIDs.get(j));
                    }
                    // swap i & j if b1 subset of b2
                    if (bIsSubset) {
                        swap(taxonSets, i, j);
                        swap(distributions, i, j);
                        swap(m_bounds, i, j);
                        swap(taxonSetIDs, i, j);
                    }
                }
            }
        }

        // build tree of mono constraints such that i is parent of j => j is subset of i
        final int[] nParent = new int[isMonophyletic];
        children = new List[isMonophyletic + 1];
        for (int i = 0; i < isMonophyletic + 1; i++) {
            children[i] = new ArrayList<Integer>();
        }
        for (int i = 0; i < isMonophyletic; i++) {
            int j = i + 1;
            while (j < isMonophyletic && !isSubset(taxonSets.get(i), taxonSets.get(j))) {
                j++;
            }
            nParent[i] = j;
            children[j].add(i);
        }

        // make sure upper bounds of a child does not exceed the upper bound of its parent
        for (int i = 0; i < isMonophyletic; i++) {
            if (nParent[i] < isMonophyletic) {
                if (m_bounds.get(i).upper > m_bounds.get(nParent[i]).upper) {
                    m_bounds.get(i).upper = m_bounds.get(nParent[i]).upper - 1e-100;
                }
            }
        }


        final PopulationFunction popFunction = populationFunctionInput.get();

        simulateTree(sTaxa, popFunction);
        if (rootHeightInput.get() != null) {
        	scaleToFit(rootHeightInput.get() / root.getHeight(), root);
        }

        nodeCount = 2 * sTaxa.size() - 1;
        internalNodeCount = sTaxa.size() - 1;
        leafNodeCount = sTaxa.size();
        initArrays();

        if (m_initial.get() != null) {
            m_initial.get().assignFromWithoutID(this);
        }
    }

    private void scaleToFit(double scale, Node node) {
        if (!node.isLeaf()) {
	    	double oldHeight = node.getHeight();
	    	node.height *= scale;
	        final Integer iConstraint = getDistrConstraint(node);
	        if (iConstraint != null) {
	            if (node.height < m_bounds.get(iConstraint).lower || node.height > m_bounds.get(iConstraint).upper) {
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
    public void getInitialisedStateNodes(final List<StateNode> stateNodes) {
        stateNodes.add(m_initial.get());
    }

    /**
     * Simulates a coalescent tree, given a taxon list.
     *
     * @param taxa         the set of taxa to simulate a coalescent tree between
     * @param demoFunction the demographic function to use
     */
    public void simulateTree(final List<String> taxa, final PopulationFunction demoFunction) {
        if (taxa.size() == 0)
            return;

        for (int attempts = 0; attempts < 1000; ++attempts) {
            try {
                nextNodeNr = nrOfTaxa;
                final List<Node> candidates = new ArrayList<Node>();
                for (int i = 0; i < taxa.size(); i++) {
                    final Node node = new Node();
                    node.setNr(i);
                    node.setID(taxa.get(i));
                    node.setHeight(0.0);
                    candidates.add(node);
                }

                if (m_initial.get() != null)
                    processCandidateTraits(candidates, m_initial.get().m_traitList.get());
                else
                    processCandidateTraits(candidates, m_traitList.get());

                // TODO: deal with dated taxa
                double fMostRecent = 0;
                for (final Node node : candidates) {
                    fMostRecent = Math.max(fMostRecent, node.getHeight());
                }
                // dr.evolution.util.Date mostRecent = null;
                // boolean usingDates = false;
                //
                // for (int i = 0; i < taxa.size(); i++) {
                // if (TaxonList.Utils.hasAttribute(taxa, i,
                // dr.evolution.util.Date.DATE)) {
                // usingDates = true;
                // dr.evolution.util.Date date =
                // (dr.evolution.util.Date)taxa.getTaxonAttribute(i,
                // dr.evolution.util.Date.DATE);
                // if ((date != null) && (mostRecent == null || date.after(mostRecent)))
                // {
                // mostRecent = date;
                // }
                // } else {
                // // assume contemporaneous tips
                // candidates.get(i).setHeight(0.0);
                // }
                // }
                //
                // if (usingDates && mostRecent != null ) {
                // TimeScale timeScale = new TimeScale(mostRecent.getUnits(), true,
                // mostRecent.getAbsoluteTimeValue());
                //
                // for (int i =0; i < taxa.size(); i++) {
                // dr.evolution.util.Date date =
                // (dr.evolution.util.Date)taxa.getTaxonAttribute(i,
                // dr.evolution.util.Date.DATE);
                // if (date == null) {
                // throw new IllegalArgumentException("Taxon, " + taxa.get(i) +
                // ", is missing its date");
                // }
                //
                // candidates.get(i).setHeight(timeScale.convertTime(date.getTimeValue(),
                // date));
                // }
                // if (demoFunction.getUnits() != mostRecent.getUnits()) {
                // //throw new
                // IllegalArgumentException("The units of the demographic model and the most recent date must match!");
                // }
                // }

                final List<Node> allCandidates = new ArrayList<Node>();
                allCandidates.addAll(candidates);
                root = simulateCoalescent(isMonophyletic, allCandidates, candidates, demoFunction);
                return;
            } catch (ConstraintViolatedException e) {
                // need to generate another tree
            }
        }
    }
    
    /**
     * Apply traits to a set of nodes.
     * @param candidates List of nodes
     * @param traitSets List of TraitSets to apply
     */
    private void processCandidateTraits(List<Node> candidates, List<TraitSet> traitSets) {
        for (TraitSet traitSet : traitSets) {
            for (Node node : candidates)
                node.setMetaData(traitSet.getTraitName(), traitSet.getValue(node.getNr()));
        }
    }


    private Node simulateCoalescent(final int iIsMonophyleticNode, final List<Node> allCandidates, final List<Node> candidates, final PopulationFunction demoFunction)
            throws ConstraintViolatedException {
        final List<Node> remainingCandidates = new ArrayList<Node>();
        final BitSet taxaDone = new BitSet(nrOfTaxa);
        for (final int iMonoNode : children[iIsMonophyleticNode]) {
            // create list of leaf nodes for this monophyletic MRCA
            final List<Node> candidates2 = new ArrayList<Node>();
            final BitSet bTaxonSet = taxonSets.get(iMonoNode);
            for (int k = bTaxonSet.nextSetBit(0); k >= 0; k = bTaxonSet.nextSetBit(k + 1)) {
                candidates2.add(allCandidates.get(k));
            }

            final Node MRCA = simulateCoalescent(iMonoNode, allCandidates, candidates2, demoFunction);
            remainingCandidates.add(MRCA);
            taxaDone.or(bTaxonSet);
        }

        for (final Node node : candidates) {
            if (!taxaDone.get(node.getNr())) {
                remainingCandidates.add(node);
            }
        }

        final Node MRCA = simulateCoalescent(remainingCandidates, demoFunction);
        return MRCA;
    }

    /**
     * @param nodes
     * @param demographic
     * @return the root node of the given array of nodes after simulation of the
     *         coalescent under the given demographic model.
     * @throws beast.evolution.tree.RandomTree.ConstraintViolatedException
     */
    public Node simulateCoalescent(final List<Node> nodes, final PopulationFunction demographic) throws ConstraintViolatedException {
        // sanity check - disjoint trees

        // if( ! Tree.Utils.allDisjoint(nodes) ) {
        // throw new RuntimeException("non disjoint trees");
        // }

        if (nodes.size() == 0) {
            throw new IllegalArgumentException("empty nodes set");
        }

        for (int attempts = 0; attempts < 1000; ++attempts) {
            final List<Node> rootNode = simulateCoalescent(nodes, demographic, 0.0, Double.POSITIVE_INFINITY);
            if (rootNode.size() == 1) {
                return rootNode.get(0);
            }
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

        // Node[] nodesLeft = new Node[nodeList.size()];
        // for (int i = 0; i < nodesLeft.length; i++) {
        // nodesLeft[i] = nodeList.get(i);
        // }
        //
        // return nodesLeft;
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
     * @return the numver of active nodes (equate to lineages)
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
     * @param fMinHeight
     * @param height
     * @return
     */
    private double coalesceTwoActiveNodes(final double fMinHeight, double height) throws ConstraintViolatedException {
        final int node1 = Randomizer.nextInt(activeNodeCount);
        int node2 = node1;
        while (node2 == node1) {
            node2 = Randomizer.nextInt(activeNodeCount);
        }

        final Node left = nodeList.get(node1);
        final Node right = nodeList.get(node2);

        final Node newNode = new Node();
//		System.err.println(2 * m_taxa.get().getNrTaxa() - nodeList.size());
        newNode.setNr(nextNodeNr++);
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
        final Integer iConstraint = getDistrConstraint(newNode);
        if (iConstraint != null) {
//			for (int i = 0; i < 1000; i++) {
//				try {
//					height = distr.sample(1)[0][0];
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				if (height > fMinHeight) {
//					break;
//				}
//			} 
            final double fMin = Math.max(m_bounds.get(iConstraint).lower, fMinHeight);
            final double fMax = m_bounds.get(iConstraint).upper;
            if (fMax < fMin) {
                // failed to draw a matching height from the MRCA distribution
                // TODO: try to scale rest of tree down
                throw new ConstraintViolatedException();
            }
            if (height < fMin || height > fMax) {
            	if (fMax == Double.POSITIVE_INFINITY) {
            		height = fMin + 0.1;
            	} else {
            		height = fMin + Randomizer.nextDouble() * (fMax - fMin);
            	}
                newNode.setHeight(height);
            }
        }


        if (getMinimumInactiveHeight() < height) {
            throw new RuntimeException(
                    "This should never happen! Somehow the current active node is older than the next inactive node!");
        }
        return height;
    }

    private Integer getDistrConstraint(final Node node) {
        for (int i = 0; i < distributions.size(); i++) {
            if (distributions.get(i) != null) {
                final BitSet taxonSet = taxonSets.get(i);
                if (traverse(node, taxonSet, taxonSet.cardinality(), new int[1]) == nrOfTaxa + 127) {
                    return i;
                }
            }
        }
        return null;
    }

    int traverse(final Node node, final BitSet MRCATaxonSet, final int nNrOfMRCATaxa, final int[] nTaxonCount) {
        if (node.isLeaf()) {
            nTaxonCount[0]++;
            if (MRCATaxonSet.get(node.getNr())) {
                return 1;
            } else {
                return 0;
            }
        } else {
            int iTaxons = traverse(node.getLeft(), MRCATaxonSet, nNrOfMRCATaxa, nTaxonCount);
            final int nLeftTaxa = nTaxonCount[0];
            nTaxonCount[0] = 0;
            if (node.getRight() != null) {
                iTaxons += traverse(node.getRight(), MRCATaxonSet, nNrOfMRCATaxa, nTaxonCount);
                final int nRightTaxa = nTaxonCount[0];
                nTaxonCount[0] = nLeftTaxa + nRightTaxa;
            }
            if (iTaxons == nrOfTaxa + 127) {
                iTaxons++;
            }
            if (iTaxons == nNrOfMRCATaxa) {
                // we are at the MRCA, return magic nr
                return nrOfTaxa + 127;
            }
            return iTaxons;
        }
    }


    @Override
    public String[] getTaxaNames() {
        if (m_sTaxaNames == null) {
            final List<String> sTaxa;
            if (taxaInput.get() != null) {
                sTaxa = taxaInput.get().getTaxaNames();
            } else {
                sTaxa = m_taxonset.get().asStringList();
            }
            m_sTaxaNames = sTaxa.toArray(new String[sTaxa.size()]);
        }
        return m_sTaxaNames;
    }

    final private ArrayList<Node> nodeList = new ArrayList<Node>();
    private int activeNodeCount = 0;


}