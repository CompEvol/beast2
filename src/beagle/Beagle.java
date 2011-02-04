/*
 * Beagle.java
 *
 */

package beagle;

/**
 * Beagle - An interface exposing the BEAGLE likelihood evaluation library.
 *
 * This interface mirrors the beagle.h API but it for a single instance only.
 * It is intended to be used by JNI wrappers of the BEAGLE library and for
 * Java implementations for testing purposes. BeagleFactory handles the creation
 * of specific istances.
 *
 * @author Andrew Rambaut
 * @author Marc A. Suchard
 * @version $Id:$
 */

public interface Beagle {

    public static int OPERATION_TUPLE_SIZE = 7;
    public static int NONE = -1;


    /**
     * Finalize this instance
     *
     * This function finalizes the instance by releasing allocated memory
     */
    void finalize() throws Throwable;


    /**
     * Set the weights for each pattern
     * @param patternWeights    Array containing patternCount weights
     */
    void setPatternWeights(final double[] patternWeights);

    /**
     * Set the compressed state representation for tip node
     *
     * This function copies a compact state representation into an instance buffer.
     * Compact state representation is an array of states: 0 to stateCount - 1 (missing = stateCount).
     * The inStates array should be patternCount in length (replication across categoryCount is not
     * required).
     *
     * @param tipIndex   Index of destination partialsBuffer (input)
     * @param inStates   Pointer to compressed states (input)
     */
    void setTipStates(
            int tipIndex,
            final int[] inStates);

    /**
     * Get the compressed state representation for tip node
     *
     * This function copies a compact state representation from an instance buffer.
     * Compact state representation is an array of states: 0 to stateCount - 1 (missing = stateCount).
     * The inStates array should be patternCount in length (replication across categoryCount is not
     * required).
     *
     * @param tipIndex   Index of destination partialsBuffer (input)
     * @param outStates   Pointer to compressed states (input)
     */
    void getTipStates(
            int tipIndex,
            final int[] outStates);

    /**
     * Set an instance partials buffer
     *
     * This function copies an array of partials into an instance buffer. The inPartials array should
     * be stateCount * patternCount in length. For most applications this will be used
     * to set the partial likelihoods for the observed states. Internally, the partials will be copied
     * categoryCount times.
     *
     * @param tipIndex   Index of destination partialsBuffer (input)
     * @param  inPartials   Pointer to partials values to set (input)
     */
    void setTipPartials(
            int tipIndex,
            final double[] inPartials);

    /**
     * Set an instance partials buffer
     *
     * This function copies an array of partials into an instance buffer. The inPartials array should
     * be stateCount * patternCount * categoryCount in length.
     *
     * @param bufferIndex   Index of destination partialsBuffer (input)
     * @param  inPartials   Pointer to partials values to set (input)
     */
    void setPartials(
            int bufferIndex,
            final double[] inPartials);

    /**
     * Get partials from an instance buffer
     *
     * This function copies an array of partials from an instance buffer. The inPartials array should
     * be stateCount * patternCount * categoryCount in length.
     *
     * @param bufferIndex   Index of destination partialsBuffer (input)
     * @param scaleIndex    Index of scaleBuffer to apply to partials (input)
     * @param  outPartials  Pointer to which to receive partialsBuffer (output)
     */
    void getPartials(
            int bufferIndex,
            int scaleIndex,
            final double []outPartials);

    /**
     * Set an eigen-decomposition buffer
     *
     * This function copies an eigen-decomposition into a instance buffer.
     *
     * @param eigenIndex                Index of eigen-decomposition buffer (input)
     * @param inEigenVectors            Flattened matrix (stateCount x stateCount) of eigen-vectors (input)
     * @param inInverseEigenVectors     Flattened matrix (stateCount x stateCount) of inverse-eigen-vectors (input)
     * @param inEigenValues             Vector of eigenvalues
     */
    void setEigenDecomposition(
            int eigenIndex,
            final double[] inEigenVectors,
            final double[] inInverseEigenVectors,
            final double[] inEigenValues);

    /**
     * Set a set of state frequences. These will probably correspond to an
     * eigen-system.
     *
     * @param stateFrequenciesIndex the index of the frequency buffer
     * @param stateFrequencies the array of frequences (stateCount)
     */
    void setStateFrequencies(int stateFrequenciesIndex,
                             final double[] stateFrequencies);

    /**
     * Set a set of category weights. These will probably correspond to an
     * eigen-system.
     *
     * @param categoryWeightsIndex the index of the buffer
     * @param categoryWeights the array of weights
     */
    void setCategoryWeights(int categoryWeightsIndex,
                            final double[] categoryWeights);

    /**
     * Set category rates
     *
     * This function sets the vector of category rates for an instance.
     *
     * @param inCategoryRates       Array containing categoryCount rate scalers (input)
     */
    void setCategoryRates(final double[] inCategoryRates);

    /**
     * Calculate a list of transition probability matrices
     *
     * This function calculates a list of transition probabilities matrices and their first and
     * second derivatives (if requested).
     *
     * @param eigenIndex                Index of eigen-decomposition buffer (input)
     * @param probabilityIndices        List of indices of transition probability matrices to update (input)
     * @param firstDerivativeIndices    List of indices of first derivative matrices to update (input, NULL implies no calculation)
     * @param secondDervativeIndices    List of indices of second derivative matrices to update (input, NULL implies no calculation)
     * @param edgeLengths               List of edge lengths with which to perform calculations (input)
     * @param count                     Length of lists
     */
    void updateTransitionMatrices(
            int eigenIndex,
            final int[] probabilityIndices,
            final int[] firstDerivativeIndices,
            final int[] secondDervativeIndices,
            final double[] edgeLengths,
            int count);

    /**
     * This function copies a finite-time transition probability matrix into a matrix buffer. This function
     * is used when the application wishes to explicitly set the transition probability matrix rather than
     * using the setEigenDecomposition and updateTransitionMatrices functions. The inMatrix array should be
     * of size stateCount * stateCount * categoryCount and will contain one matrix for each rate category.
     *
     * This function copies a finite-time transition probability matrix into a matrix buffer.
     * @param matrixIndex   Index of matrix buffer (input)
     * @param inMatrix          Pointer to source transition probability matrix (input)
     * @param paddedValue   Value to be used for padding for ambiguous states (e.g. 1 for probability matrices, 0 for derivative matrices) (input)
     */
    void setTransitionMatrix(
            int matrixIndex,			/**< Index of matrix buffer (input) */
            final double[] inMatrix, 	/**< Pointer to source transition probability matrix (input) */
            double paddedValue);
    /**
     * Get a finite-time transition probability matrix
     *
     * This function copies a finite-time transition matrix buffer into the array outMatrix. The
     * outMatrix array should be of size stateCount * stateCount * categoryCount and will be filled
     * with one matrix for each rate category.
     *
     * @param matrixIndex  Index of matrix buffer (input)
     * @param outMatrix    Pointer to destination transition probability matrix (output)
     *
     */
    void getTransitionMatrix(int matrixIndex,
                             double[] outMatrix);

    /**
     * Calculate or queue for calculation partials using a list of operations
     *
     * This function either calculates or queues for calculation a list partials. Implementations
     * supporting SYNCH may queue these calculations while other implementations perform these
     * operations immediately.  Implementations supporting GPU may perform all operations in the list
     * simultaneously.
     *
     * Operations list is a list of 7-tuple integer indices, with one 7-tuple per operation.
     * Format of 7-tuple operation: {destinationPartials,
     *                               destinationScaleWrite,
     *                               destinationScaleRead,
     *                               child1Partials,
     *                               child1TransitionMatrix,
     *                               child2Partials,
     *                               child2TransitionMatrix}
     *
     * @param operations            List of 7-tuples specifying operations (input)
     * @param operationCount        Number of operations (input)
     * @param cumulativeScaleIndex  Index number of scaleBuffer to store accumulated factors (input)
     *
     */
    void updatePartials(
            final int[] operations,
            int operationCount,
            int cumulativeScaleIndex);

    /**
     * Accumulate scale factors
     *
     * This function adds (log) scale factors from a list of scaleBuffers to a cumulative scale
     * buffer. It is used to calculate the marginal scaling at a specific node for each site.
     *
     * @param scaleIndices            	List of scaleBuffers to add (input)
     * @param count                     Number of scaleBuffers in list (input)
     * @param cumulativeScaleIndex      Index number of scaleBuffer to accumulate factors into (input)
     */
    void accumulateScaleFactors(
            final int[] scaleIndices,
            final int count,
            final int cumulativeScaleIndex
    );

    /**
     * Remove scale factors
     *
     * This function removes (log) scale factors from a cumulative scale buffer. The
     * scale factors to be removed are indicated in a list of scaleBuffers.
     *
     * @param scaleIndices            	List of scaleBuffers to remove (input)
     * @param count                     Number of scaleBuffers in list (input)
     * @param cumulativeScaleIndex    	Index number of scaleBuffer containing accumulated factors (input)
     */
    void removeScaleFactors(
            final int[] scaleIndices,
            final int count,
            final int cumulativeScaleIndex
    );


    /**
     * Copy scale factors
     *
     * This function copies scale factors from one buffer to another.
     *
     * @param instance                  Instance number (input)
     * @param destScalingIndex          Destination scaleBuffer (input)
     * @param srcScalingIndex           Source scaleBuffer (input)
     */
    void copyScaleFactors(
        int destScalingIndex,
        int srcScalingIndex
    );    

    /**
     * Reset scalefactors
     *
     * This function resets a cumulative scale buffer.
     *
     * @param cumulativeScaleIndex    	Index number of cumulative scaleBuffer (input)
     */
    void resetScaleFactors(int cumulativeScaleIndex);

    /**
     * Calculate site log likelihoods at a root node
     *
     * This function integrates a list of partials at a node with respect to a set of partials-weights and
     * state frequencies to return the log likelihoods for each site
     *
     * @param bufferIndices             List of partialsBuffer indices to integrate (input)
     * @param categoryWeightsIndices    List of indices of category weights to apply to each partialsBuffer (input)
     *                                      should be one categoryCount sized set for each of
     *                                      parentBufferIndices
     * @param stateFrequenciesIndices   List of indices of state frequencies for each partialsBuffer (input)
     *                                      should be one set for each of parentBufferIndices
     * @param cumulativeScaleIndices    List of scalingFactors indices to accumulate over (input). There
     *                                      should be one set for each of parentBufferIndices
     * @param count                     Number of partialsBuffer to integrate (input)
     * @param outSumLogLikelihood       Pointer to destination for resulting sum of log likelihoods (output)
     */

    void calculateRootLogLikelihoods(int[] bufferIndices,
                                     int[] categoryWeightsIndices,
                                     int[] stateFrequenciesIndices,
                                     int[] cumulativeScaleIndices,
                                     int count,
                                     double[] outSumLogLikelihood);

    /**
     * Calculate site log likelihoods and derivatives along an edge
     *
     * This function integrates at list of partials at a parent and child node with respect
     * to a set of partials-weights and state frequencies to return the log likelihoods
     * and first and second derivatives for each site
     *
     * @param parentBufferIndices       List of indices of parent partialsBuffers (input)
     * @param childBufferIndices        List of indices of child partialsBuffers (input)
     * @param probabilityIndices        List indices of transition probability matrices for this edge (input)
     * @param firstDerivativeIndices    List indices of first derivative matrices (input)
     * @param secondDerivativeIndices   List indices of second derivative matrices (input)
     * @param categoryWeightsIndices    List of indices of category weights to apply to each partialsBuffer (input)
     * @param stateFrequenciesIndices   List of indices of state frequencies for each partialsBuffer (input)
     *                                      There should be one set for each of parentBufferIndices
     * @param cumulativeScaleIndices    List of scalingFactors indices to accumulate over (input). There
     *                                      There should be one set for each of parentBufferIndices
     * @param count                     Number of partialsBuffers (input)
     * @param outSumLogLikelihood       Pointer to destination for resulting sum of log likelihoods (output)
     * @param outSumFirstDerivative     Pointer to destination for resulting sum of first derivatives (output)
     * @param outSumSecondDerivative    Pointer to destination for resulting sum of second derivatives (output)
     */

    void calculateEdgeLogLikelihoods(int[] parentBufferIndices,
                                     int[] childBufferIndices,
                                     int[] probabilityIndices,
                                     int[] firstDerivativeIndices,
                                     int[] secondDerivativeIndices,
                                     int[] categoryWeightsIndices,
                                     int[] stateFrequenciesIndices,
                                     int[] cumulativeScaleIndices,
                                     int count,
                                     double[] outSumLogLikelihood,
                                     double[] outSumFirstDerivative,
                                     double[] outSumSecondDerivative);

    /**
     * Return the individual log likelihoods for each site pattern.
     *
     * @param outLogLikelihoods an array in which the likelihoods will be put
     */
    void getSiteLogLikelihoods(double[] outLogLikelihoods);

    /**
     * Get a details class for this instance
     * @return
     */
    public InstanceDetails getDetails();
}