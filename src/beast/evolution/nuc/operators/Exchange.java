
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

package beast.evolution.nuc.operators;

import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;
import beast.core.Description;
import beast.core.Input;
import beast.core.State;


/*
 * KNOWN BUGS: WIDE operator cannot be used on trees with 4 or less tips!
 */
@Description(
 "Implements branch exchange operations. There is a NARROW and WIDE variety. "+
 "The narrow exchange is very similar to a rooted-beast.tree nearest-neighbour "+
 "interchange but with the restriction that node height must remain consistent.")
public class Exchange extends TreeOperator {
	//public Input<Tree> m_tree = new Input<Tree>("beast.tree","beast.tree on which an exchange operation is performed");
	public Input<Boolean> m_bIsNarrow = new Input<Boolean>("isNarrow","if true (default) a narrow exchange is performed, otherwise a wide exchange", new Boolean(true));

	@Override
	public void initAndValidate(State state) {
	}

	@Override
	public double proposal(State state) throws Exception {
		Tree tree = state.getTree(m_tree);
//		calculateHeightsFromLengths(beast.tree);


        double fLogHastingsRatio = 0;

        if (m_bIsNarrow.get()) {
        	fLogHastingsRatio = narrow(tree);
        } else {
        	fLogHastingsRatio = wide(tree);
        }

//        setLengthsFromHeights(beast.tree.getRoot());
        return fLogHastingsRatio;
    }

    /**
     * WARNING: Assumes strictly bifurcating beast.tree.
     */
    public double narrow(Tree tree) throws Exception {
        final int nNodes = tree.getNodeCount();
        final Node root = tree.getRoot();

        Node i = root;

        while( i.isRoot() || i.getParent().isRoot() ) {
            i = tree.getNode(Randomizer.nextInt(nNodes));
        }

        final Node iParent = i.getParent();
        final Node iGrandParent = iParent.getParent();
        Node iUncle = iGrandParent.m_left;
        if( iUncle.getNr() == iParent.getNr() ) {
            iUncle = iGrandParent.m_right;
        }
        assert iUncle == getOtherChild(iGrandParent, iParent);

        assert i.getHeight() <= iGrandParent.getHeight();

        if( iUncle.getHeight() < iParent.getHeight() ) {
            exchangeNodes(i, iUncle, iParent, iGrandParent);

            // exchangeNodes generates the events
            //beast.tree.pushTreeChangedEvent(iParent);
            //beast.tree.pushTreeChangedEvent(iGrandParent);
            return 0;
        } else {
        	return Double.NEGATIVE_INFINITY;
          //throw new Exception("Couldn't find valid narrow move on this beast.tree!!");
        }
    }

    /**
     * WARNING: Assumes strictly bifurcating beast.tree.
     */
    public double wide(Tree tree) throws Exception {

        final int nodeCount = tree.getNodeCount();
        final Node root = tree.getRoot();

        Node i = root;

        while( i.isRoot() ) {
            i = tree.getNode(Randomizer.nextInt(nodeCount));
        }

        Node j = i;
        while( j.getNr() == i.getNr() || j.isRoot()) {
            j = tree.getNode(Randomizer.nextInt(nodeCount));
        }

        final Node iP = i.getParent();
        final Node jP = j.getParent();

        if( (iP != jP) && (i != jP) && (j != iP)
                && (j.getHeight() < iP.getHeight())
                && (i.getHeight() < jP.getHeight()) ) {
            exchangeNodes(i, j, iP, jP);
            // System.out.println("tries = " + tries+1);
            return 0;
        }
    	return Double.NEGATIVE_INFINITY;
        //throw new Exception("Couldn't find valid wide move on this beast.tree!");
    }


	/* exchange sub-trees whose root are i and j */
	protected void exchangeNodes(Node i, Node j,
	                             Node iP, Node jP) throws Exception {
		// precondition iP -> i & jP -> j
		replace(iP, i, j);
		replace(jP, j, i);
		// postcondition iP -> j & iP -> i
	}
}
