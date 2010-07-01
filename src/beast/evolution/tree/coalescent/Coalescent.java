package beast.evolution.tree.coalescent;

import beast.core.Description;
import beast.core.Input;
import beast.core.ProbabilityDistribution;
import beast.core.State;
import beast.evolution.tree.Tree;
import beast.math.Binomial;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author Alexei Drummond
 */

@Description("Calculates the probability of a beast.tree conditional on a population size function.")
public class Coalescent extends ProbabilityDistribution {

    public Input<Tree> tree = new Input<Tree>("tree", "phylogenetic beast.tree");
    public Input<PopulationFunction.Abstract> popSize = new Input<PopulationFunction.Abstract>("populationModel", "A population size model");


    /**
     * do the actual calculation *
     */
    public double calculateLogP(State state) throws Exception {

        Tree stateTree = (Tree) state.getStateNode(tree);

        popSize.get().setState(state);

        TreeIntervals intervals = new TreeIntervals(stateTree);

        logP = calculateLogLikelihood(intervals, popSize.get());

        return logP;
    }

    @Override
    public void sample(State state, Random random) {
        // TODO this should eventually sample a coalescent tree conditional on population size function
        throw new UnsupportedOperationException("This should eventually sample a coalescent tree conditional on population size function.");
    }

    /**
     * @return a list of unique ids for the state nodes that form the argument
     */
    public List<String> getArguments() {
        return Collections.singletonList(tree.get().getID());
    }

    /**
     * @return a list of unique ids for the state nodes that make up the conditions
     */
    public List<String> getConditions() {
        return popSize.get().getParameterIds();
    }


    /**
     * Calculates the log likelihood of this set of coalescent intervals,
     * given a demographic model.
     *
     * @param intervals       the intervals whose likelihood is computed
     * @param popSizeFunction the population size function
     * @return the log likelihood of the intervals given the population size function
     */
    public static double calculateLogLikelihood(IntervalList intervals, PopulationFunction popSizeFunction) {
        return calculateLogLikelihood(intervals, popSizeFunction, 0.0);
    }

    /**
     * Calculates the log likelihood of this set of coalescent intervals,
     * given a population size function.
     *
     * @param intervals       the intervals whose likelihood is computed
     * @param popSizeFunction the population size function
     * @param threshold       the minimum allowable coalescent interval size; negative infinity will be returned if
     *                        any non-zero intervals are smaller than this
     * @return the log likelihood of the intervals given the population size function
     */
    public static double calculateLogLikelihood(IntervalList intervals, PopulationFunction popSizeFunction, double threshold) {

        double logL = 0.0;

        double startTime = 0.0;
        final int n = intervals.getIntervalCount();
        for (int i = 0; i < n; i++) {

            final double duration = intervals.getInterval(i);
            final double finishTime = startTime + duration;

            final double intervalArea = popSizeFunction.getIntegral(startTime, finishTime);
            if (intervalArea == 0 && duration != 0) {
                return Double.NEGATIVE_INFINITY;
            }
            final int lineageCount = intervals.getLineageCount(i);

            final double kChoose2 = Binomial.choose2(lineageCount);
            // common part
            logL += -kChoose2 * intervalArea;

            if (intervals.getIntervalType(i) == IntervalType.COALESCENT) {

                final double demographicAtCoalPoint = popSizeFunction.getPopSize(finishTime);

                // if value at end is many orders of magnitude different than mean over interval reject the interval
                // This is protection against cases where ridiculous infitisimal
                // population size at the end of a linear interval drive coalescent values to infinity.

                if (duration == 0.0 || demographicAtCoalPoint * (intervalArea / duration) >= threshold) {
                    //                if( duration == 0.0 || demographicAtCoalPoint >= threshold * (duration/intervalArea) ) {
                    logL -= Math.log(demographicAtCoalPoint);
                } else {
                    // remove this at some stage
                    //  System.err.println("Warning: " + i + " " + demographicAtCoalPoint + " " + (intervalArea/duration) );
                    return Double.NEGATIVE_INFINITY;
                }
            }
            startTime = finishTime;
        }
        return logL;
    }
}
