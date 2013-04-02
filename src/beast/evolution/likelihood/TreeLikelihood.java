/*
* File TreeLikelihood.java
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


package beast.evolution.likelihood;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.AscertainedAlignment;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.branchratemodel.StrictClockModel;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

@Description("Calculates the likelihood of sequence data on a beast.tree given a site and substitution model using " +
        "a variant of the 'peeling algorithm'. For details, see" +
        "Felsenstein, Joseph (1981). Evolutionary trees from DNA sequences: a maximum likelihood approach. J Mol Evol 17 (6): 368-376.")
public class TreeLikelihood extends GenericTreeLikelihood {

    public Input<Boolean> m_useAmbiguities = new Input<Boolean>("useAmbiguities", "flag to indicate leafs that sites containing ambigue states should be handled instead of ignored (the default)", false);
    
    
    enum Scaling {none, always, _default};
    public Input<Scaling> scaling = new Input<TreeLikelihood.Scaling>("scaling", "type of scaling to use, one of " + Arrays.toString(Scaling.values()) + ". If not specified, the -beagle_scaling flag is used.", Scaling._default, Scaling.values());
    

    /**
     * calculation engine *
     */
    protected LikelihoodCore m_likelihoodCore;
    BeagleTreeLikelihood m_beagle;

    /**
     * Plugin associated with inputs. Since none of the inputs are StateNodes, it
     * is safe to link to them only once, during initAndValidate.
     */
    SubstitutionModel.Base m_substitutionModel;
    protected SiteModel.Base m_siteModel;
    protected BranchRateModel.Base m_branchRateModel;

    /**
     * flag to indicate the
     * // when CLEAN=0, nothing needs to be recalculated for the node
     * // when DIRTY=1 indicates a node partial needs to be recalculated
     * // when FILTHY=2 indicates the indices for the node need to be recalculated
     * // (often not necessary while node partial recalculation is required)
     */
    protected int m_nHasDirt;

    /**
     * Lengths of the branches in the tree associated with each of the nodes
     * in the tree through their node  numbers. By comparing whether the
     * current branch length differs from stored branch lengths, it is tested
     * whether a node is dirty and needs to be recomputed (there may be other
     * reasons as well...).
     * These lengths take branch rate models in account.
     */
    protected double[] m_branchLengths;
    protected double[] m_StoredBranchLengths;

    /**
     * memory allocation for likelihoods for each of the patterns *
     */
    double[] m_fPatternLogLikelihoods;
    /**
     * memory allocation for the root partials *
     */
    double[] m_fRootPartials;
    /**
     * memory allocation for probability tables obtained from the SiteModel *
     */
    double[] m_fProbabilities;

    int m_nMatrixSize;

    /**
     * flag to indicate ascertainment correction should be applied *
     */
    boolean m_bAscertainedSitePatterns = false;

    /**
     * dealing with proportion of site being invariant *
     */
    double m_fProportionInvariant = 0;
    List<Integer> m_iConstantPattern = null;

    @Override
    public void initAndValidate() throws Exception {
        // sanity check: alignment should have same #taxa as tree
        if (m_data.get().getNrTaxa() != m_tree.get().getLeafNodeCount()) {
            throw new Exception("The number of nodes in the tree does not match the number of sequences");
        }
        m_beagle = null;
        m_beagle = new BeagleTreeLikelihood();
        try {
	        m_beagle.initByName("data", m_data.get(), "tree", m_tree.get(), "siteModel", m_pSiteModel.get(), "branchRateModel", m_pBranchRateModel.get(), "useAmbiguities", m_useAmbiguities.get(), "scaling", scaling.get().toString());
	        if (m_beagle.beagle != null) {
	            //a Beagle instance was found, so we use it
	            return;
	        }
        } catch (Exception e) {
			// ignore
		}
        // No Beagle instance was found, so we use the good old java likelihood core
        m_beagle = null;

        int nodeCount = m_tree.get().getNodeCount();
        if (!(m_pSiteModel.get() instanceof SiteModel.Base)) {
        	throw new Exception ("siteModel input should be of type SiteModel.Base");
        }
        m_siteModel = (SiteModel.Base) m_pSiteModel.get();
        m_siteModel.setDataType(m_data.get().getDataType());
        m_substitutionModel = (SubstitutionModel.Base) m_siteModel.m_pSubstModel.get();

        if (m_pBranchRateModel.get() != null) {
            m_branchRateModel = m_pBranchRateModel.get();
        } else {
            m_branchRateModel = new StrictClockModel();
        }
        m_branchLengths = new double[nodeCount];
        m_StoredBranchLengths = new double[nodeCount];

        int nStateCount = m_data.get().getMaxStateCount();
        int nPatterns = m_data.get().getPatternCount();
        if (nStateCount == 4) {
            m_likelihoodCore = new BeerLikelihoodCore4();
        } else {
            m_likelihoodCore = new BeerLikelihoodCore(nStateCount);
        }
        System.out.println("TreeLikelihood uses " + m_likelihoodCore.getClass().getName());

        m_fProportionInvariant = m_siteModel.getProportianInvariant();
        m_siteModel.setPropInvariantIsCategory(false);
        if (m_fProportionInvariant > 0) {
            calcConstantPatternIndices(nPatterns, nStateCount);
        }

        initCore();

        m_fPatternLogLikelihoods = new double[nPatterns];
        m_fRootPartials = new double[nPatterns * nStateCount];
        m_nMatrixSize = (nStateCount + 1) * (nStateCount + 1);
        m_fProbabilities = new double[(nStateCount + 1) * (nStateCount + 1)];
        Arrays.fill(m_fProbabilities, 1.0);

        if (m_data.get() instanceof AscertainedAlignment) {
            m_bAscertainedSitePatterns = true;
        }
    }


    /**
     * Determine indices of m_fRootProbabilities that need to be updates
     * // due to sites being invariant. If none of the sites are invariant,
     * // the 'site invariant' category does not contribute anything to the
     * // root probability. If the site IS invariant for a certain character,
     * // taking ambiguities in account, there is a contribution of 1 from
     * // the 'site invariant' category.
     */
    void calcConstantPatternIndices(final int nPatterns, final int nStateCount) {
        m_iConstantPattern = new ArrayList<Integer>();
        for (int i = 0; i < nPatterns; i++) {
            final int[] pattern = m_data.get().getPattern(i);
            final boolean[] bIsInvariant = new boolean[nStateCount];
            Arrays.fill(bIsInvariant, true);
            for (final int state : pattern) {
                final boolean[] bStateSet = m_data.get().getStateSet(state);
                if (m_useAmbiguities.get() || !m_data.get().getDataType().isAmbiguousState(state)) {
                    for (int k = 0; k < nStateCount; k++) {
                        bIsInvariant[k] &= bStateSet[k];
                    }
                }
            }
            for (int k = 0; k < nStateCount; k++) {
                if (bIsInvariant[k]) {
                    m_iConstantPattern.add(i * nStateCount + k);
                }
            }
        }
    }

    void initCore() {
        final int nodeCount = m_tree.get().getNodeCount();
        m_likelihoodCore.initialize(
                nodeCount,
                m_data.get().getPatternCount(),
                m_siteModel.getCategoryCount(),
                true, m_useAmbiguities.get()
        );

        final int extNodeCount = nodeCount / 2 + 1;
        final int intNodeCount = nodeCount / 2;

        if (m_useAmbiguities.get()) {
            setPartials(m_tree.get().getRoot(), m_data.get().getPatternCount());
        } else {
            setStates(m_tree.get().getRoot(), m_data.get().getPatternCount());
        }
        m_nHasDirt = Tree.IS_FILTHY;
        for (int i = 0; i < intNodeCount; i++) {
            m_likelihoodCore.createNodePartials(extNodeCount + i);
        }
    }

    /**
     * This method samples the sequences based on the tree and site model.
     */
    public void sample(State state, Random random) {
        throw new UnsupportedOperationException("Can't sample a fixed alignment!");
    }

    /**
     * set leaf states in likelihood core *
     */
    void setStates(Node node, int patternCount) {
        if (node.isLeaf()) {
            int i;
            int[] states = new int[patternCount];
            int iTaxon = m_data.get().getTaxonIndex(node.getID());
            for (i = 0; i < patternCount; i++) {
                states[i] = m_data.get().getPattern(iTaxon, i);
            }
            m_likelihoodCore.setNodeStates(node.getNr(), states);

        } else {
            setStates(node.getLeft(), patternCount);
            setStates(node.getRight(), patternCount);
        }
    }

    /**
     * set leaf partials in likelihood core *
     */
    void setPartials(Node node, int patternCount) {
        if (node.isLeaf()) {
            Alignment data = m_data.get();
            int nStates = data.getDataType().getStateCount();
            double[] partials = new double[patternCount * nStates];

            int k = 0;
            int iTaxon = m_data.get().getTaxonIndex(node.getID());
            for (int iPattern = 0; iPattern < patternCount; iPattern++) {
                int nState = data.getPattern(iTaxon, iPattern);
                boolean[] stateSet = data.getStateSet(nState);
                for (int iState = 0; iState < nStates; iState++) {
                    partials[k++] = (stateSet[iState] ? 1.0 : 0.0);
                }
            }
            m_likelihoodCore.setNodePartials(node.getNr(), partials);

        } else {
            setPartials(node.getLeft(), patternCount);
            setPartials(node.getRight(), patternCount);
        }
    }

    /**
     * Calculate the log likelihood of the current state.
     *
     * @return the log likelihood.
     */
    double m_fScale = 1.01;
    int m_nScale = 0;
    int X = 100;

    @Override
    public double calculateLogP() throws Exception {
        if (m_beagle != null) {
            logP = m_beagle.calculateLogP();
            return logP;
        }
        final Tree tree = m_tree.get();

        if (traverse(tree.getRoot()) != Tree.IS_CLEAN)
            calcLogP();

        m_nScale++;
        if (logP > 0 || (m_likelihoodCore.getUseScaling() && m_nScale > X)) {
//            System.err.println("Switch off scaling");
//            m_likelihoodCore.setUseScaling(1.0);
//            m_likelihoodCore.unstore();
//            m_nHasDirt = Tree.IS_FILTHY;
//            X *= 2;
//            traverse(tree.getRoot());
//            calcLogP();
//            return logP;
        } else if (logP == Double.NEGATIVE_INFINITY && m_fScale < 10 && !scaling.get().equals(Scaling.none)) { // && !m_likelihoodCore.getUseScaling()) {
            m_nScale = 0;
            m_fScale *= 1.01;
            System.err.println("Turning on scaling to prevent numeric instability " + m_fScale);
            m_likelihoodCore.setUseScaling(m_fScale);
            m_likelihoodCore.unstore();
            m_nHasDirt = Tree.IS_FILTHY;
            traverse(tree.getRoot());
            calcLogP();
            return logP;
        }
        return logP;
    }

    void calcLogP() throws Exception {
        logP = 0.0;
        if (m_bAscertainedSitePatterns) {
            final double ascertainmentCorrection = ((AscertainedAlignment) m_data.get()).getAscertainmentCorrection(m_fPatternLogLikelihoods);
            for (int i = 0; i < m_data.get().getPatternCount(); i++) {
                logP += (m_fPatternLogLikelihoods[i] - ascertainmentCorrection) * m_data.get().getPatternWeight(i);
            }
        } else {
            for (int i = 0; i < m_data.get().getPatternCount(); i++) {
                logP += m_fPatternLogLikelihoods[i] * m_data.get().getPatternWeight(i);
            }
        }
    }

    /* Assumes there IS a branch rate model as opposed to traverse() */
    int traverse(final Node node) throws Exception {

        int update = (node.isDirty() | m_nHasDirt);

        final int iNode = node.getNr();

        final double branchRate = m_branchRateModel.getRateForBranch(node);
        final double branchTime = node.getLength() * branchRate;
        m_branchLengths[iNode] = branchTime;

        // First update the transition probability matrix(ices) for this branch
        if (!node.isRoot() && (update != Tree.IS_CLEAN || branchTime != m_StoredBranchLengths[iNode])) {
            final Node parent = node.getParent();
            m_likelihoodCore.setNodeMatrixForUpdate(iNode);
            for (int i = 0; i < m_siteModel.getCategoryCount(); i++) {
                final double jointBranchRate = m_siteModel.getRateForCategory(i, node) * branchRate;
                m_substitutionModel.getTransitionProbabilities(node, parent.getHeight(), node.getHeight(), jointBranchRate, m_fProbabilities);
                m_likelihoodCore.setNodeMatrix(iNode, i, m_fProbabilities);
            }
            update |= Tree.IS_DIRTY;
        }

        // If the node is internal, update the partial likelihoods.
        if (!node.isLeaf()) {

            // Traverse down the two child nodes
            final Node child1 = node.getLeft(); //Two children
            final int update1 = traverse(child1);

            final Node child2 = node.getRight();
            final int update2 = traverse(child2);

            // If either child node was updated then update this node too
            if (update1 != Tree.IS_CLEAN || update2 != Tree.IS_CLEAN) {

                final int childNum1 = child1.getNr();
                final int childNum2 = child2.getNr();

                m_likelihoodCore.setNodePartialsForUpdate(iNode);
                update |= (update1 | update2);
                if (update >= Tree.IS_FILTHY) {
                    m_likelihoodCore.setNodeStatesForUpdate(iNode);
                }

                if (m_siteModel.integrateAcrossCategories()) {
                    m_likelihoodCore.calculatePartials(childNum1, childNum2, iNode);
                } else {
                    throw new Exception("Error TreeLikelihood 201: Site categories not supported");
                    //m_pLikelihoodCore->calculatePartials(childNum1, childNum2, nodeNum, siteCategories);
                }

                if (node.isRoot()) {
                    // No parent this is the root of the beast.tree -
                    // calculate the pattern likelihoods
                    final double[] frequencies = //m_pFreqs.get().
                            m_substitutionModel.getFrequencies();

                    final double[] proportions = m_siteModel.getCategoryProportions(node);
                    m_likelihoodCore.integratePartials(node.getNr(), proportions, m_fRootPartials);

                    if (m_iConstantPattern != null) { // && !SiteModel.g_bUseOriginal) {
                        m_fProportionInvariant = m_siteModel.getProportianInvariant();
                        // some portion of sites is invariant, so adjust root partials for this
                        for (final int i : m_iConstantPattern) {
                            m_fRootPartials[i] += m_fProportionInvariant;
                        }
                    }

                    m_likelihoodCore.calculateLogLikelihoods(m_fRootPartials, frequencies, m_fPatternLogLikelihoods);
                }

            }
        }
        return update;
    } // traverseWithBRM

    /** CalculationNode methods **/

    /**
     * check state for changed variables and update temp results if necessary *
     */
    @Override
    protected boolean requiresRecalculation() {
        if (m_beagle != null) {
            return m_beagle.requiresRecalculation();
        }
        m_nHasDirt = Tree.IS_CLEAN;

        if (m_data.get().isDirtyCalculation()) {
            m_nHasDirt = Tree.IS_FILTHY;
            return true;
        }
        if (m_siteModel.isDirtyCalculation()) {
            m_nHasDirt = Tree.IS_DIRTY;
            return true;
        }
        if (m_branchRateModel != null && m_branchRateModel.isDirtyCalculation()) {
            m_nHasDirt = Tree.IS_DIRTY;
            return true;
        }
        return m_tree.get().somethingIsDirty();
    }

    @Override
    public void store() {
        if (m_beagle != null) {
            m_beagle.store();
            super.store();
            return;
        }
        if (m_likelihoodCore != null) {
            m_likelihoodCore.store();
        }
        super.store();
        System.arraycopy(m_branchLengths, 0, m_StoredBranchLengths, 0, m_branchLengths.length);
    }

    @Override
    public void restore() {
        if (m_beagle != null) {
            m_beagle.restore();
            super.restore();
            return;
        }
        if (m_likelihoodCore != null) {
            m_likelihoodCore.restore();
        }
        super.restore();
        double[] tmp = m_branchLengths;
        m_branchLengths = m_StoredBranchLengths;
        m_StoredBranchLengths = tmp;
    }

    /**
     * @return a list of unique ids for the state nodes that form the argument
     */
    public List<String> getArguments() {
        return Collections.singletonList(m_data.get().getID());
    }

    /**
     * @return a list of unique ids for the state nodes that make up the conditions
     */
    public List<String> getConditions() {
        return m_siteModel.getConditions();
    }

} // class TreeLikelihood
