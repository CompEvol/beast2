
/*
 * File LineageCountCalculator.java
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
package snap.likelihood;

import java.util.Arrays;

import snap.NodeData;
import snap.matrix.*;

/** calculates lineage count probabilities **/
public class LineageCountCalculator {

	/**
	 Computes the lineage count (num lineages, no colours) probabilities for all internal nodes.
	 @param phylo<NodeData>& beast.tree  The species beast.tree
	 @param const vector<uint>& sampleSizes Array of sample sizes (n_0) for each taxon id.
	 */
	public static void computeCountProbabilities(NodeData tree, int [] sampleSizes, boolean dprint /*= false*/) throws Exception {

		//Post-order traversal
		if (tree.isLeaf()) {
			doCountProbabilitiesForLeaf(tree, sampleSizes[tree.getNr()], dprint);
		} else if (tree.getNrOfChildren() == 1) {
			NodeData p = tree.getChild(0);
			computeCountProbabilities(p, sampleSizes, dprint);
			doCountProbabilitiesForInternal(tree, p,dprint);  //Node has a single child.
		} else { // assume two children
			computeCountProbabilities(tree.getChild(0), sampleSizes, dprint);
			computeCountProbabilities(tree.getChild(1), sampleSizes, dprint);
			doCountProbabilitiesForInternal(tree, tree.getChild(0), tree.getChild(1), dprint);
		}
	} // computeCountProbabilities

	/**
	 Compute probabilities for the numbers of lineages at a leaf.
	 **/
	static void doCountProbabilitiesForLeaf(NodeData node, int nSamples, boolean dprint)
	{
		/*
		int nTaxonID = node.m_tree.getTaxonIndex(node.m_tree.getNodeTaxon(node.m_nodeRef));
		if (nTaxonID != node.getTaxonID()) {
			int h =3;
			h++;
		}
		int n = new Integer((String)node.m_tree.getTaxon(nTaxonID).getAttribute("totalcount"));
		if (n != nSamples) {
			int h =3;
			h++;

		}
		*/
		node.m_n = nSamples;
		node.resize(nSamples);
		node.m_Nb[nSamples] = 1.0;
		if( dprint ) {
			System.out.println(node.getNr() + " n=" + node.m_n + " u1.Nb " + Arrays.toString(node.m_Nb));
		}
	}

	/**
	 Compute probabilities for the numbers of lineages at a node with a single child.
	 **/
	static void doCountProbabilitiesForInternal(NodeData v, NodeData child, boolean dprint) throws Exception {
		v.resize(child.m_n);
		double g1 = child.gamma();
		double t1 = child.t();

		if( dprint ) {
			System.out.println("g1,t1 " + g1 + " " + t1 );
		}
		child.m_Nt = computeTavareProbs(g1*t1, child.m_Nb);
		if( dprint ) {
			System.out.println("u1.Nt " + Arrays.toString(child.m_Nt));
		}
		System.arraycopy(child.m_Nt, 0, v.m_Nb, 0, child.m_Nt.length);
	} // doCountProbabilitiesForInternal


	/**
	 Compute probabilities for the numbers of lineages at a node with two children.
	 **/
	static void doCountProbabilitiesForInternal(NodeData node, NodeData u1, NodeData u2, boolean dprint)throws Exception {
		int n1 = u1.m_n;
		int n2 = u2.m_n;
		int n = n1+n2;
		node.resize(n);


		double g1 = u1.gamma();
		double g2 = u2.gamma();
		double t1 = u1.t();
		double t2 = u2.t();


		u1.m_Nt = computeTavareProbs(g1*t1, u1.m_Nb);
		u2.m_Nt = computeTavareProbs(g2*t2, u2.m_Nb);

		if( dprint ) {
			double sum = 0.0;
			System.out.println("g1,t1 " + g1 + " " + t1 + " g2,t2 " + g2 + " " + t2);
			sum = 0.0;	for (int i = 0; i < u1.m_Nb.length; i++) {sum += u1.m_Nb[i];}
			System.out.println(u1.getNr() + " u1.Nb " + u1.m_Nb.length + " sum="+sum + " " + Arrays.toString(u1.m_Nb));
			sum = 0.0;	for (int i = 0; i < u1.m_Nt.length; i++) {sum += u1.m_Nt[i];}
			System.out.println(u1.getNr() + " u1.Nt " + u1.m_Nt.length + " sum="+sum + " " + Arrays.toString(u1.m_Nt));
			sum = 0.0;	for (int i = 0; i < u2.m_Nb.length; i++) {sum += u2.m_Nb[i];}
			System.out.println(u2.getNr() + " u2.Nb " + u2.m_Nb.length + " sum="+sum + " " + Arrays.toString(u2.m_Nb));
			sum = 0.0;	for (int i = 0; i < u2.m_Nt.length; i++) {sum += u2.m_Nt[i];}
			System.out.println(u2.getNr() + " u2.Nt " + u2.m_Nt.length + " sum="+sum + " " + Arrays.toString(u2.m_Nt));
		}

		//std::fill(Nb.begin(), Nb.end(), 0.0);
		//node.Nb = new double[n1+n2+1];
		for(int i1 = 1;i1<=n1;i1++) {
			for(int i2 = 1;i2<=n2;i2++) {
				node.m_Nb[i1+i2] += u1.m_Nt[i1]*u2.m_Nt[i2];
			}
		}
		if( dprint ) {
			double sum = 0.0;for (int i = 0; i < node.m_Nb.length; i++) {sum += node.m_Nb[i];}
			System.out.println(node.getNr() + " node.Nb " + node.m_Nb.length + " sum="+sum + " " +  Arrays.toString(node.m_Nb));
		}
	} // doCountProbabilitiesForInternal

	/**
	 takes a vector x of lineage probabilities for the base of the branch.
	 evaluates y = exp(H^T t) x
	 where H is the 'tavare' matrix for the coalescent probabilities.
	 Hence,
	 y(i) = \sum_j P(i lineages at top of branch | j lineages at bottom) x(j)

	 **/
	static double[] computeTavareProbs(double t,double[] x) throws Exception {

		double[] y = new double [x.length];
		//int cutoff = 10;

		//Determine n so that all non-zero entries of x are in [1,n]
		int n = 0;
		for(int i=0;i<x.length;i++)
			if (x[i]>0.0)
				n=i;

		//When n is a lot less than the vector size, it is more efficient to construct a smaller
		//vector, compute using that, then copy back in.
//		if (n<x.length-1 - cutoff) {
//		Vector<Double> xcopy = new Vector<Double>(n+1);
//		xcopy.add(0.0);
//		Vector<Double> ycopy;
//		for (int i = 0; i < n; i++) {
//			xcopy.add(x[i+1]);
//		}
		TavareMatrix tavMatrix = new TavareMatrix(n);
//		ycopy = MatrixExponentiator.expmv(t, tavMatrix, xcopy);
//		for (int i = 0; i < n; i++) {
//			y[i+1] = ycopy.elementAt(i+1);
//		}

		double [] xcopy = new double[n+1];
		System.arraycopy(x, 1, xcopy, 1, n);
		double [] ycopy = MatrixExponentiator.expmv(t, tavMatrix, xcopy);
		System.arraycopy(ycopy, 1, y, 1, n);

/*
		} else {
			TavareMatrix tavMatrix(x.length-1);
			Vector<Double> ycopy = tavMatrix.expmv(t, tavMatrix, x, 1e-6);
			Arrays.fill(y, 0.0);
			System.arraycopy(ycopy.toArray(), 0, y, 0, n+1);
			expmv(t, tavMatrix, x, y);
		}
*/
		return y;
	} // computeTavareProbs

	public static void main(String [] args) {
		try {
			double g = 0.01;
			double t = 1.188;
			double x[] = {0.0, 0.0, 4.495591095311436E-68, 8.947436264972114E-64, 6.967349831140247E-60, 2.9013143952105373E-56, 7.416976127936963E-53, 1.263144949574017E-49, 1.5132162263960989E-46, 1.3255696419683216E-43, 8.739531128937978E-41, 4.4337293813047077E-38, 1.7610529662942642E-35, 5.5523138243784144E-33, 1.4049391683242938E-30, 2.8784737779500347E-28, 4.809056286332095E-26, 6.588420690066372E-24, 7.43376182614896E-22, 6.930086117375238E-20, 5.349633497076463E-18, 3.4236877324289615E-16, 1.817002800761371E-14, 7.990464307103342E-13, 2.906161313945856E-11, 8.713918096994015E-10, 2.1438392657981754E-8, 4.298797382911207E-7, 6.960808767471294E-6, 8.986607087199268E-5, 9.087369270483264E-4, 0.007016620131895993, 0.03981559491629833, 0.15604392374479695, 0.37632755437280535, 0.4197902908081144};

			double sum = 0.0;
			for(int i = 0; i < 36; i++) {
				sum += x[i];
			}
			System.out.println("sum x = " + sum);

			double [] y = computeTavareProbs(g*t, x);
			sum = 0.0;
			for(int i = 0; i < 36; i++) {
				sum += y[i];
				System.out.print(" " + y[i]);
			}
			System.out.println("\nsum y = " + sum);
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // main

} // class CountProbabilitiesCalculator
