package beast.util;

import static beast.util.AddOnManager.beastVersion;

/*
 * File PackageDependency.java
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
/*
 * Parts copied from WEKA ClassDiscovery.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

import beast.core.Description;

/**
 * modified by Walter Xie
 */
@Description("BEAUti beastObject dependency class")
public class PackageDependency implements Comparable<Object> {
    String packageName;
    String dependson;
    Double atLeast;
    Double atMost;

    public void setAtLest(String sAtLeast) {
        if (sAtLeast == null || sAtLeast.length() == 0) {
            atLeast = 0.0;
        } else {
            atLeast = beastVersion.parseVersion(sAtLeast);
        }
    }

    public void setAtMost(String sAtMost) {
        if (sAtMost == null || sAtMost.length() == 0) {
            atMost = Double.POSITIVE_INFINITY;
        } else {
            atMost = beastVersion.parseVersion(sAtMost);
        }
    }

    @Override
    public int compareTo(Object o) {
        return dependson.toUpperCase().compareTo(o.toString().toUpperCase());
    }

    @Override
	public String toString() {
        return dependson;
    }

}
