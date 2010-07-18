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

/* BEAST 1.0: 1M samples testMCMC.xml
real	0m27.889s
user	0m29.722s
sys	0m0.356s
real	0m22.344s
user	0m24.298s
sys	0m0.252s
TreeLikelihood(treeModel) using Java nucleotide likelihood beast.core

BEAST 2.0: no store/restore
real	0m22.371s
user	0m23.565s
sys	0m0.160s

with store/restore
real	0m20.339s
user	0m21.209s
sys	0m0.176s

GORED aware
real	0m18.958s
user	0m20.401s
sys	0m0.216s

6 taxa
768 sites
69 patterns
10M samples testMCMC.xml single thread
        Beast 1.6/java  Beast1.6/native Beast 2.0
real	3m55.056s       3m38.670s       2m31.794s
user	3m54.839s       3m38.670s       2m32.162s
sys	    0m0.392s        0m0.428s        0m0.476s

46 taxa
1363 sites
199 patterns
500K samples testMCMC2.xml
        Beast 1.6/java  Beast1.6/native Beast 2.0
real	2m28.815s       2m0.174s        0m43.520s
user	2m30.493s       2m0.420s        0m45.299s
sys	    0m0.324s        0m0.632s        0m0.424s

Beast1.6/native
real	1m56.097s
user	1m58.683s
sys	0m0.332s
Beast 2.0 + auto
real	0m56.843s
user	0m59.264s
sys	0m0.428s


191 taxa
606 sites
228 patterns
100K samples testMCMC3.xml
      Beast1.6/java Beast1.6/native   Beast 2.0 no scaling    Beast 2.0 with scaling
real	8m39.146s     8m47.876s       0m28.725s               0m30.034s
user	8m37.112s     8m47.961s       0m30.610s               0m32.998s
sys	    0m0.500s      0m0.648s        0m0.452s                0m0.368s


Beast1.6/native
real	8m32.418s
user	8m33.060s
sys	0m0.476s
1.35 hours/million states

Beast 2.0 + auto optimize
real	0m43.962s = 11.636363636
user	0m46.051s = 11.130434783x
sys	0m0.432s
3022 s/sec = 0.091918523 hr/M states = 14.7 x Beast1.6


Caveat 1: no auto optimise

100 samples Ourisia60
SnAP    1 thread  2 threads Beast 2.0 1 thread 2 threads
real	4m55.316s 3m19.635s           6m9.100s 3m24.905s
user	4m52.522s 5m7.543s            6m1.491s 6m5.183s
sys	    0m0.164s  0m1.448s            0m1.328s 0m4.740s


400 taxa
34440 sites
31119 patterns
  PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME+  COMMAND
10907 rrb       20   0 3336m 1.7g 9008 S  101 58.4   1:45.05 java
1	-1,096,649.8002	2.1268E-4   	1.00000     	1,335.83 hours/million states

*
*/

package beast.evolution.likelihood;


import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.evolution.alignment.Alignment;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Description("Calculates the likelihood of sequence data on a beast.tree given a site and substitution model.")
public class TreeLikelihood extends Distribution {

    public Input<Alignment> m_data = new Input<Alignment>("data", "sequence data for the beast.tree");
    public Input<Tree> m_tree = new Input<Tree>("tree", "phylogenetic beast.tree with sequence data in the leafs");
    public Input<SiteModel> m_pSiteModel = new Input<SiteModel>("siteModel", "site model for leafs in the beast.tree");
    public Input<BranchRateModel.Base> m_pBranchRateModel = new Input<BranchRateModel.Base>("branchRateModel",
            "A model describing the rates on the branches of the beast.tree.");

    /** calculation engine **/
    LikelihoodCore m_likelihoodCore;
    /** Plugin associated with inputs. Since none of the inputs are StateNodes, it
     * is safe to link to them only once, during initAndValidate.
     */
    SubstitutionModel m_substitutionModel;
    SiteModel m_siteModel;
    BranchRateModel.Base m_branchRateModel;

    @Override
    public void initAndValidate(State state) throws Exception {
        int nStateCount = m_data.get().getMaxStateCount();
        if (nStateCount == 4) {
        	//m_likelihoodCore = new BeerLikelihoodCore4();
            m_likelihoodCore = new BeerLikelihoodCoreCnG4();
        } else {
            //m_likelihoodCore = new BeerLikelihoodCore(nStateCount);
            m_likelihoodCore = new BeerLikelihoodCoreCnG(nStateCount);
        }
        System.err.println("TreeLikelihood uses " + m_likelihoodCore.getClass().getName());

        //probabilities = new double[stateCount * stateCount];
        int nodeCount = m_tree.getStateNode().getNodeCount();
        m_likelihoodCore.initialize(
                nodeCount,
                m_data.get().getPatternCount(),
                1,
                true
        );

        int extNodeCount = nodeCount / 2 + 1;
        int intNodeCount = nodeCount / 2;

        setStates(m_tree.getStateNode().getRoot(), m_data.get().getPatternCount());

        for (int i = 0; i < intNodeCount; i++) {
            m_likelihoodCore.createNodePartials(extNodeCount + i);
        }
        m_fPatternLogLikelihoods = new double[m_data.get().getPatternCount()];
        m_fRootPartials = new double[m_data.get().getPatternCount() * nStateCount];
        m_fProbabilities = new double[nStateCount * nStateCount];
        m_bNodeIsDirty = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            m_bNodeIsDirty[i] = Tree.IS_FILTHY;
        }
        m_siteModel = m_pSiteModel.get();
        m_substitutionModel = m_siteModel.m_pSubstModel.get();
        m_branchRateModel = m_pBranchRateModel.get();
    }

    /**
     * This method samples the sequences based on the tree and site model.
     *
     * @param state the state
     */
    public void sample(State state, Random random) {
        throw new UnsupportedOperationException("Can't sample a fixed alignment!");
    }


    void setStates(Node node, int patternCount) {
        if (node.isLeaf()) {
            int i;
            int[] states = new int[patternCount];
            for (i = 0; i < patternCount; i++) {
                states[i] = m_data.get().getPattern(node.getNr(), i);
            }
            m_likelihoodCore.setNodeStates(node.getNr(), states);

        } else {
            setStates(node.m_left, patternCount);
            setStates(node.m_right, patternCount);
        }
    }

    /**
     * Calculate the log likelihood of the current state.
     *
     * @return the log likelihood.
     * @throws Exception
     */
    @Override
    public double calculateLogP() throws Exception {
        checkForDirt();
        Tree tree = m_tree.getStateNode();
        traverse(tree.getRoot());
        logP = 0.0;
        //double ascertainmentCorrection = getAscertainmentCorrection(patternLogLikelihoods);
        for (int i = 0; i < m_data.get().getPatternCount(); i++) {
            logP += m_fPatternLogLikelihoods[i] * m_data.get().getPatternWeight(i);
        }
        //if (m_fLogP == Double.NEGATIVE_INFINITY) {
        if (logP < -1e4 && !m_likelihoodCore.getUseScaling()) {
            System.err.println("Turning on scaling to prevent numeric instability");
            m_likelihoodCore.setUseScaling(true);
            Arrays.fill(m_bNodeIsDirty, Tree.IS_FILTHY);
            return calculateLogP();
        }
        return logP;
    }

    /**
     * Traverse the beast.tree calculating partial likelihoods.
     *
     * @return whether the partials for this node were recalculated.
     * @throws Exception
     */
    int traverse(Node node) throws Exception {

        int update = Tree.IS_CLEAN;

        int iNode = node.getNr();

        //Node pParent = node.getParent();

        // First update the transition probability matrix(ices) for this branch
//        /if (pParent != NULL && m_updateNode[nodeNum]) {
        if (!node.isRoot() && (m_bNodeIsDirty[iNode] != Tree.IS_CLEAN)) {

            double branchRate = 1.0;
            if (m_branchRateModel != null) branchRate = m_branchRateModel.getRateForBranch(node);

            // Get the operational time of the branch
            double branchTime = branchRate * node.getLength();//((*pParent).height - (*pNode).height);
            if (branchTime < 0) {
                throw new Exception("Negative branch length found");
            }

            m_likelihoodCore.setNodeMatrixForUpdate(iNode);
            //if (m_bNodeIsDirty[nodeNum] == IS_GORED) {
            //	m_pLikelihoodCore->setNodeStatesForUpdate(nodeNum);
            //}

            for (int i = 0; i < m_siteModel.getCategoryCount(); i++) {
                double branchLength = m_siteModel.getRateForCategory(i) * branchTime;
//                m_siteModel
//                        /*m_pSubstModel.get()*/.getTransitionProbabilities(branchLength, m_fProbabilities);
                m_substitutionModel.getTransitionProbabilities(branchLength, m_fProbabilities);
                m_likelihoodCore.setNodeMatrix(iNode, i, m_fProbabilities);
            }

            update = m_bNodeIsDirty[iNode];
        }

        // If the node is internal, update the partial likelihoods.
        if (!node.isLeaf()) {

            // Traverse down the two child nodes
            Node child1 = node.m_left; //Two children
            int update1 = traverse(child1);

            Node child2 = node.m_right;
            int update2 = traverse(child2);

            // If either child node was updated then update this node too
            if (update1 != Tree.IS_CLEAN || update2 != Tree.IS_CLEAN) {

                int childNum1 = child1.getNr();
                int childNum2 = child2.getNr();

                m_likelihoodCore.setNodePartialsForUpdate(iNode);
                // TODO: CHECK THAT IS_DIRTY IS ALREADY SUFFICIENT FOR UPDATE
                if (m_bNodeIsDirty[iNode] >= Tree.IS_FILTHY || update1 >= Tree.IS_FILTHY || update2 >= Tree.IS_FILTHY) {
                    m_likelihoodCore.setNodeStatesForUpdate(iNode);
                }

                if (m_siteModel.integrateAcrossCategories()) {
//cout << "cal partials " << childNum1 << " " << childNum2 << " " << nodeNum << " " <<
//m_bNodeIsDirty[childNum1] << " " << m_bNodeIsDirty[childNum2] << " " << m_bNodeIsDirty[nodeNum] <<endl;
                    m_likelihoodCore.calculatePartials(childNum1, childNum2, iNode);
                } else {
                    throw new Exception("Error TreeLikelihood 201: Site categories not supported");
                    //m_pLikelihoodCore->calculatePartials(childNum1, childNum2, nodeNum, siteCategories);
                }

                if (node.isRoot()) {
                    // No parent this is the root of the beast.tree -
                    // calculate the pattern likelihoods
                    double[] frequencies = //m_pFreqs.get().
                            m_siteModel.getFrequencies();

                    //getRootPartials(node.getNr());
                    double[] proportions = m_siteModel.getCategoryProportions();
                    m_likelihoodCore.integratePartials(node.getNr(), proportions, m_fRootPartials);

                    m_likelihoodCore.calculateLogLikelihoods(m_fRootPartials, frequencies, m_fPatternLogLikelihoods);
                }

                update = Math.max(Math.max(update1, update2), m_bNodeIsDirty[iNode]);
            }
        }
        return update;
    } // traverse


    /**
     * check state for changed variables and update temp results if necessary *
     */
    public void checkForDirt() {
        int hasDirt = Tree.IS_CLEAN;
        if (m_siteModel.isDirty()) {
            hasDirt = Tree.IS_DIRTY;
        }
        if (m_branchRateModel != null && m_branchRateModel.isDirty()) {
            hasDirt = Tree.IS_DIRTY;
        }
    	//Arrays.fill(m_bNodeIsDirty, Tree.IS_FILTHY);
        // int hasDirt = (m_pSubstModel.get().isDirty(state) ? State.IS_DIRTY : State.IS_CLEAN);
        //m_likelihoodCore.checkForDirt(state);
        Tree tree = m_tree.getStateNode();
        tree.syncTreeWithTraitsInState();
        checkNodesForDirt(tree.getRoot(), hasDirt);
    } // checkForDirt

    void checkNodesForDirt(Node node, int hasDirt) {
        m_bNodeIsDirty[node.getNr()] = Math.max(node.isDirty(), hasDirt);
        if (!node.isLeaf()) {
            checkNodesForDirt(node.m_left, hasDirt);
            checkNodesForDirt(node.m_right, hasDirt);
        }
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

    @Override
    public void store(int nSample) {
        super.store(nSample);
        //m_siteModel.store(nSample);
        //m_pSubstModel.get().store(nSample);
        m_likelihoodCore.store();
        //if (branchRateModel.get() != null) {
        //	branchRateModel.get().store(nSample);
        //}
    }

    @Override
    public void restore(int nSample) {
        super.restore(nSample);
        //m_siteModel.restore(nSample);
        //m_pSubstModel.get().restore(nSample);
        m_likelihoodCore.restore();
        //if (branchRateModel.get() != null) {
        //	branchRateModel.get().restore(nSample);
        //}
    }

    double[] m_fPatternLogLikelihoods;
    double[] m_fRootPartials;
    double[] m_fProbabilities;
    // when CLEAN=0, nothing needs to be recalculated for the node
    // when DIRTY=1 indicates a node partial needs to be recalculated
    // when GORED=2 indicates the indices for the node need to be recalculated
    // (often not necessary while node partial recalculation is required)
    int[] m_bNodeIsDirty;

//    @Override
//    public String getCitation() {
//        return "Felsenstein, Joseph (1981). Evolutionary trees from DNA sequences: a maximum likelihood approach. J Mol Evol 17 (6): 368-376. ";
//    }
} // class TreeLikelihood
