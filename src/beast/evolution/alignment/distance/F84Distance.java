/*
 * F84DistanceMatrix.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
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

package beast.evolution.alignment.distance;

import beast.core.Description;
import beast.evolution.alignment.Alignment;
import beast.evolution.substitutionmodel.Frequencies;


/**
 * compute HKY corrected distance matrix
 *
 * @author Andrew Rambaut
 * @version $Id: F84DistanceMatrix.java,v 1.3 2005/05/24 20:25:56 rambaut Exp $
 */
@Description("compute HKY corrected distance")
public class F84Distance extends Distance.Base {

    /**
     * set the pattern source
     */
    @Override
    public void setPatterns(Alignment patterns) {
        super.setPatterns(patterns);

        Frequencies frequencies = new Frequencies();
        try {
            frequencies.initByName("data", patterns, "estimate", true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        double[] freqs = frequencies.getFreqs();
        stateCount = dataType.getStateCount();
        if (stateCount != 4) {
            throw new IllegalArgumentException("F84DistanceMatrix must have nucleotide patterns");
        }

        double freqA = freqs[0];
        double freqC = freqs[1];
        double freqG = freqs[2];
        double freqT = freqs[3];

        double freqR = freqA + freqG;
        double freqY = freqC + freqT;

        constA = ((freqA * freqG) / freqR) + ((freqC * freqT) / freqY);
        constB = (freqA * freqG) + (freqC * freqT);
        constC = (freqR * freqY);
    }

    /**
     * Calculate a pairwise distance
     */
    @Override
    public double pairwiseDistance(int taxon1, int taxon2) {
        int state1, state2;

        int n = patterns.getPatternCount();
        double weight, distance;
        double sumTs = 0.0;
        double sumTv = 0.0;
        double sumWeight = 0.0;

        int[] pattern;

        for (int i = 0; i < n; i++) {
            pattern = patterns.getPattern(i);

            state1 = pattern[taxon1];
            state2 = pattern[taxon2];

            weight = patterns.getPatternWeight(i);
            if (!dataType.isAmbiguousCode(state1) && !dataType.isAmbiguousCode(state2) && state1 != state2) {

                if ((state1 == 0 && state2 == 2) || (state1 == 2 && state2 == 0)) {
                    // it's a transition
                    sumTs += weight;
                } else {
                    // it's a transversion
                    sumTv += weight;
                }
            }
            sumWeight += weight;
        }

        double P = sumTs / sumWeight;
        double Q = sumTv / sumWeight;

        double tmp1 = Math.log(1.0 - (P / (2.0 * constA)) -
                (((constA - constB) * Q) / (2.0 * constA * constC)));

        double tmp2 = Math.log(1.0 - (Q / (2.0 * constC)));

        distance = -(2.0 * constA * tmp1) +
                (2.0 * (constA - constB - constC) * tmp2);

        if (distance < MAX_DISTANCE) {
            return distance;
        } else {
            return MAX_DISTANCE;
        }
    }

    //
    // Private stuff
    //

    private int stateCount;

    //used in correction formula
    private double constA, constB, constC;

}
