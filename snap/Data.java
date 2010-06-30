
/*
 * File Data.java
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
package snap;

import beast.core.Description;


@Description("Represents sequence data for SnAP analysis. "+
 "The difference with standard sequence data is that constant sites "+
 "are removed + 2 patterns are added at the end representing these "+
 "constant sites, but with zero weight. The likelihood calculator "+
 "deals with these different sites.")
public class Data extends beast.core.Data {

	/** check whether a pattern is all red or all green **/
	private boolean isConstant(int iSite) {
		int nTaxa = m_counts.size();
		boolean bAllZero = true;
		boolean bAllMax = true;
		for (int i = 0; i < nTaxa; i++) {
			int iValue = m_counts.get(i).get(iSite);
			if (iValue > 0) {
				bAllZero = false;
				if (bAllMax == false) {
					return false;
				}
			}
			if (iValue < m_nStateCounts.get(i)) {
				bAllMax = false;
				if (bAllZero == false) {
					return false;
				}
			}
		}
		return true;
	} // isConstant

	/** calculate patterns from sequence data
	 * The difference with standard sequence data is that constant sites
	 * are removed + 2 patterns are added at the end representing these
	 * constant sites, but with zero weight. The likelihood calculator
	 * deals with these different sites.
	 *
	 * **/
	@Override
	protected void calcPatterns() {
		// remove constant sites
		int nTaxa = m_counts.size();
		for (int i = 0; i < m_counts.get(0).size(); i++) {
			if (isConstant(i)) {
				for (int j = 0; j < nTaxa; j++) {
					m_counts.get(j).remove(i);
				}
				i--;
			}
		}

		// find unique patterns
		int nSites = m_counts.get(0).size();
		int [] weights = new int[nSites];
		for (int i = 0; i < weights.length; i++) {
			int j = 0;
			for (j = 0; j < i; j++) {
				if (isEqual(i,j)) {
					break;
				}
			}
			weights[j]++;
		}
		// count nr of patterns
		int nPatterns = 0;
		for (int i = 0; i < weights.length; i++) {
			if (weights[i]>0) {
				nPatterns++;
			}
		}
		m_nWeight = new int[nPatterns];
		m_nPatterns = new int[nPatterns+2][nTaxa];
//		m_nPatterns = new int[nPatterns][nTaxa];
		m_nPatternIndex = new int[nSites];

		nPatterns = 0;
		int iSite = 0;
		// instantiate patterns
		for (int i = 0; i < nSites; i++) {
			if (weights[i]>0) {
				m_nWeight[nPatterns] = weights[i];
				for (int j = 0; j < nTaxa; j++) {
					m_nPatterns[nPatterns][j] = m_counts.get(j).get(i);
				}
				for (int k = 0; k < weights[i]; k++) {
					m_nPatternIndex[iSite++] = nPatterns;
				}
				nPatterns++;
			}
		}
		m_nMaxStateCount = 0;
		for (int i = 0; i < m_nStateCounts.size(); i++) {
			m_nMaxStateCount = Math.max(m_nMaxStateCount, m_nStateCounts.get(i));
		}
		// report some statistics
		for (int i = 0; i < m_sTaxaNames.size(); i++) {
			System.err.println(m_sTaxaNames.get(i) + ": " + m_counts.get(i).size() + " " + m_nStateCounts.get(i));
		}
		// add dummy patterns
		for (int i = 0; i < nTaxa; i++) {
			m_nPatterns[nPatterns + 1][i] = 0;
			m_nPatterns[nPatterns + 1][i] = m_nStateCounts.get(i);
		}
		System.err.println(getSiteCount() + " sites");
		System.err.println(getPatternCount() + " patterns (2 dummies)");
	} // calc


	/** test whether two columns (e.g. sites) contain equal values **/
	protected boolean isEqual(int iSite1, int iSite2) {
		for (int i = 0; i < m_counts.size(); i++) {
			if (m_counts.get(i).get(iSite1)
					!= m_counts.get(i).get(iSite2)) {
				return false;
			}
		}
		return true;
	} // isEqual

}
