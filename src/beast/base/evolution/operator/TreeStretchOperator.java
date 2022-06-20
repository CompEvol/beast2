package beast.base.evolution.operator;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;

@Description("Scales tree skewing parts to take tip dates in account")
public class TreeStretchOperator extends EpochFlexOperator {
    final public Input<RealParameter> meanRateInput = new Input<>("meanRate", "mean clock rate -- inversely scaled if specified");
	
	// TODO: make up/down version that takes clock rates in account
	
	private double [] oldLengths;
	private double logHR;
	private RealParameter meanRate = null;
	
	@Override
	public void initAndValidate() {
		super.initAndValidate();
		meanRate = meanRateInput.get();
	}
	
	@Override
	public double proposal() {
    	Tree tree = treeInput.get();
    	double oldHeight = tree.getRoot().getHeight();
		Node [] nodes = tree.getNodesAsArray();
		
		if (oldLengths == null) {
			oldLengths = new double[nodes.length];
		}

		for (int i = 0; i < nodes.length; i++) {
			oldLengths[i] = nodes[i].getLength();
		}

		double scale = kernelDistribution.getScaler(0, scaleFactor);
		logHR = 0;
		scale(tree.getRoot(), scale);

		
		// sanity check
		for (Node node0 : nodes) {
			if (node0.getLength() < 0) {
				return Double.NEGATIVE_INFINITY;
			}
		}
		
		if (meanRate != null) {
			double newHeight = tree.getRoot().getHeight();
			double newRate = meanRate.getValue() * oldHeight/newHeight;
			
			if (newRate < meanRate.getLower() || newRate > meanRate.getUpper()) {
				return Double.NEGATIVE_INFINITY;
			}
			meanRate.setValue(newRate);
			logHR += Math.log(newHeight/oldHeight);
		}
		
		return logHR;
	}

	/**
	 * scales the branch lengths instead of node heights
	 * mismatches between left and right branch are averaged  
	 */
	private void scale(Node node, double scale) {
		if (!node.isLeaf()) {
			for (Node child : node.getChildren()) {
				scale(child, scale);
			}
			if (!node.isFake()) {
				// only change "real" internal nodes, not ancestral ones
				Node left = node.getLeft();
				double h1 = left.getHeight() + oldLengths[left.getNr()] * scale;
				Node right = node.getRight();
				double h2 = right.getHeight() + oldLengths[right.getNr()] * scale;
				//h2 = Math.max(h1, h2);
				h2 = (h1+ h2)/2.0;
				logHR += Math.log(h2/node.getHeight());
				// logHR += Math.log(scale);
				node.setHeight(h2);
			}
		}
	}
	
}
