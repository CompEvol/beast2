package beast.math.distributions;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Distribution;
import beast.core.Function;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.State;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import org.apache.commons.math.MathException;


@Description("Produces prior (log) probability of value x." +
        "If x is multidimensional, the components of x are assumed to be independent, " +
        "so the sum of log probabilities of all elements of x is returned as the prior.")
public class Prior extends Distribution {
    final public Input<Function> m_x = new Input<>("x", "point at which the density is calculated", Validate.REQUIRED);
    final public Input<ParametricDistribution> distInput = new Input<>("distr", "distribution used to calculate prior, e.g. normal, beta, gamma.", Validate.REQUIRED);

    /**
     * shadows distInput *
     */
    protected ParametricDistribution dist;

    @Override
    public void initAndValidate() {
        dist = distInput.get();
        calculateLogP();
    }

    @Override
    public double calculateLogP() {
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
                    logP = Double.NEGATIVE_INFINITY;
                    return Double.NEGATIVE_INFINITY;
                }
            }
        }
        logP = dist.calcLogP(x);
        if (logP == Double.POSITIVE_INFINITY) {
            logP = Double.NEGATIVE_INFINITY;
        }
        return logP;
    }

    /**
     * return name of the parameter this prior is applied to *
     */
    public String getParameterName() {
        if (m_x.get() instanceof BEASTObject) {
            return ((BEASTObject) m_x.get()).getID();
        }
        return m_x.get() + "";
    }

    @Override
    public void sample(State state, Random random) {

        if (sampledFlag)
            return;

        sampledFlag = true;

        // Cause conditional parameters to be sampled

        ParametricDistribution dist = distInput.get();

        // sample distribution parameters
        Function x = m_x.get();
        if (x instanceof RealParameter) {
            sampleInputDistribution("x", (RealParameter) x, state, random);
        } else if (x instanceof IntegerParameter) {
            sampleInputDistribution("x", (IntegerParameter) x, state, random);
        } else {
            throw new RuntimeException("ERROR: Can't sample from a Function unless it can be cast to a StateNode.");
        }

        Double[] newx;
        try {
            newx = dist.sample(1)[0];

            if (x instanceof RealParameter) {
                for (int i = 0; i < newx.length; i++) {
                    ((RealParameter) x).setValue(i, newx[i]);
                }
            } else if (x instanceof IntegerParameter) {
                for (int i = 0; i < newx.length; i++) {
                    ((IntegerParameter) x).setValue(i, (int)Math.round(newx[i]));
                }
            }

        } catch (MathException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to sample!");
        }
    }

    @Override
    public List<String> getConditions() {
        List<String> conditions = new ArrayList<>();
        conditions.addAll(dist.getInputs().keySet());

        return conditions;
    }

    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add("x");

        return arguments;
    }
}
