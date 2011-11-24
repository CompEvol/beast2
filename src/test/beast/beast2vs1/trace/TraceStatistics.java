package test.beast.beast2vs1.trace;

import beast.util.HeapSort;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Walter Xie
 */
public class TraceStatistics {
    private static final int MAX_LAG = 2000;
    private boolean isValid = true;
    private boolean hasGeometricMean = false;

    private double minimum, maximum;
    private double mean;
    private double median;
    private double geometricMean;
    private double stdev;
    private double variance;
    private double cpdLower, cpdUpper, hpdLower, hpdUpper;
    private double ESS;

    private double stdErrorOfMean;
    private double autoCorrelationTime;
    private double stdevAutoCorrelationTime;

    public TraceStatistics(double[] values) {
        analyseDistributionContinuous(values, 0.95);
    }

    public TraceStatistics(double[] values, int stepSize) {
        this(values);

        if (isValid) {
            analyseCorrelationContinuous(values, stepSize);
        }
    }

    /**
     * @param valuesC    the values to analyze
     * @param proportion the proportion of probability mass included within interval.
     */
    private void analyseDistributionContinuous(double[] valuesC, double proportion) {

        mean = DiscreteStatistics.mean(valuesC);
        stdev = DiscreteStatistics.stdev(valuesC);
        variance = DiscreteStatistics.variance(valuesC);

        minimum = Double.POSITIVE_INFINITY;
        maximum = Double.NEGATIVE_INFINITY;

        for (double value : valuesC) {
            if (value < minimum) minimum = value;
            if (value > maximum) maximum = value;
        }

        if (minimum > 0) {
            geometricMean = DiscreteStatistics.geometricMean(valuesC);
            hasGeometricMean = true;
        }

        if (maximum == minimum) {
            isValid = false;
            return;
        }

        int[] indices = new int[valuesC.length];
        HeapSort.sort(valuesC, indices);
        median = DiscreteStatistics.quantile(0.5, valuesC, indices);
        cpdLower = DiscreteStatistics.quantile(0.025, valuesC, indices);
        cpdUpper = DiscreteStatistics.quantile(0.975, valuesC, indices);
        calculateHPDInterval(proportion, valuesC, indices);
        ESS = valuesC.length;

    }


    /**
     * @param proportion the proportion of probability mass included within interval.
     * @param array      the data array
     * @param indices    the indices of the ranks of the values (sort order)
     */
    private void calculateHPDInterval(double proportion, double[] array, int[] indices) {
        final double[] hpd = DiscreteStatistics.HPDInterval(proportion, array, indices);
        hpdLower = hpd[0];
        hpdUpper = hpd[1];
    }

    /**
     * Analyze trace
     *
     * @param values   the values
     * @param stepSize the sampling frequency of the values
     */
    private void analyseCorrelationContinuous(double[] values, int stepSize) {

        final int samples = values.length;
        int maxLag = Math.min(samples - 1, MAX_LAG);

        double[] gammaStat = new double[maxLag];
        //double[] varGammaStat = new double[maxLag];
        double varStat = 0.0;
        //double varVarStat = 0.0;
        //double assVarCor = 1.0;
        //double del1, del2;

        for (int lag = 0; lag < maxLag; lag++) {
            for (int j = 0; j < samples - lag; j++) {
                final double del1 = values[j] - mean;
                final double del2 = values[j + lag] - mean;
                gammaStat[lag] += (del1 * del2);
                //varGammaStat[lag] += (del1*del1*del2*del2);
            }

            gammaStat[lag] /= ((double) (samples - lag));
            //varGammaStat[lag] /= ((double) samples-lag);
            //varGammaStat[lag] -= (gammaStat[0] * gammaStat[0]);

            if (lag == 0) {
                varStat = gammaStat[0];
                //varVarStat = varGammaStat[0];
                //assVarCor = 1.0;
            } else if (lag % 2 == 0) {
                // fancy stopping criterion :)
                if (gammaStat[lag - 1] + gammaStat[lag] > 0) {
                    varStat += 2.0 * (gammaStat[lag - 1] + gammaStat[lag]);
                    // varVarStat += 2.0*(varGammaStat[lag-1] + varGammaStat[lag]);
                    // assVarCor  += 2.0*((gammaStat[lag-1] * gammaStat[lag-1]) + (gammaStat[lag] * gammaStat[lag])) / (gammaStat[0] * gammaStat[0]);
                }
                // stop
                else
                    maxLag = lag;
            }
        }

        // standard error of mean
        stdErrorOfMean = Math.sqrt(varStat / samples);
        // auto correlation time
        autoCorrelationTime = stepSize * varStat / gammaStat[0];
        // effective sample size
        ESS = (stepSize * samples) / autoCorrelationTime;
        // standard deviation of autocorrelation time
        stdevAutoCorrelationTime = (2.0 * Math.sqrt(2.0 * (2.0 * (double) (maxLag + 1)) / samples) * (varStat / gammaStat[0]) * stepSize);

        isValid = true;
    }

    public double getMinimum() {
        return minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    public double getMean() {
        return mean;
    }

    public double getMedian() {
        return median;
    }

    public double getGeometricMean() {
        if (hasGeometricMean) return geometricMean;
        return Double.NaN;
    }

    public double getStdev() {
        return stdev;
    }

    public double getVariance() {
        return variance;
    }

    public double getCpdLower() {
        return cpdLower;
    }

    public double getCpdUpper() {
        return cpdUpper;
    }

    public double getHpdLower() {
        return hpdLower;
    }

    public double getHpdUpper() {
        return hpdUpper;
    }

    public double getESS() {
        return ESS;
    }

    public double getStdErrorOfMean() {
        return stdErrorOfMean;
    }

    public double getAutoCorrelationTime() {
        return autoCorrelationTime;
    }

    public double getStdevAutoCorrelationTime() {
        return stdevAutoCorrelationTime;
    }
}
