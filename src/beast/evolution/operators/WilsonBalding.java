/*
* File WilsonBalding.java
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
/*
 * WilsonBalding.java
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

package beast.evolution.operators;

import beast.core.Description;
import beast.core.State;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;

/**
 * WILSON, I. J. and D. J. BALDING, 1998  Genealogical inference from microsatellite data.
 * Genetics 150:499-51
 * http://www.genetics.org/cgi/ijlink?linkType=ABST&journalCode=genetics&resid=150/1/499
 */
@Description("Implements the unweighted Wilson-Balding branch swapping move. " +
        "This move is similar to one proposed by WILSON and BALDING 1998  " +
        "and involves removing a subtree and re-attaching it on a new parent branch. " +
        "See <a href='http://www.genetics.org/cgi/content/full/161/3/1307/F1'>picture</a>.")
public class WilsonBalding extends TreeOperator {

    @Override
    public void initAndValidate(State state) {
    }

    /**
     * WARNING: Assumes strictly bifurcating beast.tree.
     */
    @Override
    public double proposal() throws Exception {
        Tree tree = m_tree.get();//(Tree) state.getStateNode(m_tree);
//		calculateHeightsFromLengths(beast.tree);

        double oldMinAge, newMinAge, newRange, oldRange, newAge, fHastingsRatio;

        //Bchoose

        //for (int n =0; n < beast.tree.getNodeCount(); n++) {
        //	System.out.println(n + " " + ( (beast.tree.getNode(n) == null) ? "null" : beast.tree.getNode(n).getId()));
        //}

        // choose a random node avoiding root
        final int nodeCount = tree.getNodeCount();
        Node i;
        do {
            i = tree.getNode(Randomizer.nextInt(nodeCount));
        } while (i.isRoot());
        Node iP = i.getParent();

        // choose another random node to insert i above
        Node j;
        Node jP;

        // make sure that the target branch <k, j> is above the subtree being moved
        do {
            j = tree.getNode(Randomizer.nextInt(nodeCount));
            jP = j.getParent();
        } while ((jP != null && jP.getHeight() <= i.getHeight()) || (i.getNr() == j.getNr()));

        // disallow moves that change the root.
        if (j.isRoot() || iP.isRoot()) {
            return Double.NEGATIVE_INFINITY;
            //throw new Exception("Root changes not allowed!");
        }

        if (jP.getNr() == iP.getNr() || j.getNr() == iP.getNr() || jP.getNr() == i.getNr())
            return Double.NEGATIVE_INFINITY;
        //throw new Exception("move failed");

        final Node CiP = getOtherChild(iP, i);
//        if (CiP.getNr() == j.getNr()) {
//        	return Double.NEGATIVE_INFINITY;
//        }


        Node PiP = CiP.getParent();

        newMinAge = Math.max(i.getHeight(), j.getHeight());
        newRange = jP.getHeight() - newMinAge;
        newAge = newMinAge + (Randomizer.nextDouble() * newRange);
        oldMinAge = Math.max(i.getHeight(), CiP.getHeight());
        oldRange = PiP.getHeight() - oldMinAge;
        fHastingsRatio = newRange / Math.abs(oldRange);

        //update

        if (j.isRoot()) {

            replace(iP, CiP, j);
            replace(PiP, iP, CiP);
            // 1. remove edges <iP, CiP>
            //beast.tree.removeChild(parent, CiP);
            //beast.tree.removeChild(PiP, parent);

            // 2. add edges <k, iP>, <iP, j>, <PiP, CiP>
            //beast.tree.addChild(parent, j);
            //beast.tree.addChild(PiP, CiP);

            // iP is the new root
            iP.setParent(null);
            tree.setRoot(iP);

        } else if (iP.isRoot()) {
            replace(jP, j, iP);
            replace(iP, CiP, iP);
            // 1. remove edges <k, j>, <iP, CiP>, <PiP, iP>
            //beast.tree.removeChild(k, j);
            //beast.tree.removeChild(parent, CiP);

            // 2. add edges <k, iP>, <iP, j>, <PiP, CiP>
            //beast.tree.addChild(parent, j);
            //beast.tree.addChild(k, parent);

            //CiP is the new root
            CiP.setParent(null);
            tree.setRoot(CiP);

        } else {
            // disconnect iP
            replace(iP.getParent(), iP, CiP);
            // re-attach, first child node to iP
            replace(iP, CiP, j);
            // then parent node of j to iP
            replace(jP, j, iP);


            // 1. remove edges <k, j>, <iP, CiP>, <PiP, iP>
            //beast.tree.removeChild(k, j);
            //beast.tree.removeChild(parent, CiP);
            //beast.tree.removeChild(PiP, parent);

            // 2. add edges <k, iP>, <iP, j>, <PiP, CiP>
            //beast.tree.addChild(parent, j);
            //beast.tree.addChild(k, parent);
            //beast.tree.addChild(PiP, CiP);
        }

        iP.setHeight(newAge);

//        if (beast.tree.getExternalNodeCount() != tipCount) {
//            int newCount = beast.tree.getExternalNodeCount();
//            throw new RuntimeException("Lost some tips in modified SPR! (" +
//                    tipCount + "-> " + newCount + ")");
//        }
//       	setLengthsFromHeights(beast.tree.getRoot());
        return Math.log(fHastingsRatio);
    }


} // class WilsonBalding
