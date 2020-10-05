package beast.core.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Loggable;


//import beast.core.Distribution;

@Description("Report effective sample size of a parameter or log values from a distribution. " +
        "This uses the same criterion as Tracer and assumes 10% burn in.")
public class ESS extends BEASTObject implements Loggable {
    final public Input<Function> functionInput =
            new Input<>("arg", "value (e.g. parameter or distribution) to report ESS for", Validate.REQUIRED);

    /**
     * values from which the ESS is calculated *
     */
    protected List<Double> trace;
    /**
     * sum of trace, excluding burn-in *
     */
    protected double sum = 0;
    /**
     * keep track of sums of trace(i)*trace(i_+ lag) for all lags, excluding burn-in  *
     */
    protected List<Double> squareLaggedSums;

    @Override
    public void initAndValidate() {
        trace = new ArrayList<>();
        squareLaggedSums = new ArrayList<>();
    }

    @Override
    public void init(PrintStream out) {
        final String id = ((BEASTObject) functionInput.get()).getID();
        out.print("ESS(" + id + ")\t");
    }

    final static int MAX_LAG = 2000;

//  We determine the Effective Sample Size (ESS) based on the auto correlation (AC) between the sequence and the same
//  sequence delayed by some amount.  For a highly correlated sequence the AC will be high for a small delay,
//  and is expected to drop to around zero when the delay is large enough. The delay when the AC is zero is the ACT (auto
//  correlation time), and the ESS is the number of samples remaining when keeping only one sample out of every ACT.
//
//  The (squared) auto correlation between two sequences is the covariance divided by the product of the individual
//  variances. Since both sequences are essentially the same sequence we do not bother to scale.
//
//  The simplest criteria to use to find the point where the AC "gets" to zero is to take the first time it becomes
//  negative. This is deemed too simple and instead we first find the approximate point - the first time where the sum of
//  two consecutive values is negative, and then determine the ACT by assuming the AC - as a function of the delay - is
//  roughly linear and so the ACT (the point on the X axis) is approximately equal to twice the area under the curve divided
//  by the value at x=0 (the AC of the sequence). This is the reason for summing up twice the variances inside the loop - a
//  basic numerical integration technique.

    @Override
    public void log(final long sample, PrintStream out) {
        final Double newValue = functionInput.get().getArrayValue();
        trace.add(newValue);
        sum += newValue;

        final int totalSamples = trace.size();

        // take 10% burn in
        final int start = totalSamples / 10;
        if (start != ((totalSamples - 1) / 10)) {
            // compensate for 10% burnin
            sum -= trace.get((totalSamples - 1) / 10);
        }
        final int sampleCount = totalSamples - start;
        final int maxLag = Math.min(sampleCount, MAX_LAG);

        // calculate mean
        final double mean = sum / sampleCount;

        if (start != ((totalSamples - 1) / 10)) {
            // compensate for 10% burnin
            int traceIndex = ((totalSamples - 1) / 10);
            for (int lagIndex = 0; lagIndex < squareLaggedSums.size(); lagIndex++) {
                squareLaggedSums.set(lagIndex, squareLaggedSums.get(lagIndex) - trace.get(traceIndex) * trace.get(traceIndex + lagIndex));
            }
        }

        while (squareLaggedSums.size() < maxLag) {
            squareLaggedSums.add(0.0);
        }

        // calculate auto correlation for selected lag times
        double[] autoCorrelation = new double[maxLag];
        // sum1 = \sum_{start ... totalSamples-lagIndex-1} trace
        double sum1 = sum;
        // sum2 = \sum_{start+lagIndex ... totalSamples-1} trace
        double sum2 = sum;
        for (int lag = 0; lag < maxLag; lag++) {
            squareLaggedSums.set(lag, squareLaggedSums.get(lag) + trace.get(totalSamples - lag - 1) * trace.get(totalSamples - 1));
            // The following line is the same approximation as in Tracer 
            // (valid since mean *(samples - lag), sum1, and sum2 are approximately the same)
            // though a more accurate estimate would be
            // autoCorrelation[lag] = m_fSquareLaggedSums.get(lag) - sum1 * sum2
            autoCorrelation[lag] = squareLaggedSums.get(lag) - (sum1 + sum2) * mean + mean * mean * (sampleCount - lag);
            autoCorrelation[lag] /= (sampleCount - lag);
            sum1 -= trace.get(totalSamples - 1 - lag);
            sum2 -= trace.get(start + lag);
        }

        double integralOfACFunctionTimes2 = 0.0;
        for (int lagIndex = 0; lagIndex < maxLag; lagIndex++) {
            if (lagIndex == 0) {
                integralOfACFunctionTimes2 = autoCorrelation[0];
            } else if (lagIndex % 2 == 0) {
                // fancy stopping criterion - see main comment
                if (autoCorrelation[lagIndex - 1] + autoCorrelation[lagIndex] > 0) {
                    integralOfACFunctionTimes2 += 2.0 * (autoCorrelation[lagIndex - 1] + autoCorrelation[lagIndex]);
                } else {
                    // stop
                    break;
                }
            }
        }

        // auto correlation time
        final double act = integralOfACFunctionTimes2 / autoCorrelation[0];

        // effective sample size
        final double ess = sampleCount / act;
        String str = ess + "";
        str = str.substring(0, str.indexOf('.') + 2);
        out.print(str + "\t");
    } // log

    @Override
    public void close(PrintStream out) {
        // nothing to do
    }


    /**
     * return ESS time of a sample, batch version.
     * Can be used to calculate effective sample size
     *
     * @param trace:         values from which the ACT is calculated
     * @param sampleInterval time between samples *
     */
    public static double calcESS(List<Double> trace) {
        return calcESS(trace.toArray(new Double[0]), 1);
    }

    public static double calcESS(Double[] trace, int sampleInterval) {
        return trace.length / (ACT(trace, sampleInterval) / sampleInterval);
    }

    public static double ACT(Double[] trace, int sampleInterval) {
        /** sum of trace, excluding burn-in **/
        double sum = 0.0;
        /** keep track of sums of trace(i)*trace(i_+ lag) for all lags, excluding burn-in  **/
        double[] squareLaggedSums = new double[MAX_LAG];
        double[] autoCorrelation = new double[MAX_LAG];
        for (int i = 0; i < trace.length; i++) {
            sum += trace[i];
            // calculate mean
            final double mean = sum / (i + 1);

            // calculate auto correlation for selected lag times
            // sum1 = \sum_{start ... totalSamples-lag-1} trace
            double sum1 = sum;
            // sum2 = \sum_{start+lag ... totalSamples-1} trace
            double sum2 = sum;
            for (int lagIndex = 0; lagIndex < Math.min(i + 1, MAX_LAG); lagIndex++) {
                squareLaggedSums[lagIndex] = squareLaggedSums[lagIndex] + trace[i - lagIndex] * trace[i];
                // The following line is the same approximation as in Tracer
                // (valid since mean *(samples - lag), sum1, and sum2 are approximately the same)
                // though a more accurate estimate would be
                // autoCorrelation[lag] = m_fSquareLaggedSums.get(lag) - sum1 * sum2
                autoCorrelation[lagIndex] = squareLaggedSums[lagIndex] - (sum1 + sum2) * mean + mean * mean * (i + 1 - lagIndex);
                autoCorrelation[lagIndex] /= (i + 1 - lagIndex);
                sum1 -= trace[i - lagIndex];
                sum2 -= trace[lagIndex];
            }
        }

        final int maxLag = Math.min(trace.length, MAX_LAG);
        double integralOfACFunctionTimes2 = 0.0;
        for (int lagIndex = 0; lagIndex < maxLag; lagIndex++) //{
            if (lagIndex == 0) //{
                integralOfACFunctionTimes2 = autoCorrelation[0];
            else if (lagIndex % 2 == 0)
                // fancy stopping criterion - see main comment in Tracer code of BEAST 1
                if (autoCorrelation[lagIndex - 1] + autoCorrelation[lagIndex] > 0) //{
                    integralOfACFunctionTimes2 += 2.0 * (autoCorrelation[lagIndex - 1] + autoCorrelation[lagIndex]);
                else
                    // stop
                    break;
        //}
        //}
        //}

        // auto correlation time
        return sampleInterval * integralOfACFunctionTimes2 / autoCorrelation[0];
    }

    public static double stdErrorOfMean(Double[] trace, int sampleInterval) {
        /** sum of trace, excluding burn-in **/
        double sum = 0.0;
        /** keep track of sums of trace(i)*trace(i_+ lag) for all lags, excluding burn-in  **/
        double[] squareLaggedSums = new double[MAX_LAG];
        double[] autoCorrelation = new double[MAX_LAG];
        for (int i = 0; i < trace.length; i++) {
            sum += trace[i];
            // calculate mean
            final double mean = sum / (i + 1);

            // calculate auto correlation for selected lag times
            // sum1 = \sum_{start ... totalSamples-lag-1} trace
            double sum1 = sum;
            // sum2 = \sum_{start+lag ... totalSamples-1} trace
            double sum2 = sum;
            for (int lagIndex = 0; lagIndex < Math.min(i + 1, MAX_LAG); lagIndex++) {
                squareLaggedSums[lagIndex] = squareLaggedSums[lagIndex] + trace[i - lagIndex] * trace[i];
                // The following line is the same approximation as in Tracer
                // (valid since mean *(samples - lag), sum1, and sum2 are approximately the same)
                // though a more accurate estimate would be
                // autoCorrelation[lag] = m_fSquareLaggedSums.get(lag) - sum1 * sum2
                autoCorrelation[lagIndex] = squareLaggedSums[lagIndex] - (sum1 + sum2) * mean + mean * mean * (i + 1 - lagIndex);
                autoCorrelation[lagIndex] /= (i + 1 - lagIndex);
                sum1 -= trace[i - lagIndex];
                sum2 -= trace[lagIndex];
            }
        }

        final int maxLag = Math.min(trace.length, MAX_LAG);
        double integralOfACFunctionTimes2 = 0.0;
        for (int lagIndex = 0; lagIndex < maxLag; lagIndex++) //{
            if (lagIndex == 0) //{
                integralOfACFunctionTimes2 = autoCorrelation[0];
            else if (lagIndex % 2 == 0)
                // fancy stopping criterion - see main comment in Tracer code of BEAST 1
                if (autoCorrelation[lagIndex - 1] + autoCorrelation[lagIndex] > 0) //{
                    integralOfACFunctionTimes2 += 2.0 * (autoCorrelation[lagIndex - 1] + autoCorrelation[lagIndex]);
                else
                    // stop
                    break;
        //}
        //}
        //}

        // auto correlation time
        return Math.sqrt(integralOfACFunctionTimes2 / trace.length);
    }

} // class ESS
