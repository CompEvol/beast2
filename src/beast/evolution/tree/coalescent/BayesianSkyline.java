package beast.evolution.tree.coalescent;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Function;
import beast.core.Input;
import beast.core.State;
import beast.core.BEASTObject;
import beast.core.Input.Validate;
import beast.core.parameter.IntegerParameter;
import beast.evolution.speciation.SpeciesTreeDistribution;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeDistribution;
import beast.math.Binomial;



/**
 * @author Alexei Drummond
 */
@Description("A likelihood function for the generalized skyline plot coalescent.")
public class BayesianSkyline extends TreeDistribution {
//public class BayesianSkyline extends PopulationFunction.Abstract {

    final public Input<Function> popSizeParamInput = new Input<>("popSizes", "present-day population size. "
            + "If time units are set to Units.EXPECTED_SUBSTITUTIONS then"
            + "the N0 parameter will be interpreted as N0 * mu. "
            + "Also note that if you are dealing with a diploid population " + "N0 will be out by a factor of 2.",
            Validate.REQUIRED);
    final public Input<IntegerParameter> groupSizeParamInput = new Input<>("groupSizes",
            "the group sizes parameter", Validate.REQUIRED);
    // public Input<Tree> treeInput = new Input<>("tree",
    // "The tree containing coalescent node times for use in defining BSP.");
//	public Input<TreeIntervals> m_treeIntervals = new Input<>("treeIntervals",
//			"The intervals of the tree containing coalescent node times for use in defining BSP.", Validate.REQUIRED);

    Function popSizes;
    IntegerParameter groupSizes;
    Tree tree;
    TreeIntervals intervals;
    double[] coalescentTimes;

    int[] cumulativeGroupSizes;
    boolean m_bIsPrepared = false;

    public BayesianSkyline() {
    }

    // /**
    // * This pseudo-constructor is only used for junit tests
    // *
    // * @param populationSize
    // * @param groupSizes
    // * @param tree
    // * @throws Exception
    // */
    // public void init(RealParameter populationSize, IntegerParameter
    // groupSizes, Tree tree) throws Exception {
    // super.init(populationSize, groupSizes, tree);
    // }

    public void initAndValidate() throws Exception {
        if (treeInput.get() != null) {
            throw new Exception("only tree intervals (not tree) should not be specified");
        }
        intervals = treeIntervalsInput.get();
        groupSizes = groupSizeParamInput.get();
        popSizes = popSizeParamInput.get();

        // make sure that the sum of groupsizes == number of coalescent events
        int events = intervals.treeInput.get().getInternalNodeCount();
        if (groupSizes.getDimension() > events) {
            throw new IllegalArgumentException("There are more groups than coalescent nodes in the tree.");
        }
        int paramDim2 = groupSizes.getDimension();

        int eventsCovered = 0;
        for (int i = 0; i < groupSizes.getDimension(); i++) {
            eventsCovered += groupSizes.getValue(i);
        }

        if (eventsCovered != events) {
            if (eventsCovered == 0 || eventsCovered == paramDim2) {
                // double[] uppers = new double[paramDim2];
                // double[] lowers = new double[paramDim2];

                // For these special cases we assume that the XML has not
                // specified initial group sizes
                // or has set all to 1 and we set them here automatically...
                int eventsEach = events / paramDim2;
                int eventsExtras = events % paramDim2;
                Integer[] values = new Integer[paramDim2];
                for (int i = 0; i < paramDim2; i++) {
                    if (i < eventsExtras) {
                        values[i] = eventsEach + 1;
                    } else {
                        values[i] = eventsEach;
                    }
                    // uppers[i] = Double.MAX_VALUE;
                    // lowers[i] = 1.0;
                }

                // if (type == EXPONENTIAL_TYPE || type == LINEAR_TYPE) {
                // lowers[0] = 2.0;
                // }
                IntegerParameter parameter = new IntegerParameter(values);
                parameter.setBounds(1, Integer.MAX_VALUE);
                groupSizes.assignFromWithoutID(parameter);
            } else {
                // ... otherwise assume the user has made a mistake setting
                // initial group sizes.
                throw new IllegalArgumentException(
                        "The sum of the initial group sizes does not match the number of coalescent events in the tree.");
            }
        }

        prepare();
    }

    public void prepare() {
        cumulativeGroupSizes = new int[groupSizes.getDimension()];

        int intervalCount = 0;
        for (int i = 0; i < cumulativeGroupSizes.length; i++) {
            intervalCount += groupSizes.getValue(i);
            cumulativeGroupSizes[i] = intervalCount;
        }

        coalescentTimes = intervals.getCoalescentTimes(coalescentTimes);

        assert (intervals.getSampleCount() == intervalCount);
        m_bIsPrepared = true;
    }

    /**
     * CalculationNode methods *
     */
    @Override
    protected boolean requiresRecalculation() {
        m_bIsPrepared = false;
        return true;
    }

    @Override
    public void store() {
        m_bIsPrepared = false;
        super.store();
    }

    @Override
    public void restore() {
        m_bIsPrepared = false;
        super.restore();
    }

    public List<String> getParameterIds() {

        List<String> paramIDs = new ArrayList<>();
        paramIDs.add(((BEASTObject) popSizes).getID());
        paramIDs.add(groupSizes.getID());

        return paramIDs;
    }

    /**
     * Calculates the log likelihood of this set of coalescent intervals, given
     * a demographic model.
     */
    @Override
    public double calculateLogP() throws Exception {
        if (!m_bIsPrepared) {
            prepare();
        }

        logP = 0.0;

        double currentTime = 0.0;

        int groupIndex = 0;
        // int[] groupSizes = getGroupSizes();
        // double[] groupEnds = getGroupHeights();

        int subIndex = 0;

        //ConstantPopulation cp = new ConstantPopulation();// Units.Type.YEARS);

        for (int j = 0; j < intervals.getIntervalCount(); j++) {

            // set the population size to the size of the middle of the current
            // interval
            final double ps = getPopSize(currentTime + (intervals.getInterval(j) / 2.0));
            //cp.setN0(ps);
            if (intervals.getIntervalType(j) == IntervalType.COALESCENT) {
                subIndex += 1;
                if (subIndex >= groupSizes.getValue(groupIndex)) {
                    groupIndex += 1;
                    subIndex = 0;
                }
            }

            logP += calculateIntervalLikelihood(ps, intervals.getInterval(j), currentTime,
                    intervals.getLineageCount(j), intervals.getIntervalType(j));

            // insert zero-length coalescent intervals
            int diff = intervals.getCoalescentEvents(j) - 1;
            for (int k = 0; k < diff; k++) {
                //cp.setN0(getPopSize(currentTime));
                double fPopSize = getPopSize(currentTime);
                logP += calculateIntervalLikelihood(fPopSize, 0.0, currentTime, intervals.getLineageCount(j) - k - 1,
                        IntervalType.COALESCENT);
                subIndex += 1;
                if (subIndex >= groupSizes.getValue(groupIndex)) {
                    groupIndex += 1;
                    subIndex = 0;
                }
            }

            currentTime += intervals.getInterval(j);
        }
        return logP;
    }

    public static double calculateIntervalLikelihood(double popSize, double width,
                                                     double timeOfPrevCoal, int lineageCount, IntervalType type) {
        final double timeOfThisCoal = width + timeOfPrevCoal;

        final double intervalArea = (timeOfThisCoal - timeOfPrevCoal) / popSize;
        //demogFunction.getIntegral(timeOfPrevCoal, timeOfThisCoal);
        final double kchoose2 = Binomial.choose2(lineageCount);
        double like = -kchoose2 * intervalArea;

        switch (type) {
            case COALESCENT:
                final double demographic = Math.log(popSize);//demogFunction.getLogDemographic(timeOfThisCoal);
                like += -demographic;

                break;
            default:
                break;
        }

        return like;
    }

    /**
     * @param t time
     * @return
     */
    public double getPopSize(double t) {

        if (!m_bIsPrepared) {
            prepare();
        }

        if (t > coalescentTimes[coalescentTimes.length - 1])
            return popSizes.getArrayValue(popSizes.getDimension() - 1);

        int epoch = Arrays.binarySearch(coalescentTimes, t);
        if (epoch < 0) {
            epoch = -epoch - 1;
        }

        int groupIndex = Arrays.binarySearch(cumulativeGroupSizes, epoch);

        if (groupIndex < 0) {
            groupIndex = -groupIndex - 1;
        } else {
            groupIndex++;
        }
        if (groupIndex >= popSizes.getDimension()) {
            groupIndex = popSizes.getDimension() - 1;
        }

        return popSizes.getArrayValue(groupIndex);
    }

    @Override
    public List<String> getArguments() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getConditions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void sample(State state, Random random) {
        // TODO Auto-generated method stub

    }


// This is the implementation of BayesianSkyline as PopulationFunction.Abstract, which is somewhat slower 
//	than the implementation as a Distribution (43s/Msamples agains 41s/Msamples on Dengue data)
//	/**
//	 * @param t
//	 *            time
//	 * @return
//	 */
//	@Override
//	public double getIntensity(double t) {
//		if (!m_bIsPrepared) {
//			prepare();
//		}
//
//		int index = 0;
//		int groupIndex = 0;
//
//		t -= 1e-100;
//		if (t > coalescentTimes[coalescentTimes.length - 1]) {
//			t = coalescentTimes[coalescentTimes.length - 1];
//		}
//
//		if (t < coalescentTimes[0]) {
//			return t / popSizes.getArrayValue(0);
//		} else {
//
//			double intensity = coalescentTimes[0] / popSizes.getArrayValue(0);
//			index += 1;
//			if (index >= cumulativeGroupSizes[groupIndex]) {
//				groupIndex += 1;
//			}
//
//			while (t > coalescentTimes[index]) {
//
//				intensity += (coalescentTimes[index] - coalescentTimes[index - 1]) / popSizes.getArrayValue(groupIndex);
//
//				index += 1;
//				if (index >= cumulativeGroupSizes[groupIndex]) {
//					groupIndex += 1;
//				}
//			}
//			intensity += (t - coalescentTimes[index - 1]) / popSizes.getArrayValue(groupIndex);
//
//			return intensity;
//		}
//	}
//
//	public double getInverseIntensity(double x) {
//		throw new UnsupportedOperationException();
//	}
}
