package beast.inference.operator.kernel;

import beast.base.Description;
import beast.base.Input;
import beast.inference.Operator;


@Description("Operator with a flexible kernel distribution")
public abstract class KernelOperator extends Operator {
    public final Input<KernelDistribution> kernelDistributionInput = new Input<>("kernelDistribution", "provides sample distribution for proposals", 
    		KernelDistribution.newDefaultKernelDistribution());

    protected KernelDistribution kernelDistribution;

	@Override
	public void initAndValidate() {
    	kernelDistribution = kernelDistributionInput.get();
	}
}
