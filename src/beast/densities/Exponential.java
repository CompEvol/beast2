package beast.densities;


import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.math.distributions.Distribution;
import beast.math.distributions.ExponentialDistribution;

/**
 * @author Joseph Heled
 */

@Description("Exponential distribution: f(x;\\lambda) = \\lambda e^{-\\lambda x}, if x >= 0.")
public class Exponential extends ParametricDistribution {
	public Input<Valuable> m_lambda = new Input<Valuable>("lambda","rate parameter, defaults to 1");

    private double getLambda() {
        if (m_lambda.get() == null) {
            return 1;
        } else {
            double lambda = m_lambda.get().getArrayValue();
            if (lambda <= 0) {
                System.err.println("Exponential::Lambda should be positive not "+lambda+". Assigning default value.");
                lambda = 1;
            }
            return lambda;
        }
    }

    private ExponentialDistribution exponential;

	@Override
	public void initAndValidate() throws Exception {
        exponential = new ExponentialDistribution(getLambda());
    }

    @Override
    public Distribution getDistribution() {
        exponential.setLambda(getLambda());
        return exponential;
    }
}