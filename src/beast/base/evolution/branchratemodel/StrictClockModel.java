package beast.base.evolution.branchratemodel;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.evolution.tree.Node;
import beast.base.inference.parameter.RealParameter;

/**
 * @author Alexei Drummond
 */

@Description("Defines a mean rate for each branch in the beast.tree.")
public class StrictClockModel extends BranchRateModel.Base {

    //public Input<RealParameter> muParameterInput = new Input<>("clock.rate", "the clock rate (defaults to 1.0)");

    Function muParameter;

    @Override
    public void initAndValidate() {
        muParameter = meanRateInput.get();
        if (muParameter != null) {
        	if (muParameter instanceof RealParameter) { 
        		RealParameter mu = (RealParameter) muParameter;
        		mu.setBounds(Math.max(0.0, mu.getLower()), mu.getUpper());
        	}
            mu = muParameter.getArrayValue();
        }
    }

    @Override
    public double getRateForBranch(final Node node) {
        return mu;
    }

    @Override
    public boolean requiresRecalculation() {
        mu = muParameter.getArrayValue();
        return true;
    }

    @Override
    protected void restore() {
        mu = muParameter.getArrayValue();
        super.restore();
    }

    @Override
    protected void store() {
        mu = muParameter.getArrayValue();
        super.store();
    }

    private double mu = 1.0;
}
