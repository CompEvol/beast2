
/*
 * File FCacheT.java
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

import java.util.Arrays;
import java.util.Vector;

import snap.FMatrix;
import snap.NodeData;

/** cache for storing F matrices used for Site Probability calculation **/
public class FCacheT extends FCache {

	public FCacheT(int nNodeNrMax, int nRedsMax) {
		super(nNodeNrMax, nRedsMax);
	} //c'tor

	CacheObject getLeafF(NodeData node, int numReds) {
		if (m_leafCache[node.getNr()][numReds] == null) {
			// it's not in the cache yet, so create the object
			SiteProbabilityCalculator.doLeafLikelihood(node, numReds, false);
			//FMatrix Fb = node.cloneFb();
			FMatrix Fb = node.getFb();
			synchronized(this) {
				if (m_leafCache[node.getNr()][numReds] != null) {
					return m_leafCache[node.getNr()][numReds];
				}
				CacheObject o = new CacheObject(Fb, nextID());
				m_leafCache[node.getNr()][numReds] = o;
			}
		}
		return m_leafCache[node.getNr()][numReds];
	} // getLeafF
	
	CacheObject getTopOfBrancheF(int nCacheID, NodeData node, double u, double v) throws Exception {
		while (nCacheID >= m_TopOfBranche.size()) {
			m_TopOfBranche.add(null);
		}
		CacheObject o = m_TopOfBranche.elementAt(nCacheID);//m_TopOfBrancheMap.get(nCacheID);
		if (o == null) {
			// it's not in the cache yet, so create the object
			SiteProbabilityCalculator.doTopOfBranchLikelihood(node, u, v, false);
			//FMatrix Ft = node.cloneFt(); 
			synchronized (this) {
				if (m_TopOfBranche.elementAt(nCacheID) == null) {
					FMatrix Ft = node.getFt(); 
					o = new CacheObject(Ft, nextID());
					m_TopOfBrancheID[node.getNr()].add(o);
					m_TopOfBranche.set(nCacheID, o);
				} else {
					o = m_TopOfBranche.elementAt(nCacheID);
				}
			}
//		} else if (o.m_F == null) {
//			// it's removed from the cache, so recalculate the F matrix
//			SiteProbabilityCalculator.doTopOfBranchLikelihood(node, u, v, false);
//			synchronized (this) {
//				if (m_TopOfBranche.elementAt(nCacheID).m_F == null) {
//					o.m_F = node.getFt();
//				} else {
//					o = m_TopOfBranche.elementAt(nCacheID);
//				}
//			}
		}
		return o;
	} // getTopOfBrancheF


	CacheObject getBottomOfBrancheF(int nCacheID1, int nCacheID2, NodeData u1, NodeData u2, NodeData parent) {
//		if (false) {
//			SiteProbabilityCalculator.doInternalLikelihood(u1, u2, parent, false);
//			return new CacheObject(parent.getFb(), -1);
//		}
		// try to fetch result from cache
		while (m_BottomOfBranche.size() <= nCacheID1) {
			m_BottomOfBranche.add(null);
		}
		Vector<CacheObject2> nodeCache2 = m_BottomOfBranche.elementAt(nCacheID1);

		if (nodeCache2 == null) {
			synchronized (this) {
				if (m_BottomOfBranche.elementAt(nCacheID1) != null) {
					// in case antoher thread got here first
					return getBottomOfBrancheF(nCacheID1, nCacheID2, u1, u2, parent);
				}
				nodeCache2 = new Vector<CacheObject2>();
			}
			m_BottomOfBranche.set(nCacheID1, nodeCache2);
			// not in cache, try to fetch from cache with IDs swapped
			// TODO: this does not get any hits as long as branch lengths (=t*gamma) are not part of the Key.
			// TODO: Fix this!
			SiteProbabilityCalculator.doInternalLikelihood(u1, u2, parent, false);
			//FMatrix Fb = parent.cloneFb();
			FMatrix Fb = parent.getFb();
			synchronized (this) {
				nodeCache2 = m_BottomOfBranche.elementAt(nCacheID1);
				if (nodeCache2.size() > 0) {
					// some other thread was here before the current one
					return getBottomOfBrancheF(nCacheID1, nCacheID2, u1, u2, parent);
				}
				CacheObject2 o = new CacheObject2(Fb, nextID(), nCacheID2);
				m_BottomOfBrancheID[parent.getNr()].add(o);
				nodeCache2.add(o);
				return o;
			}
		} 
		for (int i = 0; i < nodeCache2.size(); i++) {
			CacheObject2 o = nodeCache2.elementAt(i);
			if (o.m_nCacheID2 == nCacheID2) {
//				if (o.m_F == null) {
//					// it was removed from the cache, so recalculate it
//					SiteProbabilityCalculator.doInternalLikelihood(u1, u2, parent, false);
//					o.m_F = parent.getFb();
//				}
				return o;
			}
		}
		// it's not in the cache yet, so create the object
		SiteProbabilityCalculator.doInternalLikelihood(u1, u2, parent, false);
		//FMatrix Fb = parent.cloneFb();
		FMatrix Fb = parent.getFb();
		synchronized(this) {
			// make sure another thread did not already put the result in the cache
			for (int i = 0; i < nodeCache2.size(); i++) {
				CacheObject2 o = nodeCache2.elementAt(i);
				if (o.m_nCacheID2 == nCacheID2) {
					return o;
				}
			}
			// good to go now, just update the cache with our result
			CacheObject2 o = new CacheObject2(Fb, nextID(), nCacheID2);
			m_BottomOfBrancheID[parent.getNr()].add(o);
			nodeCache2.add(o);
			return o;
		}
	} // getNodeF
	
	/** print F matrix, for debugging purposes **/
	void printF(double[][]F) {
		for (int i = 1; i < F.length; i++) {
			System.err.println(Arrays.toString(F[i]));
		}
	}
} // class FCache
