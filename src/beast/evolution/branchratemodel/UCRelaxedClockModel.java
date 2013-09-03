package beast.evolution.branchratemodel;



import beast.core.Citation;
import beast.core.Description;
import beast.core.Input;
import beast.core.BEASTObject;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.JukesCantor;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.math.distributions.ParametricDistribution;

/**
 * @author Alexei Drummond
 */

@Description("Defines an uncorrelated relaxed molecular clock.")
@Citation(value = "Drummond AJ, Ho SYW, Phillips MJ, Rambaut A (2006) Relaxed Phylogenetics and Dating with Confidence. PLoS Biol 4(5): e88", DOI = "10.1371/journal.pbio.0040088")
public class UCRelaxedClockModel extends BranchRateModel.Base {

    public Input<ParametricDistribution> rateDistInput = new Input<ParametricDistribution>("distr", "the distribution governing the rates among branches. Must have mean of 1. The clock.rate parameter can be used to change the mean rate.", Input.Validate.REQUIRED);
    public Input<IntegerParameter> categoryInput = new Input<IntegerParameter>("rateCategories", "the rate categories associated with nodes in the tree for sampling of individual rates among branches.", Input.Validate.REQUIRED);
    public Input<Tree> treeInput = new Input<Tree>("tree", "the tree this relaxed clock is associated with.", Input.Validate.REQUIRED);
    public Input<Boolean> normalizeInput = new Input<Boolean>("normalize", "Whether to normalize the average rate (default false).", false);
//    public Input<Boolean> initialiseInput = new Input<Boolean>("initialise", "Whether to initilise rates by a heuristic instead of random (default false).", false);

    RealParameter meanRate;
//    boolean initialise;

    @Override
    public void initAndValidate() throws Exception {

        tree = treeInput.get();

        categories = categoryInput.get();
        int nCategoryCount = tree.getNodeCount() - 1;
        categories.setDimension(nCategoryCount);
        Integer[] iCategories = new Integer[nCategoryCount];
        for (int i = 0; i < nCategoryCount; i++) {
            iCategories[i] = i;
        }
        IntegerParameter other = new IntegerParameter(iCategories);
        categories.assignFromWithoutID(other);
        categories.setLower(0);
        categories.setUpper(categories.getDimension() - 1);

        distribution = rateDistInput.get();

        rates = new double[categories.getDimension()];
        storedRates = new double[categories.getDimension()];
        for (int i = 0; i < rates.length; i++) {
            rates[i] = distribution.inverseCumulativeProbability((i + 0.5) / rates.length);
        }
        System.arraycopy(rates, 0, storedRates, 0, rates.length);
        normalize = normalizeInput.get();

        meanRate = meanRateInput.get();
        if (meanRate == null) {
            meanRate = new RealParameter("1.0");
        }
        
        try {
        	double mean = rateDistInput.get().getMean();
        	if (Math.abs(mean-1.0) > 1e-6) {
        		System.out.println("WARNING: mean of distribution for relaxed clock model is not 1.0.");
        	}
        } catch (RuntimeException e) {
			// ignore
		}
//        initialise = initialiseInput.get();
    }

    public double getRateForBranch(Node node) {
        if (node.isRoot()) {
            // root has no rate
            return 1;
        }
        if (recompute) {
            prepare();
            recompute = false;
        }
        if (renormalize) {
            if (normalize) {
                computeFactor();
            }
            renormalize = false;
        }

        int nodeNumber = node.getNr();

        if (nodeNumber == categories.getDimension()) {
            // root node has nr less than #categories, so use that nr
            nodeNumber = node.getTree().getRoot().getNr();
        }

        int rateCategory = categories.getValue(nodeNumber);

        return rates[rateCategory] * scaleFactor * meanRate.getValue();
    }

    // compute scale factor

    private void computeFactor() {

        //scale mean rate to 1.0 or separate parameter

        double treeRate = 0.0;
        double treeTime = 0.0;

        for (int i = 0; i < tree.getNodeCount(); i++) {
            Node node = tree.getNode(i);
            if (!node.isRoot()) {
                int nodeNumber = node.getNr();
                if (nodeNumber == categories.getDimension()) {
                    // root node has nr less than #categories, so use that nr
                    nodeNumber = node.getTree().getRoot().getNr();
                }
                int rateCategory = categories.getValue(nodeNumber);
                treeRate += rates[rateCategory] * node.getLength();
                treeTime += node.getLength();

                //System.out.println("rates and time\t" + rates[rateCategory] + "\t" + node.getLength());
            }
        }
        //treeRate /= treeTime;

        scaleFactor = 1.0 / (treeRate / treeTime);


        //System.out.println("scaleFactor\t\t\t\t\t" + scaleFactor);
    }


    private void prepare() {
//    	if (initialise) {
//    		initialise();
//    		initialise = false;
//    	}
        //System.out.println("prepare");

        categories = (IntegerParameter) categoryInput.get();

        distribution = rateDistInput.get();

        tree = treeInput.get();

        rates = new double[categories.getDimension()];
        try {
            for (int i = 0; i < rates.length; i++) {
                rates[i] = distribution.inverseCumulativeProbability((i + 0.5) / rates.length);
            }
        } catch (Exception e) {
            // Exception due to distribution not having  inverseCumulativeProbability implemented.
            // This should already been caught at initAndValidate()
            e.printStackTrace();
            System.exit(0);
        }

        //if (normalize) computeFactor();
    }

    /** initialise rate categories by matching rates to tree using JC69 **/
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
//            double [] fParentPartials = new double[patterncount * statecount];
//    		likelihoodCore.getNodePartials(node.getParent().getNr(), fParentPartials);
//    		if (node.isLeaf()) {
//        		// distance of leaf to its parent, ignores ambiguities
//    			int [] nStates = new int[patterncount ];
//        		likelihoodCore.getNodeStates(iNode, nStates);
//        		double distance = 0;
//        		for (int i = 0; i < patterncount; i++) {
//        			int k = nStates[i];
//        			double d = 0;
//        			for (int j = 0; j < statecount; j++) {
//        				if (j == k) {
//        					d += 1.0 - fParentPartials[i * statecount + j];
//        				} else {
//        					d += fParentPartials[i * statecount + j];
//        				}
//        			}
//        			distance += d * dataInput.get().getPatternWeight(i);
//        		}
//    			return distance;
//    		} else {
//        		// L1 distance of internal node partials to its parent partials
//                double [] fPartials = new double[fParentPartials.length];
//        		likelihoodCore.getNodePartials(iNode, fPartials);
//        		double distance = 0;
//        		for (int i = 0; i < patterncount; i++) {
//        			double d = 0;
//        			for (int j = 0; j < statecount; j++) {
//       					d += Math.abs(fPartials[i * statecount + j] - fParentPartials[i * statecount + j]);
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
        if (categoryInput.get().somethingIsDirty()) {
            //recompute = true;
            return true;
        }
        if (meanRate.somethingIsDirty()) {
            return true;
        }

        return recompute;
    }

    @Override
    public void store() {
        System.arraycopy(rates, 0, storedRates, 0, rates.length);
        storedScaleFactor = scaleFactor;
        super.store();
    }

    @Override
    public void restore() {
        double[] tmp = rates;
        rates = storedRates;
        storedRates = tmp;
        scaleFactor = storedScaleFactor;
        super.restore();
    }

    ParametricDistribution distribution;
    IntegerParameter categories;
    Tree tree;

    private boolean normalize = false;
    private boolean recompute = true;
    private boolean renormalize = true;

    private double[] rates;
    private double[] storedRates;
    private double scaleFactor = 1.0;
    private double storedScaleFactor = 1.0;

}
