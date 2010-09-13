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
import java.util.ArrayList;
import java.util.List;

@Description("MCMC chain. This is the main element that controls which posterior " +
        "to calculate, how long to run the chain and all other properties, " +
        "which operators to apply on the state space and where to log results.")
@Citation("A prototype for BEAST 2.0: The computational science of evolutionary software. Bouckaert, Drummond, Rambaut, Alekseyenko, Suchard & the BEAST Core Development Team. 2010")
public class MCMC extends Runnable {

    public Input<Integer> m_oBurnIn =
            new Input<Integer>("preBurnin", "Number of burn in samples taken before entering the main loop", 0);

    public Input<Integer> m_oChainLength =
            new Input<Integer>("chainLength", "Length of the MCMC chain i.e. number of samples taken in main loop",
                    Input.Validate.REQUIRED);

    public Input<State> m_startState =
            new Input<State>("state", "elements of the state space", new State(), Input.Validate.REQUIRED);

    public Input<Distribution> posteriorInput =
            new Input<Distribution>("distribution", "probability distribution to sample over (e.g. a posterior)",
                    Input.Validate.REQUIRED);

    public Input<List<Operator>> operatorsInput =
            new Input<List<Operator>>("operator", "operator for generating proposals in MCMC state space",
                    new ArrayList<Operator>(), Input.Validate.REQUIRED);

    public Input<List<Logger>> m_loggers =
            new Input<List<Logger>>("logger", "loggers for reporting progress of MCMC chain",
                    new ArrayList<Logger>(), Input.Validate.REQUIRED);

    /** Alternative representation of operatorsInput that allows random selection
     * of operators and calculation of statistics.
     */
    public OperatorSet operatorSet = new OperatorSet();


    /** The state that takes care of managing StateNodes, 
     * operations on StateNodes and propagates store/restore/requireRecalculation
     * calls to the appropriate Plugins.
     */
    protected State state;
    
    /**
     * number of samples taken where calculation is checked against full
     * recalculation of the posterior.
     */
    //final protected int NR_OF_DEBUG_SAMPLES = 0;
    final protected int NR_OF_DEBUG_SAMPLES = 2000;

    @Override
    public void initAndValidate() throws Exception {
        System.out.println("======================================================");
        System.out.println("Please cite the following when publishing this model:\n");
        System.out.println(getCitations());
        System.out.println("======================================================");

        for (Operator op : operatorsInput.get()) {
            operatorSet.addOperator(op);
        }

        // state initialisation
        this.state = m_startState.get();
        this.state.setPosterior(posteriorInput.get());

        // initialises log so that log file headers are written, etc.
        for (Logger log : m_loggers.get()) {
            log.init();
        }
        
    } // init

    public void log(int nSample) {
        for (Logger log : m_loggers.get()) {
            log.log(nSample);
        }
    } // log

    public void close() {
        for (Logger log : m_loggers.get()) {
            log.close();
        }
    } // close

    /* Class for a collection of Operators. The main usage is to
     * be able to draw operators randomly proportionally to their
     * weight.
     */
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

        /** randomly select an operator with probability proportional to the weight of the operator **/
        public Operator selectOperator() {
            int iOperator = Randomizer.randomChoice(m_fCumulativeProbs);
            return m_operators.get(iOperator);
        }

        /** report operator statistics **/
        public void showOperatorRates(PrintStream out) {
            out.println("Operator                                        #accept\t#reject\t#total\tacceptance rate");
            for (int i = 0; i < m_operators.size(); i++) {
                out.println(m_operators.get(i));
            }
        }
    } // class OperatorSet


    @Override
    public void run() throws Exception {
    	// set up state (again). Other plugins may have manipulated the
    	// StateNodes, e.g. set up bounds or dimensions
    	state.initAndValidate();
    	// also, initialise state with the file name to store and set-up whether to resume from file
    	state.setStateFileName(m_sStateFile);
        if (m_bRestoreFromFile) {
        	state.restoreFromFile();
        }
        long tStart = System.currentTimeMillis();

        System.err.println("Start state:");
        System.err.println(state.toString());

        state.setEverythingDirty(true);
        Distribution posterior = posteriorInput.get();
        int nBurnIn = m_oBurnIn.get();
        int nChainLength = m_oChainLength.get();

        // do the sampling
        double logAlpha = 0;

        boolean bDebug = true;
        state.setEverythingDirty(true);
        state.checkCalculationNodesDirtiness();
        double fOldLogLikelihood = posterior.calculateLogP();
        System.err.println("Start likelihood: " + fOldLogLikelihood);

        // main MCMC loop 
        for (int iSample = -nBurnIn; iSample <= nChainLength; iSample++) {
            state.store(iSample);

            Operator operator = operatorSet.selectOperator();
            //System.out.print("\n" + iSample + " " + operator.getName()+ ":");
            double fLogHastingsRatio = operator.proposal();
            if (fLogHastingsRatio != Double.NEGATIVE_INFINITY) {
            	state.storeCalculationNodes();
                state.checkCalculationNodesDirtiness();

                double fNewLogLikelihood = posterior.calculateLogP();

                logAlpha = fNewLogLikelihood - fOldLogLikelihood + fLogHastingsRatio; //CHECK HASTINGS
                //System.out.println(logAlpha + " " + fNewLogLikelihood + " " + fOldLogLikelihood);
                if (logAlpha >= 0 || Randomizer.nextDouble() < Math.exp(logAlpha)) {
                    // accept
                    fOldLogLikelihood = fNewLogLikelihood;
                    state.acceptCalculationNodes();

                    if (iSample >= 0) {
                        operator.accept();
                    }
                    //System.out.print(" accept");
                } else {
                    // reject
                    if (iSample >= 0) {
                        operator.reject();
                    }
                    state.restore();
                    state.restoreCalculationNodes();
                    //System.out.print(" reject");
                }
                state.setEverythingDirty(false);
            } else {
                // operation failed
                if (iSample > 0) {
                    operator.reject();
                }
                state.restore();
                //System.out.print(" direct reject");
            }
            log(iSample);

            if (bDebug && iSample % 3 == 0) {
            	//System.out.print("*");
            	// check that the posterior is correctly calculated
                state.store(-1);
                state.setEverythingDirty(true);
                state.checkCalculationNodesDirtiness();

                double fLogLikelihood = posterior.calculateLogP();

                if (Math.abs(fLogLikelihood - fOldLogLikelihood) > 1e-10) {
                    throw new Exception("Likelihood incorrectly calculated: " + fOldLogLikelihood + " != " + fLogLikelihood);
                }
                if (iSample > NR_OF_DEBUG_SAMPLES * 3) {
                    bDebug = false;
                }
                state.setEverythingDirty(false);
            } else {
                operator.optimize(logAlpha);
            }
        }
        operatorSet.showOperatorRates(System.out);
        long tEnd = System.currentTimeMillis();
        System.out.println("Total calculation time: " + (tEnd - tStart) / 1000.0 + " seconds");
        close();

        System.err.println("End likelihood: " + fOldLogLikelihood);
        System.err.println(state);
        state.storeToFile();
    } // run;

} // class MCMC

