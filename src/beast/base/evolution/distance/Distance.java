package beast.base.evolution.distance;

import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.datatype.DataType;

@Description("Provides distance between taxa")
public interface Distance {
	
	/** return distance of two taxa, identified by their indices **/
	double pairwiseDistance(int taxon1, int taxon2);
	
	
	@Description("Provides distance between two sequences in an alignment")
	public class Base extends BEASTObject implements Distance {
	
	    //public static final double MAX_DISTANCE = 1000.0;
	    public static final double MAX_DISTANCE = 5.0;
	
	    @Override
	    public void initAndValidate() {
	        // nothing to do
	    }
	
	    /**
	     * set the pattern source
	     */
	    public void setPatterns(Alignment patterns) {
	        this.taxa = new TaxonSet();
	        try {
	            this.taxa.alignmentInput.setValue(patterns, this.taxa);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        this.patterns = patterns;
	        dimension = patterns.getTaxonCount();
	        dataType = patterns.getDataType();
	        distancesKnown = false;
	    }
	
	
	    /**
	     * Calculate a pairwise distance
	     */
	    @Override
	    public double pairwiseDistance(int taxon1, int taxon2) {
	        int state1, state2;
	
	        int n = patterns.getPatternCount();
	        double weight, distance;
	        double sumDistance = 0.0;
	        double sumWeight = 0.0;
	
	        int[] pattern;
	
	        for (int i = 0; i < n; i++) {
	            pattern = patterns.getPattern(i);
	
	            state1 = pattern[taxon1];
	            state2 = pattern[taxon2];
	
	            weight = patterns.getPatternWeight(i);
	//			sumDistance += dataType.getObservedDistance(state1, state2) * weight;
	            if (!dataType.isAmbiguousCode(state1) && !dataType.isAmbiguousCode(state2) &&
	                    state1 != state2) {
	                sumDistance += weight;
	            }
	            sumWeight += weight;
	        }
	
	        distance = sumDistance / sumWeight;
	
	        return distance;
	    }
	
	
	    //
	    // Private stuff
	    //
	
	    protected DataType dataType = null;
	    int dimension = 0;
	    boolean distancesKnown;
	    //    private double[][] distances = null;
	    protected Alignment patterns = null;
	    private TaxonSet taxa = null;
	}
}
