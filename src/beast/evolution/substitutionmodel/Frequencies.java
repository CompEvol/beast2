/*
* File Frequencies.java
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
package beast.evolution.substitutionmodel;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;
import beast.evolution.alignment.Alignment;

@Description("Represents character frequencies typically used as distribution of the root of the tree. " +
        "Calculates empirical frequencies of characters in sequence data, or simply assumes a uniform " +
        "distribution if the estimate flag is set to false.")
public class Frequencies extends Plugin {
    public Input<Alignment> m_data = new Input<Alignment>("data", "Sequence data for which frequencies are calculated");
    public Input<String> m_fixed = new Input<String>("frequencies", "Fixed set of frequencies specified as space separated values summing to 1", Validate.XOR, m_data);
    public Input<Boolean> m_bEstimate = new Input<Boolean>("estimate", "Whether to estimate the frequencies from data (true=default) or assume a uniform distribution over characters (false)", true);

    @Override
    public void initAndValidate() throws Exception {
    	if (m_fixed.get() != null) {
        	// if user specified, parse frequencies from space delimited string
    		String [] sValues = m_fixed.get().split("\\s+");
            m_fFreqs = new double[sValues.length];
    		for (int i = 0; i < sValues.length; i++) {
    			m_fFreqs[i] = Double.parseDouble(sValues[i]);
    		}
    		// sanity check
    		double fSum = 0;
    		for (int i = 0; i < sValues.length; i++) {
    			fSum += m_fFreqs[i];
    		}
    		if (Math.abs(fSum-1.0)>1e-6) {
    			throw new Exception("Frequencies do not add up to 1");
    		}
    		return;
    	}

    	// if not user specified, either estimate from data or set as fixed
    	if (m_bEstimate.get()) {
    		// estimate
            calcFrequencies();
            checkFrequencies();
        } else {
    		// fixed
            int nStates = m_data.get().getMaxStateCount();
            m_fFreqs = new double[nStates];
            for (int i = 0; i < nStates; i++) {
                m_fFreqs[i] = 1.0 / nStates;
            }
        }
    }

    double[] m_fFreqs;

    public double[] getFreqs() {
        return m_fFreqs;
    }

    void calcFrequencies() {
        Alignment alignment = m_data.get();
        m_fFreqs = new double[alignment.getMaxStateCount()];
        for (int i = 0; i < alignment.getPatternCount(); i++) {
            int[] nPattern = alignment.getPattern(i);
            int nWeight = alignment.getPatternWeight(i);
            for (int iValue : nPattern) {
                if (iValue != m_fFreqs.length) { // ignore unknowns
                    m_fFreqs[iValue] += nWeight;
                }
            }
        }
        // normalize
        double fSum = 0;
        for (double f : m_fFreqs) {
            fSum += f;
        }
        for (int i = 0; i < m_fFreqs.length; i++) {
            m_fFreqs[i] /= fSum;
        }
    } // calcFrequencies

    /**
     * Ensures that frequencies are not smaller than MINFREQ and
     * that two frequencies differ by at least 2*MINFDIFF.
     * This avoids potential problems later when eigenvalues
     * are computed.
     */
    private void checkFrequencies() {
        // required frequency difference
        double MINFDIFF = 1.0E-10;

        // lower limit on frequency
        double MINFREQ = 1.0E-10;

        int maxi = 0;
        double sum = 0.0;
        double maxfreq = 0.0;
        for (int i = 0; i < m_fFreqs.length; i++) {
            double freq = m_fFreqs[i];
            if (freq < MINFREQ) m_fFreqs[i] = MINFREQ;
            if (freq > maxfreq) {
                maxfreq = freq;
                maxi = i;
            }
            sum += m_fFreqs[i];
        }
        double diff = 1.0 - sum;
        m_fFreqs[maxi] += diff;

        for (int i = 0; i < m_fFreqs.length - 1; i++) {
            for (int j = i + 1; j < m_fFreqs.length; j++) {
                if (m_fFreqs[i] == m_fFreqs[j]) {
                    m_fFreqs[i] += MINFDIFF;
                    m_fFreqs[j] += MINFDIFF;
                }
            }
        }
    }
} // class Frequencies
