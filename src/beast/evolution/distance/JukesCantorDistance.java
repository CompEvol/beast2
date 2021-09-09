/*
 * JukesCantorDistanceMatrix.java
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

package beast.evolution.distance;

import beast.base.Description;
import beast.evolution.alignment.Alignment;

/**
 * @author Andrew Rambaut
 * @author Korbinian Strimmer
 * @version $Id: JukesCantorDistanceMatrix.java,v 1.4 2005/05/24 20:25:56 rambaut Exp $
 */
@Description("compute jukes-cantor corrected distance")
public class JukesCantorDistance extends Distance.Base {


    /**
     * set the pattern source
     */
    @Override
    public void setPatterns(Alignment patterns) {
        super.setPatterns(patterns);

        final int stateCount = dataType.getStateCount();

        const1 = ((double) stateCount - 1) / stateCount;
        const2 = ((double) stateCount) / (stateCount - 1);
    }

    /**
     * Calculate a pairwise distance
     */
    @Override
    public double pairwiseDistance(int i, int j) {
        final double obsDist = super.pairwiseDistance(i, j);

        if (obsDist == 0.0) return 0.0;

        if (obsDist >= const1) {
            return MAX_DISTANCE;
        }

        final double expDist = -const1 * Math.log(1.0 - (const2 * obsDist));

        if (expDist < MAX_DISTANCE) {
            return expDist;
        } else {
            return MAX_DISTANCE;
        }
    }

    //
    // Private stuff
    //

    //used in correction formula
    private double const1, const2;
}
