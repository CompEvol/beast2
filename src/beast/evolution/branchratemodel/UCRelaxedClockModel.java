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

    final public Input<RealParameter> quantileInput = new Input<>("rateQuantiles", "the rate quantiles associated with nodes in the tree for sampling of individual rates among branches.", Input.Validate.XOR, categoryInput);

    final public Input<Tree> treeInput = new Input<>("tree", "the tree this relaxed clock is associated with.", Input.Validate.REQUIRED);
    final public Input<Boolean> normalizeInput = new Input<>("normalize", "Whether to normalize the average rate (default false).", false);
//    public Input<Boolean> initialiseInput = new Input<>("initialise", "Whether to initialise rates by a heuristic instead of random (default false).", false);

    RealParameter meanRate;
//    boolean initialise;

    int LATTICE_SIZE_FOR_DISCRETIZED_RATES = 100;

    // true if quantiles are used, false if discrete rate categories are used.
    boolean usingQuantiles;

    private int branchCount;

    @Override
    public void initAndValidate() throws Exception {

        tree = treeInput.get();
        branchCount = tree.getNodeCount() - 1;

        categories = categoryInput.get();
        usingQuantiles = (categories == null);

        if (!usingQuantiles) {
            LATTICE_SIZE_FOR_DISCRETIZED_RATES = numberOfDiscreteRates.get();
            if (LATTICE_SIZE_FOR_DISCRETIZED_RATES <= 0) LATTICE_SIZE_FOR_DISCRETIZED_RATES = branchCount;
            Log.info.println("  UCRelaxedClockModel: using " + LATTICE_SIZE_FOR_DISCRETIZED_RATES + " rate " +
                    "categories to approximate rate distribution across branches.");
        } else {
            if (numberOfDiscreteRates.get() != -1) {
                throw new RuntimeException("Can't specify both numberOfDiscreteRates and rateQuantiles inputs.");
            }
            Log.info.println("  UCRelaxedClockModel: using quantiles for rate distribution across branches.");
        }

        if (usingQuantiles) {
            quantiles = quantileInput.get();
            quantiles.setDimension(branchCount);
            Double[] initialQuantiles = new Double[branchCount];
            for (int i = 0; i < branchCount; i++) {
                initialQuantiles[i] = Randomizer.nextDouble();
            }
            RealParameter other = new RealParameter(initialQuantiles);
            quantiles.assignFromWithoutID(other);
            quantiles.setLower(0.0);
            quantiles.setUpper(1.0);
        } else {
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

        distribution = rateDistInput.get();

        if (!usingQuantiles) {
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

        if (!usingQuantiles) {
            for (int i = 0; i < tree.getNodeCount(); i++) {
                Node node = tree.getNode(i);
                if (!node.isRoot()) {
                    treeRate += getRawRateForCategory(node) * node.getLength();
                    treeTime += node.getLength();
                }
            }
        } else {
            for (int i = 0; i < tree.getNodeCount(); i++) {
                Node node = tree.getNode(i);
                if (!node.isRoot()) {
                    treeRate += getRawRateForQuantile(node) * node.getLength();
                    treeTime += node.getLength();
                }
            }
        }

        scaleFactor = 1.0 / (treeRate / treeTime);
    }

    private double getRawRate(Node node) {
        if (usingQuantiles) {
            return getRawRateForQuantile(node);
        }
        return getRawRateForCategory(node);
    }

    /**
     * @param node the node to get the rate of
     * @return the rate of the branch
     */
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

    private void prepare() {

        categories = categoryInput.get();

        usingQuantiles = (categories == null);

        distribution = rateDistInput.get();

        tree = treeInput.get();

        if (!usingQuantiles) {
            // rates array initialized to correct length in initAndValidate
            // here we just reset rates to zero and they are computed by getRawRate(int i) as needed
            Arrays.fill(rates, 0.0);
        }
    }

    /**
     * initialise rate categories by matching rates to tree using JC69 *
     */
//    private void initialise() {
//    	try {
//			for (BEASTObject output : outputs) {
//				if (output.getInput("data") != null && output.getInput("tree") != null) {
//					
//					// set up treelikelihood with Jukes Cantor no gamma, no inv, strict clock
//					Alignment data = (Alignment) output.getInput("data").get();
//					Tree tree = (Tree) output.getInput("tree").get();
//					TreeLikelihoodD likelihood = new TreeLikelihoodD();
//					SiteModel siteModel = new SiteModel();
//					JukesCantor substitutionModel = new JukesCantor();
//					substitutionModel.initAndValidate();
//					siteModel.initByName("substModel", substitutionModel);
//					likelihood.initByName("data", data, "tree", tree, "siteModel", siteModel);
//					likelihood.calculateLogP();
//					
//					// calculate distances
//					Node [] nodes = tree.getNodesAsArray();
//					double [] distance = new double[nodes.length];
//					for (int i = 0; i < distance.length - 1; i++) {
//						double len = nodes[i].getLength();
//						double dist = likelihood.calcDistance(nodes[i]);
//						distance[i] = len / dist;
//					}
//					
//					// match category to distance
//					double min = distance[0], max = min;
//					for (int i = 1; i < distance.length - 1; i++) {
//						if (!Double.isNaN(distance[i]) && !Double.isInfinite(distance[i])) {
//							min = Math.min(min, distance[i]);
//							max = Math.max(max, distance[i]);
//						}
//					}
//					IntegerParameter categoriesParameter = categoryInput.get();
//					Integer[] categories = new Integer[categoriesParameter.getDimension()];
//					for (int i = 0; i < distance.length - 1; i++) {
//						if (!Double.isNaN(distance[i]) && !Double.isInfinite(distance[i])) {
//							categories[i] = (int)((distance.length - 2) * (distance[i]-min)/(max-min));
//						} else {
//							categories[i] = distance.length / 2;
//						}
//					}
//					IntegerParameter other = new IntegerParameter(categories);
//					other.setBounds(categoriesParameter.getLower(), categoriesParameter.getUpper());
//					categoriesParameter.assignFromWithoutID(other);
//				}
//			}
//    	} catch (Exception e) {
//			// ignore
//    		System.err.println("WARNING: UCRelaxedClock heuristic initialisation failed");
//		}
//	}
//
//    @Description("Treelikelihood used to guesstimate rates on branches by using the JC69 model on the data")
//    class TreeLikelihoodD extends TreeLikelihood {
//    
//    	double calcDistance(Node node) {
//    		int iNode = node.getNr();
//    		int patterncount = dataInput.get().getPatternCount();
//    		int statecount = dataInput.get().getDataType().getStateCount();
//            double [] parentPartials = new double[patterncount * statecount];
//    		likelihoodCore.getNodePartials(node.getParent().getNr(), parentPartials);
//    		if (node.isLeaf()) {
//        		// distance of leaf to its parent, ignores ambiguities
//    			int [] states = new int[patterncount ];
//        		likelihoodCore.getNodeStates(iNode, states);
//        		double distance = 0;
//        		for (int i = 0; i < patterncount; i++) {
//        			int k = states[i];
//        			double d = 0;
//        			for (int j = 0; j < statecount; j++) {
//        				if (j == k) {
//        					d += 1.0 - parentPartials[i * statecount + j];
//        				} else {
//        					d += parentPartials[i * statecount + j];
//        				}
//        			}
//        			distance += d * dataInput.get().getPatternWeight(i);
//        		}
//    			return distance;
//    		} else {
//        		// L1 distance of internal node partials to its parent partials
//                double [] partials = new double[parentPartials.length];
//        		likelihoodCore.getNodePartials(iNode, partials);
//        		double distance = 0;
//        		for (int i = 0; i < patterncount; i++) {
//        			double d = 0;
//        			for (int j = 0; j < statecount; j++) {
//       					d += Math.abs(partials[i * statecount + j] - parentPartials[i * statecount + j]);
//        			}
//        			distance += d * dataInput.get().getPatternWeight(i);
//        		}
//    			return distance;
//    		}
//    	}
//    	
//    }
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

        if (meanRate.somethingIsDirty()) {
            return true;
        }

        return recompute;
    }

    @Override
    public void store() {
        if (!usingQuantiles) System.arraycopy(rates, 0, storedRates, 0, rates.length);

        storedScaleFactor = scaleFactor;
        super.store();
    }

    @Override
    public void restore() {
        if (!usingQuantiles) {
            double[] tmp = rates;
            rates = storedRates;
            storedRates = tmp;
        }
        scaleFactor = storedScaleFactor;
        super.restore();
    }

    ParametricDistribution distribution;
    IntegerParameter categories;
    RealParameter quantiles;
    Tree tree;

    private boolean normalize = false;
    private boolean recompute = true;
    private boolean renormalize = true;

    private double[] rates;
    private double[] storedRates;
    private double scaleFactor = 1.0;
    private double storedScaleFactor = 1.0;

}