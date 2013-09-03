package beast.math.distributions;


import java.util.List;
import java.util.Random;

import beast.core.*;
import beast.core.Input.Validate;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;


@Description("Produces prior (log) probability of value x." +
        "If x is multidimensional, the components of x are assumed to be independent, " +
        "so the sum of log probabilities of all elements of x is returned as the prior.")
public class Prior extends Distribution {
    public Input<Function> m_x = new Input<Function>("x", "point at which the density is calculated", Validate.REQUIRED);
    public Input<ParametricDistribution> distInput = new Input<ParametricDistribution>("distr", "distribution used to calculate prior, e.g. normal, beta, gamma.", Validate.REQUIRED);

    /**
     * shadows m_distInput *
     */
    ParametricDistribution dist;

    @Override
    public void initAndValidate() throws Exception {
        dist = distInput.get();
        calculateLogP();
    }

    @Override
    public double calculateLogP() throws Exception {
        Function x = m_x.get();
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
        logP = dist.calcLogP(x);
        return logP;
    }
    
    /** return name of the parameter this prior is applied to **/
    public String getParameterName() {
    	if (m_x.get() instanceof BEASTObject) {
    		return ((BEASTObject) m_x.get()).getID();
    	}
    	return m_x.get() + "";
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
