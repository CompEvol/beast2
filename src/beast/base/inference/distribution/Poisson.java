package beast.base.inference.distribution;


import org.apache.commons.math.distribution.PoissonDistributionImpl;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;


@Description("Poisson distribution, used as prior  f(k; lambda)=\\frac{lambda^k e^{-lambda}}{k!}  " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Poisson extends ParametricDistribution {
    final public Input<RealParameter> lambdaInput = new Input<>("lambda", "rate parameter, defaults to 1");

    org.apache.commons.math.distribution.PoissonDistribution dist = new PoissonDistributionImpl(1);


    // Must provide empty constructor for construction by XML. Note that this constructor DOES NOT call initAndValidate();
    public Poisson() {
    }

    public Poisson(RealParameter lambda) {

        try {
            initByName("lambda", lambda);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initByName lambda parameter when constructing Poisson instance.");
        }
    }

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    @SuppressWarnings("deprecation")
	void refresh() {
        double m_fLambda;
        if (lambdaInput.get() == null) {
            m_fLambda = 1;
        } else {
            m_fLambda = lambdaInput.get().getValue();
            if (m_fLambda < 0) {
                m_fLambda = 1;
            }
        }
        dist.setMean(m_fLambda);
    }

    @Override
    public org.apache.commons.math.distribution.Distribution getDistribution() {
        refresh();
        return dist;
    }
    
    @Override
    public double getMeanWithoutOffset() {
    	return lambdaInput.get().getValue();
    }
    
} // class Poisson
