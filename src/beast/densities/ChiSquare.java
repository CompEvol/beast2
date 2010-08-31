package beast.densities;


import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.IntegerParameter;
import beast.math.distributions.Distribution;
import beast.math.distributions.ChiSquareDistribution;

@Description("Chi square distribution. sum of n random normal variables N(0,1). "
        + "  f(x; k) = \\frac{1}{2^{k/2}Gamma(k/2)} x^{k/2-1} e^{-x/2} ")
public class ChiSquare extends ParametricDistribution {
	public Input<IntegerParameter> m_df = new Input<IntegerParameter>("df", "degrees if freedin, defaults to 1");

    private double getDF() {
        if (m_df.get() == null) {
            return 1;
        } else {
            int df = m_df.get().getValue();
            if (df <= 0) {
                System.err.println("Chi Square::df should be positive not "+df+". Assigning default value.");
                df = 1;
            }
            return df;
        }
    }

    private ChiSquareDistribution chi;

	@Override
	public void initAndValidate() throws Exception {
        chi = new ChiSquareDistribution(getDF()) ;
    }

    @Override
    public Distribution getDistribution() {
        chi.setN(getDF());
        return chi;
    }
}