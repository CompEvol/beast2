package beagle;

public class GeneralBeagleImpl implements Beagle {

    public static final boolean DEBUG = false;
    public static final boolean SCALING = true;

    // These settings are chosen for single precision computation.
    // The single precision exponents go from -126 to 127 (2 ^ x)
    public static final int SCALING_FACTOR_COUNT = 254; // -126, 127
    public static final int SCALING_FACTOR_OFFSET = 126; // the zero point
    private static final int SCALING_EXPONENT_THRESHOLD = 2;

    protected final int tipCount;
    protected final int partialsBufferCount;
    protected final int compactBufferCount;
    protected final int stateCount;
    protected final int patternCount;
    protected final int eigenBufferCount;
    protected final int matrixBufferCount;
    protected final int categoryCount;

    protected int partialsSize;
    protected int matrixSize;

    protected double[][] cMatrices;
    protected double[][] eigenValues;

    protected double[][] stateFrequencies;

    protected double[] categoryRates;
    protected double[][] categoryWeights;

    protected double[] patternWeights;
    protected double[][] partials;
    protected int[][] scalingFactorCounts;

    protected int[][] tipStates;

    protected double[][] matrices;

    double[] tmpPartials;


    protected double[] scalingFactors;
    protected double[] logScalingFactors;

    /**
     * Constructor
     *
     * @param stateCount number of states
     */
    public GeneralBeagleImpl(final int tipCount,
                             final int partialsBufferCount,
                             final int compactBufferCount,
                             final int stateCount,
                             final int patternCount,
                             final int eigenBufferCount,
                             final int matrixBufferCount,
                             final int categoryCount,
                             final int scaleBufferCount) {

        this.tipCount = tipCount;
        this.partialsBufferCount = partialsBufferCount;
        this.compactBufferCount = compactBufferCount;
        this.stateCount = stateCount;
        this.patternCount = patternCount;
        this.eigenBufferCount = eigenBufferCount;
        this.matrixBufferCount = matrixBufferCount;
        this.categoryCount = categoryCount;

//        Logger.getLogger("beagle").info("Constructing double-precision Java BEAGLE implementation.");

        if (patternCount < 1) {
            throw new IllegalArgumentException("Pattern count must be at least 1");
        }

        if (categoryCount < 1) {
            throw new IllegalArgumentException("Category count must be at least 1");
        }

        cMatrices = new double[eigenBufferCount][stateCount * stateCount * stateCount];

        eigenValues = new double[eigenBufferCount][stateCount];

        stateFrequencies = new double[eigenBufferCount][stateCount];

        categoryWeights = new double[eigenBufferCount][categoryCount];
        categoryRates = new double[categoryCount];

        partialsSize = patternCount * stateCount * categoryCount;

        patternWeights = new double[patternCount];

        tipStates = new int[compactBufferCount][];
        partials = new double[partialsBufferCount][];
        for (int i = 0; i < partialsBufferCount; i++) {
            partials[i] = new double[partialsSize];
        }

        if (SCALING) {
            // create the scaling factor accumulation counts.
            // These mirror each partials buffer
            scalingFactorCounts = new int[partialsBufferCount][];
            for (int i = 0; i < partialsBufferCount; i++) {
                scalingFactorCounts[i] = new int[SCALING_FACTOR_COUNT];
            }

            // Create the scaling factor look up tables. These
            // could be statics I guess.
            scalingFactors = new double[SCALING_FACTOR_COUNT];
            logScalingFactors = new double[SCALING_FACTOR_COUNT];
            int exponent = -126;
            for (int i = 0; i < SCALING_FACTOR_COUNT; i++) {
                scalingFactors[i] = Math.pow(2.0, exponent);
                logScalingFactors[i] = Math.log(scalingFactors[i]);
                exponent ++;
            }
        }

        tmpPartials = new double[patternCount * stateCount];

        matrixSize = stateCount * stateCount;

        matrices = new double[matrixBufferCount][categoryCount * matrixSize];
    }

    public void finalize() throws Throwable {
        super.finalize();
    }

    public void setPatternWeights(final double[] patternWeights) {
        System.arraycopy(patternWeights, 0, this.patternWeights, 0, this.patternWeights.length);
    }

    /**
     * Sets partials for a tip - these are numbered from 0 and remain
     * constant throughout the run.
     *
     * @param tipIndex the tip index
     * @param states   an array of patternCount state indices
     */
    public void setTipStates(int tipIndex, int[] states) {
        assert(tipIndex >= 0 && tipIndex < tipCount);
        if (this.tipStates[tipIndex] == null) {
            tipStates[tipIndex] = new int[patternCount];
        }
        int k = 0;
        for (int state : states) {
            this.tipStates[tipIndex][k] = (state < stateCount ? state : stateCount);
            k++;
        }

    }

    public void getTipStates(int tipIndex, int[] states) {
        assert(tipIndex >= 0 && tipIndex < tipCount);
        if (this.tipStates[tipIndex] == null) {
            throw new RuntimeException("Unset tip states");
        }
        System.arraycopy(this.tipStates[tipIndex], 0, states, 0, states.length);
    }

    public void setTipPartials(int tipIndex, double[] inPartials) {
        assert(tipIndex >= 0 && tipIndex < tipCount);
        if (this.partials[tipIndex] == null) {
            this.partials[tipIndex] = new double[partialsSize];
        }
        int k = 0;
        for (int i = 0; i < categoryCount; i++) {
            System.arraycopy(inPartials, 0, this.partials[tipIndex], k, inPartials.length);
            k += inPartials.length;
        }
    }

    public void setPartials(final int bufferIndex, final double[] partials) {
        assert(this.partials[bufferIndex] != null);
        System.arraycopy(partials, 0, this.partials[bufferIndex], 0, partialsSize);
    }

    public void getPartials(final int bufferIndex, final int scaleIndex, final double[] partials) {
        System.arraycopy(this.partials[bufferIndex], 0, partials, 0, partialsSize);
    }

    public void setEigenDecomposition(int eigenIndex, double[] eigenVectors, double[] inverseEigenValues, double[] eigenValues) {
        int l =0;
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < stateCount; j++) {
                for (int k = 0; k < stateCount; k++) {
                    cMatrices[eigenIndex][l] = eigenVectors[(i * stateCount) + k] * inverseEigenValues[(k * stateCount) + j];
                    l++;
                }
            }
        }
        System.arraycopy(eigenValues, 0, this.eigenValues[eigenIndex], 0, eigenValues.length);
    }

    public void setStateFrequencies(final int stateFrequenciesIndex, final double[] stateFrequencies) {
        System.arraycopy(stateFrequencies, 0, this.stateFrequencies[stateFrequenciesIndex], 0, stateCount);
    }

    public void setCategoryWeights(final int categoryWeightsIndex, final double[] categoryWeights) {
        System.arraycopy(categoryWeights, 0, this.categoryWeights[categoryWeightsIndex], 0, categoryCount);
    }

    public void setCategoryRates(double[] categoryRates) {
        System.arraycopy(categoryRates, 0, this.categoryRates, 0, this.categoryRates.length);
    }

    public void setTransitionMatrix(final int matrixIndex, final double[] inMatrix, final double paddedValue) {
        System.arraycopy(inMatrix, 0, this.matrices[matrixIndex], 0, this.matrixSize);
    }

    public void getTransitionMatrix(final int matrixIndex, final double[] outMatrix) {
        System.arraycopy(this.matrices[matrixIndex],0,outMatrix,0,outMatrix.length);
    }

    public void updateTransitionMatrices(final int eigenIndex,
                                         final int[] probabilityIndices,
                                         final int[] firstDerivativeIndices,
                                         final int[] secondDervativeIndices,
                                         final double[] edgeLengths,
                                         final int count) {
        for (int u = 0; u < count; u++) {
            int matrixIndex = probabilityIndices[u];

            if (DEBUG) System.err.println("Updating matrix for node " + matrixIndex);

            double[] tmp = new double[stateCount];

            int n = 0;
            for (int l = 0; l < categoryCount; l++) {
//	    if (DEBUG) System.err.println("1: Rate "+l+" = "+categoryRates[l]);
                for (int i = 0; i < stateCount; i++) {
                    tmp[i] =  Math.exp(eigenValues[eigenIndex][i] * edgeLengths[u] * categoryRates[l]);
                }
//            if (DEBUG) System.err.println(new dr.math.matrixAlgebra.Vector(tmp));
                //        if (DEBUG) System.exit(-1);

                int m = 0;
                for (int i = 0; i < stateCount; i++) {
                    for (int j = 0; j < stateCount; j++) {
                        double sum = 0.0;
                        for (int k = 0; k < stateCount; k++) {
                            sum += cMatrices[eigenIndex][m] * tmp[k];
                            m++;
                        }
                        //	    if (DEBUG) System.err.println("1: matrices[][]["+n+"] = "+sum);
                        if (sum > 0)
                            matrices[matrixIndex][n] = sum;
                        else
                            matrices[matrixIndex][n] = 0; // TODO Decision: set to -sum (as BEAST does)
                        n++;
                    }
                }

//            if (DEBUG) System.err.println(new dr.math.matrixAlgebra.Vector(matrices[currentMatricesIndices[nodeIndex]][nodeIndex]));
//            if (DEBUG) System.exit(0);
            }
        }
    }

    /**
     * Operations list is a list of 7-tuple integer indices, with one 7-tuple per operation.
     * Format of 7-tuple operation: {destinationPartials,
     *                               destinationScaleWrite,
     *                               destinationScaleRead,
     *                               child1Partials,
     *                               child1TransitionMatrix,
     *                               child2Partials,
     *                               child2TransitionMatrix}
     *
     */
    public void updatePartials(final int[] operations, final int operationCount, final int cumulativeScaleIndex) {

        int x = 0;
        for (int op = 0; op < operationCount; op++) {
            int bufferIndex3 = operations[x];
            int bufferIndex1 = operations[x + 3];
            int matrixIndex1 = operations[x + 4];
            int bufferIndex2 = operations[x + 5];
            int matrixIndex2 = operations[x + 6];

            x += Beagle.OPERATION_TUPLE_SIZE;

            int exponent = 0;

            if (compactBufferCount == 0) {
                exponent = updatePartialsPartials(bufferIndex1, matrixIndex1, bufferIndex2, matrixIndex2, bufferIndex3);
            } else {
                if (bufferIndex1 < tipCount && tipStates[bufferIndex1] != null) {
                    if (bufferIndex2 < tipCount && tipStates[bufferIndex2] != null) {
                        exponent = updateStatesStates(bufferIndex1, matrixIndex1, bufferIndex2, matrixIndex2, bufferIndex3);
                    } else {
                        exponent = updateStatesPartials(bufferIndex1, matrixIndex1, bufferIndex2, matrixIndex2, bufferIndex3);
                    }
                } else {
                    if (bufferIndex2 < tipCount && tipStates[bufferIndex2] != null) {
                        exponent = updateStatesPartials(bufferIndex2, matrixIndex2, bufferIndex1, matrixIndex1, bufferIndex3);
                    } else {
                        exponent = updatePartialsPartials(bufferIndex1, matrixIndex1, bufferIndex2, matrixIndex2, bufferIndex3);
                    }
                }
            }

            if (SCALING) {
                if (exponent > SCALING_EXPONENT_THRESHOLD) {
                    rescalePartials(bufferIndex3);
                }
            }
        }
    }

    private void rescalePartials(final int bufferIndex) {

        double[] partials = this.partials[bufferIndex];
        int[] counts = scalingFactorCounts[bufferIndex];

        if (DEBUG) {
            System.err.println("rescaling buffer "+ bufferIndex);
        }

        int u = 0;
        for (int l = 0; l < categoryCount; l++) {

            for (int k = 0; k < patternCount; k++) {

                double maxValue = partials[u];

                for (int i = 1; i < stateCount; i++) {

                    if (partials[u + i] > maxValue) {
                        maxValue = partials[u + i];
                    }
                }

                // find the exponent for the largest value
                int exponent = Math.getExponent(maxValue);
                if (exponent != 0) {
                    // invert and offset it to get the index of the appropriate scale factor
                    int index = SCALING_FACTOR_OFFSET - exponent;
                    double scalingFactor = scalingFactors[index];

                    // increment the count of how many times this factor has been used
                    counts[index] += patternWeights[k];
                    if (DEBUG) {
                        System.err.println("exponent "+ exponent + ", index " + index + ", factor " + scalingFactor);
                    }

                    // do the rescaling
                    for (int i = 0; i < stateCount; i++) {
                        partials[u] *= scalingFactor;
                        u++;
                    }
                }
            }
        }
    }

    public void accumulateScaleFactors(int[] scaleIndices, int count, int outScaleIndex) {
//        throw new UnsupportedOperationException("accumulateScaleFactors not implemented in GeneralBeagleImpl");

    }

    public void removeScaleFactors(int[] scaleIndices, int count, int cumulativeScaleIndex) {
//        throw new UnsupportedOperationException("accumulateScaleFactors not implemented in GeneralBeagleImpl");
    }

    public void copyScaleFactors(int destScalingIndex, int srcScalingIndex) {
//        throw new UnsupportedOperationException("accumulateScaleFactors not implemented in GeneralBeagleImpl");
    }

    public void resetScaleFactors(int cumulativeScaleIndex) {
//        throw new UnsupportedOperationException("accumulateScaleFactors not implemented in GeneralBeagleImpl");
    }

    /**
     * Calculates partial likelihoods at a node when both children have states.
     * @returns the larges absolute exponent
     */
    protected int updateStatesStates(int bufferIndex1, int matrixIndex1, int bufferIndex2, int matrixIndex2, int bufferIndex3)
    {
        double[] matrices1 = matrices[matrixIndex1];
        double[] matrices2 = matrices[matrixIndex2];

        int[] states1 = tipStates[bufferIndex1];
        int[] states2 = tipStates[bufferIndex2];

        double[] partials3 = partials[bufferIndex3];

        if (SCALING) {
            // zero the scaling factor counts for this node (only tips below)
            int[] counts3 = scalingFactorCounts[bufferIndex3];
            for (int i = 0; i < counts3.length; i++) {
                counts3[i] = 0;
            }
        }

        int v = 0;

        for (int l = 0; l < categoryCount; l++) {

            for (int k = 0; k < patternCount; k++) {

                int state1 = states1[k];
                int state2 = states2[k];

                int w = l * matrixSize;

                if (state1 < stateCount && state2 < stateCount) {

                    for (int i = 0; i < stateCount; i++) {

                        partials3[v] = matrices1[w + state1] * matrices2[w + state2];

                        v++;
                        w += stateCount;
                    }

                } else if (state1 < stateCount) {
                    // child 2 has a gap or unknown state so treat it as unknown

                    for (int i = 0; i < stateCount; i++) {

                        partials3[v] = matrices1[w + state1];

                        v++;
                        w += stateCount;
                    }
                } else if (state2 < stateCount) {
                    // child 2 has a gap or unknown state so treat it as unknown

                    for (int i = 0; i < stateCount; i++) {

                        partials3[v] = matrices2[w + state2];

                        v++;
                        w += stateCount;
                    }
                } else {
                    // both children have a gap or unknown state so set partials to 1

                    for (int j = 0; j < stateCount; j++) {
                        partials3[v] = 1.0;
                        v++;
                    }
                }

            }
        }

        return 0; // don't bother checking exponents for cherries
    }

    /**
     * Calculates partial likelihoods at a node when one child has states and one has partials.
     */
    protected int updateStatesPartials(int bufferIndex1, int matrixIndex1, int bufferIndex2, int matrixIndex2, int bufferIndex3)
    {
        double[] matrices1 = matrices[matrixIndex1];
        double[] matrices2 = matrices[matrixIndex2];

        int[] states1 = tipStates[bufferIndex1];
        double[] partials2 = partials[bufferIndex2];

        double[] partials3 = partials[bufferIndex3];

        double sum, tmp;

        int u = 0;
        int v = 0;

        int exponent = 0;

        if (SCALING) {
            int[] counts2 = scalingFactorCounts[bufferIndex2];
            int[] counts3 = scalingFactorCounts[bufferIndex3];
            // only one internal node below so just copy those scaling factor counts
            System.arraycopy(counts2, 0, counts3, 0, counts2.length);
        }

        for (int l = 0; l < categoryCount; l++) {

            for (int k = 0; k < patternCount; k++) {

                int state1 = states1[k];

                int w = l * matrixSize;

                if (state1 < stateCount) {

                    for (int i = 0; i < stateCount; i++) {

                        tmp = matrices1[w + state1];

                        sum = 0.0;
                        for (int j = 0; j < stateCount; j++) {
                            sum += matrices2[w] * partials2[v + j];
                            w++;
                        }

                        partials3[u] = tmp * sum;

                        if (SCALING) {
                            // this is to find the largest absolute exponent
                            exponent |= Math.abs(Math.getExponent(partials3[u]));
                        }

                        u++;
                    }

                    v += stateCount;
                } else {
                    // Child 1 has a gap or unknown state so don't use it

                    for (int i = 0; i < stateCount; i++) {

                        sum = 0.0;
                        for (int j = 0; j < stateCount; j++) {
                            sum += matrices2[w] * partials2[v + j];
                            w++;
                        }

                        partials3[u] = sum;

                        if (SCALING) {
                            exponent |= Math.abs(Math.getExponent(partials3[u]));
                        }

                        u++;
                    }

                    v += stateCount;
                }
            }
        }

        return exponent;
    }

    protected int updatePartialsPartials(int bufferIndex1, int matrixIndex1, int bufferIndex2, int matrixIndex2, int bufferIndex3)
    {
        double[] matrices1 = matrices[matrixIndex1];
        double[] matrices2 = matrices[matrixIndex2];

        double[] partials1 = partials[bufferIndex1];

        double[] partials2 = partials[bufferIndex2];

        double[] partials3 = partials[bufferIndex3];

        double sum1, sum2;

        int exponent = 0;
        if (SCALING) {
            int[] counts1 = scalingFactorCounts[bufferIndex1];
            int[] counts2 = scalingFactorCounts[bufferIndex2];
            int[] counts3 = scalingFactorCounts[bufferIndex3];
            for (int i = 0; i < counts1.length; i++) {
                // The scale factor counts is the sum of the two nodes below
                counts3[i] = counts1[i] + counts2[i];
            }
        }

        int u = 0;
        int v = 0;
        for (int l = 0; l < categoryCount; l++) {

            for (int k = 0; k < patternCount; k++) {

                int w = l * matrixSize;


                for (int i = 0; i < stateCount; i++) {

                    sum1 = sum2 = 0.0;

                    for (int j = 0; j < stateCount; j++) {
                        sum1 += matrices1[w] * partials1[v + j];
                        sum2 += matrices2[w] * partials2[v + j];

                        w++;
                    }

                    partials3[u] = sum1 * sum2;

                    if (SCALING) {
                        // this is to find the largest absolute exponent
                        exponent |= Math.abs(Math.getExponent(partials3[u]));
                    }

                    u++;
                }
                v += stateCount;

            }

            if (DEBUG) {
//    	    	System.err.println("1:PP node = "+nodeIndex3);
//    	    	for(int p=0; p<partials3.length; p++) {
//    	    		System.err.println("1:PP\t"+partials3[p]);
//    	    	}
//                System.err.println("node = "+nodeIndex3);
//                System.err.println(new dr.math.matrixAlgebra.Vector(partials3));
//                System.err.println(new dr.math.matrixAlgebra.Vector(scalingFactors[currentPartialsIndices[nodeIndex3]][nodeIndex3]));
                //System.exit(-1);
            }
        }

        return exponent;
    }

    public void calculateRootLogLikelihoods(final int[] bufferIndices, final int[] categoryWeightsIndices, final int[] stateFrequenciesIndices, final int[] cumulativeScaleIndices, final int count, final double[] outSumLogLikelihood) {

        assert(count == 1); // @todo implement integration across multiple subtrees

        double[] rootPartials = partials[bufferIndices[0]];

        int u = 0;
        int v = 0;
        for (int k = 0; k < patternCount; k++) {

            for (int i = 0; i < stateCount; i++) {

                tmpPartials[u] = rootPartials[v] * categoryWeights[categoryWeightsIndices[0]][0];
                u++;
                v++;
            }
        }


        for (int l = 1; l < categoryCount; l++) {
            u = 0;

            for (int k = 0; k < patternCount; k++) {

                for (int i = 0; i < stateCount; i++) {

                    tmpPartials[u] += rootPartials[v] *  categoryWeights[categoryWeightsIndices[0]][l];
                    u++;
                    v++;
                }
            }
        }

        u = 0;
        outSumLogLikelihood[0] = 0.0;
        for (int k = 0; k < patternCount; k++) {

            double sum = 0.0;
            for (int i = 0; i < stateCount; i++) {

                sum += stateFrequencies[stateFrequenciesIndices[0]][i] * tmpPartials[u];
                u++;
            }

            outSumLogLikelihood[0] += Math.log(sum) * patternWeights[k];
        }

        if (SCALING) {
            int[] rootCounts = scalingFactorCounts[bufferIndices[0]];
            for (int i = 0; i < SCALING_FACTOR_COUNT; i++) {
                // we multiplied the scaling factors in so now subtract the logs
                outSumLogLikelihood[0] -= (logScalingFactors[i] * rootCounts[i]);
            }
        }

    }


    public void calculateEdgeLogLikelihoods(final int[] parentBufferIndices, final int[] childBufferIndices, final int[] probabilityIndices, final int[] firstDerivativeIndices, final int[] secondDerivativeIndices, final int[] categoryWeightsIndices, final int[] stateFrequenciesIndices, final int[] cumulativeScaleIndices, final int count, final double[] outSumLogLikelihood, final double[] outSumFirstDerivative, final double[] outSumSecondDerivative) {
        throw new UnsupportedOperationException("calculateEdgeLogLikelihoods not implemented in GeneralBeagleImpl");
    }

    public void getSiteLogLikelihoods(final double[] outLogLikelihoods) {
        throw new UnsupportedOperationException("getSiteLogLikelihoods not implemented in GeneralBeagleImpl");
    }


    public InstanceDetails getDetails() {
        InstanceDetails details = new InstanceDetails();
        details.setResourceNumber(0);
        details.setFlags(BeagleFlag.PRECISION_DOUBLE.getMask());
        return details;
    }

}