package beast.evolution.nuc.branchratemodel;

import beast.core.Node;
import beast.core.Plugin;
import beast.core.Description;
import beast.core.State;

/**
 * @author Alexei Drummond
 */
@Description("Defines a mean rate for each branch in the beast.tree.")
public abstract class BranchRateModel extends Plugin {

    abstract double getRateForBranch(State state, Node node);
}
