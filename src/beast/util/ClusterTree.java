/*
* File ClusterTree.java
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
package beast.util;


import beast.core.Description;
import beast.core.Input;
import beast.core.StateNode;
import beast.core.StateNodeInitialiser;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.distance.Distance;
import beast.evolution.alignment.distance.JukesCantorDistance;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;





/**
 * Adapted from Weka's HierarchicalClustering class *
 */
@Description("Create initial beast.tree by hierarchical clustering, either through one of the classic link methods " +
        "or by neighbor joining. The following link methods are supported: " +
        "<br/>o single link, " +
        "<br/>o complete link, " +
        "<br/>o UPGMA=average link, " +
        "<br/>o mean link, " +
        "<br/>o centroid, " +
        "<br/>o Ward and " +
        "<br/>o adjusted complete link " +
        "<br/>o neighborjoining " +
        "<br/>o neighborjoining2 - corrects tree for tip data, unlike plain neighborjoining")
public class ClusterTree extends Tree implements StateNodeInitialiser {

    enum Type {single, average, complete, upgma, mean, centroid, ward, adjcomplete, neighborjoining, neighborjoining2}


    double EPSILON = 1e-10;

    public Input<Type> clusterTypeInput = new Input<Type>("clusterType", "type of clustering algorithm used for generating initial beast.tree. " +
            "Should be one of " + Type.values() + " (default " + Type.average + ")", Type.average, Type.values());
    public Input<Alignment> dataInput = new Input<Alignment>("taxa", "alignment data used for calculating distances for clustering");

    public Input<Distance> distanceInput = new Input<Distance>("distance", "method for calculating distance between two sequences (default Jukes Cantor)");

    public Input<RealParameter> clockRateInput = new Input<RealParameter>("clock.rate",
            "the clock rate parameter, used to divide all divergence times by, to convert from substitutions to times. (default 1.0)",
            new RealParameter(new Double[] {1.0}));

    /**
     * Whether the distance represent node height (if false) or branch length (if true).
     */
    protected boolean distanceIsBranchLength = false;
    Distance distance;
    List<String> taxaNames;

    /**
     * Holds the Link type used calculate distance between clusters
     */
    Type nLinkType = Type.single;


    @Override
    public void initAndValidate() throws Exception {
    	
        RealParameter clockRate = clockRateInput.get();

    	if (dataInput.get() != null) {
    		taxaNames = dataInput.get().getTaxaNames();
    	} else {
    		if (m_taxonset.get() == null) {
    			throw new Exception("At least one of taxa and taxonset input needs to be specified");
    		}
    		taxaNames = m_taxonset.get().asStringList();
    	}
        if (Boolean.valueOf(System.getProperty("beast.resume")) &&
                (isEstimatedInput.get() || (m_initial.get() != null && m_initial.get().isEstimatedInput.get()))) {
            // don't bother creating a cluster tree to save some time, if it is read from file anyway
            // make a caterpillar
            Node left = newNode();
            left.setNr(0);
            left.setID(taxaNames.get(0));
            left.setHeight(0);
            for (int i = 1; i < taxaNames.size(); i++) {
                final Node right = newNode();
                right.setNr(i);
                right.setID(taxaNames.get(i));
                right.setHeight(0);
                final Node parent = newNode();
                parent.setNr(taxaNames.size() + i - 1);
                parent.setHeight(i);
                left.setParent(parent);
                parent.setLeft(left);
                right.setParent(parent);
                parent.setRight(right);
                left = parent;
            }
            root = left;
            leafNodeCount = taxaNames.size();
            nodeCount = leafNodeCount * 2 - 1;
            internalNodeCount = leafNodeCount - 1;
            super.initAndValidate();
            return;
        }

        distance = distanceInput.get();
        if (distance == null) {
            distance = new JukesCantorDistance();
        }
        if (distance instanceof Distance.Base){
        	if (dataInput.get() == null) {
        		// Distance requires an alignment?
        	}
        	((Distance.Base) distance).setPatterns(dataInput.get());
        }

        nLinkType = clusterTypeInput.get();

        if (nLinkType == Type.upgma) nLinkType = Type.average;

        if (nLinkType == Type.neighborjoining || nLinkType == Type.neighborjoining2) {
            distanceIsBranchLength = true;
        }
        final Node root = buildClusterer();
        setRoot(root);
        root.labelInternalNodes((getNodeCount() + 1) / 2);
        super.initAndValidate();
        if (nLinkType == Type.neighborjoining2) {
            // set tip dates to zero
            final Node[] nodes = getNodesAsArray();
            for (int i = 0; i < getLeafNodeCount(); i++) {
                nodes[i].setHeight(0);
            }
            super.initAndValidate();
        }

        if (m_initial.get() != null)
            processTraits(m_initial.get().m_traitList.get());
        else
            processTraits(m_traitList.get());

        if (timeTraitSet != null)
            adjustTreeNodeHeights(root);
        else {
        	// all nodes should be at zero height if no date-trait is available
        	for (int i = 0; i < getLeafNodeCount(); i++) {
        		getNode(i).setHeight(0);
        	}
        }

        //divide all node heights by clock rate to convert from substitutions to time.
        for (Node node : getInternalNodes()) {
            double height = node.getHeight();
            node.setHeight(height/clockRate.getValue());
        }

        initStateNodes();
    }


    public ClusterTree() {
    } // c'tor


    /**
     * class representing node in cluster hierarchy *
     */
    class NodeX {
        NodeX m_left;
        NodeX m_right;
        NodeX m_parent;
        int m_iLeftInstance;
        int m_iRightInstance;
        double m_fLeftLength = 0;
        double m_fRightLength = 0;
        double m_fHeight = 0;

        void setHeight(double fHeight1, double fHeight2) {
            if (fHeight1 < EPSILON) {
                fHeight1 = EPSILON;
            }
            if (fHeight2 < EPSILON) {
                fHeight2 = EPSILON;
            }
            m_fHeight = fHeight1;
            if (m_left == null) {
                m_fLeftLength = fHeight1;
            } else {
                m_fLeftLength = fHeight1 - m_left.m_fHeight;
            }
            if (m_right == null) {
                m_fRightLength = fHeight2;
            } else {
                m_fRightLength = fHeight2 - m_right.m_fHeight;
            }
        }

        void setLength(double fLength1, double fLength2) {
            if (fLength1 < EPSILON) {
                fLength1 = EPSILON;
            }
            if (fLength2 < EPSILON) {
                fLength2 = EPSILON;
            }
            m_fLeftLength = fLength1;
            m_fRightLength = fLength2;
            m_fHeight = fLength1;
            if (m_left != null) {
                m_fHeight += m_left.m_fHeight;
            }
        }

        public String toString() {
            final DecimalFormat myFormatter = new DecimalFormat("#.#####", new DecimalFormatSymbols(Locale.US));

            if (m_left == null) {
                if (m_right == null) {
                    return "(" + taxaNames.get(m_iLeftInstance) + ":" + myFormatter.format(m_fLeftLength) + "," +
                            taxaNames.get(m_iRightInstance) + ":" + myFormatter.format(m_fRightLength) + ")";
                } else {
                    return "(" + taxaNames.get(m_iLeftInstance) + ":" + myFormatter.format(m_fLeftLength) + "," +
                            m_right.toString() + ":" + myFormatter.format(m_fRightLength) + ")";
                }
            } else {
                if (m_right == null) {
                    return "(" + m_left.toString() + ":" + myFormatter.format(m_fLeftLength) + "," +
                            taxaNames.get(m_iRightInstance) + ":" + myFormatter.format(m_fRightLength) + ")";
                } else {
                    return "(" + m_left.toString() + ":" + myFormatter.format(m_fLeftLength) + "," + m_right.toString() + ":" + myFormatter.format(m_fRightLength) + ")";
                }
            }
        }

        Node toNode() throws Exception {
            final Node node = newNode();
            node.setHeight(m_fHeight);
            if (m_left == null) {
                node.setLeft(newNode());
                node.getLeft().setNr(m_iLeftInstance);
                node.getLeft().setID(taxaNames.get(m_iLeftInstance));
                node.getLeft().setHeight(m_fHeight - m_fLeftLength);
                if (m_right == null) {
                    node.setRight(newNode());
                    node.getRight().setNr(m_iRightInstance);
                    node.getRight().setID(taxaNames.get(m_iRightInstance));
                    node.getRight().setHeight(m_fHeight - m_fRightLength);
                } else {
                    node.setRight(m_right.toNode());
                }
            } else {
                node.setLeft(m_left.toNode());
                if (m_right == null) {
                    node.setRight(newNode());
                    node.getRight().setNr(m_iRightInstance);
                    node.getRight().setID(taxaNames.get(m_iRightInstance));
                    node.getRight().setHeight(m_fHeight - m_fRightLength);
                } else {
                    node.setRight(m_right.toNode());
                }
            }
            if (node.getHeight() < node.getLeft().getHeight() + EPSILON) {
                node.setHeight(node.getLeft().getHeight() + EPSILON);
            }
            if (node.getHeight() < node.getRight().getHeight() + EPSILON) {
                node.setHeight(node.getRight().getHeight() + EPSILON);
            }

            node.getRight().setParent(node);
            node.getLeft().setParent(node);
            return node;
        }
    } // class NodeX

    /**
     * used for priority queue for efficient retrieval of pair of clusters to merge*
     */
    class Tuple {
        public Tuple(final double d, final int i, final int j, final int nSize1, final int nSize2) {
            m_fDist = d;
            m_iCluster1 = i;
            m_iCluster2 = j;
            m_nClusterSize1 = nSize1;
            m_nClusterSize2 = nSize2;
        }

        double m_fDist;
        int m_iCluster1;
        int m_iCluster2;
        int m_nClusterSize1;
        int m_nClusterSize2;
    }

    /**
     * comparator used by priority queue*
     */
    class TupleComparator implements Comparator<Tuple> {
        public int compare(final Tuple o1, final Tuple o2) {
            if (o1.m_fDist < o2.m_fDist) {
                return -1;
            } else if (o1.m_fDist == o2.m_fDist) {
                return 0;
            }
            return 1;
        }
    }

    // return distance according to distance metric
    double distance(final int iTaxon1, final int iTaxon2) {
        return distance.pairwiseDistance(iTaxon1, iTaxon2);
    } // distance

    // 1-norm
    double distance(final double[] nPattern1, final double[] nPattern2) {
        double fDist = 0;
        for (int i = 0; i < dataInput.get().getPatternCount(); i++) {
            fDist += dataInput.get().getPatternWeight(i) * Math.abs(nPattern1[i] - nPattern2[i]);
        }
        return fDist / dataInput.get().getSiteCount();
    }


    @SuppressWarnings("unchecked")
    public Node buildClusterer() throws Exception {
        final int nTaxa = taxaNames.size();
        if (nTaxa == 1) {
            // pathological case
            final Node node = newNode();
            node.setHeight(1);
            node.setNr(0);
            return node;
        }

        // use array of integer vectors to store cluster indices,
        // starting with one cluster per instance
        final List<Integer>[] nClusterID = new ArrayList[nTaxa];
        for (int i = 0; i < nTaxa; i++) {
            nClusterID[i] = new ArrayList<Integer>();
            nClusterID[i].add(i);
        }
        // calculate distance matrix
        final int nClusters = nTaxa;

        // used for keeping track of hierarchy
        final NodeX[] clusterNodes = new NodeX[nTaxa];
        if (nLinkType == Type.neighborjoining || nLinkType == Type.neighborjoining2) {
            neighborJoining(nClusters, nClusterID, clusterNodes);
        } else {
            doLinkClustering(nClusters, nClusterID, clusterNodes);
        }

        // move all clusters in m_nClusterID array
        // & collect hierarchy
        for (int i = 0; i < nTaxa; i++) {
            if (nClusterID[i].size() > 0) {
                return clusterNodes[i].toNode();
            }
        }
        return null;
    } // buildClusterer

    /**
     * use neighbor joining algorithm for clustering
     * This is roughly based on the RapidNJ simple implementation and runs at O(n^3)
     * More efficient implementations exist, see RapidNJ (or my GPU implementation :-))
     *
     * @param nClusters
     * @param nClusterID
     * @param clusterNodes
     */
    void neighborJoining(int nClusters, final List<Integer>[] nClusterID, final NodeX[] clusterNodes) {
        final int n = taxaNames.size();

        final double[][] fDist = new double[nClusters][nClusters];
        for (int i = 0; i < nClusters; i++) {
            fDist[i][i] = 0;
            for (int j = i + 1; j < nClusters; j++) {
                fDist[i][j] = getDistance0(nClusterID[i], nClusterID[j]);
                fDist[j][i] = fDist[i][j];
            }
        }

        final double[] fSeparationSums = new double[n];
        final double[] fSeparations = new double[n];
        final int[] nNextActive = new int[n];

        //calculate initial separation rows
        for (int i = 0; i < n; i++) {
            double fSum = 0;
            for (int j = 0; j < n; j++) {
                fSum += fDist[i][j];
            }
            fSeparationSums[i] = fSum;
            fSeparations[i] = fSum / (nClusters - 2);
            nNextActive[i] = i + 1;
        }

        while (nClusters > 2) {
            // find minimum
            int iMin1 = -1;
            int iMin2 = -1;
            double fMin = Double.MAX_VALUE;
            {
                int i = 0;
                while (i < n) {
                    final double fSep1 = fSeparations[i];
                    final double[] fRow = fDist[i];
                    int j = nNextActive[i];
                    while (j < n) {
                        final double fSep2 = fSeparations[j];
                        final double fVal = fRow[j] - fSep1 - fSep2;
                        if (fVal < fMin) {
                            // new minimum
                            iMin1 = i;
                            iMin2 = j;
                            fMin = fVal;
                        }
                        j = nNextActive[j];
                    }
                    i = nNextActive[i];
                }
            }
            // record distance
            final double fMinDistance = fDist[iMin1][iMin2];
            nClusters--;
            final double fSep1 = fSeparations[iMin1];
            final double fSep2 = fSeparations[iMin2];
            final double fDist1 = (0.5 * fMinDistance) + (0.5 * (fSep1 - fSep2));
            final double fDist2 = (0.5 * fMinDistance) + (0.5 * (fSep2 - fSep1));
            if (nClusters > 2) {
                // update separations  & distance
                double fNewSeparationSum = 0;
                final double fMutualDistance = fDist[iMin1][iMin2];
                final double[] fRow1 = fDist[iMin1];
                final double[] fRow2 = fDist[iMin2];
                for (int i = 0; i < n; i++) {
                    if (i == iMin1 || i == iMin2 || nClusterID[i].size() == 0) {
                        fRow1[i] = 0;
                    } else {
                        final double fVal1 = fRow1[i];
                        final double fVal2 = fRow2[i];
                        final double fDistance = (fVal1 + fVal2 - fMutualDistance) / 2.0;
                        fNewSeparationSum += fDistance;
                        // update the separationsum of cluster i.
                        fSeparationSums[i] += (fDistance - fVal1 - fVal2);
                        fSeparations[i] = fSeparationSums[i] / (nClusters - 2);
                        fRow1[i] = fDistance;
                        fDist[i][iMin1] = fDistance;
                    }
                }
                fSeparationSums[iMin1] = fNewSeparationSum;
                fSeparations[iMin1] = fNewSeparationSum / (nClusters - 2);
                fSeparationSums[iMin2] = 0;
                merge(iMin1, iMin2, fDist1, fDist2, nClusterID, clusterNodes);
                int iPrev = iMin2;
                // since iMin1 < iMin2 we havenActiveRows[0] >= 0, so the next loop should be save
                while (nClusterID[iPrev].size() == 0) {
                    iPrev--;
                }
                nNextActive[iPrev] = nNextActive[iMin2];
            } else {
                merge(iMin1, iMin2, fDist1, fDist2, nClusterID, clusterNodes);
                break;
            }
        }

        for (int i = 0; i < n; i++) {
            if (nClusterID[i].size() > 0) {
                for (int j = i + 1; j < n; j++) {
                    if (nClusterID[j].size() > 0) {
                        final double fDist1 = fDist[i][j];
                        if (nClusterID[i].size() == 1) {
                            merge(i, j, fDist1, 0, nClusterID, clusterNodes);
                        } else if (nClusterID[j].size() == 1) {
                            merge(i, j, 0, fDist1, nClusterID, clusterNodes);
                        } else {
                            merge(i, j, fDist1 / 2.0, fDist1 / 2.0, nClusterID, clusterNodes);
                        }
                        break;
                    }
                }
            }
        }
    } // neighborJoining

    /**
     * Perform clustering using a link method
     * This implementation uses a priority queue resulting in a O(n^2 log(n)) algorithm
     *
     * @param nClusters    number of clusters
     * @param nClusterID
     * @param clusterNodes
     */
    void doLinkClustering(int nClusters, final List<Integer>[] nClusterID, final NodeX[] clusterNodes) {
        final int nInstances = taxaNames.size();
        final PriorityQueue<Tuple> queue = new PriorityQueue<Tuple>(nClusters * nClusters / 2, new TupleComparator());
        final double[][] fDistance0 = new double[nClusters][nClusters];
        for (int i = 0; i < nClusters; i++) {
            fDistance0[i][i] = 0;
            for (int j = i + 1; j < nClusters; j++) {
                fDistance0[i][j] = getDistance0(nClusterID[i], nClusterID[j]);
                fDistance0[j][i] = fDistance0[i][j];
                queue.add(new Tuple(fDistance0[i][j], i, j, 1, 1));
            }
        }
        while (nClusters > 1) {
            int iMin1 = -1;
            int iMin2 = -1;
            // use priority queue to find next best pair to cluster
            Tuple t;
            do {
                t = queue.poll();
            }
            while (t != null && (nClusterID[t.m_iCluster1].size() != t.m_nClusterSize1 || nClusterID[t.m_iCluster2].size() != t.m_nClusterSize2));
            iMin1 = t.m_iCluster1;
            iMin2 = t.m_iCluster2;
            merge(iMin1, iMin2, t.m_fDist/2.0, t.m_fDist/2.0, nClusterID, clusterNodes);
            // merge  clusters

            // update distances & queue
            for (int i = 0; i < nInstances; i++) {
                if (i != iMin1 && nClusterID[i].size() != 0) {
                    final int i1 = Math.min(iMin1, i);
                    final int i2 = Math.max(iMin1, i);
                    final double fDistance = getDistance(fDistance0, nClusterID[i1], nClusterID[i2]);
                    queue.add(new Tuple(fDistance, i1, i2, nClusterID[i1].size(), nClusterID[i2].size()));
                }
            }

            nClusters--;
        }
    } // doLinkClustering

    void merge(int iMin1, int iMin2, double fDist1, double fDist2, final List<Integer>[] nClusterID, final NodeX[] clusterNodes) {
        if (iMin1 > iMin2) {
            final int h = iMin1;
            iMin1 = iMin2;
            iMin2 = h;
            final double f = fDist1;
            fDist1 = fDist2;
            fDist2 = f;
        }
        nClusterID[iMin1].addAll(nClusterID[iMin2]);
        //nClusterID[iMin2].removeAllElements();
        nClusterID[iMin2].removeAll(nClusterID[iMin2]);

        // track hierarchy
        final NodeX node = new NodeX();
        if (clusterNodes[iMin1] == null) {
            node.m_iLeftInstance = iMin1;
        } else {
            node.m_left = clusterNodes[iMin1];
            clusterNodes[iMin1].m_parent = node;
        }
        if (clusterNodes[iMin2] == null) {
            node.m_iRightInstance = iMin2;
        } else {
            node.m_right = clusterNodes[iMin2];
            clusterNodes[iMin2].m_parent = node;
        }
        if (distanceIsBranchLength) {
            node.setLength(fDist1, fDist2);
        } else {
            node.setHeight(fDist1, fDist2);
        }
        clusterNodes[iMin1] = node;
    } // merge

    /**
     * calculate distance the first time when setting up the distance matrix *
     */
    double getDistance0(final List<Integer> cluster1, final List<Integer> cluster2) {
        double fBestDist = Double.MAX_VALUE;
        switch (nLinkType) {
            case single:
            case neighborjoining:
            case neighborjoining2:
            case centroid:
            case complete:
            case adjcomplete:
            case average:
            case mean:
                // set up two instances for distance function
                fBestDist = distance(cluster1.get(0), cluster2.get(0));
                break;
            case ward: {
                // finds the distance of the change in caused by merging the cluster.
                // The information of a cluster is calculated as the error sum of squares of the
                // centroids of the cluster and its members.
                final double ESS1 = calcESS(cluster1);
                final double ESS2 = calcESS(cluster2);
                final List<Integer> merged = new ArrayList<Integer>();
                merged.addAll(cluster1);
                merged.addAll(cluster2);
                final double ESS = calcESS(merged);
                fBestDist = ESS * merged.size() - ESS1 * cluster1.size() - ESS2 * cluster2.size();
            }
            break;
        }
        return fBestDist;
    } // getDistance0

    /**
     * calculate the distance between two clusters
     *
     * @param cluster1 list of indices of instances in the first cluster
     * @param cluster2 dito for second cluster
     * @return distance between clusters based on link type
     */
    double getDistance(final double[][] fDistance, final List<Integer> cluster1, final List<Integer> cluster2) {
        double fBestDist = Double.MAX_VALUE;
        switch (nLinkType) {
            case single:
                // find single link distance aka minimum link, which is the closest distance between
                // any item in cluster1 and any item in cluster2
                fBestDist = Double.MAX_VALUE;
                for (int i = 0; i < cluster1.size(); i++) {
                    final int i1 = cluster1.get(i);
                    for (int j = 0; j < cluster2.size(); j++) {
                        final int i2 = cluster2.get(j);
                        final double fDist = fDistance[i1][i2];
                        if (fBestDist > fDist) {
                            fBestDist = fDist;
                        }
                    }
                }
                break;
            case complete:
            case adjcomplete:
                // find complete link distance aka maximum link, which is the largest distance between
                // any item in cluster1 and any item in cluster2
                fBestDist = 0;
                for (int i = 0; i < cluster1.size(); i++) {
                    final int i1 = cluster1.get(i);
                    for (int j = 0; j < cluster2.size(); j++) {
                        final int i2 = cluster2.get(j);
                        final double fDist = fDistance[i1][i2];
                        if (fBestDist < fDist) {
                            fBestDist = fDist;
                        }
                    }
                }
                if (nLinkType == Type.complete) {
                    break;
                }
                // calculate adjustment, which is the largest within cluster distance
                double fMaxDist = 0;
                for (int i = 0; i < cluster1.size(); i++) {
                    final int i1 = cluster1.get(i);
                    for (int j = i + 1; j < cluster1.size(); j++) {
                        final int i2 = cluster1.get(j);
                        final double fDist = fDistance[i1][i2];
                        if (fMaxDist < fDist) {
                            fMaxDist = fDist;
                        }
                    }
                }
                for (int i = 0; i < cluster2.size(); i++) {
                    final int i1 = cluster2.get(i);
                    for (int j = i + 1; j < cluster2.size(); j++) {
                        final int i2 = cluster2.get(j);
                        final double fDist = fDistance[i1][i2];
                        if (fMaxDist < fDist) {
                            fMaxDist = fDist;
                        }
                    }
                }
                fBestDist -= fMaxDist;
                break;
            case average:
                // finds average distance between the elements of the two clusters
                fBestDist = 0;
                for (int i = 0; i < cluster1.size(); i++) {
                    final int i1 = cluster1.get(i);
                    for (int j = 0; j < cluster2.size(); j++) {
                        final int i2 = cluster2.get(j);
                        fBestDist += fDistance[i1][i2];
                    }
                }
                fBestDist /= (cluster1.size() * cluster2.size());
                break;
            case mean: {
                // calculates the mean distance of a merged cluster (akak Group-average agglomerative clustering)
                final List<Integer> merged = new ArrayList<Integer>();
                merged.addAll(cluster1);
                merged.addAll(cluster2);
                fBestDist = 0;
                for (int i = 0; i < merged.size(); i++) {
                    final int i1 = merged.get(i);
                    for (int j = i + 1; j < merged.size(); j++) {
                        final int i2 = merged.get(j);
                        fBestDist += fDistance[i1][i2];
                    }
                }
                final int n = merged.size();
                fBestDist /= (n * (n - 1.0) / 2.0);
            }
            break;
            case centroid:
                // finds the distance of the centroids of the clusters
                final int nPatterns = dataInput.get().getPatternCount();
                final double[] centroid1 = new double[nPatterns];
                for (int i = 0; i < cluster1.size(); i++) {
                    final int iTaxon = cluster1.get(i);
                    for (int j = 0; j < nPatterns; j++) {
                        centroid1[j] += dataInput.get().getPattern(iTaxon, j);
                    }
                }
                final double[] centroid2 = new double[nPatterns];
                for (int i = 0; i < cluster2.size(); i++) {
                    final int iTaxon = cluster2.get(i);
                    for (int j = 0; j < nPatterns; j++) {
                        centroid2[j] += dataInput.get().getPattern(iTaxon, j);
                    }
                }
                for (int j = 0; j < nPatterns; j++) {
                    centroid1[j] /= cluster1.size();
                    centroid2[j] /= cluster2.size();
                }
                fBestDist = distance(centroid1, centroid2);
                break;
            case ward: {
                // finds the distance of the change in caused by merging the cluster.
                // The information of a cluster is calculated as the error sum of squares of the
                // centroids of the cluster and its members.
                final double ESS1 = calcESS(cluster1);
                final double ESS2 = calcESS(cluster2);
                final List<Integer> merged = new ArrayList<Integer>();
                merged.addAll(cluster1);
                merged.addAll(cluster2);
                final double ESS = calcESS(merged);
                fBestDist = ESS * merged.size() - ESS1 * cluster1.size() - ESS2 * cluster2.size();
            }
            break;
        }
        return fBestDist;
    } // getDistance


    /**
     * calculated error sum-of-squares for instances wrt centroid *
     */
    double calcESS(final List<Integer> cluster) {
        final int nPatterns = dataInput.get().getPatternCount();
        final double[] centroid = new double[nPatterns];
        for (int i = 0; i < cluster.size(); i++) {
            final int iTaxon = cluster.get(i);
            for (int j = 0; j < nPatterns; j++) {
                centroid[j] += dataInput.get().getPattern(iTaxon, j);
            }
        }
        for (int j = 0; j < nPatterns; j++) {
            centroid[j] /= cluster.size();
        }
        // set up two instances for distance function
        double fESS = 0;
        for (int i = 0; i < cluster.size(); i++) {
            final double[] instance = new double[nPatterns];
            final int iTaxon = cluster.get(i);
            for (int j = 0; j < nPatterns; j++) {
                instance[j] += dataInput.get().getPattern(iTaxon, j);
            }
            fESS += distance(centroid, instance);
        }
        return fESS / cluster.size();
    } // calcESS

    @Override
    public void initStateNodes() {
        if (m_initial.get() != null) {
            m_initial.get().assignFromWithoutID(this);
        }
    }

    @Override
    public void getInitialisedStateNodes(final List<StateNode> stateNodes) {
        if (m_initial.get() != null) {
            stateNodes.add(m_initial.get());
        }
    }

} // class ClusterTree
