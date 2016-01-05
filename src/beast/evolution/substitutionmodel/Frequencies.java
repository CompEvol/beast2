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

import java.util.Arrays;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.datatype.DataType;



// RRB: TODO: make this an interface?

@Description("Represents character frequencies typically used as distribution of the root of the tree. " +
        "Calculates empirical frequencies of characters in sequence data, or simply assumes a uniform " +
        "distribution if the estimate flag is set to false.")
public class Frequencies extends CalculationNode {
    final public Input<Alignment> dataInput = new Input<>("data", "Sequence data for which frequencies are calculated");
    final public Input<Boolean> estimateInput = new Input<>("estimate", "Whether to estimate the frequencies from data (true=default) or assume a uniform distribution over characters (false)", true);
    final public Input<RealParameter> frequenciesInput = new Input<>("frequencies", "A set of frequencies specified as space separated values summing to 1", Validate.XOR, dataInput);

    /**
     * contains frequency distribution *
     */
    protected double[] freqs;

    /**
     * flag to indicate m_fFreqs is up to date *
     */
    boolean needsUpdate;


    @Override
    public void initAndValidate() throws Exception {
        update();
        double fSum = getSumOfFrequencies(getFreqs());
        // sanity check
        if (Math.abs(fSum - 1.0) > 1e-6) {
            throw new IllegalArgumentException("Frequencies do not add up to 1");
        }

    }

    /**
     * return up to date frequencies *
     */
    public double[] getFreqs() {
        if (needsUpdate) {

            update();
        }

        return freqs;
    }

    /**
     * recalculate frequencies, unless it is fixed *
     */
    void update() {
        if (frequenciesInput.get() != null) {

            // if user specified, parse frequencies from space delimited string
            freqs = new double[frequenciesInput.get().getDimension()];

            for (int i = 0; i < freqs.length; i++) {
                freqs[i] = frequenciesInput.get().getValue(i);
            }


        } else if (estimateInput.get()) { // if not user specified, either estimate from data or set as fixed
            // estimate
            estimateFrequencies();
            checkFrequencies();
        } else {
            // uniformly distributed
            int nStates = dataInput.get().getMaxStateCount();
            freqs = new double[nStates];
            for (int i = 0; i < nStates; i++) {
                freqs[i] = 1.0 / nStates;
            }
        }
        needsUpdate = false;
    } // update


    /**
     * Estimate from sequence alignment.
     * This version matches the implementation in Beast 1 & PAUP  *
     */
    void estimateFrequencies() {
        Alignment alignment = dataInput.get();
        DataType dataType = alignment.getDataType();
        int stateCount = alignment.getMaxStateCount();

        freqs = new double[stateCount];
        Arrays.fill(freqs, 1.0 / stateCount);

        int nAttempts = 0;
        double fDifference;
        do {
            double[] fTmpFreq = new double[stateCount];

            double fTotal = 0.0;
            for (int i = 0; i < alignment.getPatternCount(); i++) {
                int[] nPattern = alignment.getPattern(i);
                double fWeight = alignment.getPatternWeight(i);

                for (int iValue : nPattern) {
                    int[] codes = dataType.getStatesForCode(iValue);

                    double sum = 0.0;
                    for (int iCode : codes) {
                        sum += freqs[iCode];
                    }

                    for (int iCode : codes) {
                        double fTmp = (freqs[iCode] * fWeight) / sum;
                        fTmpFreq[iCode] += fTmp;
                        fTotal += fTmp;
                    }
                }
            }

            fDifference = 0.0;
            for (int i = 0; i < stateCount; i++) {
                fDifference += Math.abs((fTmpFreq[i] / fTotal) - freqs[i]);
                freqs[i] = fTmpFreq[i] / fTotal;
            }
            nAttempts++;
        } while (fDifference > 1E-8 && nAttempts < 1000);

//    	Alignment alignment = m_data.get();
//        m_fFreqs = new double[alignment.getMaxStateCount()];
//        for (int i = 0; i < alignment.getPatternCount(); i++) {
//            int[] nPattern = alignment.getPattern(i);
//            double fWeight = alignment.getPatternWeight(i);
//            DataType dataType = alignment.getDataType();
//            for (int iValue : nPattern) {
//            	if (iValue < 4) {
//            	int [] codes = dataType.getStatesForCode(iValue);
//            	for (int iCode : codes) {
//                    m_fFreqs[iCode] += fWeight / codes.length;
//            	}
//            	}
////                if (iValue < m_fFreqs.length) { // ignore unknowns
////                    m_fFreqs[iValue] += nWeight;
////                }
//            }
//        }
//        // normalize
//        double fSum = 0;
//        for (double f : m_fFreqs) {
//            fSum += f;
//        }
//        for (int i = 0; i < m_fFreqs.length; i++) {
//            m_fFreqs[i] /= fSum;
//        }
        Log.info.println("Starting frequencies: " + Arrays.toString(freqs));
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
        for (int i = 0; i < freqs.length; i++) {
            double freq = freqs[i];
            if (freq < MINFREQ) freqs[i] = MINFREQ;
            if (freq > maxfreq) {
                maxfreq = freq;
                maxi = i;
            }
            sum += freqs[i];
        }
        double diff = 1.0 - sum;
        freqs[maxi] += diff;

        for (int i = 0; i < freqs.length - 1; i++) {
            for (int j = i + 1; j < freqs.length; j++) {
                if (freqs[i] == freqs[j]) {
                    freqs[i] += MINFDIFF;
                    freqs[j] -= MINFDIFF;
                }
            }
        }
    } // checkFrequencies

    /**
     * CalculationNode implementation *
     */
    @Override
    protected boolean requiresRecalculation() {
        boolean recalculates = false;
        if (frequenciesInput.get().somethingIsDirty()) {

            needsUpdate = true;
            recalculates = true;
        }

        return recalculates;
    }

    /**
     * @param frequencies the frequencies
     * @return return the sum of frequencies
     */
    private double getSumOfFrequencies(double[] frequencies) {
        double total = 0.0;
        for (double frequency : frequencies) {
            total += frequency;
        }
        return total;
    }

    @Override
	public void restore() {
        needsUpdate = true;
        super.restore();
    }

} // class Frequencies
