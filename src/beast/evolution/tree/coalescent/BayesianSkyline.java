package beast.evolution.tree.coalescent;

import beast.core.Input;
import beast.core.Parameter;
import beast.core.State;
import beast.evolution.tree.Tree;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexei Drummond
 */
public class BayesianSkyline extends PopulationFunction.Abstract {

    public Input<Parameter> popSizeParamInput = new Input<Parameter>("popSizes", "the popSizes parameter");
    public Input<Parameter> groupSizeParamInput = new Input<Parameter>("groupSizes", "the group sizes parameter");
    public Input<Tree> treeInput = new Input<Tree>("tree", "The tree containing coalescent node times for use in defining BSP.");

    Parameter popSizes;
    Parameter groupSizes;
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
        double[] sizes = popSizes.getValues();

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
