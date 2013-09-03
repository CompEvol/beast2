/*
* File Exchange.java
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
 * ExchangeOperator.java
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
import beast.core.Input;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;


/*
 * KNOWN BUGS: WIDE operator cannot be used on trees with 4 or less tips!
 */

@Description("Implements branch exchange operations. There is a NARROW and WIDE variety. " +
        "The narrow exchange is very similar to a rooted-beast.tree nearest-neighbour " +
        "interchange but with the restriction that node height must remain consistent.")
public class Exchange extends TreeOperator {
    public Input<Boolean> isNarrowInput = new Input<Boolean>("isNarrow", "if true (default) a narrow exchange is performed, otherwise a wide exchange", true);

    @Override
    public void initAndValidate() {
    }

    /**
     * override this for proposals,
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {
        final Tree tree = treeInput.get(this);

        double fLogHastingsRatio = 0;

        if (isNarrowInput.get()) {
            fLogHastingsRatio = narrow(tree);
        } else {
            fLogHastingsRatio = wide(tree);
        }

        return fLogHastingsRatio;
    }

    private int isg(final Node n) {
      return (n.getLeft().isLeaf() && n.getRight().isLeaf()) ? 0 : 1;
    }

    private int sisg(final Node n) {
        return n.isLeaf() ? 0 : isg(n);
    }

    /**
     * WARNING: Assumes strictly bifurcating beast.tree.
     */
    public double narrow(final Tree tree) {

        if( true ) {
            //        Alternative implementation that has less risk of rejection due to
            //    	  selecting an invalid initial node
            //
            final int nInternalNodes = tree.getInternalNodeCount();
            if (nInternalNodes <= 1) {
                return Double.NEGATIVE_INFINITY;
            }

            Node iGrandParent = tree.getNode(nInternalNodes + 1 + Randomizer.nextInt(nInternalNodes));
            while (iGrandParent.getLeft().isLeaf() && iGrandParent.getRight().isLeaf()) {
                iGrandParent = tree.getNode(nInternalNodes + 1 + Randomizer.nextInt(nInternalNodes));
            }

            Node iParent = iGrandParent.getLeft();
            Node iUncle = iGrandParent.getRight();
            if (iParent.getHeight() < iUncle.getHeight()) {
                iParent = iGrandParent.getRight();
                iUncle = iGrandParent.getLeft();
            }

            if( iParent.isLeaf() ) {
                // tree with dated tips
                return Double.NEGATIVE_INFINITY;
            }

            int validGP = 0;
            {
                for(int i = nInternalNodes + 1; i < 1 + 2*nInternalNodes; ++i) {
                    validGP += isg(tree.getNode(i));
                }
            }

            final int c2 = sisg(iParent) + sisg(iUncle);

            final Node i = (Randomizer.nextBoolean() ? iParent.getLeft() : iParent.getRight());
            exchangeNodes(i, iUncle, iParent, iGrandParent);

            final int validGPafter = validGP - c2 + sisg(iParent) + sisg(iUncle);

            return Math.log((float)validGP/validGPafter);
        } else {

            final int nNodes = tree.getNodeCount();

            Node i = tree.getRoot();

            while (i.isRoot() || i.getParent().isRoot()) {
                i = tree.getNode(Randomizer.nextInt(nNodes));
            }

            final Node iParent = i.getParent();
            final Node iGrandParent = iParent.getParent();
            Node iUncle = iGrandParent.getLeft();
            if (iUncle.getNr() == iParent.getNr()) {
                iUncle = iGrandParent.getRight();
                assert (iUncle.getNr() != iParent.getNr());
            }
            assert iUncle == getOtherChild(iGrandParent, iParent);

            assert i.getHeight() <= iGrandParent.getHeight();

            if (//i.getHeight() < iUncle.getHeight() &&
                    iUncle.getHeight() < iParent.getHeight()) {
                exchangeNodes(i, iUncle, iParent, iGrandParent);
                return 0;
            } else {
                // Couldn't find valid narrow move on this beast.tree!!
                return Double.NEGATIVE_INFINITY;
            }
        }
    }

    /**
     * WARNING: Assumes strictly bifurcating beast.tree.
     * @param tree
     */
    public double wide(final Tree tree) {

        final int nodeCount = tree.getNodeCount();

        Node i = tree.getRoot();

        while (i.isRoot()) {
            i = tree.getNode(Randomizer.nextInt(nodeCount));
        }

        Node j = i;
        while (j.getNr() == i.getNr() || j.isRoot()) {
            j = tree.getNode(Randomizer.nextInt(nodeCount));
        }

        final Node iP = i.getParent();
        final Node jP = j.getParent();

        if ((iP != jP) && (i != jP) && (j != iP)
                && (j.getHeight() < iP.getHeight())
                && (i.getHeight() < jP.getHeight())
//                && ((iP.getHeight() < jP.getHeight() && i.getHeight() < j.getHeight()) ||
//                (iP.getHeight() > jP.getHeight() && i.getHeight() > j.getHeight()))
                ) {
            exchangeNodes(i, j, iP, jP);
            // System.out.println("tries = " + tries+1);
            return 0;
        }
        // Couldn't find valid wide move on this beast.tree!
        return Double.NEGATIVE_INFINITY;
    }


    /* exchange sub-trees whose root are i and j */

    protected void exchangeNodes(Node i, Node j,
                                 Node iP, Node jP) {
        // precondition iP -> i & jP -> j
        replace(iP, i, j);
        replace(jP, j, i);
        // postcondition iP -> j & iP -> i
    }
}
