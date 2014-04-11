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




import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.Map;
import beast.evolution.datatype.DataType;
import beast.util.AddOnManager;



@Description("Class representing alignment data")
public class Alignment extends Map<String> {
	protected Class<?> mapType() {return String.class;}

	/**
     * default data type *
     */
    final static String NUCLEOTIDE = "nucleotide";

    /**
     * directory to pick up data types from *
     */
    final static String[] IMPLEMENTATION_DIR = {"beast.evolution.datatype"};

    /**
     * list of data type descriptions, obtained from DataType classes *
     */
    static List<String> types = new ArrayList<String>();

    static {
    	findDataTypes();
    }
    
    static public void findDataTypes() {
        // build up list of data types
        List<String> m_sDataTypes = AddOnManager.find(beast.evolution.datatype.DataType.class, IMPLEMENTATION_DIR);
        for (String sDataType : m_sDataTypes) {
            try {
                DataType dataType = (DataType) Class.forName(sDataType).newInstance();
                if (dataType.isStandard()) {
                    String sDescription = dataType.getDescription();
                    if (!types.contains(sDescription)) {
                    	types.add(sDescription);
                    }
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }


    public Input<List<Sequence>> sequenceInput =
            new Input<List<Sequence>>("sequence", "sequence and meta data for particular taxon", new ArrayList<Sequence>(), Validate.OPTIONAL);
    public Input<Integer> stateCountInput = new Input<Integer>("statecount", "maximum number of states in all sequences");
    //public Input<String> m_sDataType = new Input<String>("dataType", "data type, one of " + Arrays.toString(TYPES), NUCLEOTIDE, TYPES);
    public Input<String> dataTypeInput = new Input<String>("dataType", "data type, one of " + types, NUCLEOTIDE, types.toArray(new String[0]));
    public Input<DataType.Base> userDataTypeInput= new Input<DataType.Base>("userDataType", "non-standard, user specified data type, if specified 'dataType' is ignored");
    public Input<Boolean> stripInvariantSitesInput = new Input<Boolean>("strip", "sets weight to zero for sites that are invariant (e.g. all 1, all A or all unkown)", false);
    public Input<String> siteWeightsInput = new Input<String>("weights","comma separated list of weights, one for each site in the sequences. If not specified, each site has weight 1");
    
    /**
     * list of taxa names defined through the sequences in the alignment *
     */
    protected List<String> taxaNames = new ArrayList<String>();

    /**
     * list of state counts for each of the sequences, typically these are
     * constant throughout the whole alignment.
     */
    protected List<Integer> stateCounts = new ArrayList<Integer>();

    /**
     * maximum of m_nStateCounts *
     */
    protected int maxStateCount;

    /**
     * state codes for the sequences *
     */
    protected List<List<Integer>> counts = new ArrayList<List<Integer>>();

    /**
     * data type, useful for converting String sequence to Code sequence, and back *
     */
    protected DataType m_dataType;

    /**
     * weight over the columns of a matrix *
     */
    protected int [] patternWeight;

    /**
     * weights of sites -- assumed 1 for each site if not specified
     */
    protected int [] siteWeights = null;
    
    /**
     * pattern state encodings *
     */
    protected int [][] sitePatterns; // #patters x #taxa

    /**
     * maps site nr to pattern nr *
     */
    protected int [] patternIndex;


    public Alignment() {
    }

    /**
     * Constructor for testing purposes.
     *
     * @param sequences
     * @param stateCount
     * @param dataType
     * @throws Exception when validation fails
     */
    public Alignment(List<Sequence> sequences, Integer stateCount, String dataType) throws Exception {

        for (Sequence sequence : sequences) {
            sequenceInput.setValue(sequence, this);
        }
        //m_nStateCount.setValue(stateCount, this);
        dataTypeInput.setValue(dataType, this);
        initAndValidate();
    }


    @Override
    public void initAndValidate() throws Exception {
    	if (sequenceInput.get().size() == 0 && defaultInput.get().size() == 0) {
    		throw new Exception("Either a sequence input must be specified, or a map of strings must be specified");
    	}
    	
    	if (siteWeightsInput.get() != null) {
    		String sStr = siteWeightsInput.get().trim();
    		String [] strs = sStr.split(",");
    		siteWeights = new int[strs.length];
    		for (int i = 0; i< strs.length; i++) {
    			siteWeights[i] = Integer.parseInt(strs[i].trim());
    		}    		
    	}
    	
        // determine data type, either user defined or one of the standard ones
        if (userDataTypeInput.get() != null) {
            m_dataType = userDataTypeInput.get();
        } else {
            if (types.indexOf(dataTypeInput.get()) < 0) {
                throw new Exception("data type + '" + dataTypeInput.get() + "' cannot be found. " +
                        "Choose one of " + Arrays.toString(types.toArray(new String[0])));
            }
            List<String> sDataTypes = AddOnManager.find(beast.evolution.datatype.DataType.class, IMPLEMENTATION_DIR);
            for (String sDataType : sDataTypes) {
                DataType dataType = (DataType) Class.forName(sDataType).newInstance();
                if (dataTypeInput.get().equals(dataType.getDescription())) {
                    m_dataType = dataType;
                    break;
                }
            }
        }
        
        // grab data from child sequences
        taxaNames.clear();
        stateCounts.clear();
        counts.clear();
        if (sequenceInput.get().size() > 0) {
	        for (Sequence seq : sequenceInput.get()) {
	            //m_counts.add(seq.getSequence(getMap()));
	            counts.add(seq.getSequence(m_dataType));
	            if (taxaNames.indexOf(seq.taxonInput.get()) >= 0) {
	                throw new Exception("Duplicate taxon found in alignment: " + seq.taxonInput.get());
	            }
	            taxaNames.add(seq.taxonInput.get());
	            stateCounts.add(seq.totalCountInput.get());
	        }
	        if (counts.size() == 0) {
	            // no sequence data
	            throw new Exception("Sequence data expected, but none found");
	        }
        } else {
        	for (String key : map.keySet()) {
        		String sequence = map.get(key);
        		List<Integer> list = m_dataType.string2state(sequence);
        		counts.add(list);
	            if (taxaNames.indexOf(key) >= 0) {
	                throw new Exception("Duplicate taxon found in alignment: " + key);
	            }
	            taxaNames.add(key);
	            stateCounts.add(m_dataType.getStateCount());
        	}
        }
        // Sanity check: make sure sequences are of same length
        int nLength = counts.get(0).size();
        for (List<Integer> seq : counts) {
            if (seq.size() != nLength) {
                throw new Exception("Two sequences with different length found: " + nLength + " != " + seq.size());
            }
        }
        if (siteWeights != null && siteWeights.length != nLength) {
        	throw new RuntimeException("Number of weights (" + siteWeights.length + ") does not match sequence length (" + nLength +")");
        }

        calcPatterns();
    } // initAndValidate


    /*
     * assorted getters and setters *
     */
    public List<String> getTaxaNames() {
        if (taxaNames.size() == 0) {
        	try {
        		initAndValidate();
        	} catch (Exception e) {
        		e.printStackTrace();
        		throw new RuntimeException(e);
        	}
        }
        return taxaNames;
    }

    public List<Integer> getStateCounts() {
        return stateCounts;
    }

    public List<List<Integer>> getCounts() {
        return counts;
    }

    public DataType getDataType() {
        return m_dataType;
    }

    public int getNrTaxa() {
        return taxaNames.size();
    }

    public int getTaxonIndex(String sID) {
        return taxaNames.indexOf(sID);
    }

    /**
     * @return Number of unique character patterns in alignment.
     */
    public int getPatternCount() {
        return sitePatterns.length;
    }

    public int[] getPattern(int iPattern) {
        return sitePatterns[iPattern];
    }

    public int getPattern(int iTaxon, int iPattern) {
        return sitePatterns[iPattern][iTaxon];
    }

    /**
     * Retrieve the "weight" of a particular pattern: the number of sites
     * having that pattern.
     * 
     * @param iPattern Index into pattern array.
     * @return pattern weight
     */
    public int getPatternWeight(int iPattern) {
        return patternWeight[iPattern];
    }

    public int getMaxStateCount() {
        return maxStateCount;
    }

    /**
     * Retrieve index of pattern corresponding to a particular site.
     * 
     * @param iSite Index of site.
     * @return Index of pattern.
     */
    public int getPatternIndex(int iSite) {
        return patternIndex[iSite];
    }

    /**
     * @return Total number of sites in alignment.
     */
    public int getSiteCount() {
        return patternIndex.length;
    }

    /**
     * Retrieve an array containing the number of times each character pattern
     * occurs in the alignment.
     * 
     * @return Pattern weight array.
     */
    public int[] getWeights() {
        return patternWeight;
    }


    /**
     * SiteComparator is used for ordering the sites,
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
        int nTaxa = counts.size();
        int nSites = counts.get(0).size();

        // convert data to transposed int array
        int[][] nData = new int[nSites][nTaxa];
        for (int i = 0; i < nTaxa; i++) {
            List<Integer> sites = counts.get(i);
            for (int j = 0; j < nSites; j++) {
                nData[j][i] = sites.get(j);
            }
        }

        // sort data
        SiteComparator comparator = new SiteComparator();
        Arrays.sort(nData, comparator);

        // count patterns in sorted data
        // if (siteWeights != null) the weights are recalculated below
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
        patternWeight = new int[nPatterns];
        sitePatterns = new int[nPatterns][nTaxa];
        for (int i = 0; i < nPatterns; i++) {
            patternWeight[i] = weights[i];
            sitePatterns[i] = nData[i];
        }

        // find patterns for the sites
        patternIndex = new int[nSites];
        for (int i = 0; i < nSites; i++) {
            int[] sites = new int[nTaxa];
            for (int j = 0; j < nTaxa; j++) {
                sites[j] = counts.get(j).get(i);
            }
            patternIndex[i] = Arrays.binarySearch(sitePatterns, sites, comparator);
        }
        
        if (siteWeights != null) {
        	Arrays.fill(patternWeight, 0);
            for (int i = 0; i < nSites; i++) {
            	patternWeight[patternIndex[i]] += siteWeights[i];
            }        	
        }

        // determine maximum state count
        // Usually, the state count is equal for all sites,
        // though for SnAP analysis, this is typically not the case.
        maxStateCount = 0;
        for (int m_nStateCount1 : stateCounts) {
            maxStateCount = Math.max(maxStateCount, m_nStateCount1);
        }
        // report some statistics
        if (taxaNames.size() < 30) {
	        for (int i = 0; i < taxaNames.size(); i++) {
	            System.err.println(taxaNames.get(i) + ": " + counts.get(i).size() + " " + stateCounts.get(i));
	        }
        }

        if (stripInvariantSitesInput.get()) {
            // don't add patterns that are invariant, e.g. all gaps
            System.err.print("Stripping invariant sites");
            int removedSites = 0;
            for (int i = 0; i < nPatterns; i++) {
                int[] nPattern = sitePatterns[i];
                int iValue = nPattern[0];
                boolean bIsInvariant = true;
                for (int k = 1; k < nPattern.length; k++) {
                    if (nPattern[k] != iValue) {
                        bIsInvariant = false;
                        break;
                    }
                }
                if (bIsInvariant) {
                	removedSites += patternWeight[i]; 
                    patternWeight[i] = 0;
                    System.err.print(" <" + iValue + "> ");
                }
            }
            System.err.println(" removed " + removedSites + " sites ");
        }

        int totalWeight = 0;
        for (int weight : patternWeight) {
        	totalWeight += weight;
        }
        
        System.out.println(getNrTaxa() + " taxa");
        System.out.println(getSiteCount() + " sites" + (totalWeight == getSiteCount() ? "" : " with weight " + totalWeight));
        System.out.println(getPatternCount() + " patterns");

    } // calcPatterns


    /**
     * returns an array containing the non-ambiguous states that this state represents.
     */
    public boolean[] getStateSet(int iState) {
        return m_dataType.getStateSet(iState);
//        if (!isAmbiguousState(iState)) {
//            boolean[] stateSet = new boolean[m_nMaxStateCount];
//            stateSet[iState] = true;
//            return stateSet;
//        } else {
//        }
    }

    boolean isAmbiguousState(int state) {
        return (state >= 0 && state < maxStateCount);
    }

} // class Data
