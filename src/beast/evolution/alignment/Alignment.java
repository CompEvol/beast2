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


import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.datatype.DataType;
import beast.util.ClassDiscovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/* Class representing alignment data.
 * **/

@Description("Class representing alignment data")
public class Alignment extends CalculationNode {
	/** default data type **/
    final static String NUCLEOTIDE = "nucleotide";
    
    /** directory to pick up data types from **/
	final static String[] IMPLEMENTATION_DIR = {"beast.evolution.datatype"};

	/** list of data type descriptions, obtained from DataType classes **/
	static List<String> m_sTypes = new ArrayList<String>();

	static {
		// build up list of data types
		List<String> m_sDataTypes = ClassDiscovery.find(beast.evolution.datatype.DataType.class, IMPLEMENTATION_DIR);
		for (String sDataType : m_sDataTypes) {
			try {
				DataType dataType = (DataType) Class.forName(sDataType).newInstance();
				if (dataType.isStandard()) {
					String sDescription = dataType.getDescription();
					m_sTypes.add(sDescription);
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}
	
	
	
    public Input<List<Sequence>> m_pSequences =
            new Input<List<Sequence>>("sequence", "sequence and meta data for particular taxon", new ArrayList<Sequence>(), Validate.REQUIRED);
    //public Input<Integer> m_nStateCount = new Input<Integer>("statecount", "maximum number of states in all sequences");
    //public Input<String> m_sDataType = new Input<String>("dataType", "data type, one of " + Arrays.toString(TYPES), NUCLEOTIDE, TYPES);
    public Input<String> m_sDataType = new Input<String>("dataType", "data type, one of " + m_sTypes, NUCLEOTIDE, m_sTypes.toArray(new String[0]));
    public Input<DataType.Base> m_userDataType = new Input<DataType.Base>("userDataType", "non-standard, user specified data type", Validate.XOR, m_sDataType);


    /** list of taxa names defined through the sequences in the alignment **/
    protected List<String> m_sTaxaNames = new ArrayList<String>();
    
    /** list of state counts for each of the sequences, typically these are
     * constant throughout the whole alignment.
     */
    protected List<Integer> m_nStateCounts = new ArrayList<Integer>();
    
    /** maximum of m_nStateCounts **/
    protected int m_nMaxStateCount;
    
    /** state codes for the sequences **/
    protected List<List<Integer>> m_counts = new ArrayList<List<Integer>>();
    
    /** data type, useful for converting String sequence to Code sequence, and back **/
    protected DataType m_dataType;
    
    /** weight over the columns of a matrix **/
    protected int[] m_nWeight;
    
    /** pattern state encodings **/
    protected int[][] m_nPatterns; // #patters x #taxa

    /**maps site nr to pattern nr **/
    protected int[] m_nPatternIndex;
    
    
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
        //m_nStateCount.setValue(stateCount, this);
        m_sDataType.setValue(dataType, this);
        initAndValidate();
    }



    @Override
    public void initAndValidate() throws Exception {
    	// determine data type, either user defined or one of the standard ones
        if( m_sDataType.get() == null ) {
        	m_dataType = m_userDataType.get();
        } else {
	        if (m_sTypes.indexOf(m_sDataType.get()) < 0) {
	            throw new Exception("data type + '" + m_sDataType.get() +"' cannot be found. " +
	            		"Choose one of " + m_sTypes.toArray(new String[0]));
	        }
			List<String> sDataTypes = ClassDiscovery.find(beast.evolution.datatype.DataType.class, IMPLEMENTATION_DIR);
			for (String sDataType : sDataTypes) {
				DataType dataType = (DataType) Class.forName(sDataType).newInstance();
				if (m_sDataType.get().equals(dataType.getDescription())) {
					m_dataType = dataType;
					break;
				}
			}
        }
        
        // grab data from child sequences
        for (Sequence seq : m_pSequences.get()) {
            //m_counts.add(seq.getSequence(getMap()));
            m_counts.add(seq.getSequence(m_dataType));
            if (m_sTaxaNames.indexOf(seq.m_sTaxon.get()) >= 0) {
            	throw new Exception("Duplicate taxon found in alignment: " + seq.m_sTaxon.get());
            }
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


    /*
     * assorted getters and setters *
     */
    public List<String> getTaxaNames() {return m_sTaxaNames;}
    public List<Integer> getStateCounts() {return m_nStateCounts;}
    public List<List<Integer>> getCounts() {return m_counts;}
    public DataType getDataType() {return m_dataType;}

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

    public int [] getWeights() {
        return m_nWeight;
    }


    /** SiteComparator is used for ordering the sites,
     * which makes it easy to identify patterns.
     */
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
