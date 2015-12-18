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

import java.util.*;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.Map;
import beast.core.util.Log;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.StandardData;
import beast.util.AddOnManager;

@Description("Class representing alignment data")
public class Alignment extends Map<String> {

    protected Class<?> mapType() {
        return String.class;
    }

    /**
     * default data type *
     */
    protected final static String NUCLEOTIDE = "nucleotide";

    /**
     * directory to pick up data types from *
     */
    final static String[] IMPLEMENTATION_DIR = {"beast.evolution.datatype"};

    /**
     * list of data type descriptions, obtained from DataType classes *
     */
    static List<String> types = new ArrayList<>();

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
                    String sDescription = dataType.getTypeDescription();
                    if (!types.contains(sDescription)) {
                        types.add(sDescription);
                    }
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    final public Input<List<Sequence>> sequenceInput =
            new Input<>("sequence", "sequence and meta data for particular taxon", new ArrayList<>(), Validate.OPTIONAL);

    final public Input<TaxonSet> taxonSetInput =
            new Input<>("taxa", "An optional taxon-set used only to sort the sequences into the same order as they appear in the taxon-set.", new TaxonSet(), Validate.OPTIONAL);

    final public Input<Integer> stateCountInput = new Input<>("statecount", "maximum number of states in all sequences");
    final public Input<String> dataTypeInput = new Input<>("dataType", "data type, one of " + types, NUCLEOTIDE, types.toArray(new String[0]));
    final public Input<DataType.Base> userDataTypeInput = new Input<>("userDataType", "non-standard, user specified data type, if specified 'dataType' is ignored");
    final public Input<Boolean> stripInvariantSitesInput = new Input<>("strip", "sets weight to zero for sites that are invariant (e.g. all 1, all A or all unkown)", false);
    final public Input<String> siteWeightsInput = new Input<>("weights", "comma separated list of weights, one for each site in the sequences. If not specified, each site has weight 1");

    final public Input<Boolean> isAscertainedInput = new Input<>("ascertained", "is true if the alignment allows ascertainment correction, i.e., conditioning the " +
            "Felsenstein likelihood on excluding constant sites from the alignment", false);
    /**
     * Inputs from AscertainedAlignment
     */
    final public Input<Integer> excludefromInput = new Input<>("excludefrom", "first site to condition on, default 0", 0);
    final public Input<Integer> excludetoInput = new Input<>("excludeto", "last site to condition on (but excluding this site), default 0", 0);
    final public Input<Integer> excludeeveryInput = new Input<>("excludeevery", "interval between sites to condition on (default 1)", 1);

    /**
     * list of sequences in the alignment *
     */
    protected List<Sequence> sequences = new ArrayList<>();

    /**
     * list of taxa names defined through the sequences in the alignment *
     */
    protected List<String> taxaNames = new ArrayList<>();

    /**
     * list of state counts for each of the sequences, typically these are
     * constant throughout the whole alignment.
     */
    protected List<Integer> stateCounts = new ArrayList<>();

    /**
     * maximum of m_nStateCounts *
     */
    protected int maxStateCount;

    /**
     * state codes for the sequences *
     */
    protected List<List<Integer>> counts = new ArrayList<>();

    /**
     * data type, useful for converting String sequence to Code sequence, and back *
     */
    protected DataType m_dataType;

    /**
     * weight over the columns of a matrix *
     */
    protected int[] patternWeight;

    /**
     * weights of sites -- assumed 1 for each site if not specified
     */
    protected int[] siteWeights = null;

    /**
     * Probabilities associated with each tip of the tree, for use when the
     * characters are uncertain.
     */
    public List<double[][]> tipLikelihoods = new ArrayList<>(); // #taxa x #sites x #states
    private boolean usingTipLikelihoods = false;
    
    /**
     * pattern state encodings *
     */
    protected int [][] sitePatterns; // #patterns x #taxa

    /**
     * maps site nr to pattern nr *
     */
    protected int[] patternIndex;

    /**
     * From AscertainedAlignment
     */
    Set<Integer> excludedPatterns;

    /**
     * A flag to indicate if the alignment is ascertained
     */
    public boolean isAscertained;

    public Alignment() {
    }

    /**
     * Constructor for testing purposes.
     *
     * @param sequences
     * @param stateCount
     * @param dataType
     * @throws Exception when validation fails
     * @deprecated This is the deprecated legacy form and will be removed
     * at some point. Use {@link #Alignment(List, String)} instead.
     */
    @Deprecated
    public Alignment(List<Sequence> sequences, Integer stateCount, String dataType) throws Exception {
        this(sequences, dataType);
    }

    /**
     * Constructor for testing purposes.
     *
     * @param sequences
     * @param dataType
     * @throws Exception when validation fails
     */
    public Alignment(List<Sequence> sequences, String dataType) throws Exception {

        for (Sequence sequence : sequences) {
            sequenceInput.setValue(sequence, this);
        }
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
            String[] strs = sStr.split(",");
            siteWeights = new int[strs.length];
            for (int i = 0; i < strs.length; i++) {
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
            // seems to spend forever in there??
            List<String> sDataTypes = AddOnManager.find(beast.evolution.datatype.DataType.class, IMPLEMENTATION_DIR);
            for (String sDataType : sDataTypes) {
                DataType dataType = (DataType) Class.forName(sDataType).newInstance();
                if (dataTypeInput.get().equals(dataType.getTypeDescription())) {
                    m_dataType = dataType;
                    break;
                }
            }
        }

        // initialize the sequence list
        if (sequenceInput.get().size() > 0) {
            sequences = sequenceInput.get();
        } else {
            // alignment defined by a map of id -> sequence
            List<String> taxa = new ArrayList<>();
            taxa.addAll(map.keySet());
            sequences.clear();
            for (String key : taxa) {
                String sequence = map.get(key);
                sequences.add(new Sequence(key, sequence));
            }
        }

        // initialize the alignment from the given list of sequences
        initializeWithSequenceList(sequences, true);

        if (taxonSetInput.get() != null && taxonSetInput.get().getTaxonCount() > 0) {
            sortByTaxonSet(taxonSetInput.get());
        }
        Log.info.println(toString(false));
    }

    /**
     * Initializes the alignment given the provided list of sequences and no other information.
     * It site weights and/or data type have been previously set up with initAndValidate then they
     * remain in place. This method is used mainly to re-order the sequences to a new taxon order
     * when an analysis of multiple alignments on the same taxa are undertaken.
     *
     * @param sequences
     */
    private void initializeWithSequenceList(List<Sequence> sequences, boolean log) {
        this.sequences = sequences;
        taxaNames.clear();
        stateCounts.clear();
        counts.clear();
        try {
            for (Sequence seq : sequences) {

                counts.add(seq.getSequence(m_dataType));
                if (taxaNames.contains(seq.getTaxon())) {
                    throw new RuntimeException("Duplicate taxon found in alignment: " + seq.getTaxon());
                }
                taxaNames.add(seq.getTaxon());
                tipLikelihoods.add(seq.getLikelihoods());
                // if seq.isUncertain() == false then the above line adds 'null'
	            // to the list, indicating that this particular sequence has no tip likelihood information
                usingTipLikelihoods |= (seq.getLikelihoods() != null);	            

                if (seq.totalCountInput.get() != null) {
                    stateCounts.add(seq.totalCountInput.get());
                } else {
                    stateCounts.add(m_dataType.getStateCount());
                }
            }
            if (counts.size() == 0) {
                // no sequence data
                throw new RuntimeException("Sequence data expected, but none found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        sanityCheckCalcPatternsSetUpAscertainment(log);
    }

    /**
     * Checks that sequences are all the same length, calculates patterns and sets up ascertainment.
     */
    private void sanityCheckCalcPatternsSetUpAscertainment(boolean log) {
        // Sanity check: make sure sequences are of same length
        int nLength = counts.get(0).size();
        if (!(m_dataType instanceof StandardData)) {
            for (List<Integer> seq : counts) {
                if (seq.size() != nLength) {
                    throw new RuntimeException("Two sequences with different length found: " + nLength + " != " + seq.size());
                }
            }
        }
        if (siteWeights != null && siteWeights.length != nLength) {
            throw new RuntimeException("Number of weights (" + siteWeights.length + ") does not match sequence length (" + nLength + ")");
        }

        calcPatterns(log);
        setupAscertainment();
    }

    /**
     * Sorts an alignment by a provided TaxonSet, so that the sequence/taxon pairs in the alignment match the order
     * that the taxa appear in the TaxonSet (i.e. not necessarily alphabetically).
     *
     * @param toSortBy the taxon set that species the order on the taxa.
     */
    public void sortByTaxonSet(TaxonSet toSortBy) {

        List<Sequence> sortedSeqs = new ArrayList<>();
        sortedSeqs.addAll(sequences);
        Collections.sort(sortedSeqs, (Sequence o1, Sequence o2) -> {
                return Integer.compare(toSortBy.getTaxonIndex(o1.getTaxon()), toSortBy.getTaxonIndex(o2.getTaxon()));
            }
        );
        initializeWithSequenceList(sortedSeqs, false);
    }

    void setupAscertainment() {
        isAscertained = isAscertainedInput.get();

        if (isAscertained) {
            //From AscertainedAlignment
            int iFrom = excludefromInput.get();
            int iTo = excludetoInput.get();
            int iEvery = excludeeveryInput.get();
            excludedPatterns = new HashSet<>();
            for (int i = iFrom; i < iTo; i += iEvery) {
                int iPattern = patternIndex[i];
                // reduce weight, so it does not confuse the tree likelihood
                patternWeight[iPattern] = 0;
                excludedPatterns.add(iPattern);
            }
        } else {
        	// sanity check
            int iFrom = excludefromInput.get();
            int iTo = excludetoInput.get();
            if (iFrom != excludefromInput.defaultValue || iTo != excludetoInput.defaultValue) {
            	Log.warning.println("WARNING: excludefrom or excludeto is specified, but 'ascertained' flag is not set to true");
            	Log.warning.println("WARNING: to suppress this warning, remove the excludefrom or excludeto attributes (if no astertainment correction is required)");
            	Log.warning.println("WARNING: or set the 'ascertained' flag to true on element with id=" + getID());
            }
        }

    } // initAndValidate

    static String getSequence(Alignment data, int taxonIndex) {

        int[] nStates = new int[data.getPatternCount()];
        for (int i = 0; i < data.getPatternCount(); i++) {
            int[] sitePattern = data.getPattern(i);
            nStates[i] = sitePattern[taxonIndex];
        }
        try {
            return data.getDataType().state2string(nStates);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return null;
    }


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

    /**
     * Returns a List of Integer Lists where each Integer List represents
     * the sequence corresponding to a taxon.  The taxon is identified by
     * the position of the Integer List in the outer List, which corresponds
     * to the nodeNr of the corresponding leaf node and the position of the
     * taxon name in the taxaNames list.
     *
     * @return integer representation of sequence alignment
     */
    public List<List<Integer>> getCounts() {
        return counts;
    }

    public DataType getDataType() {
        return m_dataType;
    }

    /**
     * @return number of taxa in Alignment.
     */
    public int getTaxonCount() {
        //if (taxonsetInput.get() != null) {
        //	return taxonsetInput.get().getTaxonCount();
        //}
        return taxaNames.size();
    }

    /**
     * @return number of taxa in Alignment.
     * @deprecated Use getTaxonCount() instead.
     */
    @Deprecated
    public int getNrTaxa() {
        return getTaxonCount();
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

    public int getPattern(int taxonIndex, int iPattern) {
        return sitePatterns[iPattern][taxonIndex];
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


    protected void calcPatterns() {
        calcPatterns(true);
    }

        /**
         * calculate patterns from sequence data
         * *
         */
    private void calcPatterns(boolean log) {
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
            if (usingTipLikelihoods || comparator.compare(nData[i - 1], nData[i]) != 0) {
            	// In the case where we're using tip probabilities, we need to treat each 
            	// site as a unique pattern, because it could have a unique probability vector.
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
        if (log && taxaNames.size() < 30) {
            for (int i = 0; i < taxaNames.size(); i++) {
                Log.info.println(taxaNames.get(i) + ": " + counts.get(i).size() + " " + stateCounts.get(i));
            }
        }

        if (stripInvariantSitesInput.get()) {
            // don't add patterns that are invariant, e.g. all gaps
            if (log) Log.info.println("Stripping invariant sites");

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

                    if (log) Log.info.print(" <" + iValue + "> ");
                }
            }
            if (log) Log.info.println(" removed " + removedSites + " sites ");
        }
    } // calcPatterns

    /**
     * @return the total weight of all the patterns (this is the effective number of sites)
     */
    private long getTotalWeight() {
        long totalWeight = 0;
        for (int weight : patternWeight) {
            totalWeight += weight;
        }
        return totalWeight;
    }

    /**
     * Pretty printing of vital statistics of an alignment including id, #taxa, #sites, #patterns and totalweight
     *
     * @param singleLine true if the string should fit on one line
     * @return string representing this alignment
     */
    public String toString(boolean singleLine) {
        long totalWeight = getTotalWeight();
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName() + "(" + getID() + ")");

        if (singleLine) {
            builder.append(": [taxa, patterns, sites] = [" + getTaxonCount() + ", " + getPatternCount());
            builder.append(", " + getTotalWeight() + "]");
        } else {

            long siteCount = getSiteCount();

            builder.append('\n');
            builder.append("  " + getTaxonCount() + " taxa");
            builder.append('\n');
            builder.append("  " + siteCount + (siteCount == 1 ? " site" : " sites") + (totalWeight == getSiteCount() ? "" : " with weight " + totalWeight + ""));
            builder.append('\n');
            if (siteCount > 1) {
                builder.append("  " + getPatternCount() + " patterns");
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    public double[] getTipLikelihoods(int iTaxon, int iPattern) {
    	if (iTaxon >= tipLikelihoods.size() || tipLikelihoods.get(iTaxon) == null) { 
    		return null; 
    	} else { 
    		return tipLikelihoods.get(iTaxon)[iPattern];
    	}
    	
    }
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

    //Methods from AscertainedAlignment
    public Set<Integer> getExcludedPatternIndices() {
        return excludedPatterns;
    }

    public int getExcludedPatternCount() {
        return excludedPatterns.size();
    }

    public double getAscertainmentCorrection(double[] patternLogProbs) {
        double excludeProb = 0, includeProb = 0, returnProb = 1.0;

        for (int i : excludedPatterns) {
            excludeProb += Math.exp(patternLogProbs[i]);
        }

        if (includeProb == 0.0) {
            returnProb -= excludeProb;
        } else if (excludeProb == 0.0) {
            returnProb = includeProb;
        } else {
            returnProb = includeProb - excludeProb;
        }
        return Math.log(returnProb);
    } // getAscertainmentCorrection

    /**
     * Should not be used. No special order of taxa are assumed. Taxa order should be left to user input.
     */
    @Deprecated
    static public void sortByTaxonName(List<Sequence> seqs) {
        Collections.sort(seqs, (Sequence o1, Sequence o2) -> {
                return o1.taxonInput.get().compareTo(o2.taxonInput.get());
            }
        );
    }

    /** 
     * Get String representation of a sequence according to the current datatype
     * @param taxon the name of the taxon to get the sequence from in the alignment
     * @return sequence in String representation
     */
	public String getSequenceAsString(String taxon) {
		int i = getTaxonIndex(taxon);		

		// build up string from underlying data using the current datatype
		int [] states = new int[getSiteCount()];
		for (int k = 0; k < getSiteCount(); k++) {
			int d = sitePatterns[patternIndex[k]][i];
			states[k] = d;
		}
		String seq = null;
		try {
			seq = m_dataType.state2string(states);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return seq;
	}
} // class Data
