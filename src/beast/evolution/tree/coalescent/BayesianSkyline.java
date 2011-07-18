package beast.evolution.tree.coalescent;

import beast.core.Description;
import beast.core.Input;
import beast.core.Plugin;
import beast.core.Valuable;
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

    public Input<Valuable> popSizeParamInput = new Input<Valuable>("popSizes",
            "present-day population size. " +
                    "If time units are set to Units.EXPECTED_SUBSTITUTIONS then" +
                    "the N0 parameter will be interpreted as N0 * mu. " +
                    "Also note that if you are dealing with a diploid population " +
                    "N0 will be out by a factor of 2.", Validate.REQUIRED);
    public Input<IntegerParameter> groupSizeParamInput = new Input<IntegerParameter>("groupSizes", "the group sizes parameter", Validate.REQUIRED);
    //public Input<Tree> treeInput = new Input<Tree>("tree", "The tree containing coalescent node times for use in defining BSP.");
    public Input<TreeIntervals> m_treeIntervals = new Input<TreeIntervals>("treeIntervals", "The intervals of teh tree containing coalescent node times for use in defining BSP.", Validate.REQUIRED);

    Valuable popSizes;
    IntegerParameter groupSizes;
    Tree tree;
    TreeIntervals intervals;
    double[] coalescentTimes;
    
    
    int[] cumulativeGroupSizes;
    boolean m_bIsPrepared = false;


    public BayesianSkyline() {
    }

//    /**
//     * This pseudo-constructor is only used for junit tests
//     *
//     * @param populationSize
//     * @param groupSizes
//     * @param tree
//     * @throws Exception
//     */
//    public void init(RealParameter populationSize, IntegerParameter groupSizes, Tree tree) throws Exception {
//        super.init(populationSize, groupSizes, tree);
//    }

    public void initAndValidate() throws Exception {
    	intervals = m_treeIntervals.get();
    	groupSizes = groupSizeParamInput.get();
        popSizes = popSizeParamInput.get();
    	
    	// make sure that the sum of groupsizes == number of coalescent events
        int events = intervals.m_tree.get().getInternalNodeCount();
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
//                double[] uppers = new double[paramDim2];
//                double[] lowers = new double[paramDim2];

                // For these special cases we assume that the XML has not specified initial group sizes
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
//                    uppers[i] = Double.MAX_VALUE;
//                    lowers[i] = 1.0;
                }

//                if (type == EXPONENTIAL_TYPE || type == LINEAR_TYPE) {
//                    lowers[0] = 2.0;
//                }
                IntegerParameter parameter = new IntegerParameter(values);
                parameter.setBounds(1, Integer.MAX_VALUE);
                groupSizes.assignFromWithoutID(parameter);
            } else {
                // ... otherwise assume the user has made a mistake setting initial group sizes.
                throw new IllegalArgumentException("The sum of the initial group sizes does not match the number of coalescent events in the tree.");
            }
        }
        
        prepare();
    }

    @Override
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
        paramIDs.add(((Plugin)popSizes).getID());
        paramIDs.add(groupSizes.getID());

        return paramIDs;
    }

    /**
     * Not yet tested!
     *
     * @param t time
     * @return
     */
    @Override
    public double getPopSize(double t) {
    	
    	if (!m_bIsPrepared) {
    		prepare();
    	}

        if (t > coalescentTimes[coalescentTimes.length - 1]) return popSizes.getArrayValue(popSizes.getDimension() - 1);

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

        return popSizes.getArrayValue(groupIndex);
    }

    /**
     * Not yet tested!
     *
     * @param t time
     * @return
     */
    @Override
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
            return t / popSizes.getArrayValue(0);
        } else {

            double intensity = coalescentTimes[0] / popSizes.getArrayValue(0);
            index += 1;
            if (index >= cumulativeGroupSizes[groupIndex]) {
                groupIndex += 1;
            }

            while (t > coalescentTimes[index]) {

                intensity += (coalescentTimes[index] - coalescentTimes[index - 1]) / popSizes.getArrayValue(groupIndex);

                index += 1;
                if (index >= cumulativeGroupSizes[groupIndex]) {
                    groupIndex += 1;
                }
            }
            intensity += (t - coalescentTimes[index - 1]) / popSizes.getArrayValue(groupIndex);

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
