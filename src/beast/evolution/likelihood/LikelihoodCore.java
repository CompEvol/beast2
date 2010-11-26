
/*
 * File LikelihoodCore.java
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

/** The likelihood core is the class that performs the calculations
 * in the peeling algorithm (see Felsenstein, Joseph (1981). 
 * Evolutionary trees from DNA sequences: a maximum likelihood approach. 
 * J Mol Evol 17 (6): 368-376.). It does this by calculating the partial
 * results for all sites, node by node. The order in which the nodes
 * are visited is controlled by the TreeLikelihood. T
 * 
 * In order to reuse computations of previous likelihood calculations,
 * a current state, and a stored state are maintained. Again, the TreeLikelihood
 * controls when to update from current to stored and vice versa. So, in
 * LikelihoodCore implementations, duplicates need to be kept for all partials.
 * Also, a set of indices to indicate which of the data is stored state and which
 * is current state is commonly the most efficient way to sort out which is which.
 *   
 *   
 */

abstract public class LikelihoodCore {

	/** reserve memory for partials, indices and other 
	 * data structures required by the core **/
	abstract public void initialize(int nNodeCount, int nPatternCount, int nMatrixCount, boolean bIntegrateCategories);
	
	/** clean up after last likelihood calculation, if at all required **/
	abstract public void finalize() throws java.lang.Throwable;

	/** reserve memory for partials for node with number iNode **/
	abstract public void createNodePartials(int iNode);
	
	
	/** indicate that the partials for node 
	 * iNode is about the be changed, that is, that the stored
	 * state for node iNode cannot be reused **/
	abstract public void setNodePartialsForUpdate(int iNode);
	/** assign values of partials for node with number iNode **/
	// do we need these???
	//abstract public void setNodePartials(int iNode, double[] fPartials);
    //abstract public void setCurrentNodePartials(int iNode, double[] fPartials);

    /** reserve memory for states for node with number iNode **/
	//abstract public void createNodeStates(int iNode);

	/** assign values of states for node with number iNode **/
	abstract public void setNodeStates(int iNode, int[] iStates);
	
	/** indicate that the probability transition matrix for node 
	 * iNode is about the be changed, that is, that the stored
	 * state for node iNode cannot be reused **/
	abstract public void setNodeMatrixForUpdate(int iNode);
	
    /** assign values of states for probability transition matrix for node with number iNode **/
	abstract public void setNodeMatrix(int iNode, int iMatrixIndex, double[] fMatrix);
    /** assign values of states for probability transition matrices 
     * padded with 1s for dealing with unknown characters for node with number iNode **/
	abstract public void setPaddedNodeMatrices(int iNode, double[] fMatrix);


    
    /** indicate that the topology of the tree chanced so the cache 
	 * data structures cannot be reused **/
    public void setNodeStatesForUpdate(int iNode) {};
    

    
	/** flag to indicate whether scaling should be used in the
	 * likelihood calculation. Scaling can help in dealing with
	 * numeric issues (underflow).
	 */
	boolean m_bUseScaling = false;
	abstract public void setUseScaling(double fScale);
	public boolean getUseScaling() {return m_bUseScaling;}
	/** return the cumulative scaling effect. Should be zero if no scaling is used **/
    abstract public double getLogScalingFactor(int iPattern);

    /** Calculate partials for node iNode3, with children iNode1 and iNode2. 
     * NB Depending on whether the child nodes contain states or partials, the
     * calculation differs-**/
    abstract public void calculatePartials(int iNode1, int iNode2, int iNode3);
    //abstract public void calculatePartials(int iNode1, int iNode2, int iNode3, int[] iMatrixMap);
    /** integrate partials over categories (if any). **/
    abstract public void integratePartials(int iNode, double[] fProportions, double[] fOutPartials);

    /** calculate log likelihoods at the root of the tree,
     * using fFrequencies as root node distribution.
     * fOutLogLikelihoods contains the resulting probabilities for each of 
     * the patterns **/
	abstract public void calculateLogLikelihoods(double[] fPartials, double[] fFrequencies, double[] fOutLogLikelihoods);
	
    
    public void processStack() {}
	abstract protected void calculateIntegratePartials(double[] fInPartials, double[] fProportions, double[] fOutPartials);
    abstract public void calcRootPsuedoRootPartials(double[] fFrequencies, int iNode, double [] fPseudoPartials);
    abstract public void calcNodePsuedoRootPartials(double[] fInPseudoPartials, int iNode, double [] fOutPseudoPartials);
    abstract public void calcPsuedoRootPartials(double [] fParentPseudoPartials, int iNode, double [] fPseudoPartials);
    abstract void integratePartialsP(double [] fInPartials, double [] fProportions, double [] m_fRootPartials);
    abstract void calculateLogLikelihoodsP(double[] fPartials,double[] fOutLogLikelihoods);
    
    /** store current state **/
    abstract public void store();
    /** reset current state to stored state, only used when switching from non-scaled to scaled or vice versa **/
    abstract public void unstore();
    /** restore state **/
    abstract public void restore();
//    /** do internal diagnosics, and suggest an alternative core if appropriate **/ 
//    abstract LikelihoodCore feelsGood();
}
