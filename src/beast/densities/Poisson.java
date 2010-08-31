package beast.densities;


import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.math.distributions.Distribution;
import beast.math.distributions.PoissonDistribution;

/**
 * @author Joseph Heled
 */

@Description("Poisson distribution: f(k; lambda)=\\frac{lambda^k e^{-lambda}}{k!}")
public class Poisson extends ParametricDistribution {
	public Input<Valuable> m_lambda = new Input<Valuable>("lambda", "rate parameter, defaults to 1");

    private PoissonDistribution poisson;

    private double getLambda() {
        double l = 1;
        final Valuable v = m_lambda.get();
        if ( v != null) {
            l =  v.getArrayValue();
            if( l <= 0 ) {
                System.err.println("Poisson::Lambda should be positive not "+l+". Assigning default value.");
                l= 1;
            }
        }
        return l;
    }

	@Override
	public void initAndValidate() throws Exception {
        poisson = new PoissonDistribution(getLambda());
    }

    @Override
    public Distribution getDistribution() {
        poisson.setMean(getLambda());

        return poisson;
    }
}
