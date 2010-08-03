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
@Description("Random Local Clock Model.")
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
        prepare();
    }

    private void calculateUnscaledBranchRates(Tree tree) {
        cubr(tree.getRoot(), 1.0);
    }


    /**
     * This is a recursive function that does the work of
     * calculating the unscaled branch rates across the tree
     * taking into account the indicator variables.
     *
     * @param node the node
     * @param rate the rate of the parent node
     */
    private void cubr(Node node, double rate) {

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
            cubr(node.m_left, rate);
            cubr(node.m_right, rate);
        }
    }

    private void recalculateScaleFactor() {

        calculateUnscaledBranchRates(tree);

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

        if (meanRate != null) scaleFactor *= meanRate.getValue();
    }

    public void prepare() {

        tree = treeInput.get();

        indicators = indicatorParamInput.get();
        indicators.setLower(0);
        indicators.setUpper(1);

        rates = rateParamInput.get();
        rates.setLower(0.0);
        rates.setUpper(Double.MAX_VALUE);

        meanRate = meanRateInput.get();

        unscaledBranchRates = new double[indicators.getDimension()];
        recalculateScaleFactor();
    }


    public double getRateForBranch(Node node) {

        if (recompute) {
            prepare();
            recompute = false;
        }

        return unscaledBranchRates[node.getNr()] * scaleFactor;
    }

    @Override
    public void store(final int sample) {
        m_bIsDirty = false;
    }

    @Override
    public void restore(final int sample) {
        recompute = true;
        m_bIsDirty = true;
    }

    boolean m_bIsDirty = true;

    @Override
    public boolean isDirty() {
    	recompute = true;
    	if (true) {
            return false;
    	}
    	
    	if (recompute || m_bIsDirty) {
        	recompute = true;
            m_bIsDirty = true;
            return true;
        }

        if (treeInput.get().isDirty()) {
        	recompute = true;
            m_bIsDirty = true;
            return true;
        }

        RealParameter meanRate_ = meanRateInput.get();
        if (meanRate_ != null && meanRate_.isDirty()) {
        	recompute = true;
            m_bIsDirty = true;
            return true;
        }
        RealParameter rates_ = rateParamInput.get();
        if (rates_ != null && rates_.isDirty()) {
        	recompute = true;
            m_bIsDirty = true;
            return true;
        }
        IntegerParameter indicators_ = indicatorParamInput.get();
        if (indicators_ != null && indicators_.isDirty()) {
        	recompute = true;
            m_bIsDirty = true;
            return true;
        }
        
        return false;
    }


    double[] unscaledBranchRates;
    double scaleFactor;

    boolean recompute = true;

    RealParameter meanRate; // can be null
    RealParameter rates;
    IntegerParameter indicators;
    Tree tree;
    boolean ratesAreMultipliers = false;


}
