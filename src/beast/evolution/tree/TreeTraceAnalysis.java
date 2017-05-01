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


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import beast.util.CredibleSet;
import beast.util.FrequencySet;
import beast.util.NexusParser;


// TODO: Calculate mean node heights for trees in credible set.

/**
 * Partial re-implementation of TreeTraceAnalysis from BEAST 1.
 * <p/>
 * Represents an analysis of a list of trees obtained either directly
 * from a logger or from a trace file. The set of tree topologies is
 * calculated by given credible set probability threshold (default 95%).
 *
 *
 * @author Walter Xie
 * @author Alexei Drummond
 * @author Tim Vaughan
 */
public class TreeTraceAnalysis {

    public static final double DEFAULT_BURN_IN_FRACTION = 0.1;

    protected List<Tree> treeInTrace;

    protected int totalTrees; // total from original log
    protected int burnin;

    protected FrequencySet<String> topologiesFrequencySet;
    protected CredibleSet<String> credibleSet;

    protected boolean isTaxaLabel = true; // false to display node index instead

    public TreeTraceAnalysis(List<Tree> posteriorTreeList) {
        this(posteriorTreeList, DEFAULT_BURN_IN_FRACTION);
    }

    /**
     * default credible set probability threshold 95%
     * analyze() needs after create TreeTraceAnalysis
     * @param posteriorTreeList
     * @param burninFraction
     */
    public TreeTraceAnalysis(List<Tree> posteriorTreeList, double burninFraction) {
        assert posteriorTreeList != null;
        removeBurnin(posteriorTreeList, burninFraction);
    }

    /**
     * Analyse tree topologies, and set credSetProbability 95%
     */
    public void analyze() {
        // 0.95
        analyze(FrequencySet.DEFAULT_CRED_SET);
    }

    /**
     * Analyse tree topologies, and set credSetProbability
     * @param credSetProbability
     */
    public void analyze(double credSetProbability) {
        // set credSetProbability
        topologiesFrequencySet = new FrequencySet<>();
        topologiesFrequencySet.setCredSetProbability(credSetProbability);

        for (Tree tree : treeInTrace) {
            String topology = uniqueNewick(tree.getRoot());
            topologiesFrequencySet.add(topology, 1);
        }

        credibleSet = topologiesFrequencySet.getCredibleSet();
    }

    /**
     * report number of unique tree topologies and total trees in the credible set
     *
     * @param oStream Print stream to write output to.
     * @param verbose if true then print all trees
     */
    public void report(PrintStream oStream, boolean verbose) {
        // prefix non-tabular lines with # so file can be read into R
        oStream.println("# burnin = " + String.valueOf(burnin));
        oStream.println("# total trees used (total - burnin) = "
                + String.valueOf(treeInTrace.size()));

        // prefix non-tabular lines with # so file can be read into R
        oStream.print("# \n# " + String.valueOf(topologiesFrequencySet.getCredSetProbability() * 100)
                + "% credible set");

        oStream.println(" (" + String.valueOf(credibleSet.credibleSetList.size())
                + " unique tree topologies, "
                + String.valueOf(credibleSet.sumFrequency)
                + " trees in total)");

        if (verbose) {
            oStream.println("Rank\tCount\tPercent\tRunning\tTree");
            double runningPercent = 0;
            for (int i = 0; i < credibleSet.credibleSetList.size(); i++) {
                double percent = 100.0 * credibleSet.getFrequency(i, topologiesFrequencySet) / (totalTrees - burnin);
                runningPercent += percent;

                oStream.print((i + 1) + "\t");
                oStream.print(credibleSet.getFrequency(i, topologiesFrequencySet) + "\t");
                oStream.format("%.2f%%\t", percent);
                oStream.format("%.2f%%\t", runningPercent);
                oStream.println(credibleSet.credibleSetList.get(i));
            }
        }
    }


    public void report(PrintStream oStream) {
        report(oStream, true);
    }

    /**
     * Recursive function for constructing a Newick tree representation
     * in the given buffer. This string representation defines the
     * topology of a tree for the purposes of determining samples that
     * have identical topologies.
     *
     * @param node
     * @return
     */
    public String uniqueNewick(Node node) {
        return TreeUtils.sortedNewickTopology(node, isTaxaLabel);
    }

    public boolean isTaxaLabel() {
        return isTaxaLabel;
    }

    public void setTaxaLabel(boolean taxaLabel) {
        this.isTaxaLabel = taxaLabel;
    }

    public int getBurnin() {
        return burnin;
    }

    public double getBurninFraction() {
        return (double) burnin / (double) totalTrees;
    }

    public double getCredSetProbability() {
        return topologiesFrequencySet.getCredSetProbability();
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
     * total from original log
     *
     * @return  Number of trees from log
     */
    public int getTotalTreesInLog() {
        return totalTrees;
    }

    /**
     * Obtain total number of trees analysed after burnin removed.
     *
     * @return Number of trees analysed.
     */
    public int getTotalTreesBurninRemoved() {
        return treeInTrace.size();
    }

    public Map<String, Integer> getTopologyCounts() {
        return topologiesFrequencySet.getFrequencyMap();
    }

    /**
     * Obtain frequencies with which members of the credible set appeared
     * in the original tree list.
     *
     * @return List of absolute topology frequencies.
     */
//    public List<Integer> getCredibleSetFreqs() {
//        return credibleSetFreqs;
//    }

    /**
     * static Utils
     */
    public static class Utils {
        /**
         * get list of trees from file
         * @param treeFile
         * @return
         * @throws IOException 
         */
        public static List<Tree> getTrees (File treeFile) throws IOException {
            NexusParser parser = new NexusParser();
            parser.parseFile(treeFile);
            return parser.trees;
        }

        /**
         * get burn in from total and burninFraction
         * @param total
         * @param burninFraction
         * @return
         */
        public static int getBurnIn(int total, double burninFraction) {
            // Record original list length and burnin for report:
            int burnin = (int) (total * burninFraction);
            assert burnin < total;
            return burnin;
        }

        /**
         * get a subset of trees from total trees in a range.
         * it can be used to
         *
         * @param rawTreeList
         * @param start
         * @param end
         * @return
         */
        public static List<Tree> getSubListOfTrees(List<Tree> rawTreeList, int start, int end) {
            assert start < end;
            return new ArrayList<>(rawTreeList.subList(start, end));
        }

        public static List<Tree> getSubListOfTrees(List<Tree> rawTreeList, int start) {
            return getSubListOfTrees(rawTreeList, start, rawTreeList.size());
        }
    }

    //******** protected *****

    // Remove burnin
    protected void removeBurnin(List<Tree> posteriorTreeList, double burninFraction) {
        totalTrees = posteriorTreeList.size();
        burnin = Utils.getBurnIn(totalTrees, burninFraction);

        // Remove burnin from trace:
        treeInTrace = Utils.getSubListOfTrees(posteriorTreeList, burnin);
    }

    //******** main *****
    public static void main(String[] args) {
        PrintStream out = System.out;
        File inputFile = null;

        if (args.length > 0) {
            System.out.println("Input file  = " + args[0]);
            inputFile = new File(args[0]);
        } else {
            System.out.println("Error: Expected nexus file, but not file name was provided.");
            System.exit(1);
        }

        if (args.length > 1) {
            System.out.println("Output file = " + args[1]);
            try {
                out = new PrintStream(new FileOutputStream(args[1]));
            } catch (FileNotFoundException e) {
                System.out.println("Error: Unable to create output file.");
                System.exit(1);
            }
        }

        List<Tree> trees = null;
        try {
            trees = TreeTraceAnalysis.Utils.getTrees(inputFile);
        } catch (Exception e) {
            System.out.println("Error occurred while parsing input file.");
            System.exit(1);
        }
        TreeTraceAnalysis analysis = new TreeTraceAnalysis(trees); // default 0.1, 0.95
        analysis.analyze();
        analysis.report(out);
    }

}