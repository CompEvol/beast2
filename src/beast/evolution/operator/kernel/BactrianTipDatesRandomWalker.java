package beast.evolution.operator.kernel;

import beast.base.Description;
import beast.base.Input;
import beast.evolution.operator.TipDatesRandomWalker;
import beast.evolution.tree.Node;
import beast.inference.operator.kernel.KernelDistribution;
import beast.util.Randomizer;

@Description("Randomly moves tip dates on a tree by randomly selecting one from (a subset of) taxa using a Bactrian proposal")
public class BactrianTipDatesRandomWalker extends TipDatesRandomWalker {
    public final Input<KernelDistribution> kernelDistributionInput = new Input<>("kernelDistribution", "provides sample distribution for proposals", 
    		KernelDistribution.newDefaultKernelDistribution());

    protected KernelDistribution kernelDistribution;

	@Override
	public void initAndValidate() {
    	kernelDistribution = kernelDistributionInput.get();

    	super.initAndValidate();
    }
	
    public double proposal() {
        // randomly select leaf node
        int i = Randomizer.nextInt(taxonIndices.length);
        Node node = treeInput.get().getNode(taxonIndices[i]);

        double value = node.getHeight();
        double newValue = value + kernelDistribution.getRandomDelta(i, value, windowSize);

        if (newValue > node.getParent().getHeight()) { // || newValue < 0.0) {
            if (reflectValue) {
                newValue = reflectValue(newValue, 0.0, node.getParent().getHeight());
            } else {
                return Double.NEGATIVE_INFINITY;
            }
        }
        if (newValue == value) {
            // this saves calculating the posterior
            return Double.NEGATIVE_INFINITY;
        }
        node.setHeight(newValue);

        return 0.0;
    }
}
