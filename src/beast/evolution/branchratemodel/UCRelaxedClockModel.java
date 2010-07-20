package beast.evolution.branchratemodel;


import beast.core.Citation;
import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.IntegerParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.math.distributions.ParametricDistribution;

/**
 * @author Alexei Drummond
 */

@Description("Defines an uncorrelated relaxed molecular clock.")
@Citation(value = "Drummond AJ, Ho SYW, Phillips MJ, Rambaut A (2006) Relaxed Phylogenetics and Dating with Confidence. PLoS Biol 4(5): e88", DOI = "10.1371/journal.pbio.0040088")
public class UCRelaxedClockModel extends BranchRateModel.Base {

    public Input<ParametricDistribution> rateDistInput = new Input<ParametricDistribution>("distribution", "the distribution governing the rates among branches", Input.Validate.REQUIRED);
    public Input<IntegerParameter> categoryInput = new Input<IntegerParameter>("rateCategories", "the rate categories associated with nodes in the tree for sampling of individual rates among branches.", Input.Validate.REQUIRED);
    public Input<Tree> treeInput = new Input<Tree>("tree", "the tree this relaxed clock is associated with.", Input.Validate.REQUIRED);

    @Override
    public void initAndValidate() throws Exception {

        tree = treeInput.get();

        categories = categoryInput.get();
        categories.setLower(0);
        categories.setUpper(tree.getNodeCount() - 1);

        distribution = rateDistInput.get();

        rates = new double[categories.getDimension()];
        for (int i = 0; i < rates.length; i++) {
            rates[i] = distribution.getDistribution().quantile((i + 0.5) / rates.length);
        }
    }

    public double getRateForBranch(Node node) {

        if (recompute < 0) {
            prepare();
            recompute = 1;
//            recompute = state.stateNumber;
        }

        assert !node.isRoot() : "root node doesn't have a rate!";

        int nodeNumber = node.getNr();

        int rateCategory = categories.getValue(nodeNumber);

        return rates[rateCategory] * scaleFactor;
    }

    // compute scale factor

    private void computeFactor() {

        //scale mean rate to 1.0 or separate parameter

        double treeRate = 0.0;
        double treeTime = 0.0;

        //normalizeBranchRateTo = 1.0;
        for (int i = 0; i < tree.getNodeCount(); i++) {
            Node node = tree.getNode(i);
            if (!node.isRoot()) {
                int rateCategory = categories.getValue(node.getNr());
                treeRate += rates[rateCategory] * node.getLength();
                treeTime += node.getLength();

                //System.out.println("rates and time\t" + rates[rateCategory] + "\t" + node.getLength());
            }
        }
        //treeRate /= treeTime;

        scaleFactor = normalizeBranchRateTo / (treeRate / treeTime);
        //System.out.println("scaleFactor\t\t\t\t\t" + scaleFactor);
    }


    public void prepare() {

        //System.out.println("prepare");

        categories = (IntegerParameter) categoryInput.get();

        distribution = rateDistInput.get();

        tree = treeInput.get();

        rates = new double[categories.getDimension()];
        for (int i = 0; i < rates.length; i++) {
            rates[i] = distribution.getDistribution().quantile((i + 0.5) / rates.length);
        }

        if (normalize) computeFactor();
    }

    ParametricDistribution distribution;
    IntegerParameter categories;
    Tree tree;

    @Override
    public void store(final int sample) {
    	m_bIsDirty = false;
    }

    @Override
    public void restore(final int sample) {
    	recompute = -Integer.MAX_VALUE;
    	m_bIsDirty = true;
    }
    
    boolean  m_bIsDirty = true;

    @Override
    public boolean isDirty() {
        if (recompute < 0 || m_bIsDirty) {
        	m_bIsDirty = true;
        	return true;
        }
        
//	    processed as trait on the tree      
//        if (categoryInput.get().isDirty()) {
//       	    return true;
//        }
        if (treeInput.get().isDirty()) {
        	m_bIsDirty = true;
        	return true;
        }
        // rateDistInput cannot be dirty?!?
//        if (rateDistInput.get().isDirty()) {
//        	m_bIsDirty = true;
//        	return true;
//        }
    	return false;
    }

    private int recompute = -Integer.MAX_VALUE;

    private boolean normalize = false;
    private double normalizeBranchRateTo = Double.NaN;
    private double[] rates;
    private double scaleFactor = 1.0;

}
