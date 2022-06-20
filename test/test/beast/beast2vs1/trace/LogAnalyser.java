/*
 * LogAnalyser.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
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

package test.beast.beast2vs1.trace;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.MCMC;
import beast.base.inference.Runnable;


@Description("Log Analyser: analyse BEAST log and provide statistics result including mean, min, max, median, standard deviation, " +
        "mean squared error, variance, ESS, hpd lower, hpd upper, cpd lower, cpd upper, standard error of mean, auto correlation time " +
        "and geometric mean, which does not store the input data values in memory.")
@Citation("Created by Alexei Drummond and modified by Walter Xie")
public class LogAnalyser extends Runnable {
    public Input<String> m_sFileName =
            new Input<>("fileName", "Name of the log file to be analysed", Validate.REQUIRED);
    public Input<Integer> m_iBurnIn =
            new Input<>("burnIn", "Number of burn in samples taken before log statistics analysis. " +
                    "If burnIn = -1 then burnIn = 0.1 * maxState.", -1);
    public Input<Boolean> m_bDisplayStat =
            new Input<>("displayStatistics", "Display a brief statistics result", true);
    public Input<Boolean> m_bDisplayAll =
            new Input<>("displayAll", "Display all availble statistics result", false);
    public Input<Boolean> m_bReport =
            new Input<>("report", "Display all availble information", false);

    public Input<List<Expectation>> m_pExpectations = new Input<>("expectation",
            "Expectation of log statistics analysis regarding a loggable beastObject.",
            new ArrayList<Expectation>(), Validate.REQUIRED);
    public Input<MCMC> m_pMCMC = new Input<>("expectation",
            "Expectation of log statistics analysis regarding a loggable beastObject.", Validate.REQUIRED);

//    <run spec='beast.trace.LogAnalyser' fileName="test.$(seed).log" report="true">
//        <input idref='MCMC'/>   
//        <input spec='beast.trace.Expectation' traceName="hky.kappa" expectedValue="32.724"/>
//    </run>

    public List<TraceStatistics> traceStatisticsList = new ArrayList<TraceStatistics>(); // use for JUnit test

    static final String error1 = " is significantly different to the expectation : ";
    static final String error2 = " has very low effective sample sizes (ESS) !";
    private static final int fieldWidth = 14;
    private static final int firstField = 25;
    NumberFormatter formatter = new NumberFormatter(6);

    private List<String> warning = new ArrayList<String>();

    // this constructor is used by Unit test
    public LogAnalyser(String fileName, List<Expectation> expectations) throws Exception {
        this.m_sFileName.setValue(fileName, this);
        this.m_pExpectations.get().addAll(expectations);
        LogFileTraces traces = readLog(m_sFileName.get(), -1);
        analyseLog(traces);
    }

    @Override
    public void initAndValidate() {

    }

    @Override
    public void run() throws Exception {
        LogFileTraces traces = readLog(m_sFileName.get(), m_iBurnIn.get()); // if burnIn = -1 then burnIn = 0.1 * maxState
        analyseLog(traces);
    }

    public void analyseLog(LogFileTraces traces) throws TraceException, IOException {
        formatter.setPadding(true);
        formatter.setFieldWidth(fieldWidth);

        if (m_bReport.get()) tracesTittleReport(traces, m_bDisplayAll.get());

        for (int i = 0; i < traces.getTraceCount(); i++) {
            TraceStatistics distribution = traces.analyseTrace(i);
            traceStatisticsList.add(distribution);

            for (Expectation expectation : m_pExpectations.get()) {
                if (traces.getTraceName(i).equals(expectation.traceName.get())) {
                    if (!expectation.assertExpectation(distribution, m_bDisplayStat.get())) { // record isPassed in Expectation here
                        warning.add(" * WARNING: " + expectation.traceName.get() + " has significantly different value "
                                + distribution.getMean() + " with the expectation " + expectation.expValue.get() + " !");
                    }

                    if (!expectation.isValid()) {
                        warning.add(" * WARNING: " + expectation.traceName.get() + " has very low effective sample sizes (ESS) "
                                + distribution.getESS() + " !");
                    }

                    if (m_bReport.get()) {
                        traceStatisticalReport(expectation, m_bDisplayAll.get(), null);
                    }
                }
            }
        }

        if (m_bReport.get()) {
            System.out.println();
            for (String s : warning) { // print warning here
                System.out.println(s);
            }
        }
    }

    private void tracesTittleReport(LogFileTraces traces, boolean displayAllStatistics) {
        System.out.println();
        System.out.println("burnIn   <= " + traces.getBurnIn());
        System.out.println("maxState  = " + traces.getMaxState());
        System.out.println();

        System.out.print(formatter.formatToFieldWidth("statistic", firstField));
        String[] names;

        if (!displayAllStatistics)
            names = new String[]{"mean", "error", "test", "stdev", "hpdLower", "hpdUpper", "ESS"};
        else
            names = new String[]{"mean", "error", "test", "stdev", "hpdLower", "hpdUpper", "ESS", "stdErrorOfMeanFromLog",
                    "min", "max", "median", "geometricMean", "meanSquaredError", "variance", "cpdLower",
                    "cpdUpper", "autoCorrelationTime", "stdevAutoCorrelationTime"};

        for (String name : names) {
            System.out.print(formatter.formatToFieldWidth(name, fieldWidth));
        }
        System.out.println();

    }

    public void traceStatisticalReport(Expectation expectation, boolean displayAllStatistics,
                                       String likelihoodName) throws IOException, TraceException {

        TraceStatistics distribution = expectation.getTraceStatistics();
        double mean = distribution.getMean();
        double error = expectation.getStdError();

        System.out.print(formatter.formatToFieldWidth(expectation.traceName.get(), firstField));

        System.out.print(formatter.format(mean));
        System.out.print("+-" + formatter.format(error));

        if (expectation.isPassed()) {
            System.out.print(formatter.formatToFieldWidth("OK", fieldWidth));
        } else {
            System.out.print(formatter.formatToFieldWidth("FAILED", fieldWidth));
        }

        System.out.print(formatter.format(distribution.getStdev()));
        System.out.print(formatter.format(distribution.getHpdLower()));
        System.out.print(formatter.format(distribution.getHpdUpper()));
        System.out.print(formatter.format(distribution.getESS()));

        if (displayAllStatistics) {
            System.out.print(formatter.format(distribution.getStdErrorOfMean()));
            System.out.print(formatter.format(distribution.getMinimum()));
            System.out.print(formatter.format(distribution.getMaximum()));
            System.out.print(formatter.format(distribution.getMedian()));
            System.out.print(formatter.format(distribution.getGeometricMean()));
            System.out.print(formatter.format(distribution.getVariance()));
            System.out.print(formatter.format(distribution.getCpdLower()));
            System.out.print(formatter.format(distribution.getCpdUpper()));
            System.out.print(formatter.format(distribution.getAutoCorrelationTime()));
            System.out.print(formatter.format(distribution.getStdevAutoCorrelationTime()));
        }

        System.out.println();

        if (likelihoodName != null) {
//            System.out.println();
//            int traceIndex = -1;
//            for (int i = 0; i < traces.getTraceCount(); i++) {
//                String traceName = traces.getTraceName(i);
//                if (traceName.equals(likelihoodName)) {
//                    traceIndex = i;
//                    break;
//                }
//            }
//
//            if (traceIndex == -1) {
//                throw new TraceException("Column '" + likelihoodName +
//                        "' can not be found for marginal likelihood analysis.");
//            }
//
//            boolean harmonicOnly = false;
//            int bootstrapLength = 1000;
//
//            List<Double> sample = traces.getValues(traceIndex);
//
//            MarginalLikelihoodAnalysis analysis = new MarginalLikelihoodAnalysis(sample,
//                    traces.getTraceName(traceIndex), burnin, harmonicOnly, bootstrapLength);
//
//            System.out.println(analysis.toString());
        }

        System.out.flush();
    }

    public LogFileTraces readLog(String fileName, long inBurnin) throws TraceException, IOException {
        File file = new File(fileName);
        LogFileTraces traces = new LogFileTraces(fileName, file);
        traces.loadTraces();

        long burnin = inBurnin;
        if (burnin == -1) {
            burnin = traces.getMaxState() / 10;
        }

        traces.setBurnIn(burnin);

        return traces;
    }

    /**
     * @param burnin     the number of states of burnin or if -1 then use 10%
     * @param filename   the file name of the log file to report on
     * @param drawHeader if true then draw header
     * @param stdErr     if true then report the standard deviation of the mean
     * @param hpds       if true then report 95% hpd upper and lower
     * @param individualESSs minimum number of ESS with which to throw warning
     * @param likelihoodName column name
     * @return the traces loaded from given file to create this short report
     * @throws java.io.IOException if general error reading file
     * @throws TraceException      if trace file in wrong format or corrupted

    public static TraceList shortReport(String filename,
    final int burnin, boolean drawHeader,
    boolean hpds, boolean individualESSs, boolean stdErr,
    String likelihoodName) throws java.io.IOException, TraceException {

    TraceList traces = analyzeLogFile(filename, burnin);

    int maxState = traces.getMaxState();

    double minESS = Double.MAX_VALUE;

    if (drawHeader) {
    System.out.print("file\t");
    for (int i = 0; i < traces.getTraceCount(); i++) {
    String traceName = traces.getTraceName(i);
    System.out.print(traceName + "\t");
    if (stdErr)
    System.out.print(traceName + " stdErr\t");
    if (hpds) {
    System.out.print(traceName + " hpdLower\t");
    System.out.print(traceName + " hpdUpper\t");
    }
    if (individualESSs) {
    System.out.print(traceName + " ESS\t");
    }
    }
    System.out.print("minESS\t");
    if (likelihoodName != null) {
    System.out.print("marginal likelihood\t");
    System.out.print("stdErr\t");
    }
    System.out.println("chainLength");
    }

    System.out.print(filename + "\t");
    for (int i = 0; i < traces.getTraceCount(); i++) {
    //TraceDistribution distribution = traces.getDistributionStatistics(i);
    TraceCorrelation distribution = traces.getCorrelationStatistics(i);
    System.out.print(distribution.getMean() + "\t");
    if (stdErr)
    System.out.print(distribution.getStdErrorOfMean() + "\t");
    if (hpds) {
    System.out.print(distribution.getLowerHPD() + "\t");
    System.out.print(distribution.getUpperHPD() + "\t");
    }
    if (individualESSs) {
    System.out.print(distribution.getESS() + "\t");
    }
    double ess = distribution.getESS();
    if (ess < minESS) {
    minESS = ess;
    }
    }

    System.out.print(minESS + "\t");

    if (likelihoodName != null) {
    int traceIndex = -1;
    for (int i = 0; i < traces.getTraceCount(); i++) {
    String traceName = traces.getTraceName(i);
    if (traceName.equals(likelihoodName)) {
    traceIndex = i;
    break;
    }
    }

    if (traceIndex == -1) {
    throw new TraceException("Column '" + likelihoodName + "' can not be found in file " + filename + ".");
    }

    boolean harmonicOnly = false;
    int bootstrapLength = 1000;

    List<Double> sample = traces.getValues(traceIndex);

    MarginalLikelihoodAnalysis analysis = new MarginalLikelihoodAnalysis(sample,
    traces.getTraceName(traceIndex), burnin, harmonicOnly, bootstrapLength);

    System.out.print(analysis.getLogMarginalLikelihood() + "\t");
    System.out.print(analysis.getBootstrappedSE() + "\t");
    }

    System.out.println(maxState);
    return traces;
    } */

    /**
     * @param fileName the name of the log file to analyze
     * @param burnin   the state to discard up to
     * @return an array og analyses of the statistics in a log file.
     * @throws java.io.IOException if general error reading file
     * @throws TraceException      if trace file in wrong format or corrupted

    public static LogFileTraces analyzeLogFile(String fileName, int burnin) throws java.io.IOException, TraceException {

    File file = new File(fileName);
    LogFileTraces traces = new LogFileTraces(fileName, file);
    traces.loadTraces();
    traces.setBurnIn(burnin);

    for (int i = 0; i < traces.getTraceCount(); i++) {
    traces.analyseTrace(i);
    }
    return traces;
    } */

}
