package beast.densities;

import beast.core.*;
import beast.core.Input.Validate;
import beast.math.distributions.Distribution;

import java.util.List;
import java.util.Random;

 /**
 * @author Joseph Heled
 */

@Description("Produces prior (log) probability of value(s). If the input x is a multidimensional parameter, " +
        "each of the dimensions is considered as a separate independent component.")
public class Prior extends Density {
    public Input<Valuable> m_x = new Input<Valuable>("x", "point at which the density is calculated", Validate.REQUIRED);
    public Input<ParametricDistribution> m_dist = new Input<ParametricDistribution>("density",
            "the prior density to sample.", Validate.REQUIRED);

    @Override
    public double calculateLogP() throws Exception {
        logP = 0.0;
        final Valuable val = m_x.get();
        final Distribution distribution = m_dist.get().getDistribution();
        for(int k = 0; k < val.getDimension(); ++k) {
            logP += distribution.logPdf(val.getArrayValue(k));
        }
        return logP;
    }

    @Override
    public boolean requiresRecalculation() {
        // we only get here when a StateNode input has changed, so are guaranteed recalculation is required.
        try {
            calculateLogP();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override public void sample(State state, Random random) {}
	@Override public List<String> getArguments() {return null;}
	@Override public List<String> getConditions() {return null;}
}
