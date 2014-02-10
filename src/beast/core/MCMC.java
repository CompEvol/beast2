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
import beast.core.util.Evaluator;
import beast.core.util.Log;
import beast.util.Randomizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Description("MCMC chain. This is the main element that controls which posterior " +
        "to calculate, how long to run the chain and all other properties, " +
        "which operators to apply on the state space and where to log results.")
@Citation(value= "Remco Bouckaert, Joseph Heled, Denise Kuehnert, Tim Vaughan, Chieh-Hsi Wu, Dong Xie, Marc Suchard, Andrew Rambaut, Alexei J Drummond "+ 
        "BEAST 2: A software platform for Bayesian evolutionary analysis. PLOS Computational Biology (accepted 2014)", year = 2014, firstAuthorSurname = "bouckaert")
public class MCMC extends Runnable {

    public Input<Integer> chainLengthInput =
            new Input<Integer>("chainLength", "Length of the MCMC chain i.e. number of samples taken in main loop",
                    Input.Validate.REQUIRED);

    public Input<State> startStateInput =
            new Input<State>("state", "elements of the state space");

    public Input<List<StateNodeInitialiser>> initialisersInput =
            new Input<List<StateNodeInitialiser>>("init", "one or more state node initilisers used for determining " +
                    "the start state of the chain",
                    new ArrayList<StateNodeInitialiser>());

    public Input<Integer> storeEveryInput =
            new Input<Integer>("storeEvery", "store the state to disk every X number of samples so that we can " +
                    "resume computation later on if the process failed half-way.", -1);

    public Input<Integer> burnInInput =
            new Input<Integer>("preBurnin", "Number of burn in samples taken before entering the main loop", 0);

    public Input<Distribution> posteriorInput =
            new Input<Distribution>("distribution", "probability distribution to sample over (e.g. a posterior)",
                    Input.Validate.REQUIRED);

    public Input<List<Operator>> operatorsInput =
            new Input<List<Operator>>("operator", "operator for generating proposals in MCMC state space",
                    new ArrayList<Operator>());//, Input.Validate.REQUIRED);

    public Input<List<Logger>> loggersInput =
            new Input<List<Logger>>("logger", "loggers for reporting progress of MCMC chain",
                    new ArrayList<Logger>(), Input.Validate.REQUIRED);

    public Input<Boolean> sampleFromPriorInput = new Input<Boolean>("sampleFromPrior", "whether to ignore the likelihood when sampling (default false). " +
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

    /**
     * Interval for storing state to disk, if negative the state will not be stored periodically *
     * Mirrors m_storeEvery input, or if this input is negative, the State.m_storeEvery input
     */
    protected int storeEvery;

    public MCMC() {
    }

    /**
     * Constructor for MCMC chain.
     *
     * @param chainLength
     * @param state
     * @param storeEvery
     * @param preBurnin
     * @param posterior
     * @param operators
     * @param loggers
     * @throws Exception
     */
    public MCMC(
            @Param(name = "chainLength", description = "Length of the MCMC chain i.e. number of samples taken in main loop") int chainLength,
            @Param(name = "state", description = "elements of the state space") State state,
            @Param(name = "initialisers", description = "one or more state node initilisers used for determining the start state of the chain") List<StateNodeInitialiser> initialisers,
            @Param(name = "storeEvery", description = "store the state to disk every X number of samples so that we can resume computation later on if the process failed half-way.") int storeEvery,
            @Param(name = "preBurnin", description = "Number of burn in samples taken before entering the main loop", defaultValue = "0") int preBurnin,
            @Param(name = "posterior", description = "probability distribution to sample over (e.g. a posterior)") Distribution posterior,
            @Param(name = "operators", description = "operator for generating proposals in MCMC state space") List<Operator> operators,
            @Param(name = "loggers", description = "loggers for reporting progress of MCMC chain") List<Logger> loggers,
            @Param(name = "sampleFromPrior", description = "whether to ignore the likelihood when sampling (default false). The distribution with id 'likelihood' in the posterior input will be ignored when this flag is set.", defaultValue = "false") boolean sampleFromPrior,
            @Param(name = "operatorSchedule", description = "specify operator selection and optimisation schedule", optional = true) OperatorSchedule operatorSchedule) {

        try {
            initByName(
                    "chainLength", chainLength,
                    "state", state,
                    "initialisers", initialisers,
                    "storeEvery", storeEvery,
                    "preBurnin", preBurnin,
                    "distribution", posterior,
                    "operator", operators,
                    "logger", loggers,
                    "sampleFromPrior", sampleFromPrior,
                    "operatorSchedule", operatorSchedule
            );
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException();
        }
    }

    @Override
    public void initAndValidate() throws Exception {
        Log.info.println("======================================================");
        Log.info.println("Please cite the following when publishing this model:\n");
        Log.info.println(getCitations());
        Log.info.println("======================================================");

        operatorSchedule = operatorScheduleInput.get();
        for (final Operator op : operatorsInput.get()) {
            operatorSchedule.addOperator(op);
        }

        if (sampleFromPriorInput.get()) {
            // remove plugin with id likelihood from posterior, if it is a CompoundDistribution
            if (posteriorInput.get() instanceof CompoundDistribution) {
                final CompoundDistribution posterior = (CompoundDistribution) posteriorInput.get();
                final List<Distribution> distrs = posterior.pDistributions.get();
                final int nDistr = distrs.size();
                for (int i = 0; i < nDistr; i++) {
                    final Distribution distr = distrs.get(i);
                    final String sID = distr.getID();
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
                throw new Exception("Don't know how to sample from prior since posterior is not a compound distribution. " +
                        "Suggestion: set sampleFromPrior flag to false.");
            }
        }


        // StateNode initialisation, only required when the state is not read from file
        if (restoreFromFile) {
            final HashSet<StateNode> initialisedStateNodes = new HashSet<StateNode>();
            for (final StateNodeInitialiser initialiser : initialisersInput.get()) {
                // make sure that the initialiser does not re-initialises a StateNode
                final List<StateNode> list = new ArrayList<StateNode>(1);
                initialiser.getInitialisedStateNodes(list);
                for (final StateNode stateNode : list) {
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
        final HashSet<StateNode> operatorStateNodes = new HashSet<StateNode>();
        for (final Operator op : operatorsInput.get()) {
            for (final StateNode stateNode : op.listStateNodes()) {
                operatorStateNodes.add(stateNode);
            }
        }
        if (startStateInput.get() != null) {
            this.state = startStateInput.get();
            if (storeEveryInput.get() > 0) {
                this.state.m_storeEvery.setValue(storeEveryInput.get(), this.state);
            }
        } else {
            // create state from scratch by collecting StateNode inputs from Operators
            this.state = new State();
            for (final StateNode stateNode : operatorStateNodes) {
                this.state.stateNodeInput.setValue(stateNode, this.state);
            }
            this.state.m_storeEvery.setValue(storeEveryInput.get(), this.state);
        }

        // grab the interval for storing the state to file
        if (storeEveryInput.get() > 0) {
            storeEvery = storeEveryInput.get();
        } else {
            storeEvery = state.m_storeEvery.get();
        }

        this.state.initialise();
        this.state.setPosterior(posteriorInput.get());

        // sanity check: all operator state nodes should be in the state
        final List<StateNode> stateNodes = this.state.stateNodeInput.get();
        for (final Operator op : operatorsInput.get()) {
            for (final StateNode stateNode : op.listStateNodes()) {
                if (!stateNodes.contains(stateNode)) {
                    throw new Exception("Operator " + op.getID() + " has a statenode " + stateNode.getID() + " in its inputs that is missing from the state.");
                }
            }
        }
        // sanity check: all state nodes should be operated on
        for (final StateNode stateNode : stateNodes) {
            if (!operatorStateNodes.contains(stateNode)) {
                System.out.println("Warning: state contains a node " + stateNode.getID() + " for which there is no operator.");
            }
        }
    } // init

    public void log(final int sampleNr) {
        for (final Logger log : loggersInput.get()) {
            log.log(sampleNr);
        }
    } // log

    public void close() {
        for (final Logger log : loggersInput.get()) {
            log.close();
        }
    } // close


    protected double logAlpha;
    protected boolean debugFlag;
    protected double oldLogLikelihood;
    protected double newLogLikelihood;
    protected int burnIn;
    protected int chainLength;
    protected Distribution posterior;

    @Override
    public void run() throws Exception {
        // set up state (again). Other plugins may have manipulated the
        // StateNodes, e.g. set up bounds or dimensions
        state.initAndValidate();
        // also, initialise state with the file name to store and set-up whether to resume from file
        state.setStateFileName(stateFileName);
        operatorSchedule.setStateFileName(stateFileName);

        burnIn = burnInInput.get();
        chainLength = chainLengthInput.get();
        int nInitiliasiationAttemps = 0;
        state.setEverythingDirty(true);
        posterior = posteriorInput.get();

        if (restoreFromFile) {
            state.restoreFromFile();
            operatorSchedule.restoreFromFile();
            burnIn = 0;
            oldLogLikelihood = state.robustlyCalcPosterior(posterior);
        } else {
            do {
                for (final StateNodeInitialiser initialiser : initialisersInput.get()) {
                    initialiser.initStateNodes();
                }
                oldLogLikelihood = state.robustlyCalcPosterior(posterior);
            } while (Double.isInfinite(oldLogLikelihood) && nInitiliasiationAttemps++ < 10);
        }
        final long startTime = System.currentTimeMillis();

        // do the sampling
        logAlpha = 0;
        debugFlag = Boolean.valueOf(System.getProperty("beast.debug"));


//        System.err.println("Start state:");
//        System.err.println(state.toString());

        System.err.println("Start likelihood: " + oldLogLikelihood + " " + (nInitiliasiationAttemps > 1 ? "after " + nInitiliasiationAttemps + " initialisation attempts" : ""));
        if (Double.isInfinite(oldLogLikelihood) || Double.isNaN(oldLogLikelihood)) {
            reportLogLikelihoods(posterior, "");
            throw new Exception("Could not find a proper state to initialise. Perhaps try another seed.");
        }

        // initialises log so that log file headers are written, etc.
        for (final Logger log : loggersInput.get()) {
            log.init();
        }

        doLoop();

        operatorSchedule.showOperatorRates(System.out);
        final long endTime = System.currentTimeMillis();
        System.out.println("Total calculation time: " + (endTime - startTime) / 1000.0 + " seconds");
        close();

        System.err.println("End likelihood: " + oldLogLikelihood);
//        System.err.println(state);
        state.storeToFile(chainLength);
        operatorSchedule.storeToFile();
        //Randomizer.storeToFile(stateFileName);
    } // run;


    /**
     * main MCMC loop *
     */
    protected void doLoop() throws Exception {
        int corrections = 0;
        for (int sampleNr = -burnIn; sampleNr <= chainLength; sampleNr++) {
            final int currentState = sampleNr;

            state.store(currentState);
//            if (m_nStoreEvery > 0 && iSample % m_nStoreEvery == 0 && iSample > 0) {
//                state.storeToFile(iSample);
//            	operatorSchedule.storeToFile();
//            }

            final Operator operator = operatorSchedule.selectOperator();
            //System.out.print("\n" + sampleNr + " " + operator.getName()+ ":");

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

            final double logHastingsRatio = operator.proposal(evaluator);

            if (logHastingsRatio != Double.NEGATIVE_INFINITY) {

                state.storeCalculationNodes();
                state.checkCalculationNodesDirtiness();

                newLogLikelihood = posterior.calculateLogP();

                logAlpha = newLogLikelihood - oldLogLikelihood + logHastingsRatio; //CHECK HASTINGS
                //System.out.println(logAlpha + " " + newLogLikelihood + " " + oldLogLikelihood);
                if (logAlpha >= 0 || Randomizer.nextDouble() < Math.exp(logAlpha)) {
                    // accept
                    oldLogLikelihood = newLogLikelihood;
                    state.acceptCalculationNodes();

                    if (sampleNr >= 0) {
                        operator.accept();
                    }
                    //System.out.print(" accept");
                } else {
                    // reject
                    if (sampleNr >= 0) {
                        operator.reject(newLogLikelihood == Double.NEGATIVE_INFINITY ? -1 : 0);
                    }
                    state.restore();
                    state.restoreCalculationNodes();
                    //System.out.print(" reject");
                }
                state.setEverythingDirty(false);
            } else {
                // operation failed
                if (sampleNr >= 0) {
                    operator.reject(-2);
                }
                state.restore();
                //System.out.print(" direct reject");
            }
            log(sampleNr);

            if (debugFlag && sampleNr % 3 == 0 || sampleNr % 10000 == 0) {
                // check that the posterior is correctly calculated at every third
                // sample, as long as we are in debug mode
                final double fLogLikelihood = state.robustlyCalcPosterior(posterior);
                if (Math.abs(fLogLikelihood - oldLogLikelihood) > 1e-6) {
                    reportLogLikelihoods(posterior, "");
                    System.err.println("At sample " + sampleNr + "\nLikelihood incorrectly calculated: " + oldLogLikelihood + " != " + fLogLikelihood
                            + " Operator: " + operator.getClass().getName());
                }
                if (sampleNr > NR_OF_DEBUG_SAMPLES * 3) {
                    // switch off debug mode once a sufficient large sample is checked
                    debugFlag = false;
                    if (Math.abs(fLogLikelihood - oldLogLikelihood) > 1e-6) {
                        // incorrect calculation outside debug period.
                        // This happens infrequently enough that it should repair itself after a robust posterior calculation
                        corrections++;
                        if (corrections > 100) {
                            // after 100 repairs, there must be something seriously wrong with the implementation
                            System.err.println("Too many corrections. There is something seriously wrong that cannot be corrected");
                            state.storeToFile(sampleNr);
                            operatorSchedule.storeToFile();
                            System.exit(0);
                        }
                        oldLogLikelihood = fLogLikelihood;
                    }
                } else {
                    if (Math.abs(fLogLikelihood - oldLogLikelihood) > 1e-6) {
                        // halt due to incorrect posterior during intial debug period
                        state.storeToFile(sampleNr);
                        operatorSchedule.storeToFile();
                        System.exit(0);
                    }
                }
            } else {
                operator.optimize(logAlpha);
            }
            callUserFunction(sampleNr);

            // make sure we always save just before exiting
            if (storeEvery > 0 && (sampleNr + 1) % storeEvery == 0 || sampleNr == chainLength) {
                /*final double fLogLikelihood = */
                state.robustlyCalcPosterior(posterior);
                state.storeToFile(sampleNr);
                operatorSchedule.storeToFile();
            }
        }
        if (corrections > 0) {
            System.err.println("\n\nNB: " + corrections + " posterior calculation corrections were required. This analysis may not be valid!\n\n");
        }
    }

    /**
     * report posterior and subcomponents recursively, for debugging
     * incorrectly recalculated posteriors *
     */
    protected void reportLogLikelihoods(final Distribution distr, final String tabString) {
        System.err.println(tabString + "P(" + distr.getID() + ") = " + distr.logP + " (was " + distr.storedLogP + ")");
        if (distr instanceof CompoundDistribution) {
            for (final Distribution distr2 : ((CompoundDistribution) distr).pDistributions.get()) {
                reportLogLikelihoods(distr2, tabString + "\t");
            }
        }
    }

    protected void callUserFunction(final int iSample) {
    }


    /**
     * Calculate posterior by setting all StateNodes and CalculationNodes dirty.
     * Clean everything afterwards.
     */
    public double robustlyCalcPosterior(final Distribution posterior) throws Exception {
        return state.robustlyCalcPosterior(posterior);
    }
//        state.store(-1);
//        state.setEverythingDirty(true);
//        //state.storeCalculationNodes();
//        state.checkCalculationNodesDirtiness();
//        double fLogLikelihood = posterior.calculateLogP();
//        state.setEverythingDirty(false);
//        state.acceptCalculationNodes();
//        return fLogLikelihood;
//    }

} // class MCMC

