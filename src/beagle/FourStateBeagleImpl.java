package beagle;

import java.util.logging.Logger;

public class FourStateBeagleImpl extends GeneralBeagleImpl {

    public static final boolean DEBUG = false;

    public FourStateBeagleImpl(final int tipCount, final int partialsBufferCount, final int compactBufferCount, final int patternCount, final int eigenBufferCount, final int matrixBufferCount, final int categoryCount, final int scaleBufferCount) {
        super(tipCount, partialsBufferCount, compactBufferCount, 4, patternCount, eigenBufferCount, matrixBufferCount, categoryCount, scaleBufferCount);
        Logger.getLogger("beagle").info("Constructing double-precision 4-state Java BEAGLE implementation.");
    }

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
        int u = 0;
        for (int j = 0; j < categoryCount; j++) {

            for (int k = 0; k < patternCount; k++) {

                int w = u;

                int state1 = states1[k];
                int state2 = states2[k];

                if (state1 < 4 && state2 < 4) {

                    partials3[v] = matrices1[w + state1] * matrices2[w + state2];
                    v++;	w += 4;

                    partials3[v] = matrices1[w + state1] * matrices2[w + state2];
                    v++;	w += 4;

                    partials3[v] = matrices1[w + state1] * matrices2[w + state2];
                    v++;	w += 4;

                    partials3[v] = matrices1[w + state1] * matrices2[w + state2];
                    v++;	w += 4;

                } else if (state1 < 4) {
                    // child 2 has a gap or unknown state so don't use it

                    partials3[v] = matrices1[w + state1];
                    v++;	w += 4;
                    partials3[v] = matrices1[w + state1];
                    v++;	w += 4;
                    partials3[v] = matrices1[w + state1];
                    v++;	w += 4;
                    partials3[v] = matrices1[w + state1];
                    v++;	w += 4;

                } else if (state2 < 4) {
                    // child 2 has a gap or unknown state so don't use it
                    partials3[v] = matrices2[w + state2];
                    v++;	w += 4;
                    partials3[v] = matrices2[w + state2];
                    v++;	w += 4;
                    partials3[v] = matrices2[w + state2];
                    v++;	w += 4;
                    partials3[v] = matrices2[w + state2];
                    v++;	w += 4;

                } else {
                    // both children have a gap or unknown state so set partials to 1
                    partials3[v] = 1.0;
                    v++;
                    partials3[v] = 1.0;
                    v++;
                    partials3[v] = 1.0;
                    v++;
                    partials3[v] = 1.0;
                    v++;
                }
            }

            u += matrixSize;
        }
        return 0; // don't bother checking exponents for cherries
    }

    protected int updateStatesPartials(int bufferIndex1, int matrixIndex1, int bufferIndex2, int matrixIndex2, int bufferIndex3)
    {
        double[] matrices1 = matrices[matrixIndex1];
        double[] matrices2 = matrices[matrixIndex2];

        int[] states1 = tipStates[bufferIndex1];
        double[] partials2 = partials[bufferIndex2];

        double[] partials3 = partials[bufferIndex3];

        // copied from NucleotideLikelihoodCore
        int u = 0;
        int v = 0;
        int w = 0;

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

                if (state1 < 4) {

                    double sum;

                    sum =	matrices2[w] * partials2[v];
                    sum +=	matrices2[w + 1] * partials2[v + 1];
                    sum +=	matrices2[w + 2] * partials2[v + 2];
                    sum +=	matrices2[w + 3] * partials2[v + 3];
                    partials3[u] = matrices1[w + state1] * sum;
                    if (SCALING) {
                        exponent |= Math.abs(Math.getExponent(partials3[u]));
                    }
                    u++;

                    sum =	matrices2[w + 4] * partials2[v];
                    sum +=	matrices2[w + 5] * partials2[v + 1];
                    sum +=	matrices2[w + 6] * partials2[v + 2];
                    sum +=	matrices2[w + 7] * partials2[v + 3];
                    partials3[u] = matrices1[w + 4 + state1] * sum;
                    if (SCALING) {
                        exponent |= Math.abs(Math.getExponent(partials3[u]));
                    }
                    u++;

                    sum =	matrices2[w + 8] * partials2[v];
                    sum +=	matrices2[w + 9] * partials2[v + 1];
                    sum +=	matrices2[w + 10] * partials2[v + 2];
                    sum +=	matrices2[w + 11] * partials2[v + 3];
                    partials3[u] = matrices1[w + 8 + state1] * sum;
                    if (SCALING) {
                        exponent |= Math.abs(Math.getExponent(partials3[u]));
                    }
                    u++;

                    sum =	matrices2[w + 12] * partials2[v];
                    sum +=	matrices2[w + 13] * partials2[v + 1];
                    sum +=	matrices2[w + 14] * partials2[v + 2];
                    sum +=	matrices2[w + 15] * partials2[v + 3];
                    partials3[u] = matrices1[w + 12 + state1] * sum;
                    if (SCALING) {
                        exponent |= Math.abs(Math.getExponent(partials3[u]));
                    }
                    u++;

                    v += 4;

                } else {
                    // Child 1 has a gap or unknown state so don't use it

                    double sum;

                    sum =	matrices2[w] * partials2[v];
                    sum +=	matrices2[w + 1] * partials2[v + 1];
                    sum +=	matrices2[w + 2] * partials2[v + 2];
                    sum +=	matrices2[w + 3] * partials2[v + 3];
                    partials3[u] = sum;
                    if (SCALING) {
                        exponent |= Math.abs(Math.getExponent(partials3[u]));
                    }
                    u++;

                    sum =	matrices2[w + 4] * partials2[v];
                    sum +=	matrices2[w + 5] * partials2[v + 1];
                    sum +=	matrices2[w + 6] * partials2[v + 2];
                    sum +=	matrices2[w + 7] * partials2[v + 3];
                    partials3[u] = sum;
                    if (SCALING) {
                        exponent |= Math.abs(Math.getExponent(partials3[u]));
                    }
                    u++;

                    sum =	matrices2[w + 8] * partials2[v];
                    sum +=	matrices2[w + 9] * partials2[v + 1];
                    sum +=	matrices2[w + 10] * partials2[v + 2];
                    sum +=	matrices2[w + 11] * partials2[v + 3];
                    partials3[u] = sum;
                    if (SCALING) {
                        exponent |= Math.abs(Math.getExponent(partials3[u]));
                    }
                    u++;

                    sum =	matrices2[w + 12] * partials2[v];
                    sum +=	matrices2[w + 13] * partials2[v + 1];
                    sum +=	matrices2[w + 14] * partials2[v + 2];
                    sum +=	matrices2[w + 15] * partials2[v + 3];
                    partials3[u] = sum;
                    if (SCALING) {
                        exponent |= Math.abs(Math.getExponent(partials3[u]));
                    }
                    u++;

                    v += 4;
                }
            }

            w += matrixSize;
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

        if (SCALING) {
            int[] counts1 = scalingFactorCounts[bufferIndex1];
            int[] counts2 = scalingFactorCounts[bufferIndex2];
            int[] counts3 = scalingFactorCounts[bufferIndex3];
            for (int i = 0; i < counts1.length; i++) {
                // The scale factor counts is the sum of the two nodes below
                counts3[i] = counts1[i] + counts2[i];
            }
        }

        double sum1, sum2;

        int u = 0;
        int v = 0;
        int w = 0;

        int exponent = 0;

        for (int l = 0; l < categoryCount; l++) {
            for (int k = 0; k < patternCount; k++) {

                sum1 = matrices1[w] * partials1[v];
                sum2 = matrices2[w] * partials2[v];
                sum1 += matrices1[w + 1] * partials1[v + 1];
                sum2 += matrices2[w + 1] * partials2[v + 1];
                sum1 += matrices1[w + 2] * partials1[v + 2];
                sum2 += matrices2[w + 2] * partials2[v + 2];
                sum1 += matrices1[w + 3] * partials1[v + 3];
                sum2 += matrices2[w + 3] * partials2[v + 3];
                partials3[u] = sum1 * sum2;
                if (SCALING) {
                    exponent |= Math.abs(Math.getExponent(partials3[u]));
                }
                u++;

                sum1 = matrices1[w + 4] * partials1[v];
                sum2 = matrices2[w + 4] * partials2[v];
                sum1 += matrices1[w + 5] * partials1[v + 1];
                sum2 += matrices2[w + 5] * partials2[v + 1];
                sum1 += matrices1[w + 6] * partials1[v + 2];
                sum2 += matrices2[w + 6] * partials2[v + 2];
                sum1 += matrices1[w + 7] * partials1[v + 3];
                sum2 += matrices2[w + 7] * partials2[v + 3];
                partials3[u] = sum1 * sum2;
                if (SCALING) {
                    exponent |= Math.abs(Math.getExponent(partials3[u]));
                }
                u++;

                sum1 = matrices1[w + 8] * partials1[v];
                sum2 = matrices2[w + 8] * partials2[v];
                sum1 += matrices1[w + 9] * partials1[v + 1];
                sum2 += matrices2[w + 9] * partials2[v + 1];
                sum1 += matrices1[w + 10] * partials1[v + 2];
                sum2 += matrices2[w + 10] * partials2[v + 2];
                sum1 += matrices1[w + 11] * partials1[v + 3];
                sum2 += matrices2[w + 11] * partials2[v + 3];
                partials3[u] = sum1 * sum2;
                if (SCALING) {
                    exponent |= Math.abs(Math.getExponent(partials3[u]));
                }
                u++;

                sum1 = matrices1[w + 12] * partials1[v];
                sum2 = matrices2[w + 12] * partials2[v];
                sum1 += matrices1[w + 13] * partials1[v + 1];
                sum2 += matrices2[w + 13] * partials2[v + 1];
                sum1 += matrices1[w + 14] * partials1[v + 2];
                sum2 += matrices2[w + 14] * partials2[v + 2];
                sum1 += matrices1[w + 15] * partials1[v + 3];
                sum2 += matrices2[w + 15] * partials2[v + 3];
                partials3[u] = sum1 * sum2;
                if (SCALING) {
                    exponent |= Math.abs(Math.getExponent(partials3[u]));
                }
                u++;

                v += 4;
            }

            w += matrixSize;
        }

        return exponent;
    }

    @Override
    public void calculateRootLogLikelihoods(final int[] bufferIndices, final int[] categoryWeightsIndices, final int[] stateFrequenciesIndices, final int[] cumulativeScaleIndices, final int count, final double[] outSumLogLikelihood) {

        double[] rootPartials = partials[bufferIndices[0]];
        double[] weights = categoryWeights[categoryWeightsIndices[0]];
        double[] freqs = stateFrequencies[stateFrequenciesIndices[0]];

        int u = 0;
        int v = 0;
        for (int k = 0; k < patternCount; k++) {

            tmpPartials[u] = rootPartials[v] * weights[0]; u++; v++;
            tmpPartials[u] = rootPartials[v] * weights[0]; u++; v++;
            tmpPartials[u] = rootPartials[v] * weights[0]; u++; v++;
            tmpPartials[u] = rootPartials[v] * weights[0]; u++; v++;
        }

        for (int j = 1; j < categoryCount; j++) {
            u = 0;
            for (int k = 0; k < patternCount; k++) {
                tmpPartials[u] += rootPartials[v] * weights[j]; u++; v++;
                tmpPartials[u] += rootPartials[v] * weights[j]; u++; v++;
                tmpPartials[u] += rootPartials[v] * weights[j]; u++; v++;
                tmpPartials[u] += rootPartials[v] * weights[j]; u++; v++;

            }
        }

        outSumLogLikelihood[0] = 0.0;

        v = 0;
        for (int k = 0; k < patternCount; k++) {
            double sum = freqs[0] * tmpPartials[v];	v++;
            sum += freqs[1] * tmpPartials[v];	v++;
            sum += freqs[2] * tmpPartials[v];	v++;
            sum += freqs[3] * tmpPartials[v];	v++;
            outSumLogLikelihood[0] += Math.log(sum) * patternWeights[k];
        }

        if (SCALING) {
            int[] rootCounts = scalingFactorCounts[bufferIndices[0]];
            for (int i = 0; i < SCALING_FACTOR_COUNT; i++) {
                // we multiplied the scaling factors in so now subtract the logs
                outSumLogLikelihood[0] -= rootCounts[i] * logScalingFactors[i];
            }
        }

    }
}