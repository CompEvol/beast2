package beast.base.evolution.speciation;


import java.util.List;
import java.util.Random;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.tree.Node;
import beast.base.inference.State;
import beast.base.inference.distribution.Gamma;
import beast.base.inference.parameter.RealParameter;

@Description("Species tree prior for *BEAST analysis")
public class SpeciesTreePrior extends SpeciesTreePopFunction {

    public final Input<RealParameter> gammaParameterInput = new Input<>("gammaParameter", "scale parameter of the gamma distribution over population sizes. "
    		+ "This makes this parameter half the expected population size on all branches for constant population function, "
    		+ "but a quarter of the expected population size for tip branches only for linear population functions.", Validate.REQUIRED);

    private Gamma gamma2Prior;
    private Gamma gamma4Prior;

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

        // bottom prior = Gamma(2,Psi)
        gamma2Prior = new Gamma();
        gamma2Prior.betaInput.setValue(gammaParameterInput.get(), gamma2Prior);

        // top prior = Gamma(4,Psi)
        gamma4Prior = new Gamma();
        final RealParameter parameter = new RealParameter(new Double[]{4.0});
        gamma4Prior.alphaInput.setValue(parameter, gamma4Prior);
        gamma4Prior.betaInput.setValue(gammaParameterInput.get(), gamma4Prior);

        if (popFunction != TreePopSizeFunction.constant && gamma4Prior == null) {
            throw new IllegalArgumentException("Top prior must be specified when population function is not constant");
        }
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
//				double popSize = m_fPopSizesBottom.getValue(i);
//				logP += m_bottomPrior.logDensity(popSize); 
//			}
                    break;
                case linear:
                    // linear pop size function
//			int speciesCount = m_tree.get().getLeafNodeCount();
//			m_fPopSizesBottom.setDimension(speciesCount);
//			logP += m_gamma4Prior.calcLogP(m_fPopSizesBottom);
//			int nodeCount = m_tree.get().getNodeCount();
//			m_fPopSizesTop.setDimension(nodeCount-1);
//			logP += m_gamma2Prior.calcLogP(m_fPopSizesTop);

                    for (int i = 0; i < speciesNodes.length; i++) {
                        final Node node = speciesNodes[i];
                        final double popSizeBottom;
                        if (node.isLeaf()) {
                            // Gamma(4, psi) prior
                            popSizeBottom = popSizesBottom.getValue(i);
                            logP += gamma4Prior.logDensity(popSizeBottom);
                        }
                        final double popSizeTop = popSizesTop.getValue(i);
                        logP += gamma2Prior.logDensity(popSizeTop);
                    }
                    break;
                case linear_with_constant_root:
//			logP += m_gamma4Prior.calcLogP(m_fPopSizesBottom);
//			logP += m_gamma2Prior.calcLogP(m_fPopSizesTop);
//			int rootNr = m_tree.get().getRoot().getNr();
//			double popSize = m_fPopSizesTop.getValue(rootNr);
//			logP -= m_gamma2Prior.logDensity(popSize); 

                    for (int i = 0; i < speciesNodes.length; i++) {
                        final Node node = speciesNodes[i];
                        if (node.isLeaf()) {
                            final double popSizeBottom = popSizesBottom.getValue(i);
                            logP += gamma4Prior.logDensity(popSizeBottom);
                        }
                        if (!node.isRoot()) {
                            if (i < speciesNodes.length - 1) {
                                final double popSizeTop = popSizesTop.getArrayValue(i);
                                logP += gamma2Prior.logDensity(popSizeTop);
                            } else {
                                final int nodeIndex = treeInput.get().getRoot().getNr();
                                final double popSizeTop = popSizesTop.getArrayValue(nodeIndex);
                                logP += gamma2Prior.logDensity(popSizeTop);
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
