package beast.evolution.likelihood;


/** standard likelihood core, uses no caching **/
public class BeerLikelihoodCore extends LikelihoodCore {
    protected int m_nStates;
    protected int m_nNodes;
    protected int m_nPatterns;
    protected int m_nPartialsSize;
    protected int m_nMatrixSize;
    protected int m_nMatrices;

    protected boolean m_bIntegrateCategories;

    protected double[][][] m_fPartials;

    protected int[][] m_iStates;

    protected double[][][] m_fMatrices;

    protected int[] m_iCurrentMatrices;
    protected int[] m_iStoredMatrices;
    protected int[] m_iCurrentPartials;
    protected int[] m_iStoredPartials;

    protected boolean m_bUseScaling = false;

    protected double[][][] m_fScalingFactors;

    private double m_fScalingThreshold = 1.0E-100;
    double SCALE = 2;

	public BeerLikelihoodCore(int nStateCount) {
		this.m_nStates = nStateCount;
	} // c'tor
	

	/**
	 * Calculates partial likelihoods at a node when both children have states.
	 */
	protected void calculateStatesStatesPruning(int[] iStates1, double[] fMatrices1,
												int[] iStates2, double[] fMatrices2,
												double[] fPartials3)
	{
		int v = 0;

		for (int l = 0; l < m_nMatrices; l++) {

			for (int k = 0; k < m_nPatterns; k++) {

				int state1 = iStates1[k];
				int state2 = iStates2[k];

				int w = l * m_nMatrixSize;

                if (state1 < m_nStates && state2 < m_nStates) {

					for (int i = 0; i < m_nStates; i++) {

						fPartials3[v] = fMatrices1[w + state1] * fMatrices2[w + state2];

						v++;
						w += m_nStates;
					}

				} else if (state1 < m_nStates) {
					// child 2 has a gap or unknown state so treat it as unknown

					for (int i = 0; i < m_nStates; i++) {

						fPartials3[v] = fMatrices1[w + state1];

						v++;
						w += m_nStates;
					}
				} else if (state2 < m_nStates) {
					// child 2 has a gap or unknown state so treat it as unknown

					for (int i = 0; i < m_nStates; i++) {

						fPartials3[v] = fMatrices2[w + state2];

						v++;
						w += m_nStates;
					}
				} else {
					// both children have a gap or unknown state so set partials to 1

					for (int j = 0; j < m_nStates; j++) {
						fPartials3[v] = 1.0;
						v++;
					}
				}
			}
		}
	}

	/**
	 * Calculates partial likelihoods at a node when one child has states and one has partials.
	 */
	protected void calculateStatesPartialsPruning(	int[] iStates1, double[] fMatrices1,
													double[] fPartials2, double[] fMatrices2,
													double[] fPartials3)
	{

		double sum, tmp;

		int u = 0;
		int v = 0;

		for (int l = 0; l < m_nMatrices; l++) {
			for (int k = 0; k < m_nPatterns; k++) {

				int state1 = iStates1[k];

                int w = l * m_nMatrixSize;

				if (state1 < m_nStates) {


					for (int i = 0; i < m_nStates; i++) {

						tmp = fMatrices1[w + state1];

						sum = 0.0;
						for (int j = 0; j < m_nStates; j++) {
							sum += fMatrices2[w] * fPartials2[v + j];
							w++;
						}

						fPartials3[u] = tmp * sum;
						u++;
					}

					v += m_nStates;
				} else {
					// Child 1 has a gap or unknown state so don't use it

					for (int i = 0; i < m_nStates; i++) {

						sum = 0.0;
						for (int j = 0; j < m_nStates; j++) {
							sum += fMatrices2[w] * fPartials2[v + j];
							w++;
						}

						fPartials3[u] = sum;
						u++;
					}

					v += m_nStates;
				}
			}
		}
	}

	/**
	 * Calculates partial likelihoods at a node when both children have partials.
	 */
	protected void calculatePartialsPartialsPruning(double[] fPartials1, double[] fMatrices1,
													double[] fPartials2, double[] fMatrices2,
													double[] fPartials3)
	{
		double sum1, sum2;

		int u = 0;
		int v = 0;

		for (int l = 0; l < m_nMatrices; l++) {

			for (int k = 0; k < m_nPatterns; k++) {

                int w = l * m_nMatrixSize;

				for (int i = 0; i < m_nStates; i++) {

					sum1 = sum2 = 0.0;

					for (int j = 0; j < m_nStates; j++) {
						sum1 += fMatrices1[w] * fPartials1[v + j];
						sum2 += fMatrices2[w] * fPartials2[v + j];

						w++;
					}

					fPartials3[u] = sum1 * sum2;
					u++;
				}
				v += m_nStates;
			}
		}
	}

	/**
	 * Calculates partial likelihoods at a node when both children have states.
	 */
	protected void calculateStatesStatesPruning(int[] iStates1, double[] fMatrices1,
												int[] iStates2, double[] fMatrices2,
												double[] fPartials3, int[] iMatrixMap)
	{
		int v = 0;

		for (int k = 0; k < m_nPatterns; k++) {

			int state1 = iStates1[k];
			int state2 = iStates2[k];

			int w = iMatrixMap[k] * m_nMatrixSize;

			if (state1 < m_nStates && state2 < m_nStates) {

				for (int i = 0; i < m_nStates; i++) {

					fPartials3[v] = fMatrices1[w + state1] * fMatrices2[w + state2];

					v++;
					w += m_nStates;
				}

			} else if (state1 < m_nStates) {
				// child 2 has a gap or unknown state so treat it as unknown

				for (int i = 0; i < m_nStates; i++) {

					fPartials3[v] = fMatrices1[w + state1];

					v++;
					w += m_nStates;
				}
			} else if (state2 < m_nStates) {
				// child 2 has a gap or unknown state so treat it as unknown

				for (int i = 0; i < m_nStates; i++) {

					fPartials3[v] = fMatrices2[w + state2];

					v++;
					w += m_nStates;
				}
			} else {
				// both children have a gap or unknown state so set partials to 1

				for (int j = 0; j < m_nStates; j++) {
					fPartials3[v] = 1.0;
					v++;
				}
			}
		}
	}

	/**
	 * Calculates partial likelihoods at a node when one child has states and one has partials.
	 */
	protected void calculateStatesPartialsPruning(	int[] iStates1, double[] fMatrices1,
													double[] fPartials2, double[] fMatrices2,
													double[] fPartials3, int[] iMatrixMap)
	{

		double sum, tmp;

		int u = 0;
		int v = 0;

		for (int k = 0; k < m_nPatterns; k++) {

			int state1 = iStates1[k];

			int w = iMatrixMap[k] * m_nMatrixSize;

			if (state1 < m_nStates) {

				for (int i = 0; i < m_nStates; i++) {

					tmp = fMatrices1[w + state1];

					sum = 0.0;
					for (int j = 0; j < m_nStates; j++) {
						sum += fMatrices2[w] * fPartials2[v + j];
						w++;
					}

					fPartials3[u] = tmp * sum;
					u++;
				}

				v += m_nStates;
			} else {
				// Child 1 has a gap or unknown state so don't use it

				for (int i = 0; i < m_nStates; i++) {

					sum = 0.0;
					for (int j = 0; j < m_nStates; j++) {
						sum += fMatrices2[w] * fPartials2[v + j];
						w++;
					}

					fPartials3[u] = sum;
					u++;
				}

				v += m_nStates;
			}
		}
	}

	/**
	 * Calculates partial likelihoods at a node when both children have partials.
	 */
	protected void calculatePartialsPartialsPruning(double[] fPartials1, double[] fMatrices1,
													double[] fPartials2, double[] fMatrices2,
													double[] fPartials3, int[] iMatrixMap)
	{
		double sum1, sum2;

		int u = 0;
		int v = 0;

		for (int k = 0; k < m_nPatterns; k++) {

			int w = iMatrixMap[k] * m_nMatrixSize;

			for (int i = 0; i < m_nStates; i++) {

				sum1 = sum2 = 0.0;

				for (int j = 0; j < m_nStates; j++) {
					sum1 += fMatrices1[w] * fPartials1[v + j];
					sum2 += fMatrices2[w] * fPartials2[v + j];

					w++;
				}

				fPartials3[u] = sum1 * sum2;
				u++;
			}
			v += m_nStates;
		}
	}

	/**
	 * Integrates partials across categories.
     * @param fInPartials the array of partials to be integrated
	 * @param fProportions the proportions of sites in each category
	 * @param fOutPartials an array into which the partials will go
	 */
	protected void calculateIntegratePartials(double[] fInPartials, double[] fProportions, double[] fOutPartials)
	{

		int u = 0;
		int v = 0;
		for (int k = 0; k < m_nPatterns; k++) {

			for (int i = 0; i < m_nStates; i++) {

				fOutPartials[u] = fInPartials[v] * fProportions[0];
				u++;
				v++;
			}
		}


		for (int l = 1; l < m_nMatrices; l++) {
			u = 0;

			for (int k = 0; k < m_nPatterns; k++) {

				for (int i = 0; i < m_nStates; i++) {

					fOutPartials[u] += fInPartials[v] * fProportions[l];
					u++;
					v++;
				}
			}
		}
	}

	/**
	 * Calculates pattern log likelihoods at a node.
	 * @param fPartials the partials used to calculate the likelihoods
	 * @param fFrequencies an array of state frequencies
	 * @param fOutLogLikelihoods an array into which the likelihoods will go
	 */
	public void calculateLogLikelihoods(double[] fPartials, double[] fFrequencies, double[] fOutLogLikelihoods)
	{
        int v = 0;
		for (int k = 0; k < m_nPatterns; k++) {

            double sum = 0.0;
			for (int i = 0; i < m_nStates; i++) {

				sum += fFrequencies[i] * fPartials[v];
				v++;
			}
            fOutLogLikelihoods[k] = Math.log(sum) + getLogScalingFactor(k);
		}
	}



    /**
     * initializes partial likelihood arrays.
     *
     * @param nNodeCount           the number of nodes in the tree
     * @param nPatternCount        the number of patterns
     * @param nMatrixCount         the number of matrices (i.e., number of categories)
     * @param bIntegrateCategories whether sites are being integrated over all matrices
     */
    public void initialize(int nNodeCount, int nPatternCount, int nMatrixCount, boolean bIntegrateCategories) {

        this.m_nNodes = nNodeCount;
        this.m_nPatterns = nPatternCount;
        this.m_nMatrices = nMatrixCount;

        this.m_bIntegrateCategories = bIntegrateCategories;

        if (bIntegrateCategories) {
            m_nPartialsSize = nPatternCount * m_nStates * nMatrixCount;
        } else {
            m_nPartialsSize = nPatternCount * m_nStates;
        }

        m_fPartials = new double[2][nNodeCount][];

        m_iCurrentMatrices = new int[nNodeCount];
        m_iStoredMatrices = new int[nNodeCount];

        m_iCurrentPartials = new int[nNodeCount];
        m_iStoredPartials = new int[nNodeCount];

        m_iStates = new int[nNodeCount][];

        for (int i = 0; i < nNodeCount; i++) {
            m_fPartials[0][i] = null;
            m_fPartials[1][i] = null;

            m_iStates[i] = null;
        }

        m_nMatrixSize = m_nStates * m_nStates;

        m_fMatrices = new double[2][nNodeCount][nMatrixCount * m_nMatrixSize];
    }

    /**
     * cleans up and deallocates arrays.
     */
    public void finalize() throws java.lang.Throwable  {
        m_nNodes = 0;
        m_nPatterns = 0;
        m_nMatrices = 0;

        m_fPartials = null;
        m_iCurrentPartials = null;
        m_iStoredPartials = null;
        m_iStates = null;
        m_fMatrices = null;
        m_iCurrentMatrices = null;
        m_iStoredMatrices = null;

        m_fScalingFactors = null;
    }

    @Override
    public void setUseScaling(double  fScale) {
   		m_bUseScaling = (fScale != 1.0);

        if (m_bUseScaling) {
            m_fScalingFactors = new double[2][m_nNodes][m_nPatterns];
        }
    }

    /**
     * Allocates partials for a node
     */
    public void createNodePartials(int iNodeIndex) {

        this.m_fPartials[0][iNodeIndex] = new double[m_nPartialsSize];
        this.m_fPartials[1][iNodeIndex] = new double[m_nPartialsSize];
    }

    /**
     * Sets partials for a node
     */
    public void setNodePartials(int iNodeIndex, double[] fPartials) {

        if (this.m_fPartials[0][iNodeIndex] == null) {
            createNodePartials(iNodeIndex);
        }
        if (fPartials.length < m_nPartialsSize) {
            int k = 0;
            for (int i = 0; i < m_nMatrices; i++) {
                System.arraycopy(fPartials, 0, this.m_fPartials[0][iNodeIndex], k, fPartials.length);
                k += fPartials.length;
            }
        } else {
            System.arraycopy(fPartials, 0, this.m_fPartials[0][iNodeIndex], 0, fPartials.length);
        }
    }

    /**
     * Allocates states for a node
     */
    public void createNodeStates(int iNodeIndex) {

        this.m_iStates[iNodeIndex] = new int[m_nPatterns];
    }

    /**
     * Sets states for a node
     */
    public void setNodeStates(int iNodeIndex, int[] iStates) {

        if (this.m_iStates[iNodeIndex] == null) {
            createNodeStates(iNodeIndex);
        }
        System.arraycopy(iStates, 0, this.m_iStates[iNodeIndex], 0, m_nPatterns);
    }

    /**
     * Gets states for a node
     */
    public void getNodeStates(int iNodeIndex, int[] iStates) {
        System.arraycopy(this.m_iStates[iNodeIndex], 0, iStates, 0, m_nPatterns);
    }
    
    @Override
    public void setNodeMatrixForUpdate(int iNodeIndex) {
        m_iCurrentMatrices[iNodeIndex] = 1 - m_iCurrentMatrices[iNodeIndex];

    }


    /**
     * Sets probability matrix for a node
     */
    public void setNodeMatrix(int iNodeIndex, int iMatrixIndex, double[] fMatrix) {
        System.arraycopy(fMatrix, 0, m_fMatrices[m_iCurrentMatrices[iNodeIndex]][iNodeIndex],
                iMatrixIndex * m_nMatrixSize, m_nMatrixSize);
    }

	public void setPaddedNodeMatrices(int iNode, double[] fMatrix) {
        System.arraycopy(fMatrix, 0, m_fMatrices[m_iCurrentMatrices[iNode]][iNode],
                0, m_nMatrices * m_nMatrixSize);
    }
    
    

    /**
     * Gets probability matrix for a node
     */
    public void getNodeMatrix(int iNodeIndex, int iMatrixIndex, double[] fMatrix) {
        System.arraycopy(m_fMatrices[m_iCurrentMatrices[iNodeIndex]][iNodeIndex],
                iMatrixIndex * m_nMatrixSize, fMatrix, 0, m_nMatrixSize);
    }

    @Override
    public void setNodePartialsForUpdate(int iNodeIndex) {
        m_iCurrentPartials[iNodeIndex] = 1 - m_iCurrentPartials[iNodeIndex];
    }

    /**
     * Sets the currently updating node partials for node nodeIndex. This may
     * need to repeatedly copy the partials for the different category partitions
     */
    public void setCurrentNodePartials(int iNodeIndex, double[] fPartials) {
        if (fPartials.length < m_nPartialsSize) {
            int k = 0;
            for (int i = 0; i < m_nMatrices; i++) {
                System.arraycopy(fPartials, 0, this.m_fPartials[m_iCurrentPartials[iNodeIndex]][iNodeIndex], k, fPartials.length);
                k += fPartials.length;
            }
        } else {
            System.arraycopy(fPartials, 0, this.m_fPartials[m_iCurrentPartials[iNodeIndex]][iNodeIndex], 0, fPartials.length);
        }
    }

    /**
     * Calculates partial likelihoods at a node.
     *
     * @param iNodeIndex1 the 'child 1' node
     * @param iNodeIndex2 the 'child 2' node
     * @param iNodeIndex3 the 'parent' node
     */
    public void calculatePartials(int iNodeIndex1, int iNodeIndex2, int iNodeIndex3) {
        if (m_iStates[iNodeIndex1] != null) {
            if (m_iStates[iNodeIndex2] != null) {
                calculateStatesStatesPruning(
                        m_iStates[iNodeIndex1], m_fMatrices[m_iCurrentMatrices[iNodeIndex1]][iNodeIndex1],
                        m_iStates[iNodeIndex2], m_fMatrices[m_iCurrentMatrices[iNodeIndex2]][iNodeIndex2],
                        m_fPartials[m_iCurrentPartials[iNodeIndex3]][iNodeIndex3]);
            } else {
                calculateStatesPartialsPruning(m_iStates[iNodeIndex1], m_fMatrices[m_iCurrentMatrices[iNodeIndex1]][iNodeIndex1],
                        m_fPartials[m_iCurrentPartials[iNodeIndex2]][iNodeIndex2], m_fMatrices[m_iCurrentMatrices[iNodeIndex2]][iNodeIndex2],
                        m_fPartials[m_iCurrentPartials[iNodeIndex3]][iNodeIndex3]);
            }
        } else {
            if (m_iStates[iNodeIndex2] != null) {
                calculateStatesPartialsPruning(m_iStates[iNodeIndex2], m_fMatrices[m_iCurrentMatrices[iNodeIndex2]][iNodeIndex2],
                        m_fPartials[m_iCurrentPartials[iNodeIndex1]][iNodeIndex1], m_fMatrices[m_iCurrentMatrices[iNodeIndex1]][iNodeIndex1],
                        m_fPartials[m_iCurrentPartials[iNodeIndex3]][iNodeIndex3]);
            } else {
                calculatePartialsPartialsPruning(m_fPartials[m_iCurrentPartials[iNodeIndex1]][iNodeIndex1], m_fMatrices[m_iCurrentMatrices[iNodeIndex1]][iNodeIndex1],
                        m_fPartials[m_iCurrentPartials[iNodeIndex2]][iNodeIndex2], m_fMatrices[m_iCurrentMatrices[iNodeIndex2]][iNodeIndex2],
                        m_fPartials[m_iCurrentPartials[iNodeIndex3]][iNodeIndex3]);
            }
        }

        if (m_bUseScaling) {
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
        if (m_iStates[iNodeIndex1] != null) {
            if (m_iStates[iNodeIndex2] != null) {
                calculateStatesStatesPruning(
                        m_iStates[iNodeIndex1], m_fMatrices[m_iCurrentMatrices[iNodeIndex1]][iNodeIndex1],
                        m_iStates[iNodeIndex2], m_fMatrices[m_iCurrentMatrices[iNodeIndex2]][iNodeIndex2],
                        m_fPartials[m_iCurrentPartials[iNodeIndex3]][iNodeIndex3], iMatrixMap);
            } else {
                calculateStatesPartialsPruning(
                        m_iStates[iNodeIndex1], m_fMatrices[m_iCurrentMatrices[iNodeIndex1]][iNodeIndex1],
                        m_fPartials[m_iCurrentPartials[iNodeIndex2]][iNodeIndex2], m_fMatrices[m_iCurrentMatrices[iNodeIndex2]][iNodeIndex2],
                        m_fPartials[m_iCurrentPartials[iNodeIndex3]][iNodeIndex3], iMatrixMap);
            }
        } else {
            if (m_iStates[iNodeIndex2] != null) {
                calculateStatesPartialsPruning(
                        m_iStates[iNodeIndex2], m_fMatrices[m_iCurrentMatrices[iNodeIndex2]][iNodeIndex2],
                        m_fPartials[m_iCurrentPartials[iNodeIndex1]][iNodeIndex1], m_fMatrices[m_iCurrentMatrices[iNodeIndex1]][iNodeIndex1],
                        m_fPartials[m_iCurrentPartials[iNodeIndex3]][iNodeIndex3], iMatrixMap);
            } else {
                calculatePartialsPartialsPruning(
                        m_fPartials[m_iCurrentPartials[iNodeIndex1]][iNodeIndex1], m_fMatrices[m_iCurrentMatrices[iNodeIndex1]][iNodeIndex1],
                        m_fPartials[m_iCurrentPartials[iNodeIndex2]][iNodeIndex2], m_fMatrices[m_iCurrentMatrices[iNodeIndex2]][iNodeIndex2],
                        m_fPartials[m_iCurrentPartials[iNodeIndex3]][iNodeIndex3], iMatrixMap);
            }
        }

        if (m_bUseScaling) {
            scalePartials(iNodeIndex3);
        }
    }



    public void integratePartials(int iNodeIndex, double[] fProportions, double[] fOutPartials) {
        calculateIntegratePartials(m_fPartials[m_iCurrentPartials[iNodeIndex]][iNodeIndex], fProportions, fOutPartials);
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
//    	double [] fPartials = m_fPartials[m_iCurrentPartialsIndices[iNodeIndex]][iNodeIndex];
//        for (int i = 0; i < m_nPatternCount; i++) {
//            for (int k = 0; k < m_nMatrixCount; k++) {
//                for (int j = 0; j < m_nStateCount; j++) {
//                	fPartials[v] *= SCALE;
//                	v++;
//                }
//            }
//        }
        int u = 0;

        for (int i = 0; i < m_nPatterns; i++) {

            double scaleFactor = 0.0;
            int v = u;
            for (int k = 0; k < m_nMatrices; k++) {
                for (int j = 0; j < m_nStates; j++) {
                    if (m_fPartials[m_iCurrentPartials[iNodeIndex]][iNodeIndex][v] > scaleFactor) {
                        scaleFactor = m_fPartials[m_iCurrentPartials[iNodeIndex]][iNodeIndex][v];
                    }
                    v++;
                }
                v += (m_nPatterns - 1) * m_nStates;
            }

            if (scaleFactor < m_fScalingThreshold) {

                v = u;
                for (int k = 0; k < m_nMatrices; k++) {
                    for (int j = 0; j < m_nStates; j++) {
                        m_fPartials[m_iCurrentPartials[iNodeIndex]][iNodeIndex][v] /= scaleFactor;
                        v++;
                    }
                    v += (m_nPatterns - 1) * m_nStates;
                }
                m_fScalingFactors[m_iCurrentPartials[iNodeIndex]][iNodeIndex][i] = Math.log(scaleFactor);

            } else {
                m_fScalingFactors[m_iCurrentPartials[iNodeIndex]][iNodeIndex][i] = 0.0;
            }
            u += m_nStates;


        }
    }

    /**
     * This function returns the scaling factor for that pattern by summing over
     * the log scalings used at each node. If scaling is off then this just returns
     * a 0.
     *
     * @return the log scaling factor
     */
    public double getLogScalingFactor(int iPattern) {
//    	if (m_bUseScaling) {
//    		return -(m_nNodeCount/2) * Math.log(SCALE);
//    	} else {
//    		return 0;
//    	}        
    	double logScalingFactor = 0.0;
        if (m_bUseScaling) {
            for (int i = 0; i < m_nNodes; i++) {
                logScalingFactor += m_fScalingFactors[m_iCurrentPartials[i]][i][iPattern];
            }
        }
        return logScalingFactor;
    }

    /**
     * Gets the partials for a particular node.
     *
     * @param iNodeIndex   the node
     * @param fOutPartials an array into which the partials will go
     */
    public void getPartials(int iNodeIndex, double[] fOutPartials) {
        double[] partials1 = m_fPartials[m_iCurrentPartials[iNodeIndex]][iNodeIndex];

        System.arraycopy(partials1, 0, fOutPartials, 0, m_nPartialsSize);
    }

    /**
     * Store current state
     */
	@Override
	public void restore() {
        // Rather than copying the stored stuff back, just swap the pointers...
        int[] iTmp1 = m_iCurrentMatrices;
        m_iCurrentMatrices = m_iStoredMatrices;
        m_iStoredMatrices = iTmp1;

        int[] iTmp2 = m_iCurrentPartials;
        m_iCurrentPartials = m_iStoredPartials;
        m_iStoredPartials = iTmp2;
    }

    public void unstore() {
        System.arraycopy(m_iStoredMatrices, 0, m_iCurrentMatrices, 0, m_nNodes);
        System.arraycopy(m_iStoredPartials, 0, m_iCurrentPartials, 0, m_nNodes);
    }
    
    /**
     * Restore the stored state
     */
	@Override
	public void store() {
        System.arraycopy(m_iCurrentMatrices, 0, m_iStoredMatrices, 0, m_nNodes);
        System.arraycopy(m_iCurrentPartials, 0, m_iStoredPartials, 0, m_nNodes);
    }


	@Override
    public void calcRootPsuedoRootPartials(double[] fFrequencies, int iNode, double [] fPseudoPartials) {
		int u = 0;
		double [] fInPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
		for (int k = 0; k < m_nPatterns; k++) {
			for (int l = 0; l < m_nMatrices; l++) {
				for (int i = 0; i < m_nStates; i++) {
					fPseudoPartials[u] = fInPartials[u] * fFrequencies[i];
					u++;
				}
			}
		}
    }
	@Override
    public void calcNodePsuedoRootPartials(double[] fInPseudoPartials, int iNode, double [] fOutPseudoPartials) {
		double [] fPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
		double [] fOldPartials = m_fPartials[m_iStoredPartials[iNode]][iNode];
		int nMaxK = m_nPatterns * m_nMatrices * m_nStates; 
		for (int k = 0; k < nMaxK; k++) {
			fOutPseudoPartials[k] = fInPseudoPartials[k] * fPartials[k] / fOldPartials[k];
		}
	}
    
	@Override
    public void calcPsuedoRootPartials(double [] fParentPseudoPartials, int iNode, double [] fPseudoPartials) {
		int v = 0;
		int u = 0;
		double [] fMatrices = m_fMatrices[m_iCurrentMatrices[iNode]][iNode];
		for (int k = 0; k < m_nPatterns; k++) {
			for (int l = 0; l < m_nMatrices; l++) {
				for (int i = 0; i < m_nStates; i++) {
					int w = 0;
					double fSum = 0;
					for (int j = 0; j < m_nStates; j++) {
					      fSum += fParentPseudoPartials[u+j] * fMatrices[w + i];
					      w+=m_nStates;
					}
					fPseudoPartials[v] = fSum;
					v++;
//					int w = l * m_nMatrixSize;
//					double fSum = 0;
//					for (int j = 0; j < m_nStates; j++) {
//					      fSum += fParentPseudoPartials[u+j] * fMatrices[w+j];
//					}
//					fPseudoPartials[v] = fSum;
//					v++;
				}
				u += m_nStates;
			}
		}
    }


    @Override
    void integratePartialsP(double [] fInPartials, double [] fProportions, double [] m_fRootPartials) {
		int nMaxK = m_nPatterns * m_nStates;
		for (int k = 0; k < nMaxK; k++) {
			m_fRootPartials[k] = fInPartials[k] * fProportions[0];
		}

		for (int l = 1; l < m_nMatrices; l++) {
			int n = nMaxK * l;
			for (int k = 0; k < nMaxK; k++) {
				m_fRootPartials[k] += fInPartials[n+k] * fProportions[l];
			}
		}
    } // integratePartials

	/**
	 * Calculates pattern log likelihoods at a node.
	 * @param fPartials the partials used to calculate the likelihoods
	 * @param fFrequencies an array of state frequencies
	 * @param fOutLogLikelihoods an array into which the likelihoods will go
	 */
    @Override
	public void calculateLogLikelihoodsP(double[] fPartials,double[] fOutLogLikelihoods)
	{
        int v = 0;
		for (int k = 0; k < m_nPatterns; k++) {
            double sum = 0.0;
			for (int i = 0; i < m_nStates; i++) {
				sum += fPartials[v];
				v++;
			}
            fOutLogLikelihoods[k] = Math.log(sum) + getLogScalingFactor(k);
		}
	}
	
	
	//    @Override
//    LikelihoodCore feelsGood() {return null;}
} // class BeerLikelihoodCore
