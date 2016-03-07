package beast.util;


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
public class PackageDependency {
    public final String dependencyName;
    public final PackageVersion atLeast, atMost;

    public PackageDependency(String dependencyName,
                             PackageVersion minimumVersion,
                             PackageVersion maximumVersion) {
    	if (dependencyName.equals("beast2")) {
    		dependencyName = AddOnManager.BEAST_PACKAGE_NAME;
    	}
        this.dependencyName = dependencyName;
        
        atLeast = minimumVersion;
        atMost = maximumVersion;
    }

    /**
     * Test to see whether given version of package satisfies
     * version range of this package dependency.
     *
     * @param version version of package to check
     * @return true iff version meets criterion
     */
    public boolean isMetBy(PackageVersion version) {
        return (atLeast == null || version.compareTo(atLeast)>=0)
                && (atMost == null || version.compareTo(atMost)<=0);
    }

    public String getRangeString() {
        if (atLeast != null && atMost != null)
            return "versions " + atLeast + " to " + atMost;

        if (atLeast != null)
            return "version " + atLeast + " or greater";

        return "version " + atMost + " or lesser";
    }

    @Override
	public String toString() {
        return dependencyName + " " + getRangeString();
    }
}
