
/*
 * File SnAPLikelihoodCoreT.java
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
package snap.likelihood;



import snap.NodeData;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import beast.app.BeastMCMC;

import beast.core.Data;
import beast.core.Node;

/** threaded version of SSSLikelihoodCore **/
public class SnAPLikelihoodCoreT  extends SnAPLikelihoodCore {

	public SnAPLikelihoodCoreT(Node root, Data data) {
		super(root, data);
	}

	static boolean stopRequested = false;
	Lock [] m_lock;
	private class SSSRunnable implements Runnable {
		int m_iStart;
		int m_iStep;
		int m_iMax;
		Data m_data;
		NodeData m_root;
		double m_u;
		double m_v;
		boolean m_bUseCache;

	  SSSRunnable(int iStart, int iStep, int iMax, Data data, NodeData root, double u, double v, boolean bUseCache) {
	    m_iStart = iStart;
	    m_iStep = iStep;
	    m_iMax = iMax;
	    m_data = data;
	    m_root = root;
	    m_u = u;
	    m_v = v;
	    m_bUseCache = bUseCache;
	  }
	  public void run() {
		  int iThread = m_iStart;
		  m_lock[iThread].lock();
	    for (int id = m_iStart; id < m_iMax; id+= m_iStep) {
			if (id>0 && id%100 == 0)
				System.err.print(id + " ");
			double siteL=0.0;
			try {
				int [] thisSite = m_data.getPattern(id);
				//siteL =  SiteProbabilityCalculatorT.computeSiteLikelihood(m_root, m_u, m_v, thisSite, m_bUseCache, false, m_iStart);
				siteL =  SiteProbabilityCalculatorT.computeSiteLikelihood(m_root, m_u, m_v, thisSite, m_bUseCache, false, 0);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				System.exit(1);
			}
//			if (siteL==0.0) {
//				return -10e100;
//			}
			patternProb[id] = siteL;
	    }
	    m_lock[iThread].unlock();
	  }
	}

	double [] patternProb;

	/**
	 Compute Likelihood of the allele counts

	 @param root  The beast.tree. Uses branch lengths and gamma values stored on this beast.tree.
	 @param u  Mutation rate from red to green
	 @param v Mutation rate from green to red
	 @param sampleSize  Number of samples taken at each species (index by id field of the NodeData)
	 @param redCount  Each entry is a different marker... for each marker the number of red alleles in each species.
	 @param siteProbs  Vector of probabilities (logL) for each site.
	 * @throws Exception
	 **/

	public double computeLogLikelihood(NodeData root, double u, double v,
			int [] sampleSizes,
			Data data,
			boolean bUseCache,
			boolean dprint /*= false*/) throws Exception
	{
		LineageCountCalculator.computeCountProbabilities(root,sampleSizes,dprint);
		//dprint = true;

			//TODO: Partial subtree updates over all sites.


			double forwardLogL = 0.0;
			int numPatterns = data.getPatternCount();

			//Temporarily store pattern probabilities... used for numerical checks.
			patternProb = new double[numPatterns];
			Arrays.fill(patternProb, -1);
			int nThreads = BeastMCMC.m_nThreads;
			SiteProbabilityCalculatorT.clearCache(root.getNodeCount(), data.getMaxStateCount(), nThreads);

			m_lock = new ReentrantLock[nThreads];
			for (int i = 0; i < nThreads; i++) {
				m_lock[i] = new ReentrantLock();
				BeastMCMC.g_exec.execute(new SSSRunnable(i, nThreads, numPatterns, data, root.copy(), u, v, bUseCache));
			}

			// correction for constant sites
//			int [] thisSite = new int[data.m_sTaxaNames.size()];
//			double P0 =  SiteProbabilityCalculator.computeSiteLikelihood(root,u,v,thisSite, false, false);
//			for (int i = 0; i < thisSite.length; i++) {
//				thisSite[i] = data.m_nStateCounts.elementAt(i);
//			}
//			double P1 =  SiteProbabilityCalculator.computeSiteLikelihood(root,u,v,thisSite, false, false);
//			forwardLogL-=(double) data.getSiteCount() * Math.log(1.0 - P0 - P1);
//			System.err.println(numPatterns + " " + forwardLogL);

			// wait for the threads to lock
			Thread.sleep(50);
			// wait for the other thread to finish
			for (int i = 0; i < nThreads; i++) {
				m_lock[i].lock();
			}

			for(int id = 0; id < numPatterns-2; id++) {
				forwardLogL+=(double)data.getPatternWeight(id) * Math.log(patternProb[id]);
			}
			// correction for constant sites
			double P0 =  patternProb[numPatterns - 2];
			double P1 =  patternProb[numPatterns -1 ];
			forwardLogL-=(double) data.getSiteCount() * Math.log(1.0 - P0 - P1);
			//System.err.println(numPatterns + " " + forwardLogL);
			return forwardLogL;
	} // computeLogLikelihood


} // class TreeLikelihood
