
/*
 * File COMPLEX.java
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
package snap.likelihood;

public class COMPLEX {
	public double m_fRe;
	public double m_fIm;
	public COMPLEX() {
		m_fRe = 0;
		m_fIm = 0;
	}
	public COMPLEX(double fRe, double fIm) {
		m_fRe = fRe;
		m_fIm = fIm;
	}
	
	public void divide(COMPLEX cNumerator, double fDivisor) {
		m_fRe = cNumerator.m_fRe / fDivisor;
		m_fIm = cNumerator.m_fIm / fDivisor;
	}

//	public void times(COMPLEX cMultiplier1, COMPLEX cMultiplier2) {
//		m_fRe = cMultiplier2.m_fRe * cMultiplier1.m_fRe - cMultiplier2.m_fIm * cMultiplier1.m_fIm;
//		m_fIm = cMultiplier2.m_fRe * cMultiplier1.m_fIm + cMultiplier2.m_fIm * cMultiplier1.m_fRe;
//	}
	
//	public void timesadd(COMPLEX cMultiplier, COMPLEX cResult) {
//		cResult.m_fRe += m_fRe * cMultiplier.m_fRe - m_fIm * cMultiplier.m_fIm;
//		cResult.m_fIm += m_fRe * cMultiplier.m_fIm + m_fIm * cMultiplier.m_fRe;
//	}

	public void muladd(COMPLEX cMul1, COMPLEX cMul2) {
		m_fRe += cMul2.m_fRe * cMul1.m_fRe - cMul2.m_fIm * cMul1.m_fIm;
		m_fIm += cMul2.m_fRe * cMul1.m_fIm + cMul2.m_fIm * cMul1.m_fRe;
	}
	
	public void muladd(double f1, COMPLEX x1, double f2, COMPLEX x2 ) {
		m_fRe = f1 * x1.m_fRe + f2 * x2.m_fRe;
		m_fIm = f1 * x1.m_fIm + f2 * x2.m_fIm;
	}
	// calc
	public void divide(COMPLEX cNumerator, COMPLEX cDivisor) {
		double f = cDivisor.m_fRe * cDivisor.m_fRe + cDivisor.m_fIm * cDivisor.m_fIm; 
		m_fRe = (cNumerator.m_fRe * cDivisor.m_fRe + cNumerator.m_fIm * cDivisor.m_fIm) / f;
		m_fIm = (cNumerator.m_fIm * cDivisor.m_fRe - cNumerator.m_fRe * cDivisor.m_fIm) / f;
	}
	public void mul(COMPLEX cMul1, COMPLEX cMul2) {
		m_fRe = cMul2.m_fRe * cMul1.m_fRe - cMul2.m_fIm * cMul1.m_fIm;
		m_fIm = cMul2.m_fRe * cMul1.m_fIm + cMul2.m_fIm * cMul1.m_fRe;
	}
	public String toString() {
		return "(" +m_fRe + "," + m_fIm + ")";
	}
	
}
