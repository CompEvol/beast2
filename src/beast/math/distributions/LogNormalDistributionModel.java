package beast.math.distributions;

import beast.core.Parameter;
import beast.core.Input;
import beast.core.State;
import beast.core.Description;

/**
 * @author Alexei Drummond
 */
@Description("A lognormal distribution.")
public class LogNormalDistributionModel extends ParametricDistribution {

    public Input<Parameter> MParameter = new Input<Parameter>("M", "M parameter of lognormal distribution. Equal to the mean of the log-transformed distribution.");
    public Input<Parameter> SParameter = new Input<Parameter>("S", "S parameter of lognormal distribution. Equal to the standard deviation of the log-transformed distribution.");

    public void initAndValidate(State state) throws Exception {

        if (MParameter.get() != null) {
            MParameter.get().setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        if (SParameter.get() != null) {
            SParameter.get().setBounds(0.0, Double.POSITIVE_INFINITY);
        }
    }


    public Distribution getDistribution(State state) {

        logNormal.setM(state.getValue(MParameter));
        logNormal.setS(state.getValue(SParameter));
        return logNormal;
    }

    LogNormalDistribution logNormal = new LogNormalDistribution(0,1);
}
