
/*
 * File NodeData.java
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


import java.io.Serializable;

import beast.core.Description;
import beast.core.Node;


@Description("This node has population parameter (gamma=2/theta) " +
		"and some specific members for performing SnAP (SNP and AFLP) " +
		"analysis.")
public class NodeData extends Node implements Serializable  {
	private static final long serialVersionUID = 1L;

	private	double m_fGamma;

	//Number of individuals at or below this node.
	public	int m_n;
	//Number of species at or below this node
	//public int nspecies;
	//public int mintaxa; //Minimum id taxa below this node.
	//Vector of probabilities for the number of ancestral lineages at this node.
	public double [] m_Nt;
	public double [] m_Nb;
	//This is the likelihood multiplied by the lineage probabilities Pr(Ry | n,r ) x Pr(n)  in the paper.
	FMatrix m_Fb;
	public FMatrix getFb() {return m_Fb;}
	public void initFb(int n, int nReds) {
		m_Fb = new FMatrix(n, nReds);
	}
	public void initFb(int n, double [] FAsArray) {
		m_Fb = new FMatrix(n, FAsArray);
	}
	public void initFb(FMatrix F) {
		m_Fb = F;
	}
	FMatrix m_Ft;
	public FMatrix getFt() {return m_Ft;}
	public void initFt(FMatrix F) {
		m_Ft = F;
	}
	//public double [][] F;
	//TODO: Change notation in the paper to fit with this.


	//height.... only used for debugging
	//public double height;

	/* NB: length comes from basic_newick beast.tree **/
	//public double length;

	/** Tree related methods **/
	//SSSTreeLikelihood m_tree;
	public NodeData getChild(int i) {
		if (i == 0) {
			return (NodeData) m_left;
		} else if (i== 1) {
			return (NodeData) m_right;
		}
		return null;
	}
	public int getNrOfChildren() {
		if (m_left == null) {
			return 0;
		}
		if (m_right == null) {
			return 1;
		}
		return 2;
	}

	public NodeData() {
		m_n = -1;
		//cerr<<"YYY\t\tallocating\t"<<this<<endl;
		//m_children = new Vector<NodeData>();
		m_Fb = new FMatrix();
		m_Ft = new FMatrix();
//		m_nTaxonID = 0;
	}

	public NodeData(int nmax) {
		m_n = nmax;
		//cerr<<"YYY\t\tallocating\t"<<this<<endl;
		//m_children = new Vector<NodeData>();
		m_Fb = new FMatrix();
		m_Ft = new FMatrix();
		resize(m_n);
//		m_nTaxonID = 0;
	}

	public void resize(int nmax) {
			m_n = nmax;
			if (nmax>=1) {
				m_Nt = new double[m_n+1];
				m_Nb = new double[m_n+1];
				m_Ft.resize(m_n);
				m_Fb.resize(m_n);
			}
		}

	public double t() {return getLength();}
	public double gamma() {return m_fGamma;}
//	public void set_t(double t) {m_fLength = t;}
	public void set_gamma(double gamma) {m_fGamma = gamma;}

	public NodeData copyx() throws CloneNotSupportedException {
		NodeData node = new NodeData();
		node.m_fHeight = m_fHeight;
		node.m_iLabel = m_iLabel;
		node.m_sMetaData = m_sMetaData;
		node.setParent(null);
		if (m_left != null) {
			node.m_left = ((NodeData)m_left).copyx();
			node.m_right = ((NodeData)m_right).copyx();
			node.m_left.setParent(node);
			node.m_right.setParent(node);
		}
		node.m_n = m_n;
		return node;
	}

	public NodeData copy() {
		NodeData node = new NodeData();
		node.m_fHeight = m_fHeight;
		node.m_iLabel = m_iLabel;
		node.m_sMetaData = m_sMetaData;

		node.set_gamma(m_fGamma);
		node.m_n = m_n;
		node.m_Nt = new double[m_Nt.length];
		System.arraycopy(m_Nt, 0, node.m_Nt, 0, m_Nt.length);
		node.m_Nb = new double[m_Nb.length];
		System.arraycopy(m_Nb, 0, node.m_Nb, 0, m_Nb.length);
//		node.Ft = new FMatrix(Ft);
//		node.Fb = new FMatrix(Fb);

		node.setParent(null);
		if (m_left != null) {
			NodeData left = ((NodeData)m_left).copy();
			NodeData right = ((NodeData)m_right).copy();
			node.m_left = left;
			node.m_right = right;
			left.setParent(node);
			right.setParent(node);
		}
		return node;
	}

	public String getNewickMetaData() {
		return "[gamma=" + gamma() + ']';
	}


//		return (NodeData) this.clone();
//			if (this!=data) {
//				data = (NodeData) this.clone();
//				/*
//				Phylib::basic_newick::copy(data);
//				resize(data.n);
//				nspecies = data.nspecies;
//				set_gamma(data.gamma());
//				std::copy(data.Nb.begin(),data.Nb.end(),Nb.begin());
//				std::copy(data.Nt.begin(),data.Nt.end(),Nt.begin());
//
//				for(uint i=0;i<F.size();i++)
//					std::copy(data.F[i].begin(),data.F[i].end(),F[i].begin());
//				*/
//			}
//		}


/*
		NodeData operator=(NodeData& data) {
			copy(data);
			return *this;
		}
*/





	public void resizeF(int n) {
		m_Fb.resize(n);
		m_Ft.resize(n);
	} // resizeF
	public FMatrix cloneFbx() {
		return new FMatrix(m_Fb);
	} // cloneF
	public FMatrix cloneFtx() {
		return new FMatrix(m_Ft);
	} // cloneF
	public void assignFbx(FMatrix _F) {
		m_Fb.assign(_F);
	} // cloneF
	public void assignFt(FMatrix _F) {
		m_Ft.assign(_F);
	} // cloneF

	int m_nCacheIDB;
	public int getCacheIDB() {
		return m_nCacheIDB;
	}
	public void setCacheIDB(int cacheIDB) {
		m_nCacheIDB = cacheIDB;
	}

	int m_nCacheIDT;
	public int getCacheIDT() {
		return m_nCacheIDT;
	}
	public void setCacheIDT(int cacheIDT) {
		m_nCacheIDT = cacheIDT;
	}

	/** used for lazy updating **/
	//boolean m_bIsDirty = true;

	public void setMetaData(String sPattern, double fValue) {
		if (sPattern.equals("gamma")) {
			m_fGamma = fValue;
		}
		if (sPattern.equals("theta")) {
			m_fGamma = 2.0/fValue;
		}
		super.setMetaData(sPattern, fValue);
	}

	public double getMetaData(String sPattern) {
		if (sPattern.equals("gamma")) {
			return m_fGamma;
		}
		if (sPattern.equals("theta")) {
			 return 2.0/m_fGamma;
		}
		return super.getMetaData(sPattern);
	}
}

