package beast.tree.coalescent;

import beast.core.Description;
import beast.core.Plugin;
import beast.core.State;
import beast.math.Binomial;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.integration.RombergIntegrator;
import beast.util.Randomizer;


/**
 * This interface provides methods that describe a population size function.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Korbinian Strimmer
 */
public interface PopulationFunction extends UnivariateRealFunction {

    /**
     * @param t time
     * @return value of the demographic function N(t) at time t
     */
    double getPopSize(double t);

    double getLogPopSize(double t);

    /**
     * @param t time
     * @return value of demographic intensity function at time t (= integral 1/N(x) dx from 0 to t).
     */
    double getIntensity(double t);

    /**
     * @param x the coalescent intensity
     * @return value of inverse demographic intensity function
     *         (returns time, needed for simulation of coalescent intervals).
     */
    double getInverseIntensity(double x);

    /**
     * Calculates the integral 1/N(x) dx between start and finish.
     *
     * @param start  point
     * @param finish point
     * @return integral value
     */
    double getIntegral(double start, double finish);

    /**
     * @return the number of arguments for this function.
     */
    int getNumArguments();

    /**
     * @param n the index of argument to retrieve the name of
     * @return the name of the n'th argument of this function.
     */
    String getArgumentName(int n);

    /**
     * @param n the argument index
     * @return the value of the n'th argument of this function.
     */
    double getArgument(int n);

    /**
     * @param n     the argument index
     * @param value the value to set for the n'th argument
     *              Sets the value of the nth argument of this function.
     */
    void setArgument(int n, double value);

    /**
     * @param n the argument index
     * @return the lower bound of the nth argument of this function.
     */
    double getLowerBound(int n);

    /**
     * @param n the argument index
     * @return the upper bound of the nth argument of this function.
     */
    double getUpperBound(int n);

    /**
     * @return a copy of this function.
     */
    PopulationFunction getCopy();

    /**
     * A threshold for underflow on calculation of likelihood of internode intervals.
     * Most population size functions could probably return 0.0 but (e.g.,) the Extended Skyline
     * needs a non zero value to prevent a numerical problem.
     *
     * @return the minimum coalescent interval
     */
    double getThreshold();

    @Description("An abstract implementation of a population size function plugin.")
    public abstract class Abstract extends Plugin implements PopulationFunction {

        RombergIntegrator numericalIntegrator = new RombergIntegrator();
        State state;

        /**
         * Construct demographic model with default settings
         */
        public Abstract() {
        }

        // general functions

        /**
         * Default implementation
         *
         * @param t the time
         * @return log(demographic(t))
         */
        public double getLogPopSize(double t) {
            return Math.log(getPopSize(t));
        }

        public double getThreshold() {
            return 0;
        }

        public void setState(State state) {
            this.state = state;
        }

        /**
         * Calculates the integral 1/N(x) dx between start and finish.
         */
        public double getIntegral(double start, double finish) {
            return getIntensity(finish) - getIntensity(start);
        }

        /**
         * @param start  the start time of the definite integral
         * @param finish the end time of the definite integral
         * @return the integral of 1/N(x) between start and finish, calling either the getAnalyticalIntegral or
         *         getNumericalIntegral function as appropriate.
         */
        public double getNumericalIntegral(double start, double finish) {
            // AER 19th March 2008: I switched this to use the RombergIntegrator from
            // commons-beast.math v1.2.

            if (start > finish) {
                throw new RuntimeException("NumericalIntegration start > finish");
            }

            if (start == finish) {
                return 0.0;
            }

            try {
                return numericalIntegrator.integrate(this, start, finish);
            } catch (MaxIterationsExceededException e) {
                throw new RuntimeException(e);
            } catch (FunctionEvaluationException e) {
                throw new RuntimeException(e);
            }
        }

        // **************************************************************
        // UnivariateRealFunction IMPLEMENTATION
        // **************************************************************

        /**
         * Return the intensity at a given time for numerical integration
         *
         * @param x the time
         * @return the intensity
         */
        public double value(double x) {
            return 1.0 / getPopSize(x);
        }
    }

    public static class Utils {
        private static double getInterval(double U, PopulationFunction populationFunction,
                                          int lineageCount, double timeOfLastCoalescent) {
            final double intensity = populationFunction.getIntensity(timeOfLastCoalescent);
            final double tmp = -Math.log(U) / Binomial.choose2(lineageCount) + intensity;

            return populationFunction.getInverseIntensity(tmp) - timeOfLastCoalescent;
        }

        /**
         * @param populationFunction   the population size function
         * @param lineageCount         the number of lineages spanning the interval
         * @param timeOfLastCoalescent the start time for the interval to be simulated
         * @return a random interval size selected from the Kingman prior of the demographic model.
         */
        public static double getSimulatedInterval(PopulationFunction populationFunction,
                                                  int lineageCount, double timeOfLastCoalescent) {
            final double U = Randomizer.nextDouble(); // create unit uniform random variate
            return getInterval(U, populationFunction, lineageCount, timeOfLastCoalescent);
        }

        public static double getMedianInterval(PopulationFunction populationFunction,
                                               int lineageCount, double timeOfLastCoalescent) {
            return getInterval(0.5, populationFunction, lineageCount, timeOfLastCoalescent);
        }

        /**
         * This function tests the consistency of the
         * getIntensity and getInverseIntensity methods
         * of this demographic model. If the model is
         * inconsistent then a RuntimeException will be thrown.
         *
         * @param populationFunction the population size function to test.
         * @param steps              the number of steps between 0.0 and maxTime to test.
         * @param maxTime            the maximum time to test.
         */
        public static void testConsistency(PopulationFunction populationFunction, int steps, double maxTime) {

            double delta = maxTime / (double) steps;

            for (int i = 0; i <= steps; i++) {
                double time = (double) i * delta;
                double intensity = populationFunction.getIntensity(time);
                double newTime = populationFunction.getInverseIntensity(intensity);

                if (Math.abs(time - newTime) > 1e-12) {
                    throw new RuntimeException(
                            "Demographic model not consistent! error size = " +
                                    Math.abs(time - newTime));
                }
            }
        }
    }


}
