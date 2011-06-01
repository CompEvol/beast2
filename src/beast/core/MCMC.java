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


import beast.core.util.CompoundDistribution;
import beast.util.Randomizer;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
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
            new Input<State>("state", "elements of the state space");
    
    public Input<Integer> m_storeEvery = 
        new Input<Integer>("storeEvery", "store the state to disk every X number of samples so that we can " +
		"resume computation later on if the process failed half-way.", -1);
    
    public Input<Distribution> posteriorInput =
            new Input<Distribution>("distribution", "probability distribution to sample over (e.g. a posterior)",
                    Input.Validate.REQUIRED);

    public Input<List<Operator>> operatorsInput =
            new Input<List<Operator>>("operator", "operator for generating proposals in MCMC state space",
                    new ArrayList<Operator>(), Input.Validate.REQUIRED);

    public Input<List<Logger>> m_loggers =
            new Input<List<Logger>>("logger", "loggers for reporting progress of MCMC chain",
                    new ArrayList<Logger>(), Input.Validate.REQUIRED);

    public Input<List<StateNodeInitialiser>> m_initilisers = 
    	new Input<List<StateNodeInitialiser>>("init", "one or more state node initilisers used for determining " +
    			"the start state of the chain", 
    			new ArrayList<StateNodeInitialiser>());
    /** Alternative representation of operatorsInput that allows random selection
     * of operators and calculation of statistics.
     */
    protected OperatorSet operatorSet = new OperatorSet();


    /** The state that takes care of managing StateNodes, 
     * operations on StateNodes and propagates store/restore/requireRecalculation
     * calls to the appropriate Plugins.
     */
    protected State state;
    
    /**
     * number of samples taken where calculation is checked against full
     * recalculation of the posterior. Note that after every proposal that
     * is checked, there are 2 that are not checked. This allows errors 
     * in store/restore to be detected that cannot be found when every single
     * consecutive sample is checked.
     * So, only after 3*NR_OF_DEBUG_SAMPLES samples checking is stopped.
     */
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

        // StateNode initialisation
        HashSet<StateNode> initialisedStateNodes = new HashSet<StateNode>();
        for (StateNodeInitialiser initialiser : m_initilisers.get()) {
        	// make sure that the initialiser does not re-initialises a StateNode
        	List<StateNode> list = initialiser.getInitialisedStateNodes();
        	for (StateNode stateNode : list) {
        		if (initialisedStateNodes.contains(stateNode)) {
        			throw new Exception("Trying to initialise stateNode (id=" + stateNode.getID() + ") more than once. " +
        					"Remove an initialiser from MCMC to fix this.");
        		}
        	}
        	initialisedStateNodes.addAll(list);
        	// do the initialisation
        	//initialiser.initStateNodes();
        }

        // State initialisation
        if (m_startState.get() != null) {
	        this.state = m_startState.get();
        } else {
            // create state from scratch
            this.state = new State();
            HashSet<StateNode> stateNodes = new HashSet<StateNode>();
            for (Operator op : operatorsInput.get()) {
            	for (Plugin o : op.listActivePlugins()) {
            		if (o instanceof StateNode) {
            			stateNodes.add((StateNode) o);
            		}
            	}
            }
            for (StateNode stateNode : stateNodes) {
            	this.state.stateNodeInput.setValue(stateNode, this.state);
            }
            this.state.m_storeEvery.setValue(m_storeEvery.get(), this.state);
        }
        this.state.initialise();
        this.state.setPosterior(posteriorInput.get());
        
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


    
    double logAlpha;
    boolean bDebug;
    double fOldLogLikelihood; 
    double fNewLogLikelihood; 
    int nBurnIn;
    int nChainLength;
    Distribution posterior;
    
    @Override
    public void run() throws Exception {
    	// set up state (again). Other plugins may have manipulated the
    	// StateNodes, e.g. set up bounds or dimensions
    	state.initAndValidate();
    	// also, initialise state with the file name to store and set-up whether to resume from file
    	state.setStateFileName(m_sStateFile);

        nBurnIn = m_oBurnIn.get();
        nChainLength = m_oChainLength.get();
        if (m_bRestoreFromFile) {
        	state.restoreFromFile();
        	nBurnIn = 0;
        }
        long tStart = System.currentTimeMillis();

        System.err.println("Start state:");
        System.err.println(state.toString());

        state.setEverythingDirty(true);
        posterior = posteriorInput.get();

        // do the sampling
        logAlpha = 0;
        bDebug = true;
        fOldLogLikelihood = robustlyCalcPosterior(posterior); 
        
        int nInitiliasiationAttemps = 0;
        while (Double.isInfinite(fOldLogLikelihood) && nInitiliasiationAttemps++ < 10) {
            for (StateNodeInitialiser initialiser : m_initilisers.get()) {
            	initialiser.initStateNodes();
            }
            fOldLogLikelihood = robustlyCalcPosterior(posterior);
        }
        System.err.println("Start likelihood: " + fOldLogLikelihood + " " + (nInitiliasiationAttemps > 1?"after " + nInitiliasiationAttemps + " initialisation attempts":""));

        // initialises log so that log file headers are written, etc.
        for (Logger log : m_loggers.get()) {
            log.init();
        }

        doLoop();
        
        operatorSet.showOperatorRates(System.out);
        long tEnd = System.currentTimeMillis();
        System.out.println("Total calculation time: " + (tEnd - tStart) / 1000.0 + " seconds");
        close();

        System.err.println("End likelihood: " + fOldLogLikelihood);
        System.err.println(state);
        state.storeToFile();
    } // run;

    
    /** main MCMC loop **/ 
    void doLoop() throws Exception {
        for (int iSample = -nBurnIn; iSample <= nChainLength; iSample++) {
            state.store(iSample);

            Operator operator = operatorSet.selectOperator();
            //System.out.print("\n" + iSample + " " + operator.getName()+ ":");
            double fLogHastingsRatio = operator.proposal();
            if (fLogHastingsRatio != Double.NEGATIVE_INFINITY) {
            	state.storeCalculationNodes();
                state.checkCalculationNodesDirtiness();

                fNewLogLikelihood = posterior.calculateLogP();

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
            
            if (bDebug && iSample % 3 == 0 || iSample % 10000 == 0) { 
            	// check that the posterior is correctly calculated at every third
            	// sample, as long as we are in debug mode
                double fLogLikelihood = robustlyCalcPosterior(posterior); 
                if (Math.abs(fLogLikelihood - fOldLogLikelihood) > 1e-6) {
                	reportLogLikelihoods(posterior, "");
                    throw new Exception("At sample "+ iSample + "\nLikelihood incorrectly calculated: " + fOldLogLikelihood + " != " + fLogLikelihood
                    		+ " Operator: " + operator.getClass().getName());
                }
                if (iSample > NR_OF_DEBUG_SAMPLES * 3) {
                	// switch of debug mode once a sufficient large sample is checked
                    bDebug = false;
                }
            } else {
                operator.optimize(logAlpha);
            }
            callUserFunction(iSample);
        }
    }    
    
    /** report posterior and subcomponents recursively, for debugging 
     * incorrectly recalculated posteriors **/ 
    private void reportLogLikelihoods(Distribution distr, String sTab) {
		System.err.println(sTab + "P(" + distr.getID() + ") = " + distr.logP + " (was " + distr.storedLogP +")");
		if (distr instanceof CompoundDistribution) {
			for (Distribution distr2 : ((CompoundDistribution) distr).pDistributions.get()) {
				reportLogLikelihoods(distr2, sTab + "\t");
			}
		}
	}

	void callUserFunction(int iSample) {}

    
    /** Calculate posterior by setting all StateNodes and CalculationNodes dirty.
     * Clean everything afterwards.
     */
    protected double robustlyCalcPosterior(Distribution posterior) throws Exception {
        state.store(-1);
        state.setEverythingDirty(true);
    	state.storeCalculationNodes();
        state.checkCalculationNodesDirtiness();
        double fLogLikelihood = posterior.calculateLogP();
        state.setEverythingDirty(false);
        state.acceptCalculationNodes();
        return fLogLikelihood;
    }
    
} // class MCMC

