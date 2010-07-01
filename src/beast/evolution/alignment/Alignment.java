
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
package beast.evolution.alignment;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import beast.core.*;
import beast.core.Input.Validate;

/* Class representing alignment data.
 * **/
@Description("Class representing alignment data")
public class Alignment extends Plugin {

	public Input<List<Sequence>> m_pSequences = new Input<List<Sequence>>("sequence", "sequence and meta data for particular taxon", new ArrayList<Sequence>(), Validate.REQUIRED);
	public Input<Integer> m_nStateCount = new Input<Integer>("statecount", "maximum number of states in all sequences");
	public Input<String> m_sDataType = new Input<String>("dataType", "data type (nucleotide, integerdata, etc)", Validate.REQUIRED);

	public List<String> m_sTaxaNames = new ArrayList<String>();
	public List<Integer> m_nStateCounts = new ArrayList<Integer>();
	public List<List<Integer>> m_counts = new ArrayList<List<Integer>>();

	// weight over the columns of a matrix
	public int [] m_nWeight;
	public int [][] m_nPatterns; // #patters x #taxa
	protected int m_nMaxStateCount;
	/** maps site nr to pattern nr **/
	protected int [] m_nPatternIndex;

	@Override
	public void initAndValidate(State state) throws Exception {

		// grab data from child sequences
		for (Sequence seq : m_pSequences.get()) {
			m_counts.add(seq.getSequence(getMap()));
			m_sTaxaNames.add(seq.m_sTaxon.get());
			m_nStateCounts.add(seq.m_nTotalCount.get());
		}
		if (m_counts.size() == 0) {
			// no sequence data
			throw new Exception("Sequence data expected, but none found");
		}

		// Sanity check: make sure sequences are of same length
		int nLength = m_counts.get(0).size();
		for (List<Integer> seq : m_counts) {
			if (seq.size() != nLength) {
				throw new Exception("Two sequences with different length found: " + nLength +" != " + seq.size());
			}
		}

		calcPatterns();
	} // initAndValidate

	/** return string (depending on datatype) which represents map of character onto
	 * state representation, e.g. the string "ACGT" corresponds to a map that encodes
	 * 'A' as 0, 'C' as 1, 'G' as 2 and 'T' as 3. Unknown characters are mapped to the
	 * length of the string, so a '-' with map "ACGT" is encoded as 4.**/
	String getMap() {
		if (m_sDataType.get().equals("nucleotide")) {
			return "ACGT";
		}
		if (m_sDataType.get().equals("binary")) {
			return "01";
		}
		if (m_sDataType.get().equals("twoStateCovarion")) {
			return "abcd";
		}
		return null;
	} // getMap


	/** assorted getters and setters **/
	public int getNrTaxa() {return m_sTaxaNames.size();}
	public int getTaxonIndex(String sID) {return m_sTaxaNames.indexOf(sID);}
	public int getPatternCount() {return m_nPatterns.length;}
	public int [] getPattern(int id) {return m_nPatterns[id];}
	public int getPattern(int iTaxon, int id) {return m_nPatterns[id][iTaxon];}
	public int getPatternWeight(int id) {return m_nWeight[id];}
	public int getMaxStateCount() {return m_nMaxStateCount;}
	public int getPatternIndex(int id) {return m_nPatternIndex[id];}
	public int getSiteCount() {return m_nPatternIndex.length;}



	class SiteComparator implements Comparator<int[]> {
		public int compare(int[] o1, int[] o2) {
			for (int i = 0; i < o1.length; i++) {
				if (o1[i] > o2[i]) {
					return 1;
				}
				if (o1[i] < o2[i]) {
					return -1;
				}
			}
			return 0;
		}
	} // class SiteComparator

	/** calculate patterns from sequence data
	 * **/
	protected void calcPatterns() {
		int nTaxa = m_counts.size();
		int nSites = m_counts.get(0).size();

		// convert data to transposed int array
		int [][] nData = new int[nSites][nTaxa];
		for (int i = 0; i < nTaxa; i++) {
			List<Integer> sites = m_counts.get(i);
			for (int j = 0; j < nSites; j++) {
				nData[j][i] = sites.get(j);
			}
		}

		// sort data
		SiteComparator comparator = new SiteComparator();
		Arrays.sort(nData, comparator);

		// count patterns in sorted data
		int nPatterns = 1;
		int [] weights = new int[nSites];
		weights[0] = 1;
		m_nPatternIndex = new int[nSites];
		m_nPatternIndex[0] = 0;
		for (int i = 1; i < nSites; i++) {
			if (comparator.compare(nData[i-1], nData[i]) != 0) {
				nPatterns++;
				nData[nPatterns-1] = nData[i];
			}
			weights[nPatterns-1]++;
			m_nPatternIndex[i] = nPatterns - 1;
		}
		// reserve memory for patterns
		m_nWeight = new int[nPatterns];
		m_nPatterns = new int[nPatterns][nTaxa];
		for (int i = 0; i < nPatterns; i++) {
			m_nWeight[i] = weights[i];
			m_nPatterns[i] = nData[i];
		}






//		// find unique patterns
//		int nSites = m_counts.get(0).size();
//		int [] weights = new int[nSites];
//		for (int i = 0; i < weights.length; i++) {
//			int j = 0;
//			for (j = 0; j < i; j++) {
//				if (isEqual(i,j)) {
//					break;
//				}
//			}
//			weights[j]++;
//		}
//		// count nr of patterns
//		int nPatterns = 0;
//		for (int i = 0; i < weights.length; i++) {
//			if (weights[i]>0) {
//				nPatterns++;
//			}
//		}
//		// reserve memory for patterns
//		m_nWeight = new int[nPatterns];
//		m_nPatterns = new int[nPatterns][nTaxa];
//		m_nPatternIndex = new int[nSites];
//
//		nPatterns = 0;
//		int iSite = 0;
//		// instantiate patterns
//		for (int i = 0; i < nSites; i++) {
//			if (weights[i]>0) {
//				m_nWeight[nPatterns] = weights[i];
//				for (int j = 0; j < nTaxa; j++) {
//					m_nPatterns[nPatterns][j] = m_counts.get(j).get(i);
//				}
//				for (int k = 0; k < weights[i]; k++) {
//					m_nPatternIndex[iSite++] = nPatterns;
//				}
//				nPatterns++;
//			}
//		}
		// determine maximum state count
		// Usually, the state count is equal for all sites,
		// though for SnAP analysis, this is typically not the case.
		m_nMaxStateCount = 0;
		for (int i = 0; i < m_nStateCounts.size(); i++) {
			m_nMaxStateCount = Math.max(m_nMaxStateCount, m_nStateCounts.get(i));
		}
		// report some statistics
		for (int i = 0; i < m_sTaxaNames.size(); i++) {
			System.err.println(m_sTaxaNames.get(i) + ": " + m_counts.get(i).size() + " " + m_nStateCounts.get(i));
		}
		System.err.println(getNrTaxa() + " taxa");
		System.err.println(getSiteCount() + " sites");
		System.err.println(getPatternCount() + " patterns");
	} // calcPatterns


} // class Data
