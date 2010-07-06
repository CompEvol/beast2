package beast.evolution.branchratemodel;

import beast.core.Citation;
import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.core.parameter.Parameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.math.distributions.ParametricDistribution;

/**
 * @author Alexei Drummond
 */

@Description("Defines an uncorrelated relaxed molecular clock.")
@Citation(value = "Drummond AJ, Ho SYW, Phillips MJ, Rambaut A (2006) Relaxed Phylogenetics and Dating with Confidence. PLoS Biol 4(5): e88", DOI = "10.1371/journal.pbio.0040088")
public class UCRelaxedClockModel extends BranchRateModel.Base {

    public Input<ParametricDistribution> rateDistInput = new Input<ParametricDistribution>("distribution", "the distribution governing the rates among branches");
    public Input<Parameter<Integer>> categoryInput = new Input<Parameter<Integer>>("rateCategories", "the rate categories associated with nodes in the tree for sampling of individual rates among branches.");
    public Input<Tree> treeInput = new Input<Tree>("tree", "the tree this relaxed clock is associated with.");

    @Override
    public void initAndValidate(State state) throws Exception {

        //categories = new Parameter.Integer();
    }

    public double getRateForBranch(State state, Node node) {

        assert !node.isRoot() : "root node doesn't have a rate!";

        int rateCategory = categories.getValue(node.getNr());

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

                System.out.println("rates and time\t" + rates[rateCategory] + "\t" + node.getLength());
            }
        }
        //treeRate /= treeTime;

        scaleFactor = normalizeBranchRateTo / (treeRate / treeTime);
        System.out.println("scaleFactor\t\t\t\t\t" + scaleFactor);
    }


    public void prepare(final State state) {

        categories = (Parameter<Integer>) state.getStateNode(categoryInput);

        tree = (Tree) state.getStateNode(treeInput);

        if (normalize) computeFactor();
    }

    Parameter<Integer> categories;
    Tree tree;

    private boolean normalize = false;
    private double normalizeBranchRateTo = Double.NaN;
    private double[] rates;
    private double scaleFactor = 1.0;

}
