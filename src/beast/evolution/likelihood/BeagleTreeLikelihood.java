/*
 * TreeLikelihood.java
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

package beast.evolution.likelihood;

import beagle.*;
import beast.core.Description;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.AscertainedAlignment;
import beast.evolution.substitutionmodel.EigenDecomposition;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

import java.util.*;

/**
 * BeagleTreeLikelihoodModel - implements a Likelihood Function for sequences on a tree.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Marc Suchard
 * @version $Id$
 */

@Description("Uses Beagle library to calculate Tree likelihood")
public class BeagleTreeLikelihood extends TreeLikelihood {

    // This property is a comma-delimited list of resource numbers (0 == CPU) to
    // allocate each BEAGLE instance to. If less than the number of instances then
    // will wrap around.
	// note: to use a different device, say device 2, start beast with 
	// java -Dbeagle.resource.order=2 beast.app.BeastMCMC 
    private static final String RESOURCE_ORDER_PROPERTY = "beagle.resource.order";
    private static final String PREFERRED_FLAGS_PROPERTY = "beagle.preferred.flags";
    private static final String REQUIRED_FLAGS_PROPERTY = "beagle.required.flags";
    private static final String SCALING_PROPERTY = "beagle.scaling";

    private static int instanceCount = 0;
    private static List<Integer> resourceOrder = null;
    private static List<Integer> preferredOrder = null;
    private static List<Integer> requiredOrder = null;
    private static List<String> scalingOrder = null;

    private static final int RESCALE_FREQUENCY = 10000;
    private static final int RESCALE_TIMES = 1;

    boolean m_bUseAmbiguities;
    int m_nStateCount;
    int m_nNodeCount;
    
    @Override
    public void initAndValidate() {
        m_nNodeCount = m_tree.get().getNodeCount();
        m_bUseAmbiguities = m_useAmbiguities.get();
        m_siteModel = m_pSiteModel.get();
        m_siteModel.setDataType(m_data.get().getDataType());
        m_substitutionModel = m_siteModel.m_pSubstModel.get();
        m_branchRateModel = m_pBranchRateModel.get();
        if (m_branchRateModel != null) {
        	m_branchLengths = new double[m_nNodeCount];
        	m_StoredBranchLengths = new double[m_nNodeCount];
        } else {
        	m_branchLengths = new double[0];
        	m_StoredBranchLengths = new double[0];
        }

        m_nStateCount = m_data.get().getMaxStateCount();
        m_nPatternCount = m_data.get().getPatternCount();

        	System.err.println("Using BEAGLE TreeLikelihood");

            eigenCount = 1;//this.branchSubstitutionModel.getEigenCount();

            this.categoryCount = m_siteModel.getCategoryCount();
            tipCount  = m_tree.get().getLeafNodeCount();

            internalNodeCount = m_nNodeCount - tipCount;

            int compactPartialsCount = tipCount;
            if (m_bUseAmbiguities) {
                // if we are using ambiguities then we don't use tip partials
                compactPartialsCount = 0;
            }

            // one partials buffer for each tip and two for each internal node (for store restore)
            partialBufferHelper = new BufferIndexHelper(m_nNodeCount, tipCount);

            // two eigen buffers for each decomposition for store and restore.
            eigenBufferHelper = new BufferIndexHelper(eigenCount, 0);

            // two matrices for each node less the root
            matrixBufferHelper = new BufferIndexHelper(m_nNodeCount, 0);

            // one scaling buffer for each internal node plus an extra for the accumulation, then doubled for store/restore
            scaleBufferHelper = new BufferIndexHelper(getScaleBufferCount(), 0);

            // Attempt to get the resource order from the System Property
            if (resourceOrder == null) {
                resourceOrder = parseSystemPropertyIntegerArray(RESOURCE_ORDER_PROPERTY);
            }
            if (preferredOrder == null) {
                preferredOrder = parseSystemPropertyIntegerArray(PREFERRED_FLAGS_PROPERTY);
            }
            if (requiredOrder == null) {
                requiredOrder = parseSystemPropertyIntegerArray(REQUIRED_FLAGS_PROPERTY);
            }
            if (scalingOrder == null) {
                scalingOrder = parseSystemPropertyStringArray(SCALING_PROPERTY);
            }

            // first set the rescaling scheme to use from the parser
            rescalingScheme = PartialsRescalingScheme.DEFAULT;// = rescalingScheme;
            int[] resourceList = null;
            long preferenceFlags = 0;
            long requirementFlags = 0;

            if (scalingOrder.size() > 0) {
                this.rescalingScheme = PartialsRescalingScheme.parseFromString(
                        scalingOrder.get(instanceCount % scalingOrder.size()));
            }
        
            if (resourceOrder.size() > 0) {
                // added the zero on the end so that a CPU is selected if requested resource fails
                resourceList = new int[]{resourceOrder.get(instanceCount % resourceOrder.size()), 0};
                if (resourceList[0] > 0) {
                    preferenceFlags |= BeagleFlag.PROCESSOR_GPU.getMask(); // Add preference weight against CPU
                }
            }

            if (preferredOrder.size() > 0) {
                preferenceFlags = preferredOrder.get(instanceCount % preferredOrder.size());
            }

            if (requiredOrder.size() > 0) {
                requirementFlags = requiredOrder.get(instanceCount % requiredOrder.size());
            }

            // Define default behaviour here
            if (this.rescalingScheme == PartialsRescalingScheme.DEFAULT) {
                //if GPU: the default is dynamic scaling in BEAST
                if (resourceList != null && resourceList[0] > 1) {
                    this.rescalingScheme = PartialsRescalingScheme.DYNAMIC;
                } else { // if CPU: just run as fast as possible
//                    this.rescalingScheme = PartialsRescalingScheme.NONE;
                    // Dynamic should run as fast as none until first underflow
                    this.rescalingScheme = PartialsRescalingScheme.DYNAMIC;
                }
            }

            if (this.rescalingScheme == PartialsRescalingScheme.AUTO) {
                preferenceFlags |= BeagleFlag.SCALING_AUTO.getMask();
                useAutoScaling = true;
            } else {
//                preferenceFlags |= BeagleFlag.SCALING_MANUAL.getMask();
            }

            if (preferenceFlags == 0 && resourceList == null) { // else determine dataset characteristics
                if (m_nStateCount == 4 && m_nPatternCount < 10000) // TODO determine good cut-off
                    preferenceFlags |= BeagleFlag.PROCESSOR_CPU.getMask();
            }

            if (m_substitutionModel.canReturnComplexDiagonalization()) {
                requirementFlags |= BeagleFlag.EIGEN_COMPLEX.getMask();
            }

            instanceCount++;

            beagle = BeagleFactory.loadBeagleInstance(
                    tipCount,
                    partialBufferHelper.getBufferCount(),
                    compactPartialsCount,
                    m_nStateCount,
                    m_nPatternCount,
                    eigenBufferHelper.getBufferCount(),            // eigenBufferCount
                    matrixBufferHelper.getBufferCount(),
                    categoryCount,
                    scaleBufferHelper.getBufferCount(), // Always allocate; they may become necessary
                    resourceList,
                    preferenceFlags,
                    requirementFlags
            );

            InstanceDetails instanceDetails = beagle.getDetails();
            ResourceDetails resourceDetails = null;

            if (instanceDetails != null) {
                resourceDetails = BeagleFactory.getResourceDetails(instanceDetails.getResourceNumber());
                if (resourceDetails != null) {
                    StringBuilder sb = new StringBuilder("  Using BEAGLE resource ");
                    sb.append(resourceDetails.getNumber()).append(": ");
                    sb.append(resourceDetails.getName()).append("\n");
                    if (resourceDetails.getDescription() != null) {
                        String[] description = resourceDetails.getDescription().split("\\|");
                        for (String desc : description) {
                            if (desc.trim().length() > 0) {
                                sb.append("    ").append(desc.trim()).append("\n");
                            }
                        }
                    }
                    sb.append("    with instance flags: ").append(instanceDetails.toString());
                    System.err.println(sb.toString());
                } else {
                	System.err.println("  Error retrieving BEAGLE resource for instance: " + instanceDetails.toString());
                }
            } else {
            	System.err.println("  No external BEAGLE resources available, or resource list/requirements not met, using Java implementation");
            }
            System.err.println("  " + (m_bUseAmbiguities ? "Using" : "Ignoring") + " ambiguities in tree likelihood.");
            System.err.println("  With " + m_nPatternCount + " unique site patterns.");

            for (int i = 0; i < tipCount; i++) {
               if (m_bUseAmbiguities) {
                   setPartials(beagle, i);
                } else {
                   setStates(beagle, i);
                }
            }

            if (m_data.get() instanceof AscertainedAlignment) {
                ascertainedSitePatterns = true;
            }
                
            double [] fPatternWeights = new double[m_nPatternCount];
            for (int i = 0; i < m_nPatternCount; i++) {
            	fPatternWeights[i] = m_data.get().getPatternWeight(i);
            }
            beagle.setPatternWeights(fPatternWeights);

            if (this.rescalingScheme == PartialsRescalingScheme.AUTO &&
                    resourceDetails != null &&
                    (resourceDetails.getFlags() & BeagleFlag.SCALING_AUTO.getMask()) == 0) {
                // If auto scaling in BEAGLE is not supported then do it here
                this.rescalingScheme = PartialsRescalingScheme.DYNAMIC;
                System.err.println("  Auto rescaling not supported in BEAGLE, using : " + this.rescalingScheme.getText());
            } else {
            	System.err.println("  Using rescaling scheme : " + this.rescalingScheme.getText());
            }

            if (this.rescalingScheme == PartialsRescalingScheme.DYNAMIC) {
                everUnderflowed = true; // If commented out, BEAST does not rescale until first under-/over-flow.
            }

            updateSubstitutionModel = true;
            updateSiteModel = true;
            // some subst models (e.g. WAG) never become dirty, so set up subst models right now
            setUpSubstModel();
    }

    private static List<Integer> parseSystemPropertyIntegerArray(String propertyName) {
        List<Integer> order = new ArrayList<Integer>();
        String r = System.getProperty(propertyName);
        if (r != null) {
            String[] parts = r.split(",");
            for (String part : parts) {
                try {
                    int n = Integer.parseInt(part.trim());
                    order.add(n);
                } catch (NumberFormatException nfe) {
                    System.err.println("Invalid entry '" + part + "' in " + propertyName);
                }
            }
        }
        return order;
    }

    private static List<String> parseSystemPropertyStringArray(String propertyName) {

        List<String> order = new ArrayList<String>();

        String r = System.getProperty(propertyName);
        if (r != null) {
            String[] parts = r.split(",");
            for (String part : parts) {
                try {
                    String s = part.trim();
                    order.add(s);
                } catch (NumberFormatException nfe) {
                    System.err.println("Invalid getEigenDecompositionentry '" + part + "' in " + propertyName);
                }
            }
        }
        return order;
    }

    protected int getScaleBufferCount() {
        return internalNodeCount + 1;
    }

    /**
     * Sets the partials from a sequence in an alignment.
     *
     * @param beagle        beagle
     * @param patternList   patternList
     * @param sequenceIndex sequenceIndex
     * @param nodeIndex     nodeIndex
     */
    protected final void setPartials(Beagle beagle,
                                     int nodeIndex) {
    	Alignment data = m_data.get();
    	
        double[] partials = new double[m_nPatternCount * m_nStateCount * categoryCount];

        boolean[] stateSet;

        int v = 0;
        for (int i = 0; i < m_nPatternCount; i++) {

            int state = data.getPattern(nodeIndex, i); 
            stateSet = data.getStateSet(state);

            for (int j = 0; j < m_nStateCount; j++) {
                if (stateSet[j]) {
                    partials[v] = 1.0;
                } else {
                    partials[v] = 0.0;
                }
                v++;
            }
        }

        // if there is more than one category then replicate the partials for each
        int n = m_nPatternCount * m_nStateCount;
        int k = n;
        for (int i = 1; i < categoryCount; i++) {
            System.arraycopy(partials, 0, partials, k, n);
            k += n;
        }

        beagle.setPartials(nodeIndex, partials);
    }

    public int getPatternCount() {
        return m_nPatternCount;
    }

    void setUpSubstModel() {
    	// we are currently assuming a no-category model...
    	// TODO More efficient to update only the substitution model that changed, instead of all
	    for (int i = 0; i < eigenCount; i++) {
	        //EigenDecomposition ed = m_substitutionModel.getEigenDecomposition(i, 0);
	        EigenDecomposition ed = m_substitutionModel.getEigenDecomposition(null);
	
	        eigenBufferHelper.flipOffset(i);
	
	        beagle.setEigenDecomposition(
	            eigenBufferHelper.getOffsetIndex(i),
	            ed.getEigenVectors(),
	            ed.getInverseEigenVectors(),
	            ed.getEigenValues());
	    }
	}
    /**
     * Sets the partials from a sequence in an alignment.
     *
     * @param beagle        beagle
     * @param patternList   patternList
     * @param sequenceIndex sequenceIndex
     * @param nodeIndex     nodeIndex
     */
    protected final void setStates(Beagle beagle,
                                   int nodeIndex) {
    	Alignment data = m_data.get();
        int i;

        int[] states = new int[m_nPatternCount];

        for (i = 0; i < m_nPatternCount; i++) {
            int state = data.getPattern(nodeIndex, i); 
            states[i] = state;
        }

        beagle.setTipStates(nodeIndex, states);
    }


//    public void setStates(int tipIndex, int[] states) {
//        System.err.println("BTL:setStates");
//        beagle.setTipStates(tipIndex, states);
//        makeDirty();
//    }
//
//    public void getStates(int tipIndex, int[] states) {
//        System.err.println("BTL:getStates");
//        beagle.getTipStates(tipIndex, states);
//    }


    /**
     * check state for changed variables and update temp results if necessary *
     */
    @Override
    protected boolean requiresRecalculation() {
        m_nHasDirt = Tree.IS_CLEAN;
        
        updateSiteModel = m_siteModel.isDirtyCalculation();
        updateSubstitutionModel = m_substitutionModel.isDirtyCalculation();

        if (m_branchRateModel != null && m_branchRateModel.isDirtyCalculation()) {
            m_nHasDirt = Tree.IS_FILTHY;
            return true;
        }
        if (m_data.get().isDirtyCalculation()) {
            m_nHasDirt = Tree.IS_FILTHY;
            return true;
        }
        if (m_siteModel.isDirtyCalculation()) {
            m_nHasDirt = Tree.IS_DIRTY;
            return true;
        }
        return m_tree.get().somethingIsDirty();
    }

    /**
     * Stores the additional state other than model components
     */
    @Override
    public void store() {
        partialBufferHelper.storeState();
        eigenBufferHelper.storeState();
        matrixBufferHelper.storeState();

        if (useScaleFactors || useAutoScaling) { // Only store when actually used
            scaleBufferHelper.storeState();
            System.arraycopy(scaleBufferIndices, 0, storedScaleBufferIndices, 0, scaleBufferIndices.length);
//            storedRescalingCount = rescalingCount;
        }
        super.store();
        System.arraycopy(m_branchLengths, 0, m_StoredBranchLengths, 0, m_branchLengths.length);
    }

    @Override
    public void restore() {
        updateSiteModel = true; // this is required to upload the categoryRates to BEAGLE after the restore

        partialBufferHelper.restoreState();
        eigenBufferHelper.restoreState();
        matrixBufferHelper.restoreState();

        if (useScaleFactors || useAutoScaling) {
            scaleBufferHelper.restoreState();
            int[] tmp = storedScaleBufferIndices;
            storedScaleBufferIndices = scaleBufferIndices;
            scaleBufferIndices = tmp;
//            rescalingCount = storedRescalingCount;
        }

//        updateRestrictedNodePartials = true;
        super.restore();
        double [] tmp = m_branchLengths;
        m_branchLengths = m_StoredBranchLengths;
        m_StoredBranchLengths = tmp;
    }

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    /**
     * Calculate the log likelihood of the current state.
     *
     * @return the log likelihood.
     */
    @Override
    public double calculateLogP() throws Exception {

        if (patternLogLikelihoods == null) {
            patternLogLikelihoods = new double[m_nPatternCount];
        }

        if (matrixUpdateIndices == null) {
            matrixUpdateIndices = new int[eigenCount][m_nNodeCount];
            branchLengths = new double[eigenCount][m_nNodeCount];
            branchUpdateCount = new int[eigenCount];
            scaleBufferIndices = new int[internalNodeCount];
            storedScaleBufferIndices = new int[internalNodeCount];
        }

        if (operations == null) {
            operations = new int[1][internalNodeCount * Beagle.OPERATION_TUPLE_SIZE];
            operationCount = new int[1];
        }

        recomputeScaleFactors = false;

        if (this.rescalingScheme == PartialsRescalingScheme.ALWAYS) {
            useScaleFactors = true;
            recomputeScaleFactors = true;
        } else if (this.rescalingScheme == PartialsRescalingScheme.DYNAMIC && everUnderflowed) {
            useScaleFactors = true;
            if (rescalingCountInner < RESCALE_TIMES) {
                recomputeScaleFactors = true;
                m_nHasDirt = Tree.IS_FILTHY;// makeDirty();
//                System.err.println("Recomputing scale factors");
            }

            rescalingCountInner++;
            rescalingCount++;
            if (rescalingCount > RESCALE_FREQUENCY) {
                rescalingCount = 0;
                rescalingCountInner = 0;
            }
        }

        for (int i = 0; i < eigenCount; i++) {
            branchUpdateCount[i] = 0;
        }
        operationListCount = 0;

        operationCount[0] = 0;

        final Node root = m_tree.get().getRoot();
        traverse(root, null, true);

        if (updateSubstitutionModel) { 
        	setUpSubstModel();
        }

        if (updateSiteModel) {
            double[] categoryRates = m_siteModel.getCategoryRates(null);
            beagle.setCategoryRates(categoryRates);
        }

        for (int i = 0; i < eigenCount; i++) {
            if (branchUpdateCount[i] > 0) {
                beagle.updateTransitionMatrices(
                    eigenBufferHelper.getOffsetIndex(i),
                    matrixUpdateIndices[i],
                    null,
                    null,
                    branchLengths[i],
                    branchUpdateCount[i]);
            }
        }

//        if (COUNT_TOTAL_OPERATIONS) {
//            for (int i = 0; i < eigenCount; i++) {
//                totalMatrixUpdateCount += branchUpdateCount[i];
//            }
//            
//            for (int i = 0; i <= numRestrictedPartials; i++) {
//                totalOperationCount += operationCount[i];
//            }
//        }

        double logL;
        boolean done;
        boolean firstRescaleAttempt = true;

        do {

        	beagle.updatePartials(operations[0], operationCount[0], Beagle.NONE);

            int rootIndex = partialBufferHelper.getOffsetIndex(root.getNr());

            double[] categoryWeights = m_siteModel.getCategoryProportions(null);
            double[] frequencies = m_substitutionModel.getFrequencies();

            int cumulateScaleBufferIndex = Beagle.NONE;
            if (useScaleFactors) {

                if (recomputeScaleFactors) {
                    scaleBufferHelper.flipOffset(internalNodeCount);
                    cumulateScaleBufferIndex = scaleBufferHelper.getOffsetIndex(internalNodeCount);
                    beagle.resetScaleFactors(cumulateScaleBufferIndex);
                    beagle.accumulateScaleFactors(scaleBufferIndices, internalNodeCount, cumulateScaleBufferIndex);
                } else {
                    cumulateScaleBufferIndex = scaleBufferHelper.getOffsetIndex(internalNodeCount);
                }
            } else if (useAutoScaling) {
                beagle.accumulateScaleFactors(scaleBufferIndices, internalNodeCount, Beagle.NONE);
            }

            // these could be set only when they change but store/restore would need to be considered
            beagle.setCategoryWeights(0, categoryWeights);
            beagle.setStateFrequencies(0, frequencies);

            double[] sumLogLikelihoods = new double[1];

            beagle.calculateRootLogLikelihoods(new int[]{rootIndex}, new int[]{0}, new int[]{0},
                    new int[]{cumulateScaleBufferIndex}, 1, sumLogLikelihoods);

            logL = sumLogLikelihoods[0];

            if (ascertainedSitePatterns) {
                // Need to correct for ascertainedSitePatterns
                beagle.getSiteLogLikelihoods(patternLogLikelihoods);
                logL = getAscertainmentCorrectedLogLikelihood((AscertainedAlignment) m_data.get(),
                        patternLogLikelihoods, m_data.get().getWeights());
            }

            if (Double.isNaN(logL) || Double.isInfinite(logL)) {
                everUnderflowed = true;
                logL = Double.NEGATIVE_INFINITY;

                if (firstRescaleAttempt && rescalingScheme == PartialsRescalingScheme.DYNAMIC) {
                    // we have had a potential under/over flow so attempt a rescaling
                    useScaleFactors = true;
                    recomputeScaleFactors = true;

                    for (int i = 0; i < eigenCount; i++) {
                        branchUpdateCount[i] = 0;
                    }

                    operationCount[0] = 0;

                    // traverse again but without flipping partials indices as we
                    // just want to overwrite the last attempt. We will flip the
                    // scale buffer indices though as we are recomputing them.
                    traverse(root, null, false);

                    done = false; // Run through do-while loop again
                    firstRescaleAttempt = false; // Only try to rescale once
                } else {
                    // we have already tried a rescale, not rescaling or always rescaling
                    // so just return the likelihood...
                    done = true;
                }
            } else {
                done = true; // No under-/over-flow, then done
            }

        } while (!done);

        // If these are needed...
        //beagle.getSiteLogLikelihoods(patternLogLikelihoods);

        //********************************************************************
        // after traverse all nodes and patterns have been updated --
        //so change flags to reflect this.
//        for (int i = 0; i < m_nNodeCount; i++) {
//            updateNode[i] = false;
//        }

        updateSubstitutionModel = false;
        updateSiteModel = false;
        //********************************************************************

        logP = logL;
        return logL;
    }

    protected void getPartials(int number, double[] partials) {
        int cumulativeBufferIndex = Beagle.NONE;
        /* No need to rescale partials */
        beagle.getPartials(partialBufferHelper.getOffsetIndex(number), cumulativeBufferIndex, partials);
    }

    protected void setPartials(int number, double[] partials) {
        beagle.setPartials(partialBufferHelper.getOffsetIndex(number), partials);
    }

    private double getAscertainmentCorrectedLogLikelihood(AscertainedAlignment patternList,
                                                          double[] patternLogLikelihoods,
                                                          int[] patternWeights) {
        double logL = 0.0;
        double ascertainmentCorrection = patternList.getAscertainmentCorrection(patternLogLikelihoods);
        for (int i = 0; i < m_nPatternCount; i++) {
            logL += (patternLogLikelihoods[i] - ascertainmentCorrection) * patternWeights[i];
        }
        return logL;
    }

    /**
     * Traverse the tree calculating partial likelihoods.
     *
     * @param tree           tree
     * @param node           node
     * @param operatorNumber operatorNumber
     * @param flip           flip
     * @return boolean
     */
    private int traverse(Node node, int[] operatorNumber, boolean flip) {

        int nodeNum = node.getNr();

        Node parent = node.getParent();

        if (operatorNumber != null) {
            operatorNumber[0] = -1;
        }

        // First update the transition probability matrix(ices) for this branch
        int update = (node.isDirty() | m_nHasDirt);
        if (!node.isRoot() && (update != Tree.IS_CLEAN)) {

            final double branchRate = (m_branchRateModel == null ? 1.0:m_branchRateModel.getRateForBranch(node));

            // Get the operational time of the branch
            final double branchTime = branchRate * (parent.getHeight() - node.getHeight());

            if (branchTime < 0.0) {
                throw new RuntimeException("Negative branch length: " + branchTime);
            }

            if (flip) {
                // first flip the matrixBufferHelper
                matrixBufferHelper.flipOffset(nodeNum);
            }

            // then set which matrix to update
            final int eigenIndex = 0;// = m_substitutionModel.getBranchIndex(node);
            final int updateCount = branchUpdateCount[eigenIndex];
            matrixUpdateIndices[eigenIndex][updateCount] = matrixBufferHelper.getOffsetIndex(nodeNum);

            branchLengths[eigenIndex][updateCount] = branchTime;
            branchUpdateCount[eigenIndex]++;

            update |= Tree.IS_DIRTY;
        }

        // If the node is internal, update the partial likelihoods.
        if (!node.isLeaf()) {

            // Traverse down the two child nodes
            Node child1 = node.m_left;
            final int[] op1 = {-1};
            final int update1 = traverse(child1, op1, flip);

            Node child2 = node.m_right;
            final int[] op2 = {-1};
            final int update2 = traverse(child2, op2, flip);

            // If either child node was updated then update this node too
            if (update1 != Tree.IS_CLEAN || update2 != Tree.IS_CLEAN) {

                int x = operationCount[operationListCount] * Beagle.OPERATION_TUPLE_SIZE;

                if (flip) {
                    // first flip the partialBufferHelper
                    partialBufferHelper.flipOffset(nodeNum);
                }

                final int[] operations = this.operations[operationListCount];

                operations[x] = partialBufferHelper.getOffsetIndex(nodeNum);

                if (useScaleFactors) {
                    // get the index of this scaling buffer
                    int n = nodeNum - tipCount;

                    if (recomputeScaleFactors) {
                        // flip the indicator: can take either n or (internalNodeCount + 1) - n
                        scaleBufferHelper.flipOffset(n);

                        // store the index
                        scaleBufferIndices[n] = scaleBufferHelper.getOffsetIndex(n);

                        operations[x + 1] = scaleBufferIndices[n]; // Write new scaleFactor
                        operations[x + 2] = Beagle.NONE;

                    } else {
                        operations[x + 1] = Beagle.NONE;
                        operations[x + 2] = scaleBufferIndices[n]; // Read existing scaleFactor
                    }

                } else {

                    if (useAutoScaling) {
                        scaleBufferIndices[nodeNum - tipCount] = partialBufferHelper.getOffsetIndex(nodeNum);
                    }
                    operations[x + 1] = Beagle.NONE; // Not using scaleFactors
                    operations[x + 2] = Beagle.NONE;
                }

                operations[x + 3] = partialBufferHelper.getOffsetIndex(child1.getNr()); // source node 1
                operations[x + 4] = matrixBufferHelper.getOffsetIndex(child1.getNr()); // source matrix 1
                operations[x + 5] = partialBufferHelper.getOffsetIndex(child2.getNr()); // source node 2
                operations[x + 6] = matrixBufferHelper.getOffsetIndex(child2.getNr()); // source matrix 2

                operationCount[operationListCount]++;

                update |= (update1|update2);

            }
        }

        return update;

    }

    // **************************************************************
    // INSTANCE VARIABLES
    // **************************************************************

    private int eigenCount;
    private int[][] matrixUpdateIndices;
    private double[][] branchLengths;
    private int[] branchUpdateCount;
    private int[] scaleBufferIndices;
    private int[] storedScaleBufferIndices;

    private int[][] operations;
    private int operationListCount;
    private int[] operationCount;

    protected BufferIndexHelper partialBufferHelper;
    private /*final*/ BufferIndexHelper eigenBufferHelper;
    protected BufferIndexHelper matrixBufferHelper;
    protected BufferIndexHelper scaleBufferHelper;

    protected /*final*/ int tipCount;
    protected /*final*/ int internalNodeCount;
    protected /*final*/ int m_nPatternCount;

    private PartialsRescalingScheme rescalingScheme;
    protected boolean useScaleFactors = false;
    private boolean useAutoScaling = false;
    private boolean recomputeScaleFactors = false;
    private boolean everUnderflowed = false;
    private int rescalingCount = 0;
    private int rescalingCountInner = 0;

    /**
     * the pattern likelihoods
     */
    protected double[] patternLogLikelihoods = null;

    /**
     * the number of rate categories
     */
    protected int categoryCount;

    /**
     * an array used to transfer tip partials
     */
    protected double[] tipPartials;

    /**
     * the BEAGLE library instance
     */
    protected Beagle beagle;

    /**
     * Flag to specify that the substitution model has changed
     */
    protected boolean updateSubstitutionModel;
    protected boolean storedUpdateSubstitutionModel;

    /**
     * Flag to specify that the site model has changed
     */
    protected boolean updateSiteModel;
    protected boolean storedUpdateSiteModel;

//    /***
//     * Flag to specify if LikelihoodCore supports dynamic rescaling
//     */
//    private boolean dynamicRescaling = false;


    /**
     * Flag to specify if site patterns are acertained
     */

    private boolean ascertainedSitePatterns = false;

    protected class BufferIndexHelper {
        /**
         * @param maxIndexValue the number of possible input values for the index
         * @param minIndexValue the minimum index value to have the mirrored buffers
         */
        BufferIndexHelper(int maxIndexValue, int minIndexValue) {
            this.maxIndexValue = maxIndexValue;
            this.minIndexValue = minIndexValue;

            offsetCount = maxIndexValue - minIndexValue;
            indexOffsets = new int[offsetCount];
            storedIndexOffsets = new int[offsetCount];
        }

        public int getBufferCount() {
            return 2 * offsetCount + minIndexValue;
        }

        void flipOffset(int i) {
            if (i >= minIndexValue) {
                indexOffsets[i - minIndexValue] = offsetCount - indexOffsets[i - minIndexValue];
            } // else do nothing
        }

        int getOffsetIndex(int i) {
            if (i < minIndexValue) {
                return i;
            }
            return indexOffsets[i - minIndexValue] + i;
        }

        void getIndices(int[] outIndices) {
            for (int i = 0; i < maxIndexValue; i++) {
                outIndices[i] = getOffsetIndex(i);
            }
        }

        void storeState() {
            System.arraycopy(indexOffsets, 0, storedIndexOffsets, 0, indexOffsets.length);

        }

        void restoreState() {
            int[] tmp = storedIndexOffsets;
            storedIndexOffsets = indexOffsets;
            indexOffsets = tmp;
        }

        private final int maxIndexValue;
        private final int minIndexValue;
        private final int offsetCount;

        private int[] indexOffsets;
        private int[] storedIndexOffsets;

    }
    
    public enum PartialsRescalingScheme {

        DEFAULT("default"),
        NONE("none"),
        DYNAMIC("dynamic"),
        AUTO("auto"),
        KICK_ASS("kickAss"),
        ALWAYS("always");

        PartialsRescalingScheme(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        private final String text;

        public static PartialsRescalingScheme parseFromString(String text) {
            for(PartialsRescalingScheme scheme : PartialsRescalingScheme.values()) {
                if (scheme.getText().compareToIgnoreCase(text) == 0)
                    return scheme;
            }
            return DEFAULT;
        }
    }
}