package beast.math.distributions;


import org.apache.commons.math.distribution.PoissonDistributionImpl;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;

@Description("Poisson distribution, used as prior  f(k; lambda)=\\frac{lambda^k e^{-lambda}}{k!}  " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Poisson extends ParametricDistribution {
    public Input<RealParameter> m_lambda = new Input<RealParameter>("lambda", "rate parameter, defaults to 1");

    static org.apache.commons.math.distribution.PoissonDistribution m_dist = new PoissonDistributionImpl(1);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    void refresh() {
        double m_fLambda;
        if (m_lambda.get() == null) {
            m_fLambda = 1;
        } else {
            m_fLambda = m_lambda.get().getValue();
            if (m_fLambda < 0) {
                m_fLambda = 1;
            }
        }
        m_dist.setMean(m_fLambda);
    }

    @Override
    public org.apache.commons.math.distribution.Distribution getDistribution() {
        refresh();
        return m_dist;
    }

} // class Poisson
