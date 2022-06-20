package beast.base.evolution.branchratemodel;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.tree.Node;
import beast.base.inference.CalculationNode;
import beast.base.inference.parameter.RealParameter;

/**
 * @author Alexei Drummond
 */
@Description("Defines a mean rate for each branch in the beast.tree.")
public interface BranchRateModel {

    public double getRateForBranch(Node node);

    @Description(value = "Base implementation of a clock model.", isInheritable = false)
    public abstract class Base extends CalculationNode implements BranchRateModel {
        final public Input<RealParameter> meanRateInput = new Input<>("clock.rate", "mean clock rate (defaults to 1.0)");

        // empty at the moment but brings together the required interfaces
    }
}
