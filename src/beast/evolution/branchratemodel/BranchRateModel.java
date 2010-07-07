package beast.evolution.branchratemodel;

import beast.core.Cacheable;
import beast.core.Description;
import beast.core.Plugin;
import beast.core.State;
import beast.evolution.tree.Node;

/**
 * @author Alexei Drummond
 */
@Description("Defines a mean rate for each branch in the beast.tree.")
public interface BranchRateModel {

    public double getRateForBranch(State state, Node node);

    public abstract class Base extends Plugin implements BranchRateModel, Cacheable {
        public void store(final int sample) {
        }

        public void restore(final int sample) {
        }

        public boolean isDirty(State state) {
        	return false;
        }
    }
}
