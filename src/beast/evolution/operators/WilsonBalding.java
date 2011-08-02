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
    public void initAndValidate() {
    }

    /**
     * WARNING: Assumes strictly bifurcating beast.tree.
     */
    /** override this for proposals,
	 * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted **/
    @Override
    public double proposal() {
        Tree tree = m_tree.get(this);

        double oldMinAge, newMinAge, newRange, oldRange, newAge, fHastingsRatio;

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
        }

        if (jP.getNr() == iP.getNr() || j.getNr() == iP.getNr() || jP.getNr() == i.getNr())
            return Double.NEGATIVE_INFINITY;

        final Node CiP = getOtherChild(iP, i);

        Node PiP = iP.getParent();

        newMinAge = Math.max(i.getHeight(), j.getHeight());
        newRange = jP.getHeight() - newMinAge;
        newAge = newMinAge + (Randomizer.nextDouble() * newRange);
        oldMinAge = Math.max(i.getHeight(), CiP.getHeight());
        oldRange = PiP.getHeight() - oldMinAge;
        fHastingsRatio = newRange / Math.abs(oldRange);
        
        if (oldRange == 0 || newRange == 0) {
        	// This happens when some branch lengths are zero.
        	// If oldRange = 0, fHastingsRatio == Double.POSITIVE_INFINITY and 
        	// node i can be catapulted anywhere in the tree, resulting in 
        	// very bad trees that are always accepted.
        	// For symmetry, newRange = 0 should therefore be ruled out as well
            return Double.NEGATIVE_INFINITY;
        }

        //update
        if (j.isRoot()) {
            // 1. remove edges <iP, CiP>
            // 2. add edges <k, iP>, <iP, j>, <PiP, CiP>

            replace(iP, CiP, j);
            replace(PiP, iP, CiP);

            // iP is the new root
            iP.setParent(null);
            tree.setRoot(iP);

        } else if (iP.isRoot()) {
            // 1. remove edges <k, j>, <iP, CiP>, <PiP, iP>
            // 2. add edges <k, iP>, <iP, j>, <PiP, CiP>

        	replace(jP, j, iP);
            //replace(iP, CiP, iP);
            replace(iP, CiP, j);

            // CiP is the new root
            CiP.setParent(null);
            tree.setRoot(CiP);

        } else {
            // 1. remove edges <k, j>, <iP, CiP>, <PiP, iP>
            // 2. add edges <k, iP>, <iP, j>, <PiP, CiP>

        	// disconnect iP
            replace(iP.getParent(), iP, CiP);
            // re-attach, first child node to iP
            replace(iP, CiP, j);
            // then parent node of j to iP
            replace(jP, j, iP);
        }

        iP.setHeight(newAge);

        return Math.log(fHastingsRatio);
    }


} // class WilsonBalding
