package beast.evolution.speciation;

import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;



// From Gernhard 2008, Yule density (p; conditioned on n nodes) should be:
// double p = 0.0;
// p = lambda^(n-1) * exp(-lambda*rootHeight);
// for (int i = 1; i < n; i++) {
//    p *= exp(-lambda*height[i])
// }

@Description("Pure birth model (i.e. no deaths)")
public class YuleModel extends SpeciesTreeDistribution {
    public Input<RealParameter> birthDiffRateParameterInput =
            new Input<RealParameter>("birthDiffRate", "birth difference rate parameter, lambda - mu in birth/death model", Validate.REQUIRED);
    public Input<Boolean> conditionlOnRootInput =
            new Input<Boolean>("conditionalOnRoot", "Whether to condition on the root (default false)", false);

    protected boolean conditionalOnRoot;

    @Override
    public void initAndValidate() throws Exception {
        super.initAndValidate();
        conditionalOnRoot = conditionlOnRootInput.get();

        // make sure that all tips are at the same height,
        // otherwise this Yule Model is not appropriate
        Tree tree = treeInput.get();
        if (tree == null) {
        	tree = treeIntervalsInput.get().treeInput.get();
        }
        List<Node> leafs = tree.getExternalNodes();
        double height = leafs.get(0).getHeight();
        for (Node leaf : leafs) {
        	if (Math.abs(leaf.getHeight() - height) > 1e-8) {
        		System.err.println("WARNING: Yule Model cannot handle dated tips. Use for example a coalescent prior instead.");
        		break;
        	}
        }
    }

    @Override
    public double calculateTreeLogLikelihood(final Tree tree) {
        return calculateTreeLogLikelihood(tree, 1, 0);
    }

    protected double calculateTreeLogLikelihood(final Tree tree, final double rho, final double a) {
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
        if (!conditionalOnRoot) {
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
        final double mrh = -r * height;

        if (!conditionalOnRoot) {
            final double z = Math.log(rho + ((1 - rho) - a) * Math.exp(mrh));
            double l = -2 * z + mrh;

            if (node.isRoot()) {
                l += mrh - z;
            }
            return l;
        } else {
            double l;
            if (!node.isRoot()) {
                final double z = Math.log(1 - a * Math.exp(mrh));
                l = -2 * z + mrh;
            } else {
                // Root dependent coefficient from each internal node
                final double ca = 1 - a;
                final double emrh = Math.exp(-mrh);
                if (emrh != 1.0) {
                    l = (taxonCount - 2) * Math.log(r * ca * (1 + ca / (emrh - 1)));
                } else {  // use exp(x)-1 = x for x near 0
                    l = (taxonCount - 2) * Math.log(ca * (r + ca / height));
                }
            }
            return l;
        }
    } // calcLogNodeProbability

//    public boolean includeExternalNodesInLikelihoodCalculation() {
//        return false;
//    }

    @Override
    protected boolean requiresRecalculation() {
        return super.requiresRecalculation() || birthDiffRateParameterInput.get().somethingIsDirty();
    }
    
    @Override
	public boolean canHandleTipDates() {
		return false;
	}

}
