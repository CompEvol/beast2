/*
* File Alignment.java
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

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/* Class representing alignment data.
 * **/

@Description("Class representing alignment data")
public class Alignment extends Plugin {
    static final String NUCLEOTIDE = "nucleotide";
    static final String BINARY = "binary";
    static final String TWOSTATECOVARION = "twoStateCovarion";
    static final String INTEGERDATA = "integerdata";
    static final String AMINOACID = "aminoacid";
    /** currently supported types **/
	String[] TYPES = {NUCLEOTIDE,BINARY,TWOSTATECOVARION,INTEGERDATA,AMINOACID};
	
    public Input<List<Sequence>> m_pSequences =
            new Input<List<Sequence>>("sequence", "sequence and meta data for particular taxon", new ArrayList<Sequence>(), Validate.REQUIRED);
    public Input<Integer> m_nStateCount = new Input<Integer>("statecount", "maximum number of states in all sequences");
    public Input<String> m_sDataType = new Input<String>("dataType", "data type, one of " + Arrays.toString(TYPES), NUCLEOTIDE, TYPES);
    //public Input<String> m_sName = new Input<String>("name", "name of the alignment");

    public List<String> m_sTaxaNames = new ArrayList<String>();
    public List<Integer> m_nStateCounts = new ArrayList<Integer>();
    public List<List<Integer>> m_counts = new ArrayList<List<Integer>>();


    public Alignment() {
    }

    /**
     * Constructor for testing purposes.
     *
     * @param sequences
     * @param stateCount
     * @param dataType
     * @throws Exception  when validation fails
     */
    public Alignment(List<Sequence> sequences, Integer stateCount, String dataType) throws Exception {

    	for (Sequence sequence : sequences) {
    		m_pSequences.setValue(sequence, this);
    	}
        m_nStateCount.setValue(stateCount, this);
        m_sDataType.setValue(dataType, this);
        initAndValidate();
    }


    // weight over the columns of a matrix
    public int[] m_nWeight;
    public int[][] m_nPatterns; // #patters x #taxa
    protected int m_nMaxStateCount;
    /**
     * maps site nr to pattern nr *
     */
    protected int[] m_nPatternIndex;

    @Override
    public void initAndValidate() throws Exception {
        if( m_sDataType == null ) {
            throw new Exception("Input 'dataType' must be specifie");
        }
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
                throw new Exception("Two sequences with different length found: " + nLength + " != " + seq.size());
            }
        }

        calcPatterns();
    } // initAndValidate

    /**
     * @return string (depending on datatype) which represents map of character onto
     * state representation, e.g. the string "ACGT" corresponds to a map that encodes
     * 'A' as 0, 'C' as 1, 'G' as 2 and 'T' as 3. Unknown characters are mapped to the
     * length of the string, so a '-' with map "ACGT" is encoded as 4.*
     */
    public String getMap() {
        if (m_sDataType.get().equals(NUCLEOTIDE)) {
            return "ACGT";
        }
        if (m_sDataType.get().equals(BINARY)) {
            return "01";
        }
        if (m_sDataType.get().equals(AMINOACID)) {
            return "arndcqeghilkmfpstwyv";
        }
        if (m_sDataType.get().equals(TWOSTATECOVARION)) {
            return "abcd";
        }
        return null;
    } // getMap


    /*
     * assorted getters and setters *
     */
    public int getNrTaxa() {
        return m_sTaxaNames.size();
    }

    public int getTaxonIndex(String sID) {
        return m_sTaxaNames.indexOf(sID);
    }

    public int getPatternCount() {
        return m_nPatterns.length;
    }

    public int[] getPattern(int id) {
        return m_nPatterns[id];
    }

    public int getPattern(int iTaxon, int id) {
        return m_nPatterns[id][iTaxon];
    }

    public int getPatternWeight(int id) {
        return m_nWeight[id];
    }

    public int getMaxStateCount() {
        return m_nMaxStateCount;
    }

    public int getPatternIndex(int iSite) {
        return m_nPatternIndex[iSite];
    }

    public int getSiteCount() {
        return m_nPatternIndex.length;
    }


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

    /**
     * calculate patterns from sequence data
     * *
     */
    protected void calcPatterns() {
        int nTaxa = m_counts.size();
        int nSites = m_counts.get(0).size();

        // convert data to transposed int array
        int[][] nData = new int[nSites][nTaxa];
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
        int[] weights = new int[nSites];
        weights[0] = 1;
        for (int i = 1; i < nSites; i++) {
            if (comparator.compare(nData[i - 1], nData[i]) != 0) {
                nPatterns++;
                nData[nPatterns - 1] = nData[i];
            }
            weights[nPatterns - 1]++;
        }
        
        // reserve memory for patterns
        m_nWeight = new int[nPatterns];
        m_nPatterns = new int[nPatterns][nTaxa];
        for (int i = 0; i < nPatterns; i++) {
            m_nWeight[i] = weights[i];
            m_nPatterns[i] = nData[i];
        }

        // find patterns for the sites
        m_nPatternIndex = new int[nSites];
        for (int i = 0; i < nSites; i++) {
        	int [] sites = new int[nTaxa];
            for (int j = 0; j < nTaxa; j++) {
            	sites[j] = m_counts.get(j).get(i);
            }
            m_nPatternIndex[i] = Arrays.binarySearch(m_nPatterns, sites, comparator);
        }

        // determine maximum state count
        // Usually, the state count is equal for all sites,
        // though for SnAP analysis, this is typically not the case.
        m_nMaxStateCount = 0;
        for(int m_nStateCount1 : m_nStateCounts) {
            m_nMaxStateCount = Math.max(m_nMaxStateCount, m_nStateCount1);
        }
        // report some statistics
        for (int i = 0; i < m_sTaxaNames.size(); i++) {
            System.err.println(m_sTaxaNames.get(i) + ": " + m_counts.get(i).size() + " " + m_nStateCounts.get(i));
        }
        System.err.println(getNrTaxa() + " taxa");
        System.err.println(getSiteCount() + " sites");
        System.err.println(getPatternCount() + " patterns");
    } // calcPatterns



    /**
     * returns an array containing the non-ambiguous states that this state represents.
     */
    public boolean[] getStateSet(int state) {

        boolean[] stateSet = new boolean[m_nMaxStateCount];
        if (!isAmbiguousState(state)) {
            for (int i = 0; i < m_nMaxStateCount; i++) {
                stateSet[i] = false;
            }

            stateSet[state] = true;
        } else {
            for (int i = 0; i < m_nMaxStateCount; i++) {
                stateSet[i] = true;
            }
        }
        return stateSet;
    }
    boolean isAmbiguousState(int state) {
    	return (state >=0 && state < m_nMaxStateCount);
    }

} // class Data
