package beast.evolution.branchratemodel;

import beast.core.Description;
import beast.core.Function;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;

/**
 * @author Alexei Drummond
 */

@Description("Defines a mean rate for each branch in the beast.tree.")
public class StrictClockModel extends BranchRateModel.Base {

    // public Input<RealParameter> muParameterInput = new Input<>("clock.rate", "the
    // clock rate (defaults to 1.0)");

    protected Function muSource;
    private double mu = 1.0;

    @Override
    public void initAndValidate() {
        if (meanRateInput.get() != null) {
            muSource = meanRateInput.get();
            if (muSource instanceof RealParameter) {
                RealParameter muParameter = (RealParameter) muSource;
                muParameter.setBounds(Math.max(0.0, muParameter.getLower()), muParameter.getUpper());
            }
            mu = muSource.getArrayValue();
        }
    }

    @Override
    public double getRateForBranch(final Node node) {
        return mu;
    }

    @Override
    public boolean requiresRecalculation() {
        mu = muSource.getArrayValue();
        return true;
    }

    @Override
    protected void restore() {
        mu = muSource.getArrayValue();
        super.restore();
    }

    @Override
    protected void store() {
        mu = muSource.getArrayValue();
        super.store();
    }

}
