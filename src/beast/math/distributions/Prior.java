package beast.math.distributions;

import beast.core.*;
import beast.core.Input.Validate;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;

import java.util.List;
import java.util.Random;

@Description("Produces prior (log) probability of value x." +
        "If x is multidimensional, the components of x are assumed to be independent, " +
        "so the sum of log probabilities of all elements of x is returned as the prior.")
public class Prior extends Distribution {
    public Input<Valuable> m_x = new Input<Valuable>("x", "point at which the density is calculated", Validate.REQUIRED);
    public Input<ParametricDistribution> m_distInput = new Input<ParametricDistribution>("distr", "distribution used to calculate prior, e.g. normal, beta, gamma.", Validate.REQUIRED);

    /**
     * shadows m_distInput *
     */
    ParametricDistribution m_dist;

    @Override
    public void initAndValidate() {
        m_dist = m_distInput.get();
    }

    @Override
    public double calculateLogP() throws Exception {
        Valuable x = m_x.get();
        if (x instanceof RealParameter || x instanceof IntegerParameter) {
        	// test that parameter is inside its bounds
            double l = 0.0;
            double h = 0.0;
        	if (x instanceof RealParameter) {
                l = ((RealParameter) x).getLower();
                h = ((RealParameter) x).getUpper();
        	} else {
                l = ((IntegerParameter) x).getLower();
                h = ((IntegerParameter) x).getUpper();
        	}
            for (int i = 0; i < x.getDimension(); i++) {
            	double value = x.getArrayValue(i);
            	if (value < l || value > h) {
            		return Double.NEGATIVE_INFINITY;
            	}
            }
        }
        logP = m_dist.calcLogP(x);
        return logP;
    }

    @Override
    public void sample(State state, Random random) {
    }

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }
}
