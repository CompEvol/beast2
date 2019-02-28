/*
 * Copyright (C) 2014 Tim Vaughan <tgvaughan@gmail.com>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package test.beast.evolution.operator;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import beast.core.Operator;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.operators.Exchange;
import beast.evolution.operators.SubtreeSlide;
import beast.evolution.operators.WilsonBalding;
import beast.evolution.tree.Node;
import beast.evolution.tree.RandomTree;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;
import beast.evolution.tree.coalescent.ConstantPopulation;
import beast.evolution.tree.coalescent.TreeIntervals;
import beast.util.Randomizer;
import beast.util.TreeParser;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class TestNonBinaryTreeOperators extends TestCase {

     @Test
     public void testCoalescentTimes() throws Exception {
         
         Randomizer.setSeed(1);

         int Niter = 5000000;

         Tree treeState = new TreeParser("(((t1:4,t2:4):4,(t3:4,t4:4):4):4,((t5:4,t6:4):4,(t7:4,t8:4):4):4):4;");
         int Nleaves = treeState.getLeafNodeCount();

         TreeIntervals intervals = new TreeIntervals();                  
         intervals.initByName("tree", treeState);
         
         // Set up operators
         Operator wb = new WilsonBalding();
         wb.initByName("tree", treeState,
        		 "weight", "5");
         Operator ss = new SubtreeSlide();
         ss.initByName("tree", treeState,
        		 "weight", "5");
         Operator xw = new Exchange();
         xw.initByName("tree", treeState,
        		 "isNarrow", "false",
        		 "weight", "5");
         
         
         // Estimate coalescence time moments
         
         double[] coalTimeMeans = new double[Nleaves-1];
         double[] coalTimeVars = new double[Nleaves-1];
         double[] coalTimes = new double[Nleaves-1];
         
         for (int i=0; i<Niter; i++) {
        	 wb.proposal();
        	 xw.proposal();
         }
         for (int i=0; i<Niter; i++) {
        	 wb.proposal();
        	 xw.proposal();
        	 ss.proposal();
        	 treeState.scale(12.0/treeState.getRoot().getHeight());
        	 intervals.setIntervalsUnknown();
             intervals.getCoalescentTimes(coalTimes);
             
             for (int j=0; j<Nleaves-1; j++) {
                 coalTimeMeans[j] += coalTimes[j];
                 coalTimeVars[j] += coalTimes[j]*coalTimes[j];
             }
         }
         
         
         // Normalise means and variances
         for (int j=0; j<Nleaves-1; j++) {
             coalTimeMeans[j] /= Niter;
             coalTimeVars[j] /= Niter;
             coalTimeVars[j] -= coalTimeMeans[j]*coalTimeMeans[j];
         }

         treeState.assignFrom(
        		 new TreeParser("((((t1:2):2,t2:4):4,(t3:4,t4:4):4):4,((t5:4,t6:4):4,(t7:4,t8:4):4):4):4;"));

         // Estimate coalescence time moments
         
         double[] xcoalTimeMeans = new double[Nleaves-1];
         double[] xcoalTimeVars = new double[Nleaves-1];
         double[] xcoalTimes = new double[Nleaves-1];
         double xMeanUnitaryHeights = 0.0;

         Node unitary = treeState.getNode(8);
         
         System.out.println(treeState.getNode(8).getChildren().size());
         for (int i=0; i<Niter; i++) {
             wb.proposal();
             assert (unitary.getChildren().size() == 1);
             xw.proposal();
             assert (unitary.getChildren().size() == 1);
         }
         System.out.format("%s;\n", treeState.toString());
         for (int i=0; i<Niter; i++) {
        	 wb.proposal();
             assert (unitary.getChildren().size() == 1);
        	 xw.proposal();
             assert (unitary.getChildren().size() == 1);
        	 ss.proposal();
             assert (unitary.getChildren().size() == 1);
             double effectiveHeight = (treeState.getRoot() == unitary) ? (
            		 treeState.getRoot().getChild(0).getHeight()) : (
            				 treeState.getRoot().getHeight());
        	 treeState.scale(12.0/effectiveHeight);
        	 intervals.setIntervalsUnknown();
             intervals.getCoalescentTimes(xcoalTimes);

          
             for (int j=0; j<Nleaves-1; j++) {
                 xcoalTimeMeans[j] += xcoalTimes[j];
                 xcoalTimeVars[j] += xcoalTimes[j]*xcoalTimes[j];
             }
             xMeanUnitaryHeights += unitary.getHeight();
         }
         
         // Normalise means and variances
         for (int j=0; j<Nleaves-1; j++) {
             xcoalTimeMeans[j] /= Niter;
             xcoalTimeVars[j] /= Niter;
             xcoalTimeVars[j] -= xcoalTimeMeans[j]*xcoalTimeMeans[j];
         }
         xMeanUnitaryHeights /= Niter;

        
         // Test means and variances against independently estimated values
         System.out.format("%g\n", xMeanUnitaryHeights);
         for (int j=0; j<Nleaves-1; j++) {
        	 System.out.format("%d %g %g -- %g %g\n", j,
        			 coalTimeMeans[j], coalTimeVars[j],
                	 xcoalTimeMeans[j], xcoalTimeVars[j]);
         }
         Assert.assertArrayEquals(coalTimeMeans, xcoalTimeMeans, 5e-1);
     }
}
