package beast.evolution.branchratemodel;


import beast.core.Input;
import beast.core.Description;
import beast.core.parameter.BooleanParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

/**
 * @author Alexei Drummond
 */
@Description("Random Local Clock Model, whatever that is....")
public class RandomLocalClockModel extends BranchRateModel.Base {

    public Input<BooleanParameter> indicatorParamInput =
            new Input<BooleanParameter>("indicators",
                    "the indicators associated with nodes in the tree for sampling of individual rate changes among branches.",
                    Input.Validate.REQUIRED);
    public Input<RealParameter> rateParamInput =
            new Input<RealParameter>("rates",
                    "the rate parameters associated with nodes in the tree for sampling of individual rates among branches.",
                    Input.Validate.REQUIRED);
    //    public Input<RealParameter> meanRateInput =
//            new Input<RealParameter>("meanRate",
//                    "an optional parameter to set the mean rate across the whole tree");
    public Input<Tree> treeInput =
            new Input<Tree>("tree", "the tree this relaxed clock is associated with.", Input.Validate.REQUIRED);
    public Input<Boolean> ratesAreMultipliersInput =
            new Input<Boolean>("ratesAreMultipliers", "true if the rates should be treated as multipliers (default false).", false);

    Tree m_tree;
    RealParameter meanRate;

    @Override
    public void initAndValidate() throws Exception {
        m_tree = treeInput.get();

        BooleanParameter indicators = indicatorParamInput.get();

        if (indicators.getDimension() != m_tree.getNodeCount() - 1) {
            System.out.println("RandomLocalClockModel::Setting dimension of indicators to " + (m_tree.getNodeCount() - 1));
            indicators.setDimension(m_tree.getNodeCount() - 1);
        }

        unscaledBranchRates = new double[m_tree.getNodeCount()];

        RealParameter rates = rateParamInput.get();
        if (rates.lowerValueInput.get() == null || rates.lowerValueInput.get() < 0.0) {
            rates.setLower(0.0);
        }
        if (rates.upperValueInput.get() == null || rates.upperValueInput.get() < 0.0) {
            rates.setUpper(Double.MAX_VALUE);
        }
        if (rates.getDimension() != m_tree.getNodeCount() - 1) {
            System.out.println("RandomLocalClockModel::Setting dimension of rates to " + (m_tree.getNodeCount() - 1));
            rates.setDimension(m_tree.getNodeCount() - 1);
        }


        ratesAreMultipliers = ratesAreMultipliersInput.get();

        meanRate = meanRateInput.get();
        if (meanRate == null) {
            meanRate = new RealParameter("1.0");
        }
    }

    /**
     * This is a recursive function that does the work of
     * calculating the unscaled branch rates across the tree
     * taking into account the indicator variables.
     *
     * @param node the node
     * @param rate the rate of the parent node
     */
    private void calculateUnscaledBranchRates(Node node, double rate, BooleanParameter indicators, RealParameter rates) {

        int nodeNumber = getNr(node);

        if (!node.isRoot()) {
            if (indicators.getValue(nodeNumber)) {
                if (ratesAreMultipliers) {
                    rate *= rates.getValue(nodeNumber);
                } else {
                    rate = rates.getValue(nodeNumber);
                }
            }
        }
        unscaledBranchRates[nodeNumber] = rate;

        if (!node.isLeaf()) {
            calculateUnscaledBranchRates(node.getLeft(), rate, indicators, rates);
            calculateUnscaledBranchRates(node.getRight(), rate, indicators, rates);
        }
    }

    private void recalculateScaleFactor() {

        BooleanParameter indicators = indicatorParamInput.get();
        RealParameter rates = rateParamInput.get();

        calculateUnscaledBranchRates(m_tree.getRoot(), 1.0, indicators, rates);

        double timeTotal = 0.0;
        double branchTotal = 0.0;

        for (int i = 0; i < m_tree.getNodeCount(); i++) {
            Node node = m_tree.getNode(i);
            if (!node.isRoot()) {

                double branchInTime = node.getParent().getHeight() - node.getHeight();

                double branchLength = branchInTime * unscaledBranchRates[node.getNr()];

                timeTotal += branchInTime;
                branchTotal += branchLength;
            }
        }

        scaleFactor = timeTotal / branchTotal;

        scaleFactor *= meanRate.getValue();
    }

    @Override
    public double getRateForBranch(Node node) {
        if (recompute) {
            recalculateScaleFactor();
            recompute = false;
        }

        return unscaledBranchRates[getNr(node)] * scaleFactor;
    }

    private int getNr(Node node) {
        int nNodeNr = node.getNr();
        if (nNodeNr > m_tree.getRoot().getNr()) {
            nNodeNr--;
        }
        return nNodeNr;
    }

    @Override
    protected boolean requiresRecalculation() {
        // this is only called if any of its inputs is dirty, hence we need to recompute
        recompute = true;
        return true;
    }

    @Override
    protected void store() {
        recompute = true;
        super.store();
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
