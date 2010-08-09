package beast.evolution.branchratemodel;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.evolution.tree.Node;

/**
 * @author Alexei Drummond
 */
@Description("Defines a mean rate for each branch in the beast.tree.")
public interface BranchRateModel {

    public double getRateForBranch(Node node);

    public abstract class Base extends CalculationNode implements BranchRateModel {
        // empty at the moment but brings together the required interfaces
    }
}
