/*
* File Uniform.java
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
 * UniformOperator.java
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
 * A generic uniform sampler/operator for use with a multi-dimensional parameter.
 */
@Description("A generic uniform sampler/operator for use with a multi-dimensional parameter.")
public class Uniform extends TreeOperator {

//	public Input<Tree> m_tree = new Input<Tree>("beast.tree","beast.tree on which a uniform operation is performed");


    @Override
    public void initAndValidate(State state) {
    }

    /**
     * change the parameter and return the hastings ratio.
     */
    @Override
    public double proposal() throws Exception {
        Tree tree = m_tree.get(this);//(Tree) state.getStateNode(m_tree);

        // randomly select internal node
        int nNodeCount = tree.getNodeCount();
        Node node;
        do {
            final int iNodeNr = nNodeCount / 2 + 1 + Randomizer.nextInt(nNodeCount / 2);
            node = tree.getNode(iNodeNr);
        } while (node.isRoot() || node.isLeaf());
        double fUpper = node.getParent().getHeight();
        double fLower = Math.max(node.m_left.getHeight(), node.m_right.getHeight());
        final double newValue = (Randomizer.nextDouble() * (fUpper - fLower)) + fLower;
        node.setHeight(newValue);


//        // find how much the height can be shifted around
//        final double fChildRoom = Math.max(-node.m_left.m_fLength, -node.m_right.m_fLength);
//        final double fNodeRoom = node.m_fLength;
//        final double fDeltaLength = (Randomizer.nextDouble() * (fNodeRoom - fChildRoom)) + fChildRoom;
//
//        // apply height shift
//        if (node.isRoot()) {
//        	node.setLength(0);
//        } else {
//        	node.addLength(-fDeltaLength);
//        }
//        node.m_left.addLength(fDeltaLength);
//        node.m_right.addLength(fDeltaLength);

        return 0.0;
    }

}
