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

    public Input<Integer> m_oBurnIn = new Input<Integer>("preBurnin", "Number of burn in samples taken before entering the main loop", new Integer(0));
    public Input<Integer> m_oChainLength = new Input<Integer>("chainLength", "Length of the MCMC chain i.e. number of samples taken in main loop", Input.Validate.REQUIRED);
    public Input<State> m_startState = new Input<State>("state", "elements of the state space", new State(), Input.Validate.REQUIRED);
    public Input<Distribution> posteriorInput = new Input<Distribution>("distribution", "probability distribution to sample over (e.g. a posterior)", Input.Validate.REQUIRED);

    public Input<List<Operator>> operatorsInput = new Input<List<Operator>>("operator", "operator for generating proposals in MCMC state space", new ArrayList<Operator>(), Input.Validate.REQUIRED);
    public OperatorSet operatorSet = new OperatorSet();

    public Input<List<Logger>> m_loggers = new Input<List<Logger>>("log", "loggers for reporting progress of MCMC chain", new ArrayList<Logger>(), Input.Validate.REQUIRED);
    protected State state;

    @Override
    public void initAndValidate(State state) throws Exception {
        System.out.println("======================================================");
        System.out.println("Please cite the following when publishing this model:\n");
        System.out.println(getCitations());
        System.out.println("======================================================");

        for (Operator op : operatorsInput.get()) {
            operatorSet.addOperator(op);
        }

        // state initialization
        this.state = m_startState.get();
        this.state.getInputsConnectedToState(this);

        for (Logger log : m_loggers.get()) {
            log.init(this.state);
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
            log.log(nSample, state);
        }
    } // log

    public void close() {
        for (Logger log : m_loggers.get()) {
            log.close();
        }
    } // close

    protected void showOperatorRates(PrintStream out) {
        out.println("Operator                                        #accept\t#reject\t#total\tacceptance rate");
        for (int i = 0; i < operatorSet.getNrOperators(); i++) {
            out.println(operatorSet.getOperator(i));
        }
    }

    public void run() throws Exception {
        long tStart = System.currentTimeMillis();
        state.setDirty(true);

        int nBurnIn = m_oBurnIn.get();
        int nChainLength = m_oChainLength.get();

        System.err.println("Start state:");
        System.err.println(state.toString());

        // do the sampling
        double logAlpha = 0;

        boolean bDebug = true;
        state.setDirty(true);
        double fOldLogLikelihood = posteriorInput.get().calculateLogP();
        System.err.println("Start likelihood: = " + fOldLogLikelihood);
        for (int iSample = -nBurnIn; iSample <= nChainLength; iSample++) {

            //State proposedState = state.copy();
        	state.store();
            state.stateNumber = iSample;
            Operator operator = operatorSet.selectOperator();
            if (iSample == 24) {
                int h = 3;
                h++;
                //proposedState.makeDirty(State.IS_GORED);
            }
            double fLogHastingsRatio = operator.proposal(state);
            if (fLogHastingsRatio != Double.NEGATIVE_INFINITY) {
                //System.out.print("store ");
                storeCachables(iSample);
//				proposedState.makeDirty(State.IS_GORED);
                //System.out.print(operator.getName()+ "\n");
                //System.err.println(proposedState.toString());
                if (bDebug) {
                    //System.out.print(operator.getName()+ "\n");
                    //System.err.println(proposedState.toString());
                    state.validate();
                }

                //prepareCachables(proposedState);

                double fNewLogLikelihood = posteriorInput.get().calculateLogP();
                logAlpha = fNewLogLikelihood - fOldLogLikelihood + fLogHastingsRatio; //CHECK HASTINGS
                if (logAlpha >= 0 || Randomizer.nextDouble() < Math.exp(logAlpha)) {
                    // accept
                    fOldLogLikelihood = fNewLogLikelihood;
                    //state = proposedState;
                    state.setDirty(false);
                    if (iSample >= 0) {
                        operator.accept();
                    }
                } else {
                    // reject
                    if (iSample >= 0) {
                        operator.reject();
                    }
                    state.restore();
                    state.setDirty(false);
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
                state.validate();
                state.setDirty(true);
                //prepareCachables(state);
                //System.err.println(state.toString());
                double fLogLikelihood = posteriorInput.get().calculateLogP();
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
        for (Cacheable cacheable : Plugin.cacheables) {
            cacheable.restore(nSample);
        }
    }

    public void storeCachables(int nSample) {
        for (Cacheable cacheable : Plugin.cacheables) {
            cacheable.store(nSample);
        }
    }

//    public void prepareCachables(State state) {
//        for (Cacheable cacheable : Plugin.cacheables) {
//            cacheable.prepare(state);
//        }
//    }
} // class MCMC



/*
File: examples/testHKY.xml seed: 127 threads: 1
Lower = 0.0
Upper = null
human: 768 4
chimp: 768 4
bonobo: 768 4
gorilla: 768 4
orangutan: 768 4
siamang: 768 4
6 taxa
768 sites
69 patterns
TreeLikelihood uses beast.evolution.likelihood.BeerLikelihoodCoreCnG4
======================================================
Please cite the following when publishing this model:

A prototype for BEAST 2.0: The computational science of evolutionary software. Bouckaert, Drummond, Rambaut, Alekseyenko, Suchard & the BEAST Core Development Team. 2010

Hasegawa, M., Kishino, H and Yano, T. 1985. Dating the human-ape splitting by a molecular clock of mitochondrial DNA. Journal of Molecular Evolution 22:160-174.


======================================================
Sample	treeLikelihood	hky.kappa	
Start state:
hky.kappa: 1.0 
((((0:0.037,(1:0.017,2:0.017):0.020):0.015,3:0.052):0.040,4:0.092):0.015,5:0.107):0.000

Start likelihood: = -1986.8413139341137
0	-1986.8413139341137	1.0	55h33m20s/Msamples
10000	-1814.92113338769	21.66796994677653	2m54s/Msamples
20000	-1818.8101068616031	51.98152434088116	1m41s/Msamples
30000	-1819.113725846539	49.03766158847062	1m15s/Msamples
40000	-1815.9663938391454	18.629217392424117	1m1s/Msamples
50000	-1817.144049480813	55.81063632037919	53s/Msamples
60000	-1815.2571396605886	22.147665912494507	48s/Msamples
70000	-1816.0511221698043	24.08873388729444	44s/Msamples
80000	-1814.2518084163676	21.073628288342405	41s/Msamples
90000	-1814.7930847528262	28.26427465971234	39s/Msamples
100000	-1815.5227937881589	40.94855267668329	37s/Msamples
110000	-1813.5123750008786	32.21727495653929	36s/Msamples
120000	-1819.6721489955953	14.538413824582234	35s/Msamples
130000	-1814.1181305547886	26.36806603344612	35s/Msamples
140000	-1816.1132951004397	21.381522899499952	34s/Msamples
150000	-1817.5057640208154	18.877928721085038	33s/Msamples
160000	-1815.0159082796106	34.87536922803266	32s/Msamples
170000	-1816.3491406208418	28.08820453587267	32s/Msamples
180000	-1814.5526381353877	19.90808310405016	31s/Msamples
190000	-1817.460189041238	61.17332776603251	30s/Msamples
200000	-1818.8357813654582	70.24727650928979	30s/Msamples
210000	-1814.9261685669217	40.8580753199353	29s/Msamples
220000	-1818.907363494417	41.99976265360081	29s/Msamples
230000	-1814.9228311898894	35.71729427466099	28s/Msamples
240000	-1817.794769913191	36.23375632942445	28s/Msamples
250000	-1815.3481685825905	38.44708497269882	28s/Msamples
260000	-1814.761852338523	35.30188693115693	27s/Msamples
270000	-1813.3980755204464	32.91251642975589	27s/Msamples
280000	-1814.4740619398299	20.64708480493364	27s/Msamples
290000	-1818.7866799654755	63.82548882530605	26s/Msamples
300000	-1815.888601854904	23.519043483831584	26s/Msamples
*/