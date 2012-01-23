package beast.evolution.alignment.distance;

import beast.core.Description;


@Description("Hamming distance is the mean number of characters that differ between sequences. " +
        "Note that unknowns are not ignored, so if both are unknowns '?' the distance is zero.")
public class HammingDistance extends Distance {

    @Override
    public double calculatePairwiseDistance(int taxon1, int taxon2) {
        double fDist = 0;
        for (int i = 0; i < patterns.getPatternCount(); i++) {
            if (patterns.getPattern(taxon1, i) != patterns.getPattern(taxon2, i)) {
                fDist += patterns.getPatternWeight(i);
            }
        }
        return fDist / patterns.getSiteCount();
    }

}
