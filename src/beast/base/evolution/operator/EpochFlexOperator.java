package beast.base.evolution.operator;


import java.text.DecimalFormat;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Operator;
import beast.base.inference.operator.kernel.KernelDistribution;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.core.Input.Validate;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeIntervals;
import beast.base.util.Randomizer;

@Description("Scale operator that scales random epoch in a tree")
@Citation(value="Bouckaert RR. An efficient coalescent epoch model for Bayesian phylogenetic inference. Systematic Biology, syac015, 2022", DOI="DOI:10.1093/sysbio/syac015")
public class EpochFlexOperator extends Operator {
    final public Input<Tree> treeInput = new Input<>("tree", "beast.tree on which this operation is performed", Validate.REQUIRED);
    final public Input<KernelDistribution> kernelDistributionInput = new Input<>("kernelDistribution", "provides sample distribution for proposals", 
    		KernelDistribution.newDefaultKernelDistribution());
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);
    final public Input<Double> scaleFactorInput = new Input<>("scaleFactor", "scaling factor -- positive number that determines size of the jump: higher means bigger jumps.", 0.05);

    final public Input<Boolean> fromOldestTipOnlyInput = new Input<>("fromOldestTipOnly", "only scale parts between root and oldest tip. If false, use any epoch between youngest tip and root.", true); 

    final public Input<IntegerParameter> groupSizeParamInput = new Input<>("groupSizes", "the group sizes parameter. If specified, use group sizes as boundaries"
    		+ "(and fromOldestTipOnly is ignored)");
    final public Input<TreeIntervals> treeIntervalsInput = new Input<>("treeIntervals", "Intervals for a phylogenetic beast tree. Must be specified if groupSizes is specified.");
    
    protected KernelDistribution kernelDistribution;
    protected double scaleFactor;
    private boolean fromOldestTipOnly;
    private IntegerParameter groupSizes;
    private TreeIntervals treeIntervals;
    
    @Override
	public void initAndValidate() {
    	kernelDistribution = kernelDistributionInput.get();
    	scaleFactor = scaleFactorInput.get();
    	fromOldestTipOnly = fromOldestTipOnlyInput.get();
    	groupSizes = groupSizeParamInput.get();
    	if (groupSizes != null) {
    		treeIntervals = treeIntervalsInput.get();
    		if (treeIntervals == null) {
    			throw new IllegalArgumentException("treeIntervals must be specified if groupSizes are specified");
    		}
    	}
	}	
	
	public EpochFlexOperator(){}
	
	public EpochFlexOperator(Tree tree, double weight) {
		initByName("tree", tree, "weight", weight);
	}
	
	
	

    @Override
    public double proposal() {
    	Tree tree = treeInput.get();
    	double oldHeight = tree.getRoot().getHeight();
    	
    	double upper = tree.getRoot().getHeight();
		double lower0 = 0;
		Node [] nodes = tree.getNodesAsArray();
		
		if (fromOldestTipOnly) {
			for (int i = 0; i < nodes.length/2+1; i++) {
				lower0 = Math.max(nodes[i].getHeight(), lower0);
			}
		}
	
		double intervalLow = 0;
		double intervalHi = 0;
		
		if (groupSizes != null) {
			int k = Randomizer.nextInt(groupSizes.getDimension());
			
			int j = 0;
			for (int i = 0; i < k; i++) {
				j += groupSizes.getValue(i);
			}
			intervalLow = treeIntervals.getIntervalTime(j);
			intervalHi = treeIntervals.getIntervalTime(j + groupSizes.getValue(k));
		} else {
			int x = Randomizer.nextInt(tree.getInternalNodeCount());
			intervalLow = tree.getNode(tree.getLeafNodeCount() + x).getHeight();
			int y = x;
			while (x == y) {
				y = Randomizer.nextInt(tree.getInternalNodeCount());
				intervalHi  = tree.getNode(tree.getLeafNodeCount() + y).getHeight();
			}
		}
		
		if (intervalHi < intervalLow) {
			// make sure intervalLow < intervalHi
			double tmp = intervalHi; intervalHi = intervalLow; intervalLow = tmp;
		}

		double scale = kernelDistribution.getScaler(1, scaleFactor);
		double to = intervalLow + scale * (intervalHi - intervalLow);
		double delta = to-intervalHi;
		
		int scaled = 0;
		for (int i = nodes.length/2+1; i < nodes.length; i++) {
			Node node = nodes[i];
			if (!node.isFake()) {
				// only change "real" internal nodes, not ancestral ones
				double h = node.getHeight();
				if (h > intervalLow && h <= intervalHi) {
					h = intervalLow + scale * (h-intervalLow);
					node.setHeight(h);
					scaled++;
				} else if (h > intervalHi) {				
					h += delta;
					node.setHeight(h);
				}
			}
		}
		
		if (scaled < 2) {
			// let L = intervalLow, U = intervalHi, s = scale
			// with 2 nodes between L and U and one node above U, L, U and s are uniquely determined
//			Given two nodes at height h1, h2 between U and L, then
//
//			h1' = L + s * (h1-L)
//			h2' = L + s * (h2-L)
//
//			Subtract second form first:
//
//			h1’-h2’ = s * (h1-h2) so s =  (h1’-h2’)/(h1-h2). Fill s in first equation gives L.
//
//			Given two more nodes above U at heights h3, h4
//
//			h3’ = h3 + delta
//
//			so delta = h3’ - h3
//
//			delta = U’ - U where U’ = L + s * (U-L) so
//			delta = L + s * (U-L) - U =>
//			delta = (1+s)L + (s-1)U
//			U = (delta - (1+s)L)/(s-1)
//
//			All are unique, so it takes just 2 nodes between U and L (assuming U < root height) to guarantee unique U, L and s.
			
			// with 1 node between L and U, 
			// there are multiple L,U and s values resulting in the same proposal
			// and we cannot guarantee HR is correct
			return Double.NEGATIVE_INFINITY;
		}
		
		for (Node node0 : nodes) {
			if (node0.getLength() < 0) {
				return Double.NEGATIVE_INFINITY;
			}
		}

		double logHR = scaled * Math.log(scale);
		return logHR;
    }

    
    
    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
    	if (optimiseInput.get()) {
	        double delta = calcDelta(logAlpha);
	        double scaleFactor = getCoercableParameterValue();
	        delta += Math.log(scaleFactor);
	        scaleFactor = Math.exp(delta);
	        setCoercableParameterValue(scaleFactor);
    	}
    }
    
    @Override
    public double getTargetAcceptanceProbability() {
    	return 0.4;
    }
    
    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
    	scaleFactor = value; // Math.max(Math.min(value, upper), lower);
    }


    @Override
    public String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = getCoercableParameterValue() * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.40) {
            return "Try setting scale factor to about " + formatter.format(newWindowSize);
        } else return "";
    }
}
