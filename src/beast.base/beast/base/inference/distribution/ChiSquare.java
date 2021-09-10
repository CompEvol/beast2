package beast.base.inference.distribution;


import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.commons.math.distribution.ContinuousDistribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.parameter.IntegerParameter;



@Description("Chi square distribution, f(x; k) = \\frac{1}{2^{k/2}Gamma(k/2)} x^{k/2-1} e^{-x/2} " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class ChiSquare extends ParametricDistribution {
    final public Input<IntegerParameter> dfInput = new Input<>("df", "degrees if freedin, defaults to 1");

    org.apache.commons.math.distribution.ChiSquaredDistribution m_dist = new ChiSquaredDistributionImpl(1);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    @SuppressWarnings("deprecation")
	void refresh() {
        int dF;
        if (dfInput.get() == null) {
            dF = 1;
        } else {
            dF = dfInput.get().getValue();
            if (dF <= 0) {
                dF = 1;
            }
        }
        m_dist.setDegreesOfFreedom(dF);
    }

    @Override
    public ContinuousDistribution getDistribution() {
        refresh();
        return m_dist;
    }

} // class ChiSquare
