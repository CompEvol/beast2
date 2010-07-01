/*
* File MCMC.java
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
package beast.core;


import beast.util.Randomizer;

import java.io.PrintStream;
import java.util.*;

@Description("MCMC chain. This is the main element that controls which posterior " +
        "to calculate, how long to run the chain and all other properties, " +
        "which operators to apply on the state space and where to log results.")
@Citation("A prototype for BEAST 2.0: The computational science of evolutionary software. Bouckaert, Drummond, Rambaut & Suchard. 2010")
public class MCMC extends Plugin implements Runnable{

    public Input<Integer> m_oBurnIn = new Input<Integer>("preBurnin", "Number of burn in samples taken before entering the main loop", new Integer(0));
    public Input<Integer> m_oChainLength = new Input<Integer>("chainLength", "Length of the MCMC chain i.e. number of samples taken in main loop", Input.Validate.REQUIRED);
    public Input<State> m_startState = new Input<State>("state", "elements of the state space", new State(), Input.Validate.REQUIRED);
    public Input<ProbabilityDistribution> m_uncertainty = new Input<ProbabilityDistribution>("probabilityDistribution", "probability distribution to sample over (e.g. a posterior)", Input.Validate.REQUIRED);

    public Input<List<Operator>> m_operators = new Input<List<Operator>>("operator", "operator for generating proposals in MCMC state space", new ArrayList<Operator>(), Input.Validate.REQUIRED);
    public OperatorSet m_operatorset = new OperatorSet();

    public Input<List<Logger>> m_loggers = new Input<List<Logger>>("log", "loggers for reporting progress of MCMC chain", new ArrayList<Logger>(), Input.Validate.REQUIRED);
    protected State m_state;

    @Override
    public void initAndValidate(State state) throws Exception {
        System.out.println("======================================================");
        System.out.println("Please cite the following when publishing this model:\n");
        System.out.println(getCitations());
        System.out.println("======================================================");

        for (Operator op : m_operators.get()) {
            m_operatorset.addOperator(op);
        }

        m_state = m_startState.get();

        for (Logger log : m_loggers.get()) {
            log.init(m_state);
        }


    } // init
    
    /**
     * number of samples taken where calculation is checked against full
     * recalculation of the posterior.
     */
    final protected int NR_OF_DEBUG_SAMPLES = 2000;

    public class OperatorSet {
        List<Operator> m_operators = new ArrayList<Operator>();
        double m_fTotalWeight = 0;
        /**
         * the relative weights add to unity *
         */
        double[] m_fRelativeOperatorWeigths;
        double[] m_fCumulativeProbs;

        public void addOperator(Operator p) {
            m_operators.add(p);
            m_fTotalWeight += p.getWeight();
            m_fCumulativeProbs = new double[m_operators.size()];
            m_fCumulativeProbs[0] = m_operators.get(0).getWeight() / m_fTotalWeight;
            for (int i = 1; i < m_operators.size(); i++) {
                m_fCumulativeProbs[i] = m_operators.get(i).getWeight() / m_fTotalWeight + m_fCumulativeProbs[i - 1];
            }
        }

        public int getNrOperators() {
            return m_operators.size();
        }

        public Operator getOperator(int iOperator) {
            return m_operators.get(iOperator);
        }


        public Operator selectOperator() {
            int iOperator = Randomizer.randomChoice(m_fCumulativeProbs);
            return m_operators.get(iOperator);
        }

    } // class OperatorSet


    public void log(int nSample) {
        for (Logger log : m_loggers.get()) {
            log.log(nSample, m_state);
        }
    } // log

    public void close() {
        for (Logger log : m_loggers.get()) {
            log.close();
        }
    } // close

    protected void showOperatorRates(PrintStream out) {
        out.println("Operator                                        #accept\t#reject\t#total\tacceptance rate");
        for (int i = 0; i < m_operatorset.getNrOperators(); i++) {
            out.println(m_operatorset.getOperator(i));
        }
    }

    public void run() throws Exception {
        long tStart = System.currentTimeMillis();
        m_state.setDirty(true);

        int nBurnIn = m_oBurnIn.get();
        int nChainLength = m_oChainLength.get();

        System.err.println("Start state:");
        System.err.println(m_state.toString());

        // do the sampling
        double logAlpha = 0;

        boolean bDebug = true;
        m_state.setDirty(true);
        double fOldLogLikelihood = m_uncertainty.get().calculateLogP(m_state);
        System.err.println("Start likelihood: = " + fOldLogLikelihood);
        for (int iSample = -nBurnIn; iSample <= nChainLength; iSample++) {

            State proposedState = m_state.copy();
            Operator operator = m_operatorset.selectOperator();
            if (iSample == 24) {
                int h = 3;
                h++;
                //proposedState.makeDirty(State.IS_GORED);
            }
            double fLogHastingsRatio = operator.proposal(proposedState);
            if (fLogHastingsRatio != Double.NEGATIVE_INFINITY) {
                //System.out.print("store ");
                storeCachables(iSample);
//				proposedState.makeDirty(State.IS_GORED);
                //System.out.print(operator.getName()+ "\n");
                //System.err.println(proposedState.toString());
                if (bDebug) {
                    //System.out.print(operator.getName()+ "\n");
                    //System.err.println(proposedState.toString());
                    proposedState.validate();
                }


                double fNewLogLikelihood = m_uncertainty.get().calculateLogP(proposedState);
                logAlpha = fNewLogLikelihood - fOldLogLikelihood + fLogHastingsRatio; //CHECK HASTINGS
                if (logAlpha >= 0 || Randomizer.nextDouble() < Math.exp(logAlpha)) {
                    // accept
                    fOldLogLikelihood = fNewLogLikelihood;
                    m_state = proposedState;
                    m_state.setDirty(false);
                    if (iSample >= 0) {
                        operator.accept();
                    }
                } else {
                    // reject
                    if (iSample >= 0) {
                        operator.reject();
                    }
                    restoreCachables(iSample);
                    //System.out.println("restore ");
                }
            } else {
                // operation failed
                if (iSample > 0) {
                    operator.reject();
                }
            }
            log(iSample);

            if (bDebug) {
                m_state.validate();
                m_state.setDirty(true);
                //System.err.println(m_state.toString());
                double fLogLikelihood = m_uncertainty.get().calculateLogP(m_state);
                if (Math.abs(fLogLikelihood - fOldLogLikelihood) > 1e-10) {
                    throw new Exception("Likelihood incorrectly calculated: " + fOldLogLikelihood + " != " + fLogLikelihood);
                }
                if (iSample > NR_OF_DEBUG_SAMPLES) {
                    bDebug = false;
                }
            } else {
                operator.optimize(logAlpha);
            }
        }
        showOperatorRates(System.out);
        long tEnd = System.currentTimeMillis();
        System.out.println("Total calculation time: " + (tEnd - tStart) / 1000.0 + " seconds");
        close();
    } // run;

    public void restoreCachables(int nSample) {
        m_uncertainty.get().restore(nSample);
    }

    public void storeCachables(int nSample) {
        m_uncertainty.get().store(nSample);
    }
} // class MCMC
