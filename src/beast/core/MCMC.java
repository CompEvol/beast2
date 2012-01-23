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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


@Description("MCMC chain. This is the main element that controls which posterior " +
        "to calculate, how long to run the chain and all other properties, " +
        "which operators to apply on the state space and where to log results.")
@Citation("A prototype for BEAST 2.0: The computational science of evolutionary software. Bouckaert, Drummond, Rambaut, Alekseyenko, Suchard, Walter & the BEAST Core Development Team. 2010")
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
                    new ArrayList<Operator>());//, Input.Validate.REQUIRED);

    public Input<List<Logger>> m_loggers =
            new Input<List<Logger>>("logger", "loggers for reporting progress of MCMC chain",
                    new ArrayList<Logger>(), Input.Validate.REQUIRED);

    public Input<List<StateNodeInitialiser>> m_initilisers =
            new Input<List<StateNodeInitialiser>>("init", "one or more state node initilisers used for determining " +
                    "the start state of the chain",
                    new ArrayList<StateNodeInitialiser>());

    public Input<Boolean> sampleFromPrior = new Input<Boolean>("sampleFromPrior", "whether to ignore the likelihood when sampling (default false). " +
            "The distribution with id 'likelihood' in the posterior input will be ignored when this flag is set.", false);

    public Input<OperatorSchedule> operatorScheduleInput = new Input<OperatorSchedule>("operatorschedule", "specify operator selection and optimisation schedule", new OperatorSchedule());

    /**
     * Alternative representation of operatorsInput that allows random selection
     * of operators and calculation of statistics.
     */
    protected OperatorSchedule operatorSchedule;


    /**
     * The state that takes care of managing StateNodes,
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

        operatorSchedule = operatorScheduleInput.get();
        for (Operator op : operatorsInput.get()) {
            operatorSchedule.addOperator(op);
        }

        if (sampleFromPrior.get()) {
            // remove plugin with id likelihood from posterior, if it is a CompoundDistribution
            if (posteriorInput.get() instanceof CompoundDistribution) {
                CompoundDistribution posterior = (CompoundDistribution) posteriorInput.get();
                List<Distribution> distrs = posterior.pDistributions.get();
                int nDistr = distrs.size();
                for (int i = 0; i < nDistr; i++) {
                    Distribution distr = distrs.get(i);
                    String sID = distr.getID();
                    if (sID != null && sID.equals("likelihood")) {
                        distrs.remove(distr);
                        break;
                    }
                }
                if (distrs.size() == nDistr) {
                    throw new Exception("Sample from prior flag is set, but distribution with id 'likelihood' is " +
                            "not an input to posterior.");
                }
            } else {
                throw new Exception("Don't know how to cample from prior since posterior is not a compound distribution. " +
                        "Suggestion: set sampleFromPrior flag to false.");
            }
        }


        // StateNode initialisation, only required when the state is not read from file
        if (m_bRestoreFromFile) {
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
        }

        // State initialisation
        HashSet<StateNode> operatorStateNodes = new HashSet<StateNode>();
        for (Operator op : operatorsInput.get()) {
            for (Plugin o : op.listActivePlugins()) {
                if (o instanceof StateNode) {
                    operatorStateNodes.add((StateNode) o);
                }
            }
        }
        if (m_startState.get() != null) {
            this.state = m_startState.get();
        } else {
            // create state from scratch by collecting StateNode inputs from Operators
            this.state = new State();
            for (StateNode stateNode : operatorStateNodes) {
                this.state.stateNodeInput.setValue(stateNode, this.state);
            }
            this.state.m_storeEvery.setValue(m_storeEvery.get(), this.state);
        }
        this.state.initialise();
        this.state.setPosterior(posteriorInput.get());

        // sanity check: all operator state nodes should be in the state
        List<StateNode> stateNodes = this.state.stateNodeInput.get();
        for (Operator op : operatorsInput.get()) {
            for (Plugin o : op.listActivePlugins()) {
                if (o instanceof StateNode) {
                    if (!stateNodes.contains((StateNode) o)) {
                        throw new Exception("Operator " + op.getID() + " has a statenode " + o.getID() + " in its inputs that is missing from the state.");
                    }
                }
            }
        }
        // sanity check: all state nodes should be operated on
        for (StateNode stateNode : stateNodes) {
            if (!operatorStateNodes.contains(stateNode)) {
                System.out.println("Warning: state contains a node " + stateNode.getID() + " for which there is no operator.");
            }
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


    protected double logAlpha;
    protected boolean bDebug;
    protected double fOldLogLikelihood;
    protected double fNewLogLikelihood;
    protected int nBurnIn;
    protected int nChainLength;
    protected Distribution posterior;

    @Override
    public void run() throws Exception {
        // set up state (again). Other plugins may have manipulated the
        // StateNodes, e.g. set up bounds or dimensions
        state.initAndValidate();
        // also, initialise state with the file name to store and set-up whether to resume from file
        state.setStateFileName(m_sStateFile);
        operatorSchedule.setStateFileName(m_sStateFile);

        nBurnIn = m_oBurnIn.get();
        nChainLength = m_oChainLength.get();
        int nInitiliasiationAttemps = 0;
        state.setEverythingDirty(true);
        posterior = posteriorInput.get();

        if (m_bRestoreFromFile) {
            state.restoreFromFile();
            operatorSchedule.restoreFromFile();
            nBurnIn = 0;
            fOldLogLikelihood = robustlyCalcPosterior(posterior);
        } else {
            do {
                for (StateNodeInitialiser initialiser : m_initilisers.get()) {
                    initialiser.initStateNodes();
                }
                fOldLogLikelihood = robustlyCalcPosterior(posterior);
            } while (Double.isInfinite(fOldLogLikelihood) && nInitiliasiationAttemps++ < 10);
        }
        long tStart = System.currentTimeMillis();

        // do the sampling
        logAlpha = 0;
        bDebug = Boolean.valueOf(System.getProperty("beast.debug"));


        System.err.println("Start state:");
        System.err.println(state.toString());

        System.err.println("Start likelihood: " + fOldLogLikelihood + " " + (nInitiliasiationAttemps > 1 ? "after " + nInitiliasiationAttemps + " initialisation attempts" : ""));
        if (Double.isInfinite(fOldLogLikelihood) || Double.isNaN(fOldLogLikelihood)) {
            reportLogLikelihoods(posterior, "");
            throw new Exception("Could not find a proper state to initialise. Perhaps try another seed.");
        }

        // initialises log so that log file headers are written, etc.
        for (Logger log : m_loggers.get()) {
            log.init();
        }

        doLoop();

        operatorSchedule.showOperatorRates(System.out);
        long tEnd = System.currentTimeMillis();
        System.out.println("Total calculation time: " + (tEnd - tStart) / 1000.0 + " seconds");
        close();

        System.err.println("End likelihood: " + fOldLogLikelihood);
        System.err.println(state);
        state.storeToFile(nChainLength);
        operatorSchedule.storeToFile();
    } // run;


    /**
     * main MCMC loop *
     */
    protected void doLoop() throws Exception {
        for (int iSample = -nBurnIn; iSample <= nChainLength; iSample++) {
            final int currentState = iSample;

            state.store(currentState);

            Operator operator = operatorSchedule.selectOperator();
            //System.out.print("\n" + iSample + " " + operator.getName()+ ":");

            final Distribution evaluatorDistribution = operator.getEvaluatorDistribution();
            Evaluator evaluator = null;

            if (evaluatorDistribution != null) {
                evaluator = new Evaluator() {
                    @Override
                    public double evaluate() {
                        double logP = 0.0;

                        state.storeCalculationNodes();
                        state.checkCalculationNodesDirtiness();

                        try {
                            logP = evaluatorDistribution.calculateLogP();
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.exit(1);
                        }

                        state.restore();
                        state.store(currentState);

                        return logP;
                    }
                };
            }

            double fLogHastingsRatio = operator.proposal(evaluator);

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
                if (iSample >= 0) {
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
                    throw new Exception("At sample " + iSample + "\nLikelihood incorrectly calculated: " + fOldLogLikelihood + " != " + fLogLikelihood
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

    /**
     * report posterior and subcomponents recursively, for debugging
     * incorrectly recalculated posteriors *
     */
    protected void reportLogLikelihoods(Distribution distr, String sTab) {
        System.err.println(sTab + "P(" + distr.getID() + ") = " + distr.logP + " (was " + distr.storedLogP + ")");
        if (distr instanceof CompoundDistribution) {
            for (Distribution distr2 : ((CompoundDistribution) distr).pDistributions.get()) {
                reportLogLikelihoods(distr2, sTab + "\t");
            }
        }
    }

    protected void callUserFunction(int iSample) {
    }


    /**
     * Calculate posterior by setting all StateNodes and CalculationNodes dirty.
     * Clean everything afterwards.
     */
    public double robustlyCalcPosterior(Distribution posterior) throws Exception {
        state.store(-1);
        state.setEverythingDirty(true);
        //state.storeCalculationNodes();
        state.checkCalculationNodesDirtiness();
        double fLogLikelihood = posterior.calculateLogP();
        state.setEverythingDirty(false);
        state.acceptCalculationNodes();
        return fLogLikelihood;
    }

} // class MCMC

