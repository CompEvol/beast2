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

package test.beast.evolution.tree;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.RandomTree;
import beast.base.evolution.tree.TraitSet;
import beast.base.evolution.tree.coalescent.ConstantPopulation;
import beast.base.evolution.tree.coalescent.TreeIntervals;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class RandomTreeTest {
    
    public RandomTreeTest() { }

     @Test
     public void testCoalescentTimes() throws Exception {
         
         Randomizer.setSeed(53);

         int Nleaves = 10;
         int Niter = 5000;

         // (Serially sampled) coalescent time means and variances
         // estimated from 50000 trees simulated using MASTER

         double[] coalTimeMeansTruth = {
             1.754662,
             2.833337,
             3.843532,
             4.850805,
             5.849542,
             6.847016,
             7.8482,
             8.855137,
             10.15442};
         
         double[] coalTimeVarsTruth = {
             0.2751625,
             0.2727121,
             0.2685172,
             0.2705117,
             0.2678611,
             0.2671793,
             0.2686952,
             0.2828477,
             1.076874};
         
         
         // Assemble BEASTObjects needed by RandomTree
         
         StringBuilder traitSB = new StringBuilder();
         List<Sequence> seqList = new ArrayList<Sequence>();

         for (int i=0; i<Nleaves; i++) {
             String taxonID = "t " + i;
             seqList.add(new Sequence(taxonID, "?"));
             
             if (i>0)
                 traitSB.append(",");
             traitSB.append(taxonID).append("=").append(i);
         }

         Alignment alignment = new Alignment(seqList, "nucleotide");
         TaxonSet taxonSet = new TaxonSet(alignment);
         TraitSet timeTrait = new TraitSet();

         timeTrait.initByName(
                 "traitname", "date-backward",
                 "taxa", taxonSet,
                 "value", traitSB.toString());
         
         ConstantPopulation popFunc = new ConstantPopulation();
         popFunc.initByName("popSize", new RealParameter("1.0"));
         
         
         // Create RandomTree and TreeInterval instances
         RandomTree tree = new RandomTree();
         TreeIntervals intervals = new TreeIntervals();                  

         // Estimate coalescence time moments
         
         double[] coalTimeMeans = new double[Nleaves-1];
         double[] coalTimeVars = new double[Nleaves-1];
         double[] coalTimes = new double[Nleaves-1];

         for (int i=0; i<Niter; i++) {
             tree.initByName(
                     "taxa", alignment,
                     "populationModel", popFunc,
                     "trait", timeTrait);


             intervals.initByName("tree", tree);
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


         // Test means and variances against independently estimated values
         for (int j=0; j<Nleaves-1; j++) {
//             System.out.format("%d %g %g\n", j,
//                     relError(coalTimeMeans[j],coalTimeMeansTruth[j]),
//                     relError(coalTimeVars[j],coalTimeVarsTruth[j]));
             
             assert(relError(coalTimeMeans[j],coalTimeMeansTruth[j]) < 5e-3);
             assert(relError(coalTimeVars[j],coalTimeVarsTruth[j]) < 5e-2);
         }
     }
     
     /**
      * Return the relative difference between val and truth.
      * 
      * @param val
      * @param truth
      * @return relative error
      */
     private double relError(double val, double truth) {
         return 2.0*Math.abs(val-truth)/(val+truth);
     }
}
