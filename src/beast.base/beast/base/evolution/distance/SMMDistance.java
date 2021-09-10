package beast.base.evolution.distance;

import beast.base.core.Description;


/**
 * @author Chieh-Hsi Wu
 */
@Description("Calculate the distance between different microsatellite alleles")
public class SMMDistance extends Distance.Base {

    /**
     * constructor taking a pattern source
     *
     * @param patterns a pattern of a microsatellite locus
     */
    @Override
    public double pairwiseDistance(int taxon1, int taxon2) {

        int[] pattern = patterns.getPattern(0);
        int state1 = pattern[taxon1];

        int state2 = pattern[taxon2];
        double distance = 0.0;

        if (!dataType.isAmbiguousCode(state1) && !dataType.isAmbiguousCode(state2))
            distance = Math.abs(state1 - state2);

        return distance;
    }
}
