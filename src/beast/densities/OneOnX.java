package beast.densities;

import beast.core.Description;
import beast.math.distributions.Distribution;
import beast.math.distributions.OneOnXdistribution;

@Description("OneOnX distribution f(x) = 1/x. ")
public class OneOnX extends ParametricDistribution {

	@Override
	public void initAndValidate() {
	}

    private Distribution distribution = new OneOnXdistribution();

    @Override
    public Distribution getDistribution() {
        return distribution;
    }
}
