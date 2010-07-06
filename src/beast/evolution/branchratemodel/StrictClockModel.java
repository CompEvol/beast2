package beast.evolution.branchratemodel;

import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;

/**
 * @author Alexei Drummond
 */

@Description("Defines a mean rate for each branch in the beast.tree.")
public class StrictClockModel extends BranchRateModel.Base {

    public Input<RealParameter> muParameter = new Input<RealParameter>("clock.rate", "the clock rate (defaults to 1.0)");

    @Override
    public void initAndValidate(State state) throws Exception {
        if (muParameter.get() != null) {
            muParameter.get().setBounds(0.0, Double.POSITIVE_INFINITY);
        }
    }

    public double getRateForBranch(State state, Node node) {
        return mu;
    }

    public void prepare(final State state) {
        if (muParameter.get() != null) {
            mu = state.getParameter(muParameter).getValue();
        }
    }

    private double mu = 1.0;
}
