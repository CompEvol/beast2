
/*
 * File GammaParameter.java
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
package snap;


import beast.core.Description;
import beast.core.Input;
import beast.core.Node;
import beast.core.Parameter;
import beast.core.State;
import beast.core.Tree;

@Description("Represents population size associated with each node in a beast.tree.")
public class GammaParameter extends Parameter {
	public Input<Tree> m_pTree = new Input<Tree>("tree", "beast.tree associated with this (array) parameter");
	public Input<Boolean> m_bInitFromTree = new Input<Boolean>("initFromTree", "whether to initialize from starting beast.tree values (if true), or vice versa (if false)");
	public Input<String> m_pPattern = new Input<String>("pattern", "pattern of metadata element associated with this parameter in the beast.tree");

	public GammaParameter() {
	}


	@Override
	public void initAndValidate(State _data) throws Exception {
		Tree tree = m_pTree.get();

		if (m_bInitFromTree.get() == true) {
			m_values = new double[tree.getNodeCount()];
			tree.getMetaData(tree.getRoot(), m_values, m_pPattern.get());
			m_nDimension.setValue(new Integer(m_values.length), this);
		} else {
			m_values = new double[tree.getNodeCount()];
			m_nDimension.setValue(new Integer(m_values.length), this);
			for (int i = 0; i < m_values.length; i++) {
				m_values[i] = (Double) m_pValues.get();
			}
			tree.setMetaData(tree.getRoot(), m_values, m_pPattern.get());
		}
	}

	@Override
    public Parameter copy() {
    	GammaParameter copy = new GammaParameter();
    	copy.m_sID = m_sID;
    	copy.m_values = new double[m_values.length];
    	System.arraycopy(m_values, 0, copy.m_values, 0, m_values.length);
    	copy.m_fLower = m_fLower;
    	copy.m_fUpper = m_fUpper;
    	copy.m_nParamNr = m_nParamNr;
    	copy.m_pTree = m_pTree;
    	copy.m_pPattern = m_pPattern;
    	return copy;
    }

	@Override
    public void prepare() throws Exception {
		syncTree(m_pTree.get().getRoot(), m_values, m_pPattern.get());
	}

	void syncTree(Node node, double [] fValues, String sPattern) {
		node.setMetaData(sPattern, fValues[Math.abs(node.getNr())]);
		if (!node.isLeaf()) {
			syncTree(node.m_left, fValues, sPattern);
			syncTree(node.m_right, fValues, sPattern);
		}
	}

    public String toString() {
    	StringBuffer buf = new StringBuffer();
    	buf.append(m_sID);
    	buf.append(": ");
    	for (int i = 0; i < m_values.length; i++) {
    		buf.append(m_values[i] + " ");
    	}
		if (m_pTree != null) {
			buf.append("Associated with " + m_pPattern.get() + " for beast.tree '" + m_pTree.get().getID() + "'");
		}
    	return buf.toString();
    }
}
