package beast.evolution.likelihood;

/**
 * nucleotide implementation of standard likelihood core *
 */
public class BeerLikelihoodCore4 extends BeerLikelihoodCore {

    public BeerLikelihoodCore4() {
        super(4);
    }

    /**
     * Calculates partial likelihoods at a node when both children have states.
     */
    @Override
	protected void calculateStatesStatesPruning(int[] iStates1, double[] matrices1,
                                                int[] iStates2, double[] matrices2,
                                                double[] partials3) {
        int v = 0;

        for (int l = 0; l < nrOfMatrices; l++) {

            for (int k = 0; k < nrOfPatterns; k++) {

                int state1 = iStates1[k];
                int state2 = iStates2[k];

                int w = l * matrixSize;

                if (state1 < 4 && state2 < 4) {

                    partials3[v] = matrices1[w + state1] * matrices2[w + state2];
                    v++;
                    w += 4;
                    partials3[v] = matrices1[w + state1] * matrices2[w + state2];
                    v++;
                    w += 4;
                    partials3[v] = matrices1[w + state1] * matrices2[w + state2];
                    v++;
                    w += 4;
                    partials3[v] = matrices1[w + state1] * matrices2[w + state2];
                    v++;
                    w += 4;

                } else if (state1 < 4) {
                    // child 2 has a gap or unknown state so don't use it

                    partials3[v] = matrices1[w + state1];
                    v++;
                    w += 4;
                    partials3[v] = matrices1[w + state1];
                    v++;
                    w += 4;
                    partials3[v] = matrices1[w + state1];
                    v++;
                    w += 4;
                    partials3[v] = matrices1[w + state1];
                    v++;
                    w += 4;

                } else if (state2 < 4) {
                    // child 2 has a gap or unknown state so don't use it
                    partials3[v] = matrices2[w + state2];
                    v++;
                    w += 4;
                    partials3[v] = matrices2[w + state2];
                    v++;
                    w += 4;
                    partials3[v] = matrices2[w + state2];
                    v++;
                    w += 4;
                    partials3[v] = matrices2[w + state2];
                    v++;
                    w += 4;

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
        }
    }

    /**
     * Calculates partial likelihoods at a node when one child has states and one has partials.
     */
    @Override
	protected void calculateStatesPartialsPruning(int[] iStates1, double[] matrices1,
                                                  double[] partials2, double[] matrices2,
                                                  double[] partials3) {

        double sum;//, tmp;

        int u = 0;
        int v = 0;

        for (int l = 0; l < nrOfMatrices; l++) {
            for (int k = 0; k < nrOfPatterns; k++) {

                int state1 = iStates1[k];

                int w = l * matrixSize;

                if (state1 < 4) {


                    sum = matrices2[w] * partials2[v];
                    sum += matrices2[w + 1] * partials2[v + 1];
                    sum += matrices2[w + 2] * partials2[v + 2];
                    sum += matrices2[w + 3] * partials2[v + 3];
                    partials3[u] = matrices1[w + state1] * sum;
                    u++;

                    sum = matrices2[w + 4] * partials2[v];
                    sum += matrices2[w + 5] * partials2[v + 1];
                    sum += matrices2[w + 6] * partials2[v + 2];
                    sum += matrices2[w + 7] * partials2[v + 3];
                    partials3[u] = matrices1[w + 4 + state1] * sum;
                    u++;

                    sum = matrices2[w + 8] * partials2[v];
                    sum += matrices2[w + 9] * partials2[v + 1];
                    sum += matrices2[w + 10] * partials2[v + 2];
                    sum += matrices2[w + 11] * partials2[v + 3];
                    partials3[u] = matrices1[w + 8 + state1] * sum;
                    u++;

                    sum = matrices2[w + 12] * partials2[v];
                    sum += matrices2[w + 13] * partials2[v + 1];
                    sum += matrices2[w + 14] * partials2[v + 2];
                    sum += matrices2[w + 15] * partials2[v + 3];
                    partials3[u] = matrices1[w + 12 + state1] * sum;
                    u++;

                    v += 4;

                } else {
                    // Child 1 has a gap or unknown state so don't use it


                    sum = matrices2[w] * partials2[v];
                    sum += matrices2[w + 1] * partials2[v + 1];
                    sum += matrices2[w + 2] * partials2[v + 2];
                    sum += matrices2[w + 3] * partials2[v + 3];
                    partials3[u] = sum;
                    u++;

                    sum = matrices2[w + 4] * partials2[v];
                    sum += matrices2[w + 5] * partials2[v + 1];
                    sum += matrices2[w + 6] * partials2[v + 2];
                    sum += matrices2[w + 7] * partials2[v + 3];
                    partials3[u] = sum;
                    u++;

                    sum = matrices2[w + 8] * partials2[v];
                    sum += matrices2[w + 9] * partials2[v + 1];
                    sum += matrices2[w + 10] * partials2[v + 2];
                    sum += matrices2[w + 11] * partials2[v + 3];
                    partials3[u] = sum;
                    u++;

                    sum = matrices2[w + 12] * partials2[v];
                    sum += matrices2[w + 13] * partials2[v + 1];
                    sum += matrices2[w + 14] * partials2[v + 2];
                    sum += matrices2[w + 15] * partials2[v + 3];
                    partials3[u] = sum;
                    u++;

                    v += 4;
                }
            }
        }
    }

    /**
     * Calculates partial likelihoods at a node when both children have partials.
     */
    @Override
	protected void calculatePartialsPartialsPruning(double[] partials1, double[] matrices1,
                                                    double[] partials2, double[] matrices2,
                                                    double[] partials3) {
        double sum1, sum2;

        int u = 0;
        int v = 0;

        for (int l = 0; l < nrOfMatrices; l++) {

            for (int k = 0; k < nrOfPatterns; k++) {

                int w = l * matrixSize;

                sum1 = matrices1[w] * partials1[v];
                sum2 = matrices2[w] * partials2[v];
                sum1 += matrices1[w + 1] * partials1[v + 1];
                sum2 += matrices2[w + 1] * partials2[v + 1];
                sum1 += matrices1[w + 2] * partials1[v + 2];
                sum2 += matrices2[w + 2] * partials2[v + 2];
                sum1 += matrices1[w + 3] * partials1[v + 3];
                sum2 += matrices2[w + 3] * partials2[v + 3];
                partials3[u] = sum1 * sum2;
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
                u++;

                v += 4;
            }
        }
    }


//
//	@Override
//    public void calcRootPseudoRootPartials(double[] frequencies, int iNode, double [] pseudoPartials) {
//		int u = 0;
//		double [] inPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
//		for (int k = 0; k < m_nPatterns * m_nMatrices; k++) {
//			pseudoPartials[u] = inPartials[u] * frequencies[0];
//			pseudoPartials[u+1] = inPartials[u+1] * frequencies[1];
//			pseudoPartials[u+2] = inPartials[u+2] * frequencies[2];
//			pseudoPartials[u+3] = inPartials[u+3] * frequencies[3];
//			u+=4;
//		}
//    }
//    
//	@Override
//    public void calcPseudoRootPartials(double [] parentPseudoPartials, int iNode, double [] pseudoPartials) {
//		int v = 0;
//		int u = 0;
//		double [] matrices = m_fMatrices[m_iCurrentMatrices[iNode]][iNode];
//		for (int k = 0; k < m_nPatterns; k++) {
//			for (int l = 0; l < m_nMatrices; l++) {
//				int w = l * m_nMatrixSize;
//				pseudoPartials[v] = parentPseudoPartials[u] * matrices[w] +
//					parentPseudoPartials[u+1] * matrices[w+1] +
//					parentPseudoPartials[u+2] * matrices[w+2] +
//					parentPseudoPartials[u+3] * matrices[w+3];
//				w += 4;
//				pseudoPartials[v+1] = parentPseudoPartials[u] * matrices[w] +
//						parentPseudoPartials[u+1] * matrices[w+1] +
//						parentPseudoPartials[u+2] * matrices[w+2] +
//						parentPseudoPartials[u+3] * matrices[w+3];
//				w += 4;
//				pseudoPartials[v+1] = parentPseudoPartials[u] * matrices[w] +
//					parentPseudoPartials[u+1] * matrices[w+1] +
//					parentPseudoPartials[u+2] * matrices[w+2] +
//					parentPseudoPartials[u+3] * matrices[w+3];
//				w += 4;
//				pseudoPartials[v+1] = parentPseudoPartials[u] * matrices[w] +
//					parentPseudoPartials[u+1] * matrices[w+1] +
//					parentPseudoPartials[u+2] * matrices[w+2] +
//					parentPseudoPartials[u+3] * matrices[w+3];
//				v += 4;
//				u += 4;
//			}
//		}
//    }
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
//            double sum = partials[v] + partials[v+1] + partials[v + 2] + partials[v + 3];
//            v +=4;
//            outLogLikelihoods[k] = Math.log(sum) + getLogScalingFactor(k);
//		}
//	}

}
