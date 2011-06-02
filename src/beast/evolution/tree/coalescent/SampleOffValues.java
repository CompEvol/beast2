package beast.evolution.tree.coalescent;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.parameter.BooleanParameter;
import beast.core.parameter.RealParameter;
import beast.math.distributions.ParametricDistribution;
import beast.util.Randomizer;

/**
 * @author Joseph Heled
 *         Date: 2/03/2011
 */
@Description("Sample values from a distribution")
public class SampleOffValues extends Operator {
    public Input<RealParameter> m_values = new Input<RealParameter>("values", "vector of target values", Input.Validate.REQUIRED);

    public Input<BooleanParameter> m_indicators = new Input<BooleanParameter>("indicators", "Sample only entries which are 'off'");

    public Input<ParametricDistribution> m_distInput = new Input<ParametricDistribution>("dist",
            "distribution to sample from.", Input.Validate.REQUIRED);

    public void initAndValidate() {
    }
    
    @Override
    public double proposal() {
        final BooleanParameter indicators = m_indicators.get(this);
        final RealParameter data = m_values.get(this);
        final ParametricDistribution distribution = m_distInput.get();

        final int idim = indicators.getDimension();

        final int offset = (data.getDimension() - 1) == idim ? 1 : 0;
        assert offset == 1 || data.getDimension() == idim : "" + idim + " (?+1) != " + data.getDimension();

        // available locations for direct sampling
        int[] loc = new int[idim];
        int nLoc = 0;

        for (int i = 0; i < idim; ++i) {
            if (! indicators.getValue(i)) {
                loc[nLoc] = i + offset;
                ++nLoc;
            }
        }

        if (nLoc > 0) {
            final int index = loc[Randomizer.nextInt(nLoc)];
            try {
                final double val = distribution.inverseCumulativeProbability(Randomizer.nextDouble());
                data.setValue(index, val);
            } catch (Exception e) {
                // some distributions fail on extreme values - currently gamma
                return Double.NEGATIVE_INFINITY;
                //throw new OperatorFailedException(e.getMessage());
            }
        } else {
            // no non-active indicators
            return Double.NEGATIVE_INFINITY;
        }
        return 0;
    }
}
