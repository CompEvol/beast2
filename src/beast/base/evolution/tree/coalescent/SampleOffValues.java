package beast.base.evolution.tree.coalescent;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Operator;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;

/**
 * @author Joseph Heled
 *         Date: 2/03/2011
 */
@Description("Sample values from a distribution")
public class SampleOffValues extends Operator {
    final public Input<RealParameter> valuesInput = new Input<>("values", "vector of target values", Input.Validate.REQUIRED);

    final public Input<BooleanParameter> indicatorsInput = new Input<>("indicators", "Sample only entries which are 'off'");

    final public Input<ParametricDistribution> distInput = new Input<>("dist",
            "distribution to sample from.", Input.Validate.REQUIRED);

    public final Input<Boolean> scaleAll =
            new Input<>("all", "if true, sample all off values in one go.", false);

    @Override
	public void initAndValidate() {
    }

    @Override
    public double proposal() {
        final BooleanParameter indicators = (BooleanParameter) InputUtil.get(indicatorsInput, this);
        final RealParameter data = (RealParameter) InputUtil.get(valuesInput, this);
        final ParametricDistribution distribution = distInput.get();

        final int idim = indicators.getDimension();

        final int offset = (data.getDimension() - 1) == idim ? 1 : 0;
        assert offset == 1 || data.getDimension() == idim : "" + idim + " (?+1) != " + data.getDimension();

        double hr = Double.NEGATIVE_INFINITY;

        if( scaleAll.get() ) {
            for (int i = offset; i < idim; ++i) {
                if( !indicators.getValue(i-offset) ) {
                    try {
                        final double val = distribution.inverseCumulativeProbability(Randomizer.nextDouble());
                        hr += distribution.logDensity(data.getValue(i));
                        data.setValue(i, val);
                    } catch (Exception e) {
                        // some distributions fail on extreme values - currently gamma
                        return Double.NEGATIVE_INFINITY;
                    }
                }
            }
        } else {

            // available locations for direct sampling
            int[] loc = new int[idim];
            int locIndex = 0;

            for (int i = 0; i < idim; ++i) {
                if( !indicators.getValue(i) ) {
                    loc[locIndex] = i + offset;
                    ++locIndex;
                }
            }

            if( locIndex > 0 ) {
                final int index = loc[Randomizer.nextInt(locIndex)];
                try {
                    final double val = distribution.inverseCumulativeProbability(Randomizer.nextDouble());
                    hr = distribution.logDensity(data.getValue(index));
                    data.setValue(index, val);
                } catch (Exception e) {
                    // some distributions fail on extreme values - currently gamma
                    return Double.NEGATIVE_INFINITY;
                    //throw new OperatorFailedException(e.getMessage());
                }
            } else {
                // no non-active indicators
                //return Double.NEGATIVE_INFINITY;
            }
        }
        return hr;
    }
}
