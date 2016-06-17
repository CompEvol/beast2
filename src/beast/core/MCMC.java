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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beast.core.util.CompoundDistribution;
import beast.core.util.Evaluator;
import beast.core.util.Log;
import beast.util.Randomizer;

@Description("MCMC chain. This is the main element that controls which posterior " +
        "to calculate, how long to run the chain and all other properties, " +
        "which operators to apply on the state space and where to log results.")
@Citation(value=
        "Bouckaert RR, Heled J, Kuehnert D, Vaughan TG, Wu C-H, Xie D, Suchard MA,\n" +
                "  Rambaut A, Drummond AJ (2014) BEAST 2: A software platform for Bayesian\n" +
                "  evolutionary analysis. PLoS Computational Biology 10(4): e1003537"
        , year = 2014, firstAuthorSurname = "bouckaert",
        DOI="10.1371/journal.pcbi.1003537")
public class MCMC extends Runnable {

    final public Input<Integer> chainLengthInput =
            new Input<>("chainLength", "Length of the MCMC chain i.e. number of samples taken in main loop",
                    Input.Validate.REQUIRED);

    final public Input<State> startStateInput =
            new Input<>("state", "elements of the state space");

    final public Input<List<StateNodeInitialiser>> initialisersInput =
            new Input<>("init", "one or more state node initilisers used for determining " +
                    "the start state of the chain",
                    new ArrayList<>());

    final public Input<Integer> storeEveryInput =
            new Input<>("storeEvery", "store the state to disk every X number of samples so that we can " +
                    "resume computation later on if the process failed half-way.", -1);

    final public Input<Integer> burnInInput =
            new Input<>("preBurnin", "Number of burn in samples taken before entering the main loop", 0);


    final public Input<Integer> numInitializationAttempts =
            new Input<>("numInitializationAttempts", "Number of initialization attempts before failing (default=10)", 10);

    final public Input<Distribution> posteriorInput =
            new Input<>("distribution", "probability distribution to sample over (e.g. a posterior)",
                    Input.Validate.REQUIRED);

    final public Input<List<Operator>> operatorsInput =
            new Input<>("operator", "operator for generating proposals in MCMC state space",
                    new ArrayList<>());//, Input.Validate.REQUIRED);

    final public Input<List<Logger>> loggersInput =
            new Input<>("logger", "loggers for reporting progress of MCMC chain",
                    new ArrayList<>(), Input.Validate.REQUIRED);

    final public Input<Boolean> sampleFromPriorInput = new Input<>("sampleFromPrior", "whether to ignore the likelihood when sampling (default false). " +
            "The distribution with id 'likelihood' in the posterior input will be ignored when this flag is set.", false);

    final public Input<OperatorSchedule> operatorScheduleInput = new Input<>("operatorschedule", "specify operator selection and optimisation schedule", new OperatorSchedule());

    /**
     * Alternative representation of operatorsInput that allows random selection
     * of operators and calculation of statistics.
     */
    protected OperatorSchedule operatorSchedule;

    /**
     * The state that takes care of managing StateNodes,
     * operations on StateNodes and propagates store/restore/requireRecalculation
     * calls to the appropriate BEASTObjects.
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

    /**
     * Set this to true to enable detailed MCMC debugging information
     * to be displayed.
     */
    private static final boolean printDebugInfo = false;

    public MCMC() {
    }


    @Override
    public void initAndValidate() {
        Log.info.println("===============================================================================");
        Log.info.println("Citations for this model:");
        Log.info.println(getCitations());
        Log.info.println("===============================================================================");

        operatorSchedule = operatorScheduleInput.get();
        for (final Operator op : operatorsInput.get()) {
            operatorSchedule.addOperator(op);
        }

        if (sampleFromPriorInput.get()) {
            // remove beastObject with id likelihood from posterior, if it is a CompoundDistribution
            if (posteriorInput.get() instanceof CompoundDistribution) {
                final CompoundDistribution posterior = (CompoundDistribution) posteriorInput.get();
                final List<Distribution> distrs = posterior.pDistributions.get();
                final int distrCount = distrs.size();
                for (int i = 0; i < distrCount; i++) {
                    final Distribution distr = distrs.get(i);
                    final String id = distr.getID();
                    if (id != null && id.equals("likelihood")) {
                        distrs.remove(distr);
                        break;
                    }
                }
                if (distrs.size() == distrCount) {
                    throw new RuntimeException("Sample from prior flag is set, but distribution with id 'likelihood' is " +
                            "not an input to posterior.");
                }
            } else {
                throw new RuntimeException("Don't know how to sample from prior since posterior is not a compound distribution. " +
                        "Suggestion: set sampleFromPrior flag to false.");
            }
        }


        // StateNode initialisation, only required when the state is not read from file
        if (restoreFromFile) {
            final HashSet<StateNode> initialisedStateNodes = new HashSet<>();
            for (final StateNodeInitialiser initialiser : initialisersInput.get()) {
                // make sure that the initialiser does not re-initialises a StateNode
                final List<StateNode> list = new ArrayList<>(1);
                initialiser.getInitialisedStateNodes(list);
                for (final StateNode stateNode : list) {
                    if (initialisedStateNodes.contains(stateNode)) {
                        throw new RuntimeException("Trying to initialise stateNode (id=" + stateNode.getID() + ") more than once. " +
                                "Remove an initialiser from MCMC to fix this.");
                    }
                }
                initialisedStateNodes.addAll(list);
                // do the initialisation
                //initialiser.initStateNodes();
            }
        }

        // State initialisation
        final HashSet<StateNode> operatorStateNodes = new HashSet<>();
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
            List<StateNode> nodes = op.listStateNodes();
            if (nodes.size() == 0) {
                    throw new RuntimeException("Operator " + op.getID() + " has no state nodes in the state. "
                                    + "Each operator should operate on at least one estimated state node in the state. "
                                    + "Remove the operator or add its statenode(s) to the state and/or set estimate='true'.");
                    // otherwise the chain may hang without obvious reason
            }
	        for (final StateNode stateNode : op.listStateNodes()) {
	            if (!stateNodes.contains(stateNode)) {
	                throw new RuntimeException("Operator " + op.getID() + " has a statenode " + stateNode.getID() + " in its inputs that is missing from the state.");
	            }
	        }
	    }
    
        // sanity check: at least one operator required to run MCMC
        if (operatorsInput.get().size() == 0) {
        	Log.warning.println("Warning: at least one operator required to run the MCMC properly, but none found.");
        }
        
        // sanity check: all state nodes should be operated on
        for (final StateNode stateNode : stateNodes) {
            if (!operatorStateNodes.contains(stateNode)) {
                Log.warning.println("Warning: state contains a node " + stateNode.getID() + " for which there is no operator.");
            }
        }
    } // init

    public void log(final int sampleNr) {
        for (final Logger log : loggers) {
            log.log(sampleNr);
        }
    } // log

    public void close() {
        for (final Logger log : loggers) {
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

    protected List<Logger> loggers;

    @Override
    public void run() throws IOException, SAXException, ParserConfigurationException {
        // set up state (again). Other beastObjects may have manipulated the
        // StateNodes, e.g. set up bounds or dimensions
        state.initAndValidate();
        // also, initialise state with the file name to store and set-up whether to resume from file
        state.setStateFileName(stateFileName);
        operatorSchedule.setStateFileName(stateFileName);

        burnIn = burnInInput.get();
        chainLength = chainLengthInput.get();
        int initialisationAttempts = 0;
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
                initialisationAttempts += 1;
            } while (Double.isInfinite(oldLogLikelihood) && initialisationAttempts < numInitializationAttempts.get());
        }
        final long startTime = System.currentTimeMillis();

        state.storeCalculationNodes();

        
        // do the sampling
        logAlpha = 0;
        debugFlag = Boolean.valueOf(System.getProperty("beast.debug"));


//        System.err.println("Start state:");
//        System.err.println(state.toString());

        Log.info.println("Start likelihood: " + oldLogLikelihood + " " + (initialisationAttempts > 1 ? "after " + initialisationAttempts + " initialisation attempts" : ""));
        if (Double.isInfinite(oldLogLikelihood) || Double.isNaN(oldLogLikelihood)) {
            reportLogLikelihoods(posterior, "");
            throw new RuntimeException("Could not find a proper state to initialise. Perhaps try another seed.");
        }

        loggers = loggersInput.get();

        // put the loggers logging to stdout at the bottom of the logger list so that screen output is tidier.
        Collections.sort(loggers, (o1, o2) -> {
            if (o1.isLoggingToStdout()) {
                return o2.isLoggingToStdout() ? 0 : 1;
            } else {
                return o2.isLoggingToStdout() ? -1 : 0;
            }
        });
        // warn if none of the loggers is to stdout, so no feedback is given on screen
        boolean hasStdOutLogger = false;
        boolean hasScreenLog = false;
        for (Logger l : loggers) {
        	if (l.isLoggingToStdout()) {
        		hasStdOutLogger = true;
        	}
        	if (l.getID() != null && l.getID().equals("screenlog")) {
        		hasScreenLog = true;
        	}
        }
        if (!hasStdOutLogger) {
        	Log.warning.println("WARNING: If nothing seems to be happening on screen this is because none of the loggers give feedback to screen.");
        	if (hasScreenLog) {
        		Log.warning.println("WARNING: This happens when a filename  is specified for the 'screenlog' logger.");
        		Log.warning.println("WARNING: To get feedback to screen, leave the filename for screenlog blank.");
        		Log.warning.println("WARNING: Otherwise, the screenlog is saved into the specified file.");
        	}
        }

        // initialises log so that log file headers are written, etc.
        for (final Logger log : loggers) {
            log.init();
        }

        doLoop();

        Log.info.println();
        operatorSchedule.showOperatorRates(System.out);

        Log.info.println();
        final long endTime = System.currentTimeMillis();
        Log.info.println("Total calculation time: " + (endTime - startTime) / 1000.0 + " seconds");
        close();

        Log.warning.println("End likelihood: " + oldLogLikelihood);
//        System.err.println(state);
        state.storeToFile(chainLength);
        operatorSchedule.storeToFile();
        //Randomizer.storeToFile(stateFileName);
    } // run;


    /**
     * main MCMC loop 
     * @throws IOException *
     */
    protected void doLoop() throws IOException {
        int corrections = 0;
        final boolean isStochastic = posterior.isStochastic();
        
        if (burnIn > 0) {
        	Log.warning.println("Please wait while BEAST takes " + burnIn + " pre-burnin samples");
        }
        for (int sampleNr = -burnIn; sampleNr <= chainLength; sampleNr++) {
            final int currentState = sampleNr;

            state.store(currentState);
//            if (m_nStoreEvery > 0 && sample % m_nStoreEvery == 0 && sample > 0) {
//                state.storeToFile(sample);
//            	operatorSchedule.storeToFile();
//            }

            final Operator operator = operatorSchedule.selectOperator();

            if (printDebugInfo) System.err.print("\n" + sampleNr + " " + operator.getName()+ ":");

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

            	if (operator.requiresStateInitialisation()) {
            		state.storeCalculationNodes();
            		state.checkCalculationNodesDirtiness();
            	}

                newLogLikelihood = posterior.calculateLogP();

                logAlpha = newLogLikelihood - oldLogLikelihood + logHastingsRatio; //CHECK HASTINGS
                if (printDebugInfo) System.err.print(logAlpha + " " + newLogLikelihood + " " + oldLogLikelihood);

                if (logAlpha >= 0 || Randomizer.nextDouble() < Math.exp(logAlpha)) {
                    // accept
                    oldLogLikelihood = newLogLikelihood;
                    state.acceptCalculationNodes();

                    if (sampleNr >= 0) {
                        operator.accept();
                    }
                    if (printDebugInfo) System.err.print(" accept");
                } else {
                    // reject
                    if (sampleNr >= 0) {
                        operator.reject(newLogLikelihood == Double.NEGATIVE_INFINITY ? -1 : 0);
                    }
                    state.restore();
                    state.restoreCalculationNodes();
                    if (printDebugInfo) System.err.print(" reject");
                }
                state.setEverythingDirty(false);
            } else {
                // operation failed
                if (sampleNr >= 0) {
                    operator.reject(-2);
                }
                state.restore();
				if (!operator.requiresStateInitialisation()) {
                    state.setEverythingDirty(false);
                    state.restoreCalculationNodes();
				}
				if (printDebugInfo) System.err.print(" direct reject");
            }
            log(sampleNr);

            if (debugFlag && sampleNr % 3 == 0 || sampleNr % 10000 == 0) {
                // check that the posterior is correctly calculated at every third
                // sample, as long as we are in debug mode
            	final double originalLogP = isStochastic ? posterior.getNonStochasticLogP() : oldLogLikelihood;
                final double logLikelihood = isStochastic ? state.robustlyCalcNonStochasticPosterior(posterior) : state.robustlyCalcPosterior(posterior);
                if (isTooDifferent(logLikelihood, originalLogP)) {
                    reportLogLikelihoods(posterior, "");
                    Log.err.println("At sample " + sampleNr + "\nLikelihood incorrectly calculated: " + originalLogP + " != " + logLikelihood
                    		+ "(" + (originalLogP - logLikelihood) + ")"
                            + " Operator: " + operator.getClass().getName());
                }
                if (sampleNr > NR_OF_DEBUG_SAMPLES * 3) {
                    // switch off debug mode once a sufficient large sample is checked
                    debugFlag = false;
                    if (isTooDifferent(logLikelihood, originalLogP)) {
                        // incorrect calculation outside debug period.
                        // This happens infrequently enough that it should repair itself after a robust posterior calculation
                        corrections++;
                        if (corrections > 100) {
                            // after 100 repairs, there must be something seriously wrong with the implementation
                        	Log.err.println("Too many corrections. There is something seriously wrong that cannot be corrected");
                            state.storeToFile(sampleNr);
                            operatorSchedule.storeToFile();
                            System.exit(1);
                        }
                        oldLogLikelihood = state.robustlyCalcPosterior(posterior);;
                    }
                } else {
                    if (isTooDifferent(logLikelihood, originalLogP)) {
                        // halt due to incorrect posterior during intial debug period
                        state.storeToFile(sampleNr);
                        operatorSchedule.storeToFile();
                        System.exit(1);
                    }
                }
            } else {
                if (sampleNr >= 0) {
                	operator.optimize(logAlpha);
                }
            }
            callUserFunction(sampleNr);

            // make sure we always save just before exiting
            if (storeEvery > 0 && (sampleNr + 1) % storeEvery == 0 || sampleNr == chainLength) {
                /*final double logLikelihood = */
                state.robustlyCalcNonStochasticPosterior(posterior);
                state.storeToFile(sampleNr);
                operatorSchedule.storeToFile();
            }
        }
        if (corrections > 0) {
        	Log.err.println("\n\nNB: " + corrections + " posterior calculation corrections were required. This analysis may not be valid!\n\n");
        }
    }

    private boolean isTooDifferent(double logLikelihood, double originalLogP) {
    	//return Math.abs((logLikelihood - originalLogP)/originalLogP) > 1e-6;
    	return Math.abs(logLikelihood - originalLogP) > 1e-6;
	}


	/*
     * report posterior and subcomponents recursively, for debugging
     * incorrectly recalculated posteriors *
     */
    protected void reportLogLikelihoods(final Distribution distr, final String tabString) {
        final double full =  distr.logP, last = distr.storedLogP;
        final String changed = full == last ? "" : "  **";
        Log.err.println(tabString + "P(" + distr.getID() + ") = " + full + " (was " + last + ")" + changed);
        if (distr instanceof CompoundDistribution) {
            for (final Distribution distr2 : ((CompoundDistribution) distr).pDistributions.get()) {
                reportLogLikelihoods(distr2, tabString + "\t");
            }
        }
    }

    protected void callUserFunction(final int sample) {
    }


    /**
     * Calculate posterior by setting all StateNodes and CalculationNodes dirty.
     * Clean everything afterwards.
     */
    public double robustlyCalcPosterior(final Distribution posterior) {
        return state.robustlyCalcPosterior(posterior);
    }

    
    /**
     * Calculate posterior by setting all StateNodes and CalculationNodes dirty.
     * Clean everything afterwards.
     */
    public double robustlyCalcNonStochasticPosterior(final Distribution posterior) {
        return state.robustlyCalcNonStochasticPosterior(posterior);
    }
} // class MCMC

