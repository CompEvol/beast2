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
    protected void calculateStatesStatesPruning(int[] iStates1, double[] fMatrices1,
                                                int[] iStates2, double[] fMatrices2,
                                                double[] fPartials3) {
        int v = 0;

        for (int l = 0; l < m_nMatrices; l++) {

            for (int k = 0; k < m_nPatterns; k++) {

                int state1 = iStates1[k];
                int state2 = iStates2[k];

                int w = l * m_nMatrixSize;

                if (state1 < 4 && state2 < 4) {

                    fPartials3[v] = fMatrices1[w + state1] * fMatrices2[w + state2];
                    v++;
                    w += 4;
                    fPartials3[v] = fMatrices1[w + state1] * fMatrices2[w + state2];
                    v++;
                    w += 4;
                    fPartials3[v] = fMatrices1[w + state1] * fMatrices2[w + state2];
                    v++;
                    w += 4;
                    fPartials3[v] = fMatrices1[w + state1] * fMatrices2[w + state2];
                    v++;
                    w += 4;

                } else if (state1 < 4) {
                    // child 2 has a gap or unknown state so don't use it

                    fPartials3[v] = fMatrices1[w + state1];
                    v++;
                    w += 4;
                    fPartials3[v] = fMatrices1[w + state1];
                    v++;
                    w += 4;
                    fPartials3[v] = fMatrices1[w + state1];
                    v++;
                    w += 4;
                    fPartials3[v] = fMatrices1[w + state1];
                    v++;
                    w += 4;

                } else if (state2 < 4) {
                    // child 2 has a gap or unknown state so don't use it
                    fPartials3[v] = fMatrices2[w + state2];
                    v++;
                    w += 4;
                    fPartials3[v] = fMatrices2[w + state2];
                    v++;
                    w += 4;
                    fPartials3[v] = fMatrices2[w + state2];
                    v++;
                    w += 4;
                    fPartials3[v] = fMatrices2[w + state2];
                    v++;
                    w += 4;

                } else {
                    // both children have a gap or unknown state so set partials to 1
                    fPartials3[v] = 1.0;
                    v++;
                    fPartials3[v] = 1.0;
                    v++;
                    fPartials3[v] = 1.0;
                    v++;
                    fPartials3[v] = 1.0;
                    v++;
                }
            }
        }
    }

    /**
     * Calculates partial likelihoods at a node when one child has states and one has partials.
     */
    protected void calculateStatesPartialsPruning(int[] iStates1, double[] fMatrices1,
                                                  double[] fPartials2, double[] fMatrices2,
                                                  double[] fPartials3) {

        double sum;//, tmp;

        int u = 0;
        int v = 0;

        for (int l = 0; l < m_nMatrices; l++) {
            for (int k = 0; k < m_nPatterns; k++) {

                int state1 = iStates1[k];

                int w = l * m_nMatrixSize;

                if (state1 < 4) {


                    sum = fMatrices2[w] * fPartials2[v];
                    sum += fMatrices2[w + 1] * fPartials2[v + 1];
                    sum += fMatrices2[w + 2] * fPartials2[v + 2];
                    sum += fMatrices2[w + 3] * fPartials2[v + 3];
                    fPartials3[u] = fMatrices1[w + state1] * sum;
                    u++;

                    sum = fMatrices2[w + 4] * fPartials2[v];
                    sum += fMatrices2[w + 5] * fPartials2[v + 1];
                    sum += fMatrices2[w + 6] * fPartials2[v + 2];
                    sum += fMatrices2[w + 7] * fPartials2[v + 3];
                    fPartials3[u] = fMatrices1[w + 4 + state1] * sum;
                    u++;

                    sum = fMatrices2[w + 8] * fPartials2[v];
                    sum += fMatrices2[w + 9] * fPartials2[v + 1];
                    sum += fMatrices2[w + 10] * fPartials2[v + 2];
                    sum += fMatrices2[w + 11] * fPartials2[v + 3];
                    fPartials3[u] = fMatrices1[w + 8 + state1] * sum;
                    u++;

                    sum = fMatrices2[w + 12] * fPartials2[v];
                    sum += fMatrices2[w + 13] * fPartials2[v + 1];
                    sum += fMatrices2[w + 14] * fPartials2[v + 2];
                    sum += fMatrices2[w + 15] * fPartials2[v + 3];
                    fPartials3[u] = fMatrices1[w + 12 + state1] * sum;
                    u++;

                    v += 4;

                } else {
                    // Child 1 has a gap or unknown state so don't use it


                    sum = fMatrices2[w] * fPartials2[v];
                    sum += fMatrices2[w + 1] * fPartials2[v + 1];
                    sum += fMatrices2[w + 2] * fPartials2[v + 2];
                    sum += fMatrices2[w + 3] * fPartials2[v + 3];
                    fPartials3[u] = sum;
                    u++;

                    sum = fMatrices2[w + 4] * fPartials2[v];
                    sum += fMatrices2[w + 5] * fPartials2[v + 1];
                    sum += fMatrices2[w + 6] * fPartials2[v + 2];
                    sum += fMatrices2[w + 7] * fPartials2[v + 3];
                    fPartials3[u] = sum;
                    u++;

                    sum = fMatrices2[w + 8] * fPartials2[v];
                    sum += fMatrices2[w + 9] * fPartials2[v + 1];
                    sum += fMatrices2[w + 10] * fPartials2[v + 2];
                    sum += fMatrices2[w + 11] * fPartials2[v + 3];
                    fPartials3[u] = sum;
                    u++;

                    sum = fMatrices2[w + 12] * fPartials2[v];
                    sum += fMatrices2[w + 13] * fPartials2[v + 1];
                    sum += fMatrices2[w + 14] * fPartials2[v + 2];
                    sum += fMatrices2[w + 15] * fPartials2[v + 3];
                    fPartials3[u] = sum;
                    u++;

                    v += 4;
                }
            }
        }
    }

    /**
     * Calculates partial likelihoods at a node when both children have partials.
     */
    protected void calculatePartialsPartialsPruning(double[] fPartials1, double[] fMatrices1,
                                                    double[] fPartials2, double[] fMatrices2,
                                                    double[] fPartials3) {
        double sum1, sum2;

        int u = 0;
        int v = 0;

        for (int l = 0; l < m_nMatrices; l++) {

            for (int k = 0; k < m_nPatterns; k++) {

                int w = l * m_nMatrixSize;

                sum1 = fMatrices1[w] * fPartials1[v];
                sum2 = fMatrices2[w] * fPartials2[v];
                sum1 += fMatrices1[w + 1] * fPartials1[v + 1];
                sum2 += fMatrices2[w + 1] * fPartials2[v + 1];
                sum1 += fMatrices1[w + 2] * fPartials1[v + 2];
                sum2 += fMatrices2[w + 2] * fPartials2[v + 2];
                sum1 += fMatrices1[w + 3] * fPartials1[v + 3];
                sum2 += fMatrices2[w + 3] * fPartials2[v + 3];
                fPartials3[u] = sum1 * sum2;
                u++;

                sum1 = fMatrices1[w + 4] * fPartials1[v];
                sum2 = fMatrices2[w + 4] * fPartials2[v];
                sum1 += fMatrices1[w + 5] * fPartials1[v + 1];
                sum2 += fMatrices2[w + 5] * fPartials2[v + 1];
                sum1 += fMatrices1[w + 6] * fPartials1[v + 2];
                sum2 += fMatrices2[w + 6] * fPartials2[v + 2];
                sum1 += fMatrices1[w + 7] * fPartials1[v + 3];
                sum2 += fMatrices2[w + 7] * fPartials2[v + 3];
                fPartials3[u] = sum1 * sum2;
                u++;

                sum1 = fMatrices1[w + 8] * fPartials1[v];
                sum2 = fMatrices2[w + 8] * fPartials2[v];
                sum1 += fMatrices1[w + 9] * fPartials1[v + 1];
                sum2 += fMatrices2[w + 9] * fPartials2[v + 1];
                sum1 += fMatrices1[w + 10] * fPartials1[v + 2];
                sum2 += fMatrices2[w + 10] * fPartials2[v + 2];
                sum1 += fMatrices1[w + 11] * fPartials1[v + 3];
                sum2 += fMatrices2[w + 11] * fPartials2[v + 3];
                fPartials3[u] = sum1 * sum2;
                u++;

                sum1 = fMatrices1[w + 12] * fPartials1[v];
                sum2 = fMatrices2[w + 12] * fPartials2[v];
                sum1 += fMatrices1[w + 13] * fPartials1[v + 1];
                sum2 += fMatrices2[w + 13] * fPartials2[v + 1];
                sum1 += fMatrices1[w + 14] * fPartials1[v + 2];
                sum2 += fMatrices2[w + 14] * fPartials2[v + 2];
                sum1 += fMatrices1[w + 15] * fPartials1[v + 3];
                sum2 += fMatrices2[w + 15] * fPartials2[v + 3];
                fPartials3[u] = sum1 * sum2;
                u++;

                v += 4;
            }
        }
    }


//
//	@Override
//    public void calcRootPsuedoRootPartials(double[] fFrequencies, int iNode, double [] fPseudoPartials) {
//		int u = 0;
//		double [] fInPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
//		for (int k = 0; k < m_nPatterns * m_nMatrices; k++) {
//			fPseudoPartials[u] = fInPartials[u] * fFrequencies[0];
//			fPseudoPartials[u+1] = fInPartials[u+1] * fFrequencies[1];
//			fPseudoPartials[u+2] = fInPartials[u+2] * fFrequencies[2];
//			fPseudoPartials[u+3] = fInPartials[u+3] * fFrequencies[3];
//			u+=4;
//		}
//    }
//    
//	@Override
//    public void calcPsuedoRootPartials(double [] fParentPseudoPartials, int iNode, double [] fPseudoPartials) {
//		int v = 0;
//		int u = 0;
//		double [] fMatrices = m_fMatrices[m_iCurrentMatrices[iNode]][iNode];
//		for (int k = 0; k < m_nPatterns; k++) {
//			for (int l = 0; l < m_nMatrices; l++) {
//				int w = l * m_nMatrixSize;
//				fPseudoPartials[v] = fParentPseudoPartials[u] * fMatrices[w] +
//					fParentPseudoPartials[u+1] * fMatrices[w+1] +
//					fParentPseudoPartials[u+2] * fMatrices[w+2] +
//					fParentPseudoPartials[u+3] * fMatrices[w+3];
//				w += 4;
//				fPseudoPartials[v+1] = fParentPseudoPartials[u] * fMatrices[w] +
//						fParentPseudoPartials[u+1] * fMatrices[w+1] +
//						fParentPseudoPartials[u+2] * fMatrices[w+2] +
//						fParentPseudoPartials[u+3] * fMatrices[w+3];
//				w += 4;
//				fPseudoPartials[v+1] = fParentPseudoPartials[u] * fMatrices[w] +
//					fParentPseudoPartials[u+1] * fMatrices[w+1] +
//					fParentPseudoPartials[u+2] * fMatrices[w+2] +
//					fParentPseudoPartials[u+3] * fMatrices[w+3];
//				w += 4;
//				fPseudoPartials[v+1] = fParentPseudoPartials[u] * fMatrices[w] +
//					fParentPseudoPartials[u+1] * fMatrices[w+1] +
//					fParentPseudoPartials[u+2] * fMatrices[w+2] +
//					fParentPseudoPartials[u+3] * fMatrices[w+3];
//				v += 4;
//				u += 4;
//			}
//		}
//    }
//
//	/**
//	 * Calculates pattern log likelihoods at a node.
//	 * @param fPartials the partials used to calculate the likelihoods
//	 * @param fFrequencies an array of state frequencies
//	 * @param fOutLogLikelihoods an array into which the likelihoods will go
//	 */
//    @Override
//	public void calculateLogLikelihoodsP(double[] fPartials,double[] fOutLogLikelihoods)
//	{
//        int v = 0;
//		for (int k = 0; k < m_nPatterns; k++) {
//            double sum = fPartials[v] + fPartials[v+1] + fPartials[v + 2] + fPartials[v + 3];
//            v +=4;
//            fOutLogLikelihoods[k] = Math.log(sum) + getLogScalingFactor(k);
//		}
//	}

}
