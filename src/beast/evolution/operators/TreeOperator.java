/*
* File TreeOperator.java
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
package beast.evolution.operators;


import beast.core.Description;

import beast.core.Input;
import beast.evolution.tree.Node;
import beast.core.Operator;
import beast.evolution.tree.Tree;
import beast.core.Input.Validate;

@Description("This operator changes a beast.tree.")
abstract public class TreeOperator extends Operator {
    public Input<Tree> m_tree = new Input<Tree>("tree", "beast.tree on which this operation is performed", Validate.REQUIRED);


    /**
     * @param parent the parent
     * @param child  the child that you want the sister of
     * @return the other child of the given parent.
     */
    protected Node getOtherChild(Node parent, Node child) {
        if (parent.getLeft().getNr() == child.getNr()) {
            return parent.getRight();
        } else {
            return parent.getLeft();
        }
    }

    /**
     * replace child with another node
     *
     * @param node
     * @param child
     * @param replacement
     */
    public void replace(Node node, Node child, Node replacement) {
    	node.removeChild(child);
    	node.addChild(replacement);
        node.makeDirty(Tree.IS_FILTHY);
        replacement.makeDirty(Tree.IS_FILTHY);
    }

}
