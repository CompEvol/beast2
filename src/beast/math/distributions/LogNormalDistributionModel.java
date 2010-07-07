package beast.math.distributions;

import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.core.parameter.RealParameter;

/**
 * @author Alexei Drummond
 */
@Description("A lognormal distribution.")
public class LogNormalDistributionModel extends ParametricDistribution {

    public Input<RealParameter> MParameter = new Input<RealParameter>("M", "M parameter of lognormal distribution. Equal to the mean of the log-transformed distribution.");
    public Input<RealParameter> SParameter = new Input<RealParameter>("S", "S parameter of lognormal distribution. Equal to the standard deviation of the log-transformed distribution.");

    public void initAndValidate(State state) throws Exception {

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


    public Distribution getDistribution(State state) {

        logNormal.setM(state.getParameter(MParameter).getValue());
        logNormal.setS(state.getParameter(SParameter).getValue());
        return logNormal;
    }

    LogNormalDistribution logNormal = new LogNormalDistribution(0, 1);
}
