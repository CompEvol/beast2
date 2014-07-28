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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


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

    protected List<Tree> treeInCredSetList;
    protected int burnin, totalTrees;

    protected FrequencySet<String> topologiesFrequencySet;
    protected CredibleSet<String> credibleSet;

    protected boolean taxaLabel = true; // false to display node index instead

    public TreeTraceAnalysis(List<Tree> posteriorTreeList) {
        this(posteriorTreeList, DEFAULT_BURN_IN_FRACTION);
    }

    /**
     * default credible set probability threshold 95%
     *
     * @param posteriorTreeList
     * @param burninFraction
     */
    public TreeTraceAnalysis(List<Tree> posteriorTreeList, double burninFraction) {
        removeBurnin(posteriorTreeList, burninFraction);
        analyze();
    }

    /**
     *
     *
     * @param posteriorTreeList
     * @param burninFraction        such as 0.1 not 10
     * @param credSetProbability
     */
    public TreeTraceAnalysis(List<Tree> posteriorTreeList, double burninFraction, double credSetProbability) {
        removeBurnin(posteriorTreeList, burninFraction);
        analyze(credSetProbability);
    }

    /**
     * Analyse tree topologies, and set credSetProbability 95%
     */
    public void analyze() {
        // 0.95
        topologiesFrequencySet = new FrequencySet<String>();

        analyze(topologiesFrequencySet);
    }

    /**
     * Analyse tree topologies, and set credSetProbability
     * @param credSetProbability
     */
    public void analyze(double credSetProbability) {
        // set credSetProbability
        topologiesFrequencySet = new FrequencySet<String>(credSetProbability);

        analyze(topologiesFrequencySet);
    }

    /**
     * report number of unique tree topologies and total trees in the credible set
     *
     * @param oStream Print stream to write output to.
     * @param shortReport
     */
    public void report(PrintStream oStream, boolean shortReport) {
        // prefix non-tabular lines with # so file can be read into R
        oStream.println("# burnin = " + String.valueOf(burnin));
        oStream.println("# total trees used (total - burnin) = "
                + String.valueOf(treeInCredSetList.size()));

        // prefix non-tabular lines with # so file can be read into R
        oStream.print("# \n# " + String.valueOf(topologiesFrequencySet.getCredSetProbability() * 100)
                + "% credible set");

        oStream.println(" (" + String.valueOf(credibleSet.credibleSetList.size())
                + " unique tree topologies, "
                + String.valueOf(credibleSet.sumFrequency)
                + " trees in total)");

        if (!shortReport) {
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
        report(oStream, false);
    }


    public int getTreeCount() {
        return treeInCredSetList.size();
    }


    // Remove burnin
    protected void removeBurnin(List<Tree> posteriorTreeList, double burninFraction) {
        totalTrees = posteriorTreeList.size();
        burnin = Utils.getBurnIn(totalTrees, burninFraction);

        // Remove burnin from trace:
        treeInCredSetList = Utils.getSubListOfTrees(posteriorTreeList, burnin);
    }

    // topologiesFrequencySet = new FrequencySet<String>(double credSetProbability);
    protected void analyze(FrequencySet<String> topologiesFrequencySet) {

        for (Tree tree : treeInCredSetList) {
            String topology = uniqueNewick(tree.getRoot());
            topologiesFrequencySet.add(topology, 1);
        }

        credibleSet = topologiesFrequencySet.getCredibleSet();
    }

    /**
     * Recursive function for constructing a Newick tree representation
     * in the given buffer.
     *
     * @param node
     * @return
     */
    public String uniqueNewick(Node node) {
        if (node.isLeaf()) {
            if (taxaLabel) {
                return String.valueOf(node.getID());
            } else {
                return String.valueOf(node.getNr());
            }
        } else {
            StringBuilder builder = new StringBuilder("(");

            List<String> subTrees = new ArrayList<String>();
            for (int i = 0; i < node.getChildCount(); i++) {
                subTrees.add(uniqueNewick(node.getChild(i)));
            }

            Collections.sort(subTrees);

            for (int i = 0; i < subTrees.size(); i++) {
                builder.append(subTrees.get(i));
                if (i < subTrees.size() - 1) {
                    builder.append(",");
                }
            }
            builder.append(")");

            return builder.toString();
        }
    }

    public boolean isTaxaLabel() {
        return taxaLabel;
    }

    public void setTaxaLabel(boolean taxaLabel) {
        this.taxaLabel = taxaLabel;
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
     * @return List of absolute topology frequencies.
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

    public Map<String, Integer> getTopologyCounts() {
        return topologiesFrequencySet.getFrequencyMap();
    }

    public static class Utils {
        /**
         * get list of trees from file
         * @param treeFile
         * @return
         * @throws Exception
         */
        public static List<Tree> getTrees (File treeFile) throws Exception {
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
            return new ArrayList<Tree>(rawTreeList.subList(start, end));
        }

        public static List<Tree> getSubListOfTrees(List<Tree> rawTreeList, int start) {
            return getSubListOfTrees(rawTreeList, start, rawTreeList.size());
        }
    }

    public static void main(String[] args) {
        PrintStream out = System.out;
        File inputFile = null;

        if (args.length > 0) {
            System.out.println("Input file  = " + args[0]);
            inputFile = new File(args[0]);
        } else {
            System.out.println("Error: Expected nexus file, but not file name was provided.");
            System.exit(0);
        }

        if (args.length > 1) {
            System.out.println("Output file = " + args[1]);
            try {
                out = new PrintStream(new FileOutputStream(args[1]));
            } catch (FileNotFoundException e) {
                System.out.println("Error: Unable to create output file.");
                System.exit(0);
            }
        }

        NexusParser parser = new NexusParser();
        try {
            parser.parseFile(inputFile);
        } catch (Exception e) {
            System.out.println("Error occurred while parsing input file.");
            System.exit(0);
        }
        TreeTraceAnalysis analysis = new TreeTraceAnalysis(parser.trees); // default 0.1, 0.95
        analysis.report(out);
    }
}