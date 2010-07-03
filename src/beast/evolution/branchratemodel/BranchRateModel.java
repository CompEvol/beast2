package beast.evolution.branchratemodel;

import beast.core.*;
import beast.evolution.tree.Node;

/**
 * @author Alexei Drummond
 */
@Description("Defines a mean rate for each branch in the beast.tree.")
public interface BranchRateModel {

    double getRateForBranch(State state, Node node);

    public abstract class Base extends Plugin implements Cacheable {
        public void store(final int sample) {
        }

        public void restore(final int sample) {
        }

        
    }
}
