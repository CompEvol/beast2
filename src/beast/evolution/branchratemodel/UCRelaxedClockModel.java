package beast.evolution.branchratemodel;


import java.util.Arrays;

import org.apache.commons.math.MathException;

import beast.core.Citation;
import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.math.distributions.ParametricDistribution;
import beast.util.Randomizer;
import beast.math.distributions.LogNormalDistributionModel;

/**
 * @author Alexei Drummond
 */

@Description("Defines an uncorrelated relaxed molecular clock.")
@Citation(value =
        "Drummond AJ, Ho SYW, Phillips MJ, Rambaut A (2006) Relaxed Phylogenetics and\n" +
                "  Dating with Confidence. PLoS Biol 4(5): e88", DOI = "10.1371/journal.pbio.0040088",
        year = 2006, firstAuthorSurname = "drummond")
public class UCRelaxedClockModel extends BranchRateModel.Base {
    final public Input<ParametricDistribution> rateDistInput = new Input<>("distr", "the distribution governing the rates among branches. Must have mean of 1. The clock.rate parameter can be used to change the mean rate.", Input.Validate.REQUIRED);
    final public Input<IntegerParameter> categoryInput = new Input<>("rateCategories", "the rate categories associated with nodes in the tree for sampling of individual rates among branches.", Input.Validate.REQUIRED);
    final public Input<Integer> numberOfDiscreteRates = new Input<>("numberOfDiscreteRates", "the number of discrete rate categories to approximate the rate distribution by. A value <= 0 will cause the number of categories to be set equal to the number of branches in the tree. (default = -1)", -1);
    final public Input<RealParameter> quantileInput = new Input<>("rateQuantiles", "the rate quantiles associated with nodes in the tree for sampling of individual rates among branches.");
    final public Input<RealParameter> rateInput = new Input<>("rates", "the rates associated with nodes in the tree for sampling of individual rates among branches.", Input.Validate.XOR, categoryInput);
    final public Input<Tree> treeInput = new Input<>("tree", "the tree this relaxed clock is associated with.", Input.Validate.REQUIRED);
    final public Input<Boolean> normalizeInput = new Input<>("normalize", "Whether to normalize the average rate (default false).", false);
    // there are three modes to represent the rates on the branches
    enum Mode {
        categories,
        quantiles,
        rates
    }
    Mode mode = Mode.categories;//initialize the mode
    // either categories or quantiles or rateParameter is used
    RealParameter rateParameter; //when mode=rates
    IntegerParameter categories; //when mode=categories
    RealParameter quantiles; // when mode=quantiles

    // if using categories, then it is set to be true; otherwise, it is set to be false.
    //boolean usingcategories;

    ParametricDistribution distribution; //the distribution of the rates
    // i.e. LogNormal(M,S,MeanInRealSpace), then get the three parameters
    double M;double S;boolean MeanInRealSpace;

    RealParameter meanRate;
    Tree tree;
    private int branchCount;//the number of branches of the tree
    private boolean normalize = false;//
    private boolean recompute = true;//
    private boolean renormalize = true;//
    private double[] rates; //the output rates
    private double[] storedRates; //
    private double scaleFactor = 1.0; //initial
    private double storedScaleFactor = 1.0; //initial
    int LATTICE_SIZE_FOR_DISCRETIZED_RATES = 100;//

    @Override
    public void initAndValidate() {
        tree = treeInput.get();
        branchCount = tree.getNodeCount() - 1;
        categories = categoryInput.get();
        quantiles = quantileInput.get();
        rateParameter = rateInput.get();
        distribution = rateDistInput.get();

        //get the mean and standard deviation of lognormal distribution
        if(distribution instanceof LogNormalDistributionModel){
            LogNormalDistributionModel mylognormal=(LogNormalDistributionModel)distribution;
             M=mylognormal.MParameterInput.get().getValue();
             S=mylognormal.SParameterInput.get().getValue();
             MeanInRealSpace=mylognormal.hasMeanInRealSpaceInput.get();
        }
        // if categories is null, then usingcategories is false; otherwise, it is set to be true.
        //if(categories==null){
            //usingcategories = false;
        //}
       if (categories == null) {
            if (quantiles != null) {
                mode = Mode.quantiles;
            } else if (rateParameter != null) {
                mode = Mode.rates;
            }
       } else
           mode = Mode.categories; // usingcategories = false; //right or wrong?
        //Initialization for three modes
        //(1)// print information about which mode is used
        if (mode == Mode.categories) {
            LATTICE_SIZE_FOR_DISCRETIZED_RATES = numberOfDiscreteRates.get();
            if (LATTICE_SIZE_FOR_DISCRETIZED_RATES <= 0) LATTICE_SIZE_FOR_DISCRETIZED_RATES = branchCount;
            Log.info.println("  UCRelaxedClockModel: using " + LATTICE_SIZE_FOR_DISCRETIZED_RATES + " rate " +
                    "categories to approximate rate distribution across branches.");
        } else {
            if (numberOfDiscreteRates.get() != -1) {
                throw new RuntimeException("Can't specify both numberOfDiscreteRates and rateQuantiles or rates inputs.");
            }
            else {
                if (mode == Mode.rates) {
                    Log.info.println("  UCRelaxedClockModel: using real rates for rate distribution across branches.");
                }
                else {
                    Log.info.println("  UCRelaxedClockModel: using quantiles for rate distribution across branches.");
                }
            }
        }
        //initialize rates in three modes
        switch (mode) {
            case quantiles: {
                quantiles.setDimension(branchCount);
                Double[] initialQuantiles = new Double[branchCount];
                for (int i = 0; i < branchCount; i++) {
                    initialQuantiles[i] = Randomizer.nextDouble();
                }
                RealParameter other = new RealParameter(initialQuantiles);
                quantiles.assignFromWithoutID(other);
                quantiles.setLower(0.0);
                quantiles.setUpper(1.0);
            }
            break;
            case categories: {
                categories.setDimension(branchCount);
                Integer[] initialCategories = new Integer[branchCount];
                for (int i = 0; i < branchCount; i++) {
                    initialCategories[i] = Randomizer.nextInt(LATTICE_SIZE_FOR_DISCRETIZED_RATES);
                }
                // set initial values of rate categories
                IntegerParameter other = new IntegerParameter(initialCategories);
                categories.assignFromWithoutID(other);
                categories.setLower(0);
                categories.setUpper(LATTICE_SIZE_FOR_DISCRETIZED_RATES - 1);
            }
            break;
            case rates: {
                if (rateParameter.getDimension() != branchCount) {
                    rateParameter.setDimension(branchCount);
                    //randomly draw rates from the lognormal distribution
                    Double [] initialRates = new Double[branchCount];
                    for (int i = 0; i < branchCount; i++) {
                        initialRates[i] =Randomizer.nextLogNormal(M,S,MeanInRealSpace);
                    }
                    RealParameter other = new RealParameter(initialRates);
                    rateParameter.assignFromWithoutID(other);
                }
                rateParameter.setLower(0.0);
            }
        }

        if (mode == Mode.categories) {
            // rates are initially zero and are computed by getRawRate(int i) as needed
            rates = new double[LATTICE_SIZE_FOR_DISCRETIZED_RATES];
            storedRates = new double[LATTICE_SIZE_FOR_DISCRETIZED_RATES];
            //System.arraycopy(rates, 0, storedRates, 0, rates.length);
        }
        normalize = normalizeInput.get();
        meanRate = meanRateInput.get();
        if (meanRate == null) {
            meanRate = new RealParameter("1.0");
        }
        try {
            double mean = rateDistInput.get().getMean();
            if (Math.abs(mean - 1.0) > 1e-6) {
                Log.warning.println("WARNING: mean of distribution for relaxed clock model is not 1.0.");
            }
        } catch (RuntimeException e) {
            // ignore
        }
    }

    @Override
    //get the rate for node
    //R=r*scale*meanRate
    public double getRateForBranch(Node node) {
        if (node.isRoot()) {
            // root has no rate
            return 1;
        }
        if (recompute) {
            // this must be synchronized to avoid being called simultaneously by
            // two different likelihood threads
            synchronized (this) {
                prepare();
                recompute = false;
            }
        }
        if (renormalize) {
            if (normalize) {
                synchronized (this) {
                    computeFactor();
                }
            }
            renormalize = false;
        }
        return getRawRate(node) * scaleFactor * meanRate.getValue();
    }

    /**
     * Computes a scale factor for normalization. Only called if normalize=true.
     */
    private void computeFactor() {
        //scale mean rate to 1.0 or separate parameter
        double treeRate = 0.0;
        double treeTime = 0.0;
        for (int i = 0; i < tree.getNodeCount(); i++) {
            Node node = tree.getNode(i);
            if (!node.isRoot()) {
                treeRate += getRawRate(node) * node.getLength();
                treeTime += node.getLength();
            }
        }
        scaleFactor = 1.0 / (treeRate / treeTime);
    }

    /**
     * Computes Raw rate for node
     */
    private double getRawRate(Node node) {
        switch (mode) {
            case categories: return getRawRateForCategory(node);
            case quantiles: return getRawRateForQuantile(node);
            case rates:default: return getRawRateFromRates(node);
        }
    }
    // when mode=rates, return the value in rateParameter, i.e. what is input
    private double getRawRateFromRates(Node node) {
        int nodeNumber = node.getNr();
        if (nodeNumber == branchCount) {
            // root node has nr less than #categories, so use that nr
            nodeNumber = node.getTree().getRoot().getNr();
        }
        return rateParameter.getValue(nodeNumber);
    }
    // when mode=categories
    private double getRawRateForCategory(Node node) {
        int nodeNumber = node.getNr();
        if (nodeNumber == branchCount) {
            // root node has nr less than #categories, so use that nr
            nodeNumber = node.getTree().getRoot().getNr();
        }
        int category = categories.getValue(nodeNumber);
        if (rates[category] == 0.0) {
            try {
                rates[category] = distribution.inverseCumulativeProbability((category + 0.5) / rates.length);
            } catch (MathException e) {
                throw new RuntimeException("Failed to compute inverse cumulative probability!");
            }
        }
        return rates[category];
    }
    // when mode=quantiles
    private double getRawRateForQuantile(Node node) {
        int nodeNumber = node.getNr();
        if (nodeNumber == branchCount) {
            // root node has nr less than #categories, so use that nr
            nodeNumber = node.getTree().getRoot().getNr();
        }
        try {
            return distribution.inverseCumulativeProbability(quantiles.getValue(nodeNumber));
        } catch (MathException e) {
            throw new RuntimeException("Failed to compute inverse cumulative probability!");
        }
    }

   //???
  private void prepare() {
        if (mode == Mode.categories) {
            // rates array initialized to correct length in initAndValidate
            // here we just reset rates to zero and they are computed by getRawRate(int i) as needed
           Arrays.fill(rates, 0.0);
        }
    }

    //???
    @Override
    protected boolean requiresRecalculation() {
        recompute = false;
        renormalize = true;

//        if (treeInput.get().somethingIsDirty()) {
//        	recompute = true;
//            return true;
//        }
        // rateDistInput cannot be dirty?!?
        if (rateDistInput.get().isDirtyCalculation()) {
            recompute = true;
            return true;
        }
        // NOT processed as trait on the tree, so DO mark as dirty
        if (categoryInput.get() != null && categoryInput.get().somethingIsDirty()) {
            //recompute = true;
            return true;
        }

        if (quantileInput.get() != null && quantileInput.get().somethingIsDirty()) {
            return true;
        }

        if (rateInput.get() != null && rateInput.get().somethingIsDirty()) {
            return true;
        }
        if (meanRate.somethingIsDirty()) {
            return true;
        }

        return recompute;
    }

    @Override
    public void store() {
        //used to "if (!usingquantiles)", which means categories are used
            if (mode == Mode.categories) {
           System.arraycopy(rates, 0, storedRates, 0, rates.length);
            //double[] tmp1 = rates;
            //rates = storedRates;
            //storedRates = tmp1;
        }
        storedScaleFactor = scaleFactor;
        super.store();
    }

    @Override
    public void restore() {
        if(mode == Mode.categories){
            double[] tmp = rates;
            rates = storedRates;
            storedRates = tmp;
        }
        scaleFactor = storedScaleFactor;
        super.restore();
    }


}