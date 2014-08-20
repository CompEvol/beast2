/*
 * TreeAnnotator.java
 *
 * Copyright (C) 2002-2010 Alexei Drummond and Andrew Rambaut
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

package beast.app.treeannotator;

import beast.app.BEASTVersion;
import beast.app.beauti.BeautiDoc;
import beast.app.tools.LogCombiner;
import beast.app.util.Arguments;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeUtils;
import beast.math.statistic.DiscreteStatistics;
import beast.util.CollectionUtils;
import beast.util.HeapSort;
import beast.util.TreeParser;
import jam.console.ConsoleApplication;

import javax.swing.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

//import org.rosuda.JRI.REXP;
//import org.rosuda.JRI.RVector;
//import org.rosuda.JRI.Rengine;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * 
 * TreeAnnotator ported from BEAST 1
 */
public class TreeAnnotator {

    private final static BEASTVersion version = new BEASTVersion();

    private final static boolean USE_R = false;

    private static boolean forceIntegerToDiscrete = false;

	Tree [] trees;

    enum Target {
        MAX_CLADE_CREDIBILITY("Maximum clade credibility tree"),
        MAX_SUM_CLADE_CREDIBILITY("Maximum sum of clade credibilities"),
        USER_TARGET_TREE("User target tree");

        String desc;

        Target(String s) {
            desc = s;
        }

        public String toString() {
            return desc;
        }
    }

    enum HeightsSummary {
        MEDIAN_HEIGHTS("Median heights"),
        MEAN_HEIGHTS("Mean heights"),
        KEEP_HEIGHTS("Keep target heights"),
        CA_HEIGHTS("Common Ancestor heights");

        String desc;

        HeightsSummary(String s) {
            desc = s;
        }

        public String toString() {
            return desc;
        }
    }


    // Messages to stderr, output to stdout
    private static PrintStream progressStream = System.err;

//    private final String location1Attribute = "longLat1";
//    private final String location2Attribute = "longLat2";
//    private final String locationOutputAttribute = "location";

    public TreeAnnotator() { }

    public TreeAnnotator(final int burninPercentage,
    					boolean bAllowSingleChild,
                         HeightsSummary heightsOption,
                         double posteriorLimit,
                         double hpd2D,
                         Target targetOption,
                         String targetTreeFileName,
                         String inputFileName,
                         String outputFileName
    ) throws Exception {

        this.posteriorLimit = posteriorLimit;
        this.hpd2D = hpd2D;

        attributeNames.add("height");
        attributeNames.add("length");

        CladeSystem cladeSystem = new CladeSystem();

        totalTrees = 10000;
        totalTreesUsed = 0;

        //progressStream.println("Reading trees (bar assumes 10,000 trees)...");
        progressStream.println("0              25             50             75            100");
        progressStream.println("|--------------|--------------|--------------|--------------|");

        int stepSize = Math.max(totalTrees / 60, 1);

        TreeSetParser parser = new TreeSetParser(burninPercentage, bAllowSingleChild);
        try {
        	Node [] roots = parser.parseFile(inputFileName);
        	trees = new Tree[roots.length];
        	int i = 0;
        	for (Node root : roots) {
        		trees[i++] = new Tree(root);
        	}
        	progressStream.println("Read " + trees.length + " trees from file" +
                    (burninPercentage > 0 ? " after ignoring first " + burninPercentage + "% trees." : "."));
        } catch (Exception e) {
        	e.printStackTrace();
            System.err.println("Error Parsing Input Tree: " + e.getMessage());
            return;
		}
        
        if (targetOption != Target.USER_TARGET_TREE) {
            try {
	            for (Tree tree : trees) {
	            	cladeSystem.add(tree, false);
	                totalTreesUsed++;
	            }
	            totalTrees = totalTreesUsed * 100 / (100-Math.max(burninPercentage, 0));
            } catch (Exception e) {
                System.err.println("Error Processing Input Tree: " + e.getMessage());
                return;
            }
            progressStream.println();
            progressStream.println();

            if (totalTrees < 1) {
                System.err.println("No trees");
                return;
            }
            if (totalTreesUsed <= 1) {
                if (burninPercentage > 0) {
                    System.err.println("No trees to use: burnin too high");
                    return;
                }
            }
            cladeSystem.calculateCladeCredibilities(totalTreesUsed);

            progressStream.println("Total trees have " + totalTrees + ", where " + totalTreesUsed + " are used.");
//            if (burninPercentage > 0) {
//                progressStream.println("Ignoring first " + burninPercentage + "% trees.");
//            }

            progressStream.println("Total unique clades: " + cladeSystem.getCladeMap().keySet().size());
            progressStream.println();
        }

        Tree targetTree = null;

        switch (targetOption) {
            case USER_TARGET_TREE: {
                if (targetTreeFileName != null) {
                    progressStream.println("Reading user specified target tree, " + targetTreeFileName);
                    
                    String sTree = BeautiDoc.load(targetTreeFileName);
                   
                    TreeParser parser2 = new TreeParser();
                    try {
                    	Node root = parser2.parseNewick(sTree);
                    	targetTree = new Tree(root);
                    } catch (Exception e) {
                        System.err.println("Error Parsing Target Tree: " + e.getMessage());
                        return;
                    }
                } else {
                    System.err.println("No user target tree specified.");
                    return;
                }
                break;
            }
            case MAX_CLADE_CREDIBILITY: {
                progressStream.println("Finding maximum credibility tree...");
                targetTree = summarizeTrees(cladeSystem, false).copy();
                break;
            }
            case MAX_SUM_CLADE_CREDIBILITY: {
                progressStream.println("Finding maximum sum clade credibility tree...");
                targetTree = summarizeTrees(cladeSystem, true).copy();
                break;
            }
        }

        progressStream.println("Collecting node information...");
        progressStream.println("0              25             50             75            100");
        progressStream.println("|--------------|--------------|--------------|--------------|");

        stepSize = Math.max(totalTrees / 60, 1);
        int reported = 0;

        // this call increments the clade counts and it shouldn't
        // this is remedied with removeClades call after while loop below
        cladeSystem = new CladeSystem(targetTree);
        totalTreesUsed = 0;
        try {
            setupAttributes(trees[0]);
            int counter = 0;
        	for (Tree tree: trees) {
                cladeSystem.collectAttributes(tree, attributeNames);
                if (counter > 0 && counter % stepSize == 0 && reported < 61) {
                    progressStream.print("*");
                    progressStream.flush();
                    reported++;
                }
                totalTreesUsed++;
                counter++;
        	}
        	
            cladeSystem.removeClades(targetTree.getRoot(), true);
            //progressStream.println("totalTreesUsed=" + totalTreesUsed);
            cladeSystem.calculateCladeCredibilities(totalTreesUsed);
        } catch (Exception e) {
            System.err.println("Error Parsing Input Tree: " + e.getMessage());
            return;
        }
        progressStream.println();
        progressStream.println();

        progressStream.println("Annotating target tree...");

        try {
            annotateTree(cladeSystem, targetTree.getRoot(), null, heightsOption);

            if( heightsOption == HeightsSummary.CA_HEIGHTS ) {
                setTreeHeightsByCA(targetTree);
            }
        } catch (Exception e) {
        	e.printStackTrace();
            System.err.println("Error to annotate tree: " + e.getMessage() + "\nPlease check the tree log file format.");
            return;
        }

        progressStream.println("Writing annotated tree....");

        
        processMetaData(targetTree.getRoot());
        try {
            final PrintStream stream = outputFileName != null ?
                    new PrintStream(new FileOutputStream(outputFileName)) :
                    System.out;
            targetTree.init(stream);
            stream.println();
            
            stream.print("tree TREE1 = ");
            int[] dummy = new int[1];
            String sNewick = targetTree.getRoot().toSortedNewick(dummy, true);
            stream.print(sNewick);
            stream.println(";");
//            stream.println(targetTree.getRoot().toShortNewick(false));
//            stream.println();
            targetTree.close(stream);
            stream.println();
        } catch (Exception e) {
            System.err.println("Error to write annotated tree file: " + e.getMessage());
            return;
        }

    }

    private void processMetaData(Node node) {
		for (Node child : node.getChildren()) {
			processMetaData(child);
		}
		Set<String> metaDataNames = node.getMetaDataNames(); 
		if (metaDataNames != null) {
			String metadata = "";
			for (String name : metaDataNames) {
				Object value = node.getMetaData(name);
				metadata += name + "=";
				if (value instanceof Object[]) {
					Object [] values = (Object[]) value;
					metadata += "{";
					for (int i = 0; i < values.length; i++) {
						metadata += values[i].toString();
						if (i < values.length - 1) {
							metadata += ",";
						}
					}
					metadata += "}";
				} else {
					 metadata += value.toString();
				}
				metadata += ",";
			}
			metadata = metadata.substring(0, metadata.length() - 1);
			node.metaDataString = metadata;
		}		
	}

	private void setupAttributes(Tree tree) {
        for (int i = 0; i < tree.getNodeCount(); i++) {
            Node node = tree.getNode(i);
            Set<String> iter = node.getMetaDataNames();
            if (iter != null) {
            	for (String name : iter) {
                    attributeNames.add(name);
                }
            }
        }

        for (TreeAnnotationPlugin plugin : plugins) {
            Set<String> claimed = plugin.setAttributeNames(attributeNames);
            attributeNames.removeAll(claimed);
        }
    }

    private Tree summarizeTrees(CladeSystem cladeSystem, boolean useSumCladeCredibility) throws IOException {

        Tree bestTree = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        progressStream.println("Analyzing " + totalTreesUsed + " trees...");
        progressStream.println("0              25             50             75            100");
        progressStream.println("|--------------|--------------|--------------|--------------|");

        int stepSize = Math.max(totalTrees / 60, 1);
        int reported = 0;

        int counter = 0;
        for (Tree tree : trees) {
            double score = scoreTree(tree, cladeSystem, useSumCladeCredibility);
          if (score > bestScore) {
              bestTree = tree;
              bestScore = score;
          }
          if (counter > 0 && counter % stepSize == 0 && reported < 61) {
              progressStream.print("*");
              progressStream.flush();
              reported++;
          }
          counter++;
        }
        progressStream.println();
        progressStream.println();
        if (useSumCladeCredibility) {
            progressStream.println("Highest Sum Clade Credibility: " + bestScore);
        } else {
            progressStream.println("Highest Log Clade Credibility: " + bestScore);
        }

        return bestTree;
    }

    public double scoreTree(Tree tree, CladeSystem cladeSystem, boolean useSumCladeCredibility) {
        if (useSumCladeCredibility) {
            return cladeSystem.getSumCladeCredibility(tree.getRoot(), null);
        } else {
            return cladeSystem.getLogCladeCredibility(tree.getRoot(), null);
        }
    }


    private void annotateTree(CladeSystem cladeSystem, Node node, BitSet bits, HeightsSummary heightsOption) {

        BitSet bits2 = new BitSet();

        if (node.isLeaf()) {

            int index = cladeSystem.getTaxonIndex(node);
            bits2.set(index);

            annotateNode(cladeSystem, node, bits2, true, heightsOption);
        } else {

            for (int i = 0; i < node.getChildCount(); i++) {

                Node node1 = node.getChild(i);

                annotateTree(cladeSystem, node1, bits2, heightsOption);
            }

            annotateNode(cladeSystem, node, bits2, false, heightsOption);
        }

        if (bits != null) {
            bits.or(bits2);
        }
    }

    private void annotateNode(CladeSystem cladeSystem, Node node, BitSet bits, boolean isTip, HeightsSummary heightsOption) {
        CladeSystem.Clade clade = cladeSystem.cladeMap.get(bits);
        assert clade != null : "Clade missing?";

        boolean filter = false;
        if (!isTip) {
            final double posterior = clade.getCredibility();
            node.setMetaData("posterior", posterior);
            if (posterior < posteriorLimit) {
                filter = true;
            }
        }

        int i = 0;
        for (String attributeName : attributeNames) {

            if (clade.attributeValues != null && clade.attributeValues.size() > 0) {
                double[] values = new double[clade.attributeValues.size()];

                HashMap<Object, Integer> hashMap = new HashMap<Object, Integer>();

                Object[] v = clade.attributeValues.get(0);
                if (v[i] != null) {

                    final boolean isHeight = attributeName.equals("height");
                    boolean isBoolean = v[i] instanceof Boolean;

                    boolean isDiscrete = v[i] instanceof String;

                    if (forceIntegerToDiscrete && v[i] instanceof Integer) isDiscrete = true;

                    double minValue = Double.MAX_VALUE;
                    double maxValue = -Double.MAX_VALUE;

                    final boolean isArray = v[i] instanceof Object[];
                    boolean isDoubleArray = isArray && ((Object[]) v[i])[0] instanceof Double;
                    // This is Java, friends - first value type does not imply all.
                    if (isDoubleArray) {
                        for (Object n : (Object[]) v[i]) {
                            if (!(n instanceof Double)) {
                                isDoubleArray = false;
                                break;
                            }
                        }
                    }
                    // todo Handle other types of arrays

                    double[][] valuesArray = null;
                    double[] minValueArray = null;
                    double[] maxValueArray = null;
                    int lenArray = 0;

                    if (isDoubleArray) {
                        lenArray = ((Object[]) v[i]).length;

                        valuesArray = new double[lenArray][clade.attributeValues.size()];
                        minValueArray = new double[lenArray];
                        maxValueArray = new double[lenArray];

                        for (int k = 0; k < lenArray; k++) {
                            minValueArray[k] = Double.MAX_VALUE;
                            maxValueArray[k] = -Double.MAX_VALUE;
                        }
                    }

                    for (int j = 0; j < clade.attributeValues.size(); j++) {
                        Object value = clade.attributeValues.get(j)[i];
                        if (isDiscrete) {
                            final Object s = value;
                            if (hashMap.containsKey(s)) {
                                hashMap.put(s, hashMap.get(s) + 1);
                            } else {
                                hashMap.put(s, 1);
                            }
                        } else if (isBoolean) {
                            values[j] = (((Boolean) value) ? 1.0 : 0.0);
                        } else if (isDoubleArray) {
                            // Forcing to Double[] causes a cast exception. MAS
                            try {
                                Object[] array = (Object[]) value;
                                for (int k = 0; k < lenArray; k++) {
                                    valuesArray[k][j] = ((Double) array[k]);
                                    if (valuesArray[k][j] < minValueArray[k]) minValueArray[k] = valuesArray[k][j];
                                    if (valuesArray[k][j] > maxValueArray[k]) maxValueArray[k] = valuesArray[k][j];
                                }
                            } catch (Exception e) {
                                // ignore
                            }
                        } else {
                            // Ignore other (unknown) types
                            if (value instanceof Number) {
                                values[j] = ((Number) value).doubleValue();
                                if (values[j] < minValue) minValue = values[j];
                                if (values[j] > maxValue) maxValue = values[j];
                            }
                        }
                    }
                    if (isHeight) {
                        if (heightsOption == HeightsSummary.MEAN_HEIGHTS) {
                            final double mean = DiscreteStatistics.mean(values);
                            node.setHeight(mean);
                        } else if (heightsOption == HeightsSummary.MEDIAN_HEIGHTS) {
                            final double median = DiscreteStatistics.median(values);
                            node.setHeight(median);
                        } else {
                            // keep the existing height
                        }
                    }

                    if (!filter) {
                        boolean processed = false;
                        for (TreeAnnotationPlugin plugin : plugins) {
                            if (plugin.handleAttribute(node, attributeName, values)) {
                                processed = true;
                            }
                        }

                        if (!processed) {
                            if (!isDiscrete) {
                                if (!isDoubleArray)
                                    annotateMeanAttribute(node, attributeName, values);
                                else {
                                    for (int k = 0; k < lenArray; k++) {
                                        annotateMeanAttribute(node, attributeName + (k + 1), valuesArray[k]);
                                    }
                                }
                            } else {
                                annotateModeAttribute(node, attributeName, hashMap);
                                annotateFrequencyAttribute(node, attributeName, hashMap);
                            }
                            if (!isBoolean && minValue < maxValue && !isDiscrete && !isDoubleArray) {
                                // Basically, if it is a boolean (0, 1) then we don't need the distribution information
                                // Likewise if it doesn't vary.
                                annotateMedianAttribute(node, attributeName + "_median", values);
                                annotateHPDAttribute(node, attributeName + "_95%_HPD", 0.95, values);
                                annotateRangeAttribute(node, attributeName + "_range", values);
                            }

                            if (isDoubleArray) {
                                String name = attributeName;
                                // todo
//                                    if (name.equals(location1Attribute)) {
//                                        name = locationOutputAttribute;
//                                    }
                                boolean want2d = processBivariateAttributes && lenArray == 2;
                                if (name.equals("dmv")) {  // terrible hack
                                    want2d = false;
                                }
                                for (int k = 0; k < lenArray; k++) {
                                    if (minValueArray[k] < maxValueArray[k]) {
                                        annotateMedianAttribute(node, name + (k + 1) + "_median", valuesArray[k]);
                                        annotateRangeAttribute(node, name + (k + 1) + "_range", valuesArray[k]);
                                        if (!want2d)
                                            annotateHPDAttribute(node, name + (k + 1) + "_95%_HPD", 0.95, valuesArray[k]);
                                    }
                                }
                                // 2D contours
                                if (want2d) {

                                    boolean variationInFirst = (minValueArray[0] < maxValueArray[0]);
                                    boolean variationInSecond = (minValueArray[1] < maxValueArray[1]);

                                    if (variationInFirst && !variationInSecond)
                                        annotateHPDAttribute(node, name + "1" + "_95%_HPD", 0.95, valuesArray[0]);

                                    if (variationInSecond && !variationInFirst)
                                        annotateHPDAttribute(node, name + "2" + "_95%_HPD", 0.95, valuesArray[1]);

                                    if (variationInFirst && variationInSecond)
                                        annotate2DHPDAttribute(node, name, "_" + (int) (100 * hpd2D) + "%HPD", hpd2D, valuesArray);
                                }
                            }
                        }
                    }
                }
            }
            i++;
        }
    }

    private void annotateMeanAttribute(Node node, String label, double[] values) {
        double mean = DiscreteStatistics.mean(values);
        node.setMetaData(label, mean);
    }

    private void annotateMedianAttribute(Node node, String label, double[] values) {
        double median = DiscreteStatistics.median(values);
        node.setMetaData(label, median);

    }

    private void annotateModeAttribute(Node node, String label, HashMap<Object, Integer> values) {
        Object mode = null;
        int maxCount = 0;
        int totalCount = 0;
        int countInMode = 1;

        for (Object key : values.keySet()) {
            int thisCount = values.get(key);
            if (thisCount == maxCount) {
                // I hope this is the intention
                mode = mode.toString().concat("+" + key);
                countInMode++;
            } else if (thisCount > maxCount) {
                mode = key;
                maxCount = thisCount;
                countInMode = 1;
            }
            totalCount += thisCount;
        }
        double freq = (double) maxCount / (double) totalCount * countInMode;
        node.setMetaData(label, mode);
        node.setMetaData(label + ".prob", freq);
    }

    private void annotateFrequencyAttribute(Node node, String label, HashMap<Object, Integer> values) {
        double totalCount = 0;
        Set<?> keySet = values.keySet();
        int length = keySet.size();
        String[] name = new String[length];
        Double[] freq = new Double[length];
        int index = 0;
        for (Object key : values.keySet()) {
            name[index] = key.toString();
            freq[index] = new Double(values.get(key));
            totalCount += freq[index];
            index++;
        }
        for (int i = 0; i < length; i++)
            freq[i] /= totalCount;

        node.setMetaData(label + ".set", name);
        node.setMetaData(label + ".set.prob", freq);
    }

    private void annotateRangeAttribute(Node node, String label, double[] values) {
        double min = DiscreteStatistics.min(values);
        double max = DiscreteStatistics.max(values);
        node.setMetaData(label, new Object[]{min, max});
    }

    private void annotateHPDAttribute(Node node, String label, double hpd, double[] values) {
        int[] indices = new int[values.length];
        HeapSort.sort(values, indices);

        double minRange = Double.MAX_VALUE;
        int hpdIndex = 0;

        int diff = (int) Math.round(hpd * (double) values.length);
        for (int i = 0; i <= (values.length - diff); i++) {
            double minValue = values[indices[i]];
            double maxValue = values[indices[i + diff - 1]];
            double range = Math.abs(maxValue - minValue);
            if (range < minRange) {
                minRange = range;
                hpdIndex = i;
            }
        }
        double lower = values[indices[hpdIndex]];
        double upper = values[indices[hpdIndex + diff - 1]];
        node.setMetaData(label, new Object[]{lower, upper});
    }

    // todo Move rEngine to outer class; create once.
//        Rengine rEngine = null;
//
//        private final String[] rArgs = {"--no-save"};
//
//
//        private final String[] rBootCommands = {
//                "library(MASS)",
//                "makeContour = function(var1, var2, prob=0.95, n=50, h=c(1,1)) {" +
//                        "post1 = kde2d(var1, var2, n = n, h=h); " +    // This had h=h in argument
//                        "dx = diff(post1$x[1:2]); " +
//                        "dy = diff(post1$y[1:2]); " +
//                        "sz = sort(post1$z); " +
//                        "c1 = cumsum(sz) * dx * dy; " +
//                        "levels = sapply(prob, function(x) { approx(c1, sz, xout = 1 - x)$y }); " +
//                        "line = contourLines(post1$x, post1$y, post1$z, level = levels); " +
//                        "return(line) }"
//        };
//
//        private String makeRString(double[] values) {
//            StringBuffer sb = new StringBuffer("c(");
//            sb.append(values[0]);
//            for (int i = 1; i < values.length; i++) {
//                sb.append(",");
//                sb.append(values[i]);
//            }
//            sb.append(")");
//            return sb.toString();
//        }

    public static final String CORDINATE = "cordinates";

//		private String formattedLocation(double loc1, double loc2) {
//			return formattedLocation(loc1) + "," + formattedLocation(loc2);
//		}

    private String formattedLocation(double x) {
        return String.format("%5.2f", x);
    }

    private void annotate2DHPDAttribute(Node node, String preLabel, String postLabel,
                                        double hpd, double[][] values) {
        if (USE_R) {

            // Uses R-Java interface, and the HPD routines from 'emdbook' and 'coda'

//                int N = 50;
//                if (rEngine == null) {
//
//                    if (!Rengine.versionCheck()) {
//                        throw new RuntimeException("JRI library version mismatch");
//                    }
//
//                    rEngine = new Rengine(rArgs, false, null);
//
//                    if (!rEngine.waitForR()) {
//                        throw new RuntimeException("Cannot load R");
//                    }
//
//                    for (String command : rBootCommands) {
//                        rEngine.eval(command);
//                    }
//                }
//
//                // todo Need a good method to pick grid size
//
//
//                REXP x = rEngine.eval("makeContour(" +
//                        makeRString(values[0]) + "," +
//                        makeRString(values[1]) + "," +
//                        hpd + "," +
//                        N + ")");
//
//                RVector contourList = x.asVector();
//                int numberContours = contourList.size();
//
//                if (numberContours > 1) {
//                    System.err.println("Warning: a node has a disjoint " + 100 * hpd + "% HPD region.  This may be an artifact!");
//                    System.err.println("Try decreasing the enclosed mass or increasing the number of samples.");
//                }
//
//
//                node.setMetaData(preLabel + postLabel + "_modality", numberContours);
//
//                StringBuffer output = new StringBuffer();
//                for (int i = 0; i < numberContours; i++) {
//                    output.append("\n<" + CORDINATE + ">\n");
//                    RVector oneContour = contourList.at(i).asVector();
//                    double[] xList = oneContour.at(1).asDoubleArray();
//                    double[] yList = oneContour.at(2).asDoubleArray();
//                    StringBuffer xString = new StringBuffer("{");
//                    StringBuffer yString = new StringBuffer("{");
//                    for (int k = 0; k < xList.length; k++) {
//                        xString.append(formattedLocation(xList[k])).append(",");
//                        yString.append(formattedLocation(yList[k])).append(",");
//                    }
//                    xString.append(formattedLocation(xList[0])).append("}");
//                    yString.append(formattedLocation(yList[0])).append("}");
//
//                    node.setMetaData(preLabel + "1" + postLabel + "_" + (i + 1), xString);
//                    node.setMetaData(preLabel + "2" + postLabel + "_" + (i + 1), yString);
//                }


        } else { // do not use R


//                KernelDensityEstimator2D kde = new KernelDensityEstimator2D(values[0], values[1], N);
            //ContourMaker kde = new ContourWithSynder(values[0], values[1], N);
            boolean bandwidthLimit = false;

            ContourMaker kde = new ContourWithSynder(values[0], values[1], bandwidthLimit);

            ContourPath[] paths = kde.getContourPaths(hpd);

            node.setMetaData(preLabel + postLabel + "_modality", paths.length);

            if (paths.length > 1) {
                System.err.println("Warning: a node has a disjoint " + 100 * hpd + "% HPD region.  This may be an artifact!");
                System.err.println("Try decreasing the enclosed mass or increasing the number of samples.");
            }

            StringBuffer output = new StringBuffer();
            int i = 0;
            for (ContourPath p : paths) {
                output.append("\n<" + CORDINATE + ">\n");
                double[] xList = p.getAllX();
                double[] yList = p.getAllY();
                StringBuffer xString = new StringBuffer("{");
                StringBuffer yString = new StringBuffer("{");
                for (int k = 0; k < xList.length; k++) {
                    xString.append(formattedLocation(xList[k])).append(",");
                    yString.append(formattedLocation(yList[k])).append(",");
                }
                xString.append(formattedLocation(xList[0])).append("}");
                yString.append(formattedLocation(yList[0])).append("}");

                node.setMetaData(preLabel + "1" + postLabel + "_" + (i + 1), xString);
                node.setMetaData(preLabel + "2" + postLabel + "_" + (i + 1), yString);
                i++;

            }
        }
    }

    int totalTrees = 0;
    int totalTreesUsed = 0;
    double posteriorLimit = 0.0;
    double hpd2D = 0.80;

    private final List<TreeAnnotationPlugin> plugins = new ArrayList<TreeAnnotationPlugin>();

    Set<String> attributeNames = new HashSet<String>();
    TaxonSet taxa = null;

    static boolean processBivariateAttributes = false;

//    static {
//        try {
//            System.loadLibrary("jri");
//            processBivariateAttributes = true;
//            System.err.println("JRI loaded. Will process bivariate attributes");
//        } catch (UnsatisfiedLinkError e) {
//            System.err.print("JRI not available. ");
//            if (!USE_R) {
//                processBivariateAttributes = true;
//                System.err.println("Using Java bivariate attributes");
//            } else {
//                System.err.println("Will not process bivariate attributes");
//            }
//        }
//    }

    public static void printTitle() {
        progressStream.println();
        centreLine("TreeAnnotator " + version.getVersionString() + ", " + version.getDateString(), 60);
        centreLine("MCMC Output analysis", 60);
        centreLine("by", 60);
        centreLine("Andrew Rambaut and Alexei J. Drummond", 60);
        progressStream.println();
        centreLine("Institute of Evolutionary Biology", 60);
        centreLine("University of Edinburgh", 60);
        centreLine("a.rambaut@ed.ac.uk", 60);
        progressStream.println();
        centreLine("Department of Computer Science", 60);
        centreLine("University of Auckland", 60);
        centreLine("alexei@cs.auckland.ac.nz", 60);
        progressStream.println();
        progressStream.println();
    }

    public static void centreLine(String line, int pageWidth) {
        int n = pageWidth - line.length();
        int n1 = n / 2;
        for (int i = 0; i < n1; i++) {
            progressStream.print(" ");
        }
        progressStream.println(line);
    }


    public static void printUsage(Arguments arguments) {

        arguments.printUsage("treeannotator", "<input-file-name> [<output-file-name>]");
        progressStream.println();
        progressStream.println("  Example: treeannotator test.trees out.txt");
        progressStream.println("  Example: treeannotator -burnin 10 -heights mean test.trees out.txt");
        progressStream.println("  Example: treeannotator -burnin 20 -target map.tree test.trees out.txt");
        progressStream.println();
    }

    //Main method
    public static void main(String[] args) throws IOException {

        // There is a major issue with languages that use the comma as a decimal separator.
        // To ensure compatibility between programs in the package, enforce the US locale.
        Locale.setDefault(Locale.US);

        String targetTreeFileName = null;
        String inputFileName = null;
        String outputFileName = null;

        if (args.length == 0) {
            System.setProperty("com.apple.macos.useScreenMenuBar", "true");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.showGrowBox", "true");

            java.net.URL url = LogCombiner.class.getResource("/images/utility.png");
            javax.swing.Icon icon = null;

            if (url != null) {
                icon = new javax.swing.ImageIcon(url);
            }

            final String versionString = version.getVersionString();
            String nameString = "TreeAnnotator " + versionString;
            String aboutString = "<html><center><p>" + versionString + ", " + version.getDateString() + "</p>" +
                    "<p>by<br>" +
                    "Andrew Rambaut and Alexei J. Drummond</p>" +
                    "<p>Institute of Evolutionary Biology, University of Edinburgh<br>" +
                    "<a href=\"mailto:a.rambaut@ed.ac.uk\">a.rambaut@ed.ac.uk</a></p>" +
                    "<p>Department of Computer Science, University of Auckland<br>" +
                    "<a href=\"mailto:alexei@cs.auckland.ac.nz\">alexei@cs.auckland.ac.nz</a></p>" +
                    "<p>Part of the BEAST package:<br>" +
                    "<a href=\"http://beast.bio.ed.ac.uk/\">http://beast.bio.ed.ac.uk/</a></p>" +
                    "</center></html>";

            new ConsoleApplication(nameString, aboutString, icon, true);

            // The ConsoleApplication will have overridden System.out so set progressStream
            // to capture the output to the window:
            progressStream = System.out;

            printTitle();

            TreeAnnotatorDialog dialog = new TreeAnnotatorDialog(new JFrame());

            if (!dialog.showDialog("TreeAnnotator " + versionString)) {
                return;
            }

            int burninPercentage = dialog.getBurninPercentage();
            double posteriorLimit = dialog.getPosteriorLimit();
            double hpd2D = 0.80;
            Target targetOption = dialog.getTargetOption();
            HeightsSummary heightsOption = dialog.getHeightsOption();

            targetTreeFileName = dialog.getTargetFileName();
            if (targetOption == Target.USER_TARGET_TREE && targetTreeFileName == null) {
                System.err.println("No target file specified");
                return;
            }

            inputFileName = dialog.getInputFileName();
            if (inputFileName == null) {
                System.err.println("No input file specified");
                return;
            }

            outputFileName = dialog.getOutputFileName();
            if (outputFileName == null) {
                System.err.println("No output file specified");
                return;
            }

            try {
            	boolean allowSingleChild = false;
                new TreeAnnotator(burninPercentage,
                		allowSingleChild,
                        heightsOption,
                        posteriorLimit,
                        hpd2D,
                        targetOption,
                        targetTreeFileName,
                        inputFileName,
                        outputFileName);

            } catch (Exception ex) {
                System.err.println("Exception: " + ex.getMessage());
            }

            progressStream.println("Finished - Quit program to exit.");
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        printTitle();

        Arguments arguments = new Arguments(
                new Arguments.Option[]{
                        //new Arguments.StringOption("target", new String[] { "maxclade", "maxtree" }, false, "an option of 'maxclade' or 'maxtree'"),
                        new Arguments.StringOption("heights", new String[]{"keep", "median", "mean", "ca"}, false,
                                "an option of 'keep' (default), 'median', 'mean' or 'ca'"),
                        new Arguments.IntegerOption("burnin", "the percentage of states to be considered as 'burn-in'"),
                        new Arguments.RealOption("limit", "the minimum posterior probability for a node to be annotated"),
                        new Arguments.StringOption("target", "target_file_name", "specifies a user target tree to be annotated"),
                        new Arguments.Option("help", "option to print this message"),
                        new Arguments.Option("forceDiscrete", "forces integer traits to be treated as discrete traits."),
                        new Arguments.RealOption("hpd2D", "the HPD interval to be used for the bivariate traits")
                });

        try {
            arguments.parseArguments(args);
        } catch (Arguments.ArgumentException ae) {
            progressStream.println(ae);
            printUsage(arguments);
            System.exit(1);
        }

        if (arguments.hasOption("forceDiscrete")) {
            System.out.println("  Forcing integer traits to be treated as discrete traits.");
            forceIntegerToDiscrete = true;
        }

        if (arguments.hasOption("help")) {
            printUsage(arguments);
            System.exit(0);
        }

        HeightsSummary heights = HeightsSummary.CA_HEIGHTS;
        if (arguments.hasOption("heights")) {
            String value = arguments.getStringOption("heights");
            if (value.equalsIgnoreCase("mean")) {
                heights = HeightsSummary.MEAN_HEIGHTS;
            } else if (value.equalsIgnoreCase("median")) {
                heights = HeightsSummary.MEDIAN_HEIGHTS;
            } else if (value.equalsIgnoreCase("ca")) {
                heights = HeightsSummary.CA_HEIGHTS;
                System.out.println("Please cite: Heled and Bouckaert: Looking for trees in the forest:\n" +
                                        "summary tree from posterior samples. BMC Evolutionary Biology 2013 13:221.");
            }
        }

        int burnin = -1;
        if (arguments.hasOption("burnin")) {
            burnin = arguments.getIntegerOption("burnin");
        }
        if (burnin >= 100) {
        	System.err.println("burnin is a percentage and should be less than 100.");
        	System.exit(0);
        }

        double posteriorLimit = 0.0;
        if (arguments.hasOption("limit")) {
            posteriorLimit = arguments.getRealOption("limit");
        }

        double hpd2D = 0.80;
        if (arguments.hasOption("hpd2D")) {
            hpd2D = arguments.getRealOption("hpd2D");
        }

        Target target = Target.MAX_CLADE_CREDIBILITY;
        if (arguments.hasOption("target")) {
            target = Target.USER_TARGET_TREE;
            targetTreeFileName = arguments.getStringOption("target");
        }

        final String[] args2 = arguments.getLeftoverArguments();

        switch (args2.length) {
            case 2:
                outputFileName = args2[1];
                // fall to
            case 1:
                inputFileName = args2[0];
                break;
            default: {
                System.err.println("Unknown option: " + args2[2]);
                System.err.println();
                printUsage(arguments);
                System.exit(1);
            }
        }
        boolean allowSingleChild = false;
        
        try {
        	new TreeAnnotator(burnin, allowSingleChild, heights, posteriorLimit, hpd2D, target, targetTreeFileName, inputFileName, outputFileName);
        } catch (IOException e) {
        	throw e;
        } catch (Exception e) {
			e.printStackTrace();
		}

        System.exit(0);
    }

    /**
     * @author Andrew Rambaut
     * @version $Id$
     */
    //TODO code review: it seems not necessary
    public static interface TreeAnnotationPlugin {
        Set<String> setAttributeNames(Set<String> attributeNames);

        boolean handleAttribute(Node node, String attributeName, double[] values);
    }

    boolean setTreeHeightsByCA(Tree targetTree)
            throws Exception {
        progressStream.println("Setting node heights...");
        progressStream.println("0              25             50             75            100");
        progressStream.println("|--------------|--------------|--------------|--------------|");

        int reportStepSize = totalTrees / 60;
        if (reportStepSize < 1) reportStepSize = 1;
        int reported = 0;


        // this call increments the clade counts and it shouldn't
        // this is remedied with removeClades call after while loop below
        CladeSystem cladeSystem = new CladeSystem(targetTree);
        final int nClades = cladeSystem.getCladeMap().size();

        // allocate posterior tree nodes order once
        int[] postOrderList = new int[nClades];
        BitSet[] ctarget = new BitSet[nClades];
        BitSet[] ctree = new BitSet[nClades];

        for (int k = 0; k < nClades; ++k) {
            ctarget[k] = new BitSet();
            ctree[k] = new BitSet();
        }

        cladeSystem.getTreeCladeCodes(targetTree, ctarget);

        // temp collecting heights inside loop allocated once
        double[] hs = new double[nClades];

        // heights total sum from posterior trees
        double[] ths = new double[nClades];

        totalTreesUsed = 0;

        int counter = 0;
        for (Tree tree: trees) {
            TreeUtils.preOrderTraversalList(tree, postOrderList);
            cladeSystem.getTreeCladeCodes(tree, ctree);
            for (int k = 0; k < nClades; ++k) {
                int j = postOrderList[k];
                for (int i = 0; i < nClades; ++i) {
                    if( CollectionUtils.isSubSet(ctarget[i], ctree[j]) ) {
                        hs[i] = tree.getNode(j).getHeight();
                    }
                }
            }
            for (int k = 0; k < nClades; ++k) {
                ths[k] += hs[k];
            }
            totalTreesUsed += 1;
            if (counter > 0 && counter % reportStepSize == 0 && reported < 61) {
                progressStream.print("*");
                progressStream.flush();
                reported++;
            }
            counter++;

        }
        targetTree.initAndValidate();
        cladeSystem.removeClades(targetTree.getRoot(), true);
        for (int k = 0; k < nClades; ++k) {
            ths[k] /= totalTreesUsed;
            final Node node = targetTree.getNode(k);
            node.setHeight(ths[k]);
        }

        progressStream.println();
        progressStream.println();

        return true;
    }

}

