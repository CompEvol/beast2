package beast.evolution.branchratemodel;

import beast.core.Input;
import beast.core.Description;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

/**
 * @author Alexei Drummond
 */
@Description("Random Local Clock Model, whatever that is....")
public class RandomLocalClockModel extends BranchRateModel.Base {

    public Input<IntegerParameter> indicatorParamInput =
            new Input<IntegerParameter>("indicators",
                    "the indicators associated with nodes in the tree for sampling of individual rate changes among branches.",
                    Input.Validate.REQUIRED);
    public Input<RealParameter> rateParamInput =
            new Input<RealParameter>("rates",
                    "the rate parameters associated with nodes in the tree for sampling of individual rates among branches.",
                    Input.Validate.REQUIRED);
    public Input<RealParameter> meanRateInput =
            new Input<RealParameter>("meanRate",
                    "an optional parameter to set the mean rate across the whole tree");
    public Input<Tree> treeInput =
            new Input<Tree>("tree", "the tree this relaxed clock is associated with.", Input.Validate.REQUIRED);
    public Input<Boolean> ratesAreMultipliersInput =
            new Input<Boolean>("ratesAreMultipliers", "true if the rates should be treated as multipliers.");

    @Override
    public void initAndValidate() throws Exception {
    	IntegerParameter indicators = indicatorParamInput.get();
        indicators.setLower(0);
        indicators.setUpper(1);

        unscaledBranchRates = new double[indicators.getDimension()];
        
        RealParameter rates = rateParamInput.get();
        rates.setLower(0.0);
        rates.setUpper(Double.MAX_VALUE);

    }

    /**
     * This is a recursive function that does the work of
     * calculating the unscaled branch rates across the tree
     * taking into account the indicator variables.
     *
     * @param node the node
     * @param rate the rate of the parent node
     */
    private void calculateUnscaledBranchRates(Node node, double rate, IntegerParameter indicators, RealParameter rates) {

        int nodeNumber = node.getNr();

        if (!node.isRoot()) {
            if (indicators.getValue(nodeNumber) == 1) {
                if (ratesAreMultipliers) {
                    rate *= rates.getValue(nodeNumber);
                } else {
                    rate = rates.getValue(nodeNumber);
                }
            }
        }
        unscaledBranchRates[nodeNumber] = rate;

        if (!node.isLeaf()) {
        	calculateUnscaledBranchRates(node.m_left, rate, indicators, rates);
        	calculateUnscaledBranchRates(node.m_right, rate, indicators, rates);
        }
    }

    private void recalculateScaleFactor() {

        Tree tree = treeInput.get();
    	IntegerParameter indicators = indicatorParamInput.get();
        RealParameter rates = rateParamInput.get();
        
        calculateUnscaledBranchRates(tree.getRoot(), 1.0, indicators, rates);

        double timeTotal = 0.0;
        double branchTotal = 0.0;

        for (int i = 0; i < tree.getNodeCount(); i++) {
            Node node = tree.getNode(i);
            if (!node.isRoot()) {

                double branchInTime = node.getParent().getHeight() - node.getHeight();

                double branchLength = branchInTime * unscaledBranchRates[node.getNr()];

                timeTotal += branchInTime;
                branchTotal += branchLength;
            }
        }

        scaleFactor = timeTotal / branchTotal;

        RealParameter meanRate = meanRateInput.get();
        if (meanRate != null) scaleFactor *= meanRate.getValue();
    }

    @Override
    public double getRateForBranch(Node node) {
        if (recompute) {
            recalculateScaleFactor();
            recompute = false;
        }

        return unscaledBranchRates[node.getNr()] * scaleFactor;
    }

    @Override
    protected boolean requiresRecalculation() {
    	// this is only called if any of its inputs is dirty, hence we need to recompute
    	recompute = true;
    	return true;
    }
    
    @Override
    protected void restore() {
    	recompute = true;
    	super.restore();
    }

    private boolean recompute = true;
    double[] unscaledBranchRates;
    double scaleFactor;
    boolean ratesAreMultipliers = false;
}
