package beast.math.distributions;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.core.parameter.BooleanParameter;
import beast.core.parameter.RealParameter;

import java.util.List;
import java.util.Random;

@Description("Bernoulli distribution, used as prior or likelihood." +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class BernoulliDistribution extends Distribution {

    final public Input<RealParameter> pInput = new Input<>("p", "probability p parameter. Must be either " +
            "size 1 for iid trials, or the same dimension as trials parameter if inhomogeneous bernoulli process.", Input.Validate.REQUIRED);
    final public Input<BooleanParameter> trialsInput = new Input<>("parameter", "the results of a series of bernoulli trials.");

    public double calculateLogP() {
        logP = 0.0;

        BooleanParameter trials = trialsInput.get();
        RealParameter p = pInput.get();

        // for efficiency split the two options
        if (p.getDimension() == 1) {
            double prob = p.getArrayValue();
            double logProb = Math.log(prob);
            double log1MinusProb = Math.log(1.0-prob);

            for (int i = 0; i < trials.getDimension(); i++) {
                logP += trials.getValue(i) ? logProb : log1MinusProb;
            }

        } else {
            for (int i = 0; i < trials.getDimension(); i++) {
                double prob = p.getArrayValue(i);
                logP += Math.log(trials.getValue(i) ? prob : 1.0 - prob);
            }

        }
        return logP;
    }

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }

    @Override
    public void sample(State state, Random random) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initAndValidate() {
        if (pInput.get().getDimension() != 1 && pInput.get().getDimension() != trialsInput.get().getDimension()) {
            throw new RuntimeException("p parameter must be size 1 or the same size as trials parameter but it was dimension " + pInput.get().getDimension());
        }
    }
}
