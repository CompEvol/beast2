/*
 * Copyright (C) 2012 Tim Vaughan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package beast.evolution.tree;


import beast.util.CredibleSet;
import beast.util.FrequencySet;
import beast.util.NexusParser;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


// TODO: Calculate mean node heights for trees in credible set.

/**
 * Partial re-implementation of TreeTraceAnalysis from BEAST 1.
 *
 * Represents an analysis of a list of trees obtained either directly
 * from a logger or from a trace file.  Currently only the 95% credible
 * set of tree topologies is calculated.
 *
 * @author Tim Vaughan
 * @author Walter Xie
 */
public class TreeTraceAnalysis {

    public static final double DEFAULT_BURN_IN_FRACTION = 0.1;

    protected List<Tree> treeInCredSetList;
    protected int burnin, totalTrees;

    protected FrequencySet<String> topologiesFrequencySet; // include credSetProbability
    protected CredibleSet<String> credibleSet;

    public TreeTraceAnalysis() {   }

    public TreeTraceAnalysis(List<Tree> posteriorTreeList) {
        this(posteriorTreeList, DEFAULT_BURN_IN_FRACTION);
    }

    /**
     * need to run analyze(credSetProbability)
     *
     * @param posteriorTreeList
     * @param burninPercentage
     */
    public TreeTraceAnalysis(List<Tree> posteriorTreeList, double burninPercentage) {
        totalTrees = posteriorTreeList.size();
        burnin = getBurnIn(totalTrees, burninPercentage);

        // Remove burnin from trace:
        treeInCredSetList = getSubListOfTrees(posteriorTreeList, burnin);
    }

    public TreeTraceAnalysis(List<Tree> posteriorTreeList, double burninPercentage, double credSetProbability) {
        this(posteriorTreeList, burninPercentage);
        analyze(credSetProbability);
    }

    /**
     * Analyse tree topologies, and set credSetProbability
     */
    public void analyze(double credSetProbability) {
        // set credSetProbability
        topologiesFrequencySet = new FrequencySet<String>(credSetProbability);

        for (Tree tree : treeInCredSetList) {
            String topology = uniqueNewick(tree.getRoot());
            topologiesFrequencySet.add(topology, 1);
        }

        credibleSet = topologiesFrequencySet.getCredibleSet();
    }

    public static int getBurnIn(int total, double burninFraction) {
        // Record original list length and burnin for report:
        int burnin = (int)(total * burninFraction);
        assert burnin < total;
        return burnin;
    }

    /**
     * used to remove burn in
     * @param rawTreeList
     * @param start
     * @param end
     * @return
     */
    public static List<Tree> getSubListOfTrees(List<Tree> rawTreeList, int start, int end) {
        assert start < end;
        return new ArrayList<Tree>(rawTreeList.subList(start, end));
    }

    public static List<Tree> getSubListOfTrees(List<Tree> rawTreeList, int start) {
        return getSubListOfTrees(rawTreeList, start, rawTreeList.size());
    }

    public void reportShort(PrintStream oStream) {
        oStream.println("burnin = " + String.valueOf(burnin));
        oStream.println("total trees used (total - burnin) = "
                + String.valueOf(treeInCredSetList.size()));
    }

    /**
     * Generate report summarising analysis.
     *
     * @param oStream Print stream to write output to.
     */
    public void report(PrintStream oStream) {
        reportShort(oStream);

        oStream.print("\n" + String.valueOf(topologiesFrequencySet.getCredSetProbability()*100)
                + "% credible set");

        oStream.println(" (" + String.valueOf(credibleSet.credibleSetList.size())
                + " unique tree topologies, "
                + String.valueOf(credibleSet.sumFrequency)
                + " trees in total)");

        oStream.println("Count\tPercent\tRunning\tTree");
        double runningPercent = 0;
        for (int i=0; i<credibleSet.credibleSetList.size(); i++) {
            double percent = 100.0*credibleSet.getFrequency(i, topologiesFrequencySet)/(totalTrees-burnin);
            runningPercent += percent;

            oStream.print(credibleSet.getFrequency(i, topologiesFrequencySet) + "\t");
            oStream.format("%.2f%%\t", percent);
            oStream.format("%.2f%%\t", runningPercent);
            oStream.println(credibleSet.credibleSetList.get(i));
        }
    }


    /**
     * Recursive function for constructing a Newick tree representation
     * in the given buffer.
     *
     * @param node
     * @return
     */
    protected String uniqueNewick(Node node) {
        if (node.isLeaf()) {
            return String.valueOf(node.getNr());
        } else {
            StringBuilder builder = new StringBuilder("(");

            List<String> subTrees = new ArrayList<String>();
            for (int i=0; i<node.getChildCount(); i++) {
                subTrees.add(uniqueNewick(node.getChild(i)));
            }

            Collections.sort(subTrees);

            for (int i=0; i<subTrees.size(); i++) {
                builder.append(subTrees.get(i));
                if (i<subTrees.size()-1) {
                    builder.append(",");
                }
            }
            builder.append(")");

            return builder.toString();
        }
    }

    /**
     * Obtain credible set of tree topologies
     *
     * @return List of tree topologies as Newick-formatted strings.
     */
    public List<String> getCredibleSetList() {
        return credibleSet.credibleSetList;
    }

    /**
     * Obtain frequencies with which members of the credible set appeared
     * in the original tree list.
     *
     * @return  List of absolute topology frequencies.
     */
//    public List<Integer> getCredibleSetFreqs() {
//        return credibleSetFreqs;
//    }

    /**
     * Obtain total number of trees analysed (excluding burnin).
     *
     * @return Number of trees analysed.
     */
    public int getTotalTreesUsed() {
        return treeInCredSetList.size();
    }

    public Map<String,Integer> getTopologyCounts() {
        return topologiesFrequencySet.getFrequencyMap();
    }

    public static void main(String[] args) {
        try {
            NexusParser parser = new NexusParser();
            parser.parseFile(new File(args[0]));
            TreeTraceAnalysis analysis = new TreeTraceAnalysis(parser.trees);
            analysis.analyze(0.95);
            analysis.report(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}