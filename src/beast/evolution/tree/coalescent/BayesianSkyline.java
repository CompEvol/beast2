package beast.evolution.tree.coalescent;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
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
    //public Input<Tree> treeInput = new Input<Tree>("tree", "The tree containing coalescent node times for use in defining BSP.");
    public Input<TreeIntervals> m_treeIntervals = new Input<TreeIntervals>("treeIntervals", "The intervals of teh tree containing coalescent node times for use in defining BSP.", Validate.REQUIRED);

    RealParameter popSizes;
    IntegerParameter groupSizes;
    Tree tree;
    TreeIntervals intervals;
    double[] coalescentTimes;
    
    
    int[] cumulativeGroupSizes;
    boolean m_bIsPrepared = false;


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
    	// todo: make sure that the sum of groupsizes == number of coalescent events
    	intervals = m_treeIntervals.get();
        prepare();
    }

    public void prepare() {
        super.prepare();
        popSizes = popSizeParamInput.get();
        groupSizes = groupSizeParamInput.get();
//        tree = treeInput.get();
//        intervals = new TreeIntervals(tree);

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

    /** CalculationNode methods **/
    @Override
    protected boolean requiresRecalculation() {
    	m_bIsPrepared = false;
    	return true;
    }
    @Override
    protected void store() {
    	m_bIsPrepared = false;
    	super.store();
    }
    @Override
    protected void restore() {
    	m_bIsPrepared = false;
    	super.restore();
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
    	
    	if (!m_bIsPrepared) {
    		prepare();
    	}

        if (t > coalescentTimes[coalescentTimes.length - 1]) return popSizes.getValue(popSizes.getDimension() - 1);

        int epoch = Arrays.binarySearch(coalescentTimes, t);
        if (epoch < 0) {
            epoch = -epoch;
        }

        int groupIndex = Arrays.binarySearch(cumulativeGroupSizes, epoch);

        if (groupIndex < 0) {
            groupIndex = -groupIndex - 1;
        }
        if (groupIndex >= popSizes.getDimension()) {
        	groupIndex = popSizes.getDimension() - 1;
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
    	if (!m_bIsPrepared) {
    		prepare();
    	}

        int index = 0;
        int groupIndex = 0;
        
        t -= 1e-100;
        if (t > coalescentTimes[coalescentTimes.length-1]) {
        	t = coalescentTimes[coalescentTimes.length-1];
        }

        if (t < coalescentTimes[0]) {
            return t / popSizes.getValue(0);
        } else {

            double intensity = coalescentTimes[0] / popSizes.getValue(0);
            index += 1;
            if (index >= cumulativeGroupSizes[groupIndex]) {
                groupIndex += 1;
            }

            while (t > coalescentTimes[index]) {

                intensity += (coalescentTimes[index] - coalescentTimes[index - 1]) / popSizes.getValue(groupIndex);

                index += 1;
                if (index >= cumulativeGroupSizes[groupIndex]) {
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
//
//    public int getNumArguments() {
//        throw new UnsupportedOperationException();
//    }
//
//    public String getArgumentName(int n) {
//        throw new UnsupportedOperationException();
//    }
//
//    public double getArgument(int n) {
//        throw new UnsupportedOperationException();
//    }
//
//    public void setArgument(int n, double value) {
//        throw new UnsupportedOperationException();
//    }
//
//    public double getLowerBound(int n) {
//        throw new UnsupportedOperationException();
//    }
//
//    public double getUpperBound(int n) {
//        throw new UnsupportedOperationException();
//    }
//
//    public PopulationFunction getCopy() {
//        throw new UnsupportedOperationException();
//    }
}
