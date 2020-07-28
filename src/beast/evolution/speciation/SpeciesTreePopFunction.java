package beast.evolution.speciation;


import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.State;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.TreeDistribution;
import beast.math.distributions.Gamma;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Description("Species tree prior for *BEAST analysis")
public class SpeciesTreePopFunction extends TreeDistribution {

    protected enum TreePopSizeFunction {constant, linear, linear_with_constant_root}

    public final Input<TreePopSizeFunction> popFunctionInput = new Input<>("popFunction", "Population function. " +
            "This can be " + Arrays.toString(TreePopSizeFunction.values()) + " (default 'constant')", TreePopSizeFunction.constant, TreePopSizeFunction.values());

    public final Input<RealParameter> popSizesBottomInput = new Input<>("bottomPopSize", "population size parameter for populations at the bottom of a branch. " +
            "For constant population function, this is the same at the top of the branch.", Validate.REQUIRED);
    public final Input<RealParameter> popSizesTopInput = new Input<>("topPopSize", "population size parameter at the top of a branch. " +
            "Ignored for constant population function, but required for linear population function.");

    /**
     * m_taxonSet is used by GeneTreeForSpeciesTreeDistribution *
     */
    final public Input<TaxonSet> taxonSetInput = new Input<>("taxonset", "set of taxa mapping lineages to species", Validate.REQUIRED);


    TreePopSizeFunction popFunction;
    RealParameter popSizesBottom;
    RealParameter popSizesTop;

    @Override
    public void initAndValidate() {
        popFunction = popFunctionInput.get();
        popSizesBottom = popSizesBottomInput.get();
        popSizesTop = popSizesTopInput.get();

        // set up sizes of population functions
        final int speciesCount = treeInput.get().getLeafNodeCount();
        final int nodeCount = treeInput.get().getNodeCount();
        switch (popFunction) {
            case constant:
                popSizesBottom.setDimension(nodeCount);
                break;
            case linear:
                if (popSizesTop == null) {
                    throw new IllegalArgumentException("topPopSize must be specified");
                }
                popSizesBottom.setDimension(speciesCount);
                popSizesTop.setDimension(nodeCount);
                break;
            case linear_with_constant_root:
                if (popSizesTop == null) {
                    throw new IllegalArgumentException("topPopSize must be specified");
                }
                popSizesBottom.setDimension(speciesCount);
                popSizesTop.setDimension(nodeCount - 1);
                break;
        }
    }

    @Override
    public double calculateLogP() {
        logP = 0;
        return logP;
    }

    @Override
    protected boolean requiresRecalculation() {
        return true;
    }

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }

    @Override
    public void sample(final State state, final Random random) {
    }
}
