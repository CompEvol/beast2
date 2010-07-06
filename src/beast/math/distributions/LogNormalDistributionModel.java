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
            MParameter.get().setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        if (SParameter.get() != null) {
            SParameter.get().setBounds(0.0, Double.POSITIVE_INFINITY);
        }
    }


    public Distribution getDistribution(State state) {

        logNormal.setM(state.getParameter(MParameter).getValue());
        logNormal.setS(state.getParameter(SParameter).getValue());
        return logNormal;
    }

    LogNormalDistribution logNormal = new LogNormalDistribution(0, 1);
}
