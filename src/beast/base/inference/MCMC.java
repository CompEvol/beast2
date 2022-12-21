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
package beast.base.inference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beast.base.core.BEASTInterface;
import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.core.Input.Validate;
import beast.base.util.Randomizer;

@Description("MCMC chain. This is the main element that controls which posterior " +
        "to calculate, how long to run the chain and all other properties, " +
        "which operators to apply on the state space and where to log results.")
//@Citation(value=
//        "Bouckaert RR, Heled J, Kuehnert D, Vaughan TG, Wu C-H, Xie D, Suchard MA,\n" +
//                "  Rambaut A, Drummond AJ (2014) BEAST 2: A software platform for Bayesian\n" +
//                "  evolutionary analysis. PLoS Computational Biology 10(4): e1003537"
//        , year = 2014, firstAuthorSurname = "bouckaert",
//        DOI="10.1371/journal.pcbi.1003537")
@Citation(value="Bouckaert, Remco, Timothy G. Vaughan, Joëlle Barido-Sottani, Sebastián Duchêne, Mathieu Fourment, \n"
		+ "Alexandra Gavryushkina, Joseph Heled, Graham Jones, Denise Kühnert, Nicola De Maio, Michael Matschiner, \n"
        + "Fábio K. Mendes, Nicola F. Müller, Huw A. Ogilvie, Louis du Plessis, Alex Popinga, Andrew Rambaut, \n"
        + "David Rasmussen, Igor Siveroni, Marc A. Suchard, Chieh-Hsi Wu, Dong Xie, Chi Zhang, Tanja Stadler, \n"
        + "Alexei J. Drummond \n"
		+ "  BEAST 2.5: An advanced software platform for Bayesian evolutionary analysis. \n"
		+ "  PLoS computational biology 15, no. 4 (2019): e1006650.", 
        year = 2019, firstAuthorSurname = "bouckaert",
		DOI="10.1371/journal.pcbi.1006650")
public class MCMC extends Runnable {

    final public Input<Long> chainLengthInput =
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

    final public Input<OperatorSchedule> operatorScheduleInput = new Input<>("operatorschedule", "specify operator selection and optimisation schedule", new OperatorSchedule(), Validate.REQUIRED);

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

    public void log(final long sampleNr) {
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
    protected long chainLength;
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
            throw new RuntimeException("Could not find a proper state to initialise. Perhaps try another seed.\nSee http://www.beast2.org/2018/07/04/fatal-errors.html for other possible solutions.");
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

        // warn if loggers log with different frequencies (ignoring any stdout logger)
        long logFrequency = -1;
        boolean equalLogFrequency = true;
        for (Logger l : loggers) {
        	if (!l.isLoggingToStdout()) {
        		if (logFrequency < 0) {
        			logFrequency = l.everyInput.get();
        		} else if (logFrequency != l.everyInput.get()) {
        			equalLogFrequency = false;
        			break;
        		}
        	}
        }
        if (!equalLogFrequency) {
        	Log.warning("WARNING: Loggers appear to have different log frequency.");
        	Log.warning("WARNING: This may cause problems in post-processing based on more than one log files.");
        	Log.warning("WARNING: Therefore, it is recommended to use the same log frequency");
        	Log.warning("Hint: note that TreeWithMetaDataLogger with dp=\"X\" prints out trees with decimal places, potentially reducing tree file sizes substantially.");
        }

        // initialises log so that log file headers are written, etc.
        if (restoreFromFile) {
        	makeSureLogFilesAreSameLength();
        }
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


    protected void makeSureLogFilesAreSameLength() throws IOException {
    	// make sure log files all end in the same state
    	long min = -1;
    	long [] lengths = new long[loggers.size()];
    	for (int i = 0; i < loggers.size(); i++) {
    		Logger logger = loggers.get(i);
    		if (!logger.isLoggingToStdout()) {
    			long offset = logger.getLogOffset();
    			lengths[i] = offset;
    			min = min == -1 ? offset : Math.min(min, offset); 
    		}
    	}
    	if (min == 0) {
    		// at least one log file is empty
    		return;
    	}
    	for (int i = 0; i < loggers.size(); i++) {
    		Logger logger = loggers.get(i);
    		if (!logger.isLoggingToStdout() && min != lengths[i]) {
    			logger.setLogOffset(min);
    		}
    	}
	}


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
        for (long sampleNr = -burnIn; sampleNr <= chainLength; sampleNr++) {
            final Operator operator = propagateState(sampleNr);

            if (debugFlag && sampleNr % 1 == 0 || sampleNr % 10000 == 0) {
                // check that the posterior is correctly calculated at every third
                // sample, as long as we are in debug mode
            	final double originalLogP = isStochastic ? posterior.getNonStochasticLogP() : oldLogLikelihood;
                final double logLikelihood = isStochastic ? state.robustlyCalcNonStochasticPosterior(posterior) : state.robustlyCalcPosterior(posterior);
                if (isTooDifferent(logLikelihood, originalLogP)) {
                    reportLogLikelihoods(posterior, "");
                    Log.err.println("At sample " + sampleNr + "\nLikelihood incorrectly calculated: " + originalLogP + " != " + logLikelihood
                    		+ "(" + (originalLogP - logLikelihood) + ")"
                            + " Operator: " + operator.getName());
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
                        // halt due to incorrect posterior during initial debug period
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
            
            if (posterior.getCurrentLogP() == Double.POSITIVE_INFINITY) {
            	throw new RuntimeException("Encountered a positive infinite posterior. This is a sign there may be numeric instability in the model.");
            }
        }
        if (corrections > 0) {
        	Log.err.println("\n\nNB: " + corrections + " posterior calculation corrections were required. This analysis may not be valid!\n\n");
        }
    }

    /**
     * Perform a single MCMC propose+accept/reject step.
     *
     * @param sampleNr the index of the current MCMC step
     * @return the selected {@link beast.base.inference.Operator}
     */
    protected Operator propagateState(final long sampleNr) {
        state.store(sampleNr);
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
                    state.store(sampleNr);

                    return logP;
                }
            };
        }

        if (debugFlag) {
            // Store a checksum of each calculation node to ensure that the initial
            // state is restored when a step is rejected (see validateReject).
            for (CalculationNode node : listAllCalcNodes()) {
                node.storeChecksum();
            }
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

            if (logAlpha >= 0 || (logAlpha != Double.NEGATIVE_INFINITY && Randomizer.nextDouble() < Math.exp(logAlpha))) {
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
                if (debugFlag) {
                    validateReject(operator);
                }
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
            if (debugFlag) {
                validateReject(operator);
            }
        }
        log(sampleNr);
        return operator;
    }

    /**
     * Test whether state nodes and fat calculation nodes have been correctly
     * restored (according to their checksums) after the operator was rejected.
     * Caching an incorrect state could lead to undetected errors, here we
     * explicitly throw an exception if something went wrong.
     *
     * @param operator: the operator applied in the current MCMC step.
     */
    private void validateReject(Operator operator) throws RuntimeException {
        List<CalculationNode> affectedNodes = new ArrayList<>();
        // Relevant nodes: all calculation nodes that have changed in this step
        affectedNodes.addAll(state.getCurrentCalculationNodes());
        // ...and all state nodes that are affected by the operator.
        affectedNodes.addAll(operator.listStateNodes());

        for (CalculationNode node : affectedNodes) {
            if (!node.matchesOldChecksum()) {
                throw new RuntimeException("Node " + node.getID() + " was incorrectly restored after rejecting "
                        + "operator " + operator.getID() + " (according to calculatioNode checksum).");
            }
        }
    }

    /**
     * Recursively iterate over all inputs of the BEASTInterface o and
     * add the calculationNodes to allNodes.
     * @param o the BEASTInterface object at which the recursive search is started
     * @param allNodes the list where the calculationNodes are collected.
     **/
    protected void collectCalcNodes(BEASTInterface o, Set<CalculationNode> allNodes) {
        if (o instanceof CalculationNode) {
            allNodes.add((CalculationNode) o);
        }
        for (BEASTInterface o2 : o.listActiveBEASTObjects()) {
            collectCalcNodes(o2, allNodes);
        }
    }

    /**
     * Collect all calculation nodes used in the posterior in a list.
     * @return The list of all calculation nodes.
     */
    protected Set<CalculationNode> listAllCalcNodes() {
        Set<CalculationNode> allNodes = new HashSet<>();
        collectCalcNodes(posterior, allNodes);
        return allNodes;
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
        Log.info.println(tabString + "P(" + distr.getID() + ") = " + full + " (was " + last + ")" + changed);
        if (distr instanceof CompoundDistribution) {
            for (final Distribution distr2 : ((CompoundDistribution) distr).pDistributions.get()) {
                reportLogLikelihoods(distr2, tabString + "\t");
            }
        }
    }

    protected void callUserFunction(final long sample) {
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
    
    public OperatorSchedule getOperatorSchedule() {
    	return operatorSchedule;
    }
} // class MCMC

