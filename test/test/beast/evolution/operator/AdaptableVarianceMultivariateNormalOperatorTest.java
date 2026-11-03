package test.beast.evolution.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.jupiter.api.Test;

import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.evolution.operator.kernel.AdaptableVarianceMultivariateNormalOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.Distribution;
import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.distribution.Prior;
import beast.base.inference.operator.kernel.Transform;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;

/**
 * Hastings ratio of the AVMN operator when it moves a tree. AVMN sets the root
 * height and scales all dof internal node heights to match, so the proposal must
 * add (dof - 1) log(s) on top of the transform's own Jacobian for the root height.
 * BEAUti puts the tree in a NoTransform, where that own Jacobian is 0.
 */
public class AdaptableVarianceMultivariateNormalOperatorTest {

    @Test
    public void noTransformTreeCountsTheCoScaledHeights() throws Exception {
        assertLogHastingsRatio(new Transform.NoTransform(), 2);
    }

    @Test
    public void logTransformTreeCountsEveryScaledHeight() throws Exception {
        assertLogHastingsRatio(new Transform.LogTransform(), 3);
    }

    /**
     * expectedTreeDims is the multiple of log(h'/h), h the root height, the proposal
     * must return for the 4-taxon ultrametric tree (dof 3): dof - 1 under
     * NoTransform, dof under LogTransform. The log-transformed parameter adds log(p'/p).
     */
    private void assertLogHastingsRatio(Transform treeTransform, int expectedTreeDims) throws Exception {
        Randomizer.setSeed(127);
        Tree tree = ultrametricTree();
        RealParameter param = new RealParameter(new Double[]{1.0});
        param.setID("param");
        AdaptableVarianceMultivariateNormalOperator operator = avmn(param, tree, treeTransform);
        registerInState(tree, param);

        int proposed = 0;
        for (int i = 0; i < 200; i++) {
            double[] heights = heightsOf(tree);
            double h = tree.getRoot().getHeight();
            double p = param.getValue();

            double logHR = operator.proposal();
            if (logHR != Double.NEGATIVE_INFINITY) {
                proposed++;
                double expected = expectedTreeDims * Math.log(tree.getRoot().getHeight() / h)
                        + Math.log(param.getValue() / p);
                assertEquals(expected, logHR, 1e-10, "proposal " + i);
            }

            restoreHeights(tree, heights);
            param.setValue(p);
        }
        assertTrue(proposed > 0, "every proposal was rejected");
    }

    /**
     * Density exp(-S) on node heights, S the sum of internal node heights. AVMN is
     * the only tree operator, so the height proportions stay fixed and S must be
     * sampled from Gamma(3, 1). This is the BEAUti set-up: the tree in a NoTransform,
     * a parameter in a LogTransform.
     */
    @Test
    public void noTransformTreeSamplesFromGammaPrior() throws Exception {
        Randomizer.setSeed(123);

        Tree tree = ultrametricTree();
        RealParameter param = new RealParameter(new Double[]{1.0});
        param.setID("param");

        AdaptableVarianceMultivariateNormalOperator operator = avmn(param, tree, new Transform.NoTransform());

        ExpSumOfHeights treePrior = new ExpSumOfHeights();
        treePrior.initByName("tree", tree);
        LogNormalDistributionModel logNormal = new LogNormalDistributionModel();
        logNormal.initByName("M", "1.0", "S", "1.0", "meanInRealSpace", true);
        Prior paramPrior = new Prior();
        paramPrior.initByName("x", param, "distr", logNormal);
        CompoundDistribution posterior = new CompoundDistribution();
        posterior.initByName("distribution", treePrior, "distribution", paramPrior);

        State state = new State();
        state.initByName("stateNode", tree, "stateNode", param);

        Trace trace = new Trace();
        trace.initByName("log", tree, "log", param, "logEvery", 10, "burnin", 10000);

        MCMC mcmc = new MCMC();
        mcmc.initByName(
                "chainLength", "2000000",
                "state", state,
                "distribution", posterior,
                "operator", operator,
                "logger", trace);
        mcmc.run();

        double[] s = trace.sumOfHeights();
        double[] p = trace.paramValues();
        // 3 internal nodes: Gamma(3, 1) has mean 3 and variance 3
        assertEquals(3.0, StatUtils.mean(s), 0.05);
        assertEquals(3.0, StatUtils.variance(s), 0.2);
        // LogNormal with real-space mean 1 and S = 1
        assertEquals(1.0, StatUtils.mean(p), 0.05);
        assertEquals(Math.exp(-0.5), StatUtils.percentile(p, 50), 0.03);
    }

    private AdaptableVarianceMultivariateNormalOperator avmn(RealParameter param, Tree tree,
                                                            Transform treeTransform) {
        Transform.LogTransform paramTransform = new Transform.LogTransform();
        paramTransform.initByName("f", param);
        ((BEASTObject) treeTransform).initByName("f", tree);
        AdaptableVarianceMultivariateNormalOperator operator =
                new AdaptableVarianceMultivariateNormalOperator();
        operator.initByName("weight", 1.0,
                "transformations", Arrays.asList(paramTransform, treeTransform),
                "beta", 0.05, "optimise", false);
        return operator;
    }

    /** Internal node heights 1, 0.5 and 2 (root). */
    private Tree ultrametricTree() {
        Tree tree = new TreeParser("((A:1,B:1):1,(C:0.5,D:0.5):1.5);");
        tree.setID("tree");
        return tree;
    }

    private static double sumOfInternalHeights(Tree tree) {
        double sum = 0;
        for (Node node : tree.getInternalNodes()) {
            sum += node.getHeight();
        }
        return sum;
    }

    private double[] heightsOf(Tree tree) {
        Node[] nodes = tree.getNodesAsArray();
        double[] heights = new double[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            heights[i] = nodes[i].getHeight();
        }
        return heights;
    }

    private void restoreHeights(Tree tree, double[] heights) {
        Node[] nodes = tree.getNodesAsArray();
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].setHeight(heights[i]);
        }
    }

    /** Operators need their StateNodes owned by a State before proposal() works. */
    private void registerInState(StateNode... nodes) throws Exception {
        State state = new State();
        state.initByName("stateNode", Arrays.asList(nodes));
        state.initialise();
        state.setPosterior(new CompoundDistribution());
    }

    /** log density -S on node heights, S the sum of internal node heights. */
    public static class ExpSumOfHeights extends Distribution {
        final public Input<Tree> treeInput = new Input<>("tree", "tree", Input.Validate.REQUIRED);

        @Override
        public double calculateLogP() {
            logP = -sumOfInternalHeights(treeInput.get());
            return logP;
        }

        @Override
        protected boolean requiresRecalculation() {
            return true;
        }

        @Override
        public List<String> getArguments() {
            return Arrays.asList(treeInput.get().getID());
        }

        @Override
        public List<String> getConditions() {
            return new ArrayList<>();
        }

        @Override
        public void sample(State state, Random random) {
            throw new UnsupportedOperationException();
        }
    }

    /** Records the logged tree's sum of internal node heights and the logged parameter. */
    public static class Trace extends Logger {
        final public Input<Integer> burninInput = new Input<>("burnin", "samples to skip", 0);

        private final List<Double> sumOfHeights = new ArrayList<>();
        private final List<Double> paramValues = new ArrayList<>();

        @Override
        public void initAndValidate() {
        }

        @Override
        public void init() {
        }

        @Override
        public void log(long sample) {
            if (sample % everyInput.get() > 0 || sample < burninInput.get()) {
                return;
            }
            for (BEASTObject o : loggersInput.get()) {
                if (o instanceof Tree) {
                    sumOfHeights.add(sumOfInternalHeights((Tree) o));
                } else if (o instanceof RealParameter) {
                    paramValues.add(((RealParameter) o).getValue());
                }
            }
        }

        @Override
        public void close() {
        }

        double[] sumOfHeights() {
            return sumOfHeights.stream().mapToDouble(Double::doubleValue).toArray();
        }

        double[] paramValues() {
            return paramValues.stream().mapToDouble(Double::doubleValue).toArray();
        }
    }
}
