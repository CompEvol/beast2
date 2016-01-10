package beast.evolution.likelihood;


/**
 * standard likelihood core, uses no caching *
 */
public class BeerLikelihoodCore extends LikelihoodCore {
    protected int nrOfStates;
    protected int nrOfNodes;
    protected int nrOfPatterns;
    protected int partialsSize;
    protected int matrixSize;
    protected int nrOfMatrices;

    protected boolean integrateCategories;

    protected double[][][] partials;

    protected int[][] states;

    protected double[][][] matrices;

    protected int[] currentMatrixIndex;
    protected int[] storedMatrixIndex;
    protected int[] currentPartialsIndex;
    protected int[] storedPartialsIndex;

    protected boolean useScaling = false;

    protected double[][][] scalingFactors;

    private double scalingThreshold = 1.0E-100;
    double SCALE = 2;

    public BeerLikelihoodCore(int nrOfStates) {
        this.nrOfStates = nrOfStates;
    } // c'tor


    /**
     * Calculates partial likelihoods at a node when both children have states.
     */
    protected void calculateStatesStatesPruning(int[] iStates1, double[] matrices1,
                                                int[] iStates2, double[] matrices2,
                                                double[] partials3) {
        int v = 0;

        for (int l = 0; l < nrOfMatrices; l++) {

            for (int k = 0; k < nrOfPatterns; k++) {

                int state1 = iStates1[k];
                int state2 = iStates2[k];

                int w = l * matrixSize;

                if (state1 < nrOfStates && state2 < nrOfStates) {

                    for (int i = 0; i < nrOfStates; i++) {

                        partials3[v] = matrices1[w + state1] * matrices2[w + state2];

                        v++;
                        w += nrOfStates;
                    }

                } else if (state1 < nrOfStates) {
                    // child 2 has a gap or unknown state so treat it as unknown

                    for (int i = 0; i < nrOfStates; i++) {

                        partials3[v] = matrices1[w + state1];

                        v++;
                        w += nrOfStates;
                    }
                } else if (state2 < nrOfStates) {
                    // child 2 has a gap or unknown state so treat it as unknown

                    for (int i = 0; i < nrOfStates; i++) {

                        partials3[v] = matrices2[w + state2];

                        v++;
                        w += nrOfStates;
                    }
                } else {
                    // both children have a gap or unknown state so set partials to 1

                    for (int j = 0; j < nrOfStates; j++) {
                        partials3[v] = 1.0;
                        v++;
                    }
                }
            }
        }
    }

    /**
     * Calculates partial likelihoods at a node when one child has states and one has partials.
     */
    protected void calculateStatesPartialsPruning(int[] iStates1, double[] matrices1,
                                                  double[] partials2, double[] matrices2,
                                                  double[] partials3) {

        double sum, tmp;

        int u = 0;
        int v = 0;

        for (int l = 0; l < nrOfMatrices; l++) {
            for (int k = 0; k < nrOfPatterns; k++) {

                int state1 = iStates1[k];

                int w = l * matrixSize;

                if (state1 < nrOfStates) {


                    for (int i = 0; i < nrOfStates; i++) {

                        tmp = matrices1[w + state1];

                        sum = 0.0;
                        for (int j = 0; j < nrOfStates; j++) {
                            sum += matrices2[w] * partials2[v + j];
                            w++;
                        }

                        partials3[u] = tmp * sum;
                        u++;
                    }

                    v += nrOfStates;
                } else {
                    // Child 1 has a gap or unknown state so don't use it

                    for (int i = 0; i < nrOfStates; i++) {

                        sum = 0.0;
                        for (int j = 0; j < nrOfStates; j++) {
                            sum += matrices2[w] * partials2[v + j];
                            w++;
                        }

                        partials3[u] = sum;
                        u++;
                    }

                    v += nrOfStates;
                }
            }
        }
    }

    /**
     * Calculates partial likelihoods at a node when both children have partials.
     */
    protected void calculatePartialsPartialsPruning(double[] partials1, double[] matrices1,
                                                    double[] partials2, double[] matrices2,
                                                    double[] partials3) {
        double sum1, sum2;

        int u = 0;
        int v = 0;

        for (int l = 0; l < nrOfMatrices; l++) {

            for (int k = 0; k < nrOfPatterns; k++) {

                int w = l * matrixSize;

                for (int i = 0; i < nrOfStates; i++) {

                    sum1 = sum2 = 0.0;

                    for (int j = 0; j < nrOfStates; j++) {
                        sum1 += matrices1[w] * partials1[v + j];
                        sum2 += matrices2[w] * partials2[v + j];

                        w++;
                    }

                    partials3[u] = sum1 * sum2;
                    u++;
                }
                v += nrOfStates;
            }
        }
    }

    /**
     * Calculates partial likelihoods at a node when both children have states.
     */
    protected void calculateStatesStatesPruning(int[] iStates1, double[] matrices1,
                                                int[] iStates2, double[] matrices2,
                                                double[] partials3, int[] iMatrixMap) {
        int v = 0;

        for (int k = 0; k < nrOfPatterns; k++) {

            int state1 = iStates1[k];
            int state2 = iStates2[k];

            int w = iMatrixMap[k] * matrixSize;

            if (state1 < nrOfStates && state2 < nrOfStates) {

                for (int i = 0; i < nrOfStates; i++) {

                    partials3[v] = matrices1[w + state1] * matrices2[w + state2];

                    v++;
                    w += nrOfStates;
                }

            } else if (state1 < nrOfStates) {
                // child 2 has a gap or unknown state so treat it as unknown

                for (int i = 0; i < nrOfStates; i++) {

                    partials3[v] = matrices1[w + state1];

                    v++;
                    w += nrOfStates;
                }
            } else if (state2 < nrOfStates) {
                // child 2 has a gap or unknown state so treat it as unknown

                for (int i = 0; i < nrOfStates; i++) {

                    partials3[v] = matrices2[w + state2];

                    v++;
                    w += nrOfStates;
                }
            } else {
                // both children have a gap or unknown state so set partials to 1

                for (int j = 0; j < nrOfStates; j++) {
                    partials3[v] = 1.0;
                    v++;
                }
            }
        }
    }

    /**
     * Calculates partial likelihoods at a node when one child has states and one has partials.
     */
    protected void calculateStatesPartialsPruning(int[] iStates1, double[] matrices1,
                                                  double[] partials2, double[] matrices2,
                                                  double[] partials3, int[] iMatrixMap) {

        double sum, tmp;

        int u = 0;
        int v = 0;

        for (int k = 0; k < nrOfPatterns; k++) {

            int state1 = iStates1[k];

            int w = iMatrixMap[k] * matrixSize;

            if (state1 < nrOfStates) {

                for (int i = 0; i < nrOfStates; i++) {

                    tmp = matrices1[w + state1];

                    sum = 0.0;
                    for (int j = 0; j < nrOfStates; j++) {
                        sum += matrices2[w] * partials2[v + j];
                        w++;
                    }

                    partials3[u] = tmp * sum;
                    u++;
                }

                v += nrOfStates;
            } else {
                // Child 1 has a gap or unknown state so don't use it

                for (int i = 0; i < nrOfStates; i++) {

                    sum = 0.0;
                    for (int j = 0; j < nrOfStates; j++) {
                        sum += matrices2[w] * partials2[v + j];
                        w++;
                    }

                    partials3[u] = sum;
                    u++;
                }

                v += nrOfStates;
            }
        }
    }

    /**
     * Calculates partial likelihoods at a node when both children have partials.
     */
    protected void calculatePartialsPartialsPruning(double[] partials1, double[] matrices1,
                                                    double[] partials2, double[] matrices2,
                                                    double[] partials3, int[] iMatrixMap) {
        double sum1, sum2;

        int u = 0;
        int v = 0;

        for (int k = 0; k < nrOfPatterns; k++) {

            int w = iMatrixMap[k] * matrixSize;

            for (int i = 0; i < nrOfStates; i++) {

                sum1 = sum2 = 0.0;

                for (int j = 0; j < nrOfStates; j++) {
                    sum1 += matrices1[w] * partials1[v + j];
                    sum2 += matrices2[w] * partials2[v + j];

                    w++;
                }

                partials3[u] = sum1 * sum2;
                u++;
            }
            v += nrOfStates;
        }
    }

    /**
     * Integrates partials across categories.
     *
     * @param inPartials  the array of partials to be integrated
     * @param proportions the proportions of sites in each category
     * @param outPartials an array into which the partials will go
     */
    @Override
	protected void calculateIntegratePartials(double[] inPartials, double[] proportions, double[] outPartials) {

        int u = 0;
        int v = 0;
        for (int k = 0; k < nrOfPatterns; k++) {

            for (int i = 0; i < nrOfStates; i++) {

                outPartials[u] = inPartials[v] * proportions[0];
                u++;
                v++;
            }
        }


        for (int l = 1; l < nrOfMatrices; l++) {
            u = 0;

            for (int k = 0; k < nrOfPatterns; k++) {

                for (int i = 0; i < nrOfStates; i++) {

                    outPartials[u] += inPartials[v] * proportions[l];
                    u++;
                    v++;
                }
            }
        }
    }

    /**
     * Calculates pattern log likelihoods at a node.
     *
     * @param partials          the partials used to calculate the likelihoods
     * @param frequencies       an array of state frequencies
     * @param outLogLikelihoods an array into which the likelihoods will go
     */
    @Override
	public void calculateLogLikelihoods(double[] partials, double[] frequencies, double[] outLogLikelihoods) {
        int v = 0;
        for (int k = 0; k < nrOfPatterns; k++) {

            double sum = 0.0;
            for (int i = 0; i < nrOfStates; i++) {

                sum += frequencies[i] * partials[v];
                v++;
            }
            outLogLikelihoods[k] = Math.log(sum) + getLogScalingFactor(k);
        }
    }


    /**
     * initializes partial likelihood arrays.
     *
     * @param nodeCount           the number of nodes in the tree
     * @param patternCount        the number of patterns
     * @param matrixCount         the number of matrices (i.e., number of categories)
     * @param integrateCategories whether sites are being integrated over all matrices
     */
    @Override
	public void initialize(int nodeCount, int patternCount, int matrixCount, boolean integrateCategories, boolean useAmbiguities) {

        this.nrOfNodes = nodeCount;
        this.nrOfPatterns = patternCount;
        this.nrOfMatrices = matrixCount;

        this.integrateCategories = integrateCategories;

        if (integrateCategories) {
            partialsSize = patternCount * nrOfStates * matrixCount;
        } else {
            partialsSize = patternCount * nrOfStates;
        }

        partials = new double[2][nodeCount][];

        currentMatrixIndex = new int[nodeCount];
        storedMatrixIndex = new int[nodeCount];

        currentPartialsIndex = new int[nodeCount];
        storedPartialsIndex = new int[nodeCount];

        states = new int[nodeCount][];

        for (int i = 0; i < nodeCount; i++) {
            partials[0][i] = null;
            partials[1][i] = null;

            states[i] = null;
        }

        matrixSize = nrOfStates * nrOfStates;

        matrices = new double[2][nodeCount][matrixCount * matrixSize];
    }

    /**
     * cleans up and deallocates arrays.
     */
    @Override
	public void finalize() throws java.lang.Throwable {
        nrOfNodes = 0;
        nrOfPatterns = 0;
        nrOfMatrices = 0;

        partials = null;
        currentPartialsIndex = null;
        storedPartialsIndex = null;
        states = null;
        matrices = null;
        currentMatrixIndex = null;
        storedMatrixIndex = null;

        scalingFactors = null;
    }

    @Override
    public void setUseScaling(double scale) {
        useScaling = (scale != 1.0);

        if (useScaling) {
            scalingFactors = new double[2][nrOfNodes][nrOfPatterns];
        }
    }

    /**
     * Allocates partials for a node
     */
    @Override
	public void createNodePartials(int iNodeIndex) {

        this.partials[0][iNodeIndex] = new double[partialsSize];
        this.partials[1][iNodeIndex] = new double[partialsSize];
    }

    /**
     * Sets partials for a node
     */
    @Override
	public void setNodePartials(int iNodeIndex, double[] partials) {

        if (this.partials[0][iNodeIndex] == null) {
            createNodePartials(iNodeIndex);
        }
        if (partials.length < partialsSize) {
            int k = 0;
            for (int i = 0; i < nrOfMatrices; i++) {
                System.arraycopy(partials, 0, this.partials[0][iNodeIndex], k, partials.length);
                k += partials.length;
            }
        } else {
            System.arraycopy(partials, 0, this.partials[0][iNodeIndex], 0, partials.length);
        }
    }

    @Override
    public void getNodePartials(int iNodeIndex, double[] partialsOut) {
        System.arraycopy(partials[currentPartialsIndex[iNodeIndex]][iNodeIndex], 0, partialsOut, 0, partialsOut.length);
    }

    /**
     * Allocates states for a node
     */
    public void createNodeStates(int iNodeIndex) {

        this.states[iNodeIndex] = new int[nrOfPatterns];
    }

    /**
     * Sets states for a node
     */
    @Override
	public void setNodeStates(int iNodeIndex, int[] iStates) {

        if (this.states[iNodeIndex] == null) {
            createNodeStates(iNodeIndex);
        }
        System.arraycopy(iStates, 0, this.states[iNodeIndex], 0, nrOfPatterns);
    }

    /**
     * Gets states for a node
     */
    @Override
	public void getNodeStates(int iNodeIndex, int[] iStates) {
        System.arraycopy(this.states[iNodeIndex], 0, iStates, 0, nrOfPatterns);
    }

    @Override
    public void setNodeMatrixForUpdate(int iNodeIndex) {
        currentMatrixIndex[iNodeIndex] = 1 - currentMatrixIndex[iNodeIndex];

    }


    /**
     * Sets probability matrix for a node
     */
    @Override
	public void setNodeMatrix(int iNodeIndex, int iMatrixIndex, double[] matrix) {
        System.arraycopy(matrix, 0, matrices[currentMatrixIndex[iNodeIndex]][iNodeIndex],
                iMatrixIndex * matrixSize, matrixSize);
    }

    public void setPaddedNodeMatrices(int iNode, double[] matrix) {
        System.arraycopy(matrix, 0, matrices[currentMatrixIndex[iNode]][iNode],
                0, nrOfMatrices * matrixSize);
    }


    /**
     * Gets probability matrix for a node
     */
    @Override
	public void getNodeMatrix(int iNodeIndex, int iMatrixIndex, double[] matrix) {
        System.arraycopy(matrices[currentMatrixIndex[iNodeIndex]][iNodeIndex],
                iMatrixIndex * matrixSize, matrix, 0, matrixSize);
    }

    @Override
    public void setNodePartialsForUpdate(int iNodeIndex) {
        currentPartialsIndex[iNodeIndex] = 1 - currentPartialsIndex[iNodeIndex];
    }

    /**
     * Sets the currently updating node partials for node nodeIndex. This may
     * need to repeatedly copy the partials for the different category partitions
     */
    public void setCurrentNodePartials(int iNodeIndex, double[] partials) {
        if (partials.length < partialsSize) {
            int k = 0;
            for (int i = 0; i < nrOfMatrices; i++) {
                System.arraycopy(partials, 0, this.partials[currentPartialsIndex[iNodeIndex]][iNodeIndex], k, partials.length);
                k += partials.length;
            }
        } else {
            System.arraycopy(partials, 0, this.partials[currentPartialsIndex[iNodeIndex]][iNodeIndex], 0, partials.length);
        }
    }

    /**
     * Calculates partial likelihoods at a node.
     *
     * @param iNodeIndex1 the 'child 1' node
     * @param iNodeIndex2 the 'child 2' node
     * @param iNodeIndex3 the 'parent' node
     */
    @Override
	public void calculatePartials(int iNodeIndex1, int iNodeIndex2, int iNodeIndex3) {
        if (states[iNodeIndex1] != null) {
            if (states[iNodeIndex2] != null) {
                calculateStatesStatesPruning(
                        states[iNodeIndex1], matrices[currentMatrixIndex[iNodeIndex1]][iNodeIndex1],
                        states[iNodeIndex2], matrices[currentMatrixIndex[iNodeIndex2]][iNodeIndex2],
                        partials[currentPartialsIndex[iNodeIndex3]][iNodeIndex3]);
            } else {
                calculateStatesPartialsPruning(states[iNodeIndex1], matrices[currentMatrixIndex[iNodeIndex1]][iNodeIndex1],
                        partials[currentPartialsIndex[iNodeIndex2]][iNodeIndex2], matrices[currentMatrixIndex[iNodeIndex2]][iNodeIndex2],
                        partials[currentPartialsIndex[iNodeIndex3]][iNodeIndex3]);
            }
        } else {
            if (states[iNodeIndex2] != null) {
                calculateStatesPartialsPruning(states[iNodeIndex2], matrices[currentMatrixIndex[iNodeIndex2]][iNodeIndex2],
                        partials[currentPartialsIndex[iNodeIndex1]][iNodeIndex1], matrices[currentMatrixIndex[iNodeIndex1]][iNodeIndex1],
                        partials[currentPartialsIndex[iNodeIndex3]][iNodeIndex3]);
            } else {
                calculatePartialsPartialsPruning(partials[currentPartialsIndex[iNodeIndex1]][iNodeIndex1], matrices[currentMatrixIndex[iNodeIndex1]][iNodeIndex1],
                        partials[currentPartialsIndex[iNodeIndex2]][iNodeIndex2], matrices[currentMatrixIndex[iNodeIndex2]][iNodeIndex2],
                        partials[currentPartialsIndex[iNodeIndex3]][iNodeIndex3]);
            }
        }

        if (useScaling) {
            scalePartials(iNodeIndex3);
        }

//
//        int k =0;
//        for (int i = 0; i < patternCount; i++) {
//            double f = 0.0;
//
//            for (int j = 0; j < stateCount; j++) {
//                f += partials[currentPartialsIndices[nodeIndex3]][nodeIndex3][k];
//                k++;
//            }
//            if (f == 0.0) {
//                Logger.getLogger("error").severe("A partial likelihood (node index = " + nodeIndex3 + ", pattern = "+ i +") is zero for all states.");
//            }
//        }
    }

    /**
     * Calculates partial likelihoods at a node.
     *
     * @param iNodeIndex1 the 'child 1' node
     * @param iNodeIndex2 the 'child 2' node
     * @param iNodeIndex3 the 'parent' node
     * @param iMatrixMap  a map of which matrix to use for each pattern (can be null if integrating over categories)
     */
    public void calculatePartials(int iNodeIndex1, int iNodeIndex2, int iNodeIndex3, int[] iMatrixMap) {
        if (states[iNodeIndex1] != null) {
            if (states[iNodeIndex2] != null) {
                calculateStatesStatesPruning(
                        states[iNodeIndex1], matrices[currentMatrixIndex[iNodeIndex1]][iNodeIndex1],
                        states[iNodeIndex2], matrices[currentMatrixIndex[iNodeIndex2]][iNodeIndex2],
                        partials[currentPartialsIndex[iNodeIndex3]][iNodeIndex3], iMatrixMap);
            } else {
                calculateStatesPartialsPruning(
                        states[iNodeIndex1], matrices[currentMatrixIndex[iNodeIndex1]][iNodeIndex1],
                        partials[currentPartialsIndex[iNodeIndex2]][iNodeIndex2], matrices[currentMatrixIndex[iNodeIndex2]][iNodeIndex2],
                        partials[currentPartialsIndex[iNodeIndex3]][iNodeIndex3], iMatrixMap);
            }
        } else {
            if (states[iNodeIndex2] != null) {
                calculateStatesPartialsPruning(
                        states[iNodeIndex2], matrices[currentMatrixIndex[iNodeIndex2]][iNodeIndex2],
                        partials[currentPartialsIndex[iNodeIndex1]][iNodeIndex1], matrices[currentMatrixIndex[iNodeIndex1]][iNodeIndex1],
                        partials[currentPartialsIndex[iNodeIndex3]][iNodeIndex3], iMatrixMap);
            } else {
                calculatePartialsPartialsPruning(
                        partials[currentPartialsIndex[iNodeIndex1]][iNodeIndex1], matrices[currentMatrixIndex[iNodeIndex1]][iNodeIndex1],
                        partials[currentPartialsIndex[iNodeIndex2]][iNodeIndex2], matrices[currentMatrixIndex[iNodeIndex2]][iNodeIndex2],
                        partials[currentPartialsIndex[iNodeIndex3]][iNodeIndex3], iMatrixMap);
            }
        }

        if (useScaling) {
            scalePartials(iNodeIndex3);
        }
    }


    @Override
	public void integratePartials(int iNodeIndex, double[] proportions, double[] outPartials) {
        calculateIntegratePartials(partials[currentPartialsIndex[iNodeIndex]][iNodeIndex], proportions, outPartials);
    }


    /**
     * Scale the partials at a given node. This uses a scaling suggested by Ziheng Yang in
     * Yang (2000) J. Mol. Evol. 51: 423-432
     * <p/>
     * This function looks over the partial likelihoods for each state at each pattern
     * and finds the largest. If this is less than the scalingThreshold (currently set
     * to 1E-40) then it rescales the partials for that pattern by dividing by this number
     * (i.e., normalizing to between 0, 1). It then stores the log of this scaling.
     * This is called for every internal node after the partials are calculated so provides
     * most of the performance hit. Ziheng suggests only doing this on a proportion of nodes
     * but this sounded like a headache to organize (and he doesn't use the threshold idea
     * which improves the performance quite a bit).
     *
     * @param iNodeIndex
     */
    protected void scalePartials(int iNodeIndex) {
//        int v = 0;
//    	double [] partials = m_fPartials[m_iCurrentPartialsIndices[iNodeIndex]][iNodeIndex];
//        for (int i = 0; i < m_nPatternCount; i++) {
//            for (int k = 0; k < m_nMatrixCount; k++) {
//                for (int j = 0; j < m_nStateCount; j++) {
//                	partials[v] *= SCALE;
//                	v++;
//                }
//            }
//        }
        int u = 0;

        for (int i = 0; i < nrOfPatterns; i++) {

            double scaleFactor = 0.0;
            int v = u;
            for (int k = 0; k < nrOfMatrices; k++) {
                for (int j = 0; j < nrOfStates; j++) {
                    if (partials[currentPartialsIndex[iNodeIndex]][iNodeIndex][v] > scaleFactor) {
                        scaleFactor = partials[currentPartialsIndex[iNodeIndex]][iNodeIndex][v];
                    }
                    v++;
                }
                v += (nrOfPatterns - 1) * nrOfStates;
            }

            if (scaleFactor < scalingThreshold) {

                v = u;
                for (int k = 0; k < nrOfMatrices; k++) {
                    for (int j = 0; j < nrOfStates; j++) {
                        partials[currentPartialsIndex[iNodeIndex]][iNodeIndex][v] /= scaleFactor;
                        v++;
                    }
                    v += (nrOfPatterns - 1) * nrOfStates;
                }
                scalingFactors[currentPartialsIndex[iNodeIndex]][iNodeIndex][i] = Math.log(scaleFactor);

            } else {
                scalingFactors[currentPartialsIndex[iNodeIndex]][iNodeIndex][i] = 0.0;
            }
            u += nrOfStates;


        }
    }

    /**
     * This function returns the scaling factor for that pattern by summing over
     * the log scalings used at each node. If scaling is off then this just returns
     * a 0.
     *
     * @return the log scaling factor
     */
    @Override
	public double getLogScalingFactor(int iPattern) {
//    	if (m_bUseScaling) {
//    		return -(m_nNodeCount/2) * Math.log(SCALE);
//    	} else {
//    		return 0;
//    	}        
        double logScalingFactor = 0.0;
        if (useScaling) {
            for (int i = 0; i < nrOfNodes; i++) {
                logScalingFactor += scalingFactors[currentPartialsIndex[i]][i][iPattern];
            }
        }
        return logScalingFactor;
    }

    /**
     * Gets the partials for a particular node.
     *
     * @param iNodeIndex   the node
     * @param outPartials an array into which the partials will go
     */
    public void getPartials(int iNodeIndex, double[] outPartials) {
        double[] partials1 = partials[currentPartialsIndex[iNodeIndex]][iNodeIndex];

        System.arraycopy(partials1, 0, outPartials, 0, partialsSize);
    }

    /**
     * Store current state
     */
    @Override
    public void restore() {
        // Rather than copying the stored stuff back, just swap the pointers...
        int[] iTmp1 = currentMatrixIndex;
        currentMatrixIndex = storedMatrixIndex;
        storedMatrixIndex = iTmp1;

        int[] iTmp2 = currentPartialsIndex;
        currentPartialsIndex = storedPartialsIndex;
        storedPartialsIndex = iTmp2;
    }

    @Override
	public void unstore() {
        System.arraycopy(storedMatrixIndex, 0, currentMatrixIndex, 0, nrOfNodes);
        System.arraycopy(storedPartialsIndex, 0, currentPartialsIndex, 0, nrOfNodes);
    }

    /**
     * Restore the stored state
     */
    @Override
    public void store() {
        System.arraycopy(currentMatrixIndex, 0, storedMatrixIndex, 0, nrOfNodes);
        System.arraycopy(currentPartialsIndex, 0, storedPartialsIndex, 0, nrOfNodes);
    }


//	@Override
//    public void calcRootPsuedoRootPartials(double[] frequencies, int iNode, double [] pseudoPartials) {
//		int u = 0;
//		double [] inPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
//		for (int k = 0; k < m_nPatterns; k++) {
//			for (int l = 0; l < m_nMatrices; l++) {
//				for (int i = 0; i < m_nStates; i++) {
//					pseudoPartials[u] = inPartials[u] * frequencies[i];
//					u++;
//				}
//			}
//		}
//    }
//	@Override
//    public void calcNodePsuedoRootPartials(double[] inPseudoPartials, int iNode, double [] outPseudoPartials) {
//		double [] partials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
//		double [] oldPartials = m_fPartials[m_iStoredPartials[iNode]][iNode];
//		int maxK = m_nPatterns * m_nMatrices * m_nStates; 
//		for (int k = 0; k < maxK; k++) {
//			outPseudoPartials[k] = inPseudoPartials[k] * partials[k] / oldPartials[k];
//		}
//	}
//    
//	@Override
//    public void calcPsuedoRootPartials(double [] parentPseudoPartials, int iNode, double [] pseudoPartials) {
//		int v = 0;
//		int u = 0;
//		double [] matrices = m_fMatrices[m_iCurrentMatrices[iNode]][iNode];
//		for (int k = 0; k < m_nPatterns; k++) {
//			for (int l = 0; l < m_nMatrices; l++) {
//				for (int i = 0; i < m_nStates; i++) {
//					int w = 0;
//					double sum = 0;
//					for (int j = 0; j < m_nStates; j++) {
//					      sum += parentPseudoPartials[u+j] * matrices[w + i];
//					      w+=m_nStates;
//					}
//					pseudoPartials[v] = sum;
//					v++;
////					int w = l * m_nMatrixSize;
////					double sum = 0;
////					for (int j = 0; j < m_nStates; j++) {
////					      sum += parentPseudoPartials[u+j] * matrices[w+j];
////					}
////					pseudoPartials[v] = sum;
////					v++;
//				}
//				u += m_nStates;
//			}
//		}
//    }
//
//
//    @Override
//    void integratePartialsP(double [] inPartials, double [] proportions, double [] m_fRootPartials) {
//		int maxK = m_nPatterns * m_nStates;
//		for (int k = 0; k < maxK; k++) {
//			m_fRootPartials[k] = inPartials[k] * proportions[0];
//		}
//
//		for (int l = 1; l < m_nMatrices; l++) {
//			int n = maxK * l;
//			for (int k = 0; k < maxK; k++) {
//				m_fRootPartials[k] += inPartials[n+k] * proportions[l];
//			}
//		}
//    } // integratePartials
//
//	/**
//	 * Calculates pattern log likelihoods at a node.
//	 * @param partials the partials used to calculate the likelihoods
//	 * @param frequencies an array of state frequencies
//	 * @param outLogLikelihoods an array into which the likelihoods will go
//	 */
//    @Override
//	public void calculateLogLikelihoodsP(double[] partials,double[] outLogLikelihoods)
//	{
//        int v = 0;
//		for (int k = 0; k < m_nPatterns; k++) {
//            double sum = 0.0;
//			for (int i = 0; i < m_nStates; i++) {
//				sum += partials[v];
//				v++;
//			}
//            outLogLikelihoods[k] = Math.log(sum) + getLogScalingFactor(k);
//		}
//	}
//	
//	
//	//    @Override
////    LikelihoodCore feelsGood() {return null;}
} // class BeerLikelihoodCore
