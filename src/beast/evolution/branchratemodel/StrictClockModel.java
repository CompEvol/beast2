package beast.evolution.branchratemodel;

import beast.base.Description;
import beast.evolution.tree.Node;
import beast.inference.parameter.RealParameter;

/**
 * @author Alexei Drummond
 */

@Description("Defines a mean rate for each branch in the beast.tree.")
public class StrictClockModel extends BranchRateModel.Base {

    //public Input<RealParameter> muParameterInput = new Input<>("clock.rate", "the clock rate (defaults to 1.0)");

    RealParameter muParameter;

    @Override
    public void initAndValidate() {
        muParameter = meanRateInput.get();
        if (muParameter != null) {
            muParameter.setBounds(Math.max(0.0, muParameter.getLower()), muParameter.getUpper());
            mu = muParameter.getValue();
        }
    }

    @Override
    public double getRateForBranch(final Node node) {
        return mu;
    }

    @Override
    public boolean requiresRecalculation() {
        mu = muParameter.getValue();
        return true;
    }

    @Override
    protected void restore() {
        mu = muParameter.getValue();
        super.restore();
    }

    @Override
    protected void store() {
        mu = muParameter.getValue();
        super.store();
    }

    private double mu = 1.0;
}
