package beast.evolution.tree.coalescent;

import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.HeapSort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * TreeIntervals.java
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

/**
 * Extracts the intervals from a beast.tree.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: TreeIntervals.java,v 1.9 2005/05/24 20:25:56 rambaut Exp $
 */
public class TreeIntervals implements IntervalList {

    /**
     * Parameterless constructor.
     */
    public TreeIntervals() {
    }


    public TreeIntervals(Tree tree) {
        setTree(tree);
    }


    /**
     * @param tree the beast.tree for which intervals are obtained
     */
    public void setTree(Tree tree) {
        this.tree = tree;
        intervalsKnown = false;
    }

    /**
     * Specifies that the intervals are unknown (i.e., the beast.tree has changed).
     */
    public void setIntervalsUnknown() {
        intervalsKnown = false;
    }

    /**
     * Sets the limit for which adjacent events are merged.
     *
     * @param multifurcationLimit A value of 0 means merge addition of leafs (terminal nodes) when possible but
     *                            return each coalescense as a separate event.
     */
    public void setMultifurcationLimit(double multifurcationLimit) {
        this.multifurcationLimit = multifurcationLimit;
        intervalsKnown = false;
    }

    public int getSampleCount() {
        // Assumes a binary tree!
        return (tree.getNodeCount() - 1) / 2;
    }

    /**
     * get number of intervals
     */
    public int getIntervalCount() {
        if (!intervalsKnown) {
            calculateIntervals();
        }
        return intervalCount;
    }

    /**
     * Gets an interval.
     */
    public double getInterval(int i) {
        if (!intervalsKnown) {
            calculateIntervals();
        }
        if (i < 0 || i >= intervalCount) throw new IllegalArgumentException();
        return intervals[i];
    }

    /**
     * Defensive implementation creates copy
     *
     * @return
     */
    public double[] getIntervals(double[] inters) {
        if (!intervalsKnown) {
            calculateIntervals();
        }
        if (inters == null) inters = new double[intervals.length];
        System.arraycopy(intervals, 0, inters, 0, intervals.length);
        return inters;
    }

    public double[] getCoalescentTimes(double[] coalescentTimes) {

        if (coalescentTimes == null) coalescentTimes = new double[getSampleCount()];

        double time = 0;
        int coalescentIndex = 0;
        for (int i = 0; i < intervals.length; i++) {
            time += intervals[i];
            for (int j = 0; j < getCoalescentEvents(i); j++) {
                coalescentTimes[coalescentIndex] = time;
                coalescentIndex += 1;
            }
        }
        return coalescentTimes;
    }

    /**
     * Returns the number of uncoalesced lineages within this interval.
     * Required for s-coalescents, where new lineages are added as
     * earlier samples are come across.
     */
    public int getLineageCount(int i) {
        if (!intervalsKnown) {
            calculateIntervals();
        }
        if (i >= intervalCount) throw new IllegalArgumentException();
        return lineageCounts[i];
    }

    /**
     * @param interval the index of the interval
     * @return a list of the nodes representing the lineages in the ith interval.
     */
    public final List<Node> getLineages(int interval) {

        if (lineages[interval] == null) {

            List<Node> lines = new ArrayList<Node>();
            for (int i = 0; i <= interval; i++) {
                if (lineagesAdded[i] != null) lines.addAll(lineagesAdded[i]);
                if (lineagesRemoved[i] != null) lines.removeAll(lineagesRemoved[i]);
            }
            lineages[interval] = Collections.unmodifiableList(lines);

        }
        return lineages[interval];
    }

    /**
     * Returns the number coalescent events in an interval
     */
    public int getCoalescentEvents(int i) {
        if (!intervalsKnown) {
            calculateIntervals();
        }
        if (i >= intervalCount) throw new IllegalArgumentException();
        if (i < intervalCount - 1) {
            return lineageCounts[i] - lineageCounts[i + 1];
        } else {
            return lineageCounts[i] - 1;
        }
    }

    /**
     * Returns the type of interval observed.
     */
    public IntervalType getIntervalType(int i) {
        if (!intervalsKnown) {
            calculateIntervals();
        }
        if (i >= intervalCount) throw new IllegalArgumentException();
        int numEvents = getCoalescentEvents(i);

        if (numEvents > 0) return IntervalType.COALESCENT;
        else if (numEvents < 0) return IntervalType.SAMPLE;
        else return IntervalType.NOTHING;
    }

    public Node getCoalescentNode(int interval) {
        if (getIntervalType(interval) == IntervalType.COALESCENT) {
            if (lineagesRemoved[interval] != null) {
                if (lineagesRemoved[interval].size() == 1) {
                    return lineagesRemoved[interval].get(0);
                } else throw new IllegalArgumentException("multiple lineages lost over this interval!");
            } else throw new IllegalArgumentException("Inconsistent: no intervals lost over this interval!");
        } else throw new IllegalArgumentException("Interval " + interval + " is not a coalescent interval.");
    }

    /**
     * get the total height of the genealogy represented by these
     * intervals.
     */
    public double getTotalDuration() {

        if (!intervalsKnown) {
            calculateIntervals();
        }
        double height = 0.0;
        for (int j = 0; j < intervalCount; j++) {
            height += intervals[j];
        }
        return height;
    }

    /**
     * Checks whether this set of coalescent intervals is fully resolved
     * (i.e. whether is has exactly one coalescent event in each
     * subsequent interval)
     */
    public boolean isBinaryCoalescent() {
        if (!intervalsKnown) {
            calculateIntervals();
        }
        for (int i = 0; i < intervalCount; i++) {
            if (getCoalescentEvents(i) > 0) {
                if (getCoalescentEvents(i) != 1) return false;
            }
        }

        return true;
    }

    /**
     * Checks whether this set of coalescent intervals coalescent only
     * (i.e. whether is has exactly one or more coalescent event in each
     * subsequent interval)
     */
    public boolean isCoalescentOnly() {
        if (!intervalsKnown) {
            calculateIntervals();
        }
        for (int i = 0; i < intervalCount; i++) {
            if (getCoalescentEvents(i) < 1) return false;
        }

        return true;
    }

    /**
     * Recalculates all the intervals for the given beast.tree.
     */
    private void calculateIntervals() {

        int nodeCount = tree.getNodeCount();

        double[] times = new double[nodeCount];
        int[] childCounts = new int[nodeCount];

        collectTimes(tree, times, childCounts);

        int[] indices = new int[nodeCount];

        HeapSort.sort(times, indices);

        if (intervals == null || intervals.length != nodeCount) {
            intervals = new double[nodeCount];
            lineageCounts = new int[nodeCount];
            lineagesAdded = new List[nodeCount];
            lineagesRemoved = new List[nodeCount];
            lineages = new List[nodeCount];
        }

        // start is the time of the first tip
        double start = times[indices[0]];
        int numLines = 0;
        int nodeNo = 0;
        intervalCount = 0;
        while (nodeNo < nodeCount) {

            int lineagesRemoved = 0;
            int lineagesAdded = 0;

            double finish = times[indices[nodeNo]];
            double next;

            do {
                final int childIndex = indices[nodeNo];
                final int childCount = childCounts[childIndex];
                // dont use nodeNo from here on in do loop
                nodeNo += 1;
                if (childCount == 0) {
                    addLineage(intervalCount, tree.getNode(childIndex));
                    lineagesAdded += 1;
                } else {
                    lineagesRemoved += (childCount - 1);

                    // record removed lineages
                    final Node parent = tree.getNode(childIndex);
                    //assert childCounts[indices[nodeNo]] == beast.tree.getChildCount(parent);
                    //for (int j = 0; j < lineagesRemoved + 1; j++) {
                    for (int j = 0; j < childCount; j++) {
                        Node child = j == 0 ? parent.m_left : parent.m_right;
                        removeLineage(intervalCount, child);
                    }

                    // record added lineages
                    addLineage(intervalCount, parent);
                    // no mix of removed lineages when 0 th
                    if (multifurcationLimit == 0.0) {
                        break;
                    }
                }

                if (nodeNo < nodeCount) {
                    next = times[indices[nodeNo]];
                } else break;
            } while (Math.abs(next - finish) <= multifurcationLimit);

            if (lineagesAdded > 0) {

                if (intervalCount > 0 || ((finish - start) > multifurcationLimit)) {
                    intervals[intervalCount] = finish - start;
                    lineageCounts[intervalCount] = numLines;
                    intervalCount += 1;
                }

                start = finish;
            }

            // add sample event
            numLines += lineagesAdded;

            if (lineagesRemoved > 0) {

                intervals[intervalCount] = finish - start;
                lineageCounts[intervalCount] = numLines;
                intervalCount += 1;
                start = finish;
            }
            // coalescent event
            numLines -= lineagesRemoved;
        }

        intervalsKnown = true;
    }

    private void addLineage(int interval, Node node) {
        if (lineagesAdded[interval] == null) lineagesAdded[interval] = new ArrayList<Node>();
        lineagesAdded[interval].add(node);
    }

    private void removeLineage(int interval, Node node) {
        if (lineagesRemoved[interval] == null) lineagesRemoved[interval] = new ArrayList<Node>();
        lineagesRemoved[interval].add(node);
    }

    /**
     * @return the delta parameter of Pybus et al (Node spread statistic)
     */
    public double getDelta() {

        return IntervalList.Utils.getDelta(this);
    }

    /**
     * extract coalescent times and tip information into array times from beast.tree.
     *
     * @param tree        the beast.tree
     * @param times       the times of the nodes in the beast.tree
     * @param childCounts the number of children of each node
     */
    private static void collectTimes(Tree tree, double[] times, int[] childCounts) {

        for (int i = 0; i < tree.getNodeCount(); i++) {
            Node node = tree.getNode(i);
            times[i] = node.getHeight();
            childCounts[i] = node.isLeaf() ? 0 : 2;
        }
    }

    /**
     * The beast.tree.
     */
    private Tree tree = null;

    /**
     * The widths of the intervals.
     */
    private double[] intervals;

    /**
     * The number of uncoalesced lineages within a particular interval.
     */
    private int[] lineageCounts;

    /**
     * The lineages in each interval (stored by node ref).
     */
    private List<Node>[] lineagesAdded;
    private List<Node>[] lineagesRemoved;
    private List<Node>[] lineages;

    private int intervalCount = 0;

    /**
     * are the intervals known?
     */
    private boolean intervalsKnown = false;

    private double multifurcationLimit = -1.0;
}