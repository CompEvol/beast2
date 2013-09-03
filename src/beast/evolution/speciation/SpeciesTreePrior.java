package beast.evolution.speciation;


import java.util.Arrays;
import java.util.List;
import java.util.Random;

import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.TreeDistribution;
import beast.math.distributions.Gamma;



@Description("Species tree prior for *BEAST analysis")
public class SpeciesTreePrior extends TreeDistribution {
    //public Input<Tree> m_speciesTree = new Input<Tree>("speciesTree", "species tree containing the associated gene tree", Validate.REQUIRED);

    protected enum PopSizeFunction {constant, linear, linear_with_constant_root}

    public final Input<PopSizeFunction> popFunctionInput = new Input<PopSizeFunction>("popFunction", "Population function. " +
            "This can be " + Arrays.toString(PopSizeFunction.values()) + " (default 'constant')", PopSizeFunction.constant, PopSizeFunction.values());

    public final Input<RealParameter> popSizesBottomInput = new Input<RealParameter>("bottomPopSize", "population size parameter for populations at the bottom of a branch. " +
            "For linear population function, this is the same at the top of the branch.", Validate.REQUIRED);
    public final Input<RealParameter> popSizesTopInput = new Input<RealParameter>("topPopSize", "population size parameter at the top of a branch. " +
            "Ignored for constant population function, but required for linear population function.");

    public final Input<RealParameter> gammaParameterInput = new Input<RealParameter>("gammaParameter", "shape parameter of the gamma distribution", Validate.REQUIRED);

//	public Input<RealParameter> m_rootHeightParameter = new Input<RealParameter>("rootBranchHeight","height of the node above the root, representing the root branch", Validate.REQUIRED);
    /**
     * m_taxonSet is used by GeneTreeForSpeciesTreeDistribution *
     */
    public Input<TaxonSet> taxonSetInput = new Input<TaxonSet>("taxonset", "set of taxa mapping lineages to species", Validate.REQUIRED);


    private PopSizeFunction popFunction;
    private RealParameter popSizesBottom;
    private RealParameter popSizesTop;

    private Gamma gamma2Prior;
    private Gamma gamma4Prior;

    @Override
    public void initAndValidate() throws Exception {
        popFunction = popFunctionInput.get();
        popSizesBottom = popSizesBottomInput.get();
        popSizesTop = popSizesTopInput.get();

        // set up sizes of population functions
        final int nSpecies = treeInput.get().getLeafNodeCount();
        final int nNodes = treeInput.get().getNodeCount();
        switch (popFunction) {
            case constant:
                popSizesBottom.setDimension(nNodes);
                break;
            case linear:
                if (popSizesTop == null) {
                    throw new Exception("topPopSize must be specified");
                }
                popSizesBottom.setDimension(nSpecies);
                popSizesTop.setDimension(nNodes);
                break;
            case linear_with_constant_root:
                if (popSizesTop == null) {
                    throw new Exception("topPopSize must be specified");
                }
                popSizesBottom.setDimension(nSpecies);
                popSizesTop.setDimension(nNodes - 1);
                break;
        }

        // bottom prior = Gamma(2,Psi)
        gamma2Prior = new Gamma();
        gamma2Prior.betaInput.setValue(gammaParameterInput.get(), gamma2Prior);

        // top prior = Gamma(4,Psi)
        gamma4Prior = new Gamma();
        final RealParameter parameter = new RealParameter(new Double[]{4.0});
        gamma4Prior.alphaInput.setValue(parameter, gamma4Prior);
        gamma4Prior.betaInput.setValue(gammaParameterInput.get(), gamma4Prior);

        if (popFunction != PopSizeFunction.constant && gamma4Prior == null) {
            throw new Exception("Top prior must be specified when population function is not constant");
        }
        // make sure the m_taxonSet is a set of taxonsets
// HACK to make Beauti initialise: skip the check here
//		for (Taxon taxon : m_taxonSet.get().m_taxonset.get()) {
//			if (!(taxon instanceof TaxonSet)) {
//				throw new Exception("taxonset should be sets of taxa only, not individual taxons");
//			}
//		}
    }

    @Override
    public double calculateLogP() {
        logP = 0;
        // make sure the root branch length is positive
//		if (m_rootHeightParameter.get().getValue() < m_speciesTree.get().getRoot().getHeight()) {
//			logP = Double.NEGATIVE_INFINITY;
//			return logP;
//		}

        final Node[] speciesNodes = treeInput.get().getNodesAsArray();
        try {
            switch (popFunction) {
                case constant:
                    // constant pop size function
                    logP += gamma2Prior.calcLogP(popSizesBottom);
//			for (int i = 0; i < speciesNodes.length; i++) {
//				double fPopSize = m_fPopSizesBottom.getValue(i);
//				logP += m_bottomPrior.logDensity(fPopSize); 
//			}
                    break;
                case linear:
                    // linear pop size function
//			int nSpecies = m_tree.get().getLeafNodeCount();
//			m_fPopSizesBottom.setDimension(nSpecies);
//			logP += m_gamma4Prior.calcLogP(m_fPopSizesBottom);
//			int nNodes = m_tree.get().getNodeCount();
//			m_fPopSizesTop.setDimension(nNodes-1);
//			logP += m_gamma2Prior.calcLogP(m_fPopSizesTop);

                    for (int i = 0; i < speciesNodes.length; i++) {
                        final Node node = speciesNodes[i];
                        final double fPopSizeBottom;
                        if (node.isLeaf()) {
                            // Gamma(4, fPsi) prior
                            fPopSizeBottom = popSizesBottom.getValue(i);
                            logP += gamma4Prior.logDensity(fPopSizeBottom);
                        }
                        final double fPopSizeTop = popSizesTop.getValue(i);
                        logP += gamma2Prior.logDensity(fPopSizeTop);
                    }
                    break;
                case linear_with_constant_root:
//			logP += m_gamma4Prior.calcLogP(m_fPopSizesBottom);
//			logP += m_gamma2Prior.calcLogP(m_fPopSizesTop);
//			int iRoot = m_tree.get().getRoot().getNr();
//			double fPopSize = m_fPopSizesTop.getValue(iRoot);
//			logP -= m_gamma2Prior.logDensity(fPopSize); 

                    for (int i = 0; i < speciesNodes.length; i++) {
                        final Node node = speciesNodes[i];
                        if (node.isLeaf()) {
                            final double fPopSizeBottom = popSizesBottom.getValue(i);
                            logP += gamma4Prior.logDensity(fPopSizeBottom);
                        }
                        if (!node.isRoot()) {
                            if (i < speciesNodes.length - 1) {
                                final double fPopSizeTop = popSizesTop.getArrayValue(i);
                                logP += gamma2Prior.logDensity(fPopSizeTop);
                            } else {
                                final int iNode = treeInput.get().getRoot().getNr();
                                final double fPopSizeTop = popSizesTop.getArrayValue(iNode);
                                logP += gamma2Prior.logDensity(fPopSizeTop);
                            }
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            // exceptions can be thrown by the gamma priors
            e.printStackTrace();
            return Double.NEGATIVE_INFINITY;
        }
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
