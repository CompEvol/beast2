package beast.base.evolution.speciation;



import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.core.Input.Validate;
import beast.base.evolution.tree.MRCAPrior;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeInterface;
import beast.base.evolution.tree.TreeUtils;
import beast.base.inference.*;
import beast.base.inference.parameter.RealParameter;



// From Gernhard 2008, Yule density (p; conditioned on n nodes) should be:
// double p = 0.0;
// p = lambda^(n-1) * exp(-lambda*rootHeight);
// for (int i = 1; i < n; i++) {
//    p *= exp(-lambda*height[i])
// }

@Description("Pure birth model (i.e. no deaths)")
public class YuleModel extends SpeciesTreeDistribution {
    final public Input<RealParameter> birthDiffRateParameterInput =
            new Input<>("birthDiffRate", "birth difference rate parameter, lambda - mu in birth/death model (diversification rate)", Validate.REQUIRED);
    final public Input<RealParameter> originHeightParameterInput =
            new Input<>("originHeight", "the height of the point of origin of the process");
    final public Input<Boolean> conditionalOnRootInput =
            new Input<>("conditionalOnRoot", "Whether to condition on the root (default false)", false);

    protected boolean conditionalOnRoot;
    protected boolean conditionalOnOrigin;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        conditionalOnRoot = conditionalOnRootInput.get();
        conditionalOnOrigin = originHeightParameterInput.get() != null;

        if (conditionalOnRoot && conditionalOnOrigin) {
            throw new RuntimeException("ERROR: Cannot condition on both root and origin.");
        }

        // make sure that all tips are at the same height,
        // otherwise this Yule Model is not appropriate
        TreeInterface tree = treeInput.get();
        if (tree == null) {
            tree = treeIntervalsInput.get().treeInput.get();
        }
        if (!TreeUtils.isUltrametric(tree)) {
            Log.warning.println("WARNING: This model (tree prior) cannot handle dated tips. " +
                    "Please select a tree prior which can, otherwise " +
                    "results may be invalid.");
        }
    }

    @Override
    public double calculateTreeLogLikelihood(final TreeInterface tree) {
        return calculateTreeLogLikelihood(tree, 1, 0);
    }

    protected double calculateTreeLogLikelihood(final TreeInterface tree, final double rho, final double a) {

        if (conditionalOnOrigin && tree.getRoot().getHeight() > originHeightParameterInput.get().getValue())
            return Double.NEGATIVE_INFINITY;

        final int taxonCount = tree.getLeafNodeCount();
        final double r = birthDiffRateParameterInput.get().getValue();

        double logL = logTreeProbability(taxonCount, r, rho, a);

        final Node[] nodes = tree.getNodesAsArray();
        for (int i = taxonCount; i < nodes.length; i++) {
            assert (!nodes[i].isLeaf());
            logL += calcLogNodeProbability(nodes[i], r, rho, a, taxonCount);
        }

        return logL;
    }

    /**
     * calculate contribution of the tree to the log likelihood
     *
     * @param taxonCount
     * @param r          relative birth rate (birth rate - death rate)
     * @param rho        parameter in Gernhard 2008 birth death model
     * @param a          death/birth rates ratio
     * @return
     */
    protected double logTreeProbability(final int taxonCount, double r, double rho, double a) {
        double c1 = logCoeff(taxonCount);
        if (conditionalOnOrigin) {
            final double height = originHeightParameterInput.get().getValue();
            c1 += (taxonCount - 1) * calcLogConditioningTerm(height, r, rho, a);
        } else if (!conditionalOnRoot) {
            c1 += (taxonCount - 1) * Math.log(r * rho) + taxonCount * Math.log(1 - a);
        }
        return c1;
    }

    /**
     * default implementation, equivalent with unscaled tree in Gernhard 2008 model
     *
     * @param taxonCount
     * @return
     */
    protected double logCoeff(final int taxonCount) {
        //return logGamma(taxonCount + 1);?
        return 0.0;
    }

    /**
     * contribution of a single node to the log likelihood
     * r = relative birth rate (birth rate - death rate)
     * rho = rho parameter in Gernhard 2008 birth death model
     * a = death rate relative to birth rate
     *
     * @param node
     * @param r
     * @param rho
     * @param a
     * @param taxonCount
     * @return
     */
    protected double calcLogNodeProbability(Node node, double r, double rho, double a, int taxonCount) {
        final double height = node.getHeight();

        if (conditionalOnRoot && node.isRoot()) {
            return (taxonCount - 2) * calcLogConditioningTerm(height, r, rho, a);
        }

        final double mrh = -r * height;
        final double z = Math.log(rho + ((1 - rho) - a) * Math.exp(mrh));
        double l = -2 * z + mrh;

        if (!conditionalOnOrigin && !conditionalOnRoot && node.isRoot())
            l += mrh - z;

        return l;

    } // calcLogNodeProbability

    //    public boolean includeExternalNodesInLikelihoodCalculation() {
//        return false;
//    }
    // r = birth - death
    // a = death/birth
    double calcLogConditioningTerm(double height, double r, double rho, double a) {
        final double ca = 1 - a;
        final double erh = Math.exp(r * height);
        if (erh != 1.0) {
            return Math.log(r * ca * (rho + ca / (erh - 1)));
        } else {  // use exp(x)-1 = x for x near 0
            return Math.log(ca * (r * rho + ca / height));
        }
    }

    @Override
    protected boolean requiresRecalculation() {
        return super.requiresRecalculation()
                || birthDiffRateParameterInput.get().somethingIsDirty()
                || (conditionalOnOrigin && originHeightParameterInput.get().somethingIsDirty());
    }

    @Override
    public boolean canHandleTipDates() {
        return false;
    }


    @Override
    public void validateInputs() {
        if (conditionalOnRootInput.get()) {
            // make sure there is an MRCAPrior on the root
            TreeInterface tree = treeInput.get();
            int n = tree.getTaxonset().getTaxonCount();
            boolean found = false;
            for (BEASTInterface o : ((BEASTInterface) tree).getOutputs()) {
                if (o instanceof MRCAPrior) {
                    MRCAPrior prior = (MRCAPrior) o;
                    int n2 = prior.taxonsetInput.get().taxonsetInput.get().size();
                    if (n2 == n) {
                        found = true;
                    }
                }
            }
            if (!found) {
                Log.warning("WARNING: There must be an MRCAPrior on the root when conditionalOnRoot=true, but could not find any");
            }
        }

        super.validateInputs();
    }

    /**
     * Sampling only implemented for no-origin case currently.
     */
    @Override
    public void sample(State state, Random random) {

        if (sampledFlag)
            return;

        sampledFlag = true;

        // Cause conditional parameters to be sampled

        sampleConditions(state, random);

        Tree tree = (Tree) treeInput.get();
        RealParameter birthRate = birthDiffRateParameterInput.get();

        // Simulate tree conditional on new parameters

        List<Node> activeLineages = new ArrayList<>();
        for (Node oldLeaf : tree.getExternalNodes()) {
            Node newLeaf = new Node(oldLeaf.getID());
            newLeaf.setNr(oldLeaf.getNr());
            newLeaf.setHeight(0.0);
            activeLineages.add(newLeaf);
        }

        int nextNr = activeLineages.size();

        double t = 0.0;
        while (activeLineages.size() > 1) {
            int k = activeLineages.size();
            double a = birthRate.getValue() * k;

            t += -Math.log(random.nextDouble())/a;

            Node node1 = activeLineages.get(random.nextInt(k));
            Node node2;
            do {
                node2 = activeLineages.get(random.nextInt(k));
            } while (node2.equals(node1));

            Node newParent = new Node();
            newParent.setNr(nextNr++);
            newParent.setHeight(t);
            newParent.addChild(node1);
            newParent.addChild(node2);

            activeLineages.remove(node1);
            activeLineages.remove(node2);
            activeLineages.add(newParent);
        }

        tree.assignFromWithoutID(new Tree(activeLineages.get(0)));
    }

    @Override
    public List<String> getConditions() {
        List<String> conditions = new ArrayList<>();
        conditions.add(birthDiffRateParameterInput.get().getID());

        return conditions;
    }

    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add(treeInput.get().getID());

        return arguments;
    }
}
