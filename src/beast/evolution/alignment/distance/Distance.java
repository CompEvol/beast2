package beast.evolution.alignment.distance;

import beast.core.Description;
import beast.core.BEASTObject;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.datatype.DataType;

@Description("Provides distance between two sequences in an alignment")
public class Distance extends BEASTObject {

    //public static final double MAX_DISTANCE = 1000.0;
    public static final double MAX_DISTANCE = 5.0;

    @Override
    public void initAndValidate() throws Exception {
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
        dimension = patterns.getNrTaxa();
        dataType = patterns.getDataType();
        distancesKnown = false;
    }


    /**
     * Calculate a pairwise distance
     */
    public double calculatePairwiseDistance(int taxon1, int taxon2) {
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
            if (!dataType.isAmbiguousState(state1) && !dataType.isAmbiguousState(state2) &&
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
