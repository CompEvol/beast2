package beast.evolution.operators;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.parameter.RealParameter;
import beast.densities.ParametricDistribution;
import beast.math.distributions.Distribution;
import beast.util.Randomizer;

/**
 * @author Joseph Heled
 *         Date: 1/09/2010
 */
@Description("Populate a parameter with independet samples from a continuous density." +
        " If the parameter is multidimetional, one dimension is randomly chosen and changed.")
public class SampleDensity extends Operator {

    public Input<ParametricDistribution> m_dist = new Input<ParametricDistribution>("density",
            "the density to sample", Input.Validate.REQUIRED);

    public Input<RealParameter> parameterInput =
                new Input<RealParameter>("parameter", "the target parameter to populate.", Input.Validate.REQUIRED);

    @Override
    public void initAndValidate() {
    }

    @Override
    public double proposal() {
        final RealParameter param = parameterInput.get();
        final int dimension = param.getDimension();
        final int index = ( dimension == 1 ) ? 0 : Randomizer.nextInt(dimension);

        final Distribution density = m_dist.get().getDistribution();

        final double val = density.quantile(Randomizer.nextDouble());
        param.setValue(index, val);

        // in fact this is a Gibbs sampler ....
        return 0;
    }
}
