package beast.base.evolution.operator.kernel;

import java.text.DecimalFormat;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.operator.TreeOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.operator.kernel.KernelDistribution;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;

@Description("Randomly selects true internal tree node (i.e. not the root) and move node height uniformly in interval " +
        "restricted by the nodes parent and children.")
public class BactrianNodeOperator extends TreeOperator {
    final public Input<Double> scaleFactorInput = new Input<>("scaleFactor", "scaling factor: larger means more bold proposals", 0.1);
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);
    public final Input<KernelDistribution> kernelDistributionInput = new Input<>("kernelDistribution", "provides sample distribution for proposals", 
    		KernelDistribution.newDefaultKernelDistribution());

    protected KernelDistribution kernelDistribution;


    protected double scaleFactor;

	// empty constructor to facilitate construction by XML + initAndValidate
	public BactrianNodeOperator() {
	}
	
	public BactrianNodeOperator(Tree tree) {
	    try {
	        initByName(treeInput.getName(), tree);
	    } catch (Exception e) {
	        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
	        throw new RuntimeException("Failed to construct Uniform Tree Operator.");
	    }
	}
	
	@Override
	public void initAndValidate() {
    	kernelDistribution = kernelDistributionInput.get();
	    scaleFactor = scaleFactorInput.get();
	}

    /**
     * change the parameter and return the hastings ratio.
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {
        Tree tree = (Tree) InputUtil.get(treeInput, this);

        // randomly select internal node
        int nodeCount = tree.getNodeCount();
        
        // Abort if no non-root internal nodes
        if (tree.getInternalNodeCount()==1)
            return Double.NEGATIVE_INFINITY;
        
        Node node;
        do {
            int nodeNr = nodeCount / 2 + 1 + Randomizer.nextInt(nodeCount / 2);
            node = tree.getNode(nodeNr);
        } while (node.isRoot() || node.isLeaf());
        double upper = node.getParent().getHeight();
        double lower = Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
        
        double scale = kernelDistribution.getScaler(0, Double.NaN, scaleFactor);

        // transform value
        double value = node.getHeight();
        double y = (upper - value) / (value - lower);
        y *= scale;
        double newValue = (upper + lower * y) / (y + 1.0);
        
        if (newValue < lower || newValue > upper) {
        	return Double.NEGATIVE_INFINITY;
        	//throw new RuntimeException("programmer error: new value proposed outside range");
        }
        
        node.setHeight(newValue);

        double logHR = Math.log(scale) + 2.0 * Math.log((newValue - lower)/(value - lower));
        return logHR;
    }

    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(double value) {
    	scaleFactor = value;
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
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
    	return 0.3;
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
