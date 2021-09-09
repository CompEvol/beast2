package beast.evolution.distance;

import beast.base.Description;


@Description("Hamming distance is the mean number of characters that differ between sequences. " +
        "Note that unknowns are not ignored, so if both are unknowns '?' the distance is zero.")
public class HammingDistance extends Distance.Base {

    @Override
    public double pairwiseDistance(int taxon1, int taxon2) {
        double dist = 0;
        for (int i = 0; i < patterns.getPatternCount(); i++) {
            if (patterns.getPattern(taxon1, i) != patterns.getPattern(taxon2, i)) {
                dist += patterns.getPatternWeight(i);
            }
        }
        return dist / patterns.getSiteCount();
    }

}
