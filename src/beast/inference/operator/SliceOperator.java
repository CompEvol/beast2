package beast.inference.operator;

import java.text.DecimalFormat;

import beast.base.Description;
import beast.base.Input;
import beast.base.Log;
import beast.base.Input.Validate;
import beast.inference.Distribution;
import beast.inference.Evaluator;
import beast.inference.Operator;
import beast.inference.parameter.RealParameter;
import beast.util.Randomizer;




@Description("A random walk operator that selects a random dimension of the real parameter and perturbs the value a " +
        "random amount within +/- windowSize.")
public class SliceOperator extends Operator {
    final public Input<RealParameter> parameterInput =
            new Input<>("parameter", "the parameter to operate a random walk on.", Validate.REQUIRED);
    final public Input<Double> windowSizeInput =
            new Input<>("windowSize", "the size of the step for finding the slice boundaries", Input.Validate.REQUIRED);
    final public Input<Distribution> sliceDensityInput =
            new Input<>("sliceDensity", "The density to sample from using slice sampling.", Input.Validate.REQUIRED);

    Double totalDelta;
    int totalNumber;
    int n_learning_iterations;
    double W;

    double windowSize = 1;
    Distribution sliceDensity;

    @Override
	public void initAndValidate() {
        totalDelta = 0.0;
        totalNumber = 0;
        n_learning_iterations = 100;
        W = 0.0;
        windowSize = windowSizeInput.get();
        sliceDensity = sliceDensityInput.get();
    }

    boolean in_range(RealParameter X, double x) {
        return (X.getLower() < x && x < X.getUpper());
    }

    boolean below_lower_bound(RealParameter X, double x) {
        return (x < X.getLower());
    }

    boolean above_upper_bound(RealParameter X, double x) {
        return (x > X.getUpper());
    }

    @Override
	public Distribution getEvaluatorDistribution() {
        return sliceDensity;
    }

    Double evaluate(Evaluator E) {
        return E.evaluate();
    }

    Double evaluate(Evaluator E, RealParameter X, double x) {
        X.setValue(0, x);
        return evaluate(E);
    }

    Double[] find_slice_boundaries_stepping_out(Evaluator E, RealParameter X, double logy, double w, int m) {
        double x0 = X.getValue(0);

        assert in_range(X, x0);

        double u = Randomizer.nextDouble() * w;
        Double L = x0 - u;
        Double R = x0 + (w - u);

        // Expand the interval until its ends are outside the slice, or until
        // the limit on steps is reached.

        if (m > 1) {
            int J = (int) Math.floor(Randomizer.nextDouble() * m);
            int K = (m - 1) - J;

            while (J > 0 && (!below_lower_bound(X, L)) && evaluate(E, X, L) > logy) {
                L -= w;
                J--;
            }

            while (K > 0 && (!above_upper_bound(X, R)) && evaluate(E, X, R) > logy) {
                R += w;
                K--;
            }
        } else {
            while ((!below_lower_bound(X, L)) && evaluate(E, X, L) > logy)
                L -= w;

            while ((!above_upper_bound(X, R)) && evaluate(E, X, R) > logy)
                R += w;
        }

        // Shrink interval to lower and upper bounds.

        if (below_lower_bound(X, L))
            L = X.getLower();
        if (above_upper_bound(X, R))
            R = X.getUpper();

        assert L < R;

        Double[] range = {L, R};
        return range;
    }

    // Does this x0 really need to be the original point?
    // I think it just serves to let you know which way the interval gets shrunk...

    double search_interval(Evaluator E, double x0, RealParameter X, Double L, Double R, double logy) {
        //	assert evaluate(E,x0) > evaluate(E,L) && evaluate(E,x0) > evaluate(E,R);

        assert evaluate(E, X, x0) >= logy;
        assert L < R;
        assert L <= x0 && x0 <= R;

        double L0 = L, R0 = R;

        double gx0 = evaluate(E, X, x0);
        assert logy < gx0;

        double x1 = x0;
        for (int i = 0; i < 200; i++) {
            x1 = L + Randomizer.nextDouble() * (R - L);
            double gx1 = evaluate(E, X, x1);

            //	    System.err.println("    L0 = " + L0 + "   x0 = " + x0 + "   R0 = " + R0 + "   gx0 = " + gx0);
            //	    System.err.println("    L  = " + L  + "   x1 = " + x1 + "   R  = " + R0 + "   gx1 = " + gx1);
            //	    System.err.println("    logy  = " + logy);

            if (gx1 >= logy) return x1;

            if (x1 > x0)
                R = x1;
            else
                L = x1;
        }
        Log.warning.println("Warning!  Is size of the interval really ZERO?");
        //	double logy_x0 = evaluate(E,X,x0);
        Log.warning.println("    L0 = " + L0 + "   x0 = " + x0 + "   R0 = " + R0 + "   gx0 = " + gx0);
        Log.warning.println("    L  = " + L + "   x1 = " + x1 + "   R  = " + R0 + "   gx1 = " + evaluate(E));

        return x0;
    }


    @Override
    public double proposal() {
        return 0;
    }

    /**
     * override this for proposals,
     * returns log of hastingRatio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal(Evaluator E) {

        int m = 100;

        RealParameter X = parameterInput.get();

        // Find the density at the current point
        Double gx0 = evaluate(E);

        //	System.err.println("gx0 = " + gx0);
        // Get the 1st element
        Double x0 = X.getValue(0);
        //	System.err.println("x0 = " + x0);

        // Determine the slice level, in log terms.
        double logy = gx0 - Randomizer.nextExponential(1);

        // Find the initial interval to sample from.
        Double[] range = find_slice_boundaries_stepping_out(E, X, logy, windowSize, m);
        Double L = range[0];
        Double R = range[1];

        // Sample from the interval, shrinking it on each rejection
        double x_new = search_interval(E, x0, X, L, R, logy);
        X.setValue(x_new);

        if (n_learning_iterations > 0) {
            n_learning_iterations--;

            totalDelta += Math.abs(x_new - x0);
            totalNumber++;

            double W_predicted = totalDelta / totalNumber * 4.0;
            if (totalNumber > 3) {
                W = 0.95 * W + 0.05 * W_predicted;
                windowSize = W;
            }
            //	    System.err.println("W = " + W);
        }

        return Double.POSITIVE_INFINITY;
    }


    @Override
    public double getCoercableParameterValue() {
        return windowSize;
    }

    @Override
    public void setCoercableParameterValue(double value) {
        windowSize = value;
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
        double delta = calcDelta(logAlpha);

        delta += Math.log(windowSize);
        windowSize = Math.exp(delta);
    }

    @Override
    public final String getPerformanceSuggestion() {
        // new scale factor
        double newWindowSize = totalDelta / totalNumber * 4;

        if (newWindowSize / windowSize < 0.8 || newWindowSize / windowSize > 1.2) {
            DecimalFormat formatter = new DecimalFormat("#.###");
            return "Try setting window size to about " + formatter.format(newWindowSize);
        } else
            return "";
    }
} // class IntRandomWalkOperator