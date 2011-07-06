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

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.StateNode;
import beast.core.StateNodeInitialiser;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.coalescent.PopulationFunction;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.ParametricDistribution;
import beast.util.HeapSort;
import beast.util.Randomizer;

import java.util.*;

@Description("This class provides the basic engine for coalescent simulation of a given demographic model over a given time period. "
		+ "(Does not deal with serial taxa yet)")
public class RandomTree extends Tree implements StateNodeInitialiser {
    public Input<Alignment> m_taxa = new Input<Alignment>("taxa","set of taxa to initialise tree with specified by alignment");
    public Input<TaxonSet> m_taxonset = new Input<TaxonSet>("taxonset","set of taxa to initialise tree with specified by a taxonset", Validate.XOR, m_taxa);
    public Input<PopulationFunction> m_populationFunction = new Input<PopulationFunction>("populationModel","population function for generating coalescent???", Validate.REQUIRED);
    public Input<List<MRCAPrior>> m_calibrations = new Input<List<MRCAPrior>>("constraint", "specifies (monophyletic or height distribution) constraints on internal nodes", new ArrayList<MRCAPrior>());

    // total nr of taxa
    int m_nTaxa;
    // list of bitset representation of the taxon sets
    List<BitSet> m_bTaxonSets;
    // the first m_nIsMonophyletic of the m_bTaxonSets are monophyletic, while the remainder are not
    int m_nIsMonophyletic;
    // list of parametric distribution constraining the MRCA of taxon sets, null if not present
    List<ParametricDistribution> m_distributions;
    
    List<Integer> [] m_children;

    // number of the next internal node, used when creating new internal nodes
    int m_nNextNodeNr;
    
    @Override
    public void initAndValidate() throws Exception {
        List<String> sTaxa;
        if (m_taxa.get() != null) {
                sTaxa = m_taxa.get().getTaxaNames();
        } else {
                sTaxa = m_taxonset.get().asStringList();
        }
        m_nTaxa = sTaxa.size();

        // find taxon sets we are dealing with
        m_bTaxonSets = new ArrayList<BitSet>();
        m_distributions = new ArrayList<ParametricDistribution>();
        m_nIsMonophyletic = 0;
        for (MRCAPrior prior : m_calibrations.get()) {
    		TaxonSet taxonSet = prior.m_taxonset.get();
    		BitSet bTaxa = new BitSet(m_nTaxa);
    		for (String sTaxonID : taxonSet.asStringList()) {
    			int iID = sTaxa.indexOf(sTaxonID);
    			if (iID < 0) {
    				throw new Exception("Taxon <" + sTaxonID + "> could not be found in list of taxa. Choose one of " + sTaxa.toArray(new String[0]));
    			}
    			bTaxa.set(iID);
    		}
    		if (prior.m_bIsMonophyleticInput.get()) {
    			m_bTaxonSets.add(m_nIsMonophyletic, bTaxa);
    			m_distributions.add(m_nIsMonophyletic, prior.m_distInput.get());
    			m_nIsMonophyletic++;
    		} else {
        		m_bTaxonSets.add(bTaxa);
        		m_distributions.add(prior.m_distInput.get());
    		}
    	}
    	
    	// sort constraints such that if taxon set i is subset of taxon set j, then i < j
        for (int i = 0; i < m_nIsMonophyletic; i++) {
        	for (int j = i + 1; j < m_nIsMonophyletic; j++) {
        		boolean bIsSubset = isSubset(m_bTaxonSets.get(i), m_bTaxonSets.get(j));
    			// swap i & j if b1 subset of b2
        		if (bIsSubset) {
        			BitSet bTmp = m_bTaxonSets.get(i);
        			m_bTaxonSets.set(i, m_bTaxonSets.get(j));
        			m_bTaxonSets.set(j, bTmp);
        			ParametricDistribution tmp = m_distributions.get(i);
        			m_distributions.set(i, m_distributions.get(j));
        			m_distributions.set(j, tmp);
        		}
			}
        }
        
    	// build tree of mono constraints such that i is parent of j => j is subset of i
//        int [] nParent = new int[m_nIsMonophyletic];
        m_children = new List[m_nIsMonophyletic+1];
        for (int i = 0; i < m_nIsMonophyletic+1; i++) {
        	m_children[i] = new ArrayList<Integer>();
        }        
        for (int i = 0; i < m_nIsMonophyletic; i++) {
        	int j = i+1;
        	while (j < m_nIsMonophyletic && !isSubset(m_bTaxonSets.get(i), m_bTaxonSets.get(j))) {
        		j++;
        	}
//        	nParent[i] = j;
        	m_children[j].add(i);
        }
        
        initStateNodes();
        super.initAndValidate();
    }

	// taxonset subset test
    private boolean isSubset(BitSet bitSet, BitSet bitSet2) {
		boolean bIsSubset = true;
		for (int k = bitSet.nextSetBit(0); bIsSubset && k >= 0; k = bitSet.nextSetBit(k+1)) {
			bIsSubset = bitSet2.get(k);
		}
		return bIsSubset;
	}

	@Override
    public void initStateNodes() {
            List<String> sTaxa;
            if (m_taxa.get() != null) {
                    sTaxa = m_taxa.get().getTaxaNames();
            } else {
                    sTaxa = m_taxonset.get().asStringList();
            }
            PopulationFunction popFunction = m_populationFunction.get();

            m_nNextNodeNr = m_nTaxa;
            simulateTree(sTaxa, popFunction);

            nodeCount = 2*sTaxa.size()-1;
            internalNodeCount = sTaxa.size()-1;
            leafNodeCount = sTaxa.size();
            initArrays();
            
            if (m_initial.get() != null) {
                    m_initial.get().assignFromWithoutID(this);
            }
    }

    @Override
    public List<StateNode> getInitialisedStateNodes() {
            List<StateNode> stateNodes = new ArrayList<StateNode>();
            stateNodes.add(m_initial.get());
            return stateNodes;
	}

	/**
	 * Simulates a coalescent tree, given a taxon list.
	 * 
	 * @param taxa
	 *            the set of taxa to simulate a coalescent tree between
	 * @param demoFunction
	 *            the demographic function to use
	 */
	public void simulateTree(List<String> taxa, PopulationFunction demoFunction) {
		if (taxa.size() == 0)
			return;

		List<Node> candidates = new ArrayList<Node>();
		for (int i = 0; i < taxa.size(); i++) {
			Node node = new Node();
			node.setNr(i);
			node.setID(taxa.get(i));
			node.setHeight(0.0);
			candidates.add(node);
		}

		if (m_trait.get() != null) {
			// set tip dates
			for (Node node : candidates) {
				node.setMetaData(m_trait.get().getTraitName(), m_trait.get().getValue(node.getNr()));
			}
		}

		// TODO: deal with dated taxa
		double fMostRecent = 0;
		for (Node node : candidates) {
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

		List<Node> allCandidates = new ArrayList<Node>();
		allCandidates.addAll(candidates);
		root = simulateCoalescent(m_nIsMonophyletic, allCandidates, candidates, demoFunction);
	}

	private Node simulateCoalescent(int iIsMonophyleticNode, List<Node> allCandidates, List<Node> candidates, PopulationFunction demoFunction) {
		List<Node> remainingCandidates = new ArrayList<Node>();
		BitSet taxaDone = new BitSet(m_nTaxa);
		for (int iMonoNode : m_children[iIsMonophyleticNode]) {
			// create list of leaf nodes for this monophyletic MRCA
			List<Node> candidates2 = new ArrayList<Node>();
			BitSet bTaxonSet = m_bTaxonSets.get(iMonoNode);
			for (int k = bTaxonSet.nextSetBit(0); k >=0; k = bTaxonSet.nextSetBit(k+1)) {
				candidates2.add(allCandidates.get(k));
			}
			
			Node MRCA = simulateCoalescent(iMonoNode, allCandidates, candidates2, demoFunction);
			remainingCandidates.add(MRCA);
			taxaDone.or(bTaxonSet);
		}
		
		for (Node node : candidates) {
			if (!taxaDone.get(node.getNr())) {
				remainingCandidates.add(node);
			}
		}
		
		Node MRCA = simulateCoalescent(remainingCandidates, demoFunction);
		return MRCA;
	}

	/**
	 * @return the root node of the given array of nodes after simulation of the
	 *         coalescent under the given demographic model.
	 */
	public Node simulateCoalescent(List<Node> nodes, PopulationFunction demographic) {
		// sanity check - disjoint trees

		// if( ! Tree.Utils.allDisjoint(nodes) ) {
		// throw new RuntimeException("non disjoint trees");
		// }

		if (nodes.size() == 0) {
			throw new IllegalArgumentException("empty nodes set");
		}

		for (int attempts = 0; attempts < 1000; ++attempts) {
			List<Node> rootNode = simulateCoalescent(nodes, demographic, 0.0, Double.POSITIVE_INFINITY);
			if (rootNode.size() == 1) {
				return rootNode.get(0);
			}
		}

		throw new RuntimeException("failed to merge trees after 1000 tries!");
	}

	public List<Node> simulateCoalescent(List<Node> nodes, PopulationFunction demographic, double currentHeight,
			double maxHeight) {
		// If only one node, return it
		// continuing results in an infinite loop
		if (nodes.size() == 1)
			return nodes;

		double[] heights = new double[nodes.size()];
		for (int i = 0; i < nodes.size(); i++) {
			heights[i] = nodes.get(i).getHeight();
		}
		int[] indices = new int[nodes.size()];
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
				currentHeight = nextCoalescentHeight;
				coalesceTwoActiveNodes(currentHeight);
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
	 */
	private void setCurrentHeight(double height) {
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
	 */
	private void coalesceTwoActiveNodes(double height) {
		int node1 = Randomizer.nextInt(activeNodeCount);
		int node2 = node1;
		while (node2 == node1) {
			node2 = Randomizer.nextInt(activeNodeCount);
		}

		Node left = nodeList.get(node1);
		Node right = nodeList.get(node2);

		Node newNode = new Node();
//		System.err.println(2 * m_taxa.get().getNrTaxa() - nodeList.size());
		newNode.setNr(m_nNextNodeNr++);
		newNode.setHeight(height);
		newNode.m_left = left;
		left.setParent(newNode);
		newNode.m_right = right;
		right.setParent(newNode);

		nodeList.remove(left);
		nodeList.remove(right);

		activeNodeCount -= 2;

		nodeList.add(activeNodeCount, newNode);

		activeNodeCount += 1;

		if (getMinimumInactiveHeight() < height) {
			throw new RuntimeException(
					"This should never happen! Somehow the current active node is older than the next inactive node!");
		}
	}
	
	@Override
    public String [] getTaxaNames() {
    	if (m_sTaxaNames == null) {
            List<String> sTaxa;
            if (m_taxa.get() != null) {
                    sTaxa = m_taxa.get().getTaxaNames();
            } else {
                    sTaxa = m_taxonset.get().asStringList();
            }
            m_sTaxaNames = sTaxa.toArray(new String[0]);
    	}
    	return m_sTaxaNames;
    }

	final private ArrayList<Node> nodeList = new ArrayList<Node>();
	private int activeNodeCount = 0;

	
}