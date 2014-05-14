/*
* File CompoundProbabilityDistribution.java
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
package beast.core.util;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import beast.app.BeastMCMC;
import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.core.BEASTObject;


@Description("Takes a collection of distributions, typically a number of likelihoods " +
        "and priors and combines them into the compound of these distributions " +
        "typically interpreted as the posterior.")
public class CompoundDistribution extends Distribution {
    // no need to make this input REQUIRED. If no distribution input is
    // specified the class just returns probability 1.
    public Input<List<Distribution>> pDistributions =
            new Input<List<Distribution>>("distribution",
                    "individual probability distributions, e.g. the likelihood and prior making up a posterior",
                    new ArrayList<Distribution>());
    public Input<Boolean> useThreadsInput = new Input<Boolean>("useThreads", "calculated the distributions in parallel using threads (default false)", false);
    public Input<Boolean> ignoreInput = new Input<Boolean>("ignore", "ignore all distributions and return 1 as distribution (default false)", false);
    
    /**
     * flag to indicate threads should be used. Only effective if the useThreadsInput is
     * true and BeasMCMC.nrOfThreads > 1
     */
    boolean useThreads;
    boolean ignore;

    @Override
    public void initAndValidate() throws Exception {
        super.initAndValidate();
        useThreads = useThreadsInput.get() && (BeastMCMC.m_nThreads > 1);
        ignore = ignoreInput.get();

        if (pDistributions.get().size() == 0) {
            logP = 0;
        }
//        for(Distribution dists : pDistributions.get()) {
//        	logP += dists.calculateLogP();
//        }
    }


    /**
     * Distribution implementation follows *
     */
    @Override
    public double calculateLogP() throws Exception {
        logP = 0;
        if (ignore) {
        	return logP;
        }
        if (useThreads) {
            logP = calculateLogPUsingThreads();
        } else {
            for (Distribution dists : pDistributions.get()) {
                if (dists.isDirtyCalculation()) {
                    logP += dists.calculateLogP();
                } else {
                    logP += dists.getCurrentLogP();
                }
                if (Double.isInfinite(logP) || Double.isNaN(logP)) {
                    return logP;
                }
            }
        }
        return logP;
    }

    class CoreRunnable implements Runnable {
        Distribution distr;

        CoreRunnable(Distribution core) {
            distr = core;
        }

        public void run() {
            try {
                if (distr.isDirtyCalculation()) {
                    logP += distr.calculateLogP();
                } else {
                    logP += distr.getCurrentLogP();
                }
            } catch (Exception e) {
                System.err.println("Something went wrong in a calculation of " + distr.getID());
                e.printStackTrace();
                System.exit(0);
            }
            m_nCountDown.countDown();
        }

    } // CoreRunnable

    CountDownLatch m_nCountDown;

    private double calculateLogPUsingThreads() throws Exception {
        try {

            int nrOfDirtyDistrs = 0;
            for (Distribution dists : pDistributions.get()) {
                if (dists.isDirtyCalculation()) {
                    nrOfDirtyDistrs++;
                }
            }
            m_nCountDown = new CountDownLatch(nrOfDirtyDistrs);
            // kick off the threads
            for (Distribution dists : pDistributions.get()) {
                if (dists.isDirtyCalculation()) {
                    CoreRunnable coreRunnable = new CoreRunnable(dists);
                    BeastMCMC.g_exec.execute(coreRunnable);
                }
            }
            m_nCountDown.await();
            logP = 0;
            for (Distribution distr : pDistributions.get()) {
                logP += distr.getCurrentLogP();
            }
            return logP;
        } catch (RejectedExecutionException e) {
            useThreads = false;
            System.err.println("Stop using threads: " + e.getMessage());
            // refresh thread pool
            BeastMCMC.g_exec = Executors.newFixedThreadPool(BeastMCMC.m_nThreads);
            return calculateLogP();
        }
    }


    @Override
    public void sample(State state, Random random) {
        for (Distribution distribution : pDistributions.get()) {
            distribution.sample(state, random);
        }
    }

    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<String>();
        for (Distribution distribution : pDistributions.get()) {
            arguments.addAll(distribution.getArguments());
        }
        return arguments;
    }

    @Override
    public List<String> getConditions() {
        List<String> conditions = new ArrayList<String>();
        for (Distribution distribution : pDistributions.get()) {
            conditions.addAll(distribution.getConditions());
        }
        return conditions;
    }

    @Override
    public List<BEASTObject> listActivePlugins() throws IllegalArgumentException, IllegalAccessException {
    	if (ignoreInput.get()) {
    		return new ArrayList<BEASTObject>();
    	} else {
    		return super.listActivePlugins();
    	}
    }

    @Override
    public boolean isStochastic() {
        for (Distribution distribution : pDistributions.get()) {
            if (distribution.isStochastic())
                return true;
        }
        
        return false;
    }
    
    @Override
    public double getNonStochasticLogP() throws Exception {
        double logP = 0;
        if (ignore) {
        	return logP;
        }
        // The loop could gain a little bit from being multithreaded
        // though getNonStochasticLogP is called for debugging purposes only
        // so efficiency is not an immediate issue.
        for (Distribution dists : pDistributions.get()) {
            logP += dists.getNonStochasticLogP();
            if (Double.isInfinite(logP) || Double.isNaN(logP)) {
                return logP;
            }
        }
        return logP;
    }
    
} // class CompoundDistribution
