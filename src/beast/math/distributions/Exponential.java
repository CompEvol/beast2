package beast.math.distributions;


import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.ExponentialDistributionImpl;

@Description("Exponential distribution.  f(x;\\lambda) = \\lambda e^{-\\lambda x}, if x >= 0 " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Exponential extends ParametricDistribution {
    public Input<RealParameter> m_lambda = new Input<RealParameter>("lambda", "rate parameter, defaults to 1");

    static org.apache.commons.math.distribution.ExponentialDistribution m_dist = new ExponentialDistributionImpl(1);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    void refresh() {
        double fLambda;
        if (m_lambda.get() == null) {
            fLambda = 1;
        } else {
            fLambda = m_lambda.get().getValue();
            if (fLambda < 0) {
                System.err.println("Exponential::Lambda should be positive not " + fLambda + ". Assigning default value.");
                fLambda = 1;
            }
        }
        m_dist.setMean(fLambda);
    }

    @Override
    public ContinuousDistribution getDistribution() {
        refresh();
        return m_dist;
    }

} // class Exponential
