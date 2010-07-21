package beast.evolution.tree.coalescent;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Alexei Drummond
 */
@Description("A likelihood function for the generalized skyline plot coalescent.")
public class BayesianSkyline extends PopulationFunction.Abstract {

    public Input<RealParameter> popSizeParamInput = new Input<RealParameter>("popSizes",
            "present-day population size. " +
                    "If time units are set to Units.EXPECTED_SUBSTITUTIONS then" +
                    "the N0 parameter will be interpreted as N0 * mu. " +
                    "Also note that if you are dealing with a diploid population " +
                    "N0 will be out by a factor of 2.");
    public Input<IntegerParameter> groupSizeParamInput = new Input<IntegerParameter>("groupSizes", "the group sizes parameter");
    public Input<Tree> treeInput = new Input<Tree>("tree", "The tree containing coalescent node times for use in defining BSP.");

    RealParameter popSizes;
    IntegerParameter groupSizes;
    Tree tree;
    TreeIntervals intervals;
    double[] coalescentTimes;
    int[] cumulativeGroupSizes;


    public BayesianSkyline() {
    }

    /**
     * This pseudo-constructor is only used for junit tests
     *
     * @param populationSize
     * @param groupSizes
     * @param tree
     * @throws Exception
     */
    public void init(RealParameter populationSize, IntegerParameter groupSizes, Tree tree) throws Exception {
        super.init(populationSize, groupSizes, tree);
    }

    public void initAndValidate() throws Exception {

        prepare();
    }

    public void prepare() {
        super.prepare();
        popSizes = popSizeParamInput.get();//state.getParameter(popSizeParamInput);
        groupSizes = groupSizeParamInput.get();//(IntegerParameter) state.getStateNode(groupSizeParamInput);
        tree = treeInput.get();//(Tree) state.getStateNode(treeInput);
        intervals = new TreeIntervals(tree);

        cumulativeGroupSizes = new int[groupSizes.getDimension()];

        int intervalCount = 0;
        for (int i = 0; i < cumulativeGroupSizes.length; i++) {
            intervalCount += groupSizes.getValue(i);
            cumulativeGroupSizes[i] = intervalCount;
        }

        coalescentTimes = intervals.getCoalescentTimes(coalescentTimes);

        assert (intervals.getSampleCount() == intervalCount);
    }

    public List<String> getParameterIds() {

        List<String> paramIDs = new ArrayList<String>();
        paramIDs.add(popSizes.getID());
        paramIDs.add(groupSizes.getID());

        return paramIDs;
    }

    /**
     * Not yet tested!
     *
     * @param t time
     * @return
     */
    public double getPopSize(double t) {
        if (t > coalescentTimes[coalescentTimes.length - 1]) return popSizes.getValue(popSizes.getDimension() - 1);

        int epoch = Arrays.binarySearch(coalescentTimes, t);
        if (epoch < 0) {
            epoch = -epoch;
        }

        int groupIndex = Arrays.binarySearch(cumulativeGroupSizes, epoch);

        if (groupIndex < 0) {
            groupIndex = -groupIndex - 1;
        }

        return popSizes.getValue(groupIndex);
    }

    /**
     * Not yet tested!
     *
     * @param t time
     * @return
     */
    public double getIntensity(double t) {

        int index = 0;
        int groupIndex = 0;

        if (t < coalescentTimes[0]) {
            return t / popSizes.getValue(0);
        } else {

            double intensity = coalescentTimes[0] / popSizes.getValue(0);
            index += 1;
            if (cumulativeGroupSizes[groupIndex] >= index) {
                groupIndex += 1;
            }
            while (t > coalescentTimes[index]) {

                intensity += (coalescentTimes[index] - coalescentTimes[index - 1]) / popSizes.getValue(groupIndex);

                index += 1;
                if (cumulativeGroupSizes[groupIndex] >= index) {
                    groupIndex += 1;
                }
            }
            intensity += (t - coalescentTimes[index - 1]) / popSizes.getValue(groupIndex);

            return intensity;
        }
    }

    public double getInverseIntensity(double x) {
        throw new UnsupportedOperationException();
    }

    public int getNumArguments() {
        throw new UnsupportedOperationException();
    }

    public String getArgumentName(int n) {
        throw new UnsupportedOperationException();
    }

    public double getArgument(int n) {
        throw new UnsupportedOperationException();
    }

    public void setArgument(int n, double value) {
        throw new UnsupportedOperationException();
    }

    public double getLowerBound(int n) {
        throw new UnsupportedOperationException();
    }

    public double getUpperBound(int n) {
        throw new UnsupportedOperationException();
    }

    public PopulationFunction getCopy() {
        throw new UnsupportedOperationException();
    }
}
