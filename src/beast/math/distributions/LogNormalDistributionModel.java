package beast.math.distributions;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;

/**
 * @author Alexei Drummond
 */
@Description("A log-normal distribution with mean and variance parameters.")
public class LogNormalDistributionModel extends ParametricDistribution {

    public Input<RealParameter> MParameter = new Input<RealParameter>("M", "M parameter of lognormal distribution. Equal to the mean of the log-transformed distribution.");
    public Input<RealParameter> SParameter = new Input<RealParameter>("S", "S parameter of lognormal distribution. Equal to the standard deviation of the log-transformed distribution.");

    public void initAndValidate() throws Exception {

        if (MParameter.get() != null) {
            if (MParameter.get().getLower() == null) {
                MParameter.get().setLower(Double.NEGATIVE_INFINITY);
            }
            if (MParameter.get().getUpper() == null) {
                MParameter.get().setUpper(Double.POSITIVE_INFINITY);
            }
        }

        if (SParameter.get() != null) {
            if (SParameter.get().getLower() == null) {
                SParameter.get().setLower(0.0);
            }
            if (SParameter.get().getUpper() == null) {
                SParameter.get().setUpper(Double.POSITIVE_INFINITY);
            }
        }
    }


    public Distribution getDistribution() {

        logNormal.setM(MParameter.get().getValue());
        logNormal.setS(SParameter.get().getValue());
        return logNormal;
    }

    @Override
    public boolean requiresRecalculation() {
    	return true;
    }
    @Override
    public void store() {
    	super.store();
    }
    @Override
    public void restore() {
    	super.restore();
    }
    LogNormalDistribution logNormal = new LogNormalDistribution(0, 1);
}
