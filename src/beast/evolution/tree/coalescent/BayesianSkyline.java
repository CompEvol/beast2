package beast.evolution.tree.coalescent;

import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Tree;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexei Drummond
 */
@Description("A likelihood function for the generalized skyline plot coalescent.")
public class BayesianSkyline extends PopulationFunction.Abstract {

    public Input<RealParameter> popSizeParamInput = new Input<RealParameter>("popSizes", 
    		"present-day population size. " +
    		"If time units are set to Units.EXPECTED_SUBSTITUTIONS then"+
    		"the N0 parameter will be interpreted as N0 * mu. "+
    		"Also note that if you are dealing with a diploid population "+
    		"N0 will be out by a factor of 2.");
    public Input<RealParameter> groupSizeParamInput = new Input<RealParameter>("groupSizes", "the group sizes parameter");
    public Input<Tree> treeInput = new Input<Tree>("tree", "The tree containing coalescent node times for use in defining BSP.");

    RealParameter popSizes;
    RealParameter groupSizes;
    Tree tree;
    TreeIntervals intervals;

    public void initAndValidate(State state) throws Exception {
        prepare(state);
    }

    public void prepare(State state) {
        super.prepare(state);
        popSizes = state.getParameter(popSizeParamInput);
        groupSizes = state.getParameter(groupSizeParamInput);
        tree = (Tree) state.getStateNode(treeInput);
        intervals = new TreeIntervals(tree);
    }

    public List<String> getParameterIds() {

        List<String> paramIDs = new ArrayList<String>();
        paramIDs.add(popSizes.getID());
        paramIDs.add(groupSizes.getID());

        return paramIDs;
    }

    public double getPopSize(double t) {
        double[] heights = intervals.getIntervals();
        Double[] sizes = popSizes.getValues();

        for (int i = 0; i < heights.length; i++) {
            if (t < heights[i]) {
                return sizes[i];
            }
        }

        return Double.NaN;
    }

    public double getIntensity(double t) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public double getInverseIntensity(double x) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getNumArguments() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getArgumentName(int n) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public double getArgument(int n) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setArgument(int n, double value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public double getLowerBound(int n) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public double getUpperBound(int n) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PopulationFunction getCopy() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
