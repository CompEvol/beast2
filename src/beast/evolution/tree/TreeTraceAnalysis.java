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

    protected int nTrees; // total from original log

    protected FrequencySet<String> topologiesFrequencySet = new FrequencySet<>();
    protected CredibleSet<String> credibleSet;

    protected boolean displayTaxonLabels = true; // false to display node index instead

    public TreeTraceAnalysis() { };

    public TreeTraceAnalysis(List<Tree> posteriorTreeList, double burninFraction) {
        this();

        addAllTrees(posteriorTreeList, burninFraction);
    }

    public TreeTraceAnalysis(List<Tree> posteriorTreeList) {
        this(posteriorTreeList, 0.1);
    }

    public void addTree(Tree tree) {
        analyzeTree(tree);
        nTrees += 1;
    }

    public void addAllTrees(List<Tree> trees, double burninFraction) {
        int burnin = (int)Math.round(trees.size()*burninFraction);

        for (int i=burnin; i<trees.size(); i++)
            addTree(trees.get(i));
    }

    public void addAllTrees(List<Tree> trees) {
        addAllTrees(trees, 0);
    }

    /**
     * Overridable hook which is called whenever a tree is added to the analysis.
     * When overriding, call super(tree) to ensure parent analyses are still computed.
     *
     * @param tree tree being added
     */
    public void analyzeTree(Tree tree) {
        String topology = uniqueNewick(tree);
        topologiesFrequencySet.add(topology, 1);
    }

    /**
     * Analyse tree topologies using default credibility threshold (0.95) and no target tree
     */
    public void computeCredibleSet() {
        computeCredibleSet(FrequencySet.DEFAULT_CRED_SET, null);
    }

    /**
     * Compute credible set with no target tree.
     *
     * @param credSetProbability credibility threshold
     */
    public void computeCredibleSet(double credSetProbability) {
        computeCredibleSet(credSetProbability, null);
    }

    /**
     * Analyse tree topologies, and set credSetProbability
     *
     * @param credSetProbability credibility threshold
     * @param targetTree target tree (null implies no target tree)
     */
    public void computeCredibleSet(double credSetProbability, Tree targetTree) {
        topologiesFrequencySet.setCredSetProbability(credSetProbability);

        if (targetTree != null)
            credibleSet = topologiesFrequencySet.getCredibleSet(uniqueNewick(targetTree));
        else
            credibleSet = topologiesFrequencySet.getCredibleSet();
    }

    /**
     * Produce analysis report.
     *
     * @param ps Print stream to write output to.
     * @param verbose if true then print all trees
     */
    public void report(PrintStream ps, boolean verbose) {
        // prefix non-tabular lines with # so file can be read into R
        ps.println("# total number of trees used = " + String.valueOf(nTrees));

        // prefix non-tabular lines with # so file can be read into R
        ps.print("# \n# " + String.valueOf(topologiesFrequencySet.getCredSetProbability() * 100)
                + "% credible set");

        ps.println(" (" + String.valueOf(credibleSet.credibleSetList.size())
                + " unique tree topologies, "
                + String.valueOf(credibleSet.sumFrequency)
                + " trees in total)");

        if (verbose) {
            ps.println("Rank\tCount\tPercent\tRunning\tTree");
            double runningPercent = 0;
            for (int i = 0; i < credibleSet.credibleSetList.size(); i++) {
                double percent = 100.0 * credibleSet.getFrequency(i, topologiesFrequencySet) / nTrees;
                runningPercent += percent;

                ps.print((i + 1) + "\t");
                ps.print(credibleSet.getFrequency(i, topologiesFrequencySet) + "\t");
                ps.format("%.2f%%\t", percent);
                ps.format("%.2f%%\t", runningPercent);
                ps.println(credibleSet.credibleSetList.get(i));
            }
        }
    }


    /**
     * Produce analysis report.
     *
     * @param ps Print stream to write output to.
     */
    public void report(PrintStream ps) {
        report(ps, true);
    }

    /**
     * Method called to produce Newick string from given tree which uniquely represents the
     * trees topology.  I.e. two of these strings are equal if and only if the corresponding
     * trees have the same topology.
     *
     * @param tree tree to convert to Newick representation.
     * @return string uniquely representing topology of tree.
     */
    public String uniqueNewick(Tree tree) {
        return TreeUtils.sortedNewickTopology(tree.getRoot(), displayTaxonLabels);
    }

    public Map<String, Integer> getTopologyCounts() {
        return topologiesFrequencySet.getFrequencyMap();
    }

    public int getNTrees() {
        return nTrees;
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

        TreeTraceAnalysis analysis = new TreeTraceAnalysis(trees, 0.1);
        analysis.computeCredibleSet(0.95, null);
        analysis.report(out);
    }

}