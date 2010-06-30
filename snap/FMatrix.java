
/*
 * File FMatrix.java
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

import java.util.Arrays;

/** The FMatrix contains a site distribution for a node.
 *  This is the likelihood multiplied by the lineage probabilities Pr(Ry | n,r ) x Pr(n)  in the paper.
 *  **/
public class FMatrix {
	int m_n;
	private double [] F;
	int getSize() {
		return m_n;
	}
	public double get(int n, int r) {return F[n*(n+1)/2-1+r];}
	private void set(int n, int r, double f) {F[n*(n+1)/2-1+r] = f;}
//	private void mul(int n, int r, double f) {F[n*(n+1)/2-1+r] *= f;}
//	private void add(int n, int r, double f) {F[n*(n+1)/2-1+r] += f;}
//	private void setZero(int n) {Arrays.fill(F, n*(n+1)/2-1, (n+1)*(n+2)/2-1, 0.0);} 
//	private void setZero() {Arrays.fill(F, 0.0);} 

	public double [] asVectorCopy() {
		double [] copy = new double[F.length];
		System.arraycopy(F,0,copy,0,F.length);
		return copy;
	}
	public double [] asVectorCopyBase1() {
		double [] copy = new double[F.length + 1];
		System.arraycopy(F,0,copy,1,F.length);
		return copy;
	}
	public double [] asVector() {return F;}
	public FMatrix() {
	}

	public FMatrix(int n) {
		resize(n);
	}
	
	public FMatrix(FMatrix other) {
		assign(other);
	} // c'tor
	
	/** construct a leaf likelihood **/
	public FMatrix(int n, int nReds) {
		resize(n);
		set(n, nReds, 1.0);
	} // c'tor

	/** construct a top-of-branch likelihood **/
	public FMatrix(int n, double []_F) {
		m_n = n;
		F = _F;
	} // c'tor
	
	public void assign(double [][] other) {
		for(int i=1;i<=m_n;i++) {
			System.arraycopy(other[i], 0, F , i*(i+1)/2-1 , i+1);
		}
	} // copy
	
	void resize(int n) {
		if (F != null && getSize() == n) {
			// no need to resize, just init to zero
			Arrays.fill(F, 0);
		}
		m_n = n;
		F = new double[(n+1)*(n+2)/2-1];
	} // resize
	
	void rawresize(int n) {
		if (F != null && getSize() == n) {
			return;
		}
		m_n = n;
		F = new double[(n+1)*(n+2)/2-1];
	} // resize
	

	public void assign(FMatrix other) {
		int n = other.getSize();
		rawresize(n);
		if (getSize() != other.getSize()) {
			System.err.println("diff in length " + getSize() +"!="+ other.getSize());
		}
		System.arraycopy(other.F, 0, F , 0 , F.length);
	} // assign
	
	public String toString() {
		int n = getSize();
		StringBuffer buf = new StringBuffer(); 
		for(int i=1;i<=n;i++) {
			for(int r=0;r<=i;r++) {
				buf.append(get(i,r)+" ");
			}
			buf.append(';');
		} 
		return buf.toString();
	} // toString
} // class FMatrix
