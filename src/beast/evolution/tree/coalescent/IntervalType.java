package beast.evolution.tree.coalescent;

/*
 * IntervalType.java
 *
 * Copyright (C) 2002-2009 Alexei Drummond and Andrew Rambaut
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
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

/**
 * Specifies the interval types.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: IntervalType.java,v 1.9 2005/05/24 20:25:56 rambaut Exp $
 */
public enum IntervalType {

    /**
     * Denotes an interval at the end of which a new sample addition is
     * observed (i.e. the number of lineages is larger in the next interval).
     */
    SAMPLE("sample"),

    /**
     * Denotes an interval after which a coalescent event is observed
     * (i.e. the number of lineages is smaller in the next interval)
     */
    COALESCENT("coalescent"),

    /**
     * Denotes an interval at the end of which a migration event occurs.
     * This means that the colour of one lineage changes.
     */
    MIGRATION("migration"),

    /**
     * Denotes an interval at the end of which nothing is
     * observed (i.e. the number of lineages is the same in the next interval).
     */
    NOTHING("nothing");

    /**
     * private constructor.
     *
     * @param name the name of the interval type
     */
    private IntervalType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    private final String name;
}